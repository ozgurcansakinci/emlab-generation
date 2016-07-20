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
import emlab.gen.domain.market.electricity.YearlySegmentClearingPointMarketInformation;
import emlab.gen.domain.technology.Interconnector;
import emlab.gen.repository.Reps;
import emlab.gen.util.Utils;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

//TODO: add papaya here, get the absolute values of electricity prices

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

    //////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////

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

            double numberofMarkets = reps.marketRepository.countAllElectricitySpotMarkets();

            int numberOfElectricitySpotMarkets = (int) numberofMarkets;

            int numberOfPowerPlants = reps.powerPlantRepository.findNumberOfOperationalPowerPlants(getCurrentTick());

            // int timeSteps = 8760;

            // The i th row contains the array of variable generation capacity
            // of plant i
            IloNumVar[][] generationCapacityofPlantsMatrix = new IloNumVar[numberOfPowerPlants][timeSteps];

            // Only works when there is one interconnector
            // TODO:think about the multi node implementation

            double maxMarketCrossBorderFlowAandB = reps.template.findAll(Interconnector.class).iterator().next()
                    .getCapacity(getCurrentTick());
            double minMarketCrossBorderFlowAandB = -maxMarketCrossBorderFlowAandB;

            // IloNumVar[] crossBorderGenerationAtoB = new IloNumVar[timeSteps];
            // // Power flow from zone A to B
            // for (int i = 0; i < timeSteps; i++) {
            // crossBorderGenerationAtoB[i] =
            // cplex.numVar(minMarketCrossBorderFlowAandB,
            // maxMarketCrossBorderFlowAandB);
            // }
            //
            // IloNumVar[] crossBorderGenerationBtoA = new IloNumVar[timeSteps];
            // // Power flow from zone A to B
            // for (int i = 0; i < timeSteps; i++) {
            // crossBorderGenerationBtoA[i] =
            // cplex.numVar(minMarketCrossBorderFlowAandB,
            // maxMarketCrossBorderFlowAandB);
            // }
            // TODO: Right now, it is assumed that there are only two markets.

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

            IloNumVar[] crossBorderGenerationAandB = new IloNumVar[timeSteps];
            // for (int i = 0; i < timeSteps; i++) {
            // crossBorderGenerationAandB[i] =
            // cplex.numVar(minMarketCrossBorderFlowAandB,
            // maxMarketCrossBorderFlowAandB);
            // }

            IloLinearNumExpr[][] generationEquationsForAllMarkets = new IloLinearNumExpr[numberOfElectricitySpotMarkets][timeSteps];

            IloLinearNumExpr[][] demandEquationsForAllMarkets = new IloLinearNumExpr[numberOfElectricitySpotMarkets][timeSteps];

            IloLinearNumExpr[] carbonEmissionsEquationsForAllMarkets = new IloLinearNumExpr[timeSteps];

            IloNumVar[][] inelasticDemandForAllMarkets = new IloNumVar[numberOfElectricitySpotMarkets][timeSteps];

            IloNumVar[][] valueOfLostLoadInMWH = new IloNumVar[numberOfElectricitySpotMarkets][timeSteps];

            IloLinearNumExpr objective = cplex.linearNumExpr();

            // double[] marginalCostOfPowerPlantsForCurrentTick = new
            // double[numberOfPowerPlants];

            // double[] emissionsIntensityOfPowerPlantsForCurrentTick = new
            // double[numberOfPowerPlants];

            int marketIndex = 0;
            int plantIndex = 0;
            int Index = 0;

            for (ElectricitySpotMarket market : reps.marketRepository.findAllElectricitySpotMarkets()) {
                // ESMtoPPList.get(market)
                for (int i = 0; i < timeSteps; i++) {

                    generationEquationsForAllMarkets[marketIndex][i] = cplex.linearNumExpr();

                    crossBorderGenerationAandB[i] = cplex.numVar(minMarketCrossBorderFlowAandB,
                            maxMarketCrossBorderFlowAandB);
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

                    generationEquationsForAllMarkets[marketIndex][i].addTerm(1, valueOfLostLoadInMWH[marketIndex][i]);

                    objective.addTerm(market.getValueOfLostLoad(), valueOfLostLoadInMWH[marketIndex][i]);

                    if (marketIndex == 0)
                        carbonEmissionsEquationsForAllMarkets[i] = cplex.linearNumExpr();
                }
                for (PpdpAnnual ppdp : ESMtoPPDPList.get(market)) {

                    // marginalCostOfPowerPlantsForCurrentTick[plantIndex] =
                    // ppdp.getPrice();

                    System.out.println(
                            "Plant: " + ppdp.getPowerPlant().toString() + " Bids at Price: " + ppdp.getPrice());

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

            // for (int i = 0; i < timeSteps; i++) {
            // crossBorderGenerationAandB
            // generationEquationsForAllMarkets[0][i].addTerm(1,
            // crossBorderGenerationBtoA[i]);
            // demandEquationsForAllMarkets[0][i].addTerm(1,
            // crossBorderGenerationAtoB[i]);
            //
            // generationEquationsForAllMarkets[1][i].addTerm(1,
            // crossBorderGenerationAtoB[i]);
            // demandEquationsForAllMarkets[1][i].addTerm(1,
            // crossBorderGenerationBtoA[i]);
            // }
            // System.out.println(Arrays.toString(marginalCostOfPowerPlantsForCurrentTick));
            cplex.addMinimize(objective);
            // List<IloRange> constraints = new ArrayList<IloRange>();
            // for (int j = 0; j < numberOfElectricitySpotMarkets; j++) {
            // for (int i = 0; i < timeSteps; i++) {
            // cplex.addEq(generationEquationsForAllMarkets[j][i],
            // demandEquationsForAllMarkets[j][i]);
            // }
            // }
            IloRange[][] constraints = new IloRange[numberOfElectricitySpotMarkets][timeSteps];

            // TODO: Right now, the for loop below is only working for 2 markets
            // for (int j = 0; j < numberOfElectricitySpotMarkets; j++) {
            for (int i = 0; i < timeSteps; i++) {
                // cplex.addEq(generationEquationsForAllMarkets[j][i],
                // demandEquationsForAllMarkets[j][i]);
                constraints[0][i] = (IloRange) cplex.addEq(generationEquationsForAllMarkets[0][i],
                        cplex.sum(demandEquationsForAllMarkets[0][i], crossBorderGenerationAandB[i]));
                constraints[1][i] = (IloRange) cplex.addEq(generationEquationsForAllMarkets[1][i],
                        cplex.diff(demandEquationsForAllMarkets[1][i], crossBorderGenerationAandB[i]));
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

            // for (PpdpAnnual ppdp1 : ppdpAnnualList1) {
            // // if (ppdp1.getPowerPlant().getTechnology().isIntermittent() =
            // // "false") {
            //
            // System.out.println("Capacity is: " +
            // ppdp1.getAvailableHourlyAmount().getHourlyArray(0).toString()
            // + " Price is: " + ppdp1.getPrice());
            //
            // // }
            // }

            if (cplex.solve()) {
                int ppdpIndex = 0;
                System.out.println("----------------------------------------------------------");
                System.out.println("Objective = " + cplex.getObjValue());
                System.out.println("Objective = " + cplex.getStatus());
                System.out.println("---------------------Market Clearing-------------------------");
                // for (int k = 0; k < numberOfElectricitySpotMarkets; k++) {
                // System.out.print("Dual Constraint for market " + k + " =");
                // for (int i = 0; i < timeSteps; i++) {
                // System.out.print(cplex.getDual(constraints[k][i]) + " ");
                // }
                // System.out.println(" ");
                // }
                int ind = 0;
                System.out.println("Carbon constraint = " + cplex.getDual(carbonConstraint));
                for (ElectricitySpotMarket market : reps.marketRepository.findAllElectricitySpotMarkets()) {
                    YearlySegment ys = reps.marketRepository.findYearlySegmentForElectricitySpotMarketForTime(market);
                    // YearlySegmentClearingPointMarketInformation info = new
                    // YearlySegmentClearingPointMarketInformation();
                    double[] gen = new double[timeSteps];
                    double[] dem = new double[timeSteps];
                    double[] price = new double[timeSteps];
                    for (int i = 0; i < timeSteps; i++) {
                        gen[i] = cplex.getValue(generationEquationsForAllMarkets[ind][i]);
                        dem[i] = cplex.getValue(demandEquationsForAllMarkets[ind][i]);
                        price[i] = Math.abs(cplex.getDual(constraints[ind][i]));
                    }
                    storeInDatabase(price, gen, dem, cplex.getValues(valueOfLostLoadInMWH[ind]), market, ys,
                            getCurrentTick(), Math.abs(cplex.getDual(carbonConstraint)));
                    // System.out.print(Arrays.toString(info.getMarketPrice().getHourlyArray(0)));
                    //
                    // System.out.print(Arrays.toString(info.getMarketDemand().getHourlyArray(0)));

                    for (PpdpAnnual ppdp : ESMtoPPDPList.get(market)) {
                        changeAcceptedAmountPpdpAnnual(ppdp,
                                cplex.getValues(generationCapacityofPlantsMatrix[ppdpIndex]));
                        ppdpIndex++;
                        acceptAnnualBids(ppdp);
                    }
                    ind++;
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
                // + cplex.getValue(valueOfLostLoadInMWH[1][i]) + "\n");
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
            } else {
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
    public void changeAcceptedAmountPpdpAnnual(PpdpAnnual ppdp, double[] amount) {
        ppdp.setAcceptedHourlyAmount(amount);
        ppdp.persist();
    }

    @Transactional
    public void storeInDatabase(double[] price, double[] generation, double[] demand, double[] valueOfLostLoad,
            ElectricitySpotMarket market, YearlySegment ys, long time, double cO2Price) {
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
        info.persist();
    }

}
