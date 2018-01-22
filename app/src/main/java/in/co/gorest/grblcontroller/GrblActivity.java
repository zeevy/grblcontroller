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
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;

import org.greenrobot.eventbus.EventBus;

import java.lang.reflect.Method;

import in.co.gorest.grblcontroller.events.BluetoothDisconnectEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.helpers.NotificationHelper;
import in.co.gorest.grblcontroller.listners.ConsoleLoggerListner;
import in.co.gorest.grblcontroller.listners.FileSenderListner;
import in.co.gorest.grblcontroller.listners.MachineStatusListner;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.service.FileStreamerIntentService;
import in.co.gorest.grblcontroller.service.GrblSerialService;
import in.co.gorest.grblcontroller.service.GrblSerialService.GrblSerialServiceBinder;
import in.co.gorest.grblcontroller.util.GrblUtils;

public abstract class GrblActivity extends AppCompatActivity {

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static final String TAG = GrblActivity.class.getSimpleName();
    public static boolean isAppRunning;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 11;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 12;

    private boolean mBound = false;
    private GrblSerialService grblSerialService = null;
    String lastToastMessage = null;
    private Toast toastMessage;

    private BluetoothAdapter bluetoothAdapter = null;

    private String mConnectedDeviceName = null;

    EnhancedSharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = EnhancedSharedPreferences.getInstance(GrblConttroller.getContext(), getString(R.string.shared_preference_key));

        NotificationHelper notificationHelper = new NotificationHelper(this);
        notificationHelper.createChannels();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) {

            grblToast(getString(R.string.text_no_bt_adapter));
            finish();
        }else{
            Intent intent = new Intent(getApplicationContext(), GrblSerialService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStart(){
        super.onStart();

        if(!bluetoothAdapter.isEnabled()){
            Thread thread = new Thread(){
                @Override
                public void run(){
                    try{
                        bluetoothAdapter.enable();
                    }catch (RuntimeException e){
                        EventBus.getDefault().post(new UiToastEvent(getString(R.string.no_bluetooth_permission)));
                        finish();
                    }
                }
            };
            thread.start();
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(grblSerialService != null && grblSerialService.getState() == GrblSerialService.STATE_NONE && bluetoothAdapter.isEnabled() && sharedPref.getBoolean(getString(R.string.auto_connect), false)){
                    String lastAddress = sharedPref.getString(getString(R.string.last_connected_device), null);
                    if(lastAddress == null){
                        Intent serverIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
                        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                    }else{
                        Intent intent = new Intent(getApplicationContext(), GrblSerialService.class);
                        intent.putExtra(GrblSerialService.KEY_MAC_ADDRESS, lastAddress);

                        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1){
                            getApplicationContext().startForegroundService(intent);
                        }else{
                            startService(intent);
                        }
                    }
                }
            }
        }, 1000);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mBound){
            grblSerialService.setMessageHandler(null);
            unbindService(serviceConnection);
            mBound = false;
        }

        stopService(new Intent(this, GrblSerialService.class));
        stopService(new Intent(this, FileStreamerIntentService.class));

        EventBus.getDefault().unregister(this);

        ConsoleLoggerListner.resetClass();
        FileSenderListner.resetClass();
        MachineStatusListner.resetClass();

        isAppRunning = false;
    }

    @Override
    public void onResume(){
        super.onResume();
        if(grblSerialService != null) onBluetoothStateChange(grblSerialService.getState());
    }

    @Override
    public void onBackPressed(){ moveTaskToBack(true); }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            GrblSerialServiceBinder binder = (GrblSerialServiceBinder) service;
            grblSerialService = binder.getService();
            mBound = true;
            grblSerialService.setMessageHandler(grblServiceMessageHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private final Handler grblServiceMessageHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    onBluetoothStateChange(msg.arg1);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    grblToast(getString(R.string.text_connected_to) + mConnectedDeviceName);
                    break;
                case Constants.MESSAGE_TOAST:
                    grblToast(msg.getData().getString(Constants.TOAST));
                    break;
            }
        }
    };

    private void onBluetoothStateChange(int currentState){
        switch (currentState){
            case GrblSerialService.STATE_CONNECTED:
                if(getSupportActionBar() != null) getSupportActionBar().setSubtitle((mConnectedDeviceName != null) ? mConnectedDeviceName : getString(R.string.text_connected));
                invalidateOptionsMenu();
                break;
            case GrblSerialService.STATE_CONNECTING:
                if(getSupportActionBar() != null) getSupportActionBar().setSubtitle(getString(R.string.text_connecting));
                break;
            case GrblSerialService.STATE_LISTEN:
            case GrblSerialService.STATE_NONE:
                EventBus.getDefault().post(new BluetoothDisconnectEvent(getString(R.string.text_connection_lost)));
                MachineStatusListner.getInstance().setState(MachineStatusListner.STATE_NOT_CONNECTED);
                if(getSupportActionBar() != null) getSupportActionBar().setSubtitle(getString(R.string.text_not_connected));
                invalidateOptionsMenu();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        if(menu != null){
            if(menu.getClass().getSimpleName().equals("MenuBuilder")){
                try{
                    Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                }
                catch(NoSuchMethodException e){
                    Log.e(TAG, "onMenuOpened", e);
                }
                catch(Exception e){
                    throw new RuntimeException(e);
                }
            }
        }

        MenuItem actionConnect = menu.findItem(R.id.action_connect);
        MenuItem actionGrblSoftReset = menu.findItem(R.id.action_grbl_reset);

        actionGrblSoftReset.setIcon(new IconDrawable(this, FontAwesomeIcons.fa_power_off).colorRes(R.color.colorWhite).sizeDp(24));

        if(grblSerialService != null){
            if(grblSerialService.getState() == GrblSerialService.STATE_CONNECTED){
                actionConnect.setIcon(new IconDrawable(this, FontAwesomeIcons.fa_bluetooth).colorRes(R.color.colorWhite).sizeDp(24));
            }else{
                actionConnect.setIcon(new IconDrawable(this, FontAwesomeIcons.fa_bluetooth_b).colorRes(R.color.colorWhite).sizeDp(24));
            }
        }else{
            actionConnect.setIcon(new IconDrawable(this, FontAwesomeIcons.fa_bluetooth_b).colorRes(R.color.colorWhite).sizeDp(24));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case R.id.action_connect:
                if(bluetoothAdapter.isEnabled()){

                    if(grblSerialService != null){
                        if(grblSerialService.getState() == GrblSerialService.STATE_CONNECTED){
                            new AlertDialog.Builder(this)
                                    .setTitle(R.string.text_disconnect)
                                    .setMessage(getString(R.string.text_disconnect_confirm))
                                    .setPositiveButton(getString(R.string.yes_confirm), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            if(grblSerialService != null) grblSerialService.disconnectService();
                                        }
                                    })
                                    .setNegativeButton(getString(R.string.cancel), null)
                                    .show();

                        }else{
                            Intent serverIntent = new Intent(this, DeviceListActivity.class);
                            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                        }
                    }else{
                        EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_bt_service_not_running)));
                    }

                }else{
                    grblToast(getString(R.string.text_bt_not_enabled));
                }
                return true;

            case R.id.action_grbl_reset:
                boolean resetConfirm = sharedPref.getBoolean(getString(R.string.confirm_grbl_soft_reset), true);
                if(resetConfirm){
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.text_grbl_soft_reset)
                            .setMessage(R.string.text_grbl_soft_reset_desc)
                            .setPositiveButton(getString(R.string.yes_confirm), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if(FileStreamerIntentService.getIsServiceRunning()){
                                        FileStreamerIntentService.setShouldContinue(false);
                                        Intent intent = new Intent(getApplicationContext(), FileStreamerIntentService.class);
                                        stopService(intent);
                                    }
                                    onGrblRealTimeCommandReceived(GrblUtils.GRBL_RESET_COMMAND);
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), null)
                            .show();

                }else{
                    onGrblRealTimeCommandReceived(GrblUtils.GRBL_RESET_COMMAND);
                }
                return true;

            case R.id.app_settings:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                return true;

            case R.id.app_about:
                startActivity(new Intent(getApplicationContext(), AboutActivity.class));
                return true;

            case R.id.share:

                try {
                    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");
                    String shareBodyText = "Grbl Controller. Very cool CNC controller for grbl firmware https://goo.gl/aVnvp4";

                    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,"Grbl Controller");
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBodyText);
                    startActivity(Intent.createChooser(sharingIntent, "Sharing Option"));
                }catch (ActivityNotFoundException e){
                    Crashlytics.logException(e);
                    grblToast("No application available to perform this action!");
                }

                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                if(resultCode == Activity.RESULT_OK) connectDevice(data, true);
                break;

            case REQUEST_CONNECT_DEVICE_INSECURE:
                if(resultCode == Activity.RESULT_OK) connectDevice(data, false);
                break;

        }
    }

    private void connectDevice(Intent data, boolean secure) {
        String address = data.getExtras().getString(DeviceListActivity . EXTRA_DEVICE_ADDRESS);
        if(grblSerialService != null && bluetoothAdapter.isEnabled()){
            Intent intent = new Intent(getApplicationContext(), GrblSerialService.class);
            intent.putExtra(GrblSerialService.KEY_MAC_ADDRESS, address);
            sharedPref.edit().putString(getString(R.string.last_connected_device), address).apply();
            startService(intent);
        }

    }

    protected void grblToast(String message){

        if(toastMessage == null){
            toastMessage = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
            toastMessage.setGravity(Gravity.FILL_HORIZONTAL|Gravity.TOP, 0, 120);
        }

        toastMessage.setText(message);
        toastMessage.show();
        this.lastToastMessage = message;
    }

    public void onGcodeCommandReceived(String command) {
        if(grblSerialService != null) grblSerialService.serialWriteString(command);
    }

    public void onGrblRealTimeCommandReceived(byte command) {
        if(grblSerialService != null) grblSerialService.serialWriteByte(command);
    }

}
