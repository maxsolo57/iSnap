package com.apparitionhq.instasnap;

import com.apphance.android.Log;
import com.apptentive.android.sdk.Apptentive;
import com.apptentive.android.sdk.ApptentiveActivity;
import com.apptentive.android.sdk.module.survey.OnSurveyFetchedListener;
import com.google.analytics.tracking.android.EasyTracker;

import android.os.Bundle;
import android.content.Context;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class MakeIsYours extends ApptentiveActivity {
	
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
		Log.w("XMPPClient", "[MakeIsYours] System.gc()");
		System.gc();
	}
	
	@Override
    public void onPause() {
        super.onPause();
        mSexPixApplication.removeBanner(this);
    }
	
	@Override 
    protected void onResume() { 
        super.onResume();
		
        mSexPixApplication.setBanner(this);
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_make_is_yours);
		
		mSexPixApplication = (SexPixApplication) getApplication(); 
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.make_is_yours, menu);
		return true;
	}
	
	public void butFeedback(View view) {
		Log.i("XMPPClient", "[MakeIsYours] feedback");

		Apptentive.getFeedbackModule().forceShowFeedbackDialog(this, "MakeISYours", mSexPixApplication.strUserLogin);
	}
	
	public void butSurvey(View view) {
		final Context cContext = this;
		Toast.makeText(this, R.string.str_req_survey, Toast.LENGTH_LONG).show();
		Apptentive.getSurveyModule().fetchSurvey(new OnSurveyFetchedListener() {
			public void onSurveyFetched(final boolean success) {
				Apptentive.getSurveyModule().show(cContext);
			}
		});
	}

}
