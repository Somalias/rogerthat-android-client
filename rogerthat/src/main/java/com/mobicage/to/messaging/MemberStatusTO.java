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

package com.mobicage.to.messaging;

import com.mobicage.rpc.IncompleteMessageException;

import java.util.LinkedHashMap;
import java.util.Map;

public class MemberStatusTO implements com.mobicage.rpc.IJSONable {

    public long acked_timestamp;
    public String button_id;
    public String custom_reply;
    public String member;
    public long received_timestamp;
    public long status;

    public MemberStatusTO() {
    }

    public MemberStatusTO(Map<String, Object> json) throws IncompleteMessageException {
        if (json.containsKey("acked_timestamp")) {
            Object val = json.get("acked_timestamp");
            this.acked_timestamp = ((Long) val).longValue();
        } else {
            throw new IncompleteMessageException("com.mobicage.to.messaging.MemberStatusTO object is missing field 'acked_timestamp'");
        }
        if (json.containsKey("button_id")) {
            Object val = json.get("button_id");
            this.button_id = (String) val;
        } else {
            throw new IncompleteMessageException("com.mobicage.to.messaging.MemberStatusTO object is missing field 'button_id'");
        }
        if (json.containsKey("custom_reply")) {
            Object val = json.get("custom_reply");
            this.custom_reply = (String) val;
        } else {
            throw new IncompleteMessageException("com.mobicage.to.messaging.MemberStatusTO object is missing field 'custom_reply'");
        }
        if (json.containsKey("member")) {
            Object val = json.get("member");
            this.member = (String) val;
        } else {
            throw new IncompleteMessageException("com.mobicage.to.messaging.MemberStatusTO object is missing field 'member'");
        }
        if (json.containsKey("received_timestamp")) {
            Object val = json.get("received_timestamp");
            this.received_timestamp = ((Long) val).longValue();
        } else {
            throw new IncompleteMessageException("com.mobicage.to.messaging.MemberStatusTO object is missing field 'received_timestamp'");
        }
        if (json.containsKey("status")) {
            Object val = json.get("status");
            this.status = ((Long) val).longValue();
        } else {
            throw new IncompleteMessageException("com.mobicage.to.messaging.MemberStatusTO object is missing field 'status'");
        }
    }

    @Override
    public Map<String, Object> toJSONMap() {
        Map<String, Object> obj = new LinkedHashMap<String, Object>();
        obj.put("acked_timestamp", this.acked_timestamp);
        obj.put("button_id", this.button_id);
        obj.put("custom_reply", this.custom_reply);
        obj.put("member", this.member);
        obj.put("received_timestamp", this.received_timestamp);
        obj.put("status", this.status);
        return obj;
    }

}