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
package emlab.gen.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;

import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.Segment;
import emlab.gen.domain.market.electricity.TimeSeriesToLDCClearingPoint;

public interface TimeSeriesToLDCClearingPointRepository extends GraphRepository<TimeSeriesToLDCClearingPoint> {

    @Query(value = "g.v(segment).in('PRICE_POINT').propertyFilter('time', FilterPipe.Filter.EQUAL, time).as('x').out('MARKET_POINT').idFilter(market, FilterPipe.Filter.EQUAL).back('x')", type = QueryType.Gremlin)
    TimeSeriesToLDCClearingPoint findOneTimeSeriesToLDCClearingPointForMarketSegmentAndTime(@Param("time") long time,
            @Param("segment") Segment segment, @Param("market") ElectricitySpotMarket electricitySpotMarket);
}
