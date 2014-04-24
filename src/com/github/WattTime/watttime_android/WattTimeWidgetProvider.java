package com.github.WattTime.watttime_android;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.github.WattTime.watttime_android.ASyncTasks.APIGet;
import com.github.WattTime.watttime_android.Activities.MainActivity;
import com.github.WattTime.watttime_android.Utilites.Utilities;

//TODO
// Make the widget only update when the phone is awake
public class WattTimeWidgetProvider extends AppWidgetProvider {
	Context mContext;
	AppWidgetManager mAppWidgetManager;
	int[] mAppWidgetIds;

	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		//Save globals
		mContext = context;
		mAppWidgetManager = appWidgetManager;
		mAppWidgetIds = appWidgetIds;

		//Pull location data and make an API request(s) to get the percentage data
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		String locationProvider = LocationManager.NETWORK_PROVIDER;
		Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);

		final String abbrevStem = context.getResources().getString(R.string.abbr_api_stem);
		final String dataURL = context.getResources().getString(R.string.widget_api_stem);
		final String abbrevURL = Utilities.makeAbbrevURL(lastKnownLocation, abbrevStem);
		final ArrayList<String> prefs = Utilities.getPrefsAsList(PreferenceManager.getDefaultSharedPreferences(context));

		if (internetConnected(context)) {
			new APIGet() {
				@Override
				protected void onPostExecute(JSONArray jSON) {
					final String apiAbbrev = Utilities.parseAbbrevJSON(jSON);
					new APIGet() {
						@Override
						protected void onPostExecute(JSONArray jSONArray) {
							JSONObject jSON;
							double total = 0;
							double green = 0;
							try {
								jSON = jSONArray.getJSONObject(0);
								if (jSON.has("results")) {
									JSONObject results = jSON.getJSONArray("results").getJSONObject(0);
									if (results != null) {
										JSONArray genmix = results.getJSONArray("genmix");
										for(int j = 0; j < genmix.length(); j += 1) {
											JSONObject pt = genmix.getJSONObject(j);
											String fuel = pt.getString("fuel");
											double mw = pt.getDouble("gen_MW");
											total += mw;
											if (prefs.contains(fuel)) {
												green += mw;
											}
										}
									}
								}	
							} catch (JSONException e) {
								Log.e("WattTimeWidget", "Parsing data from server failed.");
							}
							double percentage = green / total;
							doPostComplete(percentage);
						}
					}.execute(String.format(Locale.US, dataURL, apiAbbrev));
				}
			}.execute(abbrevURL);
		} else {
			setIntentsNoNetwork();
			//pass. FIXME
		}
	}
	private void doPostComplete(double percent) {
		// Perform this loop procedure for each App Widget that belongs to this provider
		for (int appWidgetId : mAppWidgetIds) {
			// Create an Intent to launch ExampleActivity
			Intent intent = new Intent(mContext, MainActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);

			// Get the layout for the App Widget and attach an on-click listener
			// to the button
			RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.widget_layout);
			views.setTextViewText(R.id.widget_percentage, MessageFormat.format("{0,number,#.##%}", percent));
			views.setOnClickPendingIntent(R.id.watttime_appwidget, pendingIntent);

			// Tell the AppWidgetManager to perform an update on the current app widget
			mAppWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}
	private void setIntentsNoNetwork() { //TODO control flow better.
		for (int appWidgetId : mAppWidgetIds) {
			// Create an Intent to launch ExampleActivity
			Intent intent = new Intent(mContext, MainActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
			// Get the layout for the App Widget and attach an on-click listener
			// to the button
			RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.widget_layout);
			views.setOnClickPendingIntent(R.id.watttime_appwidget, pendingIntent);
			// Tell the AppWidgetManager to perform an update on the current app widget
			mAppWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}
	/**
	 * Utility function to determine whether the internet is connected or not
	 * @return Boolean representing internet connectivity.
	 */
	private boolean internetConnected(Context context) {
		/* Check for network connection! */
		ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo == null || !networkInfo.isConnected()) {
			return false;
		}
		return true;
	}

}

