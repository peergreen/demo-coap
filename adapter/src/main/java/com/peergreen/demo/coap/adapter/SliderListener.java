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

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import com.peergreen.demo.coap.Sensor;

@Component
@Instantiate
@Provides(specifications=SliderListener.class)
public class SliderListener implements Runnable  {

    @Requires(filter="(name=slider)")
    private Sensor sliderSensor;


    @Validate
    public void start() {
       new Thread(this).start();
    }


    @Override
    public void run() {
//        while (true) {
//            String name = sliderSensor.getName();
//            String unit = sliderSensor.getUnit();
//            double value = sliderSensor.getValue();
//            System.out.println("Value of slider is = " + name + ":" + value + " " + unit);
//            try {
//                Thread.sleep(10000L);
//            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }

    }

}
