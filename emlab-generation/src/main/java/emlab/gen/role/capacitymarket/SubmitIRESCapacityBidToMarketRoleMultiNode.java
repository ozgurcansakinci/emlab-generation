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
import emlab.gen.domain.market.Bid;
import emlab.gen.domain.market.capacity.CapacityDispatchPlan;
import emlab.gen.domain.market.capacity.CapacityMarket;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.technology.IntermittentResourceProfile;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.repository.Reps;
import emlab.gen.role.AbstractEnergyProducerRole;

//import org.springframework.data.neo4j.annotation.NodeEntity;

/**
 * @author Salman
 * 
 */

@RoleComponent
public class SubmitIRESCapacityBidToMarketRoleMultiNode extends AbstractEnergyProducerRole<EnergyProducer>
        implements Role<EnergyProducer> {

    Logger logger = Logger.getLogger(SubmitIRESCapacityBidToMarketRoleMultiNode.class);

    @Autowired
    Reps reps;

    @Override
    @Transactional
    public void act(EnergyProducer producer) {
        // logger.warn("***********Submitting Bid Role for Energy Producer
        // ********"
        // + producer.getName());

        // for (PowerPlant plant :
        // reps.powerPlantRepository.findOperationalPowerPlantsByOwner(producer,
        // getCurrentTick())) {

        for (PowerPlant plant : reps.powerPlantRepository.findOperationalIntermittentPowerPlantsByOwner(producer,
                getCurrentTick())) {

            CapacityMarket market = reps.capacityMarketRepository
                    .findCapacityMarketForZone(plant.getLocation().getZone());

            if (market != null) {
                ElectricitySpotMarket eMarket = reps.marketRepository
                        .findElectricitySpotMarketForZone(plant.getLocation().getZone());

                // logger.warn("Bid calculation for PowerPlant " +
                // plant.getName());
                // get market for the plant by zone

                // logger.warn("CapacityMarket is " + market.getName());

                // if (getCurrentTick() > 0) {
                //
                // CashFlow revenue =
                // reps.cashFlowRepository.findAnnualRevenueCashFlowsForPowerPlantForTime(plant,
                // getCurrentTick() - 1);
                //
                // logger.warn("Revenue for Plant: " +
                // plant.getName().toString() + " is: " + revenue);
                // }

                double bidPrice = 0;
                double capacity = 0;

                if (getCurrentTick() == 0) {
                    capacity = plant.getActualNominalCapacity()
                            * plant.getTechnology().getPeakSegmentDependentAvailability();

                } else {
                    double[] demand = reps.yearlySegmentClearingPointMarketInformationRepository
                            .findMarketInformationForMarketAndTime(getCurrentTick() - 1, eMarket).getMarketDemand();
                    double[] max = getMaxIndex(demand);
                    IntermittentResourceProfile availability = reps.intermittentResourceProfileRepository
                            .findIntermittentResourceProfileByTechnologyAndNode(plant.getTechnology(),
                                    plant.getLocation());
                    capacity = plant.getActualNominalCapacity() * availability.getHourlyArray(0)[(int) max[0]];
                    // double []

                }
                // logger.warn(plant.getTechnology().toString() + "RES bids " +
                // capacity + "for the capacity market");
                CapacityDispatchPlan plan = new CapacityDispatchPlan().persist();

                plan.specifyAndPersist(plant, producer, market, getCurrentTick(), bidPrice, capacity, Bid.SUBMITTED);

                // logger.warn("CDP for powerplant " + plan.getPlant().getName()
                // + "rev " + netRevenues);
                // logger.warn("CDP price is " + plan.getPrice());
                // logger.warn("CDP amount is " + plan.getAmount());
                // logger.warn("CDP amount is " + plan.getBiddingMarket());

            }
        }
    }

    public double[] getMaxIndex(double[] v) {
        double maxIndex = -1;
        double maxValue = -Double.MAX_VALUE;
        for (int i = 0; i < v.length; i++) {
            if (v[i] > maxValue) {
                maxIndex = i;
                maxValue = v[i];
            }
        }
        double[] output = new double[2];
        output[0] = maxIndex;
        output[1] = maxValue;
        return output;
    }
}