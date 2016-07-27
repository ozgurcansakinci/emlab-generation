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

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

import emlab.gen.domain.agent.DecarbonizationModel;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.repository.Reps;
import ilog.concert.IloException;
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

    // String inputFileDemandInZoneA = "/home/sk/Test CSVs/15 Time
    // Steps/Time_Series_Demand_A.csv";
    // String inputFileSolarIrradianceInZoneA = "/home/sk/Test CSVs/15 Time
    // Steps/Solar_Irradiance_A.csv";
    // String inputFileWindSpeedInZoneA = "/home/sk/Test CSVs/15 Time
    // Steps/Wind_Speed_A.csv";
    //
    // String inputFileElasticDemandInZoneA = "/home/sk/Test CSVs/8760 Time
    // Steps/Elastic_Time_Series_Demand_AA.csv";
    //
    // BufferedReader brDemandInZoneA = null;
    // BufferedReader brSolarIrradianceInZoneA = null;
    // BufferedReader brWindSpeedInZoneA = null;
    // BufferedReader brElasticDemandInZoneA = null;
    //
    // String line = "";
    //
    // ArrayList<String> DemandInZoneA = new ArrayList<String>();
    // ArrayList<String> SolarIrradianceInZoneA = new ArrayList<String>();
    // ArrayList<String> WindSpeedInZoneA = new ArrayList<String>();
    // ArrayList<String> ElasticDemandInZoneA = new ArrayList<String>();
    //
    // double[] totalDemand;
    // double[] SolarIrradiance;
    // double[] WindSpeed;
    // double[] ElasticDemandPerDay;

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


    @Override
    @Transactional
    public void act(DecarbonizationModel model) {

        //public void run_optimization(ArrayList<Plant> pp) {

        // Timer hourlyTimerMarket = new Timer();
        // hourlyTimerMarket.start();

        System.out.println("----------------------------------------------------------");
        System.out.println("Starting optimization model");
        //System.out.println(totalDemand.length);

        //int timeSteps = totalDemand.length;

        //YearlySegment timestep;

        int timeSteps = 8760;

        //System.out.println(timeSteps);

        System.gc();

        //System.exit(0);

        try {
            IloCplex cplex = new IloCplex();

            Iterable<ElectricitySpotMarket> marketList = new ArrayList<ElectricitySpotMarket>();
            marketList = reps.marketRepository.findAllElectricitySpotMarkets();
            Iterable<PowerPlant> powerPlantList = null;

            for (ElectricitySpotMarket market : marketList) {
                powerPlantList = reps.powerPlantRepository
                        .findOperationalPowerPlantsInMarket(market, getCurrentTick());
            }

            /*
             * Iterable<PowerPlantDispatchPlan> sortedListofPPDP =
             * plantDispatchPlanRepository
             * .findDescendingSortedPowerPlantDispatchPlansForSegmentForTime(
             * currentSegment, getCurrentTick(), false);
             *
             * for (PowerPlantDispatchPlan currentPPDP: sortedListofPPDP){
             */

            for (PowerPlant p : powerPlantList) {

                // ArrayList<IloNumVar> generationCapacityOfPlant = new
                // ArrayList<IloNumVar>(timeSteps);

                for (int i = 0; i < timeSteps; i++) {

                    // if (p.getZone().equals("Zone Country A") &&
                    // p.getTechnology().equals("Wind")) {
                    IloNumVar[] generationCapacityofPlant = new IloNumVar[timeSteps];
                    generationCapacityofPlant[i] = cplex.numVar(0,
                            p.getActualHourlyNominalCapacity().getHourlyArray(0)[i]);

                    System.out.println(generationCapacityofPlant[i]);

                }
            }

            //
            // // else if (p.getZone().equals("Zone Country A") &&
            // // p.getTechnology().equals("Photovoltaic")) {
            // //
            // // generationCapacityOfPlant.add(i, cplex.numVar(0,
            // // p.getAvailableRESCapacity().get(i)));
            // //
            // // }
            // // if (p.getZone().equals("Zone Country B") &&
            // // p.getTechnology().equals("Wind")) {
            // //
            // // generationCapacityOfPlant.add(i, cplex.numVar(0,
            // // p.getAvailableRESCapacity().get(i)));
            // // }
            // //
            // // else if (p.getZone().equals("Zone Country B") &&
            // // p.getTechnology().equals("Photovoltaic")) {
            // //
            // // generationCapacityOfPlant.add(i, cplex.numVar(0,
            // // p.getAvailableRESCapacity().get(i)));
            // //
            // // } else {
            // // generationCapacityOfPlant.add(i, cplex.numVar(0,
            // // p.getActualNominalCapacity()));
            // // }
            // // }
            // // p.setGenerationCapacityOfPlant(generationCapacityOfPlant);
            // // }
            //
            // // defining variables for Country A
            //
            // IloNumVar[] demandPerHourA = new IloNumVar[timeSteps];
            // for (int i = 0; i < timeSteps; i++) {
            // demandPerHourA[i] = cplex.numVar(totalDemand[i], totalDemand[i]);
            // }
            //
            // IloNumVar[] storageChargingA = new IloNumVar[timeSteps];
            // for (int i = 0; i < timeSteps; i++) {
            // storageChargingA[i] = cplex.numVar(minStorageFlowInA,
            // maxStorageFlowInA);
            // }
            //
            // IloNumVar[] storageDischargingA = new IloNumVar[timeSteps];
            // for (int i = 0; i < timeSteps; i++) {
            // storageDischargingA[i] = cplex.numVar(minStorageFlowOutA,
            // maxStorageFlowOutA);
            // }
            //
            // IloNumVar[] stateOfChargeInStorageA = new IloNumVar[timeSteps];
            // stateOfChargeInStorageA[0] =
            // cplex.numVar(initialChargeInStorageA,
            // initialChargeInStorageA);
            // for (int i = 1; i < timeSteps; i++) {
            // stateOfChargeInStorageA[i] =
            // cplex.numVar(minEnergycontentStorageA,
            // maxEnergycontentStorageA);
            // }
            //
            // // defining variables for Country B
            //
            // IloNumVar[] demandPerHourB = new IloNumVar[timeSteps];
            // for (int i = 0; i < timeSteps; i++) {
            // demandPerHourB[i] = cplex.numVar(totalDemand[i], totalDemand[i]);
            // }
            //
            // IloNumVar[] storageChargingB = new IloNumVar[timeSteps];
            // for (int i = 0; i < timeSteps; i++) {
            // storageChargingB[i] = cplex.numVar(minStorageFlowInB,
            // maxStorageFlowInB);
            // }
            //
            // IloNumVar[] storageDischargingB = new IloNumVar[timeSteps];
            // for (int i = 0; i < timeSteps; i++) {
            // storageDischargingB[i] = cplex.numVar(minStorageFlowOutB,
            // maxStorageFlowOutB);
            // }
            //
            // IloNumVar[] stateOfChargeInStorageB = new IloNumVar[timeSteps];
            // stateOfChargeInStorageB[0] =
            // cplex.numVar(initialChargeInStorageB,
            // initialChargeInStorageB);
            // for (int i = 1; i < timeSteps; i++) {
            // stateOfChargeInStorageB[i] =
            // cplex.numVar(minEnergycontentStorageB,
            // maxEnergycontentStorageB);
            // }
            //
            // // Market coupling (congestion management) variables
            //
            // IloNumVar[] interconnectorCapacityAandB = new
            // IloNumVar[timeSteps];
            // for (int i = 0; i < timeSteps; i++) {
            // interconnectorCapacityAandB[i] = cplex.numVar(maxInterCapAandB,
            // maxInterCapAandB);
            // }
            //
            // IloNumVar[] crossBorderGenerationAtoB = new IloNumVar[timeSteps];
            // // Power flow from zone A to B
            // for (int i = 0; i < timeSteps; i++) {
            // crossBorderGenerationAtoB[i] =
            // cplex.numVar(minMarketCrossBorderFlowAandB,
            // maxMarketCrossBorderFlowAandB);
            // }
            //
            // IloNumVar[] crossBorderGenerationBtoA = new IloNumVar[timeSteps];
            // // Power flow from zone B to A
            // for (int i = 0; i < timeSteps; i++) {
            // crossBorderGenerationBtoA[i] =
            // cplex.numVar(minMarketCrossBorderFlowAandB,
            // maxMarketCrossBorderFlowAandB);
            // }
            //
            // // defining expressions for Country A
            //
            // IloLinearNumExpr[] GenerationSideA = new
            // IloLinearNumExpr[timeSteps];
            // for (int i = 0; i < timeSteps; i++) {
            // GenerationSideA[i] = cplex.linearNumExpr();
            // for (Plant p : pp) {
            // if (p.getZone().equals("Zone Country A")) {
            // GenerationSideA[i].addTerm(1,
            // p.getGenerationCapacityOfPlant().get(i));
            // }
            // }
            // GenerationSideA[i].addTerm(1, storageDischargingA[i]);
            // GenerationSideA[i].addTerm(1, crossBorderGenerationBtoA[i]);
            // }
            //
            // IloLinearNumExpr[] DemandSideA = new IloLinearNumExpr[timeSteps];
            // for (int i = 0; i < timeSteps; i++) {
            // DemandSideA[i] = cplex.linearNumExpr();
            // DemandSideA[i].addTerm(1, demandPerHourA[i]);
            // DemandSideA[i].addTerm(1, storageChargingA[i]);
            // DemandSideA[i].addTerm(1, crossBorderGenerationAtoB[i]);
            // }
            //
            // IloLinearNumExpr[] exprStorageContentA = new
            // IloLinearNumExpr[timeSteps];
            // for (int i = 1; i < timeSteps; i++) {
            // exprStorageContentA[i] = cplex.linearNumExpr();
            // exprStorageContentA[i].addTerm(1, stateOfChargeInStorageA[i -
            // 1]);
            // exprStorageContentA[i].addTerm(nA, storageChargingA[i]);
            // exprStorageContentA[i].addTerm(-nInvA, storageDischargingA[i]);
            // }
            //
            // // defining expressions for Country B
            //
            // IloLinearNumExpr[] GenerationSideB = new
            // IloLinearNumExpr[timeSteps];
            // for (int i = 0; i < timeSteps; i++) {
            // GenerationSideB[i] = cplex.linearNumExpr();
            // for (Plant p : pp) {
            // if (p.getZone().equals("Zone Country B")) {
            // GenerationSideB[i].addTerm(1,
            // p.getGenerationCapacityOfPlant().get(i));
            // }
            // }
            // GenerationSideB[i].addTerm(1, storageDischargingA[i]);
            // GenerationSideB[i].addTerm(1, crossBorderGenerationAtoB[i]);
            // }
            //
            // IloLinearNumExpr[] DemandSideB = new IloLinearNumExpr[timeSteps];
            // for (int i = 0; i < timeSteps; i++) {
            // DemandSideB[i] = cplex.linearNumExpr();
            // DemandSideB[i].addTerm(1, demandPerHourA[i]);
            // DemandSideB[i].addTerm(1, storageChargingA[i]);
            // DemandSideB[i].addTerm(1, crossBorderGenerationBtoA[i]);
            // }
            //
            // IloLinearNumExpr[] exprStorageContentB = new
            // IloLinearNumExpr[timeSteps];
            // for (int i = 1; i < timeSteps; i++) {
            // exprStorageContentB[i] = cplex.linearNumExpr();
            // exprStorageContentB[i].addTerm(1, stateOfChargeInStorageB[i -
            // 1]);
            // exprStorageContentB[i].addTerm(nB, storageChargingB[i]);
            // exprStorageContentB[i].addTerm(-nInvB, storageDischargingB[i]);
            // }
            //
            // // OBJECTIVE FUNCTION EXPRESSION
            //
            // Iterable<PpdpAnnual> allSubmittedPpdpAnnuals =
            // reps.ppdpAnnualRepository.findAllSubmittedPpdpAnnualForGivenTime(getCurrentTick());
            //
            // IloLinearNumExpr objective = cplex.linearNumExpr();
            // for (int i = 0; i < timeSteps; ++i) {
            // for (PpdpAnnual p : allSubmittedPpdpAnnuals) {
            // objective.addTerm(p.getPrice(),
            // p.getAvailableHourlyAmount().getHourlyArray(i));
            // }
            // }
            //
            // // defining objective
            //
            // cplex.addMinimize(objective);
            //
            // // defining constraints for Country A
            //
            // for (int i = 0; i < timeSteps; ++i) {
            // cplex.addEq(GenerationSideA[i], DemandSideA[i]);
            // }
            //
            // cplex.addEq(stateOfChargeInStorageA[0], initialChargeInStorageA);
            //
            // cplex.addLe(storageDischargingA[0], stateOfChargeInStorageA[0]);
            //
            // for (int i = 1; i < timeSteps; i++) {
            // cplex.addLe(storageDischargingA[i], cplex.prod(nA,
            // stateOfChargeInStorageA[i]));
            // }
            //
            // for (int i = 1; i < timeSteps; i++) {
            // cplex.addEq(exprStorageContentA[i], stateOfChargeInStorageA[i]);
            // }
            //
            // // defining constraints for Country B
            //
            // for (int i = 0; i < timeSteps; ++i) {
            // cplex.addEq(GenerationSideB[i], DemandSideB[i]);
            // }
            //
            // cplex.addEq(stateOfChargeInStorageB[0], initialChargeInStorageB);
            //
            // cplex.addLe(storageDischargingB[0], stateOfChargeInStorageB[0]);
            //
            // for (int i = 1; i < timeSteps; i++) {
            // cplex.addLe(storageDischargingB[i], cplex.prod(nB,
            // stateOfChargeInStorageB[i]));
            // }
            //
            // for (int i = 1; i < timeSteps; i++) {
            // cplex.addEq(exprStorageContentB[i], stateOfChargeInStorageB[i]);
            // }
            //
            // // solve
            //
            // if (cplex.solve()) {
            // System.out.println("----------------------------------------------------------");
            // System.out.println("Objective = " + cplex.getObjValue());
            // System.out.println("Objective = " + cplex.getStatus());
            // System.out.println("---------------------Market
            // Cleared-----------------------");
            //
            //// for (int i = 0; i < timeSteps; i++) {
            //// for (Plant p : pp) {
            //// System.out.println(
            //// "Generation Capacity: " +
            //// cplex.getValue(p.getGenerationCapacityOfPlant().get(i)));
            //// }
            //// System.out.println("Time Step over:" + i
            //// +
            //// "-------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
            ////
            //// }
            //// for (int i = 0; i < timeSteps; i++) {
            //// System.out.println(
            //// "Cross Border Generation from B to A = " +
            //// cplex.getValue(crossBorderGenerationBtoA[i]));
            //// System.out.println(
            //// "Cross Border Generation from A to B = " +
            //// cplex.getValue(crossBorderGenerationAtoB[i]));
            //// }
            //
            //
            //
            // } else {
            // System.out.println("Something went wrong");
            // }
            // cplex.end();
            //
            //
            // hourlyTimerMarket.stop();
            // System.out.println("Optimization took: " +
            // hourlyTimerMarket.seconds() +
            // " seconds");
            // System.exit(0);
        } catch (IloException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    // public void populate_plantValues(ArrayList<Plant> pp) {
    //
    //
    //
    // }

}

// Iterable<PowerPlant> sortedListw = null;
// Iterable<PowerPlant> sortedListp = null;
// // Map<Iterable<PowerPlant>, Zone> plantListByZone = new
// // HashMap<Iterable<PowerPlant>, Zone>();
// // Map<Zone, Iterable<PowerPlant>> plantListByZone = new
// HashMap<Zone,
// // Iterable<PowerPlant>>();
//
// for (Zone zone : template.findAll(Zone.class)) {
//
// ElectricitySpotMarket market =
// reps.marketRepository.findElectricitySpotMarketForZone(zone);
//
// sortedListw =
// reps.powerPlantRepository.findOperationalWindPlantsInMarket(market,
// getCurrentTick());
//
// // plantListByZone.put(zone, sortedListw);
//
// sortedListp = reps.powerPlantRepository
// .findOperationalPhotovoltaicPlantsInMarket(market, getCurrentTick());
//
// // plantListByZone.put(zone, sortedListp);
// for (PowerPlant current : sortedListw) {
// System.out.println(current.getName());
// System.out.println(current.getLocation().getZone());
// }
// for (PowerPlant current : sortedListp) {
//
// System.out.println(current.getName());
// System.out.println(current.getLocation().getZone());
// }
// }

// for (Zone zone : plantListByZone.keySet()) {
//
// String key = zone.toString();
// String value = plantListByZone.get(zone).toString();
// System.out.println(key + " " + value);
// }

// System.exit(0);

// readInput();
//
// int timeSteps = totalDemand.length;
//
// for (Plant p : pp) {
//
// if (p.getZone().equals("Zone Country A") && p.getTechnology().equals("Wind"))
// {
//
// ArrayList<Double> availableWindPlantCapacity = new
// ArrayList<Double>(timeSteps);
//
// for (int i = 0; i < timeSteps; i++)
//
// {
// availableWindPlantCapacity.add(i, p.getActualNominalCapacity() *
// WindSpeed[i]);
// }
// p.setAvailableRESCapacity(availableWindPlantCapacity);
// }
//
// else if (p.getZone().equals("Zone Country A") &&
// p.getTechnology().equals("Photovoltaic")) {
//
// ArrayList<Double> availableSolarPlantCapacity = new
// ArrayList<Double>(timeSteps);
//
// for (int i = 0; i < timeSteps; i++)
//
// {
// availableSolarPlantCapacity.add(i, p.getActualNominalCapacity() *
// SolarIrradiance[i]);
// }
// p.setAvailableRESCapacity(availableSolarPlantCapacity);
// }
//
// else if (p.getZone().equals("Zone Country B") &&
// p.getTechnology().equals("Wind")) {
//
// ArrayList<Double> availableWindPlantCapacity = new
// ArrayList<Double>(timeSteps);
//
// for (int i = 0; i < timeSteps; i++)
//
// {
// availableWindPlantCapacity.add(i, p.getActualNominalCapacity() *
// WindSpeed[i]);
// }
// p.setAvailableRESCapacity(availableWindPlantCapacity);
// }
//
// else if (p.getZone().equals("Zone Country B") &&
// p.getTechnology().equals("Photovoltaic")) {
//
// ArrayList<Double> availableSolarPlantCapacity = new
// ArrayList<Double>(timeSteps);
//
// for (int i = 0; i < timeSteps; i++)
//
// {
// availableSolarPlantCapacity.add(i, p.getActualNominalCapacity() *
// SolarIrradiance[i]);
// }
// p.setAvailableRESCapacity(availableSolarPlantCapacity);
// }
// }
// run_optimization(pp);
// }

// ArrayList<IloNumVar[]> generationCapacityPlantArrayList = new
// ArrayList<IloNumVar[]>();

// public void readInput() {
// try {
// brDemandInZoneA = new BufferedReader(new
// FileReader(inputFileDemandInZoneA));
// while ((line = brDemandInZoneA.readLine()) != null) {
// DemandInZoneA.add(line);
// }
//
// brSolarIrradianceInZoneA = new BufferedReader(new
// FileReader(inputFileSolarIrradianceInZoneA));
// while ((line = brSolarIrradianceInZoneA.readLine()) != null) {
// SolarIrradianceInZoneA.add(line);
// }
//
// brWindSpeedInZoneA = new BufferedReader(new
// FileReader(inputFileWindSpeedInZoneA));
// while ((line = brWindSpeedInZoneA.readLine()) != null) {
// WindSpeedInZoneA.add(line);
// }
//
// brElasticDemandInZoneA = new BufferedReader(new
// FileReader(inputFileElasticDemandInZoneA));
// while ((line = brElasticDemandInZoneA.readLine()) != null) {
// ElasticDemandInZoneA.add(line);
// }
//
// } catch (FileNotFoundException e) {
// e.printStackTrace();
// } catch (IOException e) {
// e.printStackTrace();
// } finally {
// if (brDemandInZoneA != null) {
// try {
// brDemandInZoneA.close();
// } catch (IOException e) {
// e.printStackTrace();
// }
// }
// if (brSolarIrradianceInZoneA != null) {
// try {
// brSolarIrradianceInZoneA.close();
// } catch (IOException e) {
// e.printStackTrace();
// }
// }
// if (brWindSpeedInZoneA != null) {
// try {
// brWindSpeedInZoneA.close();
// } catch (IOException e) {
// e.printStackTrace();
// }
// }
// if (brElasticDemandInZoneA != null) {
// try {
// brElasticDemandInZoneA.close();
// } catch (IOException e) {
// e.printStackTrace();
// }
// }
// }
// totalDemand = new double[DemandInZoneA.size()];
// for (int i = 0; i < totalDemand.length; i++) {
// totalDemand[i] = Double.parseDouble(DemandInZoneA.get(i));
// }
// SolarIrradiance = new double[SolarIrradianceInZoneA.size()];
// for (int i = 0; i < SolarIrradiance.length; i++) {
// SolarIrradiance[i] = Double.parseDouble(SolarIrradianceInZoneA.get(i));
// }
// WindSpeed = new double[WindSpeedInZoneA.size()];
// for (int i = 0; i < WindSpeed.length; i++) {
// WindSpeed[i] = Double.parseDouble(WindSpeedInZoneA.get(i));
// }
// ElasticDemandPerDay = new double[ElasticDemandInZoneA.size()];
// for (int i = 0; i < ElasticDemandPerDay.length; i++) {
// ElasticDemandPerDay[i] = Double.parseDouble(ElasticDemandInZoneA.get(i));
// }
// }
