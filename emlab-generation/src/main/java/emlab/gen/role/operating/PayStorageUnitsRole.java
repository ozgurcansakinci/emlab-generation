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
package emlab.gen.role.operating;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.contract.CashFlow;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.YearlySegmentClearingPointMarketInformation;
import emlab.gen.repository.Reps;
import emlab.gen.role.AbstractEnergyProducerRole;

/**
 * @author asmkhan
 * 
 *         Energy producer pays for the carbon emisssions to the government
 *
 */
@RoleComponent
public class PayStorageUnitsRole extends AbstractEnergyProducerRole implements Role<EnergyProducer> {

    @Autowired
    Reps reps;

    public Reps getReps() {
        return reps;
    }

    @Override
    @Transactional
    public void act(EnergyProducer producer) {
        logger.info("Make the payments for storage");
        ElectricitySpotMarket operatingMarket = producer.getInvestorMarket();
        YearlySegmentClearingPointMarketInformation info = reps.yearlySegmentClearingPointMarketInformationRepository
                .findMarketInformationForMarketAndTime(getCurrentTick(), operatingMarket);
        double money = calculateYearlyStorageRevenue(info) - calculateYearlyStorageExpenses(info);
        CashFlow cf = reps.nonTransactionalCreateRepository.createCashFlow(operatingMarket, producer, money,
                CashFlow.STORAGE, getCurrentTick(), null);
        logger.info("Cash flow created: {}", cf);
        logger.warn("money={}", money);
    }

    public double calculateYearlyStorageRevenue(YearlySegmentClearingPointMarketInformation info) {
        DoubleMatrix1D marginalCostOfGenerationForMarket = new DenseDoubleMatrix1D(info.getMarketPrice());
        DoubleMatrix1D storageDischargingInMW = new DenseDoubleMatrix1D(info.getStorageDischargingInMW());
        return marginalCostOfGenerationForMarket.zDotProduct(storageDischargingInMW);
    }

    public double calculateYearlyStorageExpenses(YearlySegmentClearingPointMarketInformation info) {
        DoubleMatrix1D marginalCostOfGenerationForMarket = new DenseDoubleMatrix1D(info.getMarketPrice());
        DoubleMatrix1D storageChargingInMW = new DenseDoubleMatrix1D(info.getStorageChargingInMW());
        return marginalCostOfGenerationForMarket.zDotProduct(storageChargingInMW);
    }

}
