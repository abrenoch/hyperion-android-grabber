package com.abrenoch.hyperiongrabber.mobile;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import androidx.appcompat.app.ActionBar;
import android.view.MenuItem;
import android.widget.Toast;
import java.lang.reflect.Field;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    public static final String EXTRA_SHOW_TOAST_KEY = "extra_show_toast_key";
    public static final int EXTRA_SHOW_TOAST_SETUP_REQUIRED_FOR_QUICK_TILE = 1;


    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (preference, value) -> {
        final int prefResourceID = getResourceId(preference.getKey(), R.string.class);

        // verify we have a valid int value for the following preference keys
        switch (prefResourceID) {
           case R.string.pref_key_port:
           case R.string.pref_key_reconnect_delay:
           case R.string.pref_key_priority:
           case R.string.pref_key_x_led:
           case R.string.pref_key_y_led:
           case R.string.pref_key_framerate:
               try {
                   Integer.parseInt(value.toString());
               } catch (NumberFormatException e) {
                   e.printStackTrace();
                   return false;
               }
               break;
        }

        String stringValue = value.toString();
            preference.setSummary(stringValue);

        return true;
    };

    /**
     * Returns the resource ID of the provided string
     */
    public static int getResourceId(String resourceName, Class<?> c) {
        try {
            Field idField = c.getDeclaredField(resourceName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new GeneralPreferenceFragment()).commit();

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(EXTRA_SHOW_TOAST_KEY)){
            if (extras.getInt(EXTRA_SHOW_TOAST_KEY) == EXTRA_SHOW_TOAST_SETUP_REQUIRED_FOR_QUICK_TILE){
                Toast.makeText(getApplicationContext(), R.string.quick_tile_toast_setup_required, Toast.LENGTH_SHORT).show();
            }
        }

        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_host)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_port)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_priority)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_framerate)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_reconnect_delay)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_x_led)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_y_led)));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
