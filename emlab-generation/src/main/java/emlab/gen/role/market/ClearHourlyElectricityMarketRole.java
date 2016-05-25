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

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
import emlab.gen.domain.technology.Interconnector;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.repository.Reps;
import emlab.gen.trend.HourlyCSVTimeSeries;
import emlab.gen.util.Utils;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
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

    ///////////////////////////////////////// parameters for country A

    double maxEnergycontentStorageA = 1000;
    double minEnergycontentStorageA = 0;
    double initialChargeInStorageA = 250;
    double maxStorageFlowInA = 1;
    double minStorageFlowInA = 0;
    double maxStorageFlowOutA = 1;
    double minStorageFlowOutA = 0;
    double nA = 0.90; // Storage efficiency
    double nInvA = 1 / nA; // Inverse efficiency

    double dailyMinConsumptionA = 0;
    double dailyMaxConsumptionA = Double.MAX_VALUE;

    //////////////////////////////////////////////////////////////////

    ///////////////////////////////////////// parameters for country B

    double maxEnergycontentStorageB = 1000;
    double minEnergycontentStorageB = 0;
    double initialChargeInStorageB = 500;
    double maxStorageFlowInB = 1;
    double minStorageFlowInB = 0;
    double maxStorageFlowOutB = 1;
    double minStorageFlowOutB = 0;
    double nB = 0.90; // Storage efficiency
    double nInvB = 1 / nB; // Inverse efficiency

    double dailyMinConsumptionB = 0;
    double dailyMaxConsumptionB = Double.MAX_VALUE;

    //////////////////////////////////////////////////////////////////

    ///////////////////////////////// parameters for Congestion Management

    double maxInterCapAandB = 3000;

    double minMarketCrossBorderFlowAandB = 0;
    double maxMarketCrossBorderFlowAandB = 3000;

    //////////////////////////////////////////////////////////////////



    ///////////////////////////////////////////////////////////////////

    @Transactional
    @Override
    public void act(DecarbonizationModel model) {

        try {
            Government gov = template.findAll(Government.class).iterator().next();
            IloCplex cplex = new IloCplex();
            double co2Cap = gov.getCo2Cap(getCurrentTick());

            Iterable<ElectricitySpotMarket> marketList = new ArrayList<ElectricitySpotMarket>();
            marketList = reps.marketRepository.findAllElectricitySpotMarkets();

            Iterable<PowerPlant> powerPlantList = new ArrayList<PowerPlant>();

            List<Zone> zoneList = Utils.asList(reps.template.findAll(Zone.class));

            List<PowerGeneratingTechnology> technologyList = Utils
                    .asList(reps.powerGeneratingTechnologyRepository.findAllIntermittentPowerGeneratingTechnologies());

            Map<Zone, List<PowerGridNode>> zoneToNodeList = new HashMap<Zone, List<PowerGridNode>>();

            // int numberOfPowerPlants = 0;
            for (Zone zone : zoneList) {
                List<PowerGridNode> nodeList = Utils
                        .asList(reps.powerGridNodeRepository.findAllPowerGridNodesByZone(zone));
                zoneToNodeList.put(zone, nodeList);
            }

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


            powerPlantList = reps.powerPlantRepository.findOperationalPowerPlants(getCurrentTick());

            double numberofMarkets =reps.marketRepository.countAllElectricitySpotMarkets();

            int numberOfElectricitySpotMarkets = (int) numberofMarkets;
            int numberOfPowerPlants = reps.powerPlantRepository.findNumberOfOperationalPowerPlants(getCurrentTick());

            int timeSteps = 8760;

            // The i th row contains the array of variable generation capacity
            // of plant i
            IloNumVar[][] generationCapacityofPlantsMatrix = new
                    IloNumVar[numberOfPowerPlants][timeSteps];

            // Only works when there is one interconnector
            // TODO:think about the multi node implementation
            double minMarketCrossBorderFlowAandB = 0;
            double maxMarketCrossBorderFlowAandB = reps.template.findAll(Interconnector.class).iterator().next()
                    .getCapacity(getCurrentTick());
            ;

            IloNumVar[] crossBorderGenerationAtoB = new IloNumVar[timeSteps];
            // Power flow from zone A to B
            for (int i = 0; i < timeSteps; i++) {
                crossBorderGenerationAtoB[i] = cplex.numVar(minMarketCrossBorderFlowAandB,
                        maxMarketCrossBorderFlowAandB);
            }

            IloNumVar[] crossBorderGenerationBtoA = new IloNumVar[timeSteps];
            // Power flow from zone A to B
            for (int i = 0; i < timeSteps; i++) {
                crossBorderGenerationBtoA[i] = cplex.numVar(minMarketCrossBorderFlowAandB,
                        maxMarketCrossBorderFlowAandB);
            }

            int marketIndex = 0;
            int plantIndex = 0;
            int Index = 0;
            //TODO: Right now, it is assumed that there are only two markets.

            Map<ElectricitySpotMarket, List<PowerPlant>> ESMtoPPList = new HashMap<ElectricitySpotMarket, List<PowerPlant>>();
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
            }

            IloLinearNumExpr[][] generationEquationsForAllMarkets = new IloLinearNumExpr[numberOfElectricitySpotMarkets][timeSteps];
            IloLinearNumExpr[][] demandEquationsForAllMarkets = new IloLinearNumExpr[numberOfElectricitySpotMarkets][timeSteps];
            IloLinearNumExpr[] carbonEmissionsEquationsForAllMarkets = new IloLinearNumExpr[timeSteps];
            IloNumVar[][] inelasticDemandForAllMarkets = new IloNumVar[numberOfElectricitySpotMarkets][timeSteps];
            IloLinearNumExpr objective = cplex.linearNumExpr();
            double[] marginalCostOfPowerPlantsForCurrentTick = new double[numberOfPowerPlants];
            double[] emissionsIntensityOfPowerPlantsForCurrentTick = new double[numberOfPowerPlants];
            for (ElectricitySpotMarket market : reps.marketRepository.findAllElectricitySpotMarkets()) {
                // ESMtoPPList.get(market)
                for (int i = 0; i < timeSteps; i++) {
                    generationEquationsForAllMarkets[marketIndex][i] = cplex.linearNumExpr();
                    inelasticDemandForAllMarkets[marketIndex][i] = cplex.numVar(
                            market.getHourlyInElasticDemandForESMarket().getHourlyArray(0)[i],
                            market.getHourlyInElasticDemandForESMarket().getHourlyArray(0)[i]);
                    demandEquationsForAllMarkets[marketIndex][i] = cplex.linearNumExpr();
                    demandEquationsForAllMarkets[marketIndex][i].addTerm(1,
                            inelasticDemandForAllMarkets[marketIndex][i]);
                    if (marketIndex == 0)
                        carbonEmissionsEquationsForAllMarkets[i] = cplex.linearNumExpr();
                }
                for (PpdpAnnual ppdp : ESMtoPPDPList.get(market)) {
                    marginalCostOfPowerPlantsForCurrentTick[plantIndex] = ppdp.getPrice();
                    emissionsIntensityOfPowerPlantsForCurrentTick[plantIndex] = ppdp.getPowerPlant()
                            .calculateEmissionIntensity();
                    for (int i = 0; i < timeSteps; i++) {
                        generationCapacityofPlantsMatrix[plantIndex][i] = cplex.numVar(0,
                                ppdp.getAvailableHourlyAmount().getHourlyArray(0)[i]);
                        generationEquationsForAllMarkets[marketIndex][i].addTerm(1,
                                generationCapacityofPlantsMatrix[plantIndex][i]);
                        objective.addTerm(marginalCostOfPowerPlantsForCurrentTick[plantIndex],
                                generationCapacityofPlantsMatrix[plantIndex][i]);
                        carbonEmissionsEquationsForAllMarkets[i].addTerm(
                                emissionsIntensityOfPowerPlantsForCurrentTick[plantIndex],
                                generationCapacityofPlantsMatrix[plantIndex][i]);
                    }
                    plantIndex++;
                }
                // for (PowerPlant plant : ESMtoPPList.get(market)) {
                // marginalCostOfPowerPlantsForCurrentTick[plantIndex] =
                // reps.powerPlantRepository
                // .findMarginalCostOfOperationalPlant(plant, (int)
                // getCurrentTick());
                //
                // for (int i = 0; i < timeSteps; i++) {
                // generationCapacityofPlantsMatrix[plantIndex][i] =
                // cplex.numVar(0,
                // plant.getActualHourlyNominalCapacity().getHourlyArray(0)[i]);
                //
                // generationEquationsForAllMarkets[marketIndex][i].addTerm(1,
                // generationCapacityofPlantsMatrix[plantIndex][i]);
                //
                // objective.addTerm(marginalCostOfPowerPlantsForCurrentTick[plantIndex],
                // generationCapacityofPlantsMatrix[plantIndex][i]);
                // }
                // plantIndex++;
                // }
                marketIndex++;
            }

            for (int i = 0; i < timeSteps; i++) {

                generationEquationsForAllMarkets[0][i].addTerm(1, crossBorderGenerationBtoA[i]);
                demandEquationsForAllMarkets[0][i].addTerm(1, crossBorderGenerationAtoB[i]);

                generationEquationsForAllMarkets[1][i].addTerm(1, crossBorderGenerationAtoB[i]);
                demandEquationsForAllMarkets[1][i].addTerm(1, crossBorderGenerationBtoA[i]);
            }

            cplex.addMinimize(objective);

            for (int j = 0; j < numberOfElectricitySpotMarkets; j++) {
                for (int i = 0; i < timeSteps; i++) {
                    cplex.addEq(generationEquationsForAllMarkets[j][i], demandEquationsForAllMarkets[j][i]);


                }
            }
            cplex.addLe(cplex.sum(carbonEmissionsEquationsForAllMarkets), co2Cap);

            System.out.println(generationCapacityofPlantsMatrix.length);
            System.out.println(generationCapacityofPlantsMatrix[0].length);
            System.out.println(co2Cap);

            cplex.setParam(IloCplex.IntParam.Simplex.Display, 0);

            if (cplex.solve()) {
                int ppdpIndex = 0;
                System.out.println("----------------------------------------------------------");
                System.out.println("Objective = " + cplex.getObjValue());
                System.out.println("Objective = " + cplex.getStatus());
                System.out.println("---------------------Market Clearing-------------------------");
                for (ElectricitySpotMarket market : reps.marketRepository.findAllElectricitySpotMarkets()) {
                    for (PpdpAnnual ppdp : ESMtoPPDPList.get(market)) {
                        changeAcceptedAmountPpdpAnnual(ppdp,
                                cplex.getValues(generationCapacityofPlantsMatrix[ppdpIndex]));
                        ppdpIndex++;
                        acceptAnnualBids(ppdp);// TODO:Cash flow and emissions.
                                               // Think about how to store the
                                               // supply of a plant over a year
                                               // (hashmap etc..)
                        // TODO:Investment role
                    }
                }
                try{
                    FileWriter FW = new FileWriter(
                            "/home/sk/Test CSVs/4380 Time Steps/Output/Optimization_Test_Writer_Generation.csv");
                    for (int i = 0; i < timeSteps; ++i) {
                        for (ElectricitySpotMarket market : reps.marketRepository.findAllElectricitySpotMarkets()) {
                            for (PpdpAnnual ppdp : ESMtoPPDPList.get(market)) {
                                FW.write(ppdp.getPowerPlant().toString() + " " + ",");
                                FW.write(ppdp.getAcceptedHourlyAmount()[i] + " " + ",");
                            }
                        }
                        FW.write("\n");
                    }
                    FW.flush();
                    FW.close();

                    FileWriter FW1 = new FileWriter(
                            "/home/sk/Test CSVs/4380 Time Steps/Output/Optimization_Test_Writer_Emission.csv");
                    for (int i = 0; i < timeSteps; ++i) {
                        FW1.write(cplex.getValue(carbonEmissionsEquationsForAllMarkets[i]) + " " + "," + "\n");
                    }
                    FW1.flush();
                    FW1.close();

                }

                catch (IOException e){
                    e.printStackTrace();
                }

            } else {
                System.out.println("Something went wrong");
            }
            cplex.end();
            System.out.println("------------------------------------------------------");


        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Starting optimization model");
        System.out.println("Starting optimization model");
        System.out.println("Starting optimization model");
        System.out.println("Starting optimization model");
        System.out.println("Starting optimization model");

        // System.exit(0);

    }



    @Transactional
    public void chageactualhourlynomcap(PowerPlant plant, HourlyCSVTimeSeries num) {
        plant.setActualHourlyNominalCapacity(num);
        plant.persist();
        // logger.warn("check within function " +
        // plant.getActualHourlyNominalCapacity().getHourlyArray(1));
    }

    @Transactional
    public void changeAcceptedAmountPpdpAnnual(PpdpAnnual ppdp, double[] amount) {
        ppdp.setAcceptedHourlyAmount(amount);
        ppdp.persist();
    }


}
