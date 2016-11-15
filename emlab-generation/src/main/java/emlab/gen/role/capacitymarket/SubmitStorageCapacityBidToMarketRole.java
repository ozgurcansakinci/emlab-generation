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

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.contract.CashFlow;
import emlab.gen.domain.market.Bid;
import emlab.gen.domain.market.capacity.CapacityDispatchPlan;
import emlab.gen.domain.market.capacity.CapacityMarket;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.technology.EnergyStorageTechnology;
import emlab.gen.repository.Reps;
import emlab.gen.role.AbstractEnergyProducerRole;

//import org.springframework.data.neo4j.annotation.NodeEntity;

/**
 * @author
 * 
 */

@RoleComponent
public class SubmitStorageCapacityBidToMarketRole extends AbstractEnergyProducerRole<EnergyProducer>
        implements Role<EnergyProducer> {

    Logger logger = Logger.getLogger(SubmitStorageCapacityBidToMarketRole.class);

    @Autowired
    Reps reps;

    @Override
    @Transactional
    public void act(EnergyProducer producer) {

        // logger.warn("***********Submitting Bid Role for Energy
        // Producer********" + producer.getName());

        EnergyStorageTechnology storageTech = reps.energyProducerRepository
                .findStorageTechnologyForEnergyProducer(producer);

        // logger.warn("***********Submitting Bid for Storage Unit********" +
        // storageTech.getName());

        CapacityMarket market = reps.capacityMarketRepository
                .findCapacityMarketForZone(producer.getInvestorMarket().getZone());

        // logger.warn("***********Submitting Bid in********" +
        // market.getName());
        // logger.warn("***********Submitting Bid in********" +
        // producer.getInvestorMarket().getZone());

        if (market != null) {
            ElectricitySpotMarket eMarket = reps.marketRepository
                    .findElectricitySpotMarketForZone(producer.getInvestorMarket().getZone());

            double bidPrice = 0d;
            double expectedStorageRevenues = 0;
            double netRevenuesForStorage = 0;

            // Getting storage revenue and payments from the previous year

            if (getCurrentTick() == 0) {
                netRevenuesForStorage = 0d;

            } else {

                CashFlow revenue = reps.cashFlowRepository.findAllCashFlowsForStorageRevenueForTime(producer,
                        getCurrentTick() - 1);

                CashFlow omCosts = reps.cashFlowRepository.findAllCashFlowsForStorageOMCostsForTime(producer,
                        getCurrentTick() - 1);

                // logger.warn("Storage revenues from year " + (getCurrentTick()
                // - 1) + " Are: " + revenue.getMoney());
                // logger.warn("Storage O&M Costs from year " +
                // (getCurrentTick() - 1) + " Are: " + omCosts.getMoney());

                if (revenue != null)
                    netRevenuesForStorage = revenue.getMoney() - omCosts.getMoney();
                else
                    netRevenuesForStorage = -omCosts.getMoney();
            }

            if (getCurrentTick() == 0) {

                bidPrice = 0;

            } else {

                if (netRevenuesForStorage >= 0) {

                    bidPrice = 0d;

                } else {

                    bidPrice = (netRevenuesForStorage * (-1)) / storageTech.getCurrentMaxStorageCapacity();
                }
            }

            // Storage can only bid 8% of the available discharging capacity

            double capacity = 0;
            double availablePeakHourCapacity = storageTech.getCurrentMaxStorageCapacity()
                    / storageTech.getPercentageCMBidding();

            if (storageTech.getCurrentMaxStorageDischargingRate() > availablePeakHourCapacity) {
                capacity = availablePeakHourCapacity;
            } else {
                capacity = storageTech.getCurrentMaxStorageDischargingRate();
            }

            CapacityDispatchPlan plan = new CapacityDispatchPlan().persist();

            plan.specifyAndPersistForStorage(storageTech, producer, market, getCurrentTick(), bidPrice, capacity,
                    Bid.SUBMITTED);

            // logger.warn("CDP for storage " + plan.getStorage().getName() +
            // "rev " + netRevenuesForStorage);
            // logger.warn("CDP price is " + plan.getPrice());
            // logger.warn("CDP amount is " + plan.getAmount());
            // logger.warn("CDP amount is " + plan.getBiddingMarket());

        }
    }

}