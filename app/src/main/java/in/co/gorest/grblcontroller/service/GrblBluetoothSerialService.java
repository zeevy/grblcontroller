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


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.events.GrblRealTimeCommandEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.helpers.NotificationHelper;
import in.co.gorest.grblcontroller.listeners.SerialBluetoothCommunicationHandler;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.model.GcodeCommand;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class GrblBluetoothSerialService extends Service{

    private static final String TAG = GrblBluetoothSerialService.class.getSimpleName();
    public static final String KEY_MAC_ADDRESS = "KEY_MAC_ADDRESS";

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "HC-05";
    private static final String NAME_INSECURE = "HC-05";
    private static final byte[] BYTE_NEWLINE = { 0x0A };

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothAdapter mAdapter;
    Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private int mNewState;

    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    public static volatile boolean isGrblFound = false;

    SerialBluetoothCommunicationHandler serialBluetoothCommunicationHandler;
    private final IBinder mBinder = new GrblSerialServiceBinder();

    private long statusUpdatePoolInterval = Constants.GRBL_STATUS_UPDATE_INTERVAL;

    @Override
    public void onCreate(){
        super.onCreate();
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        mState = STATE_NONE;
        mNewState = mState;

        if(mAdapter == null){
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_bluetooth_adapter_error), true, true));
            stopSelf();
        }else{
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1){
                startForeground(Constants.BLUETOOTH_SERVICE_NOTIFICATION_ID, this.getNotification(null));
            }

            serialBluetoothCommunicationHandler = new SerialBluetoothCommunicationHandler(this);
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return mBinder; }

    public class GrblSerialServiceBinder extends Binder {
        public GrblBluetoothSerialService getService() {
            return GrblBluetoothSerialService.this;
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
                    this.connect(device);
                }catch(IllegalArgumentException e){
                    EventBus.getDefault().post(new UiToastEvent(e.getMessage(), true, true));
                    disconnectService();
                    stopSelf();
                }
            }
        }else{
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_unknown_error), true, true));
            disconnectService();
            stopSelf();
        }

        return Service.START_NOT_STICKY;
    }

    public void disconnectService(){
        serialBluetoothCommunicationHandler.stopGrblStatusUpdateService();
        this.stop();
        isGrblFound = false;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        disconnectService();
        mState = STATE_NONE;
        this.stop();
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1){
            stopForeground(true);
        }
        updateUserInterfaceTitle();
        EventBus.getDefault().unregister(this);
    }

    private synchronized void updateUserInterfaceTitle() {
        mState = getState();
        mNewState = mState;
        if(mHandler != null){
            mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
        }
    }

    public synchronized int getState() {
        return mState;
    }

    synchronized void start() {
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
        // Update UI title
        updateUserInterfaceTitle();
    }

    synchronized void connect(BluetoothDevice device) {

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, false);
        mConnectThread.start();
        // Update UI title
        updateUserInterfaceTitle();
    }

    @SuppressLint("MissingPermission")
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        if(mHandler != null){
            Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.DEVICE_NAME, device.getName());
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
        updateUserInterfaceTitle();

        try {
            wait(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(!isGrblFound) serialWriteByte(GrblUtils.GRBL_RESET_COMMAND);
    }

    synchronized void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        mState = STATE_NONE;
        updateUserInterfaceTitle();
    }

    private void connectionFailed() {
        // Send a failure message back to the Activity
        if(mHandler != null){
            Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.TOAST, getString(R.string.text_unable_to_connect_to_device));
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }

        mState = STATE_NONE;
        updateUserInterfaceTitle();

        this.start();
    }

    private void connectionLost() {
        // Send a failure message back to the Activity
        if(mHandler != null){
            Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.TOAST, getString(R.string.text_device_connection_was_lost));
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        this.start();
    }

    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private final String mSocketType;

        @SuppressLint("MissingPermission")
        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException | NullPointerException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
            mState = STATE_LISTEN;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + mSocketType + "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException | NullPointerException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                connected(socket, socket.getRemoteDevice(), mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException |NullPointerException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException | NullPointerException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final String mSocketType;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                }
            } catch (IOException | NullPointerException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        @SuppressLint("MissingPermission")
        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException | NullPointerException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException | NullPointerException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +" socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException | NullPointerException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        StringBuilder rxBuffer = new StringBuilder();

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException | NullPointerException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(mmInStream))){
                    String readMessage;
                    while((readMessage = reader.readLine()) != null) {
                        serialBluetoothCommunicationHandler.obtainMessage(Constants.MESSAGE_READ, readMessage.length(), -1, readMessage).sendToTarget();
                    }
                } catch(IOException | NullPointerException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException | NullPointerException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException | NullPointerException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private void serialWriteBytes(byte[] b) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.write(b);
    }

    public void serialWriteString(String s){
        byte[] buffer = s.getBytes();
        this.serialWriteBytes(buffer);
        this.serialWriteBytes(BYTE_NEWLINE);
        serialBluetoothCommunicationHandler.obtainMessage(Constants.MESSAGE_WRITE, s.length(), -1, s).sendToTarget();
    }

    public void serialWriteByte(byte b){
        byte[] c = {b};
        serialWriteBytes(c);
    }

    private Notification getNotification(String message){

        if(message == null) message = getString(R.string.text_bluetooth_service_foreground_message);

        return new NotificationCompat.Builder(getApplicationContext(), NotificationHelper.CHANNEL_SERVICE_ID)
                .setContentTitle(getString(R.string.text_bluetooth_service))
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setAutoCancel(true).build();
    }

    public long getStatusUpdatePoolInterval(){
        return this.statusUpdatePoolInterval;
    }

    public void setStatusUpdatePoolInterval(long poolInterval){
        this.statusUpdatePoolInterval = poolInterval;
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onGrblGcodeSendEvent(GcodeCommand event){
        serialWriteString(event.getCommandString());
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onGrblRealTimeCommandEvent(GrblRealTimeCommandEvent grblRealTimeCommandEvent){
        serialWriteByte(grblRealTimeCommandEvent.getCommand());
    }

}
