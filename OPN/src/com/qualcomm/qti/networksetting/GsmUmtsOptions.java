package com.qualcomm.qti.networksetting;

import android.content.Intent;
import android.content.res.Resources;
import android.os.PersistableBundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class GsmUmtsOptions {
    private static final String BUTTON_APN_EXPAND_KEY = "button_apn_key";
    private static final String BUTTON_APN_KEY_CDMA = "button_apn_key_cdma";
    private static final String BUTTON_CARRIER_SETTINGS_KEY = "carrier_settings_key";
    private static final String BUTTON_OPERATOR_SELECTION_EXPAND_KEY = "button_carrier_sel_key";
    public static final String EXTRA_SUB_ID = "sub_id";
    private static final String LOG_TAG = "GsmUmtsOptions";
    private PreferenceScreen mButtonAPNExpand;
    private PreferenceScreen mButtonOperatorSelectionExpand;
    private CarrierConfigManager mConfigManager = ((CarrierConfigManager) this.mPrefActivity.getSystemService("carrier_config"));
    private Phone mPhone;
    private PreferenceActivity mPrefActivity;
    private PreferenceScreen mPrefScreen;
    private int mSubId;

    public GsmUmtsOptions(PreferenceActivity prefActivity, PreferenceScreen prefScreen, int subId) {
        this.mPrefActivity = prefActivity;
        this.mPrefScreen = prefScreen;
        this.mSubId = subId;
        updatePhone(subId);
        create();
    }

    /* Access modifiers changed, original: protected */
    public void create() {
        this.mPrefActivity.addPreferencesFromResource(R.xml.gsm_umts_options);
        this.mButtonAPNExpand = (PreferenceScreen) this.mPrefScreen.findPreference(BUTTON_APN_EXPAND_KEY);
        boolean removedAPNExpand = false;
        this.mButtonOperatorSelectionExpand = (PreferenceScreen) this.mPrefScreen.findPreference(BUTTON_OPERATOR_SELECTION_EXPAND_KEY);
        PersistableBundle carrierConfig = this.mConfigManager.getConfigForSubId(this.mSubId);
        if (this.mPhone.getPhoneType() != 1) {
            log("Not a GSM phone");
            if (this.mButtonAPNExpand != null) {
                this.mPrefScreen.removePreference(this.mButtonAPNExpand);
                removedAPNExpand = true;
            }
            this.mButtonOperatorSelectionExpand.setEnabled(false);
        } else {
            log("Not a CDMA phone");
            Resources res = this.mPrefActivity.getResources();
            PreferenceScreen buttonAPNCDMA = (PreferenceScreen) this.mPrefScreen.findPreference(BUTTON_APN_KEY_CDMA);
            if (buttonAPNCDMA != null) {
                this.mPrefScreen.removePreference(buttonAPNCDMA);
            }
            if (!(carrierConfig.getBoolean("apn_expand_bool") || this.mButtonAPNExpand == null)) {
                this.mPrefScreen.removePreference(this.mButtonAPNExpand);
                removedAPNExpand = true;
            }
            if (!carrierConfig.getBoolean("operator_selection_expand_bool")) {
                this.mPrefScreen.removePreference(this.mPrefScreen.findPreference(BUTTON_OPERATOR_SELECTION_EXPAND_KEY));
            }
            if (carrierConfig.getBoolean("csp_enabled_bool")) {
                if (this.mPhone.isCspPlmnEnabled()) {
                    log("[CSP] Enabling Operator Selection menu.");
                    this.mButtonOperatorSelectionExpand.setEnabled(true);
                } else {
                    log("[CSP] Disabling Operator Selection menu.");
                    this.mPrefScreen.removePreference(this.mPrefScreen.findPreference(BUTTON_OPERATOR_SELECTION_EXPAND_KEY));
                }
            }
        }
        if (!carrierConfig.getBoolean("carrier_settings_enable_bool")) {
            Preference pref = this.mPrefScreen.findPreference(BUTTON_CARRIER_SETTINGS_KEY);
            if (pref != null) {
                this.mPrefScreen.removePreference(pref);
            }
        }
        if (!removedAPNExpand) {
            this.mButtonAPNExpand.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent("android.settings.APN_SETTINGS");
                    intent.putExtra(":settings:show_fragment_as_subsetting", true);
                    intent.putExtra("sub_id", GsmUmtsOptions.this.mSubId);
                    GsmUmtsOptions.this.mPrefActivity.startActivity(intent);
                    return true;
                }
            });
        }
        if (this.mPrefScreen.findPreference(BUTTON_OPERATOR_SELECTION_EXPAND_KEY) != null) {
            this.mButtonOperatorSelectionExpand.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent("org.codeaurora.settings.NETWORK_OPERATOR_SETTINGS_ASYNC");
                    intent.putExtra("sub_id", GsmUmtsOptions.this.mSubId);
                    GsmUmtsOptions.this.mPrefActivity.startActivity(intent);
                    return true;
                }
            });
        }
    }

    public boolean preferenceTreeClick(Preference preference) {
        log("preferenceTreeClick: return false");
        return false;
    }

    private void updatePhone(int subId) {
        this.mPhone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(subId));
        if (this.mPhone == null) {
            this.mPhone = PhoneFactory.getDefaultPhone();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updatePhone:- subId=");
        stringBuilder.append(subId);
        log(stringBuilder.toString());
    }

    /* Access modifiers changed, original: protected */
    public void log(String s) {
        Log.d(LOG_TAG, s);
    }
}
