/*
* Copyright (C) 2013 The OmniROM Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;

public class Startup extends BroadcastReceiver {
    private static void restore(String file, boolean enabled) {
        if (file == null) {
            return;
        }
        Utils.writeValue(file, enabled ? "1" : "0");
    }

    private static void restore(String file, String value) {
        if (file == null) {
            return;
        }
        Utils.writeValue(file, value);
    }

    private static String getGestureFile(String key) {
        return GestureSettings.getGestureFile(key);
    }

    private void maybeImportOldSettings(Context context) {
        boolean imported = Settings.System.getInt(context.getContentResolver(), "omni_device_setting_imported", 0) != 0;
        if (!imported) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean enabled = sharedPrefs.getBoolean(DeviceSettings.KEY_SRGB_SWITCH, false);
            Settings.System.putInt(context.getContentResolver(), SRGBModeSwitch.SETTINGS_KEY, enabled ? 1 : 0);

            enabled = sharedPrefs.getBoolean(DeviceSettings.KEY_HBM_SWITCH, false);
            Settings.System.putInt(context.getContentResolver(), HBMModeSwitch.SETTINGS_KEY, enabled ? 1 : 0);

            enabled = sharedPrefs.getBoolean(DeviceSettings.KEY_DCI_SWITCH, false);
            Settings.System.putInt(context.getContentResolver(), DCIModeSwitch.SETTINGS_KEY, enabled ? 1 : 0);

            enabled = sharedPrefs.getBoolean(DeviceSettings.KEY_NIGHT_SWITCH, false);
            Settings.System.putInt(context.getContentResolver(), NightModeSwitch.SETTINGS_KEY, enabled ? 1 : 0);

            enabled = sharedPrefs.getBoolean(DeviceSettings.KEY_ADAPTIVE_SWITCH, false);
            Settings.System.putInt(context.getContentResolver(), AdaptiveModeSwitch.SETTINGS_KEY, enabled ? 1 : 0);

            enabled = sharedPrefs.getBoolean(DeviceSettings.KEY_ONEPLUS_SWITCH, false);
            Settings.System.putInt(context.getContentResolver(), OnePlusModeSwitch.SETTINGS_KEY, enabled ? 1 : 0);

            enabled = sharedPrefs.getBoolean(DeviceSettings.KEY_OTG_SWITCH, false);
            Settings.System.putInt(context.getContentResolver(), UsbOtgSwitch.SETTINGS_KEY, enabled ? 1 : 0);

            String vibrStrength = sharedPrefs.getString(DeviceSettings.KEY_VIBSTRENGTH, VibratorStrengthPreference.DEFAULT_VALUE); 
            Settings.System.putString(context.getContentResolver(), VibratorStrengthPreference.SETTINGS_KEY, vibrStrength);

            Settings.System.putInt(context.getContentResolver(), "omni_device_setting_imported", 1);
        }
    }

    @Override
    public void onReceive(final Context context, final Intent bootintent) {
        maybeImportOldSettings(context);
        restoreAfterUserSwitch(context);
    }

    public static void restoreAfterUserSwitch(Context context) {
        // double swipe -> music play
        String mapping = GestureSettings.DEVICE_GESTURE_MAPPING_0;
        String value = Settings.System.getString(context.getContentResolver(), mapping);
        if (TextUtils.isEmpty(value)) {
            value = AppSelectListPreference.MUSIC_PLAY_ENTRY;
            Settings.System.putString(context.getContentResolver(), mapping, value);
        }
        boolean enabled = !value.equals(AppSelectListPreference.DISABLED_ENTRY);
        restore(getGestureFile(GestureSettings.KEY_DOUBLE_SWIPE_APP), enabled);

        // circle -> camera
        mapping = GestureSettings.DEVICE_GESTURE_MAPPING_1;
        value = Settings.System.getString(context.getContentResolver(), mapping);
        if (TextUtils.isEmpty(value)) {
            value = AppSelectListPreference.CAMERA_ENTRY;
            Settings.System.putString(context.getContentResolver(), mapping, value);
        }
        enabled = !value.equals(AppSelectListPreference.DISABLED_ENTRY);
        restore(getGestureFile(GestureSettings.KEY_CIRCLE_APP), enabled);

        // down arrow -> flashlight
        mapping = GestureSettings.DEVICE_GESTURE_MAPPING_2;
        value = Settings.System.getString(context.getContentResolver(), mapping);
        if (TextUtils.isEmpty(value)) {
            value = AppSelectListPreference.TORCH_ENTRY;
            Settings.System.putString(context.getContentResolver(), mapping, value);
        }
        enabled = !value.equals(AppSelectListPreference.DISABLED_ENTRY);
        restore(getGestureFile(GestureSettings.KEY_DOWN_ARROW_APP), enabled);

        // up arrow
        value = Settings.System.getString(context.getContentResolver(), GestureSettings.DEVICE_GESTURE_MAPPING_3);
        enabled = !TextUtils.isEmpty(value) && !value.equals(AppSelectListPreference.DISABLED_ENTRY);
        restore(getGestureFile(GestureSettings.KEY_UP_ARROW_APP), enabled);

        // left arrow -> music prev
        mapping = GestureSettings.DEVICE_GESTURE_MAPPING_4;
        value = Settings.System.getString(context.getContentResolver(), mapping);
        if (TextUtils.isEmpty(value)) {
            value = AppSelectListPreference.MUSIC_PREV_ENTRY;
            Settings.System.putString(context.getContentResolver(), mapping, value);
        }
        enabled = !value.equals(AppSelectListPreference.DISABLED_ENTRY);
        restore(getGestureFile(GestureSettings.KEY_LEFT_ARROW_APP), enabled);

        // right arrow -> music next
        mapping = GestureSettings.DEVICE_GESTURE_MAPPING_5;
        value = Settings.System.getString(context.getContentResolver(), mapping);
        if (TextUtils.isEmpty(value)) {
            value = AppSelectListPreference.MUSIC_NEXT_ENTRY;
            Settings.System.putString(context.getContentResolver(), mapping, value);
        }
        enabled = !value.equals(AppSelectListPreference.DISABLED_ENTRY);
        restore(getGestureFile(GestureSettings.KEY_RIGHT_ARROW_APP), enabled);

        // down swipe
        value = Settings.System.getString(context.getContentResolver(), GestureSettings.DEVICE_GESTURE_MAPPING_6);
        enabled = !TextUtils.isEmpty(value) && !value.equals(AppSelectListPreference.DISABLED_ENTRY);
        restore(getGestureFile(GestureSettings.KEY_DOWN_SWIPE_APP), enabled);

        // up swipe
        value = Settings.System.getString(context.getContentResolver(), GestureSettings.DEVICE_GESTURE_MAPPING_7);
        enabled = !TextUtils.isEmpty(value) && !value.equals(AppSelectListPreference.DISABLED_ENTRY);
        restore(getGestureFile(GestureSettings.KEY_UP_SWIPE_APP), enabled);

        // left swipe
        value = Settings.System.getString(context.getContentResolver(), GestureSettings.DEVICE_GESTURE_MAPPING_8);
        enabled = !TextUtils.isEmpty(value) && !value.equals(AppSelectListPreference.DISABLED_ENTRY);
        restore(getGestureFile(GestureSettings.KEY_LEFT_SWIPE_APP), enabled);

        // right swipe
        value = Settings.System.getString(context.getContentResolver(), GestureSettings.DEVICE_GESTURE_MAPPING_9);
        enabled = !TextUtils.isEmpty(value) && !value.equals(AppSelectListPreference.DISABLED_ENTRY);
        restore(getGestureFile(GestureSettings.KEY_RIGHT_SWIPE_APP), enabled);

        // fp down swipe
        value = Settings.System.getString(context.getContentResolver(), GestureSettings.DEVICE_GESTURE_MAPPING_10);
        enabled = !TextUtils.isEmpty(value) && !value.equals(AppSelectListPreference.DISABLED_ENTRY);
        restore(getGestureFile(GestureSettings.FP_GESTURE_LONG_PRESS_APP), enabled);

        enabled = Settings.System.getInt(context.getContentResolver(), SRGBModeSwitch.SETTINGS_KEY, 0) != 0;
        restore(SRGBModeSwitch.getFile(), enabled);

        enabled = Settings.System.getInt(context.getContentResolver(), DCIModeSwitch.SETTINGS_KEY, 0) != 0;
        restore(DCIModeSwitch.getFile(), enabled);

        enabled = Settings.System.getInt(context.getContentResolver(), NightModeSwitch.SETTINGS_KEY, 0) != 0;
        restore(NightModeSwitch.getFile(), enabled);

        enabled = Settings.System.getInt(context.getContentResolver(), AdaptiveModeSwitch.SETTINGS_KEY, 0) != 0;
        restore(AdaptiveModeSwitch.getFile(), enabled);

        enabled = Settings.System.getInt(context.getContentResolver(), OnePlusModeSwitch.SETTINGS_KEY, 0) != 0;
        restore(OnePlusModeSwitch.getFile(), enabled);

        enabled = Settings.System.getInt(context.getContentResolver(), HBMModeSwitch.SETTINGS_KEY, 0) != 0;
        restore(HBMModeSwitch.getFile(), enabled);

        enabled = Settings.System.getInt(context.getContentResolver(), UsbOtgSwitch.SETTINGS_KEY, 0) != 0;
        restore(UsbOtgSwitch.getFile(), enabled);

        VibratorStrengthPreference.restore(context);
    }
}
