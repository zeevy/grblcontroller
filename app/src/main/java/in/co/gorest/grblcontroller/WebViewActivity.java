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

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import org.greenrobot.eventbus.EventBus;

import java.util.Collection;

import im.delight.android.webview.AdvancedWebView;
import in.co.gorest.grblcontroller.events.UiToastEvent;

public class WebViewActivity extends AppCompatActivity implements AdvancedWebView.Listener {

    private AdvancedWebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        getSupportActionBar().setSubtitle(getIntent().getStringExtra("sub_title"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mWebView = (AdvancedWebView) findViewById(R.id.webview);
        mWebView.addPermittedHostname("linuxcnc.org");
        mWebView.addPermittedHostname("github.com");
        mWebView.addPermittedHostname("zeevy.github.io");
        mWebView.addPermittedHostname("gorest.co.in");
        mWebView.setListener(this, this);
        mWebView.loadUrl(getIntent().getStringExtra("target_url"));

    }

    @Override
    protected void onResume() {
        super.onResume();
        mWebView.onResume();
    }

    @Override
    protected void onPause() {
        mWebView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mWebView.onDestroy();
        super.onDestroy();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        mWebView.onActivityResult(requestCode, resultCode, intent);
    }

    public void onPageStarted(String url, Bitmap favicon) {
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    }


    public void onPageFinished(String url) {
        findViewById(R.id.progressBar).setVisibility(View.GONE);
        findViewById(R.id.webview).setVisibility(View.VISIBLE);
    }

    public void onExternalPageRequest(String url) {
        AdvancedWebView.Browsers.openUrl(this, url);
    }

    public void onReceivedError(AdvancedWebView mWebView, int errorCode, String description, String failingUrl) {
        EventBus.getDefault().post(new UiToastEvent("Unable to load the page. Please try again!"));
        mWebView.loadUrl("about:blank");
    }

    public void onPageError(int errorCode, String description, String failingUrl) {
        EventBus.getDefault().post(new UiToastEvent("Unable to load the page. Please try again!"));
    }

    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) {
        if (AdvancedWebView.handleDownload(this, url, suggestedFilename)) {
            // download successfully handled
        }
        else {
            EventBus.getDefault().post(new UiToastEvent("Download failed. Please try again!"));
        }
    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(!mWebView.onBackPressed()) return;
        super.onBackPressed();
    }
}
