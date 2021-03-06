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
package com.mobicage.rpc;

import com.mobicage.rogerthat.util.logging.L;
import com.mobicage.rogerthat.util.system.SafeRunnable;
import com.mobicage.rogerthat.util.system.T;
import com.mobicage.to.system.SaveSettingsResponse;

public class SaveSettingsResponseHandler extends ResponseHandler<SaveSettingsResponse> {

    @Override
    public void handle(final IResponse<SaveSettingsResponse> response) {
        T.BIZZ();
        try {
            final SaveSettingsResponse result = response.getResponse();
            mMainService.postOnUIHandler(new SafeRunnable() {
                @Override
                protected void safeRun() throws Exception {
                    mMainService.processSettings(result.settings, false);
                }
            });

        } catch (Exception e) {
            L.d("Share save settings api call failed", e);
        }
    }
}