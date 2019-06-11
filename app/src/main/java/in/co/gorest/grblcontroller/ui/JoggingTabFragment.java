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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TableRow;

import com.joanzapata.iconify.widget.IconButton;
import com.joanzapata.iconify.widget.IconToggleButton;
import com.warkiz.widget.IndicatorSeekBar;
import com.warkiz.widget.OnSeekChangeListener;
import com.warkiz.widget.SeekParams;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentJoggingTabBinding;
import in.co.gorest.grblcontroller.events.GrblOkEvent;
import in.co.gorest.grblcontroller.events.JogCommandEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.helpers.RepeatListener;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class JoggingTabFragment extends BaseFragment implements View.OnClickListener, View.OnLongClickListener{

    private static final String TAG = JoggingTabFragment.class.getSimpleName();
    private MachineStatusListener machineStatus;
    private EnhancedSharedPreferences sharedPref;
    private BlockingQueue<Integer> completedCommands;
    private CustomCommandsAsyncTask customCommandsAsyncTask;

    public JoggingTabFragment() {}

    public static JoggingTabFragment newInstance() {
        return new JoggingTabFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        machineStatus = MachineStatusListener.getInstance();
        sharedPref = EnhancedSharedPreferences.getInstance(Objects.requireNonNull(getActivity()).getApplicationContext(), getString(R.string.shared_preference_key));
        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        SetCustomButtons(Objects.requireNonNull(getView()));

        String joggingPadRotateAngle = sharedPref.getString(getString(R.string.preference_xy_jog_pad_rotation), "0");
        String[] joggingPadTags = rotateJogPad(Integer.valueOf(joggingPadRotateAngle));
        int jogPadIndex = 0;
        for(int resourceId : new Integer[]{R.id.jog_xy_top_left, R.id.jog_y_positive, R.id.jog_xy_top_right, R.id.jog_x_negative, R.id.jog_x_positive, R.id.jog_xy_bottom_left, R.id.jog_y_negative, R.id.jog_xy_bottom_right}){
            final IconButton jogButton = getView().findViewById(resourceId);
            jogButton.setTag(joggingPadTags[jogPadIndex]);
            jogPadIndex++;
        }

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentJoggingTabBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_jogging_tab, container, false);
        binding.setMachineStatus(machineStatus);
        View view = binding.getRoot();

        RelativeLayout joggingStepFeedView = view.findViewById(R.id.jogging_step_feed_view);
        joggingStepFeedView.setOnClickListener(this);

        for(int resourceId : new Integer[]{R.id.jog_y_positive, R.id.jog_x_positive, R.id.jog_z_positive,
                R.id.jog_xy_top_left, R.id.jog_xy_top_right, R.id.jog_xy_bottom_left, R.id.jog_xy_bottom_right,
                R.id.jog_y_negative, R.id.jog_x_negative, R.id.jog_z_negative}){

            final IconButton iconButton = view.findViewById(resourceId);
            iconButton.setOnTouchListener(new RepeatListener(false, 300, 35));

            iconButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(isAdded()){
                        sendJogCommand(iconButton.getTag().toString());
                    }
                }
            });

        }

        for(int resourceId: new Integer[]{R.id.jog_cancel, R.id.run_homing_cycle, R.id.goto_x_zero, R.id.goto_y_zero, R.id.goto_z_zero}){
            IconButton iconButton = view.findViewById(resourceId);
            iconButton.setOnLongClickListener(this);
        }

        for(int resourceId: new Integer[]{R.id.run_homing_cycle, R.id.jog_cancel}){
            IconButton iconButton = view.findViewById(resourceId);
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

                        if(tag.equals(GrblUtils.GRBL_KILL_ALARM_LOCK_COMMAND)){
                            if(!machineStatus.getState().equals(Constants.MACHINE_STATUS_RUN)){
                                fragmentInteractionListener.onGcodeCommandReceived(tag);
                            }
                            return;
                        }

                        new AlertDialog.Builder(getActivity())
                                .setTitle(getString(R.string.text_zero_selected_axis))
                                .setMessage(getString(R.string.text_set_axis_location_in_current_wpos) + tag)
                                .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        sendCommandIfIdle(tag);
                                    }
                                })
                                .setNegativeButton(getString(R.string.text_no_confirm), null)
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
                        if(machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)){
                            sendCommandIfIdle(view.getTag().toString());
                            sendCommandIfIdle(GrblUtils.GRBL_VIEW_PARSER_STATE_COMMAND);
                            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_selected_coordinate_system) + view.getTag().toString()));
                        }else{
                            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
                        }
                    }
                });
                wposLayoutView.setOnLongClickListener(this);
            }
        }

        return view;
    }

    private void SetCustomButtons(View view){
        TableRow customButtonLayout = view.findViewById(R.id.custom_button_layout);
        if(customButtonLayout == null) return;

        if(sharedPref.getBoolean(getString(R.string.preference_enable_custom_buttons), false)){
            customButtonLayout.setVisibility(View.VISIBLE);

            for(int resourceId: new Integer[]{R.id.custom_button_1, R.id.custom_button_2, R.id.custom_button_3, R.id.custom_button_4}){
                IconButton iconButton = view.findViewById(resourceId);

                if(resourceId == R.id.custom_button_1) iconButton.setText(sharedPref.getString(getString(R.string.preference_custom_button_one), getString(R.string.text_value_na)));
                if(resourceId == R.id.custom_button_2) iconButton.setText(sharedPref.getString(getString(R.string.preference_custom_button_two), getString(R.string.text_value_na)));
                if(resourceId == R.id.custom_button_3) iconButton.setText(sharedPref.getString(getString(R.string.preference_custom_button_three), getString(R.string.text_value_na)));
                if(resourceId == R.id.custom_button_4) iconButton.setText(sharedPref.getString(getString(R.string.preference_custom_button_four), getString(R.string.text_value_na)));

                iconButton.setOnLongClickListener(this);
                iconButton.setOnClickListener(this);
            }
        }else{
            customButtonLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        switch(id){
            case R.id.jogging_step_feed_view:
                this.setJoggingStepAndFeed();
                return;

            case R.id.run_homing_cycle:
                if(machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE) || machineStatus.getState().equals(Constants.MACHINE_STATUS_ALARM)){
                    new AlertDialog.Builder(getActivity())
                            .setTitle(getString(R.string.text_homing_cycle))
                            .setMessage(getString(R.string.text_do_homing_cycle))
                            .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GRBL_RUN_HOMING_CYCLE);
                                }
                            })
                            .setNegativeButton(getString(R.string.text_no_confirm), null)
                            .show();
                }else{
                    EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
                }
                break;

            case R.id.jog_cancel:
                if(machineStatus.getState().equals(Constants.MACHINE_STATUS_JOG)){
                    fragmentInteractionListener.onGrblRealTimeCommandReceived(GrblUtils.GRBL_JOG_CANCEL_COMMAND);
                }

                if(customCommandsAsyncTask != null && customCommandsAsyncTask.getStatus() == AsyncTask.Status.RUNNING){
                    customCommandsAsyncTask.cancel(true);
                    fragmentInteractionListener.onGrblRealTimeCommandReceived(GrblUtils.GRBL_RESET_COMMAND);
                }

                break;

            case R.id.custom_button_1:
            case R.id.custom_button_2:
            case R.id.custom_button_3:
            case R.id.custom_button_4:
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
                        .setTitle(getString(R.string.text_set_coordinate_system))
                        .setMessage(getString(R.string.text_set_all_axes_zero))
                        .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                sendCommandIfIdle(GrblUtils.GCODE_RESET_COORDINATES_TO_ZERO);
                            }
                        })
                        .setNegativeButton(getString(R.string.text_no_confirm), null)
                        .show();
                return true;

            case R.id.jog_cancel:
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.text_return_to_zero_position))
                        .setMessage(getString(R.string.text_go_to_zero_position_in_current_wpos))
                        .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                for (String gCommand : GrblUtils.getReturnToHomeCommands(machineStatus.getWorkPosition())) {
                                    sendCommandIfIdle(gCommand);
                                }
                            }
                        })
                        .setNegativeButton(getString(R.string.text_no_confirm), null)
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

            case R.id.custom_button_1:
            case R.id.custom_button_2:
            case R.id.custom_button_3:
            case R.id.custom_button_4:
                customButton(id, true);
                return true;
        }

        return false;
    }

    private void customButton(int resourceId, boolean isLongClick){

        if(!machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)){
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
            return;
        }

        String title = "";
        String commands = "";
        boolean confirmFirst = true;

        if(resourceId == R.id.custom_button_1){
            title = sharedPref.getString(getString(R.string.preference_custom_button_one), getString(R.string.text_value_na));
            commands = isLongClick ? sharedPref.getString(getString(R.string.preference_custom_button_one_long_click), "") : sharedPref.getString(getString(R.string.preference_custom_button_one_short_click), "");
            confirmFirst = sharedPref.getBoolean(getString(R.string.preference_custom_button_one_confirm), true);
        }

        if(resourceId == R.id.custom_button_2){
            title = sharedPref.getString(getString(R.string.preference_custom_button_one), getString(R.string.text_value_na));
            commands = isLongClick ? sharedPref.getString(getString(R.string.preference_custom_button_two_long_click), "") : sharedPref.getString(getString(R.string.preference_custom_button_two_short_click), "");
            confirmFirst = sharedPref.getBoolean(getString(R.string.preference_custom_button_two_confirm), true);
        }

        if(resourceId == R.id.custom_button_3){
            title = sharedPref.getString(getString(R.string.preference_custom_button_one), getString(R.string.text_value_na));
            commands = isLongClick ? sharedPref.getString(getString(R.string.preference_custom_button_three_long_click), "") : sharedPref.getString(getString(R.string.preference_custom_button_three_short_click), "");
            confirmFirst = sharedPref.getBoolean(getString(R.string.preference_custom_button_three_confirm), true);
        }

        if(resourceId == R.id.custom_button_4){
            title = sharedPref.getString(getString(R.string.preference_custom_button_one), getString(R.string.text_value_na));
            commands = isLongClick ? sharedPref.getString(getString(R.string.preference_custom_button_four_long_click), "") : sharedPref.getString(getString(R.string.preference_custom_button_four_short_click), "");
            confirmFirst = sharedPref.getBoolean(getString(R.string.preference_custom_button_four_confirm), true);
        }

        if(commands.trim().length() <= 0){
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_empty_command), true, true));
            return;
        }

        final String finalCommands = commands;

        if(confirmFirst){
            String alertSummary = isLongClick ? getString(R.string.text_long_click) : getString(R.string.text_short_click);
            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.text_custom_action) + title)
                    .setMessage(getString(R.string.text_send_custom_command) + alertSummary + getString(R.string.text_on_button) + title)
                    .setPositiveButton(getString(R.string.text_send), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            customCommandsAsyncTask = new CustomCommandsAsyncTask();
                            customCommandsAsyncTask.execute(finalCommands);
                        }
                    })
                    .setNegativeButton(getString(R.string.text_cancel), null)
                    .show();
        }else{
            customCommandsAsyncTask = new CustomCommandsAsyncTask();
            customCommandsAsyncTask.execute(finalCommands);
        }

    }

    private class CustomCommandsAsyncTask extends AsyncTask<String, Integer, Integer>{

        private int MAX_RX_SERIAL_BUFFER = Constants.DEFAULT_SERIAL_RX_BUFFER - 3;
        private int CURRENT_RX_SERIAL_BUFFER;
        private LinkedList<Integer> activeCommandSizes;

        protected void onPreExecute(){
            MachineStatusListener.CompileTimeOptions compileTimeOptions = MachineStatusListener.getInstance().getCompileTimeOptions();
            if(compileTimeOptions.serialRxBuffer > 0) MAX_RX_SERIAL_BUFFER = compileTimeOptions.serialRxBuffer - 3;

            completedCommands = new ArrayBlockingQueue<>(Constants.DEFAULT_SERIAL_RX_BUFFER);
            activeCommandSizes = new LinkedList<>();
            CURRENT_RX_SERIAL_BUFFER = 0;
        }

        protected Integer doInBackground(String... commands){

            long startTime = System.currentTimeMillis();

            String lines[] = commands[0].split("[\r\n]+");
            for(String command: lines){
                if(isCancelled()) break;
                streamLine(command);
            }

            long endTime = System.currentTimeMillis();
            long timeTaken = (endTime - startTime)/1000;

            return 1;
        }

        private void streamLine(String gcodeCommand){

            int commandSize = gcodeCommand.length() + 1;

            // Wait until there is room, if necessary.
            while (MAX_RX_SERIAL_BUFFER < (CURRENT_RX_SERIAL_BUFFER + commandSize)) {
                try {
                    completedCommands.take();
                    if(activeCommandSizes.size() > 0) CURRENT_RX_SERIAL_BUFFER -= activeCommandSizes.removeFirst();
                } catch (InterruptedException e) {
                    Log.e(TAG, e.getMessage(), e);
                    return;
                }
            }

            activeCommandSizes.offer(commandSize);
            CURRENT_RX_SERIAL_BUFFER += commandSize;
            fragmentInteractionListener.onGcodeCommandReceived(gcodeCommand);
        }

    }

    private void gotoAxisZero(final String axis){
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.text_move) + axis + getString(R.string.text_axis_to_zero_position))
                .setMessage(getString(R.string.text_go_to_zero_position) + axis + "0")
                .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        sendCommandIfIdle("G0 " + axis + "0");
                    }
                })
                .setNegativeButton(getString(R.string.text_no_confirm), null)
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
                .setTitle(R.string.text_save_coordinate_system)
                .setMessage(getString(R.string.text_save_coordinate_system_desc) + " " + wpos + "?")
                .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        sendCommandIfIdle(String.format("G10 L20 %s X0Y0Z0", slot));
                    }
                })
                .setNegativeButton(getString(R.string.text_no_confirm), null)
                .show();
    }

    private void sendJogCommand(String tag){
        if(machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE) || machineStatus.getState().equals(Constants.MACHINE_STATUS_JOG)){
            String units = machineStatus.getJogging().inches ? "G20" : "G21";
            Double stepSize;
            if(tag.toUpperCase().contains("Z")){
                stepSize = machineStatus.getJogging().stepZ;
            }else{
                stepSize = machineStatus.getJogging().stepXY;
            }

            String jog = String.format(tag, units, stepSize, machineStatus.getJogging().feed);
            EventBus.getDefault().post(new JogCommandEvent(jog));
        }else{
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
        }
    }

    private void setJoggingStepAndFeed(){
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final ViewGroup nullParent = null;
        View view = inflater.inflate(R.layout.dialog_step_and_feed, nullParent, false);

        final IndicatorSeekBar jogStepSeekBarXY = view.findViewById(R.id.jog_xy_step_seek_bar);
        jogStepSeekBarXY.setProgress(machineStatus.getJogging().stepXY.floatValue());
        jogStepSeekBarXY.setMax(sharedPref.getInt(getString(R.string.preference_jogging_max_step_size), 10));
        jogStepSeekBarXY.setIndicatorTextFormat("XY: ${PROGRESS}");
        jogStepSeekBarXY.setDecimalScale(2);

        for(int resourceId: new Integer[]{R.id.jog_xy_step_small, R.id.jog_xy_step_medium, R.id.jog_xy_step_high}){
            final IconButton iconButton = view.findViewById(resourceId);

            iconButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    new AlertDialog.Builder(getActivity())
                            .setTitle("Change Quick Button Value")
                            .setMessage("do you want to change the quick button value to " + Float.toString(jogStepSeekBarXY.getProgressFloat()))
                            .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    EnhancedSharedPreferences.Editor editor = sharedPref.edit();
                                    editor.putString(iconButton.getTag().toString(), Float.toString(jogStepSeekBarXY.getProgressFloat())).commit();
                                }
                            })
                            .setNegativeButton(getString(R.string.text_no_confirm), null)
                            .show();

                    return true;
                }
            });

            iconButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(isAdded()){
                        String stepValue = sharedPref.getString(iconButton.getTag().toString(), "0");
                        if(stepValue.length() > 0){
                            float step_value = Float.valueOf(stepValue);
                            if(step_value > jogStepSeekBarXY.getMax()){
                                EventBus.getDefault().post(new UiToastEvent("Value is grater than the bar size", true, true));
                                return;
                            }

                            jogStepSeekBarXY.setProgress(step_value);
                            EventBus.getDefault().post(new UiToastEvent("XY Axis step value is set to " + String.valueOf(step_value)));
                            EnhancedSharedPreferences.Editor editor = sharedPref.edit();
                            editor.putDouble(getString(R.string.preference_jogging_step_size), Double.parseDouble(Float.toString(step_value))).commit();
                        }else{
                            EventBus.getDefault().post(new UiToastEvent("Invalid step size value, please check settings", true, true));
                        }
                    }
                }
            });
        }

        jogStepSeekBarXY.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {
                machineStatus.setJogging(Double.parseDouble(Float.toString(seekParams.progressFloat)), machineStatus.getJogging().stepZ, machineStatus.getJogging().feed, sharedPref.getBoolean(getString(R.string.preference_jogging_in_inches), false));
            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
                EnhancedSharedPreferences.Editor editor = sharedPref.edit();
                editor.putDouble(getString(R.string.preference_jogging_step_size), Double.parseDouble(Float.toString(seekBar.getProgressFloat()))).commit();
            }
        });

        final IndicatorSeekBar jogStepSeekBarZ = view.findViewById(R.id.jog_z_step_seek_bar);
        jogStepSeekBarZ.setProgress(machineStatus.getJogging().stepZ.floatValue());
        jogStepSeekBarZ.setMax(sharedPref.getInt(getString(R.string.preference_jogging_max_step_size_z), 5));
        jogStepSeekBarZ.setIndicatorTextFormat("Z: ${PROGRESS}");
        jogStepSeekBarZ.setDecimalScale(2);

        for(int resourceId: new Integer[]{R.id.jog_z_step_small, R.id.jog_z_step_medium, R.id.jog_z_step_high}){
            final IconButton iconButton = view.findViewById(resourceId);

            iconButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    new AlertDialog.Builder(getActivity())
                            .setTitle("Change Quick Button Value")
                            .setMessage("do you want to change the quick button value to " + Float.toString(jogStepSeekBarZ.getProgressFloat()))
                            .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    EnhancedSharedPreferences.Editor editor = sharedPref.edit();
                                    editor.putString(iconButton.getTag().toString(), Float.toString(jogStepSeekBarZ.getProgressFloat())).commit();
                                }
                            })
                            .setNegativeButton(getString(R.string.text_no_confirm), null)
                            .show();

                    return true;
                }
            });

            iconButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(isAdded()){
                        String stepValue = sharedPref.getString(iconButton.getTag().toString(), "0");
                        if(stepValue.length() > 0){
                            float step_value = Float.valueOf(stepValue);
                            if(step_value > jogStepSeekBarZ.getMax()){
                                EventBus.getDefault().post(new UiToastEvent("Value is grater than the bar size", true, true));
                                return;
                            }

                            jogStepSeekBarZ.setProgress(step_value);
                            EventBus.getDefault().post(new UiToastEvent("Z Axis step value is set to " + String.valueOf(step_value)));
                            EnhancedSharedPreferences.Editor editor = sharedPref.edit();
                            editor.putDouble(getString(R.string.preference_jogging_step_size_z), Double.parseDouble(Float.toString(step_value))).commit();
                        }else{
                            EventBus.getDefault().post(new UiToastEvent("Invalid step size value, please check settings", true, true));
                        }
                    }
                }
            });
        }

        jogStepSeekBarZ.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {
                machineStatus.setJogging(machineStatus.getJogging().stepXY, Double.parseDouble(Float.toString(seekParams.progressFloat)), machineStatus.getJogging().feed, sharedPref.getBoolean(getString(R.string.preference_jogging_in_inches), false));
            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
                EnhancedSharedPreferences.Editor editor = sharedPref.edit();
                editor.putDouble(getString(R.string.preference_jogging_step_size_z), Double.parseDouble(Float.toString(seekBar.getProgressFloat()))).commit();
            }
        });

        IndicatorSeekBar jogFeedSeekBar = view.findViewById(R.id.jog_feed_seek_bar);
        jogFeedSeekBar.setProgress(machineStatus.getJogging().feed.floatValue());
        Double maxFeedRate = sharedPref.getDouble(getString(R.string.preference_jogging_max_feed_rate), 2400.00);
        jogFeedSeekBar.setMax(Float.parseFloat(maxFeedRate.toString()));
        jogFeedSeekBar.setIndicatorTextFormat("Feed: ${PROGRESS}");

        jogFeedSeekBar.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {
                machineStatus.setJogging(machineStatus.getJogging().stepXY, machineStatus.getJogging().stepZ, seekParams.progress, sharedPref.getBoolean(getString(R.string.preference_jogging_in_inches), false));
            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
                EnhancedSharedPreferences.Editor editor = sharedPref.edit();
                editor.putDouble(getString(R.string.preference_jogging_feed_rate), seekBar.getProgress()).commit();
            }
        });

        SwitchCompat jogInches = view.findViewById(R.id.jog_inches);
        jogInches.setChecked(sharedPref.getBoolean(getString(R.string.preference_jogging_in_inches), false));
        jogInches.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                machineStatus.setJogging(machineStatus.getJogging().stepXY, machineStatus.getJogging().stepZ, machineStatus.getJogging().feed, b);
                EnhancedSharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(getString(R.string.preference_jogging_in_inches), b).commit();
            }
        });


        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(view);
        //alertDialogBuilder.setTitle(getString(R.string.text_jogging_step_and_feed));
        alertDialogBuilder.setCancelable(true)
                .setPositiveButton(getString(R.string.text_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });

        AlertDialog dialog = alertDialogBuilder.create();
        dialog.setCancelable(false);
        dialog.show();
    }

    private String[] rotateJogPad(int angle) {

        switch (angle){

            case 90:
                return new String[]{"$J=%1$sG91X-%2$sY-%2$sF%3$s", "$J=%sG91X-%sF%s", "$J=%1$sG91X-%2$sY%2$sF%3$s", "$J=%sG91Y-%sF%s", "$J=%sG91Y%sF%s", "$J=%1$sG91X%2$sY-%2$sF%3$s", "$J=%sG91X%sF%s", "$J=%1$sG91X%2$sY%2$sF%3$s"};

            case 180:
                return new String[]{"$J=%1$sG91X%2$sY-%2$sF%3$s", "$J=%sG91Y-%sF%s", "$J=%1$sG91X-%2$sY-%2$sF%3$s", "$J=%sG91X%sF%s", "$J=%sG91X-%sF%s", "$J=%1$sG91X%2$sY%2$sF%3$s", "$J=%sG91Y%sF%s", "$J=%1$sG91X-%2$sY%2$sF%3$s"};

            case 270:
                return new String[]{"$J=%1$sG91X%2$sY%2$sF%3$s", "$J=%sG91X%sF%s", "$J=%1$sG91X%2$sY-%2$sF%3$s", "$J=%sG91Y%sF%s", "$J=%sG91Y-%sF%s", "$J=%1$sG91X-%2$sY%2$sF%3$s", "$J=%sG91X-%sF%s", "$J=%1$sG91X-%2$sY-%2$sF%3$s"};

            default:
                return new String[]{"$J=%1$sG91X-%2$sY%2$sF%3$s", "$J=%sG91Y%sF%s", "$J=%1$sG91X%2$sY%2$sF%3$s", "$J=%sG91X-%sF%s", "$J=%sG91X%sF%s", "$J=%1$sG91X-%2$sY-%2$sF%3$s", "$J=%sG91Y-%sF%s", "$J=%1$sG91X%2$sY-%2$sF%3$s"};
        }
    }

    private void sendCommandIfIdle(String command){
        if(machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)){
            fragmentInteractionListener.onGcodeCommandReceived(command);
        }else{
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onGrblOkEvent(GrblOkEvent event){
        if(customCommandsAsyncTask != null && customCommandsAsyncTask.getStatus() == AsyncTask.Status.RUNNING){
            completedCommands.offer(1);
        }
    }


}
