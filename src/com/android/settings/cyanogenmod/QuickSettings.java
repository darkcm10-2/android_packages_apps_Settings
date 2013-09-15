/*
 * Copyright (C) 2011 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.util.cm.QSConstants;
import com.android.internal.util.cm.QSUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.util.Helpers;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class QuickSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "QuickSettingsPanel";

    private static final String SEPARATOR = "OV=I=XseparatorX=I=VO";
    private static final String EXP_RING_MODE = "pref_ring_mode";
    private static final String EXP_NETWORK_MODE = "pref_network_mode";
    private static final String EXP_SCREENTIMEOUT_MODE = "pref_screentimeout_mode";
    private static final String QUICK_PULLDOWN = "quick_pulldown";
    private static final String GENERAL_SETTINGS = "pref_general_settings";
    private static final String STATIC_TILES = "static_tiles";
    private static final String DYNAMIC_TILES = "pref_dynamic_tiles";
    private static final String PREF_FLIP_QS_TILES = "flip_qs_tiles";
    private static final String FLOATING_WINDOW ="floating_window";
    private static final String QUICK_SETTINGS_COLUMNS = "quick_settings_columns";
    private static final String PREF_QUICK_TILES_BG_COLOR = "quick_tiles_bg_color";
    private static final String PREF_QUICK_TILES_BG_PRESSED_COLOR = "quick_tiles_bg_pressed_color";

    private static final int DEFAULT_QUICK_TILES_BG_COLOR = 0xff161616;
    private static final int DEFAULT_QUICK_TILES_BG_PRESSED_COLOR = 0xff212121;

    private MultiSelectListPreference mRingMode;
    private ListPreference mNetworkMode;
    private ListPreference mScreenTimeoutMode;
    CheckBoxPreference mFlipQsTiles;
    CheckBoxPreference mFloatingWindow;
    private ListPreference mQuickPulldown;
    private ListPreference mQuickSettingsColumns;
    private PreferenceCategory mGeneralSettings;
    private PreferenceCategory mStaticTiles;
    private PreferenceCategory mDynamicTiles;
    private ColorPickerPreference mQuickTilesBgColor;
    private ColorPickerPreference mQuickTilesBgPressedColor;

    private PowerManager pm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.quick_settings_panel);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();
        mGeneralSettings = (PreferenceCategory) prefSet.findPreference(GENERAL_SETTINGS);
        mStaticTiles = (PreferenceCategory) prefSet.findPreference(STATIC_TILES);
        mDynamicTiles = (PreferenceCategory) prefSet.findPreference(DYNAMIC_TILES);
        mQuickPulldown = (ListPreference) prefSet.findPreference(QUICK_PULLDOWN);
        mQuickSettingsColumns = (ListPreference) prefSet.findPreference(QUICK_SETTINGS_COLUMNS);

        if (!Utils.isPhone(getActivity())) {
            if (mQuickPulldown != null) {
                mGeneralSettings.removePreference(mQuickPulldown);
            }
        } else {
            mQuickPulldown.setOnPreferenceChangeListener(this);
            int quickPulldownValue = Settings.System.getInt(resolver,
                    Settings.System.QS_QUICK_PULLDOWN, 0);
            mQuickPulldown.setValue(String.valueOf(quickPulldownValue));
            updatePulldownSummary(quickPulldownValue);
        }

        mFloatingWindow = (CheckBoxPreference) prefSet.findPreference(FLOATING_WINDOW);
        mFloatingWindow.setChecked(Settings.System.getInt(resolver, Settings.System.QS_FLOATING_WINDOW, 0) == 1);

        // Add the sound mode
        mRingMode = (MultiSelectListPreference) prefSet.findPreference(EXP_RING_MODE);
        String storedRingMode = Settings.System.getString(resolver,
                Settings.System.EXPANDED_RING_MODE);
        if (storedRingMode != null) {
            String[] ringModeArray = TextUtils.split(storedRingMode, SEPARATOR);
            mRingMode.setValues(new HashSet<String>(Arrays.asList(ringModeArray)));
            updateSummary(storedRingMode, mRingMode, R.string.pref_ring_mode_summary);
        }
        mRingMode.setOnPreferenceChangeListener(this);

        mFlipQsTiles = (CheckBoxPreference) findPreference(PREF_FLIP_QS_TILES);
        mFlipQsTiles.setChecked(Settings.System.getInt(resolver,
                Settings.System.QUICK_SETTINGS_TILES_FLIP, 1) == 1);

        // Add the network mode preference
        mNetworkMode = (ListPreference) prefSet.findPreference(EXP_NETWORK_MODE);
        if (mNetworkMode != null) {
            mNetworkMode.setSummary(mNetworkMode.getEntry());
            mNetworkMode.setOnPreferenceChangeListener(this);
        }

        // Screen timeout mode
        mScreenTimeoutMode = (ListPreference) prefSet.findPreference(EXP_SCREENTIMEOUT_MODE);
        mScreenTimeoutMode.setSummary(mScreenTimeoutMode.getEntry());
        mScreenTimeoutMode.setOnPreferenceChangeListener(this);

        // Remove unsupported options
        if (!QSUtils.deviceSupportsDockBattery(getActivity())) {
            mDynamicTiles.removePreference(findPreference(Settings.System.QS_DYNAMIC_DOCK_BATTERY));
        }
        if (!QSUtils.deviceSupportsImeSwitcher(getActivity())) {
            mDynamicTiles.removePreference(findPreference(Settings.System.QS_DYNAMIC_IME));
        }
        if (!QSUtils.deviceSupportsUsbTether(getActivity())) {
            mDynamicTiles.removePreference(findPreference(Settings.System.QS_DYNAMIC_USBTETHER));
        }
        if (!QSUtils.deviceSupportsWifiDisplay(getActivity())) {
            mDynamicTiles.removePreference(findPreference(Settings.System.QS_DYNAMIC_WIFI));
        }

        mQuickSettingsColumns.setOnPreferenceChangeListener(this);
        int quickSettingsColumnsValue = Settings.System.getInt(resolver,
        	Settings.System.QUICK_SETTINGS_COLUMNS, 3);
        mQuickSettingsColumns.setValue(String.valueOf(quickSettingsColumnsValue));

        mQuickTilesBgColor = (ColorPickerPreference) findPreference(PREF_QUICK_TILES_BG_COLOR);
        mQuickTilesBgColor.setNewPreviewColor(DEFAULT_QUICK_TILES_BG_COLOR);
        mQuickTilesBgColor.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.QUICK_TILES_BG_COLOR, -2);
        if (intColor == -2) {
            mQuickTilesBgColor.setSummary(getResources().getString(R.string.none));
        } else {
            mQuickTilesBgColor.setNewPreviewColor(intColor);
        }

        mQuickTilesBgPressedColor = (ColorPickerPreference) findPreference(PREF_QUICK_TILES_BG_PRESSED_COLOR);
        mQuickTilesBgPressedColor.setNewPreviewColor(DEFAULT_QUICK_TILES_BG_PRESSED_COLOR);
        mQuickTilesBgPressedColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.QUICK_TILES_BG_PRESSED_COLOR, -2);
        if (intColor == -2) {
            mQuickTilesBgPressedColor.setSummary(getResources().getString(R.string.none));
        } else {
            mQuickTilesBgPressedColor.setNewPreviewColor(intColor);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.quick_settings_tiles_style, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reset:
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.QUICK_TILES_BG_COLOR, -2);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.QUICK_TILES_BG_PRESSED_COLOR, -2);

                //refreshSettings();
		mQuickTilesBgColor.setNewPreviewColor(DEFAULT_QUICK_TILES_BG_COLOR);
		mQuickTilesBgPressedColor.setNewPreviewColor(DEFAULT_QUICK_TILES_BG_PRESSED_COLOR);
                Helpers.restartSystemUI();
                pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                pm.goToSleep(SystemClock.uptimeMillis());
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        QuickSettingsUtil.updateAvailableTiles(getActivity());

        if (mNetworkMode != null) {
            if (QuickSettingsUtil.isTileAvailable(QSConstants.TILE_NETWORKMODE)) {
                mStaticTiles.addPreference(mNetworkMode);
            } else {
                mStaticTiles.removePreference(mNetworkMode);
            }
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mFlipQsTiles) {
            Settings.System.putInt(resolver,
                    Settings.System.QUICK_SETTINGS_TILES_FLIP,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mFloatingWindow) {
            Settings.System.putInt(resolver, Settings.System.QS_FLOATING_WINDOW,
                    mFloatingWindow.isChecked() ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private class MultiSelectListPreferenceComparator implements Comparator<String> {
        private MultiSelectListPreference pref;

        MultiSelectListPreferenceComparator(MultiSelectListPreference p) {
            pref = p;
        }

        @Override
        public int compare(String lhs, String rhs) {
            return Integer.compare(pref.findIndexOfValue(lhs),
                    pref.findIndexOfValue(rhs));
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getContentResolver();
        if (preference == mRingMode) {
            ArrayList<String> arrValue = new ArrayList<String>((Set<String>) newValue);
            Collections.sort(arrValue, new MultiSelectListPreferenceComparator(mRingMode));
            String value = TextUtils.join(SEPARATOR, arrValue);
            Settings.System.putString(resolver, Settings.System.EXPANDED_RING_MODE, value);
            updateSummary(value, mRingMode, R.string.pref_ring_mode_summary);
            return true;
        } else if (preference == mNetworkMode) {
            int value = Integer.valueOf((String) newValue);
            int index = mNetworkMode.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver, Settings.System.EXPANDED_NETWORK_MODE, value);
            mNetworkMode.setSummary(mNetworkMode.getEntries()[index]);
            return true;
        } else if (preference == mQuickPulldown) {
            int quickPulldownValue = Integer.valueOf((String) newValue);
            Settings.System.putInt(resolver, Settings.System.QS_QUICK_PULLDOWN,
                    quickPulldownValue);
            updatePulldownSummary(quickPulldownValue);
            return true;
        } else if (preference == mScreenTimeoutMode) {
            int value = Integer.valueOf((String) newValue);
            int index = mScreenTimeoutMode.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver, Settings.System.EXPANDED_SCREENTIMEOUT_MODE, value);
            mScreenTimeoutMode.setSummary(mScreenTimeoutMode.getEntries()[index]);
            return true;
        } else if (preference == mQuickSettingsColumns) {
            int value = Integer.valueOf((String) newValue);
            int index = mQuickSettingsColumns.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver, Settings.System.QUICK_SETTINGS_COLUMNS, value);
            mQuickSettingsColumns.setSummary(mQuickSettingsColumns.getEntries()[index]);
            return true;
        } else if (preference == mQuickTilesBgColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QUICK_TILES_BG_COLOR,
                    intHex);
            //Helpers.restartSystemUI();
            //pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            //pm.goToSleep(SystemClock.uptimeMillis());
            return true;
        } else if (preference == mQuickTilesBgPressedColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QUICK_TILES_BG_PRESSED_COLOR,
                    intHex);
            //Helpers.restartSystemUI();
            //pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            //pm.goToSleep(SystemClock.uptimeMillis());
            return true;
        }
        return false;
    }

    private void updateSummary(String val, MultiSelectListPreference pref, int defSummary) {
        // Update summary message with current values
        final String[] values = parseStoredValue(val);
        if (values != null) {
            final int length = values.length;
            final CharSequence[] entries = pref.getEntries();
            StringBuilder summary = new StringBuilder();
            for (int i = 0; i < length; i++) {
                CharSequence entry = entries[Integer.parseInt(values[i])];
                if (i != 0) {
                    summary.append(" | ");
                }
                summary.append(entry);
            }
            pref.setSummary(summary);
        } else {
            pref.setSummary(defSummary);
        }
    }

    private void updatePulldownSummary(int value) {
        Resources res = getResources();

        if (value == 0) {
            /* quick pulldown deactivated */
            mQuickPulldown.setSummary(res.getString(R.string.quick_pulldown_off));
        } else if (value == 3) {
            mQuickPulldown.setSummary(res.getString(R.string.quick_pulldown_summary_always));
        } else {
            String direction = res.getString(value == 2
                    ? R.string.quick_pulldown_summary_left
                    : R.string.quick_pulldown_summary_right);
            mQuickPulldown.setSummary(res.getString(R.string.summary_quick_pulldown, direction));
        }
    }

    public static String[] parseStoredValue(CharSequence val) {
        if (TextUtils.isEmpty(val)) {
            return null;
        } else {
            return val.toString().split(SEPARATOR);
        }
    }
}
