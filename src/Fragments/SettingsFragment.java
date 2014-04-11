/**
 * 
 */
package Fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.github.WattTime.watttime_android.R;

public class SettingsFragment extends PreferenceFragment {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
	//TODO replace this with pretty button implementation
}
