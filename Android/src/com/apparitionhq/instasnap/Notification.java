package com.apparitionhq.instasnap;

import com.google.analytics.tracking.android.EasyTracker;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class Notification extends Activity {
	
	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this); 
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notification);
		
		((TextView) findViewById(R.id.txt_notification_alert)).setText(getIntent().getStringExtra("EXTRA_ALERT"));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_notification, menu);
		return true;
	}
	
	public void butOk(View view) {
		Intent launch = new Intent(Intent.ACTION_MAIN);
		launch.setClass(getApplicationContext(), SplashScreen.class);
		launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(launch);
		finish();
	}

}
