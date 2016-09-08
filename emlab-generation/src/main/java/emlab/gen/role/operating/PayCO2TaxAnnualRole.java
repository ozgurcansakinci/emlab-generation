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
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.Government;
import emlab.gen.domain.agent.NationalGovernment;
import emlab.gen.domain.contract.CashFlow;
import emlab.gen.domain.market.electricity.PpdpAnnual;
import emlab.gen.repository.Reps;
import emlab.gen.role.AbstractEnergyProducerRole;

/**
 * @author asmkhan
 * 
 *         Energy producer pays for the carbon emisssions to the government
 *
 */
@RoleComponent
public class PayCO2TaxAnnualRole extends AbstractEnergyProducerRole implements Role<EnergyProducer> {

    @Autowired
    Reps reps;

    public Reps getReps() {
        return reps;
    }

    @Override
    @Transactional
    public void act(EnergyProducer producer) {
        logger.info("Pay the CO2 tax");

        Government government = reps.genericRepository.findFirst(Government.class);

        for (PpdpAnnual plan : reps.ppdpAnnualRepository.findAllAcceptedPpdpAnnualForGivenProducerAndTime(producer,
                getCurrentTick())) {

            double money = calculateCO2TaxAnnual(plan, false, getCurrentTick());
            CashFlow cf1 = reps.nonTransactionalCreateRepository.createCashFlow(producer, government, money,
                    CashFlow.CO2TAX, getCurrentTick(), plan.getPowerPlant());
            logger.info("Cash flow created: {}", cf1);

            double minCO2Money = calculatePaymentEffictiveCO2NationalMinimumPriceCostAnnual(plan, false,
                    getCurrentTick());
            NationalGovernment nationalGovernment = reps.nationalGovernmentRepository
                    .findNationalGovernmentByPowerPlant(plan.getPowerPlant());
            CashFlow cf2 = reps.nonTransactionalCreateRepository.createCashFlow(producer, nationalGovernment,
                    minCO2Money, CashFlow.NATIONALMINCO2, getCurrentTick(), plan.getPowerPlant());
            logger.info("Cash flow created: {}", cf2);
        }
    }

}
