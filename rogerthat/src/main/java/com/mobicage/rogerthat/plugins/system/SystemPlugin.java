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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.mobicage.rogerth.at.R;
import com.mobicage.rogerthat.MainService;
import com.mobicage.rogerthat.config.Configuration;
import com.mobicage.rogerthat.config.ConfigurationProvider;
import com.mobicage.rogerthat.plugins.MobicagePlugin;
import com.mobicage.rogerthat.plugins.messaging.BrandingMgr;
import com.mobicage.rogerthat.util.db.DatabaseManager;
import com.mobicage.rogerthat.util.logging.L;
import com.mobicage.rogerthat.util.net.NetworkConnectivityManager;
import com.mobicage.rogerthat.util.system.SafeRunnable;
import com.mobicage.rogerthat.util.system.SystemUtils;
import com.mobicage.rogerthat.util.system.T;
import com.mobicage.to.js_embedding.JSEmbeddingItemTO;
import com.mobicage.to.system.HeartBeatRequestTO;
import com.mobicage.to.system.SettingsTO;

public class SystemPlugin implements MobicagePlugin {

    public static final String SYSTEM_PLUGIN_MUST_REFRESH_JS_EMBEDDING = "com.mobicage.rogerthat.plugins.system.SYSTEM_PLUGIN_MUST_REFRESH_JS_EMBEDDING";

    private static final String CONFIGKEY = "com.mobicage.rogerthat.plugins.system";
    private static final String HEARTBEAT_INFO = "heartbeat_info";

    private static final String CONFIG_WIFI_ONLY_DOWNLOADS = "wifiOnlyDownloads";

    private final ConfigurationProvider mConfigProvider;
    private final NetworkConnectivityManager mNetworkConnectivityManager;
    private final MainService mMainService;
    private final BrandingMgr mBrandingMgr;

    private final SystemStore mStore;
    private boolean mWifiOnlyDownloads = false;

    public SystemPlugin(final MainService mainService, ConfigurationProvider pConfigProvider,
        NetworkConnectivityManager pNetworkConnectivityManager, final BrandingMgr brandingMgr,
        final DatabaseManager dbManager) {
        T.UI();
        mMainService = mainService;
        mConfigProvider = pConfigProvider;
        mNetworkConnectivityManager = pNetworkConnectivityManager;
        mBrandingMgr = brandingMgr;
        mStore = new SystemStore(mainService, dbManager);
    }

    public BrandingMgr getBrandingMgr() {
        T.dontCare();
        return mBrandingMgr;
    }

    public void doHeartbeat() {
        T.UI();

        MobileInfo info = gatherMobileInfo(mMainService);

        Configuration cfg = mConfigProvider.getConfiguration(CONFIGKEY);
        String heartBeatInfo = info.getFingerPrint();
        if (heartBeatInfo.equals(cfg.get(HEARTBEAT_INFO, null)))
            return;

        final long now = System.currentTimeMillis();
        final HeartBeatResponseHandler rh = new HeartBeatResponseHandler();
        rh.setRequestSubmissionTimestamp(now);

        HeartBeatRequestTO request = new HeartBeatRequestTO();

        // general info
        request.buildFingerPrint = Build.FINGERPRINT;
        request.flushBackLog = false;
        request.networkState = mNetworkConnectivityManager.getNetworkState();
        request.timestamp = now / 1000;

        // app info
        request.appType = info.app.type;
        request.majorVersion = info.app.majorVersion;
        request.minorVersion = info.app.minorVersion;
        request.product = info.app.name;

        // sim card info
        request.simCountry = info.sim.isoCountryCode;
        request.simCountryCode = info.sim.mobileCountryCode;
        request.simCarrierCode = info.sim.mobileNetworkCode;
        request.simCarrierName = info.sim.carrierName;

        // net info
        request.netCountry = info.network.isoCountryCode;
        request.netCountryCode = info.network.mobileCountryCode;
        request.netCarrierCode = info.network.mobileNetworkCode;
        request.netCarrierName = info.network.carrierName;

        // locale info
        request.localeCountry = info.locale.country;
        request.localeLanguage = info.locale.language;

        // timeZone info
        request.timezone = info.timeZone.abbrevation;
        request.timezoneDeltaGMT = info.timeZone.secondsFromGMT;

        // device info
        request.deviceModelName = info.device.modelName;
        request.SDKVersion = info.device.osVersion;

        try {
            L.d("Heartbeating to server");
            com.mobicage.api.system.Rpc.heartBeat(rh, request);

            cfg.put(HEARTBEAT_INFO, heartBeatInfo);
            mConfigProvider.updateConfigurationNow(CONFIGKEY, cfg);
        } catch (Exception e) {
            L.d("Cannot heartbeat", e);
        }
    }

    @Override
    public void destroy() {
        T.UI();
        mConfigProvider.unregisterListener(CONFIGKEY, this);
        mBrandingMgr.close();
    }

    @Override
    public void processSettings(SettingsTO settings) {
        T.UI();
        final Configuration cfg = mConfigProvider.getConfiguration(CONFIGKEY);

        mWifiOnlyDownloads = settings.wifiOnlyDownloads;
        cfg.put(CONFIG_WIFI_ONLY_DOWNLOADS, mWifiOnlyDownloads);

        mConfigProvider.updateConfigurationLater(CONFIGKEY, cfg);
    }

    @Override
    public void reconfigure() {
        T.UI();
    }

    public SystemStore getStore() {
        T.dontCare();
        return mStore;
    }

    @Override
    public void initialize() {
        T.UI();
        mWifiOnlyDownloads = mConfigProvider.getConfiguration(CONFIGKEY).get(CONFIG_WIFI_ONLY_DOWNLOADS, false);

        mConfigProvider.registerListener(CONFIGKEY, this);

        // ugly hack to run this initialization _after_ all plugins have been initialized
        // XXX: build support in MainService framework
        mMainService.postOnUIHandler(new SafeRunnable() {
            @Override
            protected void safeRun() throws Exception {
                T.UI();
                mMainService.postOnBIZZHandler(new SafeRunnable() {
                    @Override
                    protected void safeRun() throws Exception {
                        if (mMainService.getPluginDBUpdates(SystemPlugin.class).contains(
                            SYSTEM_PLUGIN_MUST_REFRESH_JS_EMBEDDING)) {

                            // Set an empty array in the DB to clear all packets
                            updateJSEmbeddedPackets(new JSEmbeddingItemTO[0]);

                            final com.mobicage.to.js_embedding.GetJSEmbeddingRequestTO request = new com.mobicage.to.js_embedding.GetJSEmbeddingRequestTO();
                            final GetJSEmbeddingResponseHandler responseHandler = new GetJSEmbeddingResponseHandler();
                            try {
                                com.mobicage.api.system.Rpc.getJsEmbedding(responseHandler, request);
                            } catch (Exception e) {
                                L.bug(e);
                            }

                            mMainService.clearPluginDBUpdate(SystemPlugin.class,
                                SYSTEM_PLUGIN_MUST_REFRESH_JS_EMBEDDING);
                        }

                    }
                });
            }
        });
    }

    public boolean getWifiOnlyDownloads() {
        return mWifiOnlyDownloads;
    }

    public Map<String, JSEmbedding> getJSEmbeddedPackets() {
        return mStore.getJSEmbeddedPackets();
    }

    public void updateJSEmbeddedPacket(final String name, final String embeddingHash, final long status) {
        mStore.updateJSEmbeddedPacket(name, embeddingHash, status);
    }

    public void updateJSEmbeddedPackets(final JSEmbeddingItemTO[] packets) {
        Map<String, JSEmbedding> oldPackets = getJSEmbeddedPackets();
        List<JSEmbeddingItemTO> packetsToDownload = new ArrayList<JSEmbeddingItemTO>();
        for (final JSEmbeddingItemTO packet : packets) {
            JSEmbedding s = oldPackets.get(packet.name);
            oldPackets.remove(packet.name);
            if (!(s != null && s.getEmeddingHash().equals(packet.hash) && s.getStatus() == JSEmbedding.STATUS_AVAILABLE)) {
                updateJSEmbeddedPacket(packet.name, packet.hash, JSEmbedding.STATUS_UNAVAILABLE);
                packetsToDownload.add(packet);
            }
        }
        for (final JSEmbeddingItemTO packet : packetsToDownload) {
            mBrandingMgr.queue(packet);
        }

        for (String key : oldPackets.keySet()) {
            mStore.deleteJSEmbeddedPacket(key);
            mBrandingMgr.cleanupJSEmbeddingPacket(key);
        }
    }

    public static MobileInfo gatherMobileInfo(MainService mainService) {
        T.dontCare();
        MobileInfo info = new MobileInfo();

        // Application info
        info.app.majorVersion = mainService.getMajorVersion();
        info.app.minorVersion = mainService.getMinorVersion();
        info.app.name = mainService.getString(R.string.app_name) + " Android";
        info.app.type = 4;

        // Carrier info
        TelephonyManager telephonyManager = (TelephonyManager) mainService.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            info.sim.carrierName = telephonyManager.getSimOperatorName();
            info.sim.mobileNetworkCode = telephonyManager.getSimOperator();
            info.sim.mobileCountryCode = null;
            info.sim.isoCountryCode = telephonyManager.getSimCountryIso();

            // Result may be unreliable on CDMA networks
            if (telephonyManager.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
                info.network.carrierName = telephonyManager.getNetworkOperatorName();
                info.network.mobileNetworkCode = telephonyManager.getNetworkOperator();
                info.network.mobileCountryCode = null;
                info.network.isoCountryCode = telephonyManager.getNetworkCountryIso();
            }
        }

        // Device info
        info.device.modelName = SystemUtils.isRunningInEmulator(mainService) ? "Android emulator" : Build.MODEL;
        info.device.osVersion = SystemUtils.getAndroidVersion() + "";

        // Locale info
        Locale locale = Locale.getDefault();
        info.locale.country = locale.getCountry();
        info.locale.language = locale.getLanguage();

        // TimeZone info
        TimeZone timeZone = TimeZone.getDefault();
        info.timeZone.abbrevation = timeZone.getID();
        info.timeZone.secondsFromGMT = timeZone.getRawOffset() / 1000;

        return info;
    }
}