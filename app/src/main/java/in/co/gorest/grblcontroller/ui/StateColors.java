package in.co.gorest.grblcontroller.ui;

import android.content.Context;

import androidx.core.content.ContextCompat;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.model.Constants;

/** Colour for the machine state word on the status card. Used from data binding. */
public final class StateColors {

    private StateColors() {}

    public static int forState(Context context, String state) {
        int color = R.color.colorTextSecondary;
        if (state != null) {
            switch (state) {
                case Constants.MACHINE_STATUS_IDLE:  color = R.color.stateIdle;  break;
                case Constants.MACHINE_STATUS_RUN:   color = R.color.stateRun;   break;
                case Constants.MACHINE_STATUS_JOG:   color = R.color.stateJog;   break;
                case Constants.MACHINE_STATUS_HOLD:  color = R.color.stateHold;  break;
                case Constants.MACHINE_STATUS_DOOR:  color = R.color.stateDoor;  break;
                case Constants.MACHINE_STATUS_CHECK: color = R.color.stateCheck; break;
                case Constants.MACHINE_STATUS_HOME:  color = R.color.stateHome;  break;
                case Constants.MACHINE_STATUS_ALARM: color = R.color.stateAlarm; break;
                default: break;
            }
        }
        return ContextCompat.getColor(context, color);
    }
}
