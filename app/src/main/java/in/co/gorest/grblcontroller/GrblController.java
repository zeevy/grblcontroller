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


import android.app.Application;
import android.content.Context;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;

import in.co.gorest.grblcontroller.util.GoRestBitmapCache;
import io.fabric.sdk.android.Fabric;

public class GrblController extends Application {

    private final String TAG = GrblController.class.getSimpleName();
    private static GrblController grblController;
    private RequestQueue requestQueue;
    private ImageLoader imageLoader;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics(), new Answers());
        grblController = this;
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    public static synchronized GrblController getInstance(){
        return grblController;
    }

    public RequestQueue getRequestQueue(){
        if(requestQueue == null) requestQueue = Volley.newRequestQueue(getApplicationContext());

        return requestQueue;
    }

    public ImageLoader getImageLoader(){
        getRequestQueue();
        if(imageLoader == null){
            imageLoader = new ImageLoader(this.requestQueue, new GoRestBitmapCache());
        }

        return this.imageLoader;
    }

    public <T> void addToRequestQueue(Request<T> req, String tag) {
        // set the default tag if tag is empty
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(req);
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    public void cancelPendingRequests(Object tag) {
        if (requestQueue != null) {
            requestQueue.cancelAll(tag);
        }
    }


}
