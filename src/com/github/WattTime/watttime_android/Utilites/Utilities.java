package com.github.WattTime.watttime_android.Utilites;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

public class Utilities {
	public static String makeAbbrevURL(Location lastKnownLocation, String urlStem) {
		double lon = lastKnownLocation.getLongitude();
		double lat = lastKnownLocation.getLatitude();
		String apiUrl = String.format(Locale.US, urlStem, lon,lat);
		return apiUrl;
	}
	
	public static String[] getRenewablePrefs(SharedPreferences sharedPrefs) {
		ArrayList<String> greens = getPrefsAsList(sharedPrefs);
		String[] renewables = new String[17];
		renewables = greens.toArray(renewables);
		return renewables;
	}
	
	
	public static ArrayList<String> getPrefsAsList(SharedPreferences sharedPrefs) {
		ArrayList<String> greens = new ArrayList<String>(17);
		if (sharedPrefs != null) {
			for (Entry<String, ?> kp : sharedPrefs.getAll().entrySet()) {
				try {
					if (Boolean.class.cast(kp.getValue())) {
						greens.add(kp.getKey());
					}
				} catch (ClassCastException e) {
					//Error ignored because of non boolean prefs, which we don't care about..
					Log.i("Get Renew Prefs", "Cast error, pass.");
				}
			}
		}
		return greens;
	}
	
	public static String parseAbbrevJSON(JSONArray jSON) {
		/*Need to check if the JSON data returned is null (error, and handle it)*/
		if (jSON == null) {
			return null;
		}
		String abbrev;
		/*Process JSON */
		if (jSON.length() < 1) {
			Log.w("JSON Parse", "Didn't have an iso for this location.");
			return null; //TODO make a behavior for no iso for this loc.
		}
		try {
			abbrev = jSON.getJSONObject(0).getString("abbrev");
		} catch (JSONException e) {
			Log.e("GetAPIData", "Error parsing Json (#2)");
			return null;
		}
		return abbrev;
	}
	
}