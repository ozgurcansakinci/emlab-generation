/*******************************************************************************
 * Copyright 2012 the original author or authors.
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
package emlab.gen.role.investment;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.AbstractRole;
import agentspring.role.RoleComponent;
import emlab.gen.domain.agent.CommoditySupplier;
import emlab.gen.domain.agent.Government;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.agent.StrategicReserveOperator;
import emlab.gen.domain.contract.CashFlow;
import emlab.gen.domain.contract.Loan;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.ClearingPoint;
import emlab.gen.domain.market.DecarbonizationMarket;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.PpdpAnnual;
import emlab.gen.domain.market.electricity.Segment;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.domain.technology.Substance;
import emlab.gen.domain.technology.SubstanceShareInFuelMix;
import emlab.gen.repository.Reps;
import emlab.gen.repository.StrategicReserveOperatorRepository;
import emlab.gen.util.MapValueComparator;
import emlab.gen.util.MapValueReverseComparator;

/**
 * @author pradyumnabhagwat
 * 
 */

@RoleComponent
public class DismantlePowerPlantOperationalLossRole extends AbstractRole<ElectricitySpotMarket> {

    @Autowired
    Reps reps;

    @Autowired
    StrategicReserveOperatorRepository strategicReserveOperatorRepository;

    public Reps getReps() {
        return reps;
    }

    @Transactional
    public void act(ElectricitySpotMarket market) {

        // if (getCurrentTick() > 0) {
        for (PowerPlant plant : reps.powerPlantRepository.findOperationalPowerPlantsInMarket(market,
                getCurrentTick())) {

            double age = 0;
            long currentLiftime = 0;

            currentLiftime = getCurrentTick() - plant.getConstructionStartTime()
                    - plant.getTechnology().getExpectedLeadtime() - plant.getTechnology().getExpectedPermittime();

            plant.setActualLifetime(currentLiftime);

            age = (double) plant.getActualLifetime() / (((double) plant.getTechnology().getExpectedLifetime()));

            plant.setAgeFraction((double) age);

            if (plant.getAgeFraction() > 1.00D) {

                double ModifiedOM = plant.getActualFixedOperatingCost()
                        * Math.pow((1 + (plant.getTechnology().getFixedOperatingCostModifierAfterLifetime())),
                                ((double) plant.getActualLifetime()
                                        - (((double) plant.getTechnology().getExpectedLifetime()))));

                plant.setActualFixedOperatingCost(ModifiedOM);
            }

            long yearIterator = 1;

            double profitability = 0;
            double totalInvestment = 0;
            for (yearIterator = 1; yearIterator <= market.getLookback() && yearIterator > 0; yearIterator++) {
                // for (yearIterator = 0; yearIterator <= market.getLookback()
                // && yearIterator >= 0; yearIterator++) {
                double profit = 0;
                double plantMarginalCost = 0;
                double cost = 0;
                double revenue = 0;
                double energyGenerated = 0;
                double calculatedOM = 0;
                // logger.warn("Tick:" + getCurrentTick());
                if ((getCurrentTick() - yearIterator) >= 0) {

                    // for (Segment currentSegment :
                    // reps.segmentRepository.findAll()) {
                    //
                    // PowerPlantDispatchPlan ppdp = new
                    // PowerPlantDispatchPlan();
                    // ppdp = reps.powerPlantDispatchPlanRepository
                    // .findOnePowerPlantDispatchPlanForPowerPlantForSegmentForTime(plant,
                    // currentSegment,
                    // getCurrentTick() - yearIterator, false);
                    //
                    // if (ppdp != null) {
                    //
                    // double segmentMC = 0;
                    // double mc = 0;
                    // double acceptedAmount = 0;
                    // double energyInSegment = 0;
                    //
                    // acceptedAmount = ppdp.getAcceptedAmount();
                    // mc = calculateMarginalCostExclCO2MarketCost(plant,
                    // getCurrentTick());
                    // segmentMC = mc * acceptedAmount *
                    // currentSegment.getLengthInHours();
                    // energyInSegment = acceptedAmount *
                    // currentSegment.getLengthInHours();
                    // plantMarginalCost += segmentMC;
                    // energyGenerated += energyInSegment;
                    // }
                    //
                    // }
                    // for (YearlySegment currentSegment :
                    // reps.yearlySegmentRepository.findAll()) {
                    PpdpAnnual ppdp = new PpdpAnnual();
                    ppdp = reps.ppdpAnnualRepository.findPPDPAnnualforPlantForCurrentTick(plant,
                            getCurrentTick() - yearIterator);
                    if (ppdp != null) {

                        double segmentMC = 0;
                        double mc = 0;
                        double acceptedAmount = 0;
                        double energyInSegment = 0;

                        acceptedAmount = ppdp.getYearlySupply();
                        mc = calculateMarginalCostExclCO2MarketCost(plant, getCurrentTick() - yearIterator);
                        segmentMC = mc * acceptedAmount * (acceptedAmount / plant.getActualNominalCapacity());
                        energyInSegment = acceptedAmount * (acceptedAmount / plant.getActualNominalCapacity());
                        plantMarginalCost += segmentMC;
                        energyGenerated += energyInSegment;
                        // logger.warn("ppdp accepted amount " +
                        // acceptedAmount);
                    }

                    // }

                    for (CashFlow cf : reps.cashFlowRepository.findAllCashFlowsForPowerPlantForTime(plant,
                            (getCurrentTick() - yearIterator))) {

                        if (cf.getType() == CashFlow.FIXEDOMCOST) {
                            calculatedOM = cf.getMoney();
                        }

                        if (cf.getType() == CashFlow.COMMODITY || cf.getType() == CashFlow.CO2TAX
                                || cf.getType() == CashFlow.CO2AUCTION) {

                            cost = cost + cf.getMoney();
                        }

                        if (cf.getType() == CashFlow.ELECTRICITY_SPOT
                                || cf.getType() == CashFlow.SIMPLE_CAPACITY_MARKET) {
                            revenue = revenue + cf.getMoney();
                        }
                        if (cf.getType() == CashFlow.STRRESPAYMENT && cf.getTo().equals(plant.getOwner())) {
                            revenue = revenue + cf.getMoney();
                        }

                    }

                    // -
                    // (plant.getTechnology().getVariableOperatingCostinEURPerMWh()
                    // * 8760 * plant.getTechnology().getCapacity())) +
                    // (plant.getTechnology().getVariableOperatingCostinEURPerMWh()
                    // * energyGenerated

                    double actualOM = (calculatedOM);

                    cost = cost + plantMarginalCost + actualOM;
                    profit = (revenue - cost);
                    // logger.warn("profits " + profit);

                }
                profitability += profit;
                totalInvestment = plant.getTechnology().getInvestmentCost(plant.getConstructionStartTime())
                        * plant.getActualNominalCapacity();
            }

            plant.setProfitability((profitability / totalInvestment));

            // logger.warn("1 ROI " + plant.getProfitability());
            // logger.warn("prof " + plant.getProfitability() + " plant " +
            // plant.getName());
        }

        for (PowerPlant plant : reps.powerPlantRepository.findOperationalPowerPlantsInMarket(market,
                getCurrentTick())) {

            if (plant.getOwner().equals(reps.targetInvestorRepository.findTargetInvestorByMarket(market))) {

                // logger.warn(" *********************In target investor
                // loop");

                double prolongYearsOfDismantlng = plant.getTechnology().getMaximumLifeExtension()
                        + plant.getTechnology().getExpectedLifetime();

                // logger.warn(" Plant Actual Life time: " +
                // plant.getActualLifetime() + " prolongYearsOfDismantlng: "
                // + prolongYearsOfDismantlng);

                if (plant.getActualLifetime() > (prolongYearsOfDismantlng)) {

                    logger.warn(" **********************  OLD PLANT DISMANTLED: " + plant.getName() + " Age "
                            + plant.getActualLifetime());
                    plant.dismantlePowerPlant(getCurrentTick());

                }
                // } else {
                // int prolongYearsOfDismantlng = plant.getOwner()
                // .getDismantlingProlongingYearsAfterTechnicalLifetime();
                // if (!plant.isWithinTechnicalLifetime(getCurrentTick() +
                // prolongYearsOfDismantlng)) {
                // logger.warn(
                // " Dismantling power plant because the technical life time
                // has
                // passed: " + plant);
                // plant.dismantlePowerPlant(getCurrentTick());
                // }
                // }
            }
        }

        Map<PowerPlant, Double> marginalCostMap = new HashMap<PowerPlant, Double>();
        Map<PowerPlant, Double> meritOrder;

        // for (PowerPlant plant1 :
        // reps.powerPlantRepository.findExpectedOperationalPowerPlantsInMarket(market,
        // getCurrentTick())) {

        // for (PowerPlant plant1 : reps.powerPlantRepository
        // .findExpectedOperationalNonIntermittentPowerPlantsInMarket(market,
        // getCurrentTick())) {
        // marginalCostMap.put(plant1,
        // calculateMarginalCostExclCO2MarketCost(plant1,
        // (getCurrentTick())));
        //
        // }
        for (PowerPlant plant1 : reps.powerPlantRepository
                .findExpectedOperationalNonIntermittentPowerPlantsInMarket(market, getCurrentTick())) {
            marginalCostMap.put(plant1, calculateMarginalCostExclCO2MarketCost(plant1, (getCurrentTick())));

        }

        MapValueComparator comp = new MapValueComparator(marginalCostMap);
        meritOrder = new TreeMap<PowerPlant, Double>(comp);
        meritOrder.putAll(marginalCostMap);

        // for (PowerPlant plant : reps.powerPlantRepository
        // .findOperationalPowerPlantsByAscendingProfitabilityAndMarket(market,
        // getCurrentTick())) {
        for (PowerPlant plant : reps.powerPlantRepository
                .findOperationalNonIntermittentPowerPlantsByAscendingProfitabilityAndMarketExcludingTargetInvestor(
                        market, getCurrentTick())) {

            // logger.warn("profitability " + plant.getProfitability());

            if (plant.getProfitability() < 0 && plant.getActualLifetime() > 4) {

                // double totalInvestment =
                // plant.getTechnology().getInvestmentCost(plant.getConstructionStartTime())
                // * plant.getActualNominalCapacity();
                // logger.warn("total investment " + totalInvestment);
                // if (plant.getProfitability() < (totalInvestment *
                // -0.01))
                // {
                double totalProfit = 0;
                long lookforward = 1;
                long iterator = 0;
                // for (iterator = 0; iterator <= lookforward;
                // iterator++) {
                // logger.warn("year" + (getCurrentTick() + iterator));
                // Marginal cost Map was here earlier

                double mc = 0;
                double OM = 0;
                double sumProfit = 0;
                double energy = 0;

                double demandGrowthFactor = 0;
                double assignment = 0;

                mc = calculateMarginalCostExclCO2MarketCost(plant, (getCurrentTick() + iterator));

                OM = plant.getActualFixedOperatingCost();

                // if ((getCurrentTick() + iterator) == 1) {
                // demandGrowthFactor =
                // (market.getDemandGrowthTrend().getValue((getCurrentTick()
                // + iterator)));
                // }
                if ((getCurrentTick() + iterator) > 0) {
                    // SimpleRegression sr = new SimpleRegression();
                    // for (long time = (getCurrentTick() + iterator) -
                    // 1;
                    // time >= (getCurrentTick() + iterator)
                    // -
                    // market.getBacklookingForDemandForecastinginDismantling()
                    // && time >= 0; time = time - 1) {
                    // sr.addData(time,
                    // market.getDemandGrowthTrend().getValue(time));
                    // }
                    // demandGrowthFactor =
                    // (market.getDemandGrowthTrend().getValue((getCurrentTick()
                    // + iterator)));
                    // }
                    int iteration = 0;
                    double avgGrowthFactor = 0;
                    for (long time = (getCurrentTick() + iterator) - 1; time >= (getCurrentTick() + iterator)
                            - market.getBacklookingForDemandForecastinginDismantling(); time = time - 1) {
                        iteration++;
                        if (time >= 0)
                            avgGrowthFactor += market.getDemandGrowthTrend().getValue(time);
                        else // TODO:not sure if we should take 0th tick
                             // or
                             // current tick
                            avgGrowthFactor += market.getDemandGrowthTrend().getValue(0);
                    }
                    // demandGrowthFactor = Math.pow(avgGrowthFactor /
                    // iteration,
                    // (double) (getCurrentTick() + iterator));
                    demandGrowthFactor = (avgGrowthFactor / iteration);
                }

                // Could not understand the purpose of the following
                // block,
                // so commented it out!
                // double range = 0;
                //
                // if ((getCurrentTick() + iterator) == 1) {
                // // double max =
                // //
                // reps.powerPlantRepository.calculateBaseCapacityOfOperationalPowerPlantsInMarket(
                // // market, (getCurrentTick() + iterator));
                // double max = reps.powerPlantRepository
                // .calculateBaseCapacityOfNonIntermittentOperationalPowerPlantsInMarket(market,
                // (getCurrentTick() + iterator));
                // // double min =
                // //
                // reps.powerPlantRepository.calculatePeakCapacityOfOperationalPowerPlantsInMarket(
                // // market, (getCurrentTick() + iterator));
                // double min = reps.powerPlantRepository
                // .calculatePeakCapacityOfNonIntermittentOperationalPowerPlantsInMarket(market,
                // (getCurrentTick() + iterator));
                // range = ((max + min) / 2);
                // }
                //
                // if ((getCurrentTick() + iterator) > 1) {
                // SimpleRegression sr = new SimpleRegression();
                // for (long time = (getCurrentTick() + iterator) - 1;
                // time
                // >= (getCurrentTick() + iterator)
                // -
                // market.getBacklookingForDemandForecastinginDismantling()
                // && time >= 0; time = time - 1) {
                // // double max = reps.powerPlantRepository
                // //
                // .calculateBaseCapacityOfOperationalPowerPlantsInMarket(market,
                // // time);
                // // double min = reps.powerPlantRepository
                // //
                // .calculatePeakCapacityOfOperationalPowerPlantsInMarket(market,
                // // time);
                // double max = reps.powerPlantRepository
                // .calculateBaseCapacityOfNonIntermittentOperationalPowerPlantsInMarket(market,
                // (getCurrentTick() + iterator));
                // double min = reps.powerPlantRepository
                // .calculatePeakCapacityOfNonIntermittentOperationalPowerPlantsInMarket(market,
                // (getCurrentTick() + iterator));
                // sr.addData(time, ((max + min) / 2));
                //
                // }
                //
                // range = (sr.predict((getCurrentTick() + iterator)));
                // }

                // * ((100 + r.nextGaussian() * 20) / 100);

                double reservePrice = 0;
                double reserveVolume = 0;
                for (StrategicReserveOperator operator : strategicReserveOperatorRepository.findAll()) {
                    ElectricitySpotMarket market1 = reps.marketRepository
                            .findElectricitySpotMarketForZone(operator.getZone());
                    if (market.getNodeId().intValue() == market1.getNodeId().intValue()) {
                        reservePrice = operator.getReservePriceSR();
                        reserveVolume = operator.getReserveVolume();
                    }
                }

                // for (Segment currentSegment :
                // reps.segmentRepository.findAll()) {
                for (SegmentLoad segmentL : market.getLoadDurationCurve()) {
                    Segment currentSegment = segmentL.getSegment();
                    // for (Segment currentSegment :
                    // reps.segmentRepository.findAll()) {
                    double segmentCapacity = 0;
                    // double segmentLoad = demandGrowthFactor *
                    // reps.segmentLoadRepository
                    // .returnSegmentBaseLoadBySegmentAndMarket(currentSegment,
                    // market);
                    double segmentLoad = demandGrowthFactor * segmentL.getResidualGLDC();

                    // for (PowerPlant pplants :
                    // reps.powerPlantRepository.findExpectedOperationalPowerPlantsInMarket(
                    // market, getCurrentTick())) {
                    // segmentCapacity = segmentCapacity
                    // + pplants.getAvailableCapacity((getCurrentTick()
                    // +
                    // iterator), currentSegment,
                    // reps.segmentRepository.count());
                    // }
                    segmentCapacity = reps.powerPlantRepository
                            .calculateBaseCapacityOfNonIntermittentOperationalPowerPlantsInMarket(market,
                                    getCurrentTick());
                    if (segmentLoad > (segmentCapacity)) {
                        double price = 0;
                        double profit1 = 0;
                        price = market.getValueOfLostLoad();
                        profit1 = currentSegment.getLengthInHoursGLDCForInvestmentRole()
                                * plant.getAvailableCapacity((getCurrentTick() + iterator), currentSegment,
                                        reps.segmentRepository.count())
                                * (price - mc);

                        sumProfit += profit1;
                        energy += currentSegment.getLengthInHoursGLDCForInvestmentRole() * plant.getAvailableCapacity(
                                (getCurrentTick() + iterator), currentSegment, reps.segmentRepository.count());
                    }
                    if (segmentLoad <= (segmentCapacity) && ((segmentCapacity - segmentLoad) <= (reserveVolume))) {
                        double price = 0;
                        double profit1 = 0;
                        price = reservePrice;
                        profit1 = currentSegment.getLengthInHoursGLDCForInvestmentRole()
                                * plant.getAvailableCapacity((getCurrentTick() + iterator), currentSegment,
                                        reps.segmentRepository.count())
                                * (price - mc);

                        sumProfit += profit1;
                        energy += currentSegment.getLengthInHoursGLDCForInvestmentRole() * plant.getAvailableCapacity(
                                (getCurrentTick() + iterator), currentSegment, reps.segmentRepository.count());
                    }

                    if (segmentLoad <= (segmentCapacity) && ((segmentCapacity - segmentLoad) > (reserveVolume))) {
                        double price = 0;
                        double capacityCounter = 0;

                        for (Entry<PowerPlant, Double> plantCost : meritOrder.entrySet()) {
                            PowerPlant plant1 = plantCost.getKey();

                            if (capacityCounter < segmentLoad) {
                                if (!plant1.getTechnology().isIntermittent()) {
                                    capacityCounter += plant1.getAvailableCapacity((getCurrentTick() + iterator),
                                            currentSegment, reps.segmentRepository.count());
                                    price = plantCost.getValue();
                                }
                            }

                        }
                        // logger.warn("CP " + price + " MC " + mc);
                        if (price > mc) {
                            double profit1 = 0;
                            if (!plant.getTechnology().isIntermittent()) {
                                profit1 = currentSegment.getLengthInHoursGLDCForInvestmentRole()
                                        * plant.getAvailableCapacity((getCurrentTick() + iterator), currentSegment,
                                                reps.segmentRepository.count())
                                        * (price - mc);
                                energy += currentSegment.getLengthInHoursGLDCForInvestmentRole()
                                        * plant.getAvailableCapacity((getCurrentTick() + iterator), currentSegment,
                                                reps.segmentRepository.count());
                                sumProfit += profit1;
                            }
                        }
                    }

                }

                long yIterator = 1;
                double counter = 0;
                double cmRevenue = 0;
                // for (yIterator = 1; yIterator <= market.getLookback()
                // &&
                // yIterator > 0; yIterator++) {
                // counter += 1;

                double totalPeakCapacityAtFuturePoint = reps.powerPlantRepository
                        .calculatePeakCapacityOfNonIntermittentOperationalPowerPlantsInMarket(market, getCurrentTick());
                // double totalPeakDemandAtFuturePoint =
                // reps.segmentLoadRepository.peakLoadbyMarketandTime(market,
                // getCurrentTick());
                double totalPeakDemandAtFuturePoint = reps.segmentLoadRepository
                        .nonAdjustedPeakLoadbyMarketAnnual(market) * demandGrowthFactor;

                Zone zoneTemp = market.getZone();
                Regulator regulator = reps.regulatorRepository.findRegulatorForZone(zoneTemp);
                if ((plant.getOwner().isSimpleCapacityMarketEnabled() && regulator != null)) {
                    double phaseInPeriod = 0;

                    if (getCurrentTick() <= (long) regulator.getImplementationPhaseLength()
                            && regulator.getImplementationPhaseLength() > 0) {
                        phaseInPeriod = regulator.getReserveMargin()
                                - ((((regulator.getReserveMargin() - regulator.getInitialSupplyMargin())
                                        / regulator.getImplementationPhaseLength()) * getCurrentTick())
                                        + regulator.getInitialSupplyMargin());
                    }

                    if (totalPeakCapacityAtFuturePoint < totalPeakDemandAtFuturePoint
                            * (1 + (regulator.getReserveMargin() - phaseInPeriod
                                    - regulator.getReserveDemandLowerMargin()))) {
                        cmRevenue = plant.getTechnology().getCapacity()
                                * plant.getTechnology().getPeakSegmentDependentAvailability()
                                * regulator.getCapacityMarketPriceCap();
                    }

                    if ((totalPeakCapacityAtFuturePoint > (totalPeakDemandAtFuturePoint * (1
                            + (regulator.getReserveMargin() - phaseInPeriod - regulator.getReserveDemandLowerMargin())))
                            && totalPeakCapacityAtFuturePoint <= (totalPeakDemandAtFuturePoint
                                    * (1 + (regulator.getReserveMargin() - phaseInPeriod
                                            + regulator.getReserveDemandUpperMargin()))))) {

                        double reserveMargin = 1 + regulator.getReserveMargin() - phaseInPeriod;
                        double lowerMargin = reserveMargin - regulator.getReserveDemandLowerMargin();
                        double upperMargin = reserveMargin + regulator.getReserveDemandUpperMargin();
                        double marketCap = regulator.getCapacityMarketPriceCap();

                        cmRevenue = (-(marketCap / (upperMargin - lowerMargin))
                                * ((totalPeakCapacityAtFuturePoint / totalPeakDemandAtFuturePoint) - upperMargin))
                                * plant.getTechnology().getCapacity()
                                * plant.getTechnology().getPeakSegmentDependentAvailability();
                    }

                    if (totalPeakCapacityAtFuturePoint > (totalPeakDemandAtFuturePoint
                            * (1 + (regulator.getReserveMargin() - phaseInPeriod
                                    + regulator.getReserveDemandUpperMargin())))) {
                        cmRevenue = 0;
                    }
                } else {
                    cmRevenue = 0;
                }
                if (reserveVolume > 0.01) {
                    double counterReserve = 0;
                    Map<PowerPlant, Double> reverseMarginalCostMap = new HashMap<PowerPlant, Double>();
                    Map<PowerPlant, Double> reverseMeritOrder;
                    for (PowerPlant plant1 : reps.powerPlantRepository
                            .findExpectedOperationalPowerPlantsInMarket(market, getCurrentTick())) {
                        reverseMarginalCostMap.put(plant1,
                                calculateMarginalCostExclCO2MarketCost(plant1, (getCurrentTick())));

                    }

                    MapValueReverseComparator comp1 = new MapValueReverseComparator(reverseMarginalCostMap);
                    reverseMeritOrder = new TreeMap<PowerPlant, Double>(comp1);
                    reverseMeritOrder.putAll(reverseMarginalCostMap);
                    for (Entry<PowerPlant, Double> plantVol : reverseMeritOrder.entrySet()) {
                        if (plantVol.getKey().equals(plant)) {
                        }
                        if (counterReserve < reserveVolume) {
                            counterReserve = counterReserve + plantVol.getKey().getActualNominalCapacity();
                            if (plantVol.getKey().equals(plant)) {
                                cmRevenue = cmRevenue + plant.getActualFixedOperatingCost();
                            }

                        }
                    }

                }

                // for (CashFlow cf :
                // reps.cashFlowRepository.findAllCashFlowsForPowerPlantForTime(plant,
                // (getCurrentTick() - 1))) {
                // if (cf.getType() == CashFlow.STRRESPAYMENT &&
                // cf.getTo().equals(plant.getOwner())) {
                // cmRevenue = cmRevenue + cf.getMoney();
                // }
                // }

                // }
                totalProfit = ((sumProfit + (cmRevenue) - OM));

                // * plant
                // .getActualNominalCapacity())
                // ((OM -
                // (plant.getTechnology().getVariableOperatingCostinEURPerMWh()
                // * 8760 * plant.getTechnology().getCapacity())) +
                // (plant.getTechnology().getVariableOperatingCostinEURPerMWh()
                // * energy))
                // }
                if ((totalProfit) < 0) {
                    // + plant.getProfitability()
                    // REMAINING LOAN-----//
                    Loan loan = plant.getLoan();

                    if (loan != null) {

                        logger.info("Found a loan: {}", loan);
                        if (loan.getNumberOfPaymentsDone() < loan.getTotalNumberOfPayments()) {

                            double payment = loan.getAmountPerPayment()
                                    * (loan.getTotalNumberOfPayments() - loan.getNumberOfPaymentsDone());

                            reps.nonTransactionalCreateRepository.createCashFlow(plant.getOwner(), loan.getTo(),
                                    payment, CashFlow.LOAN, getCurrentTick(), loan.getRegardingPowerPlant());

                            loan.setNumberOfPaymentsDone(loan.getNumberOfPaymentsDone()
                                    + (loan.getTotalNumberOfPayments() - loan.getNumberOfPaymentsDone()));

                            // logger.warn("DISMANTLING: Paying {}
                            // (euro)
                            // for remaining loan {}", payment, loan);
                        }
                    }
                    Loan downpayment = plant.getDownpayment();
                    if (downpayment != null) {
                        logger.info("Found downpayment");
                        if (downpayment.getNumberOfPaymentsDone() < downpayment.getTotalNumberOfPayments()) {

                            double payment = downpayment.getAmountPerPayment()
                                    * (downpayment.getTotalNumberOfPayments() - downpayment.getNumberOfPaymentsDone());
                            reps.nonTransactionalCreateRepository.createCashFlow(plant.getOwner(), downpayment.getTo(),
                                    payment, CashFlow.DOWNPAYMENT, getCurrentTick(),
                                    downpayment.getRegardingPowerPlant());

                            downpayment.setNumberOfPaymentsDone(downpayment.getNumberOfPaymentsDone()
                                    + (downpayment.getTotalNumberOfPayments() - downpayment.getNumberOfPaymentsDone()));

                            // logger.warn("DISMANTLING: Paying {}
                            // (euro)
                            // for remaining downpayment {}", payment,
                            // downpayment);
                        }
                    }
                    if (getCurrentTick() < 8 && plant.getActualLifetime() < 15) {
                        logger.warn(" **********************  TOO early to dismantle: " + plant.getName() + " Age "
                                + plant.getActualLifetime());
                    } else {
                        logger.warn(" **********************  ENERGY PRODUCER DISMANTLED: " + plant.getName() + " Age "
                                + plant.getActualLifetime());
                        plant.dismantlePowerPlant(getCurrentTick());
                    }
                }
            }
        }
        // }

    }

    public double calculateMarginalCostExclCO2MarketCost(PowerPlant powerPlant, long clearingTick) {
        double mc = 0d;
        // fuel cost
        mc += calculateMarginalFuelCost(powerPlant, clearingTick);
        mc += calculateCO2TaxMarginalCost(powerPlant, clearingTick);
        logger.info("Margincal cost excluding CO2 auction/market cost for plant {} is {}", powerPlant.getName(), mc);
        return mc;
    }

    public double calculateMarginalFuelCost(PowerPlant powerPlant, long clearingTick) {
        double fc = 0d;
        // fuel cost for each fuel
        for (SubstanceShareInFuelMix mix : powerPlant.getFuelMix()) {

            double amount = mix.getShare();
            logger.info("Calculating need for fuel: {} units of {}", mix.getShare(), mix.getSubstance().getName());
            double fuelPrice = findLastKnownPriceForSubstance(mix.getSubstance(), clearingTick);
            fc += amount * fuelPrice;
            logger.info("Calculating marginal cost and found a fuel price which is {} per unit of fuel", fuelPrice);
        }

        return fc;
    }

    public double calculateCO2TaxMarginalCost(PowerPlant powerPlant, long tick) {
        double co2Intensity = powerPlant.calculateEmissionIntensity();
        Government government = reps.genericRepository.findFirst(Government.class);
        double co2Tax = government.getCO2Tax(tick);
        return co2Intensity * co2Tax;
    }

    public double findLastKnownPriceForSubstance(Substance substance, long clearingTick) {

        DecarbonizationMarket market = reps.marketRepository.findFirstMarketBySubstance(substance);
        if (market == null) {
            logger.warn("No market found for {} so no price can be found", substance.getName());
            return 0d;
        } else {
            return findLastKnownPriceOnMarket(market, clearingTick);
        }
    }

    public double findLastKnownPriceOnMarket(DecarbonizationMarket market, long clearingTick) {
        Double average = calculateAverageMarketPriceBasedOnClearingPoints(
                reps.clearingPointRepositoryOld.findClearingPointsForMarketAndTime(market, clearingTick, false));
        Substance substance = market.getSubstance();

        if (average != null) {
            logger.info("Average price found on market for this tick for {}", substance.getName());
            return average;
        }

        average = calculateAverageMarketPriceBasedOnClearingPoints(
                reps.clearingPointRepositoryOld.findClearingPointsForMarketAndTime(market, clearingTick - 1, false));
        if (average != null) {
            logger.info("Average price found on market for previous tick for {}", substance.getName());
            return average;
        }

        if (market.getReferencePrice() > 0) {
            logger.info("Found a reference price found for market for {}", substance.getName());
            return market.getReferencePrice();
        }

        for (CommoditySupplier supplier : reps.genericRepository.findAll(CommoditySupplier.class)) {
            if (supplier.getSubstance().equals(substance)) {

                return supplier.getPriceOfCommodity().getValue(clearingTick);
            }
        }

        logger.info("No price has been found for {}", substance.getName());
        return 0d;
    }

    private Double calculateAverageMarketPriceBasedOnClearingPoints(Iterable<ClearingPoint> clearingPoints) {
        double priceTimesVolume = 0d;
        double volume = 0d;

        for (ClearingPoint point : clearingPoints) {
            priceTimesVolume += point.getPrice() * point.getVolume();
            volume += point.getVolume();
        }
        if (volume > 0) {
            return priceTimesVolume / volume;
        }
        return null;
    }

}