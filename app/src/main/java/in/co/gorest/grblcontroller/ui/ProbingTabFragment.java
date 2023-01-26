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
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.databinding.DataBindingUtil;

import com.joanzapata.iconify.widget.IconButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import in.co.gorest.grblcontroller.GrblActivity;
import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentProbingTabBinding;
import in.co.gorest.grblcontroller.events.GrblProbeEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class ProbingTabFragment extends BaseFragment {

    private MachineStatusListener machineStatus;
    private EnhancedSharedPreferences sharedPref;

    private TextView probingFeedRate, probingPlateThickness, probingDistance;
    private Double probeStartPosition = null;
    private SwitchCompat autoZeroAfterProbe;
    private Integer probeType;
    private String editIcon;

    public ProbingTabFragment() {}

    public static ProbingTabFragment newInstance() {
        return new ProbingTabFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        machineStatus = MachineStatusListener.getInstance();
        sharedPref = EnhancedSharedPreferences.getInstance(requireActivity().getApplicationContext(), getString(R.string.shared_preference_key));

        if(GrblActivity.isTablet(requireActivity())){
            this.editIcon = " {fa-edit 22sp}";
        }else{
            this.editIcon = " {fa-edit 16sp}";
        }

        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        FragmentProbingTabBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_probing_tab, container, false);
        binding.setMachineStatus(machineStatus);
        View view = binding.getRoot();

        final RelativeLayout probingFeedRateView = view.findViewById(R.id.probing_feed_rate_view);
        probingFeedRateView.setOnClickListener(view13 -> setProbingFeedRate());

        RelativeLayout probingPlateThicknessView = view.findViewById(R.id.probing_plate_thickness_view);
        probingPlateThicknessView.setOnClickListener(view14 -> setProbingPlateThickness());

        final RelativeLayout probingDistanceView = view.findViewById(R.id.probing_distance_view);
        probingDistanceView.setOnClickListener(view15 -> setProbingDistance());

        probingFeedRate = view.findViewById(R.id.probing_feed_rate);
        probingFeedRate.setText(sharedPref.getString(getString(R.string.preference_probing_feed_rate), String.valueOf(Constants.PROBING_FEED_RATE)) + this.editIcon);

        probingPlateThickness = view.findViewById(R.id.probing_plate_thickness);
        probingPlateThickness.setText(sharedPref.getString(getString(R.string.preference_probing_plate_thickness), String.valueOf(Constants.PROBING_PLATE_THICKNESS)) + this.editIcon);

        probingDistance = view.findViewById(R.id.probing_distance);
        probingDistance.setText(sharedPref.getString(getString(R.string.preference_probing_distance), String.valueOf(Constants.PROBING_DISTANCE)) + this.editIcon);

        IconButton startProbe = view.findViewById(R.id.start_probe);
        startProbe.setOnClickListener(view12 -> new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.text_straight_probe))
                .setMessage(getString(R.string.text_straight_probe_desc))
                .setPositiveButton(getString(R.string.text_yes_confirm), (dialog, which) -> {
                    probeType = Constants.PROBE_TYPE_NORMAL;
                    doProbing();
                })
                .setNegativeButton(getString(R.string.text_cancel), null)
                .show());

        IconButton startToolOffset = view.findViewById(R.id.start_tool_length_offset);
        startToolOffset.setOnClickListener(view1 -> {
            if(machineStatus.getLastProbePosition() == null){
                EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_last_probe_location_unknown), true, true));
                return;
            }

            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.text_dynamic_tool_length_offset))
                    .setMessage(getString(R.string.text_dynamic_tlo_desc))
                    .setPositiveButton(getString(R.string.text_ok), (dialog, which) -> {
                        probeType = Constants.PROBE_TYPE_TOOL_OFFSET;
                        doProbing();
                    })
                    .setNegativeButton(getString(R.string.text_cancel), null)
                    .show();
        });

        IconButton cancelToolOffset = view.findViewById(R.id.cancel_tool_offset);
        cancelToolOffset.setOnClickListener(view16 -> {
            if(machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)){
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.text_cancel_tlo))
                        .setMessage(getString(R.string.text_cancel_tlo_desc))
                        .setPositiveButton(getString(R.string.text_yes_confirm), (dialog, which) -> {
                            fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GCODE_CANCEL_TOOL_OFFSETS);
                            fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GRBL_VIEW_GCODE_PARAMETERS_COMMAND);
                        })
                        .setNegativeButton(getString(R.string.text_no_confirm), null)
                        .show();
            }else{
                EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
            }
        });

        autoZeroAfterProbe = view.findViewById(R.id.auto_zero_after_probe);

        RelativeLayout probingHelp = view.findViewById(R.id.probing_help);
        probingHelp.setOnClickListener(view17 -> showProbingHelp());

        return view;
    }

    private void doProbing(){
        if(machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)){

            fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GRBL_VIEW_PARSER_STATE_COMMAND);

            String probeDistance = sharedPref.getString(getString(R.string.preference_probing_distance), String.valueOf(Constants.PROBING_DISTANCE));
            final String probeFeedRate = sharedPref.getString(getString(R.string.preference_probing_feed_rate), String.valueOf(Constants.PROBING_FEED_RATE));
            final double distanceToProbe = machineStatus.getWorkPosition().getCordZ() - Double.parseDouble(probeDistance);
            probeStartPosition = machineStatus.getMachinePosition().getCordZ();

            // Wait for few milliseconds, just to make sure we got the parser state
            new Handler().postDelayed(() -> {
                String distanceMode = machineStatus.getParserState().distanceMode;
                String unitSelection = machineStatus.getParserState().unitSelection;

                fragmentInteractionListener.onGcodeCommandReceived("G38.3 Z" + distanceToProbe + " F" + probeFeedRate);
                fragmentInteractionListener.onGcodeCommandReceived(distanceMode + unitSelection);
            }, (Constants.GRBL_STATUS_UPDATE_INTERVAL + 100));


        }else{
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
        }
    }

    @SuppressLint("SetTextI18n")
    private void setProbingDistance(){
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View v = inflater.inflate(R.layout.dialog_input_decimal, null, false);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(v);
        alertDialogBuilder.setTitle(getString(R.string.text_probing_distance));

        final EditText editText = v.findViewById(R.id.dialog_input_decimal);
        editText.setText(sharedPref.getString(getString(R.string.preference_probing_distance), "10.0"));
        editText.setSelection(editText.getText().length());

        final String faEditIcon = this.editIcon;
        alertDialogBuilder.setCancelable(true)
                .setPositiveButton(getString(R.string.text_yes_confirm), (dialog, id) -> {
                    String distance = editText.getText().toString();
                    if(distance.length() <=0) distance = "0";
                    sharedPref.edit().putString(getString(R.string.preference_probing_distance), distance).apply();
                    probingDistance.setText(distance  + faEditIcon);
                })
                .setNegativeButton(getString(R.string.text_cancel),
                        (dialog, id) -> dialog.cancel());

        AlertDialog dialog = alertDialogBuilder.create();
        if(dialog.getWindow() != null){
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        dialog.show();
    }

    @SuppressLint("SetTextI18n")
    private void setProbingPlateThickness(){
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View v = inflater.inflate(R.layout.dialog_input_decimal, null, false);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(v);
        alertDialogBuilder.setTitle(getString(R.string.text_touch_plate_thickness));

        final EditText editText = v.findViewById(R.id.dialog_input_decimal);
        editText.setText(sharedPref.getString(getString(R.string.preference_probing_plate_thickness), "10.0"));
        editText.setSelection(editText.getText().length());

        final String faEditIcon = this.editIcon;
        alertDialogBuilder.setCancelable(true)
                .setPositiveButton(getString(R.string.text_ok), (dialog, id) -> {
                    String thickness = editText.getText().toString();
                    if(thickness.length() <= 0) thickness = "0";
                    sharedPref.edit().putString(getString(R.string.preference_probing_plate_thickness), thickness).apply();
                    probingPlateThickness.setText(thickness + faEditIcon);
                })
                .setNegativeButton(getString(R.string.text_cancel),
                        (dialog, id) -> dialog.cancel());

        AlertDialog dialog = alertDialogBuilder.create();
        if(dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();

    }

    @SuppressLint("SetTextI18n")
    private void setProbingFeedRate(){
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View v = inflater.inflate(R.layout.dialog_input_decimal, null, false);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(v);
        alertDialogBuilder.setTitle(getString(R.string.text_probing_feed_rate));

        final EditText editText = v.findViewById(R.id.dialog_input_decimal);
        editText.setText(sharedPref.getString(getString(R.string.preference_probing_feed_rate), "10.0"));
        editText.setSelection(editText.getText().length());

        final String faEditIcon = this.editIcon;
        alertDialogBuilder.setCancelable(true)
                .setPositiveButton(getString(R.string.text_ok), (dialog, id) -> {
                    String feedRate = editText.getText().toString();
                    if(feedRate.length() <= 0) feedRate = "0";
                    sharedPref.edit().putString(getString(R.string.preference_probing_feed_rate), feedRate).apply();
                    probingFeedRate.setText(feedRate + faEditIcon);
                })
                .setNegativeButton(getString(R.string.text_cancel), (dialog, id) -> dialog.cancel());

        AlertDialog dialog = alertDialogBuilder.create();
        if(dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private void showProbingHelp(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.text_manual_tool_change))
                .setMessage(R.string.text_probing_help)
                .setPositiveButton(getString(R.string.text_ok), (dialog, which) -> { })
                .setCancelable(false);

        alertDialogBuilder.show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGrblProbeEvent(GrblProbeEvent event){
        if(probeType == null || probeStartPosition == null) return;

        if(!event.getIsProbeSuccess()){
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_probe_failed), true, true));
            fragmentInteractionListener.onGcodeCommandReceived("G53G0Z" + probeStartPosition.toString());
            probeType = null;
            return;
        }

        if(probeType == Constants.PROBE_TYPE_NORMAL){
            if(autoZeroAfterProbe.isChecked()){
                double probePlateThickness = Double.parseDouble(sharedPref.getString(getString(R.string.preference_probing_plate_thickness), String.valueOf(Constants.PROBING_PLATE_THICKNESS)));
                fragmentInteractionListener.onGcodeCommandReceived("G53G0Z" + event.getProbeCordZ().toString());
                fragmentInteractionListener.onGcodeCommandReceived("G10L20P0Z" + probePlateThickness);
                autoZeroAfterProbe.setChecked(false);
                EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_probe_success_auto_zero)));
            }else{
                EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_probe_success)));
            }
        }

        if(probeType == Constants.PROBE_TYPE_TOOL_OFFSET){
            Double lastProbeCordZ = Math.abs(machineStatus.getLastProbePosition().getCordZ());
            Double currentProbeCordZ = Math.abs(event.getProbeCordZ());

            double toolOffset =  lastProbeCordZ - currentProbeCordZ;
            fragmentInteractionListener.onGcodeCommandReceived("G43.1Z" + toolOffset);
            fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GRBL_VIEW_GCODE_PARAMETERS_COMMAND);
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_probe_success_with_tlo)));
        }

        fragmentInteractionListener.onGcodeCommandReceived("G53G0Z" + probeStartPosition.toString());
        probeStartPosition = null; probeType = null;
        machineStatus.setLastProbePosition(event.getProbePosition());
    }

}
