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

package in.co.gorest.grblcontroller.listners;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import in.co.gorest.grblcontroller.GrblConttroller;
import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.events.ConsoleMessageEvent;
import in.co.gorest.grblcontroller.events.GrblAlarmEvent;
import in.co.gorest.grblcontroller.events.GrblErrorEvent;
import in.co.gorest.grblcontroller.events.GrblOkEvent;
import in.co.gorest.grblcontroller.events.GrblProbeEvent;
import in.co.gorest.grblcontroller.events.GrblSettingMessageEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.model.Position;
import in.co.gorest.grblcontroller.service.GrblSerialService;
import in.co.gorest.grblcontroller.service.SerialThreadService;
import in.co.gorest.grblcontroller.util.GrblLookups;
import in.co.gorest.grblcontroller.util.GrblUtils;
import in.co.gorest.grblcontroller.listners.MachineStatusListner.BuildInfo;

import static org.greenrobot.eventbus.EventBus.TAG;

public class SerialCommunicationHandler extends Handler {

    private final ExecutorService singleThreadExecutor;
    private final MachineStatusListner machineStatus;
    private ScheduledExecutorService grblStatusUpdater = null;
    private static final long GRBL_STATUS_UPDATE_INTERVAL = 250;

    private static GrblLookups GrblErrors;
    private static GrblLookups GrblAlarms;
    private static GrblLookups GrblSettings;

    private final WeakReference<GrblSerialService> mService;

    public SerialCommunicationHandler(GrblSerialService grblSerialService){

        mService = new WeakReference<>(grblSerialService);

        singleThreadExecutor = Executors.newSingleThreadExecutor();
        machineStatus = MachineStatusListner.getInstance();

        GrblAlarms = new GrblLookups(GrblConttroller.getContext(), "alarm_codes");
        GrblErrors = new GrblLookups(GrblConttroller.getContext(), "error_codes");
        GrblSettings = new GrblLookups(GrblConttroller.getContext(), "setting_codes");

    }

    @Override
    public void handleMessage(Message msg){

        final GrblSerialService grblSerialService = mService.get();

        switch(msg.what){
            case Constants.MESSAGE_READ:
                if(msg.arg1 > 0){
                    final String message = (String) msg.obj;
                    if(!singleThreadExecutor.isShutdown()){
                        singleThreadExecutor.submit(new Runnable() {
                            @Override
                            public void run() {
                                onBluetoothSerialRead(message.trim(), grblSerialService);
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

    private void onBluetoothSerialRead(String message, GrblSerialService grblSerialService){

        if(GrblUtils.isGrblOkMessage(message)){
            if(!machineStatus.getState().equals(MachineStatusListner.STATE_CHECK)) EventBus.getDefault().post(new GrblOkEvent(message));

        }else if(GrblUtils.isGrblStatusString(message)){
            this.updateMachineStatus(message);
            if(machineStatus.getVerboseOutput()) EventBus.getDefault().post(new ConsoleMessageEvent(message));

        }else if(GrblUtils.isGrblAlarmMessage(message)){
            GrblAlarmEvent alarmEvent = new GrblAlarmEvent(GrblAlarms, message);
            machineStatus.setState(MachineStatusListner.STATE_ALARM);
            EventBus.getDefault().post(alarmEvent);
            EventBus.getDefault().post(new UiToastEvent(alarmEvent.getAlarmDescription()));

        }else if(GrblUtils.isGrblFeedbackMessage(message)){
            EventBus.getDefault().post(new ConsoleMessageEvent(message));

        }else if(GrblUtils.isGrblErrorMessage(message)) {
            GrblErrorEvent errorEvent = new GrblErrorEvent(GrblErrors, message);
            EventBus.getDefault().post(errorEvent);
            EventBus.getDefault().post(new UiToastEvent(errorEvent.getErrorDescription()));

        }else if(GrblUtils.isGrblProbeMessage(message)){
            String probeString = GrblUtils.getProbeString(message);
            if(probeString != null){
                EventBus.getDefault().post(new GrblProbeEvent(probeString));
            }
            EventBus.getDefault().post(new ConsoleMessageEvent(message));

        }else if(GrblUtils.isGrblSettingMessage(message)){
            GrblSettingMessageEvent settingMessageEvent = new GrblSettingMessageEvent(GrblSettings, message);
            EventBus.getDefault().post(settingMessageEvent);
            EventBus.getDefault().post(new ConsoleMessageEvent(settingMessageEvent.toString()));

        }else if(GrblUtils.isGrblVersionString(message)) {

            double versionDouble =  GrblUtils.getVersionDouble(message);
            Character versionLetter = GrblUtils.getVersionLetter(message);

            BuildInfo buildInfo = new BuildInfo(versionDouble, versionLetter);
            machineStatus.setBuildInfo(buildInfo);

            if(machineStatus.getBuildInfo().versionDouble < 1.1){
                EventBus.getDefault().post(new UiToastEvent(GrblConttroller.getContext().getString(R.string.grbl_unsupported)));
                grblSerialService.disconnectService();
            }else{
                SerialThreadService.isGrblFound = true;
                EventBus.getDefault().post(new ConsoleMessageEvent(message));

                grblSerialService.serialWriteString(GrblUtils.GRBL_BUILD_INFO_COMMAND);
                grblSerialService.serialWriteString(GrblUtils.GRBL_VIEW_SETTINGS_COMMAND);
                grblSerialService.serialWriteString(GrblUtils.GRBL_VIEW_PARSER_STATE_COMMAND);
                grblSerialService.serialWriteString(GrblUtils.GRBL_VIEW_GCODE_PARAMETERS_COMMAND);
                startGrblStatusUpdateService(grblSerialService);
            }


        }else if(GrblUtils.isBuildOptionsMessage(message)) {
            String buildOptions = GrblUtils.getBuildOptionString(message);
            machineStatus.setCompileTimeOptions(GrblUtils.getCompileTimeOptionsFromString(buildOptions));
            EventBus.getDefault().post(new ConsoleMessageEvent(message));

        }else if(GrblUtils.isParserStateMessage(message)) {
            String parserStateString = GrblUtils.getParserStateString(message);
            String[] parts = parserStateString.split("\\s+");
            if (parts.length >= 6) {
                machineStatus.setParserState(parserStateString);
                EventBus.getDefault().post(new ConsoleMessageEvent(message));
            }

        }else if(GrblUtils.isGrblToolLengthOffsetMessage(message)){
            machineStatus.setToolLengthOffset(GrblUtils.getToolLengthOffset(message));
            EventBus.getDefault().post(new ConsoleMessageEvent(message));
        }else{
            EventBus.getDefault().post(new ConsoleMessageEvent(message));
            Log.d(TAG, "MESSAGE NOT HANDLED: " + message);
        }

    }

    private void startGrblStatusUpdateService(final GrblSerialService grblSerialService){

        stopGrblStatusUpdateService();

        grblStatusUpdater = Executors.newScheduledThreadPool(1);
        grblStatusUpdater.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if(grblSerialService.getState() == SerialThreadService.STATE_CONNECTED) grblSerialService.serialWriteByte(GrblUtils.GRBL_STATUS_COMMAND);
            }
        }, GRBL_STATUS_UPDATE_INTERVAL, GRBL_STATUS_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);

    }

    public void stopGrblStatusUpdateService(){
        if(grblStatusUpdater != null) grblStatusUpdater.shutdownNow();
    }


    private synchronized void updateMachineStatus(String statusMessage){

        Position MPos = null;
        Position WPos = null;
        Position WCO = null;
        Boolean hasOverrides = false;
        Boolean enabledPinsChanged = false;
        Boolean accessoryStatesChanged = false;

        for (String part : statusMessage.substring(0, statusMessage.length()-1).split("\\|")) {

            if(part.startsWith("<")) {
                int idx = part.indexOf(':');
                machineStatus.setState((idx == -1) ? part.substring(1) : part.substring(1, idx));
            }else if (part.startsWith("MPos:")) {
                MPos = GrblUtils.getPositionFromStatusString(statusMessage, GrblUtils.machinePattern);

            }else if (part.startsWith("WPos:")) {
                WPos = GrblUtils.getPositionFromStatusString(statusMessage, GrblUtils.workPattern);

            }else if (part.startsWith("WCO:")) {
                WCO = GrblUtils.getPositionFromStatusString(statusMessage, GrblUtils.wcoPattern);

            }else if(part.startsWith("Bf:")){
                String[] bufferStateParts = part.substring(3).trim().split(",");
                if(bufferStateParts.length == 2){
                    int planerBuffer = Integer.parseInt(bufferStateParts[0]);
                    int serialBuffer = Integer.parseInt(bufferStateParts[1]);
                    machineStatus.setPlannerBuffer(planerBuffer);
                    machineStatus.setSerialRxBuffer(serialBuffer);
                }

            }else if (part.startsWith("Ov:")) {
                String[] overrideParts = part.substring(3).trim().split(",");
                if (overrideParts.length == 3) {
                    machineStatus.setOverridePercents(Integer.parseInt(overrideParts[0]), Integer.parseInt(overrideParts[1]), Integer.parseInt(overrideParts[2]));
                }

                hasOverrides = true;

            }else if (part.startsWith("F:")) {
                machineStatus.setFeedRate(Double.parseDouble(part.substring(2)));

            }else if (part.startsWith("FS:")) {
                String[] parts = part.substring(3).split(",");
                machineStatus.setFeedRate(Double.parseDouble(parts[0]));
                machineStatus.setSpindleSpeed(Double.parseDouble(parts[1]));

            }else if (part.startsWith("Pn:")) {
                String value = part.substring(part.indexOf(':')+1);
                machineStatus.setEnabledPins(value);
                enabledPinsChanged = true;

            }else if (part.startsWith("A:")) {
                String value = part.substring(part.indexOf(':')+1);
                machineStatus.setAccessoryStates(value);
                accessoryStatesChanged = true;
            }
        }

        if(WCO == null){
            if(machineStatus.getWorkCoordsOffset() != null){
                WCO = machineStatus.getWorkCoordsOffset();
            } else {
                WCO = new Position(0,0,0);
            }
        }

        if(WPos == null && MPos != null) WPos = new Position(MPos.getCordX() - WCO.getCordX(), MPos.getCordY() - WCO.getCordY(), MPos.getCordZ() - WCO.getCordZ());
        if(MPos == null && WPos != null) MPos = new Position(WPos.getCordX() + WCO.getCordX(), WPos.getCordY() + WCO.getCordY(), WPos.getCordZ() + WCO.getCordZ());

        machineStatus.setMachinePosition(MPos);
        machineStatus.setWorkPosition(WPos);
        machineStatus.setWorkCoordsOffset(WCO);
        if(!enabledPinsChanged) machineStatus.setEnabledPins("");

        if(hasOverrides){
            if(!accessoryStatesChanged) machineStatus.setAccessoryStates("");
        }
    }


}
