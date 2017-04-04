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
import emlab.gen.trend.TimeSeriesImpl;

@NodeEntity
public class ElectricitySpotMarket extends DecarbonizationMarket {

    @RelatedTo(type = "SEGMENT_LOAD", elementClass = SegmentLoad.class, direction = Direction.OUTGOING)
    private Set<SegmentLoad> loadDurationCurve;

    @RelatedTo(type = "YEARLYSEGMENT_MARKET", elementClass = YearlySegment.class, direction = Direction.OUTGOING)
    private YearlySegment yearlySegment;

    @RelatedTo(type = "YEARLYSEGMENTLOAD", elementClass = YearlySegmentLoad.class, direction = Direction.OUTGOING)
    private YearlySegmentLoad yearlySegmentLoad;

    @RelatedTo(type = "DEMANDGROWTH_TREND", elementClass = TimeSeriesImpl.class, direction = Direction.OUTGOING)
    private TimeSeriesImpl demandGrowthTrend;
    // <<<<<<< HEAD

    private boolean dailyDemandResponseImplemented;

    private boolean storageImplemented;

    public boolean isStorageImplemented() {
        return storageImplemented;
    }

    public void setStorageImplemented(boolean storageImplemented) {
        this.storageImplemented = storageImplemented;
    }

    public boolean isDailyDemandResponseImplemented() {
        return dailyDemandResponseImplemented;
    }

    public void setDailyDemandResponseImplemented(boolean dailyDemandResponseImplemented) {
        this.dailyDemandResponseImplemented = dailyDemandResponseImplemented;
    }

    public YearlySegment getYearlySegment() {
        return yearlySegment;
    }

    public void setYearlySegment(YearlySegment yearlySegment) {
        this.yearlySegment = yearlySegment;
    }

    // =======
    // >>>>>>> PCBhagwat/feature/mergingEconomicDismantlingAndCapacityMarkets2

    public YearlySegmentLoad getYearlySegmentLoad() {
        return yearlySegmentLoad;
    }

    public void setYearlySegmentLoad(YearlySegmentLoad yearlySegmentLoad) {
        this.yearlySegmentLoad = yearlySegmentLoad;
    }

    private double valueOfLostLoad;

    private double demandShiftCost;

    private double totalShiftedDemand;

    public double getTotalShiftedDemand() {
        return totalShiftedDemand;
    }

    public void setTotalShiftedDemand(double totalShiftedDemand) {
        this.totalShiftedDemand = totalShiftedDemand;
    }

    public double getDemandShiftCost() {
        return demandShiftCost;
    }

    public void setDemandShiftCost(double demandShiftCost) {
        this.demandShiftCost = demandShiftCost;
    }

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