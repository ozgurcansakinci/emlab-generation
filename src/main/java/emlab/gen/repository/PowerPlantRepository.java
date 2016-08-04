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

import java.util.ArrayList;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.domain.technology.Substance;

/**
 * Repository for {PowerPlant}
 *
 * @author ejlchappin
 * @author jcrichstein
 *
 */
@Repository
public interface PowerPlantRepository extends GraphRepository<PowerPlant> {

    /**
     * Finds plants by owner.
     *
     * @param owner
     *            of the plants
     * @return the list of plants
     */
    @Query("start owner=node({owner}) match (owner)<-[:POWERPLANT_OWNER]-(plant) return plant")
    public Iterable<PowerPlant> findPowerPlantsByOwner(@Param("owner") EnergyProducer owner);

    @Query("start owner=node({owner}) match (owner)<-[:POWERPLANT_OWNER]-(plant) return count(plant)")
    public long countPowerPlantsByOwner(@Param("owner") EnergyProducer owner);

    /**
     * Finds operational plants (only use for current tick, since only
     * officially dismantled powerplants and plants in the building process will
     * be excluded).
     *
     * @param owner
     *            of the plants
     * @param tick
     *            at which the operationality it is checked
     * @return the list of plants
     */
    @Query(value = "g.idx('__types__')[[className:'emlab.gen.domain.technology.PowerPlant']].filter{(it.dismantleTime > tick) && ((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick)}", type = QueryType.Gremlin)
    Iterable<PowerPlant> findOperationalPowerPlants(@Param("tick") long tick);

    @Query(value = "g.idx('__types__')[[className:'emlab.gen.domain.technology.PowerPlant']].filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.expectedEndOfLife > tick)}", type = QueryType.Gremlin)
    public Iterable<PowerPlant> findExpectedOperationalPowerPlants(@Param("tick") long tick);

    @Query(value = "g.idx('__types__')[[className:'emlab.gen.domain.technology.PowerPlant']].as('x').out('TECHNOLOGY').filter{(it.dismantleTime > tick) && ((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick)}", type = QueryType.Gremlin)
    Iterable<PowerPlant> findOperationalPowerPlantsWithFuelsGreaterZero(@Param("tick") long tick);

    // @Query(value =
    // "g.V.filter{it.__type__=='emlab.gen.domain.technology.PowerPlant' &&
    // ((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <=
    // tick) && (it.dismantleTime > tick)}",
    // type = QueryType.Gremlin)
    // Iterable<PowerPlant> findOperationalPowerPlants(@Param("tick") long
    // tick);

    @Query(value = "g.idx('__types__')[[className:'emlab.gen.domain.technology.PowerPlant']] .propertyFilter('dismantleTime', FilterPipe.Filter.GREATER_THAN, tick)", type = QueryType.Gremlin)
    Iterable<PowerPlant> findAllPowerPlantsWhichAreNotDismantledBeforeTick(@Param("tick") long tick);

    @Query(value = "g.idx('__types__')[[className:'emlab.gen.domain.technology.PowerPlant']] .propertyFilter('dismantleTime', FilterPipe.Filter.LESS_THAN, tick)", type = QueryType.Gremlin)
    Iterable<PowerPlant> findAllPowerPlantsDismantledBeforeTick(@Param("tick") long tick);

    @Query(value = "g.idx('__types__')[[className:'emlab.gen.domain.technology.PowerPlant']].filter{it.constructionStartTime==tick}", type = QueryType.Gremlin)
    Iterable<PowerPlant> findAllPowerPlantsWithConstructionStartTimeInTick(@Param("tick") long tick);

    /**
     * Finds operational plants and gives them back as a list (only use for
     * current tick, since only officially dismantled powerplants and plants in
     * the building process will be excluded).
     *
     * @param owner
     *            of the plants
     * @param tick
     *            at which the operationality it is checked
     * @return the list of plants
     */
    @Query(value = "g.idx('__types__')[[className:'emlab.gen.domain.technology.PowerPlant']].filter{(it.dismantleTime > tick) && ((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick)}.toList()", type = QueryType.Gremlin)
    ArrayList<PowerPlant> findOperationalPowerPlantsAsList(@Param("tick") long tick);

    @Query(value = "g.idx('__types__')[[className:'emlab.gen.domain.technology.PowerPlant']].filter{(it.dismantleTime > tick) && ((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick)}.sum{it.actualNominalCapacity};", type = QueryType.Gremlin)
    double calculateCapacityOfOperationalPowerPlants(@Param("tick") long tick);

    @Query(value = "t = new Table();"
            + "g.idx('__types__')[[className:'emlab.gen.domain.technology.PowerPlant']].filter{(it.dismantleTime > tick) && ((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick)}.as('pp').out('TECHNOLOGY').as('ty').table(t){it.actualNominalCapacity}{it.peakSegmentDependentAvailability}.cap().next(); "
            + "capacitySum = 0; for (row in t){capacitySum += row.get(0) * row.get(1);}; return capacitySum;", type = QueryType.Gremlin)
    double calculatePeakCapacityOfOperationalPowerPlants(@Param("tick") long tick);

    /**
     * Finds plants by owner and selects only operational plants.
     *
     * @param owner
     *            of the plants
     * @param tick
     *            at which the operationality it is checked
     * @return the list of plants
     */
    @Query(value = "g.v(owner).in('POWERPLANT_OWNER').filter{(it.dismantleTime > tick) && ((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick)}", type = QueryType.Gremlin)
    Iterable<PowerPlant> findOperationalPowerPlantsByOwner(@Param("owner") EnergyProducer owner,
            @Param("tick") long tick);

    @Query(value = "g.v(owner).in('POWERPLANT_OWNER').as('x').out('TECHNOLOGY').filter{it.out('FUEL').count()>0}.back('x').filter{(it.dismantleTime > tick) && ((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick)}", type = QueryType.Gremlin)
    Iterable<PowerPlant> findOperationalPowerPlantsWithFuelsGreaterZeroByOwner(@Param("owner") EnergyProducer owner,
            @Param("tick") long tick);

    /**
     * Finds plants by owner and selects only operational plants.
     *
     * @param owner
     *            of the plants
     * @param tick
     *            at which the operationality it is checked
     * @return the list of plants
     */
    @Query(value = "g.v(tech).in('TECHNOLOGY').filter{(it.dismantleTime > tick) && ((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.dismantleTime > tick)}", type = QueryType.Gremlin)
    Iterable<PowerPlant> findOperationalPowerPlantsByTechnology(@Param("tech") PowerGeneratingTechnology technology,
            @Param("tick") long tick);

    @Query(value = "result = g.v(tech).as('x').in('TECHNOLOGY').filter{(it.dismantleTime > tick) && ((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick)}.sum{it.actualNominalCapacity};"
            + "if(result == null){return 0;} else{return result;}", type = QueryType.Gremlin)
    double calculateCapacityOfOperationalPowerPlantsByTechnology(@Param("tech") PowerGeneratingTechnology technology,
            @Param("tick") long tick);

    @Query(value = "result = g.v(owner).in('POWERPLANT_OWNER').filter{it.historicalCvarDummyPlant == true}.filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.dismantleTime > tick)}.as('x').out('TECHNOLOGY').filter{it.name==g.v(tech).name}.back('x');"
            + "if(!result.hasNext()){return null;} else{return result.next();}", type = QueryType.Gremlin)
    PowerPlant findOneOperationalHistoricalCvarDummyPowerPlantsByOwnerAndTechnology(
            @Param("tech") PowerGeneratingTechnology technology, @Param("tick") long tick,
            @Param("owner") EnergyProducer owner);

    @Query(value = "result = g.v(owner).in('POWERPLANT_OWNER').filter{it.historicalCvarDummyPlant == false}.filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.dismantleTime > tick)}.as('x').out('TECHNOLOGY').filter{it.name==g.v(tech).name}.back('x').sum{it.actualNominalCapacity};"
            + "if(result == null){return 0} else{return result}", type = QueryType.Gremlin)
    public double calculateCapacityOfOperationalPowerPlantsByOwnerAndTechnology(
            @Param("tech") PowerGeneratingTechnology technology, @Param("tick") long tick,
            @Param("owner") EnergyProducer owner);

    @Query("start tech=node({tech}) match (tech)<-[:TECHNOLOGY]-(plant) return plant")
    public Iterable<PowerPlant> findPowerPlantsByTechnology(@Param("tech") PowerGeneratingTechnology technology);

    @Query("start gridnode=node({gridnode}) match (gridnode)<-[:LOCATION]-(plant) return plant")
    public Iterable<PowerPlant> findPowerPlantsByPowerGridNode(@Param("gridnode") PowerGridNode node);

    @Query(value = "g.v(gridnode).in('LOCATION').filter{(it.dismantleTime > tick) && ((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick)}", type = QueryType.Gremlin)
    public Iterable<PowerPlant> findOperationalPowerPlantsByPowerGridNode(@Param("gridnode") PowerGridNode node,
            @Param("tick") long tick);

    @Query("START owner=node({owner}), market=node({market}) "
            + "MATCH (owner)<-[:POWERPLANT_OWNER]-(plant), (market)-[:ZONE]->()<-[:REGION]-()<-[:LOCATION]-(plant)"
            + "RETURN plant")
    public Iterable<PowerPlant> findPowerPlantsByOwnerAndMarket(@Param("owner") EnergyProducer owner,
            @Param("market") ElectricitySpotMarket market);

    @Query(value = "g.v(market).out('ZONE').in('REGION').in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'}.filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.expectedEndOfLife > tick)}", type = QueryType.Gremlin)
    public Iterable<PowerPlant> findOperationalPowerPlantsInMarket(@Param("market") ElectricitySpotMarket market,
            @Param("tick") long tick);

    /////////////////////////////////////////////////////////////////

    //@Query(value = "g.v(market).out('ZONE').in('REGION').in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'}.filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.expectedEndOfLife > tick)}.out('TECHNOLOGY').filter{it.windPlant == true}.in('TECHNOLOGY')", type = QueryType.Gremlin)

    @Query(value = "g.v(market).out('ZONE').in('REGION').in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'}.filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.expectedEndOfLife > tick)}.as('x').out('TECHNOLOGY').filter{it.name == 'Wind'|| it.name=='WindOffshore'}.back('x')", type = QueryType.Gremlin)
    public Iterable<PowerPlant> findOperationalWindPlantsInMarket(@Param("market") ElectricitySpotMarket market,
            @Param("tick") long tick);

    @Query(value = "g.v(market).out('ZONE').in('REGION').in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'}.filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.expectedEndOfLife > tick)}.as('x').out('TECHNOLOGY').filter{it.name == 'Photovoltaic'}.back('x')", type = QueryType.Gremlin)
    public Iterable<PowerPlant> findOperationalPhotovoltaicPlantsInMarket(@Param("market") ElectricitySpotMarket market,
            @Param("tick") long tick);

    /////////////////////////////////////////////////////////////////

    @Query(value = "g.v(market).out('ZONE').in('REGION').in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'}.filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.dismantleTime > tick)}.sum{it.actualNominalCapacity}", type = QueryType.Gremlin)
    public double calculateCapacityOfOperationalPowerPlantsInMarket(@Param("market") ElectricitySpotMarket market,
            @Param("tick") long tick);

    @Query(value = "t = new Table();"
            + "g.v(market).out('ZONE').in('REGION').in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'}.filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.dismantleTime > tick)}.as('pp').out('TECHNOLOGY').as('ty').table(t){it.actualNominalCapacity}{it.peakSegmentDependentAvailability}.cap().next(); "
            + "capacitySum = 0; for (row in t){capacitySum += row.get(0) * row.get(1);}; return capacitySum;", type = QueryType.Gremlin)
    public double calculatePeakCapacityOfOperationalPowerPlantsInMarket(@Param("market") ElectricitySpotMarket market,
            @Param("tick") long tick);

    @Query(value = "g.v(market).out('ZONE').in('REGION').in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'}.filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.expectedEndOfLife > tick)}", type = QueryType.Gremlin)
    public Iterable<PowerPlant> findExpectedOperationalPowerPlantsInMarket(
            @Param("market") ElectricitySpotMarket market, @Param("tick") long tick);

    @Query(value = "g.v(market).out('ZONE').in('REGION').in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'}.filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.expectedEndOfLife > tick)}.sum{it.actualNominalCapacity}", type = QueryType.Gremlin)
    public double calculateCapacityOfExpectedOperationalPowerPlantsInMarket(
            @Param("market") ElectricitySpotMarket market, @Param("tick") long tick);

    @Query(value = "g.v(market).out('ZONE').in('REGION').in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'}.as('x').out('TECHNOLOGY').filter{it==g.v(tech)}.back('x').filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.expectedEndOfLife > tick)}", type = QueryType.Gremlin)
    public Iterable<PowerPlant> findExpectedOperationalPowerPlantsInMarketAndTechnology(
            @Param("market") ElectricitySpotMarket market, @Param("tick") long tick);

    @Query(value = "result = g.v(market).out('ZONE').in('REGION').in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'}.filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.expectedEndOfLife > tick)}.as('x').out('TECHNOLOGY').filter{it.name==g.v(tech).name}.back('x').sum{it.actualNominalCapacity};"
            + "if(result == null){return 0} else{return result}", type = QueryType.Gremlin)
    public double calculateCapacityOfExpectedOperationalPowerPlantsInMarketAndTechnology(
            @Param("market") ElectricitySpotMarket market, @Param("tech") PowerGeneratingTechnology technology,
            @Param("tick") long tick);

    @Query(value = "result = g.v(node).in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'}.filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.expectedEndOfLife > tick)}.as('x').out('TECHNOLOGY').filter{it.name==g.v(tech).name}.back('x').sum{it.actualNominalCapacity};"
            + "if(result == null){return 0} else{return result}", type = QueryType.Gremlin)
    public double calculateCapacityOfExpectedOperationalPowerPlantsByNodeAndTechnology(
            @Param("node") PowerGridNode node, @Param("tech") PowerGeneratingTechnology technology,
            @Param("tick") long tick);

    @Query(value = "result = g.v(market).out('ZONE').in('REGION').in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'}.as('owner').out('POWERPLANT_OWNER').filter{it==g.v(owner)}.back('owner').filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.expectedEndOfLife > tick)}.out('TECHNOLOGY').filter{it==g.v(tech)};", type = QueryType.Gremlin)
    public Iterable<PowerPlant> findExpectedOperationalPowerPlantsInMarketByOwnerAndTechnology(
            @Param("market") ElectricitySpotMarket market, @Param("tech") PowerGeneratingTechnology technology,
            @Param("tick") long tick, @Param("owner") EnergyProducer owner);

    @Query(value = "result = g.v(market).out('ZONE').in('REGION').in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'}.as('owner').out('POWERPLANT_OWNER').filter{it==g.v(owner)}.back('owner').filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.expectedEndOfLife > tick)}.as('x').out('TECHNOLOGY').filter{it==g.v(tech)}.back('x').sum{it.actualNominalCapacity};"
            + "if(result == null){return 0} else{return result}", type = QueryType.Gremlin)
    public double calculateCapacityOfExpectedOperationalPowerPlantsInMarketByOwnerAndTechnology(
            @Param("market") ElectricitySpotMarket market, @Param("tech") PowerGeneratingTechnology technology,
            @Param("tick") long tick, @Param("owner") EnergyProducer owner);

    @Query(value = "result = g.v(owner).in('POWERPLANT_OWNER').filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.expectedEndOfLife > tick)}.sum{it.actualNominalCapacity};"
            + "if(result == null){return 0} else{return result}", type = QueryType.Gremlin)
    public double calculateCapacityOfExpectedOperationalPowerPlantsByOwner(@Param("tick") long tick,
            @Param("owner") EnergyProducer owner);

    @Query(value = "result = g.v(owner).in('POWERPLANT_OWNER').filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.expectedEndOfLife > tick)}.as('x').out('TECHNOLOGY').filter{it.name==g.v(tech).name}.back('x').sum{it.actualNominalCapacity};"
            + "if(result == null){return 0} else{return result}", type = QueryType.Gremlin)
    public double calculateCapacityOfExpectedOperationalPowerPlantsByOwnerByTechnology(@Param("tick") long tick,
            @Param("owner") EnergyProducer owner, @Param("tech") PowerGeneratingTechnology pgt);

    @Query(value = "result = g.v(owner).in('POWERPLANT_OWNER').filter{it.expectedEndOfLife == tick}.as('x').out('TECHNOLOGY').filter{it.name==g.v(tech).name}.back('x').sum{it.actualNominalCapacity};"
            + "if(result == null){return 0} else{return result}", type = QueryType.Gremlin)
    public double calculateCapacityOfExpectedDismantledPowerPlantsByOwnerByTechnology(@Param("tick") long tick,
            @Param("owner") EnergyProducer owner, @Param("tech") PowerGeneratingTechnology pgt);

    @Query(value = "result = g.v(market).out('ZONE').in('REGION').in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'}.as('x').out('POWERPLANT_OWNER').filter{it==g.v(owner)}.back('x').filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.expectedEndOfLife > tick)}.out('TECHNOLOGY');", type = QueryType.Gremlin)
    public Iterable<PowerPlant> findExpectedOperationalPowerPlantsInMarketByOwner(
            @Param("market") ElectricitySpotMarket market, @Param("tick") long tick,
            @Param("owner") EnergyProducer owner);

    @Query(value = "result = g.v(market).out('ZONE').in('REGION').in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'}.out('POWERPLANT_OWNER').filter{it==g.v(owner)}.in('POWERPLANT_OWNER').filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.expectedEndOfLife > tick)}.sum{it.actualNominalCapacity};"
            + "if(result == null){return 0} else{return result}", type = QueryType.Gremlin)
    public double calculateCapacityOfExpectedOperationalPowerPlantsInMarketByOwner(
            @Param("market") ElectricitySpotMarket market, @Param("tick") long tick,
            @Param("owner") EnergyProducer owner);

    @Query(value = "g.v(market).out('ZONE').in('REGION').in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'}", type = QueryType.Gremlin)
    public Iterable<PowerPlant> findPowerPlantsInMarket(@Param("market") ElectricitySpotMarket market);

    @Query(value = "g.v(market).out('ZONE').in('REGION').in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'}.as('plant').out('POWERPLANT_OWNER').filter{it==g.v(owner)}.back('plant').filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.dismantleTime > tick)}", type = QueryType.Gremlin)
    public Iterable<PowerPlant> findOperationalPowerPlantsByOwnerAndMarket(@Param("owner") EnergyProducer owner,
            @Param("market") ElectricitySpotMarket market, @Param("tick") long tick);

    @Query(value = "plantByOwnerMarket=g.v(market).out('ZONE').in('REGION').in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'}.as('plant').out('POWERPLANT_OWNER').filter{it==g.v(owner)}.back('plant');"
            + "plantByOwnerMarket.filter{!((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.dismantleTime > tick || it.dismantleTime == 0)}", type = QueryType.Gremlin)
    public Iterable<PowerPlant> findPowerPlantsByOwnerAndMarketInPipeline(@Param("owner") EnergyProducer owner,
            @Param("market") ElectricitySpotMarket market, @Param("tick") long tick);

    @Query(value = "plantByTechnology=g.v(tech).in('TECHNOLOGY');"
            + "plantByTechnology.filter{!((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.dismantleTime > tick || it.dismantleTime == 0)}", type = QueryType.Gremlin)
    public Iterable<PowerPlant> findPowerPlantsByTechnologyInPipeline(
            @Param("tech") PowerGeneratingTechnology technology, @Param("tick") long tick);

    @Query(value = "plantByTechnology=g.v(tech).in('TECHNOLOGY');"
            + "result = plantByTechnology.filter{!((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.dismantleTime > tick || it.dismantleTime == 0)}.sum{it.actualNominalCapacity};"
            + "if(result == null){return 0} else{return result}", type = QueryType.Gremlin)
    public double calculateCapacityOfPowerPlantsByTechnologyInPipeline(
            @Param("tech") PowerGeneratingTechnology technology, @Param("tick") long tick);

    @Query(value = "plantByMarket=g.v(market).out('ZONE').in('REGION').in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'};"
            + "result = plantByMarket.filter{!((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.dismantleTime > tick || it.dismantleTime == 0)}.sum{it.actualNominalCapacity};"
            + "if(result == null){return 0} else{return result}", type = QueryType.Gremlin)
    public double calculateCapacityOfPowerPlantsByMarketInPipeline(@Param("market") ElectricitySpotMarket market,
            @Param("tick") long tick);

    @Query(value = "substanceShares = g.v(substance).in('SUBSTANCE').filter{it.__type__=='emlab.gen.domain.technology.SubstanceShareInFuelMix'};"
            + "sum=substanceShares.sum{it.share}; if(sum!=null) return sum else return 0;;", type = QueryType.Gremlin)
    public double calculateSubstanceUsage(@Param("substance") Substance substance);

    @Query(value = "g.v(gridnode).in('LOCATION').filter{(it.__type__=='emlab.gen.domain.technology.PowerPlant')}.as('p').out('TECHNOLOGY').filter{it.intermittent == true}.back('p').filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.dismantleTime > tick)}", type = QueryType.Gremlin)
    Iterable<PowerPlant> findOperationalIntermittentPowerPlantsByPowerGridNode(@Param("gridnode") PowerGridNode node,
            @Param("tick") long tick);

    @Query(value = "result = g.v(gridnode).in('LOCATION').filter{(it.__type__=='emlab.gen.domain.technology.PowerPlant')}.as('p').out('TECHNOLOGY').filter{it.intermittent == true}.back('p').filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.dismantleTime > tick)}.out('TECHNOLOGY').sum{it.capacity};"
            + "if(result == null){return 0;} else{return result;}", type = QueryType.Gremlin)
    double calculateCapacityOfIntermittentPowerPlantsByPowerGridNode(@Param("gridnode") PowerGridNode node,
            @Param("tick") long tick);

    @Query(value = "g.v(gridnode).in('LOCATION').filter{(it.__type__=='emlab.gen.domain.technology.PowerPlant')}.as('p').out('TECHNOLOGY').filter{it.intermittent == true && it==g.v(technology)}.back('p').filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.dismantleTime > tick)}", type = QueryType.Gremlin)
    Iterable<PowerPlant> findOperationalIntermittentPowerPlantsByPowerGridNodeAndTechnology(
            @Param("gridnode") PowerGridNode node,
            @Param("technology") PowerGeneratingTechnology powerGeneratingTechnology, @Param("tick") long tick);

    @Query(value = "result = g.v(gridnode).in('LOCATION').filter{(it.__type__=='emlab.gen.domain.technology.PowerPlant')}.as('p').out('TECHNOLOGY').filter{it.intermittent == true && it==g.v(technology)}.back('p').filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.dismantleTime > tick)}.sum{it.actualNominalCapacity};"
            + "if(result == null){return 0;} else{return result;}", type = QueryType.Gremlin)
    double calculateCapacityOfOperationalIntermittentPowerPlantsByPowerGridNodeAndTechnology(
            @Param("gridnode") PowerGridNode node,
            @Param("technology") PowerGeneratingTechnology powerGeneratingTechnology, @Param("tick") long tick);

    @Query(value = "result=g.v(plant).in('REGARDING_POWERPLANT').propertyFilter('type', FilterPipe.Filter.EQUAL, 1).propertyFilter('time', FilterPipe.Filter.EQUAL, tick).money.sum(); if(result==null){result=0}; return result", type = QueryType.Gremlin)
    double calculateSpotMarketRevenueOfPowerPlant(@Param("plant") PowerPlant plant, @Param("tick") long tick);

    @Query(value = "result=g.v(plant).in('REGARDING_POWERPLANT').propertyFilter('type', FilterPipe.Filter.EQUAL, 2).propertyFilter('time', FilterPipe.Filter.EQUAL, tick).money.sum(); if(result==null){result=0}; return result", type = QueryType.Gremlin)
    double calculateLongTermContractRevenueOfPowerPlant(@Param("plant") PowerPlant plant, @Param("tick") long tick);

    @Query(value = "result=g.v(plant).in('REGARDING_POWERPLANT').propertyFilter('type', FilterPipe.Filter.EQUAL, 10).propertyFilter('time', FilterPipe.Filter.EQUAL, tick).money.sum(); if(result==null){result=0}; return result", type = QueryType.Gremlin)
    double calculateStrategicReserveRevenueOfPowerPlant(@Param("plant") PowerPlant plant, @Param("tick") long tick);

    @Query(value = "result=g.v(plant).in('REGARDING_POWERPLANT').propertyFilter('type', FilterPipe.Filter.EQUAL, 11).propertyFilter('time', FilterPipe.Filter.EQUAL, tick).money.sum(); if(result==null){result=0}; return result", type = QueryType.Gremlin)
    double calculateCapacityMarketRevenueOfPowerPlant(@Param("plant") PowerPlant plant, @Param("tick") long tick);

    @Query(value = "result=g.v(plant).in('REGARDING_POWERPLANT').propertyFilter('type', FilterPipe.Filter.EQUAL, 12).propertyFilter('time', FilterPipe.Filter.EQUAL, tick).money.sum(); if(result==null){result=0}; return result", type = QueryType.Gremlin)
    double calculateCO2HedgingRevenueOfPowerPlant(@Param("plant") PowerPlant plant, @Param("tick") long tick);

    @Query(value = "result=g.v(plant).in('REGARDING_POWERPLANT').filter{it.type==5 || it.type==6 || it.type==9}.propertyFilter('time', FilterPipe.Filter.EQUAL, tick).money.sum(); if(result==null){result=0}; return result", type = QueryType.Gremlin)
    double calculateCO2CostsOfPowerPlant(@Param("plant") PowerPlant plant, @Param("tick") long tick);

    @Query(value = "result=g.v(plant).in('REGARDING_POWERPLANT').filter{it.type==3 || it.type==7 || it.type==8}.propertyFilter('time', FilterPipe.Filter.EQUAL, tick).money.sum(); if(result==null){result=0}; return result", type = QueryType.Gremlin)
    double calculateFixedCostsOfPowerPlant(@Param("plant") PowerPlant plant, @Param("tick") long tick);

    @Query(value = "ppdps=g.v(plant).in('POWERPLANT_DISPATCHPLAN').filter{it.time==tick}.propertyFilter('forecast', FilterPipe.Filter.EQUAL, false); sum=0;"
            + "fullLoadHours=0;" + "for(ppdp in ppdps){"
            + "totalAmount = ppdp.getProperty('acceptedAmount') + ppdp.getProperty('capacityLongTermContract');"
            + "if(totalAmount==null) totalAmount=0;"
            + "hoursInSegment = ppdp.out('SEGMENT_DISPATCHPLAN').next().getProperty('lengthInHours');"
            + "production = totalAmount * hoursInSegment;"
            + "fullLoadHours = fullLoadHours + hoursInSegment * totalAmount  / (ppdp.out('POWERPLANT_DISPATCHPLAN').next().actualNominalCapacity *1.0d);"
            + "}; return fullLoadHours;", type = QueryType.Gremlin)
    double calculateFullLoadHoursOfPowerPlant(@Param("plant") PowerPlant plant, @Param("tick") long tick);

}