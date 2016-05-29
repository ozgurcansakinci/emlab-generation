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
package emlab.gen.domain.market.electricity;

import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import agentspring.simulation.SimulationParameter;
import emlab.gen.domain.market.DecarbonizationMarket;
import emlab.gen.trend.DailyCSVTimeSeries;
import emlab.gen.trend.HourlyCSVTimeSeries;
import emlab.gen.trend.TimeSeriesImpl;
import ilog.concert.IloLinearNumExpr;

@NodeEntity
public class ElectricitySpotMarket extends DecarbonizationMarket {

    @RelatedTo(type = "SEGMENT_LOAD", elementClass = SegmentLoad.class, direction = Direction.OUTGOING)
    private Set<SegmentLoad> loadDurationCurve;

    @RelatedTo(type = "DEMANDGROWTH_TREND", elementClass = TimeSeriesImpl.class, direction = Direction.OUTGOING)
    private TimeSeriesImpl demandGrowthTrend;
    // <<<<<<< HEAD

    @RelatedTo(type = "HOURLYDEMAND", elementClass = HourlyCSVTimeSeries.class, direction = Direction.OUTGOING)
    private HourlyCSVTimeSeries hourlyInElasticDemandForESMarket;

    @RelatedTo(type = "DAILYDEMAND", elementClass = DailyCSVTimeSeries.class, direction = Direction.OUTGOING)
    private DailyCSVTimeSeries dailyElasticDemandForESMarket;

    private IloLinearNumExpr[] generationEquationForElectrictySpotMarket;

    public IloLinearNumExpr[] getGenerationEquationForElectrictySpotMarket() {
        return generationEquationForElectrictySpotMarket;
    }

    public HourlyCSVTimeSeries getHourlyInElasticDemandForESMarket() {
        return hourlyInElasticDemandForESMarket;
    }

    public void setHourlyInElasticDemandForESMarket(HourlyCSVTimeSeries hourlyInElasticDemandForESMarket) {
        this.hourlyInElasticDemandForESMarket = hourlyInElasticDemandForESMarket;
    }

    public DailyCSVTimeSeries getDailyElasticDemandForESMarket() {
        return dailyElasticDemandForESMarket;
    }

    public void setDailyElasticDemandForESMarket(DailyCSVTimeSeries dailyElasticDemandForESMarket) {
        this.dailyElasticDemandForESMarket = dailyElasticDemandForESMarket;
    }
    // =======
    // >>>>>>> PCBhagwat/feature/mergingEconomicDismantlingAndCapacityMarkets2

    private double valueOfLostLoad;

    @SimulationParameter(label = "Lookback for dismantling", from = 0, to = 10)
    private long lookback;

    @SimulationParameter(label = "Look back for demand forecasting", from = 0, to = 10)
    private long backlookingForDemandForecastinginDismantling;

    public long getLookback() {
        return lookback;
    }

    public void setLookback(long lookback) {
        this.lookback = lookback;
    }

    public long getBacklookingForDemandForecastinginDismantling() {
        return backlookingForDemandForecastinginDismantling;
    }

    public void setBacklookingForDemandForecastinginDismantling(long backlookingForDemandForecastinginDismantling) {
        this.backlookingForDemandForecastinginDismantling = backlookingForDemandForecastinginDismantling;
    }

    public Set<SegmentLoad> getLoadDurationCurve() {
        return loadDurationCurve;
    }

    public void setLoadDurationCurve(Set<SegmentLoad> loadDurationCurve) {
        this.loadDurationCurve = loadDurationCurve;
    }

    public double getValueOfLostLoad() {
        return valueOfLostLoad;
    }

    public void setValueOfLostLoad(double valueOfLostLoad) {
        this.valueOfLostLoad = valueOfLostLoad;
    }

    public TimeSeriesImpl getDemandGrowthTrend() {
        return demandGrowthTrend;
    }

    public void setDemandGrowthTrend(TimeSeriesImpl demandGrowthTrend) {
        this.demandGrowthTrend = demandGrowthTrend;
    }

}
