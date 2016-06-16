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
import emlab.gen.domain.market.electricity.PpdpAnnual;
import emlab.gen.domain.market.electricity.Segment;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.market.electricity.YearlySegmentClearingPointMarketInformation;
import emlab.gen.domain.technology.Interconnector;
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
public class DetermineAnnualResidualLoadCurvesForTwoCountriesRole extends AbstractRole<DecarbonizationModel>
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

        logger.warn("0. Determining the residual load duration curve");

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
        int INTERCONNECTOR = columnIterator;
        columnIterator++;

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

        double interConnectorCapacity = reps.template.findAll(Interconnector.class).iterator().next()
                .getCapacity(clearingTick);

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
        m.viewColumn(INTERCONNECTOR).assign(-interConnectorCapacity);

        logger.debug("First 10 values of matrix: \n " + m.viewPart(0, 0, 10, m.columns()).toString());

        // 2. Build national load curves, by adding up grid node load curves in
        // each zone.
        // also fill the residual load columns with the initial load curves.
        // for now simply multiplied with the market wide growth factor
        for (Zone zone : zoneList) {
            YearlySegmentClearingPointMarketInformation info = reps.yearlySegmentClearingPointMarketInformationRepository
                    .findMarketInformationForMarketAndTime(getCurrentTick(),
                            reps.marketRepository.findElectricitySpotMarketForZone(zone));
            ///////////////////////////////////////////////////////////////////////////////////////////////
            // We changed the hourlyArray from demand to supply. We need to find
            // a way to make generation growing
            ////////////////////////////////////////////////////////////////////////////////////////////////
            DoubleMatrix1D hourlyArray = new DenseDoubleMatrix1D(info.getMarketSupply());
            DoubleMatrix1D priceArray = new DenseDoubleMatrix1D(info.getMarketPrice());
            double growthRate = reps.marketRepository.findElectricitySpotMarketForZone(zone).getDemandGrowthTrend()
                    .getValue(clearingTick);
            DoubleMatrix1D growthFactors = hourlyArray.copy();
            growthFactors.assign(growthRate);
            hourlyArray.assign(growthFactors, Functions.mult);
            m.viewColumn(LOADINZONE.get(zone)).assign(hourlyArray, Functions.plus);
            m.viewColumn(RLOADINZONE.get(zone)).assign(hourlyArray, Functions.plus);
            m.viewColumn(PRICEFORZONE.get(zone)).assign(priceArray, Functions.plus);

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

        int noSegments = (int) reps.segmentRepository.count();

        double[] upperBoundSplit = new double[noSegments];

        if (hoursWithoutZeroRLoad > 8750) {
            for (int i = 0; i < noSegments; i++) {
                upperBoundSplit[i] = max - (((double) (i)) / noSegments * (max - min));
            }
        } else {
            for (int i = 0; i < (noSegments - 1); i++) {
                upperBoundSplit[i] = max - (((double) (i)) / (noSegments - 1) * (max - min));
            }
            upperBoundSplit[noSegments - 1] = 0;
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

        DynamicBin1D[] segmentInterConnectorBins = new DynamicBin1D[noSegments];
        for (int i = 0; i < noSegments; i++) {
            segmentInterConnectorBins[i] = new DynamicBin1D();
        }

        Map<Zone, DynamicBin1D[]> segmentRloadBinsByZone = new HashMap<Zone, DynamicBin1D[]>();
        Map<Zone, DynamicBin1D[]> segmentLoadBinsByZone = new HashMap<Zone, DynamicBin1D[]>();
        Map<Zone, DynamicBin1D[]> segmentPriceBinsByZone = new HashMap<Zone, DynamicBin1D[]>();

        for (Zone zone : zoneList) {
            DynamicBin1D[] segmentRloadBinInZone = new DynamicBin1D[noSegments];
            DynamicBin1D[] segmentLoadBinInZone = new DynamicBin1D[noSegments];
            DynamicBin1D[] segmentPriceBinInZone = new DynamicBin1D[noSegments];
            for (int i = 0; i < noSegments; i++) {
                segmentRloadBinInZone[i] = new DynamicBin1D();
                segmentLoadBinInZone[i] = new DynamicBin1D();
                segmentPriceBinInZone[i] = new DynamicBin1D();
            }
            segmentRloadBinsByZone.put(zone, segmentRloadBinInZone);
            segmentLoadBinsByZone.put(zone, segmentLoadBinInZone);
            segmentPriceBinsByZone.put(zone, segmentPriceBinInZone);
        }

        // Assign hours and load to bins and segments
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
                segmentLoadBinsByZone.get(zone)[currentSegmentID - 1].add(m.get(row, LOADINZONE.get(zone)));
                segmentPriceBinsByZone.get(zone)[currentSegmentID - 1].add(m.get(row, PRICEFORZONE.get(zone)));
            }
            segmentInterConnectorBins[currentSegmentID - 1].add(m.get(row, INTERCONNECTOR));
            hoursAssignedToCurrentSegment++;
        }

        // Assign hours to segments according to residual load in this country.
        // Only for error estimation purposes

        for (Zone zone : zoneList) {
            currentSegmentID = 1;
            double minInZone = m.viewColumn(RLOADINZONE.get(zone)).aggregate(Functions.min, Functions.identity);
            double maxInZone = m.viewColumn(RLOADINZONE.get(zone)).aggregate(Functions.max, Functions.identity);

            // double minPriceInZone =
            // m.viewColumn(PRICEFORZONE.get(zone)).aggregate(Functions.min,
            // Functions.identity);
            // double maxPriceInZone =
            // m.viewColumn(PRICEFORZONE.get(zone)).aggregate(Functions.max,
            // Functions.identity);

            double[] upperBoundSplitInZone = new double[noSegments];
            // double[] upperBoundPriceSplitInZone = new double[noSegments];

            for (int i = 0; i < noSegments; i++) {
                upperBoundSplitInZone[i] = maxInZone - (((double) (i)) / noSegments * (maxInZone - minInZone));
                // upperBoundPriceSplitInZone[i] = maxPriceInZone - (((double)
                // (i)) / noSegments * (maxPriceInZone - minPriceInZone));
            }

            m = m.viewSorted(RLOADINZONE.get(zone)).viewRowFlip();
            int hoursInDifferentSegment = 0;
            double averageSegmentDeviation = 0;
            hoursAssignedToCurrentSegment = 0;
            for (int row = 0; row < m.rows() && currentSegmentID <= noSegments; row++) {
                while (currentSegmentID < noSegments && hoursAssignedToCurrentSegment > 0
                        && m.get(row, RLOADINZONE.get(zone)) <= upperBoundSplitInZone[currentSegmentID]) {
                    currentSegmentID++;
                    hoursAssignedToCurrentSegment = 0;
                }
                m.set(row, SEGMENTFORZONE.get(zone), currentSegmentID);
                if (currentSegmentID != m.get(row, SEGMENT)) {
                    hoursInDifferentSegment++;
                    averageSegmentDeviation += Math.abs(currentSegmentID - m.get(row, SEGMENT));
                }
                hoursAssignedToCurrentSegment++;
            }
            if (hoursInDifferentSegment != 0) {
                averageSegmentDeviation = averageSegmentDeviation / hoursInDifferentSegment;
                averageSegmentDeviation = averageSegmentDeviation * 1000;
                averageSegmentDeviation = Math.round(averageSegmentDeviation);
                averageSegmentDeviation = averageSegmentDeviation / 1000;
                logger.warn("For " + zone + ", " + hoursInDifferentSegment
                        + " hours would have been in different segments, and on average " + averageSegmentDeviation
                        + " Segments away from the segment they were in.");
            } else {
                logger.warn("For " + zone + ", all hours were in the same segment, as for combined sorting!");
            }

        }

        // m = m.viewSorted(RLOADTOTAL).viewRowFlip();
        //
        // logger.debug("First 30 values of matrix: \n " + m.viewPart(0, 0, 30,
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

        it = 1;
        for (DynamicBin1D bin : segmentInterConnectorBins) {
            // logger.warn("Segment " + it + "\n Size: " + bin.size() +
            // "\n Mean IntCapacity~: "
            // + Math.round(bin.mean()) + "\n Max IntCapacity~: " +
            // Math.round(bin.max())
            // + "\n Min IntCapacity~: " + Math.round(bin.min()) +
            // "\n STD IntCapacity~: "
            // + Math.round(bin.standardDeviation()));
            it++;
        }

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

        // 8. Store the segment duration and the average load in that segment
        // per country.

        Iterable<SegmentLoad> segmentLoads = reps.segmentLoadRepository.findAll();
        for (SegmentLoad segmentLoad : segmentLoads) {
            Segment segment = segmentLoad.getSegment();
            // System.out.println(segmentLoad.toString());
            Zone zone = segmentLoad.getElectricitySpotMarket().getZone();
            double demandGrowthFactor = reps.marketRepository.findElectricitySpotMarketForZone(zone)
                    .getDemandGrowthTrend().getValue(clearingTick);
            segmentLoad.setResidualGLDC(segmentRloadBinsByZone.get(zone)[segment.getSegmentID() - 1].mean());
            segmentLoad
                    .setResidualGLDCSegmentPrice(segmentPriceBinsByZone.get(zone)[segment.getSegmentID() - 1].mean());
            logger.warn("Segment " + segment.getSegmentID() + ": " + segmentLoad.getBaseLoad() + "MW" + "Segment Price "
                    + segmentLoad.getResidualGLDCSegmentPrice() + segmentLoad.getElectricitySpotMarket().toString());
        }

        Iterable<Segment> segments = reps.segmentRepository.findAll();
        for (Segment segment : segments) {
            // System.out.println(segment.toString());
            // segment.setLengthInHours(segmentRloadBins[segment.getSegmentID()
            // - 1].size());
            segment.setLengthInHoursGLDCForInvestmentRole(segmentRloadBins[segment.getSegmentID() - 1].size());
            // logger.warn("Segment " + segment.getSegmentID() + ": " +
            // segment.getLengthInHoursGLDCForInvestmentRole()
            // + "hours");
        }

    }

    public Reps getReps() {
        return reps;
    }

}
