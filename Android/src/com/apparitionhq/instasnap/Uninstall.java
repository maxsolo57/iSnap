package com.apparitionhq.instasnap;

import java.util.HashMap;
import java.util.Map;

import com.flurry.android.FlurryAgent;
import com.google.analytics.tracking.android.EasyTracker;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;
//import android.util.Log;
import com.apphance.android.Log;

import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class Uninstall extends Activity {
	private SexPixApplication mSexPixApplication;
	public Handler unHandler = new Handler();
	
	private Runnable finishRunnable = new Runnable() {
		@Override
		public void run() {			
			finish();					
		}
    	
    };
    
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
		setContentView(R.layout.activity_uninstall);
		
		mSexPixApplication = (SexPixApplication) getApplication();
		FlurryAgent.onStartSession(this, mSexPixApplication.FLURRY_APP_KEY);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_uninstall, menu);
		return true;
	}
	
	public void butProceed(View view) {
		
		RadioGroup radioGroup = (RadioGroup) findViewById(R.id.RadioGroup1);
		int checkedRadioButton = radioGroup.getCheckedRadioButtonId();

		switch (checkedRadioButton) {
			case R.id.radioButton1 : 
				Log.i("XMPPClient", "[Uninstall] reason 1");				
				break;
			case R.id.radioButton2 : 
				Log.i("XMPPClient", "[Uninstall] reason 2");
				break;
			case R.id.radioButton3 : 
				Log.i("XMPPClient", "[Uninstall] reason 3");
				break;
			case R.id.radioButton4 : 
				Log.i("XMPPClient", "[Uninstall] reason 4");
				break;
			default:
				Toast.makeText(this, R.string.str_choose_reason_uninstall, Toast.LENGTH_SHORT).show(); 	
				return;
		}

		try{
			String res = "" + ((RadioButton) findViewById(checkedRadioButton)).getText();

			Map<String, String> params = new HashMap<String, String>();
			params.put("Reason", res);	        		        	
			FlurryAgent.logEvent("App Uninstalled", params);
			FlurryAgent.onEndSession(this);
			
		} catch (Exception e){			
		}
		
		((Button) findViewById(R.id.but_uninstall)).setVisibility(Button.GONE);
		((ProgressBar) findViewById(R.id.bar_uninstall)).setVisibility(ProgressBar.VISIBLE);
		
		
		unHandler.postDelayed(finishRunnable, 3000);
			
	//	finish();		
	}
	
	
	// on Back button
    @Override 
    public boolean onKeyDown(int keyCode, KeyEvent event)  { 
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
        	
        	Intent localIntent = new Intent("android.intent.action.MAIN");
            localIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            localIntent.addCategory("android.intent.category.HOME");
            startActivity(localIntent);
            
        	finish();        	
        }
		return false;
    }

}
