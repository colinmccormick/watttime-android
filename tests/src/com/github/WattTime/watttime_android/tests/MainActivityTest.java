package com.github.WattTime.watttime_android.tests;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.androidplot.xy.XYPlot;
import com.github.WattTime.watttime_android.R;
import com.github.WattTime.watttime_android.Activities.MainActivity;

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
	
	private MainActivity mActivity;
	private RelativeLayout mRoot;
	private ProgressBar mProgressBar;
	private TextView mPercentage;
	private XYPlot mPlot;
	private LinearLayout mLinearHolder;
	
	/**
	 * JUnit constructor, allows us to define the activity named in
	 * the app's android manifest.
	 */
	public MainActivityTest() {
		super(MainActivity.class);
	}

	/**
	 * Set up method that runs before *every* test.
	 * Allows us to define an test env.
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		//Turns off touch mode on the device to allow us to pass in touch input
		setActivityInitialTouchMode(false);
		
		//This call calls the activity's onCreate()
		mActivity = getActivity();
		
		//These establish our layout objects
		mRoot = (RelativeLayout) mActivity.findViewById(R.id.main_root);
		mProgressBar = (ProgressBar) mActivity.findViewById(R.id.main_progressbar);
		mPercentage = (TextView) mActivity.findViewById(R.id.main_percentage);
		mPlot = (XYPlot) mActivity.findViewById(R.id.main_xyplot);
		mLinearHolder = (LinearLayout) mActivity.findViewById(R.id.generated_content);
	}
	
	@SmallTest
	public void testPreconditions() {
		//Checks to see that our UI objects were "successfully" created.
		//If any of these fail, our tests will be unreliable.
		assertNotNull(mActivity);
		assertNotNull(mRoot);
		assertNotNull(mProgressBar);
		assertNotNull(mPercentage);
		assertNotNull(mPlot);
		assertNotNull(mLinearHolder);
	}
	
}

