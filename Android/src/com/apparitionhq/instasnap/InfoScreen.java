package com.apparitionhq.instasnap;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.XMPPException;

import com.apparitionhq.instasnap.billing.BillingHelper;
import com.apptentive.android.sdk.Apptentive;
import com.apptentive.android.sdk.module.survey.OnSurveyFetchedListener;
import de.guitarcollege.custom.dialog.CustomAlertDialog;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
//import android.util.Log;
import com.apphance.android.Log;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;

import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class InfoScreen extends Activity {
	private SexPixApplication mSexPixApplication;
	private static final int STATUS_REQUEST_CODE = 6;
	
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
		setContentView(R.layout.activity_info_screen);
		
		mSexPixApplication = (SexPixApplication) getApplication(); 
		
		PackageInfo pInfo;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			String cVersion = pInfo.versionName;
			
			((TextView) findViewById(R.id.txtVersion)).setText("Version " + cVersion);
			
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
//		FlurryAgent.initializeAds(this);		
	}
	
	public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getWindow().setBackgroundDrawable(getResources().getDrawable( R.drawable.bg_dark ));
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
		Log.w("XMPPClient", "[Picture] System.gc()");
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
        
        if (mSexPixApplication.upgrade){
        	(this.findViewById(R.id.butGoPro)).setVisibility(Button.GONE);
        	(this.findViewById(R.id.textView_goPro)).setVisibility(TextView.GONE);
        }
		
        mSexPixApplication.setBanner(this);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_info_screen, menu);
		return true;
	}
	
	public void butGoPro(View view) {		
	//	startActivity(new Intent(this, UpgradeScreen.class));	
		
		
		final Context mContext = this;
		
		CustomAlertDialog.Builder alert = new CustomAlertDialog.Builder(this);
		alert.setTitle(getResources().getString(R.string.str_upgrade));
		alert.setMessage(getResources().getString(R.string.str_open_play_store));
		alert.setPositiveButton(getResources().getString(R.string.str_verify), new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    		
    			if(BillingHelper.isBillingSupported()){
    				Log.i("XMPPClient", "[UpgradeScreen] Billing is supported");
    				
    				BillingHelper.requestPurchase(mContext, "upgrade");  
    				
    				// send GA event 	
    				GoogleAnalytics myInstance = GoogleAnalytics.getInstance(mSexPixApplication.getApplicationContext());
    				Tracker myDefaultTracker = myInstance.getDefaultTracker();
    				myDefaultTracker.sendEvent("ui_action", "button_press", "but_InfoScreen_GoPro", null);
    				
    				
    				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext());     		         
    				SharedPreferences.Editor editor = preferences.edit();
    				editor.putString("str_upgradeButton", "but_UpgradeScreen_Upgrade");
    				editor.commit();
    				
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

	public void butGiveLove(View view) {		
		startActivity(new Intent(this, GiveLove.class));		
	}
	
	public void butMakeISYours(View view) {		
		startActivity(new Intent(this, MakeIsYours.class));		
	}
	
	public void butIcons(View view) {		
		startActivity(new Intent(this, StatusIcons.class));		
	}
	
	public void butRestorePurchases(View view) {	
		// TODO restore
		if(BillingHelper.isBillingSupported()){
			Log.i("XMPPClient", "[BgService-Billing] Restoring transactions");		
			BillingHelper.restoreTransactions();
		}
		Toast.makeText(this, R.string.str_restore_purchases_progress, Toast.LENGTH_LONG).show();				
	}
	
	public void butFeedback(View view) {
		Log.i("XMPPClient", "[InfoScreen] feedback");
		Apptentive.getFeedbackModule().forceShowFeedbackDialog(this, "InfoScreen", mSexPixApplication.strUserLogin);
	}
	
	public void butTopApps(View view) {
//		int timeout = 5000;
//        FrameLayout container = new FrameLayout(this);
//        FlurryAgent.getAd(this, "InstaSnap_ad_top_apps", container, FlurryAdSize.FULLSCREEN, timeout);
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
    				myDefaultTracker.sendEvent("ui_action", "button_press", "but_InfoScreen_Upgrade", null);
    				
    				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext());     		         
    				SharedPreferences.Editor editor = preferences.edit();
    				editor.putString("str_upgradeButton", "but_InfoScreen_Upgrade");
    				editor.commit();

    				
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
	
	public void butSurvey(View view) {
		final Context cContext = this;
		Toast.makeText(this, R.string.str_req_survey, Toast.LENGTH_LONG).show();
		Apptentive.getSurveyModule().fetchSurvey(new OnSurveyFetchedListener() {
			public void onSurveyFetched(final boolean success) {
				Apptentive.getSurveyModule().show(cContext);
			}
		});

	}
	
	public void butSettings(View view) {
		Intent i = new Intent(mSexPixApplication.getApplicationContext(), SettingsScreen.class);
		startActivityForResult(i, STATUS_REQUEST_CODE);
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {		
		if (requestCode == STATUS_REQUEST_CODE) {
			if (resultCode == Activity.RESULT_OK) {
				if (data != null) {
					if (data.getExtras().getString("message").equals("exit")){
//						Toast.makeText(this, R.string.str_disconnected, Toast.LENGTH_SHORT).show();	
//						finish();
//						
//						mSexPixApplication.backgroundSerice.stopSelf();
//						
//						// stop UAship									
//						Intent s = new Intent(this, com.urbanairship.push.PushService.class);
//						stopService(s);
//
//
//
//						try {
//							if (mSexPixApplication.connection!=null){
//								mSexPixApplication.connection.disconnect();
//							}	
//						} catch (Exception e){
//							Log.w("XMPPClient", "[Home-onActivityResult] disconnect failed");
//						}
//						
//
//						android.os.Process.killProcess(android.os.Process.myPid());
						
						
						Intent i = new Intent();
					    i.putExtra("message", "exit");			   
						setResult(Activity.RESULT_OK, i);
						finish();
					}
					if (data.getExtras().getString("message").equals("delete")){

						Log.i("XMPPClient", "[Home-onActivityResult] deleting the account");

						if (mSexPixApplication.connection!=null){
							AccountManager mAccount = new AccountManager (mSexPixApplication.connection); 
							try {
								mAccount.deleteAccount();

								// Saving Login and Password
								SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext());
								SharedPreferences.Editor editor = preferences.edit();

								mSexPixApplication.strUserLogin = "";
								mSexPixApplication.strUserPassword = "";

								editor.putString("strUserLogin", mSexPixApplication.strUserLogin);
								editor.putString("strUserPassword", mSexPixApplication.strUserPassword);
								editor.commit();



								Intent svcintent = new Intent(BgService.ACTION_NETWORK_DISCONNECT);          
								startService(svcintent);
								Toast.makeText(this, R.string.str_account_deleted, Toast.LENGTH_SHORT).show();
								Intent intent = new Intent(this, SplashScreen.class);
								intent.putExtra("deleted", true);
								startActivity(intent);
								finish();

							} catch (XMPPException e) {
								Toast.makeText(this, R.string.str_connectionError, Toast.LENGTH_SHORT).show();
								Log.w("XMPPClient", "[Home-onActivityResult] unable to delete account");								
								e.printStackTrace();
							}	
							catch (IllegalStateException e) {
								Toast.makeText(this, R.string.str_connectionError, Toast.LENGTH_SHORT).show();
								Log.w("XMPPClient", "[Home-onActivityResult] unable to delete account");
								e.printStackTrace();
							}
						}						
					}
				}			
			}
		}
	}

}
