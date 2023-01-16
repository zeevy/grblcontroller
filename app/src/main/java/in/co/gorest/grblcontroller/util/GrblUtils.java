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

package in.co.gorest.grblcontroller.util;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import in.co.gorest.grblcontroller.listeners.MachineStatusListener.CompileTimeOptions;
import in.co.gorest.grblcontroller.model.Overrides;
import in.co.gorest.grblcontroller.model.Position;

public class GrblUtils {

    // Real time
    public static final byte GRBL_PAUSE_COMMAND = '!';
    public static final byte GRBL_RESUME_COMMAND = '~';
    public static final byte GRBL_STATUS_COMMAND = '?';
    public static final byte GRBL_DOOR_COMMAND = (byte)0x84;
    public static final byte GRBL_JOG_CANCEL_COMMAND = (byte)0x85;
    public static final byte GRBL_RESET_COMMAND = 0x18;
    // Non real time
    public static final String GRBL_KILL_ALARM_LOCK_COMMAND = "$X";
    public static final String GRBL_TOGGLE_CHECK_MODE_COMMAND = "$C";
    public static final String GRBL_VIEW_PARSER_STATE_COMMAND = "$G";
    public static final String GRBL_VIEW_SETTINGS_COMMAND = "$$";
    public static final String GRBL_RUN_HOMING_CYCLE = "$H";
    public static final String GRBL_SLEEP_COMMAND = "$SLP";
    public static final String GRBL_BUILD_INFO_COMMAND = "$I";
    public static final String GRBL_VIEW_GCODE_PARAMETERS_COMMAND = "$#";

    // Gcode Commands
    public static final String GCODE_RESET_COORDINATES_TO_ZERO = "G10 L20 P0 X0Y0Z0";
    public static final String GCODE_RESET_COORDINATE_TO_ZERO = "G10 P0 L20 %c0";
    public static final String GCODE_CANCEL_TOOL_OFFSETS = "G49";

    private static final String GCODE_RETURN_TO_ZERO_LOCATION_XY = "G90 G0 X0 Y0";
    private static final String GCODE_RETURN_TO_ZERO_LOCATION_Z0 = "G90 G0 Z0";
    private static final String GCODE_RETURN_TO_ZERO_LOCATION_Z0_IN_MACHINE_CORDS = "G53 G0 Z0";

    public static Boolean isGrblVersionString(final String response) {
        boolean version = response.toLowerCase().startsWith("grbl");
        return version && (getVersionDouble(response) != -1);
    }

    private final static String VERSION_DOUBLE_REGEX = "\\d*\\.\\d*";
    private final static Pattern VERSION_DOUBLE_PATTERN = Pattern.compile(VERSION_DOUBLE_REGEX);
    public static double getVersionDouble(final String response) {
        double retValue = -1;
        Matcher matcher = VERSION_DOUBLE_PATTERN.matcher(response);
        if (matcher.find()) {
            retValue = Double.parseDouble(Objects.requireNonNull(matcher.group(0)));
        }
        return retValue;
    }

    private final static String VERSION_LETTER_REGEX = "(?<=\\d\\.\\d)[a-zA-Z]";
    private final static Pattern VERSION_LETTER_PATTERN = Pattern.compile(VERSION_LETTER_REGEX);
    public static Character getVersionLetter(final String response) {
        Character retValue = null;

        // Search for a version.
        Matcher matcher = VERSION_LETTER_PATTERN.matcher(response);
        if (matcher.find()) {
            retValue = Objects.requireNonNull(matcher.group(0)).charAt(0);
            //retValue = Double.parseDouble(matcher.group(0));
        }

        return retValue;
    }

    private static final Pattern TLO_PATTERN = Pattern.compile("^\\[TLO:(.*)]$");
    public static boolean isGrblToolLengthOffsetMessage(final String response){
        return TLO_PATTERN.matcher(response).find();
    }

    public static Double getToolLengthOffset(final String response){
        Matcher matcher = TLO_PATTERN.matcher(response);
        return matcher.find() ? Double.parseDouble(Objects.requireNonNull(matcher.group(1))) : 0.0;
    }

    private static final String PROBE_REGEX = "^\\[PRB:(.*)]$";
    private static final Pattern PROBE_PATTERN = Pattern.compile(PROBE_REGEX);
    public static boolean isGrblProbeMessage(final String response) {
        return PROBE_PATTERN.matcher(response).find();
    }

    public static String getProbeString(final String response){
        Matcher matcher = PROBE_PATTERN.matcher(response);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static final String STATUS_REGEX = "^<.*>$";
    private static final Pattern STATUS_PATTERN = Pattern.compile(STATUS_REGEX);
    public static boolean isGrblStatusString(final String response) {
        return STATUS_PATTERN.matcher(response).find();
    }

    private static final String FEEDBACK_REGEX = "^\\[MSG:.*]$";
    private static final Pattern FEEDBACK_PATTERN = Pattern.compile(FEEDBACK_REGEX);
    public static boolean isGrblFeedbackMessage(final String response) {
        return FEEDBACK_PATTERN.matcher(response).find();
    }

    private static final String BUILD_OPTIONS_REGEX = "^\\[OPT:(.*)]$";
    private static final Pattern BUILD_OPTIONS_PATTERN = Pattern.compile(BUILD_OPTIONS_REGEX);
    public static boolean isBuildOptionsMessage(final String response) {
        return BUILD_OPTIONS_PATTERN.matcher(response).find();
    }

    public static String getBuildOptionString(final String response){
        Matcher matcher = BUILD_OPTIONS_PATTERN.matcher(response);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static final String PARSER_STATE_REGEX = "^\\[GC:(.*)]$";
    private static final Pattern PARSER_STATE_PATTERN = Pattern.compile(PARSER_STATE_REGEX);
    public static boolean isParserStateMessage(final String response){
        return PARSER_STATE_PATTERN.matcher(response).find();
    }

    public static String getParserStateString(final String response){
        Matcher matcher = PARSER_STATE_PATTERN.matcher(response);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static final String SETTING_REGEX = "^\\$\\d+=.+";
    private static final Pattern SETTING_PATTERN = Pattern.compile(SETTING_REGEX);
    public static boolean   isGrblSettingMessage(final String response) {
        return SETTING_PATTERN.matcher(response).find();
    }

    public static boolean isGrblErrorMessage(final String response){
        return response.toLowerCase().startsWith("error:");
    }

    public static boolean isGrblOkMessage(final String response){
        return response.toLowerCase().startsWith("ok");
    }

    public static boolean isGrblAlarmMessage(final String response){
        return response.toLowerCase().startsWith("alarm:");
    }

    public static final Pattern machinePattern = Pattern.compile("(?<=MPos:)(-?\\d*\\..\\d*),(-?\\d*\\..\\d*),(-?\\d*\\..\\d*)");
    public static final Pattern workPattern = Pattern.compile("(?<=WPos:)(-?\\d*\\..\\d*),(-?\\d*\\..\\d*),(-?\\d*\\..\\d*)");
    public static final Pattern wcoPattern = Pattern.compile("(?<=WCO:)(-?\\d*\\..\\d*),(-?\\d*\\..\\d*),(-?\\d*\\..\\d*)");
    public static Position getPositionFromStatusString(final String status, final Pattern pattern) {
        Matcher matcher = pattern.matcher(status);
        if (matcher.find()) {
            return new Position(
                    Double.parseDouble(Objects.requireNonNull(matcher.group(1))),
                    Double.parseDouble(Objects.requireNonNull(matcher.group(2))),
                    Double.parseDouble(Objects.requireNonNull(matcher.group(3)))
            );
        }
        return null;
    }

    public static ArrayList<String> getReturnToHomeCommands(){
        ArrayList<String> commands = new ArrayList<>();
        commands.add(GrblUtils.GCODE_RETURN_TO_ZERO_LOCATION_Z0_IN_MACHINE_CORDS);
        commands.add(GrblUtils.GCODE_RETURN_TO_ZERO_LOCATION_XY);
        commands.add(GrblUtils.GCODE_RETURN_TO_ZERO_LOCATION_Z0);

        return commands;
    }

    public static CompileTimeOptions getCompileTimeOptionsFromString(String buildOptions){
        int plannerBuffer = 15; int serialRxBuffer = 128;

        if (buildOptions.indexOf(",") > 0) {
            String[] parts = buildOptions.split(",");
            if (parts.length >= 3) {
                int tmpPBuffer = Integer.parseInt(parts[1]);
                if (tmpPBuffer > 0) plannerBuffer = tmpPBuffer;

                int tmpRxBuffer = Integer.parseInt(parts[2]);
                if (tmpRxBuffer > 0) serialRxBuffer = tmpRxBuffer;
            }
        }

        return new CompileTimeOptions(buildOptions, plannerBuffer, serialRxBuffer);
    }

    private final static String EEPROM_COMMAND_PATTERN = "G10|G28|G30|\\$x=|\\$I|\\$N|\\$RST=|G5[456789]|\\$\\$|\\$#";
    private final static Pattern EEPROM_COMMAND = Pattern.compile(EEPROM_COMMAND_PATTERN, Pattern.CASE_INSENSITIVE);
    public static boolean hasRomAccess(String command){
        return EEPROM_COMMAND.matcher(command).find();
    }

    public static String implode(String glue, String[] array) {
        if(array == null || array.length == 0) return "";

        StringBuilder sb = new StringBuilder();
        sb.append(array[0]);
        for(int i = 1; i < array.length; i++) sb.append(glue).append(array[i]);

        return sb.toString();
    }


    static public Byte getOverrideForEnum(final Overrides command) {
        switch (command) {
            //CMD_DEBUG_REPORT, // 0x85 // Only when DEBUG enabled, sends debug report in '{}' braces.
            case CMD_FEED_OVR_RESET:
                return (byte)0x90; // Restores feed override value to 100%.
            case CMD_FEED_OVR_COARSE_PLUS:
                return (byte)0x91;
            case CMD_FEED_OVR_COARSE_MINUS:
                return (byte)0x92;
            case CMD_FEED_OVR_FINE_PLUS :
                return (byte)0x93;
            case CMD_FEED_OVR_FINE_MINUS :
                return (byte)0x94;
            case CMD_RAPID_OVR_RESET:
                return (byte)0x95;
            case CMD_RAPID_OVR_MEDIUM:
                return (byte)0x96;
            case CMD_RAPID_OVR_LOW:
                return (byte)0x97;
            case CMD_SPINDLE_OVR_RESET:
                return (byte)0x99; // Restores spindle override value to 100%.
            case CMD_SPINDLE_OVR_COARSE_PLUS:
                return (byte)0x9A;
            case CMD_SPINDLE_OVR_COARSE_MINUS:
                return (byte)0x9B;
            case CMD_SPINDLE_OVR_FINE_PLUS:
                return (byte)0x9C;
            case CMD_SPINDLE_OVR_FINE_MINUS:
                return (byte)0x9D;
            case CMD_TOGGLE_SPINDLE:
                return (byte)0x9E;
            case CMD_TOGGLE_FLOOD_COOLANT:
                return (byte)0xA0;
            case CMD_TOGGLE_MIST_COOLANT:
                return (byte)0xA1;
        }
        return null;
    }


}
