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

import java.util.ArrayList;

import ilog.concert.IloNumVar;

/**
 * @author asmkhan
 *
 */

public class Plant {

    public String name;
    public String technology;
    public String owner;
    public double actualNominalCapacity;
    public double mc;
    public String zone;
    public long tick;
    public double emissionsIntensity;
    public double[] availableRESCapacityArray;// changes by emile
    public ArrayList<Double> availableRESCapacity = new ArrayList<Double>(1);
    public ArrayList<IloNumVar> generationCapacityOfPlant = new ArrayList<IloNumVar>(1);
    // IloNumVar[] generationCapacityOfPlant;

    public ArrayList<IloNumVar> getGenerationCapacityOfPlant() {
        return generationCapacityOfPlant;
    }

    public void setGenerationCapacityOfPlant(ArrayList<IloNumVar> generationCapacityOfPlant) {
        this.generationCapacityOfPlant = generationCapacityOfPlant;
    }

    /*
     * public Arraylist<Double> getAvailableRESCapacity() { return
     * availableRESCapacity; }
     *
     * public void setAvailableRESCapacity(Arraylist< double >
     * availableRESCapacity) { // this.availableRESCapacity =
     * availableRESCapacity; // System.arraycopy(availableRESCapacity, 0,
     * this.availableRESCapacity, // 0, availableRESCapacity.length);
     * this.availableRESCapacity = new double[availableRESCapacity.length];
     * this.availableRESCapacity = Arrays.copyOf(availableRESCapacity,
     * availableRESCapacity.length); // for(int i=0; i< availableRES) }
     */

    public ArrayList<Double> getAvailableRESCapacity() {
        return availableRESCapacity;
    }

    public void setAvailableRESCapacity(ArrayList<Double> availableRESCapacity) {
        this.availableRESCapacity = availableRESCapacity;
    }

    public double getEmissionsIntensity() {
        return emissionsIntensity;
    }

    public void setEmissionsIntensity(double emissionsIntensity) {
        this.emissionsIntensity = emissionsIntensity;
    }

    public long getTick() {
        return tick;
    }

    public void setTick(long tick) {
        this.tick = tick;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String energyProducer) {
        this.owner = energyProducer;
    }

    public double getActualNominalCapacity() {
        return actualNominalCapacity;
    }

    public void setActualNominalCapacity(double actualNominalCapacity) {
        this.actualNominalCapacity = actualNominalCapacity;
    }

    public double getMc() {
        return mc;
    }

    public void setMc(double mc) {
        this.mc = mc;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public void setTechnology(String technology) {
        this.technology = technology;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTechnology() {
        return technology;
    }

}
