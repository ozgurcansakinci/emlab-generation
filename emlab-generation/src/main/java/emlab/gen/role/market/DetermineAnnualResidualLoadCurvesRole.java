package emlab.gen.role.market;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.AbstractRole;
import agentspring.role.Role;
import agentspring.role.RoleComponent;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.math.Functions;
import emlab.gen.domain.agent.DecarbonizationModel;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.electricity.IntermittentTechnologyNodeLoadFactor;
import emlab.gen.domain.market.electricity.PpdpAnnual;
import emlab.gen.domain.market.electricity.Segment;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.market.electricity.TimeSeriesToLDCClearingPoint;
import emlab.gen.domain.market.electricity.YearlySegmentClearingPointMarketInformation;
import emlab.gen.domain.technology.IntermittentResourceProfile;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.repository.Reps;
import emlab.gen.util.Utils;
import hep.aida.bin.DynamicBin1D;

/**
 * *
 *
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a>
 *
 * @author <a href="mailto:J.Richstein@tudelft.nl">Jörn Richstein</a>
 * 
 *         Modifications for the hourly optimization by Salman and Ozgur
 *
 */
@RoleComponent
public class DetermineAnnualResidualLoadCurvesRole extends AbstractRole<DecarbonizationModel>
        implements Role<DecarbonizationModel> {

    @Autowired
    private Reps reps;

    /**
     * Is transactional, since it works a lot on SegmentIntermittentProduction
     * classes.
     */

    @Override
    @Transactional
    public void act(DecarbonizationModel model) {

        long clearingTick = getCurrentTick();

        // for zonlogger.warn("0. Determining the residual load duration
        // curve");

        // 1. Create big matrix which contains columns for the information later
        // used.
        // Fill the columns with starting information (hour of year, initial
        // maximum interconnector capacity

        // Create Matrix with following columns
        // Hour of year | SegmentId | 2x Load | 2x intermittent Prod. | 2x Res.
        // Load | Res.Load Total | Interc. Cap. | SegmentsAccordingToA |
        // SegmentsAccordingtoB
        // When creating views, and changes are made to the views, the
        // original matrix is changed as well (see Colt package).

        List<Zone> zoneList = Utils.asList(reps.template.findAll(Zone.class));
        List<PowerGeneratingTechnology> technologyList = Utils
                .asList(reps.powerGeneratingTechnologyRepository.findAllIntermittentPowerGeneratingTechnologies());
        Map<Zone, List<PowerGridNode>> zoneToNodeList = new HashMap<Zone, List<PowerGridNode>>();
        for (Zone zone : zoneList) {
            List<PowerGridNode> nodeList = Utils.asList(reps.powerGridNodeRepository.findAllPowerGridNodesByZone(zone));
            zoneToNodeList.put(zone, nodeList);
        }

        int columnIterator = 0;

        // Naming of columns, since dynamically created cannot be done as an
        // enum.
        int HOUR = columnIterator;
        columnIterator++;

        int SEGMENT = columnIterator;
        columnIterator++;

        Map<Zone, Integer> LOADINZONE = new HashMap<Zone, Integer>();
        for (Zone zone : zoneList) {
            LOADINZONE.put(zone, columnIterator);
            columnIterator++;
        }

        Map<Zone, Integer> DEMANDINZONE = new HashMap<Zone, Integer>();
        for (Zone zone : zoneList) {
            DEMANDINZONE.put(zone, columnIterator);
            columnIterator++;
        }

        Map<Zone, Integer> IGEN = new HashMap<Zone, Integer>();
        for (Zone zone : zoneList) {
            IGEN.put(zone, columnIterator);
            columnIterator++;
        }

        Map<Zone, Integer> RLOADINZONE = new HashMap<Zone, Integer>();
        for (Zone zone : zoneList) {
            RLOADINZONE.put(zone, columnIterator);
            columnIterator++;
        }

        int RLOADTOTAL = columnIterator;
        columnIterator++;

        int DEMANDTOTAL = columnIterator;
        columnIterator++;

        int GENTOTAL = columnIterator;
        columnIterator++;

        // int INTERCONNECTOR = columnIterator;
        // columnIterator++;

        Map<Zone, Integer> SEGMENTFORZONE = new HashMap<Zone, Integer>();
        for (Zone zone : zoneList) {
            SEGMENTFORZONE.put(zone, columnIterator);
            columnIterator++;
        }

        Map<Zone, Integer> PRICEFORZONE = new HashMap<Zone, Integer>();
        for (Zone zone : zoneList) {
            PRICEFORZONE.put(zone, columnIterator);
            columnIterator++;
        }

        Map<Zone, Map<PowerGridNode, Map<PowerGeneratingTechnology, Integer>>> TECHNOLOGYLOADFACTORSFORZONEANDNODE = new HashMap<Zone, Map<PowerGridNode, Map<PowerGeneratingTechnology, Integer>>>();
        for (Zone zone : zoneList) {
            Map<PowerGridNode, Map<PowerGeneratingTechnology, Integer>> NODETOTECHNOLOGY = new HashMap<PowerGridNode, Map<PowerGeneratingTechnology, Integer>>();
            for (PowerGridNode node : zoneToNodeList.get(zone)) {
                Map<PowerGeneratingTechnology, Integer> technologyToColumn = new HashMap<PowerGeneratingTechnology, Integer>();
                for (PowerGeneratingTechnology technology : technologyList) {
                    technologyToColumn.put(technology, columnIterator);
                    columnIterator++;
                }
                NODETOTECHNOLOGY.put(node, technologyToColumn);
            }
            TECHNOLOGYLOADFACTORSFORZONEANDNODE.put(zone, NODETOTECHNOLOGY);
        }

        // double interConnectorCapacity =
        // reps.template.findAll(Interconnector.class).iterator().next()
        // .getCapacity(clearingTick);

        // Create globalResidualLoadMatrix and add hours.

        DoubleMatrix2D m = new DenseDoubleMatrix2D(8760, columnIterator);
        m.assign(0d);

        for (int row = 0; row < 8760; row++) {
            m.set(row, HOUR, row);
        }

        // Create vector of 1:
        DoubleMatrix1D oneVector = new DenseDoubleMatrix1D(m.rows());
        oneVector.assign(1);

        // Is set to negative, since later on a max(-interconnector, Rload) is
        // applied.

        // m.viewColumn(INTERCONNECTOR).assign(-interConnectorCapacity);

        logger.debug("First 10 values of matrix: \n " + m.viewPart(0, 0, 10, m.columns()).toString());

        // 2. Build national load curves, by adding up grid node load curves in
        // each zone.
        // also fill the residual load columns with the initial load curves.
        // for now simply multiplied with the market wide growth factor
        for (Zone zone : zoneList) {
            YearlySegmentClearingPointMarketInformation info = reps.yearlySegmentClearingPointMarketInformationRepository
                    .findMarketInformationForMarketAndTime(getCurrentTick(),
                            reps.marketRepository.findElectricitySpotMarketForZone(zone));
            // DoubleMatrix1D interconnectorFlow = null;
            // for (Interconnector inter :
            // reps.interconnectorRepository.findAllInterconnectors()) {
            // // Interconnector
            // // inter=reps.interconnectorRepository.findAllInterconnectors()
            // YearlySegmentClearingPointInterconnectorInformation interInfo =
            // reps.yearlySegmentClearingPointInterconnectorInformationRepository
            // .findInterconnectorInformationForTime(getCurrentTick(), inter);
            // interconnectorFlow = new
            // DenseDoubleMatrix1D(interInfo.getYearlyInterconnectorFlow());
            // }
            ///////////////////////////////////////////////////////////////////////////////////////////////
            // We changed the hourlyArray from demand to supply. We need to find
            // a way to make generation growing
            ////////////////////////////////////////////////////////////////////////////////////////////////
            DoubleMatrix1D hourlyArray = new DenseDoubleMatrix1D(info.getMarketSupply());
            DoubleMatrix1D hourlyDemand = new DenseDoubleMatrix1D(info.getMarketDemand());
            // DoubleMatrix1D elasticDemand = new
            // DenseDoubleMatrix1D(info.getElasticDemand());
            DoubleMatrix1D priceArray = new DenseDoubleMatrix1D(info.getMarketPrice());
            DoubleMatrix1D valueOfLostLoad = new DenseDoubleMatrix1D(info.getValueOfLostLoad());
            // double growthRate =
            // reps.marketRepository.findElectricitySpotMarketForZone(zone).getDemandGrowthTrend()
            // .getValue(clearingTick);
            // DoubleMatrix1D growthFactors = hourlyArray.copy();
            // growthFactors.assign(growthRate);
            // hourlyArray.assign(growthFactors, Functions.mult);
            m.viewColumn(LOADINZONE.get(zone)).assign(hourlyArray, Functions.plus);
            // m.viewColumn(LOADINZONE.get(zone)).assign(valueOfLostLoad,
            // Functions.minus);
            m.viewColumn(RLOADINZONE.get(zone)).assign(m.viewColumn(LOADINZONE.get(zone)), Functions.plus);
            m.viewColumn(PRICEFORZONE.get(zone)).assign(priceArray, Functions.plus);
            m.viewColumn(DEMANDINZONE.get(zone)).assign(hourlyDemand, Functions.plus);
            m.viewColumn(DEMANDTOTAL).assign(m.viewColumn(DEMANDINZONE.get(zone)), Functions.plus);
            // m.viewColumn(DEMANDTOTAL).assign(valueOfLostLoad,
            // Functions.plus);
            // m.viewColumn(DEMANDTOTAL).assign(elasticDemand, Functions.minus);
            m.viewColumn(GENTOTAL).assign(m.viewColumn(LOADINZONE.get(zone)), Functions.plus);
            // m.viewColumn(DEMANDTOTAL).assign(hourlyDemand, Functions.plus);
            // m.viewColumn(GENTOTAL).assign(hourlyArray, Functions.plus);
            // m.viewColumn(RLOADINZONE.get(zone)).assign(valueOfLostLoad,
            // Functions.minus);
            // m.viewColumn(GENTOTAL).assign(valueOfLostLoad, Functions.minus);
            // m.viewColumn(LOADINZONE.get(zone)).assign(valueOfLostLoad,
            // Functions.minus);

            // Subtract interconnector flow from everything(trial)
            // m.viewColumn(RLOADINZONE.get(zone)).assign(interconnectorFlow,
            // Functions.minus);
            // m.viewColumn(LOADINZONE.get(zone)).assign(interconnectorFlow,
            // Functions.minus);
            // m.viewColumn(GENTOTAL).assign(interconnectorFlow,
            // Functions.minus);
            // }

        }

        // 3. For each power grid node multiply the time series of each
        // intermittent technology type with
        // the installed capacity of that technology type. Substract
        // intermittent production from the
        // the residual load column (one column per zone). Calculate the total
        // residual load (assuming
        // no interconnector constraints). Reduce the load factors by obvious
        // spill, that is RES production greater than demand + interconnector
        // capacity.

        for (Zone zone : zoneList) {
            for (PowerGridNode node : zoneToNodeList.get(zone)) {
                for (PowerGeneratingTechnology technology : technologyList) {
                    IntermittentResourceProfile intermittentResourceProfile = reps.intermittentResourceProfileRepository
                            .findIntermittentResourceProfileByTechnologyAndNode(technology, node);
                    DoubleMatrix1D intResourceProfile = new DenseDoubleMatrix1D(
                            intermittentResourceProfile.getHourlyArray(getCurrentTick()));
                    m.viewColumn(TECHNOLOGYLOADFACTORSFORZONEANDNODE.get(zone).get(node).get(technology))
                            .assign(intResourceProfile, Functions.plus);
                }
            }
            for (PpdpAnnual ppdp : reps.ppdpAnnualRepository
                    .findAllAcceptedPpdpAnnualForIntermittentTechnologiesForMarketAndTime(
                            reps.marketRepository.findElectricitySpotMarketForZone(zone), getCurrentTick())) {

                double[] intermittentProduction = ppdp.getAcceptedHourlyAmount();

                // logger.warn(technology.getName() + ": " +
                // intermittentCapacityOfTechnologyInNode + " MW in Node "
                // + node.getName() + " and Zone: " + zone.getName());
                // Calculates hourly production of intermittent renewable
                // technology per node

                DoubleMatrix1D hourlyRESGenerationPerZone = new DenseDoubleMatrix1D(intermittentProduction);
                // System.out.println(hourlyRESGenerationPerZone.zSum());
                m.viewColumn(IGEN.get(zone)).assign(hourlyRESGenerationPerZone, Functions.plus);
                // Add to zonal-technological RES column
                // Substracts the above from the residual load curve
                m.viewColumn(RLOADINZONE.get(zone)).assign(hourlyRESGenerationPerZone, Functions.minus);

            }

            DoubleMatrix1D spillVector = m.viewColumn(LOADINZONE.get(zone)).copy();
            spillVector.assign(m.viewColumn(IGEN.get(zone)), Functions.div);
            spillVector.assign(oneVector, Functions.min);
            for (PowerGridNode node : zoneToNodeList.get(zone)) {
                for (PowerGeneratingTechnology technology : technologyList) {

                    m.viewColumn(TECHNOLOGYLOADFACTORSFORZONEANDNODE.get(zone).get(node).get(technology))
                            .assign(spillVector, Functions.mult);

                }
            }

            m.viewColumn(RLOADTOTAL).assign(m.viewColumn(RLOADINZONE.get(zone)), Functions.plus);

        }
        // 5. Order the hours in the global residual load curve. Peak load
        // first, base load last.

        // Sorts matrix by the load curve in descending order

        m = m.viewSorted(RLOADTOTAL).viewRowFlip();
        // for (Zone zone : zoneList) {
        // m = m.viewSorted(RLOADINZONE.get(zone)).viewRowFlip();
        // }

        // 6. Find values, so that each segments has approximately equal
        // capacity
        // needs.
        int noSegments = (int) reps.segmentRepository.count();

        double min = m.viewColumn(RLOADTOTAL).aggregate(Functions.min, Functions.identity);
        double max = m.viewColumn(RLOADTOTAL).aggregate(Functions.max, Functions.identity);
        double hoursWithoutZeroRLoad = 0;
        for (int row = 0; row < m.rows(); row++) {
            if (m.get(row, RLOADTOTAL) > 0) {
                hoursWithoutZeroRLoad++;
            } else {
                break;
            }
        }

        double[] upperBoundSplit = new double[noSegments];

        if (hoursWithoutZeroRLoad > 8750) {
            for (int i = 0; i < noSegments; i++) {
                upperBoundSplit[i] = max - (((double) (i)) / noSegments * (max - min));
                // logger.warn("Split for rload" + upperBoundSplit[i]);
            }
        } else {
            for (int i = 0; i < (noSegments - 1); i++) {
                upperBoundSplit[i] = max - (((double) (i)) / (noSegments - 1) * (max - min));
            }
            upperBoundSplit[noSegments - 1] = 0;
        }

        m = m.viewSorted(DEMANDTOTAL).viewRowFlip();
        double minDemand = m.viewColumn(DEMANDTOTAL).aggregate(Functions.min, Functions.identity);
        double maxDemand = m.viewColumn(DEMANDTOTAL).aggregate(Functions.max, Functions.identity);
        double[] upperBoundSplitForDemand = new double[noSegments];
        for (int i = 0; i < noSegments; i++) {
            upperBoundSplitForDemand[i] = maxDemand - (((double) (i)) / noSegments * (maxDemand - minDemand));
            // logger.warn("Split for demand" + upperBoundSplitForDemand[i]);
        }

        m = m.viewSorted(GENTOTAL).viewRowFlip();
        double minSupply = m.viewColumn(GENTOTAL).aggregate(Functions.min, Functions.identity);
        double maxSupply = m.viewColumn(GENTOTAL).aggregate(Functions.max, Functions.identity);
        double[] upperBoundSplitForGeneration = new double[noSegments];
        for (int i = 0; i < noSegments; i++) {
            upperBoundSplitForGeneration[i] = maxSupply - (((double) (i)) / noSegments * (maxSupply - minSupply));
            // logger.warn("Split for generation" +
            // upperBoundSplitForGeneration[i]);
        }

        Map<Zone, DoubleMatrix1D> spillFactorMap = new HashMap<Zone, DoubleMatrix1D>();
        for (Zone zone : zoneList) {
            spillFactorMap.put(zone, m.viewColumn(IGEN.get(zone)).copy());
        }

        for (Zone zone : zoneList) {
            DoubleMatrix1D minValuesVector = spillFactorMap.get(zone).like();
            minValuesVector.assign(Double.MIN_NORMAL);
            spillFactorMap.get(zone).assign(minValuesVector, Functions.plus);
            m.viewColumn(IGEN.get(zone)).assign(minValuesVector, Functions.plus);
            spillFactorMap.get(zone).assign(m.viewColumn(IGEN.get(zone)), Functions.div);
            m.viewColumn(IGEN.get(zone)).assign(minValuesVector, Functions.minus);
        }

        for (Zone zone : zoneList) {
            for (PowerGridNode node : zoneToNodeList.get(zone)) {

                for (PowerGeneratingTechnology technology : technologyList) {

                    m.viewColumn(TECHNOLOGYLOADFACTORSFORZONEANDNODE.get(zone).get(node).get(technology))
                            .assign(spillFactorMap.get(zone), Functions.div);

                }

            }
        }

        // 7. Create DynamicBins as representation for segments and for later
        // calculation of means, no etc. Per bin one sort of information (e.g.
        // residual
        // load, interconnector capacity) and the corresponding hour of the yea
        // can be stored.
        // Thus connection to the matrix remains.

        DynamicBin1D[] segmentRloadBins = new DynamicBin1D[noSegments];
        for (int i = 0; i < noSegments; i++) {
            segmentRloadBins[i] = new DynamicBin1D();
        }

        DynamicBin1D[] segmentDemandBins = new DynamicBin1D[noSegments];
        for (int i = 0; i < noSegments; i++) {
            segmentDemandBins[i] = new DynamicBin1D();
        }

        DynamicBin1D[] segmentGenerationBins = new DynamicBin1D[noSegments];
        for (int i = 0; i < noSegments; i++) {
            segmentGenerationBins[i] = new DynamicBin1D();
        }

        Map<Zone, DynamicBin1D[]> segmentRloadBinsByZone = new HashMap<Zone, DynamicBin1D[]>();
        Map<Zone, DynamicBin1D[]> segmentLoadBinsByZone = new HashMap<Zone, DynamicBin1D[]>();
        Map<Zone, DynamicBin1D[]> segmentPriceBinsByZone = new HashMap<Zone, DynamicBin1D[]>();
        Map<Zone, DynamicBin1D[]> segmentDemandBinsByZone = new HashMap<Zone, DynamicBin1D[]>();

        for (Zone zone : zoneList) {
            DynamicBin1D[] segmentRloadBinInZone = new DynamicBin1D[noSegments];
            DynamicBin1D[] segmentLoadBinInZone = new DynamicBin1D[noSegments];
            DynamicBin1D[] segmentPriceBinInZone = new DynamicBin1D[noSegments];
            DynamicBin1D[] segmentDemandBinInZone = new DynamicBin1D[noSegments];
            for (int i = 0; i < noSegments; i++) {
                segmentRloadBinInZone[i] = new DynamicBin1D();
                segmentLoadBinInZone[i] = new DynamicBin1D();
                segmentPriceBinInZone[i] = new DynamicBin1D();
                segmentDemandBinInZone[i] = new DynamicBin1D();
            }
            segmentRloadBinsByZone.put(zone, segmentRloadBinInZone);
            segmentLoadBinsByZone.put(zone, segmentLoadBinInZone);
            segmentPriceBinsByZone.put(zone, segmentPriceBinInZone);
            segmentDemandBinsByZone.put(zone, segmentDemandBinInZone);
        }

        Map<Zone, Map<PowerGridNode, Map<PowerGeneratingTechnology, DynamicBin1D[]>>> loadFactorBinMap = new HashMap<Zone, Map<PowerGridNode, Map<PowerGeneratingTechnology, DynamicBin1D[]>>>();
        for (Zone zone : zoneList) {
            Map<PowerGridNode, Map<PowerGeneratingTechnology, DynamicBin1D[]>> NODETOTECHNOLOGY = new HashMap<PowerGridNode, Map<PowerGeneratingTechnology, DynamicBin1D[]>>();
            for (PowerGridNode node : zoneToNodeList.get(zone)) {
                Map<PowerGeneratingTechnology, DynamicBin1D[]> technologyToBins = new HashMap<PowerGeneratingTechnology, DynamicBin1D[]>();
                for (PowerGeneratingTechnology technology : technologyList) {
                    DynamicBin1D[] technologyLoadFactorInNode = new DynamicBin1D[noSegments];
                    for (int i = 0; i < noSegments; i++) {
                        technologyLoadFactorInNode[i] = new DynamicBin1D();
                    }
                    technologyToBins.put(technology, technologyLoadFactorInNode);
                }
                NODETOTECHNOLOGY.put(node, technologyToBins);
            }
            loadFactorBinMap.put(zone, NODETOTECHNOLOGY);
        }

        // for (Zone zone : zoneList) {
        // // double minRLoadInZone =
        // // m.viewColumn(RLOADINZONE.get(zone)).aggregate(Functions.min,
        // // Functions.identity);
        // // double maxRLoadInZone =
        // // m.viewColumn(RLOADINZONE.get(zone)).aggregate(Functions.max,
        // // Functions.identity);
        // double minDemandInZone =
        // m.viewColumn(DEMANDINZONE.get(zone)).aggregate(Functions.min,
        // Functions.identity);
        // double maxDemandInZone =
        // m.viewColumn(DEMANDINZONE.get(zone)).aggregate(Functions.max,
        // Functions.identity);
        // double minGenerationInZone =
        // m.viewColumn(LOADINZONE.get(zone)).aggregate(Functions.min,
        // Functions.identity);
        // double maxGenerationInZone =
        // m.viewColumn(LOADINZONE.get(zone)).aggregate(Functions.max,
        // Functions.identity);
        // // double[] upperBoundSplitInZoneForRLoad = new double[noSegments];
        // double[] upperBoundSplitInZoneForDemand = new double[noSegments];
        // double[] upperBoundSplitInZoneForGeneration = new double[noSegments];
        // // double[] upperBoundPriceSplitInZone = new double[noSegments];
        //
        // for (int i = 0; i < noSegments; i++) {
        // // upperBoundSplitInZoneForRLoad[i] = maxRLoadInZone
        // // - (((double) (i)) / noSegments * (maxRLoadInZone -
        // // minRLoadInZone));
        // upperBoundSplitInZoneForDemand[i] = maxDemandInZone
        // - (((double) (i)) / noSegments * (maxDemandInZone -
        // minDemandInZone));
        // upperBoundSplitInZoneForGeneration[i] = maxGenerationInZone
        // - (((double) (i)) / noSegments * (maxGenerationInZone -
        // minGenerationInZone));
        // logger.warn("Demand split in " + zone +
        // upperBoundSplitInZoneForDemand[i]);
        // logger.warn("Supply split in " + zone +
        // upperBoundSplitInZoneForGeneration[i]);
        // }
        //
        // // m = m.viewSorted(RLOADINZONE.get(zone)).viewRowFlip();
        // // int currentSegmentID = 1;
        // // int hoursAssignedToCurrentSegment = 0;
        // // for (int row = 0; row < m.rows() && currentSegmentID <=
        // // noSegments; row++) {
        // // // IMPORTANT: since [] is zero-based index, it checks one index
        // // // ahead of current segment.
        // // while (currentSegmentID < noSegments &&
        // // hoursAssignedToCurrentSegment > 0
        // // && m.get(row, RLOADINZONE.get(zone)) <=
        // // upperBoundSplitInZoneForRLoad[currentSegmentID]) {
        // // currentSegmentID++;
        // // hoursAssignedToCurrentSegment = 0;
        // // }
        // // m.set(row, SEGMENT, currentSegmentID);
        // // // segmentRloadBins[currentSegmentID - 1].add(m.get(row,
        // // // RLOADINZONE.get(zone)));
        // // segmentRloadBinsByZone.get(zone)[currentSegmentID -
        // // 1].add(m.get(row, RLOADINZONE.get(zone)));
        // // // segmentLoadBinsByZone.get(zone)[currentSegmentID -
        // // // 1].add(m.get(row, LOADINZONE.get(zone)));
        // // segmentPriceBinsByZone.get(zone)[currentSegmentID -
        // // 1].add(m.get(row, PRICEFORZONE.get(zone)));
        // // // segmentDemandBinsByZone.get(zone)[currentSegmentID -
        // // // 1].add(m.get(row, DEMANDINZONE.get(zone)));
        // // hoursAssignedToCurrentSegment++;
        // // }
        //
        // // for (PowerGridNode node : zoneToNodeList.get(zone)) {
        // // for (PowerGeneratingTechnology technology :
        // // reps.powerGeneratingTechnologyRepository
        // // .findAllIntermittentPowerGeneratingTechnologies()) {
        // // DynamicBin1D[] currentBinArray =
        // // loadFactorBinMap.get(zone).get(node).get(technology);
        // // int columnNumber =
        // //
        // TECHNOLOGYLOADFACTORSFORZONEANDNODE.get(zone).get(node).get(technology);
        // // currentSegmentID = 1;
        // // hoursAssignedToCurrentSegment = 0;
        // // for (int row = 0; row < m.rows() && currentSegmentID <=
        // // noSegments; row++) {
        // // // IMPORTANT: since [] is zero-based index, it checks
        // // // one index
        // // // ahead of current segment.
        // // while (currentSegmentID < noSegments &&
        // // hoursAssignedToCurrentSegment > 0 && m.get(row,
        // // RLOADINZONE.get(zone)) <=
        // // upperBoundSplitInZoneForRLoad[currentSegmentID]) {
        // // currentSegmentID++;
        // // hoursAssignedToCurrentSegment = 0;
        // // }
        // // currentBinArray[currentSegmentID - 1].add(m.get(row,
        // // columnNumber));
        // // hoursAssignedToCurrentSegment++;
        // // }
        // // loadFactorBinMap.get(zone).get(node).put(technology,
        // // currentBinArray);
        // // }
        // // }
        //
        // int currentSegmentID = 1;
        // int hoursAssignedToCurrentSegment = 0;
        // m = m.viewSorted(DEMANDINZONE.get(zone)).viewRowFlip();
        // for (int row = 0; row < m.rows() && currentSegmentID <= noSegments;
        // row++) {
        // // IMPORTANT: since [] is zero-based index, it checks one index
        // // ahead of current segment.
        // while (currentSegmentID < noSegments && hoursAssignedToCurrentSegment
        // > 0
        // && m.get(row, DEMANDINZONE.get(zone)) <=
        // upperBoundSplitInZoneForDemand[currentSegmentID]) {
        // currentSegmentID++;
        // hoursAssignedToCurrentSegment = 0;
        // }
        // segmentDemandBinsByZone.get(zone)[currentSegmentID -
        // 1].add(m.get(row, DEMANDINZONE.get(zone)));
        // hoursAssignedToCurrentSegment++;
        // }
        //
        // currentSegmentID = 1;
        // hoursAssignedToCurrentSegment = 0;
        // m = m.viewSorted(LOADINZONE.get(zone)).viewRowFlip();
        // for (int row = 0; row < m.rows() && currentSegmentID <= noSegments;
        // row++) {
        // // IMPORTANT: since [] is zero-based index, it checks one index
        // // ahead of current segment.
        // while (currentSegmentID < noSegments && hoursAssignedToCurrentSegment
        // > 0
        // && m.get(row, LOADINZONE.get(zone)) <=
        // upperBoundSplitInZoneForGeneration[currentSegmentID]) {
        // currentSegmentID++;
        // hoursAssignedToCurrentSegment = 0;
        // }
        // // m.set(row, SEGMENT, currentSegmentID);
        // // segmentGenerationBins[currentSegmentID - 1].add(m.get(row,
        // // GENTOTAL));
        // segmentLoadBinsByZone.get(zone)[currentSegmentID - 1].add(m.get(row,
        // LOADINZONE.get(zone)));
        // hoursAssignedToCurrentSegment++;
        // }
        //
        // }

        m = m.viewSorted(RLOADTOTAL).viewRowFlip();
        // // Assign hours and load to bins and segments
        int currentSegmentID = 1;
        int hoursAssignedToCurrentSegment = 0;
        for (int row = 0; row < m.rows() && currentSegmentID <= noSegments; row++) {
            // IMPORTANT: since [] is zero-based index, it checks one index
            // ahead of current segment.
            while (currentSegmentID < noSegments && hoursAssignedToCurrentSegment > 0
                    && m.get(row, RLOADTOTAL) <= upperBoundSplit[currentSegmentID]) {
                currentSegmentID++;
                hoursAssignedToCurrentSegment = 0;
            }
            m.set(row, SEGMENT, currentSegmentID);
            segmentRloadBins[currentSegmentID - 1].add(m.get(row, RLOADTOTAL));
            for (Zone zone : zoneList) {
                segmentRloadBinsByZone.get(zone)[currentSegmentID - 1].add(m.get(row, RLOADINZONE.get(zone)));
                // segmentLoadBinsByZone.get(zone)[currentSegmentID -
                // 1].add(m.get(row, LOADINZONE.get(zone)));
                segmentPriceBinsByZone.get(zone)[currentSegmentID - 1].add(m.get(row, PRICEFORZONE.get(zone)));
                // segmentDemandBinsByZone.get(zone)[currentSegmentID -
                // 1].add(m.get(row, DEMANDINZONE.get(zone)));
            }
            hoursAssignedToCurrentSegment++;
        }
        // Assign rows to bins for the demand LDC(capacity market)
        currentSegmentID = 1;
        hoursAssignedToCurrentSegment = 0;
        m = m.viewSorted(DEMANDTOTAL).viewRowFlip();
        for (int row = 0; row < m.rows() && currentSegmentID <= noSegments; row++) {
            // IMPORTANT: since [] is zero-based index, it checks one index
            // ahead of current segment.
            while (currentSegmentID < noSegments && hoursAssignedToCurrentSegment > 0
                    && m.get(row, DEMANDTOTAL) <= upperBoundSplitForDemand[currentSegmentID]) {
                currentSegmentID++;
                hoursAssignedToCurrentSegment = 0;
            }
            // m.set(row, SEGMENT, currentSegmentID);
            segmentDemandBins[currentSegmentID - 1].add(m.get(row, DEMANDTOTAL));
            for (Zone zone : zoneList) {
                segmentDemandBinsByZone.get(zone)[currentSegmentID - 1].add(m.get(row, DEMANDINZONE.get(zone)));
            }
            hoursAssignedToCurrentSegment++;
        }
        // m = m.viewSorted(RLOADTOTAL).viewRowFlip();

        // Assign rows to bins for the generation LDC(investment role,
        // private
        // investment enabled)
        currentSegmentID = 1;
        hoursAssignedToCurrentSegment = 0;
        m = m.viewSorted(GENTOTAL).viewRowFlip();
        for (int row = 0; row < m.rows() && currentSegmentID <= noSegments; row++) {
            // IMPORTANT: since [] is zero-based index, it checks one index
            // ahead of current segment.
            while (currentSegmentID < noSegments && hoursAssignedToCurrentSegment > 0
                    && m.get(row, GENTOTAL) <= upperBoundSplitForGeneration[currentSegmentID]) {
                currentSegmentID++;
                hoursAssignedToCurrentSegment = 0;
            }
            // m.set(row, SEGMENT, currentSegmentID);
            segmentGenerationBins[currentSegmentID - 1].add(m.get(row, GENTOTAL));
            for (Zone zone : zoneList) {
                segmentLoadBinsByZone.get(zone)[currentSegmentID - 1].add(m.get(row, LOADINZONE.get(zone)));
            }
            hoursAssignedToCurrentSegment++;
        }
        m = m.viewSorted(RLOADTOTAL).viewRowFlip();

        for (Zone zone : zoneList) {
            for (PowerGridNode node : zoneToNodeList.get(zone)) {
                for (PowerGeneratingTechnology technology : reps.powerGeneratingTechnologyRepository
                        .findAllIntermittentPowerGeneratingTechnologies()) {
                    DynamicBin1D[] currentBinArray = loadFactorBinMap.get(zone).get(node).get(technology);
                    int columnNumber = TECHNOLOGYLOADFACTORSFORZONEANDNODE.get(zone).get(node).get(technology);
                    currentSegmentID = 1;
                    hoursAssignedToCurrentSegment = 0;
                    for (int row = 0; row < m.rows() && currentSegmentID <= noSegments; row++) {
                        // IMPORTANT: since [] is zero-based index, it checks
                        // one index
                        // ahead of current segment.
                        while (currentSegmentID < noSegments && hoursAssignedToCurrentSegment > 0
                                && m.get(row, RLOADTOTAL) <= upperBoundSplit[currentSegmentID]) {
                            currentSegmentID++;
                            hoursAssignedToCurrentSegment = 0;
                        }
                        currentBinArray[currentSegmentID - 1].add(m.get(row, columnNumber));
                        hoursAssignedToCurrentSegment++;
                    }
                    loadFactorBinMap.get(zone).get(node).put(technology, currentBinArray);
                }
            }
        }

        // Assign hours to segments according to residual load in this country.
        // Only for error estimation purposes

        // for (Zone zone : zoneList) {
        // currentSegmentID = 1;
        // double minInZone =
        // m.viewColumn(RLOADINZONE.get(zone)).aggregate(Functions.min,
        // Functions.identity);
        // double maxInZone =
        // m.viewColumn(RLOADINZONE.get(zone)).aggregate(Functions.max,
        // Functions.identity);
        //
        // // double minPriceInZone =
        // // m.viewColumn(PRICEFORZONE.get(zone)).aggregate(Functions.min,
        // // Functions.identity);
        // // double maxPriceInZone =
        // // m.viewColumn(PRICEFORZONE.get(zone)).aggregate(Functions.max,
        // // Functions.identity);
        //
        // double[] upperBoundSplitInZone = new double[noSegments];
        // // double[] upperBoundPriceSplitInZone = new double[noSegments];
        //
        // for (int i = 0; i < noSegments; i++) {
        // upperBoundSplitInZone[i] = maxInZone - (((double) (i)) / noSegments *
        // (maxInZone - minInZone));
        // // upperBoundPriceSplitInZone[i] = maxPriceInZone - (((double)
        // // (i)) / noSegments * (maxPriceInZone - minPriceInZone));
        // }
        //
        // m = m.viewSorted(RLOADINZONE.get(zone)).viewRowFlip();
        // int hoursInDifferentSegment = 0;
        // double averageSegmentDeviation = 0;
        // hoursAssignedToCurrentSegment = 0;
        // for (int row = 0; row < m.rows() && currentSegmentID <= noSegments;
        // row++) {
        // while (currentSegmentID < noSegments && hoursAssignedToCurrentSegment
        // > 0
        // && m.get(row, RLOADINZONE.get(zone)) <=
        // upperBoundSplitInZone[currentSegmentID]) {
        // currentSegmentID++;
        // hoursAssignedToCurrentSegment = 0;
        // }
        // m.set(row, SEGMENTFORZONE.get(zone), currentSegmentID);
        // if (currentSegmentID != m.get(row, SEGMENT)) {
        // hoursInDifferentSegment++;
        // averageSegmentDeviation += Math.abs(currentSegmentID - m.get(row,
        // SEGMENT));
        // }
        // hoursAssignedToCurrentSegment++;
        // }
        // if (hoursInDifferentSegment != 0) {
        // averageSegmentDeviation = averageSegmentDeviation /
        // hoursInDifferentSegment;
        // averageSegmentDeviation = averageSegmentDeviation * 1000;
        // averageSegmentDeviation = Math.round(averageSegmentDeviation);
        // averageSegmentDeviation = averageSegmentDeviation / 1000;
        // logger.warn("For " + zone + ", " + hoursInDifferentSegment
        // + " hours would have been in different segments, and on average " +
        // averageSegmentDeviation
        // + " Segments away from the segment they were in.");
        // } else {
        // // logger.warn("For " + zone + ", all hours were in the same
        // // segment, as for combined sorting!");
        // }
        //
        // }

        m = m.viewSorted(RLOADTOTAL).viewRowFlip();
        //
        // logger.warn("First 30 values of matrix: \n " + m.viewPart(0, 0, 30,
        // m.columns()).toString());

        // Printing of segments
        int it = 1;
        // for (DynamicBin1D bin : segmentRloadBins) {
        // logger.warn("Segment " + it + "\n Size: " + bin.size() + "\n Mean
        // RLOAD~: " + Math.round(bin.mean())
        // + "\n Max RLOAD~: " + Math.round(bin.max()) + "\n Min RLOAD~: " +
        // Math.round(bin.min())
        // + "\n Std RLOAD~: " + Math.round(bin.standardDeviation()));
        // it++;
        // }

        for (Zone zone : zoneList) {
            // logger.warn("Bins for " + zone);
            it = 1;
            String meanRLoad = new String("Residual load in " + zone.getName() + ":");
            String meanLoad = new String("Load in " + zone.getName() + ":");
            String segmentLength = new String("Segment length " + zone.getName() + ":");
            for (DynamicBin1D bin : segmentRloadBinsByZone.get(zone)) {
                // logger.warn("Segment " + it + " " + zone.getName() + "\n
                // Size: " + bin.size() + "\n Mean RLOAD~: "
                // + Math.round(bin.mean()) + "\n Max RLOAD~: " +
                // Math.round(bin.max()) + "\n Min RLOAD~: "
                // + Math.round(bin.min()) + "\n Std RLOAD~: " +
                // Math.round(bin.standardDeviation()));
                it++;
                double mean = bin.mean() * 1000;
                mean = Math.round(mean);
                mean = mean / 1000.0;
                meanRLoad = meanRLoad.concat("," + mean);
                segmentLength = segmentLength.concat("," + bin.size());
            }
            it = 1;
            for (DynamicBin1D bin : segmentLoadBinsByZone.get(zone)) {
                // logger.warn("Segment " + it + " " + zone.getName() + "\n
                // Size: " + bin.size() + "\n Mean LOAD~: "
                // + Math.round(bin.mean()) + "\n Max LOAD~: " +
                // Math.round(bin.max()) + "\n Min LOAD~: "
                // + Math.round(bin.min()) + "\n Std LOAD~: " +
                // Math.round(bin.standardDeviation()));
                it++;
                double mean = bin.mean() * 1000;
                mean = Math.round(mean);
                mean = mean / 1000.0;
                meanLoad = meanLoad.concat("," + mean);
                segmentLength = segmentLength.concat("," + bin.size());
            }
            // logger.warn(meanRLoad);
            // logger.warn(meanLoad);
            // logger.warn(segmentLength);
        }
        // 9. Store the load factors in the IntermittentTechnologyLoadFactors
        for (Zone zone : zoneList) {
            for (PowerGridNode node : zoneToNodeList.get(zone)) {

                for (PowerGeneratingTechnology technology : technologyList) {
                    String loadFactorString = new String(technology.getName() + " LF in " + node.toString() + ":");
                    // logger.warn("Bins for " + zone + ", " + node + "and " +
                    // technology);
                    IntermittentTechnologyNodeLoadFactor intTechnologyNodeLoadFactor = reps.intermittentTechnologyNodeLoadFactorRepository
                            .findIntermittentTechnologyNodeLoadFactorForNodeAndTechnology(node, technology);
                    if (intTechnologyNodeLoadFactor == null) {
                        intTechnologyNodeLoadFactor = new IntermittentTechnologyNodeLoadFactor().persist();
                        intTechnologyNodeLoadFactor.setLoadFactors(new double[noSegments]);
                        intTechnologyNodeLoadFactor.setNode(node);
                        intTechnologyNodeLoadFactor.setTechnology(technology);
                    }
                    ;
                    it = 1;
                    for (DynamicBin1D bin : loadFactorBinMap.get(zone).get(node).get(technology)) {
                        // logger.warn("Segment " + it + "\n Size: " +
                        // bin.size() + "\n Mean RLOAD~: " + bin.mean()
                        // + "\n Max RLOAD~: " + bin.max() + "\n Min RLOAD~: " +
                        // bin.min()
                        // + "\n Std RLOAD~: findAll" +
                        // bin.standardDeviation());
                        intTechnologyNodeLoadFactor.setLoadFactorForSegmentId(it, bin.mean());
                        double mean = bin.mean() * 1000000;
                        mean = Math.round(mean);
                        mean = mean / 1000000.0;
                        loadFactorString = loadFactorString.concat(" " + mean);
                        it++;
                        // logger.warn(technology + " node load factor for
                        // segment " + (it - 1) + "is"
                        // +
                        // intTechnologyNodeLoadFactor.getLoadFactorForSegmentId(it
                        // - 1));
                    }
                    // logger.warn(loadFactorString);
                }

            }
        }

        // 8. Store the segment duration and the average load in that segment
        // per country.

        Iterable<Segment> segments = reps.segmentRepository.findAll();
        for (Segment segment : segments) {
            // System.out.println(segment.toString());
            // segment.setLengthInHours(segmentRloadBins[segment.getSegmentID()
            // - 1].size());
            segment.setLengthInHoursGLDCForInvestmentRole(segmentRloadBins[segment.getSegmentID() - 1].size());
            segment.setLengthInHoursDLDCForCapacityMarket(segmentDemandBins[segment.getSegmentID() - 1].size());
            segment.setLengthInHoursTotalGLDCForInvestmentRole(
                    segmentGenerationBins[segment.getSegmentID() - 1].size());
            // logger.warn("Segment " + segment.getSegmentID() + ": " +
            // segment.getLengthInHoursGLDCForInvestmentRole()
            // + "hours");
        }

        Iterable<SegmentLoad> segmentLoads = reps.segmentLoadRepository.findAll();
        for (SegmentLoad segmentLoad : segmentLoads) {
            Segment segment = segmentLoad.getSegment();
            // System.out.println(segmentLoad.toString());
            Zone zone = segmentLoad.getElectricitySpotMarket().getZone();
            TimeSeriesToLDCClearingPoint priceClearingPoint = new TimeSeriesToLDCClearingPoint();
            priceClearingPoint.setSegment(segment);
            priceClearingPoint.setAbstractMarket(segmentLoad.getElectricitySpotMarket());
            priceClearingPoint.setPrice(Math.abs(segmentPriceBinsByZone.get(zone)[segment.getSegmentID() - 1].mean()));
            priceClearingPoint.setTime(getCurrentTick());
            if (segmentRloadBinsByZone.get(zone)[segment.getSegmentID() - 1].mean() > 0) {
                priceClearingPoint.setVolume(segmentRloadBinsByZone.get(zone)[segment.getSegmentID() - 1].mean());
                segmentLoad.setResidualGLDC(segmentRloadBinsByZone.get(zone)[segment.getSegmentID() - 1].mean());
            } else {
                priceClearingPoint.setVolume(0);
                segmentLoad.setResidualGLDC(0);
            }
            priceClearingPoint.persist();

            if (segmentDemandBinsByZone.get(zone)[segment.getSegmentID() - 1].mean() > 0)
                segmentLoad.setDemandLDC(segmentDemandBinsByZone.get(zone)[segment.getSegmentID() - 1].mean());
            else
                segmentLoad.setDemandLDC(0);

            if (segmentLoadBinsByZone.get(zone)[segment.getSegmentID() - 1].mean() > 0)
                segmentLoad.setGenerationLDC(segmentLoadBinsByZone.get(zone)[segment.getSegmentID() - 1].mean());
            else
                segmentLoad.setGenerationLDC(0);

            // segmentLoad
            // .setResidualGLDCSegmentPrice(segmentPriceBinsByZone.get(zone)[segment.getSegmentID()
            // - 1].mean());

            // logger.warn("Segment " + segment.getSegmentID() + ": " +
            // segmentLoad.getBaseLoad() + "MW" + "Segment Price "
            // + priceClearingPoint.getPrice() +
            // segmentLoad.getElectricitySpotMarket().toString());

            // logger.warn("Segment " + segment.getSegmentID() + ": " +
            // segmentLoad.getResidualGLDC() + " MW--"
            // + "Hours S: " + segment.getLengthInHoursGLDCForInvestmentRole() +
            // "Demand "
            // + segmentLoad.getDemandLDC() + "Hours D" +
            // segment.getLengthInHoursDLDCForCapacityMarket()
            // + "Generation " + segmentLoad.getGenerationLDC() + "Hours G"
            // + segment.getLengthInHoursTotalGLDCForInvestmentRole() + " Price
            // " + priceClearingPoint.getPrice()
            // + "Eur/MWh--" + " " +
            // segmentLoad.getElectricitySpotMarket().toString());
        }
    }

    public Reps getReps() {
        return reps;
    }

}