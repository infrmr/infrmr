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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.steamcrafted.loadtoast.LoadToast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import infrmr.newsapp.github.com.ifrmr.article.ArticleActivity;
import infrmr.newsapp.github.com.ifrmr.settings.SettingsActivity;


public class MainActivity extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * TODO
     * - If desired, auto refresh in onResume
     */

    public static final String PREF_CONNECTIVITY = "connectivityPref";
    public static final String PREF_TOPIC = "topicPref";
    // For checking user network connection preference
    public static final String PREF_CONNECTIVITY_WIFI = "Wi-Fi";
    public static final String PREF_CONNECTIVITY_ANY = "Any";
    public static final String DEFAULT_PREF_CONNECTIVITY = PREF_CONNECTIVITY_WIFI;
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
    private List<TheVergeXmlParser.Entry> entries;
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

    /**
     * Method which gets the users current network status, compares to users network preference,
     * then calls the loadPage() method if desired.
     */
    public void checkConnectionThenLoadPage() {

        // Gets the user's network preference settings
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Retrieves a string value for the preferences. The second parameter
        // is the default value to use if a preference value is not found.
        connectivityPref = sharedPrefs.getString(PREF_CONNECTIVITY, DEFAULT_PREF_CONNECTIVITY);

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

        // Gets the user's network preference settings
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Retrieves a string value for the preferences. The second parameter
        // is the default value to use if a preference value is not found.
        connectivityPref = sharedPrefs.getString(PREF_CONNECTIVITY, "Wi-Fi");

        // CHeck internet connection
        updateConnectedFlags();

        if (((connectivityPref.equals(PREF_CONNECTIVITY_ANY)) && (wifiConnected || mobileConnected))
                || ((connectivityPref.equals(PREF_CONNECTIVITY_WIFI)) && (wifiConnected))) {
            new DownloadXmlTask().execute(topicPref);
            loadToast = new LoadToast(this);
            loadToast.setText(getString(R.string.loading_news));
            loadToast.show();
        } else {
            //TODO avoid toasts! Have a look at Croutons
            Toast.makeText(getApplicationContext(), "Unable to load content. Check your network " +
                    "connection and try again.", Toast.LENGTH_SHORT).show();
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

        // Show hidden layout
        LinearLayout articleLayout = (LinearLayout) findViewById(R.id.linearLayoutNews);
        articleLayout.setVisibility(View.VISIBLE);

        setTitleString();
        setArticleData();
    }

    /**
     * Iterate through TextView's, setting title and onClickListener for each.
     */
    private void setArticleData() {
        // Create array of TextView references
        //TODO this is not extensible!
        int[] textViewIDs = new int[]{R.id.article1, R.id.article2, R.id.article3, R.id.article4,
                R.id.article5, R.id.article6, R.id.article7, R.id.article8, R.id.article9, R.id.article10};

        // Iterate through array
        for (int i = 0; i < textViewIDs.length; i++) {
            //TODO what is the use for making it final?
            final int finalI = i;

            TextView tv = (TextView) findViewById(textViewIDs[i]);
            tv.setText(entries.get(i).title);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getApplicationContext(), ArticleActivity.class);
                    intent.putExtra("title", entries.get(finalI).title);
                    intent.putExtra("content", entries.get(finalI).content);
                    intent.putExtra("link", entries.get(finalI).link);
                    startActivity(intent);
                }
            });
        }
    }

    /**
     * Helper method which returns title string & last updated text
     */
    private void setTitleString() {
        // Get title String
        StringBuilder newsString = new StringBuilder();
        // Use these to get time
        Calendar rightNow = Calendar.getInstance();
        DateFormat formatter = new SimpleDateFormat("MMM dd h:mmaa", Locale.ENGLISH);
        // Build title string
        newsString.append(getResources().getString(R.string.page_title)).append("\n\n");
        newsString.append(getResources().getString(R.string.updated)).append(" ").append(formatter.format(rightNow.getTime()));
        // Get reference to title TextView, then set text
        TextView textViewNews = (TextView) findViewById(R.id.textViewNews);
        textViewNews.setText(newsString);
    }

    /**
     * NavigationDrawer methods below
     */

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, SectionFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(mTitle);
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class SectionFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public SectionFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static SectionFragment newInstance(int sectionNumber) {
            SectionFragment fragment = new SectionFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_main, container, false);
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    /**
     * Implementation of AsyncTask used to download XML feed from TheVerge.com.
     */
    private class DownloadXmlTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //setContentView(R.layout.activity_main);
            TextView textView = (TextView) findViewById(R.id.textViewNews);
            textView.setText(getString(R.string.loading));
            LinearLayout articleLayout = (LinearLayout) findViewById(R.id.linearLayoutNews);
            articleLayout.setVisibility(View.INVISIBLE);
        }

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
             * If successful, update UI with article information, else show error message.
             */
            if (entries != null && entries.size() > 0) {
                Log.i(TAG, "Post Execute - entries: " + entries.size());
                updateArticles();
                // Call this if it was successful
                loadToast.success();
            } else {
                // Or this method if it failed
                loadToast.error();
                Log.i(TAG, "Post Execute - entries 0 / null");
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Fragment

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

            // Checks the user prefs and the network connection. Based on the result, decides
            // whether
            // to refresh the display or keep the current display.
            // If the userpref is Wi-Fi only, checks to see if the device has a Wi-Fi connection.
            if (PREF_CONNECTIVITY_WIFI.equals(connectivityPref) && networkInfo != null
                    && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                // If device has its Wi-Fi connection, sets refreshDisplay
                // to true. This causes the display to be refreshed when the user
                // returns to the app.
                refreshDisplay = true;
                Toast.makeText(context, R.string.wifi_connected, Toast.LENGTH_SHORT).show();

                // If the setting is ANY network and there is a network connection
                // (which by process of elimination would be mobile), sets refreshDisplay to true.
            } else if (PREF_CONNECTIVITY_ANY.equals(connectivityPref) && networkInfo != null) {
                refreshDisplay = true;

                // Otherwise, the app can't download content--either because there is no network
                // connection (mobile or Wi-Fi), or because the pref setting is WIFI, and there
                // is no Wi-Fi connection.
                // Sets refreshDisplay to false.
            } else {
                refreshDisplay = false;
                Toast.makeText(context, R.string.lost_connection, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
