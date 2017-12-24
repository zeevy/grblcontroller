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

package in.co.gorest.grblcontroller.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TableRow;

import com.joanzapata.iconify.widget.IconButton;
import com.joanzapata.iconify.widget.IconToggleButton;
import com.xw.repo.BubbleSeekBar;

import org.greenrobot.eventbus.EventBus;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentJoggingTabBinding;
import in.co.gorest.grblcontroller.events.JogCommandEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.helpers.RepeatListener;
import in.co.gorest.grblcontroller.listners.MachineStatusListner;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class JoggingTabFragment extends BaseFragment implements View.OnClickListener, View.OnLongClickListener{

    private static final String TAG = JoggingTabFragment.class.getSimpleName();
    private MachineStatusListner machineStatus;
    private EnhancedSharedPreferences sharedPref;

    public JoggingTabFragment() {}

    public static JoggingTabFragment newInstance() {
        return new JoggingTabFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        machineStatus = MachineStatusListner.getInstance();
        sharedPref = EnhancedSharedPreferences.getInstance(getActivity().getApplicationContext(), getString(R.string.shared_preference_key));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentJoggingTabBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_jogging_tab, container, false);
        binding.setMachineStatus(machineStatus);
        View view = binding.getRoot();

        RelativeLayout joggingStepFeedView = view.findViewById(R.id.jogging_step_feed_view);
        joggingStepFeedView.setOnClickListener(this);

        for(int resourceId : new Integer[]{R.id.jog_y_positive, R.id.jog_x_positive, R.id.jog_z_positive,
                R.id.jog_xy_top_left, R.id.jog_xy_top_right, R.id.jog_xy_bottom_left, R.id.jog_xy_bottom_right,
                R.id.jog_y_negative, R.id.jog_x_negative, R.id.jog_z_negative}){

            final IconButton iconButton = view.findViewById(resourceId);
            iconButton.setOnTouchListener(new RepeatListener(false, 300, 25));

            iconButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendJogCommand(iconButton.getTag().toString());
                }
            });

        }

        for(int resourceId: new Integer[]{R.id.jog_cancel, R.id.run_homing_cycle, R.id.goto_x_zero, R.id.goto_y_zero, R.id.goto_z_zero}){
            IconButton iconButton = view.findViewById(resourceId);
            iconButton.setOnLongClickListener(this);
        }

        for(int resourseId: new Integer[]{R.id.run_homing_cycle, R.id.jog_cancel}){
            IconButton iconButton = view.findViewById(resourseId);
            iconButton.setOnClickListener(this);
        }

        TableRow resetZeroLayout = view.findViewById(R.id.reset_zero_layout);
        for(int i=0; i<resetZeroLayout.getChildCount(); i++){
            View resetZeroLayoutView = resetZeroLayout.getChildAt(i);
            if(resetZeroLayoutView instanceof IconButton || resetZeroLayoutView instanceof IconToggleButton){
                resetZeroLayoutView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final String tag = view.getTag().toString();

                        if(tag.equals("$X")){
                            if(!machineStatus.getState().equals(MachineStatusListner.STATE_RUN)) fragmentInteractionListener.onGcodeCommandReceived(tag);
                            return;
                        }

                        new AlertDialog.Builder(getActivity())
                                .setTitle("Zero selected axis")
                                .setMessage("set selected axis location in current coordinate system to zero | " + tag)
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        sendCommandIfIdle(tag);
                                    }
                                })
                                .setNegativeButton("No", null)
                                .show();
                    }
                });
            }
        }

        TableRow wposLayout = view.findViewById(R.id.wpos_layout);
        for(int i=0; i<wposLayout.getChildCount(); i++){
            View wposLayoutView = wposLayout.getChildAt(i);
            if(wposLayoutView instanceof Button){
                wposLayoutView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(machineStatus.getState().equals(MachineStatusListner.STATE_IDLE)){
                            sendCommandIfIdle(view.getTag().toString());
                            sendCommandIfIdle(GrblUtils.GRBL_VIEW_PARSER_STATE_COMMAND);
                            EventBus.getDefault().post(new UiToastEvent("Selected coordinate system " + view.getTag().toString()));
                        }else{
                            EventBus.getDefault().post(new UiToastEvent(getString(R.string.machine_not_idle)));
                        }
                    }
                });
                wposLayoutView.setOnLongClickListener(this);
            }
        }

        TableRow customButtonLayout = view.findViewById(R.id.custom_button_layout);
        if(sharedPref.getBoolean(getString(R.string.enable_custom_buttons), false)){
            customButtonLayout.setVisibility(View.VISIBLE);

            for(int resourceId: new Integer[]{R.id.custom_buton_1, R.id.custom_buton_2, R.id.custom_buton_3, R.id.custom_buton_4}){
                IconButton iconButton = view.findViewById(resourceId);

                if(resourceId == R.id.custom_buton_1) iconButton.setText(sharedPref.getString(getString(R.string.custom_button_one), getString(R.string.value_na)));
                if(resourceId == R.id.custom_buton_2) iconButton.setText(sharedPref.getString(getString(R.string.custom_button_two), getString(R.string.value_na)));
                if(resourceId == R.id.custom_buton_3) iconButton.setText(sharedPref.getString(getString(R.string.custom_button_three), getString(R.string.value_na)));
                if(resourceId == R.id.custom_buton_4) iconButton.setText(sharedPref.getString(getString(R.string.custom_button_four), getString(R.string.value_na)));

                iconButton.setOnLongClickListener(this);
                iconButton.setOnClickListener(this);
            }
        }

        return view;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        switch(id){
            case R.id.jogging_step_feed_view:
                this.setJoggingStepAndFeed();
                return;

            case R.id.run_homing_cycle:
                if(machineStatus.getState().equals(MachineStatusListner.STATE_IDLE) || machineStatus.getState().equals(MachineStatusListner.STATE_ALARM)){
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Homing cycle $H")
                            .setMessage("Perform homing cycle?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GRBL_RUN_HOMING_CYCLE);
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                }else{
                    EventBus.getDefault().post(new UiToastEvent(getString(R.string.machine_not_idle)));
                }
                break;

            case R.id.jog_cancel:
                if(machineStatus.getState().equals(MachineStatusListner.STATE_JOG)){
                    fragmentInteractionListener.onGrblRealTimeCommandReceived(GrblUtils.GRBL_JOG_CANCEL_COMMAND);
                }
                break;

            case R.id.custom_buton_1:
            case R.id.custom_buton_2:
            case R.id.custom_buton_3:
            case R.id.custom_buton_4:
                customButton(id, false);
                break;
        }
    }


    @Override
    public boolean onLongClick(View view) {
        int id = view.getId();

        switch(id){

            case R.id.run_homing_cycle:
                new AlertDialog.Builder(getActivity())
                        .setTitle("G10 L20 Set coordinate system")
                        .setMessage("set all axis location in current coordinate system to zero | G10 L20 P0 X0Y0Z0")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                sendCommandIfIdle(GrblUtils.GCODE_RESET_COORDINATES_TO_ZERO);
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
                return true;

            case R.id.jog_cancel:
                new AlertDialog.Builder(getActivity())
                        .setTitle("Return to zero postion")
                        .setMessage("Go to position X0 Y0 Z0 in current coordinates system")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                for (String gCommand : GrblUtils.getReturnToHomeCommands(machineStatus.getWorkPosition())) {
                                    sendCommandIfIdle(gCommand);
                                }
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
                return true;

            case R.id.wpos_g54:
            case R.id.wpos_g55:
            case R.id.wpos_g56:
            case R.id.wpos_g57:
                saveWPos((Button) view);
                return true;

            case R.id.goto_x_zero:
                gotoAxisZero("X");
                return true;

            case R.id.goto_y_zero:
                gotoAxisZero("Y");
                return true;

            case R.id.goto_z_zero:
                gotoAxisZero("Z");
                return true;

            case R.id.custom_buton_1:
            case R.id.custom_buton_2:
            case R.id.custom_buton_3:
            case R.id.custom_buton_4:
                customButton(id, true);
                return true;
        }

        return false;
    }

    private void customButton(int resourceId, boolean isLongClick){

        if(!machineStatus.getState().equals(MachineStatusListner.STATE_IDLE)){
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.machine_not_idle)));
            return;
        }

        String title = "";
        String commands = "";
        boolean confirmFirst = true;

        if(resourceId == R.id.custom_buton_1){
            title = sharedPref.getString(getString(R.string.custom_button_one), getString(R.string.value_na));
            commands = isLongClick ? sharedPref.getString(getString(R.string.custom_button_one_long_click), "") : sharedPref.getString(getString(R.string.custom_button_one_short_click), "");
            confirmFirst = sharedPref.getBoolean(getString(R.string.custom_button_one_confirm), true);
        }

        if(resourceId == R.id.custom_buton_2){
            title = sharedPref.getString(getString(R.string.custom_button_one), getString(R.string.value_na));
            commands = isLongClick ? sharedPref.getString(getString(R.string.custom_button_two_long_click), "") : sharedPref.getString(getString(R.string.custom_button_two_short_click), "");
            confirmFirst = sharedPref.getBoolean(getString(R.string.custom_button_two_confirm), true);
        }

        if(resourceId == R.id.custom_buton_3){
            title = sharedPref.getString(getString(R.string.custom_button_one), getString(R.string.value_na));
            commands = isLongClick ? sharedPref.getString(getString(R.string.custom_button_three_long_click), "") : sharedPref.getString(getString(R.string.custom_button_three_short_click), "");
            confirmFirst = sharedPref.getBoolean(getString(R.string.custom_button_three_confirm), true);
        }

        if(resourceId == R.id.custom_buton_4){
            title = sharedPref.getString(getString(R.string.custom_button_one), getString(R.string.value_na));
            commands = isLongClick ? sharedPref.getString(getString(R.string.custom_button_four_long_click), "") : sharedPref.getString(getString(R.string.custom_button_four_short_click), "");
            confirmFirst = sharedPref.getBoolean(getString(R.string.custom_button_four_confirm), true);
        }

        if(commands.trim().length() <= 0){
            EventBus.getDefault().post(new UiToastEvent("Empty command"));
            return;
        }

        final String finalCommands = commands;

        if(confirmFirst){
            String alertSummary = isLongClick ? "long click" : "short click";
            new AlertDialog.Builder(getActivity())
                    .setTitle("Custom action " + title)
                    .setMessage("send custom commands for the action " + alertSummary + " on button " + title)
                    .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            customButtonCommands(finalCommands);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }else{
            customButtonCommands(finalCommands);
        }

    }

    private void customButtonCommands(String commands){
        String lines[] = commands.split("[\r\n]+");
        if(lines.length > 14){
            EventBus.getDefault().post(new UiToastEvent("Total commands should not exceed 14"));
        }else{
            for(String command: lines){
                fragmentInteractionListener.onGcodeCommandReceived(command);
            }
        }
    }

    private void gotoAxisZero(final String axis){
        new AlertDialog.Builder(getActivity())
                .setTitle("Move " + axis + " axis to zero position?")
                .setMessage("goto axis zero position in current coordinate system | GO " + axis + "0")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        sendCommandIfIdle("G0 " + axis + "0");
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void saveWPos(Button button){
        String wpos = button.getTag().toString();
        final String slot;

        switch (wpos){
            case "G54":
                slot = "P1";
                break;
            case "G55":
                slot = "P2";
                break;
            case "G56":
                slot = "P3";
                break;
            case "G57":
                slot = "P4";
                break;
            default:
                slot = "P2";
        }

        new AlertDialog.Builder(getActivity())
                .setTitle("Save coordinate system")
                .setMessage("save current position to coordinate system " + wpos + "?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        sendCommandIfIdle(String.format("G10 L20 %s X0Y0Z0", slot));
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void sendJogCommand(String tag){
        if(machineStatus.getState().equals(MachineStatusListner.STATE_IDLE) || machineStatus.getState().equals(MachineStatusListner.STATE_JOG)){
            String units = machineStatus.getJogging().inches ? "G20" : "G21";
            String jog = String.format(tag, units, machineStatus.getJogging().step, machineStatus.getJogging().feed);
            EventBus.getDefault().post(new JogCommandEvent(jog));
        }else{
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.machine_not_idle)));
        }
    }

    private void setJoggingStepAndFeed(){
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final ViewGroup nullParent = null;
        View view = inflater.inflate(R.layout.dialog_step_and_feed, nullParent, false);

        BubbleSeekBar jogStepSeekBar = view.findViewById(R.id.jog_step_seek_bar);
        jogStepSeekBar.setProgress(machineStatus.getJogging().step.floatValue());
        jogStepSeekBar.getConfigBuilder().max(sharedPref.getInt(getString(R.string.jogging_max_step_size), 10)).build();

        jogStepSeekBar.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
                machineStatus.setJogging(Double.parseDouble(Float.toString(progressFloat)), machineStatus.getJogging().feed, sharedPref.getBoolean(getString(R.string.jogging_in_inches), false));
            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
                EnhancedSharedPreferences.Editor editor = sharedPref.edit();
                editor.putDouble(getString(R.string.jogging_step_size), Double.parseDouble(Float.toString(progressFloat))).commit();
            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

            }
        });

        BubbleSeekBar jogFeedSeekBar = view.findViewById(R.id.jog_feed_seek_bar);
        jogFeedSeekBar.setProgress(machineStatus.getJogging().feed.floatValue());
        Double maxFeedRate = sharedPref.getDouble(getString(R.string.jogging_max_feed_rate), machineStatus.getJogging().feed);
        jogFeedSeekBar.getConfigBuilder().max(Float.parseFloat(maxFeedRate.toString())).build();

        jogFeedSeekBar.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
                machineStatus.setJogging(machineStatus.getJogging().step, progress, sharedPref.getBoolean(getString(R.string.jogging_in_inches), false));
            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
                EnhancedSharedPreferences.Editor editor = sharedPref.edit();
                editor.putDouble(getString(R.string.jogging_feed_rate), progress).commit();
            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {}
        });

        SwitchCompat jogInches = view.findViewById(R.id.jog_inches);
        jogInches.setChecked(sharedPref.getBoolean(getString(R.string.jogging_in_inches), false));
        jogInches.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                machineStatus.setJogging(machineStatus.getJogging().step, machineStatus.getJogging().feed, b);
                EnhancedSharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(getString(R.string.jogging_in_inches), b).commit();
            }
        });


        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(view);
        alertDialogBuilder.setTitle("Jogging step and feed");
        alertDialogBuilder.setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });

        AlertDialog dialog = alertDialogBuilder.create();
        dialog.show();
    }

    private void sendCommandIfIdle(String command){
        if(machineStatus.getState().equals(MachineStatusListner.STATE_IDLE)){
            fragmentInteractionListener.onGcodeCommandReceived(command);
        }else{
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.machine_not_idle)));
        }
    }

}
