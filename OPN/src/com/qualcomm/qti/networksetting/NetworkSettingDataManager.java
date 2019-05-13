package com.qualcomm.qti.networksetting;

import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class NetworkSettingDataManager {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "NetworkSettingDataManager";
    private boolean mAppDisableData = DBG;
    Context mContext;
    Message mMsg;
    private boolean mNetworkSearchDataDisabled = false;
    private boolean mNetworkSearchDataDisconnecting = false;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NetworkSettingDataManager.this.mNetworkSearchDataDisconnecting && action.equals("android.intent.action.ANY_DATA_STATE") && NetworkSettingDataManager.this.mTelephonyManager.getDataState() == 0) {
                NetworkSettingDataManager.this.log("network disconnect data done");
                NetworkSettingDataManager.this.mNetworkSearchDataDisabled = NetworkSettingDataManager.DBG;
                NetworkSettingDataManager.this.mNetworkSearchDataDisconnecting = false;
                if (NetworkSettingDataManager.this.mMsg != null) {
                    Handler handler = NetworkSettingDataManager.this.mMsg.getTarget();
                    if (handler != null) {
                        handler.sendMessage(handler.obtainMessage(NetworkSettingDataManager.this.mMsg.what, 1, NetworkSettingDataManager.this.mMsg.arg2));
                    }
                }
            }
        }
    };
    private TelephonyManager mTelephonyManager;

    private final class ConfirmDialogListener implements OnClickListener, OnCancelListener {
        Message msg1;

        ConfirmDialogListener(Message msg) {
            this.msg1 = msg;
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == -1) {
                if (NetworkSettingDataManager.this.mAppDisableData) {
                    NetworkSettingDataManager.this.log("disable data");
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction("android.intent.action.ANY_DATA_STATE");
                    NetworkSettingDataManager.this.mContext.registerReceiver(NetworkSettingDataManager.this.mReceiver, intentFilter);
                    NetworkSettingDataManager.this.mNetworkSearchDataDisconnecting = NetworkSettingDataManager.DBG;
                    NetworkSettingDataManager.this.mTelephonyManager.setDataEnabled(SubscriptionManager.getDefaultDataSubscriptionId(), false);
                } else {
                    this.msg1.arg1 = 1;
                    this.msg1.sendToTarget();
                }
            } else if (which == -2) {
                NetworkSettingDataManager.this.log("network search, do nothing");
                this.msg1.arg1 = 0;
                this.msg1.sendToTarget();
            }
        }

        public void onCancel(DialogInterface dialog) {
            this.msg1.arg1 = 0;
            this.msg1.sendToTarget();
        }
    }

    public NetworkSettingDataManager(Context context) {
        this.mContext = context;
        log(" Create NetworkSettingDataManager");
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mAppDisableData = SystemProperties.getBoolean("persist.radio.plmn_disable_data", DBG);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mAppDisableData: ");
        stringBuilder.append(this.mAppDisableData);
        log(stringBuilder.toString());
    }

    public void updateDataState(boolean enable, Message msg) {
        if (enable) {
            if ((this.mAppDisableData && this.mNetworkSearchDataDisabled) || this.mNetworkSearchDataDisconnecting) {
                log("Enabling data");
                this.mTelephonyManager.setDataEnabled(SubscriptionManager.getDefaultDataSubscriptionId(), DBG);
                try {
                    this.mContext.unregisterReceiver(this.mReceiver);
                } catch (Exception e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unregisterReceiver e = ");
                    stringBuilder.append(e.toString());
                    log(stringBuilder.toString());
                }
                this.mNetworkSearchDataDisabled = false;
                this.mNetworkSearchDataDisconnecting = false;
            }
        } else if (this.mTelephonyManager.getDataState() == 2 || this.mTelephonyManager.getDataEnabled()) {
            log("Data is in CONNECTED state");
            this.mMsg = msg;
            ConfirmDialogListener listener = new ConfirmDialogListener(msg);
            new Builder(this.mContext).setTitle(17039380).setIcon(17301543).setMessage(R.string.disconnect_data_confirm).setPositiveButton(17039370, listener).setNegativeButton(17039369, listener).setOnCancelListener(listener).create().show();
        } else {
            msg.arg1 = 1;
            msg.sendToTarget();
        }
    }

    private void log(String msg) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[NetworkSettingDataManager] ");
        stringBuilder.append(msg);
        Log.d(str, stringBuilder.toString());
    }
}
