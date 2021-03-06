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
package com.mobicage.rogerthat.plugins.friends;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.mobicage.rogerthat.MyIdentity;

public class FriendAvatar {

    public static Drawable getAvatar(Context context, Friend friend) {
        return new BitmapDrawable(context.getResources(), BitmapFactory.decodeByteArray(friend.avatar, 0,
            friend.avatar.length));
    }

    public static Drawable getAvatar(Context context, MyIdentity identity) {
        byte[] avatar = identity.getAvatar();
        return new BitmapDrawable(context.getResources(), BitmapFactory.decodeByteArray(avatar, 0, avatar.length));
    }

}