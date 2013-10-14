/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

//
// Performance Profile Related Settings
//
public class PerformanceProfile extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String PERF_PROFILE_PREF = "pref_perf_profile";

    public static final String SOB_PREF = "pref_perf_profile_set_on_boot";

    private class PerformanceProfileObserver extends ContentObserver {
        public PerformanceProfileObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            setCurrentValue();
        }
    }

    private String mPerfProfileDefaultEntry;

    private ListPreference mPerfProfilePref;

    private String[] mPerfProfileEntries;
    private String[] mPerfProfileValues;

    private ContentObserver mPerformanceProfileObserver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPerfProfileDefaultEntry = getString(
                com.android.internal.R.string.config_perf_profile_default_entry);
        mPerfProfileEntries = getResources().getStringArray(
                com.android.internal.R.array.perf_profile_entries);
        mPerfProfileValues = getResources().getStringArray(
                com.android.internal.R.array.perf_profile_values);

        addPreferencesFromResource(R.xml.perf_profile_settings);

        PreferenceScreen prefScreen = getPreferenceScreen();

        mPerformanceProfileObserver = new PerformanceProfileObserver(new Handler());

        mPerfProfilePref = (ListPreference) prefScreen.findPreference(PERF_PROFILE_PREF);
        mPerfProfilePref.setEntries(mPerfProfileEntries);
        mPerfProfilePref.setEntryValues(mPerfProfileValues);
        setCurrentValue();
        mPerfProfilePref.setOnPreferenceChangeListener(this);
    }

    private void setCurrentValue() {
        String value = getCurrentPerformanceProfile();
        mPerfProfilePref.setValue(value);
        setCurrentPerfProfileSummary();
    }

    @Override
    public void onResume() {
        super.onResume();
        setCurrentValue();
        ContentResolver resolver = getActivity().getContentResolver();
        resolver.registerContentObserver(Settings.System.getUriFor(
                Settings.System.PERFORMANCE_PROFILE), false, mPerformanceProfileObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        ContentResolver resolver = getActivity().getContentResolver();
        resolver.unregisterContentObserver(mPerformanceProfileObserver);
    }

    public void setCurrentPerfProfileSummary() {
        String value = getCurrentPerformanceProfile();
        String summary = "";
        int count = mPerfProfileValues.length;
        for (int i = 0; i < count; i++) {
            try {
                if (mPerfProfileValues[i].compareTo(value) == 0) {
                    summary = mPerfProfileEntries[i];
                }
            } catch (IndexOutOfBoundsException ex) {
                // Ignore
            }
        }
        mPerfProfilePref.setSummary(String.format("%s", summary));
    }

    private String getCurrentPerformanceProfile() {
        String value = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.PERFORMANCE_PROFILE);
        if (TextUtils.isEmpty(value)) {
            value = mPerfProfileDefaultEntry;
        }
        return value;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue != null) {
            if (preference == mPerfProfilePref) {
                Settings.System.putString(getActivity().getContentResolver(),
                        Settings.System.PERFORMANCE_PROFILE, String.valueOf(newValue));
                setCurrentPerfProfileSummary();
                return true;
            }
        }
        return false;
    }
}
