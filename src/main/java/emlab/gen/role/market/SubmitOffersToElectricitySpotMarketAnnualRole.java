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
package emlab.gen.role.market;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import emlab.gen.domain.agent.DecarbonizationModel;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.Government;
import emlab.gen.domain.market.Bid;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.PowerPlantDispatchPlan;
import emlab.gen.domain.market.electricity.PpdpAnnual;
import emlab.gen.domain.market.electricity.Segment;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.domain.technology.Substance;
import emlab.gen.domain.technology.SubstanceShareInFuelMix;
import emlab.gen.repository.Reps;
import emlab.gen.role.AbstractEnergyProducerRole;
import emlab.gen.trend.HourlyCSVTimeSeries;

/**
 * {@link EnergyProducer} submits offers to the {@link ElectricitySpotMarket}.
 * One {@link Bid} per {@link PowerPlant}.
 *
 * @author <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a> @author
 *         <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a>
 *
 */
@RoleComponent
public class SubmitOffersToElectricitySpotMarketAnnualRole extends AbstractEnergyProducerRole<EnergyProducer>
implements
Role<EnergyProducer> {

    @Autowired
    Reps reps;

    @Override
    public void act(EnergyProducer producer) {

        createOffersForElectricitySpotMarket(producer, getCurrentTick(), false, null);
    }

    @Transactional
    // Change from PowerPlantDispatchPlan to PpdpAnnual
    public List<PpdpAnnual> createOffersForElectricitySpotMarket(EnergyProducer producer, long tick,
            boolean forecast, Map<Substance, Double> forecastedFuelPrices) {
        List<PpdpAnnual> ppdpList = new ArrayList<PpdpAnnual>();

        if (forecastedFuelPrices == null && !forecast) {
            DecarbonizationModel model = reps.genericRepository.findFirst(DecarbonizationModel.class);
            forecastedFuelPrices = new HashMap<Substance, Double>();
            for (Substance substance : reps.substanceRepository.findAllSubstancesTradedOnCommodityMarkets()) {
                forecastedFuelPrices.put(substance, findLastKnownPriceForSubstance(substance, getCurrentTick()));
            }
        }


        long numberOfSegments = reps.segmentRepository.count();
        ElectricitySpotMarket market = null;
        if (producer != null)
            market = producer.getInvestorMarket();

        Iterable<PowerPlant> powerPlants;
        if (producer != null) {
            powerPlants = forecast ? reps.powerPlantRepository
                    .findExpectedOperationalPowerPlantsInMarketByOwner(market, tick, producer) : reps.powerPlantRepository
                    .findOperationalPowerPlantsByOwner(producer, tick);
        } else {
            powerPlants = forecast ? reps.powerPlantRepository.findExpectedOperationalPowerPlants(tick)
                    : reps.powerPlantRepository.findOperationalPowerPlants(tick);
        }

        boolean producerIsNull = (producer == null) ? true : false;
        // find all my operating power plants
        for (PowerPlant plant : powerPlants) {

            if (producerIsNull) {
                market = reps.marketRepository.findElectricitySpotMarketForZone(plant.getLocation().getZone());
                producer = plant.getOwner();
            }

            HourlyCSVTimeSeries hourlyCapacity = plant.getActualHourlyNominalCapacity();
            // hourlyCapacity.setHourlyArray(plant.getActualNominalCapacity(),tick);
            // hourlyCapacity.scalarMultiply(plant.getActualNominalCapacity());
            double mc;
            double price;
            if (!forecast) {
                mc = calculateMarginalCostExclCO2MarketCost(plant, tick);
                price = mc * producer.getPriceMarkUp();
            } else {
                mc = calculateExpectedMarginalCostExclCO2MarketCost(plant, forecastedFuelPrices, tick);
                price = mc * producer.getPriceMarkUp();
            }

            logger.info("Submitting offers for {} with technology {}", plant.getName(), plant.getTechnology().getName());

            for (SegmentLoad segmentload : market.getLoadDurationCurve()) {
                Segment segment = segmentload.getSegment();
                double capacity;
                if (tick == getCurrentTick()) {
                    capacity = plant.getAvailableCapacity(tick, segment, numberOfSegments);
                } else {
                    capacity = plant.getExpectedAvailableCapacity(tick, segment, numberOfSegments);
                }

                logger.info("I bid capacity: {} and price: {}", capacity, mc);

                // PowerPlantDispatchPlan plan =
                // reps.powerPlantDispatchPlanRepository
                // .findOnePowerPlantDispatchPlanForPowerPlantForSegmentForTime(plant,
                // segment, tick, forecast);
                // TODO: handle exception
                PpdpAnnual ppdpAnnual = null;
                // plan =
                // reps.powerPlantDispatchPlanRepository.findOnePowerPlantDispatchPlanForPowerPlantForSegmentForTime(plant,
                // segment,
                // getCurrentTick());
                // Iterable<PowerPlantDispatchPlan> plans =
                // reps.powerPlantDispatchPlanRepository
                // .findAllPowerPlantDispatchPlanForPowerPlantForSegmentForTime(plant,
                // segment, getCurrentTick());

                ppdpAnnual = new PpdpAnnual().persist();


                ppdpAnnual.specifyNotPersist(plant, producer, market, tick, price, hourlyCapacity, Bid.SUBMITTED);

                ppdpList.add(ppdpAnnual);

            }

        }

        return ppdpList;
    }

    @Transactional
    void updateMarginalCostInclCO2AfterFuelMixChange(double co2Price,
            Map<ElectricitySpotMarket, Double> nationalMinCo2Prices, long clearingTick, boolean forecast,
            Map<Substance, Double> fuelPriceMap) {

        int i = 0;
        int j = 0;

        Government government = reps.template.findAll(Government.class).iterator().next();
        for (PowerPlantDispatchPlan plan : reps.powerPlantDispatchPlanRepository
                .findAllPowerPlantDispatchPlansForTime(
                        clearingTick, forecast)) {
            j++;

            double effectiveCO2Price;

            double capacity = plan.getAmount();
            if (nationalMinCo2Prices.get(plan.getBiddingMarket()) > co2Price)
                effectiveCO2Price = nationalMinCo2Prices.get(plan.getBiddingMarket());
            else
                effectiveCO2Price = co2Price;

            if (plan.getPowerPlant().getFuelMix().size() > 1) {

                double oldmc = plan.getBidWithoutCO2();

                // Fuels
                Set<Substance> possibleFuels = plan.getPowerPlant().getTechnology().getFuels();
                Map<Substance, Double> substancePriceMap = new HashMap<Substance, Double>();

                for (Substance substance : possibleFuels) {
                    substancePriceMap.put(substance, fuelPriceMap.get(substance));
                }
                Set<SubstanceShareInFuelMix> fuelMix = calculateFuelMix(plan.getPowerPlant(), substancePriceMap,
                        government.getCO2Tax(clearingTick) + effectiveCO2Price);
                plan.getPowerPlant().setFuelMix(fuelMix);
                double mc = calculateMarginalCostExclCO2MarketCost(plan.getPowerPlant(), clearingTick);
                if (mc != oldmc) {
                    plan.setBidWithoutCO2(mc);
                    i++;
                }

            }

            plan.setPrice(plan.getBidWithoutCO2()
                    + (effectiveCO2Price * plan.getPowerPlant().calculateEmissionIntensity()));

            plan.setStatus(Bid.SUBMITTED);
            plan.setAmount(capacity);
            plan.setAcceptedAmount(0d);
            plan.setCapacityLongTermContract(0d);

        }

        //logger.warn("Marginal cost of {} of {} plans changed", i, j);

    }

}