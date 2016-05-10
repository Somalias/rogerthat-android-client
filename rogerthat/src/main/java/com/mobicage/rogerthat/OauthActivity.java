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
package com.mobicage.rogerthat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.annotation.SuppressLint;

import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.mobicage.rogerth.at.R;
import com.mobicage.rogerthat.util.logging.L;
import com.mobicage.rpc.config.CloudConstants;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class OauthActivity extends Activity {

    public static final String FINISHED_OAUTH = "com.mobicage.rogerthat.FINISHED_OAUTH";

    public static String STATE = "state";
    public static String CLIENT_ID = "client_id";
    public static String OAUTH_URL = "oauth_url";
    public static String SCOPES = "scopes";

    public static String RESULT = "result";
    public static String SUCCESS = "success";

    private String mState;
    private String mClientId;
    private String mOauthUrl;
    private String mScopes; // Comma separated

    private WebView mWebview;

    @SuppressLint({ "SetJavaScriptEnabled", "NewApi" })
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.oauth);

        mWebview = (WebView) findViewById(R.id.webview);

        Intent intent = getIntent();
        mState = intent.getStringExtra(OauthActivity.STATE);
        mClientId = intent.getStringExtra(OauthActivity.CLIENT_ID);
        mOauthUrl = intent.getStringExtra(OauthActivity.OAUTH_URL);
        mScopes = intent.getStringExtra(OauthActivity.SCOPES);

        Uri.Builder builder = Uri.parse(mOauthUrl).buildUpon();
        builder.appendQueryParameter("state", mState);
        builder.appendQueryParameter("client_id", mClientId);
        builder.appendQueryParameter("scope", mScopes);
        builder.appendQueryParameter("redirect_uri", getCallbackUrl());
        builder.appendQueryParameter("response_type", "code");
        final String url = builder.build().toString();


        WebSettings webviewSettings = mWebview.getSettings();
        webviewSettings.setJavaScriptEnabled(true);

        webviewSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        if (Build.VERSION.SDK_INT <= 18) {
            mWebview.getSettings().setSavePassword(false);
        }


        mWebview.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                L.d("shouldOverrideUrlLoading: " + url);
                interceptUrlCompat(view, url, true);
                return true;
            }

            private boolean interceptUrlCompat(WebView view, String url, boolean loadUrl) {
                String redirectUri = getCallbackUrl();
                if (isRedirectUriFound(url, redirectUri)) {
                    AuthorizationCodeResponseUrl responseUrl = new AuthorizationCodeResponseUrl(url);
                    String error = responseUrl.getError();

                    if (TextUtils.isEmpty(error)) {
                        L.d("Code: " + responseUrl.getCode());

                        Intent resultIntent = new Intent(OauthActivity.FINISHED_OAUTH);
                        resultIntent.putExtra(OauthActivity.RESULT, responseUrl.getCode());
                        resultIntent.putExtra(OauthActivity.SUCCESS, true);
                        setResult(Activity.RESULT_OK, resultIntent);
                        finish();

                    } else {
                        L.d("error: " + error);
                        L.d("errorMsg: " + responseUrl.getErrorDescription());

                        Intent resultIntent = new Intent(OauthActivity.FINISHED_OAUTH);
                        resultIntent.putExtra(OauthActivity.RESULT, responseUrl.getErrorDescription());
                        resultIntent.putExtra(OauthActivity.SUCCESS, false);
                        setResult(Activity.RESULT_OK, resultIntent);
                        finish();
                    }
                    return true;
                }
                if (loadUrl) {
                    view.loadUrl(url);
                }
                return false;
            }

        });

        mWebview.loadUrl(url);
        mWebview.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                setResult(Activity.RESULT_CANCELED);
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    static String getCallbackUrl() {
        return "oauth-" + CloudConstants.APP_ID + "://x-callback-url";
    }

    static boolean isRedirectUriFound(String uri, String redirectUri) {
        Uri u = null;
        Uri r = null;
        try {
            u = Uri.parse(uri);
            r = Uri.parse(redirectUri);
        } catch (NullPointerException e) {
            return false;
        }
        if (u == null || r == null) {
            return false;
        }
        boolean rOpaque = r.isOpaque();
        boolean uOpaque = u.isOpaque();
        if (rOpaque != uOpaque) {
            return false;
        }
        if (rOpaque) {
            return TextUtils.equals(uri, redirectUri);
        }
        if (!TextUtils.equals(r.getScheme(), u.getScheme())) {
            return false;
        }
        if (!TextUtils.equals(r.getAuthority(), u.getAuthority())) {
            return false;
        }
        if (r.getPort() != u.getPort()) {
            return false;
        }
        if (!TextUtils.isEmpty(r.getPath()) && !TextUtils.equals(r.getPath(), u.getPath())) {
            return false;
        }
        Set<String> paramKeys = getQueryParameterNames(r);
        for (String key : paramKeys) {
            if (!TextUtils.equals(r.getQueryParameter(key), u.getQueryParameter(key))) {
                return false;
            }
        }
        String frag = r.getFragment();
        if (!TextUtils.isEmpty(frag)
                && !TextUtils.equals(frag, u.getFragment())) {
            return false;
        }
        return true;
    }

    static Set<String> getQueryParameterNames(Uri uri) {
        if (uri.isOpaque()) {
            throw new UnsupportedOperationException("This isn't a hierarchical URI.");
        }

        String query = uri.getEncodedQuery();
        if (query == null) {
            return Collections.emptySet();
        }

        Set<String> names = new LinkedHashSet<String>();
        int start = 0;
        do {
            int next = query.indexOf('&', start);
            int end = (next == -1) ? query.length() : next;

            int separator = query.indexOf('=', start);
            if (separator > end || separator == -1) {
                separator = end;
            }

            String name = query.substring(start, separator);
            names.add(Uri.decode(name));

            // Move start to end of name
            start = end + 1;
        } while (start < query.length());

        return Collections.unmodifiableSet(names);
    }
}
