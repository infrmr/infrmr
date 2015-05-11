package infrmr.newsapp.github.com.ifrmr;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import infrmr.newsapp.github.com.ifrmr.settings.SettingsActivity;


public class MainActivity extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks, ArticleListFragment.OnFragmentInteractionListener {

    /**
     * TODO
     * - Set drawable background for cardViews
     * - Only refresh when needed (not in every onResume)
     * - Check what Network Adapter actually does
     */

    // Whether the display should be refreshed.
    // Tag for debugging
    public String TAG = getClass().getSimpleName();
    // For setting title
    private CharSequence mTitle;

    // Whether the display should be refreshed.
    public static boolean refreshDisplay = true;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTitle = getTitle();

        // Init Navigation Drawer
        NavigationDrawerFragment mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.my_navigation_drawer);


        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.my_navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
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
            // Call ArticleListFragments loadPage() method
            ArticleListFragment fragment = (ArticleListFragment) getSupportFragmentManager().findFragmentById(R.id.container);
            fragment.loadPage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Method callback for Nav Drawer selection
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
    public void onFragmentInteraction(int position) {
        // The user selected the headline of an article from the HeadlinesFragment
        // Do something here to display that article
    }


}
