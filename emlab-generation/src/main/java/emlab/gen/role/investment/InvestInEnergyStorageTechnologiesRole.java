/*******************************************************************************
 * Copyright 2014 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.contract.CashFlow;
import emlab.gen.domain.technology.EnergyStorageTechnology;
import emlab.gen.repository.Reps;
import emlab.gen.role.AbstractEnergyProducerRole;

/**
 * @author asmkhan
 */
@RoleComponent
public class InvestInEnergyStorageTechnologiesRole extends AbstractEnergyProducerRole implements Role<EnergyProducer> {
    @Transient
    @Autowired
    Reps reps;

    @Transient
    @Autowired
    Neo4jTemplate template;

    @Override
    @Transactional
    public void act(EnergyProducer agent) {

        EnergyStorageTechnology storageTech = reps.energyProducerRepository
                .findStorageTechnologyForEnergyProducer(agent);

        // CashFlow revenue =
        // reps.cashFlowRepository.findAllCashFlowsForStorageRevenueForTime(agent,
        // getCurrentTick());
        // CashFlow revenueCM =
        // reps.cashFlowRepository.findAllCashFlowsForStorageRevenueForCapacityMarketForTime(agent,
        // getCurrentTick());

        double storageRevenue = reps.cashFlowRepository.findAllStorageRevenuesForTime(agent, getCurrentTick());

        logger.warn("Revenues: " + storageRevenue);

        // CashFlow omCosts =
        // reps.cashFlowRepository.findAllCashFlowsForStorageOMCostsForTime(agent,
        // getCurrentTick());

        double OMAndInvCosts = reps.cashFlowRepository.findAllCurrentStorageCostsForTime(agent, getCurrentTick());

        double IncInvCosts = 0;

        if (getCurrentTick() > 0) {
            IncInvCosts = reps.cashFlowRepository.findAllPreviousStorageCostsForTime(agent, getCurrentTick() - 1);
        }

        double totalStorageCosts = OMAndInvCosts + IncInvCosts;

        logger.warn("Costs: " + totalStorageCosts);

        // if ((revenue != null) || (revenueCM != null)) {
        // if ((revenue.getMoney() + revenueCM.getMoney()) > 1.2 *
        // omCosts.getMoney()) {

        // if (storageRevenue > 1.2 * omCosts.getMoney()) {

        // if ((storageRevenue > (1.2 * totalStorageCosts)) || (storageRevenue -
        // totalStorageCosts > 0)) {

        double peakGeneration = reps.segmentLoadRepository
                .nonAdjustedPeakGenerationbyMarketAnnual(agent.getInvestorMarket());

        if (storageRevenue - totalStorageCosts > 0) {

            logger.warn("Revenue greater than costs - Investment in Storage");

            double storageExpansionRate = ((storageRevenue - totalStorageCosts) / totalStorageCosts)
                    / storageTech.getStorageInvestmentCalibrator();

            double expansionRate = 0;

            // if current storage capacity is more then 10% of the peak
            // generation in the zone, then the max increase in the capacity
            // should not be more then 5%

            if ((peakGeneration * 0.10) < storageTech.getCurrentMaxStorageCapacity()) {

                if (storageExpansionRate > 0.05) {
                    expansionRate = 0.05;
                } else {
                    expansionRate = storageExpansionRate;
                }
            }

            // if current storage capacity is less then 10% of the peak
            // generation in the zone, then the max increase in the capacity
            // should not be more then 15%

            else {

                if (storageExpansionRate > 0.15) {
                    expansionRate = 0.15;
                } else {
                    expansionRate = storageExpansionRate;
                }
            }

            double incrementalCapitalCost = storageTech.getCurrentMaxStorageCapacity() * expansionRate
                    * storageTech.getFixedCapitalCostTimeSeriesForStoragePerMWh().getValue(getCurrentTick());

            CashFlow cf = reps.nonTransactionalCreateRepository.createCashFlow(agent, null, incrementalCapitalCost,
                    CashFlow.INC_STORAGE_CC, getCurrentTick(), null);

            storageTech.setCurrentMaxStorageCapacity(storageTech.getCurrentMaxStorageCapacity() * (1 + expansionRate));
            storageTech.setCurrentMaxStorageChargingRate(
                    storageTech.getCurrentMaxStorageChargingRate() * (1 + expansionRate));
            storageTech.setCurrentMaxStorageDischargingRate(
                    storageTech.getCurrentMaxStorageDischargingRate() * (1 + expansionRate));
            storageTech.setInitialStateOfChargeInStorage(
                    storageTech.getInitialStateOfChargeInStorage() * (1 + expansionRate));
            storageTech
                    .setFinalStateOfChargeInStorage(storageTech.getFinalStateOfChargeInStorage() * (1 + expansionRate));

            logger.warn("The amount paid for investment is {}", incrementalCapitalCost);
            logger.warn("The new current maximum storage capacity is {}", storageTech.getCurrentMaxStorageCapacity());

            // } else if ((revenue.getMoney() + revenueCM.getMoney()) -
            // omCosts.getMoney() > 0) {

            // } else if (storageRevenue - omCosts.getMoney() > 0) {

            // } else if (storageRevenue - totalStorageCosts > 0) {
            //
            // logger.warn("Investment in storage!");
            //
            // double storageExpansionRate = ((storageRevenue -
            // totalStorageCosts) /
            // totalStorageCosts)
            // / storageTech.getStorageInvestmentCalibrator();
            //
            // double expansionRate = 0;
            //
            // if (storageExpansionRate > 0.25) {
            // expansionRate = 0.25;
            // } else {
            // expansionRate = storageExpansionRate;
            // }
            //
            // double incrementalCapitalCost =
            // storageTech.getCurrentMaxStorageCapacity() * expansionRate
            // *
            // storageTech.getFixedCapitalCostTimeSeriesForStoragePerMWh().getValue(getCurrentTick());
            //
            // CashFlow cf =
            // reps.nonTransactionalCreateRepository.createCashFlow(agent, null,
            // incrementalCapitalCost,
            // CashFlow.INC_STORAGE_CC, getCurrentTick(), null);
            //
            // storageTech.setCurrentMaxStorageCapacity(storageTech.getCurrentMaxStorageCapacity()
            // * (1 + expansionRate));
            // storageTech.setCurrentMaxStorageChargingRate(
            // storageTech.getCurrentMaxStorageChargingRate() * (1 +
            // expansionRate));
            // storageTech.setCurrentMaxStorageDischargingRate(
            // storageTech.getCurrentMaxStorageDischargingRate() * (1 +
            // expansionRate));
            //
            // logger.warn("The amount paid for investment is {}",
            // incrementalCapitalCost);
            // logger.warn("The new current maximum storage capacity is {}",
            // storageTech.getCurrentMaxStorageCapacity());
            //
            // }

        } else if ((storageRevenue - totalStorageCosts < 0) && (storageRevenue > (0.85 * totalStorageCosts))) {

            // } else if (storageRevenue - totalStorageCosts < 0) {

            if ((peakGeneration * 0.04) > storageTech.getCurrentMaxStorageCapacity()) {

                logger.warn("Storage cant be decreased beyond 4% of peak load");

            } else {

                logger.warn("Storage is making a loss so the capacity has to be decreased!");

                double storageReductionRate = ((totalStorageCosts - storageRevenue) / totalStorageCosts)
                        / storageTech.getStorageInvestmentCalibrator();

                double reductionRate = 0;

                if (storageReductionRate > 0.05) {
                    reductionRate = 0.05;
                } else {
                    reductionRate = storageReductionRate;
                }

                storageTech
                        .setCurrentMaxStorageCapacity(storageTech.getCurrentMaxStorageCapacity() * (1 - reductionRate));
                storageTech.setCurrentMaxStorageChargingRate(
                        storageTech.getCurrentMaxStorageChargingRate() * (1 - reductionRate));
                storageTech.setCurrentMaxStorageDischargingRate(
                        storageTech.getCurrentMaxStorageDischargingRate() * (1 - reductionRate));
                storageTech.setInitialStateOfChargeInStorage(
                        storageTech.getInitialStateOfChargeInStorage() * (1 - reductionRate));
                storageTech.setFinalStateOfChargeInStorage(
                        storageTech.getFinalStateOfChargeInStorage() * (1 - reductionRate));

                logger.warn("The new current maximum storage capacity is {}",
                        storageTech.getCurrentMaxStorageCapacity());
            }

        } else {

            logger.warn("No change in storage capacity!");

        }
        // } else
        // logger.warn("No investment in storage because it did not run!");
    }
}