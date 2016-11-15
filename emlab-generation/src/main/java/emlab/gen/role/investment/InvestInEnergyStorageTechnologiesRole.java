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

        // logger.warn("REV: " + storageRevenue);

        // CashFlow omCosts =
        // reps.cashFlowRepository.findAllCashFlowsForStorageOMCostsForTime(agent,
        // getCurrentTick());

        double OMAndInvCosts = reps.cashFlowRepository.findAllCurrentStorageCostsForTime(agent, getCurrentTick());

        double IncInvCosts = 0;

        if (getCurrentTick() > 0) {
            IncInvCosts = reps.cashFlowRepository.findAllPreviousStorageCostsForTime(agent, getCurrentTick() - 1);
        }

        double totalStorageCosts = OMAndInvCosts + IncInvCosts;

        // if ((revenue != null) || (revenueCM != null)) {
        // if ((revenue.getMoney() + revenueCM.getMoney()) > 1.2 *
        // omCosts.getMoney()) {

        // if (storageRevenue > 1.2 * omCosts.getMoney()) {

        if (storageRevenue > 1.2 * totalStorageCosts) {

            logger.warn("Revenue greater than 20%");

            double storageExpansionRate = ((storageRevenue - totalStorageCosts) / totalStorageCosts)
                    / storageTech.getStorageInvestmentCalibrator();

            double incrementalCapitalCost = storageTech.getCurrentMaxStorageCapacity() * storageExpansionRate
                    * storageTech.getFixedCapitalCostTimeSeriesForStoragePerMWh().getValue(getCurrentTick());

            CashFlow cf = reps.nonTransactionalCreateRepository.createCashFlow(agent, null, incrementalCapitalCost,
                    CashFlow.INC_STORAGE_CC, getCurrentTick(), null);

            storageTech.setCurrentMaxStorageCapacity(
                    storageTech.getCurrentMaxStorageCapacity() * (1 + storageExpansionRate));
            storageTech.setCurrentMaxStorageChargingRate(
                    storageTech.getCurrentMaxStorageChargingRate() * (1 + storageExpansionRate));
            storageTech.setCurrentMaxStorageDischargingRate(
                    storageTech.getCurrentMaxStorageDischargingRate() * (1 + storageExpansionRate));

            logger.warn("The amount paid for investment is {}", incrementalCapitalCost);
            logger.warn("The new current maximum storage capacity is {}", storageTech.getCurrentMaxStorageCapacity());

            // } else if ((revenue.getMoney() + revenueCM.getMoney()) -
            // omCosts.getMoney() > 0) {

            // } else if (storageRevenue - omCosts.getMoney() > 0) {

        } else if (storageRevenue - totalStorageCosts > 0) {

            logger.warn("Investment in storage!");

            double storageExpansionRate = ((storageRevenue - totalStorageCosts) / totalStorageCosts)
                    / storageTech.getStorageInvestmentCalibrator();

            double incrementalCapitalCost = storageTech.getCurrentMaxStorageCapacity() * storageExpansionRate
                    * storageTech.getFixedCapitalCostTimeSeriesForStoragePerMWh().getValue(getCurrentTick());

            CashFlow cf = reps.nonTransactionalCreateRepository.createCashFlow(agent, null, incrementalCapitalCost,
                    CashFlow.INC_STORAGE_CC, getCurrentTick(), null);

            storageTech.setCurrentMaxStorageCapacity(
                    storageTech.getCurrentMaxStorageCapacity() * (1 + storageExpansionRate));
            storageTech.setCurrentMaxStorageChargingRate(
                    storageTech.getCurrentMaxStorageChargingRate() * (1 + storageExpansionRate));
            storageTech.setCurrentMaxStorageDischargingRate(
                    storageTech.getCurrentMaxStorageDischargingRate() * (1 + storageExpansionRate));

            logger.warn("The amount paid for investment is {}", incrementalCapitalCost);
            logger.warn("The new current maximum storage capacity is {}", storageTech.getCurrentMaxStorageCapacity());

        } else if ((storageRevenue - totalStorageCosts < 0) && (storageRevenue > 0.7 * totalStorageCosts)) {

            logger.warn("Storage is making a loss so the capacity has to be decreased!");

            double storageExpansionRate = ((totalStorageCosts - storageRevenue) / totalStorageCosts)
                    / storageTech.getStorageInvestmentCalibrator();

            storageTech.setCurrentMaxStorageCapacity(
                    storageTech.getCurrentMaxStorageCapacity() * (1 - storageExpansionRate));
            storageTech.setCurrentMaxStorageChargingRate(
                    storageTech.getCurrentMaxStorageChargingRate() * (1 - storageExpansionRate));
            storageTech.setCurrentMaxStorageDischargingRate(
                    storageTech.getCurrentMaxStorageDischargingRate() * (1 - storageExpansionRate));

            logger.warn("The new current maximum storage capacity is {}", storageTech.getCurrentMaxStorageCapacity());

        } else {

            logger.warn("No investment in storage because it was not making enough profit!");

        }
        // } else
        // logger.warn("No investment in storage because it did not run!");
    }
}