package com.qualcomm.qti.networksetting;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;

public class ProgressPrefCategory extends PreferenceCategory {
    private String mEmptyTextRes;
    private boolean mNoResultFoundAdded;
    private Preference mNoResultFoundPreference;
    private boolean mProgress = false;
    private boolean mScanStarted = false;

    public ProgressPrefCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.string.Far_EasTone);
        this.mEmptyTextRes = context.getString(attrs.getAttributeResourceValue(null, "empty_text", 0));
        setTitle(attrs.getAttributeResourceValue(null, "title", 0));
    }

    public void onBindView(View view) {
        super.onBindView(view);
        View progressBar = view.findViewById(R.id.scanning_progress);
        boolean noResultFound = getPreferenceCount() == 0 || (getPreferenceCount() == 1 && getPreference(0) == this.mNoResultFoundPreference);
        progressBar.setVisibility(this.mProgress ? 0 : 8);
        if (this.mProgress || !noResultFound) {
            if (this.mNoResultFoundAdded) {
                removePreference(this.mNoResultFoundPreference);
                this.mNoResultFoundAdded = false;
            }
        } else if (!this.mNoResultFoundAdded && this.mScanStarted) {
            if (this.mNoResultFoundPreference == null) {
                this.mNoResultFoundPreference = new Preference(getContext());
                this.mNoResultFoundPreference.setLayoutResource(R.string.timepicker_selection_radius_multiplier);
                this.mNoResultFoundPreference.setTitle(this.mEmptyTextRes);
                this.mNoResultFoundPreference.setSelectable(false);
            }
            addPreference(this.mNoResultFoundPreference);
            this.mNoResultFoundAdded = true;
        }
    }

    public void setProgress(boolean progressOn) {
        this.mProgress = progressOn;
        if (progressOn) {
            this.mScanStarted = true;
        }
        notifyChanged();
    }
}
