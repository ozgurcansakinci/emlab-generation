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
public class YearlySegmentClearingPointMarketInformation extends SegmentClearingPoint {

    @RelatedTo(type = "YEARLY_SEGMENT_POINT", elementClass = YearlySegment.class, direction = Direction.OUTGOING)
    private YearlySegment yearlySegment;

    private HourlyCSVTimeSeries marketPrice;
    private HourlyCSVTimeSeries marketVolume;
    private HourlyCSVTimeSeries timeStep;

    public YearlySegment getYearlySegment() {
        return yearlySegment;
    }

    public void setYearlySegment(YearlySegment yearlySegment) {
        this.yearlySegment = yearlySegment;
    }

    public HourlyCSVTimeSeries getMarketPrice() {
        return marketPrice;
    }

    public void setMarketPrice(HourlyCSVTimeSeries marketPrice) {
        this.marketPrice = marketPrice;
    }

    public HourlyCSVTimeSeries getMarketVolume() {
        return marketVolume;
    }

    public void setMarketVolume(HourlyCSVTimeSeries marketVolume) {
        this.marketVolume = marketVolume;
    }

    public HourlyCSVTimeSeries getTimeStep() {
        return timeStep;
    }

    public void setTimeStep(HourlyCSVTimeSeries timeStep) {
        this.timeStep = timeStep;
    }

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
