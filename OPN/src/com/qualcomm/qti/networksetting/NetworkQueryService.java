package com.qualcomm.qti.networksetting;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.qualcomm.qcrilhook.QcRilHook;
import com.qualcomm.qcrilhook.QcRilHookCallback;
import com.qualcomm.qti.networksetting.INetworkQueryService.Stub;
import java.util.ArrayList;

public class NetworkQueryService extends Service {
    private static final String ACTION_INCREMENTAL_NW_SCAN_IND = "qualcomm.intent.action.ACTION_INCREMENTAL_NW_SCAN_IND";
    private static final boolean DBG = true;
    private static final int EVENT_NETWORK_SCAN_COMPLETED = 100;
    private static final String LOG_TAG = "NetworkQuery";
    private static int PHONE_COUNT = 0;
    public static final int QUERY_EXCEPTION = -1;
    private static final int QUERY_IS_RUNNING = -2;
    public static final int QUERY_OK = 0;
    private static final int QUERY_PARTIAL = 1;
    private static final int QUERY_READY = -1;
    private final Stub mBinder = new Stub() {
        public void startNetworkQuery(INetworkQueryServiceCallback cb, int subId) {
            if (cb != null) {
                synchronized (NetworkQueryService.this.mCallbacks) {
                    int phoneId = SubscriptionManager.getPhoneId(subId);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("phoneIndex");
                    stringBuilder.append(phoneId);
                    NetworkQueryService.log(stringBuilder.toString());
                    if (SubscriptionManager.isValidPhoneId(phoneId)) {
                        NetworkQueryService.this.mCallbacks.register(cb, new Integer(phoneId));
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("registering callback ");
                        stringBuilder.append(cb.getClass().toString());
                        NetworkQueryService.log(stringBuilder.toString());
                        QueryDetails queryDetails = NetworkQueryService.this.mQueryDetails[phoneId];
                        switch (queryDetails.state) {
                            case NetworkQueryService.QUERY_IS_RUNNING /*-2*/:
                                NetworkQueryService.log("query already in progress");
                                break;
                            case -1:
                                queryDetails.storeScanInfo = null;
                                boolean success = NetworkQueryService.this.mQcRilHook.qcRilPerformIncrManualScan(phoneId);
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("start scan, result:");
                                stringBuilder2.append(success);
                                NetworkQueryService.log(stringBuilder2.toString());
                                if (!success) {
                                    Phone phone = PhoneFactory.getPhone(phoneId);
                                    if (phone != null) {
                                        phone.getAvailableNetworks(NetworkQueryService.this.mHandler.obtainMessage(100, new Integer(phoneId)));
                                        queryDetails.state = NetworkQueryService.QUERY_IS_RUNNING;
                                        NetworkQueryService.log("starting new query");
                                    } else {
                                        NetworkQueryService.log("phone is null");
                                    }
                                    break;
                                }
                                queryDetails.state = NetworkQueryService.QUERY_IS_RUNNING;
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }

        public void stopNetworkQuery(INetworkQueryServiceCallback cb, int subId) {
            unregisterCallback(cb);
            int phoneId = SubscriptionManager.getPhoneId(subId);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("phoneIndex");
            stringBuilder.append(phoneId);
            NetworkQueryService.log(stringBuilder.toString());
            if (SubscriptionManager.isValidPhoneId(phoneId)) {
                synchronized (NetworkQueryService.this.mCallbacks) {
                    QueryDetails queryDetails = NetworkQueryService.this.mQueryDetails[phoneId];
                    switch (queryDetails.state) {
                        case NetworkQueryService.QUERY_IS_RUNNING /*-2*/:
                            if (!NetworkQueryService.this.mQcRilHook.qcRilAbortNetworkScan(phoneId)) {
                                NetworkQueryService.log("failed to cancel ongoing network query");
                                break;
                            }
                            NetworkQueryService.log("successfully cancelled ongoing network query");
                            queryDetails.state = -1;
                            break;
                        case -1:
                            NetworkQueryService.log("network scan not running.. no need to send cancel");
                            break;
                        default:
                            break;
                    }
                }
                return;
            }
            NetworkQueryService.log("invalid phone id");
        }

        public void unregisterCallback(INetworkQueryServiceCallback cb) {
            if (cb != null) {
                synchronized (NetworkQueryService.this.mCallbacks) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unregistering callback ");
                    stringBuilder.append(cb.getClass().toString());
                    NetworkQueryService.log(stringBuilder.toString());
                    NetworkQueryService.this.mCallbacks.unregister(cb);
                }
            }
        }
    };
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            NetworkQueryService.log("onreceive qualcomm.intent.action.ACTION_INCREMENTAL_NW_SCAN_IND");
            if (NetworkQueryService.ACTION_INCREMENTAL_NW_SCAN_IND.equals(intent.getAction())) {
                NetworkQueryService.this.broadcastIncrementalQueryResults(intent);
            } else if ("android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction())) {
                for (int i = 0; i < NetworkQueryService.PHONE_COUNT; i++) {
                    int[] subId = SubscriptionManager.getSubId(i);
                    if (subId != null && subId.length > 0) {
                        int simState = TelephonyManager.getDefault().getSimState(SubscriptionManager.getSlotIndex(subId[0]));
                        if (simState != 5) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("SIM state is not ready: sim_state = ");
                            stringBuilder.append(simState);
                            stringBuilder.append("slot = ");
                            stringBuilder.append(SubscriptionManager.getSlotIndex(subId[0]));
                            NetworkQueryService.log(stringBuilder.toString());
                            NetworkQueryService.this.mQueryDetails[i].reset();
                        }
                    }
                }
            }
        }
    };
    final RemoteCallbackList<INetworkQueryServiceCallback> mCallbacks = new RemoteCallbackList();
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 100) {
                NetworkQueryService.log("scan completed, broadcasting results");
                NetworkQueryService.this.broadcastQueryResults((AsyncResult) msg.obj);
            }
        }
    };
    private final IBinder mLocalBinder = new LocalBinder();
    private QcRilHook mQcRilHook;
    private QcRilHookCallback mQcRilHookCallback = new QcRilHookCallback() {
        public void onQcRilHookReady() {
            NetworkQueryService.log("onqcrilhookready");
        }

        public void onQcRilHookDisconnected() {
            NetworkQueryService.log("onqcrilhookdisconnected");
        }
    };
    private QueryDetails[] mQueryDetails;

    public class LocalBinder extends Binder {
        /* Access modifiers changed, original: 0000 */
        public INetworkQueryService getService() {
            return NetworkQueryService.this.mBinder;
        }
    }

    class QueryDetails {
        int state = -1;
        String[] storeScanInfo = null;

        QueryDetails() {
        }

        /* Access modifiers changed, original: 0000 */
        public void concatScanInfo(String[] scanInfo) {
            String[] concatScanInfo = new String[(this.storeScanInfo.length + scanInfo.length)];
            System.arraycopy(this.storeScanInfo, 0, concatScanInfo, 0, this.storeScanInfo.length);
            System.arraycopy(scanInfo, 0, concatScanInfo, this.storeScanInfo.length, scanInfo.length);
            this.storeScanInfo = concatScanInfo;
        }

        /* Access modifiers changed, original: 0000 */
        public void reset() {
            this.state = -1;
            this.storeScanInfo = null;
        }
    }

    public void onCreate() {
        PHONE_COUNT = TelephonyManager.getDefault().getPhoneCount();
        this.mQueryDetails = new QueryDetails[PHONE_COUNT];
        for (int i = 0; i < PHONE_COUNT; i++) {
            this.mQueryDetails[i] = new QueryDetails();
        }
        this.mQcRilHook = new QcRilHook(this, this.mQcRilHookCallback);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_INCREMENTAL_NW_SCAN_IND);
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        registerReceiver(this.mBroadcastReceiver, intentFilter);
    }

    public void onStart(Intent intent, int startId) {
    }

    public void onDestroy() {
        unregisterReceiver(this.mBroadcastReceiver);
    }

    public IBinder onBind(Intent intent) {
        log("binding service implementation");
        return this.mLocalBinder;
    }

    private void broadcastQueryResults(AsyncResult ar) {
        if (ar == null) {
            log("AsyncResult is null.");
            return;
        }
        Integer phoneIndex = ar.userObj;
        int phoneId = phoneIndex.intValue();
        if (SubscriptionManager.isValidPhoneId(phoneId)) {
            QueryDetails queryDetails = this.mQueryDetails[phoneId];
            synchronized (this.mCallbacks) {
                int i = -1;
                if (queryDetails.state == -1) {
                    return;
                }
                queryDetails.state = -1;
                if (ar.exception == null) {
                    i = 0;
                }
                int exception = i;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AsyncResult has exception ");
                stringBuilder.append(exception);
                log(stringBuilder.toString());
                for (i = this.mCallbacks.beginBroadcast() - 1; i >= 0; i--) {
                    INetworkQueryServiceCallback cb = (INetworkQueryServiceCallback) this.mCallbacks.getBroadcastItem(i);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("broadcasting results to ");
                    stringBuilder2.append(cb.getClass().toString());
                    log(stringBuilder2.toString());
                    try {
                        if (phoneIndex.equals(this.mCallbacks.getBroadcastCookie(i))) {
                            cb.onQueryComplete((ArrayList) ar.result, exception);
                        }
                    } catch (RemoteException e) {
                    }
                }
                this.mCallbacks.finishBroadcast();
            }
        }
    }

    /* JADX WARNING: Missing block: B:42:0x00ed, code skipped:
            return;
     */
    private void broadcastIncrementalQueryResults(android.content.Intent r13) {
        /*
        r12 = this;
        r0 = r12.mCallbacks;
        monitor-enter(r0);
        r1 = "scan_result";
        r2 = -1;
        r1 = r13.getIntExtra(r1, r2);	 Catch:{ all -> 0x00ee }
        r3 = "sub_id";
        r4 = 2147483647; // 0x7fffffff float:NaN double:1.060997895E-314;
        r3 = r13.getIntExtra(r3, r4);	 Catch:{ all -> 0x00ee }
        r4 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00ee }
        r4.<init>();	 Catch:{ all -> 0x00ee }
        r5 = "phoneid ";
        r4.append(r5);	 Catch:{ all -> 0x00ee }
        r4.append(r3);	 Catch:{ all -> 0x00ee }
        r4 = r4.toString();	 Catch:{ all -> 0x00ee }
        log(r4);	 Catch:{ all -> 0x00ee }
        r4 = android.telephony.SubscriptionManager.isValidPhoneId(r3);	 Catch:{ all -> 0x00ee }
        if (r4 != 0) goto L_0x002f;
    L_0x002d:
        monitor-exit(r0);	 Catch:{ all -> 0x00ee }
        return;
    L_0x002f:
        r4 = "incr_nw_scan_data";
        r4 = r13.getStringArrayExtra(r4);	 Catch:{ all -> 0x00ee }
        r5 = r12.mQueryDetails;	 Catch:{ all -> 0x00ee }
        r5 = r5[r3];	 Catch:{ all -> 0x00ee }
        r6 = r5.state;	 Catch:{ all -> 0x00ee }
        if (r6 != r2) goto L_0x003f;
    L_0x003d:
        monitor-exit(r0);	 Catch:{ all -> 0x00ee }
        return;
    L_0x003f:
        r6 = 1;
        if (r1 == r6) goto L_0x0044;
    L_0x0042:
        r5.state = r2;	 Catch:{ all -> 0x00ee }
    L_0x0044:
        r2 = android.telephony.SubscriptionManager.getSubId(r3);	 Catch:{ all -> 0x00ee }
        if (r2 == 0) goto L_0x00e7;
    L_0x004a:
        r7 = r2.length;	 Catch:{ all -> 0x00ee }
        if (r7 <= 0) goto L_0x00e7;
    L_0x004d:
        r7 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00ee }
        r7.<init>();	 Catch:{ all -> 0x00ee }
        r8 = "on scann result received, result:";
        r7.append(r8);	 Catch:{ all -> 0x00ee }
        r7.append(r1);	 Catch:{ all -> 0x00ee }
        r8 = ", subscription:";
        r7.append(r8);	 Catch:{ all -> 0x00ee }
        r8 = 0;
        r9 = r2[r8];	 Catch:{ all -> 0x00ee }
        r7.append(r9);	 Catch:{ all -> 0x00ee }
        r9 = ",scanInfo:";
        r7.append(r9);	 Catch:{ all -> 0x00ee }
        if (r4 != 0) goto L_0x006d;
    L_0x006c:
        goto L_0x006e;
    L_0x006d:
        r8 = r4.length;	 Catch:{ all -> 0x00ee }
    L_0x006e:
        r7.append(r8);	 Catch:{ all -> 0x00ee }
        r7 = r7.toString();	 Catch:{ all -> 0x00ee }
        log(r7);	 Catch:{ all -> 0x00ee }
        r7 = r5.storeScanInfo;	 Catch:{ all -> 0x00ee }
        if (r7 == 0) goto L_0x0082;
    L_0x007c:
        if (r4 == 0) goto L_0x0082;
    L_0x007e:
        r5.concatScanInfo(r4);	 Catch:{ all -> 0x00ee }
        goto L_0x0084;
    L_0x0082:
        r5.storeScanInfo = r4;	 Catch:{ all -> 0x00ee }
    L_0x0084:
        r7 = new java.lang.Integer;	 Catch:{ all -> 0x00ee }
        r7.<init>(r3);	 Catch:{ all -> 0x00ee }
        r8 = r12.mCallbacks;	 Catch:{ all -> 0x00ee }
        r8 = r8.beginBroadcast();	 Catch:{ all -> 0x00ee }
        r8 = r8 - r6;
    L_0x0090:
        r6 = r8;
        if (r6 < 0) goto L_0x00e1;
    L_0x0093:
        r8 = r12.mCallbacks;	 Catch:{ all -> 0x00ee }
        r8 = r8.getBroadcastItem(r6);	 Catch:{ all -> 0x00ee }
        r8 = (com.qualcomm.qti.networksetting.INetworkQueryServiceCallback) r8;	 Catch:{ all -> 0x00ee }
        r9 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00ee }
        r9.<init>();	 Catch:{ all -> 0x00ee }
        r10 = "broadcasting results to ";
        r9.append(r10);	 Catch:{ all -> 0x00ee }
        r10 = r8.getClass();	 Catch:{ all -> 0x00ee }
        r10 = r10.toString();	 Catch:{ all -> 0x00ee }
        r9.append(r10);	 Catch:{ all -> 0x00ee }
        r9 = r9.toString();	 Catch:{ all -> 0x00ee }
        log(r9);	 Catch:{ all -> 0x00ee }
        r9 = r12.mCallbacks;	 Catch:{ RemoteException -> 0x00c9 }
        r9 = r9.getBroadcastCookie(r6);	 Catch:{ RemoteException -> 0x00c9 }
        r9 = r7.equals(r9);	 Catch:{ RemoteException -> 0x00c9 }
        if (r9 == 0) goto L_0x00c8;
    L_0x00c3:
        r9 = r5.storeScanInfo;	 Catch:{ RemoteException -> 0x00c9 }
        r8.onIncrementalManualScanResult(r9, r1);	 Catch:{ RemoteException -> 0x00c9 }
    L_0x00c8:
        goto L_0x00de;
    L_0x00c9:
        r9 = move-exception;
        r10 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00ee }
        r10.<init>();	 Catch:{ all -> 0x00ee }
        r11 = "Partial query remote exception ";
        r10.append(r11);	 Catch:{ all -> 0x00ee }
        r10.append(r9);	 Catch:{ all -> 0x00ee }
        r10 = r10.toString();	 Catch:{ all -> 0x00ee }
        log(r10);	 Catch:{ all -> 0x00ee }
    L_0x00de:
        r8 = r6 + -1;
        goto L_0x0090;
    L_0x00e1:
        r6 = r12.mCallbacks;	 Catch:{ all -> 0x00ee }
        r6.finishBroadcast();	 Catch:{ all -> 0x00ee }
        goto L_0x00ec;
    L_0x00e7:
        r6 = "no valid sub_id for nw scan result";
        log(r6);	 Catch:{ all -> 0x00ee }
    L_0x00ec:
        monitor-exit(r0);	 Catch:{ all -> 0x00ee }
        return;
    L_0x00ee:
        r1 = move-exception;
        monitor-exit(r0);	 Catch:{ all -> 0x00ee }
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.qualcomm.qti.networksetting.NetworkQueryService.broadcastIncrementalQueryResults(android.content.Intent):void");
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
