package com.github.WattTime.watttime_android.Activities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.WattTime.watttime_android.R;
import com.github.WattTime.watttime_android.ASyncTasks.APIGet;
import com.github.WattTime.watttime_android.DataModels.FuelDataList;
import com.github.WattTime.watttime_android.Fragments.SettingsFragment;
import com.github.WattTime.Keys;

public class MainActivity extends Activity {

	/* Activity string with information on current API abbrev.*/
	private String apiAbbrev;
	
	/* Comment goes here */
	private FuelDataList mFuelData;

	/* Object allowing us to change visibility of the progress bar
	 * TODO change this to a spinning windmill. */
	private ProgressBar mProgressBar;
	
	private TextView mPercentage;
	
	/* Resources for the navigation drawer */
	private String[] mPlanetTitles; //FIXME
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Launch loading screen
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mProgressBar = (ProgressBar) findViewById(R.id.main_progressbar);
		mPercentage = (TextView) findViewById(R.id.main_percentage);
		
		// ----- Set up the Nav drawer ------- //
		mPlanetTitles = getResources().getStringArray(R.array.navigation_array); //TODO
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mPlanetTitles)); //This is where you should edit the adapter - can expand
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        
        mTitle = mDrawerTitle = getTitle(); //TODO 
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
                ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActionBar().setTitle(mTitle);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActionBar().setTitle(mDrawerTitle);
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

		// Get the last known network (coarse) location
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		String locationProvider = LocationManager.NETWORK_PROVIDER; //Change to GPS_PROVIDER for fine loc
		Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);


		// Check to see if we're recreating from a recent bundle
		if (savedInstanceState != null) {

			// If we are, then check to see if the location is fresh and close by
			long MAX_TIME = 172800000; //TWO DAYS
			long TIME_SAVED = savedInstanceState.getLong("time"); 
			long TIME_DELTA = System.currentTimeMillis() - TIME_SAVED;

			float MAX_DISTANCE = Float.valueOf("32186.9"); //TWENTY MILES TODO Externalize
			Location LOCATION_SAVED = (Location) savedInstanceState.getParcelable("location");
			float DISTANCE_DELTA = lastKnownLocation.distanceTo(LOCATION_SAVED);

			// Try and get the saved abbrev (save an api call)
			if (TIME_DELTA < MAX_TIME && DISTANCE_DELTA < MAX_DISTANCE) {
				String savedAb = savedInstanceState.getString("apiAbbrev");
				apiAbbrev = savedAb;
			}

		} else if (internetConnected()) {
			if (apiAbbrev == null) {
				//apiabbrev must be generated new.
				new APIGet() {
					@Override
					protected void onPostExecute(JSONArray jSON) {
						apiAbbrev = parseAbJSON(jSON);
						getPercentData();
					}
				}.execute(makeAbUrl(lastKnownLocation));
			} else {
				//API abbrev was retrieved from the stored data.
				getPercentData();
			}
		} else {
			//raise error - no internet.
			/*This means there's no network connection, so raise an error */
			Toast.makeText(this, R.string.connectivity_error, Toast.LENGTH_SHORT).show(); //TODO Better error dialog
		}
	}
	
	private void getPercentData() {
		new APIGet() {
			@Override
			protected void onPostExecute(JSONArray jSON) {
				mFuelData = parseDataJSON(jSON);
				double d = mFuelData.getCurrentPercent();
				String percent = String.format(Locale.US, "%2.1f", d * 100);
				mPercentage.setText(percent)	;
				mProgressBar.setVisibility(View.GONE);
				mPercentage.setVisibility(View.VISIBLE);
			}
		}.execute(makeDataUrl(apiAbbrev));
	}
	
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
          return true;
        }
        // Now handle code for clicking on settings button(s)
        switch (item.getItemId()) {
        case R.id.action_settings:
        	
        	return launchSettingsFragment();
        default:
        	return super.onOptionsItemSelected(item);
        }
    }
    
    private boolean launchSettingsFragment() {
    	// Hide the progressbar/percentage indicators before launching the fragment.
    	if (mProgressBar != null && mProgressBar.getVisibility() == View.VISIBLE) {
    		mProgressBar.setVisibility(View.INVISIBLE);
    		Log.d("SettingsFragment", "Changing visiblity of progressbar to invisible.");
    	} else if (mPercentage != null && mPercentage.getVisibility() == View.VISIBLE) {
    		mPercentage.setVisibility(View.INVISIBLE);
    		Log.d("SettingsFragment", "Changing visiblity of percentage to invisible");
    	}
    	getFragmentManager()
    	.beginTransaction()
    	.replace(android.R.id.content, new SettingsFragment() {
    		@Override 
    		public void onDestroy() {
    			if (mProgressBar != null && mProgressBar.getVisibility() == View.INVISIBLE) {
    				Log.d("SettingsFragment", "Changing visiblity of progressbar back to visible.");
    				mProgressBar.setVisibility(View.VISIBLE);
    			} else if (mPercentage != null && mPercentage.getVisibility() == View.INVISIBLE) {
    				Log.d("SettingsFragment", "Changing visiblity of percentage back to visible");
    				mPercentage.setVisibility(View.VISIBLE); //TODO check for placeholder text then set visibility
    			}
    			super.onDestroy();
    		}
    	})
    	.addToBackStack(null)
    	.commit();
    	return true;
    }
    
	/* Used for the nav drawer actions. */
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
	    @Override
	    public void onItemClick(AdapterView parent, View view, int position, long id) {
	        //Do something!
	    }
	}
	
	/*Make sure our titles are correct*/
	@Override
	public void setTitle(CharSequence title) {
	    mTitle = title;
	    getActionBar().setTitle(mTitle);
	}
	
    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
    	// Hide things that wouldn't make sense otherwise
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList); //use to check if the drawer is open
        return super.onPrepareOptionsMenu(menu);
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

	/**
	 * Utility function to take in a location object and return the properly formatted APIURL
	 * @see http://watttime-grid-api.herokuapp.com/api/v1/docs/#!/balancing_authorities/Balancing_Authority_get_0
	 * API takes in location in LONGITUDE, LATITUDE.
	 */
	private String makeAbUrl(Location loc) {
		//Debug info
		Log.d("MainActivity", Double.toString(loc.getLatitude()));
		Log.d("MainActivity", Double.toString(loc.getLongitude()));

		//Form API Url
		double lat = loc.getLatitude();
		double lon = loc.getLongitude();
		final String locFormat = "%+09.4f"; //TODO extern this
		String apiUrl = String.format(Locale.US, "http://watttime-grid-api.herokuapp.com/api/v1/balancing_authorities/?format=json&loc=POINT+%%28" +
				locFormat + "%%20" + locFormat + "%%29", lon,lat); //TODO Externalize this. 
		return apiUrl;

	}

	/**
	 * Utility function to make the API URL to get the data for our ISO
	 * @see http://watttime-grid-api.herokuapp.com/api/v1/docs/#!/datapoints/Data_Point_List_get_0
	 */
	private String makeDataUrl(String abbrev) {
		return String.format(Locale.US, "http://watttime-grid-api.herokuapp.com/api/v1/datapoints/?ba=%s&format=json", abbrev);
	}

	/**
	 * Utility function to determine whether the internet is connected or not
	 * @return Boolean representing internet connectivity.
	 */
	private boolean internetConnected() {
		/* Check for network connection! */
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo == null || !networkInfo.isConnected()) {
			return false;
		}
		return true;
	}

	/**
	 * This parses the JSON array returned for the ISO abbreviation,
	 * parsing JSON is quite easy actually.
	 */
	private String parseAbJSON(JSONArray jSON) {
		/*Need to check if the JSON data returned is null (error, and handle it)*/
		if (jSON == null) {
			Toast.makeText(this, R.string.server_error, Toast.LENGTH_SHORT).show(); //TODO Better error dialog
			return null;
		}

		String abbrev;
		/*Process JSON */
		if (jSON.length() < 1) {
			Toast.makeText(this, R.string.server_error, Toast.LENGTH_SHORT).show(); //TODO Better error dialog
			Log.w("JSON Parse", "Didn't have an iso for this location.");
			return null;
		}
		try {
			abbrev = jSON.getJSONObject(0).getString("abbrev");
		} catch (JSONException e) {
			Log.e("GetAPIData", "Error parsing Json (#2)");
			return null;
		}
		return abbrev;
	}

	/**
	 * TODO
	 */
	private FuelDataList parseDataJSON(JSONArray jSON) {
		try {
			return new FuelDataList(jSON.getJSONObject(0));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			Log.e("MainActivity", "Couldn't parse something somewhere.");
			e.printStackTrace();
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
