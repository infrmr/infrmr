package infrmr.newsapp.github.com.ifrmr.settings;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;

import infrmr.newsapp.github.com.ifrmr.MainActivity;
import infrmr.newsapp.github.com.ifrmr.R;


public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Loads the XML preferences file.
        addPreferencesFromResource(R.xml.preferences);

        // Init preference summaries
        updatePrefSummary();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Registers a callback to be invoked whenever a user changes a preference.
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

        // Sets refreshDisplay to true so that when the user returns to the main
        // activity, the display refreshes to reflect the new settings.
        MainActivity.refreshDisplay = true;
        updatePrefSummary();
    }

    /**
     * Helper method for updating preference summary
     */
    public void updatePrefSummary() {
        // The preference for news feed topic
        ListPreference feedPref = (ListPreference) getPreferenceManager().findPreference("topicPref");
        feedPref.setSummary(feedPref.getEntry());

        // The preference for network download
        ListPreference listPref = (ListPreference) getPreferenceManager().findPreference("listPref");
        listPref.setSummary(listPref.getEntry());
    }

}
