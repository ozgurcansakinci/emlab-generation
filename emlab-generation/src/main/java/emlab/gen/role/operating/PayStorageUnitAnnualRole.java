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
import emlab.gen.domain.agent.BigBank;
import emlab.gen.domain.agent.DecarbonizationModel;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.PowerPlantMaintainer;
import emlab.gen.domain.contract.CashFlow;
import emlab.gen.domain.contract.Loan;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.YearlySegmentClearingPointMarketInformation;
import emlab.gen.domain.technology.EnergyStorageTechnology;
import emlab.gen.repository.Reps;
import emlab.gen.role.AbstractEnergyProducerRole;

/**
 * @author asmkhan
 * 
 *         Energy producer pays for the carbon emisssions to the government
 *
 */
@RoleComponent
public class PayStorageUnitAnnualRole extends AbstractEnergyProducerRole implements Role<EnergyProducer> {

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

        PowerPlantMaintainer maintainer = reps.genericRepository.findFirst(PowerPlantMaintainer.class);

        EnergyStorageTechnology storageTech = reps.energyProducerRepository
                .findStorageTechnologyForEnergyProducer(producer);

        double omCost = storageTech.getFixedOperationAndMaintainanceCostTimeSeriesForStoragePerMWh()
                .getValue(getCurrentTick()) * storageTech.getCurrentMaxStorageCapacity();

        double money = calculateYearlyStorageRevenue(info) - calculateYearlyStorageExpenses(info);

        if (money != 0) {

            CashFlow cf = reps.nonTransactionalCreateRepository.createCashFlowStorage(operatingMarket, producer, money,
                    CashFlow.STORAGE, getCurrentTick(), storageTech);

            logger.info("Cash flow created for storage: {}", cf);
        }

        CashFlow cf_om = reps.nonTransactionalCreateRepository.createCashFlowStorage(producer, maintainer, omCost,
                CashFlow.STORAGE_OM, getCurrentTick(), storageTech);

        logger.warn("money={}", money);
        logger.info("Cash flow created for storage O&M cost: {}", cf_om);
        logger.warn("O&M cost={}", omCost);

        if (getCurrentTick() == 0) {

            double amount = storageTech.getCurrentMaxStorageCapacity()
                    * storageTech.getFixedCapitalCostTimeSeriesForStoragePerMWh().getValue(getCurrentTick());

            BigBank bigbank = reps.genericRepository.findFirst(BigBank.class);

            DecarbonizationModel model = reps.genericRepository.findFirst(DecarbonizationModel.class);

            Loan loan = reps.loanRepository.createLoanStorage(producer, bigbank, amount,
                    (long) model.getSimulationLength(), getCurrentTick(), storageTech);

            double amountPerPayment = determineLoanAnnuities(amount, model.getSimulationLength() - 1,
                    producer.getLoanInterestRate());

            loan.setAmountPerPayment(amountPerPayment);
            loan.setNumberOfPaymentsDone(0);
            storageTech.setLoan(loan);
            storageTech.persist();
            loan.persist();

        } else {
            Loan loan = storageTech.getLoan();

            if (loan.getNumberOfPaymentsDone() < loan.getTotalNumberOfPayments()) {

                double payment = loan.getAmountPerPayment();

                reps.nonTransactionalCreateRepository.createCashFlowStorage(producer, loan.getTo(), payment,
                        CashFlow.LOAN, getCurrentTick(), storageTech);

                loan.setNumberOfPaymentsDone(loan.getNumberOfPaymentsDone() + 1);

                logger.info("Paying {} (euro) for storage loan {}", payment, loan);

                logger.info("Number of payments done for storage {}, total needed: {}", loan.getNumberOfPaymentsDone(),
                        loan.getTotalNumberOfPayments());
            }
        }
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