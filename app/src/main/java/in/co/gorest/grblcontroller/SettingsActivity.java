/*
 *  /**
 *  * Copyright (C) 2017  Grbl Controller Contributors
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation; either version 2 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *  * <http://www.gnu.org/licenses/>
 *
 */

package in.co.gorest.grblcontroller;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;

import org.greenrobot.eventbus.EventBus;

import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.model.Constants;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getSupportActionBar() != null) getSupportActionBar().setSubtitle(getString(R.string.text_application_settings));
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName(getString(R.string.shared_preference_key));
            addPreferencesFromResource(R.xml.application_pref);
        }

        @Override
        public void onResume() {
            super.onResume();
            String defaultConnectionType = getPreferenceManager().getSharedPreferences().getString(getString(R.string.preference_default_serial_connection_type), Constants.SERIAL_CONNECTION_TYPE_BLUETOOTH);
            if(defaultConnectionType.equals(Constants.SERIAL_CONNECTION_TYPE_USB_OTG)){
                getPreferenceScreen().findPreference(getString(R.string.preference_auto_connect)).setEnabled(false);
            }

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(key.equals(getString(R.string.preference_default_serial_connection_type))){
                EventBus.getDefault().post(new UiToastEvent("Application restart required"));
            }
        }

    }

}
