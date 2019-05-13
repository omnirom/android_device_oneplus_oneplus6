package com.qualcomm.qti.networksetting;

import android.content.Intent;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.Phone;

public class CdmaOptions {
    private static final String BUTTON_APN_EXPAND_KEY = "button_apn_key_cdma";
    private static final String BUTTON_CARRIER_SETTINGS_KEY = "carrier_settings_key";
    private static final String BUTTON_CDMA_ACTIVATE_DEVICE_KEY = "cdma_activate_device_key";
    private static final String BUTTON_CDMA_SUBSCRIPTION_KEY = "cdma_subscription_key";
    private static final String BUTTON_CDMA_SYSTEM_SELECT_KEY = "cdma_system_select_key";
    private static final String LOG_TAG = "CdmaOptions";
    private PreferenceScreen mButtonAPNExpand;
    private CdmaSubscriptionListPreference mButtonCdmaSubscription;
    private CdmaSystemSelectListPreference mButtonCdmaSystemSelect;
    private CarrierConfigManager mConfigManager;
    private Phone mPhone;
    private PreferenceActivity mPrefActivity;
    private PreferenceScreen mPrefScreen;

    public CdmaOptions(PreferenceActivity prefActivity, PreferenceScreen prefScreen, Phone phone) {
        this.mPrefActivity = prefActivity;
        this.mPrefScreen = prefScreen;
        this.mPhone = phone;
        this.mConfigManager = (CarrierConfigManager) prefActivity.getSystemService("carrier_config");
        create();
    }

    /* Access modifiers changed, original: protected */
    public void create() {
        this.mPrefActivity.addPreferencesFromResource(R.xml.cdma_options);
        this.mButtonAPNExpand = (PreferenceScreen) this.mPrefScreen.findPreference(BUTTON_APN_EXPAND_KEY);
        boolean removedAPNExpand = false;
        PersistableBundle carrierConfig = this.mConfigManager.getConfigForSubId(this.mPhone.getSubId());
        if (!(carrierConfig.getBoolean("show_apn_setting_cdma_bool") || this.mButtonAPNExpand == null)) {
            this.mPrefScreen.removePreference(this.mButtonAPNExpand);
            removedAPNExpand = true;
        }
        if (!removedAPNExpand) {
            this.mButtonAPNExpand.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent("android.settings.APN_SETTINGS");
                    intent.putExtra(":settings:show_fragment_as_subsetting", true);
                    intent.putExtra("sub_id", CdmaOptions.this.mPhone.getSubId());
                    CdmaOptions.this.mPrefActivity.startActivity(intent);
                    return true;
                }
            });
        }
        this.mButtonCdmaSystemSelect = (CdmaSystemSelectListPreference) this.mPrefScreen.findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY);
        this.mButtonCdmaSubscription = (CdmaSubscriptionListPreference) this.mPrefScreen.findPreference(BUTTON_CDMA_SUBSCRIPTION_KEY);
        boolean isLTE = true;
        if (this.mPrefActivity.getResources().getBoolean(R.bool.config_disable_cdma_option)) {
            this.mPrefScreen.removePreference(this.mButtonCdmaSystemSelect);
            this.mPrefScreen.removePreference(this.mButtonCdmaSubscription);
        } else {
            this.mButtonCdmaSystemSelect.setEnabled(true);
            if (deviceSupportsNvAndRuim()) {
                log("Both NV and Ruim supported, ENABLE subscription type selection");
                this.mButtonCdmaSubscription.setEnabled(true);
            } else {
                log("Both NV and Ruim NOT supported, REMOVE subscription type selection");
                this.mPrefScreen.removePreference(this.mButtonCdmaSubscription);
            }
        }
        boolean voiceCapable = this.mPrefActivity.getResources().getBoolean(17957073);
        if (this.mPhone.getLteOnCdmaMode() != 1) {
            isLTE = false;
        }
        if (voiceCapable || isLTE) {
            this.mPrefScreen.removePreference(this.mPrefScreen.findPreference(BUTTON_CDMA_ACTIVATE_DEVICE_KEY));
        }
        if (!carrierConfig.getBoolean("carrier_settings_enable_bool")) {
            Preference pref = this.mPrefScreen.findPreference(BUTTON_CARRIER_SETTINGS_KEY);
            if (pref != null) {
                this.mPrefScreen.removePreference(pref);
            }
        }
    }

    private boolean deviceSupportsNvAndRuim() {
        String subscriptionsSupported = SystemProperties.get("ril.subscription.types");
        boolean nvSupported = false;
        boolean ruimSupported = false;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("deviceSupportsnvAnRum: prop=");
        stringBuilder.append(subscriptionsSupported);
        log(stringBuilder.toString());
        if (!TextUtils.isEmpty(subscriptionsSupported)) {
            boolean ruimSupported2 = false;
            ruimSupported = false;
            for (String subscriptionType : subscriptionsSupported.split(",")) {
                String subscriptionType2 = subscriptionType2.trim();
                if (subscriptionType2.equalsIgnoreCase("NV")) {
                    ruimSupported = true;
                }
                if (subscriptionType2.equalsIgnoreCase("RUIM")) {
                    ruimSupported2 = true;
                }
            }
            nvSupported = ruimSupported;
            ruimSupported = ruimSupported2;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("deviceSupportsnvAnRum: nvSupported=");
        stringBuilder.append(nvSupported);
        stringBuilder.append(" ruimSupported=");
        stringBuilder.append(ruimSupported);
        log(stringBuilder.toString());
        if (nvSupported && ruimSupported) {
            return true;
        }
        return false;
    }

    public boolean preferenceTreeClick(Preference preference) {
        if (preference.getKey().equals(BUTTON_CDMA_SYSTEM_SELECT_KEY)) {
            log("preferenceTreeClick: return BUTTON_CDMA_ROAMING_KEY true");
            return true;
        } else if (!preference.getKey().equals(BUTTON_CDMA_SUBSCRIPTION_KEY)) {
            return false;
        } else {
            log("preferenceTreeClick: return CDMA_SUBSCRIPTION_KEY true");
            return true;
        }
    }

    public void showDialog(Preference preference) {
        if (preference.getKey().equals(BUTTON_CDMA_SYSTEM_SELECT_KEY)) {
            this.mButtonCdmaSystemSelect.showDialog(null);
        } else if (preference.getKey().equals(BUTTON_CDMA_SUBSCRIPTION_KEY)) {
            this.mButtonCdmaSubscription.showDialog(null);
        }
    }

    /* Access modifiers changed, original: protected */
    public void log(String s) {
        Log.d(LOG_TAG, s);
    }
}
