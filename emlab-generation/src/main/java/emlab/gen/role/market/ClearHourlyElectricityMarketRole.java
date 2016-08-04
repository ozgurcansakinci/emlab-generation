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
import emlab.gen.domain.technology.EnergyStorageTechnology;
import emlab.gen.domain.technology.Interconnector;
import emlab.gen.repository.Reps;
import emlab.gen.util.Utils;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

/**
 * Market clearing role which utilizes optimization
 * 
 * @author asmkhan & ozgur
 *
 */
@RoleComponent
public class ClearHourlyElectricityMarketRole extends AbstractClearElectricitySpotMarketRole<DecarbonizationModel>
        implements Role<DecarbonizationModel> {

    @Autowired
    private Reps reps;

    @Autowired
    Neo4jTemplate template;

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
            double numberofMarkets = reps.marketRepository.countAllElectricitySpotMarkets();
            int numberOfElectricitySpotMarkets = (int) numberofMarkets;
            int numberOfPowerPlants = reps.powerPlantRepository.findNumberOfOperationalPowerPlants(getCurrentTick());

            // The i th row contains the array of variable generation capacity
            // of plant i
            IloNumVar[][] generationCapacityofPlantsMatrix = new IloNumVar[numberOfPowerPlants][timeSteps];

            // Only works when there is one interconnector
            // TODO:think about the multi node implementation
            double maxMarketCrossBorderFlowAandB = reps.template.findAll(Interconnector.class).iterator().next()
                    .getCapacity(getCurrentTick());
            double minMarketCrossBorderFlowAandB = -maxMarketCrossBorderFlowAandB;
            Map<ElectricitySpotMarket, List<PpdpAnnual>> ESMtoPPDPList = new HashMap<ElectricitySpotMarket, List<PpdpAnnual>>();
            for (ElectricitySpotMarket market : reps.marketRepository.findAllElectricitySpotMarkets()) {
                List<PpdpAnnual> ppdpAnnualListPerESMarket = Utils.asList(reps.ppdpAnnualRepository
                        .findAllSubmittedPpdpAnnualForGivenMarketAndTime(market, getCurrentTick()));
                ESMtoPPDPList.put(market, ppdpAnnualListPerESMarket);
            }

            // Initializing the CPLEX matrices, they will be created in the
            // upcoming for loops
            IloNumVar[] crossBorderGenerationAandB = new IloNumVar[timeSteps];
            IloLinearNumExpr[][] generationEquationsForAllMarkets = new IloLinearNumExpr[numberOfElectricitySpotMarkets][timeSteps];
            IloLinearNumExpr[][] demandEquationsForAllMarkets = new IloLinearNumExpr[numberOfElectricitySpotMarkets][timeSteps];
            IloLinearNumExpr[] carbonEmissionsEquationsForAllMarkets = new IloLinearNumExpr[timeSteps];
            IloNumVar[][] inelasticDemandForAllMarkets = new IloNumVar[numberOfElectricitySpotMarkets][timeSteps];

            // Demand response data structure //TODO:365 is the number of days
            // in the year. It should be taken from somewhere within the model.
            IloNumVar[][] elasticDemandForAllMarkets = new IloNumVar[numberOfElectricitySpotMarkets][timeSteps];
            IloLinearNumExpr[][] accumulativeElasticDemandPerDayForAllMarkets = new IloLinearNumExpr[numberOfElectricitySpotMarkets][365];
            IloNumVar[][] valueOfLostLoadInMWH = new IloNumVar[numberOfElectricitySpotMarkets][timeSteps];

            // Storage data structures
            IloNumVar[][] storageChargingInMW = new IloNumVar[numberOfElectricitySpotMarkets][timeSteps];
            IloNumVar[][] storageDischargingInMW = new IloNumVar[numberOfElectricitySpotMarkets][timeSteps];
            IloNumVar[][] stateOfChargeInMWh = new IloNumVar[numberOfElectricitySpotMarkets][timeSteps];
            IloLinearNumExpr[][] storageContentExpressionsForAllMarkets = new IloLinearNumExpr[numberOfElectricitySpotMarkets][timeSteps];
            IloLinearNumExpr objective = cplex.linearNumExpr();

            // Indexes for running the for loops
            int marketIndex = 0;
            int plantIndex = 0;

            // For loops that take lots of time to run, couldn't find a way to
            // avoid this.
            for (ElectricitySpotMarket market : reps.marketRepository.findAllElectricitySpotMarkets()) {

                EnergyStorageTechnology storageTechnologyInMarket;
                if (market.isStorageImplemented()) {
                    storageTechnologyInMarket = reps.energyStorageTechnologyRepository
                            .findEnergyStorageTechnologyByMarket(market);

                } else {
                    storageTechnologyInMarket = null;
                }

                for (int i = 0; i < timeSteps; i++) {
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
                        // if (i != 0) {
                        //// storageContentExpressionsForAllMarkets[marketIndex][i].addTerm(1,
                        //// stateOfChargeInMWh[marketIndex][i - 1]);
                        //// storageContentExpressionsForAllMarkets[marketIndex][i].addTerm(storageTechnologyInMarket
                        //// .getEfficiencyInFlowTimeSeries().getValue(getCurrentTick()),
                        //// storageChargingInMW[marketIndex][i]);
                        //// storageContentExpressionsForAllMarkets[marketIndex][i]
                        //// .addTerm(
                        //// -storageTechnologyInMarket.getEfficiencyOutFlowTimeSeries()
                        //// .getValue(getCurrentTick()),
                        //// storageDischargingInMW[marketIndex][i]);
                        // }
                    }

                    generationEquationsForAllMarkets[marketIndex][i] = cplex.linearNumExpr();
                    crossBorderGenerationAandB[i] = cplex.numVar(minMarketCrossBorderFlowAandB,
                            maxMarketCrossBorderFlowAandB);
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
                    if (market.isDailyDemandResponseImplemented()) {
                        elasticDemandForAllMarkets[marketIndex][i] = cplex.numVar(0, Double.MAX_VALUE);
                        demandEquationsForAllMarkets[marketIndex][i].addTerm(1,
                                elasticDemandForAllMarkets[marketIndex][i]);
                        if ((i + 1) % 24 == 0 && i != 0) {
                            accumulativeElasticDemandPerDayForAllMarkets[marketIndex][((i + 1) / 24) - 1] = cplex
                                    .linearNumExpr();
                            for (int j = 0; j < 24; j++)
                                accumulativeElasticDemandPerDayForAllMarkets[marketIndex][((i + 1) / 24) - 1].addTerm(1,
                                        elasticDemandForAllMarkets[marketIndex][i - 23 + j]);
                        }
                    }
                    if (market.isStorageImplemented()) {
                        generationEquationsForAllMarkets[marketIndex][i].addTerm(1,
                                storageDischargingInMW[marketIndex][i]);
                        demandEquationsForAllMarkets[marketIndex][i].addTerm(1, storageChargingInMW[marketIndex][i]);
                    }
                    objective.addTerm(market.getValueOfLostLoad(), valueOfLostLoadInMWH[marketIndex][i]);
                    if (marketIndex == 0)
                        carbonEmissionsEquationsForAllMarkets[i] = cplex.linearNumExpr();
                }

                for (PpdpAnnual ppdp : ESMtoPPDPList.get(market)) {
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
            IloRange[][] constraints = new IloRange[numberOfElectricitySpotMarkets][timeSteps];
            // Creating constraints
            switch (numberOfElectricitySpotMarkets) {
            case 1:
                for (int i = 0; i < timeSteps; i++) {
                    constraints[0][i] = (IloRange) cplex.addEq(generationEquationsForAllMarkets[0][i],
                            cplex.sum(demandEquationsForAllMarkets[0][i], crossBorderGenerationAandB[i]));
                }
                break;
            case 2:
                for (int j = 0; j < timeSteps; j++) {
                    constraints[0][j] = (IloRange) cplex.addEq(generationEquationsForAllMarkets[0][j],
                            cplex.sum(demandEquationsForAllMarkets[0][j], crossBorderGenerationAandB[j]));
                    constraints[1][j] = (IloRange) cplex.addEq(generationEquationsForAllMarkets[1][j],
                            cplex.diff(demandEquationsForAllMarkets[1][j], crossBorderGenerationAandB[j]));
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
                        for (int i = 0; i < 365; i++) {
                            cplex.addEq(accumulativeElasticDemandPerDayForAllMarkets[marketIndex][i],
                                    market.getYearlySegmentLoad().getDailyElasticCurrentDemandForYearlySegment()
                                            .getDailyArray(0)[i]);
                        }
                    }
                    marketIndex++;
                }
                break;
            case 3:
                // TODO:Work this out!
                for (int i = 0; i < timeSteps; i++) {
                    constraints[0][i] = (IloRange) cplex.addEq(generationEquationsForAllMarkets[0][i],
                            cplex.sum(demandEquationsForAllMarkets[0][i], crossBorderGenerationAandB[i]));
                    constraints[1][i] = (IloRange) cplex.addEq(generationEquationsForAllMarkets[1][i],
                            cplex.diff(demandEquationsForAllMarkets[1][i], crossBorderGenerationAandB[i]));
                }
                break;
            }

            IloRange carbonConstraint = (IloRange) cplex.addLe(cplex.sum(carbonEmissionsEquationsForAllMarkets),
                    co2Cap);
            System.out.println("Number of power plants in the model: " + generationCapacityofPlantsMatrix.length);
            System.out.println("CO2 Cap is " + co2Cap);
            // Disable CPLEX outputs, so that it runs faster
            cplex.setParam(IloCplex.IntParam.Simplex.Display, 0);

            // List<PpdpAnnual> ppdpAnnualList1 = Utils
            // .asList(reps.ppdpAnnualRepository.findAllSubmittedPpdpAnnualForGivenTime(getCurrentTick()));
            // System.out.println(ppdpAnnualList1.size());

            if (cplex.solve()) {
                // Indexes to be used when saving outcomes
                int ppdpIndex = 0;
                int ind = 0;
                System.out.println("---------------------Market Cleared!----------------------------");
                System.out.println("Objective = " + cplex.getObjValue());
                System.out.println("Objective = " + cplex.getStatus());
                System.out.println("----------------------------------------------------------------");
                System.out.println("Carbon constraint = " + cplex.getDual(carbonConstraint));
                for (ElectricitySpotMarket market : reps.marketRepository.findAllElectricitySpotMarkets()) {
                    // Save the optimization outcomes in MarketInformation
                    YearlySegment ys = reps.marketRepository.findYearlySegmentForElectricitySpotMarketForTime(market);
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
                        price[i] = Math.abs(cplex.getDual(constraints[ind][i]));
                        if (market.isStorageImplemented()) {
                            charging[i] = cplex.getValue(storageChargingInMW[ind][i]);
                            discharging[i] = cplex.getValue(storageDischargingInMW[ind][i]);
                            soc[i] = cplex.getValue(stateOfChargeInMWh[ind][i]);
                        }
                        if (market.isDailyDemandResponseImplemented())
                            elasticDem[i] = cplex.getValue(elasticDemandForAllMarkets[ind][i]);
                    }

                    if (market.isStorageImplemented()) {
                        if (market.isDailyDemandResponseImplemented()) {
                            storeInDatabase(price, gen, dem, elasticDem, charging, discharging, soc,
                                    cplex.getValues(valueOfLostLoadInMWH[ind]), market, ys, getCurrentTick(),
                                    Math.abs(cplex.getDual(carbonConstraint)));
                        } else {
                            storeInDatabase(price, gen, dem, null, charging, discharging, soc,
                                    cplex.getValues(valueOfLostLoadInMWH[ind]), market, ys, getCurrentTick(),
                                    Math.abs(cplex.getDual(carbonConstraint)));
                        }
                    } else {
                        if (market.isDailyDemandResponseImplemented()) {
                            storeInDatabase(price, gen, dem, elasticDem, null, null, null,
                                    cplex.getValues(valueOfLostLoadInMWH[ind]), market, ys, getCurrentTick(),
                                    Math.abs(cplex.getDual(carbonConstraint)));
                        } else {
                            storeInDatabase(price, gen, dem, null, null, null, null,
                                    cplex.getValues(valueOfLostLoadInMWH[ind]), market, ys, getCurrentTick(),
                                    Math.abs(cplex.getDual(carbonConstraint)));
                        }
                    }
                    // if (market.isStorageImplemented() &&
                    // market.isDailyDemandResponseImplemented())
                    // storeInDatabase(price, gen, dem, elasticDem, charging,
                    // discharging, soc,
                    // cplex.getValues(valueOfLostLoadInMWH[ind]), market, ys,
                    // getCurrentTick(),
                    // Math.abs(cplex.getDual(carbonConstraint)));
                    // else
                    // storeInDatabase(price, gen, dem,
                    // cplex.getValues(valueOfLostLoadInMWH[ind]), market, ys,
                    // getCurrentTick(),
                    // Math.abs(cplex.getDual(carbonConstraint)));
                    for (PpdpAnnual ppdp : ESMtoPPDPList.get(market)) {
                        changeAcceptedAmountPpdpAnnual(ppdp,
                                cplex.getValues(generationCapacityofPlantsMatrix[ppdpIndex]));
                        ppdpIndex++;
                        acceptAnnualBids(ppdp);
                    }
                    ind++;
                }
                // Write the outcomes to a file, just for comparison
                try {
                    FileWriter FW = new FileWriter("/Users/apple/Desktop/emlabGen/Generation.csv");
                    for (int i = 0; i < timeSteps; ++i) {
                        int ind1 = 0;
                        for (ElectricitySpotMarket market : reps.marketRepository.findAllElectricitySpotMarkets()) {
                            FW.write(market.toString() + " " + ",");
                            FW.write(cplex.getDual(constraints[ind1][i]) + " " + ",");
                            for (PpdpAnnual ppdp : ESMtoPPDPList.get(market)) {
                                // FW.write(ppdp.getPowerPlant().toString() + "
                                // " + ",");
                                FW.write(ppdp.getAcceptedHourlyAmount()[i] + " " + ",");
                            }
                            ind1++;
                        }

                        FW.write("\n");
                    }
                    FW.flush();
                    FW.close();
                    FileWriter FW1 = new FileWriter("/Users/apple/Desktop/emlabGen/Emission.csv");
                    FW1.write("Carbon Emissions" + " " + "," + "Generation in A" + "," + "Generation in B" + " " + ","
                            + "Demand in A" + " " + "," + "Demand in B" + " " + "," + "Cross Border Generation" + " "
                            + "," + "Amount of LL in A" + " " + "," + "Amount of LL in B" + " " + "," + "\n");
                    for (int i = 0; i < timeSteps; ++i) {
                        FW1.write(cplex.getValue(carbonEmissionsEquationsForAllMarkets[i]) + " " + ","
                                + cplex.getValue(generationEquationsForAllMarkets[0][i]) + " " + ","
                                + cplex.getValue(generationEquationsForAllMarkets[1][i]) + " " + ","
                                + cplex.getValue(demandEquationsForAllMarkets[0][i]) + " " + ","
                                + cplex.getValue(demandEquationsForAllMarkets[1][i]) + " " + ","
                                + cplex.getValue(crossBorderGenerationAandB[i]) + " " + ","
                                + cplex.getValue(valueOfLostLoadInMWH[0][i]) + " " + ","
                                + cplex.getValue(valueOfLostLoadInMWH[1][i]) + " " + ","
                                + cplex.getValue(stateOfChargeInMWh[0][i]) + " " + ","
                                + cplex.getValue(storageChargingInMW[0][i]) + " " + ","
                                + cplex.getValue(storageDischargingInMW[0][i]) + " " + ","
                                + cplex.getValue(elasticDemandForAllMarkets[0][i]) + " " + ","
                                + cplex.getValue(stateOfChargeInMWh[1][i]) + " " + ","
                                + cplex.getValue(storageChargingInMW[1][i]) + " " + ","
                                + cplex.getValue(storageDischargingInMW[1][i]) + " " + ","
                                + cplex.getValue(elasticDemandForAllMarkets[1][i]) + "\n");
                        // FW1.write(cplex.getValue(valueOfLostLoadInMWH[0][i])
                        // + " " + ","
                        // + cplex.getValue(valueOfLostLoadInMWH[1][i]) + "\n");

                    }
                    FW1.flush();
                    FW1.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                System.out.println("Something went wrong");
            }
            cplex.end();
            System.out.println("------------------------------------------------------");

        } catch (

        Exception e) {
            e.printStackTrace();
        }

        System.out.println("Optimization model completed!");

        // System.exit(0);

    }

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

    @Transactional
    public void storeInDatabase(double[] price, double[] generation, double[] demand, double[] charging,
            double[] discharging, double[] soc, double[] valueOfLostLoad, ElectricitySpotMarket market,
            YearlySegment ys, long time, double cO2Price) {
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
        info.setStorageChargingInMW(charging);
        info.setStorageDischargingInMW(discharging);
        info.setStateOfChargeInMWh(soc);
        info.persist();
    }

    // TODO:Add more functions like this, for all other cases
    @Transactional
    public void storeInDatabase(double[] price, double[] generation, double[] demand, double[] elasticDem,
            double[] charging, double[] discharging, double[] soc, double[] valueOfLostLoad,
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

}