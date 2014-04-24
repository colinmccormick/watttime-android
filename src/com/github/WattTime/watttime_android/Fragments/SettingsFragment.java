/**
 * 
 */
package com.github.WattTime.watttime_android.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.github.WattTime.watttime_android.R;

public class SettingsFragment extends PreferenceFragment {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        setHasOptionsMenu(false);
        // Get the custom preference
        Preference mypref = (Preference) findPreference("defaultreset");
        mypref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        	@Override
        	public boolean onPreferenceClick(Preference unused) {
        		final Context mContext = getActivity().getApplicationContext();
        		if (mContext == null) {
        			return false;
        		} else {
        			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        			SharedPreferences.Editor editor = preferences.edit();
        			editor.clear();
        			editor.commit();
        			PreferenceManager.setDefaultValues(mContext, R.xml.preferences, true);
        			addPreferencesFromResource(R.xml.preferences);
        			getActivity().getFragmentManager().popBackStackImmediate();
        			return true;
        		}
			} 
		});
    }
	//TODO replace this with pretty button implementation
}
