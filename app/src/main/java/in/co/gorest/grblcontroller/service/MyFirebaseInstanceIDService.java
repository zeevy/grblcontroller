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

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

import in.co.gorest.grblcontroller.BuildConfig;
import in.co.gorest.grblcontroller.GrblConttroller;
import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.model.Constants;

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private final static String TAG = MyFirebaseInstanceIDService.class.getSimpleName();

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        //Log.d(TAG, "Refreshed token: " + refreshedToken);

        EnhancedSharedPreferences sharedPref = EnhancedSharedPreferences.getInstance(GrblConttroller.getContext(), getString(R.string.shared_preference_key));
        sharedPref.edit().putString(getString(R.string.firebase_cloud_messaging_token), refreshedToken).apply();


        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(refreshedToken);
    }

    public static void sendRegistrationToServer(String token) {
        EnhancedSharedPreferences sharedPref = EnhancedSharedPreferences.getInstance(GrblConttroller.getContext(), GrblConttroller.getContext().getString(R.string.shared_preference_key));

        try{

            sharedPref.edit().putBoolean(GrblConttroller.getContext().getString(R.string.firebase_cloud_messaging_token_sent), true).apply();
            FirebaseMessaging.getInstance().subscribeToTopic(Constants.DEFAULT_NOTIFICATION_CHANNEL);
            sharedPref.edit().putBoolean(GrblConttroller.getContext().getString(R.string.subscribe_to_notifications), true).apply();

            URL url = new URL("https://gorest.co.in/fcm-registration.html");
            JSONObject postDataParams = new JSONObject();
            postDataParams.put("FcmToken[app_name]", "Grbl Controller");
            postDataParams.put("FcmToken[token]", token);
            postDataParams.put("FcmToken[device_name]", Build.MODEL);
            postDataParams.put("FcmToken[os_version]", Build.VERSION.RELEASE);
            postDataParams.put("FcmToken[app_version]", BuildConfig.VERSION_NAME);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));
            writer.flush();
            writer.close();
            os.close();

            int responseCode = conn.getResponseCode();

            if(responseCode == HttpsURLConnection.HTTP_OK){
                BufferedReader in=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuffer sb = new StringBuffer("");
                String line="";

                while((line = in.readLine()) != null) {
                    sb.append(line);
                    break;
                }

                in.close();
            }else{
                Log.d(TAG, String.valueOf(responseCode));
            }

        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
    }

    private static String getPostDataString(JSONObject params) throws Exception {

        StringBuilder result = new StringBuilder();
        boolean first = true;

        Iterator<String> itr = params.keys();

        while(itr.hasNext()){

            String key= itr.next();
            Object value = params.get(key);

            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(value.toString(), "UTF-8"));

        }
        return result.toString();
    }
}
