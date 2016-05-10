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

package com.mobicage.rpc.config;

//
// WARNING !!!
//
// THESE CONSTANTS ARE PRODUCTION CODE.
// DO NOT MODIFY
//
// FOR TESTING, CHANGE URLs IN CustomCloudConstants.java
//

public class CloudConstants {

    // Main DEBUG switch
    public final static boolean DEBUG_LOGGING = CustomCloudConstants.DEBUG_LOGGING;

    // Customizable HTTP constants
    public final static String HTTPS_BASE_URL = CustomCloudConstants.HTTPS_BASE_URL;
    public final static int HTTPS_PORT = CustomCloudConstants.HTTPS_PORT;
    public final static String HTTP_BASE_URL = CustomCloudConstants.HTTP_BASE_URL;

    // Non-customizable HTTP constants
    public final static String HEAD_URL = HTTPS_BASE_URL;
    public static final String HTTPS_LOAD_SRV_RECORDS = HTTPS_BASE_URL + "/mobi/rest/srv/load?email=";
    public final static String JSON_RPC_URL = HTTPS_BASE_URL + "/json-rpc";
    public final static String BRANDING_URL_PREFIX = HTTPS_BASE_URL + "/unauthenticated/mobi/branding/";
    public final static String AVATAR_URL_PREFIX = HTTPS_BASE_URL + "/unauthenticated/mobi/avatar/";
    public final static String CACHED_AVATAR_URL_PREFIX = HTTPS_BASE_URL + "/unauthenticated/mobi/cached/avatar/";
    public final static String DEBUG_LOG_URL = HTTPS_BASE_URL + "/unauthenticated/debug_log";
    public final static String REGISTRATION_REQUEST_URL = HTTPS_BASE_URL
        + "/unauthenticated/mobi/registration/init_via_email";
    public final static String REGISTRATION_REGISTER_INSTALL_URL = HTTPS_BASE_URL
        + "/unauthenticated/mobi/registration/register_install";
    public final static String REGISTRATION_PIN_URL = HTTPS_BASE_URL
        + "/unauthenticated/mobi/registration/verify_email";
    public final static String REGISTRATION_FACEBOOK_URL = HTTPS_BASE_URL
        + "/unauthenticated/mobi/registration/register_facebook";
    public final static String REGISTRATION_OAUTH_INFO_URL = HTTPS_BASE_URL
            + "/unauthenticated/mobi/registration/oauth/info";
    public final static String REGISTRATION_OAUTH_REGISTERED_URL = HTTPS_BASE_URL
            + "/unauthenticated/mobi/registration/oauth/registered";

    public final static String REGISTRATION_QR_URL = HTTPS_BASE_URL + "/unauthenticated/mobi/registration/register_qr";
    public final static String REGISTRATION_FINISH_URL = HTTPS_BASE_URL + "/unauthenticated/mobi/registration/finish";
    public final static String REGISTRATION_PAGE_URL_FACEBOOK = HTTPS_BASE_URL
        + "/login_facebook?display=wap&continue=%2Fregister%3Fua%3Dandroid";
    public final static String REGISTRATION_PAGE_URL_GOOGLE = HTTPS_BASE_URL
        + "/login_google?continue=%2Fregister%3Fua%3Dandroid";
    public final static String REGISTRATION_LOG_STEP_URL = HTTPS_BASE_URL
        + "/unauthenticated/mobi/registration/log_registration_step";
    public final static String TERMS_OF_SERVICE_URL = HTTPS_BASE_URL + "/static/terms_and_conditions.html";
    public final static String LOG_ERROR_URL = HTTPS_BASE_URL + "/unauthenticated/mobi/logging/exception";
    public final static String JS_EMBEDDING_URL_PREFIX = HTTPS_BASE_URL + "/mobi/js_embedding/";
    public final static String INSTALL_URL = "/install";
    public final static String MDP_SESSION_INIT_URL = HTTPS_BASE_URL + "/mobi/rest/mdp/session/init";
    public final static String MDP_SESSION_AUTHORIZED_URL = HTTPS_BASE_URL + "/mobi/rest/mdp/session/authorized";

    public final static String REGISTRATION_YSAAA_URL = HTTPS_BASE_URL
        + "/unauthenticated/mobi/registration/init_service_app";


    public final static String REGISTRATION_MAIN_SIGNATURE = CustomCloudConstants.REGISTRATION_MAIN_SIGNATURE;
    public final static String REGISTRATION_EMAIL_SIGNATURE = CustomCloudConstants.REGISTRATION_EMAIL_SIGNATURE;
    public final static String REGISTRATION_PIN_SIGNATURE = CustomCloudConstants.REGISTRATION_PIN_SIGNATURE;
    public final static String EMAIL_HASH_ENCRYPTION_KEY = CustomCloudConstants.EMAIL_HASH_ENCRYPTION_KEY;

    // Customized by App flavor
    public final static String APP_SERVICE_GUID = AppConstants.APP_SERVICE_GUID;
    public static final String FACEBOOK_APP_ID = AppConstants.FACEBOOK_APP_ID;
    public static final String APP_ID = AppConstants.APP_ID;
    public final static boolean USE_TRUSTSTORE = CustomCloudConstants.USE_TRUSTSTORE;

    // Customizable XMPP constants
    public final static String XMPP_DOMAIN = CustomCloudConstants.XMPP_DOMAIN;
    public final static boolean XMPP_MUST_VALIDATE_SSL_CERTIFICATE = CustomCloudConstants.XMPP_MUST_VALIDATE_SSL_CERTIFICATE;
    public final static boolean XMPP_DEBUG = CustomCloudConstants.XMPP_DEBUG;

    // Non-customizable XMPP constants
    public final static String XMPP_KICK_COMPONENT = "kick." + XMPP_DOMAIN;

    // Customizable KICK constants
    public final static boolean USE_XMPP_KICK_CHANNEL = CustomCloudConstants.USE_XMPP_KICK_CHANNEL;
    public final static boolean USE_GCM_KICK_CHANNEL = !CustomCloudConstants.USE_XMPP_KICK_CHANNEL;
    public final static String GCM_SENDER_ID = CustomCloudConstants.GCM_SENDER_ID;

    public static boolean isRogerthatApp() {
        return AppConstants.getAppType() == AppConstants.APP_TYPE_ROGERTHAT;
    }

    public static boolean isEnterpriseApp() {
        return AppConstants.getAppType() == AppConstants.APP_TYPE_ENTERPRISE;
    }

    public static boolean isCityApp() {
        return AppConstants.getAppType() == AppConstants.APP_TYPE_CITYAPP;
    }

    public static boolean isContentBrandingApp() {
        return AppConstants.getAppType() == AppConstants.APP_TYPE_CONTENT_BRANDING;
    }

    public static boolean isYSAAA() {
        return AppConstants.getAppType() == AppConstants.APP_TYPE_YSAAA;
    }

}