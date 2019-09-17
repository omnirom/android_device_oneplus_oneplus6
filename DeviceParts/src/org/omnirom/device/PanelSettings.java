/*
* Copyright (C) 2018 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package org.omnirom.device;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.Resources;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.PreferenceFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.util.Log;

public class PanelSettings extends PreferenceFragment implements RadioGroup.OnCheckedChangeListener {
    private RadioGroup mRadioGroup;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRadioGroup = (RadioGroup) view.findViewById(R.id.radio_group);
        int checkedButtonId = R.id.off_mode;
        if (NightModeSwitch.isCurrentlyEnabled(getContext())) {
            checkedButtonId = R.id.night_mode;
        } else if (DCIModeSwitch.isCurrentlyEnabled(getContext())) {
            checkedButtonId = R.id.dci_mode;
        } else if (SRGBModeSwitch.isCurrentlyEnabled(getContext())) {
            checkedButtonId = R.id.srgb_mode;
        } else if (AdaptiveModeSwitch.isCurrentlyEnabled(getContext())) {
            checkedButtonId = R.id.adaptive_mode;
        } else if (OnePlusModeSwitch.isCurrentlyEnabled(getContext())) {
            checkedButtonId = R.id.oneplus_mode;
        }
        mRadioGroup.check(checkedButtonId);
        mRadioGroup.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.panel_modes, container, false);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.srgb_mode) {
            Utils.writeValue(DCIModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), DCIModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(NightModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), NightModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(AdaptiveModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), AdaptiveModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(OnePlusModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), OnePlusModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(SRGBModeSwitch.getFile(), "1");
            Settings.System.putInt(getContext().getContentResolver(), SRGBModeSwitch.SETTINGS_KEY, 1);
        } else if (checkedId == R.id.dci_mode) {
            Utils.writeValue(DCIModeSwitch.getFile(), "1");
            Settings.System.putInt(getContext().getContentResolver(), DCIModeSwitch.SETTINGS_KEY, 1);
            Utils.writeValue(NightModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), NightModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(AdaptiveModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), AdaptiveModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(OnePlusModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), OnePlusModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(SRGBModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), SRGBModeSwitch.SETTINGS_KEY, 0);
        } else if (checkedId == R.id.night_mode) {
            Utils.writeValue(DCIModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), DCIModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(NightModeSwitch.getFile(), "1");
            Settings.System.putInt(getContext().getContentResolver(), NightModeSwitch.SETTINGS_KEY, 1);
            Utils.writeValue(AdaptiveModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), AdaptiveModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(OnePlusModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), OnePlusModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(SRGBModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), SRGBModeSwitch.SETTINGS_KEY, 0);
        } else if (checkedId == R.id.off_mode) {
            Utils.writeValue(DCIModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), DCIModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(NightModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), NightModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(AdaptiveModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), AdaptiveModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(OnePlusModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), OnePlusModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(SRGBModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), SRGBModeSwitch.SETTINGS_KEY, 0);
        } else if (checkedId == R.id.adaptive_mode) {
            Utils.writeValue(DCIModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), DCIModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(NightModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), NightModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(AdaptiveModeSwitch.getFile(), "1");
            Settings.System.putInt(getContext().getContentResolver(), AdaptiveModeSwitch.SETTINGS_KEY, 1);
            Utils.writeValue(OnePlusModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), OnePlusModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(SRGBModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), SRGBModeSwitch.SETTINGS_KEY, 0);
        } else if (checkedId == R.id.oneplus_mode) {
            Utils.writeValue(DCIModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), DCIModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(NightModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), NightModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(AdaptiveModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), AdaptiveModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(OnePlusModeSwitch.getFile(), "1");
            Settings.System.putInt(getContext().getContentResolver(), OnePlusModeSwitch.SETTINGS_KEY, 0);
            Utils.writeValue(SRGBModeSwitch.getFile(), "0");
            Settings.System.putInt(getContext().getContentResolver(), SRGBModeSwitch.SETTINGS_KEY, 1);
        }
    }
}
