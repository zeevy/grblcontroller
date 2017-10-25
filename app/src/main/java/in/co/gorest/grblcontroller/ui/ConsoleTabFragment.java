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
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.joanzapata.iconify.widget.IconButton;

import org.greenrobot.eventbus.EventBus;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentConsoleTabBinding;
import in.co.gorest.grblcontroller.events.GrblSettingMessageEvent;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.listners.ConsoleLoggerListner;
import in.co.gorest.grblcontroller.listners.MachineStatusListner;
import in.co.gorest.grblcontroller.util.GrblLookups;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class ConsoleTabFragment extends BaseFragment {

    private MachineStatusListner machineStatus;
    private ConsoleLoggerListner consoleLogger;
    private EnhancedSharedPreferences sharedPref;

    public ConsoleTabFragment() {}

    public static ConsoleTabFragment newInstance() {
        return new ConsoleTabFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = EnhancedSharedPreferences.getInstance(getActivity(), getString(R.string.shared_preference_key));
        consoleLogger = ConsoleLoggerListner.getInstance();
        machineStatus = MachineStatusListner.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        FragmentConsoleTabBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_console_tab, container, false);
        View view = binding.getRoot();
        binding.setConsoleLogger(consoleLogger);
        binding.setMachineStatus(machineStatus);

        TextView consoleLogView = view.findViewById(R.id.console_logger);
        consoleLogView.setMovementMethod(new ScrollingMovementMethod());

        final EditText commandInput = view.findViewById(R.id.command_input);

        IconButton sendCommand = view.findViewById(R.id.send_command);
        sendCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String command = commandInput.getText().toString();
                if(command.length() > 0){
                    fragmentInteractionListener.onGcodeCommandReceived(command.trim());
                    if(GrblUtils.isGrblSettingMessage(command.trim())){
                        GrblLookups grblSettings = new GrblLookups(getActivity().getApplicationContext(), "setting_codes");
                        EventBus.getDefault().post(new GrblSettingMessageEvent(grblSettings, command.trim()));
                    }
                    commandInput.setText(null);
                }
            }
        });

        sendCommand.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                new AlertDialog.Builder(getActivity())
                        .setTitle("Clear console messages?")
                        .setMessage("this will clear all console history")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                consoleLogger.clearMessages();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();

                return true;
            }
        });

        final SwitchCompat consoleVerboseOutput = view.findViewById(R.id.console_verbose_output);
        consoleVerboseOutput.setChecked(sharedPref.getBoolean(getString(R.string.console_verbose_mode), false));
        consoleVerboseOutput.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                MachineStatusListner.getInstance().setVerboseOutput(b);
                sharedPref.edit().putBoolean(getString(R.string.console_verbose_mode), b).apply();
            }
        });

        return view;
    }

}
