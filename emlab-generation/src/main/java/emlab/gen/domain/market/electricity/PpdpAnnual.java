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
import emlab.gen.domain.market.YearlyBid;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.trend.HourlyCSVTimeSeries;

/**
 * @author kaveri
 *
 */

@NodeEntity
public class PpdpAnnual extends YearlyBid {

    @RelatedTo(type = "YEARLY_DISPATCHPLAN", elementClass = YearlySegment.class, direction = Direction.OUTGOING)
    private YearlySegment yearlySegment;

    @RelatedTo(type = "PPDPANNUAL_POWERPLANT", elementClass = PowerPlant.class, direction = Direction.OUTGOING)
    private PowerPlant powerPlant;

    private double yearlySupply;

    private double yearlyEmissions;

    private double yearlyFuelConsumption;

    public double getYearlySupply() {
        return yearlySupply;
    }

    public void setYearlySupply(double yearlySupply) {
        this.yearlySupply = yearlySupply;
    }

    public double getYearlyEmissions() {
        return yearlyEmissions;
    }

    public void setYearlyEmissions(double yearlyEmissions) {
        this.yearlyEmissions = yearlyEmissions;
    }

    public void setYearlyEmissions() {
        this.yearlyEmissions = this.getYearlySupply() * this.getPowerPlant().calculateEmissionIntensity();
    }

    public double getYearlyFuelConsumption() {
        return yearlyFuelConsumption;
    }

    public void setYearlyFuelConsumption(double yearlyFuelConsumption) {
        this.yearlyFuelConsumption = yearlyFuelConsumption;
    }
    // private HourlyCSVTimeSeries generationInMwh;

    public YearlySegment getYearlySegment() {
        return yearlySegment;
    }

    public void setYearlySegment(YearlySegment yearlySegment) {
        this.yearlySegment = yearlySegment;
    }

    // private HourlyVariableTimeSeries generationInMWh;
    //
    // public HourlyVariableTimeSeries getGenerationInMWh() {
    // return generationInMWh;
    // }
    //
    // public void setGenerationInMWh(HourlyVariableTimeSeries generationInMWh)
    // {
    // this.generationInMWh = generationInMWh;
    // }
    //
    private HourlyCSVTimeSeries availableHourlyAmount;

    public HourlyCSVTimeSeries getAvailableHourlyAmount() {
        return this.availableHourlyAmount;
        // return powerPlant.getActualHourlyNominalCapacity();
    }

    // in hourly amount in MW
    public void setAvailableHourlyAmount(HourlyCSVTimeSeries availableHourlyAmount) {
        this.availableHourlyAmount = availableHourlyAmount;
        // this.powerPlant.setActualHourlyNominalCapacity(availableHourlyAmount);
    }

    public PowerPlant getPowerPlant() {
        return powerPlant;
    }

    public void setPowerPlant(PowerPlant powerPlant) {
        this.powerPlant = powerPlant;
    }

    public double calculateYearlyEmissions() {
        return this.getYearlySupply() * this.getPowerPlant().calculateEmissionIntensity();
    }

    public void specifyNotPersist(PowerPlant plant, EnergyProducer producer, ElectricitySpotMarket market, long time,
            double price, HourlyCSVTimeSeries spotMarketHourlyCapacity, int status) {
        this.setPowerPlant(plant);
        this.setTime(time);
        this.setBidder(producer);
        this.setBiddingMarket(market);
        this.setPrice(price);
        this.setAvailableHourlyAmount(spotMarketHourlyCapacity);
        this.setStatus(status);

    }

    // All transactional methods below are signified by starting with update
    @Transactional
    public void specifyAndPersist(PowerPlant plant, EnergyProducer producer, ElectricitySpotMarket market, long time,
            double price, HourlyCSVTimeSeries series, int status) {
        this.persist();
        this.specifyNotPersist(plant, producer, market, time, price, series, status);

    }

}