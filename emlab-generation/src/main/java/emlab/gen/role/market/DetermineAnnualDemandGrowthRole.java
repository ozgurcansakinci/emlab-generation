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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.jet.math.Functions;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.YearlySegmentLoad;
import emlab.gen.domain.technology.EnergyStorageTechnology;
import emlab.gen.repository.Reps;
import emlab.gen.trend.HourlyCSVTimeSeries;

/**
 * Makes the annual hourly demand grow by the specified growth factor.
 * 
 * @author Ozgur & Salman
 * 
 */
@RoleComponent
public class DetermineAnnualDemandGrowthRole extends AbstractMarketRole<ElectricitySpotMarket>
        implements Role<ElectricitySpotMarket> {

    @Autowired
    private Reps reps;

    @Override
    public Reps getReps() {
        return reps;
    }

    @Override
    @Transactional
    public void act(ElectricitySpotMarket market) {

        // DecarbonizationModel model =
        // reps.genericRepository.findAll(DecarbonizationModel.class).iterator().next();

        // System.out.println("Growth Rate = " +
        // market.getDemandGrowthTrend().getValue(getCurrentTick()));

        //////////// Hourly demand
        if (getCurrentTick() == 0) {
            YearlySegmentLoad segmentLoad = market.getYearlySegmentLoad();
            segmentLoad.setHourlyInElasticCurrentDemandForYearlySegment(
                    segmentLoad.getHourlyInElasticBaseDemandForYearlySegment());

            if (market.isDailyDemandResponseImplemented()) {
                segmentLoad.setDailyElasticCurrentDemandForYearlySegment(
                        segmentLoad.getDailyElasticBaseDemandForYearlySegment()); // DemandResponse
            }
            if (market.isStorageImplemented()) {

                EnergyStorageTechnology storageTech = reps.energyStorageTechnologyRepository
                        .findEnergyStorageTechnologyByMarket(market);

                storageTech.setCurrentMaxStorageCapacity(storageTech.getBaseMaxStorageCapacity());
                storageTech.setCurrentMaxStorageDischargingRate(storageTech.getBaseMaxStorageDischargingRate());
                storageTech.setCurrentMaxStorageChargingRate(storageTech.getBaseMaxStorageChargingRate()); // Storage

            }

        } else {
            DoubleMatrix1D hourlyArray = new DenseDoubleMatrix1D(
                    market.getYearlySegmentLoad().getHourlyInElasticCurrentDemandForYearlySegment().getHourlyArray(0));

            double growthRate = market.getDemandGrowthTrend().getValue(getCurrentTick());

            DoubleMatrix1D growthFactors = new DenseDoubleMatrix1D(
                    market.getYearlySegmentLoad().getHourlyInElasticBaseDemandForYearlySegment().getLengthInHours());

            growthFactors.assign(growthRate);
            // logger.warn("growth factor:" + growthFactors.toArray()[0]);

            hourlyArray.assign(growthFactors, Functions.mult);

            HourlyCSVTimeSeries newDemand = new HourlyCSVTimeSeries();
            newDemand.setLengthInHours(
                    market.getYearlySegmentLoad().getHourlyInElasticBaseDemandForYearlySegment().getLengthInHours());

            newDemand.setHourlyArray(hourlyArray.toArray(), 0);

            market.getYearlySegmentLoad().setHourlyInElasticCurrentDemandForYearlySegment(newDemand);

            double[] a = market.getYearlySegmentLoad().getHourlyInElasticCurrentDemandForYearlySegment()
                    .getHourlyArray(0);

            // System.out.println("First element of Static demand is = " +
            // a[0]);

            if (market.isDailyDemandResponseImplemented()) {

                DoubleMatrix1D dailyArray = new DenseDoubleMatrix1D(
                        market.getYearlySegmentLoad().getDailyElasticCurrentDemandForYearlySegment().getHourlyArray(0));

                DoubleMatrix1D dailyGrowth = dailyArray.copy();

                dailyGrowth.assign(growthRate);

                dailyArray.assign(dailyGrowth, Functions.mult);

                HourlyCSVTimeSeries newDailyDemand = new HourlyCSVTimeSeries();

                newDailyDemand.setLengthInHours(
                        market.getYearlySegmentLoad().getDailyElasticBaseDemandForYearlySegment().getLengthInHours());

                newDailyDemand.setHourlyArray(dailyArray.toArray(), 0);

                market.getYearlySegmentLoad().setDailyElasticCurrentDemandForYearlySegment(newDailyDemand);

                double[] b = market.getYearlySegmentLoad().getDailyElasticCurrentDemandForYearlySegment()
                        .getHourlyArray(0);

                // System.out.println("First element of elastic demand is = " +
                // b[0]);
            }
            //////////// Hourly demand
        }

    }
}