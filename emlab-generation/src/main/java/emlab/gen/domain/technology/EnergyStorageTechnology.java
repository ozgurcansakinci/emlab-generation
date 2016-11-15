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
package emlab.gen.domain.technology;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.contract.Loan;
import emlab.gen.trend.TimeSeriesImpl;

@NodeEntity
public class EnergyStorageTechnology {

    private String name;

    // added a new class for storage

    @RelatedTo(type = "STORAGE_OWNER", elementClass = EnergyProducer.class, direction = Direction.OUTGOING)
    private EnergyProducer ownerStorageUnit;

    @RelatedTo(type = "STORAGE_CAPITALCOSTS", elementClass = TimeSeriesImpl.class, direction = Direction.OUTGOING)
    private TimeSeriesImpl fixedCapitalCostTimeSeriesForStoragePerMWh;

    @RelatedTo(type = "STORAGE_OMCOSTS", elementClass = TimeSeriesImpl.class, direction = Direction.OUTGOING)
    private TimeSeriesImpl fixedOperationAndMaintainanceCostTimeSeriesForStoragePerMWh;

    @RelatedTo(type = "INFLOW_EFFICIENCYTS", elementClass = TimeSeriesImpl.class, direction = Direction.OUTGOING)
    private TimeSeriesImpl efficiencyInFlowTimeSeries;

    @RelatedTo(type = "OUTFLOW_EFFICIENCYTS", elementClass = TimeSeriesImpl.class, direction = Direction.OUTGOING)
    private TimeSeriesImpl efficiencyOutFlowTimeSeries;

    @RelatedTo(type = "STORAGE_LOAN", elementClass = Loan.class, direction = Direction.OUTGOING)
    private Loan loan;

    private double baseMaxStorageCapacity;

    private double currentMaxStorageCapacity;

    private double baseMaxStorageChargingRate;

    private double currentMaxStorageChargingRate;

    private double baseMaxStorageDischargingRate;

    private double currentMaxStorageDischargingRate;

    private double initialStateOfChargeInStorage;

    private double finalStateOfChargeInStorage;

    private double marginalCostOfCharging;

    private double marginalCostOfDischarging;

    private double storageInvestmentCalibrator;

    private double percentageCMBidding;

    public double getStorageInvestmentCalibrator() {
        return storageInvestmentCalibrator;
    }

    public void setStorageInvestmentCalibrator(double storageInvestmentCalibrator) {
        this.storageInvestmentCalibrator = storageInvestmentCalibrator;
    }

    public double getPercentageCMBidding() {
        return percentageCMBidding;
    }

    public void setPercentageCMBidding(double percentageCMBidding) {
        this.percentageCMBidding = percentageCMBidding;
    }

    public double getMarginalCostOfCharging() {
        return marginalCostOfCharging;
    }

    public void setMarginalCostOfCharging(double marginalCostOfCharging) {
        this.marginalCostOfCharging = marginalCostOfCharging;
    }

    public double getMarginalCostOfDischarging() {
        return marginalCostOfDischarging;
    }

    public void setMarginalCostOfDischarging(double marginalCostOfDischarging) {
        this.marginalCostOfDischarging = marginalCostOfDischarging;
    }

    public double getInitialStateOfChargeInStorage() {
        return initialStateOfChargeInStorage;
    }

    public void setInitialStateOfChargeInStorage(double initialStateOfChargeInStorage) {
        this.initialStateOfChargeInStorage = initialStateOfChargeInStorage;
    }

    public double getFinalStateOfChargeInStorage() {
        return finalStateOfChargeInStorage;
    }

    public void setFinalStateOfChargeInStorage(double finalStateOfChargeInStorage) {
        this.finalStateOfChargeInStorage = finalStateOfChargeInStorage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EnergyProducer getOwnerStorageUnit() {
        return ownerStorageUnit;
    }

    public void setOwnerStorageUnit(EnergyProducer ownerStorageUnit) {
        this.ownerStorageUnit = ownerStorageUnit;
    }

    public TimeSeriesImpl getFixedCapitalCostTimeSeriesForStoragePerMWh() {
        return fixedCapitalCostTimeSeriesForStoragePerMWh;
    }

    public void setFixedCapitalCostTimeSeriesForStoragePerMWh(
            TimeSeriesImpl fixedCapitalCostTimeSeriesForStoragePerMWh) {
        this.fixedCapitalCostTimeSeriesForStoragePerMWh = fixedCapitalCostTimeSeriesForStoragePerMWh;
    }

    public TimeSeriesImpl getFixedOperationAndMaintainanceCostTimeSeriesForStoragePerMWh() {
        return fixedOperationAndMaintainanceCostTimeSeriesForStoragePerMWh;
    }

    public void setFixedOperationAndMaintainanceCostTimeSeriesForStoragePerMWh(
            TimeSeriesImpl fixedOperationAndMaintainanceCostTimeSeriesForStoragePerMWh) {
        this.fixedOperationAndMaintainanceCostTimeSeriesForStoragePerMWh = fixedOperationAndMaintainanceCostTimeSeriesForStoragePerMWh;
    }

    public TimeSeriesImpl getEfficiencyInFlowTimeSeries() {
        return efficiencyInFlowTimeSeries;
    }

    public void setEfficiencyInFlowTimeSeries(TimeSeriesImpl efficiencyInFlowTimeSeries) {
        this.efficiencyInFlowTimeSeries = efficiencyInFlowTimeSeries;
    }

    public TimeSeriesImpl getEfficiencyOutFlowTimeSeries() {
        return efficiencyOutFlowTimeSeries;
    }

    public void setEfficiencyOutFlowTimeSeries(TimeSeriesImpl efficiencyOutFlowTimeSeries) {
        this.efficiencyOutFlowTimeSeries = efficiencyOutFlowTimeSeries;
    }

    public double getBaseMaxStorageCapacity() {
        return baseMaxStorageCapacity;
    }

    public void setBaseMaxStorageCapacity(double baseMaxStorageCapacity) {
        this.baseMaxStorageCapacity = baseMaxStorageCapacity;
    }

    public double getCurrentMaxStorageCapacity() {
        return currentMaxStorageCapacity;
    }

    public void setCurrentMaxStorageCapacity(double currentMaxStorageCapacity) {
        this.currentMaxStorageCapacity = currentMaxStorageCapacity;
    }

    public double getBaseMaxStorageChargingRate() {
        return baseMaxStorageChargingRate;
    }

    public void setBaseMaxStorageChargingRate(double baseMaxStorageChargingRate) {
        this.baseMaxStorageChargingRate = baseMaxStorageChargingRate;
    }

    public double getCurrentMaxStorageChargingRate() {
        return currentMaxStorageChargingRate;
    }

    public void setCurrentMaxStorageChargingRate(double currentMaxStorageChargingRate) {
        this.currentMaxStorageChargingRate = currentMaxStorageChargingRate;
    }

    public double getBaseMaxStorageDischargingRate() {
        return baseMaxStorageDischargingRate;
    }

    public void setBaseMaxStorageDischargingRate(double baseMaxStorageDischargingRate) {
        this.baseMaxStorageDischargingRate = baseMaxStorageDischargingRate;
    }

    public double getCurrentMaxStorageDischargingRate() {
        return currentMaxStorageDischargingRate;
    }

    public void setCurrentMaxStorageDischargingRate(double currentMaxStorageDischargingRate) {
        this.currentMaxStorageDischargingRate = currentMaxStorageDischargingRate;
    }

    public Loan getLoan() {
        return loan;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }

}