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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;
import com.joanzapata.iconify.fonts.FontAwesomeModule;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Method;

import in.co.gorest.grblcontroller.databinding.ActivityMainBinding;
import in.co.gorest.grblcontroller.events.ConsoleMessageEvent;
import in.co.gorest.grblcontroller.events.GrblAlarmEvent;
import in.co.gorest.grblcontroller.events.GrblErrorEvent;
import in.co.gorest.grblcontroller.events.GrblOkEvent;
import in.co.gorest.grblcontroller.events.JogCommandEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.helpers.NotificationHelper;
import in.co.gorest.grblcontroller.helpers.ReaderViewPagerTransformer;
import in.co.gorest.grblcontroller.listners.ConsoleLoggerListner;
import in.co.gorest.grblcontroller.listners.FileSenderListner;
import in.co.gorest.grblcontroller.listners.MachineStatusListner;
import in.co.gorest.grblcontroller.service.FileStreamerIntentService;
import in.co.gorest.grblcontroller.service.GrblBluetoothSerialService;
import in.co.gorest.grblcontroller.ui.BaseFragment;
import in.co.gorest.grblcontroller.ui.GrblFragmentPagerAdapter;

public abstract class GrblActivity extends AppCompatActivity implements BaseFragment.OnFragmentInteractionListener{

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    protected EnhancedSharedPreferences sharedPref;
    protected ConsoleLoggerListner consoleLogger = null;
    protected MachineStatusListner machineStatus = null;
    protected GrblBluetoothSerialService grblBluetoothSerialService = null;

    String lastToastMessage = null;
    private Toast toastMessage;
    public static boolean isAppRunning;

    private static final int VIBRATION_LENGTH_SHORT = 80;
    private static final int VIBRATION_LENGTH_LOING = 500;
    private static final int VIBRATION_LENGTH_VERY_LOING = 1500;
    private Vibrator vibrator;
    private boolean vibrationEnabled = false;


    protected final CircularFifoQueue<JogCommandEvent> jogCommandQueue = new CircularFifoQueue<>(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        sharedPref = EnhancedSharedPreferences.getInstance(GrblConttroller.getContext(), getString(R.string.shared_preference_key));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setSubtitle(getString(R.string.text_not_connected));

        applicationSetup();
        binding.setMachineStatus(machineStatus);

        CardView viewLastToast = findViewById(R.id.view_last_toast);
        viewLastToast.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(lastToastMessage != null) grblToast(lastToastMessage);
                return true;
            }
        });


        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        Iconify.with(new FontAwesomeModule());
        setupTabLayout(R.id.tab_layout, R.id.tab_layout_pager);
    }

    @Override
    public void onResume(){
        super.onResume();
        vibrationEnabled = sharedPref.getBoolean(getString(R.string.enable_haptic_feedback), false);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        stopService(new Intent(this, FileStreamerIntentService.class));
        ConsoleLoggerListner.resetClass();
        FileSenderListner.resetClass();
        MachineStatusListner.resetClass();
        isAppRunning = false;
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case R.id.app_settings:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
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
                    Crashlytics.logException(e);
                    grblToast("No application available to perform this action!");
                }

                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    protected void applicationSetup(){
        NotificationHelper notificationHelper = new NotificationHelper(this);
        notificationHelper.createChannels();

        consoleLogger = ConsoleLoggerListner.getInstance();
        machineStatus = MachineStatusListner.getInstance();
        machineStatus.setJogging(sharedPref.getDouble(getString(R.string.jogging_step_size), 1.0), sharedPref.getDouble(getString(R.string.jogging_feed_rate), 2400.0), sharedPref.getBoolean(getString(R.string.jogging_in_inches), false));
        machineStatus.setVerboseOutput(sharedPref.getBoolean(getString(R.string.console_verbose_mode), false));

    }

    protected void setupTabLayout(int tabLayoutId, int tabLayoutPagerId){
        TabLayout tabLayout = findViewById(tabLayoutId);
        tabLayout.addTab(tabLayout.newTab().setIcon(new IconDrawable(this, FontAwesomeIcons.fa_arrows_alt).colorRes(R.color.colorAccent).sizeDp(21)));
        tabLayout.addTab(tabLayout.newTab().setIcon(new IconDrawable(this, FontAwesomeIcons.fa_file_text).colorRes(R.color.colorAccent).sizeDp(21)));
        tabLayout.addTab(tabLayout.newTab().setIcon(new IconDrawable(this, FontAwesomeIcons.fa_crosshairs).colorRes(R.color.colorAccent).sizeDp(21)));
        tabLayout.addTab(tabLayout.newTab().setIcon(new IconDrawable(this, FontAwesomeIcons.fa_television).colorRes(R.color.colorAccent).sizeDp(21)));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = findViewById(tabLayoutPagerId);
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

    protected void grblToast(String message){

        if(toastMessage == null){
            toastMessage = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
            toastMessage.setGravity(Gravity.FILL_HORIZONTAL|Gravity.TOP, 0, 120);
        }

        toastMessage.setText(message);
        toastMessage.show();
        this.lastToastMessage = message;
    }

    public void vibrateShort(){
        if(vibrationEnabled) vibrator.vibrate(VIBRATION_LENGTH_SHORT);
    }

    public void vibrateLong(){
        if(vibrationEnabled) vibrator.vibrate(VIBRATION_LENGTH_LOING);
    }

    public void vibrateVeryLong(){
        if(vibrationEnabled) vibrator.vibrate(VIBRATION_LENGTH_VERY_LOING);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGrblOkEvent(GrblOkEvent event){
        if(jogCommandQueue.size() > 0) jogCommandQueue.poll();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGrblAlarmEvent(GrblAlarmEvent event){
        consoleLogger.setMessages(event.toString());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void  onGrblErrorEvent(GrblErrorEvent event){
        if(event.getErrorCode() == 15){
            jogCommandQueue.clear();
        }
        consoleLogger.setMessages(event.toString());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConsoleMessageEvent(ConsoleMessageEvent event){
        consoleLogger.setMessages(event.getMessage());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUiToastEvent(UiToastEvent event){
        grblToast(event.getMessage());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Fragment fragment = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.tab_layout_pager + ":" + 1);
        if(fragment != null) fragment.onActivityResult(requestCode, resultCode, data);
    }

}
