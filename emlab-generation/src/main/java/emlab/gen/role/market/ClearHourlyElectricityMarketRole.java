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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import cern.colt.Timer;
import emlab.gen.domain.agent.DecarbonizationModel;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.repository.Reps;
import emlab.gen.util.Utils;
import ilog.concert.IloException;
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

    double[] demand = { 300, 400, 500, 302, 230, 400, 546, 345, 486, 435 };
    // final int HOUR = 0;
    final int DEMAND = 1;
    final int IPROD = 2;
    final int RLOAD = 3;
    final int PRICE = 4;
    final int POWERPLANTNUMBER = 0;
    final int POWERPLANTVOLUME = 1;
    final int POWERPRICE = 2;
    final int CUMMULATIVEVOLUME = 3;


    @Override
    public void act(DecarbonizationModel model) {

        long clearingTick = 0;

        reps.powerPlantRepository.findOperationalPowerPlants(clearingTick);
        List<PowerPlant> powerPlantList = Utils
                .asList(reps.powerPlantRepository.findOperationalPowerPlants(clearingTick));
        // List powerPlantList =
        // Utils.asList(reps.powerPlantRepository.findOperationalPowerPlants(getCurrentTick()));

        logger.warn("List of power plants is:   " + "" + powerPlantList);

        /*
         * Iterable<PowerPlant> powerPlants; if (producer != null) { powerPlants
         * = forecast ? reps.powerPlantRepository
         * .findExpectedOperationalPowerPlantsInMarketByOwner(market, tick,
         * producer) : reps.powerPlantRepository
         * .findOperationalPowerPlantsByOwner(producer, tick); } else {
         * powerPlants = forecast ?
         * reps.powerPlantRepository.findExpectedOperationalPowerPlants(tick) :
         * reps.powerPlantRepository.findOperationalPowerPlants(tick); }
         *
         *
         * Iterable<PowerPlant> powerPlants; for (PowerPlant plant :
         * powerPlants) {
         *
         * /////////////////////////////////////////////////////////////////////
         * ///////////////////////////////////////////////////////////////
         * logger.warn("Name: " + plant.getName() + "Technology: " +
         * plant.getTechnology() + " Capacity: " +
         * plant.getActualNominalCapacity());
         * /////////////////////////////////////////////////////////////////////
         * ///////////////////////////////////////////////////////////////
         */

        /*
         * double mc; double price; mc =
         * calculateMarginalCostExclCO2MarketCost(plant, tick); price = mc *
         * producer.getPriceMarkUp();
         *
         *
         * logger.info("Submitting offers for {} with technology {}",
         * plant.getName(), plant.getTechnology().getName());
         *
         * for (SegmentLoad segmentload : market.getLoadDurationCurve()) {
         * Segment segment = segmentload.getSegment(); double capacity; if (tick
         * == getCurrentTick()) { capacity = plant.getAvailableCapacity(tick,
         * segment, numberOfSegments); } else { capacity =
         * plant.getExpectedAvailableCapacity(tick, segment, numberOfSegments);
         * }
         *
         * logger.info("I bid capacity: {} and price: {}", capacity, mc);
         *
         * logger.warn("Capacity is:" + capacity); logger.warn("Price is:" +
         * mc);}
         */




        /*
         * try { IloCplex cplex = new IloCplex();
         *
         * // defining variables IloNumVar x = cplex.numVar(0, Double.MAX_VALUE,
         * "x"); IloNumVar y = cplex.numVar(0, Double.MAX_VALUE, "y");
         *
         * // defining expressions IloLinearNumExpr objective =
         * cplex.linearNumExpr(); objective.addTerm(0.12, x);
         * objective.addTerm(0.15, y);
         *
         * // defining objective
         *
         * cplex.minimize(objective);
         *
         * // Defining constraints
         *
         * cplex.addGe(cplex.sum(cplex.prod(60, x), cplex.prod(60, y)), 300);
         * cplex.addGe(cplex.sum(cplex.prod(12, x), cplex.prod(6, y)), 36);
         * cplex.addGe(cplex.sum(cplex.prod(10, x), cplex.prod(30, y)), 90);
         *
         * // solve
         *
         * if (cplex.solve()){ logger.warn("Objective = " +
         * cplex.getObjValue()); logger.warn("x = " + cplex.getValue(x));
         * logger.warn("y = " + cplex.getValue(y)); } else { logger.warn(
         * "Something went wrong"); }
         *
         * } catch (IloException e) { // TODO Auto-generated catch block
         * e.printStackTrace(); }
         */

        Timer hourlyTimerMarket = new Timer();
        hourlyTimerMarket.start();

        double marginalCostGasPlant = 90.67;
        double marginalCostNuclearPlant = 30.98;
        double marginalCostCoalPlant = 50.44;
        double marginalCostWindPlant = 0;
        double marginalCostSolarPlant = 0;

        double maxGenerationCapacityGasPlant = 4000;
        double maxGenerationCapacityNuclearPlant = 2800;
        double maxGenerationCapacityCoalPlant = 5000;
        double maxGenerationCapacityWindPlant = 500;
        double maxGenerationCapacitySolarPlant = 1000;

        double maxEnergycontentStorage = 1000;
        double minEnergycontentStorage = 0;
        double initialChargeInStorage = 250;
        // double finalChargeInStorage = 250;
        double maxStorageFlowIn = 200;
        double minStorageFlowIn = 0;
        double maxStorageFlowOut = 200;
        double minStorageFlowOut = 0;
        double n = 0.90; // Storage efficiency
        double nInv = 1 / n; // Inverse efficiency

        double marginalCostOfStorageCharging = 0;
        double marginalCostOfStorageDisharging = 0;
        double marginalCostOfStateOfChargeInStorage = 0;

        double[] SolarIrradiance = new double[] { 0.6, 0.7, 0.8, 0.9, 0.2, 0.6, 0.7, 0.8, 0.9, 0.2, 0.6, 0.7, 0.8, 0.9,
                0.2 };
        double[] WindSpeed = new double[] { 0.6, 0.7, 0.8, 0.9, 0.2, 0.6, 0.7, 0.8, 0.9, 0.2, 0.6, 0.7, 0.8, 0.9, 0.2 };

        // double[] totalDemand = new double[] { 5000, 1200, 3200, 1200, 3000,
        // 5000, 1200, 3200, 1200, 3000, 5000, 1200, 3200, 1000, 3000 };

        double[] totalDemand = new double[] { 5000, 1000, 5000, 1000, 5000, 1000, 5000, 1000, 5000, 1000, 5000, 1000,
                5000, 1000, 5000 };

        // double[] totalDemand = new double[] { 5000, 5000, 5000, 5000, 5000,
        // 5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000 };

        double[] availableSolarCapacity = new double[SolarIrradiance.length];
        for (int i = 0; i < SolarIrradiance.length; i++) {
            availableSolarCapacity[i] = SolarIrradiance[i] * maxGenerationCapacitySolarPlant;
        }

        double[] availableWindCapacity = new double[WindSpeed.length];
        for (int i = 0; i < WindSpeed.length; i++) {
            availableWindCapacity[i] = WindSpeed[i] * maxGenerationCapacityWindPlant;
        }

        for (int i = 0; i < WindSpeed.length; i++) {
            System.out.println(availableWindCapacity[i]);
        }
        System.out.println("----------------------------------------------------------");

        for (int i = 0; i < SolarIrradiance.length; i++) {
            System.out.println(availableSolarCapacity[i]);
        }

        // Timer hourlyTimerMarket = new Timer();
        // hourlyTimerMarket.start();

        System.out.println("----------------------------------------------------------");
        System.out.println("Starting optimization model");

        int timeSteps = 15;

        try {
            IloCplex cplex1 = new IloCplex();

            // defining variables

            IloNumVar[] generationCapacityGasPlant = new IloNumVar[timeSteps];
            for (int i = 0; i < timeSteps; i++) {
                generationCapacityGasPlant[i] = cplex1.numVar(0, maxGenerationCapacityGasPlant);
            }

            IloNumVar[] generationCapacityNuclearPlant = new IloNumVar[timeSteps];
            for (int i = 0; i < timeSteps; i++) {
                generationCapacityNuclearPlant[i] = cplex1.numVar(0, maxGenerationCapacityNuclearPlant);
            }

            IloNumVar[] generationCapacityCoalPlant = new IloNumVar[timeSteps];
            for (int i = 0; i < timeSteps; i++) {
                generationCapacityCoalPlant[i] = cplex1.numVar(0, maxGenerationCapacityCoalPlant);
            }

            IloNumVar[] generationCapacityWindPlant = new IloNumVar[timeSteps];
            for (int i = 0; i < timeSteps; i++) {
                generationCapacityWindPlant[i] = cplex1.numVar(0, availableWindCapacity[i]);
            }

            IloNumVar[] generationCapacitySolarPlant = new IloNumVar[timeSteps];
            for (int i = 0; i < timeSteps; i++) {
                generationCapacitySolarPlant[i] = cplex1.numVar(0, availableSolarCapacity[i]);
            }

            IloNumVar[] demandPerHour = new IloNumVar[timeSteps];
            for (int i = 0; i < timeSteps; i++) {
                demandPerHour[i] = cplex1.numVar(totalDemand[i], totalDemand[i]);
            }

            IloNumVar[] Hour = new IloNumVar[timeSteps];
            for (int i = 0; i < timeSteps; i++) {
                Hour[i] = cplex1.numVar(i, i);
            }

            IloNumVar[] storageCharging = new IloNumVar[timeSteps];
            for (int i = 0; i < timeSteps; i++) {
                storageCharging[i] = cplex1.numVar(minStorageFlowIn, maxStorageFlowIn);
            }

            IloNumVar[] storageDischarging = new IloNumVar[timeSteps];
            for (int i = 0; i < timeSteps; i++) {
                storageDischarging[i] = cplex1.numVar(minStorageFlowOut, maxStorageFlowOut);
            }

            IloNumVar[] stateOfChargeInStorage = new IloNumVar[timeSteps];
            stateOfChargeInStorage[0] = cplex1.numVar(initialChargeInStorage, initialChargeInStorage);
            // stateOfChargeInStorage[timeSteps] =
            // cplex1.numVar(finalChargeInStorage, finalChargeInStorage);
            for (int i = 1; i < timeSteps; i++) {
                stateOfChargeInStorage[i] = cplex1.numVar(minEnergycontentStorage, maxEnergycontentStorage);
            }

            // defining expressions

            IloLinearNumExpr[] GenerationEquation = new IloLinearNumExpr[timeSteps];
            for (int i = 0; i < timeSteps; i++) {
                GenerationEquation[i] = cplex1.linearNumExpr();
                GenerationEquation[i].addTerm(1, generationCapacityWindPlant[i]);
                GenerationEquation[i].addTerm(1, generationCapacitySolarPlant[i]);
                GenerationEquation[i].addTerm(1, generationCapacityGasPlant[i]);
                GenerationEquation[i].addTerm(1, generationCapacityNuclearPlant[i]);
                GenerationEquation[i].addTerm(1, generationCapacityCoalPlant[i]);
                GenerationEquation[i].addTerm(1, storageDischarging[i]);
            }

            IloLinearNumExpr[] DemandEquation = new IloLinearNumExpr[timeSteps];
            for (int i = 0; i < timeSteps; i++) {
                DemandEquation[i] = cplex1.linearNumExpr();
                DemandEquation[i].addTerm(1, demandPerHour[i]);
                DemandEquation[i].addTerm(1, storageCharging[i]);
            }

            IloLinearNumExpr[] exprStorageContent = new IloLinearNumExpr[timeSteps];
            for (int i = 1; i < timeSteps; i++) {
                exprStorageContent[i] = cplex1.linearNumExpr();
                exprStorageContent[i].addTerm(1, stateOfChargeInStorage[i - 1]);
                exprStorageContent[i].addTerm(n, storageCharging[i - 1]);
                exprStorageContent[i].addTerm(-nInv, storageDischarging[i - 1]);
            }

            IloLinearNumExpr objective = cplex1.linearNumExpr();
            for (int i = 0; i < timeSteps; ++i) {
                // objective.addTerm(marginalCostWindPlant,
                // generationCapacityWindPlant[i]);
                // objective.addTerm(marginalCostSolarPlant,
                // generationCapacitySolarPlant[i]);
                objective.addTerm(marginalCostGasPlant, generationCapacityGasPlant[i]);
                objective.addTerm(marginalCostNuclearPlant, generationCapacityNuclearPlant[i]);
                objective.addTerm(marginalCostCoalPlant, generationCapacityCoalPlant[i]);
                // objective.addTerm(marginalCostOfStorageCharging,
                // storageCharging[i]);
                // objective.addTerm(marginalCostOfStorageDisharging,
                // storageDischarging[i]);
                // objective.addTerm(marginalCostOfStateOfChargeInStorage,
                // stateOfChargeInStorage[i]);
            }

            // defining objective

            cplex1.addMinimize(objective);

            // defining constraints

            for (int i = 0; i < timeSteps; ++i) {
                cplex1.addEq(GenerationEquation[i], DemandEquation[i]);
            }

            cplex1.addEq(stateOfChargeInStorage[0], initialChargeInStorage);

            // cplex1.addEq(stateOfChargeInStorage[timeSteps],
            // finalChargeInStorage);

            cplex1.addLe(storageDischarging[0], stateOfChargeInStorage[0]);

            // cplex1.addLe(storageDischarging[timeSteps],
            // stateOfChargeInStorage[timeSteps]);

            for (int i = 1; i < timeSteps; i++) {
                cplex1.addLe(storageDischarging[i], cplex1.prod(n, stateOfChargeInStorage[i]));
            }

            for (int i = 1; i < timeSteps; i++) {
                cplex1.addEq(exprStorageContent[i], stateOfChargeInStorage[i]);
            }

            // cplex1.setParam(IloCplex.IntParam.Simplex.Display, 0);

            // solve

            if (cplex1.solve()) {
                System.out.println("----------------------------------------------------------");
                System.out.println("Objective = " + cplex1.getObjValue());
                System.out.println("Objective = " + cplex1.getStatus());
                System.out.println("---------------------Market Opens-------------------------");

                for (int i = 0; i < timeSteps; ++i) {
                    System.out.println("----------------------Time Step " + (i + 1) + "-------------------------");
                    System.out.println(
                            "Generation Capacity of Gas Plant     = " + cplex1.getValue(generationCapacityGasPlant[i]));
                    System.out.println("Generation Capacity of Nuclear Plant = "
                            + cplex1.getValue(generationCapacityNuclearPlant[i]));
                    System.out.println("Generation Capacity of Coal Plant    = "
                            + cplex1.getValue(generationCapacityCoalPlant[i]));
                    System.out.println("Generation Capacity of Wind Plant    = "
                            + cplex1.getValue(generationCapacityWindPlant[i]));
                    System.out.println("Generation Capacity of Solar Plant   = "
                            + cplex1.getValue(generationCapacitySolarPlant[i]));
                    System.out.println("*******************************");
                    System.out.println("Storage Charging   = " + cplex1.getValue(storageCharging[i]));
                    System.out.println("Storage Discharging   = " + cplex1.getValue(storageDischarging[i]));
                    System.out.println("State of Charge in Storage   = " + cplex1.getValue(stateOfChargeInStorage[i]));
                    System.out.println("*******************************");
                    System.out.println("-------------------------------");
                    System.out.println("Total Generation =  " + cplex1.getValue(GenerationEquation[i]));
                    System.out.println("Total Demand =  " + cplex1.getValue(demandPerHour[i]));
                    System.out.println("-------------------------------");
                }
            } else {
                System.out.println("Something went wrong");
            }
            cplex1.end();
        } catch (IloException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        hourlyTimerMarket.stop();
        logger.warn("        4379 hourssssssssssssssssssssssssss took: {} seconds.", hourlyTimerMarket.seconds());


        // COMES FROM A COLT CERN CLASS
        /*
         * ver 4.38
         *
         * DoubleMatrix2D marketClearingMatrix = new DenseDoubleMatrix2D(8760,
         * 5); // Setting up hours in first column for (int HOUR = 0; HOUR <
         * 8760; HOUR++) { marketClearingMatrix.set(HOUR, 0, (HOUR + 1)); }
         * String filename =
         * "/home/sk/emlab-generation/emlab-generation/src/main/resources/data/demand_data.csv";
         * BufferedReader bufferedReader = null;
         *
         * // Setting up demand in second column try { // Read demand file
         *
         * // InputStreamReader inputStreamReader = new //
         * InputStreamReader(this.getClass().getResourceAsStream(filename)); //
         * BufferedReader bufferedReader = new //
         * BufferedReader(inputStreamReader);
         *
         * bufferedReader = new BufferedReader(new FileReader(filename));
         *
         * String line; int demand = 0; // skipping header
         * bufferedReader.readLine();
         *
         * while ((line = bufferedReader.readLine()) != null) {
         *
         * String[] demandValues = line.split(","); double demandValue =
         * Double.parseDouble(demandValues[1]); marketClearingMatrix.set(demand,
         * 1, demandValue); demand++; logger.warn("demand is" + demand);
         *
         * } bufferedReader.close(); } catch (Exception e) { logger.error(
         * "Couldn't read CSV file: " + filename); e.printStackTrace(); }
         * filename =
         * "/home/sk/emlab-generation/emlab-generation/src/main/resources/data/iprod.csv";
         *
         * // Setting up demand in second column try { // Read demand file
         *
         * bufferedReader = new BufferedReader(new FileReader(filename)); String
         * line; int iprod = 0; // skipping header bufferedReader.readLine();
         *
         * while ((line = bufferedReader.readLine()) != null) {
         *
         * String[] iprodValues = line.split(","); double iprodValue =
         * Double.parseDouble(iprodValues[1]); marketClearingMatrix.set(iprod,
         * 2, iprodValue); iprod++; logger.warn("Iprod is:  " + iprodValue);
         *
         * } bufferedReader.close(); } catch (Exception e) { logger.error(
         * "Couldn't read CSV file: " + filename); e.printStackTrace(); }
         *
         * // Seting up resudual load -- demand - iprod
         *
         * for (int row = 0; row < 8760; row++) { double demand =
         * marketClearingMatrix.get(row, 1); double iprod =
         * marketClearingMatrix.get(row, 2); double rload = demand - iprod;
         * logger.warn("Residual Load is:  " + rload);
         *
         * marketClearingMatrix.set(row, 3, rload);
         *
         * }
         *
         * // Output to test
         *
         * for (int row = 0; row < 8760; row++) { for (int col = 0; col <= 4;
         * col++) { logger.warn("The matrix is: " +
         * marketClearingMatrix.get(row, col)); //
         * System.out.print(viewSorted(marketClearingMatrix[col][row]); } //
         * System.out.println("--------------------------------------"); }
         *
         * // DoubleMatrix2D supplyCurveMatrix = new //
         * DenseDoubleMatrix2D(powerPlantList.size(), 4);
         */

    }

}