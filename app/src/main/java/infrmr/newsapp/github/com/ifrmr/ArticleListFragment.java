package infrmr.newsapp.github.com.ifrmr;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import net.steamcrafted.loadtoast.LoadToast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * A fragment representing a list of Items. Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView. Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class ArticleListFragment extends Fragment {

    // For storing users topic preference
    public static final String PREF_TOPIC = "topicPref";
    // For checking user network connection preference
    public static final String DEFAULT_PREF_TOPIC = "http://www.theverge.com/android/rss/index.xml";
    // The fragment argument representing the section number for this fragment.
    private static final String ARG_SECTION_NUMBER = "section_number";
    // Default RSS Feed before loading from preference
    private static String topicPref = null;
    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected = false;
    // Whether there is a mobile connection.
    private static boolean mobileConnected = false;
    // String class name for debugging
    public String TAG = getClass().getSimpleName();
    // Download Async Task
    DownloadXmlTask dlt;
    // Activity callback listener
    private OnFragmentInteractionListener mListener;
    // The BroadcastReceiver that tracks network connectivity changes.
    private NetworkReceiver receiver = new NetworkReceiver();
    // Instance of LoadToast library
    private LoadToast loadToast;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleListFragment() {
    }

    /**
     * Return new Fragment
     */
    public static ArticleListFragment newInstance() {
        return new ArticleListFragment();
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static ArticleListFragment newInstance(int sectionNumber) {
        ArticleListFragment fragment = new ArticleListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FrameLayout frameLayout = (FrameLayout) inflater.inflate(R.layout.recycler_view_layout, container, false);
        // Find and init RecyclerView
        RecyclerView mRecyclerView = (RecyclerView) frameLayout.findViewById(R.id.recycleView);
        //mRecyclerView.setHasFixedSize(true);

        // If big screen, use grid layout, else use small
        if ((getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK) ==
                Configuration.SCREENLAYOUT_SIZE_LARGE) {
            mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
        } else {
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            mRecyclerView.setLayoutManager(linearLayoutManager);
        }

        // specify our custom adapter
        RecyclerView.Adapter mAdapter = new RecyclerAdapter(getActivity());
        mRecyclerView.setAdapter(mAdapter);

        return frameLayout;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Register BroadcastReceiver to track connection changes.
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        getActivity().registerReceiver(receiver, filter);

        dlt = new DownloadXmlTask();
    }

    // Refreshes the display if the network connection and the pref settings allow it.
    @Override
    public void onResume() {
        super.onResume();
        Log.i("REFRESH", "MainActivity.isUpToDate: " + MainActivity.isUpToDate);
        if (!MainActivity.isUpToDate) {
            checkConnectionThenLoadPage();
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        if (dlt != null) {
            dlt.cancel(true);
        }

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    // remove Croutons and nullify listener
    @Override
    public void onDetach() {
        super.onDetach();
        Crouton.cancelAllCroutons();
        mListener = null;
    }

    // Uploads XML from TheVerge.com, parses it, and combines it with
    // HTML markup. Returns HTML string.
    private ArrayList<TheVergeXmlParser.Entry> loadXmlFromNetwork(String urlString) throws XmlPullParserException, IOException {
        InputStream stream = null;
        TheVergeXmlParser vergeXmlParser = new TheVergeXmlParser();

        try {
            stream = downloadUrl(urlString);
            return (ArrayList<TheVergeXmlParser.Entry>) vergeXmlParser.parse(stream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Crouton.cancelAllCroutons();
        if (receiver != null) {
            getActivity().unregisterReceiver(receiver);
        }
    }

    /**
     * Method which gets the users current network status, compares to users network preference,
     * then calls the loadPage() method if desired.
     */
    public void checkConnectionThenLoadPage() {

        // Gets the user's network preference settings
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // Retrieves the users preference for news topic
        topicPref = sharedPrefs.getString(PREF_TOPIC, DEFAULT_PREF_TOPIC);

        // Check internet connection
        updateConnectedFlags();

        loadPage();
        }

    /**
     * Uses AsyncTask subclass to download XML feed from TheVerge.com concurrently.
     * Checks to see if the users network preference matches available connections.
     */
    public void loadPage() {

        Log.i(TAG, "loadPage()");

        // Check internet connection
        updateConnectedFlags();

        if (wifiConnected || mobileConnected) {
            dlt = new DownloadXmlTask();
            dlt.execute(topicPref);
            // Show loading toast
            loadToast = new LoadToast(getActivity());
            loadToast.setText(getString(R.string.loading_news));
            loadToast.show();
        } else {
            // Show no connection message
            Crouton.makeText(getActivity(), R.string.no_connection, Style.INFO).show();
        }
    }

    // Checks the network connection and sets the wifiConnected and mobileConnected
    // variables accordingly.
    private void updateConnectedFlags() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected()) {
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
            mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
        } else {
            wifiConnected = false;
            mobileConnected = false;
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
     * Method for updating article data after successful download
     */
    void onItemsLoadComplete(ArrayList<TheVergeXmlParser.Entry> downloadedArticles) {
        MainActivity.isUpToDate = true;
        // Update the adapter and notify data set changed
        RecyclerAdapter.articles.clear();
        for (int i = 0; i < downloadedArticles.size(); i++) {
            RecyclerAdapter.articles.add(downloadedArticles.get(i));
        }
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity. See: "http://developer.android.com/training/basics/fragments/communicating.html"
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(int position);
    }

    /**
     * Implementation of AsyncTask used to download XML feed from TheVerge.com.
     */
    private class DownloadXmlTask extends AsyncTask<String, Void, ArrayList<TheVergeXmlParser.Entry>> {

        @Override
        protected ArrayList<TheVergeXmlParser.Entry> doInBackground(String... urls) {
            ArrayList<TheVergeXmlParser.Entry> articles = new ArrayList<>();
            try {
                articles = loadXmlFromNetwork(urls[0]);
            } catch (IOException e) {
                Crouton.makeText(getActivity(), R.string.connection_error, Style.INFO).show();
            } catch (XmlPullParserException e) {
                Crouton.makeText(getActivity(), R.string.xml_error, Style.INFO).show();
            }
            return articles;
        }

        /**
         * If successful and still in view, update UI with article information, else show error message.
         */
        @Override
        protected void onPostExecute(ArrayList<TheVergeXmlParser.Entry> articles) {

            if (articles != null && articles.size() > 0) {
                loadToast.success();
                onItemsLoadComplete(articles);
                // Show new Fragment
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, ArticleListFragment.newInstance(), "article_fragment").commit();
            } else { //
                loadToast.error();
                Log.i(TAG, "onPostExecute - Articles null or empty (Most likely connection timeout)");

            }
        }
    }

    /**
     * This BroadcastReceiver intercepts the android.net.ConnectivityManager.CONNECTIVITY_ACTION,
     * which indicates a connection change. It checks whether the type is TYPE_WIFI.
     * If it is, it checks whether Wi-Fi is connected and sets the wifiConnected flag in the
     * main activity accordingly.
     * */

    public class NetworkReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connMgr =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

            Crouton.makeText(getActivity(), "NETWORK RECIEVER: " + networkInfo, Style.ALERT).show();

            // Checks the network connection. Based on the result, decides
            // whether to refresh the display or keep the current display.
            //if (networkInfo != null) {
                // If device has a network connection, sets refreshDisplay
                // to true. This allows the display to be refreshed upon next attempt.


                // Otherwise, the app can't download content due to no network
                // connection (mobile or Wi-Fi). Sets refreshDisplay to false.
            //} else {
            //    MainActivity.refreshDisplay = false;
            //    Crouton.makeText(getActivity(), R.string.no_connection, Style.INFO).show();
            //}
        }
    }

}
