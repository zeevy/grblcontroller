
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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import java.util.Collections;
import java.util.Set;

import in.co.gorest.grblcontroller.events.UiToastEvent;


public class DeviceListActivity extends Activity {

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBtAdapter == null){
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_bluetooth_adapter_error), true, true));
            finish();
        }

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_device_list);
        setResult(Activity.RESULT_CANCELED);

        ArrayAdapter<String> pairedDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);

        ListView pairedListView = findViewById(R.id.paired_devices);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);


        Set<BluetoothDevice> pairedDevices = Collections.emptySet();
        try {
            pairedDevices = mBtAdapter.getBondedDevices();
        }catch (Exception e){
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_bluetooth_adaptor_error), true, true));
            finish();
        }

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            pairedDevicesArrayAdapter.add(getString(R.string.text_no_paired_devices));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        this.unregisterReceiver(mReceiver);
    }

    private final AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            mBtAdapter.cancelDiscovery();

            String info = ((TextView) v).getText().toString();

            if(info.length() > 16){
                String address = info.substring(info.length() - 17);
                Intent intent = new Intent();
                intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

                setResult(Activity.RESULT_OK, intent);
                finish();
            }

        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(getString(R.string.text_select_paired_device));
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    mNewDevicesArrayAdapter.add(getString(R.string.text_none_found));
                }
            }
        }
    };

}
