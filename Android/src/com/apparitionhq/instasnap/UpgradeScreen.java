package com.apparitionhq.instasnap;

import com.apparitionhq.instasnap.billing.BillingHelper;
import com.revmob.RevMob;

import de.guitarcollege.custom.dialog.CustomAlertDialog;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
//import android.util.Log;
import com.apphance.android.Log;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;

import android.view.Menu;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

public class UpgradeScreen extends Activity {
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upgrade_screen);
		
		mSexPixApplication = (SexPixApplication) getApplication();  
		
		
	}
	
	public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getWindow().setBackgroundDrawable(getResources().getDrawable( R.drawable.bg_dark ));
    }
	
	@Override 
    protected void onResume() { 
        super.onResume();

        mSexPixApplication.setBanner(this);
    }
	
	@Override
    public void onPause() {
        super.onPause();
        mSexPixApplication.removeBanner(this);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_upgrade_screen, menu);
		return true;
	}

	public void butGoAhead(View view) {
		goToNextActivity();
	}
	
	public void goToNextActivity(){
		
		String sender = getIntent().getStringExtra("sender");
		if (sender != null && sender.equals("ShowPicture")){
			// do nothing
		} else {
			startActivity(new Intent("com.apparitionhq.instasnap.Home"));			
		}	
		finish();
	}
	
	public void butUpgrade(View view) {
		final Context mContext = this;
		
		CustomAlertDialog.Builder alert = new CustomAlertDialog.Builder(this);
		alert.setTitle(getResources().getString(R.string.str_upgrade));
		alert.setMessage(getResources().getString(R.string.str_open_play_store));
		alert.setPositiveButton(getResources().getString(R.string.str_verify), new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    		
    			if(BillingHelper.isBillingSupported()){
    				Log.i("XMPPClient", "[UpgradeScreen] Billing is supported");
    				
    				BillingHelper.requestPurchase(mContext, "upgrade");  
    				
    			//	BillingHelper.requestPurchase(mContext, "android.test.purchased");  
    			//	BillingHelper.requestPurchase(mContext, "android.test.canceled");
    			//	BillingHelper.requestPurchase(mContext, "android.test.refunded");
    			//	BillingHelper.requestPurchase(mContext, "android.test.item_unavailable");
    				// android.test.purchased or android.test.canceled or android.test.refunded
    				
    				// send GA event 	
    				GoogleAnalytics myInstance = GoogleAnalytics.getInstance(mSexPixApplication.getApplicationContext());
    				Tracker myDefaultTracker = myInstance.getDefaultTracker();
    				myDefaultTracker.sendEvent("ui_action", "button_press", "but_UpgradeScreen_Upgrade", null);
    				
    				
    				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext());     		         
    				SharedPreferences.Editor editor = preferences.edit();
    				editor.putString("str_upgradeButton", "but_UpgradeScreen_Upgrade");
    				editor.commit();
    				
    				 			
    	//			goToNextActivity();

    				
    			} else {
    				Toast.makeText(mContext, R.string.str_play_store_problem, Toast.LENGTH_LONG).show();		
    			}
    		}
		});
		alert.setNegativeButton(getResources().getString(R.string.str_cancel), new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
		        		
    		}
		});
		
		alert.show();

	}

}
