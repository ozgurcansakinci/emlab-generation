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
import org.springframework.stereotype.Repository;

import emlab.gen.domain.market.electricity.YearlySegmentClearingPointInterconnectorInformation;
import emlab.gen.domain.technology.Interconnector;

/**
 * Repository for yearly segment clearing points
 * 
 * @author asmkhan
 * 
 */
@Repository
public interface YearlySegmentClearingPointInterconnectorInformationRepository
        extends GraphRepository<YearlySegmentClearingPointInterconnectorInformation> {

    @Query(value = "g.v(interconnector).out('INTERCONNECTOR_INFORMATION_POINT').propertyFilter('time', FilterPipe.Filter.EQUAL, time)", type = QueryType.Gremlin)
    YearlySegmentClearingPointInterconnectorInformation findInterconnectorInformationForTime(@Param("time") long time,
            @Param("interconnector") Interconnector interconnector);

    @Query(value = "g.v(interconnector).out('INTERCONNECTOR_INFORMATION_POINT').propertyFilter('time', FilterPipe.Filter.GREATER_THAN_EQUAL, timeFrom).propertyFilter('time', FilterPipe.Filter.LESS_THAN_EQUAL, timeTo)", type = QueryType.Gremlin)
    Iterable<YearlySegmentClearingPointInterconnectorInformation> findAllInterconnectorInformationsForInterconnectorAndTimeRange(
            @Param("interconnector") Interconnector interconnector, @Param("timeFrom") long timeFrom,
            @Param("timeTo") long timeTo);
}