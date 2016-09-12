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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import emlab.gen.domain.contract.CashFlow;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.PpdpAnnual;
import emlab.gen.domain.market.electricity.YearlySegmentClearingPointMarketInformation;
import emlab.gen.repository.Reps;

/**
 * Accept PpdpAnnual's, make necessary payments
 * 
 * @author Ozgur & Salman
 *
 */
@RoleComponent
public class ProcessAcceptedPPDPAnnualRole extends AbstractMarketRole<ElectricitySpotMarket>
        implements Role<ElectricitySpotMarket> {
    @Autowired
    Reps reps;

    @Override
    @Transactional
    public void act(ElectricitySpotMarket esm) {

        YearlySegmentClearingPointMarketInformation info = reps.yearlySegmentClearingPointMarketInformationRepository
                .findMarketInformationForMarketAndTime(getCurrentTick(), esm);

        for (PpdpAnnual plan : reps.ppdpAnnualRepository.findAllAcceptedPpdpAnnualForGivenMarketAndTime(esm,
                getCurrentTick())) {
            double cash = calculateYearlyPowerPlantRevenue(info, plan);
            plan.setYearlyEmissions();
            // logger.warn("Revenue for Plant: " +
            // plan.getPowerPlant().getName().toString() + " is: " + cash);

            reps.nonTransactionalCreateRepository.createCashFlow(esm, plan.getBidder(), cash, CashFlow.ELECTRICITY_SPOT,
                    getCurrentTick(), plan.getPowerPlant());
        }

    }

    public double calculateYearlyPowerPlantRevenue(YearlySegmentClearingPointMarketInformation info, PpdpAnnual plan) {

        DoubleMatrix1D marginalCostOfGenerationForMarket = new DenseDoubleMatrix1D(info.getMarketPrice());
        DoubleMatrix1D suppliedYearlyElectricityByPowerPlant = new DenseDoubleMatrix1D(plan.getAcceptedHourlyAmount());
        return marginalCostOfGenerationForMarket.zDotProduct(suppliedYearlyElectricityByPowerPlant);
    }

    @Override
    public Reps getReps() {
        return reps;
    }
}
