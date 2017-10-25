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
import android.content.Intent;
import android.os.Process;
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
import in.co.gorest.grblcontroller.events.GrblGcodeCommandEvent;
import in.co.gorest.grblcontroller.events.GrblOkEvent;
import in.co.gorest.grblcontroller.events.StreamingCompleteEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.listners.FileSenderListner;
import in.co.gorest.grblcontroller.listners.MachineStatusListner;


public class FileStreamerIntentService extends IntentService{

    private static final String TAG = FileStreamerIntentService.class.getSimpleName();

    public static final String CHECK_MODE_ENABLED = "CHECK_MODE_ENABLED";
    public static final String FASTER_CHECK_MODE_ENABLED = "FASTER_CHECK_MODE_ENABLED";
    private static int MAX_RX_SERIAL_BUFFER = 125;
    private static int CURRENT_RX_SERIAL_BUFFER = 0;

    private final LinkedList<Integer> activeCommandSizes = new LinkedList<>();
    private final BlockingQueue<Integer> completedCommands = new ArrayBlockingQueue<>(1024);

    private static volatile boolean isServiceRunning = false;
    private static volatile boolean shouldContinue = true;

    public synchronized static boolean getIsServiceRunning(){ return isServiceRunning; }
    private synchronized static void setIsServiceRunning(boolean running){ isServiceRunning = running; }

    public synchronized static boolean getShouldContinue(){ return shouldContinue; }
    public synchronized static void setShouldContinue(boolean b){ shouldContinue = b; }

    private FileSenderListner fileSenderListner;
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
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

        boolean isCheckMode = intent.getBooleanExtra(CHECK_MODE_ENABLED, false);

        setIsServiceRunning(true);

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

        if(isCheckMode){
            fastFileCheck();
        }else{
            streamFile();
        }

        jobTimer.cancel();
        fileSenderListner.setJobEndTime(System.currentTimeMillis());

        clearBuffers();
        setIsServiceRunning(false);

        StreamingCompleteEvent streamingCompleteEvent = new StreamingCompleteEvent("Streaming Completed");
        streamingCompleteEvent.setFileName(fileSenderListner.getGcodeFileName());
        streamingCompleteEvent.setRowsSent(fileSenderListner.getRowsSent());
        streamingCompleteEvent.setTimeMillis(fileSenderListner.getJobEndTime() - fileSenderListner.getJobStartTime());
        streamingCompleteEvent.setTimeTaken(fileSenderListner.getElaspsedTime());

        EventBus.getDefault().post(streamingCompleteEvent);

    }

    private void fastFileCheck(){
        BufferedReader br; String sCurrentLine;
        try{
            br = new BufferedReader(new FileReader(fileSenderListner.getGcodeFile()));
            int linesSent = 0;
            while ((sCurrentLine = br.readLine()) != null) {
                if(!shouldContinue) break;
                EventBus.getDefault().post(new GrblGcodeCommandEvent(sCurrentLine));
                linesSent++;
                if(linesSent%100 == 0) fileSenderListner.setRowsSent(linesSent);
            }
            br.close();

            fileSenderListner.setRowsSent(linesSent);

        }catch (IOException | NullPointerException e){
            Log.e(TAG, e.getMessage(), e);
        }

    }

    private void streamFile(){
        BufferedReader br; String sCurrentLine;
        try{
            br = new BufferedReader(new FileReader(fileSenderListner.getGcodeFile()));
            int linesSent = 0;
            while ((sCurrentLine = br.readLine()) != null) {
                if(!shouldContinue) break;

                streamLine(sCurrentLine);
                linesSent++;
                if(linesSent%5 == 0) fileSenderListner.setRowsSent(linesSent);
            }
            br.close();

            fileSenderListner.setRowsSent(linesSent);

        }catch (IOException | NullPointerException e){
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void streamLine(String s){

        // Wait until there is room, if necessary.
        while (MAX_RX_SERIAL_BUFFER < (CURRENT_RX_SERIAL_BUFFER + s.length() + 1)) {
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
            int commandSize = s.length() + 1;
            activeCommandSizes.offer(commandSize);
            CURRENT_RX_SERIAL_BUFFER += commandSize;

            EventBus.getDefault().post(new GrblGcodeCommandEvent(s));
        }

    }

    private void clearBuffers(){
        CURRENT_RX_SERIAL_BUFFER = 0;
        if(activeCommandSizes.size() > 0) activeCommandSizes.clear();
        if(completedCommands.size() > 0) completedCommands.clear();
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
