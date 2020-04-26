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
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.joanzapata.iconify.widget.IconButton;

import java.util.List;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.adapters.CommandHistoryAdapter;
import in.co.gorest.grblcontroller.databinding.FragmentConsoleTabBinding;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.listeners.ConsoleLoggerListener;
import in.co.gorest.grblcontroller.listeners.EndlessRecyclerViewScrollListener;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.CommandHistory;
import in.co.gorest.grblcontroller.model.GcodeCommand;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class ConsoleTabFragment extends BaseFragment {

    private MachineStatusListener machineStatus;
    private ConsoleLoggerListener consoleLogger;
    private EnhancedSharedPreferences sharedPref;
    private ViewSwitcher viewSwitcher;
    private List<CommandHistory> dataSet;
    private CommandHistoryAdapter commandHistoryAdapter;
    private EditText commandInput;

    public ConsoleTabFragment() {}

    public static ConsoleTabFragment newInstance() {
        return new ConsoleTabFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = EnhancedSharedPreferences.getInstance(getActivity(), getString(R.string.shared_preference_key));
        consoleLogger = ConsoleLoggerListener.getInstance();
        machineStatus = MachineStatusListener.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        FragmentConsoleTabBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_console_tab, container, false);
        View view = binding.getRoot();
        binding.setConsole(consoleLogger);
        binding.setMachineStatus(machineStatus);

        viewSwitcher = view.findViewById(R.id.console_view_switcher);
        final TextView consoleLogView = view.findViewById(R.id.console_logger);
        consoleLogView.setMovementMethod(new ScrollingMovementMethod());
        commandInput = view.findViewById(R.id.command_input);

        final EditText commandInput = view.findViewById(R.id.command_input);

        consoleLogView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().getParent().getParent().getParent().requestDisallowInterceptTouchEvent(true);
                switch (event.getAction() & MotionEvent.ACTION_MASK){
                    case MotionEvent.ACTION_UP:
                        v.getParent().getParent().getParent().getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
                return false;
            }
        });

        IconButton sendCommand = view.findViewById(R.id.send_command);
        sendCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String commandText = commandInput.getText().toString();
                if(commandText.length() > 0){
                    GcodeCommand gcodeCommand = new GcodeCommand(commandText);
                    fragmentInteractionListener.onGcodeCommandReceived(gcodeCommand.getCommandString());
                    CommandHistory.saveToHistory(commandText, gcodeCommand.getCommandString());
                    dataSet.clear();
                    dataSet.addAll(CommandHistory.getHistory("0", "15"));
                    commandHistoryAdapter.notifyDataSetChanged();
                    if(gcodeCommand.getHasRomAccess()){
                        fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GRBL_VIEW_PARSER_STATE_COMMAND);
                        fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GRBL_VIEW_GCODE_PARAMETERS_COMMAND);
                    }

                    if(gcodeCommand.getCommandString().toUpperCase().contains("G43.1Z")){
                        fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GRBL_VIEW_GCODE_PARAMETERS_COMMAND);
                    }

                    if(gcodeCommand.getCommandString().equals("$32=1")) machineStatus.setLaserModeEnabled(true);
                    if(gcodeCommand.getCommandString().equals("$32=0")) machineStatus.setLaserModeEnabled(false);
                    commandInput.setText(null);
                    viewSwitcher.setDisplayedChild(0);
                }
            }
        });

        sendCommand.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.text_clear_console))
                        .setMessage(getString(R.string.text_clear_console_description))
                        .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                consoleLogger.clearMessages();
                            }
                        })
                        .setNegativeButton(getString(R.string.text_no_confirm), null)
                        .show();

                return true;
            }
        });

        final SwitchCompat consoleVerboseOutput = view.findViewById(R.id.console_verbose_output);
        consoleVerboseOutput.setChecked(sharedPref.getBoolean(getString(R.string.preference_console_verbose_mode), false));
        consoleVerboseOutput.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                MachineStatusListener.getInstance().setVerboseOutput(b);
                sharedPref.edit().putBoolean(getString(R.string.preference_console_verbose_mode), b).apply();
            }
        });

        IconButton consoleHistory = view.findViewById(R.id.console_history);
        consoleHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewSwitcher.showNext();
            }
        });

        dataSet = CommandHistory.getHistory("0", "15");
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        commandHistoryAdapter = new CommandHistoryAdapter(dataSet);
        commandHistoryAdapter.setItemClickListener(onItemClickListener);
        commandHistoryAdapter.setItemLongClickListner(onItemLongClickListener);
        recyclerView.setAdapter(commandHistoryAdapter);

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);

        recyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                String offset = String.valueOf(page * 15);
                List<CommandHistory> moreItems = CommandHistory.getHistory(offset, "15");
                dataSet.addAll(moreItems);
                commandHistoryAdapter.notifyItemRangeInserted(commandHistoryAdapter.getItemCount(), dataSet.size() - 1);
            }
        });

        return view;
    }

    private View.OnClickListener onItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            RecyclerView.ViewHolder viewHolder = (RecyclerView.ViewHolder) view.getTag();
            int position = viewHolder.getAdapterPosition();
            if(position == RecyclerView.NO_POSITION) return;
            CommandHistory commandHistory = dataSet.get(position);
            commandInput.append(commandHistory.getCommand());
        }
    };

    private View.OnLongClickListener onItemLongClickListener = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(View view) {
            final RecyclerView.ViewHolder viewHolder = (RecyclerView.ViewHolder) view.getTag();
            final int position = viewHolder.getAdapterPosition();
            if(position == RecyclerView.NO_POSITION) return false;
            final CommandHistory commandHistory = dataSet.get(position);

            new AlertDialog.Builder(getActivity())
                    .setTitle(commandHistory.getCommand())
                    .setMessage(getString(R.string.text_delete_command_history_confirm))
                    .setPositiveButton(getActivity().getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            commandHistory.delete();
                            dataSet.remove(position);
                            commandHistoryAdapter.notifyItemRemoved(position);
                            commandHistoryAdapter.notifyItemRangeChanged(position, dataSet.size());
                        }
                    }).setNegativeButton(getActivity().getString(R.string.text_cancel), null).setCancelable(true).show();

            return true;
        }
    };

}
