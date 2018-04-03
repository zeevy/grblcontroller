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

package in.co.gorest.grblcontroller.listeners;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import in.co.gorest.grblcontroller.events.ConsoleMessageEvent;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.service.GrblBluetoothSerialService;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class SerialBluetoothCommunicationHandler extends SerialCommunicationHandler {

    private final ExecutorService singleThreadExecutor;
    private ScheduledExecutorService grblStatusUpdater = null;

    private final WeakReference<GrblBluetoothSerialService> mService;

    public SerialBluetoothCommunicationHandler(GrblBluetoothSerialService grblBluetoothSerialService){
        mService = new WeakReference<>(grblBluetoothSerialService);
        singleThreadExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void handleMessage(Message msg){

        final GrblBluetoothSerialService grblBluetoothSerialService = mService.get();

        switch(msg.what){
            case Constants.MESSAGE_READ:
                if(msg.arg1 > 0){
                    final String message = (String) msg.obj;
                    if(!singleThreadExecutor.isShutdown()){
                        singleThreadExecutor.submit(new Runnable() {
                            @Override
                            public void run() {
                                onBluetoothSerialRead(message.trim(), grblBluetoothSerialService);
                            }
                        });
                    }
                }
                break;

            case Constants.MESSAGE_WRITE:
                final String message = (String) msg.obj;
                EventBus.getDefault().post(new ConsoleMessageEvent(message));
                break;
        }

    }

    private void onBluetoothSerialRead(String message, final GrblBluetoothSerialService grblBluetoothSerialService){

        boolean isVersionString = onSerialRead(message);
        if(isVersionString){
            GrblBluetoothSerialService.isGrblFound = true;

            Handler handler = new Handler(Looper.getMainLooper());

            long delayMillis = Constants.GRBL_STATUS_UPDATE_INTERVAL;
            for(final String startUpCommand: this.getStartUpCommands()){
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        grblBluetoothSerialService.serialWriteString(startUpCommand);
                    }
                }, delayMillis);

                delayMillis = delayMillis + Constants.GRBL_STATUS_UPDATE_INTERVAL;
            }

            startGrblStatusUpdateService(grblBluetoothSerialService);
        }

    }

    private void startGrblStatusUpdateService(final GrblBluetoothSerialService grblBluetoothSerialService){

        stopGrblStatusUpdateService();

        grblStatusUpdater = Executors.newScheduledThreadPool(1);
        grblStatusUpdater.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if(grblBluetoothSerialService.getState() == GrblBluetoothSerialService.STATE_CONNECTED) grblBluetoothSerialService.serialWriteByte(GrblUtils.GRBL_STATUS_COMMAND);
            }
        }, Constants.GRBL_STATUS_UPDATE_INTERVAL, Constants.GRBL_STATUS_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);

    }

    public void stopGrblStatusUpdateService(){
        if(grblStatusUpdater != null) grblStatusUpdater.shutdownNow();
    }

}
