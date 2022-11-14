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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import com.joanzapata.iconify.widget.IconButton;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentCamTabBinding;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.util.SimpleGcodeMaker;

public class CamTabFragment extends BaseFragment {

    //private static final String TAG = FacingTabFragment.class.getSimpleName();
    private MachineStatusListener machineStatus;
    private EnhancedSharedPreferences sharedPref;
    //private FileSenderListener fileSender;


    private TextView camFeedRate;
    private TextView camZTraversal;
    private TextView camStepOver;
    private TextView camZDeep;
    private TextView camZStep;
    private TextView camFromText;
    private TextView camToText;
    private TextView camToolDia;
    private String editIcon;
    private Double Xto = 0.0;
    private Double Yto = 0.0;
    private Double Zto  = 0.0;
    private Double Xfrom = 0.0;
    private Double Yfrom = 0.0;
    private Double Zfrom = 0.0;
    private int jobType=0;
    //double Ztraversal=0.0;
    public CamTabFragment() {}

    public static CamTabFragment newInstance() {
        return new CamTabFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        machineStatus = MachineStatusListener.getInstance();
        //fileSender = FileSenderListener.getInstance();
        sharedPref = EnhancedSharedPreferences.getInstance(requireActivity().getApplicationContext(), getString(R.string.shared_preference_key));

        //if(GrblActivity.isTablet(getActivity())){
        //    this.editIcon = " {fa-edit 22sp}";
        //}else{
            this.editIcon = " {fa-edit 16sp}";
        //}

        //EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        FragmentCamTabBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_cam_tab, container, false);
        binding.setMachineStatus(machineStatus);
        View view = binding.getRoot();



        final RelativeLayout camFeedRateView = view.findViewById(R.id.cam_feed_rate_view);
        camFeedRateView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCamFeedRate();
            }
        });

        RelativeLayout camZTraversalView = view.findViewById(R.id.cam_z_traversal_view);
        camZTraversalView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCamZTraversal();
            }
        });

        final RelativeLayout camStepOverView = view.findViewById(R.id.cam_step_over_view);
        camStepOverView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCamStepOver();
            }
        });
        final RelativeLayout camZdeepView = view.findViewById(R.id.cam_z_deep_view);
        camZdeepView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCamZDeep();
            }
        });
        final RelativeLayout camZstepView = view.findViewById(R.id.cam_z_step_view);
        camZstepView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCamZStep();
            }
        });
        final RelativeLayout camToolDiaView = view.findViewById(R.id.cam_tool_dia_view);
        camToolDiaView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCamToolDia();
            }
        });

        final Spinner jobTypeSpinner = view.findViewById(R.id.job_type_spinner);
        jobTypeSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //value = parent.getItemAtPosition(position);
                //switch (position) {
                jobType=position;

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                jobType=0;

            }
        });


        camFromText = view.findViewById(R.id.cam_from_text);
        camToText = view.findViewById(R.id.cam_to_text);

        camFeedRate = view.findViewById(R.id.cam_feed_rate);
        camFeedRate.setText(sharedPref.getString(getString(R.string.preference_cam_feed_rate), String.valueOf(Constants.CAM_FEED_RATE)) + this.editIcon);

        camZTraversal = view.findViewById(R.id.cam_z_traversal);
        camZTraversal.setText(sharedPref.getString(getString(R.string.preference_cam_z_traversal), String.valueOf(Constants.CAM_TRAVERSAL)) + this.editIcon);

        camStepOver = view.findViewById(R.id.cam_step_over);
        camStepOver.setText(sharedPref.getString(getString(R.string.preference_cam_step_over), String.valueOf(Constants.CAM_STEP_OVER)) + this.editIcon);

        camZDeep = view.findViewById(R.id.cam_z_deep);
        camZDeep.setText(sharedPref.getString(getString(R.string.preference_cam_z_deep), String.valueOf(Constants.CAM_ZDEEP)) + this.editIcon);

        camZStep = view.findViewById(R.id.cam_z_step);
        camZStep.setText(sharedPref.getString(getString(R.string.preference_cam_z_step), String.valueOf(Constants.CAM_ZSTEP)) + this.editIcon);

        camToolDia = view.findViewById(R.id.cam_tool_dia);
        camToolDia.setText(sharedPref.getString(getString(R.string.preference_cam_tool_dia), String.valueOf(Constants.CAM_TOOL_DIA)) + this.editIcon);



        //
        IconButton startCamCalc = view.findViewById(R.id.start_cam_calc);
        startCamCalc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.text_start_cam_calc))
                        .setMessage(getString(R.string.text_start_cam_calc_desc))
                        .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                doCamCalculation();
                            }
                        })
                        .setNegativeButton(getString(R.string.text_cancel), null)
                        .show();

            }


        });



        IconButton camFrom = view.findViewById(R.id.cam_from);
        camFrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.text_cam_from_title))
                        .setMessage(getString(R.string.text_cam_from_message))
                        .setPositiveButton(getString(R.string.text_ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                setCamFrom();
                            }
                        })
                        .setNegativeButton(getString(R.string.text_cancel), null)
                        .show();
            }
        });

        IconButton camTo = view.findViewById(R.id.cam_to);
        camTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.text_cam_to_title))
                        .setMessage(getString(R.string.text_cam_to_message))
                        .setPositiveButton(getString(R.string.text_ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                setCamTo();
                            }
                        })
                        .setNegativeButton(getString(R.string.text_cancel), null)
                        .show();
            }
        });


        RelativeLayout camHelp = view.findViewById(R.id.cam_help);
        camHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCamHelp();
            }
        });

        return view;
    }





    private void doCamCalculation(){

        //if(machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)){

            //mettere finestra per head e tail gcode

        double Ztraversal = Double.parseDouble(camZTraversal.getText().toString().replaceAll("[^\\d.]", ""));
        double cam_z_step = Double.parseDouble(camZStep.getText().toString().replaceAll("[^\\d.]", "") + "\n");
        double cam_z_deep = Double.parseDouble(camZDeep.getText().toString().replaceAll("[^\\d.]", "") + "\n");
        double cam_feedrate = Double.parseDouble(camFeedRate.getText().toString().replaceAll("[^\\d.]", "") + "\n");
        double cam_tool_dia = Double.parseDouble(camToolDia.getText().toString().replaceAll("[^\\d.]", "") + "\n");
        if(cam_z_step > cam_z_deep){
            EventBus.getDefault().post(new UiToastEvent("error Z step greater than Z deep", true, true));
        }

        double step_over= Double.parseDouble(camStepOver.getText().toString().replaceAll("[^\\d.]", ""));

        String gcode="";
        SimpleGcodeMaker gcodemaker;
        gcodemaker = new SimpleGcodeMaker(Xfrom,Yfrom,Xto,Yto,Zfrom, cam_z_step, cam_z_deep,Ztraversal, cam_feedrate,true);
        //gcodemaker = new SimpleGcodeMaker(0.0,0.0,300,400,-10.0, cam_z_step, cam_z_deep,Ztraversal, cam_feedrate,true);


        switch (jobType){
                case 0: //<item>rectangle facing feeding X</item>
                     gcode=gcodemaker.snakeX(step_over,true);
                     break;
                case 1: //<item>rectangle facing feeding Y</item>
                    gcode=gcodemaker.snakeY(step_over,true);
                     break;
                case 2: //<item>rectangle offset in</item>
                    //only out at moment
                    gcode=gcodemaker.cutRectangle(step_over,true);
                    break;
                case 3: //<item>rectangle offset out</item>
                    gcode=gcodemaker.cutRectangle(step_over,true);
                     break;
                case 4: //<item>circle facing in</item>
                    //only out at moment
                    gcode=gcodemaker.circleCut(step_over,true);
                     break;
                case 5: //<item>circle facing out</item>
                    gcode=gcodemaker.circleCut(step_over,true);
                    break;
                case 6: //<item>rectangle engraving</item>
                    gcode=gcodemaker.cutRectangle(0.0,true);
                    break;
                case 7: //<item>circle engraving</item>
                    gcode=gcodemaker.circleCut(0.0,true);
                    break;
                case 8: //<item>rectangle profile with cornering</item>
                    gcode=gcodemaker.CorneringCut(cam_tool_dia);
                    break;
                case 9: //<item>cut on line</item>
                    gcode=gcodemaker.lineCut ();
        }


            System.out.println(gcode);

        writeOnFile(gcode);



       // }else{
       //    EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
       // }
    }




    private void writeOnFile(String gcode){
        //su file path gia in uso
        String rootPath = "/storage/";

        if(sharedPref.getBoolean(getString(R.string.preference_remember_last_file_location), true)){
            String recentFile = sharedPref.getString(getString(R.string.most_recent_selected_file), null);
            if(recentFile != null){
                File f = new File(recentFile);
                do{
                    f = new File(Objects.requireNonNull(f.getParent()));
                    rootPath = f.getAbsolutePath();
                }while (!f.isDirectory());
            }
        }

        //save gcode job on file ,save at last position of open file??
        File jobFile;

        jobFile = new File(rootPath, "job1.nc");

        try {
            FileOutputStream fos = new FileOutputStream(jobFile);
            fos.write(gcode.getBytes());
            fos.flush();
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        EventBus.getDefault().post(new UiToastEvent("new job file at "+rootPath , true, true));

    }
    private void setCamFrom() {
        if(machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)){

            Xfrom = machineStatus.getWorkPosition().getCordX();
            Yfrom = machineStatus.getWorkPosition().getCordY();
            Zfrom = machineStatus.getWorkPosition().getCordZ();
            camFromText.setText(String.valueOf(Xfrom)+','+String.valueOf(Yfrom)+','+String.valueOf(Zfrom));
            System.out.println(Xfrom);


        }else{
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
        }
    }


    private void setCamTo() {
        if(machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)){
            Xto = machineStatus.getWorkPosition().getCordX();
            Yto = machineStatus.getWorkPosition().getCordY();
            Zto = machineStatus.getWorkPosition().getCordZ();
            camToText.setText(String.valueOf(Xto)+','+String.valueOf(Yto)+','+String.valueOf(Zto));


        }else{
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
        }
    }

    private void setCamFeedRate(){
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final ViewGroup nullParent = null;
        View v = inflater.inflate(R.layout.dialog_input_decimal, nullParent, false);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(v);
        alertDialogBuilder.setTitle(getString(R.string.text_cam_feedrate));

        final EditText editText = v.findViewById(R.id.dialog_input_decimal);
        editText.setText(sharedPref.getString(getString(R.string.preference_cam_feed_rate), "10.0"));
        editText.setSelection(editText.getText().length());

        final String faEditIcon = this.editIcon;
        alertDialogBuilder.setCancelable(true)
                .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String feedrate = editText.getText().toString();
                        if(feedrate.length() <=0) feedrate = "1";
                        sharedPref.edit().putString(getString(R.string.preference_cam_feed_rate), feedrate).apply();
                        camFeedRate.setText(feedrate  + faEditIcon);
                    }
                })
                .setNegativeButton(getString(R.string.text_cancel),
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

    private void setCamStepOver(){
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final ViewGroup nullParent = null;
        View v = inflater.inflate(R.layout.dialog_input_decimal, nullParent, false);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(v);
        alertDialogBuilder.setTitle(getString(R.string.text_cam_step_over));

        final EditText editText = v.findViewById(R.id.dialog_input_decimal);
        editText.setText(sharedPref.getString(getString(R.string.preference_cam_step_over), "10.0"));
        editText.setSelection(editText.getText().length());

        final String faEditIcon = this.editIcon;
        alertDialogBuilder.setCancelable(true)
                .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String stepover = editText.getText().toString();
                        if(stepover.length() <=0) stepover = "1";
                        sharedPref.edit().putString(getString(R.string.preference_cam_step_over), stepover).apply();
                         camStepOver.setText(stepover  + faEditIcon);
                    }
                })
                .setNegativeButton(getString(R.string.text_cancel),
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

    private void setCamZTraversal(){

            LayoutInflater inflater = LayoutInflater.from(getActivity());
            final ViewGroup nullParent = null;
            View v = inflater.inflate(R.layout.dialog_input_decimal, nullParent, false);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setView(v);
            alertDialogBuilder.setTitle(getString(R.string.text_cam_traversal));

            final EditText editText = v.findViewById(R.id.dialog_input_decimal);
            editText.setText(sharedPref.getString(getString(R.string.preference_cam_z_traversal), "10.0"));
            editText.setSelection(editText.getText().length());

            final String faEditIcon = this.editIcon;
            alertDialogBuilder.setCancelable(true)
                    .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String ztraversal = editText.getText().toString();
                            if(ztraversal.length() <=0) ztraversal = "1";
                            sharedPref.edit().putString(getString(R.string.preference_cam_z_traversal), ztraversal).apply();
                            camZTraversal.setText(ztraversal  + faEditIcon);
                        }
                    })
                    .setNegativeButton(getString(R.string.text_cancel),
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

    private void setCamZDeep(){

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final ViewGroup nullParent = null;
        View v = inflater.inflate(R.layout.dialog_input_decimal, nullParent, false);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(v);
        alertDialogBuilder.setTitle(getString(R.string.text_facing_zdeep));

        final EditText editText = v.findViewById(R.id.dialog_input_decimal);
        editText.setText(sharedPref.getString(getString(R.string.preference_cam_z_deep), "0.0"));
        editText.setSelection(editText.getText().length());

        final String faEditIcon = this.editIcon;
        alertDialogBuilder.setCancelable(true)
                .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String zdeep = editText.getText().toString();
                        sharedPref.edit().putString(getString(R.string.preference_cam_z_deep), zdeep).apply();
                        camZDeep.setText(zdeep  + faEditIcon);
                    }
                })
                .setNegativeButton(getString(R.string.text_cancel),
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

    private void setCamZStep(){

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final ViewGroup nullParent = null;
        View v = inflater.inflate(R.layout.dialog_input_decimal, nullParent, false);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(v);
        alertDialogBuilder.setTitle(getString(R.string.text_facing_zstep));

        final EditText editText = v.findViewById(R.id.dialog_input_decimal);
        editText.setText(sharedPref.getString(getString(R.string.preference_cam_z_step), "0.0"));
        editText.setSelection(editText.getText().length());

        final String faEditIcon = this.editIcon;
        alertDialogBuilder.setCancelable(true)
                .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String zstep = editText.getText().toString();
                        sharedPref.edit().putString(getString(R.string.preference_cam_z_step), zstep).apply();
                        camZStep.setText(zstep  + faEditIcon);
                    }
                })
                .setNegativeButton(getString(R.string.text_cancel),
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
    private void setCamToolDia(){

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final ViewGroup nullParent = null;
        View v = inflater.inflate(R.layout.dialog_input_decimal, nullParent, false);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(v);
        alertDialogBuilder.setTitle(getString(R.string.text_cam_tool_dia));

        final EditText editText = v.findViewById(R.id.dialog_input_decimal);
        editText.setText(sharedPref.getString(getString(R.string.preference_cam_tool_dia), "10.0"));
        editText.setSelection(editText.getText().length());

        final String faEditIcon = this.editIcon;
        alertDialogBuilder.setCancelable(true)
                .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String tooldia = editText.getText().toString();
                        sharedPref.edit().putString(getString(R.string.preference_cam_tool_dia), tooldia).apply();
                        camToolDia.setText(tooldia  + faEditIcon);
                    }
                })
                .setNegativeButton(getString(R.string.text_cancel),
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


    private void showCamHelp(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.text_manual_tool_change))
                .setMessage(R.string.text_cam_help)
                .setPositiveButton(getString(R.string.text_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) { }
                })
                .setCancelable(false);

        alertDialogBuilder.show();
    }
}

