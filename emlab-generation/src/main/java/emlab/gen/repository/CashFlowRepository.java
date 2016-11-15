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

import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.contract.CashFlow;
import emlab.gen.domain.technology.PowerPlant;

public interface CashFlowRepository extends GraphRepository<CashFlow> {
    @Query("START cf=node:__types__(\"className:emlab.gen.domain.contract.CashFlow\") WHERE (cf.time={time}) RETURN cf")
    Iterable<CashFlow> findAllCashFlowsForForTime(@Param("time") long time);

    @Query(value = "g.v(plant).in.filter{it.__type__=='emlab.gen.domain.contract.CashFlow' && it.time==tick}", type = QueryType.Gremlin)
    Iterable<CashFlow> findAllCashFlowsForPowerPlantForTime(@Param("plant") PowerPlant plant, @Param("tick") long tick);

    @Query(value = "g.v(plant).in('REGARDING_POWERPLANT').filter{((it.time == tick) && (it.type == 1))}", type = QueryType.Gremlin)
    CashFlow findAnnualRevenueCashFlowsForPowerPlantForTime(@Param("plant") PowerPlant plant, @Param("tick") long tick);

    @Query(value = "g.v(producer).in('TO_AGENT').filter{((it.time == tick) && (it.type == 13))}", type = QueryType.Gremlin)
    CashFlow findAllCashFlowsForStorageRevenueForTime(@Param("producer") EnergyProducer producer,
            @Param("tick") long tick);

    @Query(value = "g.v(producer).in('TO_AGENT').filter{((it.time == tick) && (it.type == 18))}", type = QueryType.Gremlin)
    CashFlow findAllCashFlowsForStorageRevenueForCapacityMarketForTime(@Param("producer") EnergyProducer producer,
            @Param("tick") long tick);

    @Query(value = "result=g.v(producer).in('TO_AGENT').filter{it.type==13 || it.type==18}.propertyFilter('time', FilterPipe.Filter.EQUAL, tick).money.sum(); if(result==null){result=0}; return result", type = QueryType.Gremlin)
    double findAllStorageRevenuesForTime(@Param("producer") EnergyProducer producer, @Param("tick") long tick);

    @Query(value = "g.v(producer).in('FROM_AGENT').filter{((it.time == tick) && (it.type == 14))}", type = QueryType.Gremlin)
    CashFlow findAllCashFlowsForStorageOMCostsForTime(@Param("producer") EnergyProducer producer,
            @Param("tick") long tick);

    @Query(value = "result=g.v(producer).in('FROM_AGENT').filter{it.type==14 || it.type==16}.propertyFilter('time', FilterPipe.Filter.EQUAL, tick).money.sum(); if(result==null){result=0}; return result", type = QueryType.Gremlin)
    double findAllCurrentStorageCostsForTime(@Param("producer") EnergyProducer producer, @Param("tick") long tick);

    @Query(value = "result=g.v(producer).in('FROM_AGENT').filter{it.type==15}.propertyFilter('time', FilterPipe.Filter.EQUAL, tick).money.sum(); if(result==null){result=0}; return result", type = QueryType.Gremlin)
    double findAllPreviousStorageCostsForTime(@Param("producer") EnergyProducer producer, @Param("tick") long tick);

    // calculating total O&M, CO2Tax, Fuel and Storage OM payments made by the
    // energy producer
    @Query(value = "result=g.v(producer).in('FROM_AGENT').filter{it.type==3 || it.type==4 || it.type==5 || it.type==9 || it.type==14}.propertyFilter('time', FilterPipe.Filter.EQUAL, tick).money.sum(); if(result==null){result=0}; return result", type = QueryType.Gremlin)
    double calculateVariableCashOutFlowsForEnergyProducerForTime(@Param("producer") EnergyProducer producer,
            @Param("tick") long tick);

    // calculating total loans and down payments made by the energy producer
    @Query(value = "result=g.v(producer).in('FROM_AGENT').filter{it.type==7 || it.type==8 || it.type==15 || it.type==16}.propertyFilter('time', FilterPipe.Filter.EQUAL, tick).money.sum(); if(result==null){result=0}; return result", type = QueryType.Gremlin)
    double calculateFixedCashOutFlowsForEnergyProducerForTime(@Param("producer") EnergyProducer producer,
            @Param("tick") long tick);

    // calculating total payments made to the energy producer from Spot market,
    // capacity market and storage
    @Query(value = "result=g.v(producer).in('TO_AGENT').filter{it.type==1 || it.type==11 || it.type==13 || it.type==18}.propertyFilter('time', FilterPipe.Filter.EQUAL, tick).money.sum(); if(result==null){result=0}; return result", type = QueryType.Gremlin)
    double calculateVariableCashInFlowsForEnergyProducerForTime(@Param("producer") EnergyProducer producer,
            @Param("tick") long tick);
}