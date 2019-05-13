package com.qualcomm.qti.networksetting;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.qualcomm.qti.networksetting.RestrictedLockUtils.EnforcedAdmin;

public class RestrictedSwitchPreference extends SwitchPreference {
    private final Context mContext;
    private boolean mDisabledByAdmin;
    private final int mSwitchWidgetResId;

    public RestrictedSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mSwitchWidgetResId = getWidgetLayoutResource();
        this.mContext = context;
    }

    public RestrictedSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RestrictedSwitchPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 16843629);
    }

    public RestrictedSwitchPreference(Context context) {
        this(context, null);
    }

    public void onBindView(View view) {
        super.onBindView(view);
        if (this.mDisabledByAdmin) {
            view.setEnabled(true);
        }
        TextView summaryView = (TextView) view.findViewById(16908304);
        if (summaryView != null && this.mDisabledByAdmin) {
            summaryView.setText(isChecked() ? R.string.enabled_by_admin : R.string.disabled_by_admin);
            summaryView.setVisibility(0);
        }
    }

    public void checkRestrictionAndSetDisabled(String userRestriction) {
        UserManager um = UserManager.get(this.mContext);
        UserHandle user = UserHandle.of(um.getUserHandle());
        boolean disabledByAdmin = um.hasUserRestriction(userRestriction, user) && !um.hasBaseUserRestriction(userRestriction, user);
        setDisabledByAdmin(disabledByAdmin);
    }

    public void setEnabled(boolean enabled) {
        if (enabled && this.mDisabledByAdmin) {
            setDisabledByAdmin(false);
        } else {
            super.setEnabled(enabled);
        }
    }

    public void setDisabledByAdmin(boolean disabled) {
        if (this.mDisabledByAdmin != disabled) {
            this.mDisabledByAdmin = disabled;
            setWidgetLayoutResource(disabled ? R.string.T_Star : this.mSwitchWidgetResId);
            setEnabled(disabled ^ 1);
        }
    }

    public void performClick(PreferenceScreen preferenceScreen) {
        if (this.mDisabledByAdmin) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(this.mContext, new EnforcedAdmin());
        } else {
            super.performClick(preferenceScreen);
        }
    }
}
