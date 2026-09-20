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
import android.content.Context;
import android.content.ContextWrapper;

import in.co.gorest.grblcontroller.R;

public class NotificationHelper extends ContextWrapper {

    private NotificationManager notificationManager;

    public static final String CHANNEL_SERVICE_ID               = "in.co.gorest.grblcontroller.APPLICATION_SERVICE";
    public static final String CHANNEL_SERVICE_NAME             = "Service";
    public static final String CHANNEL_SERVICE_ABOUT            = "Service notification when app is working in foreground.";

    public NotificationHelper(Context base) {
        super(base);
    }

    public void createChannels(){

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
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

    private NotificationManager getNotificationManager() {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }
}
