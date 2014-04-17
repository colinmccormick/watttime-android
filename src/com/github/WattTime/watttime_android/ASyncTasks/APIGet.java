package com.github.WattTime.watttime_android.ASyncTasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

import com.github.WattTime.Keys;

public class APIGet extends AsyncTask<String, Void, JSONArray> {
	@Override
	protected JSONArray doInBackground(String... urlIn) {
		Log.d("APIConnection", "Trying to get API @ " + urlIn[0]);
		InputStream inStr = null;
		JSONArray jSON;

		/*Pull JSON from server*/
		StringBuilder sb = new StringBuilder();
		try {
			URL url = new URL(urlIn[0]);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			Log.d("APIConnection", "Opened a connection");
			conn.setReadTimeout(10000 /* milliseconds */);
			conn.setConnectTimeout(15000 /* milliseconds */);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Authorization", "Token " + Keys.API_KEY);
			conn.setDoInput(true);
			conn.connect();
			int response = conn.getResponseCode();
			Log.d("APIConnection", "The response is: " + response);
			inStr = conn.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inStr, "UTF-8"), 8);
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			jSON = new JSONArray(sb.toString());
		} catch (MalformedURLException e) {
			Log.e("GetAPIData", "Malformed URL: " + urlIn[0]);
			return null;
		} catch (JSONException e) {
			Log.w("GetAPIData", "Error parsing JSON array, trying object");
			try {
				JSONObject obj = new JSONObject(sb.toString());
				jSON = new JSONArray();
				jSON.put(obj);
			} catch (JSONException e1) {
				Log.e("GetAPIData", "JSONObject parsing failed, bad JSON from server.");
				//TODO Pop a toast here.
				return null;
			}

		} catch (IOException e) {
			Log.e("GetAPIData", "IOError");
			return null;
		} finally {
			try {
				if (inStr != null) {
					inStr.close();
				}
			} catch (IOException e) {
				Log.e("GetAPIData", "IOError");
				return null;
			}
		}
		return jSON;
	}
}
