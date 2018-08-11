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
import android.view.Menu;
import android.view.MenuItem;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.Objects;

import in.co.gorest.grblcontroller.events.BluetoothDisconnectEvent;
import in.co.gorest.grblcontroller.events.GrblSettingMessageEvent;
import in.co.gorest.grblcontroller.events.JogCommandEvent;
import in.co.gorest.grblcontroller.events.StreamingCompleteEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.service.FileStreamerIntentService;
import in.co.gorest.grblcontroller.service.GrblBluetoothSerialService;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class BluetoothConnectionActivity extends GrblActivity {

    private static final String TAG = BluetoothConnectionActivity.class.getSimpleName();

    private GrblServiceMessageHandler grblServiceMessageHandler;
    private BluetoothAdapter bluetoothAdapter = null;
    private boolean mBound = false;
    private String mConnectedDeviceName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) {
            grblToast(getString(R.string.text_no_bluetooth_adapter));
            restartInUsbMode();
        }else{
            Intent intent = new Intent(getApplicationContext(), GrblBluetoothSerialService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }

        grblServiceMessageHandler = new BluetoothConnectionActivity.GrblServiceMessageHandler(this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(grblBluetoothSerialService != null && grblBluetoothSerialService.getState() == GrblBluetoothSerialService.STATE_NONE && bluetoothAdapter.isEnabled() && sharedPref.getBoolean(getString(R.string.preference_auto_connect), false)){
                    String lastAddress = sharedPref.getString(getString(R.string.preference_last_connected_device), null);
                    connectToDevice(lastAddress);
                }
            }
        }, 1500);

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
                        EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_no_bluetooth_permission)));
                        restartInUsbMode();
                    }
                }
            };
            thread.start();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mBound){
            grblBluetoothSerialService.setMessageHandler(null);
            unbindService(serviceConnection);
            mBound = false;
        }

        stopService(new Intent(this, GrblBluetoothSerialService.class));
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        if(grblBluetoothSerialService != null) onBluetoothStateChange(grblBluetoothSerialService.getState());
    }

    private void restartInUsbMode(){
        sharedPref.edit().putString(getString(R.string.text_default_connection), Constants.SERIAL_CONNECTION_TYPE_USB_OTG).apply();
        startActivity(new Intent(this, UsbConnectionActivity.class));
        finish();
    }

    private void connectToDevice(String macAddress){
        if(macAddress == null){
            Intent serverIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
            startActivityForResult(serverIntent, Constants.CONNECT_DEVICE_INSECURE);
        }else{
            Intent intent = new Intent(getApplicationContext(), GrblBluetoothSerialService.class);
            intent.putExtra(GrblBluetoothSerialService.KEY_MAC_ADDRESS, macAddress);

            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1){
                getApplicationContext().startForegroundService(intent);
            }else{
                startService(intent);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem actionConnect = menu.findItem(R.id.action_connect);
        if(grblBluetoothSerialService != null){
            if(grblBluetoothSerialService.getState() == GrblBluetoothSerialService.STATE_CONNECTED){
                actionConnect.setIcon(new IconDrawable(this, FontAwesomeIcons.fa_bluetooth).colorRes(R.color.colorWhite).sizeDp(24));
            }else{
                actionConnect.setIcon(new IconDrawable(this, FontAwesomeIcons.fa_bluetooth_b).colorRes(R.color.colorWhite).sizeDp(24));
            }
        }else{
            actionConnect.setIcon(new IconDrawable(this, FontAwesomeIcons.fa_bluetooth_b).colorRes(R.color.colorWhite).sizeDp(24));
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case R.id.action_connect:
                if(bluetoothAdapter.isEnabled()){

                    if(grblBluetoothSerialService != null){
                        if(grblBluetoothSerialService.getState() == GrblBluetoothSerialService.STATE_CONNECTED){
                            new AlertDialog.Builder(this)
                                    .setTitle(R.string.text_disconnect)
                                    .setMessage(getString(R.string.text_disconnect_confirm))
                                    .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            if(grblBluetoothSerialService != null) grblBluetoothSerialService.disconnectService();
                                        }
                                    })
                                    .setNegativeButton(getString(R.string.text_cancel), null)
                                    .show();

                        }else{
                            Intent serverIntent = new Intent(this, DeviceListActivity.class);
                            startActivityForResult(serverIntent, Constants.CONNECT_DEVICE_INSECURE);
                        }
                    }else{
                        EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_bt_service_not_running)));
                    }

                }else{
                    grblToast(getString(R.string.text_bt_not_enabled));
                }
                return true;

            case R.id.action_grbl_reset:
                boolean resetConfirm = sharedPref.getBoolean(getString(R.string.preference_confirm_grbl_soft_reset), true);
                if(resetConfirm){
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.text_grbl_soft_reset)
                            .setMessage(R.string.text_grbl_soft_reset_desc)
                            .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if(FileStreamerIntentService.getIsServiceRunning()){
                                        FileStreamerIntentService.setShouldContinue(false);
                                        Intent intent = new Intent(getApplicationContext(), FileStreamerIntentService.class);
                                        stopService(intent);
                                    }
                                    vibrateLong();
                                    onGrblRealTimeCommandReceived(GrblUtils.GRBL_RESET_COMMAND);
                                }
                            })
                            .setNegativeButton(getString(R.string.text_cancel), null)
                            .show();

                }else{
                    vibrateLong();
                    onGrblRealTimeCommandReceived(GrblUtils.GRBL_RESET_COMMAND);
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            GrblBluetoothSerialService.GrblSerialServiceBinder binder = (GrblBluetoothSerialService.GrblSerialServiceBinder) service;
            grblBluetoothSerialService = binder.getService();
            mBound = true;
            grblBluetoothSerialService.setMessageHandler(grblServiceMessageHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private static class GrblServiceMessageHandler extends Handler {

        private final WeakReference<BluetoothConnectionActivity> mActivity;

        GrblServiceMessageHandler(BluetoothConnectionActivity activity){
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    mActivity.get().onBluetoothStateChange(msg.arg1);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    mActivity.get().mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    mActivity.get().grblToast(mActivity.get().getString(R.string.text_connected_to) + " " + mActivity.get().mConnectedDeviceName);
                    break;
                case Constants.MESSAGE_TOAST:
                    mActivity.get().grblToast(msg.getData().getString(Constants.TOAST));
                    break;
            }
        }
    }

    private void onBluetoothStateChange(int currentState){
        switch (currentState){
            case GrblBluetoothSerialService.STATE_CONNECTED:
                if(getSupportActionBar() != null) getSupportActionBar().setSubtitle((mConnectedDeviceName != null) ? mConnectedDeviceName : getString(R.string.text_connected));
                displayRewardVideoAd();
                Answers.getInstance().logCustom(new CustomEvent("Connection Type").putCustomAttribute("Connection", "Bluetooth"));
                invalidateOptionsMenu();
                break;
            case GrblBluetoothSerialService.STATE_CONNECTING:
                if(getSupportActionBar() != null) getSupportActionBar().setSubtitle(getString(R.string.text_connecting));
                break;
            case GrblBluetoothSerialService.STATE_LISTEN:
                break;
            case GrblBluetoothSerialService.STATE_NONE:
                EventBus.getDefault().post(new BluetoothDisconnectEvent(getString(R.string.text_connection_lost)));
                MachineStatusListener.getInstance().setState(Constants.MACHINE_STATUS_NOT_CONNECTED);
                if(getSupportActionBar() != null) getSupportActionBar().setSubtitle(getString(R.string.text_not_connected));
                invalidateOptionsMenu();
                break;
        }
    }

    @Override
    public void onGcodeCommandReceived(String command) {
        if(grblBluetoothSerialService != null) grblBluetoothSerialService.serialWriteString(command);
    }

    public void onGrblRealTimeCommandReceived(byte command) {
        if(grblBluetoothSerialService != null) grblBluetoothSerialService.serialWriteByte(command);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onJogCommandEvent(JogCommandEvent event){
        if(machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE) || machineStatus.getState().equals(Constants.MACHINE_STATUS_JOG)){
            if(machineStatus.getPlannerBuffer() > 5) onGcodeCommandReceived(event.getCommand());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGrblSettingMessageEvent(GrblSettingMessageEvent event){

        if(event.getSetting().equals("$10") && !event.getValue().equals("2")){
            onGcodeCommandReceived("$10=2");
        }

        if(event.getSetting().equals("$110") || event.getSetting().equals("$111") || event.getSetting().equals("$112")){
            Double maxFeedRate = Double.parseDouble(event.getValue());
            if(maxFeedRate > sharedPref.getDouble(getString(R.string.preference_jogging_max_feed_rate), machineStatus.getJogging().feed)){
                sharedPref.edit().putDouble(getString(R.string.preference_jogging_max_feed_rate), maxFeedRate).apply();
            }
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case Constants.CONNECT_DEVICE_SECURE:
            case Constants.CONNECT_DEVICE_INSECURE:
                if(resultCode == Activity.RESULT_OK){

                    try{
                        String macAddress = Objects.requireNonNull(data.getExtras()).getString(DeviceListActivity . EXTRA_DEVICE_ADDRESS);
                        if(grblBluetoothSerialService != null && bluetoothAdapter.isEnabled()){
                            sharedPref.edit().putString(getString(R.string.preference_last_connected_device), macAddress).apply();
                            connectToDevice(macAddress);
                        }

                    }catch (NullPointerException e){
                        Crashlytics.logException(e);
                    }
                }
        }
    }

}
