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

package com.mobicage.to.friends;

import com.mobicage.rpc.IncompleteMessageException;

import java.util.LinkedHashMap;
import java.util.Map;

public class ServiceMenuItemTO implements com.mobicage.rpc.IJSONable {

    public long[] coords;
    public String hashedTag;
    public String iconHash;
    public String label;
    public boolean requiresWifi;
    public boolean runInBackground;
    public String screenBranding;
    public String staticFlowHash;

    public ServiceMenuItemTO() {
    }

    public ServiceMenuItemTO(Map<String, Object> json) throws IncompleteMessageException {
        if (json.containsKey("coords")) {
            org.json.simple.JSONArray val_arr = (org.json.simple.JSONArray) json.get("coords");
            if (val_arr == null) {
                this.coords = null;
            } else {
                this.coords = new long[val_arr.size()];
                for (int i=0; i < val_arr.size(); i++) {
                    this.coords[i] = ((Long) val_arr.get(i)).longValue();
                }
            }
        } else {
            throw new IncompleteMessageException("com.mobicage.to.friends.ServiceMenuItemTO object is missing field 'coords'");
        }
        if (json.containsKey("hashedTag")) {
            Object val = json.get("hashedTag");
            this.hashedTag = (String) val;
        } else {
            this.hashedTag = null;
        }
        if (json.containsKey("iconHash")) {
            Object val = json.get("iconHash");
            this.iconHash = (String) val;
        } else {
            throw new IncompleteMessageException("com.mobicage.to.friends.ServiceMenuItemTO object is missing field 'iconHash'");
        }
        if (json.containsKey("label")) {
            Object val = json.get("label");
            this.label = (String) val;
        } else {
            throw new IncompleteMessageException("com.mobicage.to.friends.ServiceMenuItemTO object is missing field 'label'");
        }
        if (json.containsKey("requiresWifi")) {
            Object val = json.get("requiresWifi");
            this.requiresWifi = ((Boolean) val).booleanValue();
        } else {
            this.requiresWifi = false;
        }
        if (json.containsKey("runInBackground")) {
            Object val = json.get("runInBackground");
            this.runInBackground = ((Boolean) val).booleanValue();
        } else {
            this.runInBackground = true;
        }
        if (json.containsKey("screenBranding")) {
            Object val = json.get("screenBranding");
            this.screenBranding = (String) val;
        } else {
            throw new IncompleteMessageException("com.mobicage.to.friends.ServiceMenuItemTO object is missing field 'screenBranding'");
        }
        if (json.containsKey("staticFlowHash")) {
            Object val = json.get("staticFlowHash");
            this.staticFlowHash = (String) val;
        } else {
            this.staticFlowHash = null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> toJSONMap() {
        Map<String, Object> obj = new LinkedHashMap<String, Object>();
        if (this.coords == null) {
            obj.put("coords", null);
        } else {
            org.json.simple.JSONArray arr = new org.json.simple.JSONArray();
            for (int i=0; i < this.coords.length; i++) {
                arr.add(this.coords[i]);
            }
            obj.put("coords", arr);
        }
        obj.put("hashedTag", this.hashedTag);
        obj.put("iconHash", this.iconHash);
        obj.put("label", this.label);
        obj.put("requiresWifi", this.requiresWifi);
        obj.put("runInBackground", this.runInBackground);
        obj.put("screenBranding", this.screenBranding);
        obj.put("staticFlowHash", this.staticFlowHash);
        return obj;
    }

}