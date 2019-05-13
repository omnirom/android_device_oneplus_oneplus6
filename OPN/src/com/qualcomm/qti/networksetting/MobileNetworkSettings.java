package com.qualcomm.qti.networksetting;

import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import com.android.ims.ImsManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.uicc.UiccController;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.codeaurora.internal.IExtTelephony;
import org.codeaurora.internal.IExtTelephony.Stub;

public class MobileNetworkSettings extends PreferenceActivity implements OnClickListener, OnDismissListener, OnPreferenceChangeListener {
    private static final String BUTTON_4G_LTE_KEY = "enhanced_4g_lte";
    private static final String BUTTON_APN_EXPAND_KEY = "button_apn_key";
    private static final String BUTTON_CARRIER_SETTINGS_KEY = "carrier_settings_key";
    private static final String BUTTON_CDMA_LTE_DATA_SERVICE_KEY = "cdma_lte_data_service_key";
    private static final String BUTTON_CDMA_SYSTEM_SELECT_KEY = "cdma_system_select_key";
    private static final String BUTTON_CELL_BROADCAST_SETTINGS = "cell_broadcast_settings";
    private static final String BUTTON_ENABLED_NETWORKS_KEY = "enabled_networks_key";
    private static final String BUTTON_ENABLE_4G_KEY = "enable_4g_settings";
    private static final String BUTTON_OPERATOR_SELECTION_EXPAND_KEY = "button_carrier_sel_key";
    private static final String BUTTON_PREFERED_NETWORK_CMCC_MODE = "preferred_network_mode_cmcc_key";
    private static final String BUTTON_PREFERED_NETWORK_MODE = "preferred_network_mode_key";
    private static final String BUTTON_ROAMING_KEY = "button_roaming_key";
    private static final String BUTTON_UPLMN_KEY = "button_uplmn_key";
    private static final String CARRIER_MODE_CMCC = "cmcc";
    private static final String CARRIER_MODE_CT_CLASS_A = "ct_class_a";
    private static final String CARRIER_MODE_CT_CLASS_C = "ct_class_c";
    private static final String CONFIG_CURRENT_PRIMARY_SUB = "config_current_primary_sub";
    private static final boolean DBG = true;
    public static final String EXTRA_EXIT_ECM_RESULT = "exit_ecm_result";
    private static final String LOG_TAG = "MobileNetworkSettings";
    private static final int NOT_PROVISIONED = 0;
    private static final int PERMANENTLY_UNLOCKED = 100;
    private static final String PRIMARY_4G_CARD_PROPERTY_NAME = "persist.radio.detect4gcard";
    private static final String PRIMARY_CARD_PROPERTY_NAME = "persist.radio.primarycard";
    private static final boolean PRIMCARYCARD_L_W_ENABLED = SystemProperties.getBoolean("persist.radio.lw_enabled", PRIMCARYCARD_L_W_ENABLED);
    public static final int REQUEST_CODE_EXIT_ECM = 17;
    private static final int SUBSIDYLOCK_UNLOCKED = 103;
    private static final String SUBSIDY_LOCK_SYSTEM_PROPERY = "ro.radio.subsidylock";
    private static final String SUBSIDY_STATUS = "subsidy_status";
    private static final int TAB_THRESHOLD = 2;
    private static final String UP_ACTIVITY_CLASS = "com.android.settings.Settings$WirelessSettingsActivity";
    private static final String UP_ACTIVITY_PACKAGE = "com.android.settings";
    private static final String iface = "rmnet0";
    int[] callState = new int[TelephonyManager.getDefault().getPhoneCount()];
    private List<SubscriptionInfo> mActiveSubInfos;
    private SwitchPreference mButton4glte;
    private RestrictedSwitchPreference mButtonDataRoam;
    private SwitchPreference mButtonEnable4g;
    private ListPreference mButtonEnabledNetworks;
    private ListPreference mButtonPreferredCMCCNetworkMode;
    private ListPreference mButtonPreferredNetworkMode;
    private Preference mButtonUplmnPref;
    private String mCarrierMode = SystemProperties.get("persist.radio.carrier_mode", "default");
    CdmaOptions mCdmaOptions;
    private Preference mClickedPreference;
    private CarrierConfigManager mConfigManager;
    private TabContentFactory mEmptyTabContent = new TabContentFactory() {
        public View createTabContent(String tag) {
            return new View(MobileNetworkSettings.this.mTabHost.getContext());
        }
    };
    private IExtTelephony mExtTelephony = Stub.asInterface(ServiceManager.getService("extphone"));
    GsmUmtsOptions mGsmUmtsOptions;
    private MyHandler mHandler;
    private ImsManager mImsMgr;
    private boolean mIsCMCC = this.mCarrierMode.equals(CARRIER_MODE_CMCC);
    private boolean mIsCTClassC = this.mCarrierMode.equals(CARRIER_MODE_CT_CLASS_C);
    private boolean mIsGlobalCdma;
    private Preference mLteDataServicePref;
    private boolean mOkClicked;
    private final OnSubscriptionsChangedListener mOnSubscriptionsChangeListener = new OnSubscriptionsChangedListener() {
        public void onSubscriptionsChanged() {
            MobileNetworkSettings.log("onSubscriptionsChanged:");
            List<SubscriptionInfo> newSil = MobileNetworkSettings.this.mSubscriptionManager.getActiveSubscriptionInfoList();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSubscriptionsChanged: newSil: ");
            stringBuilder.append(newSil);
            stringBuilder.append(" mActiveSubInfos: ");
            stringBuilder.append(MobileNetworkSettings.this.mActiveSubInfos);
            MobileNetworkSettings.log(stringBuilder.toString());
            if (newSil == null) {
                MobileNetworkSettings.this.initializeSubscriptions();
                return;
            }
            if (MobileNetworkSettings.this.mActiveSubInfos == null || MobileNetworkSettings.this.mActiveSubInfos.size() != newSil.size()) {
                MobileNetworkSettings.this.initializeSubscriptions();
            } else {
                boolean subsChanged = MobileNetworkSettings.PRIMCARYCARD_L_W_ENABLED;
                loop0:
                for (SubscriptionInfo si : MobileNetworkSettings.this.mActiveSubInfos) {
                    for (SubscriptionInfo newSi : newSil) {
                        if (si.getSimSlotIndex() == newSi.getSimSlotIndex()) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("onSubscriptionsChanged: Slot matched SimSlotIndex: ");
                            stringBuilder2.append(si.getSimSlotIndex());
                            MobileNetworkSettings.log(stringBuilder2.toString());
                            if (!newSi.getDisplayName().equals(si.getDisplayName()) || newSi.getSubscriptionId() != si.getSubscriptionId()) {
                                MobileNetworkSettings.log("onSubscriptionsChanged: subs changed ");
                                subsChanged = MobileNetworkSettings.DBG;
                                break loop0;
                            }
                        }
                    }
                }
                if (subsChanged) {
                    MobileNetworkSettings.this.initializeSubscriptions();
                }
            }
        }
    };
    private boolean mOpSimShow = DBG;
    private Phone mPhone;
    private final BroadcastReceiver mPhoneChangeReceiver = new PhoneChangeReceiver(this, null);
    private boolean mShow4GForLTE;
    private SubscriptionManager mSubscriptionManager;
    private Handler mSubsidySettingsHandler = new Handler() {
        public void handleMessage(Message msg) {
            MobileNetworkSettings.this.updateBody();
        }
    };
    private SubsidySettingsObserver mSubsidySettingsObserver;
    private TabHost mTabHost;
    private OnTabChangeListener mTabListener = new OnTabChangeListener() {
        public void onTabChanged(String tabId) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onTabChanged: ");
            stringBuilder.append(tabId);
            MobileNetworkSettings.log(stringBuilder.toString());
            MobileNetworkSettings.this.updatePhone(Integer.parseInt(tabId));
            MobileNetworkSettings.this.updateBody();
        }
    };
    private UserManager mUm;
    private boolean mUnavailable;
    private int preferredNetworkMode = Phone.PREFERRED_NT_MODE;
    private String tabDefaultLabel = "SIM slot ";

    private class MyHandler extends Handler {
        static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 0;

        private MyHandler() {
        }

        /* synthetic */ MyHandler(MobileNetworkSettings x0, AnonymousClass1 x1) {
            this();
        }

        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                handleSetPreferredNetworkTypeResponse(msg);
            }
        }

        private void handleSetPreferredNetworkTypeResponse(Message msg) {
            if (!MobileNetworkSettings.this.isDestroyed()) {
                if (msg.obj.exception == null) {
                    MobileNetworkSettings.log("handleSetPreferredNetworkTypeResponse: Sucess");
                    if (MobileNetworkSettings.this.getPreferenceScreen().findPreference(MobileNetworkSettings.BUTTON_PREFERED_NETWORK_MODE) != null) {
                        MobileNetworkSettings.this.setPreferredNetworkMode(Integer.parseInt(MobileNetworkSettings.this.mButtonPreferredNetworkMode.getValue()));
                    }
                    if (MobileNetworkSettings.this.getPreferenceScreen().findPreference(MobileNetworkSettings.BUTTON_ENABLED_NETWORKS_KEY) != null) {
                        MobileNetworkSettings.this.setPreferredNetworkMode(Integer.parseInt(MobileNetworkSettings.this.mButtonEnabledNetworks.getValue()));
                    }
                    MobileNetworkSettings.this.updateButtonEnable4g();
                } else {
                    MobileNetworkSettings.log("handleSetPreferredNetworkTypeResponse: exception in setting network mode.");
                    MobileNetworkSettings.this.updatePreferredNetworkUIFromDb();
                }
            }
        }
    }

    private class PhoneChangeReceiver extends BroadcastReceiver {
        private PhoneChangeReceiver() {
        }

        /* synthetic */ PhoneChangeReceiver(MobileNetworkSettings x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.RADIO_TECHNOLOGY")) {
                int phoneId = intent.getIntExtra("phone", -1);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onReceive: phoneId: ");
                stringBuilder.append(phoneId);
                MobileNetworkSettings.log(stringBuilder.toString());
                if (MobileNetworkSettings.this.mPhone.getPhoneId() == phoneId) {
                    MobileNetworkSettings.this.mGsmUmtsOptions = null;
                    MobileNetworkSettings.this.mCdmaOptions = null;
                    MobileNetworkSettings.this.updateBody();
                }
            } else if (action.equals("android.intent.action.AIRPLANE_MODE") || action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                MobileNetworkSettings.this.setScreenState();
            }
        }
    }

    private class SubsidySettingsObserver extends ContentObserver {
        public SubsidySettingsObserver() {
            super(null);
        }

        public void onChange(boolean selfChange) {
            MobileNetworkSettings.this.mSubsidySettingsHandler.sendEmptyMessage(0);
        }
    }

    private enum TabState {
        NO_TABS,
        UPDATE,
        DO_NOTHING
    }

    private void listenForCallState(int subId, int listenStatus) {
        ((TelephonyManager) getSystemService("phone")).listen(new PhoneStateListener(Integer.valueOf(subId)) {
            public void onCallStateChanged(int state, String incomingNumber) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("PhoneStateListener.onCallStateChanged: state=");
                stringBuilder.append(state);
                stringBuilder.append(" SubId: ");
                stringBuilder.append(this.mSubId);
                MobileNetworkSettings.log(stringBuilder.toString());
                MobileNetworkSettings.this.callState[SubscriptionManager.getPhoneId(this.mSubId.intValue())] = state;
                boolean z = MobileNetworkSettings.PRIMCARYCARD_L_W_ENABLED;
                boolean enabled = MobileNetworkSettings.DBG;
                for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("callstate: ");
                    stringBuilder2.append(MobileNetworkSettings.this.callState[i]);
                    MobileNetworkSettings.log(stringBuilder2.toString());
                    enabled = MobileNetworkSettings.this.callState[i] == 0 ? true : MobileNetworkSettings.PRIMCARYCARD_L_W_ENABLED;
                    if (!enabled) {
                        break;
                    }
                }
                boolean enabled2 = (enabled && MobileNetworkSettings.this.mImsMgr.isNonTtyOrTtyOnVolteEnabled()) ? MobileNetworkSettings.DBG : MobileNetworkSettings.PRIMCARYCARD_L_W_ENABLED;
                Preference pref = MobileNetworkSettings.this.getPreferenceScreen().findPreference(MobileNetworkSettings.BUTTON_4G_LTE_KEY);
                if (pref != null) {
                    if (enabled2 && MobileNetworkSettings.this.hasActiveSubscriptions()) {
                        z = MobileNetworkSettings.DBG;
                    }
                    pref.setEnabled(z);
                }
            }
        }, listenStatus);
    }

    private boolean isDetect4gCardEnabled() {
        if (SystemProperties.getBoolean(PRIMARY_CARD_PROPERTY_NAME, PRIMCARYCARD_L_W_ENABLED) && SystemProperties.getBoolean(PRIMARY_4G_CARD_PROPERTY_NAME, PRIMCARYCARD_L_W_ENABLED)) {
            return DBG;
        }
        return PRIMCARYCARD_L_W_ENABLED;
    }

    private void setScreenState() {
        StringBuilder stringBuilder;
        if (this.mPhone != null) {
            int phoneId = this.mPhone.getPhoneId();
            int simState = TelephonyManager.getDefault().getSimState(phoneId);
            boolean z = PRIMCARYCARD_L_W_ENABLED;
            boolean screenState = simState != 1 ? DBG : PRIMCARYCARD_L_W_ENABLED;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("set sub screenState phoneId=");
            stringBuilder2.append(phoneId);
            stringBuilder2.append(", simState=");
            stringBuilder2.append(simState);
            log(stringBuilder2.toString());
            if (screenState && isDetect4gCardEnabled()) {
                int provStatus = 0;
                try {
                    provStatus = this.mExtTelephony.getCurrentUiccCardProvisioningStatus(phoneId);
                } catch (RemoteException ex) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("RemoteException @setScreenState=");
                    stringBuilder.append(ex);
                    stringBuilder.append(", phoneId=");
                    stringBuilder.append(phoneId);
                    loge(stringBuilder.toString());
                } catch (NullPointerException ex2) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("NullPointerException @setScreenState=");
                    stringBuilder.append(ex2);
                    stringBuilder.append(", phoneId=");
                    stringBuilder.append(phoneId);
                    loge(stringBuilder.toString());
                }
                if (provStatus != 0) {
                    z = true;
                }
                screenState = z;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("set sub screenState provStatus=");
                stringBuilder3.append(provStatus);
                stringBuilder3.append(", screenState=");
                stringBuilder3.append(screenState);
                log(stringBuilder3.toString());
            }
            getPreferenceScreen().setEnabled(screenState);
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == -1) {
            this.mPhone.setDataRoamingEnabled(DBG);
            this.mOkClicked = DBG;
            return;
        }
        this.mButtonDataRoam.setChecked(PRIMCARYCARD_L_W_ENABLED);
    }

    public void onDismiss(DialogInterface dialog) {
        this.mButtonDataRoam.setChecked(this.mOkClicked);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        int phoneSubId = this.mPhone.getSubId();
        if (preference.getKey().equals(BUTTON_4G_LTE_KEY) || preference.getKey().equals(BUTTON_ENABLE_4G_KEY)) {
            return DBG;
        }
        if (this.mGsmUmtsOptions != null && this.mGsmUmtsOptions.preferenceTreeClick(preference) == DBG) {
            return DBG;
        }
        String url = null;
        if (this.mCdmaOptions != null && this.mCdmaOptions.preferenceTreeClick(preference) == DBG) {
            if (Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
                this.mClickedPreference = preference;
                startActivityForResult(new Intent("com.android.internal.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS", null), 17);
            }
            return DBG;
        } else if (preference == this.mButtonPreferredNetworkMode) {
            this.mButtonPreferredNetworkMode.setValue(Integer.toString(getPreferredNetworkModeForSubId()));
            return DBG;
        } else if (preference == this.mButtonPreferredCMCCNetworkMode) {
            this.mButtonPreferredCMCCNetworkMode.setValue(Integer.toString(getPreferredNetworkModeForSubId()));
            return DBG;
        } else if (preference == this.mLteDataServicePref) {
            String tmpl = Global.getString(getContentResolver(), "setup_prepaid_data_service_url");
            if (TextUtils.isEmpty(tmpl)) {
                Log.e(LOG_TAG, "Missing SETUP_PREPAID_DATA_SERVICE_URL");
            } else {
                String imsi = ((TelephonyManager) getSystemService("phone")).getSubscriberId();
                if (imsi == null) {
                    imsi = "";
                }
                if (!TextUtils.isEmpty(tmpl)) {
                    url = TextUtils.expandTemplate(tmpl, new CharSequence[]{imsi}).toString();
                }
                startActivity(new Intent("android.intent.action.VIEW", Uri.parse(url)));
            }
            return DBG;
        } else if (preference == this.mButtonEnabledNetworks) {
            this.mButtonEnabledNetworks.setValue(Integer.toString(getPreferredNetworkModeForSubId()));
            return DBG;
        } else if (preference == this.mButtonDataRoam) {
            return DBG;
        } else {
            preferenceScreen.setEnabled(PRIMCARYCARD_L_W_ENABLED);
            return PRIMCARYCARD_L_W_ENABLED;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:47:0x011f A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x00e0 A:{SYNTHETIC, Splitter:B:36:0x00e0} */
    private void initializeSubscriptions() {
        /*
        r14 = this;
        r0 = r14.isDestroyed();
        if (r0 == 0) goto L_0x0007;
    L_0x0006:
        return;
    L_0x0007:
        r0 = 0;
        r1 = "initializeSubscriptions:+";
        log(r1);
        r1 = r14.mSubscriptionManager;
        r1 = r1.getActiveSubscriptionInfoList();
        r2 = "phone";
        r2 = r14.getSystemService(r2);
        r2 = (android.telephony.TelephonyManager) r2;
        r3 = r2.getPhoneCount();
        r4 = com.qualcomm.qti.networksetting.MobileNetworkSettings.TabState.UPDATE;
        r5 = 2;
        if (r3 >= r5) goto L_0x0028;
    L_0x0024:
        r4 = r14.isUpdateTabsNeeded(r1);
    L_0x0028:
        r5 = r14.mActiveSubInfos;
        r5.clear();
        r5 = 1;
        r6 = 0;
        if (r1 == 0) goto L_0x0046;
    L_0x0031:
        r7 = r14.mActiveSubInfos;
        r7.addAll(r1);
        r7 = r1.size();
        if (r7 != r5) goto L_0x0046;
    L_0x003c:
        r7 = r1.get(r6);
        r7 = (android.telephony.SubscriptionInfo) r7;
        r0 = r7.getSimSlotIndex();
    L_0x0046:
        r7 = com.qualcomm.qti.networksetting.MobileNetworkSettings.AnonymousClass6.$SwitchMap$com$qualcomm$qti$networksetting$MobileNetworkSettings$TabState;
        r8 = r4.ordinal();
        r7 = r7[r8];
        r8 = 17367117; // 0x109004d float:2.5163142E-38 double:8.580496E-317;
        switch(r7) {
            case 1: goto L_0x007d;
            case 2: goto L_0x0067;
            case 3: goto L_0x0056;
            default: goto L_0x0054;
        };
    L_0x0054:
        goto L_0x0159;
    L_0x0056:
        r5 = "initializeSubscriptions: DO_NOTHING";
        log(r5);
        r5 = r14.mTabHost;
        if (r5 == 0) goto L_0x0159;
    L_0x005f:
        r5 = r14.mTabHost;
        r0 = r5.getCurrentTab();
        goto L_0x0159;
    L_0x0067:
        r5 = "initializeSubscriptions: NO_TABS";
        log(r5);
        r5 = r14.mTabHost;
        if (r5 == 0) goto L_0x0078;
    L_0x0070:
        r5 = r14.mTabHost;
        r5.clearAllTabs();
        r5 = 0;
        r14.mTabHost = r5;
    L_0x0078:
        r14.setContentView(r8);
        goto L_0x0159;
    L_0x007d:
        r7 = "initializeSubscriptions: UPDATE";
        log(r7);
        r7 = r14.mTabHost;
        if (r7 == 0) goto L_0x008d;
    L_0x0086:
        r7 = r14.mTabHost;
        r7 = r7.getCurrentTab();
        goto L_0x008e;
    L_0x008d:
        r7 = r6;
    L_0x008e:
        r0 = r7;
        r14.setContentView(r8);
        r7 = 16908306; // 0x1020012 float:2.387728E-38 double:8.353813E-317;
        r7 = r14.findViewById(r7);
        r7 = (android.widget.TabHost) r7;
        r14.mTabHost = r7;
        r7 = r14.mTabHost;
        r7.setup();
        r7 = r6;
    L_0x00a3:
        if (r7 >= r3) goto L_0x014c;
    L_0x00a5:
        r8 = 0;
        r9 = r14.mActiveSubInfos;
        r9 = r9.iterator();
    L_0x00ac:
        r10 = r9.hasNext();
        if (r10 == 0) goto L_0x00de;
    L_0x00b2:
        r10 = r9.next();
        r10 = (android.telephony.SubscriptionInfo) r10;
        r11 = new java.lang.StringBuilder;
        r11.<init>();
        r12 = "initializeSubscriptions: si: ";
        r11.append(r12);
        r11.append(r10);
        r11 = r11.toString();
        log(r11);
        if (r10 == 0) goto L_0x00dd;
    L_0x00ce:
        r11 = r10.getSimSlotIndex();
        if (r11 != r7) goto L_0x00dd;
    L_0x00d4:
        r9 = r10.getDisplayName();
        r8 = java.lang.String.valueOf(r9);
        goto L_0x00de;
    L_0x00dd:
        goto L_0x00ac;
    L_0x00de:
        if (r8 != 0) goto L_0x011f;
    L_0x00e0:
        r9 = "com.android.settings";
        r9 = r14.createPackageContext(r9, r6);	 Catch:{ NameNotFoundException -> 0x0108 }
        r10 = r9.getResources();	 Catch:{ NameNotFoundException -> 0x0108 }
        r11 = "sim_editor_title";
        r12 = "string";
        r13 = "com.android.settings";
        r10 = r10.getIdentifier(r11, r12, r13);	 Catch:{ NameNotFoundException -> 0x0108 }
        r11 = r9.getResources();	 Catch:{ NameNotFoundException -> 0x0108 }
        r12 = new java.lang.Object[r5];	 Catch:{ NameNotFoundException -> 0x0108 }
        r13 = r7 + 1;
        r13 = java.lang.Integer.valueOf(r13);	 Catch:{ NameNotFoundException -> 0x0108 }
        r12[r6] = r13;	 Catch:{ NameNotFoundException -> 0x0108 }
        r11 = r11.getString(r10, r12);	 Catch:{ NameNotFoundException -> 0x0108 }
        r8 = r11;
        goto L_0x011f;
    L_0x0108:
        r9 = move-exception;
        r10 = "NameNotFoundException for sim_editor_title";
        loge(r10);
        r10 = new java.lang.StringBuilder;
        r10.<init>();
        r11 = r14.tabDefaultLabel;
        r10.append(r11);
        r10.append(r7);
        r8 = r10.toString();
    L_0x011f:
        r9 = new java.lang.StringBuilder;
        r9.<init>();
        r10 = "initializeSubscriptions: tab=";
        r9.append(r10);
        r9.append(r7);
        r10 = " name=";
        r9.append(r10);
        r9.append(r8);
        r9 = r9.toString();
        log(r9);
        r9 = r14.mTabHost;
        r10 = java.lang.String.valueOf(r7);
        r10 = r14.buildTabSpec(r10, r8);
        r9.addTab(r10);
        r7 = r7 + 1;
        goto L_0x00a3;
    L_0x014c:
        r5 = r14.mTabHost;
        r6 = r14.mTabListener;
        r5.setOnTabChangedListener(r6);
        r5 = r14.mTabHost;
        r5.setCurrentTab(r0);
    L_0x0159:
        r14.updatePhone(r0);
        r14.updateBody();
        r5 = "initializeSubscriptions:-";
        log(r5);
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.qualcomm.qti.networksetting.MobileNetworkSettings.initializeSubscriptions():void");
    }

    private TabState isUpdateTabsNeeded(List<SubscriptionInfo> newSil) {
        TabState state = TabState.DO_NOTHING;
        if (newSil == null) {
            if (this.mActiveSubInfos.size() >= 2) {
                log("isUpdateTabsNeeded: NO_TABS, size unknown and was tabbed");
                state = TabState.NO_TABS;
            }
        } else if (newSil.size() < 2 && this.mActiveSubInfos.size() >= 2) {
            log("isUpdateTabsNeeded: NO_TABS, size went to small");
            state = TabState.NO_TABS;
        } else if (newSil.size() >= 2 && this.mActiveSubInfos.size() < 2) {
            log("isUpdateTabsNeeded: UPDATE, size changed");
            state = TabState.UPDATE;
        } else if (newSil.size() >= 2) {
            Iterator<SubscriptionInfo> siIterator = this.mActiveSubInfos.iterator();
            for (SubscriptionInfo newSi : newSil) {
                if (!newSi.getDisplayName().equals(((SubscriptionInfo) siIterator.next()).getDisplayName())) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("isUpdateTabsNeeded: UPDATE, new name=");
                    stringBuilder.append(newSi.getDisplayName());
                    log(stringBuilder.toString());
                    state = TabState.UPDATE;
                    break;
                }
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("isUpdateTabsNeeded:- ");
        stringBuilder2.append(state);
        stringBuilder2.append(" newSil.size()=");
        stringBuilder2.append(newSil != null ? newSil.size() : 0);
        stringBuilder2.append(" mActiveSubInfos.size()=");
        stringBuilder2.append(this.mActiveSubInfos.size());
        log(stringBuilder2.toString());
        return state;
    }

    private void updatePhone(int slotId) {
        this.mPhone = PhoneFactory.getPhone(slotId);
        if (this.mPhone == null) {
            this.mPhone = PhoneFactory.getDefaultPhone();
        }
        this.preferredNetworkMode = getPreferredNetworkModeForPhoneId();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updatePhone:- slotId=");
        stringBuilder.append(slotId);
        log(stringBuilder.toString());
        this.mImsMgr = ImsManager.getInstance(this.mPhone.getContext(), this.mPhone.getPhoneId());
        if (this.mImsMgr == null) {
            log("updatePhone :: Could not get ImsManager instance!");
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("updatePhone :: mImsMgr=");
        stringBuilder.append(this.mImsMgr);
        log(stringBuilder.toString());
    }

    private TabSpec buildTabSpec(String tag, String title) {
        return this.mTabHost.newTabSpec(tag).setIndicator(title).setContent(this.mEmptyTabContent);
    }

    private Intent getOPSimIntent() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent("oneplus.intent.action.SIM_AND_NETWORK_SETTINGS");
        for (ResolveInfo resolveInfo : pm.queryIntentActivities(intent, null)) {
            if ((resolveInfo.activityInfo.applicationInfo.flags & 1) != 0) {
                return intent;
            }
        }
        return null;
    }

    /* Access modifiers changed, original: protected */
    public void onCreate(Bundle icicle) {
        log("onCreate:+");
        setTheme(16974387);
        super.onCreate(icicle);
        try {
            Intent intent = getOPSimIntent();
            if (intent != null) {
                startActivity(intent);
                this.mOpSimShow = DBG;
                finish();
                return;
            }
        } catch (ActivityNotFoundException e) {
            log("can not start OPSim");
        }
        this.mHandler = new MyHandler(this, null);
        this.mUm = (UserManager) getSystemService("user");
        this.mSubscriptionManager = SubscriptionManager.from(this);
        this.mConfigManager = (CarrierConfigManager) getSystemService("carrier_config");
        if (this.mUm.hasUserRestriction("no_config_mobile_networks")) {
            this.mUnavailable = DBG;
            setContentView(R.string.Chunghwa_Telecom);
            return;
        }
        addPreferencesFromResource(R.xml.network_setting);
        this.mButton4glte = (SwitchPreference) findPreference(BUTTON_4G_LTE_KEY);
        this.mButton4glte.setOnPreferenceChangeListener(this);
        this.mButtonEnable4g = (SwitchPreference) findPreference(BUTTON_ENABLE_4G_KEY);
        try {
            Context con = createPackageContext("com.android.systemui", 0);
            this.mShow4GForLTE = con.getResources().getBoolean(con.getResources().getIdentifier("config_show4GForLTE", "bool", "com.android.systemui"));
        } catch (NameNotFoundException e2) {
            loge("NameNotFoundException for show4GFotLTE");
            this.mShow4GForLTE = PRIMCARYCARD_L_W_ENABLED;
        }
        PreferenceScreen prefSet = getPreferenceScreen();
        this.mButtonDataRoam = (RestrictedSwitchPreference) prefSet.findPreference(BUTTON_ROAMING_KEY);
        this.mButtonPreferredNetworkMode = (ListPreference) prefSet.findPreference(BUTTON_PREFERED_NETWORK_MODE);
        this.mButtonEnabledNetworks = (ListPreference) prefSet.findPreference(BUTTON_ENABLED_NETWORKS_KEY);
        this.mButtonPreferredCMCCNetworkMode = (ListPreference) prefSet.findPreference(BUTTON_PREFERED_NETWORK_CMCC_MODE);
        this.mButtonDataRoam.setOnPreferenceChangeListener(this);
        this.mLteDataServicePref = prefSet.findPreference(BUTTON_CDMA_LTE_DATA_SERVICE_KEY);
        this.mButtonUplmnPref = prefSet.findPreference(BUTTON_UPLMN_KEY);
        this.mActiveSubInfos = new ArrayList(this.mSubscriptionManager.getActiveSubscriptionInfoCountMax());
        initializeSubscriptions();
        if (this.mIsCTClassC) {
            this.mButtonEnable4g.setOnPreferenceChangeListener(this);
            updateButtonEnable4g();
        } else {
            prefSet.removePreference(this.mButtonEnable4g);
            this.mButtonEnable4g = null;
        }
        IntentFilter intentFilter = new IntentFilter("android.intent.action.RADIO_TECHNOLOGY");
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        registerReceiver(this.mPhoneChangeReceiver, intentFilter);
        this.mOpSimShow = PRIMCARYCARD_L_W_ENABLED;
        log("onCreate:-");
        if (isSubsidyLockFeatureEnabled()) {
            this.mSubsidySettingsObserver = new SubsidySettingsObserver();
            getContentResolver().registerContentObserver(Secure.getUriFor(SUBSIDY_STATUS), PRIMCARYCARD_L_W_ENABLED, this.mSubsidySettingsObserver);
        }
    }

    private static boolean isSubsidyLockFeatureEnabled() {
        return SystemProperties.getInt(SUBSIDY_LOCK_SYSTEM_PROPERY, 0) == 1 ? DBG : PRIMCARYCARD_L_W_ENABLED;
    }

    private boolean checkForCtCard(String iccId) {
        int i = 0;
        if (iccId == null || iccId.length() < 6) {
            return PRIMCARYCARD_L_W_ENABLED;
        }
        boolean isCtCard = PRIMCARYCARD_L_W_ENABLED;
        String ctIccIdPrefix = iccId.substring(0, 6);
        String[] ctIccIdList = getResources().getStringArray(R.array.ct_iccid_prefix_list);
        if (ctIccIdList != null) {
            int length = ctIccIdList.length;
            while (i < length) {
                if (ctIccIdPrefix.equals(ctIccIdList[i])) {
                    isCtCard = DBG;
                    break;
                }
                i++;
            }
        }
        return isCtCard;
    }

    private void handleEnable4gChange() {
        int networkType;
        boolean isCtCard = PRIMCARYCARD_L_W_ENABLED;
        this.mButtonEnable4g.setEnabled(PRIMCARYCARD_L_W_ENABLED);
        int subId = this.mPhone.getSubId();
        List<SubscriptionInfo> sirList = SubscriptionManager.from(this.mPhone.getContext()).getActiveSubscriptionInfoList();
        if (sirList != null) {
            for (SubscriptionInfo sir : sirList) {
                if (sir != null && sir.getSimSlotIndex() >= 0 && sir.getSubscriptionId() == subId) {
                    isCtCard = checkForCtCard(sir.getIccId());
                    break;
                }
            }
        }
        int i;
        if (isCtCard) {
            if (this.mButtonEnable4g.isChecked()) {
                i = 10;
            } else {
                i = 7;
            }
            networkType = i;
        } else {
            if (this.mButtonEnable4g.isChecked()) {
                i = 20;
            } else {
                i = 18;
            }
            networkType = i;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Enable 4G option: isCtCard - ");
        stringBuilder.append(isCtCard);
        stringBuilder.append(", set networkType - ");
        stringBuilder.append(networkType);
        log(stringBuilder.toString());
        if (getPreferenceScreen().findPreference(BUTTON_PREFERED_NETWORK_MODE) != null) {
            this.mButtonPreferredNetworkMode.setValue(Integer.toString(networkType));
        }
        if (getPreferenceScreen().findPreference(BUTTON_ENABLED_NETWORKS_KEY) != null) {
            this.mButtonEnabledNetworks.setValue(Integer.toString(networkType));
        }
        this.mPhone.setPreferredNetworkType(networkType, this.mHandler.obtainMessage(0));
    }

    private boolean isNwModeLte() {
        int type = getPreferredNetworkModeForSubId();
        if (type == 22 || type == 20 || type == 19 || type == 17 || type == 15 || type == 12 || type == 11 || type == 10 || type == 9 || type == 8) {
            return DBG;
        }
        return PRIMCARYCARD_L_W_ENABLED;
    }

    private void updateButtonEnable4g() {
        if (this.mButtonEnable4g != null) {
            this.mButtonEnable4g.setChecked(isNwModeLte());
            int simState = TelephonyManager.getDefault().getSimState(this.mPhone.getPhoneId());
            int i = System.getInt(this.mPhone.getContext().getContentResolver(), "airplane_mode_on", 0);
            boolean z = DBG;
            boolean z2 = i == 0 && simState == 5;
            boolean enabled = z2;
            i = this.mPhone.getSubId();
            List<SubscriptionInfo> sirList = SubscriptionManager.from(this.mPhone.getContext()).getActiveSubscriptionInfoList();
            if (enabled && sirList != null) {
                for (SubscriptionInfo sir : sirList) {
                    if (sir != null && sir.getSimSlotIndex() >= 0 && sir.getSubscriptionId() == i) {
                        String iccId = sir.getIccId();
                        if (iccId == null || iccId.length() < 6) {
                            z = false;
                        }
                        enabled = z;
                    }
                }
            }
            this.mButtonEnable4g.setEnabled(enabled);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onDestroy() {
        super.onDestroy();
        if (this.mOpSimShow != DBG) {
            unregisterReceiver(this.mPhoneChangeReceiver);
        }
        if (this.mSubsidySettingsObserver != null) {
            getContentResolver().unregisterContentObserver(this.mSubsidySettingsObserver);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onResume() {
        super.onResume();
        log("onResume:+");
        if (this.mUnavailable) {
            log("onResume:- ignore mUnavailable == false");
            return;
        }
        int i = 0;
        updatePhone(this.mTabHost != null ? this.mTabHost.getCurrentTab() : 0);
        setScreenState();
        this.preferredNetworkMode = getPreferredNetworkModeForPhoneId();
        this.mButtonDataRoam.setChecked(this.mPhone.getDataRoamingEnabled());
        updateButtonEnable4g();
        if (!(getPreferenceScreen().findPreference(BUTTON_PREFERED_NETWORK_MODE) == null && getPreferenceScreen().findPreference(BUTTON_ENABLED_NETWORKS_KEY) == null)) {
            updatePreferredNetworkUIFromDb();
        }
        if (this.mImsMgr.isVolteEnabledByPlatform() && this.mImsMgr.isVolteProvisionedOnDevice()) {
            Phone[] phones = PhoneFactory.getPhones();
            int length = phones.length;
            while (i < length) {
                listenForCallState(phones[i].getSubId(), 32);
                i++;
            }
        }
        this.mButton4glte.setChecked(isEnhanced4gLteEnabled());
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangeListener);
        log("onResume:-");
    }

    private boolean isEnhanced4gLteEnabled() {
        return (this.mImsMgr.isEnhanced4gLteModeSettingEnabledByUser() && this.mImsMgr.isNonTtyOrTtyOnVolteEnabled()) ? DBG : PRIMCARYCARD_L_W_ENABLED;
    }

    private boolean isSubsidyUnlocked() {
        return Secure.getInt(getContentResolver(), SUBSIDY_STATUS, -1) == 103 ? DBG : PRIMCARYCARD_L_W_ENABLED;
    }

    private boolean isPermanentlyUnlocked() {
        return Secure.getInt(getContentResolver(), SUBSIDY_STATUS, -1) == 100 ? DBG : PRIMCARYCARD_L_W_ENABLED;
    }

    private boolean hasActiveSubscriptions() {
        boolean isActive = PRIMCARYCARD_L_W_ENABLED;
        int subId = this.mPhone.getSubId();
        for (SubscriptionInfo si : this.mActiveSubInfos) {
            if (si.getSubscriptionId() == subId) {
                isActive = DBG;
            }
        }
        return isActive;
    }

    private void updateBody() {
        int phoneType;
        Context context = getApplicationContext();
        PreferenceScreen prefSet = getPreferenceScreen();
        boolean isLteOnCdma = this.mPhone.getLteOnCdmaMode() == 1 ? DBG : PRIMCARYCARD_L_W_ENABLED;
        int phoneSubId = this.mPhone.getSubId();
        int phoneId = this.mPhone.getPhoneId();
        int currentPrimarySlot = Global.getInt(context.getContentResolver(), CONFIG_CURRENT_PRIMARY_SUB, -1);
        int settingsNetworkMode = getPreferredNetworkModeForSubId();
        PersistableBundle carrierConfig = this.mConfigManager.getConfigForSubId(phoneSubId);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateBody: isLteOnCdma= ");
        stringBuilder.append(isLteOnCdma);
        stringBuilder.append(" phoneSubId= ");
        stringBuilder.append(phoneSubId);
        stringBuilder.append(" phoneId = ");
        stringBuilder.append(phoneId);
        stringBuilder.append(" currentPrimarySlot= ");
        stringBuilder.append(currentPrimarySlot);
        stringBuilder.append(" NW mode is: ");
        stringBuilder.append(settingsNetworkMode);
        log(stringBuilder.toString());
        if (prefSet != null) {
            prefSet.removeAll();
            if (getResources().getBoolean(R.bool.config_uplmn_for_usim) || SystemProperties.getBoolean("persist.vendor.uplmn.enabled", PRIMCARYCARD_L_W_ENABLED)) {
                this.mButtonUplmnPref.getIntent().putExtra("phone", this.mPhone.getPhoneId());
                prefSet.addPreference(this.mButtonUplmnPref);
            }
            prefSet.addPreference(this.mButtonDataRoam);
            prefSet.addPreference(this.mButtonPreferredNetworkMode);
            prefSet.addPreference(this.mButtonEnabledNetworks);
            prefSet.addPreference(this.mButton4glte);
            this.mButton4glte.setChecked(isEnhanced4gLteEnabled());
            if (this.mIsCTClassC) {
                prefSet.addPreference(this.mButtonEnable4g);
                updateButtonEnable4g();
            } else if (this.mIsCMCC && !SystemProperties.getBoolean(PRIMARY_4G_CARD_PROPERTY_NAME, PRIMCARYCARD_L_W_ENABLED)) {
                if (UiccController.getInstance().getUiccCard(phoneId) != null && currentPrimarySlot == phoneId && settingsNetworkMode == 20) {
                    prefSet.addPreference(this.mButtonPreferredCMCCNetworkMode);
                }
                prefSet.removePreference(this.mButtonPreferredNetworkMode);
                prefSet.removePreference(this.mButtonEnabledNetworks);
            }
        }
        setScreenState();
        boolean z = (isLteOnCdma && carrierConfig.getBoolean("show_cdma_choices_bool")) ? DBG : PRIMCARYCARD_L_W_ENABLED;
        this.mIsGlobalCdma = z;
        if (carrierConfig.getBoolean("hide_carrier_network_settings_bool")) {
            prefSet.removePreference(this.mButtonPreferredNetworkMode);
            prefSet.removePreference(this.mButtonEnabledNetworks);
            prefSet.removePreference(this.mLteDataServicePref);
        } else if (carrierConfig.getBoolean("hide_preferred_network_type_bool") && !this.mPhone.getServiceState().getRoaming()) {
            prefSet.removePreference(this.mButtonPreferredNetworkMode);
            prefSet.removePreference(this.mButtonEnabledNetworks);
            phoneType = this.mPhone.getPhoneType();
            if (phoneType == 2) {
                this.mCdmaOptions = new CdmaOptions(this, prefSet, this.mPhone);
                if (isWorldMode()) {
                    this.mGsmUmtsOptions = null;
                }
            } else if (phoneType == 1) {
                this.mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet, phoneSubId);
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unexpected phone type: ");
                stringBuilder2.append(phoneType);
                throw new IllegalStateException(stringBuilder2.toString());
            }
            settingsNetworkMode = this.preferredNetworkMode;
        } else if (carrierConfig.getBoolean("world_phone_bool") == DBG) {
            prefSet.removePreference(this.mButtonEnabledNetworks);
            if (PRIMCARYCARD_L_W_ENABLED) {
                this.mButtonPreferredNetworkMode.setEntries(R.array.preferred_network_mode_choices);
                this.mButtonPreferredNetworkMode.setEntryValues(R.array.preferred_network_mode_values);
            }
            this.mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);
            this.mCdmaOptions = new CdmaOptions(this, prefSet, this.mPhone);
            this.mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet, phoneSubId);
        } else {
            StringBuilder stringBuilder3;
            prefSet.removePreference(this.mButtonPreferredNetworkMode);
            phoneType = this.mPhone.getPhoneType();
            int lteForced;
            if (phoneType == 2) {
                ContentResolver contentResolver = this.mPhone.getContext().getContentResolver();
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("lte_service_forced");
                stringBuilder3.append(this.mPhone.getSubId());
                lteForced = Global.getInt(contentResolver, stringBuilder3.toString(), 0);
                if (isLteOnCdma) {
                    if (lteForced != 0) {
                        switch (settingsNetworkMode) {
                            case 4:
                            case 5:
                            case 6:
                                this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_cdma_no_lte_choices);
                                this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_cdma_no_lte_values);
                                break;
                            case 7:
                            case 8:
                            case 10:
                            case 11:
                                this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_cdma_only_lte_choices);
                                this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_cdma_only_lte_values);
                                break;
                            default:
                                this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_cdma_choices);
                                this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_cdma_values);
                                break;
                        }
                    }
                    this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_cdma_choices);
                    this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_cdma_values);
                }
                this.mCdmaOptions = new CdmaOptions(this, prefSet, this.mPhone);
                if (isWorldMode()) {
                    this.mGsmUmtsOptions = null;
                }
            } else if (phoneType == 1) {
                if (isSupportTdscdma()) {
                    this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_tdscdma_choices);
                    this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_tdscdma_values);
                } else if (!carrierConfig.getBoolean("prefer_2g_bool") && !getResources().getBoolean(R.bool.config_enabled_lte)) {
                    this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_except_gsm_lte_choices);
                    this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_except_gsm_lte_values);
                } else if (!carrierConfig.getBoolean("prefer_2g_bool")) {
                    if (this.mShow4GForLTE == DBG) {
                        lteForced = R.array.enabled_networks_except_gsm_4g_choices;
                    } else {
                        lteForced = R.array.enabled_networks_except_gsm_choices;
                    }
                    this.mButtonEnabledNetworks.setEntries(lteForced);
                    this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_except_gsm_values);
                } else if (!getResources().getBoolean(R.bool.config_enabled_lte)) {
                    this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_except_lte_choices);
                    this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_except_lte_values);
                } else if (this.mIsGlobalCdma) {
                    this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_cdma_choices);
                    this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_cdma_values);
                } else {
                    if (this.mShow4GForLTE == DBG) {
                        lteForced = R.array.enabled_networks_4g_choices;
                    } else {
                        lteForced = R.array.enabled_networks_choices;
                    }
                    this.mButtonEnabledNetworks.setEntries(lteForced);
                    this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_values);
                }
                this.mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet, phoneSubId);
            } else {
                PreferenceScreen preferenceScreen = prefSet;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Unexpected phone type: ");
                stringBuilder4.append(phoneType);
                throw new IllegalStateException(stringBuilder4.toString());
            }
            if (isWorldMode()) {
                this.mButtonEnabledNetworks.setEntries(R.array.preferred_network_mode_choices_world_mode);
                this.mButtonEnabledNetworks.setEntryValues(R.array.preferred_network_mode_values_world_mode);
            }
            this.mButtonEnabledNetworks.setOnPreferenceChangeListener(this);
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("settingsNetworkMode: ");
            stringBuilder3.append(settingsNetworkMode);
            log(stringBuilder3.toString());
        }
        boolean missingDataServiceUrl = TextUtils.isEmpty(Global.getString(getContentResolver(), "setup_prepaid_data_service_url"));
        if (!isLteOnCdma || missingDataServiceUrl) {
            prefSet.removePreference(this.mLteDataServicePref);
        } else {
            Log.d(LOG_TAG, "keep ltePref");
        }
        if (!(this.mImsMgr.isVolteEnabledByPlatform() && this.mImsMgr.isVolteProvisionedOnDevice())) {
            Preference pref = prefSet.findPreference(BUTTON_4G_LTE_KEY);
            if (pref != null) {
                prefSet.removePreference(pref);
            }
        }
        phoneType = getActionBar();
        if (phoneType != 0) {
            phoneType.setDisplayHomeAsUpEnabled(DBG);
        }
        boolean isCellBroadcastAppLinkEnabled = getResources().getBoolean(17956915);
        if (!(this.mUm.isAdminUser() && isCellBroadcastAppLinkEnabled && !this.mUm.hasUserRestriction("no_config_cell_broadcasts"))) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = findPreference(BUTTON_CELL_BROADCAST_SETTINGS);
            if (ps != null) {
                root.removePreference(ps);
            }
        }
        this.mButtonDataRoam.setChecked(this.mPhone.getDataRoamingEnabled());
        this.mButtonEnabledNetworks.setValue(Integer.toString(settingsNetworkMode));
        this.mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
        this.mButtonPreferredCMCCNetworkMode.setValue(Integer.toString(settingsNetworkMode));
        UpdatePreferredNetworkModeSummary(settingsNetworkMode);
        UpdateEnabledNetworksValueAndSummary(settingsNetworkMode);
        UpdatePreferredCMCCNetworkModeSummary(settingsNetworkMode);
        boolean hasActiveSubscriptions = hasActiveSubscriptions();
        boolean canChange4glte = (((TelephonyManager) getSystemService("phone")).getCallState() == 0 && this.mImsMgr.isNonTtyOrTtyOnVolteEnabled() && carrierConfig.getBoolean("editable_enhanced_4g_lte_bool")) ? DBG : PRIMCARYCARD_L_W_ENABLED;
        this.mButtonDataRoam.setDisabledByAdmin(PRIMCARYCARD_L_W_ENABLED);
        this.mButtonDataRoam.setEnabled(hasActiveSubscriptions);
        if (this.mButtonDataRoam.isEnabled()) {
            if (RestrictedLockUtils.hasBaseUserRestriction(context, "no_data_roaming", UserHandle.myUserId())) {
                this.mButtonDataRoam.setEnabled(PRIMCARYCARD_L_W_ENABLED);
            } else {
                this.mButtonDataRoam.checkRestrictionAndSetDisabled("no_data_roaming");
            }
        }
        this.mButtonPreferredNetworkMode.setEnabled(hasActiveSubscriptions);
        this.mButtonEnabledNetworks.setEnabled(hasActiveSubscriptions);
        SwitchPreference switchPreference = this.mButton4glte;
        boolean z2 = (hasActiveSubscriptions && canChange4glte) ? DBG : PRIMCARYCARD_L_W_ENABLED;
        switchPreference.setEnabled(z2);
        this.mLteDataServicePref.setEnabled(hasActiveSubscriptions);
        prefSet = getPreferenceScreen();
        Preference ps2 = findPreference(BUTTON_CELL_BROADCAST_SETTINGS);
        if (ps2 != null) {
            ps2.setEnabled(hasActiveSubscriptions);
        }
        context = findPreference(BUTTON_APN_EXPAND_KEY);
        if (context != null) {
            context.setEnabled(hasActiveSubscriptions);
        }
        context = findPreference(BUTTON_OPERATOR_SELECTION_EXPAND_KEY);
        if (context == null || carrierConfig.getBoolean("show_apn_setting_cdma_bool")) {
        } else {
            boolean z3;
            if (hasActiveSubscriptions) {
                if (this.mPhone.getPhoneType() == 1) {
                    z3 = DBG;
                    context.setEnabled(z3);
                }
            }
            z3 = PRIMCARYCARD_L_W_ENABLED;
            context.setEnabled(z3);
        }
        context = findPreference(BUTTON_CARRIER_SETTINGS_KEY);
        if (context != null) {
            context.setEnabled(hasActiveSubscriptions);
        }
        context = findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY);
        if (context != null) {
            context.setEnabled(hasActiveSubscriptions);
        }
        if (SystemProperties.getBoolean(PRIMARY_4G_CARD_PROPERTY_NAME, PRIMCARYCARD_L_W_ENABLED) && SubscriptionManager.isValidSlotIndex(currentPrimarySlot) && phoneId != currentPrimarySlot) {
            if (PRIMCARYCARD_L_W_ENABLED) {
                this.mButtonPreferredNetworkMode.setEntries(R.array.preferred_network_mode_gsm_wcdma_choices);
                this.mButtonPreferredNetworkMode.setEntryValues(R.array.preferred_network_mode_gsm_wcdma_values);
                this.mButtonEnabledNetworks.setEntries(R.array.preferred_network_mode_gsm_wcdma_choices);
                this.mButtonEnabledNetworks.setEntryValues(R.array.preferred_network_mode_gsm_wcdma_values);
            } else if (getPreferredNetworkModeForPhoneId() == 1) {
                this.mButtonPreferredNetworkMode.setEnabled(PRIMCARYCARD_L_W_ENABLED);
                this.mButtonEnabledNetworks.setEnabled(PRIMCARYCARD_L_W_ENABLED);
            }
        }
        if (!isSubsidyLockFeatureEnabled() || !isSubsidyUnlocked()) {
            return;
        }
        if (SubscriptionManager.isValidSlotIndex(currentPrimarySlot) && phoneId == currentPrimarySlot) {
            this.mButtonPreferredNetworkMode.setEnabled(hasActiveSubscriptions);
            this.mButtonEnabledNetworks.setEnabled(hasActiveSubscriptions);
            this.mButtonPreferredNetworkMode.setEntries(R.array.enabled_networks_choices_subsidy_locked);
            this.mButtonPreferredNetworkMode.setEntryValues(R.array.enabled_networks_values_subsidy_locked);
            this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_choices_subsidy_locked);
            this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_values_subsidy_locked);
            return;
        }
        this.mButtonPreferredNetworkMode.setEnabled(PRIMCARYCARD_L_W_ENABLED);
        this.mButtonEnabledNetworks.setEnabled(PRIMCARYCARD_L_W_ENABLED);
    }

    /* Access modifiers changed, original: protected */
    public void onPause() {
        super.onPause();
        log("onPause:+");
        if (this.mImsMgr.isVolteEnabledByPlatform() && this.mImsMgr.isVolteProvisionedOnDevice()) {
            for (Phone phone : PhoneFactory.getPhones()) {
                listenForCallState(phone.getSubId(), 0);
            }
        }
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mOnSubscriptionsChangeListener);
        log("onPause:-");
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        log("onPreferenceChange");
        int phoneSubId = this.mPhone.getSubId();
        int buttonNetworkMode;
        int modemNetworkMode;
        StringBuilder stringBuilder;
        SwitchPreference enhanced4gModePref;
        if (preference == this.mButtonPreferredNetworkMode) {
            this.mButtonPreferredNetworkMode.setValue((String) objValue);
            buttonNetworkMode = Integer.parseInt((String) objValue);
            int settingsNetworkMode = getPreferredNetworkModeForSubId();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("buttonNetworkMode: ");
            stringBuilder2.append(buttonNetworkMode);
            stringBuilder2.append(" settingsNetworkMode: ");
            stringBuilder2.append(settingsNetworkMode);
            log(stringBuilder2.toString());
            if (buttonNetworkMode != settingsNetworkMode) {
                switch (buttonNetworkMode) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                        modemNetworkMode = buttonNetworkMode;
                        UpdatePreferredNetworkModeSummary(buttonNetworkMode);
                        this.mPhone.setPreferredNetworkType(modemNetworkMode, this.mHandler.obtainMessage(0));
                        break;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Invalid Network Mode (");
                        stringBuilder.append(buttonNetworkMode);
                        stringBuilder.append(") chosen. Ignore.");
                        loge(stringBuilder.toString());
                        return DBG;
                }
            }
        } else if (preference == this.mButtonEnabledNetworks) {
            this.mButtonEnabledNetworks.setValue((String) objValue);
            buttonNetworkMode = Integer.parseInt((String) objValue);
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("buttonNetworkMode: ");
            stringBuilder3.append(buttonNetworkMode);
            log(stringBuilder3.toString());
            if (buttonNetworkMode != getPreferredNetworkModeForSubId()) {
                switch (buttonNetworkMode) {
                    case 0:
                    case 1:
                    case 4:
                    case 5:
                    case 8:
                    case 9:
                    case 10:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                        modemNetworkMode = buttonNetworkMode;
                        UpdateEnabledNetworksValueAndSummary(buttonNetworkMode);
                        this.mPhone.setPreferredNetworkType(modemNetworkMode, this.mHandler.obtainMessage(0));
                        break;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Invalid Network Mode (");
                        stringBuilder.append(buttonNetworkMode);
                        stringBuilder.append(") chosen. Ignore.");
                        loge(stringBuilder.toString());
                        return DBG;
                }
            }
        } else if (preference == this.mButton4glte) {
            enhanced4gModePref = (SwitchPreference) preference;
            enhanced4gModePref.setChecked(enhanced4gModePref.isChecked() ^ DBG);
            this.mImsMgr.setEnhanced4gLteModeSetting(enhanced4gModePref.isChecked());
        } else if (this.mButtonEnable4g != null && preference == this.mButtonEnable4g) {
            enhanced4gModePref = (SwitchPreference) preference;
            enhanced4gModePref.setChecked(enhanced4gModePref.isChecked() ^ 1);
            handleEnable4gChange();
        } else if (preference == this.mButtonDataRoam) {
            log("onPreferenceTreeClick: preference == mButtonDataRoam.");
            if (this.mButtonDataRoam.isChecked()) {
                this.mPhone.setDataRoamingEnabled(PRIMCARYCARD_L_W_ENABLED);
            } else {
                this.mOkClicked = PRIMCARYCARD_L_W_ENABLED;
                new Builder(this).setMessage(getResources().getString(R.string.roaming_warning)).setTitle(R.string.roaming_alert_title).setIconAttribute(16843605).setPositiveButton(17039379, this).setNegativeButton(17039369, this).show().setOnDismissListener(this);
            }
            return DBG;
        }
        return DBG;
    }

    private void setPreferredNetworkMode(int nwMode) {
        int phoneSubId = this.mPhone.getSubId();
        int phoneId = this.mPhone.getPhoneId();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setPreferredNetworkMode: nwMode = ");
        stringBuilder.append(nwMode);
        stringBuilder.append(" phoneSubId = ");
        stringBuilder.append(phoneSubId);
        stringBuilder.append(" phoneId = ");
        stringBuilder.append(phoneId);
        log(stringBuilder.toString());
        this.preferredNetworkMode = nwMode;
        ContentResolver contentResolver = this.mPhone.getContext().getContentResolver();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("preferred_network_mode");
        stringBuilder2.append(phoneSubId);
        Global.putInt(contentResolver, stringBuilder2.toString(), nwMode);
        TelephonyManager.putIntAtIndex(this.mPhone.getContext().getContentResolver(), "preferred_network_mode", phoneId, nwMode);
    }

    private int getPreferredNetworkModeForPhoneId() {
        int phoneNwMode;
        int phoneId = this.mPhone.getPhoneId();
        try {
            phoneNwMode = TelephonyManager.getIntAtIndex(this.mPhone.getContext().getContentResolver(), "preferred_network_mode", phoneId);
        } catch (SettingNotFoundException e) {
            log("getPreferredNetworkModeForPhoneId: Could not find PREFERRED_NETWORK_MODE");
            phoneNwMode = Phone.PREFERRED_NT_MODE;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getPreferredNetworkModeForPhoneId: phoneNwMode = ");
        stringBuilder.append(phoneNwMode);
        stringBuilder.append(" phoneId = ");
        stringBuilder.append(phoneId);
        log(stringBuilder.toString());
        return phoneNwMode;
    }

    private int getPreferredNetworkModeForSubId() {
        int subId = this.mPhone.getSubId();
        int nwMode = this.mPhone.getContext().getContentResolver();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("preferred_network_mode");
        stringBuilder.append(subId);
        nwMode = Global.getInt(nwMode, stringBuilder.toString(), this.preferredNetworkMode);
        stringBuilder = new StringBuilder();
        stringBuilder.append("getPreferredNetworkModeForSubId: phoneNwMode = ");
        stringBuilder.append(nwMode);
        stringBuilder.append(" subId = ");
        stringBuilder.append(subId);
        log(stringBuilder.toString());
        return nwMode;
    }

    private void updatePreferredNetworkUIFromDb() {
        int phoneSubId = this.mPhone.getSubId();
        int settingsNetworkMode = getPreferredNetworkModeForSubId();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updatePreferredNetworkUIFromDb: settingsNetworkMode = ");
        stringBuilder.append(settingsNetworkMode);
        log(stringBuilder.toString());
        UpdatePreferredNetworkModeSummary(settingsNetworkMode);
        UpdateEnabledNetworksValueAndSummary(settingsNetworkMode);
        this.mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
    }

    private void UpdatePreferredCMCCNetworkModeSummary(int NetworkMode) {
        if (20 == NetworkMode) {
            this.mButtonPreferredCMCCNetworkMode.setSummary(R.string.preferred_network_mode_cmcc_summary);
        }
    }

    private void UpdatePreferredNetworkModeSummary(int NetworkMode) {
        switch (NetworkMode) {
            case 0:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_wcdma_perf_summary);
                return;
            case 1:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_gsm_only_summary);
                return;
            case 2:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_wcdma_only_summary);
                return;
            case 3:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_gsm_wcdma_summary);
                return;
            case 4:
                if (this.mPhone.getLteOnCdmaMode() != 1) {
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_cdma_evdo_summary);
                    return;
                } else {
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_cdma_summary);
                    return;
                }
            case 5:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_cdma_only_summary);
                return;
            case 6:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_evdo_only_summary);
                return;
            case 7:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_cdma_evdo_gsm_wcdma_summary);
                return;
            case 8:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_cdma_evdo_summary);
                return;
            case 9:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_gsm_wcdma_summary);
                return;
            case 10:
                if (this.mPhone.getPhoneType() == 2 || this.mIsGlobalCdma || isWorldMode()) {
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_global_summary);
                    return;
                } else {
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_summary);
                    return;
                }
            case 11:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_summary);
                return;
            case 12:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_wcdma_summary);
                return;
            case 13:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_tdscdma_summary);
                return;
            case 14:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_tdscdma_wcdma_summary);
                return;
            case 15:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_tdscdma_summary);
                return;
            case 16:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_tdscdma_gsm_summary);
                return;
            case 17:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_tdscdma_gsm_summary);
                return;
            case 18:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_tdscdma_gsm_wcdma_summary);
                return;
            case 19:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_tdscdma_wcdma_summary);
                return;
            case 20:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_tdscdma_gsm_wcdma_summary);
                return;
            case 21:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_tdscdma_cdma_evdo_gsm_wcdma_summary);
                return;
            case 22:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_tdscdma_cdma_evdo_gsm_wcdma_summary);
                return;
            default:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_global_summary);
                return;
        }
    }

    private void UpdateEnabledNetworksValueAndSummary(int NetworkMode) {
        ListPreference listPreference;
        int i = R.string.network_4G;
        switch (NetworkMode) {
            case 0:
            case 2:
            case 3:
                if (this.mIsGlobalCdma) {
                    this.mButtonEnabledNetworks.setValue(Integer.toString(10));
                    this.mButtonEnabledNetworks.setSummary(R.string.network_global);
                    return;
                }
                this.mButtonEnabledNetworks.setValue(Integer.toString(0));
                this.mButtonEnabledNetworks.setSummary(R.string.network_3G);
                return;
            case 1:
                if (this.mIsGlobalCdma) {
                    this.mButtonEnabledNetworks.setValue(Integer.toString(10));
                    this.mButtonEnabledNetworks.setSummary(R.string.network_global);
                    return;
                }
                this.mButtonEnabledNetworks.setValue(Integer.toString(1));
                this.mButtonEnabledNetworks.setSummary(R.string.network_2G);
                return;
            case 4:
            case 6:
            case 7:
                this.mButtonEnabledNetworks.setValue(Integer.toString(4));
                this.mButtonEnabledNetworks.setSummary(R.string.network_3G);
                return;
            case 5:
                this.mButtonEnabledNetworks.setValue(Integer.toString(5));
                this.mButtonEnabledNetworks.setSummary(R.string.network_1x);
                return;
            case 8:
                if (isWorldMode()) {
                    this.mButtonEnabledNetworks.setSummary(R.string.preferred_network_mode_lte_cdma_summary);
                    controlCdmaOptions(DBG);
                    controlGsmOptions(PRIMCARYCARD_L_W_ENABLED);
                    return;
                }
                this.mButtonEnabledNetworks.setValue(Integer.toString(8));
                this.mButtonEnabledNetworks.setSummary(R.string.network_lte);
                return;
            case 9:
                if (isWorldMode()) {
                    this.mButtonEnabledNetworks.setSummary(R.string.preferred_network_mode_lte_gsm_umts_summary);
                    controlCdmaOptions(PRIMCARYCARD_L_W_ENABLED);
                    controlGsmOptions(DBG);
                    return;
                }
                break;
            case 10:
            case 15:
            case 17:
            case 19:
            case 20:
            case 22:
                if (isSupportTdscdma()) {
                    this.mButtonEnabledNetworks.setValue(Integer.toString(22));
                    this.mButtonEnabledNetworks.setSummary(R.string.network_lte);
                    return;
                }
                if (isWorldMode()) {
                    controlCdmaOptions(DBG);
                    controlGsmOptions(PRIMCARYCARD_L_W_ENABLED);
                }
                this.mButtonEnabledNetworks.setValue(Integer.toString(10));
                if (this.mPhone.getPhoneType() == 2 || this.mIsGlobalCdma || isWorldMode()) {
                    this.mButtonEnabledNetworks.setSummary(R.string.network_global);
                    return;
                }
                listPreference = this.mButtonEnabledNetworks;
                if (this.mShow4GForLTE != DBG) {
                    i = R.string.network_lte;
                }
                listPreference.setSummary(i);
                return;
            case 11:
            case 12:
                break;
            case 13:
                this.mButtonEnabledNetworks.setValue(Integer.toString(13));
                this.mButtonEnabledNetworks.setSummary(R.string.network_3G);
                return;
            case 14:
            case 16:
            case 18:
                this.mButtonEnabledNetworks.setValue(Integer.toString(18));
                this.mButtonEnabledNetworks.setSummary(R.string.network_3G);
                return;
            case 21:
                this.mButtonEnabledNetworks.setValue(Integer.toString(21));
                this.mButtonEnabledNetworks.setSummary(R.string.network_3G);
                return;
            default:
                String errMsg = new StringBuilder();
                errMsg.append("Invalid Network Mode (");
                errMsg.append(NetworkMode);
                errMsg.append("). Ignore.");
                errMsg = errMsg.toString();
                loge(errMsg);
                this.mButtonEnabledNetworks.setSummary(errMsg);
                return;
        }
        if (this.mIsGlobalCdma) {
            this.mButtonEnabledNetworks.setValue(Integer.toString(10));
            this.mButtonEnabledNetworks.setSummary(R.string.network_global);
            return;
        }
        this.mButtonEnabledNetworks.setValue(Integer.toString(9));
        listPreference = this.mButtonEnabledNetworks;
        if (this.mShow4GForLTE != DBG) {
            i = R.string.network_lte;
        }
        listPreference.setSummary(i);
    }

    /* Access modifiers changed, original: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 17 && Boolean.valueOf(data.getBooleanExtra(EXTRA_EXIT_ECM_RESULT, PRIMCARYCARD_L_W_ENABLED)).booleanValue()) {
            this.mCdmaOptions.showDialog(this.mClickedPreference);
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(LOG_TAG, msg);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        finish();
        return DBG;
    }

    private boolean isWorldMode() {
        boolean worldModeOn = PRIMCARYCARD_L_W_ENABLED;
        TelephonyManager tm = (TelephonyManager) getSystemService("phone");
        String configString = getResources().getString(R.string.config_world_mode);
        if (!TextUtils.isEmpty(configString)) {
            String[] configArray = configString.split(";");
            if (configArray != null && ((configArray.length == 1 && configArray[0].equalsIgnoreCase("true")) || (configArray.length == 2 && !TextUtils.isEmpty(configArray[1]) && tm != null && configArray[1].equalsIgnoreCase(tm.getGroupIdLevel1())))) {
                worldModeOn = DBG;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isWorldMode=");
        stringBuilder.append(worldModeOn);
        log(stringBuilder.toString());
        return worldModeOn;
    }

    private void controlGsmOptions(boolean enable) {
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet != null) {
            if (this.mGsmUmtsOptions == null) {
                this.mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet, this.mPhone.getSubId());
            }
            PreferenceScreen apnExpand = (PreferenceScreen) prefSet.findPreference(BUTTON_APN_EXPAND_KEY);
            PreferenceScreen operatorSelectionExpand = (PreferenceScreen) prefSet.findPreference(BUTTON_OPERATOR_SELECTION_EXPAND_KEY);
            PreferenceScreen carrierSettings = (PreferenceScreen) prefSet.findPreference(BUTTON_CARRIER_SETTINGS_KEY);
            if (apnExpand != null) {
                boolean z = (isWorldMode() || enable) ? DBG : PRIMCARYCARD_L_W_ENABLED;
                apnExpand.setEnabled(z);
            }
            if (operatorSelectionExpand != null) {
                if (enable) {
                    operatorSelectionExpand.setEnabled(DBG);
                } else {
                    prefSet.removePreference(operatorSelectionExpand);
                }
            }
            if (carrierSettings != null) {
                prefSet.removePreference(carrierSettings);
            }
        }
    }

    private void controlCdmaOptions(boolean enable) {
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet != null) {
            if (enable && this.mCdmaOptions == null) {
                this.mCdmaOptions = new CdmaOptions(this, prefSet, this.mPhone);
            }
            CdmaSystemSelectListPreference systemSelect = (CdmaSystemSelectListPreference) prefSet.findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY);
            if (systemSelect != null) {
                systemSelect.setEnabled(enable);
            }
        }
    }

    private boolean isSupportTdscdma() {
        int subId = this.mPhone.getSubId();
        if (SubscriptionManager.getResourcesForSubId(this, subId).getBoolean(R.bool.config_support_tdscdma)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Supports Tdscdma, subId ");
            stringBuilder.append(subId);
            log(stringBuilder.toString());
            return DBG;
        }
        String operatorNumeric = this.mPhone.getServiceState().getOperatorNumeric();
        String[] numericArray = SubscriptionManager.getResourcesForSubId(this, subId).getStringArray(R.array.config_support_tdscdma_roaming_on_networks);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("isSupportTdscdma, operatorNumeric ");
        stringBuilder2.append(operatorNumeric);
        stringBuilder2.append(" subId ");
        stringBuilder2.append(subId);
        log(stringBuilder2.toString());
        if (numericArray.length == 0 || operatorNumeric == null) {
            return PRIMCARYCARD_L_W_ENABLED;
        }
        for (String numeric : numericArray) {
            if (operatorNumeric.equals(numeric)) {
                return DBG;
            }
        }
        return PRIMCARYCARD_L_W_ENABLED;
    }
}
