package com.qualcomm.qti.networksetting;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.provider.Settings.Global;
import android.util.AttributeSet;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class CdmaSubscriptionListPreference extends ListPreference {
    private static final int CDMA_SUBSCRIPTION_NV = 1;
    private static final int CDMA_SUBSCRIPTION_RUIM_SIM = 0;
    private static final String LOG_TAG = "CdmaSubscriptionListPreference";
    static final int preferredSubscriptionMode = 0;
    private CdmaSubscriptionButtonHandler mHandler;
    private Phone mPhone;

    private class CdmaSubscriptionButtonHandler extends Handler {
        static final int MESSAGE_SET_CDMA_SUBSCRIPTION = 0;

        private CdmaSubscriptionButtonHandler() {
        }

        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                handleSetCdmaSubscriptionMode(msg);
            }
        }

        private void handleSetCdmaSubscriptionMode(Message msg) {
            CdmaSubscriptionListPreference.this.mPhone = PhoneFactory.getDefaultPhone();
            AsyncResult ar = msg.obj;
            if (ar.exception == null) {
                Global.putInt(CdmaSubscriptionListPreference.this.mPhone.getContext().getContentResolver(), "subscription_mode", Integer.valueOf((String) ar.userObj).intValue());
                return;
            }
            Log.e(CdmaSubscriptionListPreference.LOG_TAG, "Setting Cdma subscription source failed");
        }
    }

    public CdmaSubscriptionListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPhone = PhoneFactory.getDefaultPhone();
        this.mHandler = new CdmaSubscriptionButtonHandler();
        setCurrentCdmaSubscriptionModeValue();
    }

    private void setCurrentCdmaSubscriptionModeValue() {
        setValue(Integer.toString(Global.getInt(this.mPhone.getContext().getContentResolver(), "subscription_mode", 0)));
    }

    public CdmaSubscriptionListPreference(Context context) {
        this(context, null);
    }

    /* Access modifiers changed, original: protected */
    public void showDialog(Bundle state) {
        setCurrentCdmaSubscriptionModeValue();
        super.showDialog(state);
    }

    /* Access modifiers changed, original: protected */
    public void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            int statusCdmaSubscriptionMode;
            int buttonCdmaSubscriptionMode = Integer.valueOf(getValue()).intValue();
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Setting new value ");
            stringBuilder.append(buttonCdmaSubscriptionMode);
            Log.d(str, stringBuilder.toString());
            switch (buttonCdmaSubscriptionMode) {
                case 0:
                    statusCdmaSubscriptionMode = 0;
                    break;
                case 1:
                    statusCdmaSubscriptionMode = 1;
                    break;
                default:
                    statusCdmaSubscriptionMode = 0;
                    break;
            }
            this.mPhone.setCdmaSubscription(statusCdmaSubscriptionMode, this.mHandler.obtainMessage(0, getValue()));
        }
    }
}
