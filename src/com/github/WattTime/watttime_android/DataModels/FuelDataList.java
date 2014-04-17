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
import android.util.Log;


public class FuelDataList implements Parcelable {
	private ArrayList<FuelDataPoint> dataPoints;
	private String nextURLtoLoad;
	
	public FuelDataList(JSONObject jSON) throws JSONException {
		dataPoints = new ArrayList<FuelDataPoint>(12);
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
					}
				}
			}
		}
	}

	public String getNextURL() {
		return nextURLtoLoad;
	}
	//THIS IS A HACK TODO FIXME
	public double getCurrentPercent() {
		return getCurrentPercent(new String[] {"biomass", "wind", "refuse", "biogas", "nuclear", "hydro"}); //TODO make this from defaults file
	}
	
	public double getCurrentPercent(String[] prefs) {
		return dataPoints.get(0).getPercentGreen(prefs);
	}

	public FuelDataList(Parcel in) throws JSONException {
		this(new JSONObject()); //Had to avoid ambiguous error. (New one to me!)
		for(FuelDataPoint point : (FuelDataPoint[]) in.readParcelableArray(null)) { //Uses default classloader CHECK?
			dataPoints.add(point);
		}
		nextURLtoLoad = in.readString();
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
