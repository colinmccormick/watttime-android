package com.github.WattTime.watttime_android.Activities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.github.WattTime.watttime_android.R;
import com.github.WattTime.watttime_android.ASyncTasks.APIGet;
import com.github.WattTime.watttime_android.DataModels.FuelDataList;
import com.github.WattTime.watttime_android.Fragments.SettingsFragment;

public class MainActivity extends Activity {

	/* Activity string with information on current API abbrev.*/
	private String apiAbbrev;

	/* Internal datamodel holding the most recent fuel data. */
	private FuelDataList mFuelData;

	/* String array containing our preferences */
	private String[] mRenewPrefs;
	
	/* View objects that need to be edited by Java.*/
	private ProgressBar mProgressBar; //TODO Change to windmill
	private TextView mPercentage;
	private Menu mMenu;
	private XYPlot mPlot;

	/*Constant filename for caching data */
	private final String TEMPFILENAME = "datapoints";
	
	/*Constants to describe internet quality */
	private final int HIGH_QUALITY = 0;
	private final int MED_QUALITY = 1;
	private final int LOW_QUALITY = 2;

	/*Resources file so we don't have to load it over and over */
	private Resources mRes; 
	private FragmentManager mFragMan;

	/* Resources for the navigation drawer */
	private String[] mNavigationItems;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private CharSequence mDrawerTitle;
	private CharSequence mTitle;

	/* -------------- APP LIFECYCLE METHODS ------------------ */

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@SuppressWarnings("resource") //scanner is tossed after use.
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	protected void onCreate(Bundle savedInstanceState) { //TODO check if preferences have changed, reload data

		// Launch loading screen
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mProgressBar = (ProgressBar) findViewById(R.id.main_progressbar);
		mPlot = (XYPlot) findViewById(R.id.main_xyplot_main); // For some reason calling this main_xyplot results 
															  // in an exception. What!? 
		mPercentage = (TextView) findViewById(R.id.main_percentage);
		mRes = getResources();
		mFragMan = getFragmentManager();
		
		//Set default preferences on first run, solves bug (?)
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		// ----- Set up the Nav drawer ------- //
		mNavigationItems = mRes.getStringArray(R.array.navigation_array);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		mDrawerList.setAdapter(new ArrayAdapter<String>(this,
				R.layout.drawer_list_item, mNavigationItems)); //This is where you should edit the adapter - can expand
		// Set the list's click listener
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		// Set the title
		mTitle = mDrawerTitle = getTitle();
		//Load the layout and set the toggle correctly.
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerToggle = new ActionBarDrawerToggle(
				this,                  /* host Activity */
				mDrawerLayout,         /* DrawerLayout object */
				R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
				R.string.drawer_open,  /* "open drawer" description */
				R.string.drawer_close  /* "close drawer" description */
				) {

			/* Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				getActionBar().setTitle(mTitle);
				mMenu.setGroupVisible(R.id.hide_when_drawer, true);
			}

			/* Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				getActionBar().setTitle(mDrawerTitle);
				mMenu.setGroupVisible(R.id.hide_when_drawer, false);	
			}
		};

		// Set the drawer toggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		//Nav drawer setup finished.

		// Get the last known network (coarse) location
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		String locationProvider = LocationManager.NETWORK_PROVIDER;
		Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
		
		//Check for preference changes
		boolean prefRefreshed = false;
		if (preferencesChanged()) {
			refreshData(true);
			prefRefreshed = true;
		}
		
		
		// Make the file before the if suite
		File file = new File(this.getCacheDir(), TEMPFILENAME);
		Log.d("Fileservice", "making a tempfile");
		Log.d("Fileservice", file == null ? "null file!" : Long.toString(file.length()) + " size of file");

		//Load some constants about data age.
		long MAX_DATA_AGE = mRes.getInteger(R.integer.min_data_refresh);
		double MAX_DISTANCE = mRes.getInteger(R.integer.max_distance_change);

		// Check to see if we're recreating from a recent bundle
		if (savedInstanceState != null) {
			Log.d("onCreate", "trying to create from previous bundle");
			// If we are, then check to see if the location is fresh and close by
			long TIME_SAVED = savedInstanceState.getLong("time"); 
			long TIME_DELTA = System.currentTimeMillis() - TIME_SAVED;
			Location LOCATION_SAVED = (Location) savedInstanceState.getParcelable("location");
			float DISTANCE_DELTA = lastKnownLocation.distanceTo(LOCATION_SAVED);

			// Try and get the saved abbrev (save an api call)
			if (TIME_DELTA < MAX_DATA_AGE && DISTANCE_DELTA < MAX_DISTANCE) {
				Log.d("MainActivity", "Getting apiabbrev from stored data.");
				String savedAb = savedInstanceState.getString("apiAbbrev");
				FuelDataList fuelData = (FuelDataList) savedInstanceState.getParcelable("data");
				if (savedAb != null && fuelData != null) { //Check to see if we got decent data from the bundle
					apiAbbrev = savedAb;
					launchViews(fuelData);
				} //otherwise try elsewhere.
			} 
		//Check to see if we can recreate from cached data
		//PrefRefreshed will be true if we just refreshed the data, so don't do it again
		} else if (file != null && file.length() > 0 && !prefRefreshed) {
			Log.d("Lifecycle", "The saved file exists, checking it.");
			JSONArray jSON = null;
			try {
				//Read our saved file
				jSON = new JSONArray(new Scanner(file).useDelimiter("\\A").next());

				//Pull in the old location
				Location oldLoc = new Location(LocationManager.NETWORK_PROVIDER);
				double lat = jSON.getJSONObject(1).getDouble("lat");
				double lon = jSON.getJSONObject(1).getDouble("lon");
				oldLoc.setLatitude(lat);
				oldLoc.setLongitude(lon);

				//Calculate distance and time deltas, check if they're allowable.
				float DISTANCE_DELTA = lastKnownLocation.distanceTo(oldLoc);
				long TIME_SAVED = file.lastModified();
				long TIME_DELTA = System.currentTimeMillis() - TIME_SAVED;

				//If they are, then file is OK to create from.
				if (DISTANCE_DELTA < MAX_DISTANCE) {
					Log.d("Lifecycle", "File was good to pull api abbrev from");
					String url = jSON.getJSONObject(0).getString("next");
					Log.d("url", url);
					if (!url.equals("null") && url != null) { //Need to check and see if it pulled this correctly.
						//If it didn't it's a corrupt save file.
						//Furthermore check for the JSON null (Which is different from regular null)
						apiAbbrev = pullApiAbbrev(url);
						if (TIME_DELTA < MAX_DATA_AGE) {
							Log.d("Lifecycle", "File was good to pull old data from");
							doPostPercentGet(jSON, false);
						}
					}
				}
			} catch (JSONException e) {
				Log.e("MainActivity", "Couldn't create from saved json");
				//This means our savefile was so corrupt it couldn't load, but it's
				//a nonfatal error, because it can reload the data from the server.
			} catch (FileNotFoundException e) {
				Log.wtf("Files", "Should never get here because it checks if the file's length is nonzero");
			}
		}
		//So somewhere we didn't pick up the correct data or it needs to be re downloaded
		if ((apiAbbrev == null || mFuelData == null) && internetConnected()) {
			//Check to see if we need to pull all of it again
			if (apiAbbrev == null) {
				Log.d("MainActivity", "Pulling the ApiAbbrev from the server");
				new APIGet() {
					@Override
					protected void onPostExecute(JSONArray jSON) {
						apiAbbrev = parseAbJSON(jSON);
						getPercentData();
					}
				}.execute(makeAbUrl(lastKnownLocation));
			} else {
				//API abbrev was retrieved from the stored data but not the percent data.
				getPercentData();
			}
		} else if (!internetConnected()){
			//This means there's no network connection, so raise an error
			throwFatalNetworkError();
			return;
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		Log.d("MA", "Saving Instance state");
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		String locationProvider = LocationManager.NETWORK_PROVIDER; //Change to GPS_PROVIDER for fine loc
		Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);

		savedInstanceState.putString("apiAbbrev", apiAbbrev);
		savedInstanceState.putLong("time", System.currentTimeMillis());
		savedInstanceState.putParcelable("location",  lastKnownLocation);
		savedInstanceState.putParcelable("data", mFuelData);
		super.onSaveInstanceState(savedInstanceState);
	}


	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) { 
		if (keyCode == KeyEvent.KEYCODE_MENU) { 
			if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
				mDrawerLayout.closeDrawers();
			} else {
				mDrawerLayout.openDrawer(mDrawerList);
			}
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	private boolean launchGraph() {
		if (mPlot == null || mFuelData == null || mFuelData.size() == 0) {
			Log.e("LaunchGraph", "You tried to launch a graph without data");
			return false;
		} else {
			Log.d("LaunchGraph", "Launching the graph");
			class GraphGet extends AsyncTask<Context, Void, Boolean> {
				@Override
				protected Boolean doInBackground(Context... context) {
					//We have to pull more data so we have at least the last day of data.

					
					//mPlot.setDomainBoundaries(0, 1440, BoundaryMode.FIXED); //TODO xml?
					mPlot.setRangeBoundaries(0,100, BoundaryMode.FIXED); //TODO.... 
					mPlot.setRangeStepValue(10);
					
					mPlot.setDomainValueFormat(new Format() {
						private static final long serialVersionUID = 1L; //FIXME ?
						
			            private SimpleDateFormat dateFormat = new SimpleDateFormat("hha", Locale.US); //TODO internationalize
			 
			            @Override
			            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
			                long timestamp = ((Number) obj).longValue();
			                Date date = new Date(timestamp);
			                return dateFormat.format(date, toAppendTo, pos);
			            }

			            @Override
			            public Object parseObject(String source, ParsePosition pos) {
			                return null;
			            }
			        });
					
					LineAndPointFormatter series1Format = new LineAndPointFormatter();
					series1Format.setPointLabelFormatter(null);
					series1Format.configure(context[0], R.xml.line_point_formatter_with_plf1);
					mPlot.addSeries(mFuelData.getLastDayPoints(), series1Format);
					return true;
				}
				@Override
				protected void onPostExecute(Boolean success) {
					if (success) {
						mPlot.setVisibility(View.VISIBLE);
					} else {
						throwFatalAppError();
					}
				}
			}
			new GraphGet().execute(this);
			return true;
		}
	}

	/* -------------- NAVIGATION DRAWER METHODS ------------- */

	private boolean launchSettingsFragment() {
		//Check to see if there's already a settings fragment there.
		SettingsFragment test = (SettingsFragment) getFragmentManager().findFragmentByTag("settingsTag");
		if (test != null && test.isVisible()) {
			Log.d("Settings frag", "Refused to open another settingsfragment");
			return false;
		}
		
		// Hide the progressbar/percentage indicators before launching the fragment.
		if (mProgressBar != null && mProgressBar.getVisibility() == View.VISIBLE) {
			mProgressBar.setVisibility(View.INVISIBLE);
			Log.d("SettingsFragment", "Changing visiblity of progressbar to invisible.");
		} else if (mPercentage != null && mPercentage.getVisibility() == View.VISIBLE) {
			mPercentage.setVisibility(View.INVISIBLE);
			Log.d("SettingsFragment", "Changing visiblity of percentage to invisible");
		} 
		if (mPlot != null && mPlot.getVisibility() == View.VISIBLE) {
			Log.d("SettingsFragment", "Changing visiblity of graph to invisible");
			mPlot.setVisibility(View.INVISIBLE);
		}
		mFragMan
		.beginTransaction()
		.replace(R.id.main_root, new SettingsFragment() {
			@Override 
			public void onDestroy() {
				if (mProgressBar != null && mProgressBar.getVisibility() == View.INVISIBLE) {
					Log.d("SettingsFragment", "Changing visiblity of progressbar back to visible.");
					mProgressBar.setVisibility(View.VISIBLE);
				} else if (mPercentage != null && mPercentage.getVisibility() == View.INVISIBLE) {
					Log.d("SettingsFragment", "Changing visiblity of percentage back to visible");
					mPercentage.setVisibility(View.VISIBLE);
				} 
				if (mPlot != null && mPlot.getVisibility() == View.INVISIBLE) {
					Log.d("SettingsFragment", "Changing visiblity of graph back to visible");
					mPlot.setVisibility(View.VISIBLE);
					
				}
				super.onDestroy();
			}
		}, "settingsTag")
		.addToBackStack(null)
		.commit();
		return true;
	}
	private void launchAboutFragment() {
		//do
	}
	private void launchEmbeddedDevices() {
		//do
	}


	/* Used for the nav drawer actions. */
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			mDrawerLayout.closeDrawer(mDrawerList);
			Log.d("Clicked on pos", position + "");
			switch (position) {
			case 0:
				//HOME
				Log.d("nav drawer", "home");
				//If settingsfrag is present and visible, close it.
				SettingsFragment settingsFrag = (SettingsFragment) mFragMan.findFragmentByTag("settingsTag");
				if (settingsFrag != null && settingsFrag.isVisible()) {
					Log.d("Settings frag", "Closing settingsfragment");
					mFragMan.popBackStackImmediate();
				}
				break;
			case 1:
				//EMBEDDED DEVICES
				Log.d("nav drawer", "embedded dev");
				launchEmbeddedDevices();
				break;
			case 2:
				//SETTINGS
				Log.d("nav drawer", "settings");
				launchSettingsFragment();
				break;
			case 3:
				//ABOUT
				Log.d("nav drawer", "about");
				launchAboutFragment();
				break;
			}
		}
	}


	/*Make sure our titles are correct*/
	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getActionBar().setTitle(mTitle);
	}

	/* ---------------- OPTIONS MENU METHODS ------------ */

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		mMenu = menu;
		return true;
	}

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content view
		// Hide things that wouldn't make sense otherwise
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList); //use to check if the drawer is open
		if (drawerOpen) {
			return false;
		}
		return true;
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
		case R.id.action_refresh:
			return refreshData(false);
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	/* ----------------- JSON PARSING METHODS ------------------- */

	/**
	 * This parses the JSON array returned for the ISO abbreviation,
	 * parsing JSON is quite easy actually.
	 */
	private String parseAbJSON(JSONArray jSON) {
		/*Need to check if the JSON data returned is null (error, and handle it)*/
		if (jSON == null) {
			throwFatalServerError();
			return null;
		}

		String abbrev;
		/*Process JSON */
		if (jSON.length() < 1) {
			throwFatalServerError();
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

	private FuelDataList parseDataJSON(JSONArray jSON) {
		if (jSON == null) {
			return null;
		}
		mRenewPrefs = getRenewablePrefs();
		Log.d("Attempting to parse JSON", jSON.toString());
		Log.d("Renewableprefs", Arrays.toString(mRenewPrefs));
		try { //TODO check for length of json and add to existing list if it's short.
			return new FuelDataList(jSON.getJSONObject(0), mRenewPrefs);
		} catch (JSONException e) {
			Log.e("MainActivity", "Couldn't parse something somewhere.");
			e.printStackTrace();
			throwFatalServerError();
			return null;
		}
	}

	/* -------------------- API DATA PULL METHODS ----------- */

	private void getPercentData() {
		if (apiAbbrev == null) {
			return;
		}
		new APIGet() {
			@Override
			protected void onPostExecute(JSONArray jSON) {
				doPostPercentGet(jSON, true);
			}
		}.execute(makeFullDataUrl(apiAbbrev, internetQuality()));
	}

	private boolean refreshData(boolean force) {
		Log.i("MA", "Refreshing my data");
		if (apiAbbrev == null) {
			return false; //TODO
		} else {
			String url;
			if (force || mFuelData != null) {
				url = makeShortDataUrl(apiAbbrev, internetQuality(), mFuelData.getTimeUpdated());
			} else {
				url = makeFullDataUrl(apiAbbrev, internetQuality());
			}
			new APIGet() {
				@Override
				protected void onPreExecute() {
					mPercentage.setVisibility(View.INVISIBLE);
					mPlot.setVisibility(View.INVISIBLE);
					mProgressBar.setVisibility(View.VISIBLE);
				}
				@Override
				protected void onPostExecute(JSONArray jSON) {
					doPostPercentGet(jSON, true); //TODO
				}
			}.execute(url);
		}
		return true;
	}

	private void doPostPercentGet(JSONArray jSON, boolean saveIt) {
		if (jSON == null) {
			throwFatalServerError();
			return;
		} else {
			FuelDataList data = parseDataJSON(jSON);
			//We have to check if our data is actually there. (Weird edge case error)
			if (data.size() > 0) {
				Log.d("dppget", "data was size.");
				launchViews(data);
			} else if (apiAbbrev == null || data.size() < 0) {
				Log.d("dppget", "apiabbrev null");
				Log.e("Dppget", "Something's fucky"); //FIXME
				//getPercentData();
			}
		}
		
		//Save our JSON to a file so we might not need to get it next time.
		if (saveIt) {
			File file;
			Log.d("Saving", "Saving my JSON to a file.");
			
			// Get the last known network (coarse) location
			LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
			String locationProvider = LocationManager.NETWORK_PROVIDER;
			Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
			JSONObject loc = new JSONObject();
			try {
				loc.put("lat", lastKnownLocation.getLatitude());
				loc.put("lon", lastKnownLocation.getLongitude());
				jSON.put(1,loc);
			} catch (JSONException e1) {
				Log.w("JSONSave", "Couldn't store latlon!");
			}
			try {
				file = new File(this.getCacheDir(), TEMPFILENAME);
				FileWriter fw = new FileWriter(file);
				fw.write(jSON.toString());
				fw.flush();
				fw.close();
			} catch (IOException e) {
				Log.w("PostPercentGet", "Couldn't write my JSON to a file");
			}
		}
	}
	
	private void launchViews(FuelDataList fuelData) {
		mFuelData = fuelData;
		launchGraph();
		//This pulls our preferences, then extracts the keys we consider "green"
		//And uses them to calculate the green %.

		double d = mFuelData.getCurrentPercent();
		
		String percentFormat = mRes.getString(R.string.percentage_format);
		String percent = String.format(Locale.US, percentFormat, d * 100);
		mPercentage.setText(percent)	;
		mProgressBar.setVisibility(View.GONE);
		mPercentage.setVisibility(View.VISIBLE);
	}


	/* -------------------- URL FORMING METHODS ------------- */

	/**
	 * Utility function to take in a location object and return the properly formatted APIURL
	 * @see http://api.watttime.org/api/v1/docs/#!/balancing_authorities/Balancing_Authority_get_0
	 * API takes in location in LONGITUDE, LATITUDE.
	 */
	private String makeAbUrl(Location loc) {
		//Debug info
		Log.d("MainActivity", Double.toString(loc.getLatitude()));
		Log.d("MainActivity", Double.toString(loc.getLongitude()));

		//Form API Url
		double lon = loc.getLongitude();
		double lat = loc.getLatitude();
		final String abbrStem= mRes.getString(R.string.abbr_api_stem);
		String apiUrl = String.format(Locale.US, abbrStem, lon,lat);
		return apiUrl;

	}

	/**
	 * Utility function to make the API URL to get the data for our ISO
	 * @see http://api.watttime.org/api/v1/docs/#!/datapoints/Data_Point_List_get_0
	 * Format args are BA, Start at, end at, page size, freq (5m 10m 1h)
	 */
	private String makeFullDataUrl(String balancingAuth, int internetQual) {
		Time startTime = new Time();
		startTime.setToNow();
		startTime.set(0, 0, 0, startTime.monthDay, startTime.month, startTime.year);
		startTime.switchTimezone(Time.getCurrentTimezone());
		return makeShortDataUrl(balancingAuth, internetQual, startTime);
	}
	
	private String makeShortDataUrl(String balancingAuth, int internetQual, Time lastUpdate) {
		String dataStem = mRes.getString(R.string.data_api_stem);
		Time startTime = lastUpdate;
		Time endTime = new Time(startTime);
		endTime.set(59, 59, 23, endTime.monthDay, endTime.month, endTime.year);
		
		endTime.switchTimezone(Time.getCurrentTimezone());
		startTime.switchTimezone(Time.getCurrentTimezone());
		
		String start = startTime.format3339(false);
		String end = endTime.format3339(false);
		String pageSize;
		String freq;
		Log.d("IQ NUM", Integer.toString(internetQual));
		switch (internetQual) {
		case HIGH_QUALITY:
			Log.d("IQ", "high");
			pageSize = "144"; //TODO FIX to 5m and 288.... API is too slow (20seconds as of now)
			freq = "10m";
			break;
		case MED_QUALITY:
			Log.d("IQ", "med");
			pageSize = "144";
			freq = "10m";
			break;
		default:
			Log.d("IQ", "low");
			pageSize = "24";
			freq = "1h";
			break;
		}
		return String.format(Locale.US, dataStem, balancingAuth, start, end, pageSize, freq);
	}
	/**
	 * Utility method to get the apiabbrev out of a data API url.
	 * @param url A properly formed data url, like the ones returned by makeDataUrl
	 * @return The api abbreviation from this data.
	 * @suppress magic
	 */
	private String pullApiAbbrev(String url) {
		Log.d("Pulling API Abbrev", url);
		return url.substring(61, (url.indexOf('&') == -1 ) ? url.length() : url.indexOf('&'));
	}

	/* -------------------- UTILITY METHODS ----------------- */
	
	private String[] getRenewablePrefs() {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		ArrayList<String> greens = new ArrayList<String>(17);
		if (sharedPrefs != null) {
			for (Entry<String, ?> kp : sharedPrefs.getAll().entrySet()) {
				try {
					if (Boolean.class.cast(kp.getValue())) {
						greens.add(kp.getKey());
					}
				} catch (ClassCastException e) {
					//pass
					//Error ignored because of non boolean prefs, which we don't care about..
					//Possibly fix with a separate prefs file for green data
					Log.i("Get Renew Prefs", "ClassCastException! Pass...");
				}
			}
		}
		String[] renewables = new String[17];
		renewables = greens.toArray(renewables);
		return renewables;
	}
	
	private boolean preferencesChanged() {
		if (mRenewPrefs == null) {
			Log.d("prefs", "Preferences did not change.");
			return false;
		} else {
			return !Arrays.deepEquals(mRenewPrefs, getRenewablePrefs());
		}
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
	
	private int internetQuality() {
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		switch (networkInfo.getType()) {
		case ConnectivityManager.TYPE_WIFI:
			Log.d("Internet quality", "High");
			return HIGH_QUALITY;
		case ConnectivityManager.TYPE_BLUETOOTH:
			Log.d("Internet quality", "High");
			return HIGH_QUALITY;
		case ConnectivityManager.TYPE_MOBILE:
			if (networkInfo.isRoaming()) {
				Log.d("Internet quality", "Low");
				return LOW_QUALITY;
			} else {
				Log.d("Internet quality", "Med");
				return MED_QUALITY;
			}
		default:
			Log.d("Internet quality", "Low");
			return LOW_QUALITY;
		}
	}
	
	private void throwFatalServerError() {
		Toast.makeText(this, R.string.server_error, Toast.LENGTH_SHORT).show();
	}
	
	private void throwFatalNetworkError() {
		Toast.makeText(this, R.string.connectivity_error, Toast.LENGTH_SHORT).show(); 
	}
	
	private void throwFatalAppError() {
		Toast.makeText(this, R.string.app_error, Toast.LENGTH_SHORT).show();
	}

}
