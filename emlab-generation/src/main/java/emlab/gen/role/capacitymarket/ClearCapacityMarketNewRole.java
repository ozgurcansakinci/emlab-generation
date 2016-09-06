/*******************************************************************************
 * Copyright 2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package emlab.gen.role.capacitymarket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.AbstractRole;
import agentspring.role.Role;
import agentspring.role.RoleComponent;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.market.Bid;
import emlab.gen.domain.market.capacity.CapacityClearingPoint;
import emlab.gen.domain.market.capacity.CapacityDispatchPlan;
import emlab.gen.domain.market.capacity.CapacityMarket;
import emlab.gen.repository.Reps;

/**
 * @author Prad
 * 
 */

@RoleComponent
public class ClearCapacityMarketNewRole extends AbstractRole<Regulator>implements Role<Regulator> {

    // CapacityMarketRepository capacityMarketRepository;

    @Autowired
    Reps reps;

    @Autowired
    Neo4jTemplate template;

    @Override
    @Transactional
    public void act(Regulator regulator) {

        CapacityMarket market = new CapacityMarket();

        // Query get the capacity market for the zone linked to the regulator

        market = reps.capacityMarketRepository.findCapacityMarketForZone(regulator.getZone());

        double phaseInPeriod = 0;

        // implementation phase lenght is zero so this part of the code doesnt
        // run

        if (getCurrentTick() <= (long) regulator.getImplementationPhaseLength()
                && regulator.getImplementationPhaseLength() > 0) {
            phaseInPeriod = regulator.getReserveMargin()
                    - ((((regulator.getReserveMargin() - regulator.getInitialSupplyMargin())
                            / regulator.getImplementationPhaseLength()) * getCurrentTick())
                            + regulator.getInitialSupplyMargin());
            logger.warn("1 SET phase In " + phaseInPeriod);
        }

        logger.warn("2 TEst phase In " + phaseInPeriod);
        boolean isTheMarketCleared = false;

        double marketCap = regulator.getCapacityMarketPriceCap();
        // this is 58940
        double reserveMargin = 1 + regulator.getReserveMargin() - phaseInPeriod;
        // this is 1.156 - 0
        double lowerMargin = reserveMargin - regulator.getReserveDemandLowerMargin();
        // this is 1.156 - 0.025 = 1.131
        double upperMargin = reserveMargin + regulator.getReserveDemandUpperMargin();
        // this is 1.156 + 0.025 = 1.181
        double demandTarget = regulator.getDemandTarget() / reserveMargin;
        // this is 96990 / 1.156 = 83902
        double totalVolumeBid = 0;
        double totalContractedCapacity = 0;
        double clearingPrice = 0;

        // the below if statement is not true
        if (regulator.getDemandTarget() == 0) {
            isTheMarketCleared = true;
            clearingPrice = 0;
        }

        // the below query gets all the CDPs for all capacity markets for all
        // energy producers and sorts them by price, then adds all bidded
        // capacity

        // for (CapacityDispatchPlan currentCDP : reps.capacityMarketRepository
        // .findCapacityDispatchPlansForMarketForTime(market, getCurrentTick()))
        // {
        //
        // if (currentCDP.getPlant() == null) {
        // logger.warn(currentCDP.getBiddingMarket() + " " +
        // currentCDP.getStorage().getName() + " "
        // + currentCDP.getAmount() + " " + currentCDP.getPrice());
        // } else {
        // logger.warn(currentCDP.getBiddingMarket() + " " +
        // currentCDP.getPlant().getName() + " "
        // + currentCDP.getAmount() + " " + currentCDP.getPrice());
        // }
        // }

        for (CapacityDispatchPlan currentCDP : reps.capacityMarketRepository
                .findAllSortedCapacityDispatchPlansByTime(getCurrentTick())) {
            totalVolumeBid = totalVolumeBid + currentCDP.getAmount();

            // if (currentCDP.getPlant() == null) {
            // logger.warn(currentCDP.getBiddingMarket() + "------" +
            // currentCDP.getStorage().getName() + "------"
            // + currentCDP.getAmount() + " " + currentCDP.getPrice());
            // } else {
            // logger.warn(currentCDP.getBiddingMarket() + "------" +
            // currentCDP.getPlant().getName() + "------"
            // + currentCDP.getAmount() + "------" + currentCDP.getPrice());
            // }
        }

        // logger.warn("2 TotVol "
        // + totalVolumeBid
        // + " CalVol "
        // +
        // reps.powerPlantRepository.calculatePeakCapacityOfOperationalPowerPlantsInMarket(
        // reps.marketRepository.findElectricitySpotMarketForZone(regulator.getZone()),
        // getCurrentTick())
        // + " LMD " + (demandTarget * (lowerMargin)));

        logger.warn("2 TotVolumeBidded " + totalVolumeBid + " TotVolAvailable "
                + reps.powerPlantRepository.calculatePeakCapacityOfNonIntermittentOperationalPowerPlantsInMarket(
                        reps.marketRepository.findElectricitySpotMarketForZone(regulator.getZone()), getCurrentTick())
                + " LowerMarginDemand " + (demandTarget * (lowerMargin)) + " UpperMarginDemand "
                + (demandTarget * (upperMargin)));

        // if 279276 <= (83902*1.131) or 94893 & market is not cleared

        if (totalVolumeBid <= (demandTarget * (lowerMargin)) && isTheMarketCleared == false) {

            // find all capacity dispatch plans and fully accept the capacity
            // they offer and set the price to market cap
            // means that there is a shortage of capacity in the market

            for (CapacityDispatchPlan currentCDP : reps.capacityMarketRepository
                    .findAllSortedCapacityDispatchPlansByTime(getCurrentTick())) {
                currentCDP.setStatus(Bid.ACCEPTED);
                currentCDP.setAcceptedAmount(currentCDP.getAmount());
                clearingPrice = marketCap;
                totalContractedCapacity = totalVolumeBid;

                if (currentCDP.getPlant() == null) {
                    logger.warn("1: Following bids got accepted: " + currentCDP.getStorage().getName() + " "
                            + currentCDP.getAcceptedAmount() + " " + currentCDP.getBiddingMarket());
                } else {
                    logger.warn("1: Following bids got accepted: " + currentCDP.getPlant() + " "
                            + currentCDP.getAcceptedAmount() + " " + currentCDP.getBiddingMarket());
                }

            }

            isTheMarketCleared = true;

            // Capacity in shortage..... contract all bids....
        }

        // if 279276 > 94893 and market is still not cleared

        if (totalVolumeBid > (demandTarget * (lowerMargin)) && isTheMarketCleared == false) {

            // find all capacity dispatch plans and sort them according to price

            for (CapacityDispatchPlan currentCDP : reps.capacityMarketRepository
                    .findAllSortedCapacityDispatchPlansByTime(getCurrentTick())) {

                // if Total contracted capacity + capacity of the currentCDP is
                // less then equal to 94893 and market is not cleared

                if ((totalContractedCapacity + currentCDP.getAmount()) <= (demandTarget * (lowerMargin))
                        && isTheMarketCleared == false) {

                    // accept all bids and add the total capacity which is what
                    // should be contracted
                    // again shortage in the market

                    currentCDP.setStatus(Bid.ACCEPTED);
                    currentCDP.setAcceptedAmount(currentCDP.getAmount());
                    totalContractedCapacity = totalContractedCapacity + currentCDP.getAmount();

                    if (currentCDP.getPlant() == null) {
                        logger.warn("2: Following bids got accepted: " + currentCDP.getStorage().getName() + " "
                                + currentCDP.getAcceptedAmount() + " " + currentCDP.getBiddingMarket());
                    } else {
                        logger.warn("2: Following bids got accepted: " + currentCDP.getPlant() + " "
                                + currentCDP.getAcceptedAmount() + " " + currentCDP.getBiddingMarket());
                    }

                }

                // if the total capacity bidded is greater then 94893 and market
                // is not cleared

                if ((totalContractedCapacity + currentCDP.getAmount()) > (demandTarget * (lowerMargin))
                        && isTheMarketCleared == false) {

                    // if total capacity bidded is less then (83902 * (1.181 -
                    // (price * 0.05)/58940))) = 99088

                    if ((totalContractedCapacity + currentCDP.getAmount()) < (demandTarget
                            * ((upperMargin) - ((currentCDP.getPrice() * (upperMargin - lowerMargin)) / marketCap)))) {

                        // accept all bids

                        currentCDP.setStatus(Bid.ACCEPTED);
                        currentCDP.setAcceptedAmount(currentCDP.getAmount());
                        totalContractedCapacity = totalContractedCapacity + currentCDP.getAmount();

                        if (currentCDP.getPlant() == null) {
                            logger.warn("3: Following bids got accepted: " + currentCDP.getStorage().getName() + " "
                                    + currentCDP.getAcceptedAmount() + " " + currentCDP.getBiddingMarket());
                        } else {
                            logger.warn("3: Following bids got accepted: " + currentCDP.getPlant() + " "
                                    + currentCDP.getAcceptedAmount() + " " + currentCDP.getBiddingMarket());
                        }

                    }

                    // if total capacity bidded is greater then (83902 * (1.181
                    // -
                    // (price * 0.05)/58940))) = 99088

                    if ((totalContractedCapacity + currentCDP.getAmount()) > (demandTarget
                            * ((upperMargin) - ((currentCDP.getPrice() * (upperMargin - lowerMargin)) / marketCap)))) {

                        double tempAcceptedAmount = 0;
                        tempAcceptedAmount = currentCDP.getAmount()
                                - ((totalContractedCapacity + currentCDP.getAmount()) - (demandTarget * ((upperMargin)
                                        - ((currentCDP.getPrice() * (upperMargin - lowerMargin)) / marketCap))));

                        // accepted amount = capacity bidded - (total capacity +
                        // amount) - 99088)

                        // in other words contract only what is needed to fulfil
                        // 99088

                        if (tempAcceptedAmount >= 0) {

                            // partly accept the last bid that fulfils 99088 and
                            // set the residual amount as capacity contracted
                            // for that bid

                            currentCDP.setStatus(Bid.PARTLY_ACCEPTED);

                            currentCDP.setAcceptedAmount(currentCDP.getAmount() - ((totalContractedCapacity
                                    + currentCDP.getAmount())
                                    - (demandTarget * ((upperMargin)
                                            - ((currentCDP.getPrice() * (upperMargin - lowerMargin)) / marketCap)))));

                            if (currentCDP.getPlant() == null) {
                                logger.warn("4: Following bids got partially accepted: "
                                        + currentCDP.getStorage().getName() + " " + currentCDP.getAcceptedAmount() + " "
                                        + currentCDP.getBiddingMarket());
                            } else {
                                logger.warn("4: Following bids got partially accepted: " + currentCDP.getPlant() + " "
                                        + currentCDP.getAcceptedAmount() + " " + currentCDP.getBiddingMarket());
                            }

                            // price bid by this last bid is the clearing price

                            clearingPrice = currentCDP.getPrice();

                            // add the capacity of this bid to the total
                            // capacity

                            totalContractedCapacity = totalContractedCapacity + currentCDP.getAcceptedAmount();

                            // market cleared

                            isTheMarketCleared = true;
                            currentCDP.persist();
                        }

                        // if the residual is less then zero, the capacity
                        // fulfils he demand

                        if (tempAcceptedAmount < 0) {

                            // price = - 58940/0.05 * (total capacity
                            // bidded/83902 - 1.181)

                            clearingPrice = -(marketCap / (upperMargin - lowerMargin))
                                    * ((totalContractedCapacity / demandTarget) - upperMargin);

                            isTheMarketCleared = true;
                        }

                        logger.warn("1 Pre " + (totalContractedCapacity + currentCDP.getAmount()) + " Edit "
                                + (totalContractedCapacity + currentCDP.getAcceptedAmount()));

                        logger.warn("2 true Price " + currentCDP.getPrice() + " accepted bid "
                                + currentCDP.getAcceptedAmount() + " bid qty " + currentCDP.getAmount());
                    }

                }

            }
            if (isTheMarketCleared == false)

            {
                clearingPrice = -(marketCap / (upperMargin - lowerMargin))
                        * ((totalContractedCapacity / demandTarget) - upperMargin);
                isTheMarketCleared = true;
            }

        }

        if (isTheMarketCleared == true) {
            for (

            CapacityDispatchPlan currentCDP : reps.capacityMarketRepository.findAllSortedCapacityDispatchPlansByTime(

            getCurrentTick())) {
                if (currentCDP.getStatus() == Bid.SUBMITTED) {
                    currentCDP.setStatus(Bid.FAILED);

                    logger.warn(
                            "Following bids failed: " + currentCDP.getPlant() + " " + currentCDP.getBiddingMarket());

                    if (currentCDP.getPlant() == null)
                        logger.warn("Following bids failed: " + currentCDP.getStorage().getName() + " "
                                + currentCDP.getBiddingMarket());

                    currentCDP.setAcceptedAmount(0);
                }
            }
        }

        CapacityClearingPoint clearingPoint = new CapacityClearingPoint();
        clearingPoint.persist();

        // persistClearingPoint(clearingPoint, clearingPrice, marketCap,
        // totalContractedCapacity, market,
        // isTheMarketCleared);

        // persistClearingPoint(clearingPrice, marketCap,
        // totalContractedCapacity, market, isTheMarketCleared);

        if (isTheMarketCleared == true)

        {
            if (clearingPrice > marketCap) {
                clearingPoint.setPrice(marketCap);
            } else {
                clearingPoint.setPrice(clearingPrice);
            }

            // logger.warn("MARKET CLEARED at price" +
            // clearingPoint.getPrice());
            clearingPoint.setVolume(totalContractedCapacity);
            clearingPoint.setTime(getCurrentTick());
            clearingPoint.setCapacityMarket(market);
            clearingPoint.persist();

            // logger.warn("MARKET CLEARED at Volume" +
            // clearingPoint.getVolume());
            // logger.warn("MARKET CLEARED at time" + clearingPoint.getTime());

            logger.warn("Clearing point Volume {} and Price {}", clearingPoint.getVolume(), clearingPoint.getPrice());

            // System.exit(0);

            // try {
            // Thread.sleep(10000);
            // } catch (InterruptedException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
            // }

        }

    }

    // public void persistClearingPoint(CapacityClearingPoint clearingPoint,
    // double clearingPrice, double marketCap,
    // double totalContractedCapacity, CapacityMarket market, boolean
    // isTheMarketCleared) {

    // @Transactional
    // public void persistClearingPoint(double clearingPrice, double marketCap,
    // double totalContractedCapacity,
    // CapacityMarket market, boolean isTheMarketCleared) {
    // CapacityClearingPoint clearingPoint = new CapacityClearingPoint();
    // if (isTheMarketCleared == true) {
    // if (clearingPrice > marketCap) {
    // clearingPoint.setPrice(marketCap);
    // } else {
    // clearingPoint.setPrice(clearingPrice);
    // }
    // clearingPoint.setVolume(totalContractedCapacity);
    // clearingPoint.setTime(getCurrentTick());
    // clearingPoint.setCapacityMarket(market);
    // clearingPoint.persist();
    // }
    //
    // }
}