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
package com.peergreen.demo.coap.smartprobe;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

import com.peergreen.demo.coap.Node;
import com.peergreen.demo.coap.Sensor;
import com.peergreen.demo.smartthing.json.ChannelInfo;
import com.peergreen.demo.smartthing.json.DeviceInfo;
import com.peergreen.demo.smartthing.json.SensorInfo;

/**
 * Smart probe is reporting the data collected on the smart thing service
 *
 * @author Florent Benoit
 */
@Component(propagation=true)
@Instantiate
@Provides(specifications=SmartProbe.class)
public class SmartProbe implements Runnable  {

    private final List<Node> devices;
    private final List<Sensor> sensors;

    @ServiceProperty(name="smarthing-uri", mandatory=true)
    private String smartThingUri;

    @ServiceProperty(name="interval", mandatory=true)
    private long interval;

    private final Client client;

    private final List<Node> nodesToAdd;
    private final List<Sensor> sensorsToAdd;



    public SmartProbe() {
        this.devices = new CopyOnWriteArrayList<>();
        this.nodesToAdd  = new CopyOnWriteArrayList<>();

        this.sensors = new CopyOnWriteArrayList<>();
        this.sensorsToAdd = new CopyOnWriteArrayList<>();

        this.client = ClientBuilder.newClient();

    }

    @Validate
    public void start() {
        new Thread(this).start();
    }



    @Bind(aggregate=true, optional=true)
    public void bindDevice(Node device) {
        devices.add(device);

        nodesToAdd.add(device);
    }

    @Unbind(aggregate=true, optional=true)
    public void unbindDevice(Node device) {
        devices.remove(device);
    }

    @Bind(aggregate=true, optional=true)
    public void bindSensor(Sensor sensor) {
        sensors.add(sensor);

        // register sensor
        sensorsToAdd.add(sensor);

    }

    @Unbind(aggregate=true, optional=true)
    public void unbindSensor(Sensor sensor) {
        sensors.remove(sensor);
    }


    @Override
    public void run() {
        while (true) {

            // Add nodes
            addRemainingNodes();

            // Add sensors
            addRemainingSensors();

            Iterator<Sensor> it = sensors.iterator();
            while (it.hasNext()) {
                Sensor sensor = it.next();

                try {
                    double value = sensor.getValue();

                    // publish the value
                    publishData(sensor, value);


                } catch (Exception e) {
                }
            }


            // sleep for the given interval
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    protected String cleanupUri(String input) {
        URI uri;
        try {
            uri = new URI(input);
        } catch (URISyntaxException e) {
            return input;
        }
        return uri.getHost().replace("[", "").replace("]", "");
    }


    protected void addRemainingNodes() {
        // adds the node
        for (Node node : nodesToAdd) {
            URI deviceURI = buildURI(cleanupUri(node.getURI()));

            DeviceInfo deviceInfo = buildDevice(node);
            Response response = client.target(deviceURI).request().put(Entity.entity(deviceInfo.toJSon(), MediaType.APPLICATION_JSON));
            // Node is here, no needs to add it
            if (response.getStatus() == Status.CREATED.getStatusCode() || response.getStatus() == Status.CONFLICT.getStatusCode()) {
                nodesToAdd.remove(node);
            }
        }
    }


    protected void addRemainingSensors() {
        // adds the node
        for (Sensor sensor : sensorsToAdd) {
            SensorInfo sensorInfo = buildSensor(sensor);
            URI sensorURI = buildURI(cleanupUri(sensor.getURI()).concat("/sensors/").concat(sensor.getName()));
            Response response = client.target(sensorURI).request().put(Entity.entity(sensorInfo.toJSon(), MediaType.APPLICATION_JSON));
            // Node is here, no needs to add it
            if (response.getStatus() == Status.CREATED.getStatusCode() || response.getStatus() == Status.CONFLICT.getStatusCode()) {
                // add the channel
                ChannelInfo channelInfo = buildChannel(sensor);
                URI channelURI = buildURI(cleanupUri(sensor.getURI()).concat("/sensors/").concat(sensor.getName()).concat("/channels/").concat(String.valueOf(sensor.getId())));
                Response channelResponse = client.target(channelURI).request().put(Entity.entity(channelInfo.toJSon(), MediaType.APPLICATION_JSON));
                if (channelResponse.getStatus() == Status.CREATED.getStatusCode() || channelResponse.getStatus() == Status.CONFLICT.getStatusCode()) {
                    sensorsToAdd.remove(sensor);
                }
            }
        }
    }

    protected DeviceInfo buildDevice(Node node) {
        return DeviceInfo.newDevice().manufacturer(node.getManufacturer()).model(node.getModel()).os(node.getOS()).owner(node.getOwner()).uri(node.getURI());
    }

    protected SensorInfo buildSensor(Sensor sensor) {
        return SensorInfo.newSensor().location(sensor.getLocation()).name(sensor.getName()).sensorID(String.valueOf(sensor.getId())).uri(sensor.getURI());
    }

    protected ChannelInfo buildChannel(Sensor sensor) {
        return ChannelInfo.newChannel().id(sensor.getId()).unit(sensor.getUnit());
    }




    public void publishData(Sensor sensor, double value) {
        String deviceName = cleanupUri(sensor.getURI());
        String sensorName = sensor.getName();

        URI deviceURI = buildURI(deviceName.concat("/sensors/").concat(sensorName).concat("/channels/").concat(String.valueOf(sensor.getId())).concat("/add/").concat(String.valueOf(value)));
        Response response = client.target(deviceURI).request().get();
    }



    protected URI buildURI(String path) {
        return UriBuilder.fromUri(smartThingUri).path(path).build();
    }




}
