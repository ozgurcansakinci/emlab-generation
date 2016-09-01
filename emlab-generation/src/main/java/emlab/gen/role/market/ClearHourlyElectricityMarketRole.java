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
package emlab.gen.role.market;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import emlab.gen.domain.agent.DecarbonizationModel;
import emlab.gen.domain.agent.Government;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.PpdpAnnual;
import emlab.gen.domain.market.electricity.YearlySegment;
import emlab.gen.domain.market.electricity.YearlySegmentClearingPointInterconnectorInformation;
import emlab.gen.domain.market.electricity.YearlySegmentClearingPointMarketInformation;
import emlab.gen.domain.technology.EnergyStorageTechnology;
import emlab.gen.domain.technology.Interconnector;
import emlab.gen.repository.Reps;
import emlab.gen.util.Utils;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

/**
 * @author asmkhan
 *
 */
@RoleComponent
public class ClearHourlyElectricityMarketRole extends AbstractClearElectricitySpotMarketRole<DecarbonizationModel>
        implements Role<DecarbonizationModel> {

    @Autowired
    private Reps reps;

    @Autowired
    Neo4jTemplate template;

    // merging prad's branch to get the dismantle role

    @Transactional
    @Override
    public void act(DecarbonizationModel model) {

        try {

            Government gov = template.findAll(Government.class).iterator().next();
            IloCplex cplex = new IloCplex();
            double co2Cap = gov.getCo2Cap(getCurrentTick());

            // Iterable<ElectricitySpotMarket> marketList = new
            // ArrayList<ElectricitySpotMarket>();
            // marketList =
            // reps.marketRepository.findAllElectricitySpotMarkets();

            // Iterable<PowerPlant> powerPlantList = new
            // ArrayList<PowerPlant>();

            List<Zone> zoneList = Utils.asList(reps.template.findAll(Zone.class));

            // List<PowerGeneratingTechnology> technologyList = Utils
            // .asList(reps.powerGeneratingTechnologyRepository.findAllIntermittentPowerGeneratingTechnologies());

            // Map<Zone, List<PowerGridNode>> zoneToNodeList = new HashMap<Zone,
            // List<PowerGridNode>>();
            ElectricitySpotMarket market1 = null;
            // int numberOfPowerPlants = 0;
            for (Zone zone : zoneList) {
                market1 = reps.marketRepository.findElectricitySpotMarketForZone(zone);
                // List<PowerGridNode> nodeList = Utils
                // .asList(reps.powerGridNodeRepository.findAllPowerGridNodesByZone(zone));
                // zoneToNodeList.put(zone, nodeList);
            }

            YearlySegment yearlySegment = reps.marketRepository
                    .findYearlySegmentForElectricitySpotMarketForTime(market1);

            int timeSteps = (int) yearlySegment.getYearlySegmentLengthInHours();

            int numberOfDays = (int) market1.getYearlySegmentLoad().getDailyElasticBaseDemandForYearlySegment()
                    .getLengthInHours();

            // for (Zone zone : zoneList) {
            //
            // for (PowerGridNode node : zoneToNodeList.get(zone)) {
            //
            // Iterable<PowerPlant> powerPlantListPerNode = null;
            //
            // powerPlantListPerNode =
            // reps.powerPlantRepository.findOperationalPowerPlantsByPowerGridNode(node,
            // getCurrentTick());
            //
            // for (PowerPlant plant : powerPlantListPerNode) {
            // numberOfPowerPlants++;
            // if (plant.getTechnology().isIntermittent()) {
            //
            // IntermittentResourceProfile intermittentResourceProfile =
            // reps.intermittentResourceProfileRepository
            // .findIntermittentResourceProfileByTechnologyAndNode(plant.getTechnology(),
            // node);
            //
            // HourlyCSVTimeSeries hourlyAvailabilityPerNode = new
            // HourlyCSVTimeSeries();
            //
            // hourlyAvailabilityPerNode.setHourlyArray(
            // intermittentResourceProfile.getHourlyArray(getCurrentTick()).clone(),
            // 0);
            //
            // hourlyAvailabilityPerNode.scalarMultiply(plant.getActualNominalCapacity());
            //
            // chageactualhourlynomcap(plant, hourlyAvailabilityPerNode);
            //
            // } else {
            //
            // HourlyCSVTimeSeries hourlyCapacity = new HourlyCSVTimeSeries();
            //
            // double[] temp = new double[8760];
            //
            // Arrays.fill(temp, plant.getActualNominalCapacity());
            //
            // hourlyCapacity.setHourlyArray(temp, 0);
            //
            // chageactualhourlynomcap(plant, hourlyCapacity);
            // }
            //
            // }
            // }
            // }

            // powerPlantList =
            // reps.powerPlantRepository.findOperationalPowerPlants(getCurrentTick());

            // double PTDFBtoAB = -0.6666666666666666;
            // double PTDFBtoAC = -0.3333333333333333;
            // double PTDFBtoBC = 0.3333333333333333;
            //
            // double PTDFCtoAB = -0.3333333333333333;
            // double PTDFCtoAC = -0.6666666666666666;
            // double PTDFCtoBC = -0.3333333333333333;

            double numberofMarkets = reps.marketRepository.countAllElectricitySpotMarkets();

            int numberOfElectricitySpotMarkets = (int) numberofMarkets;

            int numberOfPowerPlants = reps.powerPlantRepository.findNumberOfOperationalPowerPlants(getCurrentTick());

            int numberofInterconnectors = 0;

            if (numberOfElectricitySpotMarkets != 1) {

                double numberofICs = reps.interconnectorRepository.countAllInterconnectors();

                System.out.println("Number of interconnectors are: " + numberofICs);

                numberofInterconnectors = (int) numberofICs;
            }

            double[] linesSusceptances = new double[3];

            if (numberofInterconnectors == 3) {
                int interconnectorIndex = 0;
                for (Interconnector interconnector : reps.interconnectorRepository.findAllInterconnectors()) {
                    System.out.println("Name of interconnector: " + interconnector.getName());
                    linesSusceptances[interconnectorIndex] = interconnector.getTransmissionLineSusceptance();
                    interconnectorIndex++;
                }
            }

            double D1 = linesSusceptances[0] + linesSusceptances[2];
            double D2 = linesSusceptances[2] + linesSusceptances[1];
            double D3 = linesSusceptances[2];
            double D4 = linesSusceptances[2];
            double Da = D1 * D2;
            double Db = D3 * D4;
            double Deno = Da - Db;

            double Mbb11 = D2;
            double Mbb12 = D3;
            double Mbb21 = D4;
            double Mbb22 = D1;

            double Mb11 = Mbb11 / Deno;
            double Mb12 = Mbb12 / Deno;
            double Mb21 = Mbb21 / Deno;
            double Mb22 = Mbb22 / Deno;

            double Ma11 = -linesSusceptances[0];
            double Ma12 = 0;
            double Ma21 = 0;
            double Ma22 = -linesSusceptances[1];
            double Ma31 = D3;
            double Ma32 = -D3;

            double PTDFBtoAB = (Ma11 * Mb11) + (Ma12 * Mb21);
            double PTDFBtoAC = (Ma21 * Mb11) + (Ma22 * Mb21);
            double PTDFBtoBC = (Ma31 * Mb11) + (Ma32 * Mb21);

            double PTDFCtoAB = (Ma11 * Mb12) + (Ma12 * Mb22);
            double PTDFCtoAC = (Ma21 * Mb12) + (Ma22 * Mb22);
            double PTDFCtoBC = (Ma31 * Mb12) + (Ma32 * Mb22);

            // Map<ElectricitySpotMarket, List<PowerPlant>> ESMtoPPList = new
            // HashMap<ElectricitySpotMarket, List<PowerPlant>>();
            Map<ElectricitySpotMarket, List<PpdpAnnual>> ESMtoPPDPList = new HashMap<ElectricitySpotMarket, List<PpdpAnnual>>();

            // for (ElectricitySpotMarket market :
            // reps.marketRepository.findAllElectricitySpotMarkets()) {
            // List<PowerPlant> powerPlantListPerESMarket = Utils
            // .asList(reps.powerPlantRepository.findOperationalPowerPlantsInMarket(market,
            // getCurrentTick()));
            // ESMtoPPList.put(market, powerPlantListPerESMarket);
            // }
            for (ElectricitySpotMarket market : reps.marketRepository.findAllElectricitySpotMarkets()) {
                List<PpdpAnnual> ppdpAnnualListPerESMarket = Utils.asList(reps.ppdpAnnualRepository
                        .findAllSubmittedPpdpAnnualForGivenMarketAndTime(market, getCurrentTick()));
                ESMtoPPDPList.put(market, ppdpAnnualListPerESMarket);
                // System.out.println(market.getName());
            }

            // Interconnectors Data Structure

            // IloNumVar[] crossBorderGenerationAandB = new
            // IloNumVar[timeSteps];

            IloNumVar[][] crossBorderFlowVariablesForAllInterconnectors = new IloNumVar[numberofInterconnectors][timeSteps];

            // for (int i = 0; i < timeSteps; i++) {
            // crossBorderGenerationAandB[i] =
            // cplex.numVar(minMarketCrossBorderFlowAandB,
            // maxMarketCrossBorderFlowAandB);
            // }

            // The i th row contains the array of variable generation capacity
            // of plant i
            IloNumVar[][] generationCapacityofPlantsMatrix = new IloNumVar[numberOfPowerPlants][timeSteps];

            IloLinearNumExpr[][] generationEquationsForAllMarkets = new IloLinearNumExpr[numberOfElectricitySpotMarkets][timeSteps];

            IloLinearNumExpr[][] demandEquationsForAllMarkets = new IloLinearNumExpr[numberOfElectricitySpotMarkets][timeSteps];

            IloLinearNumExpr[] carbonEmissionsEquationsForAllMarkets = new IloLinearNumExpr[timeSteps];

            IloNumVar[][] inelasticDemandForAllMarkets = new IloNumVar[numberOfElectricitySpotMarkets][timeSteps];

            // Demand response data structure
            IloNumVar[][] elasticDemandForAllMarkets = new IloNumVar[numberOfElectricitySpotMarkets][timeSteps];

            IloLinearNumExpr[][] accumulativeElasticDemandPerDayForAllMarkets = new IloLinearNumExpr[numberOfElectricitySpotMarkets][numberOfDays];

            IloNumVar[][] valueOfLostLoadInMWH = new IloNumVar[numberOfElectricitySpotMarkets][timeSteps];

            // Storage data structures

            IloNumVar[][] storageChargingInMW = new IloNumVar[numberOfElectricitySpotMarkets][timeSteps];

            IloNumVar[][] storageDischargingInMW = new IloNumVar[numberOfElectricitySpotMarkets][timeSteps];

            IloNumVar[][] stateOfChargeInMWh = new IloNumVar[numberOfElectricitySpotMarkets][timeSteps];

            IloLinearNumExpr[][] storageContentExpressionsForAllMarkets = new IloLinearNumExpr[numberOfElectricitySpotMarkets][timeSteps];

            IloLinearNumExpr objective = cplex.linearNumExpr();

            int marketIndex = 0;
            int plantIndex = 0;

            // boolean interconnectorsCreated = false;

            for (ElectricitySpotMarket market : reps.marketRepository.findAllElectricitySpotMarkets()) {

                EnergyStorageTechnology storageTechnologyInMarket;
                if (market.isStorageImplemented()) {
                    storageTechnologyInMarket = reps.energyStorageTechnologyRepository
                            .findEnergyStorageTechnologyByMarket(market);

                } else {
                    storageTechnologyInMarket = null;
                }

                for (int i = 0; i < timeSteps; i++) {

                    if (marketIndex == 0 && numberofInterconnectors != 0) {
                        switch ((int) numberofInterconnectors) {
                        case 1:
                            for (Interconnector interconnector : reps.interconnectorRepository
                                    .findAllInterconnectors()) {
                                crossBorderFlowVariablesForAllInterconnectors[0][i] = cplex.numVar(
                                        -interconnector.getInterconnectorCapacity().getValue(getCurrentTick()),
                                        interconnector.getInterconnectorCapacity().getValue(getCurrentTick()));

                            }
                            break;
                        case 3:
                            int interconnectorIndex = 0;
                            for (Interconnector interconnector : reps.interconnectorRepository
                                    .findAllInterconnectors()) {

                                if (i == 0) {
                                    System.out.println("Interconnector name: " + interconnector.getName().toString()
                                            + "Capacity: "
                                            + interconnector.getInterconnectorCapacity().getValue(getCurrentTick()));
                                }

                                crossBorderFlowVariablesForAllInterconnectors[interconnectorIndex][i] = cplex.numVar(
                                        -interconnector.getInterconnectorCapacity().getValue(getCurrentTick()),
                                        interconnector.getInterconnectorCapacity().getValue(getCurrentTick()));
                                interconnectorIndex++;
                            }
                            // interconnectorsCreated = true;
                            break;
                        }
                    }

                    if (market.isStorageImplemented()) {
                        if (i == 0) {
                            stateOfChargeInMWh[marketIndex][i] = cplex.numVar(
                                    storageTechnologyInMarket.getInitialStateOfChargeInStorage(),
                                    storageTechnologyInMarket.getInitialStateOfChargeInStorage());
                            storageDischargingInMW[marketIndex][i] = cplex.numVar(0, 0);
                            storageChargingInMW[marketIndex][i] = cplex.numVar(0, 0);
                        } else {
                            stateOfChargeInMWh[marketIndex][i] = cplex.numVar(0,
                                    storageTechnologyInMarket.getCurrentMaxStorageCapacity());

                            storageDischargingInMW[marketIndex][i] = cplex.numVar(0,
                                    storageTechnologyInMarket.getCurrentMaxStorageDischargingRate());

                            storageChargingInMW[marketIndex][i] = cplex.numVar(0,
                                    storageTechnologyInMarket.getCurrentMaxStorageChargingRate());
                            storageContentExpressionsForAllMarkets[marketIndex][i] = cplex.linearNumExpr();
                        }

                        if (i != 0) {

                            storageContentExpressionsForAllMarkets[marketIndex][i].addTerm(1,
                                    stateOfChargeInMWh[marketIndex][i - 1]);

                            storageContentExpressionsForAllMarkets[marketIndex][i].addTerm(storageTechnologyInMarket
                                    .getEfficiencyInFlowTimeSeries().getValue(getCurrentTick()),
                                    storageChargingInMW[marketIndex][i]);

                            storageContentExpressionsForAllMarkets[marketIndex][i]
                                    .addTerm(
                                            -storageTechnologyInMarket.getEfficiencyOutFlowTimeSeries()
                                                    .getValue(getCurrentTick()),
                                            storageDischargingInMW[marketIndex][i]);
                        }
                    }

                    generationEquationsForAllMarkets[marketIndex][i] = cplex.linearNumExpr();

                    // crossBorderGenerationAandB[i] =
                    // cplex.numVar(minMarketCrossBorderFlowAandB,
                    // maxMarketCrossBorderFlowAandB);

                    // inelasticDemandForAllMarkets[marketIndex][i] =
                    // cplex.numVar(
                    // market.getHourlyInElasticDemandForESMarket().getHourlyArray(0)[i],
                    // market.getHourlyInElasticDemandForESMarket().getHourlyArray(0)[i]);

                    inelasticDemandForAllMarkets[marketIndex][i] = cplex.numVar(
                            market.getYearlySegmentLoad().getHourlyInElasticCurrentDemandForYearlySegment()
                                    .getHourlyArray(0)[i],
                            market.getYearlySegmentLoad().getHourlyInElasticCurrentDemandForYearlySegment()
                                    .getHourlyArray(0)[i]);
                    valueOfLostLoadInMWH[marketIndex][i] = cplex.numVar(0, Double.MAX_VALUE);
                    demandEquationsForAllMarkets[marketIndex][i] = cplex.linearNumExpr();
                    demandEquationsForAllMarkets[marketIndex][i].addTerm(1,
                            inelasticDemandForAllMarkets[marketIndex][i]);

                    if (market.isDailyDemandResponseImplemented()) {
                        elasticDemandForAllMarkets[marketIndex][i] = cplex.numVar(0, Double.MAX_VALUE);
                        demandEquationsForAllMarkets[marketIndex][i].addTerm(1,
                                elasticDemandForAllMarkets[marketIndex][i]);
                    }

                    generationEquationsForAllMarkets[marketIndex][i].addTerm(1, valueOfLostLoadInMWH[marketIndex][i]);

                    if (market.isStorageImplemented()) {
                        generationEquationsForAllMarkets[marketIndex][i].addTerm(1,
                                storageDischargingInMW[marketIndex][i]);
                        demandEquationsForAllMarkets[marketIndex][i].addTerm(1, storageChargingInMW[marketIndex][i]);
                    }

                    objective.addTerm(market.getValueOfLostLoad(), valueOfLostLoadInMWH[marketIndex][i]);

                    if (market.isDailyDemandResponseImplemented()) {

                        int demandShiftFactor = (int) market.getYearlySegmentLoad()
                                .getDailyElasticBaseDemandForYearlySegment().getElasticDemandShift();

                        if ((i + 1) % demandShiftFactor == 0 && i != 0) {
                            accumulativeElasticDemandPerDayForAllMarkets[marketIndex][((i + 1) / demandShiftFactor)
                                    - 1] = cplex.linearNumExpr();
                            for (int j = 0; j < demandShiftFactor; j++)
                                accumulativeElasticDemandPerDayForAllMarkets[marketIndex][((i + 1) / demandShiftFactor)
                                        - 1].addTerm(1,
                                                elasticDemandForAllMarkets[marketIndex][i - (demandShiftFactor - 1)
                                                        + j]);
                        }
                    }

                    if (marketIndex == 0)
                        carbonEmissionsEquationsForAllMarkets[i] = cplex.linearNumExpr();
                }

                for (PpdpAnnual ppdp : ESMtoPPDPList.get(market)) {

                    // marginalCostOfPowerPlantsForCurrentTick[plantIndex] =
                    // ppdp.getPrice();

                    // System.out.println(
                    // "Plant: " + ppdp.getPowerPlant().toString() + " Bids at
                    // Price: " + ppdp.getPrice());
                    //

                    // emissionsIntensityOfPowerPlantsForCurrentTick[plantIndex]
                    // = ppdp.getPowerPlant().calculateEmissionIntensity();

                    for (int i = 0; i < timeSteps; i++) {
                        generationCapacityofPlantsMatrix[plantIndex][i] = cplex.numVar(0,
                                ppdp.getAvailableHourlyAmount().getHourlyArray(0)[i]);

                        objective.addTerm(ppdp.getPrice(), generationCapacityofPlantsMatrix[plantIndex][i]);

                        carbonEmissionsEquationsForAllMarkets[i].addTerm(
                                ppdp.getPowerPlant().calculateEmissionIntensity(),
                                generationCapacityofPlantsMatrix[plantIndex][i]);

                        generationEquationsForAllMarkets[marketIndex][i].addTerm(1,
                                generationCapacityofPlantsMatrix[plantIndex][i]);

                    }
                    plantIndex++;
                }
                marketIndex++;
            }
            cplex.addMinimize(objective);

            IloRange[][] constraints = null;

            if (numberOfElectricitySpotMarkets != 3) {
                constraints = new IloRange[numberOfElectricitySpotMarkets][timeSteps];
            } else {
                constraints = new IloRange[numberOfElectricitySpotMarkets + 1][timeSteps];
            }

            // Creating constraints

            switch (numberOfElectricitySpotMarkets) {
            case 1:
                for (int i = 0; i < timeSteps; i++) {
                    constraints[0][i] = (IloRange) cplex.addEq(generationEquationsForAllMarkets[0][i],
                            demandEquationsForAllMarkets[0][i]);
                }
                marketIndex = 0;
                for (ElectricitySpotMarket market : reps.marketRepository.findAllElectricitySpotMarkets()) {

                    if (market.isStorageImplemented()) {

                        EnergyStorageTechnology storageTechnologyInMarket = reps.energyStorageTechnologyRepository
                                .findEnergyStorageTechnologyByMarket(market);

                        for (int i = 1; i < timeSteps; i++) {

                            cplex.addLe(storageDischargingInMW[marketIndex][i],
                                    cplex.prod(storageTechnologyInMarket.getEfficiencyOutFlowTimeSeries()
                                            .getValue(getCurrentTick()), stateOfChargeInMWh[marketIndex][i]));

                            cplex.addEq(storageContentExpressionsForAllMarkets[marketIndex][i],
                                    stateOfChargeInMWh[marketIndex][i]);
                        }
                        cplex.addEq(storageContentExpressionsForAllMarkets[marketIndex][timeSteps - 1],
                                storageTechnologyInMarket.getFinalStateOfChargeInStorage());

                    }
                    if (market.isDailyDemandResponseImplemented()) {

                        for (int i = 0; i < market.getYearlySegmentLoad().getDailyElasticBaseDemandForYearlySegment()
                                .getLengthInHours(); i++) {

                            cplex.addEq(accumulativeElasticDemandPerDayForAllMarkets[marketIndex][i],
                                    market.getYearlySegmentLoad().getDailyElasticCurrentDemandForYearlySegment()
                                            .getHourlyArray(0)[i]);
                        }
                    }
                    // marketIndex++;
                }
                break;
            case 2:
                for (int j = 0; j < timeSteps; j++) {

                    constraints[0][j] = (IloRange) cplex.addEq(generationEquationsForAllMarkets[0][j], cplex.sum(
                            demandEquationsForAllMarkets[0][j], crossBorderFlowVariablesForAllInterconnectors[0][j]));

                    constraints[1][j] = (IloRange) cplex.addEq(generationEquationsForAllMarkets[1][j], cplex.diff(
                            demandEquationsForAllMarkets[1][j], crossBorderFlowVariablesForAllInterconnectors[0][j]));

                }
                marketIndex = 0;
                for (ElectricitySpotMarket market : reps.marketRepository.findAllElectricitySpotMarkets()) {

                    if (market.isStorageImplemented()) {

                        EnergyStorageTechnology storageTechnologyInMarket = reps.energyStorageTechnologyRepository
                                .findEnergyStorageTechnologyByMarket(market);

                        for (int i = 1; i < timeSteps; i++) {

                            cplex.addLe(storageDischargingInMW[marketIndex][i],
                                    cplex.prod(storageTechnologyInMarket.getEfficiencyOutFlowTimeSeries()
                                            .getValue(getCurrentTick()), stateOfChargeInMWh[marketIndex][i]));

                            cplex.addEq(storageContentExpressionsForAllMarkets[marketIndex][i],
                                    stateOfChargeInMWh[marketIndex][i]);
                        }
                        cplex.addEq(storageContentExpressionsForAllMarkets[marketIndex][timeSteps - 1],
                                storageTechnologyInMarket.getFinalStateOfChargeInStorage());

                    }
                    if (market.isDailyDemandResponseImplemented()) {

                        for (int i = 0; i < market.getYearlySegmentLoad().getDailyElasticBaseDemandForYearlySegment()
                                .getLengthInHours(); i++) {

                            cplex.addEq(accumulativeElasticDemandPerDayForAllMarkets[marketIndex][i],
                                    market.getYearlySegmentLoad().getDailyElasticCurrentDemandForYearlySegment()
                                            .getHourlyArray(0)[i]);
                        }
                    }
                    marketIndex++;
                }
                break;
            case 3:

                for (int j = 0; j < timeSteps; j++) {

                    constraints[0][j] = (IloRange) cplex.addEq(
                            cplex.sum(generationEquationsForAllMarkets[0][j], generationEquationsForAllMarkets[1][j],
                                    generationEquationsForAllMarkets[2][j]),
                            cplex.sum(demandEquationsForAllMarkets[0][j], demandEquationsForAllMarkets[1][j],
                                    demandEquationsForAllMarkets[2][j]));

                    constraints[1][j] = (IloRange) cplex.addEq(crossBorderFlowVariablesForAllInterconnectors[0][j],
                            cplex.sum(
                                    cplex.prod(PTDFBtoAB,
                                            cplex.diff(generationEquationsForAllMarkets[1][j],
                                                    demandEquationsForAllMarkets[1][j])),
                                    cplex.prod(PTDFCtoAB, cplex.diff(generationEquationsForAllMarkets[2][j],
                                            demandEquationsForAllMarkets[2][j]))));

                    constraints[2][j] = (IloRange) cplex.addEq(crossBorderFlowVariablesForAllInterconnectors[1][j],
                            cplex.sum(
                                    cplex.prod(PTDFBtoAC,
                                            cplex.diff(generationEquationsForAllMarkets[1][j],
                                                    demandEquationsForAllMarkets[1][j])),
                                    cplex.prod(PTDFCtoAC, cplex.diff(generationEquationsForAllMarkets[2][j],
                                            demandEquationsForAllMarkets[2][j]))));

                    constraints[3][j] = (IloRange) cplex.addEq(crossBorderFlowVariablesForAllInterconnectors[2][j],
                            cplex.sum(
                                    cplex.prod(PTDFBtoBC,
                                            cplex.diff(generationEquationsForAllMarkets[1][j],
                                                    demandEquationsForAllMarkets[1][j])),
                                    cplex.prod(PTDFCtoBC, cplex.diff(generationEquationsForAllMarkets[2][j],
                                            demandEquationsForAllMarkets[2][j]))));
                }

                marketIndex = 0;
                for (ElectricitySpotMarket market : reps.marketRepository.findAllElectricitySpotMarkets()) {
                    logger.warn("Name of the market is {}", market.getName());
                    if (market.isStorageImplemented()) {

                        EnergyStorageTechnology storageTechnologyInMarket = reps.energyStorageTechnologyRepository
                                .findEnergyStorageTechnologyByMarket(market);

                        for (int i = 1; i < timeSteps; i++) {

                            cplex.addLe(storageDischargingInMW[marketIndex][i],
                                    cplex.prod(storageTechnologyInMarket.getEfficiencyOutFlowTimeSeries()
                                            .getValue(getCurrentTick()), stateOfChargeInMWh[marketIndex][i]));

                            cplex.addEq(storageContentExpressionsForAllMarkets[marketIndex][i],
                                    stateOfChargeInMWh[marketIndex][i]);
                        }
                        cplex.addEq(storageContentExpressionsForAllMarkets[marketIndex][timeSteps - 1],
                                storageTechnologyInMarket.getFinalStateOfChargeInStorage());

                    }
                    if (market.isDailyDemandResponseImplemented()) {

                        for (int i = 0; i < market.getYearlySegmentLoad().getDailyElasticBaseDemandForYearlySegment()
                                .getLengthInHours(); i++) {

                            cplex.addEq(accumulativeElasticDemandPerDayForAllMarkets[marketIndex][i],
                                    market.getYearlySegmentLoad().getDailyElasticCurrentDemandForYearlySegment()
                                            .getHourlyArray(0)[i]);
                        }
                    }
                    marketIndex++;
                }
                break;
            }

            // }
            // cplex.addLe(cplex.sum(carbonEmissionsEquationsForAllMarkets),
            // co2Cap);

            IloRange carbonConstraint = (IloRange) cplex.addLe(cplex.sum(carbonEmissionsEquationsForAllMarkets),
                    co2Cap);

            System.out.println(generationCapacityofPlantsMatrix.length);
            System.out.println(generationCapacityofPlantsMatrix[0].length);
            System.out.println(co2Cap);

            cplex.setParam(IloCplex.IntParam.Simplex.Display, 0);

            List<PpdpAnnual> ppdpAnnualList1 = Utils
                    .asList(reps.ppdpAnnualRepository.findAllSubmittedPpdpAnnualForGivenTime(getCurrentTick()));

            System.out.println(ppdpAnnualList1.size());

            if (cplex.solve()) {
                int ppdpIndex = 0;
                System.out.println("----------------------------------------------------------");
                System.out.println("Objective = " + cplex.getObjValue());
                System.out.println("Objective = " + cplex.getStatus());
                System.out.println("---------------------Market Clearing-------------------------");

                double[] Dual2 = null;
                double[] Dual3 = null;
                double[] Dual4 = null;
                // double[] interconnector1 = null;
                // double[] interconnector2 = null;
                // double[] interconnector3 = null;
                // double[] ESMprice1 = null;
                // double[] ESMprice2 = null;
                // double[] ESMprice3 = null;
                double ESMprice1, ESMprice2, ESMprice3;
                if (numberOfElectricitySpotMarkets == 3) {
                    Dual2 = new double[timeSteps];
                    Dual3 = new double[timeSteps];
                    Dual4 = new double[timeSteps];
                    // interconnector1 = new double[timeSteps];
                    // interconnector2 = new double[timeSteps];
                    // interconnector3 = new double[timeSteps];
                    // ESMprice1 = new double[timeSteps];
                    // ESMprice2 = new double[timeSteps];
                    // ESMprice3 = new double[timeSteps];
                }

                int ind = 0;
                System.out.println("Carbon constraint = " + cplex.getDual(carbonConstraint));
                for (ElectricitySpotMarket market : reps.marketRepository.findAllElectricitySpotMarkets()) {
                    YearlySegment ys = reps.marketRepository.findYearlySegmentForElectricitySpotMarketForTime(market);

                    // for (int i = 0; i < 20; i++) {
                    // if (market.isStorageImplemented()) {
                    // System.out.println("Market: " + ind + "Charging: "
                    // + cplex.getValue(storageChargingInMW[ind][i]) + "at time:
                    // " + i);
                    // System.out.println("Market: " + ind + "Discharging: "
                    // + cplex.getValue(storageDischargingInMW[ind][i]) + "at
                    // time: " + i);
                    // System.out.println("Market: " + ind + "SOC: " +
                    // cplex.getValue(stateOfChargeInMWh[ind][i])
                    // + "at time: " + i);
                    //
                    // }
                    // }

                    double[] gen = new double[timeSteps];
                    double[] dem = new double[timeSteps];
                    double[] elasticDem = new double[timeSteps];
                    double[] price = new double[timeSteps];
                    double[] charging = new double[timeSteps];
                    double[] discharging = new double[timeSteps];
                    double[] soc = new double[timeSteps];

                    for (int i = 0; i < timeSteps; i++) {

                        gen[i] = cplex.getValue(generationEquationsForAllMarkets[ind][i]);
                        dem[i] = cplex.getValue(demandEquationsForAllMarkets[ind][i]);

                        if (numberOfElectricitySpotMarkets != 3)
                            price[i] = Math.abs(cplex.getDual(constraints[ind][i]));

                        if (market.isStorageImplemented()) {
                            charging[i] = cplex.getValue(storageChargingInMW[ind][i]);
                            discharging[i] = cplex.getValue(storageDischargingInMW[ind][i]);
                            soc[i] = cplex.getValue(stateOfChargeInMWh[ind][i]);
                        }

                        if (market.isDailyDemandResponseImplemented())
                            elasticDem[i] = cplex.getValue(elasticDemandForAllMarkets[ind][i]);

                        if (numberOfElectricitySpotMarkets == 3) {
                            Dual2[i] = cplex.getDual(constraints[1][i]);
                            Dual3[i] = cplex.getDual(constraints[2][i]);
                            Dual4[i] = cplex.getDual(constraints[3][i]);
                            // interconnector1[i] =
                            // cplex.getValue(crossBorderFlowVariablesForAllInterconnectors[0][i]);
                            // interconnector2[i] =
                            // cplex.getValue(crossBorderFlowVariablesForAllInterconnectors[1][i]);
                            // interconnector3[i] =
                            // cplex.getValue(crossBorderFlowVariablesForAllInterconnectors[2][i]);

                            // ESMprice1[i] =
                            // Math.abs(cplex.getDual(constraints[0][i]));
                            // ESMprice2[i] = Math.abs(ESMprice1[i] - (PTDFBtoAB
                            // * Dual2[i]) - (PTDFBtoAC * Dual3[i])
                            // - (PTDFBtoBC * Dual4[i]));
                            // ESMprice3[i] = Math.abs(ESMprice1[i] - (PTDFCtoAB
                            // * Dual2[i]) - (PTDFCtoAC * Dual3[i])
                            // - (PTDFCtoBC * Dual4[i]));

                            ESMprice1 = Math.abs(cplex.getDual(constraints[0][i]));
                            ESMprice2 = Math.abs(ESMprice1 - (PTDFBtoAB * Dual2[i]) - (PTDFBtoAC * Dual3[i])
                                    - (PTDFBtoBC * Dual4[i]));
                            ESMprice3 = Math.abs(ESMprice1 - (PTDFCtoAB * Dual2[i]) - (PTDFCtoAC * Dual3[i])
                                    - (PTDFCtoBC * Dual4[i]));

                            switch (ind) {
                            case 0:
                                price[i] = ESMprice1;
                                break;
                            case 1:
                                price[i] = ESMprice2;
                                break;
                            case 2:
                                price[i] = ESMprice3;
                                break;
                            }
                        }

                    }
                    if (market.isStorageImplemented()) {
                        if (market.isDailyDemandResponseImplemented()) {
                            storeInDatabase(price, gen, dem, elasticDem, charging, discharging, soc,
                                    cplex.getValues(valueOfLostLoadInMWH[ind]), market, ys, getCurrentTick(),
                                    Math.abs(cplex.getDual(carbonConstraint)), (int) numberOfElectricitySpotMarkets);
                        } else {
                            storeInDatabase(price, gen, dem, null, charging, discharging, soc,
                                    cplex.getValues(valueOfLostLoadInMWH[ind]), market, ys, getCurrentTick(),
                                    Math.abs(cplex.getDual(carbonConstraint)), (int) numberOfElectricitySpotMarkets);
                        }
                    } else {
                        if (market.isDailyDemandResponseImplemented()) {
                            storeInDatabase(price, gen, dem, elasticDem, null, null, null,
                                    cplex.getValues(valueOfLostLoadInMWH[ind]), market, ys, getCurrentTick(),
                                    Math.abs(cplex.getDual(carbonConstraint)), (int) numberOfElectricitySpotMarkets);
                        } else {
                            storeInDatabase(price, gen, dem, null, null, null, null,
                                    cplex.getValues(valueOfLostLoadInMWH[ind]), market, ys, getCurrentTick(),
                                    Math.abs(cplex.getDual(carbonConstraint)), (int) numberOfElectricitySpotMarkets);
                        }
                    }

                    for (PpdpAnnual ppdp : ESMtoPPDPList.get(market)) {
                        storeAcceptedAmountPpdpAnnual(ppdp,
                                cplex.getValues(generationCapacityofPlantsMatrix[ppdpIndex]));
                        ppdpIndex++;
                        acceptAnnualBids(ppdp);
                    }
                    ind++;
                }
                switch ((int) numberofInterconnectors) {
                case 1:
                    for (Interconnector interconnector : reps.interconnectorRepository.findAllInterconnectors()) {

                        System.out.println("Storing Flows for: " + interconnector.getName().toString() + "Capacity: "
                                + interconnector.getInterconnectorCapacity().getValue(getCurrentTick()));

                        storeInDatabase(
                                reps.interconnectorRepository.findYearlySegmentForInterconnectorForTime(interconnector),
                                interconnector, getCurrentTick(),
                                cplex.getValues(crossBorderFlowVariablesForAllInterconnectors[0]));
                    }
                    break;
                case 3:
                    int interconnectorIndex = 0;
                    for (Interconnector interconnector : reps.interconnectorRepository.findAllInterconnectors()) {

                        System.out.println("Storing Flows for: " + interconnector.getName().toString() + "Capacity: "
                                + interconnector.getInterconnectorCapacity().getValue(getCurrentTick()));

                        storeInDatabase(
                                reps.interconnectorRepository.findYearlySegmentForInterconnectorForTime(interconnector),
                                interconnector, getCurrentTick(),
                                cplex.getValues(crossBorderFlowVariablesForAllInterconnectors[interconnectorIndex]));
                        interconnectorIndex++;
                    }
                    break;
                }
                // try {
                //
                // FileWriter FW = new FileWriter("/home/sk/Test CSVs/4380 Time
                // Steps/Output/Generation.csv");
                // for (int i = 0; i < timeSteps; ++i) {
                // int ind1 = 0;
                // for (ElectricitySpotMarket market :
                // reps.marketRepository.findAllElectricitySpotMarkets()) {
                // FW.write(market.toString() + " " + ",");
                // FW.write(cplex.getDual(constraints[ind1][i]) + " " + ",");
                // for (PpdpAnnual ppdp : ESMtoPPDPList.get(market)) {
                // // FW.write(ppdp.getPowerPlant().toString() + "
                // // " + ",");
                // FW.write(ppdp.getAcceptedHourlyAmount()[i] + " " + ",");
                // }
                // ind1++;
                // }
                //
                // FW.write("\n");
                // }
                // FW.flush();
                // FW.close();
                //
                // FileWriter FW1 = new FileWriter("/home/sk/Test CSVs/4380 Time
                // Steps/Output/Emission.csv");
                // FW1.write("Carbon Emissions" + " " + "," + "Generation in A"
                // + "," + "Generation in B" + " " + ","
                // + "Demand in A" + " " + "," + "Demand in B" + " " + "," +
                // "Cross Border Generation" + " "
                // + "," + "Amount of LL in A" + " " + "," + "Amount of LL in B"
                // + " " + "," + "\n");
                // for (int i = 0; i < timeSteps; ++i) {
                // FW1.write(cplex.getValue(carbonEmissionsEquationsForAllMarkets[i])
                // + " " + ","
                // + cplex.getValue(generationEquationsForAllMarkets[0][i]) + "
                // " + ","
                // + cplex.getValue(generationEquationsForAllMarkets[1][i]) + "
                // " + ","
                // + cplex.getValue(demandEquationsForAllMarkets[0][i]) + " " +
                // ","
                // + cplex.getValue(demandEquationsForAllMarkets[1][i]) + " " +
                // ","
                // + cplex.getValue(crossBorderGenerationAandB[i]) + " " + ","
                // + cplex.getValue(valueOfLostLoadInMWH[0][i]) + " " + ","
                // + cplex.getValue(valueOfLostLoadInMWH[1][i]) + " " + ","
                // + cplex.getValue(stateOfChargeInMWh[0][i]) + " " + ","
                // + cplex.getValue(storageChargingInMW[0][i]) + " " + ","
                // + cplex.getValue(storageDischargingInMW[0][i]) + " " + ","
                // + cplex.getValue(elasticDemandForAllMarkets[0][i]) + " " +
                // ","
                // + cplex.getValue(stateOfChargeInMWh[1][i]) + " " + ","
                // + cplex.getValue(storageChargingInMW[1][i]) + " " + ","
                // + cplex.getValue(storageDischargingInMW[1][i]) + " " + ","
                // + cplex.getValue(elasticDemandForAllMarkets[1][i]) + "\n");
                // // FW1.write(cplex.getValue(valueOfLostLoadInMWH[0][i])
                // // + " " + ","
                // // + cplex.getValue(valueOfLostLoadInMWH[1][i]) + "\n");
                //
                // }
                // FW1.flush();
                // FW1.close();
                //
                // } catch (IOException e) {
                // e.printStackTrace();
                // }

                // try {
                //
                // FileWriter FW = new FileWriter("/home/sk/Test CSVs/4380 Time
                // Steps/Output/Generation.csv");
                // for (int i = 0; i < timeSteps; ++i) {
                // int ind1 = 0;
                // for (ElectricitySpotMarket market :
                // reps.marketRepository.findAllElectricitySpotMarkets()) {
                // FW.write(market.toString() + " " + ",");
                // // FW.write(cplex.getDual(constraints[ind1][i]) + "
                // // " + ",");
                // for (PpdpAnnual ppdp : ESMtoPPDPList.get(market)) {
                // // FW.write(ppdp.getPowerPlant().toString() + "
                // // " + ",");
                // FW.write(ppdp.getAcceptedHourlyAmount()[i] + " " + ",");
                // }
                // ind1++;
                // }
                //
                // FW.write("\n");
                // }
                // FW.flush();
                // FW.close();
                //
                // FileWriter FW1 = new FileWriter("/home/sk/Test CSVs/4380 Time
                // Steps/Output/Emission.csv");
                // FW1.write("Carbon Emissions" + " " + "," + "Generation in A"
                // + "," + "Generation in B" + " " + ","
                // + "Demand in A" + " " + "," + "Demand in B" + " " + "," +
                // "Cross Border Generation" + " "
                // + "," + "Amount of LL in A" + " " + "," + "Amount of LL in B"
                // + " " + "," + "\n");
                // for (int i = 0; i < timeSteps; ++i) {
                // FW1.write(cplex.getValue(carbonEmissionsEquationsForAllMarkets[i])
                // + " " + ","
                // + cplex.getValue(generationEquationsForAllMarkets[0][i]) + "
                // " + ","
                // + cplex.getValue(demandEquationsForAllMarkets[0][i]) + " " +
                // ","
                // + cplex.getValue(generationEquationsForAllMarkets[1][i]) + "
                // " + ","
                // + cplex.getValue(demandEquationsForAllMarkets[1][i]) + " " +
                // ","
                // + cplex.getValue(generationEquationsForAllMarkets[2][i]) + "
                // " + ","
                // + cplex.getValue(demandEquationsForAllMarkets[2][i]) + " " +
                // ","
                // +
                // cplex.getValue(crossBorderFlowVariablesForAllInterconnectors[0][i])
                // + " " + ","
                // +
                // cplex.getValue(crossBorderFlowVariablesForAllInterconnectors[1][i])
                // + " " + ","
                // +
                // cplex.getValue(crossBorderFlowVariablesForAllInterconnectors[2][i])
                // + " " + ","
                // + cplex.getDual(constraints[0][i]) + " " + ","
                // + (cplex.getDual(constraints[0][i]) - (PTDFBtoAB * Dual2[i])
                // - (PTDFBtoAC * Dual3[i])
                // - (PTDFBtoBC * Dual4[i]))
                // + " " + ","
                // + (cplex.getDual(constraints[0][i]) - (PTDFCtoAB * Dual2[i])
                // - (PTDFCtoAC * Dual3[i])
                // - (PTDFCtoBC * Dual4[i]))
                // + " " + "," + cplex.getValue(valueOfLostLoadInMWH[0][i]) + "
                // " + ","
                // + cplex.getValue(valueOfLostLoadInMWH[1][i]) + " " + ","
                // + cplex.getValue(valueOfLostLoadInMWH[2][i]) + " " + ","
                // + cplex.getValue(stateOfChargeInMWh[0][i]) + " " + ","
                // + cplex.getValue(storageChargingInMW[0][i]) + " " + ","
                // + cplex.getValue(storageDischargingInMW[0][i]) + " " + ","
                // + cplex.getValue(stateOfChargeInMWh[1][i]) + " " + ","
                // + cplex.getValue(storageChargingInMW[1][i]) + " " + ","
                // + cplex.getValue(storageDischargingInMW[1][i]) + " " + ","
                // + cplex.getValue(stateOfChargeInMWh[2][i]) + " " + ","
                // + cplex.getValue(storageChargingInMW[2][i]) + " " + ","
                // + cplex.getValue(storageDischargingInMW[2][i]) + " " + ","
                // + cplex.getValue(elasticDemandForAllMarkets[0][i]) + " " +
                // ","
                // + cplex.getValue(elasticDemandForAllMarkets[1][i]) + " " +
                // ","
                // + cplex.getValue(elasticDemandForAllMarkets[2][i]) + "\n");
                //
                // // FW1.write(cplex.getValue(valueOfLostLoadInMWH[0][i])
                // // + " " + ","
                // // + cplex.getValue(valueOfLostLoadInMWH[1][i]) + "\n");
                //
                // }
                // FW1.flush();
                // FW1.close();
                //
                // } catch (IOException e) {
                // e.printStackTrace();
                // }

            } else

            {
                System.out.println("Something went wrong");
            }
            cplex.end();
            System.out.println("------------------------------------------------------");

        } catch (

        Exception e) {
            e.printStackTrace();
        }

        System.out.println("Starting optimization model");
        System.out.println("Starting optimization model");
        System.out.println("Starting optimization model");
        System.out.println("Starting optimization model");
        System.out.println("Starting optimization model");

        // System.exit(0);

    }

    // @Transactional
    // public void chageactualhourlynomcap(PowerPlant plant, HourlyCSVTimeSeries
    // num) {
    // plant.setActualHourlyNominalCapacity(num);
    // plant.persist();
    // // logger.warn("check within function " +
    // // plant.getActualHourlyNominalCapacity().getHourlyArray(1));
    // }

    @Transactional
    public void storeAcceptedAmountPpdpAnnual(PpdpAnnual ppdp, double[] amount) {
        ppdp.setAcceptedHourlyAmount(amount);
        ppdp.persist();
    }

    // TODO:Add more functions like this, for all other cases
    @Transactional
    public void storeInDatabase(double[] price, double[] generation, double[] demand, double[] elasticDem,
            double[] charging, double[] discharging, double[] soc, double[] valueOfLostLoad,
            ElectricitySpotMarket market, YearlySegment ys, long time, double cO2Price,
            int numberOfElectricitySpotMarkets) {
        YearlySegmentClearingPointMarketInformation info = new YearlySegmentClearingPointMarketInformation();
        // info.updateMarketPrice(price);
        // info.updateMarketSupply(generation);
        // info.updateValueOfLostLoad(valueOfLostLoad);
        // info.updateMarketDemand(demand);
        info.setMarketPrice(price);
        info.setMarketSupply(generation);
        info.setValueOfLostLoad(valueOfLostLoad);
        info.setMarketDemand(demand);
        info.setElectricitySpotMarket(market);
        info.setYearlySegment(ys);
        info.setTime(time);
        info.setCO2Price(cO2Price);
        if (market.isStorageImplemented()) {
            info.setStorageChargingInMW(charging);
            info.setStorageDischargingInMW(discharging);
            info.setStateOfChargeInMWh(soc);
        }
        if (market.isDailyDemandResponseImplemented()) {
            info.setElasticDemand(elasticDem);
        }
        info.persist();
    }

    @Transactional
    public void storeInDatabase(YearlySegment ys, Interconnector interconnector, long time, double[] flow) {
        YearlySegmentClearingPointInterconnectorInformation info = new YearlySegmentClearingPointInterconnectorInformation();
        info.setYearlySegment(ys);
        info.setInterconnector(interconnector);
        info.setTime(time);
        info.setYearlyInterconnectorFlow(flow);
        info.persist();
    }

}