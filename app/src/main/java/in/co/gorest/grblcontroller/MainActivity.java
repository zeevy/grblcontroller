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

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;
import com.joanzapata.iconify.fonts.FontAwesomeModule;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import in.co.gorest.grblcontroller.databinding.ActivityMainBinding;
import in.co.gorest.grblcontroller.events.ConsoleMessageEvent;
import in.co.gorest.grblcontroller.events.GrblAlarmEvent;
import in.co.gorest.grblcontroller.events.GrblErrorEvent;
import in.co.gorest.grblcontroller.events.GrblOkEvent;
import in.co.gorest.grblcontroller.events.GrblSettingMessageEvent;
import in.co.gorest.grblcontroller.events.JogCommandEvent;
import in.co.gorest.grblcontroller.events.StreamingCompleteEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.helpers.ReaderViewPagerTransformer;
import in.co.gorest.grblcontroller.listners.ConsoleLoggerListner;
import in.co.gorest.grblcontroller.listners.MachineStatusListner;
import in.co.gorest.grblcontroller.ui.BaseFragment;
import in.co.gorest.grblcontroller.ui.ConsoleTabFragment;
import in.co.gorest.grblcontroller.ui.FileSenderTabFragment;
import in.co.gorest.grblcontroller.ui.JoggingTabFragment;
import in.co.gorest.grblcontroller.ui.ProbingTabFragment;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class MainActivity extends GrblActivity implements BaseFragment.OnFragmentInteractionListener{

    private static final String TAG = MainActivity.class.getSimpleName();

    private ConsoleLoggerListner consoleLogger = null;
    private MachineStatusListner machineStatus = null;

    private int MAX_PLANNER_BUFFER = 14;

    private final CircularFifoQueue<JogCommandEvent> jogCommandQueue = new CircularFifoQueue<>(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setSubtitle(getString(R.string.text_not_connected));

        consoleLogger = ConsoleLoggerListner.getInstance();
        machineStatus = MachineStatusListner.getInstance();
        machineStatus.setJogging(sharedPref.getDouble(getString(R.string.jogging_step_size), 1.0), sharedPref.getDouble(getString(R.string.jogging_feed_rate), 2400.0), sharedPref.getBoolean(getString(R.string.jogging_in_inches), false));
        machineStatus.setVerboseOutput(sharedPref.getBoolean(getString(R.string.console_verbose_mode), false));

        binding.setMachineStatus(machineStatus);

        CardView viewLastToast = findViewById(R.id.view_last_toast);
        viewLastToast.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(lastToastMessage != null) grblToast(lastToastMessage);
                return true;
            }
        });

        if(machineStatus.getCompileTimeOptions().plannerBuffer > 0) MAX_PLANNER_BUFFER = machineStatus.getCompileTimeOptions().plannerBuffer - 1;

        Iconify.with(new FontAwesomeModule());
        this.setupTabLayout();

    }

    private void setupTabLayout(){
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setIcon(new IconDrawable(this, FontAwesomeIcons.fa_arrows_alt).colorRes(R.color.colorAccent).sizeDp(21)));
        tabLayout.addTab(tabLayout.newTab().setIcon(new IconDrawable(this, FontAwesomeIcons.fa_file_text).colorRes(R.color.colorAccent).sizeDp(21)));
        tabLayout.addTab(tabLayout.newTab().setIcon(new IconDrawable(this, FontAwesomeIcons.fa_crosshairs).colorRes(R.color.colorAccent).sizeDp(21)));
        tabLayout.addTab(tabLayout.newTab().setIcon(new IconDrawable(this, FontAwesomeIcons.fa_television).colorRes(R.color.colorAccent).sizeDp(21)));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = findViewById(R.id.tab_layout_pager);
        final PagerAdapter pagerAdapter = new PagerAdapter (getSupportFragmentManager(), tabLayout.getTabCount());
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

    private class PagerAdapter extends FragmentPagerAdapter {

        public final int tabCount;

        public PagerAdapter(FragmentManager fragmentManager, int tabCount) {
            super(fragmentManager);
            this.tabCount = tabCount;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return JoggingTabFragment.newInstance();
                case 1: return FileSenderTabFragment.newInstance();
                case 2: return ProbingTabFragment.newInstance();
                case 3: return ConsoleTabFragment.newInstance();
                default: return JoggingTabFragment.newInstance();
            }
        }

        @Override
        public int getCount() {return tabCount; }

    }

    @Override
    public void onGcodeCommandReceived(String command) {
        super.onGcodeCommandReceived(command);
    }

    public void onGrblRealTimeCommandReceived(byte command) {
        switch(command){
            case GrblUtils.GRBL_JOG_CANCEL_COMMAND:
                jogCommandQueue.clear();
                break;
        }

        super.onGrblRealTimeCommandReceived(command);
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
    public synchronized void onJogCommandEvent(JogCommandEvent event){

        if(machineStatus.getState().equals(MachineStatusListner.STATE_IDLE) || machineStatus.getState().equals(MachineStatusListner.STATE_JOG) && jogCommandQueue.size() == 0){
            jogCommandQueue.offer(event);
            onGcodeCommandReceived(event.getCommand());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConsoleMessageEvent(ConsoleMessageEvent event){
        consoleLogger.setMessages(event.getMessage());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUiToastEvent(UiToastEvent event){
        grblToast(event.getMessage());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void OnStreamingCompleteEvent(StreamingCompleteEvent event){
        if(sharedPref.getBoolean(getString(R.string.sleep_after_job), false) && !machineStatus.getState().equals(MachineStatusListner.STATE_CHECK)){
            onGcodeCommandReceived(GrblUtils.GRBL_SLEEP_COMMAND);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGrblSettingMessageEvent(GrblSettingMessageEvent event){
        if(event.getSetting().equals("$10") && !event.getValue().equals("2")){
            onGcodeCommandReceived("$10=2");
        }

        if(event.getSetting().equals("$110") || event.getSetting().equals("$111") || event.getSetting().equals("$112")){
            Double maxFeedRate = Double.parseDouble(event.getValue());
            if(maxFeedRate > sharedPref.getDouble(getString(R.string.jogging_max_feed_rate), machineStatus.getJogging().feed)){
                sharedPref.edit().putDouble(getString(R.string.jogging_max_feed_rate), maxFeedRate).apply();
            }
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.tab_layout_pager + ":" + 1);
        if(fragment != null) fragment.onActivityResult(requestCode, resultCode, data);
    }

}
