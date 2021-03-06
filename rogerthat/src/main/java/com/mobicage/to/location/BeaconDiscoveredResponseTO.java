/*
 * Copyright 2016 Mobicage NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @@license_version:1.1@@
 */

package com.mobicage.to.location;

import com.mobicage.rpc.IncompleteMessageException;

import java.util.LinkedHashMap;
import java.util.Map;

public class BeaconDiscoveredResponseTO implements com.mobicage.rpc.IJSONable {

    public String friend_email;
    public String tag;

    public BeaconDiscoveredResponseTO() {
    }

    public BeaconDiscoveredResponseTO(Map<String, Object> json) throws IncompleteMessageException {
        if (json.containsKey("friend_email")) {
            Object val = json.get("friend_email");
            this.friend_email = (String) val;
        } else {
            throw new IncompleteMessageException("com.mobicage.to.location.BeaconDiscoveredResponseTO object is missing field 'friend_email'");
        }
        if (json.containsKey("tag")) {
            Object val = json.get("tag");
            this.tag = (String) val;
        } else {
            this.tag = null;
        }
    }

    @Override
    public Map<String, Object> toJSONMap() {
        Map<String, Object> obj = new LinkedHashMap<String, Object>();
        obj.put("friend_email", this.friend_email);
        obj.put("tag", this.tag);
        return obj;
    }

}