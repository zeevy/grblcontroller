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

package in.co.gorest.grblcontroller.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.crashlytics.android.Crashlytics;
import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.Map;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.events.GrblRealTimeCommandEvent;
import in.co.gorest.grblcontroller.helpers.NotificationHelper;
import in.co.gorest.grblcontroller.listeners.SerialUsbCommunicationHandler;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.model.GcodeCommand;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class GrblUsbSerialService extends Service {

    private final String TAG = GrblUsbSerialService.class.getSimpleName();

    public static final String ACTION_USB_READY = "com.felhr.connectivityservices.USB_READY";
    public static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    public static final String ACTION_USB_NOT_SUPPORTED = "com.felhr.usbservice.USB_NOT_SUPPORTED";
    public static final String ACTION_NO_USB = "com.felhr.usbservice.NO_USB";
    public static final String ACTION_USB_PERMISSION_GRANTED = "com.felhr.usbservice.USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "com.felhr.usbservice.USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_USB_DISCONNECTED = "com.felhr.usbservice.USB_DISCONNECTED";
    public static final String ACTION_CDC_DRIVER_NOT_WORKING = "com.felhr.connectivityservices.ACTION_CDC_DRIVER_NOT_WORKING";
    public static final String ACTION_USB_DEVICE_NOT_WORKING = "com.felhr.connectivityservices.ACTION_USB_DEVICE_NOT_WORKING";
    public static final int MESSAGE_FROM_SERIAL_PORT = 0;
    public static final int CTS_CHANGE = 1;
    public static final int DSR_CHANGE = 2;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final int BAUD_RATE = 115200; // BaudRate. Change this value if you need
    public static boolean SERVICE_CONNECTED = false;
    private static final byte[] BYTE_NEW_LINE = { 0x0A };

    private final IBinder binder = new UsbSerialBinder();

    private Context context;
    private Handler mHandler;
    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;

    private boolean serialPortConnected;
    public static volatile boolean isGrblFound = false;

    private SerialUsbCommunicationHandler serialUsbCommunicationHandler;

    /*
     * onCreate will be executed when service is started. It configures an IntentFilter to listen for
     * incoming Intents (USB ATTACHED, USB DETACHED...) and it tries to open a serial port.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        this.context = this;
        serialPortConnected = false;
        GrblUsbSerialService.SERVICE_CONNECTED = true;

        setFilter();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        findSerialPortDevice();

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1){
            startForeground(Constants.USB_OTG_SERVICE_NOTIFICATION_ID, this.getNotification(null));
        }

        serialUsbCommunicationHandler = new SerialUsbCommunicationHandler(this);
        EventBus.getDefault().register(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(usbReceiver);
        GrblUsbSerialService.isGrblFound = false;
        GrblUsbSerialService.SERVICE_CONNECTED = false;

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1){
            stopForeground(true);
        }

        EventBus.getDefault().unregister(this);
    }

    public void serialWriteByte(byte b){
        byte[] c = {b};
        serialWriteBytes(c);
    }

    public void serialWriteBytes(byte[] data) {
        if (serialPort != null) serialPort.write(data);
    }

    public void serialWriteString(String s){
        this.serialWriteBytes(s.getBytes());
        this.serialWriteBytes(BYTE_NEW_LINE);
        //Log.d("SERIAL WRITE", s);
        serialUsbCommunicationHandler.obtainMessage(Constants.MESSAGE_WRITE, s.length(), -1, s).sendToTarget();
    }


    public void setMessageHandler(Handler grblServiceMessageHandler) {
        this.mHandler = grblServiceMessageHandler;
    }


    /*
     *  Data received from serial port will be received here. Just populate onReceivedData with your code
     *  In this particular example. byte stream is converted to String and send to UI thread to
     *  be treated there.
     */
    private final UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {

        private final StringBuffer rxBuffer = new StringBuffer();

        @Override
        public void onReceivedData(byte[] arg0) {
            try{
                int newLineIndex;
                rxBuffer.append(new String(arg0, "UTF-8"));

                while((newLineIndex = rxBuffer.indexOf("\n")) != -1){
                    String readMessage = rxBuffer.substring(0, newLineIndex);
                    rxBuffer.delete(0, newLineIndex+1);
                    serialUsbCommunicationHandler.obtainMessage(Constants.MESSAGE_READ, readMessage.length(), -1, readMessage).sendToTarget();
                }

//                int newLineIndex  = rxBuffer.indexOf("\n");
//                if(newLineIndex > 0){
//                    String readMessage = rxBuffer.substring(0, newLineIndex);
//                    rxBuffer.delete(0, newLineIndex+1);
//                    serialUsbCommunicationHandler.obtainMessage(Constants.MESSAGE_READ, readMessage.length(), -1, readMessage).sendToTarget();
//                }

            }catch (Exception e){
                Crashlytics.logException(e);
            }
        }
    };


    /*
     * State changes in the CTS line will be received here
     */
    private UsbSerialInterface.UsbCTSCallback ctsCallback = new UsbSerialInterface.UsbCTSCallback() {
        @Override
        public void onCTSChanged(boolean state) {
            if(mHandler != null) mHandler.obtainMessage(CTS_CHANGE).sendToTarget();
        }
    };

    /*
     * State changes in the DSR line will be received here
     */
    private UsbSerialInterface.UsbDSRCallback dsrCallback = new UsbSerialInterface.UsbDSRCallback() {
        @Override
        public void onDSRChanged(boolean state) {
            if(mHandler != null) mHandler.obtainMessage(DSR_CHANGE).sendToTarget();
        }
    };

    /*
     * Different notifications from OS will be received here (USB attached, detached, permission responses...)
     * About BroadcastReceiver: http://developer.android.com/reference/android/content/BroadcastReceiver.html
     */
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1){
            if(arg1.getAction().equals(ACTION_USB_PERMISSION)){
                boolean granted = arg1.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if(granted){
                    Intent intent = new Intent(ACTION_USB_PERMISSION_GRANTED);
                    arg0.sendBroadcast(intent);
                    connection = usbManager.openDevice(device);
                    new ConnectionThread().start();
                }else{
                    Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                    arg0.sendBroadcast(intent);
                }
            }else if(arg1.getAction().equals(ACTION_USB_ATTACHED)) {
                if(!serialPortConnected) findSerialPortDevice();
            } else if (arg1.getAction().equals(ACTION_USB_DETACHED)) {
                Intent intent = new Intent(ACTION_USB_DISCONNECTED);
                arg0.sendBroadcast(intent);
                serialUsbCommunicationHandler.stopGrblStatusUpdateService();
                if(serialPortConnected){
                    serialPort.close();
                }
                serialPortConnected = false;
            }
        }
    };

    private void findSerialPortDevice() {
        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();

                if (deviceVID != 0x1d6b && (devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003) && deviceVID != 0x5c6 && devicePID != 0x904c) {

                    // There is a device connected to our Android device. Try to open it as a Serial Port.
                    requestUserPermission();
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
            if (!keep) {
                // There is no USB devices connected (but usb host were listed). Send an intent to MainActivity.
                Intent intent = new Intent(ACTION_NO_USB);
                sendBroadcast(intent);
            }
        } else {
            // There is no USB devices connected. Send an intent to MainActivity
            Intent intent = new Intent(ACTION_NO_USB);
            sendBroadcast(intent);
        }
    }

    private void setFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        if(usbReceiver != null) registerReceiver(usbReceiver, filter);
    }

    /*
     * Request user permission. The response will be received in the BroadcastReceiver
     */
    private void requestUserPermission() {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(device, mPendingIntent);
    }

    public class UsbSerialBinder extends Binder {
        public GrblUsbSerialService getService() {
            return GrblUsbSerialService.this;
        }
    }

    /*
    * A simple thread to open a serial port.
    * Although it should be a fast operation. moving usb operations away from UI thread is a good thing.
    */
    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if (serialPort != null) {
                if (serialPort.open()) {
                    serialPortConnected = true;
                    serialPort.setBaudRate(BAUD_RATE);
                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);

                    /*
                     * Current flow control Options:
                     * UsbSerialInterface.FLOW_CONTROL_OFF
                     * UsbSerialInterface.FLOW_CONTROL_RTS_CTS only for CP2102 and FT232
                     * UsbSerialInterface.FLOW_CONTROL_DSR_DTR only for CP2102 and FT232
                     */
                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    serialPort.read(mCallback);
                    //serialPort.getCTS(ctsCallback);
                    //serialPort.getDSR(dsrCallback);

                    //
                    // Some Arduinos would need some sleep because firmware wait some time to know whether a new sketch is going
                    // to be uploaded or not

                    try {
                        Thread.sleep(2000); // sleep some. YMMV with different chips.
                    } catch (InterruptedException e) {
                        Crashlytics.logException(e);
                    }

                    // Everything went as expected. Send an intent to MainActivity
                    serialWriteByte(GrblUtils.GRBL_RESET_COMMAND);
                    Intent intent = new Intent(ACTION_USB_READY);
                    context.sendBroadcast(intent);
                } else {
                    // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
                    // Send an Intent to Main Activity
                    if (serialPort instanceof CDCSerialDevice) {
                        Intent intent = new Intent(ACTION_CDC_DRIVER_NOT_WORKING);
                        context.sendBroadcast(intent);
                    } else {
                        Intent intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
                        context.sendBroadcast(intent);
                    }
                }
            } else {
                // No driver for given device, even generic CDC driver could not be loaded
                Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
                context.sendBroadcast(intent);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onGrblGcodeSendEvent(GcodeCommand event){
        serialWriteString(event.getCommandString());
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onGrblRealTimeCommandEvent(GrblRealTimeCommandEvent grblRealTimeCommandEvent){
        serialWriteByte(grblRealTimeCommandEvent.getCommand());
    }

    private Notification getNotification(String message){

        if(message == null) message = getString(R.string.text_usb_service_foreground_message);

        return new NotificationCompat.Builder(getApplicationContext(), NotificationHelper.CHANNEL_SERVICE_ID)
                .setContentTitle(getString(R.string.text_usb_service))
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setAutoCancel(true).build();
    }

}
