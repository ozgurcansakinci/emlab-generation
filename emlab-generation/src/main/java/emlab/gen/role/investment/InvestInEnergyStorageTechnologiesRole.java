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
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.repository.Reps;
import emlab.gen.role.AbstractEnergyProducerRole;

/**
 * {@link EnergyProducer}s decide to invest in new {@link PowerPlant}
 *
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a> @author
 *         <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a> ======= import
 *         emlab.gen.util.MapValueReverseComparator;
 * 
 *         /** {@link EnergyProducer}s decide to invest in new
 *         {@link PowerPlant}
 * 
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a> @author
 *         <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a> >>>>>>>
 *         PCBhagwat/feature/mergingEconomicDismantlingAndCapacityMarkets2
 * @author JCRichstein
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
        CashFlow revenue = reps.cashFlowRepository.findAllCashFlowsForStorageRevenueForTime(agent, getCurrentTick());
        CashFlow omCosts = reps.cashFlowRepository.findAllCashFlowsForStorageOMCostsForTime(agent, getCurrentTick());
        if (revenue != null) {
            if (revenue.getMoney() > 1.2 * omCosts.getMoney()) {
                logger.warn("Revenue greater than 20%");
                double incrementalCapitalCost = storageTech.getCurrentMaxStorageCapacity() * 0.02
                        * storageTech.getFixedCapitalCostTimeSeriesForStoragePerMWh().getValue(getCurrentTick());
                CashFlow cf = reps.nonTransactionalCreateRepository.createCashFlow(agent, null, incrementalCapitalCost,
                        CashFlow.INC_STORAGE_CC, getCurrentTick(), null);
                storageTech.setCurrentMaxStorageCapacity(storageTech.getCurrentMaxStorageCapacity() * 1.02);
                storageTech.setCurrentMaxStorageChargingRate(storageTech.getCurrentMaxStorageChargingRate() * 1.02);
                storageTech
                        .setCurrentMaxStorageDischargingRate(storageTech.getCurrentMaxStorageDischargingRate() * 1.02);
                logger.warn("The amount paid for investment is {}", incrementalCapitalCost);
                logger.warn("The new current maximum storage capacity is {}",
                        storageTech.getCurrentMaxStorageCapacity());

            } else if (revenue.getMoney() - omCosts.getMoney() > 0) {
                logger.warn("Investment in storage!");
                double incrementalCapitalCost = storageTech.getCurrentMaxStorageCapacity() * 0.02
                        * storageTech.getFixedCapitalCostTimeSeriesForStoragePerMWh().getValue(getCurrentTick());
                CashFlow cf = reps.nonTransactionalCreateRepository.createCashFlow(agent, null, incrementalCapitalCost,
                        CashFlow.INC_STORAGE_CC, getCurrentTick(), null);
                storageTech.setCurrentMaxStorageCapacity(storageTech.getCurrentMaxStorageCapacity() * 1.02);
                storageTech.setCurrentMaxStorageChargingRate(storageTech.getCurrentMaxStorageChargingRate() * 1.02);
                storageTech
                        .setCurrentMaxStorageDischargingRate(storageTech.getCurrentMaxStorageDischargingRate() * 1.02);
                logger.warn("The amount paid for investment is {}", incrementalCapitalCost);
                logger.warn("The new current maximum storage capacity is {}",
                        storageTech.getCurrentMaxStorageCapacity());
            } else {
                logger.warn("No investment in storage because it was not making enough profit!");
            }
        } else
            logger.warn("No investment in storage because it did not run!");
    }
}
