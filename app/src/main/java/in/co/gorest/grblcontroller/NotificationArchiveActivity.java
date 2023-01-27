/*
 *
 *  *  /**
 *  *  * Copyright (C) 2017  Grbl Controller Contributors
 *  *  *
 *  *  * This program is free software; you can redistribute it and/or modify
 *  *  * it under the terms of the GNU General Public License as published by
 *  *  * the Free Software Foundation; either version 2 of the License, or
 *  *  * (at your option) any later version.
 *  *  *
 *  *  * This program is distributed in the hope that it will be useful,
 *  *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *  * GNU General Public License for more details.
 *  *  *
 *  *  * You should have received a copy of the GNU General Public License along
 *  *  * with this program; if not, write to the Free Software Foundation, Inc.,
 *  *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *  *  * <http://www.gnu.org/licenses/>
 *  *
 *
 */

package in.co.gorest.grblcontroller;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Objects;

import in.co.gorest.grblcontroller.adapters.NotificationAdapter;
import in.co.gorest.grblcontroller.events.FcmNotificationRecieved;
import in.co.gorest.grblcontroller.listeners.EndlessRecyclerViewScrollListener;
import in.co.gorest.grblcontroller.model.GrblNotification;

public class NotificationArchiveActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_archive);

        Objects.requireNonNull(getSupportActionBar()).setSubtitle("notification archive");

        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume(){
        super.onResume();

        final List<GrblNotification> dataSet = GrblNotification.find(GrblNotification.class, null, null, null, "id DESC", "0, 10");
        if(dataSet.size() == 0){
            TextView emptyView = findViewById(R.id.empty_view);
            emptyView.setVisibility(View.VISIBLE);
        }

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        final NotificationAdapter notificationAdapter = new NotificationAdapter(this, dataSet);
        recyclerView.setAdapter(notificationAdapter);

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        recyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                String limit = page * 10 + ", 10";
                List<GrblNotification> moreItems = GrblNotification.find(GrblNotification.class, null, null, null, "id DESC", limit);
                dataSet.addAll(moreItems);
                notificationAdapter.notifyItemRangeInserted(notificationAdapter.getItemCount(), dataSet.size() - 1);
            }
        });
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFcmNotificationReceived(FcmNotificationRecieved notificationReceived){

    }

}
