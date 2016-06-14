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

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import emlab.gen.trend.DailyCSVTimeSeries;
import emlab.gen.trend.HourlyCSVTimeSeries;

@NodeEntity
public class YearlySegmentLoad {

    @RelatedTo(type = "YEARLYSEGMENTLOAD_SEGMENT", elementClass = YearlySegment.class, direction = Direction.OUTGOING)
    private YearlySegment yearlySegment;

    @RelatedTo(type = "ELECTRICITYSPOTMARKET_LOAD", elementClass = ElectricitySpotMarket.class, direction = Direction.INCOMING)
    private ElectricitySpotMarket electricitySpotMarket;

    // private double baseLoad;

    // private double currentLoad;

    @RelatedTo(type = "HOURLY_BASE_DEMAND_SEGMENT", elementClass = HourlyCSVTimeSeries.class, direction = Direction.OUTGOING)
    private HourlyCSVTimeSeries hourlyInElasticBaseDemandForYearlySegment;

    @RelatedTo(type = "DAILY_BASE_DEMAND_SEGMENT", elementClass = DailyCSVTimeSeries.class, direction = Direction.OUTGOING)
    private DailyCSVTimeSeries dailyElasticBaseDemandForYearlySegment;

    @RelatedTo(type = "HOURLYDEMAND_SEGMENT", elementClass = HourlyCSVTimeSeries.class, direction = Direction.OUTGOING)
    private HourlyCSVTimeSeries hourlyInElasticCurrentDemandForYearlySegment;

    @RelatedTo(type = "DAILYDEMAND_SEGMENT", elementClass = DailyCSVTimeSeries.class, direction = Direction.OUTGOING)
    private DailyCSVTimeSeries dailyElasticCurrentDemandForYearlySegment;

    public HourlyCSVTimeSeries getHourlyInElasticBaseDemandForYearlySegment() {
        return hourlyInElasticBaseDemandForYearlySegment;
    }

    public void setHourlyInElasticBaseDemandForYearlySegment(
            HourlyCSVTimeSeries hourlyInElasticBaseDemandForYearlySegment) {
        this.hourlyInElasticBaseDemandForYearlySegment = hourlyInElasticBaseDemandForYearlySegment;
    }

    public DailyCSVTimeSeries getDailyElasticBaseDemandForYearlySegment() {
        return dailyElasticBaseDemandForYearlySegment;
    }

    public void setDailyElasticBaseDemandForYearlySegment(DailyCSVTimeSeries dailyElasticBaseDemandForYearlySegment) {
        this.dailyElasticBaseDemandForYearlySegment = dailyElasticBaseDemandForYearlySegment;
    }

    public HourlyCSVTimeSeries getHourlyInElasticCurrentDemandForYearlySegment() {
        return hourlyInElasticCurrentDemandForYearlySegment;
    }

    public void setHourlyInElasticCurrentDemandForYearlySegment(
            HourlyCSVTimeSeries hourlyInElasticCurrentDemandForYearlySegment) {
        this.hourlyInElasticCurrentDemandForYearlySegment = hourlyInElasticCurrentDemandForYearlySegment;
    }

    public DailyCSVTimeSeries getDailyElasticCurrentDemandForYearlySegment() {
        return dailyElasticCurrentDemandForYearlySegment;
    }

    public void setDailyElasticCurrentDemandForYearlySegment(
            DailyCSVTimeSeries dailyElasticCurrentDemandForYearlySegment) {
        this.dailyElasticCurrentDemandForYearlySegment = dailyElasticCurrentDemandForYearlySegment;
    }

    public YearlySegment getYearlySegment() {
        return yearlySegment;
    }

    public void setYearlySegment(YearlySegment yearlySegment) {
        this.yearlySegment = yearlySegment;
    }

    public ElectricitySpotMarket getElectricitySpotMarket() {
        return electricitySpotMarket;
    }

    public void setElectricitySpotMarket(ElectricitySpotMarket electricitySpotMarket) {
        this.electricitySpotMarket = electricitySpotMarket;
    }

    // public double getBaseLoad() {
    // return baseLoad;
    // }
    //
    // public void setBaseLoad(double baseLoad) {
    // this.baseLoad = baseLoad;
    // }
    //
    //
    // public double getCurrentLoad() {
    // return currentLoad;
    // }
    //
    // public void setCurrentLoad(double currentLoad) {
    // this.currentLoad = currentLoad;
    // }

    @Override
    public String toString() {
        return "yearlySegment: " + yearlySegment;
    }

}
