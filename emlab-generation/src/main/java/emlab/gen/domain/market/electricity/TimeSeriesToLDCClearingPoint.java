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

import emlab.gen.domain.market.ClearingPoint;

/**
 * The SegmentClearingPoint is used to store informationr regarding the clearing
 * of national electricity markets. All volumes (including interconnector flows)
 * are given in electrical MWh.
 * 
 * @author JCRichstein
 * 
 */
@NodeEntity
public class TimeSeriesToLDCClearingPoint extends ClearingPoint {

    @RelatedTo(type = "PRICE_POINT", elementClass = Segment.class, direction = Direction.OUTGOING)
    private Segment segment;

    public Segment getSegment() {
        return segment;
    }

    public void setSegment(Segment segment) {
        this.segment = segment;
    }

}
