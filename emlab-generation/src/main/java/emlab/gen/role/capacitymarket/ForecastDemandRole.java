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

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.AbstractRole;
import agentspring.role.Role;
import agentspring.role.RoleComponent;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.repository.Reps;

/**
 * @author Kaveri
 * 
 */
@RoleComponent
public class ForecastDemandRole extends AbstractRole<Regulator> implements Role<Regulator> {

    @Autowired
    Reps reps;

    @Override
    @Transactional
    public void act(Regulator regulator) {
        long capabilityYear = 0;
        capabilityYear = getCurrentTick() + regulator.getTargetPeriod();
        double phaseInPeriod = 0;

        if (getCurrentTick() <= (long) regulator.getImplementationPhaseLength()
                && regulator.getImplementationPhaseLength() > 0) {

            phaseInPeriod = regulator.getReserveMargin()
                    - ((((regulator.getReserveMargin() - regulator.getInitialSupplyMargin())
                            / regulator.getImplementationPhaseLength()) * getCurrentTick())
                            + regulator.getInitialSupplyMargin());
        }

        Zone zone = regulator.getZone();
        ElectricitySpotMarket market = reps.marketRepository.findElectricitySpotMarketForZone(zone);

        // double trend =
        // market.getDemandGrowthTrend().getValue(getCurrentTick());
        // double peakLoadforMarket = trend * peakLoadforMarketNOtrend;
        // double reserveMargin = regulator.getReserveMargin();
        // double demandTarget = peakLoadforMarket * (1 + reserveMargin);

        // regulator.setDemandTarget(demandTarget);

        /*
         * // Computing Demand (the current year's demand is not considered for
         * // regression, as it is forecasted. double expectedDemandFactor = 0d;
         */

        double expectedDemandFactor = 0d;
        // if (capabilityYear > getCurrentTick()) {
        // if (getCurrentTick() < 2) {
        //
        // expectedDemandFactor =
        // market.getDemandGrowthTrend().getValue(getCurrentTick());
        // } else {
        //
        // SimpleRegression sr = new SimpleRegression();
        // for (long time = getCurrentTick() - 1; time > getCurrentTick() - 1
        // - regulator.getNumberOfYearsLookingBackToForecastDemand()
        // && time >= 0; time = time - 1) {
        // sr.addData(time, market.getDemandGrowthTrend().getValue(time));
        // }
        // expectedDemandFactor = sr.predict(capabilityYear);
        // }
        //
        // } else {
        // expectedDemandFactor =
        // market.getDemandGrowthTrend().getValue(getCurrentTick());
        // }

        // GeometricTrendRegression gtr = new GeometricTrendRegression();
        // for (long time = getCurrentTick(); time > getCurrentTick()
        // - agent.getNumberOfYearsBacklookingForForecasting() && time >=
        // 0;
        // time = time - 1) {
        // gtr.addData(time, elm.getDemandGrowthTrend().getValue(time));
        // }
        if (capabilityYear > getCurrentTick()) {
            double avgGrowthFactor = 0;
            double iteration = 0;
            for (long time = getCurrentTick(); time > getCurrentTick()
                    - regulator.getNumberOfYearsLookingBackToForecastDemand(); time = time - 1) {
                iteration++;
                if (time >= 0)
                    avgGrowthFactor += market.getDemandGrowthTrend().getValue(time);
                else // TODO:not sure if we should take 0th tick or current tick
                    avgGrowthFactor += market.getDemandGrowthTrend().getValue(0);
            }
            expectedDemandFactor = Math.pow(avgGrowthFactor / iteration, (double) capabilityYear);
        } else
            expectedDemandFactor = market.getDemandGrowthTrend().getValue(getCurrentTick());
        // expectedDemandFactor = 1;
        double peakLoadforMarketNOtrend;
        double peakExpectedDemand;
        if (getCurrentTick() == 0) {
            peakLoadforMarketNOtrend = reps.capacityMarketRepository.findCapacityMarketForZone(regulator.getZone())
                    .getBaseCapacityMarketDemand();
            peakExpectedDemand = peakLoadforMarketNOtrend;

        } else {
            double[] demands = reps.yearlySegmentClearingPointMarketInformationRepository
                    .findMarketInformationForMarketAndTime(getCurrentTick() - 1, market).getMarketDemand();
            Arrays.sort(demands);
            peakLoadforMarketNOtrend = demands[demands.length - 1];
            peakExpectedDemand = peakLoadforMarketNOtrend * expectedDemandFactor;
        }
        logger.warn("ExpectedDemandFactor for this tick: " + expectedDemandFactor);

        // logger.warn("demand factor " +
        // market.getDemandGrowthTrend().getValue(getCurrentTick()));

        // Calculate peak demand across all markets

        // double peakLoadforMarketNOtrend =
        // reps.segmentLoadRepository.peakLoadbyZoneMarketandTime(zone, market);

        // peakLoadforMarketNOtrend =
        // reps.segmentLoadRepository.nonAdjustedPeakLoadbyMarketAnnual(market);

        logger.warn("peakLoadforMarketNOtrend " + peakLoadforMarketNOtrend);

        // this is 69918 for esm A

        logger.warn("peakExpectedDemand " + peakExpectedDemand);

        // this is 83902 (69918 * 1.2), the expected peak demand for the current
        // year

        // Compute demand target by multiplying reserve margin double double
        double demandTarget = peakExpectedDemand * (1 + regulator.getReserveMargin() - phaseInPeriod);

        // demand target would be 83902 * ((1+0.156)-0) = 96990

        regulator.setDemandTarget(demandTarget);

    }

}