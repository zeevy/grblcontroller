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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import in.co.gorest.grblcontroller.BuildConfig;
import in.co.gorest.grblcontroller.MainActivity;
import in.co.gorest.grblcontroller.R;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = MyFirebaseInstanceIDService.class.getSimpleName();

    private static final String KEY_NOTIFICATION_TYPE   = "type";
    private static final String KEY_TITLE               = "title";
    private static final String KEY_MESSAGE             = "message";
    private static final String KEY_VERSION_CODE        = "version";
    private static final String KEY_LINK                = "link";

    private static final String TYPE_UPDATE_AVAILABLE   = "update";
    private static final String TYPE_WEB_LINK           = "link";
    private static final String TYPE_APPLICATION        = "app";




    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
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
                sendNotification(remoteMessage);
            }catch (Exception e){
                Crashlytics.logException(e);
                Log.d(TAG, e.getMessage());;
            }
        }

    }

    private void sendNotification(RemoteMessage remoteMessage){

        if(remoteMessage.getData().get(KEY_NOTIFICATION_TYPE) == null
                || remoteMessage.getData().get(KEY_TITLE) == null
                || remoteMessage.getData().get(KEY_MESSAGE) == null) return;

        String notificationType = remoteMessage.getData().get(KEY_NOTIFICATION_TYPE);

        if(notificationType.equals(TYPE_UPDATE_AVAILABLE) && remoteMessage.getData().get(KEY_VERSION_CODE) != null){
            int versionCode = Integer.valueOf(remoteMessage.getData().get(KEY_VERSION_CODE));
            if(versionCode > BuildConfig.VERSION_CODE){
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=in.co.gorest.grblcontroller"));
                final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
                this.showNotification(remoteMessage, pendingIntent);
                Log.d(TAG, "Sent");
            }
        }

        if(notificationType.equals(TYPE_WEB_LINK) && remoteMessage.getData().get(KEY_LINK) !=  null){
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(remoteMessage.getData().get(KEY_LINK)));
            final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
            this.showNotification(remoteMessage, pendingIntent);
        }

        if(notificationType.equals(TYPE_APPLICATION)){
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
            this.showNotification(remoteMessage, pendingIntent);
        }

    }


    private void showNotification(RemoteMessage remoteMessage, PendingIntent pendingIntent){

        String channelId = getString(R.string.default_notification_channel_id);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setContentTitle(remoteMessage.getData().get(KEY_TITLE))
                .setContentText(remoteMessage.getData().get(KEY_MESSAGE))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

}