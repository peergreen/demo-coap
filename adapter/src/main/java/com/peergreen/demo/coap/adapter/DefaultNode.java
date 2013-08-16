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
package com.peergreen.demo.coap.adapter;

import static com.peergreen.demo.coap.adapter.NodeAttributes.MANUFACTURER;
import static com.peergreen.demo.coap.adapter.NodeAttributes.MODEL;
import static com.peergreen.demo.coap.adapter.NodeAttributes.NBSENSOR;
import static com.peergreen.demo.coap.adapter.NodeAttributes.OS;
import static com.peergreen.demo.coap.adapter.NodeAttributes.OWNER;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.ow2.util.log.Log;
import org.ow2.util.log.LogFactory;

import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;

import com.peergreen.demo.coap.Node;
import com.peergreen.demo.coap.Observable;
import com.peergreen.demo.coap.Sensor;

/**
 * A node will be responsible to create and register sensors which are plugged on this Node.
 *  A node is also providing data about itself
 * @author Florent Benoit
 */
@Component(propagation=true)
@Provides(specifications=Node.class)
public class DefaultNode extends AbsCoapNode implements Node, Runnable {

    /**
     * Logger.
     */
    private final Log logger = LogFactory.getLog(DefaultNode.class);

    @Requires(from="com.peergreen.demo.coap.adapter.DefaultSensor")
    private Factory sensorFactory;

    @Property(name="uri", mandatory=true)
    @ServiceProperty(name="uri", mandatory=true)
    private String uri;

    private String manufacturer;

    private String owner;

    private String model;

    private String os;

    private int nbSensors;

    private final List<ComponentInstance> instances;

    private final Map<String, Sensor> sensors;


    private final List<Observable> observables;

    private boolean listen = true;

    @ServiceController(value=false)
    private boolean controller;

    private int receivedAttributes = 0;


    @Override
    public String getURI() {
        return uri;
    }


    public DefaultNode() {
        this.instances = new ArrayList<ComponentInstance>();
        this.sensors = new HashMap<String, Sensor>();
        this.observables = new CopyOnWriteArrayList<>();
    }


    protected Request buildInfoRequest(String property) {
        return buildGetRequest(uri.concat("/admin/info?get=".concat(property)));
    }

    protected Request buildSensorObservableRequest() {
        Request request = buildGetRequest(uri.concat("/sensor"));
        request.setOption(new Option(0, OptionNumberRegistry.OBSERVE));
        return request;
    }


    @Bind(aggregate=true, optional=true)
    public void bindSensor(Sensor sensor) {
        sensors.put(sensor.getName(), sensor);

    }

    @Unbind(aggregate=true, optional=true)
    public void unbindSensor(Sensor sensor) {
        sensors.remove(sensor.getName());
    }


    @Bind(aggregate=true, optional=true)
    public void bindObservable(Observable observable) {
        observables.add(observable);

    }

    @Unbind(aggregate=true, optional=true)
    public void unbindObservable(Observable observable) {
        observables.remove(observable);
    }

    @Validate
    public void init() {
        this.receivedAttributes = 0;
        this.controller = false;
        listen = true;
        Thread t = new Thread(this);
        t.setDaemon(true);
        t.setName("DefaultNode" + uri);
        t.start();

    }

    @Override
    public void run() {

        // Get data
        for (NodeAttributes nodeAttribute : NodeAttributes.values()) {
            try {
                buildInfoRequest(nodeAttribute.attribute()).execute();
            } catch (IOException e) {
                logger.error("Unable to get node attributes", e);
            }
        }

        // Wait 10 seconds before registering as observable
        try {
            Thread.sleep(10000L);
        } catch (InterruptedException e) {
            logger.debug("Unable to wait", e);
        }

        // Be notified when a sensor is pushing values
        try {
            buildSensorObservableRequest().execute();
        } catch (IOException e) {
            logger.error("Unable to register as observable", e);
        }


        while (listen) {
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                logger.debug("Unable to wait", e);
            }
        }
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

    @Override
    public void handleResponse(Response response) {
        // reject when this is not a JSON format
        String payload = response.getPayloadString();
        if (!payload.startsWith("{") && !payload.endsWith("{")) {
            return;
        }

        try {
            JsonParser jsonParser = Json.createParser(new StringReader(payload));
            while (jsonParser.hasNext()) {
                Event event = jsonParser.next();
                if (isKey(MANUFACTURER.attribute(), event, jsonParser)) {
                    receivedAttributes++;
                    this.manufacturer = getValue(jsonParser);
                } else if (isKey(MODEL.attribute(), event, jsonParser)) {
                    this.model = getValue(jsonParser);
                    receivedAttributes++;
                } else if (isKey(NBSENSOR.attribute(), event, jsonParser)) {
                    this.nbSensors = Integer.valueOf(getValue(jsonParser));
                    createSensors();
                } else if (isKey(OS.attribute(), event, jsonParser)) {
                    this.os = getValue(jsonParser);
                    receivedAttributes++;
                } else if (isKey(OWNER.attribute(), event, jsonParser)) {
                    this.owner = getValue(jsonParser);
                    receivedAttributes++;
                } else if (isSensorObserver(event, jsonParser)) {
                    notificationOnSensor(jsonParser.getString(), getValue(jsonParser));
                }
            }
        } catch (Exception e) {
            System.err.println("Error while analyzing the payload " + payload);
            // ignore
            e.printStackTrace();
        }
        if (receivedAttributes >= 4 && !controller) {
            controller = true;
        }

    }

    protected void notificationOnSensor(String sensorName, String value) {
        // Do not make push
        /*Sensor sensor = sensors.get(sensorName);
        if (sensor != null) {
            for (Observable observable : observables) {
                observable.update(sensor);
            }
        }*/
    }

    protected boolean isSensorObserver(Event event, JsonParser jsonParser) {
        for (SensorNames sensorName : SensorNames.values()) {
            if (isKey(sensorName.attribute(), event, jsonParser)) {
                return true;
            }
        }
        return false;
    }

    @Invalidate
    public void stop() {
        listen = false;
        controller = false;
        for (ComponentInstance instance : instances) {
            instance.dispose();
        }
    }


    protected void createSensors() {
        for (int i=0; i < nbSensors; i++) {

            // build the instance of the sensor
            DefaultSensor sensor = new DefaultSensor();

            Properties props = new Properties();
            props.put("uri", uri);
            props.put("sensorID", String.valueOf(i));
            props.put("instance.object", sensor);
            ComponentInstance instance;
            try {
                instance = sensorFactory.createComponentInstance(props);
                instances.add(instance);
            } catch (UnacceptableConfiguration | MissingHandlerException | ConfigurationException e) {
                System.err.println("Unable to create the instance component for the given sensor");
            }
        }
    }



}
