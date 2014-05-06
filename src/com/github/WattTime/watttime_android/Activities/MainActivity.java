package com.github.WattTime.watttime_android.Activities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.Plot;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;
import com.github.WattTime.watttime_android.R;
import com.github.WattTime.watttime_android.ASyncTasks.APIGet;
import com.github.WattTime.watttime_android.DataModels.FuelDataList;
import com.github.WattTime.watttime_android.Fragments.AboutFragment;
import com.github.WattTime.watttime_android.Fragments.DeviceFragment;
import com.github.WattTime.watttime_android.Fragments.SettingsFragment;

public class MainActivity extends Activity {
	/* Logcat tag */
	public static final String tag = "WattTime";
	
	/* Activity string with information on current API abbrev.*/
	private String apiAbbrev;
	
	/* Holder for our last found Location state name */
	private String mLastStateName;

	/* Internal datamodel holding the most recent fuel data. */
	private FuelDataList mFuelData;

	/* String array containing our preferences */
	private String[] mRenewPrefs;

	/* View objects that need to be edited by Java.*/
	private ProgressBar mProgressBar; //TODO Change to windmill
	private TextView mPercentage;
	private Menu mMenu;
	private XYPlot mPlot;
	private LinearLayout mLinearHolder;

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
	
	/*FIXME this is a temporary thing*/
	private boolean setFrag;
	private boolean aboutFrag;
	private boolean deviceFrag;

	//TODO s:
	//widget resizing
	/* -------------- APP LIFECYCLE METHODS ------------------ */

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@SuppressWarnings("resource") //scanner is tossed after use.
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//FIXME
		setFrag = false;
		aboutFrag = false;
		deviceFrag = false;
		
		// Launch loading screen
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mProgressBar = (ProgressBar) findViewById(R.id.main_progressbar);
		mPlot = (XYPlot) findViewById(R.id.main_xyplot_main_main); // For some reason calling this main_xyplot results 
		// in an exception. What!? BLACK MAGIC IS HERE. TODO Remove magic.
		mPercentage = (TextView) findViewById(R.id.main_percentage);
		Typeface pacifico = Typeface.createFromAsset(getAssets(),"fonts/Pacifico.ttf"); 
		mPercentage.setTypeface(pacifico);
		mLinearHolder = (LinearLayout) findViewById(R.id.generated_content);
		mRes = getResources();
		mFragMan = getFragmentManager();

		//Set default preferences on first run
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
				if (!setFrag && !aboutFrag && !deviceFrag) { 
					mMenu.setGroupVisible(R.id.hide_when_drawer, true);
				}
			}

			/* Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				getActionBar().setTitle(mDrawerTitle);
				mMenu.setGroupVisible(R.id.hide_when_drawer, false);	
			}
			public void onDrawerSlide(View drawerView, float slideOffset) {
				super.onDrawerSlide(drawerView, slideOffset);
				mMenu.setGroupVisible(R.id.hide_when_drawer, false);
				//Trying to make the icons look.... right.
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
		Log.d(tag, "making a tempfile");
		Log.d(tag, file == null ? "null file!" : Long.toString(file.length()) + " size of file");

		//Load some constants about data age.
		long MAX_DATA_AGE = mRes.getInteger(R.integer.min_data_refresh);
		double MAX_DISTANCE = mRes.getInteger(R.integer.max_distance_change);

		// Check to see if we're recreating from a recent bundle
		if (savedInstanceState != null) {
			Log.d(tag, "trying to create from previous bundle");
			// If we are, then check to see if the location is fresh and close by
			long TIME_SAVED = savedInstanceState.getLong("time"); 
			long TIME_DELTA = System.currentTimeMillis() - TIME_SAVED;
			Location LOCATION_SAVED = (Location) savedInstanceState.getParcelable("location");
			float DISTANCE_DELTA = lastKnownLocation.distanceTo(LOCATION_SAVED);

			// Try and get the saved abbrev (save an api call)
			if (TIME_DELTA < MAX_DATA_AGE && DISTANCE_DELTA < MAX_DISTANCE) {
				Log.d(tag, "Getting apiabbrev from stored data.");
				String savedAb = savedInstanceState.getString("apiAbbrev");
				FuelDataList fuelData = (FuelDataList) savedInstanceState.getParcelable("data");
				if (savedAb != null && fuelData != null) { //Check to see if we got decent data from the bundle
					apiAbbrev = savedAb;
					mFuelData = fuelData;
					launchViews();
				} //otherwise try elsewhere.
			} 
		//Check to see if we can recreate from cached data
		//PrefRefreshed will be true if we just refreshed the data, so don't do it again
		} else if (file != null && file.length() > 0 && !prefRefreshed) {
			Log.d(tag, "The saved file exists, checking it.");
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
					Log.d(tag, "File was good to pull api abbrev from");
					String url = jSON.getJSONObject(0).getString("next");
					Log.d(tag, url);
					if (!url.equals("null") && url != null) { //Need to check and see if it pulled this correctly.
						//If it didn't it's a corrupt save file.
						//Furthermore check for the JSON null (Which is different from regular null)
						apiAbbrev = pullApiAbbrev(url);
						if (TIME_DELTA < MAX_DATA_AGE) {
							Log.d(tag, "File was good to pull old data from");
							doPostPercentGet(jSON, false, true);
						}
					}
				}
			} catch (JSONException e) {
				Log.e(tag, "Couldn't create from saved json");
				//This means our savefile was so corrupt it couldn't load, but it's
				//a nonfatal error, because it can reload the data from the server.
			} catch (FileNotFoundException e) {
				Log.wtf(tag, "Should never get here because it checks if the file's length is nonzero");
			}
		}
		//So somewhere we didn't pick up the correct data or it needs to be re downloaded

		if (apiAbbrev == null) {
			getAbbrev(lastKnownLocation);
		} else if (mFuelData == null) {
			//API abbrev was retrieved from the stored data but not the percent data.
			getPercentData();
		} else if (!internetConnected()) {
			//This means there's no network connection, so raise an error
			throwFatalNetworkError();
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}
	/*
	@Override
	protected void onResume() {
		super.onResume();
		if (apiAbbrev == null) {
			
		} else if (mFuelData == null || mFuelData.size() == 0) {
			
		}
	} */

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		Log.d(tag, "Saving Instance state");
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
		//Support devices with a menu button by opening the nav drawer
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
			Log.e(tag, "You tried to launch a graph without data");
			return false;
		} else {
			Log.d(tag, "Launching the graph");
			class GraphGet extends AsyncTask<Context, Void, Boolean> {
				@Override
				protected Boolean doInBackground(Context... context) {
					//This is all awful, I want to put this in XML ;_;
					Resources res = context[0].getResources();
					
					//Set title
					if (mLastStateName == null) {
						long startTime = System.currentTimeMillis();
						LocationManager locationManager = (LocationManager) context[0].getSystemService(Context.LOCATION_SERVICE);
						String locationProvider = LocationManager.NETWORK_PROVIDER;
						Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
						Geocoder geocoder = new Geocoder(context[0], Locale.getDefault());
						if (Geocoder.isPresent()) {
							List<Address> addresses;
							try {
								final int NUM_RESULTS = 1;
								addresses = geocoder.getFromLocation(lastKnownLocation.getLatitude(), 
										lastKnownLocation.getLongitude(), NUM_RESULTS);
								String state = addresses.get(0).getAdminArea();
								mLastStateName = state;
								//mPercentage.setText(String.format(res.getString(R.string.percentage_local_format), mLastStateName, mFuelData.getCurrentPercent(mRenewPrefs) * 100, Locale.US));
								Log.i(tag, "Geocode took " + (System.currentTimeMillis() - startTime) + "ms");
							} catch (IOException e) {
								Log.w(tag, e);
							}
						} else {
							Log.i(tag, "Geocode failed, going to default title");
						}
						String titleStem = res.getString(R.string.geolocated_graph_title);
						mPlot.setTitle(String.format(titleStem, mLastStateName, Locale.US));
						
						
					}
					
					// Set up the range behavior
					double max = mFuelData.getMax();
					final double MARGIN = 0.1; //TODO Externalize
					//TODO resize plot when refresh data is outside our current max.
				    mPlot.setRangeBoundaries(0, BoundaryMode.FIXED, max + MARGIN, BoundaryMode.FIXED);
					mPlot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 0.1);
					NumberFormat rangeFormat = NumberFormat.getPercentInstance();
					mPlot.setRangeValueFormat(rangeFormat);

					//Set up the domain behavior. 
					mPlot.setDomainValueFormat(new SimpleDateFormat("ha", Locale.US));
					final double MAGIC_PIXEL_VALUE = 150; //TODO make this work for multiple screen sizes and externalize.
					mPlot.setDomainStep(XYStepMode.INCREMENT_BY_PIXELS, MAGIC_PIXEL_VALUE);
					
					//Set up background color
					Paint p = new Paint();
					p.setColor(Color.TRANSPARENT); //TODO Externalize this..
					//mPlot.setBackgroundPaint(p);
					
					//Get the graph widget (Method not in javadocs, cool, right!?)
					XYGraphWidget mWid = mPlot.getGraphWidget();
					mWid.setRangeGridLinePaint(p);
					mWid.setDomainGridLinePaint(p);
					
					//This gets rid of the gray grid
					mWid.getGridBackgroundPaint().setColor(Color.TRANSPARENT);
					
					//This gets rid of the dark grey grid.
					mPlot.getGraphWidget().getBackgroundPaint().setColor(Color.TRANSPARENT);

					mPlot.setBorderStyle(Plot.BorderStyle.SQUARE, null, null);
					mPlot.getBorderPaint().setColor(Color.TRANSPARENT); //Should work, right?
					//mPlot.setBorderPaint(null);
					mPlot.getBackgroundPaint().setColor(Color.parseColor("#002C3C"));
					//mPlot.setPlotMargins(0, 0, 0, 0);
					
					//Try and thicken the axes
					mWid.getDomainOriginLinePaint().setStrokeWidth(4);
					mWid.getRangeOriginLinePaint().setStrokeWidth(4);
					
					//Set up the line format
					Paint lineFill = new Paint();
			        lineFill.setAlpha(200); //SO MANY MAGIC NUMBERS
			        //That third number below is the end position for gradient... such magic.
			        lineFill.setShader(new LinearGradient(0, 0, 0, 490, Color.argb(90, 0,120,0), Color.argb(180, 0,200,0), Shader.TileMode.MIRROR));
			        
					LineAndPointFormatter series1Format = new LineAndPointFormatter(
			                Color.rgb(0, 200, 0),                   // line color
			                null,                   				// point color
			                Color.rgb(0, 200, 0),                                   // fill color (none)
			                new PointLabelFormatter(Color.WHITE));
					series1Format.setFillPaint(lineFill);
					series1Format.setPointLabelFormatter(null);
					series1Format.getLinePaint().setStrokeWidth(10);
					series1Format.getLinePaint().setStrokeMiter(100); //Not sure if this does anything..
					//series1Format.configure(context[0], R.xml.line_point_formatter_with_plf1);
					//Add the data to the chart
					mPlot.addSeries(mFuelData.getLastDayPoints(), series1Format);
					return true;
				}
				@Override
				protected void onPostExecute(Boolean success) {
					if (success) {
						if (mLastStateName != null) {
							mPercentage.setText(String.format(getString(R.string.percentage_local_format), mLastStateName, mFuelData.getCurrentPercent(mRenewPrefs) * 100, Locale.US));
						} else {
							double d = mFuelData.getCurrentPercent(mRenewPrefs);
							String percentFormat = mRes.getString(R.string.percentage_format);
							String percent = String.format(Locale.US, percentFormat, d * 100);
							mPercentage.setText(percent);
						}
						mProgressBar.setVisibility(View.GONE);
						mPlot.setVisibility(View.VISIBLE);
						mLinearHolder.setVisibility(View.VISIBLE);
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
		setFrag = true;
		SettingsFragment test = (SettingsFragment) getFragmentManager().findFragmentByTag("settingsTag");
		if (test != null && test.isVisible()) {
			Log.d(tag, "Refused to open another settingsfragment");
			return false;
		}

		// Hide the progressbar/percentage indicators before launching the fragment.
		if (mProgressBar != null && mProgressBar.getVisibility() == View.VISIBLE) {
			mProgressBar.setVisibility(View.VISIBLE);
			Log.d(tag, "Changing visiblity of progressbar to invisible.");
		} else if (mPercentage != null && mPercentage.getVisibility() == View.VISIBLE) {
			mPercentage.setVisibility(View.INVISIBLE);
			mLinearHolder.setVisibility(View.INVISIBLE);
			Log.d(tag, "Changing visiblity of percentage to invisible");
		} 
		if (mPlot != null && mPlot.getVisibility() == View.VISIBLE) {
			Log.d(tag, "Changing visiblity of graph to invisible");
			mPlot.setVisibility(View.INVISIBLE);
			mLinearHolder.setVisibility(View.INVISIBLE);
		}
		mMenu.setGroupVisible(R.id.hide_when_drawer, false);
		mFragMan
		.beginTransaction()
		.replace(R.id.main_root, new SettingsFragment() {
			@Override 
			public void onDestroy() {
				setFrag = false;
				if (mProgressBar != null && mProgressBar.getVisibility() == View.INVISIBLE) {
					Log.d(tag, "Changing visiblity of progressbar back to visible.");
					mProgressBar.setVisibility(View.VISIBLE);
				} else if (mPercentage != null && mPercentage.getVisibility() == View.INVISIBLE) {
					Log.d(tag, "Changing visiblity of percentage back to visible");
					mPercentage.setVisibility(View.VISIBLE);
					mLinearHolder.setVisibility(View.VISIBLE);
				} 
				if (mPlot != null && mPlot.getVisibility() == View.INVISIBLE) {
					Log.d(tag, "Changing visiblity of graph back to visible");
					mPlot.setVisibility(View.VISIBLE);
					mLinearHolder.setVisibility(View.VISIBLE);
					if (preferencesChanged()) {
						refreshData(true);
					}
				}
				mMenu.setGroupVisible(R.id.hide_when_drawer, true);
				super.onDestroy();
			}
		}, "settingsTag")
		.addToBackStack(null)
		.commit();
		return true;
	}
	private boolean launchAboutFragment() {
		//Check to see if there's already an about fragment there.
		aboutFrag = true;
		Fragment test = (Fragment) getFragmentManager().findFragmentByTag("aboutTag");
		if (test != null && test.isVisible()) {
			Log.d(tag, "Refused to open another aboutfragment");
			return false;
		}

		// Hide the progressbar/percentage indicators before launching the fragment.
		if (mProgressBar != null && mProgressBar.getVisibility() == View.VISIBLE) {
			mProgressBar.setVisibility(View.INVISIBLE);
			Log.d(tag, "Changing visiblity of progressbar to invisible.");
		} else if (mPercentage != null && mPercentage.getVisibility() == View.VISIBLE) {
			mPercentage.setVisibility(View.INVISIBLE);
			mLinearHolder.setVisibility(View.INVISIBLE);
			Log.d(tag, "Changing visiblity of percentage to invisible");
		} 
		if (mPlot != null && mPlot.getVisibility() == View.VISIBLE) {
			Log.d(tag, "Changing visiblity of graph to invisible");
			mPlot.setVisibility(View.INVISIBLE);
			mLinearHolder.setVisibility(View.INVISIBLE);
		}
		mMenu.setGroupVisible(R.id.hide_when_drawer, false);
		mFragMan
		.beginTransaction()
		.replace(R.id.main_root, new AboutFragment() {
			@Override 
			public void onDestroy() {
				aboutFrag = false;
				if (mProgressBar != null && mProgressBar.getVisibility() == View.INVISIBLE) {
					Log.d(tag, "Changing visiblity of progressbar back to visible.");
					mProgressBar.setVisibility(View.VISIBLE);
				} else if (mPercentage != null && mPercentage.getVisibility() == View.INVISIBLE) {
					Log.d(tag, "Changing visiblity of percentage back to visible");
					mPercentage.setVisibility(View.VISIBLE);
					mLinearHolder.setVisibility(View.VISIBLE);
				} 
				if (mPlot != null && mPlot.getVisibility() == View.INVISIBLE) {
					Log.d(tag, "Changing visiblity of graph back to visible");
					mPlot.setVisibility(View.VISIBLE);
					mLinearHolder.setVisibility(View.VISIBLE);
				}
				mMenu.setGroupVisible(R.id.hide_when_drawer, true);
				super.onDestroy();
			}
		}, "aboutTag")
		.addToBackStack(null)
		.commit();
		return true;
		//TODO Manage the back stack a little bit better.
	}
	private boolean launchEmbeddedDevices() {
		//Check to see if there's already a settings fragment there.
		deviceFrag = true;
		Fragment test = (Fragment) getFragmentManager().findFragmentByTag("deviceTag");
		if (test != null && test.isVisible()) {
			Log.d(tag, "Refused to open another devicefragment");
			return false;
		}

		// Hide the progressbar/percentage indicators before launching the fragment.
		if (mProgressBar != null && mProgressBar.getVisibility() == View.VISIBLE) {
			mProgressBar.setVisibility(View.INVISIBLE);
			Log.d(tag, "Changing visiblity of progressbar to invisible.");
		} else if (mPercentage != null && mPercentage.getVisibility() == View.VISIBLE) {
			mPercentage.setVisibility(View.INVISIBLE);
			mLinearHolder.setVisibility(View.INVISIBLE);
			Log.d(tag, "Changing visiblity of percentage to invisible");
		} 
		if (mPlot != null && mPlot.getVisibility() == View.VISIBLE) {
			Log.d(tag, "Changing visiblity of graph to invisible");
			mPlot.setVisibility(View.INVISIBLE);
			mLinearHolder.setVisibility(View.INVISIBLE);
		}
		mMenu.setGroupVisible(R.id.hide_when_drawer, false);
		mFragMan
		.beginTransaction()
		.replace(R.id.main_root, new DeviceFragment() {
			@Override 
			public void onDestroy() {
				deviceFrag = false;
				if (mProgressBar != null && mProgressBar.getVisibility() == View.INVISIBLE) {
					Log.d(tag, "Changing visiblity of progressbar back to visible.");
					mProgressBar.setVisibility(View.VISIBLE);
				} else if (mPercentage != null && mPercentage.getVisibility() == View.INVISIBLE) {
					Log.d(tag, "Changing visiblity of percentage back to visible");
					mPercentage.setVisibility(View.VISIBLE);
					mLinearHolder.setVisibility(View.VISIBLE);
				} 
				if (mPlot != null && mPlot.getVisibility() == View.INVISIBLE) {
					Log.d(tag, "Changing visiblity of graph back to visible");
					mPlot.setVisibility(View.VISIBLE);
					mLinearHolder.setVisibility(View.VISIBLE);
				}
				mMenu.setGroupVisible(R.id.hide_when_drawer, true);
				super.onDestroy();
			}
		}, "deviceTag")
		.addToBackStack(null)
		.commit();
		return true;
		//TODO Manage the back stack a little bit better.
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
				Log.d(tag, "home");
				int entryCount = mFragMan.getBackStackEntryCount(); 
			    while (entryCount-- > 0) {
			        mFragMan.popBackStackImmediate();
			    }
				break;
			case 1:
				//EMBEDDED DEVICES
				Log.d(tag, "embedded dev");
				launchEmbeddedDevices();
				break;
			case 2:
				//SETTINGS
				Log.d(tag, "settings");
				launchSettingsFragment();
				break;
			case 3:
				//ABOUT
				Log.d(tag, "about");
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
			mMenu.setGroupVisible(R.id.hide_when_drawer, false);
			return false;
		} else {
			mMenu.setGroupVisible(R.id.hide_when_drawer, true);
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
			Log.w(tag, "Didn't have an iso for this location.");
			return null;
		}
		try {
			abbrev = jSON.getJSONObject(0).getString("abbrev");
		} catch (JSONException e) {
			Log.e(tag, "Error parsing Json (#2)");
			return null;
		}
		return abbrev;
	}

	private FuelDataList parseDataJSON(JSONArray jSON) {
		if (jSON == null) {
			return null;
		}
		mRenewPrefs = getRenewablePrefs();
		Log.d(tag, Arrays.toString(mRenewPrefs));
		try { //TODO check for length of json and add to existing list if it's short.
			return new FuelDataList(jSON.getJSONObject(0), mRenewPrefs);
		} catch (JSONException e) {
			Log.e(tag, "Couldn't parse something somewhere.");
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
				doPostPercentGet(jSON, true, true);
			}
		}.execute(makeFullDataUrl(apiAbbrev, internetQuality()));
	}

	private void getAbbrev(Location lastKnownLocation) {
		Log.d(tag, "Pulling the ApiAbbrev from the server");
		new APIGet() {
			@Override
			protected void onPostExecute(JSONArray jSON) {
				apiAbbrev = parseAbJSON(jSON);
				getPercentData();
			}
		}.execute(makeAbUrl(lastKnownLocation));
	}

	private boolean refreshData(boolean force) {
		if (!internetConnected()) {
			throwFatalNetworkError();
			return false;
		}
		Log.i(tag, "Refreshing my data");
		final boolean replaceData = force;
		if (apiAbbrev == null) {
			// Get the last known network (coarse) location
			LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
			String locationProvider = LocationManager.NETWORK_PROVIDER;
			Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
			getAbbrev(lastKnownLocation);
		} else {
			String url;
			if (!force && mFuelData != null) {
				//Add a new datapoint
				url = makeShortDataUrl(apiAbbrev, internetQuality(), mFuelData.getTimeUpdated());
			} else {
				//Full data refresh
				url = makeFullDataUrl(apiAbbrev, internetQuality());
				mPlot.clear();
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
					doPostPercentGet(jSON, true, replaceData); //TODO
				}
			}.execute(url);
		}
		return true;
	}

	private void doPostPercentGet(JSONArray jSON, boolean saveIt, boolean replaceData) {
		if (jSON == null) {
			throwFatalServerError();
			return;
		} else {
			if (mFuelData == null || replaceData) {
				mFuelData = parseDataJSON(jSON);
			} else {
				try {
					mFuelData.addPoints(jSON, mRenewPrefs);
				} catch (JSONException e) {
					throwFatalServerError();
					Log.e(tag, "Fatal server err parsing json");
				}
			}

			//We have to check if our data is actually there. (Weird edge case error)
			if (mFuelData.size() > 0) {
				Log.d(tag, "Launching views");
				launchViews();
			} else if (apiAbbrev == null || mFuelData.size() <= 0) {
				Log.e(tag, "Missing data, failed to load.");
				//This might mean it was midnight and your phone turned into a pumpkin...
				//TODO FIX midnight error
				throwFatalAppError();
			}
		}

		//Save our JSON to a file so we might not need to get it next time.
		if (saveIt) {
			File file;
			Log.d(tag, "Saving my JSON to a file.");

			// Get the last known network (coarse) location
			LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
			String locationProvider = LocationManager.NETWORK_PROVIDER;
			Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
			JSONObject loc = new JSONObject();
			try {
				loc.put("lat", lastKnownLocation.getLatitude());
				loc.put("lon", lastKnownLocation.getLongitude());
				jSON.put(1, loc);
			} catch (JSONException e1) {
				Log.w(tag, "Couldn't store latlon!");
			}
			try {
				file = new File(this.getCacheDir(), TEMPFILENAME);
				FileWriter fw = new FileWriter(file);
				fw.write(jSON.toString());
				fw.flush();
				fw.close();
			} catch (IOException e) {
				Log.w(tag, "Couldn't write my JSON to a file");
			}
		}
	}

	private void launchViews() {
		launchGraph();

		mPercentage.setVisibility(View.VISIBLE);
		//mLinearHolder.setVisibility(View.VISIBLE);
	}


	/* -------------------- URL FORMING METHODS ------------- */

	/**
	 * Utility function to take in a location object and return the properly formatted APIURL
	 * @see http://api.watttime.org/api/v1/docs/#!/balancing_authorities/Balancing_Authority_get_0
	 * API takes in location in LONGITUDE, LATITUDE.
	 */
	private String makeAbUrl(Location loc) {
		//Debug info
		Log.d(tag, Double.toString(loc.getLatitude()));
		Log.d(tag, Double.toString(loc.getLongitude()));

		//Form API Url
		double lon = loc.getLongitude();
		double lat = loc.getLatitude();
		final String abbrStem = mRes.getString(R.string.abbr_api_stem);
		String apiUrl = String.format(Locale.US, abbrStem, lon, lat);
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
		Log.d(tag, Integer.toString(internetQual));
		switch (internetQual) {
		case HIGH_QUALITY:
			Log.d(tag, "high");
			pageSize = "144"; //TODO FIX to 5m and 288.... API is too slow (20seconds as of now)
			freq = "10m";
			break;
		case MED_QUALITY:
			Log.d(tag, "med");
			pageSize = "144";
			freq = "10m";
			break;
		default:
			Log.d(tag, "low");
			pageSize = "24";
			freq = "1h";
			break;
		}
		return String.format(Locale.US, dataStem, start, end, pageSize, freq, balancingAuth);
	}
	/**
	 * Utility method to get the apiabbrev out of a data API url.
	 * @param url A properly formed data url, like the ones returned by makeDataUrl
	 * @return The api abbreviation from this data.
	 * @suppress magic
	 */
	private String pullApiAbbrev(String url) {
		Log.d(tag, url);
		return url.substring(61, (url.indexOf('&') == -1) ? url.length() : url.indexOf('&'));
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
					//Error ignored because of non boolean prefs, which we don't care about..
					Log.i(tag, "Cast error, pass.");
				}
			}
		}
		String[] renewables = new String[17];
		renewables = greens.toArray(renewables);
		return renewables;
	}

	private boolean preferencesChanged() {
		if (mRenewPrefs == null) {
			Log.d(tag, "Preferences did not change.");
			mRenewPrefs = getRenewablePrefs();
			return false;
		} else {
			String[] tRenewPrefs = getRenewablePrefs();
			if (!Arrays.deepEquals(mRenewPrefs, getRenewablePrefs())) {
				mRenewPrefs = tRenewPrefs;
				mFuelData = null;
				return true;
			} else {
				return false;
			}
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
			Log.d(tag, "High");
			return HIGH_QUALITY;
		case ConnectivityManager.TYPE_BLUETOOTH:
			Log.d(tag, "High");
			return HIGH_QUALITY;
		case ConnectivityManager.TYPE_MOBILE:
			if (networkInfo.isRoaming()) {
				Log.d(tag, "Low");
				return LOW_QUALITY;
			} else {
				Log.d(tag, "Med");
				return MED_QUALITY;
			}
		default:
			Log.d(tag, "Low");
			return LOW_QUALITY;
		}
	}

	private void throwFatalServerError() {
		Toast.makeText(this, R.string.server_error, Toast.LENGTH_SHORT).show();
		mProgressBar.setVisibility(View.INVISIBLE);
	}

	private void throwFatalNetworkError() {
		Toast.makeText(this, R.string.connectivity_error, Toast.LENGTH_SHORT).show(); 
		mProgressBar.setVisibility(View.INVISIBLE);
	}

	private void throwFatalAppError() {
		Toast.makeText(this, R.string.app_error, Toast.LENGTH_SHORT).show();
		mProgressBar.setVisibility(View.INVISIBLE);
	}

}
