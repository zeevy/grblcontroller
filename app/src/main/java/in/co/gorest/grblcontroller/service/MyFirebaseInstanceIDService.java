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

import android.os.Build;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import in.co.gorest.grblcontroller.BuildConfig;
import in.co.gorest.grblcontroller.GrblController;
import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.helpers.NotificationHelper;

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

        JSONObject fcmToken = new JSONObject();
        try {
            fcmToken.put("app_name", BuildConfig.APPLICATION_ID);
            fcmToken.put("app_version", String.valueOf(BuildConfig.VERSION_CODE));
            fcmToken.put("token", refreshedToken);
            fcmToken.put("device_name", Build.MODEL);
            fcmToken.put("os_version", Build.VERSION.RELEASE);
        } catch (JSONException e) {
            return;
        }

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.POST, "https://gorest.co.in/fcm-registration.html", fcmToken,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        boolean isSuccess = response.getBoolean("success");
                        if(isSuccess){
                            EnhancedSharedPreferences sharedPreferences = EnhancedSharedPreferences.getInstance(GrblController.getInstance(), GrblController.getInstance().getString(R.string.shared_preference_key));
                            sharedPreferences.edit().putBoolean(GrblController.getInstance().getString(R.string.firebase_cloud_messaging_token_sent), true).apply();
                        }
                    } catch (JSONException e) {
                        return;
                    }
                }
            },

            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
        });

        jsObjRequest.setShouldCache(false);

        GrblController.getInstance().addToRequestQueue(jsObjRequest);
    }

}
