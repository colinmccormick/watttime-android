/**
 * 
 */
package com.github.WattTime.watttime_android.DataModels;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.Time;
import android.util.Log;

import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYSeries;


public class FuelDataList implements Parcelable {
	private ArrayList<FuelDataPoint> dataPoints;
	private String nextURLtoLoad;
	private Time lastUpdated;
	private XYSeries mXYSeries;
	private String[] mRenewablePreferences;

	public FuelDataList(JSONObject jSON, String[] renewables) throws JSONException {
		dataPoints = new ArrayList<FuelDataPoint>(12);
		mXYSeries = new SimpleXYSeries("TODOEXTERNME"); //TODO
		mRenewablePreferences = renewables;
		if (jSON != null && jSON.length() != 0) {
			if (jSON.has("next")) {
				nextURLtoLoad = jSON.getString("next");
			}
			if (jSON.has("results")) {
				JSONArray arr = jSON.getJSONArray("results");
				for(int k = 0; k < arr.length(); k += 1) {
					JSONObject obj = arr.getJSONObject(k);
					if (obj != null) {
						FuelDataPoint fDatPt = new FuelDataPoint(obj.getString("timestamp"));
						JSONArray genmix = obj.getJSONArray("genmix");
						for(int j = 0; j < genmix.length(); j += 1) {
							JSONObject pt = genmix.getJSONObject(j);
							fDatPt.addPoint(pt.getString("fuel"), pt.getDouble("gen_MW"));
						}
						dataPoints.add(dataPoints.size(), fDatPt);
						addToXYList(fDatPt);
					}
				}
			}
		}
		lastUpdated = dataPoints.size() > 0 ? dataPoints.get(0).getTimeCreated() : null;
	}
	/* ----------------- GETTER METHODS ---------------- */
	public String getNextURL() {
		return nextURLtoLoad;
	}

	public int size() {
		return dataPoints.size();
	}

	public Time getTimeUpdated() {
		return lastUpdated;
	}

	public double getCurrentPercent(String[] prefs) {
		if (prefs == null || prefs[0] == null || prefs[0].equals("null")) {
			Log.e("FuelDataList", "Bad settings passed in");
			return -1;
		} else {
			return dataPoints.get(0).getPercentGreen(prefs);
		}
	}
	
	public double getCurrentPercent() {
		return getCurrentPercent(mRenewablePreferences);
	}

	public XYSeries getLastDayPoints() {
		//TODO fix weird behavior at midnight!
		return mXYSeries;
	}
	/*---------------------- SETTER METHODS ----------------- */
	/**
	 * Method to take in the most recent data from the server and add it to the list
	 * @param jSON The JSON returned by a call to the server.
	 * Will not add any duplicate or old data to the list, that it already doesn't have.
	 */
	public void addPoint(JSONObject jSON) throws JSONException {
		if (jSON != null && jSON.length() != 0) {
			if (jSON.has("next")) {
				nextURLtoLoad = jSON.getString("next");
			}
			int addcount = 0;
			if (jSON.has("results")) {
				JSONArray arr = jSON.getJSONArray("results");
				for(int k = 0; k < arr.length(); k += 1) {
					JSONObject obj = arr.getJSONObject(k);
					if (obj != null) {
						Time newTime = new Time();
						newTime.parse3339(obj.getString("timestamp"));
						if (newTime.after(lastUpdated == null ? new Time() : lastUpdated)) {
							FuelDataPoint fDatPt = new FuelDataPoint(newTime);
							JSONArray genmix = obj.getJSONArray("genmix");
							for(int j = 0; j < genmix.length(); j += 1) {
								JSONObject pt = genmix.getJSONObject(j);
								fDatPt.addPoint(pt.getString("fuel"), pt.getDouble("gen_MW"));
							}
							dataPoints.add(addcount, fDatPt);
							addToXYList(fDatPt);
							addcount += 1;
						}
					}
				}
			}
		}
		lastUpdated = dataPoints.get(0).getTimeCreated();
	}

	private void addToXYList(FuelDataPoint fDatPt) {
		if (fDatPt == null) {
			return;
		} else {
			Time newTime = fDatPt.getTimeCreated();
			newTime.switchTimezone(Time.getCurrentTimezone());
			if (newTime.yearDay == (lastUpdated != null ? lastUpdated.yearDay : newTime.yearDay)) { //Must handle first case... TODO
				//If the datapoint we're adding was today, go ahead and put it in the XYseries
				long millis = newTime.toMillis(false);
				double percent = fDatPt.getPercentGreen(mRenewablePreferences) * 100;
				((SimpleXYSeries) mXYSeries).addLast(millis, percent);
			}
		}
	}
	public void changePrefs(String[] prefs) {
		mRenewablePreferences = prefs;
	}

	/* ------------------------ PARCELABLE METHODS ------------------- */
	public FuelDataList(Parcel in) throws JSONException {
		this(new JSONObject(), null); //Had to avoid ambiguous error. (New one to me!)
		for(FuelDataPoint point : (FuelDataPoint[]) in.readParcelableArray(null)) { //Uses default classloader CHECK?
			dataPoints.add(point);
		}
		nextURLtoLoad = in.readString();
		in.readStringArray(mRenewablePreferences);
	}

	/* 
	 * Black magic be here.
	 */
	@Override
	public int describeContents() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		FuelDataPoint[] dp = new FuelDataPoint[dataPoints.size()];
		dest.writeParcelableArray(dataPoints.toArray(dp), 0);
		dest.writeString(nextURLtoLoad);
		dest.writeStringArray(mRenewablePreferences);
	}
	
	public static final Parcelable.Creator<FuelDataList> CREATOR
	= new Parcelable.Creator<FuelDataList>() {
		public FuelDataList createFromParcel(Parcel in) {
			try {
				return new FuelDataList(in);
			} catch (JSONException e) {
				Log.wtf("FuelDataList", "oh my lord!");
				return null;
			}
		}
		public FuelDataList[] newArray(int size) {
			return new FuelDataList[size];
		}
	};

}
