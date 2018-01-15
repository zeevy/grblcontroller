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
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Locale;
import java.util.NoSuchElementException;
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
import in.co.gorest.grblcontroller.listners.FileSenderListner;
import in.co.gorest.grblcontroller.listners.MachineStatusListner;
import in.co.gorest.grblcontroller.model.GcodeCommand;
import in.co.gorest.grblcontroller.util.GrblUtils;


public class FileStreamerIntentService extends IntentService{

    private static final String TAG = FileStreamerIntentService.class.getSimpleName();

    public static final String CHECK_MODE_ENABLED = "CHECK_MODE_ENABLED";
    private static int MAX_RX_SERIAL_BUFFER = 125;
    private static int CURRENT_RX_SERIAL_BUFFER = 0;

    private final LinkedList<Integer> activeCommandSizes = new LinkedList<>();
    private final BlockingQueue<Integer> completedCommands = new ArrayBlockingQueue<>(64);

    private static volatile boolean isServiceRunning = false;
    private static volatile boolean shouldContinue = true;

    public synchronized static boolean getIsServiceRunning(){ return isServiceRunning; }
    private synchronized static void setIsServiceRunning(boolean running){ isServiceRunning = running; }

    public synchronized static boolean getShouldContinue(){ return shouldContinue; }
    public synchronized static void setShouldContinue(boolean b){ shouldContinue = b; }

    private FileSenderListner fileSenderListner;
    private final Timer jobTimer = new Timer();

    private static final int NOTIFICATION_ID = 101;

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
        stopForeground(true);
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onHandleIntent(Intent intent){
        fileSenderListner = FileSenderListner.getInstance();
        if(fileSenderListner.getGcodeFile() == null){
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.no_gcode_file_selected)));
            return;
        }

        MachineStatusListner.CompileTimeOptions compileTimeOptions = MachineStatusListner.getInstance().getCompileTimeOptions();
        if(compileTimeOptions.serialRxBuffer > 0) MAX_RX_SERIAL_BUFFER = compileTimeOptions.serialRxBuffer - 3;
        Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);

        boolean isCheckMode = intent.getBooleanExtra(CHECK_MODE_ENABLED, false);

        setIsServiceRunning(true);
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getString(R.string.grbl_file_streaming));
        wakeLock.acquire();

        clearBuffers();
        fileSenderListner.setRowsSent(0);
        fileSenderListner.setJobStartTime(System.currentTimeMillis());

        jobTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int elapsedTimeSeconds = (int) (System.currentTimeMillis() - fileSenderListner.getJobStartTime())/1000;
                fileSenderListner.setElaspsedTime(String.format(Locale.US ,"%02d:%02d:%02d", elapsedTimeSeconds / 3600, (elapsedTimeSeconds % 3600) / 60, (elapsedTimeSeconds % 60)));
            }
        }, 0, 1000);

        fileSenderListner.setStatus(FileSenderListner.STATUS_STREAMING);

        if(isCheckMode){
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1){
                startForeground(NOTIFICATION_ID, getNotification(getString(R.string.file_checking_started), fileSenderListner.getGcodeFile().getName()));
            }

            this.checkGcodeFile();
        }else{
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1){
                startForeground(NOTIFICATION_ID, getNotification(getString(R.string.file_streaming_started), fileSenderListner.getGcodeFile().getName()));
            }

            this.startStreaming();
        }

        this.waitUntilBufferRunout();

        jobTimer.cancel();
        fileSenderListner.setJobEndTime(System.currentTimeMillis());
        fileSenderListner.setStatus(FileSenderListner.STATUS_IDLE);

        clearBuffers();
        setIsServiceRunning(false);

        StreamingCompleteEvent streamingCompleteEvent = new StreamingCompleteEvent(getString(R.string.streaming_completed));
        streamingCompleteEvent.setFileName(fileSenderListner.getGcodeFileName());
        streamingCompleteEvent.setRowsSent(fileSenderListner.getRowsSent());
        streamingCompleteEvent.setTimeMillis(fileSenderListner.getJobEndTime() - fileSenderListner.getJobStartTime());
        streamingCompleteEvent.setTimeTaken(fileSenderListner.getElaspsedTime());

        EventBus.getDefault().post(streamingCompleteEvent);
        wakeLock.release();

        stopSelf();
    }

    private void startStreaming(){

        BufferedReader br; String sCurrentLine;

        try{
            br = new BufferedReader(new FileReader(fileSenderListner.getGcodeFile()));
            int linesSent = 0;
            GcodeCommand gcodeCommand = new GcodeCommand();
            while ((sCurrentLine = br.readLine()) != null) {
                if(!shouldContinue) break;

                gcodeCommand.setCommand(sCurrentLine);
                if(gcodeCommand.getCommandString().length() > 0){

                    if(gcodeCommand.hasModalSet()){
                        streamLine(gcodeCommand);
                        this.waitUntilBufferRunout();
                        streamLine(new GcodeCommand(GrblUtils.GRBL_VIEW_PARSER_STATE_COMMAND));
                        this.waitUntilBufferRunout();
                    }else{
                        streamLine(gcodeCommand);
                    }

                    linesSent++;
                }

                if(linesSent%5 == 0){
                    fileSenderListner.setRowsSent(linesSent);
                }

            }

            br.close();
            fileSenderListner.setRowsSent(linesSent);

        }catch (IOException | NullPointerException e){
            Log.e(TAG, e.getMessage(), e);
        }

    }

    private void checkGcodeFile(){


        try{

            BufferedReader br = new BufferedReader(new FileReader(fileSenderListner.getGcodeFile()));
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
                if(linesSent%555 == 0) fileSenderListner.setRowsSent(linesSent);
            }
            br.close();

            fileSenderListner.setRowsSent(linesSent);

        }catch (IOException | NullPointerException e){
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void waitUntilBufferRunout(){
        while(CURRENT_RX_SERIAL_BUFFER > 0){
            try {
                completedCommands.take();
                if(activeCommandSizes.size() > 0) CURRENT_RX_SERIAL_BUFFER -= activeCommandSizes.removeFirst();
            } catch (Exception e) {
                Crashlytics.logException(e);
                Log.e(TAG, e.getMessage(), e);
                return;
            }
        }
    }

    private void streamLine(GcodeCommand gcodeCommand){

        String command = gcodeCommand.getCommandString();
        int commandSize = command.length() + 1;
        if(commandSize <= 1) return;

        // Wait until there is room, if necessary.
        while (MAX_RX_SERIAL_BUFFER < (CURRENT_RX_SERIAL_BUFFER + command.length() + 1)) {
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
                .setContentText(message)
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
