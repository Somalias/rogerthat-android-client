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

package com.mobicage.capi.system;

public interface IClientRpc {

    com.mobicage.to.system.ForwardLogsResponseTO forwardLogs(com.mobicage.to.system.ForwardLogsRequestTO request) throws java.lang.Exception;

    com.mobicage.to.system.IdentityUpdateResponseTO identityUpdate(com.mobicage.to.system.IdentityUpdateRequestTO request) throws java.lang.Exception;

    com.mobicage.to.system.UnregisterMobileResponseTO unregisterMobile(com.mobicage.to.system.UnregisterMobileRequestTO request) throws java.lang.Exception;

    com.mobicage.to.system.UpdateAvailableResponseTO updateAvailable(com.mobicage.to.system.UpdateAvailableRequestTO request) throws java.lang.Exception;

    com.mobicage.to.js_embedding.UpdateJSEmbeddingResponseTO updateJsEmbedding(com.mobicage.to.js_embedding.UpdateJSEmbeddingRequestTO request) throws java.lang.Exception;

    com.mobicage.to.system.UpdateSettingsResponseTO updateSettings(com.mobicage.to.system.UpdateSettingsRequestTO request) throws java.lang.Exception;

}