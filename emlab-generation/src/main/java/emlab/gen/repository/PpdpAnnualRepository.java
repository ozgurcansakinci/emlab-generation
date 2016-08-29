/*******************************************************************************
 * Copyright 2013 the original author or authors.
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

import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.market.DecarbonizationMarket;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.PpdpAnnual;
import emlab.gen.domain.technology.PowerPlant;

/**
 * @author kaveri
 *
 */
@Repository
public interface PpdpAnnualRepository extends GraphRepository<PpdpAnnual> {

    @Query(value = "g.v(market).in('BIDDINGMARKET').filter{it.__type__=='emlab.gen.domain.market.electricity.PpdpAnnual'}.propertyFilter('time', FilterPipe.Filter.EQUAL, time).filter{it.status == 1}", type = QueryType.Gremlin)
    public Iterable<PpdpAnnual> findAllSubmittedPpdpAnnualForGivenMarketAndTime(
            @Param("market") DecarbonizationMarket esm, @Param("time") long time);

    @Query(value = "g.v(market).in('BIDDINGMARKET').filter{it.__type__=='emlab.gen.domain.market.electricity.PpdpAnnual'}.propertyFilter('time', FilterPipe.Filter.EQUAL, time).filter{it.status == 2}", type = QueryType.Gremlin)
    public Iterable<PpdpAnnual> findAllAcceptedPpdpAnnualForGivenMarketAndTime(
            @Param("market") DecarbonizationMarket esm, @Param("time") long time);

    @Query(value = "g.v(producer).in('BIDDER').filter{it.__type__=='emlab.gen.domain.market.electricity.PpdpAnnual'}.propertyFilter('time', FilterPipe.Filter.EQUAL, time).filter{it.status == 2}", type = QueryType.Gremlin)
    public Iterable<PpdpAnnual> findAllAcceptedPpdpAnnualForGivenProducerAndTime(
            @Param("producer") EnergyProducer producer, @Param("time") long time);

    @Query(value = "g.v(market).in('BIDDINGMARKET').filter{it.__type__=='emlab.gen.domain.market.electricity.PpdpAnnual'}.propertyFilter('time', FilterPipe.Filter.EQUAL, time).filter{it.status == 2}.as('x').out('PPDPANNUAL_POWERPLANT').out('TECHNOLOGY').filter{it.intermittent == true}.back('x')", type = QueryType.Gremlin)
    public Iterable<PpdpAnnual> findAllAcceptedPpdpAnnualForIntermittentTechnologiesForMarketAndTime(
            @Param("market") ElectricitySpotMarket esm, @Param("time") long time);

    // g.idx('__types__')[[className:'emlab.gen.domain.technology.PowerPlant']].
    @Query(value = "g.idx('__types__')[[className:'emlab.gen.domain.market.electricity.PpdpAnnual']].propertyFilter('time', FilterPipe.Filter.EQUAL, time).filter{it.status == 1}", type = QueryType.Gremlin)
    public Iterable<PpdpAnnual> findAllSubmittedPpdpAnnualForGivenTime(@Param("time") long time);

    @Query(value = "g.idx('__types__')[[className:'emlab.gen.domain.market.electricity.PpdpAnnual']].propertyFilter('time', FilterPipe.Filter.EQUAL, time)", type = QueryType.Gremlin)
    public Iterable<PpdpAnnual> findAllPpdpAnnualForGivenTime(@Param("time") long time);

    @Query(value = "g.v(plant).in('PPDPANNUAL_POWERPLANT').filter{it.__type__=='emlab.gen.domain.market.electricity.PpdpAnnual'}.propertyFilter('time', FilterPipe.Filter.EQUAL, time)", type = QueryType.Gremlin)
    public PpdpAnnual findPPDPAnnualforPlantForCurrentTick(@Param("plant") PowerPlant plant, @Param("time") long time);
}
