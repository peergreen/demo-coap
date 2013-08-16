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

public enum SensorEnum {

    SLIDER("slider", "", 0, 1000),
    TOUCH("touch", "", 2, 900),
    BATTERY("battery", "mV", 2, 3),
    TEMPERATURE("temperature", "°C", 20,30);

    private SensorEnum(String sensorName, String unit, double rangeMin, double rangeMax) {
        this.sensorName = sensorName;
        this.unit = unit;
        this.rangeMin = rangeMin;
        this.rangeMax = rangeMax;
    }

    private String sensorName;
    private String unit;
    private double rangeMin;
    private double rangeMax;


    public double rangeMin() {
        return rangeMin;
    }

    public double rangeMax() {
        return rangeMax;
    }

    public String sensorName() {
        return sensorName;
    }

    public String unit() {
        return unit;
    }

}
