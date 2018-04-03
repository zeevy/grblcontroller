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

package in.co.gorest.grblcontroller.model;

public interface Constants {

    double MIN_SUPPORTED_VERSION        = 1.1;

    long GRBL_STATUS_UPDATE_INTERVAL    = 200;

    // Message types sent from the BluetoothService Handler
    int MESSAGE_STATE_CHANGE            = 1;
    int MESSAGE_READ                    = 2;
    int MESSAGE_WRITE                   = 3;
    int MESSAGE_DEVICE_NAME             = 4;
    int MESSAGE_TOAST                   = 5;
    int REQUEST_READ_PERMISSIONS        = 6;
    int PROBE_TYPE_NORMAL               = 7;
    int PROBE_TYPE_TOOL_OFFSET          = 8;
    int CONNECT_DEVICE_SECURE           = 9;
    int CONNECT_DEVICE_INSECURE         = 10;

    int CONSOLE_LOGGER_MAX_SIZE         = 128;
    double DEFAULT_JOGGING_FEED_RATE    = 2400.0;
    int DEFAULT_PLANNER_BUFFER          = 15;
    int DEFAULT_SERIAL_RX_BUFFER        = 128;
    int PROBING_FEED_RATE               = 50;
    int PROBING_PLATE_THICKNESS         = 20;
    int PROBING_DISTANCE                = 15;



    String MACHINE_STATUS_IDLE          = "Idle";
    String MACHINE_STATUS_JOG           = "Jog";
    String MACHINE_STATUS_RUN           = "Run";
    String MACHINE_STATUS_HOLD          = "Hold";
    String MACHINE_STATUS_ALARM         = "Alarm";
    String MACHINE_STATUS_CHECK         = "Check";
    String MACHINE_STATUS_SLEEP         = "Sleep";
    String MACHINE_STATUS_DOOR          = "Door";
    String MACHINE_STATUS_HOME          = "Home";
    String MACHINE_STATUS_NOT_CONNECTED = "Unknown";

    String[] SUPPORTED_FILE_TYPES       = {".tap",".gcode", ".nc", ".ngc"};

    String JUST_STOP_STREAMING          = "0";
    String STOP_STREAMING_AND_RESET     = "1";
    String DEVICE_NAME                  = "device_name";
    String TOAST                        = "toast";

    String SERIAL_CONNECTION_TYPE_BLUETOOTH         = "bluetooth";
    String SERIAL_CONNECTION_TYPE_USB_OTG           = "usbotg";

    int BLUETOOTH_SERVICE_NOTIFICATION_ID           = 100;
    int FILE_STREAMING_NOTIFICATION_ID              = 101;
    int USB_OTG_SERVICE_NOTIFICATION_ID             = 102;

}
