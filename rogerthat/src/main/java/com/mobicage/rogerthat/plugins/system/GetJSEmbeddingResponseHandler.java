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
package com.mobicage.rogerthat.plugins.system;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.mobicage.rogerthat.util.logging.L;
import com.mobicage.rogerthat.util.pickle.PickleException;
import com.mobicage.rogerthat.util.system.T;
import com.mobicage.rpc.IResponse;
import com.mobicage.rpc.ResponseHandler;
import com.mobicage.to.js_embedding.GetJSEmbeddingResponseTO;

public class GetJSEmbeddingResponseHandler extends ResponseHandler<GetJSEmbeddingResponseTO> {

    public void setRequestSubmissionTimestamp(final long requestSubmissionTimestamp) {
        T.UI();
    }

    @Override
    public void readFromPickle(final int version, final DataInput in) throws IOException, PickleException {
        T.dontCare();
        super.readFromPickle(version, in);
    }

    @Override
    public void writePickle(final DataOutput out) throws IOException {
        T.dontCare();
        super.writePickle(out);
    }

    @Override
    public void handle(final IResponse<GetJSEmbeddingResponseTO> response) {
        T.BIZZ();
        try {
            final SystemPlugin systemPlugin = mMainService.getPlugin(SystemPlugin.class);
            systemPlugin.updateJSEmbeddedPackets(response.getResponse().items);
        } catch (Exception e) {
            L.w("GetJSEmbedding call resulted in failure!", e);
        }
    }

}