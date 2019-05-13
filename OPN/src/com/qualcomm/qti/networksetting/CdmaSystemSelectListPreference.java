package com.qualcomm.qti.networksetting;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.provider.Settings.Global;
import android.util.AttributeSet;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class CdmaSystemSelectListPreference extends ListPreference {
    private static final boolean DBG = false;
    private static final String LOG_TAG = "CdmaRoamingListPreference";
    private MyHandler mHandler;
    private Phone mPhone;

    private class MyHandler extends Handler {
        static final int MESSAGE_GET_ROAMING_PREFERENCE = 0;
        static final int MESSAGE_SET_ROAMING_PREFERENCE = 1;

        private MyHandler() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    handleQueryCdmaRoamingPreference(msg);
                    return;
                case 1:
                    handleSetCdmaRoamingPreference(msg);
                    return;
                default:
                    return;
            }
        }

        private void handleQueryCdmaRoamingPreference(Message msg) {
            AsyncResult ar = msg.obj;
            if (ar.exception == null) {
                int statusCdmaRoamingMode = ((int[]) ar.result)[0];
                int settingsRoamingMode = Global.getInt(CdmaSystemSelectListPreference.this.mPhone.getContext().getContentResolver(), "roaming_settings", 0);
                if (statusCdmaRoamingMode == 0 || statusCdmaRoamingMode == 2) {
                    if (statusCdmaRoamingMode != settingsRoamingMode) {
                        Global.putInt(CdmaSystemSelectListPreference.this.mPhone.getContext().getContentResolver(), "roaming_settings", statusCdmaRoamingMode);
                    }
                    CdmaSystemSelectListPreference.this.setValue(Integer.toString(statusCdmaRoamingMode));
                    return;
                }
                resetCdmaRoamingModeToDefault();
            }
        }

        private void handleSetCdmaRoamingPreference(Message msg) {
            if (msg.obj.exception != null || CdmaSystemSelectListPreference.this.getValue() == null) {
                CdmaSystemSelectListPreference.this.mPhone.queryCdmaRoamingPreference(obtainMessage(0));
                return;
            }
            Global.putInt(CdmaSystemSelectListPreference.this.mPhone.getContext().getContentResolver(), "roaming_settings", Integer.valueOf(CdmaSystemSelectListPreference.this.getValue()).intValue());
        }

        private void resetCdmaRoamingModeToDefault() {
            CdmaSystemSelectListPreference.this.setValue(Integer.toString(2));
            Global.putInt(CdmaSystemSelectListPreference.this.mPhone.getContext().getContentResolver(), "roaming_settings", 2);
            CdmaSystemSelectListPreference.this.mPhone.setCdmaRoamingPreference(2, obtainMessage(1));
        }
    }

    public CdmaSystemSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mHandler = new MyHandler();
        this.mPhone = PhoneFactory.getDefaultPhone();
        this.mHandler = new MyHandler();
        this.mPhone.queryCdmaRoamingPreference(this.mHandler.obtainMessage(0));
    }

    public CdmaSystemSelectListPreference(Context context) {
        this(context, null);
    }

    /* Access modifiers changed, original: protected */
    public void showDialog(Bundle state) {
        if (!Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
            super.showDialog(state);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        int statusCdmaRoamingMode = 0;
        if (!positiveResult || getValue() == null) {
            Log.d(LOG_TAG, String.format("onDialogClosed: positiveResult=%b value=%s -- do nothing", new Object[]{Boolean.valueOf(positiveResult), getValue()}));
            return;
        }
        int buttonCdmaRoamingMode = Integer.valueOf(getValue()).intValue();
        if (buttonCdmaRoamingMode != Global.getInt(this.mPhone.getContext().getContentResolver(), "roaming_settings", 0)) {
            if (buttonCdmaRoamingMode == 2) {
                statusCdmaRoamingMode = 2;
            }
            int statusCdmaRoamingMode2 = statusCdmaRoamingMode;
            Global.putInt(this.mPhone.getContext().getContentResolver(), "roaming_settings", buttonCdmaRoamingMode);
            this.mPhone.setCdmaRoamingPreference(statusCdmaRoamingMode2, this.mHandler.obtainMessage(1));
        }
    }
}
