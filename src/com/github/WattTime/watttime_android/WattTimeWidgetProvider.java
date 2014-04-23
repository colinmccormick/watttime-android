package com.github.WattTime.watttime_android;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.github.WattTime.watttime_android.R;
import com.github.WattTime.watttime_android.Activities.MainActivity;

//TODO
// Make the widget only update when the phone is awake
public class WattTimeWidgetProvider extends AppWidgetProvider {
	  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
	        // Perform this loop procedure for each App Widget that belongs to this provider
	        for (int appWidgetId : appWidgetIds) {

	            // Create an Intent to launch ExampleActivity
	            Intent intent = new Intent(context, MainActivity.class);
	            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

	            // Get the layout for the App Widget and attach an on-click listener
	            // to the button
	            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
	            views.setOnClickPendingIntent(R.id.watttime_appwidget, pendingIntent);

	            // Tell the AppWidgetManager to perform an update on the current app widget
	            appWidgetManager.updateAppWidget(appWidgetId, views);
	        }
	    }
}
