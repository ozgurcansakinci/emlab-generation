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
package emlab.gen.role.capacitymarket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import emlab.gen.domain.contract.CashFlow;
import emlab.gen.domain.market.capacity.CapacityClearingPoint;
import emlab.gen.domain.market.capacity.CapacityDispatchPlan;
import emlab.gen.domain.market.capacity.CapacityMarket;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.repository.Reps;
import emlab.gen.role.market.AbstractMarketRole;

//import org.springframework.data.neo4j.annotation.NodeEntity;

/**
 * @author Kaveri
 * 
 */
@RoleComponent
public class PaymentFromConsumerToProducerForCapacityRole extends AbstractMarketRole<CapacityMarket>
        implements Role<CapacityMarket> {

    @Autowired
    Reps reps;

    @Override
    @Transactional
    public void act(CapacityMarket capacityMarket) {

        for (CapacityDispatchPlan plan : reps.capacityMarketRepository
                .findAllAcceptedCapacityDispatchPlansForTime(capacityMarket, getCurrentTick())) {

            // if (plan.getPlant() == null) {
            // logger.warn("Bid Accepted for " + plan.getStorage().getName() + "
            // Status: " + plan.getStatus()
            // + " Amount: " + plan.getAcceptedAmount() + " Price: " +
            // plan.getPrice() + " "
            // + plan.getBiddingMarket());
            // } else {
            //
            // logger.warn("Bid Accepted for " + plan.getPlant() + " Status: " +
            // plan.getStatus() + " Amount: "
            // + plan.getAcceptedAmount() + " Price: " + plan.getPrice() + " " +
            // plan.getBiddingMarket());
            // }

            CapacityClearingPoint capacityClearingPoint = reps.capacityMarketRepository
                    .findOneCapacityClearingPointForTimeAndMarket(capacityMarket, getCurrentTick());

            // logger.warn("We are at tick " + getCurrentTick());
            // logger.warn("capacity clearing point " +
            // capacityClearingPoint.getPrice());
            // double price = capacityClearingPoint.getPrice();

            ElectricitySpotMarket esm = reps.marketRepository
                    .findElectricitySpotMarketForZone(capacityMarket.getZone());

            // logger.warn("esm " + esm.getName());

            // reps.nonTransactionalCreateRepository.createCashFlow(esm,
            // plan.getBidder(),
            // plan.getAcceptedAmount() * capacityClearingPoint.getPrice(),
            // CashFlow.SIMPLE_CAPACITY_MARKET,
            // getCurrentTick(), plan.getPlant());

            if (plan.getPlant() == null) {
                reps.nonTransactionalCreateRepository.createCashFlowStorage(esm, plan.getBidder(),
                        plan.getAcceptedAmount() * capacityClearingPoint.getPrice(),
                        CashFlow.SIMPLE_CAPACITY_MARKET_STORAGE, getCurrentTick(), plan.getStorage());

                // logger.warn("Storage Payment made");

            } else {
                reps.nonTransactionalCreateRepository.createCashFlow(esm, plan.getBidder(),
                        plan.getAcceptedAmount() * capacityClearingPoint.getPrice(), CashFlow.SIMPLE_CAPACITY_MARKET,
                        getCurrentTick(), plan.getPlant());

                // logger.warn("Plant Payment made");
            }

            if (capacityMarket.getRegulator().isCrossBorderTradeAllowed()) {

                double crossBorderCapacity = capacityMarket.getRegulator().getCrossBorderContractedCapacity();

                if (crossBorderCapacity < 0) {

                    reps.nonTransactionalCreateRepository.createCashFlow(esm, null,
                            (Math.abs(crossBorderCapacity)) * capacityClearingPoint.getPrice(),
                            CashFlow.CROSS_BORDER_CONTRACTED_CAPACITY_PAYMENT, getCurrentTick(), null);

                } else if (crossBorderCapacity > 0) {

                    reps.nonTransactionalCreateRepository.createCashFlow(null, esm,
                            crossBorderCapacity * capacityClearingPoint.getPrice(),
                            CashFlow.CROSS_BORDER_CONTRACTED_CAPACITY_PAYMENT, getCurrentTick(), null);

                }
            }

            // logger.warn("Cash flow from consumer {} to Producer {} of value
            // {} "
            // + plan.getAcceptedAmount()
            // * capacityClearingPoint.getPrice(), plan.getBidder(),
            // capacityMarket.getConsumer());
        }

        // try {
        // Thread.sleep(10000);
        // } catch (InterruptedException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }

    }

    /*
     * (non-Javadoc)
     * 
     * @see emlab.gen.role.market.AbstractMarketRole#getReps()
     */
    @Override
    public Reps getReps() {

        return reps;

    }

}