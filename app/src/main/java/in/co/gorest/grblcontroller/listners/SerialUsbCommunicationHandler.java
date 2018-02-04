/*
 *
 *  *  /**
 *  *  * Copyright (C) 2017  Grbl Controller Contributors
 *  *  *
 *  *  * This program is free software; you can redistribute it and/or modify
 *  *  * it under the terms of the GNU General Public License as published by
 *  *  * the Free Software Foundation; either version 2 of the License, or
 *  *  * (at your option) any later version.
 *  *  *
 *  *  * This program is distributed in the hope that it will be useful,
 *  *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *  * GNU General Public License for more details.
 *  *  *
 *  *  * You should have received a copy of the GNU General Public License along
 *  *  * with this program; if not, write to the Free Software Foundation, Inc.,
 *  *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *  *  * <http://www.gnu.org/licenses/>
 *  *
 *
 */

package in.co.gorest.grblcontroller.listners;

import android.os.Message;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import in.co.gorest.grblcontroller.GrblConttroller;
import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.events.ConsoleMessageEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.listners.MachineStatusListner.BuildInfo;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.service.GrblUsbSerialService;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class SerialUsbCommunicationHandler extends SerialCommunicationHandler {

    private final ExecutorService singleThreadExecutor;
    private ScheduledExecutorService grblStatusUpdater = null;

    private final WeakReference<GrblUsbSerialService> mService;

    public SerialUsbCommunicationHandler(GrblUsbSerialService grblUsbSerialService){
        mService = new WeakReference<>(grblUsbSerialService);
        singleThreadExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void handleMessage(Message msg){

        final GrblUsbSerialService grblUsbSerialService = mService.get();

        switch(msg.what){
            case Constants.MESSAGE_READ:
                if(msg.arg1 > 0){
                    final String message = (String) msg.obj;
                    if(!singleThreadExecutor.isShutdown()){
                        singleThreadExecutor.submit(new Runnable() {
                            @Override
                            public void run() {
                                onBluetoothSerialRead(message.trim(), grblUsbSerialService);
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

    private void onBluetoothSerialRead(String message, GrblUsbSerialService grblUsbSerialService){
        if(onSerialRead(message)){
            double versionDouble =  GrblUtils.getVersionDouble(message);
            Character versionLetter = GrblUtils.getVersionLetter(message);

            BuildInfo buildInfo = new BuildInfo(versionDouble, versionLetter);
            machineStatus.setBuildInfo(buildInfo);

            if(machineStatus.getBuildInfo().versionDouble < Constants.MIN_SUPPORTED_VERSION){
                EventBus.getDefault().post(new UiToastEvent(GrblConttroller.getContext().getString(R.string.grbl_unsupported)));
            }else{
                GrblUsbSerialService.isGrblFound = true;
                EventBus.getDefault().post(new ConsoleMessageEvent(message));

                grblUsbSerialService.serialWriteString(GrblUtils.GRBL_BUILD_INFO_COMMAND);
                grblUsbSerialService.serialWriteString(GrblUtils.GRBL_VIEW_SETTINGS_COMMAND);
                grblUsbSerialService.serialWriteString(GrblUtils.GRBL_VIEW_PARSER_STATE_COMMAND);
                grblUsbSerialService.serialWriteString(GrblUtils.GRBL_VIEW_GCODE_PARAMETERS_COMMAND);
                startGrblStatusUpdateService(grblUsbSerialService);
            }
        }
    }

    private void startGrblStatusUpdateService(final GrblUsbSerialService grblUsbSerialService){
        stopGrblStatusUpdateService();

        grblStatusUpdater = Executors.newScheduledThreadPool(1);
        grblStatusUpdater.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                grblUsbSerialService.serialWriteByte(GrblUtils.GRBL_STATUS_COMMAND);
            }
        }, GRBL_STATUS_UPDATE_INTERVAL, GRBL_STATUS_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);

    }

    public void stopGrblStatusUpdateService(){
        if(grblStatusUpdater != null) grblStatusUpdater.shutdownNow();
    }

}
