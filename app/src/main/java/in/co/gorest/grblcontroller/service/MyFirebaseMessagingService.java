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

package in.co.gorest.grblcontroller.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import in.co.gorest.grblcontroller.BuildConfig;
import in.co.gorest.grblcontroller.SplashActivity;
import in.co.gorest.grblcontroller.helpers.NotificationHelper;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = MyFirebaseInstanceIDService.class.getSimpleName();

    private static final String KEY_NOTIFICATION_TITLE      = "title";
    private static final String KEY_NOTIFICATION_MESSAGE    = "message";
    private static final String KEY_CHANNEL_TYPE            = "type";
    private static final String KEY_CATEGORY_NAME           = "category_name";
    private static final String KEY_CATEGORY_VALUE          = "category_value";

    private static final String TEXT_CATEGORY_UPDATE        = "update";
    private static final String TEXT_CATEGORY_LINK          = "link";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            try{

                String notificationChannel = remoteMessage.getData().get(KEY_CHANNEL_TYPE);

                if(notificationChannel.equalsIgnoreCase(NotificationHelper.CHANNEL_BUG_TRACKER_NAME)){
                    notificationBugTracker(remoteMessage);
                }

                if(notificationChannel.equalsIgnoreCase(NotificationHelper.CHANNEL_GENERAL_NAME)){
                    notificationGeneral(remoteMessage);
                }

            }catch (Exception e){
                Crashlytics.logException(e);
                Log.d(TAG, e.getMessage());;
            }
        }

    }

    private void notificationBugTracker(RemoteMessage remoteMessage){

        String notificationTitle = remoteMessage.getData().get(KEY_NOTIFICATION_TITLE);
        String notificationMessage = remoteMessage.getData().get(KEY_NOTIFICATION_MESSAGE);
        String categoryName = remoteMessage.getData().get(KEY_CATEGORY_NAME);
        String categoryValue = remoteMessage.getData().get(KEY_CATEGORY_VALUE);

        if(notificationTitle == null || notificationMessage == null || categoryName == null || categoryValue == null) return;

        NotificationHelper notificationHelper = new NotificationHelper(this);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(categoryValue));
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        notificationHelper.getNotificationGeneral(notificationTitle, notificationMessage, pendingIntent);
    }

    private void notificationGeneral(RemoteMessage remoteMessage){

        String notificationTitle = remoteMessage.getData().get(KEY_NOTIFICATION_TITLE);
        String notificationMessage = remoteMessage.getData().get(KEY_NOTIFICATION_MESSAGE);
        String categoryName = remoteMessage.getData().get(KEY_CATEGORY_NAME);
        String categoryValue = remoteMessage.getData().get(KEY_CATEGORY_VALUE);

        if(notificationTitle == null || notificationMessage == null || categoryName == null || categoryValue == null) return;

        NotificationHelper notificationHelper = new NotificationHelper(this);

        if(categoryName.equalsIgnoreCase(TEXT_CATEGORY_UPDATE)){
            int versionCode = Integer.valueOf(categoryValue);

            if(versionCode > BuildConfig.VERSION_CODE){
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=" + getPackageName()));
                final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
                notificationHelper.getNotificationGeneral(notificationTitle, notificationMessage, pendingIntent);
            }

        }else if(categoryName.equalsIgnoreCase(TEXT_CATEGORY_LINK)){

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(categoryValue));
            final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
            notificationHelper.getNotificationGeneral(notificationTitle, notificationMessage, pendingIntent);

        }else{
            Intent intent = new Intent(this, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
            notificationHelper.getNotificationGeneral(notificationTitle, notificationMessage, pendingIntent);
        }

    }

}