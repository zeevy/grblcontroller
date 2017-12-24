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
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.joanzapata.iconify.widget.IconButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentProbingTabBinding;
import in.co.gorest.grblcontroller.events.GrblProbeEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.listners.MachineStatusListner;

public class ProbingTabFragment extends BaseFragment {

    private static final String TAG = ProbingTabFragment.class.getSimpleName();
    private MachineStatusListner machineStatus;
    private EnhancedSharedPreferences sharedPref;

    private TextView probingFeedrate, probingPlateThickness, probingDistance;

    private static final int PROBE_TYPE_AUTO_ZERO = 1;
    private static final int PROBE_TYPE_TOOL_CHANGE = 2;

    private Integer probeType = null;
    private Double probeStartPosition = null;
    private Double lastProbePosition = null;

    private SwitchCompat autoZeroAfterProbe;

    public ProbingTabFragment() {}

    public static ProbingTabFragment newInstance() {
        return new ProbingTabFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        machineStatus = MachineStatusListner.getInstance();
        sharedPref = EnhancedSharedPreferences.getInstance(getActivity().getApplicationContext(), getString(R.string.shared_preference_key));
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        FragmentProbingTabBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_probing_tab, container, false);
        binding.setMachineStatus(machineStatus);
        View view = binding.getRoot();

        final RelativeLayout probingFeedrateView = view.findViewById(R.id.probing_feedrate_view);
        probingFeedrateView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setProbingFeedrate();
            }
        });

        RelativeLayout probingPlateThicknessView = view.findViewById(R.id.probing_plate_thickness_view);
        probingPlateThicknessView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setProbingPlateThickness();
            }
        });

        final RelativeLayout probingDistanceView = view.findViewById(R.id.probing_distance_view);
        probingDistanceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setProbingDistance();
            }
        });

        probingFeedrate = view.findViewById(R.id.probing_feedrate);
        probingFeedrate.setText(sharedPref.getString(getString(R.string.probing_feedrate), "10.0"));

        probingPlateThickness = view.findViewById(R.id.probing_plate_thickness);
        probingPlateThickness.setText(sharedPref.getString(getString(R.string.probing_plate_thickness), "10.0"));

        probingDistance = view.findViewById(R.id.probing_distance);
        probingDistance.setText(sharedPref.getString(getString(R.string.probing_distance), "10.0"));

        IconButton startProbe = view.findViewById(R.id.start_probe);
        startProbe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(machineStatus.getState().equals(MachineStatusListner.STATE_IDLE)){
                    probeType = PROBE_TYPE_AUTO_ZERO;

                    String probeDistance = probingDistance.getText().toString();
                    String probeFeedrate = probingFeedrate.getText().toString();
                    Double distanceToProbe = machineStatus.getWorkPosition().getCordZ() - Double.parseDouble(probeDistance);
                    probeStartPosition = machineStatus.getMachinePosition().getCordZ();

                    String distanceMode = machineStatus.getParserState().distanceMode;
                    String unitSelection = machineStatus.getParserState().unitSelection;

                    fragmentInteractionListener.onGcodeCommandReceived("G38.3 Z" + distanceToProbe.toString() + " F" + probeFeedrate);
                    fragmentInteractionListener.onGcodeCommandReceived(distanceMode + unitSelection);
                }else{
                    EventBus.getDefault().post(new UiToastEvent(getString(R.string.machine_not_idle)));
                }

            }
        });

        autoZeroAfterProbe = view.findViewById(R.id.auto_zero_after_probe);

        RelativeLayout probingHelp = view.findViewById(R.id.probing_help);
        probingHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showProbingHelp();
            }
        });

        return view;
    }

    private void setProbingDistance(){
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final ViewGroup nullParent = null;
        View v = inflater.inflate(R.layout.dialog_input_decimal, nullParent, false);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(v);
        alertDialogBuilder.setTitle("Probing distance");

        final EditText editText = v.findViewById(R.id.dialog_input_decimal);
        editText.setText(sharedPref.getString(getString(R.string.probing_distance), "10.0"));
        editText.setSelection(editText.getText().length());

        alertDialogBuilder.setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String feedRate = editText.getText().toString();
                        sharedPref.edit().putString(getString(R.string.probing_distance), feedRate).apply();
                        probingDistance.setText(feedRate);
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog dialog = alertDialogBuilder.create();
        if(dialog.getWindow() != null){
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        dialog.show();
    }

    private void setProbingPlateThickness(){
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final ViewGroup nullParent = null;
        View v = inflater.inflate(R.layout.dialog_input_decimal, nullParent, false);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(v);
        alertDialogBuilder.setTitle("Probing plate thickness");

        final EditText editText = v.findViewById(R.id.dialog_input_decimal);
        editText.setText(sharedPref.getString(getString(R.string.probing_plate_thickness), "10.0"));
        editText.setSelection(editText.getText().length());

        alertDialogBuilder.setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String feedRate = editText.getText().toString();
                        sharedPref.edit().putString(getString(R.string.probing_plate_thickness), feedRate).apply();
                        probingPlateThickness.setText(feedRate);
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog dialog = alertDialogBuilder.create();
        if(dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();

    }

    private void setProbingFeedrate(){
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final ViewGroup nullParent = null;
        View v = inflater.inflate(R.layout.dialog_input_decimal, nullParent, false);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(v);
        alertDialogBuilder.setTitle("Probing feed rate");

        final EditText editText = v.findViewById(R.id.dialog_input_decimal);
        editText.setText(sharedPref.getString(getString(R.string.probing_feedrate), "10.0"));
        editText.setSelection(editText.getText().length());

        alertDialogBuilder.setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String feedRate = editText.getText().toString();
                        sharedPref.edit().putString(getString(R.string.probing_feedrate), feedRate).apply();
                        probingFeedrate.setText(feedRate);
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog dialog = alertDialogBuilder.create();
        if(dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private void showProbingHelp(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity())
                .setTitle("Probing help")
                .setMessage(R.string.probing_help_text)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) { }
                })
                .setCancelable(false);

        alertDialogBuilder.show();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGrblProbeEvent(GrblProbeEvent event){
        if(probeType == null || probeStartPosition == null) return;
        if(!event.getIsProbeSuccess()){
            EventBus.getDefault().post(new UiToastEvent("Probe failed, please try again"));
            fragmentInteractionListener.onGcodeCommandReceived("G53 G0 Z" + probeStartPosition.toString());
            return;
        }

        Double probePlateThickness = Double.parseDouble(probingPlateThickness.getText().toString());

        if(probeType == PROBE_TYPE_AUTO_ZERO && autoZeroAfterProbe.isChecked()){
            fragmentInteractionListener.onGcodeCommandReceived("G53 G0 Z" + event.getProbeCordZ().toString());
            fragmentInteractionListener.onGcodeCommandReceived("G10 L20 P0 Z" + probePlateThickness);
        }

        fragmentInteractionListener.onGcodeCommandReceived("G53 G0 Z" + probeStartPosition.toString());
        probeStartPosition = null;
        probeType = null;
    }

}
