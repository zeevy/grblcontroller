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

package in.co.gorest.grblcontroller.service;


import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.events.GrblRealTimeCommandEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.listners.SerialCommunicationHandler;
import in.co.gorest.grblcontroller.model.GcodeCommand;

public class GrblSerialService extends SerialThreadService{

    private static final String TAG = GrblSerialService.class.getSimpleName();
    public static final String KEY_MAC_ADDRESS = "KEY_MAC_ADDRESS";

    private final IBinder mBinder = new GrblSerialServiceBinder();

    @Override
    public void onCreate(){
        super.onCreate();
        serialCommunicationHandler = new SerialCommunicationHandler(this);
        EventBus.getDefault().register(this);
    }

    @Override
    public IBinder onBind(Intent intent) { return mBinder; }

    public class GrblSerialServiceBinder extends Binder {
        public GrblSerialService getService() {
            return GrblSerialService.this;
        }
    }

    public void setMessageHandler(Handler grblServiceMessageHandler){
        this.mHandler = grblServiceMessageHandler;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.start();

        if(intent != null){
            String deviceAddress = intent.getStringExtra(KEY_MAC_ADDRESS);
            if(deviceAddress != null){
                try{
                    BluetoothDevice device = mAdapter.getRemoteDevice(deviceAddress.toUpperCase());
                    this.connect(device, false);
                }catch(RuntimeException e){
                    EventBus.getDefault().post(new UiToastEvent(e.getMessage()));
                    disconnectService();
                    stopSelf();
                }
            }
        }else{
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.unknown_error)));
            disconnectService();
            stopSelf();
        }

        return Service.START_NOT_STICKY;
    }

    public void disconnectService(){
        serialCommunicationHandler.stopGrblStatusUpdateService();
        this.stop();
        isGrblFound = false;
    }

    @Override
    public void onDestroy(){
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onGrblGcodeSendEvent(GcodeCommand event){
        serialWriteString(event.getCommandString());
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onGrblRelatimeCommandEvent(GrblRealTimeCommandEvent grblRealTimeCommandEvent){
        serialWriteByte(grblRealTimeCommandEvent.getCommand());
    }

}
