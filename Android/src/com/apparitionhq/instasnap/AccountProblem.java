package com.apparitionhq.instasnap;

import com.apphance.android.Log;
import com.apptentive.android.sdk.Apptentive;
import com.apptentive.android.sdk.ApptentiveActivity;
import com.google.analytics.tracking.android.EasyTracker;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

public class AccountProblem extends ApptentiveActivity {
	private SexPixApplication mSexPixApplication;
	
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
	
	private void unbindDrawables(View view) {
        if (view.getBackground() != null) {
        view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
        ((ViewGroup) view).removeAllViews();
        }
    }
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

		unbindDrawables(findViewById(R.id.RootView));
		Log.w("XMPPClient", "[AccountProblem] System.gc()");
		System.gc();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_account_problem);
		
		mSexPixApplication = (SexPixApplication) getApplication(); 
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.account_problem, menu);
		return true;
	}
	
	public void butContact(View view) {
		Log.i("XMPPClient", "[AccountProblem] feedback");
		Apptentive.getFeedbackModule().forceShowFeedbackDialog(this, "AccountProblem", mSexPixApplication.strUserLogin);
	}

}
