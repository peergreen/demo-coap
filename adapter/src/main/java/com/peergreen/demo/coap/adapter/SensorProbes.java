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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

import com.peergreen.demo.coap.Observable;
import com.peergreen.demo.coap.Sensor;

/*@Component
@Instantiate
@Provides(specifications=SensorProbes.class)*/
public class SensorProbes implements Runnable  {



    private final List<Observable> observables;

    private final Map<Sensor, Double> sensors = new ConcurrentHashMap<>();


    public SensorProbes() {
        this.observables = new CopyOnWriteArrayList<>();
    }

    @Validate
    public void start() {
        new Thread(this).start();
    }


    @Bind(aggregate=true, optional=true)
    public void bindObservable(Observable observable) {
        observables.add(observable);

    }

    @Unbind(aggregate=true, optional=true)
    public void unbindObservable(Observable observable) {
        observables.remove(observable);
    }


    @Bind(aggregate=true, optional=true)
    public void bindSensor(Sensor sensor) {
        sensors.put(sensor, new Double(0));

    }

    @Unbind(aggregate=true, optional=true)
    public void unbindSensor(Sensor sensor) {
        sensors.remove(sensor);
    }


    @Override
    public void run() {
        while (true) {

            Iterator<Entry<Sensor, Double>> it = sensors.entrySet().iterator();
            while (it.hasNext()) {
                Entry<Sensor, Double> entry = it.next();

                Sensor sensor = entry.getKey();
                Double lastValue = entry.getValue();

                try {
                    double value = sensor.getValue();
                    // only if there is a big change
                    if (Math.abs(value - lastValue) > 0.5) {
                        sensors.put(sensor, value);
                        // notify
                        for (Observable observable : observables) {
                            observable.update(sensor, value);
                        }
                    }
                } catch (Exception e) {
                }
            }


            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

}
