/**
 * 
 */
package com.github.WattTime.watttime_android.DataModels;

import org.json.JSONArray;

import android.os.Parcel;
import android.os.Parcelable;


public class FuelDataList implements Parcelable {

	public FuelDataList(JSONArray inp) {
		//create point
	}

	public FuelDataList(Parcel in) {
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
	public static final Parcelable.Creator<FuelDataList> CREATOR
	= new Parcelable.Creator<FuelDataList>() {
		public FuelDataList createFromParcel(Parcel in) {
			return new FuelDataList(in);
		}
		public FuelDataList[] newArray(int size) {
			return new FuelDataList[size];
		}
	};

}
