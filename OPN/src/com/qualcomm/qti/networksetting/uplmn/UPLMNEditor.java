package com.qualcomm.qti.networksetting.uplmn;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings.System;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import com.qualcomm.qti.networksetting.R;

public class UPLMNEditor extends PreferenceActivity implements OnPreferenceChangeListener, TextWatcher {
    private static final String BUTTON_NETWORK_ID_KEY = "network_id_key";
    private static final String BUTTON_NEWWORK_MODE_KEY = "network_mode_key";
    private static final String BUTTON_PRIORITY_KEY = "priority_key";
    private static final int GSM = 0;
    private static final String LOG_TAG = "UPLMNEditor";
    private static final int LTE = 2;
    private static final int MENU_CANCEL_OPTION = 3;
    private static final int MENU_DELETE_OPTION = 1;
    private static final int MENU_SAVE_OPTION = 2;
    private static final int MODE_2G = 1;
    private static final int MODE_3G = 4;
    private static final int MODE_LTE = 8;
    private static final int MODE_TRIPLE = 13;
    private static final int NWID_DIALOG_ID = 0;
    public static final int RESULT_CODE_DELETE = 102;
    public static final int RESULT_CODE_EDIT = 101;
    private static final int TRIPLE_MODE = 3;
    public static final String UPLMN_ADD = "uplmn_add";
    public static final String UPLMN_CODE = "uplmn_code";
    public static final String UPLMN_PRIORITY = "uplmn_priority";
    public static final String UPLMN_SERVICE = "uplmn_service";
    public static final String UPLMN_SIZE = "uplmn_size";
    private static final int WCDMA_TDSCDMA = 1;
    private boolean mAirplaneModeOn = false;
    private IntentFilter mIntentFilter;
    private AlertDialog mNWIDDialog = null;
    private Preference mNWIDPref = null;
    private OnClickListener mNWIDPrefListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            if (which == -1) {
                String summary = UPLMNEditor.this.genText(UPLMNEditor.this.mNWIDText.getText().toString());
                String str = UPLMNEditor.LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("input network id is ");
                stringBuilder.append(summary);
                Log.d(str, stringBuilder.toString());
                UPLMNEditor.this.mNWIDPref.setSummary(summary);
            }
        }
    };
    private EditText mNWIDText;
    private ListPreference mNWMPref = null;
    private String mNoSet = null;
    private EditTextPreference mPRIpref = null;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                UPLMNEditor.this.mAirplaneModeOn = intent.getBooleanExtra("state", false);
                UPLMNEditor.this.setScreenEnabled();
            }
        }
    };

    /* Access modifiers changed, original: protected */
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.uplmn_editor);
        this.mNoSet = getResources().getString(R.string.voicemail_number_not_set);
        this.mNWIDPref = findPreference(BUTTON_NETWORK_ID_KEY);
        this.mPRIpref = (EditTextPreference) findPreference(BUTTON_PRIORITY_KEY);
        this.mNWMPref = (ListPreference) findPreference(BUTTON_NEWWORK_MODE_KEY);
        this.mPRIpref.setOnPreferenceChangeListener(this);
        this.mNWMPref.setOnPreferenceChangeListener(this);
        this.mIntentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        registerReceiver(this.mReceiver, this.mIntentFilter);
    }

    /* Access modifiers changed, original: protected */
    public void onResume() {
        super.onResume();
        displayNetworkInfo(getIntent());
        boolean z = true;
        if (System.getInt(getContentResolver(), "airplane_mode_on", 0) != 1) {
            z = false;
        }
        this.mAirplaneModeOn = z;
        setScreenEnabled();
    }

    /* Access modifiers changed, original: protected */
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mReceiver);
    }

    public boolean onPreferenceChange(Preference preference, Object object) {
        String value = object.toString();
        if (preference == this.mPRIpref) {
            this.mPRIpref.setSummary(genText(value));
        } else if (preference == this.mNWMPref) {
            this.mNWMPref.setValue(value);
            String summary = "";
            this.mNWMPref.setSummary(getResources().getStringArray(selectNetworkChoices(this.mNWIDPref.getSummary().toString()))[Integer.parseInt(value)]);
        }
        return true;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (!getIntent().getBooleanExtra(UPLMN_ADD, false)) {
            menu.add(0, 1, 0, 17039809);
        }
        menu.add(0, 2, 0, R.string.save);
        menu.add(0, 3, 0, 17039360);
        return true;
    }

    public boolean onMenuOpened(int featureId, Menu menu) {
        super.onMenuOpened(featureId, menu);
        boolean z = false;
        boolean isEmpty = this.mNoSet.equals(this.mNWIDPref.getSummary()) || this.mNoSet.equals(this.mPRIpref.getSummary());
        if (menu != null) {
            menu.setGroupEnabled(0, this.mAirplaneModeOn ^ 1);
            MenuItem item;
            if (getIntent().getBooleanExtra(UPLMN_ADD, true)) {
                item = menu.getItem(0);
                if (!(this.mAirplaneModeOn || isEmpty)) {
                    z = true;
                }
                item.setEnabled(z);
            } else {
                item = menu.getItem(1);
                if (!(this.mAirplaneModeOn || isEmpty)) {
                    z = true;
                }
                item.setEnabled(z);
            }
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId != 16908332) {
            switch (itemId) {
                case 1:
                    setRemovedNWInfo();
                    break;
                case 2:
                    setSavedNWInfo();
                    break;
            }
            finish();
            return super.onOptionsItemSelected(item);
        }
        finish();
        return true;
    }

    private void setSavedNWInfo() {
        Intent intent = new Intent(this, UserPLMNListPreference.class);
        genNWInfoToIntent(intent);
        setResult(101, intent);
    }

    private void genNWInfoToIntent(Intent intent) {
        int priority = 0;
        int plmnListSize = getIntent().getIntExtra(UPLMN_SIZE, 0);
        try {
            priority = Integer.parseInt(String.valueOf(this.mPRIpref.getSummary()));
        } catch (NumberFormatException e) {
            Log.d(LOG_TAG, "parse value of basband error");
        }
        if (getIntent().getBooleanExtra(UPLMN_ADD, false)) {
            if (priority > plmnListSize) {
                priority = plmnListSize;
            }
        } else if (priority >= plmnListSize) {
            priority = plmnListSize - 1;
        }
        intent.putExtra(UPLMN_PRIORITY, priority);
        try {
            intent.putExtra(UPLMN_SERVICE, convertApMode2EF(Integer.parseInt(String.valueOf(this.mNWMPref.getValue()))));
        } catch (NumberFormatException e2) {
            intent.putExtra(UPLMN_SERVICE, convertApMode2EF(0));
        }
        intent.putExtra(UPLMN_CODE, this.mNWIDPref.getSummary());
    }

    private void setRemovedNWInfo() {
        Intent intent = new Intent(this, UserPLMNListPreference.class);
        genNWInfoToIntent(intent);
        setResult(102, intent);
    }

    public static int convertEFMode2Ap(int mode) {
        if (mode == 13) {
            return 3;
        }
        if (mode == 4) {
            return 1;
        }
        if (mode == 8) {
            return 2;
        }
        return 0;
    }

    public static int convertApMode2EF(int mode) {
        if (mode == 3) {
            return 13;
        }
        if (mode == 2) {
            return 8;
        }
        if (mode == 1) {
            return 4;
        }
        return 1;
    }

    private void displayNetworkInfo(Intent intent) {
        String number = intent.getStringExtra(UPLMN_CODE);
        this.mNWIDPref.setSummary(genText(number));
        int priority = intent.getIntExtra(UPLMN_PRIORITY, 0);
        this.mPRIpref.setSummary(String.valueOf(priority));
        this.mPRIpref.setText(String.valueOf(priority));
        int act = intent.getIntExtra(UPLMN_SERVICE, 0);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("act = ");
        stringBuilder.append(act);
        Log.d(str, stringBuilder.toString());
        act = convertEFMode2Ap(act);
        if (act < 0 || act > 3) {
            act = 0;
        }
        str = "";
        this.mNWMPref.setEntries(getResources().getTextArray(selectNetworkChoices(number)));
        this.mNWMPref.setSummary(getResources().getStringArray(selectNetworkChoices(number))[act]);
        this.mNWMPref.setValue(String.valueOf(act));
    }

    public int selectNetworkChoices(String plmn) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("plmn = ");
        stringBuilder.append(plmn);
        Log.d(str, stringBuilder.toString());
        for (String CuPlmn : getResources().getStringArray(R.array.uplmn_cu_mcc_mnc_values)) {
            if (plmn.equals(CuPlmn)) {
                return R.array.uplmn_prefer_network_mode_w_choices;
            }
        }
        return R.array.uplmn_prefer_network_mode_td_choices;
    }

    private String genText(String value) {
        if (value == null || value.length() == 0) {
            return this.mNoSet;
        }
        return value;
    }

    public void buttonEnabled() {
        int len = this.mNWIDText.getText().toString().length();
        boolean state = true;
        if (len < 5 || len > 6) {
            state = false;
        }
        if (this.mNWIDDialog != null) {
            this.mNWIDDialog.getButton(-1).setEnabled(state);
        }
    }

    private void setScreenEnabled() {
        getPreferenceScreen().setEnabled(this.mAirplaneModeOn ^ 1);
        invalidateOptionsMenu();
    }

    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (preference == this.mNWIDPref) {
            removeDialog(0);
            showDialog(0);
            buttonEnabled();
        }
        return super.onPreferenceTreeClick(screen, preference);
    }

    public Dialog onCreateDialog(int id) {
        if (id != 0) {
            return null;
        }
        this.mNWIDText = new EditText(this);
        if (!this.mNoSet.equals(this.mNWIDPref.getSummary())) {
            this.mNWIDText.setText(this.mNWIDPref.getSummary());
        }
        this.mNWIDText.addTextChangedListener(this);
        this.mNWIDText.setInputType(2);
        this.mNWIDDialog = new Builder(this).setTitle(getResources().getString(R.string.network_id)).setView(this.mNWIDText).setPositiveButton(getResources().getString(17039370), this.mNWIDPrefListener).setNegativeButton(getResources().getString(17039360), null).create();
        this.mNWIDDialog.getWindow().setSoftInputMode(4);
        return this.mNWIDDialog;
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void afterTextChanged(Editable s) {
        buttonEnabled();
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }
}
