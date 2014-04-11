/**
 * 
 */
package com.github.WattTime.watttime_android.DataModels;

import android.os.Parcel;
import android.os.Parcelable;


public class FuelDataPoint implements Parcelable {

	public FuelDataPoint(int replace) {

	}

	public FuelDataPoint(Parcel in) {
		//inflate from parcel.
	}

	/* (non-Javadoc)
	 * @see android.os.Parcelable#describeContents()
	 */
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub

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
