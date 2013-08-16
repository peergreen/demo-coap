/**
 * Copyright 2013 Peergreen S.A.S.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.peergreen.demo.coap.mock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
import org.ow2.util.log.Log;
import org.ow2.util.log.LogFactory;

import com.peergreen.demo.coap.Sensor;

/**
 * Mock sensor for testing
 * @author Florent Benoit
 */
@Component(propagation=true)
@Provides
public class MockSensor implements Sensor {

    /**
     * Logger.
     */
    private final Log logger = LogFactory.getLog(MockSensor.class);

    @Property(name="uri", mandatory=true)
    @ServiceProperty(name="uri", mandatory=true)
    private String uri;

    @Property(name="sensorID", mandatory=true)
    @ServiceProperty(name="sensorID", mandatory=true)
    private int sensorID;

    @Property(name="sensorName", mandatory=true)
    @ServiceProperty(name="name")
    private String name;

    @Property(name="unit", mandatory=true)
    @ServiceProperty(name="unit")
    private String unit;

    @Property(name="location", mandatory=true)
    @ServiceProperty(name="location")
    private String location;

    @Property(name="rangeMin", mandatory=true)
    private double rangeMin;

    @Property(name="rangeMax", mandatory=true)
    private double rangeMax;


    private Random random;
    private double value;


    @Validate
    public void init() {
        this.random = new Random(System.currentTimeMillis());
        this.value = getValue();
    }


    @Override
    public String getURI() {
        return uri;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public String getName() {
        return name;
    }

    public double getLastValue() {
        return value;
    }


    @Override
    public double getValue() {
        // return a value in the given range
        this.value = rangeMin + (rangeMax - rangeMin) * random.nextDouble();
        this.value = new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
        return value;
    }

    @Override
    public String getUnit() {
        return unit;
    }

    @Override
    public int getId() {
        return sensorID;
    }


}
