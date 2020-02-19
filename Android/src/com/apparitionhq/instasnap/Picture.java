package com.apparitionhq.instasnap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.revmob.RevMob;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

//import android.util.Log;
import com.apphance.android.Log;
import com.apptentive.android.sdk.Apptentive;
import com.aviary.android.feather.Constants;
import com.aviary.android.feather.FeatherActivity;
import com.aviary.android.feather.library.filters.FilterLoaderFactory;
import com.flurry.android.FlurryAgent;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;

import de.guitarcollege.config.AviConfig;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

public class Picture extends Activity {
	public final int scaleHeight = 800;
	private static final int BROWSE_CODE = 1;
	private static final int CAMERA_CODE = 2;
	private static final int SENDPIC_CODE = 3;
	private static final int ACTION_REQUEST_FEATHER = 41;
	private SexPixApplication mSexPixApplication;
	private String chosenUser;
	
	public Uri fileUri;
	
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	
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
		Log.w("XMPPClient", "[Picture] System.gc()");
		System.gc();
	}

	public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getWindow().setBackgroundDrawable(getResources().getDrawable( R.drawable.bg_dark ));
    }


	@SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
    //	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);

        
        mSexPixApplication = (SexPixApplication) getApplication();
        Intent intent = getIntent(); 
        chosenUser = intent.getStringExtra("chosenUser");  
        
     // Starting RevMob session
        if (!mSexPixApplication.upgrade){
        	if (mSexPixApplication.revmob == null){
        		mSexPixApplication.revmob = RevMob.start(this, mSexPixApplication.REVMOB_APPLICATION_ID); 
        	}  
        	
        	
        	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext()); 
        	int countFsAd = preferences.getInt("ad_picture_sent_frequency", 1);         	
        	
        	if (countFsAd >= mSexPixApplication.ad_picture_sent_frequency){
        		mSexPixApplication.ad_picture_sent = mSexPixApplication.revmob.createFullscreen(this, "50ecab0e7076d32e03000001", null);
        	}        	
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_picture, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
	@SuppressLint("NewApi")
	public void useCamera(View view) {	
		File mediaStorageDir;		
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO){
			mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
		              Environment.DIRECTORY_DCIM).getAbsolutePath(), "Camera");
		} else {
			File rootsd = Environment.getExternalStorageDirectory();		    
		    mediaStorageDir = new File(rootsd.getAbsolutePath() + "/DCIM/Camera");	
		}
		
	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.w("XMPPClient", "[Picture - useCamera] failed to create directory for media:" + mediaStorageDir.getPath());
	        }
	    }
	    
	    
	 // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    File mediaFile;
	    mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg");

	    try {
	    	mediaFile.createNewFile();
	    } catch (IOException e) {
	    	Log.w("XMPPClient", "[Picture - useCamera] failed to create a mediafile: " + mediaFile.getPath());
	    }

	    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

	    fileUri = Uri.fromFile(mediaFile);
	    
	    
	    // save the uri
	    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext());
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("camera_output", mediaFile.getPath());
		editor.commit();
		
		
		
		// send GA event 				  						
		GoogleAnalytics myInstance = GoogleAnalytics.getInstance(this);
		Tracker myDefaultTracker = myInstance.getDefaultTracker();
		myDefaultTracker.sendEvent("ui_action", "button_press", "but_Picture_camera", null);
		 			
		
		

	    intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); 

	    startActivityForResult(intent, CAMERA_CODE);
		
	}
    

	@SuppressLint("NewApi")
	public void browseForPicture(View view) {				
		Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI);
		
		// send GA event 
		GoogleAnalytics myInstance = GoogleAnalytics.getInstance(this);
		Tracker myDefaultTracker = myInstance.getDefaultTracker();
		myDefaultTracker.sendEvent("ui_action", "button_press", "but_Picture_gallery", null);



			
		startActivityForResult(intent, BROWSE_CODE);	
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		
		
		Intent newIntent = new Intent( this, FeatherActivity.class );
		
		
		newIntent.putExtra( Constants.EXTRA_OUTPUT_FORMAT, Bitmap.CompressFormat.JPEG.name() );
		newIntent.putExtra( Constants.EXTRA_OUTPUT_QUALITY, 75 );
		
		
		newIntent.putExtra( Constants.EXTRA_EFFECTS_ENABLE_EXTERNAL_PACKS, false );
		newIntent.putExtra( Constants.EXTRA_FRAMES_ENABLE_EXTERNAL_PACKS, false );
		newIntent.putExtra( Constants.EXTRA_STICKERS_ENABLE_EXTERNAL_PACKS, false );
		
		
		if (android.os.Build.VERSION.SDK_INT > 7){
			newIntent.putExtra( "tools-list", new String[] { 
					FilterLoaderFactory.Filters.ADJUST.name(),
					FilterLoaderFactory.Filters.DRAWING.name(), 
					FilterLoaderFactory.Filters.TEXT.name(), 
			} );
		}
		else {
			newIntent.putExtra( "tools-list", new String[] { 
					FilterLoaderFactory.Filters.DRAWING.name(), 
					FilterLoaderFactory.Filters.TEXT.name(), 
			} );
		}
		

		
		
		if (!mSexPixApplication.upgrade){
			AviConfig.text_fill_colors = new int[]{0xff000000,0xffffffff,0xff00ff00,0x50ffffff};
			AviConfig.drawing_colors = new int[]{0xff000000,0xffffffff,0xff00ff00,0x50ffffff};
		} 
		else {
			AviConfig.text_fill_colors = new int[]{0xff000000,0xffffffff,0xffff0000,0xff00ff00,0xff0000ff,0x50ffffff};
			AviConfig.drawing_colors = new int[]{0xff000000,0xffffffff,0xffff0000,0xff00ff00,0xff0000ff,0x50ffffff};
		}
		
		
		
		
		
		newIntent.putExtra( Constants.EXTRA_HIDE_EXIT_UNSAVE_CONFIRMATION, true );
		newIntent.putExtra( Constants.EXTRA_TOOLS_DISABLE_VIBRATION, true );
			

		
		if (requestCode == BROWSE_CODE) {

			if (resultCode == RESULT_OK) {
				
				if (data == null || data.getData() == null){
					Log.e("XMPPClient", "[Picture - onActivityResult] intent data is null");
					return;
				}
	//			Uri targetUri = data.getData();		
				
				Log.i("XMPPClient", "[Picture] browse uri:" + data.getData().toString() + "; path:" + data.getData().getPath());
				
				newIntent.setData(data.getData());	
				
				
				// create file
				File outFile = new File(mSexPixApplication.getFilesDir().toString() + "/feather_out.jpg");	
				try {
					outFile.delete();
					outFile.createNewFile();
				} catch (IOException e) {
					Log.w("XMPPClient", "[Picture - onActivityResult] failed to create feather_out file");
				}
				
				newIntent.putExtra( Constants.EXTRA_OUTPUT, Uri.fromFile(outFile));					
				
				startActivityForResult( newIntent, ACTION_REQUEST_FEATHER );
				
							
//					Intent intent = new Intent(mSexPixApplication, SendPicture.class); 
//					intent.putExtra("chosenUser", chosenUser); 
//					startActivityForResult(intent, SENDPIC_CODE); 

			}
		}
		if (requestCode == CAMERA_CODE) {  
			if (resultCode == RESULT_OK) {
				
				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext()); 
		        String out_file = preferences.getString("camera_output", ""); 
		        
		        if (out_file.equals("")) {
		        	Log.e("XMPPClient", "[Picture - onActivityResult] output file path is null");
		        	return;
		        }
		        
		        
		        fileUri = Uri.fromFile(new File(out_file));

										
				Log.i("XMPPClient", "[Picture - onActivityResult] file from camera:" + fileUri.toString() + "; path:" + fileUri.getPath());
				
				newIntent.setData(fileUri);	
				
				
				// create file
				File outFile = new File(mSexPixApplication.getFilesDir().toString() + "/feather_out.jpg");	
				try {
					outFile.delete();
					outFile.createNewFile();
				} catch (IOException e) {
					Log.w("XMPPClient", "[Picture - onActivityResult] failed to create feather_out file");
				}
				
				newIntent.putExtra( Constants.EXTRA_OUTPUT, Uri.fromFile(outFile));
				
				
				startActivityForResult( newIntent, ACTION_REQUEST_FEATHER );
				
		
//					Intent intent = new Intent(mSexPixApplication, SendPicture.class); 	
//					intent.putExtra("chosenUser", chosenUser); 					
//				    startActivityForResult(intent, SENDPIC_CODE);

				
			} else {
				// try to delete created file

				File mediaFile;
				try{
					if (null != fileUri && null != fileUri.getPath()){
						mediaFile = new File(fileUri.getPath());
						if (fileUri.getPath().contains(".jpg")){
							mediaFile.delete();
						}
					}
				} catch (Exception e){
				}
			}
        }
		if (requestCode == SENDPIC_CODE){
			if (resultCode == RESULT_OK) {
				
				Intent intent = new Intent(mSexPixApplication, Home.class); 
				
				startActivity(intent);
				finish();
			}			
		}
		
		if (requestCode == ACTION_REQUEST_FEATHER){
			
			if (resultCode == RESULT_OK) {
				
				FlurryAgent.logEvent("Picture sent");			
				
				TaskSendPic sendPic = new TaskSendPic(mSexPixApplication, chosenUser);
				
				Intent intent = new Intent(mSexPixApplication, Home.class); 
				
				// send GA event 				  						
				GoogleAnalytics myInstance = GoogleAnalytics.getInstance(this);
				Tracker myDefaultTracker = myInstance.getDefaultTracker();
				myDefaultTracker.sendEvent("user_action", "picture_sent", "picture_sent", null);
				
				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext());  
				
				int picsSent = preferences.getInt("intPicsSent", 0);
				SharedPreferences.Editor editor = preferences.edit();
				editor.putInt("intPicsSent", picsSent+1);
				editor.commit();
				
				
				Apptentive.getRatingModule().setContext(mSexPixApplication);
				int apptentiveEvents = Apptentive.getRatingModule().getEvents();
				Apptentive.getRatingModule().setEvents(apptentiveEvents + 1);
				 			
				
				startActivity(intent);
				finish();
			}			
					
		}
	}
}


