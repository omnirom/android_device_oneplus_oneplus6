package com.qualcomm.qti.networksetting;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.util.Log;

public class SimAlertNotification extends BroadcastReceiver implements OnClickListener, OnDismissListener {
    private static final boolean DEBUG = true;
    private static final String TAG = "SimAlertNotification";
    private Context mContext;
    private AlertDialog mNotUiccCardDialog = null;

    public void onReceive(Context context, Intent intent) {
        this.mContext = context;
        alertNotUiccCard();
    }

    private void alertNotUiccCard() {
        logd("alertNotUiccCard");
        if (this.mNotUiccCardDialog == null || !this.mNotUiccCardDialog.isShowing()) {
            this.mNotUiccCardDialog = new Builder(this.mContext, R.style.CustomAlertDialogBackground).setTitle(R.string.sim_info).setMessage(R.string.alert_not_ct_uicc_card).setNegativeButton(R.string.close, this).create();
            this.mNotUiccCardDialog.getWindow().setType(2008);
            this.mNotUiccCardDialog.setOnDismissListener(this);
            this.mNotUiccCardDialog.show();
        }
    }

    public void onDismiss(DialogInterface dialog) {
        if (this.mNotUiccCardDialog == ((AlertDialog) dialog)) {
            this.mNotUiccCardDialog = null;
        }
    }

    public void onClick(DialogInterface dialog, int which) {
    }

    /* Access modifiers changed, original: 0000 */
    public void logd(String msg) {
        Log.d(TAG, msg);
    }
}
