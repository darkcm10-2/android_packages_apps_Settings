/*
 * Copyright (C) 2012 The CyanogenMod project
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

import android.app.INotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen; 
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.WindowManagerGlobal;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.Helpers; 

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemUiSettings extends SettingsPreferenceFragment  implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SystemSettings";

    private static final String KEY_NAVIGATION_BAR_HEIGHT = "navigation_bar_height";
    private static final String KEY_EXPANDED_DESKTOP = "expanded_desktop";
    private static final String KEY_EXPANDED_DESKTOP_NO_NAVBAR = "expanded_desktop_no_navbar";
    private static final String CATEGORY_NAVBAR = "navigation_bar";
    private static final String KEY_PIE_CONTROL = "pie_control";
    private static final String KEY_MMS_BREATH = "mms_breath";
    private static final String KEY_MISSED_CALL_BREATH = "missed_call_breath";
    private static final String KEY_CLEAR_RECENTS_POSITION = "clear_recents_position";
    private static final String KEY_USE_ALT_RESOLVER = "use_alt_resolver";
    private static final String KEY_LISTVIEW_ANIMATION = "listview_animation";
    private static final String KEY_LISTVIEW_INTERPOLATOR = "listview_interpolator";
    private static final String KEY_DUAL_PANEL = "force_dualpanel";

    private ListPreference mNavigationBarHeight;
    private PreferenceScreen mPieControl;
    private CheckBoxPreference mMMSBreath;
    private CheckBoxPreference mMissedCallBreath;
    private ListPreference mClearPosition;
    private CheckBoxPreference mUseAltResolver;
    private ListPreference mExpandedDesktopPref;
    private CheckBoxPreference mExpandedDesktopNoNavbarPref;
    private ListPreference mListViewAnimation;
    private ListPreference mListViewInterpolator;
    private CheckBoxPreference mDualPanel;

    private INotificationManager mNotificationManager;

    Preference mLcdDensity;
    int newDensityValue;
    DensityChanger densityFragment; 

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_ui_settings);
        PreferenceScreen prefScreen = getPreferenceScreen();

        mNotificationManager = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));

        mPieControl = (PreferenceScreen) findPreference(KEY_PIE_CONTROL);

        // Expanded desktop
        mExpandedDesktopPref = (ListPreference) findPreference(KEY_EXPANDED_DESKTOP);
        mExpandedDesktopNoNavbarPref =
                (CheckBoxPreference) findPreference(KEY_EXPANDED_DESKTOP_NO_NAVBAR);

        int expandedDesktopValue = Settings.System.getInt(getContentResolver(),
                Settings.System.EXPANDED_DESKTOP_STYLE, 0);

        //NavigationBarHeight - TODO make it depending on the user
        mNavigationBarHeight = (ListPreference) findPreference(KEY_NAVIGATION_BAR_HEIGHT);
        mNavigationBarHeight.setOnPreferenceChangeListener(this);

        int statusNavigationBarHeight = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                 Settings.System.NAVIGATION_BAR_HEIGHT, 48);
        mNavigationBarHeight.setValue(String.valueOf(statusNavigationBarHeight));
        mNavigationBarHeight.setSummary(mNavigationBarHeight.getEntry());

        try {
            // Only show the navigation bar category on devices that has a navigation bar
            // unless we are forcing it via development settings
            boolean forceNavbar = android.provider.Settings.System.getInt(getContentResolver(),
                    android.provider.Settings.System.DEV_FORCE_SHOW_NAVBAR, 0) == 1;
            boolean hasNavBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar()
                    || forceNavbar;

            // Hide no-op "Status bar visible" mode on devices without navigation bar
            if (hasNavBar) {
                mExpandedDesktopPref.setOnPreferenceChangeListener(this);
                mExpandedDesktopPref.setValue(String.valueOf(expandedDesktopValue));
                updateExpandedDesktop(expandedDesktopValue);
                prefScreen.removePreference(mExpandedDesktopNoNavbarPref);
            } else {
                mExpandedDesktopNoNavbarPref.setOnPreferenceChangeListener(this);
                mExpandedDesktopNoNavbarPref.setChecked(expandedDesktopValue > 0);
                prefScreen.removePreference(mExpandedDesktopPref);
            }

            // Hide navigation bar category on devices without navigation bar
            if (!hasNavBar) {
                prefScreen.removePreference(findPreference(CATEGORY_NAVBAR));
                mPieControl = null;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }

        int statusMMSBreath = Settings.System.getInt(getContentResolver(), Settings.System.MMS_BREATH, 1);

        mMMSBreath = (CheckBoxPreference) findPreference(KEY_MMS_BREATH);
        mMMSBreath.setOnPreferenceChangeListener(this);
        mMMSBreath.setChecked(statusMMSBreath > 0);

        int statusMissedCallBreath = Settings.System.getInt(getContentResolver(), Settings.System.MISSED_CALL_BREATH, 1);

        mMissedCallBreath = (CheckBoxPreference) findPreference(KEY_MISSED_CALL_BREATH);
        mMissedCallBreath.setOnPreferenceChangeListener(this);
        mMissedCallBreath.setChecked(statusMissedCallBreath > 0);

        mClearPosition = (ListPreference) findPreference(KEY_CLEAR_RECENTS_POSITION);
        int ClearSide = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.CLEAR_RECENTS_POSITION, 0);
        mClearPosition.setValue(String.valueOf(ClearSide));
        mClearPosition.setSummary(mClearPosition.getEntry());
        mClearPosition.setOnPreferenceChangeListener(this);

        mUseAltResolver = (CheckBoxPreference) findPreference(KEY_USE_ALT_RESOLVER);
        mUseAltResolver.setChecked(Settings.System.getBoolean(getActivity().getContentResolver(),
                Settings.System.ACTIVITY_RESOLVER_USE_ALT, false));
                
        //ListView Animations
        mListViewAnimation = (ListPreference) findPreference(KEY_LISTVIEW_ANIMATION);
        int listviewanimation = Settings.System.getInt(getActivity().getContentResolver(),
            Settings.System.LISTVIEW_ANIMATION, 0);
        mListViewAnimation.setValue(String.valueOf(listviewanimation));
        mListViewAnimation.setSummary(mListViewAnimation.getEntry());
        mListViewAnimation.setOnPreferenceChangeListener(this);

        mListViewInterpolator = (ListPreference) findPreference(KEY_LISTVIEW_INTERPOLATOR);
        int listviewinterpolator = Settings.System.getInt(getActivity().getContentResolver(),
            Settings.System.LISTVIEW_INTERPOLATOR, 0);
        mListViewInterpolator.setValue(String.valueOf(listviewinterpolator));
        mListViewInterpolator.setSummary(mListViewInterpolator.getEntry());
        mListViewInterpolator.setOnPreferenceChangeListener(this);

        mLcdDensity = findPreference("lcd_density_setup");
        String currentProperty = SystemProperties.get("ro.sf.lcd_density");
        try {
            newDensityValue = Integer.parseInt(currentProperty);
        } catch (Exception e) {
            getPreferenceScreen().removePreference(mLcdDensity);
        }

        mLcdDensity.setSummary(getResources().getString(R.string.current_lcd_density) + currentProperty);

        mDualPanel = (CheckBoxPreference) findPreference(KEY_DUAL_PANEL);
        mDualPanel.setChecked(Settings.System.getBoolean(getContentResolver(), Settings.System.FORCE_DUAL_PANEL, false));

    }

    @Override
    public void onResume() {
        super.onResume();
        updatePieControlSummary();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mExpandedDesktopPref) {
            int expandedDesktopValue = Integer.valueOf((String) objValue);
            updateExpandedDesktop(expandedDesktopValue);
            return true;
        } else if (preference == mExpandedDesktopNoNavbarPref) {
            boolean value = (Boolean) objValue;
            updateExpandedDesktop(value ? 2 : 0);
            return true;
        } else if (preference == mNavigationBarHeight) {
            int statusNavigationBarHeight = Integer.valueOf((String) objValue);
            int index = mNavigationBarHeight.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HEIGHT, statusNavigationBarHeight);
            mNavigationBarHeight.setSummary(mNavigationBarHeight.getEntries()[index]);
            return true;
        } else if (preference == mMMSBreath) {
            boolean value = (Boolean) objValue;
            Settings.System.putBoolean(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.MMS_BREATH, value);
            return true;
        } else if (preference == mMissedCallBreath) {
            boolean value = (Boolean) objValue;
            Settings.System.putBoolean(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.MISSED_CALL_BREATH, value);
            return true;
        } else if (preference == mClearPosition) {
            int side = Integer.valueOf((String) objValue);
            int index = mClearPosition.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.CLEAR_RECENTS_POSITION, side);
            mClearPosition.setSummary(mClearPosition.getEntries()[index]);
            return true;
        } else if (preference == mListViewAnimation) {
            int listviewanimation = Integer.valueOf((String) objValue);
            int index = mListViewAnimation.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LISTVIEW_ANIMATION,
                    listviewanimation);
            mListViewAnimation.setSummary(mListViewAnimation.getEntries()[index]);
            return true;
        } else if (preference == mListViewInterpolator) {
            int listviewinterpolator = Integer.valueOf((String) objValue);
            int index = mListViewInterpolator.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LISTVIEW_INTERPOLATOR,
                    listviewinterpolator);
            mListViewInterpolator.setSummary(mListViewInterpolator.getEntries()[index]);
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mUseAltResolver) {
            Settings.System.putBoolean(getActivity().getContentResolver(),
                    Settings.System.ACTIVITY_RESOLVER_USE_ALT,
                    mUseAltResolver.isChecked());
            return true;
        } else if (preference == mLcdDensity) {
            ((PreferenceActivity) getActivity())
            .startPreferenceFragment(new DensityChanger(), true);
            return true;
        } else if (preference == mDualPanel) {
            Settings.System.putBoolean(getContentResolver(),
                    Settings.System.FORCE_DUAL_PANEL, ((CheckBoxPreference) preference).isChecked());
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void updatePieControlSummary() {
        if (mPieControl != null) {
            boolean enabled = Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_CONTROLS, 0) != 0;

            if (enabled) {
                mPieControl.setSummary(R.string.pie_control_enabled);
            } else {
                mPieControl.setSummary(R.string.pie_control_disabled);
            }
        }
    }

    private void updateExpandedDesktop(int value) {
        ContentResolver cr = getContentResolver();
        Resources res = getResources();
        int summary = -1;

        Settings.System.putInt(cr, Settings.System.EXPANDED_DESKTOP_STYLE, value);

        if (value == 0) {
            // Expanded desktop deactivated
            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 0);
            Settings.System.putInt(cr, Settings.System.EXPANDED_DESKTOP_STATE, 0);
            summary = R.string.expanded_desktop_disabled;
        } else if (value == 1) {
            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            summary = R.string.expanded_desktop_status_bar;
        } else if (value == 2) {
            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            summary = R.string.expanded_desktop_no_status_bar;
        }

        if (mExpandedDesktopPref != null && summary != -1) {
            mExpandedDesktopPref.setSummary(res.getString(summary));
        }
    }
}
