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

import emlab.gen.trend.HourlyCSVTimeSeries;

/**
 * The SegmentClearingPoint is used to store informationr regarding the clearing
 * of national electricity markets. All volumes (including interconnector flows)
 * are given in electrical MWh.
 *
 * @author JCRichstein
 *
 */
@NodeEntity
public class YearlySegmentClearingPointMarketInformation {// extends
                                                          // SegmentClearingPoint
                                                          // {

    @RelatedTo(type = "YEARLY_SEGMENT_POINT", elementClass = YearlySegment.class, direction = Direction.OUTGOING)
    private YearlySegment yearlySegment;

    @RelatedTo(type = "MARKET_INFORMATION_POINT", elementClass = ElectricitySpotMarket.class, direction = Direction.INCOMING)
    private ElectricitySpotMarket electricitySpotMarket;

    // private HourlyCSVTimeSeries marketPrice;
    // private HourlyCSVTimeSeries marketSupply;
    // private HourlyCSVTimeSeries marketDemand;
    // private HourlyCSVTimeSeries valueOfLostLoad;

    private double[] marketPrice;
    private double[] marketSupply;
    private double[] marketDemand;
    private double[] valueOfLostLoad;

    private double CO2Price;

    private long time;

    /**
     * @param marketPrice
     * @param marketSupply
     * @param marketDemand
     * @param valueOfLostLoad
     */
    // public YearlySegmentClearingPointMarketInformation() {
    // // super();
    // this.marketPrice = new HourlyCSVTimeSeries();
    // this.marketSupply = new HourlyCSVTimeSeries();
    // this.marketDemand = new HourlyCSVTimeSeries();
    // this.valueOfLostLoad = new HourlyCSVTimeSeries();
    // }

    public double getCO2Price() {
        return CO2Price;
    }

    public double[] getMarketPrice() {
        return marketPrice;
    }

    public void setMarketPrice(double[] marketPrice) {
        this.marketPrice = marketPrice;
    }

    public double[] getMarketSupply() {
        return marketSupply;
    }

    public void setMarketSupply(double[] marketSupply) {
        this.marketSupply = marketSupply;
    }

    public double[] getMarketDemand() {
        return marketDemand;
    }

    public void setMarketDemand(double[] marketDemand) {
        this.marketDemand = marketDemand;
    }

    public double[] getValueOfLostLoad() {
        return valueOfLostLoad;
    }

    public void setValueOfLostLoad(double[] valueOfLostLoad) {
        this.valueOfLostLoad = valueOfLostLoad;
    }

    public void setCO2Price(double cO2Price) {
        CO2Price = cO2Price;
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

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    // public HourlyCSVTimeSeries getMarketPrice() {
    // return marketPrice;
    // }
    //
    // public void updateMarketPrice(double[] marketPrice) {
    // this.marketPrice.setHourlyArray(marketPrice, 0);
    // }
    //
    // public HourlyCSVTimeSeries getMarketSupply() {
    // return marketSupply;
    // }
    //
    // public void updateMarketSupply(double[] marketSupply) {
    // this.marketSupply.setHourlyArray(marketSupply, 0);
    // }
    //
    // public void updateMarketDemand(double[] marketDemand) {
    // this.marketDemand.setHourlyArray(marketDemand, 0);
    // }
    //
    // public HourlyCSVTimeSeries getValueOfLostLoad() {
    // return valueOfLostLoad;
    // }
    //
    // public void updateValueOfLostLoad(double[] valueOfLostLoad) {
    // this.valueOfLostLoad.setHourlyArray(valueOfLostLoad, 0);
    // }
    //
    // public void setMarketPrice(HourlyCSVTimeSeries marketPrice) {
    // this.marketPrice = marketPrice;
    // }
    //
    // public void setMarketSupply(HourlyCSVTimeSeries marketSupply) {
    // this.marketSupply = marketSupply;
    // }
    //
    // public HourlyCSVTimeSeries getMarketDemand() {
    // return marketDemand;
    // }
    //
    // public void setMarketDemand(HourlyCSVTimeSeries marketDemand) {
    // this.marketDemand = marketDemand;
    // }
    //
    // public void setValueOfLostLoad(HourlyCSVTimeSeries valueOfLostLoad) {
    // this.valueOfLostLoad = valueOfLostLoad;
    // }

    /**
     * The interconnector flow is specified as a source of electricity from the
     * point of view of the market that the segment clearing point belongs to. A
     * positive value means that the market is importing electricity, a negative
     * value mean that it is exporting it.
     */
    HourlyCSVTimeSeries yearlyInterconnectorFlow;

    public HourlyCSVTimeSeries getYearlyInterconnectorFlow() {
        return yearlyInterconnectorFlow;
    }

    public void setYearlyInterconnectorFlow(HourlyCSVTimeSeries yearlyInterconnectorFlow) {
        this.yearlyInterconnectorFlow = yearlyInterconnectorFlow;
    }

}
