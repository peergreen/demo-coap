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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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

import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;

/**
 * Component that ask all the router nodes and adds the neighbor that have been found
 * @author Florent Benoit
 */
@Component(propagation=true)
@Provides
public class NodesFinder extends AbsCoapNode implements Runnable {

    /**
     * Logger.
     */
    private final Log logger = LogFactory.getLog(NodesFinder.class);


    @Requires(from="com.peergreen.demo.coap.adapter.DefaultNode")
    private Factory nodeFactory;

    @Property(name="routerUri", mandatory=true)
    @ServiceProperty(name="routerUri", mandatory=true)
    private String routerURI;

    private final Map<String, ComponentInstance> instances;

    private boolean listen = true;


    public NodesFinder() {
        this.instances = new HashMap<>();
    }

    public String getRouteurURI() {
        return routerURI;
    }

    public Request getNeighBorRequest() {
        return buildGetRequest(getRouteurURI().concat("/neighbor"));
    }

    public Request getNeighBorRequestObserver() {
        Request request = getNeighBorRequest();
        request.setOption(new Option(0, OptionNumberRegistry.OBSERVE));
        return request;
    }




    @Validate
    public void init() {
        listen = true;
        Thread t = new Thread(this);
        t.setDaemon(true);
        t.setName("NodesFinder" + routerURI);
        t.start();

    }

    @Override
    public void run() {

        // Check if we've new nodes
        try {
            getNeighBorRequest().execute();
        } catch (IOException e) {
            logger.error("Unable to check if there are new observers", e);
        }


        // Observe new neighbors
        try {
            getNeighBorRequestObserver().execute();
        } catch (IOException e) {
            logger.error("Unable to register an observer", e);
        }

        while (listen) {
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                logger.debug("Unable to wait", e);
            }
        }



    }



    public void createNode(Properties props)  {
        String uri = props.getProperty("uri");

        // already added, ignore
        if (this.instances.containsKey(uri)) {
            return;
        }

        // create instance
        ComponentInstance instance = null;
        try {
            instance = nodeFactory.createComponentInstance(props);
        } catch (UnacceptableConfiguration | MissingHandlerException | ConfigurationException e) {
            logger.error("Unable to create a node instance", e);
        }
        if (instance != null) {
            this.instances.put(uri, instance);
        }
    }


    @Invalidate
    public void stop() {
        listen = false;
        for (ComponentInstance instance : instances.values()) {
            instance.dispose();
        }
    }


    @Override
    public void handleResponse(Response response) {
        String payload = new String(response.getPayload());

        // Check if it contains neighbors
        String[] neighborsIPs = null;

        if (payload.contains("-")) {
            neighborsIPs = payload.split("-");
        } else if (payload.contains(",")) {
            neighborsIPs = payload.split(",");
        }

        // nothing
        if (neighborsIPs == null) {
            return;
        }

        for (String lastNumberIp : neighborsIPs) {

            // ignore this special value as this is not an IP address
            if ("no".equals(lastNumberIp) || "!".equals(lastNumberIp)) {
                continue;
            }

            // Compute IP address of the node
            String host;
            try {
                host = new URI(getRouteurURI()).getHost();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Unable to get Host from router", e);
            }
            String subNetwork = host.substring(0, host.lastIndexOf(":"));
            String neighborIp = subNetwork.concat(":").concat(lastNumberIp).concat("]");
            addNeighbor(neighborIp);
        }
    }


    /**
     * A node has been found in the neighborhood, add it
     * @param ip the ip of the node
     */
    public void addNeighbor(String ip) {
        // well, create an instance

        Properties props = new Properties();
        props.put("uri", "coap://".concat(ip));
        createNode(props);

    }


}
