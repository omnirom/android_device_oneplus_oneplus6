package com.qualcomm.qti.networksetting;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.SwitchPreference;
import android.provider.Settings.Global;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.MultiSimVariants;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.qualcomm.qti.networksetting.INetworkQueryServiceCallback.Stub;
import com.qualcomm.qti.networksetting.NetworkQueryService.LocalBinder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NetworkSetting extends PreferenceActivity implements OnPreferenceClickListener {
    private static final String ACTION_INCREMENTAL_NW_SCAN_IND = "qualcomm.intent.action.ACTION_INCREMENTAL_NW_SCAN_IND";
    private static final int ALREADY_IN_AUTO_SELECTION = 1;
    private static final String BUTTON_AUTO_SELECT_KEY = "button_auto_select_key";
    private static final int DIALOG_NETWORK_AUTO_SELECT = 2;
    private static final int DIALOG_NETWORK_QUIT = 3;
    private static final int DIALOG_NETWORK_SELECTION = 1;
    private static final int EVENT_AUTO_SELECT_DONE = 1;
    private static final int EVENT_INCREMENTAL_MANUAL_SCAN_RESULTS = 5;
    private static final int EVENT_NETWORK_DATA_MANAGER_DONE = 4;
    private static final int EVENT_NETWORK_SELECTION_DONE = 2;
    private static final int EVENT_QUERY_AVAILABLE_NETWORKS = 3;
    public static final String EXTRA_SUB_ID = "sub_id";
    private static final String LIST_AVAILABLE_NETWORKS = "network_list";
    private static final int MENU_ID_SCAN = 1;
    private static final int QUERY_ABORT = 2;
    private static final int QUERY_EXCEPTION = -1;
    private static final int QUERY_OK = 0;
    private static final int QUERY_PARTIAL = 1;
    private static final int QUERY_REJ_IN_RLF = 3;
    private static final String TAG = "NetworkSetting";
    private static boolean mManuallySelectFailed = false;
    private SwitchPreference mAutoSelect;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            boolean z = true;
            if ("android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction())) {
                int simState = TelephonyManager.getDefault().getSimState(SubscriptionManager.getSlotIndex(NetworkSetting.this.mSubId));
                String simStatus = intent.getStringExtra("ss");
                int slotId = intent.getIntExtra("phone", -1);
                if ("ABSENT".equals(simStatus) && NetworkSetting.this.mSlotId == slotId) {
                    if (NetworkSetting.this.mDataManager != null) {
                        NetworkSetting.this.mDataManager.updateDataState(true, null);
                    }
                    NetworkSetting.this.finish();
                } else if (simState != 5 && NetworkSetting.this.mState == State.SCAN) {
                    NetworkSetting networkSetting = NetworkSetting.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("SIM state is not ready: sim_state = ");
                    stringBuilder.append(simState);
                    stringBuilder.append("slot = ");
                    stringBuilder.append(SubscriptionManager.getSlotIndex(NetworkSetting.this.mSubId));
                    networkSetting.logd(stringBuilder.toString());
                    NetworkSetting.this.onNetworksListLoadResult(null, -1);
                }
            } else if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                if (Global.getInt(context.getContentResolver(), "airplane_mode_on", 0) == 0) {
                    z = false;
                }
                if (z) {
                    NetworkSetting.this.finish();
                }
            } else if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                if ("homekey".equals(intent.getStringExtra("reason"))) {
                    NetworkSetting.this.logd("home key pressed");
                    NetworkSetting.this.onBackPressed();
                }
            }
        }
    };
    private final INetworkQueryServiceCallback mCallback = new Stub() {
        public void onQueryComplete(List<OperatorInfo> networkInfoArray, int status) {
            NetworkSetting.this.logd("notifying message loop of query completion.");
            NetworkSetting.this.mHandler.obtainMessage(3, status, 0, networkInfoArray).sendToTarget();
        }

        public void onIncrementalManualScanResult(String[] scanInfo, int result) {
            NetworkSetting networkSetting = NetworkSetting.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifying onincrementalmanualscan .");
            stringBuilder.append(result);
            networkSetting.logd(stringBuilder.toString());
            NetworkSetting.this.mHandler.obtainMessage(5, result, 0, scanInfo).sendToTarget();
        }
    };
    NetworkSettingDataManager mDataManager = null;
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            NetworkSetting networkSetting;
            StringBuilder stringBuilder;
            switch (msg.what) {
                case 1:
                    AsyncResult ar = msg.obj;
                    networkSetting = NetworkSetting.this;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("EVENT_AUTO_SELECT_DONE msg.arg1 = ");
                    stringBuilder2.append(msg.arg1);
                    networkSetting.logd(stringBuilder2.toString());
                    NetworkSetting.this.onAutomaticResult(ar.exception, msg.arg1);
                    return;
                case 2:
                    NetworkSetting.this.onNetworkManuallyResult(msg.obj.exception);
                    return;
                case 3:
                    ArrayList<OperatorInfo> operatorInfoList = msg.obj;
                    if (msg.arg1 == -1) {
                        NetworkSetting.this.logd("query available networks fail");
                        NetworkSetting.this.onNetworksListLoadResult(null, -1);
                        return;
                    }
                    NetworkSetting networkSetting2 = NetworkSetting.this;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("query available networks number is: ");
                    stringBuilder3.append(operatorInfoList.size());
                    networkSetting2.logd(stringBuilder3.toString());
                    if (operatorInfoList.size() > 0) {
                        Iterator it = operatorInfoList.iterator();
                        while (it.hasNext()) {
                            NetworkSetting.this.setNetworkList((OperatorInfo) it.next());
                        }
                    }
                    NetworkSetting.this.mState = State.IDLE;
                    NetworkSetting.this.updateUIState();
                    if (NetworkSetting.this.mDataManager != null) {
                        NetworkSetting.this.mDataManager.updateDataState(true, null);
                        return;
                    }
                    return;
                case 4:
                    networkSetting = NetworkSetting.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("EVENT_NETWORK_DATA_MANAGER_DONE: ");
                    stringBuilder.append(msg.arg1);
                    networkSetting.logd(stringBuilder.toString());
                    if (msg.arg1 == 1) {
                        NetworkSetting.this.loadNetworksList();
                    } else {
                        NetworkSetting.this.finish();
                    }
                    if (NetworkSetting.this.mSearchButton != null) {
                        NetworkSetting.this.mSearchButton.setEnabled(true);
                    }
                    NetworkSetting.this.updateUIState();
                    return;
                case 5:
                    try {
                        NetworkSetting.this.mNetworkQueryService.unregisterCallback(NetworkSetting.this.mCallback);
                    } catch (RemoteException e) {
                        NetworkSetting networkSetting3 = NetworkSetting.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("EVENT_INCREMENTAL_MANUAL_SCAN_RESULTS: exception ");
                        stringBuilder.append(e);
                        networkSetting3.logd(stringBuilder.toString());
                    }
                    NetworkSetting.this.onNetworksListLoadResult((String[]) msg.obj, msg.arg1);
                    return;
                default:
                    NetworkSetting.this.logd("unknown event!");
                    return;
            }
        }
    };
    private Map<OPSelectPreference, OperatorInfo> mNetworkMap;
    private INetworkQueryService mNetworkQueryService = null;
    private final ServiceConnection mNetworkQueryServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            NetworkSetting.this.logd("connection created, binding local service.");
            NetworkSetting.this.mNetworkQueryService = ((LocalBinder) service).getService();
            if (!NetworkSetting.this.mAutoSelect.isChecked()) {
                if (NetworkSetting.this.isDataDisableRequired()) {
                    NetworkSetting.this.mDataManager.updateDataState(false, NetworkSetting.this.mHandler.obtainMessage(4));
                    return;
                }
                NetworkSetting.this.loadNetworksList();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            NetworkSetting.this.logd("connection disconnected, cleaning local binding.");
            NetworkSetting.this.mNetworkQueryService = null;
        }
    };
    private String mNetworkSelectMsg;
    private Phone mPhone;
    private PreferenceCategory mProgressPref;
    private HashMap<String, String> mRatMap;
    private MenuItem mSearchButton;
    private int mSlotId;
    private State mState = State.IDLE;
    private int mSubId;

    /* renamed from: com.qualcomm.qti.networksetting.NetworkSetting$17 */
    static /* synthetic */ class AnonymousClass17 {
        static final /* synthetic */ int[] $SwitchMap$com$qualcomm$qti$networksetting$NetworkSetting$State = new int[State.values().length];

        static {
            try {
                $SwitchMap$com$qualcomm$qti$networksetting$NetworkSetting$State[State.IDLE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$qualcomm$qti$networksetting$NetworkSetting$State[State.SCAN.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    private class OPSelectPreference extends Preference {
        private CompoundButton mButton;
        Context mContext;
        private boolean mSelect = false;

        public OPSelectPreference(Context context, AttributeSet attrs) {
            super(context, attrs, 16842895);
            this.mContext = context;
            setWidgetLayoutResource(R.string.network_operator_settings_class);
            setSelect(false);
        }

        public void onBindView(View view) {
            super.onBindView(view);
            this.mButton = (CompoundButton) view.findViewById(R.id.select_widget);
            this.mButton.setChecked(true);
            int i = 0;
            this.mButton.setClickable(false);
            CompoundButton compoundButton = this.mButton;
            if (!this.mSelect) {
                i = 8;
            }
            compoundButton.setVisibility(i);
        }

        public void setSelect(boolean select) {
            this.mSelect = select;
            notifyChanged();
        }
    }

    private enum State {
        IDLE,
        SCAN
    }

    /* Access modifiers changed, original: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.op_network_select);
        this.mNetworkMap = new LinkedHashMap();
        this.mAutoSelect = (SwitchPreference) getPreferenceScreen().findPreference(BUTTON_AUTO_SELECT_KEY);
        this.mProgressPref = (PreferenceCategory) getPreferenceScreen().findPreference(LIST_AVAILABLE_NETWORKS);
        this.mProgressPref.setLayoutResource(R.string.searchview_description_clear);
        this.mSubId = getIntent().getIntExtra("subscription", SubscriptionManager.getDefaultSubscriptionId());
        this.mSlotId = SubscriptionManager.getSlotIndex(this.mSubId);
        this.mPhone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(this.mSubId));
        this.mAutoSelect.setOnPreferenceClickListener(this);
        this.mProgressPref.setOnPreferenceClickListener(this);
        this.mRatMap = new HashMap();
        this.mDataManager = new NetworkSettingDataManager(this);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("persist.radio.network_select");
        stringBuilder.append(this.mPhone.getPhoneId());
        if (SystemProperties.getBoolean(stringBuilder.toString(), false)) {
            this.mAutoSelect.setChecked(false);
        } else {
            this.mAutoSelect.setChecked(true);
        }
        initRatMap();
        startService(new Intent(this, NetworkQueryService.class));
        bindService(new Intent(this, NetworkQueryService.class), this.mNetworkQueryServiceConnection, 1);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        intentFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        registerReceiver(this.mBroadcastReceiver, intentFilter);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /* Access modifiers changed, original: protected */
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mBroadcastReceiver);
        try {
            if (this.mNetworkQueryService != null) {
                this.mNetworkQueryService.unregisterCallback(this.mCallback);
                unbindService(this.mNetworkQueryServiceConnection);
            }
        } catch (RemoteException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onDestroy: exception from unregisterCallback ");
            stringBuilder.append(e);
            logd(stringBuilder.toString());
        }
        if (this.mDataManager != null) {
            this.mDataManager.updateDataState(true, null);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onStop() {
        super.onStop();
        if (mManuallySelectFailed) {
            mManuallySelectFailed = false;
            this.mAutoSelect.setChecked(true);
            this.mState = State.IDLE;
            clearNetworkList();
            updateUIState();
            onAutomaticSelect();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        this.mSearchButton = menu.add(0, 1, 0, R.string.search_networks).setEnabled(true);
        this.mSearchButton.setShowAsAction(1);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        updateUIState();
        if (isDataDisableRequired() && this.mSearchButton != null) {
            this.mSearchButton.setEnabled(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId != 1) {
            if (itemId == 16908332) {
                onBackPressed();
            }
        } else if (isDataDisableRequired()) {
            if (this.mSearchButton != null) {
                this.mSearchButton.setEnabled(false);
            }
            this.mDataManager.updateDataState(false, this.mHandler.obtainMessage(4));
        } else {
            loadNetworksList();
        }
        return super.onOptionsItemSelected(item);
    }

    /* Access modifiers changed, original: protected */
    public Dialog onCreateDialog(int id) {
        if (id != 1 && id != 2 && id != 3) {
            return null;
        }
        Dialog dialog;
        switch (id) {
            case 1:
                dialog = new ProgressDialog(this);
                dialog.setCancelable(false);
                ((ProgressDialog) dialog).setIndeterminate(true);
                ((ProgressDialog) dialog).setMessage(this.mNetworkSelectMsg);
                return dialog;
            case 2:
                dialog = new ProgressDialog(this);
                dialog.setCancelable(false);
                ((ProgressDialog) dialog).setMessage(getString(R.string.register_automatically));
                ((ProgressDialog) dialog).setIndeterminate(true);
                return dialog;
            case 3:
                return new Builder(this).setTitle(R.string.quit_mention).setMessage(R.string.dialog_message).setPositiveButton(17039370, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        try {
                            if (NetworkSetting.this.mNetworkQueryService != null) {
                                NetworkSetting.this.mNetworkQueryService.stopNetworkQuery(NetworkSetting.this.mCallback, NetworkSetting.this.mSubId);
                            }
                        } catch (RemoteException ex) {
                            NetworkSetting networkSetting = NetworkSetting.this;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("stopnetworkquery remote exception ");
                            stringBuilder.append(ex);
                            networkSetting.logd(stringBuilder.toString());
                        }
                        NetworkSetting.this.finish();
                    }
                }).setNeutralButton(17039360, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                }).create();
            default:
                return null;
        }
    }

    public boolean onPreferenceClick(Preference preference) {
        if (preference != this.mAutoSelect) {
            OperatorInfo operatorInfo = (OperatorInfo) this.mNetworkMap.get(preference);
            for (Preference p : this.mNetworkMap.keySet()) {
                if (preference != p) {
                    p.setSelect(false);
                } else if (operatorInfo.getState() != com.android.internal.telephony.OperatorInfo.State.FORBIDDEN) {
                    p.setSelect(true);
                }
            }
            mManuallySelectFailed = true;
            this.mPhone.selectNetworkManually(operatorInfo, true, this.mHandler.obtainMessage(2));
            onNetworkManuallySelect(operatorInfo);
            return true;
        } else if (this.mAutoSelect.isChecked()) {
            try {
                if (this.mNetworkQueryService != null) {
                    this.mNetworkQueryService.stopNetworkQuery(this.mCallback, this.mSubId);
                }
            } catch (RemoteException ex) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("stopnetworkquery remote exception ");
                stringBuilder.append(ex);
                logd(stringBuilder.toString());
            }
            this.mState = State.IDLE;
            clearNetworkList();
            updateUIState();
            onAutomaticSelect();
            if (this.mDataManager != null) {
                this.mDataManager.updateDataState(true, null);
            }
            return true;
        } else {
            this.mSearchButton.setVisible(true);
            if (isDataDisableRequired()) {
                this.mDataManager.updateDataState(false, this.mHandler.obtainMessage(4));
                return false;
            }
            loadNetworksList();
            return false;
        }
    }

    private void onNetworkManuallySelect(final OperatorInfo operatorInfo) {
        runOnUiThread(new Runnable() {
            public void run() {
                NetworkSetting.this.mNetworkSelectMsg = NetworkSetting.this.getString(R.string.register_on_network, new Object[]{NetworkSetting.this.getNetworkTitle(operatorInfo)});
                NetworkSetting.this.showDialog(1);
            }
        });
    }

    private int getErrorResId(Throwable ex) {
        if (ex == null) {
            return 0;
        }
        if ((ex instanceof CommandException) && ((CommandException) ex).getCommandError() == Error.ILLEGAL_SIM_OR_ME) {
            return R.string.not_allowed;
        }
        return R.string.connect_later;
    }

    private void onNetworkManuallyResult(final Throwable ex) {
        runOnUiThread(new Runnable() {
            public void run() {
                NetworkSetting.this.removeDialog(1);
                NetworkSetting.this.onSave(NetworkSetting.this.getErrorResId(ex), true);
                NetworkSetting.this.onResult(NetworkSetting.this.getErrorResId(ex));
            }
        });
    }

    private void onSave(final int errorMsg, final boolean isManual) {
        runOnUiThread(new Runnable() {
            public void run() {
                StringBuilder stringBuilder;
                if (errorMsg == 0) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("persist.radio.network_select");
                    stringBuilder.append(NetworkSetting.this.mPhone.getPhoneId());
                    SystemProperties.set(stringBuilder.toString(), Boolean.toString(isManual));
                    return;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("persist.radio.network_select");
                stringBuilder.append(NetworkSetting.this.mPhone.getPhoneId());
                SystemProperties.set(stringBuilder.toString(), Boolean.toString(isManual ^ 1));
            }
        });
    }

    private void loadNetworksList() {
        logd("loadnetworklist");
        if (this.mState == State.IDLE) {
            if (((TelecomManager) getSystemService("telecom")).isInCall()) {
                logd("while call exists, query available networks fail");
                onNetworksListLoadResult(null, -1);
                return;
            }
            try {
                logd("loadnetworklist idle");
                this.mState = State.SCAN;
                clearNetworkList();
                updateUIState();
                if (this.mNetworkQueryService != null) {
                    this.mNetworkQueryService.startNetworkQuery(this.mCallback, this.mSubId);
                }
            } catch (RemoteException e) {
                logd("loadnetworklist exception");
                this.mState = State.IDLE;
                updateUIState();
            }
        }
    }

    private void clearNetworkList() {
        runOnUiThread(new Runnable() {
            public void run() {
                for (OPSelectPreference p : NetworkSetting.this.mNetworkMap.keySet()) {
                    NetworkSetting.this.mProgressPref.removePreference(p);
                }
                NetworkSetting.this.mNetworkMap.clear();
            }
        });
    }

    private String getLocalString(String originalString) {
        String[] origNames = getResources().getStringArray(R.array.original_carrier_names);
        String[] localNames = getResources().getStringArray(R.array.locale_carrier_names);
        for (int i = 0; i < origNames.length; i++) {
            if (origNames[i].equalsIgnoreCase(originalString)) {
                return getString(getResources().getIdentifier(localNames[i], "string", getPackageName()));
            }
        }
        return originalString;
    }

    private String getNetworkTitle(OperatorInfo ni) {
        String title;
        StringBuilder stringBuilder;
        String numericOnly = "";
        String radioTech = "";
        String operatorNumeric = ni.getOperatorNumeric();
        if (operatorNumeric != null) {
            String[] values = operatorNumeric.split("\\+");
            numericOnly = values[0];
            if (values.length > 1) {
                radioTech = values[1];
            }
        }
        if (!TextUtils.isEmpty(ni.getOperatorAlphaLong())) {
            title = getLocalString(ni.getOperatorAlphaLong());
        } else if (TextUtils.isEmpty(ni.getOperatorAlphaShort())) {
            title = numericOnly;
        } else {
            title = getLocalString(ni.getOperatorAlphaShort());
        }
        if (!TextUtils.isEmpty(radioTech)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(title);
            stringBuilder.append(" ");
            stringBuilder.append((String) this.mRatMap.get(radioTech));
            title = stringBuilder.toString();
        }
        if (ni.getState() != com.android.internal.telephony.OperatorInfo.State.FORBIDDEN) {
            return title;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(title);
        stringBuilder.append(getString(R.string.network_forbidden));
        return stringBuilder.toString();
    }

    private void setNetworkList(final OperatorInfo operatorInfo) {
        runOnUiThread(new Runnable() {
            public void run() {
                OPSelectPreference carrier = new OPSelectPreference(NetworkSetting.this, null);
                carrier.setTitle(NetworkSetting.this.getNetworkTitle(operatorInfo));
                carrier.setPersistent(false);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("persist.radio.network_select");
                stringBuilder.append(NetworkSetting.this.mPhone.getPhoneId());
                if (SystemProperties.getBoolean(stringBuilder.toString(), false) && operatorInfo.getState() == com.android.internal.telephony.OperatorInfo.State.CURRENT) {
                    carrier.setSelect(true);
                }
                NetworkSetting.this.mProgressPref.addPreference(carrier);
                carrier.setOnPreferenceClickListener(NetworkSetting.this);
                NetworkSetting.this.mNetworkMap.put(carrier, operatorInfo);
            }
        });
    }

    private void onNetworksListLoadResult(final String[] result, final int status) {
        runOnUiThread(new Runnable() {
            public void run() {
                NetworkSetting networkSetting = NetworkSetting.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("result: ");
                stringBuilder.append(status);
                networkSetting.logd(stringBuilder.toString());
                switch (status) {
                    case 0:
                    case 2:
                        break;
                    case 1:
                        break;
                    case 3:
                        NetworkSetting.this.mState = State.IDLE;
                        NetworkSetting.this.clearNetworkList();
                        Toast.makeText(NetworkSetting.this, R.string.network_query_error, 0).show();
                        break;
                    default:
                        NetworkSetting.this.mState = State.IDLE;
                        Toast.makeText(NetworkSetting.this, R.string.network_query_error, 0).show();
                        NetworkSetting.this.clearNetworkList();
                        NetworkSetting.this.updateUIState();
                        if (NetworkSetting.this.mDataManager != null) {
                            NetworkSetting.this.mDataManager.updateDataState(true, null);
                            return;
                        }
                        return;
                }
                NetworkSetting.this.mState = State.IDLE;
                NetworkSetting.this.updateUIState();
                if (NetworkSetting.this.mDataManager != null) {
                    NetworkSetting.this.mDataManager.updateDataState(true, null);
                }
                if (result == null) {
                    return;
                }
                if (result.length < 4 || result.length % 4 != 0) {
                    networkSetting = NetworkSetting.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("result.length is: ");
                    stringBuilder.append(result.length);
                    networkSetting.logd(stringBuilder.toString());
                    return;
                }
                for (int i = 0; i < result.length / 4; i++) {
                    int j = 4 * i;
                    NetworkSetting.this.setNetworkList(new OperatorInfo(result[0 + j], result[1 + j], result[2 + j], result[3 + j]));
                }
            }
        });
    }

    private void updateUIState() {
        runOnUiThread(new Runnable() {
            public void run() {
                switch (AnonymousClass17.$SwitchMap$com$qualcomm$qti$networksetting$NetworkSetting$State[NetworkSetting.this.mState.ordinal()]) {
                    case 1:
                        if (NetworkSetting.this.mAutoSelect.isChecked()) {
                            if (NetworkSetting.this.mSearchButton != null) {
                                NetworkSetting.this.mSearchButton.setVisible(false);
                                return;
                            }
                            return;
                        } else if (NetworkSetting.this.mSearchButton != null) {
                            NetworkSetting.this.mSearchButton.setVisible(true);
                            NetworkSetting.this.mSearchButton.setEnabled(true);
                            NetworkSetting.this.mSearchButton.setTitle(R.string.search_networks);
                            return;
                        } else {
                            return;
                        }
                    case 2:
                        if (NetworkSetting.this.mAutoSelect.isChecked()) {
                            if (NetworkSetting.this.mSearchButton != null) {
                                NetworkSetting.this.mSearchButton.setVisible(false);
                                return;
                            }
                            return;
                        } else if (NetworkSetting.this.mSearchButton != null) {
                            NetworkSetting.this.mSearchButton.setEnabled(false);
                            NetworkSetting.this.mSearchButton.setTitle(R.string.progress_scanning);
                            return;
                        } else {
                            return;
                        }
                    default:
                        return;
                }
            }
        });
    }

    private void onAutomaticSelect() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (!NetworkSetting.this.isFinishing()) {
                    NetworkSetting.this.showDialog(2);
                }
                NetworkSetting.this.mPhone.setNetworkSelectionModeAutomatic(NetworkSetting.this.mHandler.obtainMessage(1));
            }
        });
    }

    private void onAutomaticResult(final Throwable ex, int msgArg1) {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    NetworkSetting.this.removeDialog(2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                NetworkSetting.this.onSave(NetworkSetting.this.getErrorResId(ex), false);
                NetworkSetting.this.onResult(NetworkSetting.this.getErrorResId(ex));
            }
        });
    }

    private void onResult(final int errorMsg) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (errorMsg != 0) {
                    Toast.makeText(NetworkSetting.this, errorMsg, 0).show();
                    NetworkSetting.mManuallySelectFailed = true;
                    return;
                }
                if (NetworkSetting.this.mPhone.getServiceState().getState() == 0) {
                    Toast.makeText(NetworkSetting.this, R.string.registration_done, 0).show();
                } else {
                    Toast.makeText(NetworkSetting.this, R.string.op_setting_done, 0).show();
                }
                NetworkSetting.mManuallySelectFailed = false;
                NetworkSetting.this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        NetworkSetting.this.finish();
                    }
                }, 3000);
            }
        });
    }

    public void onBackPressed() {
        if (this.mState != State.SCAN || this.mAutoSelect.isChecked()) {
            super.onBackPressed();
        } else {
            showDialog(3);
        }
    }

    private void initRatMap() {
        this.mRatMap.put(String.valueOf(0), "Unknown");
        this.mRatMap.put(String.valueOf(1), "2G");
        this.mRatMap.put(String.valueOf(2), "2G");
        this.mRatMap.put(String.valueOf(3), "3G");
        this.mRatMap.put(String.valueOf(4), "2G");
        this.mRatMap.put(String.valueOf(5), "2G");
        this.mRatMap.put(String.valueOf(6), "2G");
        this.mRatMap.put(String.valueOf(7), "3G");
        this.mRatMap.put(String.valueOf(8), "3G");
        this.mRatMap.put(String.valueOf(9), "3G");
        this.mRatMap.put(String.valueOf(10), "3G");
        this.mRatMap.put(String.valueOf(11), "3G");
        this.mRatMap.put(String.valueOf(12), "3G");
        this.mRatMap.put(String.valueOf(13), "3G");
        this.mRatMap.put(String.valueOf(14), "4G");
        this.mRatMap.put(String.valueOf(15), "3G");
        this.mRatMap.put(String.valueOf(16), "2G");
        this.mRatMap.put(String.valueOf(17), "3G");
    }

    private boolean isDataDisableRequired() {
        boolean isRequired = getApplicationContext().getResources().getBoolean(R.bool.config_disable_data_manual_plmn);
        if (TelephonyManager.getDefault().getMultiSimConfiguration() != MultiSimVariants.DSDA || SubscriptionManager.getDefaultDataSubscriptionId() == this.mPhone.getSubId()) {
            return isRequired;
        }
        return false;
    }

    private void logd(String msg) {
        Log.d(TAG, msg);
    }
}
