package com.qualcomm.qti.networksetting.uplmn;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings.System;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.PlmnActRecord;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.qualcomm.qti.networksetting.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserPLMNListPreference extends PreferenceActivity implements OnCancelListener {
    private static final int BUSY_READING_DIALOG = 99;
    private static final int BUSY_SAVING_DIALOG = 100;
    private static final String BUTTON_UPLMN_LIST_KEY = "button_uplmn_list_key";
    private static final boolean DBG = true;
    private static final int GSM_COMPACT_MASK = 2;
    private static final int GSM_MASK = 1;
    private static final String LOG_TAG = "UserPLMNListPreference";
    private static final int LTE_MASK = 8;
    private static final int MENU_ADD_OPTIION = 1;
    private static final int UMTS_MASK = 4;
    private static final int UPLMNLIST_ADD = 101;
    private static final int UPLMNLIST_EDIT = 102;
    private static final int UPLMN_SEL_DATA_LEN = 5;
    private boolean mAirplaneModeOn = false;
    private MyHandler mHandler = new MyHandler(this, null);
    private IccFileHandler mIccFileHandler = null;
    private IntentFilter mIntentFilter;
    protected boolean mIsForeground = false;
    private int mNumRec = 0;
    private UPLMNInfoWithEf mOldInfo;
    private int mPhoneId = 0;
    private Map<Preference, UPLMNInfoWithEf> mPreferenceMap = new LinkedHashMap();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                UserPLMNListPreference.this.mAirplaneModeOn = intent.getBooleanExtra("state", false);
                UserPLMNListPreference.this.setScreenEnabled();
            }
        }
    };
    private List<UPLMNInfoWithEf> mUPLMNList;
    private PreferenceScreen mUPLMNListContainer;

    private class MyHandler extends Handler {
        private static final int MESSAGE_GET_EF_DONE = 1;
        private static final int MESSAGE_GET_UPLMN_LIST = 0;
        private static final int MESSAGE_SET_EF_DONE = 2;

        private MyHandler() {
        }

        /* synthetic */ MyHandler(UserPLMNListPreference x0, AnonymousClass1 x1) {
            this();
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    handleGetUPLMNList(msg);
                    return;
                case 1:
                    handleGetEFDone(msg);
                    return;
                case 2:
                    handleSetEFDone(msg);
                    return;
                default:
                    return;
            }
        }

        public void handleGetUPLMNList(Message msg) {
            UserPLMNListPreference.log("handleGetUPLMNList: done");
            if (msg.arg2 == 0) {
                UserPLMNListPreference.this.onFinished(UserPLMNListPreference.this.mUPLMNListContainer, UserPLMNListPreference.DBG);
            } else {
                UserPLMNListPreference.this.onFinished(UserPLMNListPreference.this.mUPLMNListContainer, false);
            }
            AsyncResult ar = msg.obj;
            if (ar.exception != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleGetUPLMNList with exception = ");
                stringBuilder.append(ar.exception);
                UserPLMNListPreference.log(stringBuilder.toString());
                if (UserPLMNListPreference.this.mUPLMNList == null) {
                    UserPLMNListPreference.this.mUPLMNList = new ArrayList();
                    return;
                }
                return;
            }
            UserPLMNListPreference.this.refreshUPLMNListPreference((ArrayList) ar.result);
        }

        public void handleSetEFDone(Message msg) {
            UserPLMNListPreference.log("handleSetEFDone: done");
            AsyncResult ar = msg.obj;
            if (ar.exception != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleSetEFDone with exception = ");
                stringBuilder.append(ar.exception);
                UserPLMNListPreference.log(stringBuilder.toString());
            } else {
                UserPLMNListPreference.log("handleSetEFDone: with OK result!");
            }
            UserPLMNListPreference.this.getUPLMNInfoFromEf();
        }

        public void handleGetEFDone(Message msg) {
            UserPLMNListPreference.log("handleGetEFDone: done");
            AsyncResult ar = msg.obj;
            if (ar.exception != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleGetEFDone with exception = ");
                stringBuilder.append(ar.exception);
                UserPLMNListPreference.log(stringBuilder.toString());
                Message message = UserPLMNListPreference.this.mHandler.obtainMessage();
                message.what = 0;
                message.obj = msg.obj;
                UserPLMNListPreference.this.mHandler.sendMessage(message);
                return;
            }
            byte[] data = ar.result;
            UserPLMNListPreference.this.mNumRec = data.length / 5;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Received a PlmnActRecord, raw=");
            stringBuilder2.append(IccUtils.bytesToHexString(data));
            UserPLMNListPreference.log(stringBuilder2.toString());
            PlmnActRecord[] plmnActRecords = PlmnActRecord.getRecords(data);
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("PlmnActRecords=");
            stringBuilder3.append(Arrays.toString(plmnActRecords));
            UserPLMNListPreference.log(stringBuilder3.toString());
            ArrayList<UPLMNInfoWithEf> ret = new ArrayList();
            int i = 0;
            for (PlmnActRecord record : plmnActRecords) {
                if (!(record.plmn.startsWith("fffff") || record.accessTechs == -1)) {
                    ret.add(new UPLMNInfoWithEf(record.plmn, UserPLMNListPreference.this.convertAccessTech2NetworkMode(record.accessTechs), i));
                }
                i++;
            }
            Message message2 = UserPLMNListPreference.this.mHandler.obtainMessage();
            message2.what = 0;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("handleGetEFDone : ret.size()");
            stringBuilder4.append(ret.size());
            UserPLMNListPreference.log(stringBuilder4.toString());
            message2.obj = new AsyncResult(message2.obj, ret, null);
            UserPLMNListPreference.this.mHandler.sendMessage(message2);
        }
    }

    class PriorityCompare implements Comparator<UPLMNInfoWithEf> {
        PriorityCompare() {
        }

        public int compare(UPLMNInfoWithEf object1, UPLMNInfoWithEf object2) {
            return object1.getPriority() - object2.getPriority();
        }
    }

    class UPLMNInfoWithEf {
        private int mNetworkMode;
        private String mOperatorNumeric;
        private int mPriority;

        public String getOperatorNumeric() {
            return this.mOperatorNumeric;
        }

        public int getNetworMode() {
            return this.mNetworkMode;
        }

        public int getPriority() {
            return this.mPriority;
        }

        public void setOperatorNumeric(String operatorNumeric) {
            this.mOperatorNumeric = operatorNumeric;
        }

        public void setPriority(int index) {
            this.mPriority = index;
        }

        public UPLMNInfoWithEf(String operatorNumeric, int mNetworkMode, int mPriority) {
            this.mOperatorNumeric = operatorNumeric;
            this.mNetworkMode = mNetworkMode;
            this.mPriority = mPriority;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UPLMNInfoWithEf ");
            stringBuilder.append(this.mOperatorNumeric);
            stringBuilder.append("/");
            stringBuilder.append(this.mNetworkMode);
            stringBuilder.append("/");
            stringBuilder.append(this.mPriority);
            return stringBuilder.toString();
        }
    }

    /* Access modifiers changed, original: protected */
    public Dialog onCreateDialog(int id) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle(getText(R.string.uplmn_list_setting_title));
        dialog.setIndeterminate(DBG);
        switch (id) {
            case 99:
                dialog.setCancelable(DBG);
                dialog.setOnCancelListener(this);
                dialog.setMessage(getText(R.string.reading_settings));
                return dialog;
            case 100:
                dialog.setCancelable(false);
                dialog.setMessage(getText(R.string.updating_settings));
                return dialog;
            default:
                return null;
        }
    }

    public void onCancel(DialogInterface dialog) {
        finish();
    }

    /* Access modifiers changed, original: protected */
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.uplmn_list);
        this.mUPLMNListContainer = (PreferenceScreen) findPreference(BUTTON_UPLMN_LIST_KEY);
        this.mPhoneId = getIntent().getIntExtra("phone", SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultSubscriptionId()));
        loadIccFileHandler();
        this.mIntentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        registerReceiver(this.mReceiver, this.mIntentFilter);
    }

    private void loadIccFileHandler() {
        UiccCard newCard = null;
        UiccController uiccController = UiccController.getInstance();
        if (uiccController != null) {
            newCard = uiccController.getUiccCard(this.mPhoneId);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("newCard = ");
        stringBuilder.append(newCard);
        log(stringBuilder.toString());
        if (newCard != null) {
            UiccCardApplication newUiccApplication = newCard.getApplication(1);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("newUiccApplication = ");
            stringBuilder2.append(newUiccApplication);
            log(stringBuilder2.toString());
            if (newUiccApplication != null) {
                this.mIccFileHandler = newUiccApplication.getIccFileHandler();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("fh = ");
                stringBuilder2.append(this.mIccFileHandler);
                log(stringBuilder2.toString());
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mReceiver);
    }

    public void onResume() {
        super.onResume();
        boolean z = DBG;
        this.mIsForeground = DBG;
        getUPLMNInfoFromEf();
        showReadingDialog();
        if (System.getInt(getContentResolver(), "airplane_mode_on", 0) != 1) {
            z = false;
        }
        this.mAirplaneModeOn = z;
    }

    public void onPause() {
        super.onPause();
        this.mIsForeground = false;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 1, 0, R.string.uplmn_list_setting_add_plmn).setShowAsAction(1);
        return DBG;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu != null) {
            menu.setGroupEnabled(0, this.mAirplaneModeOn ^ 1);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId != 1) {
            if (itemId == 16908332) {
                finish();
                return DBG;
            }
        } else if (this.mIccFileHandler == null) {
            return DBG;
        } else {
            Intent intent = new Intent(this, UPLMNEditor.class);
            intent.putExtra(UPLMNEditor.UPLMN_CODE, "");
            intent.putExtra(UPLMNEditor.UPLMN_PRIORITY, 0);
            intent.putExtra(UPLMNEditor.UPLMN_SERVICE, 0);
            intent.putExtra(UPLMNEditor.UPLMN_ADD, DBG);
            intent.putExtra(UPLMNEditor.UPLMN_SIZE, this.mUPLMNList.size());
            startActivityForResult(intent, 101);
        }
        return super.onOptionsItemSelected(item);
    }

    private void showReadingDialog() {
        if (this.mIsForeground && this.mIccFileHandler != null) {
            showDialog(99);
        }
    }

    private void showSavingDialog() {
        if (this.mIsForeground) {
            showDialog(100);
        }
    }

    private void dismissDialogSafely(int id) {
        try {
            dismissDialog(id);
        } catch (IllegalArgumentException e) {
        }
    }

    public void onFinished(Preference preference, boolean reading) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onFinished reading: ");
        stringBuilder.append(reading);
        log(stringBuilder.toString());
        if (reading) {
            dismissDialogSafely(99);
        } else {
            dismissDialogSafely(100);
        }
        preference.setEnabled(DBG);
        setScreenEnabled();
    }

    private void getUPLMNInfoFromEf() {
        log("UPLMNInfoFromEf Start read...");
        if (this.mIccFileHandler != null) {
            readEfFromIcc(this.mIccFileHandler, 28512);
        } else {
            Log.w(LOG_TAG, "mIccFileHandler is null");
        }
    }

    private void readEfFromIcc(IccFileHandler mfh, int efid) {
        mfh.loadEFTransparent(efid, this.mHandler.obtainMessage(1));
    }

    private void writeEfToIcc(IccFileHandler mfh, byte[] efdata, int efid) {
        mfh.updateEFTransparent(efid, efdata, this.mHandler.obtainMessage(2));
    }

    private void refreshUPLMNListPreference(ArrayList<UPLMNInfoWithEf> list) {
        if (this.mUPLMNListContainer.getPreferenceCount() != 0) {
            this.mUPLMNListContainer.removeAll();
        }
        if (this.mPreferenceMap != null) {
            this.mPreferenceMap.clear();
        }
        if (this.mUPLMNList != null) {
            this.mUPLMNList.clear();
        }
        this.mUPLMNList = list;
        if (list == null) {
            log("refreshUPLMNListPreference : NULL UPLMN list!");
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("refreshUPLMNListPreference : list.size()");
            stringBuilder.append(list.size());
            log(stringBuilder.toString());
        }
        if (list == null || list.size() == 0) {
            log("refreshUPLMNListPreference : NULL UPLMN list!");
            if (list == null) {
                this.mUPLMNList = new ArrayList();
            }
            return;
        }
        Iterator it = list.iterator();
        while (it.hasNext()) {
            UPLMNInfoWithEf network = (UPLMNInfoWithEf) it.next();
            addUPLMNPreference(network);
            log(network.toString());
        }
    }

    private void addUPLMNPreference(UPLMNInfoWithEf network) {
        Preference pref = new Preference(this);
        String plmnName = network.getOperatorNumeric();
        String extendName = getNetworkModeString(network.getNetworMode(), plmnName);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(plmnName);
        stringBuilder.append("(");
        stringBuilder.append(extendName);
        stringBuilder.append(")");
        pref.setTitle(stringBuilder.toString());
        this.mUPLMNListContainer.addPreference(pref);
        this.mPreferenceMap.put(pref, network);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Intent intent = new Intent(this, UPLMNEditor.class);
        UPLMNInfoWithEf info = (UPLMNInfoWithEf) this.mPreferenceMap.get(preference);
        this.mOldInfo = info;
        intent.putExtra(UPLMNEditor.UPLMN_CODE, info.getOperatorNumeric());
        intent.putExtra(UPLMNEditor.UPLMN_PRIORITY, info.getPriority());
        intent.putExtra(UPLMNEditor.UPLMN_SERVICE, info.getNetworMode());
        intent.putExtra(UPLMNEditor.UPLMN_ADD, false);
        intent.putExtra(UPLMNEditor.UPLMN_SIZE, this.mUPLMNList.size());
        startActivityForResult(intent, 102);
        return DBG;
    }

    /* Access modifiers changed, original: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("resultCode = ");
        stringBuilder.append(resultCode);
        stringBuilder.append(", requestCode = ");
        stringBuilder.append(requestCode);
        log(stringBuilder.toString());
        if (intent != null) {
            UPLMNInfoWithEf newInfo = createNetworkInfofromIntent(intent);
            if (resultCode == 102) {
                handleSetUPLMN(handleDeleteList(this.mOldInfo));
            } else if (resultCode != 101) {
            } else {
                if (requestCode == 101) {
                    handleAddList(newInfo);
                } else if (requestCode == 102) {
                    handleSetUPLMN(handleModifiedList(newInfo, this.mOldInfo));
                }
            }
        }
    }

    private UPLMNInfoWithEf createNetworkInfofromIntent(Intent intent) {
        return new UPLMNInfoWithEf(intent.getStringExtra(UPLMNEditor.UPLMN_CODE), intent.getIntExtra(UPLMNEditor.UPLMN_SERVICE, 0), intent.getIntExtra(UPLMNEditor.UPLMN_PRIORITY, 0));
    }

    public static byte[] stringToBcdPlmn(String str) {
        if (str.length() == 5) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append("f");
            str = stringBuilder.toString();
        }
        byte[] trans = IccUtils.hexStringToBytes(str);
        byte[] data = new byte[3];
        data[0] = (byte) ((trans[0] >> 4) | ((trans[0] << 4) & 240));
        data[1] = (byte) ((trans[1] >> 4) | ((trans[2] << 4) & 240));
        data[2] = (byte) ((trans[1] & 15) | (trans[2] & 240));
        return data;
    }

    private void handleSetUPLMN(ArrayList<UPLMNInfoWithEf> list) {
        int i;
        showSavingDialog();
        byte[] data = new byte[(this.mNumRec * 5)];
        byte[] mccmnc = new byte[6];
        for (i = 0; i < this.mNumRec; i++) {
            data[i * 5] = (byte) -1;
            data[(i * 5) + 1] = (byte) -1;
            data[(i * 5) + 2] = (byte) -1;
            data[(i * 5) + 3] = (byte) 0;
            data[(i * 5) + 4] = (byte) 0;
        }
        i = 0;
        while (i < list.size() && i < this.mNumRec) {
            UPLMNInfoWithEf ni = (UPLMNInfoWithEf) list.get(i);
            String strOperNumeric = ni.getOperatorNumeric();
            if (strOperNumeric == null) {
                break;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("strOperNumeric = ");
            stringBuilder.append(strOperNumeric);
            log(stringBuilder.toString());
            System.arraycopy(stringToBcdPlmn(strOperNumeric), 0, data, i * 5, 3);
            stringBuilder = new StringBuilder();
            stringBuilder.append("data[0] = ");
            stringBuilder.append(data[i * 5]);
            log(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("data[1] = ");
            stringBuilder.append(data[(i * 5) + 1]);
            log(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("data[2] = ");
            stringBuilder.append(data[(i * 5) + 2]);
            log(stringBuilder.toString());
            int accessTech = convertNetworkMode2AccessTech(ni.getNetworMode());
            data[(i * 5) + 3] = (byte) (accessTech >> 8);
            data[(i * 5) + 4] = (byte) (accessTech & 255);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("accessTech = ");
            stringBuilder2.append(accessTech);
            log(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("data[3] = ");
            stringBuilder2.append(data[(i * 5) + 3]);
            log(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("data[4] = ");
            stringBuilder2.append(data[(i * 5) + 4]);
            log(stringBuilder2.toString());
            i++;
        }
        log("update EFuplmn Start.");
        if (this.mIccFileHandler != null) {
            writeEfToIcc(this.mIccFileHandler, data, 28512);
        }
    }

    private void handleAddList(UPLMNInfoWithEf newInfo) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleAddList: add new network: ");
        stringBuilder.append(newInfo);
        log(stringBuilder.toString());
        dumpUPLMNInfo(this.mUPLMNList);
        ArrayList<UPLMNInfoWithEf> list = new ArrayList();
        for (int i = 0; i < this.mUPLMNList.size(); i++) {
            list.add((UPLMNInfoWithEf) this.mUPLMNList.get(i));
        }
        int position = Collections.binarySearch(this.mUPLMNList, newInfo, new PriorityCompare());
        if (position >= 0) {
            list.add(position, newInfo);
        } else {
            list.add(newInfo);
        }
        updateListPriority(list);
        dumpUPLMNInfo(list);
        handleSetUPLMN(list);
    }

    private void dumpUPLMNInfo(List<UPLMNInfoWithEf> list) {
        for (int i = 0; i < list.size(); i++) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("dumpUPLMNInfo : ");
            stringBuilder.append(((UPLMNInfoWithEf) list.get(i)).toString());
            log(stringBuilder.toString());
        }
    }

    private ArrayList<UPLMNInfoWithEf> handleModifiedList(UPLMNInfoWithEf newInfo, UPLMNInfoWithEf oldInfo) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleModifiedList: change old info: ");
        stringBuilder.append(oldInfo.toString());
        stringBuilder.append(" new info: ");
        stringBuilder.append(newInfo.toString());
        log(stringBuilder.toString());
        dumpUPLMNInfo(this.mUPLMNList);
        PriorityCompare pc = new PriorityCompare();
        int oldposition = Collections.binarySearch(this.mUPLMNList, oldInfo, pc);
        int newposition = Collections.binarySearch(this.mUPLMNList, newInfo, pc);
        ArrayList<UPLMNInfoWithEf> list = new ArrayList();
        for (int i = 0; i < this.mUPLMNList.size(); i++) {
            list.add((UPLMNInfoWithEf) this.mUPLMNList.get(i));
        }
        if (oldposition > newposition) {
            list.remove(oldposition);
            list.add(newposition, newInfo);
        } else if (oldposition < newposition) {
            list.add(newposition + 1, newInfo);
            list.remove(oldposition);
        } else {
            list.remove(oldposition);
            list.add(oldposition, newInfo);
        }
        updateListPriority(list);
        dumpUPLMNInfo(list);
        return list;
    }

    private void updateListPriority(ArrayList<UPLMNInfoWithEf> list) {
        int priority = 0;
        Iterator it = list.iterator();
        while (it.hasNext()) {
            int priority2 = priority + 1;
            ((UPLMNInfoWithEf) it.next()).setPriority(priority);
            priority = priority2;
        }
    }

    private ArrayList<UPLMNInfoWithEf> handleDeleteList(UPLMNInfoWithEf network) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleDeleteList : ");
        stringBuilder.append(network.toString());
        log(stringBuilder.toString());
        dumpUPLMNInfo(this.mUPLMNList);
        ArrayList<UPLMNInfoWithEf> list = new ArrayList();
        int position = Collections.binarySearch(this.mUPLMNList, network, new PriorityCompare());
        for (int i = 0; i < this.mUPLMNList.size(); i++) {
            list.add((UPLMNInfoWithEf) this.mUPLMNList.get(i));
        }
        list.remove(position);
        network.setOperatorNumeric(null);
        list.add(network);
        updateListPriority(list);
        dumpUPLMNInfo(list);
        return list;
    }

    private int convertAccessTech2NetworkMode(int accessTechs) {
        int networkMode = 0;
        if ((accessTechs & 16384) != 0) {
            networkMode = 0 | 8;
        }
        if ((32768 & accessTechs) != 0) {
            networkMode |= 4;
        }
        if ((accessTechs & 128) != 0) {
            networkMode |= 1;
        }
        if ((accessTechs & 64) != 0) {
            return networkMode | 2;
        }
        return networkMode;
    }

    private int convertNetworkMode2AccessTech(int networkMode) {
        int accessTechs = 0;
        if ((networkMode & 8) != 0) {
            accessTechs = 0 | 16384;
        }
        if ((networkMode & 4) != 0) {
            accessTechs |= 32768;
        }
        if ((networkMode & 1) != 0) {
            accessTechs |= 128;
        }
        if ((networkMode & 2) != 0) {
            return accessTechs | 64;
        }
        return accessTechs;
    }

    private String getNetworkModeString(int EFNWMode, String plmn) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("plmn = ");
        stringBuilder.append(plmn);
        Log.d(str, stringBuilder.toString());
        int index = UPLMNEditor.convertEFMode2Ap(EFNWMode);
        for (String CuPlmn : getResources().getStringArray(R.array.uplmn_cu_mcc_mnc_values)) {
            if (plmn.equals(CuPlmn)) {
                return getResources().getStringArray(R.array.uplmn_prefer_network_mode_w_choices)[index];
            }
        }
        return getResources().getStringArray(R.array.uplmn_prefer_network_mode_td_choices)[index];
    }

    private void setScreenEnabled() {
        getPreferenceScreen().setEnabled(this.mAirplaneModeOn ^ 1);
        invalidateOptionsMenu();
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
