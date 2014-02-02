/*
 * Copyright 2014 Daniel Pedraza-Arcega
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.twitt4droid.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.twitt4droid.R;
import com.twitt4droid.Twitt4droid;

import twitter4j.AsyncTwitter;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
import twitter4j.TwitterMethod;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 * This Activity provides the web based Twitter login process. Is not meant to 
 * be used directly (DO NOT START IT DIRECTLY). Add this activity to your 
 * AndroidManifest.xml like this:
 * <pre>
 * {@code 
 * <activity android:name="com.twitt4droid.activity.WebLoginActivity"
 *           android:theme="@android:style/Theme.Black.NoTitleBar" />
 * }
 * </pre>
 * 
 * @author Daniel Pedraza-Arcega
 * @since version 1.0
 */
public class WebLoginActivity extends Activity {

    /**
     * The request code for this activity.
     */
    public static final int REQUEST_CODE = 340;

    /**
     * The name of the Intent-extra used to indicate the twitter user returned.
     */
    public static final String EXTRA_USER = "com.twitt4droid.extra.user";
     
    private static final String TAG = WebLoginActivity.class.getSimpleName();
    private static final String OAUTH_VERIFIER_CALLBACK_PARAMETER = "oauth_verifier";
    private static final String DENIED_CALLBACK_PARAMETER = "denied";
    private static final String CALLBACK_URL = "oauth://twitt4droid";

    private AsyncTwitter twitter;
    private ProgressBar loadingBar;
    private MenuItem reloadCancelItem;
    private WebView webView;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Twitt4droid.areConsumerTokensAvailable(getApplicationContext())) {
            if (isConnected()) {
                setUpTwitter();
                if (Twitt4droid.isUserLoggedIn(getApplicationContext())) {
                    twitter.verifyCredentials();
                } else {
                    setContentView(R.layout.twitt4droid_web_browser);
                    setUpView();
                    twitter.getOAuthRequestTokenAsync(CALLBACK_URL);
                }
            } else {
                Log.w(TAG, "No Internet connection detected");
                showNetworkAlertDialog();
            }
        } else {
            Log.e(TAG, "Twitter consumer key and/or consumer secret are not defined correctly");
            setResult(RESULT_CANCELED, getIntent());
            finish();
        }
    }

    /**
     * Determines if this activity has Internet connectivity.
     * @return {@code true} if connectivity; otherwise {@code false}.
     */
    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Shows a network error alert dialog.
     */
    private void showNetworkAlertDialog() {
        new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.twitt4droid_nonetwork_title)
            .setMessage(R.string.twitt4droid_nonetwork_messege)
            .setNegativeButton(android.R.string.cancel, 
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i(TAG, "User canceled authentication process due to network failure");
                        setResult(RESULT_CANCELED, getIntent());
                        finish();
                    }
            })
            .setPositiveButton(R.string.twitt4droid_nonetwork_goto_settings, 
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                        finish();
                    }
                })
            .setCancelable(false)
            .show();
    }

    /**
     * Sets up views.
     */
    @SuppressWarnings("deprecation")
    private void setUpView() {
        loadingBar = (ProgressBar) findViewById(R.id.loading_bar);
        webView = (WebView) findViewById(R.id.web_view);
        CookieSyncManager.createInstance(this);
        CookieManager.getInstance().removeAllCookie();
        webView.getSettings().setSaveFormData(false);
        webView.getSettings().setSavePassword(false);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progress == 100) {
                    loadingBar.setVisibility(View.INVISIBLE);
                    loadingBar.setProgress(0);
                    if (reloadCancelItem != null) {
                        reloadCancelItem.setTitle(R.string.reload_menu_title);
                        reloadCancelItem.setIcon(R.drawable.reload_white_icon);
                    }
                } else {
                    loadingBar.setVisibility(View.VISIBLE);
                }
                loadingBar.setProgress(progress);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(CALLBACK_URL)) {
                    Uri uri = Uri.parse(url);
                    if (uri.getQueryParameter(DENIED_CALLBACK_PARAMETER) != null) {
                        Log.i(TAG, "Authentication process was denied");
                        twitter.addListener(null);
                        setResult(RESULT_CANCELED, getIntent());
                        finish();
                        return true;
                    }
                    if (uri.getQueryParameter(OAUTH_VERIFIER_CALLBACK_PARAMETER) != null) {
                        String oauthVerifier = uri.getQueryParameter(OAUTH_VERIFIER_CALLBACK_PARAMETER);
                        twitter.getOAuthAccessTokenAsync(oauthVerifier);
                        twitter.verifyCredentials();
                        return true;
                    }
                }

                return super.shouldOverrideUrlLoading(view, url);
            }
        });
    }

    /**
     * Sets up Twitter async listeners.
     */
    private void setUpTwitter() { 
        twitter = Twitt4droid.getAsyncTwitter(getApplicationContext());
        twitter.addListener(new TwitterAdapter() {
            @Override
            public void verifiedCredentials(User user) {
                Log.i(TAG, "@" + user.getScreenName() + " was successfully authenticated");
                twitter.addListener(null);
                Intent data = getIntent();
                data.putExtra(EXTRA_USER, user);
                setResult(RESULT_OK, data);
                finish();
            }

            @Override
            public void gotOAuthRequestToken(final RequestToken token) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d(TAG, "Loading authentication page...");
                        webView.loadUrl(token.getAuthenticationURL());
                    }
                });
            }

            @Override
            public void gotOAuthAccessToken(AccessToken token) {
                Log.d(TAG, "Saving access tokens...");
                Twitt4droid.saveAuthenticationInfo(getApplicationContext(), token);
            }

            @Override
            public void onException(TwitterException te, TwitterMethod method) {
                Log.e(TAG, "Twitter error", te);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showErrorAlertDialog();
                    }
                });
            }
        });
    }

    /**
     * Shows a generic alert dialog.
     */
    private void showErrorAlertDialog() {
        new AlertDialog.Builder(WebLoginActivity.this)
            .setTitle(R.string.twitt4droid_onerror_title)
            .setMessage(R.string.twitt4droid_onerror_message)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setNegativeButton(R.string.twitt4droid_onerror_return,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i(TAG, "User canceled authentication process due to an error");
                        twitter.addListener(null);
                        setResult(RESULT_CANCELED, getIntent());
                        finish();
                    }
                })
            .setPositiveButton(R.string.twitt4droid_onerror_continue, null)
            .setCancelable(false)
            .show();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.twitt4droid_web_browser, menu);
        reloadCancelItem = menu.findItem(R.id.reload_cancel_item);
        if (loadingBar.getProgress() == loadingBar.getMax() || loadingBar.getProgress() == 0) {
            reloadCancelItem.setTitle(R.string.reload_menu_title);
            reloadCancelItem.setIcon(R.drawable.reload_white_icon);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.reload_cancel_item) {
            if (item.getTitle().toString().equals(getString(R.string.cancel_menu_title))) {
                webView.stopLoading();
                item.setTitle(R.string.reload_menu_title);
                item.setIcon(R.drawable.reload_white_icon);
            } else {
                webView.reload();
                item.setTitle(R.string.cancel_menu_title);
                item.setIcon(R.drawable.cancel_white_icon);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}