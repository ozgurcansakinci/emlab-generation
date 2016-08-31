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
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.AbstractRole;
import agentspring.role.Role;
import agentspring.role.RoleComponent;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.market.capacity.CapacityMarket;
import emlab.gen.repository.Reps;

/**
 * @author Salman
 * 
 */

@RoleComponent
public class CapacityMarketMainRoleMultiNode extends AbstractRole<CapacityMarket> implements Role<CapacityMarket> {

    @Autowired
    Reps reps;

    @Autowired
    ForecastDemandRole forecastDemandRole;

    @Autowired
    SubmitCapacityBidToMarketRoleMultiNode submitCapacityBidToMarketRoleMultiNode;

    @Autowired
    SubmitStorageCapacityBidToMarketRole submitStorageCapacityBidToMarketRole;

    @Autowired
    ClearCapacityMarketRoleMultiNode clearCapacityMarketNewRoleMultiNode;

    @Autowired
    PaymentFromConsumerToProducerForCapacityRole paymentFromConsumerToProducerforCapacityRole;

    @Override
    @Transactional
    public void act(CapacityMarket market) {

        Regulator regulator = market.getRegulator();

        // Forecast Demand

        forecastDemandRole.act(regulator);

        logger.warn("Forecast demand role run");

        // Energy producers submit Bids to Capacity market

        if (market.isRenewableTargetInvestorCanInvest()) {
            for (EnergyProducer producer : reps.energyProducerRepository
                    .findAllEnergyProducersIncludingRenewableTargetInvestorsAtRandomForZone(market.getZone())) {
                // logger.warn("The bidder is" + producer.getName());
                submitCapacityBidToMarketRoleMultiNode.act(producer);
            }
        } else {
            for (EnergyProducer producer : reps.energyProducerRepository
                    .findAllEnergyProducersExceptForRenewableTargetInvestorsAtRandomForZone(market.getZone())) {

                submitCapacityBidToMarketRoleMultiNode.act(producer);
            }
        }

        if (market.isStorageBiddingAllowed()) {
            for (EnergyProducer producer : reps.energyProducerRepository
                    .findStorageOwningEnergyProducerForZone(market.getZone())) {

                submitStorageCapacityBidToMarketRole.act(producer);
            }
        }

        logger.warn("******************capacity bids submitted****************************");

        // Clear capacity market

        clearCapacityMarketNewRoleMultiNode.act(regulator);

        logger.warn("************************Capacity Market cleared******************************");

        // ensure cash flows
        paymentFromConsumerToProducerforCapacityRole.act(market);

        logger.warn("capacity payments made");
        logger.warn("Capacity Market Main Role Completed  once");

    }

}
