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

import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.PpdpAnnual;
import emlab.gen.domain.market.electricity.YearlySegmentClearingPointMarketInformation;

/**
 * Repository for yearly segment clearing points
 * 
 * @author ejlchappin
 * 
 */
@Repository
public interface YearlySegmentClearingPointMarketInformationRepository
        extends GraphRepository<YearlySegmentClearingPointMarketInformation> {
    @Query(value = "g.v(market).out('MARKET_INFORMATION_POINT').propertyFilter('time', FilterPipe.Filter.EQUAL, time).as('x').in('MARKET_INFORMATION_POINT').idFilter(market, FilterPipe.Filter.EQUAL).back('x')", type = QueryType.Gremlin)
    YearlySegmentClearingPointMarketInformation findMarketInformationForMarketAndTime(@Param("time") long time,
            @Param("market") ElectricitySpotMarket electricitySpotMarket);

    @Query(value = "g.v(ppdp).out('BIDDINGMARKET').propertyFilter('time', FilterPipe.Filter.EQUAL, time).as('x').out('MARKET_INFORMATION_POINT').back('x')", type = QueryType.Gremlin)
    YearlySegmentClearingPointMarketInformation findMarketInformationForPPDPAndTime(@Param("time") long time,
            @Param("ppdp") PpdpAnnual plan);

    @Query(value = "g.v(market).out('MARKET_INFORMATION_POINT').propertyFilter('time', FilterPipe.Filter.GREATER_THAN_EQUAL, timeFrom).propertyFilter('time', FilterPipe.Filter.LESS_THAN_EQUAL, timeTo)", type = QueryType.Gremlin)
    Iterable<YearlySegmentClearingPointMarketInformation> findAllMarketInformationsForMarketAndTimeRange(
            @Param("market") ElectricitySpotMarket market, @Param("timeFrom") long timeFrom,
            @Param("timeTo") long timeTo);
}
