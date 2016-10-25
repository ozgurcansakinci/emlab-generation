package emlab.gen.role;

import java.util.HashSet;
import java.util.Set;

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
import emlab.gen.domain.contract.CashFlow;
import emlab.gen.domain.contract.Loan;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.CO2Auction;
import emlab.gen.domain.market.ClearingPoint;
import emlab.gen.domain.market.CommodityMarket;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.PpdpAnnual;
import emlab.gen.domain.market.electricity.Segment;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.market.electricity.YearlySegment;
import emlab.gen.domain.market.electricity.YearlySegmentLoad;
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
import emlab.gen.role.investment.DismantlePowerPlantOperationalLossRole;
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
public class DismantlingOperationalLossTest {

    Logger logger = Logger.getLogger(DismantlingOperationalLossTest.class);

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

    @Autowired
    DismantlePowerPlantOperationalLossRole dismantlePowerPlantOperationalLossRole;

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

        getCurrentTick();
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
        YS.setYearlySegmentLengthInHours(8760);
        YS.persist();

        // TimeSeriesImpl intCapacity = new TimeSeriesImpl().persist();
        TimeSeriesCSVReader interconnectorCapacity = new TimeSeriesCSVReader().persist();
        interconnectorCapacity.setFilename("/data/ICCapacityNL.csv");
        interconnectorCapacity.setDelimiter(",");
        interconnectorCapacity.setVariableName("IC");

        TimeSeriesCSVReader investmentCostTrend = new TimeSeriesCSVReader().persist();
        investmentCostTrend.setFilename("/data/learningCurves.csv");
        investmentCostTrend.setStartingYear(-50);
        investmentCostTrend.setDelimiter(",");
        investmentCostTrend.setVariableName("CoalPSC_Inv");

        Interconnector interconnector = new Interconnector().persist();
        interconnector.setName("TestConnector");
        interconnector.setConnections(intNodes);
        interconnector.setInterconnectorCapacity(interconnectorCapacity);
        // interconnector.setCapacity(time, capacity);
        interconnector.setYearlySegment(YS);

        Segment S1 = new Segment();
        S1.setLengthInHours(10);
        S1.setLengthInHoursGLDCForInvestmentRole(10);
        S1.setSegmentID(1);
        S1.persist();

        Segment S2 = new Segment();
        S2.setLengthInHours(20);
        S2.setLengthInHoursGLDCForInvestmentRole(10);
        S2.setSegmentID(2);
        S2.persist();

        HourlyCSVTimeSeries hourlyDemand = new HourlyCSVTimeSeries();
        hourlyDemand.setLengthInHours(8760);
        hourlyDemand.setFilename("/data/ZoneALoad.csv");
        // hourlyDemand.setVariableName("nl");
        hourlyDemand.setDelimiter(",");
        // hourlyDemand.setTimeSeriesAreInDifferentColumns(true);
        hourlyDemand.persist();

        HourlyCSVTimeSeries dailyDemand = new HourlyCSVTimeSeries();
        dailyDemand.setLengthInHours(365);
        dailyDemand.setFilename("/data/NodeDemandDailySeriesData.csv");
        dailyDemand.setVariableName("nl");
        dailyDemand.setDelimiter(",");
        dailyDemand.setTimeSeriesAreInDifferentColumns(true);
        dailyDemand.persist();

        SegmentLoad segmentLoadMarket1S2 = new SegmentLoad().persist();
        segmentLoadMarket1S2.setSegment(S2);
        segmentLoadMarket1S2.setResidualGLDC(100);

        SegmentLoad segmentLoadMarket2S2 = new SegmentLoad().persist();
        segmentLoadMarket2S2.setSegment(S2);
        segmentLoadMarket2S2.setResidualGLDC(399.99);

        SegmentLoad segmentLoadMarket1S1 = new SegmentLoad().persist();
        segmentLoadMarket1S1.setSegment(S1);
        segmentLoadMarket1S1.setResidualGLDC(790);

        SegmentLoad segmentLoadMarket2S1 = new SegmentLoad().persist();
        segmentLoadMarket2S1.setSegment(S1);
        segmentLoadMarket2S1.setResidualGLDC(600);

        Set<SegmentLoad> segmentLoads1 = new HashSet<SegmentLoad>();
        segmentLoads1.add(segmentLoadMarket1S1);
        segmentLoads1.add(segmentLoadMarket1S2);

        Set<SegmentLoad> segmentLoads2 = new HashSet<SegmentLoad>();
        segmentLoads2.add(segmentLoadMarket2S1);
        segmentLoads2.add(segmentLoadMarket2S2);

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
        market1.setLoadDurationCurve(segmentLoads1);
        market1.setDemandGrowthTrend(demandGrowthTrend);
        market1.setValueOfLostLoad(2000);
        market1.setLookback(1);
        market1.persist();

        ElectricitySpotMarket market2 = new ElectricitySpotMarket();
        market2.setZone(zone2);
        market2.setYearlySegment(YS);
        market2.setYearlySegmentLoad(yearlySegmentLoad2);
        market2.setName("Market2");
        market2.setLoadDurationCurve(segmentLoads2);
        market2.setDemandGrowthTrend(demandGrowthTrend);
        market2.setValueOfLostLoad(2000);
        market2.setLookback(1);
        market2.persist();

        yearlySegmentLoad1.setElectricitySpotMarket(market1);
        yearlySegmentLoad1.setHourlyInElasticCurrentDemandForYearlySegment(hourlyDemand);
        yearlySegmentLoad1.setDailyElasticCurrentDemandForYearlySegment(dailyDemand);
        yearlySegmentLoad1.setHourlyInElasticBaseDemandForYearlySegment(hourlyDemand);
        yearlySegmentLoad1.setDailyElasticBaseDemandForYearlySegment(dailyDemand);
        yearlySegmentLoad1.setYearlySegment(YS);

        yearlySegmentLoad2.setElectricitySpotMarket(market2);
        yearlySegmentLoad2.setHourlyInElasticCurrentDemandForYearlySegment(hourlyDemand);
        yearlySegmentLoad2.setDailyElasticCurrentDemandForYearlySegment(dailyDemand);
        yearlySegmentLoad2.setHourlyInElasticBaseDemandForYearlySegment(hourlyDemand);
        yearlySegmentLoad2.setDailyElasticBaseDemandForYearlySegment(dailyDemand);
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
        gasPrice.setIncrement(2);

        HashSet<Substance> fuelMixCoal = new HashSet<Substance>();
        fuelMixCoal.add(coal);

        HashSet<Substance> fuelMixGas = new HashSet<Substance>();
        fuelMixGas.add(gas);

        PowerGeneratingTechnology coalTech = new PowerGeneratingTechnology();
        coalTech.setFuels(fuelMixCoal);
        coalTech.setPeakSegmentDependentAvailability(1);
        coalTech.setBaseSegmentDependentAvailability(1);
        coalTech.setInvestmentCostTimeSeries(investmentCostTrend);
        coalTech.setIntermittent(false);

        PowerGeneratingTechnology gasTech = new PowerGeneratingTechnology();
        gasTech.setFuels(fuelMixGas);
        gasTech.setPeakSegmentDependentAvailability(1);
        gasTech.setBaseSegmentDependentAvailability(1);
        gasTech.setInvestmentCostTimeSeries(investmentCostTrend);
        gasTech.setIntermittent(false);

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
        pp1.setActualNominalCapacity(500);
        pp1.setActualEfficiency(0.45);
        pp1.setLocation(node1);
        pp1.setActualPermittime(0);
        pp1.setConstructionStartTime(-10);
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
        pp2.setConstructionStartTime(-10);
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
        pp3.setActualNominalCapacity(300);
        pp3.setActualEfficiency(0.60);
        pp3.setLocation(node1);
        pp3.setActualPermittime(0);
        pp3.setConstructionStartTime(-10);
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
        pp4.setConstructionStartTime(-10);
        pp4.setActualLeadtime(0);
        pp4.setDismantleTime(10);
        pp4.setExpectedEndOfLife(10);
        pp4.setName("GasInM2");

        PowerPlant pp5 = new PowerPlant();
        pp5.setTechnology(gasTech);
        pp5.setOwner(market2Prod2);
        pp5.setActualFixedOperatingCost(99000);
        pp5.setLoan(l3);
        pp5.setActualNominalCapacity(200);
        pp5.setActualEfficiency(0.54);
        pp5.setLocation(node2);
        pp5.setActualPermittime(0);
        pp5.setConstructionStartTime(-10);
        pp5.setActualLeadtime(0);
        pp5.setDismantleTime(10);
        pp5.setExpectedEndOfLife(10);
        pp5.setName("GasInM2");

        PowerPlant pp6 = new PowerPlant();
        pp6.setTechnology(gasTech);
        pp6.setOwner(market2Prod2);
        pp6.setActualFixedOperatingCost(99000);
        pp6.setLoan(l3);
        pp6.setActualNominalCapacity(200);
        pp6.setActualEfficiency(0.54);
        pp6.setLocation(node2);
        pp6.setActualPermittime(0);
        pp6.setConstructionStartTime(-10);
        pp6.setActualLeadtime(0);
        pp6.setDismantleTime(10);
        pp6.setExpectedEndOfLife(10);
        pp6.setName("GasInM2");

        PowerPlant pp7 = new PowerPlant();
        pp7.setTechnology(gasTech);
        pp7.setOwner(market2Prod2);
        pp7.setActualFixedOperatingCost(99000);
        pp7.setLoan(l3);
        pp7.setActualNominalCapacity(200);
        pp7.setActualEfficiency(0.54);
        pp7.setLocation(node2);
        pp7.setActualPermittime(0);
        pp7.setConstructionStartTime(-10);
        pp7.setActualLeadtime(0);
        pp7.setDismantleTime(10);
        pp7.setExpectedEndOfLife(10);
        pp7.setName("GasInM2");

        pp1.persist();
        pp2.persist();
        pp3.persist();
        pp4.persist();
        pp7.persist();
        pp6.persist();
        pp5.persist();

        CashFlow cf1 = new CashFlow();
        cf1.setRegardingPowerPlant(pp1);
        cf1.setMoney(-500000000);
        cf1.setTime(0);
        cf1.setType(CashFlow.FIXEDOMCOST);
        CashFlow cf2 = new CashFlow();
        cf2.setRegardingPowerPlant(pp2);
        cf2.setMoney(0);
        cf2.setTime(0);
        cf2.setType(CashFlow.FIXEDOMCOST);
        CashFlow cf3 = new CashFlow();
        cf3.setRegardingPowerPlant(pp3);
        cf3.setMoney(0);
        cf3.setTime(0);
        cf3.setType(CashFlow.FIXEDOMCOST);
        CashFlow cf4 = new CashFlow();
        cf4.setRegardingPowerPlant(pp4);
        cf4.setMoney(0);
        cf4.setTime(0);
        cf4.setType(CashFlow.FIXEDOMCOST);
        CashFlow cf5 = new CashFlow();
        cf5.setRegardingPowerPlant(pp5);
        cf5.setMoney(0);
        cf5.setTime(0);
        cf5.setType(CashFlow.FIXEDOMCOST);
        CashFlow cf6 = new CashFlow();
        cf6.setRegardingPowerPlant(pp6);
        cf6.setMoney(0);
        cf6.setTime(0);
        cf6.setType(CashFlow.FIXEDOMCOST);
        CashFlow cf7 = new CashFlow();
        cf7.setRegardingPowerPlant(pp7);
        cf7.setMoney(0);
        cf7.setTime(0);
        cf7.setType(CashFlow.FIXEDOMCOST);

        CashFlow cf1R = new CashFlow();
        cf1R.setRegardingPowerPlant(pp1);
        cf1R.setMoney(500000000);
        cf1R.setTime(0);
        cf1R.setType(CashFlow.ELECTRICITY_SPOT);

        CashFlow cf1RR = new CashFlow();
        cf1RR.setRegardingPowerPlant(pp1);
        cf1RR.setMoney(900000000);
        cf1RR.setTime(0);
        cf1RR.setType(CashFlow.ELECTRICITY_SPOT);
        CashFlow cf2R = new CashFlow();
        cf2R.setRegardingPowerPlant(pp2);
        cf2R.setMoney(0);
        cf2R.setTime(0);
        cf2R.setType(CashFlow.ELECTRICITY_SPOT);
        CashFlow cf3R = new CashFlow();
        cf3R.setRegardingPowerPlant(pp3);
        cf3R.setMoney(0);
        cf3R.setTime(0);
        cf3R.setType(CashFlow.ELECTRICITY_SPOT);
        CashFlow cf4R = new CashFlow();
        cf4R.setRegardingPowerPlant(pp4);
        cf4R.setMoney(0);
        cf4R.setTime(0);
        cf4R.setType(CashFlow.ELECTRICITY_SPOT);
        CashFlow cf5R = new CashFlow();
        cf5R.setRegardingPowerPlant(pp5);
        cf5R.setMoney(0);
        cf5R.setTime(0);
        cf5R.setType(CashFlow.ELECTRICITY_SPOT);
        CashFlow cf6R = new CashFlow();
        cf6R.setRegardingPowerPlant(pp6);
        cf6R.setMoney(0);
        cf6R.setTime(0);
        cf6R.setType(CashFlow.ELECTRICITY_SPOT);
        CashFlow cf7R = new CashFlow();
        cf7R.setRegardingPowerPlant(pp7);
        cf7R.setMoney(0);
        cf7R.setTime(0);
        cf7R.setType(CashFlow.ELECTRICITY_SPOT);

        cf1.persist();
        cf2.persist();
        cf3.persist();
        cf4.persist();
        cf5.persist();
        cf6.persist();
        cf7.persist();

        cf1R.persist();
        cf1RR.persist();
        cf2R.persist();
        cf3R.persist();
        cf4R.persist();
        cf5R.persist();
        cf6R.persist();
        cf7R.persist();

        PpdpAnnual ppdp1 = new PpdpAnnual();
        ppdp1.setYearlySupply(200);
        ppdp1.setPowerPlant(pp1);
        ppdp1.setTime(0);

        PpdpAnnual ppdp2 = new PpdpAnnual();
        ppdp2.setYearlySupply(2000000);
        ppdp2.setPowerPlant(pp2);
        ppdp2.setTime(0);

        PpdpAnnual ppdp3 = new PpdpAnnual();
        ppdp3.setYearlySupply(2000000);
        ppdp3.setPowerPlant(pp3);
        ppdp3.setTime(0);

        PpdpAnnual ppdp4 = new PpdpAnnual();
        ppdp4.setYearlySupply(2000);
        ppdp4.setPowerPlant(pp4);
        ppdp4.setTime(0);

        PpdpAnnual ppdp5 = new PpdpAnnual();
        ppdp5.setYearlySupply(2000);
        ppdp5.setPowerPlant(pp5);
        ppdp5.setTime(0);

        PpdpAnnual ppdp6 = new PpdpAnnual();
        ppdp6.setYearlySupply(2000);
        ppdp6.setPowerPlant(pp6);
        ppdp6.setTime(0);

        PpdpAnnual ppdp7 = new PpdpAnnual();
        ppdp7.setYearlySupply(2000);
        ppdp7.setPowerPlant(pp7);
        ppdp7.setTime(0);

        ppdp1.persist();
        ppdp2.persist();
        ppdp3.persist();
        ppdp4.persist();
        ppdp5.persist();
        ppdp6.persist();
        ppdp7.persist();

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
            // submitOffersToElectricitySpotMarketAnnualRole.act(producer);
            // submitOffersToElectricitySpotMarketRole.act(producer);
            // producer.act(determineFuelMixRole);
        }

        // clearHourlyElectricityMarketRole.act(model);

        for (ElectricitySpotMarket market : reps.marketRepository.findAllElectricitySpotMarketsAsList()) {
            dismantlePowerPlantOperationalLossRole.act(market);
        }

        // Check that
        for (PowerPlant plant : reps.powerPlantRepository.findAll()) {
            // for (Segment s : reps.segmentRepository.findAll()) {
            // PowerPlantDispatchPlan plan =
            // reps.powerPlantDispatchPlanRepository
            // .findOnePowerPlantDispatchPlanForPowerPlantForSegmentForTime(plant,
            // s, 0, false);
            // PpdpAnnual plan =
            // reps.ppdpAnnualRepository.findPPDPAnnualforPlantForCurrentTick(plant,
            // (long) 0);

            // }

        }

    }

    private long getCurrentTick() {
        return 1;
    }

}

// }