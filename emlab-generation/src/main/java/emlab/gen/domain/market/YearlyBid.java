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
package emlab.gen.domain.market;

import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.transaction.annotation.Transactional;

@NodeEntity
public class YearlyBid extends Bid {

    private double[] acceptedHourlyAmount;
    // private int[] hourlyStatus;

    public double[] getAcceptedHourlyAmount() {
        return acceptedHourlyAmount;
    }

    public void setAcceptedHourlyAmount(double[] acceptedHourlyAmount) {
        this.acceptedHourlyAmount = acceptedHourlyAmount;
    }

    // public int[] getHourlyStatus() {
    // return hourlyStatus;
    // }
    //
    // public void setHourlyStatus(int[] hourlyStatus) {
    // this.hourlyStatus = hourlyStatus;
    // }

    /**
     * Changes the amount of a bid
     *
     * @param bid
     *            the bid to change
     * @param amount
     *            the new amount
     */

    @Transactional
    public void updateHourlyAmount(double[] amount) {
        setAcceptedHourlyAmount(amount);
        this.persist();
    }

    /**
     * Changes the status of a bid
     *
     * @param bid
     *            the bid to change
     * @param status
     *            the new status
     */
    @Override
    @Transactional
    public void updateStatus(int status) {
        setStatus(status);
    }

    @Override
    public String toString() {
        return "for " + getBiddingMarket() + " price: " + getPrice() + " isSupply: " + isSupplyBid();
    }
}
