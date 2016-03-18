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
package emlab.gen.domain.market.electricity;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.transaction.annotation.Transactional;

import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.market.Bid;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.trend.HourlyCSVTimeSeries;
import emlab.gen.trend.HourlyTimeSeries;

/**
 * @author kaveri
 *
 */

@NodeEntity
public class PpdpAnnual extends Bid {

    @RelatedTo(type = "PPDPANNUAL_POWERPLANT", elementClass = PowerPlant.class, direction = Direction.OUTGOING)
    private PowerPlant powerPlant;


    private HourlyCSVTimeSeries generationInMwh;
    private HourlyCSVTimeSeries availableHourlyAmount;

    public HourlyCSVTimeSeries getAvailableHourlyAmount() {
        return availableHourlyAmount;
    }

    public void setAvailableHourlyAmount(HourlyCSVTimeSeries availableHourlyAmount) {
        this.availableHourlyAmount = availableHourlyAmount;
    }

    public PowerPlant getPowerPlant() {
        return powerPlant;
    }

    public void setPowerPlant(PowerPlant powerPlant) {
        this.powerPlant = powerPlant;
    }

    public HourlyTimeSeries getGenerationInMwh() {
        return generationInMwh;
    }

    public void setGenerationInMwh(HourlyCSVTimeSeries generationInMwh) {
        this.generationInMwh = generationInMwh;
    }

    public void specifyNotPersist(PowerPlant plant, EnergyProducer producer, ElectricitySpotMarket market,
            long time,
            double price, double spotMarketCapacity, int status, HourlyCSVTimeSeries series) {
        this.setPowerPlant(plant);
        this.setTime(time);
        this.setBidder(producer);
        this.setBiddingMarket(market);
        this.setPrice(price);
        this.setAmount(spotMarketCapacity);
        this.setStatus(status);
        this.setAvailableHourlyAmount(series);
    }

    // All transactional methods below are signified by starting with update
    @Transactional
    public void specifyAndPersist(PowerPlant plant, EnergyProducer producer, ElectricitySpotMarket market,
 long time,
            double price, double spotMarketCapacity, int status, HourlyCSVTimeSeries series) {
        this.persist();
        this.specifyNotPersist(plant, producer, market, time, price, spotMarketCapacity, status, series);

    }

}
