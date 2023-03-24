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

package in.co.gorest.grblcontroller;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;
import com.joanzapata.iconify.fonts.FontAwesomeModule;
import com.joanzapata.iconify.widget.IconTextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import es.dmoral.toasty.Toasty;
import in.co.gorest.grblcontroller.databinding.ActivityMainBinding;
import in.co.gorest.grblcontroller.events.ConsoleMessageEvent;
import in.co.gorest.grblcontroller.events.GrblAlarmEvent;
import in.co.gorest.grblcontroller.events.GrblErrorEvent;
import in.co.gorest.grblcontroller.events.StreamingCompleteEvent;
import in.co.gorest.grblcontroller.events.StreamingStartedEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.helpers.NotificationHelper;
import in.co.gorest.grblcontroller.helpers.ReaderViewPagerTransformer;
import in.co.gorest.grblcontroller.listeners.ConsoleLoggerListener;
import in.co.gorest.grblcontroller.listeners.FileSenderListener;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.service.FileStreamerIntentService;
import in.co.gorest.grblcontroller.service.GrblBluetoothSerialService;
import in.co.gorest.grblcontroller.ui.BaseFragment;
import in.co.gorest.grblcontroller.ui.GrblFragmentPagerAdapter;
import in.co.gorest.grblcontroller.util.GrblUtils;

public abstract class GrblActivity extends AppCompatActivity implements BaseFragment.OnFragmentInteractionListener{

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    protected EnhancedSharedPreferences sharedPref;
    protected ConsoleLoggerListener consoleLogger = null;
    protected MachineStatusListener machineStatus = null;
    protected GrblBluetoothSerialService grblBluetoothSerialService = null;

    public static boolean isAppRunning;

    private Toast lastToast;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        sharedPref = EnhancedSharedPreferences.getInstance(GrblController.getInstance(), getString(R.string.shared_preference_key));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setSubtitle(getString(R.string.text_not_connected));

        applicationSetup();
        binding.setMachineStatus(machineStatus);

        CardView viewLastToast = findViewById(R.id.view_last_toast);
        viewLastToast.setOnLongClickListener(view -> {
            if(lastToast != null) lastToast.show();
            return true;
        });

        for(int resourceId: new Integer[]{R.id.wpos_edit_x, R.id.wpos_edit_y, R.id.wpos_edit_z}){
            IconTextView positionTextView = findViewById(resourceId);
            positionTextView.setOnClickListener(v -> setWorkPosition(v.getTag().toString()));
        }

        Iconify.with(new FontAwesomeModule());
        setupTabLayout();
        checkPowerManagement();
    }

    private boolean hasPaidVersion() {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo("in.co.gorest.grblcontroller.plus", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {}

        return false;
    }


    @Override
    public void onDestroy(){
        super.onDestroy();

        stopService(new Intent(this, FileStreamerIntentService.class));
        ConsoleLoggerListener.resetClass();
        FileSenderListener.resetClass();
        MachineStatusListener.resetClass();
        isAppRunning = false;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        lastToast = null;
    }

    @Override
    public void onBackPressed(){ moveTaskToBack(true); }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if(menu != null){
            MenuItem actionGrblSoftReset = menu.findItem(R.id.action_grbl_reset);
            actionGrblSoftReset.setIcon(new IconDrawable(this, FontAwesomeIcons.fa_power_off).colorRes(R.color.colorWhite).sizeDp(24));

        }

        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case R.id.app_settings:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                return true;

            case  R.id.app_notifications:
                startActivity(new Intent(getApplicationContext(), NotificationArchiveActivity.class));
                return true;

            case R.id.app_about:
                startActivity(new Intent(getApplicationContext(), AboutActivity.class));
                return true;

            case R.id.share:
                try {
                    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");
                    String shareBodyText = "Grbl Controller. Very cool CNC controller for grbl firmware https://goo.gl/aVnvp4";

                    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,"Grbl Controller");
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBodyText);
                    startActivity(Intent.createChooser(sharingIntent, "Sharing Option"));
                }catch (ActivityNotFoundException e){
                    showToastMessage("No application available to perform this action!", true, true);
                }

                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    protected void applicationSetup(){
        NotificationHelper notificationHelper = new NotificationHelper(this);
        notificationHelper.createChannels();

        consoleLogger = ConsoleLoggerListener.getInstance();
        machineStatus = MachineStatusListener.getInstance();
        machineStatus.setJogging(sharedPref.getDouble(getString(R.string.preference_jogging_step_size), 1.00),
                sharedPref.getDouble(getString(R.string.preference_jogging_step_size_z), 0.1),
                sharedPref.getDouble(getString(R.string.preference_jogging_feed_rate), 2400.0),
                sharedPref.getBoolean(getString(R.string.preference_jogging_in_inches), false));
        machineStatus.setVerboseOutput(sharedPref.getBoolean(getString(R.string.preference_console_verbose_mode), false));
        machineStatus.setIgnoreError20(sharedPref.getBoolean(getString(R.string.preference_ignore_error_20), false));
        machineStatus.setUsbBaudRate(Integer.parseInt(sharedPref.getString(getString(R.string.usb_serial_baud_rate), Constants.USB_BAUD_RATE)));
        machineStatus.setSingleStepMode(sharedPref.getBoolean(getString(R.string.preference_single_step_mode), false));
        machineStatus.setCustomStartUpString(sharedPref.getString(getString(R.string.preference_start_up_string), ""));

    }

    protected void setupTabLayout(){
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        if(isTablet(this)){
            tabLayout.addTab(tabLayout.newTab().setIcon(new IconDrawable(this, FontAwesomeIcons.fa_arrows_alt).colorRes(R.color.colorAccent).sizeDp(32)));
            tabLayout.addTab(tabLayout.newTab().setIcon(new IconDrawable(this, FontAwesomeIcons.fa_file_text).colorRes(R.color.colorAccent).sizeDp(32)));
            tabLayout.addTab(tabLayout.newTab().setIcon(new IconDrawable(this, FontAwesomeIcons.fa_crosshairs).colorRes(R.color.colorAccent).sizeDp(32)));
            tabLayout.addTab(tabLayout.newTab().setIcon(new IconDrawable(this, FontAwesomeIcons.fa_television).colorRes(R.color.colorAccent).sizeDp(32)));
            tabLayout.addTab(tabLayout.newTab().setIcon(new IconDrawable(this, FontAwesomeIcons.fa_arrows_alt).colorRes(R.color.colorAccent).sizeDp(32)));

        }else{
            tabLayout.addTab(tabLayout.newTab().setIcon(new IconDrawable(this, FontAwesomeIcons.fa_arrows_alt).colorRes(R.color.colorAccent).sizeDp(21)));
            tabLayout.addTab(tabLayout.newTab().setIcon(new IconDrawable(this, FontAwesomeIcons.fa_file_text).colorRes(R.color.colorAccent).sizeDp(21)));
            tabLayout.addTab(tabLayout.newTab().setIcon(new IconDrawable(this, FontAwesomeIcons.fa_crosshairs).colorRes(R.color.colorAccent).sizeDp(21)));
            tabLayout.addTab(tabLayout.newTab().setIcon(new IconDrawable(this, FontAwesomeIcons.fa_television).colorRes(R.color.colorAccent).sizeDp(21)));
            tabLayout.addTab(tabLayout.newTab().setIcon(new IconDrawable(this, FontAwesomeIcons.fa_arrows_alt).colorRes(R.color.colorAccent).sizeDp(21)));

        }


        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = findViewById(R.id.tab_layout_pager);
        final GrblFragmentPagerAdapter pagerAdapter = new GrblFragmentPagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        viewPager.setPageTransformer(false, new ReaderViewPagerTransformer(ReaderViewPagerTransformer.TransformType.DEPTH));
        viewPager.setOffscreenPageLimit(1);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setWorkPosition(final String axisLabel){
        LayoutInflater inflater = LayoutInflater.from(this);
        View v = inflater.inflate(R.layout.dialog_input_decimal_signed, null, false);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(v);
        alertDialogBuilder.setTitle(getString(R.string.test_set_cordinate_system, axisLabel));
        alertDialogBuilder.setMessage(getString(R.string.test_set_cordinate_system_description, axisLabel));

        final EditText editText = v.findViewById(R.id.dialog_input_decimal_signed);
        if(axisLabel.equalsIgnoreCase("X")) editText.setText(String.valueOf(machineStatus.getWorkPosition().getCordX()));
        if(axisLabel.equalsIgnoreCase("Y")) editText.setText(String.valueOf(machineStatus.getWorkPosition().getCordY()));
        if(axisLabel.equalsIgnoreCase("Z")) editText.setText(String.valueOf(machineStatus.getWorkPosition().getCordZ()));
        editText.setSelection(editText.getText().length());

        alertDialogBuilder.setCancelable(true)
                .setPositiveButton(getString(R.string.text_ok), (dialog, id) -> {
                    String axisValue = editText.getText().toString();
                    if(axisValue.length() > 0){
                        sendCommandIfIdle("G10L20P0" + axisLabel + axisValue);
                    }
                })
                .setNeutralButton("/2",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                Double val = Double.parseDouble( editText.getText().toString());
                                val=val/2;
                                String axisValue=val.toString();
                                if(axisValue.length() > 0){
                                    sendCommandIfIdle("G10L20P0" + axisLabel + axisValue);
                                }
                            }
                        })
                .setNegativeButton(getString(R.string.text_cancel), (dialog, id) -> dialog.cancel());

        AlertDialog dialog = alertDialogBuilder.create();
        if(dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private void sendCommandIfIdle(String command){
        if(machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)){
            onGcodeCommandReceived(command);
        }else{
            showToastMessage(getString(R.string.text_machine_not_idle), true, true);
        }
    }

    protected void showToastMessage(String message){
        this.showToastMessage(message, false, false);
    }

    @SuppressLint("ShowToast")
    protected void showToastMessage(String message, Boolean longToast, Boolean isWarning){
        if(isWarning){
            lastToast = Toasty.warning(this, message, longToast ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT, true);
        }else{
            lastToast = Toasty.success(this, message, longToast ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        }

        lastToast.setGravity(Gravity.FILL_HORIZONTAL|Gravity.TOP, 0, 120);
        lastToast.show();
    }

    @Override
    public void onGcodeCommandReceived(String command) {

    }

    @Override
    public void onGrblRealTimeCommandReceived(byte command) {

    }

    public static boolean isTablet(Context context){
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    private void checkPowerManagement(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);

            if(pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())){

                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.text_power_management_warning_title))
                        .setMessage(getString(R.string.text_power_management_warning_description))
                        .setPositiveButton(getString(R.string.text_settings), (dialog, which) -> {
                            try {
                                Intent myIntent = new Intent();
                                myIntent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                                startActivity(myIntent);
                            } catch (RuntimeException ignored) {}
                        })
                        .setNegativeButton(getString(R.string.text_cancel), null)
                        .setCancelable(false)
                        .show();

            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGrblAlarmEvent(GrblAlarmEvent event){
        consoleLogger.offerMessage(event.toString());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void  onGrblErrorEvent(GrblErrorEvent event){
        consoleLogger.offerMessage(event.toString());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConsoleMessageEvent(ConsoleMessageEvent event){
        consoleLogger.offerMessage(event.getMessage());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUiToastEvent(UiToastEvent event){
        showToastMessage(event.getMessage(), event.getLongToast(), event.getIsWarning());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void OnStreamingCompleteEvent(StreamingCompleteEvent event){
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void OnStreamingStartEvent(StreamingStartedEvent event){
        if(sharedPref.getBoolean(getString(R.string.preference_keep_screen_on), false)){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Fragment fragment = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.tab_layout_pager + ":" + 1);
        if(fragment != null) fragment.onActivityResult(requestCode, resultCode, data);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event){

        if(keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE){
            if(machineStatus.getState().equals(Constants.MACHINE_STATUS_RUN)){
                onGrblRealTimeCommandReceived(GrblUtils.GRBL_PAUSE_COMMAND);
                return true;
            }

            if(machineStatus.getState().equals(Constants.MACHINE_STATUS_HOLD)){
                onGrblRealTimeCommandReceived(GrblUtils.GRBL_RESUME_COMMAND);
                return true;
            }
        }

        return false;
    }

}
