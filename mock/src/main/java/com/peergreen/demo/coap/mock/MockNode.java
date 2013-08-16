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

import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
import org.ow2.util.log.Log;
import org.ow2.util.log.LogFactory;

import com.peergreen.demo.coap.Node;

/**
 * Mock node for testing
 * @author Florent Benoit
 */
@Component(propagation=true)
@Provides(specifications={Node.class})
public class MockNode implements Node, Runnable {

    @Requires(from="com.peergreen.demo.coap.mock.MockSensor")
    private Factory sensorFactory;

    /**
     * Logger.
     */
    private final Log logger = LogFactory.getLog(MockNode.class);

    @Property(name="uri", mandatory=true)
    @ServiceProperty(name="uri", mandatory=true)
    private String uri;

    @Property(name="manufacturer", mandatory=true)
    private String manufacturer;

    @Property(name="owner", mandatory=true)
    private String owner;

    @Property(name="model", mandatory=true)
    private String model;

    @Property(name="os", mandatory=true)
    private String os;

    @Property(name="nbSensors", mandatory=true)
    private int nbSensors;

    private final List<ComponentInstance> instances;

    public MockNode() {
        this.instances = new CopyOnWriteArrayList<>();
    }

    @Override
    public String getURI() {
        return uri;
    }

    @Override
    public String getManufacturer() {
        return manufacturer;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public String getOS() {
        return os;
    }

    @Override
    public int getSensors() {
        return nbSensors;
    }

    @Validate
    protected void init() {
        createSensors();
    }

    @Invalidate
    protected void Stop() {
        for (ComponentInstance instance : instances) {
            instance.dispose();
        }
    }


    protected void createSensors() {
        Random r = new Random(System.nanoTime());

        for (int i=0; i < nbSensors; i++) {

            SensorEnum[] sensorValues = SensorEnum.values();
            // Get a random sensor
            int rand = r.nextInt(4);

            // build a random sensor
            SensorEnum sensor = sensorValues[rand];

            // build the instance of the sensor
            Properties props = new Properties();
            props.put("uri", uri);
            props.put("sensorID", String.valueOf(i));
            props.put("sensorName", sensor.sensorName());
            props.put("unit", sensor.unit());
            props.put("location", "mock");
            props.put("rangeMin", String.valueOf(sensor.rangeMin()));
            props.put("rangeMax", String.valueOf(sensor.rangeMax()));

            ComponentInstance instance;
            try {
                instance = sensorFactory.createComponentInstance(props);
                instances.add(instance);
            } catch (UnacceptableConfiguration | MissingHandlerException | ConfigurationException e) {
                System.err.println("Unable to create the instance component for the given sensor");
            }
        }
    }

    @Override
    public void run() {

    }


}
