package infrmr.newsapp.github.com.ifrmr.article;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import infrmr.newsapp.github.com.ifrmr.ArticleListFragment;
import infrmr.newsapp.github.com.ifrmr.R;

/**
 * Activity which hosts the article Fragment
 */
public class ArticleActivity extends AppCompatActivity {

    // For storing users topic preference
    public static final String PREF_TOPIC = "topicPref";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Display the settings fragment
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, ArticleFragment.newInstance())
                .commit();

        // Set up parent to return to parent Activity
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get preference manager
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        // Retrieves the users preference for news topic
        String topicPref = sharedPrefs.getString(PREF_TOPIC, "");
        // Get Title of current topic from static fragment helper
        getSupportActionBar().setTitle(ArticleListFragment.getTopicFromPref(topicPref));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exit();
    }

    /**
     * This method guarantees the Back button and home button are equal
     */
    public void exit() {
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_article, menu);
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
            return true;
        } else if (id == R.id.home) {
            exit();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
