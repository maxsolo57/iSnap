package com.apparitionhq.instasnap;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;
import net.hockeyapp.android.UpdateManager;

import com.flurry.android.FlurryAgent;
import com.google.analytics.tracking.android.EasyTracker;

import com.revmob.RevMob;

import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import com.apphance.android.Apphance;
//import android.util.Log;
import com.apphance.android.Log;

import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class SplashScreen extends Activity {
	
	public static final String APPHANCE_APP_KEY = "501d5dc06f7b76ce6269143a584560335fa8b06f";
	private SexPixApplication mSexPixApplication;
	
	protected boolean _active = true;
	protected int _splashTime = 2000; // time to display the splash screen in ms
	protected int _firstSplashTime = 6500; // time to display the splash screen in ms
	
	public int countUpgradeScreen = 1;
	public boolean firstStart = false;
	
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
	protected void onDestroy() {
		super.onDestroy();		

		unbindDrawables(findViewById(R.id.RootView));
		Log.w("XMPPClient", "[SplashScreen] System.gc()");
		System.gc();
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
 
        
        setContentView(R.layout.activity_splash_screen);
        
        mSexPixApplication = (SexPixApplication) getApplication(); 
        
        
        
        
        Intent intent = this.getIntent(); 
        if (null != intent){
        	Uri uri = intent.getData();
        	
        	EasyTracker.getInstance().setContext(this);
        	if (intent.getData() != null) {      
        		EasyTracker.getTracker().setCampaign(uri.getPath());    
        	}
 
        }
        
        
        Thread splashThread = new Thread() {

			@Override
            public void run() {		
			//	ProgressBar splashProgress = (ProgressBar) findViewById(R.id.splashProgressBar1);				
                try {                	
                    int waited = 0;
                    while(_active && (waited < _splashTime)) {
                        sleep(100);
                        if(_active) {
                            waited += 100;  
//                            if (waited > 5000 && splashProgress.getVisibility() == ProgressBar.INVISIBLE){
//                            	splashProgress.setVisibility(ProgressBar.VISIBLE);
//                            }
                        }
                        if (firstStart && !mSexPixApplication.upgrade && mSexPixApplication.fs.isAdLoaded()){
                        	_active = false;
                        }
                    }
                } catch(InterruptedException e) {
                    // do nothing
                } finally {
                	goAhead();               	               	
                }
            }
        };
        
        
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext()); 
    	
    	mSexPixApplication.strUserLogin = preferences.getString("strUserLogin", "");             
    	mSexPixApplication.strUserPassword = preferences.getString("strUserPassword", "");	
        
    	FlurryAgent.onStartSession(mSexPixApplication, mSexPixApplication.FLURRY_APP_KEY);
        // Starting RevMob session
        if (!mSexPixApplication.upgrade){
        	
        	
        	mSexPixApplication.adSettingsRequest = new AdSettingsRequest(mSexPixApplication);
        	mSexPixApplication.adSettingsRequest.run();

        	
        	        	
        						
	    	
	    	if (mSexPixApplication.strUserLogin.equals("")){
	    		firstStart = true;
	    		_splashTime = _firstSplashTime;	
	    	}
	    	
	    	mSexPixApplication.revmob = RevMob.start(this, mSexPixApplication.REVMOB_APPLICATION_ID); 
        	
	    	String placementID = "50d1fcdea721f11200000047";  // "Closed" PlacementID
	    	if (firstStart){
	    		placementID = "50e639fe04ef69120000006b";    // "After Registration screen" PlacementID
	    		mSexPixApplication.fs = mSexPixApplication.revmob.createFullscreen(this, placementID, null);
	    	//	placementID = "50a37981056c0bfa05000001";    // "Splash" PlacementID
	    	} 
	    	

        	int countFsAd = preferences.getInt("ad_app_closed_frequency", 1);  
        	if (countFsAd >= mSexPixApplication.ad_app_closed_frequency){
        		mSexPixApplication.fs = mSexPixApplication.revmob.createFullscreen(this, placementID, null);
        	}
	    	

        	
        	
        	
        	if (mSexPixApplication.ad_banner_bottom_provider.equals("revmob")){
        		mSexPixApplication.banner = mSexPixApplication.revmob.createBanner(this, "50d1fc8d50ccc31200000045");  // placement "Bottom" 
        	}
        	  
        	
      //  	mSexPixApplication.ad_picture_sent = mSexPixApplication.revmob.createFullscreen(this, "50ecab0e7076d32e03000001", null);
        	
        	
        	// TODO flurry ads
      //  	FlurryAgent.initializeAds(mSexPixApplication);
        	
        }

        
        splashThread.setName("splashThread");
        splashThread.start();
        
        
        
        
        
    }
    
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getWindow().setBackgroundDrawable(getResources().getDrawable( R.drawable.bg_dark ));
    }
    
    public void goAhead(){
    	
    	Intent oldInt = getIntent();
    	if (oldInt != null){
    		Bundle ext = getIntent().getExtras();
    		if (ext!=null){
    			if (getIntent().getExtras().getBoolean("deleted", false)){
    	    		finish();
    	    		return;
    	    	}
    		}
    	}
    	
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext()); 
    	mSexPixApplication.strUserLogin = preferences.getString("strUserLogin", "");             
    	mSexPixApplication.strUserPassword = preferences.getString("strUserPassword", "");	
    	countUpgradeScreen = preferences.getInt("upgrade_screen_frequency", 1); 
    	
    	
    	// skip upgrade screen for full version of the app
    	Intent intent = new Intent("com.apparitionhq.instasnap.Home");

    	if (!mSexPixApplication.upgrade){
    		SharedPreferences.Editor editor = preferences.edit();
    		if (countUpgradeScreen >= mSexPixApplication.upgrade_screen_frequency){
    			intent = new Intent("com.apparitionhq.instasnap.UpgradeScreen");
    			// reset counter    		   			
    			editor.putInt("upgrade_screen_frequency", 1);
    			editor.commit();
    		} else {
    			// inc counter    		
    			editor.putInt("upgrade_screen_frequency", countUpgradeScreen + 1);
    			editor.commit();
    		}
    		
    	}


    				

		if(mSexPixApplication.strUserLogin.equals("")){
			// no User Registered
			intent = new Intent("com.apparitionhq.instasnap.Registration");		
		}
    	   	
                  
        startActivity(intent);
        

//        // show RevMob ad if ready     
//        if (firstStart && !mSexPixApplication.upgrade){
//        	if (mSexPixApplication.fs.isAdLoaded()){
//        		mSexPixApplication.fs.show();  
//        		Log.i("XMPPClient", "[splashThread] showing RevMob full screen ad");	
//        	} 
//        }

        
        

        Log.i("XMPPClient", "[splashThread] Flurry started");

        finish();

    }
    
    
    @Override
	public void onResume() {
		super.onResume();
		checkForCrashes();
		
//		if (mSexPixApplication.isBeta){
//			checkForUpdates();
//		}
		
	}

	private void checkForCrashes() {
		CrashManager.register(mSexPixApplication, mSexPixApplication.HockeyApp_KEY,	new CrashManagerListener() {
			public Boolean onCrashesFound() {
				return true;
			}
			public Boolean ignoreDefaultHandler() {
			    return true;
			}

		});
	}
	
	
	

	private void checkForUpdates() {
		// Remove this for store builds!
		UpdateManager.register(this, mSexPixApplication.HockeyApp_KEY);
		Log.w("XMPPClient", "[SplashScreen] checking HA for updates");
	}


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_splash_screen, menu);
        return true;
    }
    
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && !firstStart) {
            _active = false;
        }
        return true;
    }
    
}
