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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AlphabetIndexer;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ViewFlipper;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.WebDialog;
import com.mobicage.rogerth.at.R;
import com.mobicage.rogerthat.ContactListHelper;
import com.mobicage.rogerthat.MyIdentity;
import com.mobicage.rogerthat.ServiceBoundActivity;
import com.mobicage.rogerthat.util.FacebookUtils;
import com.mobicage.rogerthat.util.FacebookUtils.PermissionType;
import com.mobicage.rogerthat.util.RegexPatterns;
import com.mobicage.rogerthat.util.logging.L;
import com.mobicage.rogerthat.util.system.SafeBroadcastReceiver;
import com.mobicage.rogerthat.util.system.SafeRunnable;
import com.mobicage.rogerthat.util.system.SafeViewOnClickListener;
import com.mobicage.rogerthat.util.system.T;
import com.mobicage.rogerthat.util.ui.ImageHelper;
import com.mobicage.rogerthat.util.ui.Slider;
import com.mobicage.rogerthat.util.ui.Slider.Swiper;
import com.mobicage.rogerthat.util.ui.UIUtils;
import com.mobicage.rpc.config.AppConstants;
import com.mobicage.rpc.config.CloudConstants;

public class RecommendServiceActivity extends ServiceBoundActivity {

    public static final String SERVICE_EMAIL = "email";
    public static final String SHARE_DESCRIPTION = "share_description";
    public static final String SHARE_IMAGE_URL = "share_image_url";
    public static final String SHARE_LINK_URL = "share_link_url";
    public static final String SHARE_CAPTION = "share_caption";

    public static final int MY_PERMISSION_REQUEST_READ_CONTACTS = 1;

    private static final String[] ALL_RECEIVING_INTENTS = new String[] { FriendsPlugin.FRIEND_UPDATE_INTENT,
        FriendsPlugin.FRIEND_AVATAR_CHANGED_INTENT, FriendsPlugin.FRIEND_REMOVED_INTENT,
        FriendsPlugin.FRIEND_MARKED_FOR_REMOVAL_INTENT, FriendsPlugin.FRIEND_ADDED_INTENT,
        FriendsPlugin.FRIENDS_LIST_REFRESHED };

    private String mServiceEmail;
    private String mShareImageUrl;
    private String mShareDescription;
    private FriendsPlugin mFriendsPlugin;
    private PhoneContacts mPhoneContacts;
    private MyIdentity mMyIdentity;
    private List<String> mCurrentRecommendations;

    private ViewFlipper mViewFlipper;
    private GestureDetector mGestureScanner;
    private BroadcastReceiver mBroadcastReceiver;
    private String mShareLinkUrl;
    private String mShareCaption;

    private UiLifecycleHelper mUiHelper;

    private Cursor mCursorEmails = null;
    private Cursor mCursorFriends = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.recommend_service);

        mViewFlipper = (ViewFlipper) findViewById(R.id.view_flipper);

        if (!AppConstants.FRIENDS_ENABLED) {
            LinearLayout rtf = (LinearLayout) findViewById(R.id.recommend_to_friends);
            mViewFlipper.removeView(rtf);

            RelativeLayout rl = (RelativeLayout) findViewById(R.id.rogerthat_layout);
            rl.setVisibility(View.GONE);

            View ci = findViewById(R.id.contacts_indicator);
            ci.setVisibility(View.VISIBLE);
        }

        if (AppConstants.FACEBOOK_APP_ID == null) {
            LinearLayout rof = (LinearLayout) findViewById(R.id.recommend_on_fb);
            mViewFlipper.removeView(rof);

            RelativeLayout fl = (RelativeLayout) findViewById(R.id.facebook_layout);
            fl.setVisibility(View.GONE);
        }

        Swiper leftSwiper = new Swiper() {
            @Override
            public Intent onSwipe() {
                int i = mViewFlipper.getDisplayedChild() + 1;
                if (i < mViewFlipper.getChildCount()) {
                    displayTab(i);
                }
                return null;
            }
        };
        Swiper rightSwiper = new Swiper() {
            @Override
            public Intent onSwipe() {
                int i = mViewFlipper.getDisplayedChild();
                if (i > 0) {
                    displayTab(i - 1);
                }
                return null;
            }
        };
        mGestureScanner = new GestureDetector(new Slider(this, this, leftSwiper, rightSwiper));

        Intent intent = getIntent();
        mServiceEmail = intent.getStringExtra(SERVICE_EMAIL);
        mShareDescription = intent.getStringExtra(SHARE_DESCRIPTION);
        mShareCaption = intent.getStringExtra(SHARE_CAPTION);

        Uri shareLinkUrl = Uri.parse(intent.getStringExtra(SHARE_LINK_URL));
        Builder b = shareLinkUrl.buildUpon();
        b.appendQueryParameter("from", "phone");
        b.appendQueryParameter("target", "fbwall");
        b.build();
        mShareLinkUrl = b.toString();

        mShareImageUrl = intent.getStringExtra(SHARE_IMAGE_URL);

        mUiHelper = new UiLifecycleHelper(this, null);
        mUiHelper.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mUiHelper.onResume();

        if (getWasPaused() && mCursorEmails != null) {
            startManagingCursor(mCursorEmails);
        }
        if (getWasPaused() && mCursorFriends != null) {
            startManagingCursor(mCursorFriends);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mUiHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        mUiHelper.onPause();
        if (mCursorEmails != null) {
            stopManagingCursor(mCursorEmails);
        }
        if (mCursorFriends != null) {
            stopManagingCursor(mCursorFriends);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mUiHelper.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (mGestureScanner != null) {
            mGestureScanner.onTouchEvent(e);
        }
        return super.dispatchTouchEvent(e);
    }

    @Override
    protected void onServiceBound() {
        mFriendsPlugin = mService.getPlugin(FriendsPlugin.class);

        mCurrentRecommendations = new ArrayList<String>();
        mPhoneContacts = new PhoneContacts(getContentResolver());
        mMyIdentity = mService.getIdentityStore().getIdentity();
        configureTabs();
        configureContactsView();
        configureRogerthatView();
        configureMailView();
        configureFacebookView();

        mBroadcastReceiver = getBroadcastReceiver();

        IntentFilter filter = new IntentFilter();
        for (String action : ALL_RECEIVING_INTENTS) {
            filter.addAction(action);
        }
        registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    protected void onServiceUnbound() {
        T.UI();
        unregisterReceiver(mBroadcastReceiver);
    }

    private BroadcastReceiver getBroadcastReceiver() {
        T.UI();
        return new SafeBroadcastReceiver() {
            @Override
            public String[] onSafeReceive(Context context, Intent intent) {
                configureRogerthatView();
                return ALL_RECEIVING_INTENTS;
            };
        };
    }

    private void displayTab(final int tab) {
        mViewFlipper.setDisplayedChild(tab);

        int indicatorsCount = 4;
        if (!AppConstants.FRIENDS_ENABLED)
            indicatorsCount--;
        if (AppConstants.FACEBOOK_APP_ID == null)
            indicatorsCount--;

        int currentIndicatorCount = 0;
        int[] indicators = new int[indicatorsCount];
        if (AppConstants.FRIENDS_ENABLED) {
            indicators[currentIndicatorCount] = R.id.rogerthat_indicator;
            currentIndicatorCount++;
            if (!mService.isPermitted(Manifest.permission.READ_CONTACTS)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS},
                        MY_PERMISSION_REQUEST_READ_CONTACTS);
                return;
            }
        }
        indicators[currentIndicatorCount] = R.id.contacts_indicator;
        currentIndicatorCount++;
        indicators[currentIndicatorCount] = R.id.mail_indicator;
        currentIndicatorCount++;

        if (AppConstants.FACEBOOK_APP_ID != null) {
            indicators[currentIndicatorCount] = R.id.facebook_indicator;
        }

        for (int i = 0; i < indicators.length; i++) {
            findViewById(indicators[i]).setVisibility(i == tab ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void configureTabs() {
        T.UI();

        int indicatorsCount = 4;
        if (!AppConstants.FRIENDS_ENABLED)
            indicatorsCount--;
        if (AppConstants.FACEBOOK_APP_ID == null)
            indicatorsCount--;

        int currentIndicatorCount = 0;
        int[] views = new int[indicatorsCount];
        if (AppConstants.FRIENDS_ENABLED) {
            views[currentIndicatorCount] = R.id.rogerthat_layout;
            currentIndicatorCount++;
        }
        views[currentIndicatorCount] = R.id.contacts_layout;
        currentIndicatorCount++;
        views[currentIndicatorCount] = R.id.mail_layout;
        currentIndicatorCount++;

        if (AppConstants.FACEBOOK_APP_ID != null) {
            views[currentIndicatorCount] = R.id.facebook_layout;
        }

        for (int v = 0; v < views.length; v++) {
            final int x = v;
            findViewById(views[v]).setOnClickListener(new SafeViewOnClickListener() {
                @Override
                public void safeOnClick(View v) {
                    if (x != mViewFlipper.getDisplayedChild()) {
                        displayTab(x);
                    }
                }
            });
        }
    }

    private void configureContactsView() {
        if (!mService.isPermitted(Manifest.permission.READ_CONTACTS)) {
            ((TextView) findViewById(R.id.recommend_to_contacts_title)).setText( R.string.no_contacts_with_email_found);
            return;
        }
        mCursorEmails = mPhoneContacts.getContactsPerEmail(new String[] { mMyIdentity.getEmail() }, null);
        startManagingCursor(mCursorEmails);

        ((ListView) findViewById(R.id.recommend_to_contacts_listview)).setAdapter(new FriendsAdapter(this, mCursorEmails));
        ((TextView) findViewById(R.id.recommend_to_contacts_title))
            .setText(mCursorEmails.getCount() == 0 ? R.string.no_contacts_with_email_found : R.string.recommend_to_contacts);
    }

    private void configureRogerthatView() {
        if (AppConstants.FRIENDS_ENABLED) {
            mCursorFriends = mFriendsPlugin.getStore().getUserFriendListCursor();
            startManagingCursor(mCursorFriends);

            ((ListView) findViewById(R.id.recommend_to_friends_listview)).setAdapter(new FriendsAdapter(this, mCursorFriends));

            ((TextView) findViewById(R.id.recommend_to_friends_title)).setText(mCursorFriends.getCount() == 0 ? getString(
                R.string.no_friends_found, getString(R.string.app_name)) : getString(R.string.recommend_to_friends,
                getString(R.string.app_name)));
        }
    }

    private void configureMailView() {
        T.UI();
        final AutoCompleteTextView emailText = (AutoCompleteTextView) findViewById(R.id.recommend_email_text_field);
        emailText.setAdapter(new ArrayAdapter<String>(this, R.layout.list_item, new ArrayList<String>()));
        emailText.setThreshold(1);

        if (mService.isPermitted(Manifest.permission.READ_CONTACTS)) {
            mService.postAtFrontOfBIZZHandler(new SafeRunnable() {

                @SuppressWarnings("unchecked")
                @Override
                protected void safeRun() throws Exception {
                    L.d("RecommendServiceActivity getEmailAddresses");
                    List<String> emailList = ContactListHelper.getEmailAddresses(RecommendServiceActivity.this);
                    ArrayAdapter<String> a = (ArrayAdapter<String>) emailText.getAdapter();
                    for (int i = 0; i < emailList.size(); i++) {
                        a.add(emailList.get(i));
                    }
                    a.notifyDataSetChanged();
                    L.d("RecommendServiceActivity gotEmailAddresses");
                }
            });
        }

        final SafeViewOnClickListener onClickListener = new SafeViewOnClickListener() {
            @Override
            public void safeOnClick(View v) {
                String email = emailText.getText().toString().trim();
                if (RegexPatterns.EMAIL.matcher(email).matches()) {
                    mFriendsPlugin.shareService(mServiceEmail, email);
                    emailText.setText(null);
                    UIUtils.hideKeyboard(RecommendServiceActivity.this, emailText);

                    AlertDialog.Builder builder = new AlertDialog.Builder(RecommendServiceActivity.this);
                    builder.setMessage(R.string.service_recommendation_sent);
                    builder.setPositiveButton(R.string.rogerthat, null);
                    builder.create().show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(RecommendServiceActivity.this);
                    builder.setMessage(R.string.registration_email_not_valid);
                    builder.setPositiveButton(R.string.rogerthat, null);
                    builder.create().show();
                }
            }
        };

        ((Button) findViewById(R.id.recommend_email_button)).setOnClickListener(onClickListener);

        emailText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    onClickListener.onClick(view);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final Session session = Session.getActiveSession();
        if (session != null) {
            session.onActivityResult(this, requestCode, resultCode, data);
        }

        mUiHelper.onActivityResult(requestCode, resultCode, data, new FacebookDialog.Callback() {
            @Override
            public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
                if (!(error instanceof FacebookOperationCanceledException)) {
                    showFacebookErrorPopup();
                    L.bug("Error while recommending " + mServiceEmail + " on facebook wall.\nParams = " + data, error);
                }
            }

            @Override
            public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        T.UI();
        if (requestCode == MY_PERMISSION_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                configureContactsView();
            }
        }
    }

    private void configureFacebookView() {
        if (AppConstants.FACEBOOK_APP_ID != null) {
            ((Button) findViewById(R.id.recommend_on_fb_button)).setOnClickListener(new SafeViewOnClickListener() {
                @Override
                public void safeOnClick(View v) {

                    if (!mService.getNetworkConnectivityManager().isConnected()) {
                        UIUtils.showNoNetworkDialog(RecommendServiceActivity.this);
                        return;
                    }
                    FacebookUtils.ensureOpenSession(RecommendServiceActivity.this,
                        Arrays.asList("email", "user_friends"), PermissionType.READ, new Session.StatusCallback() {
                            @Override
                            public void call(Session session, SessionState state, Exception exception) {
                                if (session != Session.getActiveSession()) {
                                    session.removeCallback(this);
                                    return;
                                }
                                session.removeCallback(this);
                                if (exception == null && session.isOpened()) {

                                    L.d("Posting to facebook:\n- FACEBOOK_APP_ID: " + CloudConstants.FACEBOOK_APP_ID
                                        + "\n- Picture: " + mShareImageUrl + "\n- Catption: " + mShareCaption
                                        + "\n- Description: " + mShareDescription + "\n- Link :" + mShareLinkUrl);

                                    if (FacebookDialog.canPresentShareDialog(getApplicationContext(),
                                        FacebookDialog.ShareDialogFeature.SHARE_DIALOG)) {
                                        L.d("Share via FacebookDialog");

                                        FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(
                                            RecommendServiceActivity.this).setLink(mShareLinkUrl)
                                            .setPicture(mShareImageUrl).setCaption(mShareCaption)
                                            .setDescription(mShareDescription).build();
                                        mUiHelper.trackPendingDialogCall(shareDialog.present());
                                    } else {
                                        L.d("Share via WebDialog");

                                        final Bundle params = new Bundle();
                                        params.putString("app_id", CloudConstants.FACEBOOK_APP_ID);
                                        params.putString("picture", mShareImageUrl);
                                        params.putString("caption", mShareCaption);
                                        params.putString("description", mShareDescription);
                                        params.putString("link", mShareLinkUrl);

                                        L.d("Posting to facebook: " + params);

                                        new WebDialog.FeedDialogBuilder(RecommendServiceActivity.this, session, params)
                                            .setOnCompleteListener(new WebDialog.OnCompleteListener() {
                                                @Override
                                                public void onComplete(Bundle values, FacebookException error) {
                                                    if (error != null
                                                        && !(error instanceof FacebookOperationCanceledException)) {
                                                        showFacebookErrorPopup();
                                                        L.bug("Error while recommending " + mServiceEmail
                                                            + " on facebook wall.\nParams = " + params, error);
                                                    }
                                                }

                                            }).build().show();
                                    }
                                } else {
                                    if (exception != null && !(exception instanceof FacebookOperationCanceledException)) {
                                        showFacebookErrorPopup();
                                    }
                                }
                            }
                        }, true);
                }
            });
        }
    }

    private void showFacebookErrorPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setMessage(R.string.error_recommend_on_fb);
        builder.setPositiveButton(R.string.rogerthat, null);
        builder.create().show();
    }

    private Bitmap getAvatarBitmap(Friend friend) {
        if (friend instanceof Contact) {
            Bitmap avatar = mPhoneContacts.getAvatar((Contact) friend);
            if (avatar == null) {
                return mFriendsPlugin.getMissingFriendAvatarBitmap();
            } else {
                return ImageHelper.getRoundedCornerAvatar(avatar);
            }
        } else {
            return mFriendsPlugin.getAvatarBitmap(friend.email);
        }
    }

    private void setFriendOnView(final Friend friend, final View v) {
        ((ImageView) v.findViewById(R.id.avatar)).setImageBitmap(getAvatarBitmap(friend));
        ((TextView) v.findViewById(R.id.name)).setText(friend.getDisplayName());

        final TextView contactMethod = (TextView) v.findViewById(R.id.secondary_label);
        contactMethod.setText(friend.getDisplayEmail());
        contactMethod.setVisibility(friend instanceof Contact ? View.VISIBLE : View.GONE);

        final Button btn = (Button) v.findViewById(R.id.invite_button);
        boolean recommended = mCurrentRecommendations.contains(friend.email);
        btn.setText(recommended ? R.string.recommended_service : R.string.recommend_service);
        btn.setEnabled(!recommended);
        btn.setOnClickListener(new SafeViewOnClickListener() {
            @Override
            public void safeOnClick(View v) {
                btn.setEnabled(false);
                btn.setText(R.string.recommended_service);
                if (mFriendsPlugin.shareService(mServiceEmail, friend.email)) {
                    mCurrentRecommendations.add(friend.email);
                } else {
                    UIUtils.showAlertDialog(RecommendServiceActivity.this, null, R.string.error_please_try_again);
                }
            }
        });
        btn.getLayoutParams().width = UIUtils.convertDipToPixels(this, 100);
        btn.setTextSize(12);
    }

    private class FriendsAdapter extends CursorAdapter implements SectionIndexer {

        private final AlphabetIndexer mAlphaIndexer;

        public FriendsAdapter(Context context, Cursor c) {
            super(context, c);
            mAlphaIndexer = new AlphabetIndexer(c, 1, " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = getLayoutInflater().inflate(R.layout.add_friends_list_item, parent, false);
            }

            Cursor cursor = getCursor();
            if (!cursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }

            final Friend friend = mFriendsPlugin.getStore().readFriendFromCursor(cursor);
            setFriendOnView(friend, v);
            return v;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            L.bug("Should not come here");
            return null;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            L.bug("Should not come here");
        }

        @Override
        public int getPositionForSection(int section) {
            return mAlphaIndexer.getPositionForSection(section);
        }

        @Override
        public int getSectionForPosition(int position) {
            return mAlphaIndexer.getSectionForPosition(position);
        }

        @Override
        public Object[] getSections() {
            return mAlphaIndexer.getSections();
        }

    }

}