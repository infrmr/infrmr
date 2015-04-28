package infrmr.newsapp.github.com.ifrmr;

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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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


public class MainActivity extends AppCompatActivity {

    /**
     * TODO:
     * - Preference summary
     */

    // For checking user network connection preference
    public static final String WIFI = "Wi-Fi";
    public static final String ANY = "Any";
    // Whether the display should be refreshed.
    public static boolean refreshDisplay = true;
    // The user's current network preference setting.
    public static String sPref = null;
    // Default RSS Feed before loading from preference
    private static String URL = "http://www.theverge.com/android/rss/index.xml";
    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected = false;
    // Whether there is a mobile connection.
    private static boolean mobileConnected = false;
    // Tag for debugging
    public String TAG = getClass().getSimpleName();
    List<TheVergeXmlParser.Entry> entries = null;
    // todo
    LoadToast lt;
    // The BroadcastReceiver that tracks network connectivity changes.
    private NetworkReceiver receiver = new NetworkReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        // Gets the user's network preference settings
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Retrieves a string value for the preferences. The second parameter
        // is the default value to use if a preference value is not found.
        sPref = sharedPrefs.getString("listPref", "Wi-Fi");

        // Retrieves the users preference for news topic
        URL = sharedPrefs.getString("topicPref", "http://www.theverge.com/android/rss/index.xml");

        // CHeck internet connection
        updateConnectedFlags();

        // Only loads the page if refreshDisplay is true. Otherwise, keeps previous
        // display. For example, if the user has set "Wi-Fi only" in prefs and the
        // device loses its Wi-Fi connection midway through the user using the app,
        // you don't want to refresh the display--this would force the display of
        // an error page instead of stackoverflow.com content.
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
    private void loadPage() {
        Log.i(TAG, "Refreshing Feed");
        // launchRingDialog();

        lt = new LoadToast(this);
        lt.setText("Loading News...");
        lt.show();


        if (((sPref.equals(ANY)) && (wifiConnected || mobileConnected))
                || ((sPref.equals(WIFI)) && (wifiConnected))) {
            new DownloadXmlTask().execute(URL);
        } else {
            showErrorPage();
            Log.d(TAG, "Error - Internet Connection");
        }
    }

    // Displays an error if the app is unable to load content.
    private void showErrorPage() {
        // Update textview with error message
        setContentView(R.layout.activity_main);
        TextView textView = (TextView) findViewById(R.id.textViewNews);
        textView.setText(getString(R.string.connection_error));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
        String summary = null;
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
     * TODO - Iterate through array of TextViews to reduce size of method
     */
    private void updateArticles(String result) {
        // Prevent refresh
        refreshDisplay = false;


        setContentView(R.layout.activity_main);

        Calendar rightNow = Calendar.getInstance();
        DateFormat formatter = new SimpleDateFormat("MMM dd h:mmaa");

        StringBuilder newsString = new StringBuilder();
        newsString.append(getResources().getString(R.string.page_title) + "\n\n");
        newsString.append(getResources().getString(R.string.updated) + " " + formatter.format(rightNow.getTime()));

        TextView textViewNews = (TextView) findViewById(R.id.textViewNews);
        textViewNews.setText(newsString);

        LinearLayout articleLayout = (LinearLayout) findViewById(R.id.linearLayoutNews);
        articleLayout.setVisibility(View.VISIBLE);

        TextView article1 = (TextView) findViewById(R.id.article1);
        article1.setText(entries.get(0).title);
        article1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), ArticleActivity.class);
                i.putExtra("title", entries.get(0).title);
                i.putExtra("content", entries.get(0).content);
                i.putExtra("link", entries.get(0).link);
                startActivity(i);
            }
        });

        TextView article2 = (TextView) findViewById(R.id.article2);
        article2.setText(entries.get(1).title);
        article2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), ArticleActivity.class);
                i.putExtra("title", entries.get(1).title);
                i.putExtra("content", entries.get(1).content);
                i.putExtra("link", entries.get(1).link);
                startActivity(i);
            }
        });

        TextView article3 = (TextView) findViewById(R.id.article3);
        article3.setText(entries.get(2).title);
        article3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), ArticleActivity.class);
                i.putExtra("title", entries.get(2).title);
                i.putExtra("content", entries.get(2).content);
                i.putExtra("link", entries.get(2).link);
                startActivity(i);
            }
        });

        TextView article4 = (TextView) findViewById(R.id.article4);
        article4.setText(entries.get(3).title);
        article4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), ArticleActivity.class);
                i.putExtra("title", entries.get(3).title);
                i.putExtra("content", entries.get(3).content);
                i.putExtra("link", entries.get(3).link);
                startActivity(i);
            }
        });

        TextView article5 = (TextView) findViewById(R.id.article5);
        article5.setText(entries.get(4).title);
        article5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), ArticleActivity.class);
                i.putExtra("title", entries.get(4).title);
                i.putExtra("content", entries.get(4).content);
                i.putExtra("link", entries.get(4).link);
                startActivity(i);
            }
        });

        TextView article6 = (TextView) findViewById(R.id.article6);
        article6.setText(entries.get(5).title);
        article6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), ArticleActivity.class);
                i.putExtra("title", entries.get(5).title);
                i.putExtra("content", entries.get(5).content);
                i.putExtra("link", entries.get(5).link);
                startActivity(i);
            }
        });

        TextView article7 = (TextView) findViewById(R.id.article7);
        article7.setText(entries.get(6).title);
        article7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), ArticleActivity.class);
                i.putExtra("title", entries.get(6).title);
                i.putExtra("content", entries.get(6).content);
                i.putExtra("link", entries.get(6).link);
                startActivity(i);
            }
        });

        TextView article8 = (TextView) findViewById(R.id.article8);
        article8.setText(entries.get(7).title);
        article8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), ArticleActivity.class);
                i.putExtra("title", entries.get(7).title);
                i.putExtra("content", entries.get(7).content);
                i.putExtra("link", entries.get(7).link);
                startActivity(i);
            }
        });

        TextView article9 = (TextView) findViewById(R.id.article9);
        article9.setText(entries.get(8).title);
        article9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), ArticleActivity.class);
                i.putExtra("title", entries.get(8).title);
                i.putExtra("content", entries.get(8).content);
                i.putExtra("link", entries.get(8).link);
                startActivity(i);
            }
        });

        TextView article10 = (TextView) findViewById(R.id.article10);
        article10.setText(entries.get(9).title);
        article10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), ArticleActivity.class);
                i.putExtra("title", entries.get(9).title);
                i.putExtra("content", entries.get(9).content);
                i.putExtra("link", entries.get(9).link);
                startActivity(i);
            }
        });

    }

    // Implementation of AsyncTask used to download XML feed from TheVerge.com.
    private class DownloadXmlTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setContentView(R.layout.activity_main);
            TextView textView = (TextView) findViewById(R.id.textViewNews);
            textView.setText("Loading Data...");
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
                updateArticles(result);
                // Call this if it was successful
                lt.success();
            } else {
                // Or this method if it failed
                lt.error();
                Log.i(TAG, "Post Execute - entries 0 / null");
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
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

            // Checks the user prefs and the network connection. Based on the result, decides
            // whether
            // to refresh the display or keep the current display.
            // If the userpref is Wi-Fi only, checks to see if the device has a Wi-Fi connection.
            if (WIFI.equals(sPref) && networkInfo != null
                    && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                // If device has its Wi-Fi connection, sets refreshDisplay
                // to true. This causes the display to be refreshed when the user
                // returns to the app.
                refreshDisplay = true;
                Toast.makeText(context, R.string.wifi_connected, Toast.LENGTH_SHORT).show();

                // If the setting is ANY network and there is a network connection
                // (which by process of elimination would be mobile), sets refreshDisplay to true.
            } else if (ANY.equals(sPref) && networkInfo != null) {
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
