package emlab.gen.role;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import emlab.gen.domain.agent.DecarbonizationModel;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.Government;
import emlab.gen.domain.agent.NationalGovernment;
import emlab.gen.domain.contract.Loan;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.CO2Auction;
import emlab.gen.domain.market.ClearingPoint;
import emlab.gen.domain.market.CommodityMarket;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.PpdpAnnual;
import emlab.gen.domain.market.electricity.YearlySegment;
import emlab.gen.domain.market.electricity.YearlySegmentClearingPointMarketInformation;
import emlab.gen.domain.market.electricity.YearlySegmentLoad;
import emlab.gen.domain.technology.EnergyStorageTechnology;
import emlab.gen.domain.technology.Interconnector;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.domain.technology.Substance;
import emlab.gen.repository.BidRepository;
import emlab.gen.repository.MarketRepository;
import emlab.gen.repository.PowerPlantDispatchPlanRepository;
import emlab.gen.repository.Reps;
import emlab.gen.repository.SegmentLoadRepository;
import emlab.gen.repository.ZoneRepository;
import emlab.gen.role.market.ClearHourlyElectricityMarketRole;
import emlab.gen.role.market.ClearIterativeCO2AndElectricitySpotMarketTwoCountryRole;
import emlab.gen.role.market.SubmitOffersToElectricitySpotMarketAnnualRole;
import emlab.gen.role.market.SubmitOffersToElectricitySpotMarketRole;
import emlab.gen.role.operating.DetermineFuelMixRole;
import emlab.gen.trend.HourlyCSVTimeSeries;
import emlab.gen.trend.LinearTrend;
import emlab.gen.trend.TimeSeriesCSVReader;
import emlab.gen.trend.TriangularTrend;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "/emlab-gen-test-context.xml" })
@Transactional
public class ElectricityMarketSubmittingAndClearingTestWithCPLEXWithoutStorage {

    Logger logger = Logger.getLogger(ElectricityMarketSubmittingAndClearingTestWithCPLEXWithoutStorage.class);

    @Autowired
    Reps reps;

    @Autowired
    SegmentLoadRepository segmentLoadRepository;

    @Autowired
    MarketRepository marketRepository;

    @Autowired
    BidRepository bidRepository;

    @Autowired
    PowerPlantDispatchPlanRepository plantDispatchPlanRepository;

    @Autowired
    ZoneRepository zoneRepository;

    @Autowired
    ClearIterativeCO2AndElectricitySpotMarketTwoCountryRole clearIterativeCO2AndElectricitySpotMarketTwoCountryRole;

    @Autowired
    SubmitOffersToElectricitySpotMarketRole submitOffersToElectricitySpotMarketRole;

    @Autowired
    SubmitOffersToElectricitySpotMarketAnnualRole submitOffersToElectricitySpotMarketAnnualRole;

    @Autowired
    ClearHourlyElectricityMarketRole clearHourlyElectricityMarketRole;

    @Autowired
    DetermineFuelMixRole determineFuelMixRole;

    // 4 power plants in two markets
    @Before
    @Transactional
    public void setUp() throws Exception {
        DecarbonizationModel model = new DecarbonizationModel();
        model.setCo2TradingImplemented(false);
        model.setRealRenewableDataImplemented(false);
        model.setIterationSpeedFactor(3);
        model.setIterationSpeedCriterion(0.005);
        model.setCapDeviationCriterion(0.03);
        model.persist();

        Government gov = new Government().persist();
        LinearTrend co2TaxTrend = new LinearTrend().persist();
        co2TaxTrend.setStart(0);
        co2TaxTrend.setIncrement(0);
        gov.setCo2TaxTrend(co2TaxTrend);
        LinearTrend co2CapTrend = new LinearTrend().persist();
        co2CapTrend.setStart(62000000);
        co2CapTrend.setIncrement(0);
        gov.setCo2CapTrend(co2CapTrend);

        CO2Auction co2Auction = new CO2Auction().persist();

        Zone zone1 = new Zone();
        Zone zone2 = new Zone();
        zone1.setName("Zone 1");

        zone2.setName("Zone2");

        zone1.persist();
        zone2.persist();

        NationalGovernment natGov1 = new NationalGovernment().persist();
        NationalGovernment natGov2 = new NationalGovernment().persist();

        natGov1.setGovernedZone(zone1);
        natGov2.setGovernedZone(zone2);

        LinearTrend minCo2TaxTrend1 = new LinearTrend().persist();
        minCo2TaxTrend1.setStart(0);
        minCo2TaxTrend1.setIncrement(0);
        natGov1.setMinNationalCo2PriceTrend(minCo2TaxTrend1);

        LinearTrend minCo2TaxTrend2 = new LinearTrend().persist();
        minCo2TaxTrend2.setStart(0);
        minCo2TaxTrend2.setIncrement(0);
        natGov2.setMinNationalCo2PriceTrend(minCo2TaxTrend2);

        PowerGridNode node1 = new PowerGridNode();
        PowerGridNode node2 = new PowerGridNode();
        node1.setCapacityMultiplicationFactor(1);
        node2.setCapacityMultiplicationFactor(1);
        node1.setZone(zone1);
        node2.setZone(zone2);
        node1.setName("Node1");
        node2.setName("Node2");
        node1.persist();
        node2.persist();

        HashSet<PowerGridNode> intNodes = new HashSet<PowerGridNode>();
        intNodes.add(node1);
        intNodes.add(node2);

        YearlySegment YS = new YearlySegment();
        YS.setYearlySegmentLengthInHours(10);
        YS.persist();

        // TimeSeriesImpl intCapacity = new TimeSeriesImpl().persist();
        TimeSeriesCSVReader interconnectorCapacity = new TimeSeriesCSVReader().persist();
        interconnectorCapacity.setFilename("/data/ICCapacityNL.csv");
        interconnectorCapacity.setDelimiter(",");
        interconnectorCapacity.setVariableName("IC");

        TimeSeriesCSVReader storageInflowEfficiency = new TimeSeriesCSVReader().persist();
        storageInflowEfficiency.setFilename("/data/IFEff.csv");
        storageInflowEfficiency.setDelimiter(",");
        storageInflowEfficiency.setVariableName("IFEff-1");

        TimeSeriesCSVReader storageOutflowEfficiency = new TimeSeriesCSVReader().persist();
        storageOutflowEfficiency.setFilename("/data/OFEff.csv");
        storageOutflowEfficiency.setDelimiter(",");
        storageOutflowEfficiency.setVariableName("OFEff-1");

        Interconnector interconnector = new Interconnector().persist();
        interconnector.setName("TestConnector");
        interconnector.setConnections(intNodes);
        interconnector.setInterconnectorCapacity(interconnectorCapacity);
        // interconnector.setCapacity(time, capacity);
        interconnector.setYearlySegment(YS);

        HourlyCSVTimeSeries hourlyDemand = new HourlyCSVTimeSeries();
        hourlyDemand.setLengthInHours(10);
        hourlyDemand.setFilename("/data/NodeDemandTimeSeriesData.csv");
        hourlyDemand.setVariableName("nl");
        hourlyDemand.setDelimiter(",");
        hourlyDemand.setTimeSeriesAreInDifferentColumns(true);
        hourlyDemand.persist();

        HourlyCSVTimeSeries hourlyDemandDE = new HourlyCSVTimeSeries();
        hourlyDemandDE.setLengthInHours(10);
        hourlyDemandDE.setFilename("/data/NodeDemandTimeSeriesData.csv");
        hourlyDemandDE.setVariableName("de");
        hourlyDemandDE.setDelimiter(",");
        hourlyDemandDE.setTimeSeriesAreInDifferentColumns(true);
        hourlyDemandDE.persist();

        HourlyCSVTimeSeries dailyDemand = new HourlyCSVTimeSeries();
        dailyDemand.setLengthInHours(5);
        dailyDemand.setFilename("/data/NodeDemandDailySeriesData.csv");
        dailyDemand.setVariableName("nl");
        dailyDemand.setDelimiter(",");
        dailyDemand.setTimeSeriesAreInDifferentColumns(true);
        dailyDemand.setElasticDemandShift(2);
        dailyDemand.persist();

        HourlyCSVTimeSeries dailyDemandDE = new HourlyCSVTimeSeries();
        dailyDemandDE.setLengthInHours(5);
        dailyDemandDE.setFilename("/data/NodeDemandDailySeriesData.csv");
        dailyDemandDE.setVariableName("de");
        dailyDemandDE.setDelimiter(",");
        dailyDemandDE.setTimeSeriesAreInDifferentColumns(true);
        dailyDemandDE.setElasticDemandShift(2);
        dailyDemandDE.persist();

        TriangularTrend demandGrowthTrend = new TriangularTrend();
        demandGrowthTrend.setMax(1);
        demandGrowthTrend.setMin(1);
        demandGrowthTrend.setStart(1);
        demandGrowthTrend.setTop(1);

        demandGrowthTrend.persist();
        YearlySegmentLoad yearlySegmentLoad1 = new YearlySegmentLoad().persist();
        YearlySegmentLoad yearlySegmentLoad2 = new YearlySegmentLoad().persist();

        ElectricitySpotMarket market1 = new ElectricitySpotMarket();
        market1.setName("Market1");
        market1.setZone(zone1);
        market1.setYearlySegment(YS);
        market1.setYearlySegmentLoad(yearlySegmentLoad1);
        market1.setDemandGrowthTrend(demandGrowthTrend);
        market1.setValueOfLostLoad(2000);
        market1.setStorageImplemented(false);
        market1.setDailyDemandResponseImplemented(true);
        market1.persist();

        ElectricitySpotMarket market2 = new ElectricitySpotMarket();
        market2.setZone(zone2);
        market2.setYearlySegment(YS);
        market2.setYearlySegmentLoad(yearlySegmentLoad2);
        market2.setName("Market2");
        market2.setDemandGrowthTrend(demandGrowthTrend);
        market2.setValueOfLostLoad(2000);
        market2.setStorageImplemented(false);
        market2.setDailyDemandResponseImplemented(true);

        market2.persist();

        yearlySegmentLoad1.setElectricitySpotMarket(market1);
        yearlySegmentLoad1.setHourlyInElasticCurrentDemandForYearlySegment(hourlyDemand);
        yearlySegmentLoad1.setDailyElasticCurrentDemandForYearlySegment(dailyDemand);
        yearlySegmentLoad1.setHourlyInElasticBaseDemandForYearlySegment(hourlyDemand);
        yearlySegmentLoad1.setDailyElasticBaseDemandForYearlySegment(dailyDemand);
        yearlySegmentLoad1.setYearlySegment(YS);

        yearlySegmentLoad2.setElectricitySpotMarket(market2);
        yearlySegmentLoad2.setHourlyInElasticCurrentDemandForYearlySegment(hourlyDemandDE);
        yearlySegmentLoad2.setDailyElasticCurrentDemandForYearlySegment(dailyDemandDE);
        yearlySegmentLoad2.setHourlyInElasticBaseDemandForYearlySegment(hourlyDemandDE);
        yearlySegmentLoad2.setDailyElasticBaseDemandForYearlySegment(dailyDemandDE);
        yearlySegmentLoad2.setYearlySegment(YS);

        Substance coal = new Substance().persist();
        coal.setName("Coal");
        coal.setEnergyDensity(1000);
        Substance gas = new Substance().persist();
        gas.setName("Gas");
        gas.setEnergyDensity(1000);

        CommodityMarket coalMarket = new CommodityMarket().persist();
        CommodityMarket gasMarket = new CommodityMarket().persist();

        coalMarket.setSubstance(coal);
        gasMarket.setSubstance(gas);

        LinearTrend coalPrice = new LinearTrend().persist();
        coalPrice.setStart(3);
        coalPrice.setIncrement(1);

        LinearTrend gasPrice = new LinearTrend().persist();
        gasPrice.setStart(6);
        coalPrice.setIncrement(2);

        HashSet<Substance> fuelMixCoal = new HashSet<Substance>();
        fuelMixCoal.add(coal);

        HashSet<Substance> fuelMixGas = new HashSet<Substance>();
        fuelMixGas.add(gas);

        PowerGeneratingTechnology coalTech = new PowerGeneratingTechnology();
        coalTech.setFuels(fuelMixCoal);
        coalTech.setPeakSegmentDependentAvailability(1);
        coalTech.setBaseSegmentDependentAvailability(1);

        PowerGeneratingTechnology gasTech = new PowerGeneratingTechnology();
        gasTech.setFuels(fuelMixGas);
        gasTech.setPeakSegmentDependentAvailability(1);
        gasTech.setBaseSegmentDependentAvailability(1);

        coalTech.persist();
        gasTech.persist();

        EnergyProducer market1Prod1 = new EnergyProducer();
        market1Prod1.setName("market1Prod1");
        market1Prod1.setCash(0);
        market1Prod1.setPriceMarkUp(1);
        market1Prod1.setInvestorMarket(market1);

        EnergyProducer market1Prod2 = new EnergyProducer();
        market1Prod2.setCash(0);
        market1Prod2.setPriceMarkUp(1);
        market1Prod2.setName("market1Prod2");
        market1Prod2.setInvestorMarket(market1);

        EnergyProducer market2Prod1 = new EnergyProducer();
        market2Prod1.setCash(0);
        market2Prod1.setPriceMarkUp(1);
        market2Prod1.setName("market2Prod1");
        market2Prod1.setInvestorMarket(market2);

        EnergyProducer market2Prod2 = new EnergyProducer();
        market2Prod2.setCash(0);
        market2Prod2.setPriceMarkUp(1);
        market2Prod2.setName("market2Prod2");
        market2Prod2.setInvestorMarket(market2);

        market1Prod1.persist();
        market1Prod2.persist();
        market2Prod1.persist();
        market2Prod2.persist();

        EnergyStorageTechnology storageNL = new EnergyStorageTechnology();
        storageNL.setCurrentMaxStorageCapacity(2000);
        storageNL.setCurrentMaxStorageChargingRate(500);
        storageNL.setCurrentMaxStorageDischargingRate(500);
        storageNL.setInitialStateOfChargeInStorage(0);
        storageNL.setOwnerStorageUnit(market1Prod1);
        storageNL.setEfficiencyInFlowTimeSeries(storageInflowEfficiency);
        storageNL.setEfficiencyOutFlowTimeSeries(storageOutflowEfficiency);

        EnergyStorageTechnology storageDE = new EnergyStorageTechnology();
        storageDE.setCurrentMaxStorageCapacity(2000);
        storageDE.setCurrentMaxStorageChargingRate(500);
        storageDE.setCurrentMaxStorageDischargingRate(500);
        storageDE.setInitialStateOfChargeInStorage(0);
        storageDE.setOwnerStorageUnit(market2Prod1);
        storageDE.setEfficiencyInFlowTimeSeries(storageInflowEfficiency);
        storageDE.setEfficiencyOutFlowTimeSeries(storageOutflowEfficiency);

        storageNL.persist();
        storageDE.persist();

        Loan l1 = new Loan();
        l1.setAmountPerPayment(6000);
        l1.setNumberOfPaymentsDone(10);
        l1.setTotalNumberOfPayments(15);

        Loan l2 = new Loan();
        l2.setAmountPerPayment(5000);
        l2.setNumberOfPaymentsDone(29);
        l2.setTotalNumberOfPayments(19);

        Loan l3 = new Loan();
        l3.setAmountPerPayment(4000);
        l3.setNumberOfPaymentsDone(8);
        l3.setTotalNumberOfPayments(13);

        Loan l4 = new Loan();
        l4.setAmountPerPayment(3000);
        l4.setNumberOfPaymentsDone(7);
        l4.setTotalNumberOfPayments(12);

        Loan l5 = new Loan();
        l5.setAmountPerPayment(2000);
        l5.setNumberOfPaymentsDone(6);
        l5.setTotalNumberOfPayments(11);

        Loan l6 = new Loan();
        l6.setAmountPerPayment(1000);
        l6.setNumberOfPaymentsDone(5);
        l6.setTotalNumberOfPayments(10);

        l1.persist();
        l2.persist();
        l3.persist();
        l4.persist();
        l5.persist();
        l6.persist();

        // At 3 Eur/GJ has a mc of 24 Eur/Mwh
        PowerPlant pp1 = new PowerPlant();
        pp1.setTechnology(coalTech);
        pp1.setOwner(market1Prod1);
        pp1.setActualFixedOperatingCost(99000);
        pp1.setLoan(l1);
        pp1.setActualNominalCapacity(5000);
        pp1.setActualEfficiency(0.45);
        pp1.setLocation(node1);
        pp1.setActualPermittime(0);
        pp1.setConstructionStartTime(-2);
        pp1.setActualLeadtime(0);
        pp1.setDismantleTime(10);
        pp1.setExpectedEndOfLife(10);
        pp1.setName("CoalInM1");

        // At 3 Eur/GJ has a mc of 27 Eur/MWh
        PowerPlant pp2 = new PowerPlant();
        pp2.setTechnology(coalTech);
        pp2.setOwner(market2Prod1);
        pp2.setActualFixedOperatingCost(99000);
        pp2.setLoan(l2);
        pp2.setActualNominalCapacity(400);
        pp2.setActualEfficiency(0.40);
        pp2.setLocation(node2);
        pp2.setActualPermittime(0);
        pp2.setConstructionStartTime(-2);
        pp2.setActualLeadtime(0);
        pp2.setDismantleTime(10);
        pp2.setExpectedEndOfLife(10);
        pp2.setName("CoalInM2");

        // At 6 Eur/GJ has a mc of 36
        PowerPlant pp3 = new PowerPlant();
        pp3.setTechnology(gasTech);
        pp3.setOwner(market1Prod1);
        pp3.setActualFixedOperatingCost(99000);
        pp3.setLoan(l3);
        pp3.setActualNominalCapacity(3000);
        pp3.setActualEfficiency(0.60);
        pp3.setLocation(node1);
        pp3.setActualPermittime(0);
        pp3.setConstructionStartTime(-2);
        pp3.setActualLeadtime(0);
        pp3.setDismantleTime(1000);
        pp3.setExpectedEndOfLife(2);
        pp3.setName("GasInM1");

        // At 6 Eur/GJ has a mc of 40 Eur/MWh
        PowerPlant pp4 = new PowerPlant();
        pp4.setTechnology(gasTech);
        pp4.setOwner(market2Prod2);
        pp4.setActualFixedOperatingCost(99000);
        pp4.setLoan(l3);
        pp4.setActualNominalCapacity(200);
        pp4.setActualEfficiency(0.54);
        pp4.setLocation(node2);
        pp4.setActualPermittime(0);
        pp4.setConstructionStartTime(-2);
        pp4.setActualLeadtime(0);
        pp4.setDismantleTime(10);
        pp4.setExpectedEndOfLife(10);
        pp4.setName("GasInM2");

        pp1.persist();
        pp2.persist();
        pp3.persist();
        pp4.persist();

        ClearingPoint coalClearingPoint = new ClearingPoint().persist();
        coalClearingPoint.setAbstractMarket(coalMarket);
        coalClearingPoint.setTime(0);
        coalClearingPoint.setPrice(3);
        coalClearingPoint.setVolume(1000);
        coalClearingPoint.setForecast(false);

        ClearingPoint gasClearingPoint = new ClearingPoint().persist();
        gasClearingPoint.setAbstractMarket(gasMarket);
        gasClearingPoint.setTime(0);
        gasClearingPoint.setPrice(6);
        gasClearingPoint.setVolume(1000);
        gasClearingPoint.setForecast(false);

    }

    @Test
    public void electricityMarketTestForCurrentTick() {

        DecarbonizationModel model = reps.genericRepository.findFirst(DecarbonizationModel.class);

        for (EnergyProducer producer : reps.genericRepository.findAllAtRandom(EnergyProducer.class)) {
            determineFuelMixRole.act(producer);
            submitOffersToElectricitySpotMarketAnnualRole.act(producer);
            // submitOffersToElectricitySpotMarketRole.act(producer);
            // producer.act(determineFuelMixRole);
        }

        // submitOffersToElectricitySpotMarketRole.createOffersForElectricitySpotMarket(null,
        // getCurrentTick() + 3, true,
        // null);
        // submitOffersToElectricitySpotMarketRole.createOffersForElectricitySpotMarket(null,
        // getCurrentTick(), false,
        // null);

        // Iterable<PowerPlantDispatchPlan> ppdps =
        // reps.powerPlantDispatchPlanRepository.findAll();
        // for (PowerPlantDispatchPlan ppdp : ppdps) {
        // logger.warn(ppdp.toString() + " in " + ppdp.getBiddingMarket() +
        // " accepted: " + ppdp.getAcceptedAmount());
        // }

        // clearIterativeCO2AndElectricitySpotMarketTwoCountryRole
        // .clearIterativeCO2AndElectricitySpotMarketTwoCountryForTimestepAndFuelPrices(model,
        // false,
        // getCurrentTick(), null, null, 0);
        clearHourlyElectricityMarketRole.act(model);

        // ppdps = reps.powerPlantDispatchPlanRepository.findAll();
        // for (PowerPlantDispatchPlan ppdp : ppdps) {
        // logger.warn(ppdp.toString() + " in " + ppdp.getBiddingMarket() +
        // " accepted: " + ppdp.getAcceptedAmount());
        // }
        //
        // Iterable<SegmentClearingPoint> scps =
        // reps.segmentClearingPointRepository.findAll();
        // for (SegmentClearingPoint scp : scps) {
        // logger.warn(scp.toString());
        // }
        double totalDemand = 0;
        double totalGeneration = 0;
        for (YearlySegmentClearingPointMarketInformation info : reps.yearlySegmentClearingPointMarketInformationRepository
                .findAll()) {
            for (int i = 0; i < info.getMarketDemand().length; i++) {
                totalDemand = totalDemand + info.getMarketDemand()[i];
                totalGeneration = totalGeneration + info.getMarketSupply()[i];
            }
        }
        for (int i = 0; i < 10; i++) {
            double totalDemandPerTimeStep = 0;
            double totalGenerationPerTimeStep = 0;

            for (ElectricitySpotMarket esm : reps.marketRepository.findAllElectricitySpotMarkets()) {
                totalDemandPerTimeStep += reps.yearlySegmentClearingPointMarketInformationRepository
                        .findMarketInformationForMarketAndTime(0, esm).getMarketDemand()[i];
                for (PowerPlant plant : reps.powerPlantRepository.findOperationalPowerPlantinMarketatTime(esm, 0)) {
                    totalGenerationPerTimeStep += reps.ppdpAnnualRepository
                            .findPPDPAnnualforPlantForCurrentTick(plant, 0).getAcceptedHourlyAmount()[i];
                }
                totalGenerationPerTimeStep += reps.yearlySegmentClearingPointMarketInformationRepository
                        .findMarketInformationForMarketAndTime(0, esm).getValueOfLostLoad()[i];

            }
            assertEquals("Demand and generation per time step", totalDemandPerTimeStep, totalGenerationPerTimeStep,
                    0.001);
        }
        assertEquals("Demand and generation", totalDemand, totalGeneration, 0.001);
        // Check that
        for (PowerPlant plant : reps.powerPlantRepository.findAll()) {
            // for (Segment s : reps.segmentRepository.findAll()) {
            // PowerPlantDispatchPlan plan =
            // reps.powerPlantDispatchPlanRepository
            // .findOnePowerPlantDispatchPlanForPowerPlantForSegmentForTime(plant,
            // s, 0, false);
            PpdpAnnual plan = reps.ppdpAnnualRepository.findPPDPAnnualforPlantForCurrentTick(plant, (long) 0);
            if (plan.getPowerPlant().getName().equals("CoalInM1")) {
                // assertEquals("CoalInM1 right price", 24,
                // plan.getBidWithoutCO2(), 0.001);
                // assertEquals("CoalInM1 right amount", 500, plan.getAmount(),
                // 0.001);
                // assertEquals("CoalInM1 right price", 24, plan.getPrice(),
                // 0.001);
                // assertEquals("CoalInM1 right amount", 500,
                // plan.getYearlySupply(), 0.001);
                // switch (s.getSegmentID()) {
                // case 1:
                // assertEquals("CoalInM1 right accepted amount in S1", 500,
                // plan.getAcceptedAmount(), 0.001);
                // break;
                // case 2:
                // assertEquals("CoalInM1 right accepted amount in S2", 500,
                // plan.getAcceptedAmount(), 0.001);
                // break;
                // }

            } else if (plan.getPowerPlant().getName().equals("CoalInM2")) {
                // assertEquals("CoalInM2 right price", 27, plan.getPrice(),
                // 0.001);
                // assertEquals("CoalInM2 right amount", 400,
                // plan.getYearlySupply(), 0.001);
                // switch (s.getSegmentID()) {
                // case 1:
                // assertEquals("CoalInM2 right accepted amount in S1", 400,
                // plan.getAcceptedAmount(), 0.001);
                // break;
                // case 2:
                // assertEquals("CoalInM2 right accepted amount in S2", 399.99,
                // plan.getAcceptedAmount(), 0.001);
                // break;
                // }
            } else if (plan.getPowerPlant().getName().equals("GasInM1")) {
                // assertEquals("GasInM1 right price", 36, plan.getPrice(),
                // 0.001);
                // assertEquals("GasInM1 right amount", 300,
                // plan.getYearlySupply(), 0.001);
                // switch (s.getSegmentID()) {
                // case 1:
                // assertEquals("GasInM1 right accepted amount in S1", 290,
                // plan.getAcceptedAmount(), 0.001);
                // break;
                // case 2:
                // assertEquals("GasInM1 right accepted amount in S2", 0.01,
                // plan.getAcceptedAmount(), 0.001);
                // break;
                // }
            } else if (plan.getPowerPlant().getName().equals("GasInM2")) {
                // assertEquals("GasInM2 right price", 40, plan.getPrice(),
                // 0.001);
                // assertEquals("GasInM2 right amount", 200,
                // plan.getYearlySupply(), 0.001);
                // switch (s.getSegmentID()) {
                // case 1:
                // assertEquals("GasInM2 right accepted amount in S1", 200,
                // plan.getAcceptedAmount(), 0.001);
                // break;
                // case 2:
                // assertEquals("GasInM2 right accepted amount in S2", 0,
                // plan.getAcceptedAmount(), 0.001);
                // break;
                // }
            }
            // }

        }

    }

    private long getCurrentTick() {
        return 0;
    }

}

// }