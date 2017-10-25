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

    // Message types sent from the BluetoothService Handler
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_WRITE = 3;
    int MESSAGE_DEVICE_NAME = 4;
    int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothService Handler
    String DEVICE_NAME = "device_name";
    String TOAST = "toast";

    String STEP_PULSE = "$0";
    String STEP_IDLE_DELAY = "$1";
    String STEP_PORT_INVERT_MASK = "$2";
    String DIRECTION_PORT_INVERT_MASK = "$3";
    String STEP_ENABLE_INVERT = "$4";
    String LIMIT_PINS_INVERT= "$5";
    String PROBE_PIN_INVERT = "$6";

    String STATUS_REPORT_MASK = "$10";
    String JUNCTION_DEVIATION = "$11";
    String ARC_TOLERANCE = "$12";
    String REPORT_INCHES = "$13";

    String SOFT_LIMITS = "$20";
    String HARD_LIMITS = "$21";
    String HOMING_CYCLE = "$22";
    String HOMING_DIR_INVERT = "$23";
    String HOMING_FEED = "$24";
    String HOMING_SEEK = "$25";
    String HOMING_DEBOUNCE = "$26";
    String HOMING_PULL_OFF = "$27";

    String MAX_SPINDLE_SPEED = "$30";
    String MIN_SPINDLE_SPEED = "$31";
    String LASER_MODE = "$32";

    String AXIS_X_STEPS = "$100";
    String AXIS_Y_STEPS = "$101";
    String AXIS_Z_STEPS = "$102";

    String AXIS_X_MAX_RATE = "$110";
    String AXIS_Y_MAX_RATE = "$111";
    String AXIS_Z_MAX_RATE = "$112";

    String AXIS_X_ACCELERATION = "$120";
    String AXIS_Y_ACCELERATION = "$121";
    String AXIS_Z_ACCELERATION = "$122";

    String AXIS_X_MAX_TRAVEL = "$130";
    String AXIS_Y_MAX_TRAVEL = "$131";
    String AXIS_Z_MAX_TRAVEL = "$132";

}
