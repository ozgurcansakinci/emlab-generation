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
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
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

        for (PowerPlant plant : reps.powerPlantRepository.findOperationalIntermittentPowerPlantsByOwner(producer,
                getCurrentTick())) {

            PowerGridNode node = plant.getLocation();

            PowerGeneratingTechnology technology = plant.getTechnology();

            CapacityMarket market = reps.capacityMarketRepository
                    .findCapacityMarketForZone(plant.getLocation().getZone());

            if (market != null) {
                ElectricitySpotMarket eMarket = reps.marketRepository
                        .findElectricitySpotMarketForZone(plant.getLocation().getZone());

                double bidPrice = 0;
                double capacity = 0;
                double expectedElectricityRevenues = 0;
                double netRevenues = 0;

                double fixedOnMCost = plant.getActualFixedOperatingCost();

                capacity = plant.getActualNominalCapacity()
                        * plant.getTechnology().getPeakSegmentDependentAvailability();

                // if (getCurrentTick() == 0) {
                // capacity = plant.getActualNominalCapacity()
                // *
                // plant.getTechnology().getPeakSegmentDependentAvailability();
                //
                // } else {
                // double[] demand =
                // reps.yearlySegmentClearingPointMarketInformationRepository
                // .findMarketInformationForMarketAndTime(getCurrentTick() - 1,
                // eMarket).getMarketDemand();
                // double[] max = getMaxIndex(demand);
                // IntermittentResourceProfile availability =
                // reps.intermittentResourceProfileRepository
                // .findIntermittentResourceProfileByTechnologyAndNode(plant.getTechnology(),
                // plant.getLocation());
                // capacity = plant.getActualNominalCapacity() *
                // availability.getHourlyArray(0)[(int) max[0]];
                // }

                for (SegmentLoad segmentLoad : eMarket.getLoadDurationCurve()) {

                    double price = 0;
                    double plantLoadFactor = 0;

                    if (getCurrentTick() > 0) {
                        price = reps.timeSeriesToLDCClearingPointRepository
                                .findOneTimeSeriesToLDCClearingPointForMarketSegmentAndTime(getCurrentTick() - 1,
                                        segmentLoad.getSegment(), eMarket)
                                .getPrice();

                        plantLoadFactor = reps.intermittentTechnologyNodeLoadFactorRepository
                                .findIntermittentTechnologyNodeLoadFactorForNodeAndTechnology(node, technology)
                                .getLoadFactorForSegment(segmentLoad.getSegment());

                    } else {
                        price = 0;
                    }

                    expectedElectricityRevenues = expectedElectricityRevenues
                            + (price * plant.getActualNominalCapacity() * plantLoadFactor
                                    * segmentLoad.getSegment().getLengthInHoursGLDCForInvestmentRole());
                }

                netRevenues = expectedElectricityRevenues - fixedOnMCost;

                if (getCurrentTick() > 0) {
                    if (netRevenues >= 0) {
                        bidPrice = 0d;
                    } else {
                        bidPrice = (netRevenues * (-1)) / capacity;
                    }
                } else {
                    bidPrice = 0;
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