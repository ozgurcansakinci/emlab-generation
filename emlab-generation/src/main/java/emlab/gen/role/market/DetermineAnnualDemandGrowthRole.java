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
import emlab.gen.repository.Reps;

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

        System.out.println("Growth Rate = " + market.getDemandGrowthTrend().getValue(getCurrentTick()));

        //////////// Hourly demand
        if (getCurrentTick() == 0) {
            YearlySegmentLoad segmentLoad = market.getYearlySegmentLoad();
            segmentLoad.setHourlyInElasticCurrentDemandForYearlySegment(
                    segmentLoad.getHourlyInElasticBaseDemandForYearlySegment());
            if (market.isDailyDemandResponseImplemented()) {
                segmentLoad.setDailyElasticCurrentDemandForYearlySegment(
                        segmentLoad.getDailyElasticBaseDemandForYearlySegment()); //////////// Daily
                                                                                  //////////// demand
            }
        } else {
            DoubleMatrix1D hourlyArray = new DenseDoubleMatrix1D(
                    market.getYearlySegmentLoad().getHourlyInElasticCurrentDemandForYearlySegment().getHourlyArray(0));

            // TODO: Check this with Ozgur (base demand changed to current
            // demand

            double growthRate = market.getDemandGrowthTrend().getValue(getCurrentTick());
            DoubleMatrix1D growthFactors = hourlyArray.copy();
            growthFactors.assign(growthRate);
            hourlyArray.assign(growthFactors, Functions.mult);
            market.getYearlySegmentLoad().getHourlyInElasticCurrentDemandForYearlySegment()
                    .setHourlyArray(hourlyArray.toArray(), 0);
            if (market.isDailyDemandResponseImplemented()) {
                DoubleMatrix1D dailyArray = new DenseDoubleMatrix1D(
                        market.getYearlySegmentLoad().getDailyElasticBaseDemandForYearlySegment().getDailyArray(0));
                dailyArray.assign(
                        market.getYearlySegmentLoad().getDailyElasticBaseDemandForYearlySegment().getDailyArray(0));
                DoubleMatrix1D dailyGrowth = dailyArray.copy();
                dailyGrowth.assign(growthRate);
                dailyArray.assign(dailyGrowth, Functions.mult);
                market.getYearlySegmentLoad().getDailyElasticCurrentDemandForYearlySegment()
                        .setDailyArray(dailyArray.toArray(), 0);
            }
            //////////// Hourly demand
        }

    }
}
