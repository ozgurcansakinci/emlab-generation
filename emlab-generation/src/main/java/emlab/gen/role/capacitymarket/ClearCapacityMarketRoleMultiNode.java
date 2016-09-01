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
 * @author Salman
 * 
 */

@RoleComponent
public class ClearCapacityMarketRoleMultiNode extends AbstractRole<Regulator> implements Role<Regulator> {

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

        double reserveMargin = 1 + regulator.getReserveMargin() - phaseInPeriod;// 1.156
        double lowerMargin = reserveMargin - regulator.getReserveDemandLowerMargin();// 1.131

        double upperMargin = reserveMargin + regulator.getReserveDemandUpperMargin();// 1.181

        double demandTarget = regulator.getDemandTarget() / reserveMargin;

        double totalVolumeBid = 0;
        double totalContractedCapacity = 0;
        double clearingPrice = 0;

        if (regulator.getDemandTarget() == 0) {
            isTheMarketCleared = true;
            clearingPrice = 0;
        }

        for (CapacityDispatchPlan currentCDP : reps.capacityMarketRepository
                .findSortedCapacityDispatchPlansForMarketForTime(market, getCurrentTick())) {

            totalVolumeBid = totalVolumeBid + currentCDP.getAmount();
        }

        logger.warn("2 TotVolumeBidded " + totalVolumeBid + " TotVolAvailable "
                + reps.powerPlantRepository.calculatePeakCapacityOfNonIntermittentOperationalPowerPlantsInMarket(
                        reps.marketRepository.findElectricitySpotMarketForZone(regulator.getZone()), getCurrentTick())
                + " LowerMarginDemand " + (demandTarget * (lowerMargin)) + " UpperMarginDemand "
                + (demandTarget * (upperMargin)));

        if (totalVolumeBid <= (demandTarget * (lowerMargin)) && isTheMarketCleared == false) {

            // find all capacity dispatch plans and fully accept the capacity
            // they offer and set the price to market cap
            // means that there is a shortage of capacity in the market

            for (CapacityDispatchPlan currentCDP : reps.capacityMarketRepository
                    .findSortedCapacityDispatchPlansForMarketForTime(market, getCurrentTick())) {

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

        if (totalVolumeBid > (demandTarget * (lowerMargin)) && isTheMarketCleared == false) {

            // find all capacity dispatch plans and sort them according to price

            for (CapacityDispatchPlan currentCDP : reps.capacityMarketRepository
                    .findSortedCapacityDispatchPlansForMarketForTime(market, getCurrentTick())) {

                if ((totalContractedCapacity + currentCDP.getAmount()) <= (demandTarget * (lowerMargin))
                        && isTheMarketCleared == false) {

                    // accept all bids and add the total capacity which is what
                    // should be contracted
                    // again shortage in the market

                    currentCDP.setStatus(Bid.ACCEPTED);
                    currentCDP.setAcceptedAmount(currentCDP.getAmount());
                    totalContractedCapacity = totalContractedCapacity + currentCDP.getAmount();
                    // logger.warn("total capacity is:" +
                    // totalContractedCapacity);
                    if (currentCDP.getPlant() == null) {
                        logger.warn("2: Following bids got accepted: " + currentCDP.getStorage().getName() + " "
                                + currentCDP.getAcceptedAmount() + " " + currentCDP.getBiddingMarket());
                    } else {
                        logger.warn("2: Following bids got accepted: " + currentCDP.getPlant() + " "
                                + currentCDP.getAcceptedAmount() + " " + currentCDP.getBiddingMarket());
                    }
                    currentCDP.persist();
                    continue;
                }

                if ((totalContractedCapacity + currentCDP.getAmount()) > (demandTarget * (lowerMargin))
                        && isTheMarketCleared == false) {

                    if ((totalContractedCapacity + currentCDP.getAmount()) <= (demandTarget
                            * ((upperMargin) - ((currentCDP.getPrice() * (upperMargin - lowerMargin)) / marketCap)))) {
                        logger.warn("3 if condition: " + (demandTarget * ((upperMargin)
                                - ((currentCDP.getPrice() * (upperMargin - lowerMargin)) / marketCap))));
                        // accept all bids
                        logger.warn("bid price: " + currentCDP.getPrice());
                        currentCDP.setStatus(Bid.ACCEPTED);
                        currentCDP.setAcceptedAmount(currentCDP.getAmount());
                        totalContractedCapacity = totalContractedCapacity + currentCDP.getAmount();
                        // logger.warn("total capacity is:" +
                        // totalContractedCapacity);
                        if (currentCDP.getPlant() == null) {
                            logger.warn("3: Following bids got accepted: " + currentCDP.getStorage().getName() + " "
                                    + currentCDP.getAcceptedAmount() + " " + currentCDP.getBiddingMarket());
                        } else {
                            logger.warn("3: Following bids got accepted: " + currentCDP.getPlant() + " "
                                    + currentCDP.getAcceptedAmount() + " " + currentCDP.getBiddingMarket());
                        }
                        currentCDP.persist();
                        if (totalContractedCapacity >= (demandTarget * ((upperMargin)
                                - ((currentCDP.getPrice() * (upperMargin - lowerMargin)) / marketCap)))) {
                            clearingPrice = currentCDP.getPrice();
                            isTheMarketCleared = true;
                            // currentCDP.persist();
                        }
                        // continue;
                    }

                    else if ((totalContractedCapacity + currentCDP.getAmount()) > (demandTarget
                            * ((upperMargin) - ((currentCDP.getPrice() * (upperMargin - lowerMargin)) / marketCap)))) {
                        // logger.warn("4 if condition: " + (demandTarget *
                        // ((upperMargin)
                        // - ((currentCDP.getPrice() * (upperMargin -
                        // lowerMargin)) / marketCap))));
                        // logger.warn("bid price: " + currentCDP.getPrice());
                        double tempAcceptedAmount = 0;
                        tempAcceptedAmount = currentCDP.getAmount()
                                - ((totalContractedCapacity + currentCDP.getAmount()) - (demandTarget * ((upperMargin)
                                        - ((currentCDP.getPrice() * (upperMargin - lowerMargin)) / marketCap))));
                        // logger.warn("temp accepted amount: " +
                        // tempAcceptedAmount);
                        if (tempAcceptedAmount >= 0) {

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
                            logger.warn("total capacity is:" + totalContractedCapacity);
                            // market cleared

                            isTheMarketCleared = true;
                            currentCDP.persist();
                        }

                        // if the residual is less then zero, the capacity
                        // fulfils the demand

                        if (tempAcceptedAmount < 0) {

                            clearingPrice = -(marketCap / (upperMargin - lowerMargin))
                                    * ((totalContractedCapacity / demandTarget) - upperMargin);

                            isTheMarketCleared = true;
                        }

                        logger.warn("1 Pre " + (totalContractedCapacity + currentCDP.getAmount()) + " Edit "
                                + (totalContractedCapacity + currentCDP.getAcceptedAmount()));

                        logger.warn("2 true Price " + currentCDP.getPrice() + " accepted bid "
                                + currentCDP.getAcceptedAmount() + " bid qty " + currentCDP.getAmount());
                        continue;
                    }

                }

            }
            if (isTheMarketCleared == false) {
                clearingPrice = -(marketCap / (upperMargin - lowerMargin))
                        * ((totalContractedCapacity / demandTarget) - upperMargin);
                isTheMarketCleared = true;
            }

        }

        if (isTheMarketCleared == true) {
            for (CapacityDispatchPlan currentCDP : reps.capacityMarketRepository
                    .findSortedCapacityDispatchPlansForMarketForTime(market, getCurrentTick())) {
                if (currentCDP.getStatus() == Bid.SUBMITTED) {
                    currentCDP.setStatus(Bid.FAILED);
                    currentCDP.setAcceptedAmount(0);

                    if (currentCDP.getPlant() == null) {
                        logger.warn("Following bids failed: " + currentCDP.getStorage().getName() + " "
                                + currentCDP.getBiddingMarket());
                    } else {
                        logger.warn("Following bids failed: " + currentCDP.getPlant() + " "
                                + currentCDP.getBiddingMarket());
                    }
                }
            }
        }

        CapacityClearingPoint clearingPoint = new CapacityClearingPoint();
        clearingPoint.persist();

        if (isTheMarketCleared == true) {
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

        }

    }

}