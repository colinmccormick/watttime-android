/**
 * 
 */
package com.github.WattTime.watttime_android.DataModels;

import java.util.HashMap;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.Time;
import android.util.Log;


public class FuelDataPoint implements Parcelable {
	/*Time of creation for this specific datapoint */
	private final Time dataTime;
	/*Datastructure that allows us to map the name of the fuel to the value API returns */
	private HashMap<String, Double> dataPts;
	/* Double that allows us to keep track of the total amount of MW for this point */
	private double totalMW;
	/**
	 * Constructs a datapoint object according to UTC time.
	 * @param UTCTime the string formatted as returned from the WT server.
	 */
	public FuelDataPoint(String UTCTime) {
		dataTime = new Time();
		dataTime.parse3339(UTCTime);
		dataPts = new HashMap<String, Double>();
	}

	@SuppressWarnings("unchecked")
	public FuelDataPoint(Parcel in) {
		this(in.readString());
		Bundle bundle = in.readBundle();
		dataPts = (HashMap<String, Double>) bundle.getSerializable("map");
	}

	public void addPoint(String fuel, double value) {
		dataPts.put(fuel,value);
		totalMW += value;
		Log.d("FuelDataPoint", fuel + " " + Double.toString(value));
	}
	
	public double getPercentGreen(String[] prefs) {
		double accum = 0;
		for (String key : prefs) {
			if (dataPts.containsKey(key)) {
				double val = dataPts.get(key);
				accum += val;
			}
		}
		return accum / totalMW;
	}
	
	/* (non-Javadoc)
	 * @see android.os.Parcelable#describeContents()
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
		dest.writeString(dataTime.format3339(false));
		Bundle bundle = new Bundle();
		bundle.putSerializable("map", dataPts);
		dest.writeBundle(bundle);
	}
	
	public static final Parcelable.Creator<FuelDataPoint> CREATOR
	= new Parcelable.Creator<FuelDataPoint>() {
		public FuelDataPoint createFromParcel(Parcel in) {
			return new FuelDataPoint(in);
		}
		public FuelDataPoint[] newArray(int size) {
			return new FuelDataPoint[size];
		}
	};

}
