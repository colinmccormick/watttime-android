package com.github.WattTime.watttime_android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private String apiAbbrev;

	public final boolean debug0 = true;
	public final boolean debug1 = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		/* Launch loading screen */
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		/* Get the last know network (coarse) location */
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		String locationProvider = LocationManager.NETWORK_PROVIDER; //Change to GPS_PROVIDER for fine loc
		Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
		
		if (debug0) {
			lastKnownLocation.setLatitude(-72.519);
			lastKnownLocation.setLongitude(42.372);
		}
		
		/* Check to see if we're recreating from a recent bundle */
		if (savedInstanceState != null) {
			long MAX_TIME = 172800000; //TWO DAYS
			long TIME_SAVED = savedInstanceState.getLong("time"); 
			long TIME_DELTA = System.currentTimeMillis() - TIME_SAVED;
			
			float MAX_DISTANCE = Float.valueOf("32186.9"); //TWENTY MILES TODO Externalize
			Location LOCATION_SAVED = (Location) savedInstanceState.getParcelable("location");
			float DISTANCE_DELTA = lastKnownLocation.distanceTo(LOCATION_SAVED);
			
			/*Try and get the saved abbrev (save an api call) */
			if (TIME_DELTA < MAX_TIME && DISTANCE_DELTA < MAX_DISTANCE) {
				String savedAb = savedInstanceState.getString("apiAbbrev");
				apiAbbrev = savedAb != null ? savedAb : parseAbJSON(getApiData(makeAbUrl(lastKnownLocation))); //Make sure its not null!
			}
			
		} else {
			apiAbbrev = parseAbJSON(getApiData(makeAbUrl(lastKnownLocation)));
		}
		
		if (debug1) {apiAbbrev = "ISONE";}
		
		/* Now that we have our ISO abbreviation, go ahead and pull the data for it*/
		
		

	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		String locationProvider = LocationManager.NETWORK_PROVIDER; //Change to GPS_PROVIDER for fine loc
		Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
		
	    savedInstanceState.putString("apiAbbrev", apiAbbrev);
	    savedInstanceState.putLong("time", System.currentTimeMillis());
	    savedInstanceState.putParcelable("location",  lastKnownLocation);
	    super.onSaveInstanceState(savedInstanceState);
	}
	
	/*
	 * Utility function to take in a location object and return the properly formatted APIURL
	 * @see http://watttime-grid-api.herokuapp.com/api/v1/docs/#!/balancing_authorities/Balancing_Authority_get_0
	 */
	private String makeAbUrl(Location loc) {
		/*Debug info */
		Log.d("MainActivity", Double.toString(loc.getLatitude()));
		Log.d("MainActivity", Double.toString(loc.getLongitude()));
		
		/*Form API Url */
		double lat = loc.getLatitude();
		double lon = loc.getLongitude();
		final String locFormat = "%+09.4f"; //TODO extern this
		String apiUrl = String.format(Locale.US, "http://watttime-grid-api.herokuapp.com/api/v1/balancing_authorities/?format=json&loc=POINT+%%28" +
				locFormat + "%%20" + locFormat + "%%29", lat,lon); //TODO Externalize this.
		return apiUrl;

	}
	
	/* Utility function to return the JSON provided by the server at the inputted string.
	 * Currently logs an error if there is an error, and returns null
	 * Otherwise returns the JSON delivered by the server.
	 */
	private JSONObject getApiData(String APIurl) {
		/* Check for network connection! */
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo == null || !networkInfo.isConnected()) {
			/*This means there's no network connection, so raise an error */
			Toast.makeText(this, R.string.connectivity_error, Toast.LENGTH_SHORT).show(); //TODO Better error dialog
			return null;
		}
		
		/* Fire off a new async task to get the JSON from network */
		class getFromAPI extends AsyncTask<String, Void, JSONObject> {
			@Override
			protected JSONObject doInBackground(String... urlIn) {
				Log.d("APIDATA", "Trying to get API @ " + urlIn[0]);
				InputStream inStr = null;
				JSONObject jSON;
				
				/*Pull JSON from server*/
		  		try {
		  			URL url = new URL(urlIn[0]);
		  			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		            conn.setReadTimeout(10000 /* milliseconds */);
		            conn.setConnectTimeout(15000 /* milliseconds */);
		            conn.setRequestMethod("GET");
		            conn.setDoInput(true);
		            conn.connect();
		            int response = conn.getResponseCode();
		            Log.d("APIabbrevGet", "The response is: " + response);
		            inStr = conn.getInputStream();
		            BufferedReader reader = new BufferedReader(new InputStreamReader(inStr, "UTF-8"), 8);
		            StringBuilder sb = new StringBuilder();

		            String line = null;
		            while ((line = reader.readLine()) != null)
		            {
		                sb.append(line + "\n");
		            }
		            jSON = new JSONObject(sb.toString());
		  		} catch (MalformedURLException e) {
		  			Log.e("GetAPIData", "Malformed URL");
		  			return null;
		  		} catch (JSONException e) {
		  			Log.e("GetAPIData", "Error parsing Json");
		  			return null;
				} catch (IOException e) {
					Log.e("GetAPIData", "IOError");
		  			return null;
				} finally {
		  			try {
						inStr.close();
					} catch (IOException e) {
						Log.e("GetAPIData", "IOError");
			  			return null;
					}
		  		}
		  		return jSON;
			}
		}
		
		 AsyncTask<String, Void, JSONObject> task = new getFromAPI().execute(APIurl);
		 try {
			return task.get();
		 } catch (InterruptedException e) {
			 Log.e("GetAPIData", "Interrupted");
  			 return null;
		 } catch (ExecutionException e) {
			 Log.e("GetAPIData", "Execution error");
  			 return null;
		 }
	}
	
	private String parseAbJSON(JSONObject jSON) {
		String abbrev;
		/*Process JSON */
  		try {
			abbrev = jSON.getString("abbrev");
		} catch (JSONException e) {
			Log.e("GetAPIData", "Error parsing Json (#2)");
  			return null;
		}
  		return abbrev;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
