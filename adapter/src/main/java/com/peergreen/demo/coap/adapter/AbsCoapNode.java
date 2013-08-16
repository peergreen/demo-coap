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

import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.ResponseHandler;
import ch.ethz.inf.vs.californium.coap.TokenManager;

public abstract class AbsCoapNode implements ResponseHandler {


    public Request buildGetRequest(String uri) {
        GETRequest request = new GETRequest();
        request.setURI(uri);
        request.registerResponseHandler(this);
        request.setToken(TokenManager.getInstance().acquireToken());
        return request;
    }

    public Request buildDirectGetRequest(String uri) {
        GETRequest request = new GETRequest();
        request.setURI(uri);
        request.setToken(TokenManager.getInstance().acquireToken());
        return request;
    }

    public boolean isKey(String value, Event event, JsonParser parser) {
        return event == Event.KEY_NAME && value.equals(parser.getString());
    }


    public String getValue(JsonParser parser) {
        Event event = parser.next();
        if (Event.VALUE_STRING == event) {
            return parser.getString();
        }
        throw new IllegalStateException("Invalid Data");
    }


}
