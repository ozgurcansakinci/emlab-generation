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
package emlab.gen.trend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.transaction.annotation.Transactional;

import agentspring.simulation.SimulationParameter;
import agentspring.trend.Trend;

@NodeEntity
public class StochasticTrend extends TimeSeriesImpl implements Trend {

    static final Logger logger = LoggerFactory.getLogger(StochasticTrend.class);

    @SimulationParameter(label = "Minimum growth factor per time step")
    private double min;

    @SimulationParameter(label = "Maximum growth factor per time step")
    private double max;

    private double start;

    private double currentValue;

    private String name;

    @Override
    @Transactional
    public double getValue(long time) {
        setCurrentValue((min + Math.random() * (max - min)));
        return getCurrentValue();
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getStart() {
        return start;
    }

    public void setStart(double start) {
        this.start = start;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
