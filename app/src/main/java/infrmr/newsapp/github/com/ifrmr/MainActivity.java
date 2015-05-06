package infrmr.newsapp.github.com.ifrmr;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.steamcrafted.loadtoast.LoadToast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import infrmr.newsapp.github.com.ifrmr.settings.SettingsActivity;


public class MainActivity extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks, ArticleListFragment.OnFragmentInteractionListener {

    /**
     * TODO
     * - Set drawable background for cardViews like in Lead-Feed
     * - If desired, auto refresh in onResume (Or Nav Fragments onItemSelected method)
     * - Only refresh when needed (not in every onResume)
     * - Search for <a> + <i> tags in content & remove
     */

    public static final String PREF_TOPIC = "topicPref";
    // For checking user network connection preference
    public static final String DEFAULT_PREF_TOPIC = "http://www.theverge.com/android/rss/index.xml";
    // Whether the display should be refreshed.
    public static boolean refreshDisplay = true;
    // The user's current network preference setting.
    public static String connectivityPref = null;
    // Default RSS Feed before loading from preference
    private static String topicPref = null;
    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected = false;
    // Whether there is a mobile connection.
    private static boolean mobileConnected = false;
    // Tag for debugging
    public String TAG = getClass().getSimpleName();
    public static List<TheVergeXmlParser.Entry> entries;
    // Reference for loading toast
    LoadToast loadToast;
    // The BroadcastReceiver that tracks network connectivity changes.
    private NetworkReceiver receiver = new NetworkReceiver();

    // Fragment managing the behaviors, interactions and presentation of the navigation drawer.
    private NavigationDrawerFragment mNavigationDrawerFragment;
    // Used to store the last screen title. For use in ActionBar.
    private CharSequence mTitle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.my_navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.my_navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // Register BroadcastReceiver to track connection changes.
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, filter);
    }

    // Refreshes the display if the network connection and the
    // pref settings allow it.
    @Override
    public void onResume() {
        super.onResume();

        checkConnectionThenLoadPage();
    }

    @Override
    protected void onPause() {
        super.onPause();
        loadToast.error();
    }

    /**
     * Method which gets the users current network status, compares to users network preference,
     * then calls the loadPage() method if desired.
     */
    public void checkConnectionThenLoadPage() {

        // Gets the user's network preference settings
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Retrieves the users preference for news topic
        topicPref = sharedPrefs.getString(PREF_TOPIC, DEFAULT_PREF_TOPIC);

        // Check internet connection
        updateConnectedFlags();

        // Only loads the page if refreshDisplay is true. Otherwise, keeps previous
        // display. For example, if the user has set "Wi-Fi only" in prefs and the
        // device loses its Wi-Fi connection midway through the user using the app,
        // you don't want to refresh the display--this would force the display of
        // an error page instead of TheVerge.com content.
        if (refreshDisplay) {
            loadPage();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Crouton.cancelAllCroutons();
        if (receiver != null) {
            this.unregisterReceiver(receiver);
        }
    }

    // Checks the network connection and sets the wifiConnected and mobileConnected
    // variables accordingly.
    private void updateConnectedFlags() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected()) {
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
            mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
        } else {
            wifiConnected = false;
            mobileConnected = false;
        }
    }

    /**
     * Uses AsyncTask subclass to download XML feed from TheVerge.com concurrently.
     * Checks to see if the users network preference matches available connections.
     */
    public void loadPage() {

        // Check internet connection
        updateConnectedFlags();

        if (wifiConnected || mobileConnected) {
            new DownloadXmlTask().execute(topicPref);
            // Show loading toast & clear adapter
            loadToast = new LoadToast(this);
            loadToast.setText(getString(R.string.loading_news));
            loadToast.show();
        } else {
            Crouton.makeText(MainActivity.this, R.string.no_connection, Style.INFO).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsActivity = new Intent(getBaseContext(), SettingsActivity.class);
            startActivity(settingsActivity);
            return true;
        } else if (id == R.id.action_refresh) {
            loadPage();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Uploads XML from TheVerge.com, parses it, and combines it with
    // HTML markup. Returns HTML string.
    private void loadXmlFromNetwork(String urlString) throws XmlPullParserException, IOException {
        InputStream stream = null;
        TheVergeXmlParser vergeXmlParser = new TheVergeXmlParser();

        String title = null;
        String url = null;
        String content = null;

        try {
            stream = downloadUrl(urlString);
            entries = vergeXmlParser.parse(stream);
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

    }

    // Given a string representation of a URL, sets up a connection and gets
    // an input stream.
    private InputStream downloadUrl(String urlString) throws IOException {
        java.net.URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        return conn.getInputStream();
    }

    /**
     * Method for updating the TextViews with article information.
     */
    private void updateArticles() {
        ArticleArrayAdapter.articles.clear();
        for (int i = 0; i < entries.size(); i++) {
            ArticleArrayAdapter.articles.add(entries.get(i));
        }
    }


    /**
     * NavigationDrawer methods below
     */

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, ArticleListFragment.newInstance(position + 1)).commit();

    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(mTitle);
        }
    }

    @Override
    public void onFragmentInteraction(String id) {
        //  nothing
    }


    /**
     * Implementation of AsyncTask used to download XML feed from TheVerge.com.
     */
    private class DownloadXmlTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                loadXmlFromNetwork(urls[0]);
                return getResources().getString(R.string.connection_timeout);
            } catch (IOException e) {
                return getResources().getString(R.string.connection_error);
            } catch (XmlPullParserException e) {
                return getResources().getString(R.string.xml_error);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            /**
             * If successful and still in view, update UI with article information, else show error message.
             */

            if (ArticleListFragment.isVisable) {
                if (entries != null && entries.size() > 0) {
                    updateArticles();
                    // Show new Fragment
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction()
                            .replace(R.id.container, ArticleListFragment.newInstance()).commit();

                    // Call this if it was successful
                    loadToast.success();
                } else {
                    // Or this method if it failed
                    loadToast.error();
                    Log.i(TAG, "Post Execute - entries 0 / null");
                    Crouton.makeText(MainActivity.this, result, Style.INFO).show();
                }
            } else {
                Log.i(TAG, "VIEW NOT VISABLE");
            }
        }
    }

    /**
     * This BroadcastReceiver intercepts the android.net.ConnectivityManager.CONNECTIVITY_ACTION,
     * which indicates a connection change. It checks whether the type is TYPE_WIFI.
     * If it is, it checks whether Wi-Fi is connected and sets the wifiConnected flag in the
     * main activity accordingly.
     */
    public class NetworkReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connMgr =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

            // Checks the network connection. Based on the result, decides
            // whether to refresh the display or keep the current display.
            if (networkInfo != null) {
                // If device has a network connection, sets refreshDisplay
                // to true. This allows the display to be refreshed upon next attempt.
                refreshDisplay = true;

                // Otherwise, the app can't download content due to no network
                // connection (mobile or Wi-Fi). Sets refreshDisplay to false.
            } else {
                refreshDisplay = false;
                Crouton.makeText(MainActivity.this, R.string.no_connection, Style.INFO).show();
            }
        }
    }
}
