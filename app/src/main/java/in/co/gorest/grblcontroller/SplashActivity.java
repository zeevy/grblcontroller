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

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.model.Constants;

public class SplashActivity extends AppCompatActivity {

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EnhancedSharedPreferences sharedPref = EnhancedSharedPreferences.getInstance(GrblController.getInstance(), getString(R.string.shared_preference_key));

        String defaultConnection = sharedPref.getString(getString(R.string.preference_default_serial_connection_type), Constants.SERIAL_CONNECTION_TYPE_BLUETOOTH);

        switch (defaultConnection){
            case Constants.SERIAL_CONNECTION_TYPE_BLUETOOTH:
                startActivity(new Intent(SplashActivity.this, BluetoothConnectionActivity.class));
                break;

            case Constants.SERIAL_CONNECTION_TYPE_USB_OTG:
                startActivity(new Intent(SplashActivity.this, UsbConnectionActivity.class));
                break;

            default:
                startActivity(new Intent(SplashActivity.this, BluetoothConnectionActivity.class));
        }

        finish();
    }


}
