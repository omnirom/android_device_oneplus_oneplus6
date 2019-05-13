package com.qualcomm.qti.networksetting;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class ManagedRoaming extends Activity {
    private static final String ACTION_NETWORK_OPERATOR_SETTINGS_ASYNC = "org.codeaurora.settings.NETWORK_OPERATOR_SETTINGS_ASYNC";
    private static final String EXTRA_SUB_ID = "sub_id";
    private static final String LOG_TAG = "ManagedRoaming";
    private static final String NETWORK_SELECTION_KEY = "network_selection_key";
    private final int NETWORK_SCAN_ACTIVITY_REQUEST_CODE = 0;
    private boolean mIsMRDialogShown = false;
    private Phone mPhone;
    private int mSubscription = Integer.MAX_VALUE;
    OnClickListener onManagedRoamingDialogClick = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            StringBuilder stringBuilder;
            switch (which) {
                case -2:
                    ManagedRoaming.this.finish();
                    break;
                case NetworkQueryService.QUERY_EXCEPTION /*-1*/:
                    ManagedRoaming managedRoaming = ManagedRoaming.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Launch network settings activity sub = ");
                    stringBuilder.append(ManagedRoaming.this.mSubscription);
                    managedRoaming.log(stringBuilder.toString());
                    Intent networkSettingIntent = new Intent(ManagedRoaming.ACTION_NETWORK_OPERATOR_SETTINGS_ASYNC);
                    networkSettingIntent.putExtra("sub_id", ManagedRoaming.this.mSubscription);
                    ManagedRoaming.this.startActivityForResult(networkSettingIntent, 0);
                    break;
                default:
                    String str = ManagedRoaming.LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("received unknown button type: ");
                    stringBuilder.append(which);
                    Log.w(str, stringBuilder.toString());
                    ManagedRoaming.this.finish();
                    break;
            }
            ManagedRoaming.this.mIsMRDialogShown = false;
        }
    };

    /* Access modifiers changed, original: protected */
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        int subscription = getIntent().getIntExtra("subscription", SubscriptionManager.getDefaultSubscriptionId());
        this.mPhone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(subscription));
        if (this.mPhone == null) {
            this.mPhone = PhoneFactory.getDefaultPhone();
        }
        createManagedRoamingDialog(subscription);
    }

    private void createManagedRoamingDialog(int subscription) {
        Resources r = Resources.getSystem();
        String networkSelection = PreferenceManager.getDefaultSharedPreferences(this.mPhone.getContext());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(NETWORK_SELECTION_KEY);
        stringBuilder.append(subscription);
        networkSelection = networkSelection.getString(stringBuilder.toString(), "");
        stringBuilder = new StringBuilder();
        stringBuilder.append(" Received Managed Roaming intent, networkSelection ");
        stringBuilder.append(networkSelection);
        stringBuilder.append(" Is Dialog Displayed ");
        stringBuilder.append(this.mIsMRDialogShown);
        stringBuilder.append(" sub = ");
        stringBuilder.append(subscription);
        log(stringBuilder.toString());
        if (TextUtils.isEmpty(networkSelection) || this.mIsMRDialogShown) {
            finish();
            return;
        }
        TelephonyManager tm = (TelephonyManager) getSystemService("phone");
        String title = getString(R.string.managed_roaming_title);
        this.mSubscription = subscription;
        int phoneId = SubscriptionManager.getPhoneId(subscription);
        if (tm.isMultiSimEnabled() && tm.getPhoneCount() > phoneId) {
            title = getString(R.string.managed_roaming_title_sub, new Object[]{Integer.valueOf(phoneId + 1)});
        }
        AlertDialog managedRoamingDialog = new Builder(this).setTitle(title).setMessage(R.string.managed_roaming_dialog_content).setPositiveButton(17039379, this.onManagedRoamingDialogClick).setNegativeButton(17039369, this.onManagedRoamingDialogClick).create();
        managedRoamingDialog.setCanceledOnTouchOutside(false);
        managedRoamingDialog.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                ManagedRoaming.this.mIsMRDialogShown = false;
                ManagedRoaming.this.finish();
            }
        });
        this.mIsMRDialogShown = true;
        managedRoamingDialog.getWindow().setType(2003);
        managedRoamingDialog.show();
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    /* Access modifiers changed, original: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        log("On activity result ");
        finish();
    }
}
