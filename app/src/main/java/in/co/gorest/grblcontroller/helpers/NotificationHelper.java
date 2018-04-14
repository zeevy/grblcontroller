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

package in.co.gorest.grblcontroller.helpers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessaging;

import in.co.gorest.grblcontroller.R;

public class NotificationHelper extends ContextWrapper {

    private NotificationManager notificationManager;

    public static final String CHANNEL_GENERAL_ID               = "in.co.gorest.grblcontroller.GENERAL_SERVICE";
    public static final String CHANNEL_GENERAL_NAME             = "General";
    public static final String CHANNEL_GENERAL_ABOUT            = "application specific news, features and updates information.";

    public static final String CHANNEL_BUG_TRACKER_ID           = "in.co.gorest.grblcontroller.BUG_TRACKER_SERVICE";
    public static final String CHANNEL_BUG_TRACKER_NAME         = "BugTracker";
    public static final String CHANNEL_BUG_TRACKER_ABOUT        = "notifications about recent application bugs, issues and resolutions.";

    public static final String CHANNEL_SERVICE_ID               = "in.co.gorest.grblcontroller.APPLICATION_SERVICE";
    public static final String CHANNEL_SERVICE_NAME             = "Service";
    public static final String CHANNEL_SERVICE_ABOUT            = "Service notification when app is working in foreground.";

    public NotificationHelper(Context base) {
        super(base);
    }

    public void createChannels(){

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannelOne = new NotificationChannel(CHANNEL_GENERAL_ID, CHANNEL_GENERAL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            mChannelOne.setDescription(CHANNEL_GENERAL_ABOUT);
            mChannelOne.enableLights(false);
            mChannelOne.setLightColor(getColor(R.color.colorPrimary));
            mChannelOne.setShowBadge(true);
            mChannelOne.enableVibration(false);
            mChannelOne.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            getNotificationManager().createNotificationChannel(mChannelOne);

            NotificationChannel mChannelTwo = new NotificationChannel(CHANNEL_BUG_TRACKER_ID, CHANNEL_BUG_TRACKER_NAME, NotificationManager.IMPORTANCE_HIGH);
            mChannelTwo.setDescription(CHANNEL_BUG_TRACKER_ABOUT);
            mChannelTwo.enableLights(true);
            mChannelTwo.enableVibration(true);
            mChannelTwo.setLightColor(getColor(R.color.colorPrimary));
            mChannelTwo.setShowBadge(true);
            getNotificationManager().createNotificationChannel(mChannelTwo);

            NotificationChannel mChannelThree = new NotificationChannel(CHANNEL_SERVICE_ID, CHANNEL_SERVICE_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            mChannelThree.setDescription(CHANNEL_SERVICE_ABOUT);
            mChannelThree.enableLights(false);
            mChannelThree.enableVibration(false);
            mChannelThree.setLightColor(getColor(R.color.colorPrimary));
            mChannelThree.setShowBadge(true);
            mChannelThree.setSound(null, null);
            getNotificationManager().createNotificationChannel(mChannelThree);
        }
    }

    public void getNotificationGeneral(String title, String message, PendingIntent pendingIntent){

        NotificationCompat.Builder notificationBuilder =  new NotificationCompat.Builder(getApplicationContext(), CHANNEL_GENERAL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        this.notify((int)System.currentTimeMillis(), notificationBuilder);
    }

    public void getNotificationBugTracker(String title, String message, PendingIntent pendingIntent){

        NotificationCompat.Builder notificationBuilder =  new NotificationCompat.Builder(getApplicationContext(), CHANNEL_BUG_TRACKER_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        this.notify((int)System.currentTimeMillis(), notificationBuilder);
    }

    public void notify(int id, NotificationCompat.Builder notification) {
        getNotificationManager().notify(id, notification.build());
    }

    private NotificationManager getNotificationManager() {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }
}
