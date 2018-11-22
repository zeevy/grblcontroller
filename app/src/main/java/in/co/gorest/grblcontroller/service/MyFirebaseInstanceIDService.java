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

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.JsonObject;

import java.util.Objects;

import in.co.gorest.grblcontroller.GrblController;
import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.helpers.NotificationHelper;
import in.co.gorest.grblcontroller.model.FcmToken;
import retrofit2.Call;
import retrofit2.Callback;

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private final static String TAG = MyFirebaseInstanceIDService.class.getSimpleName();

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        EnhancedSharedPreferences sharedPreferences = EnhancedSharedPreferences.getInstance(GrblController.getInstance(), getString(R.string.shared_preference_key));
        sharedPreferences.edit().putString(getString(R.string.firebase_cloud_messaging_token), refreshedToken).apply();

        FirebaseMessaging.getInstance().subscribeToTopic(NotificationHelper.CHANNEL_GENERAL_NAME);
        FirebaseMessaging.getInstance().subscribeToTopic(NotificationHelper.CHANNEL_BUG_TRACKER_NAME);
        FirebaseMessaging.getInstance().subscribeToTopic(NotificationHelper.CHANNEL_SERVICE_NAME);

        sendRegistrationToServer(refreshedToken);
    }

    public static void sendRegistrationToServer(final String refreshedToken){

        Log.d(TAG, refreshedToken);

        GrblController.getInstance().getRetrofit().postFcmToken(new FcmToken(refreshedToken)).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull retrofit2.Response<JsonObject> response) {
                if(response.isSuccessful()){
                    Boolean isSaved = Objects.requireNonNull(response.body()).get("success").getAsBoolean();
                    if(isSaved){
                        EnhancedSharedPreferences sharedPreferences = EnhancedSharedPreferences.getInstance(GrblController.getInstance(), GrblController.getInstance().getString(R.string.shared_preference_key));
                        sharedPreferences.edit().putBoolean(GrblController.getInstance().getString(R.string.firebase_cloud_messaging_token_sent), true).apply();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable throwable) {

            }
        });

    }

}
