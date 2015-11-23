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
import emlab.gen.domain.agent.DecarbonizationModel;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.repository.Reps;
import emlab.gen.util.Utils;
import ilog.concert.IloException;
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

        logger.warn("List of power plants is:   " + powerPlantList);

        try {
            IloCplex cplex = new IloCplex();
        } catch (IloException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

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
