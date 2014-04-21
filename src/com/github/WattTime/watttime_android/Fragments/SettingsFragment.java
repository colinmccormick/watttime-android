/**
 * 
 */
package com.github.WattTime.watttime_android.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.github.WattTime.watttime_android.R;

public class SettingsFragment extends PreferenceFragment {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout mView = (LinearLayout) super.onCreateView(inflater, container, savedInstanceState);
        final Context mContext = getActivity().getApplicationContext();
        Button mButton = new Button(mContext);
        mButton.setText(mContext.getResources().getString(R.string.reset_defaults));
        mView.addView(mButton);
        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            	SharedPreferences.Editor editor = preferences.edit();
            	editor.clear();
            	editor.commit();
            	PreferenceManager.setDefaultValues(mContext, R.xml.preferences, true);
            	addPreferencesFromResource(R.xml.preferences);
            	getActivity().getFragmentManager().popBackStackImmediate();
            	//goBackWithIntent();
            }
        });

    return mView;
}

private void goBackWithIntent() { //TODO
	Intent a = new Intent();
    a.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivity(a);
}
	
	//TODO replace this with pretty button implementation
}
