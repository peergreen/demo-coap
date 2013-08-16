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

import static com.peergreen.demo.coap.adapter.SensorAttributes.LOCATION;
import static com.peergreen.demo.coap.adapter.SensorAttributes.NAME;
import static com.peergreen.demo.coap.adapter.SensorAttributes.UNIT;

import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
import org.ow2.util.log.Log;
import org.ow2.util.log.LogFactory;

import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;

import com.peergreen.demo.coap.Sensor;

@Component(propagation=true)
@Provides(specifications=Sensor.class)
public class DefaultSensor extends AbsCoapNode implements Sensor, Runnable {

    /**
     * time after we check if we've received all the data.
     */
    private static final long CHECK_TIME = 10000L;

    /**
     * Logger.
     */
    private final Log logger = LogFactory.getLog(DefaultSensor.class);

    @Property(name="uri", mandatory=true)
    @ServiceProperty(name="uri", mandatory=true)
    private String uri;

    @Property(name="sensorID", mandatory=true)
    @ServiceProperty(name="sensorID", mandatory=true)
    private int sensorID;

    @ServiceProperty(name="name")
    private String name;

    @ServiceProperty(name="unit")
    private String unit;

    @ServiceProperty(name="location")
    private String location;

    private double value;


    @ServiceController(value=false)
    private boolean controller;


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
        Request request = buildSensorValueRequest();

        ValueHandler handler = new ValueHandler();
        request.registerResponseHandler(handler);

        try {
            request.execute();
        } catch (IOException e) {
            logger.error("Unable to execute get value", e);
        }

        Response response = null;
        try {
            response = handler.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Unable to execute get value", e);
            throw new IllegalStateException("Unable to get value", e);
        }

        if (response == null) {
            throw new IllegalStateException("Unable to get value as there is no answer");
        }


        JsonParser jsonParser = Json.createParser(new StringReader(response.getPayloadString()));
        while (jsonParser.hasNext()) {
            Event event = jsonParser.next();
            if (event == Event.KEY_NAME) {
                String key = jsonParser.getString();
                // we're receiving attributes for the sensor
                if (getName().equals(key)) {
                    this.value = Double.valueOf(getValue(jsonParser));
                }
            }
        }
        return value;
    }

    @Override
    public String getUnit() {
        return unit;
    }


    protected Request buildSensorInfoRequest(String property) {
        return buildGetRequest(uri.concat("/admin/info?get=").concat(String.valueOf(sensorID)).concat("&param=").concat(property));
    }

    protected Request buildSensorValueRequest() {
        return buildDirectGetRequest(uri.concat("/sensor?name=".concat(getName())));
    }

    @Validate
    public void startup() {
        // start a thread
        new Thread(this).start();
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
                if (event == Event.KEY_NAME) {
                    String value = jsonParser.getString();
                    // we're receiving attributes for the sensor
                    if (value.startsWith(String.valueOf(sensorID).concat("."))) {
                        String[] values = value.split("\\.");
                        String attributeName = values[1];
                        if (NAME.attribute().equals(attributeName)) {
                            this.name = getValue(jsonParser);
                        } else if (LOCATION.attribute().equals(attributeName)) {
                            this.location = getValue(jsonParser);
                        } else if (UNIT.attribute().equals(attributeName)) {
                            this.unit = getValue(jsonParser);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error while analyzing the payload {0}", payload, e);
        } finally {

            // Service is now accessible ?
            if (getName() != null && getLocation() != null && getUnit() != null) {
                // flag as available
                this.controller = true;
            }
        }
    }

    @Override
    public void run() {
        // Start a timer to check that we've got all the answers
        check();
    }

    public void check() {

        // Get data
        for (SensorAttributes sensorAttribute : SensorAttributes.values()) {
            try {
                buildSensorInfoRequest(sensorAttribute.attribute()).execute();
            } catch (IOException e) {
                logger.error("Unable to get data of the sensor", e);
            }
        }

        // wait and see if we've been initialized
        try {
            Thread.sleep(CHECK_TIME);
        } catch (InterruptedException e) {
            logger.debug("Unable to wait", e);
        }

        // Not yet ready, perform a new check
        if (!controller) {
            check();
        }
    }

    @Override
    public int getId() {
        return sensorID;
    }


}
