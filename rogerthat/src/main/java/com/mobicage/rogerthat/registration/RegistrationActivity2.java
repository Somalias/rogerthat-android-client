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
package com.mobicage.rogerthat.registration;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.SSLException;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BleNotAvailableException;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.logging.Loggers;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Logger;
import org.jivesoftware.smack.XMPPConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ViewFlipper;

import com.facebook.FacebookOperationCanceledException;
import com.facebook.Session;
import com.facebook.SessionState;
import com.mobicage.rogerth.at.R;
import com.mobicage.rogerthat.Installation;
import com.mobicage.rogerthat.MainActivity;
import com.mobicage.rogerthat.MainService;
import com.mobicage.rogerthat.OauthActivity;
import com.mobicage.rogerthat.ServiceBoundActivity;
import com.mobicage.rogerthat.config.Configuration;
import com.mobicage.rogerthat.config.ConfigurationProvider;
import com.mobicage.rogerthat.plugins.messaging.Message;
import com.mobicage.rogerthat.plugins.scan.ProcessScanActivity;
import com.mobicage.rogerthat.plugins.system.MobileInfo;
import com.mobicage.rogerthat.plugins.system.SystemPlugin;
import com.mobicage.rogerthat.plugins.trackme.BeaconRegion;
import com.mobicage.rogerthat.registration.AccountManager.Account;
import com.mobicage.rogerthat.util.FacebookUtils;
import com.mobicage.rogerthat.util.FacebookUtils.PermissionType;
import com.mobicage.rogerthat.util.GoogleServicesUtils;
import com.mobicage.rogerthat.util.GoogleServicesUtils.GCMRegistrationIdFoundCallback;
import com.mobicage.rogerthat.util.RegexPatterns;
import com.mobicage.rogerthat.util.Security;
import com.mobicage.rogerthat.util.http.HTTPUtil;
import com.mobicage.rogerthat.util.logging.L;
import com.mobicage.rogerthat.util.system.SafeAsyncTask;
import com.mobicage.rogerthat.util.system.SafeBroadcastReceiver;
import com.mobicage.rogerthat.util.system.SafeRunnable;
import com.mobicage.rogerthat.util.system.SafeViewOnClickListener;
import com.mobicage.rogerthat.util.system.T;
import com.mobicage.rogerthat.util.ui.Pausable;
import com.mobicage.rogerthat.util.ui.UIUtils;
import com.mobicage.rogerthat.util.ui.Wizard;
import com.mobicage.rpc.Credentials;
import com.mobicage.rpc.config.AppConstants;
import com.mobicage.rpc.config.CloudConstants;
import com.mobicage.rpc.newxmpp.XMPPConfigurationFactory;
import com.mobicage.to.beacon.BeaconRegionTO;
import com.mobicage.to.location.BeaconDiscoveredRequestTO;

public class RegistrationActivity2 extends ServiceBoundActivity {

    private static final int XMPP_CHECK_DELAY_MILLIS = 5000;
    private static final int XMPP_MAX_NUM_ATTEMPTS = 8;
    private static final int PIN_LENGTH = 4;
    private static final int HTTP_RETRY_COUNT = 3;
    private static final int HTTP_TIMEOUT = 10000;

    public static final String QRSCAN_CONFIGKEY = "QR_SCAN";
    public static final String INVITOR_CODE_CONFIGKEY = "invitor_code";
    public static final String INVITOR_SECRET_CONFIGKEY = "invitor_secret";
    public static final String OPENED_URL_CONFIGKEY = "opened_url";

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_GET_ACCOUNTS = 2;

    private static final int START_OAUTH_REQUEST_CODE = 1;

    private static final int[] NORMAL_WIDTH_ROGERTHAT_LOGOS = new int[] { R.id.rogerthat_logo, R.id.rogerthat_logo1,
            R.id.rogerthat_logo2, R.id.rogerthat_logo3, R.id.rogerthat_logo4, R.id.rogerthat_logo5 };
    private static final int[] FULL_WIDTH_ROGERTHAT_LOGOS = new int[] { R.id.full_width_rogerthat_logo,
            R.id.full_width_rogerthat_logo1, R.id.full_width_rogerthat_logo2, R.id.full_width_rogerthat_logo3, R.id
            .full_width_rogerthat_logo4, R.id.full_width_rogerthat_logo5 };

    private Intent mNotYetProcessedIntent = null;

    private HandlerThread mWorkerThread;
    private Looper mWorkerLooper;
    private Handler mWorkerHandler;
    private Handler mUIHandler;
    private RegistrationWizard2 mWiz;
    private AutoCompleteTextView mEnterEmailAutoCompleteTextView;
    private EditText mEnterPinEditText;
    private List<Account> mAccounts;
    private AccountManager mAccountManager;
    private HttpClient mHttpClient;

    private boolean mAgeAndGenderSet = true;
    private String mDiscoveredBeacons = null;
    private BeaconManager mBeaconManager;
    private String mGCMRegistrationId = "";

    private BroadcastReceiver mBroadcastReceiver = new SafeBroadcastReceiver() {
        @Override
        public String[] onSafeReceive(Context context, Intent intent) {
            T.UI();
            if (RegistrationWizard2.INTENT_GOT_BEACON_REGIONS.equals(intent.getAction())) {
                if (mWiz.getBeaconRegions() != null && mBeaconManager == null) {
                    bindBeaconManager();
                }
            } else if (MainService.INTENT_BEACON_SERVICE_CONNECTED.equals(intent.getAction())) {
                mBeaconManager.setBackgroundMode(!mService.getScreenIsOn());
                startMonitoringBeaconRegions();
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                if (mBeaconManager != null) {
                    mBeaconManager.setBackgroundMode(true);
                }
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                if (mBeaconManager != null) {
                    mBeaconManager.setBackgroundMode(false);
                }
            }

            return null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        T.UI();
        createWorkerThread();
        mUIHandler = new Handler();
        T.setUIThread("RegistrationProcedureActivity.onCreate()");
        setTitle(R.string.registration_title);

        mHttpClient = HTTPUtil.getHttpClient(HTTP_TIMEOUT, HTTP_RETRY_COUNT);

        final IntentFilter filter = new IntentFilter(MainService.INTENT_BEACON_SERVICE_CONNECTED);
        filter.addAction(RegistrationWizard2.INTENT_GOT_BEACON_REGIONS);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mBroadcastReceiver, filter);

        startRegistrationService();

        // TODO: This has to be improved.
        // If the app relies on GCM the user should not be able to register.
        if (CloudConstants.USE_GCM_KICK_CHANNEL)
            GoogleServicesUtils.checkPlayServices(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO: This has to be improved.
        // If the app relies on GCM the user should not be able to register.
        if (CloudConstants.USE_GCM_KICK_CHANNEL)
            GoogleServicesUtils.checkPlayServices(this);
    };

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        View v = findViewById(R.id.registration_viewFlipper);
        if (v != null)
            UIUtils.hideKeyboard(this, v);
        if (mService != null && mService.getRegisteredFromConfig()) {
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mServiceIsBound) {
            processIntent(getIntent());
        } else {
            mNotYetProcessedIntent = getIntent();
        }
    }

    private void processIntent(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            String url = uri.toString();

            if (RegexPatterns.FRIEND_INVITE_WITH_SECRET_URL.matcher(url).matches()) {
                popupRegisterFirst(uri);

                String invitorCode = url.substring(ProcessScanActivity.URL_ROGERTHAT_PREFIX.length(), url.indexOf("?"));
                String secret = uri.getQueryParameter("s");
                Configuration cfg = mService.getConfigurationProvider().getConfiguration(RegistrationWizard2.CONFIGKEY);
                cfg.put(INVITOR_CODE_CONFIGKEY, invitorCode);
                cfg.put(INVITOR_SECRET_CONFIGKEY, secret);
                mService.getConfigurationProvider().updateConfigurationNow(RegistrationWizard2.CONFIGKEY, cfg);

            } else if (RegexPatterns.FRIEND_INVITE_URL.matcher(url).matches()
                || RegexPatterns.SERVICE_INTERACT_URL.matcher(url).matches()) {
                popupRegisterFirst(uri);

                Configuration cfg = mService.getConfigurationProvider().getConfiguration(RegistrationWizard2.CONFIGKEY);
                cfg.put(OPENED_URL_CONFIGKEY, url);
                mService.getConfigurationProvider().updateConfigurationNow(RegistrationWizard2.CONFIGKEY, cfg);
            }
        }
    }

    private void popupRegisterFirst(Uri uri) {
        String invitorName = uri.getQueryParameter("u");
        if (invitorName != null)
            invitorName = invitorName.replaceAll("\\+", " ");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.friend_invitation_register_first, invitorName));
        builder.setPositiveButton(R.string.rogerthat, null);
        builder.create().show();
    }

    private void startRegistrationService() {
        startService(new Intent(this, RegistrationService.class));
    }

    private void stopRegistrationService() {
        stopService(new Intent(this, RegistrationService.class));
    }

    private void showNotification() {
        mService.sendBroadcast(new Intent(RegistrationService.INTENT_SHOW_NOTIFICATION));
    }

    private void hideNotification() {
        mService.sendBroadcast(new Intent(RegistrationService.INTENT_HIDE_NOTIFICATION));
    }

    @Override
    protected void onDestroy() {
        closeWorkerThread();
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    protected void onServiceBound() {
        T.UI();
        if (mNotYetProcessedIntent != null) {
            processIntent(mNotYetProcessedIntent);
            mNotYetProcessedIntent = null;
        }

        setContentView(R.layout.registration2);

        final Typeface faTypeFace = Typeface.createFromAsset(getAssets(), "FontAwesome.ttf");
        final int[] visibleLogos;
        final int[] goneLogos;
        if (AppConstants.FULL_WIDTH_HEADERS) {
            visibleLogos = FULL_WIDTH_ROGERTHAT_LOGOS;
            goneLogos = NORMAL_WIDTH_ROGERTHAT_LOGOS;
            View viewFlipper = findViewById(R.id.registration_viewFlipper);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) viewFlipper.getLayoutParams();
            params.setMargins(0, 0, 0, 0);
        } else {
            visibleLogos = NORMAL_WIDTH_ROGERTHAT_LOGOS;
            goneLogos = FULL_WIDTH_ROGERTHAT_LOGOS;
        }

        for (int id : visibleLogos)
            findViewById(id).setVisibility(View.VISIBLE);
        for (int id : goneLogos)
            findViewById(id).setVisibility(View.GONE);

        handleScreenOrientation(getResources().getConfiguration().orientation);

        ScrollView rc = (ScrollView) findViewById(R.id.registration_container);
        Resources resources = getResources();
        if (CloudConstants.isRogerthatApp()) {
            rc.setBackgroundColor(resources.getColor(R.color.mc_page_dark));
        } else {
            rc.setBackgroundColor(resources.getColor(R.color.mc_homescreen_background));
        }

        TextView rogerthatWelcomeTextView = (TextView) findViewById(R.id.rogerthat_welcome);
        TextView tosTextView = (TextView) findViewById(R.id.registration_tos);
        Button agreeBtn = (Button) findViewById(R.id.registration_agree_tos);

        TextView registrationNeedEmail = (TextView) findViewById(R.id.registration_need_email);
        mEnterEmailAutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.registration_enter_email);

        if (CloudConstants.isEnterpriseApp()) {
            rogerthatWelcomeTextView.setText(getString(R.string.rogerthat_welcome_enterprise,
                getString(R.string.app_name)));
            tosTextView.setVisibility(View.GONE);
            agreeBtn.setText(R.string.start_registration);
            registrationNeedEmail.setText(R.string.registration_need_your_email_enterprise);
            mEnterEmailAutoCompleteTextView.setHint(R.string.registration_enter_email_hint_enterprise);
        } else {
            rogerthatWelcomeTextView.setText(getString(R.string.rogerthat_welcome, getString(R.string.app_name)));

            tosTextView.setText(Html.fromHtml("<a href=\"" + CloudConstants.TERMS_OF_SERVICE_URL + "\">"
                + tosTextView.getText() + "</a>"));
            tosTextView.setMovementMethod(LinkMovementMethod.getInstance());

            agreeBtn.setText(R.string.registration_agree_tos);

            registrationNeedEmail.setText(R.string.registration_need_your_email);
            mEnterEmailAutoCompleteTextView.setHint(R.string.registration_enter_email_hint);
        }

        agreeBtn.getBackground().setColorFilter(Message.GREEN_BUTTON_COLOR, PorterDuff.Mode.MULTIPLY);
        agreeBtn.setOnClickListener(new SafeViewOnClickListener() {
            @Override
            public void safeOnClick(View v) {
                sendRegistrationStep(RegistrationWizard2.REGISTRATION_STEP_AGREED_TOS);
                mWiz.proceedToNextPage();
            }
        });

        initLocationUsageStep(faTypeFace);

        View.OnClickListener emailLoginListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendRegistrationStep(RegistrationWizard2.REGISTRATION_STEP_EMAIL_LOGIN);
                mWiz.proceedToNextPage();
            }
        };

        findViewById(R.id.login_via_email).setOnClickListener(emailLoginListener);
        findViewById(R.id.email_login).setOnClickListener(emailLoginListener);

        Button facebookButton = (Button) findViewById(R.id.login_via_fb);
        ImageView facebookImage = (ImageView) findViewById(R.id.fb_login);

        View.OnClickListener facebookLoginListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check network connectivity
                if (!mService.getNetworkConnectivityManager().isConnected()) {
                    UIUtils.showNoNetworkDialog(RegistrationActivity2.this);
                    return;
                }

                sendRegistrationStep(RegistrationWizard2.REGISTRATION_STEP_FACEBOOK_LOGIN);

                FacebookUtils.ensureOpenSession(
                    RegistrationActivity2.this,
                    AppConstants.PROFILE_SHOW_GENDER_AND_BIRTHDATE ? Arrays.asList("email", "user_friends",
                        "user_birthday") : Arrays.asList("email", "user_friends"), PermissionType.READ,
                    new Session.StatusCallback() {
                        @Override
                        public void call(Session session, SessionState state, Exception exception) {
                            if (session != Session.getActiveSession()) {
                                session.removeCallback(this);
                                return;
                            }

                            if (exception != null) {
                                session.removeCallback(this);
                                if (!(exception instanceof FacebookOperationCanceledException)) {
                                    L.bug("Facebook SDK error during registration", exception);
                                    AlertDialog.Builder builder = new AlertDialog.Builder(RegistrationActivity2.this);
                                    builder.setMessage(R.string.error_please_try_again);
                                    builder.setPositiveButton(R.string.rogerthat, null);
                                    AlertDialog dialog = builder.create();
                                    dialog.show();
                                }
                            } else if (session.isOpened()) {
                                session.removeCallback(this);
                                if (session.getPermissions().contains("email")) {
                                    registerWithAccessToken(session.getAccessToken());
                                } else {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(RegistrationActivity2.this);
                                    builder.setMessage(R.string.facebook_registration_email_missing);
                                    builder.setPositiveButton(R.string.rogerthat, null);
                                    AlertDialog dialog = builder.create();
                                    dialog.show();
                                }
                            }
                        }
                    }, false);
            };
        };

        facebookButton.setOnClickListener(facebookLoginListener);
        facebookImage.setOnClickListener(facebookLoginListener);

        View.OnClickListener oauthLoginListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOauthRegistrationInfo();
            }
        };

        findViewById(R.id.login_via_oauth).setOnClickListener(oauthLoginListener);
        findViewById(R.id.oauth_login).setOnClickListener(oauthLoginListener);

        final Button getAccountsButton = (Button) findViewById(R.id.get_accounts);
        if (configureEmailAutoComplete()) {
            // GET_ACCOUNTS permission is granted
            getAccountsButton.setVisibility(View.GONE);
        } else {
            getAccountsButton.setTypeface(faTypeFace);
            getAccountsButton.setOnClickListener(new SafeViewOnClickListener() {
                @Override
                public void safeOnClick(View v) {
                    ActivityCompat.requestPermissions(RegistrationActivity2.this, new String[]{Manifest.permission
                            .GET_ACCOUNTS}, PERMISSION_REQUEST_GET_ACCOUNTS);
                }
            });
        }

        mEnterPinEditText = (EditText) findViewById(R.id.registration_enter_pin);

        mEnterPinEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == PIN_LENGTH)
                    onPinEntered();
            }
        });

        Button requestNewPinButton = (Button) findViewById(R.id.registration_request_new_pin);
        requestNewPinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWiz.setEmail(null);
                hideNotification();
                mWiz.reInit();
                mWiz.goBackToPrevious();
                mEnterEmailAutoCompleteTextView.setText("");
            }
        });

        mWiz = RegistrationWizard2.getWizard(mService);
        mWiz.setFlipper((ViewFlipper) findViewById(R.id.registration_viewFlipper));
        setFinishHandler();
        addAgreeTOSHandler();
        addIBeaconUsageHandler();
        addChooseLoginMethodHandler();
        addOauthMethodHandler();
        addEnterEmailHandler();
        addEnterPinHandler();
        mWiz.run();
        mWiz.setDeviceId(Installation.id(this));

        handleEnterEmail();

        if (mWiz.getBeaconRegions() != null && mBeaconManager == null) {
            bindBeaconManager();
        }

        if (CloudConstants.USE_GCM_KICK_CHANNEL && GoogleServicesUtils.checkPlayServices(this, true)) {
            GoogleServicesUtils.registerGCMRegistrationId(mService, new GCMRegistrationIdFoundCallback() {
                @Override
                public void idFound(String registrationId) {
                    mGCMRegistrationId = registrationId;
                }
            });
        }
    }

    private void initLocationUsageStep(Typeface faTypeFace) {
        ((TextView) findViewById(R.id.ibeacon_usage_icon)).setTypeface(faTypeFace);

        final String appName = getString(R.string.app_name);
        ((TextView) findViewById(R.id.ibeacon_usage_augment_experience)).setText(getString(R.string
                .ibeacon_usage_augment_experience, appName));
        ((TextView) findViewById(R.id.ibeacon_usage_used_for)).setText(getString(R.string
                .ibeacon_usage_used_for, appName));
        ((TextView) findViewById(R.id.ibeacon_usage_not_used_for)).setText(getString(R.string
                .ibeacon_usage_not_used_for, appName));

        findViewById(R.id.registration_beacon_usage_continue).setOnClickListener(new SafeViewOnClickListener() {
            @Override
            public void safeOnClick(View v) {
                ActivityCompat.requestPermissions(RegistrationActivity2.this, new String[] {Manifest.permission
                        .ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mWiz.requestBeaconRegions(mService);
                }
                mWiz.proceedToNextPage();
                break;
            case PERMISSION_REQUEST_GET_ACCOUNTS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    configureEmailAutoComplete(true);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    private void registerWithAccessToken(final String accessToken) {
        final String timestamp = "" + mWiz.getTimestamp();
        final String deviceId = mWiz.getDeviceId();
        final String registrationId = mWiz.getRegistrationId();
        final String installId = mWiz.getInstallationId();
        // Make call to Rogerthat webfarm
        final ProgressDialog progressDialog = new ProgressDialog(RegistrationActivity2.this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.registration_activating_account, getString(R.string.app_name)));
        progressDialog.setCancelable(false);
        progressDialog.show();
        final SafeRunnable showErrorDialog = new SafeRunnable() {
            @Override
            protected void safeRun() throws Exception {
                T.UI();
                progressDialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(RegistrationActivity2.this);
                builder.setMessage(R.string.registration_facebook_error);
                builder.setPositiveButton(R.string.rogerthat, null);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        };

        mWorkerHandler.post(new SafeRunnable() {
            @Override
            protected void safeRun() throws Exception {
                T.REGISTRATION();
                String version = "1";
                String signature = Security.sha256(version + " " + installId + " " + timestamp + " " + deviceId + " "
                        + registrationId + " " + accessToken + CloudConstants.REGISTRATION_MAIN_SIGNATURE);

                HttpPost httppost = new HttpPost(CloudConstants.REGISTRATION_FACEBOOK_URL);
                try {
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(8);
                    nameValuePairs.add(new BasicNameValuePair("version", version));
                    nameValuePairs.add(new BasicNameValuePair("registration_time", timestamp));
                    nameValuePairs.add(new BasicNameValuePair("device_id", deviceId));
                    nameValuePairs.add(new BasicNameValuePair("registration_id", registrationId));
                    nameValuePairs.add(new BasicNameValuePair("signature", signature));
                    nameValuePairs.add(new BasicNameValuePair("install_id", installId));
                    nameValuePairs.add(new BasicNameValuePair("access_token", accessToken));
                    nameValuePairs.add(new BasicNameValuePair("language", Locale.getDefault().getLanguage()));
                    nameValuePairs.add(new BasicNameValuePair("country", Locale.getDefault().getCountry()));
                    nameValuePairs.add(new BasicNameValuePair("app_id", CloudConstants.APP_ID));
                    nameValuePairs.add(new BasicNameValuePair("use_xmpp_kick", CloudConstants.USE_XMPP_KICK_CHANNEL
                            + ""));
                    nameValuePairs.add(new BasicNameValuePair("GCM_registration_id", mGCMRegistrationId));

                    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    // Execute HTTP Post Request
                    HttpResponse response = mHttpClient.execute(httppost);

                    int statusCode = response.getStatusLine().getStatusCode();
                    HttpEntity entity = response.getEntity();

                    if (entity == null) {
                        mUIHandler.post(showErrorDialog);
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    final Map<String, Object> responseMap = (Map<String, Object>) JSONValue
                            .parse(new InputStreamReader(entity.getContent()));

                    if (statusCode != 200 || responseMap == null) {
                        if (statusCode == 500 && responseMap != null) {
                            final String errorMessage = (String) responseMap.get("error");
                            if (errorMessage != null) {
                                mUIHandler.post(new SafeRunnable() {
                                    @Override
                                    protected void safeRun() throws Exception {
                                        T.UI();
                                        progressDialog.dismiss();
                                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                                RegistrationActivity2.this);
                                        builder.setMessage(errorMessage);
                                        builder.setPositiveButton(R.string.rogerthat, null);
                                        AlertDialog dialog = builder.create();
                                        dialog.show();
                                    }
                                });
                                return;
                            }
                        }
                        mUIHandler.post(showErrorDialog);
                        return;
                    }
                    JSONObject account = (JSONObject) responseMap.get("account");
                    final String email = (String) responseMap.get("email");
                    mAgeAndGenderSet = (Boolean) responseMap.get("age_and_gender_set");
                    final RegistrationInfo info = new RegistrationInfo(email, new Credentials((String) account
                            .get("account"), (String) account.get("password")));
                    mUIHandler.post(new SafeRunnable() {
                        @Override
                        protected void safeRun() throws Exception {
                            T.UI();
                            mWiz.setEmail(email);
                            mWiz.save();
                            hideNotification();
                            tryConnect(
                                    progressDialog,
                                    1,
                                    getString(R.string.registration_establish_connection, email,
                                            getString(R.string.app_name)) + " ", info);
                        }
                    });

                } catch (Exception e) {
                    L.d(e);
                    mUIHandler.post(showErrorDialog);
                }
            }
        });
    }

    private void getOauthRegistrationInfo() {
        final String timestamp = "" + mWiz.getTimestamp();
        final String deviceId = mWiz.getDeviceId();
        final String registrationId = mWiz.getRegistrationId();
        final String installId = mWiz.getInstallationId();
        // Make call to Rogerthat webfarm
        final ProgressDialog progressDialog = new ProgressDialog(RegistrationActivity2.this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.loading));
        progressDialog.setCancelable(false);
        progressDialog.show();
        final SafeRunnable showErrorDialog = new SafeRunnable() {
            @Override
            protected void safeRun() throws Exception {
                T.UI();
                progressDialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(RegistrationActivity2.this);
                builder.setMessage(R.string.registration_error);
                builder.setPositiveButton(R.string.rogerthat, null);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        };

        mWorkerHandler.post(new SafeRunnable() {
            @Override
            protected void safeRun() throws Exception {
                T.REGISTRATION();
                String version = "1";
                String signature = Security.sha256(version + " " + installId + " " + timestamp + " " + deviceId + " "
                        + registrationId + " " + "oauth" + CloudConstants.REGISTRATION_MAIN_SIGNATURE);

                HttpPost httppost = new HttpPost(CloudConstants.REGISTRATION_OAUTH_INFO_URL);
                try {
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(8);
                    nameValuePairs.add(new BasicNameValuePair("version", version));
                    nameValuePairs.add(new BasicNameValuePair("registration_time", timestamp));
                    nameValuePairs.add(new BasicNameValuePair("device_id", deviceId));
                    nameValuePairs.add(new BasicNameValuePair("registration_id", registrationId));
                    nameValuePairs.add(new BasicNameValuePair("signature", signature));
                    nameValuePairs.add(new BasicNameValuePair("install_id", installId));
                    nameValuePairs.add(new BasicNameValuePair("language", Locale.getDefault().getLanguage()));
                    nameValuePairs.add(new BasicNameValuePair("country", Locale.getDefault().getCountry()));
                    nameValuePairs.add(new BasicNameValuePair("app_id", CloudConstants.APP_ID));
                    nameValuePairs.add(new BasicNameValuePair("use_xmpp_kick", CloudConstants.USE_XMPP_KICK_CHANNEL
                            + ""));
                    nameValuePairs.add(new BasicNameValuePair("GCM_registration_id", mGCMRegistrationId));

                    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    // Execute HTTP Post Request
                    HttpResponse response = mHttpClient.execute(httppost);

                    int statusCode = response.getStatusLine().getStatusCode();
                    HttpEntity entity = response.getEntity();

                    if (entity == null) {
                        mUIHandler.post(showErrorDialog);
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    final Map<String, Object> responseMap = (Map<String, Object>) JSONValue
                            .parse(new InputStreamReader(entity.getContent()));

                    if (statusCode != 200 || responseMap == null) {
                        if (statusCode == 500 && responseMap != null) {
                            final String errorMessage = (String) responseMap.get("error");
                            if (errorMessage != null) {
                                mUIHandler.post(new SafeRunnable() {
                                    @Override
                                    protected void safeRun() throws Exception {
                                        T.UI();
                                        progressDialog.dismiss();
                                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                                RegistrationActivity2.this);
                                        builder.setMessage(errorMessage);
                                        builder.setPositiveButton(R.string.rogerthat, null);
                                        AlertDialog dialog = builder.create();
                                        dialog.show();
                                    }
                                });
                                return;
                            }
                        }
                        mUIHandler.post(showErrorDialog);
                        return;
                    }

                    progressDialog.dismiss();

                    final String authorizeUrl = (String) responseMap.get("authorize_url");
                    final String scopes = (String) responseMap.get("scopes");
                    final String state = (String) responseMap.get("state");
                    final String client_id = (String) responseMap.get("client_id");

                    Intent intent = new Intent(RegistrationActivity2.this, OauthActivity.class);
                    intent.putExtra(OauthActivity.STATE, state);
                    intent.putExtra(OauthActivity.CLIENT_ID, client_id);
                    intent.putExtra(OauthActivity.OAUTH_URL, authorizeUrl);
                    intent.putExtra(OauthActivity.SCOPES, scopes);

                    startActivityForResult(intent, START_OAUTH_REQUEST_CODE);


                } catch (Exception e) {
                    L.d(e);
                    mUIHandler.post(showErrorDialog);
                }
            }
        });
    }

    private void registerWithOauthCode(final String code) {
        final String timestamp = "" + mWiz.getTimestamp();
        final String deviceId = mWiz.getDeviceId();
        final String registrationId = mWiz.getRegistrationId();
        final String installId = mWiz.getInstallationId();
        // Make call to Rogerthat webfarm
        final ProgressDialog progressDialog = new ProgressDialog(RegistrationActivity2.this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.loading));
        progressDialog.setCancelable(false);
        progressDialog.show();
        final SafeRunnable showErrorDialog = new SafeRunnable() {
            @Override
            protected void safeRun() throws Exception {
                T.UI();
                progressDialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(RegistrationActivity2.this);
                builder.setMessage(R.string.registration_error);
                builder.setPositiveButton(R.string.rogerthat, null);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        };

        mWorkerHandler.post(new SafeRunnable() {
            @Override
            protected void safeRun() throws Exception {
                T.REGISTRATION();
                String version = "1";
                String signature = Security.sha256(version + " " + installId + " " + timestamp + " " + deviceId + " "
                        + registrationId + " " + code + CloudConstants.REGISTRATION_MAIN_SIGNATURE);

                HttpPost httppost = new HttpPost(CloudConstants.REGISTRATION_OAUTH_REGISTERED_URL);
                try {
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(8);
                    nameValuePairs.add(new BasicNameValuePair("version", version));
                    nameValuePairs.add(new BasicNameValuePair("registration_time", timestamp));
                    nameValuePairs.add(new BasicNameValuePair("device_id", deviceId));
                    nameValuePairs.add(new BasicNameValuePair("registration_id", registrationId));
                    nameValuePairs.add(new BasicNameValuePair("signature", signature));
                    nameValuePairs.add(new BasicNameValuePair("install_id", installId));
                    nameValuePairs.add(new BasicNameValuePair("language", Locale.getDefault().getLanguage()));
                    nameValuePairs.add(new BasicNameValuePair("country", Locale.getDefault().getCountry()));
                    nameValuePairs.add(new BasicNameValuePair("code", code));
                    nameValuePairs.add(new BasicNameValuePair("app_id", CloudConstants.APP_ID));
                    nameValuePairs.add(new BasicNameValuePair("use_xmpp_kick", CloudConstants.USE_XMPP_KICK_CHANNEL
                            + ""));
                    nameValuePairs.add(new BasicNameValuePair("GCM_registration_id", mGCMRegistrationId));

                    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    // Execute HTTP Post Request
                    HttpResponse response = mHttpClient.execute(httppost);

                    int statusCode = response.getStatusLine().getStatusCode();
                    HttpEntity entity = response.getEntity();

                    if (entity == null) {
                        mUIHandler.post(showErrorDialog);
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    final Map<String, Object> responseMap = (Map<String, Object>) JSONValue
                            .parse(new InputStreamReader(entity.getContent()));

                    if (statusCode != 200 || responseMap == null) {
                        if (statusCode == 500 && responseMap != null) {
                            final String errorMessage = (String) responseMap.get("error");
                            if (errorMessage != null) {
                                mUIHandler.post(new SafeRunnable() {
                                    @Override
                                    protected void safeRun() throws Exception {
                                        T.UI();
                                        progressDialog.dismiss();
                                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                                RegistrationActivity2.this);
                                        builder.setMessage(errorMessage);
                                        builder.setPositiveButton(R.string.rogerthat, null);
                                        AlertDialog dialog = builder.create();
                                        dialog.show();
                                    }
                                });
                                return;
                            }
                        }
                        mUIHandler.post(showErrorDialog);
                        return;
                    }

                    progressDialog.dismiss();

                    JSONObject account = (JSONObject) responseMap.get("account");
                    final String email = (String) responseMap.get("email");
                    mAgeAndGenderSet = (Boolean) responseMap.get("age_and_gender_set");
                    final RegistrationInfo info = new RegistrationInfo(email, new Credentials((String) account
                            .get("account"), (String) account.get("password")));
                    mUIHandler.post(new SafeRunnable() {
                        @Override
                        protected void safeRun() throws Exception {
                            T.UI();
                            mWiz.setEmail(email);
                            mWiz.save();
                            hideNotification();
                            tryConnect(
                                    progressDialog,
                                    1,
                                    getString(R.string.registration_establish_connection, email,
                                            getString(R.string.app_name)) + " ", info);
                        }
                    });

                } catch (Exception e) {
                    L.d(e);
                    mUIHandler.post(showErrorDialog);
                }
            }
        });
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        handleScreenOrientation(newConfig.orientation);
    }

    private void handleScreenOrientation(int orientation) {
        boolean isLandscape = orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        for (int id : (AppConstants.FULL_WIDTH_HEADERS ? FULL_WIDTH_ROGERTHAT_LOGOS : NORMAL_WIDTH_ROGERTHAT_LOGOS)) {
            View view = findViewById(id);
            if (view != null) {
                view.setVisibility(isLandscape ? View.GONE : View.VISIBLE);
            }
        }
    }

    private boolean configureEmailAutoComplete() {
        return configureEmailAutoComplete(false);
    }

    private boolean configureEmailAutoComplete(boolean autoShowSuggestions) {
        T.UI();
        final boolean getAccountsPermissionWasGranted = mService.isPermitted(Manifest.permission.GET_ACCOUNTS);
        if (getAccountsPermissionWasGranted) {
            mAccountManager = new AccountManager(this);
            mAccounts = mAccountManager.getAccounts();
            List<String> emails = new ArrayList<String>();
            for (Account account : mAccounts) {
                if (RegexPatterns.EMAIL.matcher(account.name).matches() && !emails.contains(account.name))
                    emails.add(account.name);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.list_item, emails);
            mEnterEmailAutoCompleteTextView.setAdapter(adapter);
            mEnterEmailAutoCompleteTextView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    requestPin();
                }
            });
            mEnterEmailAutoCompleteTextView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    mEnterEmailAutoCompleteTextView.setThreshold(1);
                    return false;
                }
            });
            if (autoShowSuggestions)
                mEnterEmailAutoCompleteTextView.showDropDown();
        }
        mEnterEmailAutoCompleteTextView.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO
                    || (event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    requestPin();
                    return true;
                }
                return false;
            }
        });
        return getAccountsPermissionWasGranted;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == START_OAUTH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data.getBooleanExtra(OauthActivity.SUCCESS, false)) {
                    registerWithOauthCode(data.getStringExtra(OauthActivity.RESULT));
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(RegistrationActivity2.this);
                    builder.setMessage(data.getStringExtra(OauthActivity.RESULT));
                    builder.setPositiveButton(R.string.rogerthat, null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return;
                }
            }
        } else {
            final Session session = Session.getActiveSession();
            if (session != null) {
                session.onActivityResult(this, requestCode, resultCode, data);
            }
        }
    }

    private void onPinEntered() {

        final String pin = mEnterPinEditText.getText().toString();
        // Validate pin code
        if (!RegexPatterns.PIN.matcher(pin).matches()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(RegistrationActivity2.this);
            builder.setMessage(R.string.registration_invalid_pin);
            builder.setPositiveButton(R.string.rogerthat, null);
            AlertDialog dialog = builder.create();
            dialog.show();
            return;
        }
        // Make call to Rogerthat webfarm
        final ProgressDialog progressDialog = new ProgressDialog(RegistrationActivity2.this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.registration_activating_account, getString(R.string.app_name)));
        progressDialog.setCancelable(false);
        progressDialog.show();
        final SafeRunnable showErrorDialog = new SafeRunnable() {
            @Override
            protected void safeRun() throws Exception {
                T.UI();
                progressDialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(RegistrationActivity2.this);
                builder.setMessage(R.string.registration_sending_pin_error);
                builder.setPositiveButton(R.string.rogerthat, null);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        };
        final String email = mWiz.getEmail();
        final String timestamp = "" + mWiz.getTimestamp();
        final String registrationId = mWiz.getRegistrationId();
        final String deviceId = mWiz.getDeviceId();

        mWorkerHandler.post(new SafeRunnable() {
            @Override
            protected void safeRun() throws Exception {
                T.REGISTRATION();
                String version = "2";
                String pinSignature = Security.sha256(version + " " + email + " " + timestamp + " " + deviceId + " "
                        + registrationId + " " + pin + CloudConstants.REGISTRATION_PIN_SIGNATURE);

                HttpPost httppost = new HttpPost(CloudConstants.REGISTRATION_PIN_URL);
                try {
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(6);
                    nameValuePairs.add(new BasicNameValuePair("version", version));
                    nameValuePairs.add(new BasicNameValuePair("email", email));
                    nameValuePairs.add(new BasicNameValuePair("registration_time", timestamp));
                    nameValuePairs.add(new BasicNameValuePair("device_id", deviceId));
                    nameValuePairs.add(new BasicNameValuePair("registration_id", registrationId));
                    nameValuePairs.add(new BasicNameValuePair("pin_code", pin));
                    nameValuePairs.add(new BasicNameValuePair("pin_signature", pinSignature));
                    nameValuePairs.add(new BasicNameValuePair("request_id", UUID.randomUUID().toString()));
                    nameValuePairs.add(new BasicNameValuePair("app_id", CloudConstants.APP_ID));
                    nameValuePairs.add(new BasicNameValuePair("use_xmpp_kick", CloudConstants.USE_XMPP_KICK_CHANNEL
                            + ""));
                    nameValuePairs.add(new BasicNameValuePair("GCM_registration_id", mGCMRegistrationId));

                    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    // Execute HTTP Post Request
                    HttpResponse response = mHttpClient.execute(httppost);
                    int statusCode = response.getStatusLine().getStatusCode();
                    HttpEntity entity = response.getEntity();
                    if (statusCode != 200 || entity == null) {
                        mUIHandler.post(showErrorDialog);
                        return;
                    }
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> responseMap = (Map<String, Object>) org.json.simple.JSONValue
                            .parse(new InputStreamReader(entity.getContent()));
                    if ("success".equals(responseMap.get("result"))) {

                        JSONObject account = (JSONObject) responseMap.get("account");
                        final RegistrationInfo info = new RegistrationInfo(email, new Credentials((String) account
                                .get("account"), (String) account.get("password")));

                        mAgeAndGenderSet = (Boolean) responseMap.get("age_and_gender_set");
                        mUIHandler.post(new SafeRunnable() {
                            @Override
                            protected void safeRun() throws Exception {
                                T.UI();
                                hideNotification();
                                tryConnect(
                                        progressDialog,
                                        1,
                                        getString(R.string.registration_establish_connection, email,
                                                getString(R.string.app_name)) + " ", info);
                            }
                        });
                    } else {

                        final long attempts_left = (Long) responseMap.get("attempts_left");
                        mUIHandler.post(new SafeRunnable() {
                            @Override
                            protected void safeRun() throws Exception {
                                T.UI();
                                progressDialog.dismiss();
                                if (attempts_left > 0) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(RegistrationActivity2.this);
                                    builder.setMessage(getString(R.string.registration_incorrect_pin, email));
                                    builder.setTitle(getString(R.string.registration_incorrect_pin_dialog_title));
                                    builder.setPositiveButton(R.string.rogerthat, null);
                                    AlertDialog dialog = builder.create();
                                    dialog.show();
                                    mEnterPinEditText.setText("");
                                } else {
                                    hideNotification();
                                    new AlertDialog.Builder(RegistrationActivity2.this)
                                            .setMessage(getString(R.string.registration_no_attempts_left))
                                            .setCancelable(true).setPositiveButton(R.string.try_again, null).create()
                                            .show();
                                    mWiz.reInit();
                                    mWiz.goBackToPrevious();
                                    return;
                                }
                            }
                        });

                    }
                } catch (Exception e) {
                    L.d(e);
                    mUIHandler.post(showErrorDialog);
                }
            }
        });

    }

    private void requestPin() {
        final String email = mEnterEmailAutoCompleteTextView.getText().toString().toLowerCase().trim();

        // Validate input
        if (!RegexPatterns.EMAIL.matcher(email).matches()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(RegistrationActivity2.this);
            builder.setMessage(R.string.registration_email_not_valid);
            builder.setPositiveButton(R.string.rogerthat, null);
            AlertDialog dialog = builder.create();
            dialog.show();
            return;
        }
        // Check network connectivity
        if (!mService.getNetworkConnectivityManager().isConnected()) {
            UIUtils.showNoNetworkDialog(this);
            return;
        }
        UIUtils.hideKeyboard(this, mEnterEmailAutoCompleteTextView);
        sendRegistrationRequest(email);
    }

    private void handleEnterEmail() {
        T.UI();
        final Button continueButton = (Button) findViewById(R.id.registration_continue_email);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View button) {
                T.UI();
                requestPin();
            }
        });
    }

    private void addAgreeTOSHandler() {
        mWiz.addPageHandler(new Wizard.PageHandler() {

            @Override
            public void pageDisplayed(Button back, Button next, ViewFlipper switcher) {
            }

            @Override
            public String getTitle() {
                return null;
            }

            @Override
            public boolean beforeNextClicked(Button back, Button next, ViewFlipper switcher) {
                return false;
            }

            @Override
            public boolean beforeBackClicked(Button back, Button next, ViewFlipper switcher) {
                return false;
            }
        });
    }

    private void addIBeaconUsageHandler() {
        mWiz.addPageHandler(new Wizard.PageHandler() {

            @Override
            public void pageDisplayed(Button back, Button next, ViewFlipper switcher) {
                mEnterEmailAutoCompleteTextView.setThreshold(1000); // Prevent popping up automatically
                if (Build.VERSION.SDK_INT < 18 || !supportsBeacons() || mService.isPermitted(Manifest.permission
                        .ACCESS_COARSE_LOCATION)) {
                    mWiz.proceedToNextPage(); // Skip the iBeacon usage step
                }
            }

            @Override
            public String getTitle() {
                return null;
            }

            @Override
            public boolean beforeNextClicked(Button back, Button next, ViewFlipper switcher) {
                return false;
            }

            @Override
            public boolean beforeBackClicked(Button back, Button next, ViewFlipper switcher) {
                return false;
            }
        });
    }

    @SuppressWarnings("all")
    private void addChooseLoginMethodHandler() {
        mWiz.addPageHandler(new Wizard.PageHandler() {

            @Override
            public void pageDisplayed(Button back, Button next, ViewFlipper switcher) {
                if (AppConstants.getRegistrationType() == AppConstants.REGISTRATION_TYPE_OAUTH) {
                    mWiz.proceedToNextPage();
                } else {
                    if (AppConstants.FACEBOOK_APP_ID == null || !AppConstants.FACEBOOK_REGISTRATION) {
                        sendRegistrationStep(RegistrationWizard2.REGISTRATION_STEP_EMAIL_LOGIN);
                        mWiz.proceedToNextPage();
                    } else {
                        mEnterEmailAutoCompleteTextView.setThreshold(1000); // Prevent popping up automatically
                    }
                }
            }

            @Override
            public String getTitle() {
                return null;
            }

            @Override
            public boolean beforeNextClicked(Button back, Button next, ViewFlipper switcher) {
                return false;
            }

            @Override
            public boolean beforeBackClicked(Button back, Button next, ViewFlipper switcher) {
                return false;
            }
        });
    }

    private void addOauthMethodHandler() {
        mWiz.addPageHandler(new Wizard.PageHandler() {

            @Override
            public void pageDisplayed(Button back, Button next, ViewFlipper switcher) {
                if (AppConstants.getRegistrationType() == AppConstants.REGISTRATION_TYPE_OAUTH) {
                    mEnterEmailAutoCompleteTextView.setThreshold(1000); // Prevent popping up automatically
                } else {
                    sendRegistrationStep(RegistrationWizard2.REGISTRATION_STEP_EMAIL_LOGIN);
                    mWiz.proceedToNextPage();
                }
            }

            @Override
            public String getTitle() {
                return null;
            }

            @Override
            public boolean beforeNextClicked(Button back, Button next, ViewFlipper switcher) {
                return false;
            }

            @Override
            public boolean beforeBackClicked(Button back, Button next, ViewFlipper switcher) {
                return false;
            }
        });
    }

    private void addEnterEmailHandler() {
        mWiz.addPageHandler(new Wizard.PageHandler() {

            @Override
            public void pageDisplayed(Button back, Button next, ViewFlipper switcher) {
                mEnterEmailAutoCompleteTextView.setThreshold(1);
                mEnterEmailAutoCompleteTextView.requestFocus();
                UIUtils.showKeyboard(RegistrationActivity2.this);
            }

            @Override
            public String getTitle() {
                return null;
            }

            @Override
            public boolean beforeNextClicked(Button back, Button next, ViewFlipper switcher) {
                return false;
            }

            @Override
            public boolean beforeBackClicked(Button back, Button next, ViewFlipper switcher) {
                if (AppConstants.FACEBOOK_APP_ID == null || !AppConstants.FACEBOOK_REGISTRATION)
                    return false;
                return true;
            }
        });
    }

    private void addEnterPinHandler() {
        mWiz.addPageHandler(new Wizard.PageHandler() {
            @Override
            public void pageDisplayed(Button back, Button next, ViewFlipper switcher) {
                mEnterPinEditText.setText("");
                mEnterEmailAutoCompleteTextView.setThreshold(1000); // Prevent popping up automatically

                final String f;
                if (CloudConstants.isRogerthatApp()) {
                    f = "<font color=\"#39c\">%s</font>";
                } else {
                    f = "%s";
                }
                final TextView tv = (TextView) findViewById(R.id.registration_pin_was_mailed);
                tv.setText(Html.fromHtml(getString(R.string.registration_pin_was_mailed,
                        String.format(f, mWiz.getEmail()))));
            }

            @Override
            public String getTitle() {
                return null;
            }

            @Override
            public boolean beforeNextClicked(Button back, Button next, ViewFlipper switcher) {
                return true;
            }

            @Override
            public boolean beforeBackClicked(Button back, Button next, ViewFlipper switcher) {
                return false;
            }
        });
    }

    private void setFinishHandler() {
        mWiz.setOnFinish(new SafeRunnable() {

            private void startMainActivity() {
                Intent intent = new Intent(RegistrationActivity2.this, MainActivity.class);
                intent.setAction(MainActivity.ACTION_REGISTERED);
                startActivity(intent);
                RegistrationActivity2.this.finish();
            }

            private void showPopup(Configuration cfg) {
                if (mDiscoveredBeacons != null) {
                    Intent intent = new Intent(RegistrationActivity2.this, MainActivity.class);
                    intent.setAction(MainActivity.ACTION_SHOW_DETECTED_BEACONS);
                    intent.putExtra(DetectedBeaconActivity.EXTRA_DETECTED_BEACONS, mDiscoveredBeacons);
                    intent.putExtra(DetectedBeaconActivity.EXTRA_AGE_AND_GENDER_SET, mAgeAndGenderSet);
                    intent.setFlags(MainActivity.FLAG_CLEAR_STACK);
                    startActivity(intent);
                    RegistrationActivity2.this.finish();
                    return;
                }
                if (AppConstants.PROFILE_SHOW_GENDER_AND_BIRTHDATE && !mAgeAndGenderSet) {
                    Intent intent = new Intent(RegistrationActivity2.this, MainActivity.class);
                    intent.setAction(MainActivity.ACTION_COMPLETE_PROFILE);
                    intent.setFlags(MainActivity.FLAG_CLEAR_STACK);
                    startActivity(intent);
                    RegistrationActivity2.this.finish();
                    return;
                }
                startMainActivity();
            }

            @Override
            protected void safeRun() throws Exception {
                T.UI();
                if (mEnterPinEditText != null) {
                    InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    mgr.hideSoftInputFromWindow(mEnterPinEditText.getWindowToken(), 0);
                }

                Configuration cfg = mService.getConfigurationProvider().getConfiguration(RegistrationWizard2.CONFIGKEY);

                if (cfg != null && cfg.get(INVITOR_SECRET_CONFIGKEY, null) != null
                        && cfg.get(INVITOR_CODE_CONFIGKEY, null) != null) {
                    // User pressed an invitation link with secret
                    startMainActivity();
                } else {
                    showPopup(cfg);
                }
            }
        });
    }

    @Override
    protected void onServiceUnbound() {
    }

    private void createWorkerThread() {
        T.UI();
        mWorkerThread = new HandlerThread("rogerthat_registration_worker");
        mWorkerThread.start();
        mWorkerLooper = mWorkerThread.getLooper();
        mWorkerHandler = new Handler(mWorkerLooper);
        mWorkerHandler.post(new SafeRunnable() {
            @Override
            public void safeRun() {
                T.setRegistrationThread("RegistrationProcedureActivity.createWorkerThread()");
            }
        });
    }

    private void closeWorkerThread() {
        T.UI();
        final Looper looper = mWorkerHandler.getLooper();
        if (looper != null)
            looper.quit();

        try {
            mWorkerThread.join();
        } catch (InterruptedException e) {
            L.bug(e);
        }

        mWorkerHandler = null;
        mWorkerThread = null;
        T.resetRegistrationThreadId();

    }

    private void tryConnect(final ProgressDialog pd, final int attempt, final String statusMessage,
        final RegistrationInfo info) {
        T.UI();
        final Pausable pausable = this;

        if (attempt > XMPP_MAX_NUM_ATTEMPTS) {
            pd.dismiss();

            new AlertDialog.Builder(RegistrationActivity2.this).setMessage(getString(R.string.registration_error))
                .setCancelable(true).setPositiveButton(R.string.try_again, null).create().show();
            mWiz.reInit();
            mWiz.goBackToPrevious();
            return;
        }
        pd.setMessage(statusMessage + attempt);
        if (!pd.isShowing())
            pd.show();
        L.d("Registration attempt #" + attempt);

        final String xmppServiceName = info.mCredentials.getXmppServiceName();
        final String xmppAccount = info.mCredentials.getXmppAccount();
        final String xmppPassword = info.mCredentials.getPassword();

        final ConfigurationProvider cp = mService.getConfigurationProvider();
        Configuration cfg = cp.getConfiguration(RegistrationWizard2.CONFIGKEY);
        final String invitorCode = (cfg == null) ? null : cfg.get(INVITOR_CODE_CONFIGKEY, null);
        final String invitorSecret = (cfg == null) ? null : cfg.get(INVITOR_SECRET_CONFIGKEY, null);

        Runnable runnable = new SafeRunnable() {

            @Override
            public void safeRun() {
                T.REGISTRATION();
                try {

                    if (CloudConstants.USE_XMPP_KICK_CHANNEL) {

                        final ConnectionConfiguration xmppConfig = new XMPPConfigurationFactory(cp,
                            mService.getNetworkConnectivityManager(), null)
                            .getSafeXmppConnectionConfiguration(xmppServiceName);
                        final XMPPConnection xmppCon = new XMPPConnection(xmppConfig);

                        xmppCon.setLogger(new Logger() {
                            @Override
                            public void log(String message) {
                                L.d(message);
                            }
                        });
                        xmppCon.connect();
                        xmppCon.login(xmppAccount, xmppPassword);

                        final Thread t2 = new Thread(new SafeRunnable() {
                            @Override
                            protected void safeRun() throws Exception {
                                L.d("REG Before disconnect (on separate thread) - xmpp-" + xmppCon.hashCode());
                                xmppCon.disconnect();
                                L.d("REG After disconnect (on separate thread) - xmpp-" + xmppCon.hashCode());
                            }
                        });
                        t2.setDaemon(true);
                        t2.start();

                    }

                    postFinishRegistration(info.mCredentials.getUsername(), info.mCredentials.getPassword(),
                        invitorCode, invitorSecret);

                    mUIHandler.post(new SafeRunnable(pausable) {

                        @Override
                        protected void safeRun() throws Exception {
                            T.UI();
                            mWiz.setCredentials(info.mCredentials);

                            if (CloudConstants.USE_GCM_KICK_CHANNEL && !"".equals(mGCMRegistrationId)) {
                                GoogleServicesUtils.saveGCMRegistrationId(mService, mGCMRegistrationId);
                            }

                            mService.setCredentials(mWiz.getCredentials());
                            mService.setRegisteredInConfig(true);

                            final Intent launchServiceIntent = new Intent(RegistrationActivity2.this, MainService.class);
                            launchServiceIntent.putExtra(MainService.START_INTENT_JUST_REGISTERED, true);
                            launchServiceIntent.putExtra(MainService.START_INTENT_MY_EMAIL, mWiz.getEmail());
                            RegistrationActivity2.this.startService(launchServiceIntent);
                            stopRegistrationService();
                            pd.dismiss();

                            mWiz.finish(); // finish
                        }
                    });

                } catch (Exception e) {
                    L.d("Exception while trying to end the registration process", e);
                    mUIHandler.post(new SafeRunnable(pausable) {

                        @Override
                        protected void safeRun() throws Exception {
                            T.UI();
                            tryConnect(pd, attempt + 1, statusMessage, info);
                        }
                    });
                }
            }
        };
        if (attempt == 1) {
            mWorkerHandler.post(runnable);
        } else {
            mWorkerHandler.postDelayed(runnable, XMPP_CHECK_DELAY_MILLIS);
        }
    }

    private String getMobileInfo() {
        T.REGISTRATION();
        MobileInfo info = SystemPlugin.gatherMobileInfo(mService);
        String json = JSONValue.toJSONString(info.toJSONMap());
        return json;
    }

    @SuppressWarnings("unchecked")
    private void postFinishRegistration(final String username, final String password, final String invitorCode,
        final String invitorSecret) throws ClientProtocolException, IOException {
        T.REGISTRATION();
        final String mobileInfo = getMobileInfo();
        HttpClient httpClient = HTTPUtil.getHttpClient();
        final HttpPost httpPost = new HttpPost(CloudConstants.REGISTRATION_FINISH_URL);
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
        formParams.add(new BasicNameValuePair("mobileInfo", mobileInfo));
        formParams.add(new BasicNameValuePair("account", username));
        formParams.add(new BasicNameValuePair("password", password));
        formParams.add(new BasicNameValuePair("app_id", CloudConstants.APP_ID));
        org.json.simple.JSONArray accounts = new org.json.simple.JSONArray();
        if (mAccounts != null) {
            for (Account acc : mAccounts) {
                Map<String, String> jacc = new LinkedHashMap<String, String>();
                jacc.put("type", acc.type);
                jacc.put("name", acc.name);
                accounts.add(jacc);
            }
        }
        formParams.add(new BasicNameValuePair("accounts", accounts.toString()));

        formParams.add(new BasicNameValuePair("invitor_code", invitorCode));
        formParams.add(new BasicNameValuePair("invitor_secret", invitorSecret));

        org.json.simple.JSONArray beacons = new org.json.simple.JSONArray();
        for (BeaconDiscoveredRequestTO bdr : mWiz.getDetectedBeacons()) {
            beacons.add(bdr.toJSONMap());
        }
        formParams.add(new BasicNameValuePair("beacons", beacons.toString()));

        httpPost.setEntity(new UrlEncodedFormEntity(formParams, HTTP.UTF_8));
        L.d("before http final post");
        HttpResponse response = httpClient.execute(httpPost);
        L.d("after http final post");
        final int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode != HttpStatus.SC_OK) {
            throw new IOException("HTTP request resulted in status code " + responseCode);
        }

        L.d("finish_registration call sent");
        HttpEntity httpEntity = response.getEntity();
        if (httpEntity == null) {
            throw new IOException("Response of '/unauthenticated/mobi/registration/finish' was null");
        }

        final Map<String, Object> responseMap = (Map<String, Object>) JSONValue.parse(new InputStreamReader(httpEntity
            .getContent()));
        if (responseMap == null) {
            throw new IOException("HTTP request responseMap was null");
        }

        JSONArray beaconRegions = (JSONArray) responseMap.get("discovered_beacons");
        if (beaconRegions != null && beaconRegions.size() > 0) {
            mDiscoveredBeacons = JSONValue.toJSONString(beaconRegions);
        } else {
            mDiscoveredBeacons = null;
        }
    }

    private void sendRegistrationRequest(final String email) {
        final ProgressDialog progressDialog = new ProgressDialog(RegistrationActivity2.this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.registration_sending_email, email));
        progressDialog.setCancelable(true);
        progressDialog.show();
        final SafeRunnable showErrorDialog = new SafeRunnable() {
            @Override
            protected void safeRun() throws Exception {
                T.UI();
                progressDialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(RegistrationActivity2.this);
                builder.setMessage(R.string.error_please_try_again);
                builder.setPositiveButton(R.string.rogerthat, null);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        };
        final String timestamp = "" + mWiz.getTimestamp();
        final String registrationId = mWiz.getRegistrationId();
        final String deviceId = mWiz.getDeviceId();

        mWorkerHandler.post(new SafeRunnable() {
            @Override
            protected void safeRun() throws Exception {
                T.REGISTRATION();
                String version = "2";
                String requestSignature = Security.sha256(version + email + " " + timestamp + " " + deviceId + " "
                    + registrationId + " " + CloudConstants.REGISTRATION_EMAIL_SIGNATURE);

                HttpPost httppost = new HttpPost(CloudConstants.REGISTRATION_REQUEST_URL);
                try {
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(6);
                    nameValuePairs.add(new BasicNameValuePair("version", version));
                    nameValuePairs.add(new BasicNameValuePair("email", email));
                    nameValuePairs.add(new BasicNameValuePair("registration_time", timestamp));
                    nameValuePairs.add(new BasicNameValuePair("device_id", deviceId));
                    nameValuePairs.add(new BasicNameValuePair("registration_id", registrationId));
                    nameValuePairs.add(new BasicNameValuePair("request_signature", requestSignature));
                    nameValuePairs.add(new BasicNameValuePair("install_id", mWiz.getInstallationId()));
                    nameValuePairs.add(new BasicNameValuePair("request_id", UUID.randomUUID().toString()));
                    nameValuePairs.add(new BasicNameValuePair("language", Locale.getDefault().getLanguage()));
                    nameValuePairs.add(new BasicNameValuePair("country", Locale.getDefault().getCountry()));
                    nameValuePairs.add(new BasicNameValuePair("app_id", CloudConstants.APP_ID));
                    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    // Execute HTTP Post Request
                    HttpResponse response = mHttpClient.execute(httppost);

                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == 200) {
                        mUIHandler.post(new SafeRunnable() {
                            @Override
                            protected void safeRun() throws Exception {
                                T.UI();
                                mWiz.setEmail(email);
                                mWiz.save();
                                progressDialog.dismiss();
                                mWiz.proceedToNextPage();
                                showNotification();
                            }
                        });
                    } else if (statusCode == 502) {

                        final HttpEntity entity = response.getEntity();
                        mUIHandler.post(new SafeRunnable() {
                            @Override
                            protected void safeRun() throws Exception {
                                T.UI();
                                progressDialog.dismiss();
                                AlertDialog.Builder builder = new AlertDialog.Builder(RegistrationActivity2.this);

                                boolean stringSet = false;
                                if (entity != null) {
                                    @SuppressWarnings("unchecked")
                                    final Map<String, Object> responseMap = (Map<String, Object>) org.json.simple.JSONValue
                                        .parse(new InputStreamReader(entity.getContent()));

                                    if (responseMap != null) {
                                        String result = (String) responseMap.get("result");
                                        if (result != null) {
                                            builder.setMessage(result);
                                            stringSet = true;
                                        }
                                    }
                                }

                                if (!stringSet) {
                                    builder.setMessage(R.string.registration_email_not_valid);
                                }

                                builder.setPositiveButton(R.string.rogerthat, null);
                                AlertDialog dialog = builder.create();
                                dialog.show();
                            }
                        });
                    } else {
                        mUIHandler.post(showErrorDialog);
                    }

                } catch (ClientProtocolException e) {
                    L.d(e);
                    mUIHandler.post(showErrorDialog);
                } catch (IOException e) {
                    L.d(e);
                    mUIHandler.post(showErrorDialog);
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mWiz.getPosition() == 2) {
                mWiz.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void sendRegistrationStep(final String step) {
        new SafeAsyncTask<Object, Object, Object>() {

            @Override
            protected Object safeDoInBackground(Object... params) {
                final HttpPost httpPost = new HttpPost(CloudConstants.REGISTRATION_LOG_STEP_URL);
                httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
                List<NameValuePair> formParams = new ArrayList<NameValuePair>();
                formParams.add(new BasicNameValuePair("step", step));
                formParams.add(new BasicNameValuePair("install_id", mWiz.getInstallationId()));

                UrlEncodedFormEntity entity;
                try {
                    entity = new UrlEncodedFormEntity(formParams, HTTP.UTF_8);
                } catch (UnsupportedEncodingException e) {
                    L.bug(e);
                    return false;
                }
                httpPost.setEntity(entity);
                L.d("Sending registration step: " + step);
                HttpResponse response;
                try {
                    response = HTTPUtil.getHttpClient(HTTP_TIMEOUT, HTTP_RETRY_COUNT).execute(httpPost);
                } catch (ClientProtocolException e) {
                    L.bug(e);
                    return false;
                } catch (SSLException e) {
                    L.bug(e);
                    return false;
                } catch (IOException e) {
                    L.e(e);
                    return false;
                }

                if (response.getEntity() != null) {
                    try {
                        response.getEntity().consumeContent();
                    } catch (IOException e) {
                        L.bug(e);
                        return false;
                    }
                }

                L.d("Registration step " + step + " sent");
                final int responseCode = response.getStatusLine().getStatusCode();
                if (responseCode != HttpStatus.SC_OK) {
                    L.bug("HTTP request resulted in status code " + responseCode);
                    return false;
                }

                return true;
            }

            @Override
            protected void safeOnPostExecute(Object result) {
            }

            @Override
            protected void safeOnCancelled(Object result) {
            }

            @Override
            protected void safeOnProgressUpdate(Object... values) {
            }

            @Override
            protected void safeOnPreExecute() {
            }

        }.execute();
    }

    @TargetApi(18)
    private boolean supportsBeacons() {
        boolean available = false;
        try {
            available = BeaconManager.getInstanceForApplication(mService).checkAvailability();
        } catch (NullPointerException ex) {
            L.i("BLE not available", ex);
        } catch (BleNotAvailableException ex) {
            L.d(ex.getMessage());
        }
        if (!available) {
            L.d("Bluetooth is not enabled");
        }
        return available;
    }

    @TargetApi(18)
    private void bindBeaconManager() {
        if (CloudConstants.DEBUG_LOGGING) {
            LogManager.setLogger(Loggers.verboseLogger());
            LogManager.setVerboseLoggingEnabled(true);
        } else {
            LogManager.setLogger(Loggers.empty());
            LogManager.setVerboseLoggingEnabled(false);
        }

        if (!mService.isPermitted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            L.w("ACCESS_COARSE_LOCATION is not permitted!");
            return;
        }

        mBeaconManager = BeaconManager.getInstanceForApplication(mService);
        if (!mBeaconManager.isAnyConsumerBound()) {
            mBeaconManager.getBeaconParsers().add(
                new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        }

        try {
            if (!mBeaconManager.checkAvailability()) {
                L.d("Bluetooth is not enabled");
            }
            mBeaconManager.bind(mService);
            mBeaconManager.setBackgroundMode(!mService.getScreenIsOn());
        } catch (NullPointerException ex) {
            L.i("BLE not available", ex);
        } catch (BleNotAvailableException ex) {
            L.d(ex.getMessage());
        }
    }

    private void startMonitoringBeaconRegions() {
        mBeaconManager.setMonitorNotifier(getBeaconMonitorNotifier());
        mBeaconManager.setRangeNotifier(getBeaconRangeNotifier());

        final BeaconRegionTO[] beaconRegions = mWiz.getBeaconRegions();
        if (beaconRegions != null) {
            for (BeaconRegionTO br : beaconRegions) {
                String regionId = BeaconRegion.getUniqueRegionId(br);
                L.d("Start monitoring region: " + regionId);

                try {
                    mBeaconManager.startMonitoringBeaconsInRegion(new Region(regionId, BeaconRegion.getId1(br),
                        BeaconRegion.getId2(br), BeaconRegion.getId3(br)));
                } catch (RemoteException e) {
                    L.e(e);
                }
            }
        }
    }

    private RangeNotifier getBeaconRangeNotifier() {
        return new RangeNotifier() {

            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> iBeacons, Region region) {
                L.v("\n- Current beacons in region: " + region.getUniqueId() + " (" + iBeacons.size() + "):");
                for (Beacon b : iBeacons) {
                    String uuid = b.getId1().toUuidString();
                    int major = b.getId2().toInt();
                    int minor = b.getId3().toInt();
                    mWiz.addDetectedBeacon(uuid, major, minor);
                }
            }
        };
    }

    private MonitorNotifier getBeaconMonitorNotifier() {
        return new MonitorNotifier() {

            @Override
            public void didEnterRegion(Region region) {
                L.d("didEnterRegion: " + region.getUniqueId());
                try {
                    mBeaconManager.startRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    L.e(e);
                }
            }

            @Override
            public void didExitRegion(Region region) {
                L.d("didExitRegion: " + region.getUniqueId());
                try {
                    mBeaconManager.stopRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    L.e(e);
                }
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
            }
        };
    }
}