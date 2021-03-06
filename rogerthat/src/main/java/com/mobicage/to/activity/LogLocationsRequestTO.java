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

package com.mobicage.to.activity;

import com.mobicage.rpc.IncompleteMessageException;

import java.util.LinkedHashMap;
import java.util.Map;

public class LogLocationsRequestTO implements com.mobicage.rpc.IJSONable {

    public com.mobicage.to.activity.LogLocationRecipientTO[] recipients;
    public com.mobicage.to.activity.LocationRecordTO[] records;

    public LogLocationsRequestTO() {
    }

    @SuppressWarnings("unchecked")
    public LogLocationsRequestTO(Map<String, Object> json) throws IncompleteMessageException {
        if (json.containsKey("recipients")) {
            org.json.simple.JSONArray val_arr = (org.json.simple.JSONArray) json.get("recipients");
            if (val_arr == null) {
                this.recipients = null;
            } else {
                this.recipients = new com.mobicage.to.activity.LogLocationRecipientTO[val_arr.size()];
                for (int i=0; i < val_arr.size(); i++) {
                    Object item = val_arr.get(i);
                    if (item != null) {
                        this.recipients[i] = new com.mobicage.to.activity.LogLocationRecipientTO((Map<String, Object>) item);
                    }
                }
            }
        } else {
            throw new IncompleteMessageException("com.mobicage.to.activity.LogLocationsRequestTO object is missing field 'recipients'");
        }
        if (json.containsKey("records")) {
            org.json.simple.JSONArray val_arr = (org.json.simple.JSONArray) json.get("records");
            if (val_arr == null) {
                this.records = null;
            } else {
                this.records = new com.mobicage.to.activity.LocationRecordTO[val_arr.size()];
                for (int i=0; i < val_arr.size(); i++) {
                    Object item = val_arr.get(i);
                    if (item != null) {
                        this.records[i] = new com.mobicage.to.activity.LocationRecordTO((Map<String, Object>) item);
                    }
                }
            }
        } else {
            throw new IncompleteMessageException("com.mobicage.to.activity.LogLocationsRequestTO object is missing field 'records'");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> toJSONMap() {
        Map<String, Object> obj = new LinkedHashMap<String, Object>();
        if (this.recipients == null) {
            obj.put("recipients", null);
        } else {
            org.json.simple.JSONArray arr = new org.json.simple.JSONArray();
            for (int i=0; i < this.recipients.length; i++) {
                arr.add(this.recipients[i].toJSONMap());
            }
            obj.put("recipients", arr);
        }
        if (this.records == null) {
            obj.put("records", null);
        } else {
            org.json.simple.JSONArray arr = new org.json.simple.JSONArray();
            for (int i=0; i < this.records.length; i++) {
                arr.add(this.records[i].toJSONMap());
            }
            obj.put("records", arr);
        }
        return obj;
    }

}