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

package in.co.gorest.grblcontroller.listeners;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import in.co.gorest.grblcontroller.BR;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.model.Position;

public class MachineStatusListener extends BaseObservable {

    private static final String TAG = MachineStatusListener.class.getSimpleName();

    private final String emptyString = "";
    private final Double DEFAULT_FEED_RATE          = Constants.DEFAULT_JOGGING_FEED_RATE;

    public static final String STATE_IDLE           = Constants.MACHINE_STATUS_IDLE;
    public static final String STATE_JOG            = Constants.MACHINE_STATUS_JOG;
    public static final String STATE_RUN            = Constants.MACHINE_STATUS_RUN;
    public static final String STATE_HOLD           = Constants.MACHINE_STATUS_HOLD;
    public static final String STATE_ALARM          = Constants.MACHINE_STATUS_ALARM;
    public static final String STATE_CHECK          = Constants.MACHINE_STATUS_CHECK;
    public static final String STATE_SLEEP          = Constants.MACHINE_STATUS_SLEEP;
    public static final String STATE_DOOR           = Constants.MACHINE_STATUS_DOOR;
    public static final String STATE_HOME           = Constants.MACHINE_STATUS_HOME;
    public static final String STATE_NOT_CONNECTED  = Constants.MACHINE_STATUS_NOT_CONNECTED;

    private String state;
    private Integer plannerBuffer = 0;
    private Integer serialRxBuffer = 0;
    private Double feedRate = 0.0;
    private Double spindleSpeed = 0.0;
    private Position lastProbePosition = null;
    private Double toolLengthOffset = 0.0;

    private Position machinePosition = new Position(0.00, 0.00, 0.00);
    private Position workPosition = new Position(0.00, 0.00, 0.00);
    private Position workCoordsOffset = new Position(0.00, 0.00, 0.00);
    private Jogging jogging = new Jogging(0.00, 0.00, DEFAULT_FEED_RATE, false);
    private OverridePercents overridePercents = new OverridePercents(100, 100, 100);
    private EnabledPins enabledPins = new EnabledPins(emptyString);
    private AccessoryStates accessoryStates = new AccessoryStates(emptyString);
    private BuildInfo buildInfo = null;
    private CompileTimeOptions compileTimeOptions = null;
    private ParserState parserState = null;
    private Boolean verboseOutput = false;
	private Boolean laserModeEnabled = false;

    private static MachineStatusListener machineStatus = null;
    public static MachineStatusListener getInstance(){
        if(machineStatus == null) machineStatus = new MachineStatusListener();
        return machineStatus;
    }

    public static void resetClass(){
        machineStatus = new MachineStatusListener();
    }

    private MachineStatusListener(){
        this.state = STATE_NOT_CONNECTED;
        this.compileTimeOptions = new CompileTimeOptions(emptyString, Constants.DEFAULT_PLANNER_BUFFER, Constants.DEFAULT_SERIAL_RX_BUFFER);
        this.parserState = new ParserState("G0 G54 G17 G21 G90 G94");
    }

    @Bindable
    public Boolean getVerboseOutput(){ return this.verboseOutput; }
    public void setVerboseOutput(Boolean verboseOutput){
        if(this.verboseOutput != verboseOutput){
            this.verboseOutput = verboseOutput;
            notifyPropertyChanged(BR.verboseOutput);
        }
    }

    @Bindable
    public Boolean getLaserModeEnabled(){ return this.laserModeEnabled; }
    public void setLaserModeEnabled(Boolean laserModeEnabled){
        this.laserModeEnabled = laserModeEnabled;
        notifyPropertyChanged(BR.laserModeEnabled);
    }

    @Bindable
    public String getState(){ return this.state; }
    public void setState(String state){
        if(!this.state.equals(state)){
            this.state = state;
            notifyPropertyChanged(BR.state);
        }
    }

    @Bindable
    public Integer getPlannerBuffer(){ return this.plannerBuffer; }
    public void setPlannerBuffer(Integer plannerBuffer){
        if(this.plannerBuffer.compareTo(plannerBuffer) !=  0){
            this.plannerBuffer = plannerBuffer;
            notifyPropertyChanged(BR.plannerBuffer);
        }
    }

    @Bindable
    public Integer getSerialRxBuffer(){ return this.serialRxBuffer; }
    public void setSerialRxBuffer(Integer serialRxBuffer){
        if(this.serialRxBuffer.compareTo(serialRxBuffer) != 0){
            this.serialRxBuffer = serialRxBuffer;
            notifyPropertyChanged(BR.serialRxBuffer);
        }
    }

    @Bindable
    public Double getFeedRate(){ return this.feedRate; }
    public void setFeedRate(double feedRate){
        if(this.feedRate.compareTo(feedRate) != 0){
            this.feedRate = feedRate;
            notifyPropertyChanged(BR.feedRate);
        }
    }

    @Bindable
    public Double getSpindleSpeed(){ return this.spindleSpeed; }
    public void setSpindleSpeed(double spindleSpeed){
        if(this.spindleSpeed.compareTo(spindleSpeed) != 0){
            this.spindleSpeed = spindleSpeed;
            notifyPropertyChanged(BR.spindleSpeed);
        }
    }

    @Bindable
    public Position getLastProbePosition(){ return this.lastProbePosition; }
    public void setLastProbePosition(Position lastProbePosition){
        this.lastProbePosition = lastProbePosition;
        notifyPropertyChanged(BR.lastProbePosition);
    }

    @Bindable
    public Double getToolLengthOffset(){ return this.toolLengthOffset; }
    public void setToolLengthOffset(double toolLengthOffset){
        this.toolLengthOffset = toolLengthOffset;
        notifyPropertyChanged(BR.toolLengthOffset);
    }


    @Bindable
    public Position getWorkCoordsOffset(){ return this.workCoordsOffset; }
    public void setWorkCoordsOffset(Position workCoordsOffset){
        if(this.workCoordsOffset.hasChanged(workCoordsOffset)){
            this.workCoordsOffset = workCoordsOffset;
            notifyPropertyChanged(BR.workCoordsOffset);
        }
    }

    @Bindable
    public Position getWorkPosition() { return this.workPosition; }
    public void setWorkPosition(Position workPosition){
        if(this.workPosition.hasChanged(workPosition)){
            this.workPosition = workPosition;
            notifyPropertyChanged(BR.workPosition);
        }
    }

    @Bindable
    public Position getMachinePosition(){ return this.machinePosition; }
    public void setMachinePosition(Position machinePosition){
        if(this.machinePosition.hasChanged(machinePosition)){
            this.machinePosition = machinePosition;
            notifyPropertyChanged(BR.machinePosition);
        }
    }

    @Bindable
    public Jogging getJogging(){ return this.jogging; }
    public void setJogging(double stepXY, double stepZ, double feed, boolean inches){
        Jogging jogging = new Jogging(stepXY, stepZ, feed, inches);
        if(this.jogging.hasChanged(jogging)){
            this.jogging = jogging;
            notifyPropertyChanged(BR.jogging);
        }
    }


    @Bindable
    public OverridePercents getOverridePercents(){ return this.overridePercents; }
    public void setOverridePercents(int feed, int rapid, int spindle){
        OverridePercents v = new OverridePercents(feed, rapid, spindle);
        if(this.overridePercents.hasChanged(v)){
            this.overridePercents = v;
            notifyPropertyChanged(BR.overridePercents);
        }
    }

    @Bindable
    public EnabledPins getEnabledPins(){ return this.enabledPins; }
    public void setEnabledPins(String enabled){
        EnabledPins e = new EnabledPins(enabled);

        if(this.enabledPins.hasChanged(e)){
            this.enabledPins = e;
            notifyPropertyChanged(BR.enabledPins);
        }
    }

    @Bindable
    public AccessoryStates getAccessoryStates(){ return this.accessoryStates; }
    public void setAccessoryStates(String enabled){
        AccessoryStates a = new AccessoryStates(enabled);
        if(this.accessoryStates.hasChanged(a)){
            this.accessoryStates = a;
            notifyPropertyChanged(BR.accessoryStates);
        }
    }

    @Bindable
    public BuildInfo getBuildInfo(){ return this.buildInfo; }
    public void setBuildInfo(BuildInfo buildInfo){
        this.buildInfo = buildInfo;
        notifyPropertyChanged(BR.buildInfo);
    }

    @Bindable
    public CompileTimeOptions getCompileTimeOptions(){ return this.compileTimeOptions; }
    public void setCompileTimeOptions(CompileTimeOptions compileTimeOptions){
        this.compileTimeOptions = compileTimeOptions;
        notifyPropertyChanged(BR.compileTimeOptions);
    }

    @Bindable
    public ParserState getParserState(){ return this.parserState; }
    public void setParserState(String parserState){
        this.parserState = new ParserState(parserState);
        notifyPropertyChanged(BR.parserState);
    }

    public static class BuildInfo{
        public final Double versionDouble;
        public final Character versionLetter;

        public BuildInfo(double d, char l){
            this.versionDouble = d;
            this.versionLetter = l;
        }
    }

    public static class Jogging{
        public final Double stepXY;
        public final Double stepZ;
        public final Double feed;
        public final Boolean inches;

        public Jogging(double stepXY, double stepZ, double f, boolean i){
            this.stepXY = stepXY; this.stepZ = stepZ; this.feed = f; this.inches = i;
        }

        public boolean hasChanged(Jogging jogging){
            boolean stepChanged = ((jogging.stepXY.compareTo(this.stepXY) != 0) || (jogging.stepZ.compareTo(this.stepZ) != 0));
            boolean feedChanged = (jogging.feed.compareTo(this.feed) != 0);
            boolean inchesChanged = (jogging.inches.compareTo(this.inches) != 0);

            return (stepChanged || feedChanged || inchesChanged);
        }
    }

    public static class OverridePercents{
        public final Integer feed;
        public final Integer rapid;
        public final Integer spindle;

        public OverridePercents(int f, int r, int s){
            this.feed = f; this.rapid = r; this.spindle = s;
        }

        public boolean hasChanged(OverridePercents overridePercents){
            boolean feedChanged = (overridePercents.feed.compareTo(this.feed) != 0);
            boolean rapidChanged = (overridePercents.rapid.compareTo(this.rapid) != 0);
            boolean spindleChanged = (overridePercents.spindle.compareTo(this.spindle) != 0);

            return (feedChanged || rapidChanged || spindleChanged);
        }
    }

    public static class ParserState{
        public final String motionType;
        public final String coordinateSystem;
        public final String planeSelection;
        public final String unitSelection;
        public final String distanceMode;
        public final String feedRateMode;

        public ParserState(String parserState){
            String[] parts = parserState.split("\\s+");

            motionType = parts[0].toUpperCase();
            coordinateSystem = parts[1].toUpperCase();
            planeSelection = parts[2].toUpperCase();
            unitSelection = parts[3].toUpperCase();
            distanceMode = parts[4].toUpperCase();
            feedRateMode = parts[5].toUpperCase();
        }
    }

    public static class EnabledPins{
        final public boolean x;
        final public boolean y;
        final public boolean z;
        final public boolean probe;
        final public boolean door;
        final public boolean hold;
        final public boolean softReset;
        final public boolean cycleStart;

        public EnabledPins(String enabled){
            String enabledUpper = enabled.toUpperCase();
            x = enabledUpper.contains("X");
            y = enabledUpper.contains("Y");
            z = enabledUpper.contains("Z");
            probe = enabledUpper.contains("P");
            door = enabledUpper.contains("D");
            hold = enabledUpper.contains("H");
            softReset = enabledUpper.contains("R");
            cycleStart = enabledUpper.contains("S");
        }

        public boolean hasChanged(EnabledPins enabledPins){
            boolean xChanged = (enabledPins.x != this.x);
            boolean yChanged = (enabledPins.y != this.y);
            boolean zChanged = (enabledPins.z != this.z);
            boolean probeChanged = (enabledPins.probe != this.probe);
            boolean doorChanged = (enabledPins.door != this.door);
            boolean holdChanged = (enabledPins.hold != this.hold);
            boolean softResetChanged = (enabledPins.softReset != this.softReset);
            boolean cycleStartChanged = (enabledPins.cycleStart != this.cycleStart);

            return (xChanged || yChanged || zChanged || probeChanged || doorChanged || holdChanged || softResetChanged || cycleStartChanged);
        }

    }

    public static class AccessoryStates {
        final public boolean spindleCW;
        final public boolean spindleCCW;
        final public boolean flood;
        final public boolean mist;

        public AccessoryStates(String enabled) {
            String enabledUpper = enabled.toUpperCase();
            spindleCW = enabledUpper.contains("S");
            spindleCCW = enabledUpper.contains("C");
            flood = enabledUpper.contains("F");
            mist = enabledUpper.contains("M");
        }

        public boolean hasChanged(AccessoryStates accessoryStates){
            boolean spindleCWChanged = (accessoryStates.spindleCW != this.spindleCW);
            boolean spindleCCWChanged = (accessoryStates.spindleCCW != this.spindleCCW);
            boolean floodChanged = (accessoryStates.flood != this.flood);
            boolean mistChanged = (accessoryStates.mist != mist);

            return (spindleCWChanged || spindleCCWChanged ||floodChanged || mistChanged);
        }
    }

    public static class CompileTimeOptions{

        public final boolean variableSpindle;
        public final boolean lineNumbers;
        public final boolean mistCoolant;
        public final boolean coreXY;
        public final boolean parkingMotion;
        public final boolean homingForceOrigin;
        public final boolean homingSingleAxis;
        public final boolean twoLimitSwitchesOnAxis;
        public final boolean feedRageOverrideInProbeCycles;
        public final boolean restoreAllRom;
        public final boolean restoreRomSettings;
        public final boolean restoreRomParameterData;
        public final boolean writeUserString;
        public final boolean forceSyncOnRomWrite;
        public final boolean forceSyncOnCoordinateChange;
        public final boolean homingInitLock;
        public final int plannerBuffer;
        public final int serialRxBuffer;

        public CompileTimeOptions(String enabled, int plannerBf, int serialRxBf){
            String enabledUpper = enabled.toUpperCase();

            variableSpindle = enabledUpper.contains("V");
            lineNumbers = enabledUpper.contains("N");
            mistCoolant = enabledUpper.contains("M");
            coreXY = enabledUpper.contains("C");
            parkingMotion = enabledUpper.contains("P");
            homingForceOrigin = enabledUpper.contains("Z");
            homingSingleAxis = enabledUpper.contains("H");
            twoLimitSwitchesOnAxis = enabledUpper.contains("T");
            feedRageOverrideInProbeCycles = enabledUpper.contains("A");
            restoreAllRom = enabledUpper.contains("*");
            restoreRomSettings = enabledUpper.contains("$");
            restoreRomParameterData = enabledUpper.contains("#");
            writeUserString = enabledUpper.contains("I");
            forceSyncOnRomWrite = enabledUpper.contains("E");
            forceSyncOnCoordinateChange = enabledUpper.contains("W");
            homingInitLock = enabledUpper.contains("L");
            plannerBuffer = plannerBf;
            serialRxBuffer = serialRxBf;
        }

    }
}
