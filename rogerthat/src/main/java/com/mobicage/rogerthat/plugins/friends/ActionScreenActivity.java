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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.simple.JSONValue;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.facebook.widget.WebDialog;
import com.mobicage.rogerth.at.R;
import com.mobicage.rogerthat.ServiceBoundActivity;
import com.mobicage.rogerthat.plugins.history.HistoryItem;
import com.mobicage.rogerthat.plugins.messaging.AttachmentViewerActivity;
import com.mobicage.rogerthat.plugins.messaging.BrandingFailureException;
import com.mobicage.rogerthat.plugins.messaging.BrandingMgr;
import com.mobicage.rogerthat.plugins.messaging.BrandingMgr.BrandingResult;
import com.mobicage.rogerthat.plugins.messaging.MessagingPlugin;
import com.mobicage.rogerthat.plugins.messaging.ServiceMessageDetailActivity;
import com.mobicage.rogerthat.plugins.scan.GetUserInfoResponseHandler;
import com.mobicage.rogerthat.plugins.scan.ProcessScanActivity;
import com.mobicage.rogerthat.plugins.trackme.DiscoveredBeaconProximity;
import com.mobicage.rogerthat.plugins.trackme.TrackmePlugin;
import com.mobicage.rogerthat.util.FacebookUtils;
import com.mobicage.rogerthat.util.FacebookUtils.PermissionType;
import com.mobicage.rogerthat.util.logging.L;
import com.mobicage.rogerthat.util.system.SafeBroadcastReceiver;
import com.mobicage.rogerthat.util.system.SafeRunnable;
import com.mobicage.rogerthat.util.system.T;
import com.mobicage.rogerthat.util.system.TaggedWakeLock;
import com.mobicage.rogerthat.util.ui.UIUtils;
import com.mobicage.rpc.config.AppConstants;
import com.mobicage.rpc.config.CloudConstants;
import com.mobicage.rpc.newxmpp.XMPPKickChannel;
import com.mobicage.to.friends.GetUserInfoRequestTO;

public class ActionScreenActivity extends ServiceBoundActivity {

    public static final String BRANDING_KEY = "branding_key";
    public static final String SERVICE_EMAIL = "service_email";
    public static final String ITEM_TAG_HASH = "item_tag_hash";
    public static final String ITEM_LABEL = "item_label";
    public static final String ITEM_COORDS = "item_coords";
    public static final String CONTEXT_MATCH = "context";
    public static final String RUN_IN_BACKGROUND = "run_in_background";

    private static final String POST_ACTION_PATH_FEED = "me/feed";

    private static final String POKE = "poke://";

    private static final String FACEBOOK_TYPE_CANCEL = "CANCEL";
    private static final String FACEBOOK_TYPE_ERROR = "ERROR";

    private static final Map<String, Object> FACEBOOK_MAP_CANCEL = new HashMap<String, Object>();

    private static final int CHECKIN_REQUEST_CODE = 111;
    private boolean mIsHtmlContent = true;
    private boolean mInfoSet = false;
    private boolean mApiResultHandlerSet = false;
    private boolean mJavascriptBackBtnListener = false;
    private String mCurrentBackPressedId = null;
    private long mLasttimeBackPressed = 0;

    private WebView mBranding;
    private WebView mBrandingHttp;
    private String mBrandingKey;
    private String mContextMatch = "";
    private String mServiceEmail;
    private String mItemTagHash;
    private String mItemLabel;
    private long[] mItemCoords;
    private Friend mServiceFriend;
    private boolean mRunInBackground;

    private volatile BrandingResult mBrandingResult = null;
    private TaggedWakeLock mWakeLock = null;
    private boolean mWakelockEnabled = false;

    private MessagingPlugin mMessagingPlugin;
    private FriendsPlugin mFriendsPlugin;

    private MediaPlayer mSoundMediaPlayer = null;
    private HandlerThread mSoundThread = null;
    private Handler mSoundHandler = null;

    private QRCodeScanner mQRCodeScanner = null;

    private boolean mIsListeningBacklogConnectivityChanged = false;
    private int mLastBacklogStatus = 0;

    static {
        FACEBOOK_MAP_CANCEL.put("type", FACEBOOK_TYPE_CANCEL);
    }

    private class JSInterface extends Object {

        private final ActionScreenActivity mContext;

        private JSInterface(ActionScreenActivity context) {
            this.mContext = context;
        }

        @JavascriptInterface
        @SuppressWarnings("unused")
        public void hideKeyboard() {
            InputMethodManager inputManager = (InputMethodManager) mContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(mContext.getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }

        @JavascriptInterface
        @SuppressWarnings("unused")
        public void sendApiCall(final String method, final String params, final String tag) {
            mFriendsPlugin.sendApiCall(mServiceEmail, mItemTagHash, method, params, tag);
        }

        @JavascriptInterface
        @SuppressWarnings("unused")
        public int version() {
            return 1;
        }

        @JavascriptInterface
        @SuppressWarnings("unused")
        public void invoke(final String action, final String parameters) throws JSONException {
            JSONObject params = parameters == null ? null : (JSONObject) new JSONTokener(parameters).nextValue();
            L.i("Received action '" + action + "' with params " + params);

            // FACEBOOK
            if ("facebook/login".equals(action)) {
                if (params == null) {
                    L.w("Expected params != null");
                    return;
                }
                facebookLogin(params.getString("id"), params.getString("properties"));
            } else if ("facebook/post".equals(action)) {
                if (params == null) {
                    L.w("Expected params != null");
                    return;
                }
                facebookPost(params.getString("id"), params.getString("postParams"));
            } else if ("facebook/ticker".equals(action)) {
                if (params == null) {
                    L.w("Expected params != null");
                    return;
                }
                facebookTicker(params.getString("id"), params.getString("type"), params.getString("postParams"));
            }
            // BACK
            else if ("back/registerListener".equals(action)) {
                mJavascriptBackBtnListener = true;
            } else if ("back/unregisterListener".equals(action)) {
                mJavascriptBackBtnListener = false;
            } else if ("back/backPressedCallback".equals(action)) {
                if (params == null) {
                    L.w("Expected params != null");
                    return;
                }
                if (mCurrentBackPressedId != null && mCurrentBackPressedId.equals(params.getString("requestId"))) {

                    mCurrentBackPressedId = null;
                    final boolean handled = "true".equalsIgnoreCase(params.getString("handled"));
                    L.d("Javascript " + (handled ? "handled" : "did not handle") + " backPressed");
                    if (!handled && !isFinishing()) {
                        if (T.getThreadType() == T.UI) {
                            finish();
                        } else {
                            mService.postAtFrontOfUIHandler(new SafeRunnable() {
                                @Override
                                protected void safeRun() throws Exception {
                                    finish();
                                }
                            });
                        }
                    }
                }
            }
            // LOG
            else if ("log/".equals(action)) {
                if (params == null) {
                    L.w("Expected params != null");
                    return;
                }
                final String e = params.getString("e");
                if (e != null) {
                    L.bug("ScreenBrandingException:\n- Exception logged by screenBranding of " + mServiceEmail
                        + "\n- Service menu item name: " + mItemLabel + "\n- Service menu item coords: "
                        + Arrays.toString(mItemCoords) + "\n" + e);
                }
            }
            // API
            else if ("api/resultHandlerConfigured".equals(action)) {
                mApiResultHandlerSet = true;
                deliverAllApiResults();
            }
            // USER
            else if ("user/put".equals(action)) {
                if (params == null) {
                    L.w("Expected params != null");
                    return;
                }
                mFriendsPlugin.putUserData(mServiceEmail, params.getString("u"));
            }
            // SERVICE
            else if ("service/getBeaconsInReach".equals(action)) {
                if (params == null) {
                    L.w("Expected params != null");
                    return;
                }
                String requestId = params.getString("id");

                TrackmePlugin trackmePlugin = mService.getPlugin(TrackmePlugin.class);
                List<DiscoveredBeaconProximity> beaconsInReach = trackmePlugin.getBeaconsInReach(mServiceEmail);
                Map<String, Object> result = new HashMap<String, Object>();
                result.put("beacons", beaconsInReach);
                deliverResult(requestId, result, null);
            }
            // SYSTEM
            else if ("system/onBackendConnectivityChanged".equals(action)) {
                if (params == null) {
                    L.w("Expected params != null");
                    return;
                }
                String requestId = params.getString("id");
                Map<String, Object> result = new HashMap<String, Object>();
                result.put("connected", mService.isBacklogConnected());
                deliverResult(requestId, result, null);

                if (!mIsListeningBacklogConnectivityChanged) {
                    final IntentFilter intentFilter = new IntentFilter(XMPPKickChannel.INTENT_BACKLOG_CONNECTED);
                    intentFilter.addAction(XMPPKickChannel.INTENT_BACKLOG_DISCONNECTED);
                    registerReceiver(mBroadcastReceiverBacklog, intentFilter);
                    mIsListeningBacklogConnectivityChanged = true;
                }
            }
            // UTIL
            else if ("util/isConnectedToInternet".equals(action)) {
                if (params == null) {
                    L.w("Expected params != null");
                    return;
                }
                String requestId = params.getString("id");
                Map<String, Object> result = new HashMap<String, Object>();
                boolean wifiConnected = mService.getNetworkConnectivityManager().isWifiConnected();
                result.put("connectedToWifi", wifiConnected);
                result.put("connected", wifiConnected
                    || mService.getNetworkConnectivityManager().isMobileDataConnected());
                deliverResult(requestId, result, null);
            } else if ("util/playAudio".equals(action)) {
                if (params == null) {
                    L.w("Expected params != null");
                    return;
                }
                String requestId = params.getString("id");
                final String url = params.getString("url");
                if (mSoundHandler == null) {
                    Map<String, Object> error = new HashMap<String, Object>();
                    error.put("exception", "playAudio is not supported in your app");
                    deliverResult(requestId, null, error);
                    return;
                }

                mSoundHandler.post(new SafeRunnable() {
                    @Override
                    protected void safeRun() throws Exception {
                        if (mSoundMediaPlayer != null) {
                            mSoundMediaPlayer.release();
                        }
                        mSoundMediaPlayer = new MediaPlayer();
                        try {
                            String fileOnDisk = "file://" + mBrandingResult.dir.getAbsolutePath() + "/" + url;
                            mSoundMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mSoundMediaPlayer.setDataSource(fileOnDisk);
                            mSoundMediaPlayer.setOnPreparedListener(new OnPreparedListener() {

                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    mSoundMediaPlayer.start();
                                }
                            });
                            mSoundMediaPlayer.prepare();
                        } catch (Exception e) {
                            L.i(e);
                        }
                    }
                });
                Map<String, Object> result = new HashMap<String, Object>();
                deliverResult(requestId, result, null);
            }
            // CAMERA
            else if ("camera/startScanningQrCode".equals(action)) {
                if (params == null) {
                    L.w("Expected params != null");
                    return;
                }
                String requestId = params.getString("id");
                String cameraType = params.getString("camera_type");
                if (mQRCodeScanner == null) {
                    Map<String, Object> error = new HashMap<String, Object>();
                    error.put("exception", "startScanningQrCode is not supported in your app");
                    deliverResult(requestId, null, error);
                    return;
                }

                if (!QRCodeScanner.CAMERA_TYPES.contains(cameraType)) {
                    Map<String, Object> error = new HashMap<String, Object>();
                    error.put("exception", "Unsupported camera type");
                    deliverResult(requestId, null, error);
                    return;
                }

                if (mQRCodeScanner.cameraManager.isOpen()) {
                    Map<String, Object> error = new HashMap<String, Object>();
                    error.put("exception", "Camera was already open");
                    deliverResult(requestId, null, error);
                    return;
                }

                if (Camera.getNumberOfCameras() == 1) {
                    mQRCodeScanner.currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                } else {
                    if (mQRCodeScanner.CAMERA_TYPE_BACK.equals(cameraType)) {
                        mQRCodeScanner.currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                    } else {
                        mQRCodeScanner.currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                    }
                }

                if (mQRCodeScanner.hasSurface == true
                    && (mQRCodeScanner.surfaceHolder != null || mQRCodeScanner.surfaceTexture != null)) {
                    if (mBranding.getVisibility() == View.VISIBLE) {
                        if (T.getThreadType() == T.UI) {
                            mQRCodeScanner.startScanningForQRCodes();
                        } else {
                            mService.postAtFrontOfUIHandler(new SafeRunnable() {
                                @Override
                                protected void safeRun() throws Exception {
                                    mQRCodeScanner.startScanningForQRCodes();
                                }
                            });
                        }
                    }
                }
                deliverResult(requestId, null, null);
            } else if ("camera/stopScanningQrCode".equals(action)) {
                if (params == null) {
                    L.w("Expected params != null");
                    return;
                }
                String requestId = params.getString("id");
                String cameraType = params.getString("camera_type");
                if (mQRCodeScanner == null) {
                    Map<String, Object> error = new HashMap<String, Object>();
                    error.put("exception", "stopScanningQrCode is not supported in your app");
                    deliverResult(requestId, null, error);
                    return;
                }

                if (!QRCodeScanner.CAMERA_TYPES.contains(cameraType)) {
                    Map<String, Object> error = new HashMap<String, Object>();
                    error.put("exception", "Unsupported camera type");
                    deliverResult(requestId, null, error);
                    return;
                }
                mQRCodeScanner.stopScanningForQRCodes();
                deliverResult(requestId, null, null);

            } else {
                L.d("Invoke did not handled any function");
            }
        }
    }

    private TaggedWakeLock newWakeLock() {
        TaggedWakeLock wl = TaggedWakeLock.newTaggedWakeLock(mService, PowerManager.FULL_WAKE_LOCK,
            "ACTIONSCREEN WakeLock");
        return wl;
    }

    @SuppressLint({ "SetJavaScriptEnabled", "JavascriptInterface", "NewApi" })
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (CloudConstants.isContentBrandingApp()) {
            super.setTheme(android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.action_screen);
        mBranding = (WebView) findViewById(R.id.branding);
        WebSettings brandingSettings = mBranding.getSettings();
        brandingSettings.setJavaScriptEnabled(true);
        brandingSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            brandingSettings.setAllowFileAccessFromFileURLs(true);
        }
        mBrandingHttp = (WebView) findViewById(R.id.branding_http);
        mBrandingHttp.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        WebSettings brandingSettingsHttp = mBrandingHttp.getSettings();
        brandingSettingsHttp.setJavaScriptEnabled(true);
        brandingSettingsHttp.setCacheMode(WebSettings.LOAD_DEFAULT);

        if (CloudConstants.isContentBrandingApp()) {
            mSoundThread = new HandlerThread("rogerthat_actionscreenactivity_sound");
            mSoundThread.start();
            Looper looper = mSoundThread.getLooper();
            mSoundHandler = new Handler(looper);

            int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
                mQRCodeScanner = QRCodeScanner.getInstance(this);
                final LinearLayout previewHolder = (LinearLayout) findViewById(R.id.preview_view);
                previewHolder.addView(mQRCodeScanner.view);
            }
            mBranding.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    initFullScreenForContentBranding();
                }
            });

            mBrandingHttp.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    initFullScreenForContentBranding();
                }
            });
        }

        final View brandingHeader = findViewById(R.id.branding_header_container);

        final ImageView brandingHeaderClose = (ImageView) findViewById(R.id.branding_header_close);
        final TextView brandingHeaderText = (TextView) findViewById(R.id.branding_header_text);
        brandingHeaderClose.setColorFilter(UIUtils
            .imageColorFilter(getResources().getColor(R.color.mc_homescreen_text)));

        brandingHeaderClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mQRCodeScanner != null) {
                    mQRCodeScanner.onResume();
                }
                brandingHeader.setVisibility(View.GONE);
                mBrandingHttp.setVisibility(View.GONE);
                mBranding.setVisibility(View.VISIBLE);
                mBrandingHttp.loadUrl("about:blank");
            }
        });

        final View brandingFooter = findViewById(R.id.branding_footer_container);

        if (CloudConstants.isContentBrandingApp()) {
            brandingHeaderClose.setVisibility(View.GONE);
            final ImageView brandingFooterClose = (ImageView) findViewById(R.id.branding_footer_close);
            final TextView brandingFooterText = (TextView) findViewById(R.id.branding_footer_text);
            brandingFooterText.setText(getString(R.string.back));
            brandingFooterClose.setColorFilter(UIUtils.imageColorFilter(getResources().getColor(
                R.color.mc_homescreen_text)));

            brandingFooter.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mQRCodeScanner != null) {
                        mQRCodeScanner.onResume();
                    }
                    brandingHeader.setVisibility(View.GONE);
                    brandingFooter.setVisibility(View.GONE);
                    mBrandingHttp.setVisibility(View.GONE);
                    mBranding.setVisibility(View.VISIBLE);
                    mBrandingHttp.loadUrl("about:blank");
                }
            });
        }

        final RelativeLayout openPreview = (RelativeLayout) findViewById(R.id.preview_holder);

        openPreview.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mQRCodeScanner != null) {
                    mQRCodeScanner.previewHolderClicked();
                }
            }
        });

        mBranding.addJavascriptInterface(new JSInterface(this), "__rogerthat__");
        mBranding.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                if (sourceID != null) {
                    try {
                        sourceID = new File(sourceID).getName();
                    } catch (Exception e) {
                        L.d("Could not get fileName of sourceID: " + sourceID, e);
                    }
                }
                if (mIsHtmlContent) {
                    L.i("[BRANDING] " + sourceID + ":" + lineNumber + " | " + message);
                } else {
                    L.d("[BRANDING] " + sourceID + ":" + lineNumber + " | " + message);
                }
            }
        });
        mBranding.setWebViewClient(new WebViewClient() {
            private boolean isExternalUrl(String url) {
                for (String regularExpression : mBrandingResult.externalUrlPatterns) {
                    if (url.matches(regularExpression)) {
                        return true;
                    }
                }
                return false;
            }

            @SuppressLint("DefaultLocale")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                L.i("Branding is loading url: " + url);
                Uri uri = Uri.parse(url);
                String lowerCaseUrl = url.toLowerCase();
                if (lowerCaseUrl.startsWith("tel:") || lowerCaseUrl.startsWith("mailto:") || isExternalUrl(url)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    return true;
                } else if (lowerCaseUrl.startsWith(POKE)) {
                    String tag = url.substring(POKE.length());
                    poke(tag);
                    return true;
                } else if (lowerCaseUrl.startsWith("http://") || lowerCaseUrl.startsWith("https://")) {
                    if (mQRCodeScanner != null) {
                        mQRCodeScanner.onPause();
                    }
                    brandingHeaderText.setText(getString(R.string.loading));
                    brandingHeader.setVisibility(View.VISIBLE);
                    if (CloudConstants.isContentBrandingApp()) {
                        brandingFooter.setVisibility(View.VISIBLE);
                    }
                    mBranding.setVisibility(View.GONE);
                    mBrandingHttp.setVisibility(View.VISIBLE);
                    mBrandingHttp.loadUrl(url);
                    return true;
                } else {
                    brandingHeader.setVisibility(View.GONE);
                    brandingFooter.setVisibility(View.GONE);
                    mBrandingHttp.setVisibility(View.GONE);
                    mBranding.setVisibility(View.VISIBLE);
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                L.i("onPageFinished " + url);
                if (!mInfoSet && mService != null && mIsHtmlContent) {
                    Map<String, Object> info = mFriendsPlugin.getRogerthatUserAndServiceInfo(mServiceEmail,
                        mServiceFriend);

                    executeJS(true, "if (typeof rogerthat !== 'undefined') rogerthat._setInfo(%s)",
                        JSONValue.toJSONString(info));
                    mInfoSet = true;
                }
            }


            @Override
            public WebResourceResponse shouldInterceptRequest (WebView view, String url) {
                L.i("Checking access to: '" + url + "'");
                final URL parsedUrl;
                try {
                     parsedUrl = new URL(url);
                } catch (MalformedURLException e) {
                    L.d("Webview tried to load malformed URL");
                    return new WebResourceResponse("text/plain", "UTF-8", null);
                }
                if (!parsedUrl.getProtocol().equals("file")) {
                    return null;
                }
                File urlPath = new File(parsedUrl.getPath());
                if (urlPath.getAbsolutePath().startsWith(mBrandingResult.dir.getAbsolutePath())) {
                    return null;
                }
                L.d("404: Webview tries to load outside its sandbox.");
                return new WebResourceResponse("text/plain", "UTF-8", null);
            }
        });

        mBrandingHttp.setWebViewClient(new WebViewClient() {
            @SuppressLint("DefaultLocale")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                L.i("BrandingHttp is loading url: " + url);
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                brandingHeaderText.setText(view.getTitle());
                L.i("onPageFinished " + url);
            }
        });

        Intent intent = getIntent();
        mBrandingKey = intent.getStringExtra(BRANDING_KEY);
        mServiceEmail = intent.getStringExtra(SERVICE_EMAIL);
        mItemTagHash = intent.getStringExtra(ITEM_TAG_HASH);
        mItemLabel = intent.getStringExtra(ITEM_LABEL);
        mItemCoords = intent.getLongArrayExtra(ITEM_COORDS);
        mRunInBackground = intent.getBooleanExtra(RUN_IN_BACKGROUND, true);
    }

    @Override
    protected void onPause() {
        executeJS(false, "if (typeof rogerthat !== 'undefined') rogerthat._onPause()");
        super.onPause();

        if (mWakelockEnabled) {
            mWakeLock.release();
        }

        if (mBranding.getVisibility() == View.VISIBLE) {
            if (mQRCodeScanner != null) {
                mQRCodeScanner.onPause();
            }
        }
    }

    @Override
    protected void onResume() {
        executeJS(false, "if (typeof rogerthat !== 'undefined') rogerthat._onResume()");
        super.onResume();

        if (mWakelockEnabled) {
            mWakeLock.acquire();
        }

        if (mQRCodeScanner != null) {
            if (mQRCodeScanner.cameraManager == null) {
                mQRCodeScanner.startCamera();
            } else if (mBranding.getVisibility() == View.VISIBLE) {
                    mQRCodeScanner.onResume();
            }
        }

        if (CloudConstants.isContentBrandingApp()) {
            initFullScreenForContentBranding();
        }
    }

    @SuppressLint("NewApi")
    private void initFullScreenForContentBranding() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mBranding.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

            mBrandingHttp.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void deliverAllApiResults() {
        if (!mApiResultHandlerSet) {
            L.i("apiCallResultHandler not set, thus not delivering any api call responses.");
            return;
        }
        final FriendStore store = mFriendsPlugin.getStore();
        for (ServiceApiCallbackResult r : store.getServiceApiCallbackResultsByItem(mServiceEmail, mItemTagHash)) {

            deliverApiResult(r);
            store.removeServiceApiCall(r.id);
        }
    }

    private void deliverApiResult(ServiceApiCallbackResult r) {
        // Using JSONValue.toJSONString on these string for null value support and quote encoding
        executeJS(false, "if (typeof rogerthat !== 'undefined') rogerthat.api._setResult(%s, %s, %s, %s)",
            JSONValue.toJSONString(r.method), JSONValue.toJSONString(r.result), JSONValue.toJSONString(r.error),
            JSONValue.toJSONString(r.tag));
    }

    private void deliverFacebookResult(String requestId, Map<String, Object> result, Map<String, Object> error) {
        executeJS(false, "if (typeof rogerthat !== 'undefined') rogerthat.facebook._setResult('%s', %s, %s)",
            requestId, JSONValue.toJSONString(result), JSONValue.toJSONString(error));
    }

    private void deliverResult(String requestId, Map<String, Object> result, Map<String, Object> error) {
        executeJS(false, "if (typeof rogerthat !== 'undefined') rogerthat._setResult('%s', %s, %s)", requestId,
            JSONValue.toJSONString(result), JSONValue.toJSONString(error));
    }

    protected void executeJS(final boolean force, final String jsCommandFormat, final Object... args) {
        if (T.getThreadType() == T.UI) {
            T.UI();
            String jsCommand = String.format(Locale.US, jsCommandFormat, args);
            if (mIsHtmlContent && (mInfoSet || force)) {
                L.d("Executing JS: " + jsCommand);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    mBranding.evaluateJavascript(jsCommand, null);
                } else {
                    mBranding.loadUrl("javascript:" + jsCommand);
                }
            } else {
                L.d("NOT Executing JS: " + jsCommand);
            }
        } else {
            mService.postOnUIHandler(new SafeRunnable() {
                @Override
                protected void safeRun() throws Exception {
                    executeJS(force, jsCommandFormat, args);
                }
            });
        }
    }

    private void facebookLogin(final String requestId, final String permissionsStr) {
        L.d("Authorizing with facebook with permissions: " + permissionsStr);
        final List<String> permissions = Arrays.asList(permissionsStr.split(","));

        final Session.StatusCallback statusCallback = new Session.StatusCallback() {
            @Override
            public void call(final Session session, final SessionState state, final Exception exception) {
                if (session != Session.getActiveSession()) {
                    session.removeCallback(this);
                    return;
                }

                if (exception != null) {
                    session.removeCallback(this);
                    if (exception instanceof FacebookOperationCanceledException) {
                        deliverFacebookResult(requestId, null, FACEBOOK_MAP_CANCEL);
                        L.d("user pressed cancel show other window to get user info");
                    } else {
                        Map<String, Object> error = new HashMap<String, Object>();
                        error.put("type", FACEBOOK_TYPE_ERROR);
                        error.put("exception", exception.toString());
                        L.d(exception.toString());
                        deliverFacebookResult(requestId, null, error);
                    }

                } else if (session.isOpened()) {
                    session.removeCallback(this);
                    Request.newMeRequest(session, new Request.GraphUserCallback() {
                        // callback after Graph API response with user object
                        @Override
                        public void onCompleted(GraphUser user, Response response) {
                            if (response.getError() != null) {
                                L.e("Failed to execute fb request to /me\nResponse: " + response);
                                Map<String, Object> error = new HashMap<String, Object>();
                                error.put("type", FACEBOOK_TYPE_ERROR);
                                error.put("exception",
                                    "Failed to execute fb request to /me\nResponse: " + response.toString());
                                deliverFacebookResult(requestId, null, error);
                                return;
                            }
                            deliverFacebookResult(requestId, user.asMap(), null);
                        }
                    }).executeAsync();
                }
            }
        };

        if (!mService.getNetworkConnectivityManager().isConnected()) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("type", FACEBOOK_TYPE_ERROR);
            error.put("exception", getString(R.string.registration_screen_instructions_check_network_not_available));
            deliverFacebookResult(requestId, null, error);
            return;
        }

        FacebookUtils.ensureOpenSession(this, permissions, PermissionType.READ, statusCallback, true);
    }

    private void facebookPost(final String requestId, final String postParamsStr) {
        L.d("Posting on facebook with post params: " + postParamsStr);

        final Session.StatusCallback statusCallback = new Session.StatusCallback() {
            @Override
            public void call(final Session session, final SessionState state, final Exception exception) {
                if (session != Session.getActiveSession()) {
                    session.removeCallback(this);
                    return;
                }

                if (exception != null) {
                    session.removeCallback(this);
                    if (exception instanceof FacebookOperationCanceledException) {
                        deliverFacebookResult(requestId, null, FACEBOOK_MAP_CANCEL);
                        L.d("User pressed cancel. Show other window to get user info.");
                    } else {
                        Map<String, Object> error = new HashMap<String, Object>();
                        error.put("type", FACEBOOK_TYPE_ERROR);
                        error.put("exception", exception.toString());
                        L.d(exception.toString());
                        deliverFacebookResult(requestId, null, error);
                    }

                } else if (session.isOpened()) {
                    session.removeCallback(this);
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> postParams = (Map<String, Object>) JSONValue.parse(postParamsStr);
                    L.d("Share via WebDialog");

                    Bundle bundle = new Bundle();
                    for (Map.Entry<String, Object> entry : postParams.entrySet()) {
                        if (entry.getValue() instanceof String)
                            bundle.putString(entry.getKey(), (String) entry.getValue());
                    }
                    bundle.putString("app_id", CloudConstants.FACEBOOK_APP_ID);
                    L.d("Bundle: " + bundle);

                    new WebDialog.FeedDialogBuilder(ActionScreenActivity.this, session, bundle)
                        .setOnCompleteListener(new WebDialog.OnCompleteListener() {
                            @Override
                            public void onComplete(Bundle values, FacebookException ex) {
                                if (ex != null) {
                                    if (!(ex instanceof FacebookOperationCanceledException)) {
                                        L.e("Failed to post on fb wall", ex);
                                        Map<String, Object> error = new HashMap<String, Object>();
                                        error.put("type", FACEBOOK_TYPE_ERROR);
                                        error.put("exception", ex.toString());
                                        deliverFacebookResult(requestId, null, error);
                                    } else {
                                        deliverFacebookResult(requestId, null, FACEBOOK_MAP_CANCEL);
                                    }
                                    return;
                                }
                                final String postId = values.getString("post_id");
                                if (postId == null) {
                                    deliverFacebookResult(requestId, null, FACEBOOK_MAP_CANCEL);
                                    return;
                                }

                                final Map<String, Object> result = new HashMap<String, Object>();
                                result.put("postId", postId);
                                deliverFacebookResult(requestId, result, null);
                            }

                        }).build().show();
                }
            }
        };

        if (!mService.getNetworkConnectivityManager().isConnected()) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("type", FACEBOOK_TYPE_ERROR);
            error.put("exception", getString(R.string.registration_screen_instructions_check_network_not_available));
            deliverFacebookResult(requestId, null, error);
            return;
        }

        FacebookUtils.ensureOpenSession(this, Arrays.asList("email", "user_friends"), PermissionType.READ,
            statusCallback, true);
    }

    private void facebookTicker(final String requestId, final String type, final String postParamsStr) {
        L.d("Ticker on facebook with post params: " + postParamsStr);

        final Session.StatusCallback statusCallback = new Session.StatusCallback() {
            @Override
            public void call(final Session session, final SessionState state, final Exception exception) {
                if (session != Session.getActiveSession()) {
                    session.removeCallback(this);
                    return;
                }
                if (exception != null) {
                    session.removeCallback(this);
                    if (exception instanceof FacebookOperationCanceledException) {
                        deliverFacebookResult(requestId, null, FACEBOOK_MAP_CANCEL);
                        L.d("user pressed cancel show other window to get user info");
                    } else {
                        Map<String, Object> error = new HashMap<String, Object>();
                        error.put("type", FACEBOOK_TYPE_ERROR);
                        error.put("exception", exception.toString());
                        L.d(exception.toString());
                        deliverFacebookResult(requestId, null, error);
                    }

                } else if (session.isOpened()) {
                    session.removeCallback(this);

                    Bundle bundle = new Bundle();
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> postParams = (Map<String, Object>) JSONValue.parse(postParamsStr);
                    for (Map.Entry<String, Object> entry : postParams.entrySet()) {
                        if (entry.getValue() instanceof String)
                            bundle.putString(entry.getKey(), (String) entry.getValue());
                    }
                    L.d("Ticker type: " + type + "\nBundle: " + bundle);
                    Request request = new Request(Session.getActiveSession(), type, bundle, HttpMethod.POST,
                        new Request.Callback() {
                            @Override
                            public void onCompleted(Response response) {
                                try {
                                    GraphObject graphObject = response.getGraphObject();
                                    if (graphObject == null) {
                                        L.w("Graph response is NULL");
                                        Map<String, Object> error = new HashMap<String, Object>();
                                        error.put("type", FACEBOOK_TYPE_ERROR);
                                        error.put("exception", "Graph response is NULL");
                                        deliverFacebookResult(requestId, null, error);
                                        return;
                                    }
                                    JSONObject jsonObject = graphObject.getInnerJSONObject();

                                    final Map<String, Object> result = new HashMap<String, Object>();
                                    result.put("postId", jsonObject.getString("id"));
                                    deliverFacebookResult(requestId, result, null);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    request.executeAsync();
                }
            }
        };

        if (!mService.getNetworkConnectivityManager().isConnected()) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("type", FACEBOOK_TYPE_ERROR);
            error.put("exception", getString(R.string.registration_screen_instructions_check_network_not_available));
            deliverFacebookResult(requestId, null, error);
            return;
        }

        FacebookUtils.ensureOpenSession(this, FacebookUtils.PUBLISH_PERMISSIONS, PermissionType.PUBLISH,
            statusCallback, true);
    }

    private void poke(String tag) {
        showTransmitting(null);

        mContextMatch = "SP_" + UUID.randomUUID().toString();
        boolean success = mFriendsPlugin.pokeService(mServiceEmail, tag, mContextMatch);
        if (!success) {
            completeTransmit(new SafeRunnable() {
                @Override
                protected void safeRun() throws Exception {
                    UIUtils.showAlertDialog(ActionScreenActivity.this, null, R.string.scanner_communication_failure);
                }
            });
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onServiceBound() {
        final IntentFilter intentFilter = new IntentFilter(MessagingPlugin.NEW_MESSAGE_RECEIVED_INTENT);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(FriendsPlugin.SERVICE_API_CALL_ANSWERED_INTENT);
        intentFilter.addAction(FriendsPlugin.SERVICE_DATA_UPDATED);
        intentFilter.addAction(FriendsPlugin.BEACON_IN_REACH);
        intentFilter.addAction(FriendsPlugin.BEACON_OUT_OF_REACH);

        intentFilter.addAction(FriendsPlugin.FRIEND_INFO_RECEIVED_INTENT);
        intentFilter.addAction(ProcessScanActivity.URL_REDIRECTION_DONE);

        if (CloudConstants.isContentBrandingApp()) {
            intentFilter.addAction(BrandingMgr.SERVICE_BRANDING_AVAILABLE_INTENT);
            intentFilter.addAction(FriendsPlugin.FRIEND_UPDATE_INTENT);
        }

        registerReceiver(mBroadcastReceiver, intentFilter);
        mMessagingPlugin = mService.getPlugin(MessagingPlugin.class);
        mFriendsPlugin = mService.getPlugin(FriendsPlugin.class);
        displayBranding();

        if (mQRCodeScanner != null) {
            mQRCodeScanner.mainService = mService;
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint({ "SetJavaScriptEnabled", "Wakelock" })
    private void displayBranding() {
        try {
            mServiceFriend = mFriendsPlugin.getStore().getExistingFriend(mServiceEmail);
            mBrandingResult = mMessagingPlugin.getBrandingMgr().prepareBranding(mBrandingKey, mServiceFriend, true);
            WebSettings settings = mBranding.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setBlockNetworkImage(false);

            String fileOnDisk = "file://" + mBrandingResult.file.getAbsolutePath();
            if (mBrandingResult.contentType != null
                && AttachmentViewerActivity.CONTENT_TYPE_PDF.equalsIgnoreCase(mBrandingResult.contentType)) {
                mIsHtmlContent = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    settings.setAllowUniversalAccessFromFileURLs(true);
                }
                mBranding.loadUrl("file:///android_asset/pdfjs/web/viewer.html?file=" + fileOnDisk);
            } else {
                mIsHtmlContent = true;
                mBranding.loadUrl(fileOnDisk);
                mBranding.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
                if (mBrandingResult.color != null) {
                    mBranding.setBackgroundColor(mBrandingResult.color);
                }
            }
            mWakelockEnabled = mBrandingResult.wakelockEnabeld;
            L.d("wakelockEnabeld: " + mWakelockEnabled);
            if (mWakelockEnabled && mWakeLock == null) {
                mWakeLock = newWakeLock();
                L.d("Acquiring wakelock " + mWakeLock.hashCode());
                mWakeLock.acquire();
            } else if (mWakeLock != null && !mWakelockEnabled) {
                mWakeLock.release();
            }

        } catch (BrandingFailureException e) {
            UIUtils.showLongToast(this, getString(R.string.failed_to_show_action_screen));
            finish();
            mMessagingPlugin.getBrandingMgr().queue(mServiceFriend);
            L.e("Could not display menu item with screen branding.", e);
            return;
        }

    }

    @Override
    protected void onServiceUnbound() {
        unregisterReceiver(mBroadcastReceiver);
        if (mIsListeningBacklogConnectivityChanged) {
            unregisterReceiver(mBroadcastReceiverBacklog);
        }
    }

    private BroadcastReceiver mBroadcastReceiverBacklog = new SafeBroadcastReceiver() {
        @SuppressLint("DefaultLocale")
        @Override
        public String[] onSafeReceive(final Context context, final Intent intent) {
            if (XMPPKickChannel.INTENT_BACKLOG_CONNECTED.equals(intent.getAction())) {
                if (mLastBacklogStatus <= 0) {
                    mLastBacklogStatus = 1;
                    executeJS(false,
                        "if (typeof rogerthat !== 'undefined') rogerthat._onBackendConnectivityChanged(%s)", true);
                }
            } else if (XMPPKickChannel.INTENT_BACKLOG_DISCONNECTED.equals(intent.getAction())) {
                if (mLastBacklogStatus >= 0) {
                    mLastBacklogStatus = -1;
                    executeJS(false,
                        "if (typeof rogerthat !== 'undefined') rogerthat._onBackendConnectivityChanged(%s)", false);
                }
            } else {
                return null;
            }
            return new String[] { intent.getAction() };
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new SafeBroadcastReceiver() {
        @SuppressLint("DefaultLocale")
        @Override
        public String[] onSafeReceive(final Context context, final Intent intent) {
            T.UI();
            if (MessagingPlugin.NEW_MESSAGE_RECEIVED_INTENT.equals(intent.getAction())) {
                if (mContextMatch.equals(intent.getStringExtra("context")) && isTransmitting()) {
                    mContextMatch = "";
                    completeTransmit(new SafeRunnable() {
                        @Override
                        protected void safeRun() throws Exception {
                            final Intent i = new Intent(context, ServiceMessageDetailActivity.class);
                            i.putExtra("message", intent.getStringExtra("message"));
                            startActivity(i);
                        }
                    });
                }
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                L.d("[BroadcastReceiver] Screen OFF");
                if (!mRunInBackground) {
                    finish();
                }
            } else if (FriendsPlugin.SERVICE_API_CALL_ANSWERED_INTENT.equals(intent.getAction())) {
                if (mServiceEmail.equals(intent.getStringExtra("service"))
                    && mItemTagHash.equals(intent.getStringExtra("item"))) {
                    deliverAllApiResults();
                }
            } else if (FriendsPlugin.SERVICE_DATA_UPDATED.equals(intent.getAction())) {
                if (mServiceEmail.equals(intent.getStringExtra("email"))) {
                    final String[] data = mFriendsPlugin.getStore().getServiceData(mServiceEmail);
                    if (intent.getBooleanExtra("user_data", false)) {
                        executeJS(false, "if (typeof rogerthat !== 'undefined') rogerthat._userDataUpdated(%s)",
                            data[0]);
                    }
                    if (intent.getBooleanExtra("service_data", false)) {
                        executeJS(false, "if (typeof rogerthat !== 'undefined') rogerthat._serviceDataUpdated(%s)",
                            data[1]);
                    }
                    return new String[] { FriendsPlugin.SERVICE_DATA_UPDATED };
                }
            } else if (FriendsPlugin.BEACON_IN_REACH.equals(intent.getAction())) {
                if (mServiceEmail.equals(intent.getStringExtra("email"))) {
                    final Map<String, Object> result = new HashMap<String, Object>();
                    result.put("uuid", intent.getStringExtra("uuid").toLowerCase());
                    result.put("major", "" + intent.getIntExtra("major", 0));
                    result.put("minor", "" + intent.getIntExtra("minor", 0));
                    result.put("tag", "" + intent.getStringExtra("tag"));
                    result.put("proximity", "" + intent.getIntExtra("proximity", 0));

                    executeJS(false, "if (typeof rogerthat !== 'undefined') rogerthat._onBeaconInReach(%s)",
                        JSONValue.toJSONString(result));
                }

            } else if (FriendsPlugin.BEACON_OUT_OF_REACH.equals(intent.getAction())) {
                if (mServiceEmail.equals(intent.getStringExtra("email"))) {
                    final Map<String, Object> result = new HashMap<String, Object>();
                    result.put("uuid", intent.getStringExtra("uuid").toLowerCase());
                    result.put("major", "" + intent.getIntExtra("major", 0));
                    result.put("minor", "" + intent.getIntExtra("minor", 0));
                    result.put("tag", "" + intent.getStringExtra("tag"));
                    result.put("proximity", "" + intent.getIntExtra("proximity", 0));

                    executeJS(false, "if (typeof rogerthat !== 'undefined') rogerthat._onBeaconOutOfReach(%s)",
                        JSONValue.toJSONString(result));
                }
            } else if (ProcessScanActivity.URL_REDIRECTION_DONE.equals(intent.getAction())) {

                final String rawUrl = intent.getStringExtra(ProcessScanActivity.RAWURL);
                final String emailHash = intent.getStringExtra(ProcessScanActivity.EMAILHASH);
                if (rawUrl != null) {
                    if (emailHash != null) {
                        if (intent.hasExtra(ProcessScanActivity.POKE_ACTION)) {
                            final Map<String, Object> result = new HashMap<String, Object>();
                            result.put("status", "resolved");
                            result.put("content", rawUrl);
                            executeJS(false, "if (typeof rogerthat !== 'undefined') rogerthat._qrCodeScanned(%s)",
                                JSONValue.toJSONString(result));
                        } else {
                            final GetUserInfoRequestTO request = new GetUserInfoRequestTO();
                            request.code = emailHash;
                            request.allow_cross_app = true;

                            final GetUserInfoResponseHandler handler = new GetUserInfoResponseHandler();
                            handler.setCode(emailHash);
                            handler.putStringExtra(ProcessScanActivity.RAWURL, rawUrl);
                            if (CloudConstants.isContentBrandingApp()) {
                                handler.setSendUserScanned(true);
                                handler.setServiceEmail(mServiceEmail);
                            }

                            try {
                                com.mobicage.api.friends.Rpc.getUserInfo(handler, request);
                            } catch (Exception e) {
                                finish();
                                mService.putInHistoryLog(getString(R.string.getuserinfo_failure), HistoryItem.ERROR);
                            }
                        }
                    } else {
                        Map<String, Object> result = new HashMap<String, Object>();
                        result.put("status", "resolved");
                        result.put("content", rawUrl);
                        executeJS(false, "if (typeof rogerthat !== 'undefined') rogerthat._qrCodeScanned(%s)",
                            JSONValue.toJSONString(result));
                    }
                }
                return new String[] { intent.getAction() };

            } else if (FriendsPlugin.FRIEND_INFO_RECEIVED_INTENT.equals(intent.getAction())) {
                final String rawUrl = intent.getStringExtra(ProcessScanActivity.RAWURL);
                if (rawUrl != null) {
                    final String emailHash = intent.getStringExtra(ProcessScanActivity.EMAILHASH);
                    if (emailHash != null) {
                        if (intent.getBooleanExtra(ProcessScanActivity.SUCCESS, true)) {
                            Map<String, Object> userDetails = new HashMap<String, Object>();
                            userDetails.put("email", intent.getStringExtra(ProcessScanActivity.EMAIL));
                            userDetails.put("name", intent.getStringExtra(ProcessScanActivity.NAME));
                            userDetails.put("appId", intent.getStringExtra(ProcessScanActivity.APP_ID));

                            Map<String, Object> result = new HashMap<String, Object>();
                            result.put("status", "resolved");
                            result.put("content", rawUrl);
                            result.put("userDetails", userDetails);
                            executeJS(false, "if (typeof rogerthat !== 'undefined') rogerthat._qrCodeScanned(%s)",
                                JSONValue.toJSONString(result));
                        } else {
                            Map<String, Object> result = new HashMap<String, Object>();
                            final String errorMessge = intent.getStringExtra(ProcessScanActivity.ERROR_MESSAGE);
                            result.put("status", "error");
                            result.put("content", errorMessge);
                            executeJS(false, "if (typeof rogerthat !== 'undefined') rogerthat._qrCodeScanned(%s)",
                                JSONValue.toJSONString(result));
                        }
                    } else {
                        Map<String, Object> result = new HashMap<String, Object>();
                        result.put("status", "resolved");
                        result.put("content", rawUrl);
                        executeJS(false, "if (typeof rogerthat !== 'undefined') rogerthat._qrCodeScanned(%s)",
                            JSONValue.toJSONString(result));
                    }
                }
                return new String[] { intent.getAction() };

            } else if (CloudConstants.isContentBrandingApp()) {
                if (BrandingMgr.SERVICE_BRANDING_AVAILABLE_INTENT.equals(intent.getAction())) {
                    if (mServiceEmail.equals(intent.getStringExtra(BrandingMgr.SERVICE_EMAIL))) {
                        FriendStore friendStore = mFriendsPlugin.getStore();
                        Friend f = friendStore.getFriend(mServiceEmail);
                        if (!f.contentBrandingHash.equals(mBrandingKey)
                            && f.contentBrandingHash.equals(intent.getStringExtra(BrandingMgr.BRANDING_KEY))) {
                            mBrandingKey = f.contentBrandingHash;

                            if (mBranding.getVisibility() == View.VISIBLE) {
                                if (mQRCodeScanner != null) {
                                    mQRCodeScanner.stopScanningForQRCodes();
                                }
                            }
                            mInfoSet = false;
                            displayBranding();
                        }
                    }

                } else if (FriendsPlugin.FRIEND_UPDATE_INTENT.equals(intent.getAction())) {
                    if (mServiceEmail.equals(intent.getStringExtra(BrandingMgr.SERVICE_EMAIL))) {
                        FriendStore friendStore = mFriendsPlugin.getStore();
                        Friend f = friendStore.getFriend(mServiceEmail);
                        if (!f.contentBrandingHash.equals(mBrandingKey)) {
                            BrandingMgr brandingMgr = mMessagingPlugin.getBrandingMgr();
                            try {
                                if (brandingMgr.isBrandingAvailable(f.contentBrandingHash)) {
                                    mBrandingKey = f.contentBrandingHash;
                                    if (mBranding.getVisibility() == View.VISIBLE) {
                                        if (mQRCodeScanner != null) {
                                            mQRCodeScanner.stopScanningForQRCodes();
                                        }
                                    }
                                    mInfoSet = false;
                                    displayBranding();
                                }
                            } catch (BrandingFailureException e) {
                                L.d(e);
                            }
                        }
                    }
                }

            }

            return null;
        }

    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final Session session = Session.getActiveSession();
        if (session != null) {
            session.onActivityResult(this, requestCode, resultCode, data);
        }

        if (requestCode == CHECKIN_REQUEST_CODE && data != null) {
            Bundle bundle = new Bundle();
            @SuppressWarnings("unchecked")
            final Map<String, Object> postParams = (Map<String, Object>) JSONValue.parse(data
                .getStringExtra("postParamsStr"));
            for (Map.Entry<String, Object> entry : postParams.entrySet()) {
                if (entry.getValue() instanceof String)
                    bundle.putString(entry.getKey(), (String) entry.getValue());
            }

            String[] tags = data.getStringArrayExtra("tags");
            if (tags.length > 0) {
                StringBuilder nameBuilder = new StringBuilder();

                for (String n : tags) {
                    nameBuilder.append(n + ",");
                }
                nameBuilder.deleteCharAt(nameBuilder.length() - 1);
                bundle.putString("tags", nameBuilder.toString());
            }
            L.d("Bundle: " + bundle);
            final String requestId = data.getStringExtra("requestId");
            Request request = new Request(Session.getActiveSession(), POST_ACTION_PATH_FEED, bundle, HttpMethod.POST,
                new Request.Callback() {
                    @Override
                    public void onCompleted(Response response) {
                        try {
                            FacebookRequestError fbError = response.getError();
                            if (fbError != null) {
                                L.w("Failed to check in: " + fbError);
                                Map<String, Object> error = new HashMap<String, Object>();
                                error.put("type", FACEBOOK_TYPE_ERROR);
                                error.put("exception", fbError.getErrorMessage());
                                deliverFacebookResult(requestId, null, error);
                            }
                            GraphObject graphObject = response.getGraphObject();
                            if (graphObject == null) {
                                deliverFacebookResult(requestId, null, FACEBOOK_MAP_CANCEL);
                                return;
                            }
                            JSONObject jsonObject = graphObject.getInnerJSONObject();

                            final Map<String, Object> result = new HashMap<String, Object>();
                            result.put("postId", jsonObject.getString("id"));
                            deliverFacebookResult(requestId, result, null);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            request.executeAsync();
        }
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        long now = System.currentTimeMillis();
        if (mJavascriptBackBtnListener && keyCode == KeyEvent.KEYCODE_BACK && now - mLasttimeBackPressed > 500) {
            mLasttimeBackPressed = now;
            mCurrentBackPressedId = UUID.randomUUID().toString();
            final String context = mCurrentBackPressedId;
            executeJS(false, "if (typeof rogerthat !== 'undefined') rogerthat._backPressed('%s')",
                mCurrentBackPressedId);
            // javascript has 500 ms to answer
            mService.postDelayedOnUIHandler(new SafeRunnable() {
                @Override
                protected void safeRun() throws Exception {
                    if (context.equals(mCurrentBackPressedId)) {
                        L.w("Javascript did not process the backPressed event fast enough. Finishing...");
                        finish();
                    }
                }
            }, 500);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void finish() {
        T.UI();
        if (mBranding != null) {
            // Stop all javascript
            mBranding.loadUrl("about:blank");
        }
        if (mBrandingHttp != null) {
            // Stop all javascript
            mBrandingHttp.loadUrl("about:blank");
        }
        super.finish();
    }
}