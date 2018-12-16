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

import android.app.IntentService;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.events.GrblErrorEvent;
import in.co.gorest.grblcontroller.events.GrblOkEvent;
import in.co.gorest.grblcontroller.events.StreamingCompleteEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.helpers.NotificationHelper;
import in.co.gorest.grblcontroller.listeners.FileSenderListener;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.model.GcodeCommand;
import in.co.gorest.grblcontroller.util.GrblUtils;


public class FileStreamerIntentService extends IntentService{

    private static final String TAG = FileStreamerIntentService.class.getSimpleName();

    public static final String CHECK_MODE_ENABLED = "CHECK_MODE_ENABLED";
    public static final String SERIAL_CONNECTION_TYPE = "SERIAL_CONNECTION_TYPE";
    private static int MAX_RX_SERIAL_BUFFER = Constants.DEFAULT_SERIAL_RX_BUFFER - 3;
    private static int CURRENT_RX_SERIAL_BUFFER = 0;

    private final LinkedList<Integer> activeCommandSizes = new LinkedList<>();
    private final BlockingQueue<Integer> completedCommands = new ArrayBlockingQueue<>(Constants.DEFAULT_SERIAL_RX_BUFFER);

    private static volatile boolean isServiceRunning = false;
    private static volatile boolean shouldContinue = true;

    public synchronized static boolean getIsServiceRunning(){ return isServiceRunning; }
    private synchronized static void setIsServiceRunning(boolean running){ isServiceRunning = running; }

    public synchronized static boolean getShouldContinue(){ return shouldContinue; }
    public synchronized static void setShouldContinue(boolean b){ shouldContinue = b; }

    private FileSenderListener fileSenderListener;
    private final Timer jobTimer = new Timer();

    public FileStreamerIntentService() {
        super(FileStreamerIntentService.class.getName());
    }

    @Override
    public void onCreate(){
        super.onCreate();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        clearBuffers();
        jobTimer.cancel();
        setIsServiceRunning(false);
        if(fileSenderListener != null) fileSenderListener.setStatus(FileSenderListener.STATUS_IDLE);
        stopForeground(true);
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onHandleIntent(Intent intent){
        fileSenderListener = FileSenderListener.getInstance();
        if(fileSenderListener.getGcodeFile() == null){
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_no_gcode_file_selected)));
            return;
        }

        MachineStatusListener.CompileTimeOptions compileTimeOptions = MachineStatusListener.getInstance().getCompileTimeOptions();
        if(compileTimeOptions.serialRxBuffer > 0) MAX_RX_SERIAL_BUFFER = compileTimeOptions.serialRxBuffer - 3;
        Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);

        boolean isCheckMode = intent.getBooleanExtra(CHECK_MODE_ENABLED, false);
        String defaultConnectionType = intent.getStringExtra(SERIAL_CONNECTION_TYPE);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = null;
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GrblController:WakeLockTag");
            wakeLock.acquire(24*60*60*1000);
        }


        clearBuffers();
        fileSenderListener.setRowsSent(0);
        fileSenderListener.setJobStartTime(System.currentTimeMillis());

        try {
            jobTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    int elapsedTimeSeconds = (int) (System.currentTimeMillis() - fileSenderListener.getJobStartTime())/1000;
                    fileSenderListener.setElapsedTime(String.format(Locale.US ,"%02d:%02d:%02d", elapsedTimeSeconds / 3600, (elapsedTimeSeconds % 3600) / 60, (elapsedTimeSeconds % 60)));
                }
            }, 0, 1000);
        }catch (IllegalStateException ignored){

        }

        fileSenderListener.setStatus(FileSenderListener.STATUS_STREAMING);
        setIsServiceRunning(true);

        if(isCheckMode){
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1){
                startForeground(Constants.FILE_STREAMING_NOTIFICATION_ID, getNotification(getString(R.string.text_file_checking_started), fileSenderListener.getGcodeFile().getName()));
            }

            if(defaultConnectionType != null && defaultConnectionType.equals(Constants.SERIAL_CONNECTION_TYPE_BLUETOOTH)){
                this.checkGcodeFile();
            }else{
                this.startStreaming(555);
            }
        }else{
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1){
                startForeground(Constants.FILE_STREAMING_NOTIFICATION_ID, getNotification(getString(R.string.text_file_streaming_started), fileSenderListener.getGcodeFile().getName()));
            }

            this.startStreaming(5);
        }

        this.waitUntilBufferRunOut(true);

        jobTimer.cancel();
        fileSenderListener.setJobEndTime(System.currentTimeMillis());
        fileSenderListener.setStatus(FileSenderListener.STATUS_IDLE);

        if(!getShouldContinue()){
            // Stop the spindle or laser
			EventBus.getDefault().post(new GcodeCommand("M5"));

        }

        clearBuffers();
        setIsServiceRunning(false);

        StreamingCompleteEvent streamingCompleteEvent = new StreamingCompleteEvent("Streaming Completed");
        streamingCompleteEvent.setFileName(fileSenderListener.getGcodeFileName());
        streamingCompleteEvent.setRowsSent(fileSenderListener.getRowsSent());
        streamingCompleteEvent.setTimeMillis(fileSenderListener.getJobEndTime() - fileSenderListener.getJobStartTime());
        streamingCompleteEvent.setTimeTaken(fileSenderListener.getElapsedTime());

        EventBus.getDefault().post(streamingCompleteEvent);

        if(wakeLock != null){
            try{
                wakeLock.release();
            }catch (RuntimeException ignored){}
        }

        stopSelf();
    }

    private void startStreaming(int statusUpdateInterval){

        BufferedReader br; String sCurrentLine;

        try{
            br = new BufferedReader(new FileReader(fileSenderListener.getGcodeFile()));
            int linesSent = 0;
            GcodeCommand gcodeCommand = new GcodeCommand();
            while ((sCurrentLine = br.readLine()) != null) {
                if(!shouldContinue) break;

                gcodeCommand.setCommand(sCurrentLine);
                if(gcodeCommand.getCommandString().length() > 0){

                    if(gcodeCommand.hasRomAccess()){
                        this.waitUntilBufferRunOut(true);
                        streamLine(gcodeCommand);
                        this.waitUntilBufferRunOut();
                        streamLine(new GcodeCommand(GrblUtils.GRBL_VIEW_PARSER_STATE_COMMAND));
                    }else{
                        streamLine(gcodeCommand);
                    }

                    linesSent++;
                }

                if(linesSent%statusUpdateInterval == 0){
                    fileSenderListener.setRowsSent(linesSent);
                }

            }

            br.close();
            fileSenderListener.setRowsSent(linesSent);

        }catch (IOException | NullPointerException e){
            Log.e(TAG, e.getMessage(), e);
        }

    }

    private void checkGcodeFile(){

        try{

            BufferedReader br = new BufferedReader(new FileReader(fileSenderListener.getGcodeFile()));
            int linesSent = 0;
            String sCurrentLine;
            GcodeCommand gcodeCommand = new GcodeCommand();

            while ((sCurrentLine = br.readLine()) != null) {
                if(!shouldContinue) break;
                gcodeCommand.setCommand(sCurrentLine);
                if(gcodeCommand.getCommandString().length() > 0){
                    EventBus.getDefault().post(gcodeCommand);
                    linesSent++;
                }
                if(linesSent%555 == 0) fileSenderListener.setRowsSent(linesSent);
            }
            br.close();

            fileSenderListener.setRowsSent(linesSent);

        }catch (IOException | NullPointerException e){
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void waitUntilBufferRunOut(){
        this.waitUntilBufferRunOut(false);
    }

    private void waitUntilBufferRunOut(boolean dwell){
        if(dwell) streamLine(new GcodeCommand("G4P0.01"));

        while(CURRENT_RX_SERIAL_BUFFER > 0){
            try {
                completedCommands.take();
                if(activeCommandSizes.size() > 0) CURRENT_RX_SERIAL_BUFFER -= activeCommandSizes.removeFirst();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                return;
            }
        }
    }

    private void streamLine(GcodeCommand gcodeCommand){

        String command = gcodeCommand.getCommandString();
        int commandSize = command.length() + 1;

        // Wait until there is room, if necessary.
        while (MAX_RX_SERIAL_BUFFER < (CURRENT_RX_SERIAL_BUFFER + commandSize)) {
            try {
                completedCommands.take();
                if(activeCommandSizes.size() > 0) CURRENT_RX_SERIAL_BUFFER -= activeCommandSizes.removeFirst();
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage(), e);
                return;
            }

            if(!shouldContinue) return;
        }

        if(shouldContinue){
            activeCommandSizes.offer(commandSize);
            CURRENT_RX_SERIAL_BUFFER += commandSize;
            EventBus.getDefault().post(gcodeCommand);
        }

    }

    private void clearBuffers(){
        CURRENT_RX_SERIAL_BUFFER = 0;
        if(activeCommandSizes.size() > 0) activeCommandSizes.clear();
        if(completedCommands.size() > 0) completedCommands.clear();
    }

    private Notification getNotification(String title, String message){
        return new NotificationCompat.Builder(getApplicationContext(), NotificationHelper.CHANNEL_SERVICE_ID)
                .setContentTitle(title)
                //.setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setAutoCancel(true).build();
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onGrblOkEvent(GrblOkEvent event){
        completedCommands.offer(1);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onGrblErrorEvent(GrblErrorEvent event){
        shouldContinue = false;
    }

}
