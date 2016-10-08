/*******************************************************************************
 * Copyright 2012 the original author or authors.
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

import agentspring.role.AbstractRole;
import agentspring.role.Role;
import agentspring.role.RoleComponent;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.Stakeholder;
import emlab.gen.domain.contract.CashFlow;
import emlab.gen.repository.Reps;

/**
 * {@link EnergyProducer}s repay their loans
 * 
 * @author asmkhan
 * 
 */
@RoleComponent
public class PayToStakeholderRole extends AbstractRole<EnergyProducer>implements Role<EnergyProducer> {

    @Autowired
    Reps reps;

    @Override
    @Transactional
    public void act(EnergyProducer producer) {

        Stakeholder stakeholder = reps.stakeholderRepository.findStakeholderForEnergyProducer(producer);

        double varibleCashOutFlows = reps.cashFlowRepository
                .calculateVariableCashOutFlowsForEnergyProducerForTime(producer, getCurrentTick());

        logger.warn("varibleCashOutFlows " + varibleCashOutFlows + " for " + producer.getName());

        double fixedCashOutFlows = reps.cashFlowRepository.calculateFixedCashOutFlowsForEnergyProducerForTime(producer,
                getCurrentTick() - 1);

        logger.warn("fixedCashOutFlows " + fixedCashOutFlows);

        double varibleCashInFlows = reps.cashFlowRepository
                .calculateVariableCashInFlowsForEnergyProducerForTime(producer, getCurrentTick());

        logger.warn("varibleCashInFlows " + varibleCashInFlows);

        double totalCashOutFlows = varibleCashOutFlows + fixedCashOutFlows;

        logger.warn("totalCashOutFlows " + totalCashOutFlows);

        double netProfit = varibleCashInFlows - totalCashOutFlows;

        logger.warn("netProfit " + netProfit);

        double returnOnInvestments = (netProfit / fixedCashOutFlows) * 100;

        logger.warn("returnOnInvestments " + returnOnInvestments);

        if (netProfit > 0 && returnOnInvestments > 20) {

            // double shareOfNetProfitForEnergyProducer = 20 /
            // returnOnInvestments;

            double shareOfNetProfitForEnergyProducer = 0.3;

            // logger.warn("shareOfNetProfitForEnergyProducer " +
            // shareOfNetProfitForEnergyProducer);

            double shareOfNetProfitForStakeholder = 1 - shareOfNetProfitForEnergyProducer;

            logger.warn("shareOfNetProfitForStakeholder " + shareOfNetProfitForStakeholder + " for "
                    + stakeholder.getName());

            double amountOfDividentsPayableToStakeholder = shareOfNetProfitForStakeholder * netProfit;

            CashFlow cf_ToStakeholder = reps.nonTransactionalCreateRepository.createCashFlowStorage(producer,
                    stakeholder, amountOfDividentsPayableToStakeholder, CashFlow.STAKEHOLDER_DIVIDEND, getCurrentTick(),
                    null);

            logger.warn("amountOfDividentsPayableToStakeholder " + cf_ToStakeholder.getMoney() + " for " + stakeholder);
        }

    }
}