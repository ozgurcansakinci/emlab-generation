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

import emlab.gen.domain.technology.Interconnector;

/**
 * The ClearingPoint is used to store information regarding the clearing of
 * national electricity spot markets. All interconnector flows are given in
 * electrical MWh.
 *
 * @author ASMKhan
 *
 */
@NodeEntity
public class YearlySegmentClearingPointInterconnectorInformation {

    @RelatedTo(type = "CLEARING_POINT_INTERCONNECTOR", elementClass = YearlySegment.class, direction = Direction.OUTGOING)
    private YearlySegment yearlySegment;

    @RelatedTo(type = "INTERCONNECTOR_INFORMATION_POINT", elementClass = Interconnector.class, direction = Direction.INCOMING)
    private Interconnector interconnector;

    private double[] yearlyInterconnectorFlow;

    private long time;

    private double congestionInstancesPerYear;

    private double priceConvergenceInstancesPerYear;

    public double getCongestionInstancesPerYear() {
        return congestionInstancesPerYear;
    }

    public void setCongestionInstancesPerYear(double congestionInstancesPerYear) {
        this.congestionInstancesPerYear = congestionInstancesPerYear;
    }

    public double getPriceConvergenceInstancesPerYear() {
        return priceConvergenceInstancesPerYear;
    }

    public void setPriceConvergenceInstancesPerYear(double priceConvergenceInstancesPerYear) {
        this.priceConvergenceInstancesPerYear = priceConvergenceInstancesPerYear;
    }

    public YearlySegment getYearlySegment() {
        return yearlySegment;
    }

    public void setYearlySegment(YearlySegment yearlySegment) {
        this.yearlySegment = yearlySegment;
    }

    public Interconnector getInterconnector() {
        return interconnector;
    }

    public void setInterconnector(Interconnector interconnector) {
        this.interconnector = interconnector;
    }

    /**
     * The interconnector flow is specified as a source of electricity from the
     * point of view of the market that the yearly clearing point belongs to. A
     * positive value means that the market is exporting electricity from ESM A
     * to B, a negative value mean that it is importing from ESM B to A.
     */

    public double[] getYearlyInterconnectorFlow() {
        return yearlyInterconnectorFlow;
    }

    public void setYearlyInterconnectorFlow(double[] yearlyInterconnectorFlow) {
        this.yearlyInterconnectorFlow = yearlyInterconnectorFlow;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}