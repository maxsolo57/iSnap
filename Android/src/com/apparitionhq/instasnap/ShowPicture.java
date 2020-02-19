package com.apparitionhq.instasnap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import de.guitarcollege.custom.dialog.CustomAlertDialog;
import de.guitarcollege.utils.ExtendedTouchImageView;
import de.guitarcollege.utils.Tools;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

//import android.util.Log;
import com.apphance.android.Log;

import com.apparitionhq.instasnap.billing.BillingHelper;
import com.flurry.android.FlurryAgent;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;
import com.revmob.RevMob;

import android.text.format.Time;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ShowPicture extends Activity {
	private SexPixApplication mSexPixApplication;	
	private String senderID;
	private String sender;
	
	public final int doNotShowLockScreen = 0;
	public final int showLockScreen = 1;
	private int runningMode = doNotShowLockScreen;
	public Message lastMsg = null;
	public boolean showConfirmation = true;
	public Handler sendFeedbackHandler = new Handler();
	
	private class sendFeedbackRunnable implements Runnable {
		Message msg;
		
		sendFeedbackRunnable(Message packet) { msg = packet; }
        public void run() {
        	Time now = new Time();
			now.setToNow();			
			Log.i("XMPPClient", "[sendFeedbackRunnable] attempt at:" + now.hour + ":" + now.minute + "." + now.second);
        	new sendFeedbackToSender().execute(msg);
        }
		
	}
	
	public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getWindow().setBackgroundDrawable(getResources().getDrawable( R.drawable.bg_dark ));
    }
	

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        
        // screenshot lock for Honeycomb and newer        
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB){
        	getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);        
        }
        
        setContentView(R.layout.activity_show_picture);
        
        mSexPixApplication = (SexPixApplication) getApplication(); 
        mSexPixApplication.mNotificationManager.cancelAll();
        
        FlurryAgent.onStartSession(mSexPixApplication, mSexPixApplication.FLURRY_APP_KEY);
        
        lastMsg = mSexPixApplication.getMessage(getIntent().getStringExtra("messageID"));
        
        if (lastMsg == null){
        	finish();
        	return;
        }
        
        showConfirmation = getIntent().getBooleanExtra("showConfiramtion", true);
        
        sender = (String) lastMsg.getProperty("nickName");
        
		senderID = StringUtils.parseBareAddress(lastMsg.getFrom());

        
        ((TextView) findViewById(R.id.senderName)).setText(sender);
        
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext()); 
        
        int picCounter = preferences.getInt("intPicCounter", 0);
		if (picCounter >= mSexPixApplication.lockedpic_screen_frequency && !mSexPixApplication.upgrade) {					
			runningMode = showLockScreen;				
		} else {
			runningMode = doNotShowLockScreen;		
		}		

        Log.i("XMPPClient", "[ShowPic] starting in mode:" + runningMode);
        
        // picture was seen, increase counter              
        picCounter ++;    
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt("intPicCounter", picCounter);
		editor.commit();
    }
    
    @Override
    protected void onStop() {
        super.onStop();  // Always call the superclass method first
        EasyTracker.getInstance().activityStop(this);
        mSexPixApplication.disableLock();
        finish();     
    }
    
    @Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this); 
	}


    
    
    @Override
    public void onPause() {
        super.onPause();
        mSexPixApplication.removeBanner(this);
        mSexPixApplication.disableLock();
        finish();
    }
   
	@Override 
    protected void onResume() { 
        super.onResume();  
        
        mSexPixApplication.setBanner(this);
    	
        
        String fName = (String) lastMsg.getProperty("filename");  
        
        ((ExtendedTouchImageView)findViewById(R.id.protectedImage)).setImageBitmap(BitmapFactory.decodeFile(mSexPixApplication.getFilesDir().toString() + "/" + fName + "_blur"));
		
        
        if (showConfirmation){
        	new CustomAlertDialog.Builder(this)				
    	    .setTitle(sender)
    	    .setMessage(mSexPixApplication.getResources().getString(R.string.str_open_pic))		    
    	    .setPositiveButton(mSexPixApplication.getResources().getString(R.string.str_yes), new DialogInterface.OnClickListener() {
    	        public void onClick(DialogInterface dialog, int whichButton) {
    	        	showPic();
    	        }
    	    }).setNegativeButton(mSexPixApplication.getResources().getString(R.string.str_not_now), new DialogInterface.OnClickListener() {
    	        public void onClick(DialogInterface dialog, int whichButton) {		
    	        	
    	        	finish();	        			          
    	        }
    	    }).show();
        	
        } else {
        	showPic();
        }       		     
    } 
	
	
	public void showPic(){
		
		final Activity cContext = this;
		
		switch(runningMode){
		
		case doNotShowLockScreen:
			showProtectedPicture();							
			break;
			
		case showLockScreen:
			
			if (mSexPixApplication.revmob == null){
				mSexPixApplication.revmob = RevMob.start(cContext, mSexPixApplication.REVMOB_APPLICATION_ID); 
			}

			mSexPixApplication.revmobLink = mSexPixApplication.revmob.createAdLink(cContext, "50d1fcc25263c41200000048", null);
			
			
//			TextView myMsg = new TextView(this);
//			myMsg.setText("Central blah blah tr wefgf bgrwe bgfeb bgbeb befgb bnfgd bfdgndfgnr sw bgrgn");
//			myMsg.setGravity(Gravity.CENTER_HORIZONTAL);
			  
			new CustomAlertDialog.Builder(this)				
		    .setTitle(mSexPixApplication.getResources().getString(R.string.str_unlock_full_is))
		    .setMessage(mSexPixApplication.getResources().getString(R.string.str_upgrade_message))	
//		    .setView(myMsg)
		    .setPositiveButton(mSexPixApplication.getResources().getString(R.string.str_upgrade), new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int whichButton) {
//		        	Intent intent = new Intent("com.apparitionhq.instasnap.UpgradeScreen"); 
//		        	intent.putExtra("sender", "ShowPicture");
//		        	startActivity(intent);
		        	
		        	
		        	if(BillingHelper.isBillingSupported()){
	    				Log.i("XMPPClient", "[ShowPicture] Billing is supported");
	    				
	    				BillingHelper.requestPurchase(cContext, "upgrade");  
	    				
	    			//	BillingHelper.requestPurchase(mContext, "android.test.purchased");  
	    			//	BillingHelper.requestPurchase(mContext, "android.test.canceled");
	    			//	BillingHelper.requestPurchase(mContext, "android.test.refunded");
	    			//	BillingHelper.requestPurchase(mContext, "android.test.item_unavailable");
	    				// android.test.purchased or android.test.canceled or android.test.refunded
	    				
	    				// send GA event
	    				GoogleAnalytics myInstance = GoogleAnalytics.getInstance(mSexPixApplication.getApplicationContext());
	    				Tracker myDefaultTracker = myInstance.getDefaultTracker();
	    				myDefaultTracker.sendEvent("ui_action", "button_press", "but_ShowPicture_Upgrade", null);
	    				
	    				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext());     		         
	    				SharedPreferences.Editor editor = preferences.edit();
	    				editor.putString("str_upgradeButton", "but_ShowPicture_Upgrade");
	    				editor.commit();
	    				
	    				
	    			} else {
	    				Toast.makeText(cContext, R.string.str_play_store_problem, Toast.LENGTH_LONG).show();		
	    			}
		        	
		        	// send Flurry log
		        	Map<String, String> params = new HashMap<String, String>();
		        	params.put("Action", "Upgrade");
		        	FlurryAgent.logEvent("LockedPic screen", params);

		        	
		        	finish();         
		        }
		    }).setNeutralButton(mSexPixApplication.getResources().getString(R.string.str_app), new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int whichButton) {
		        	
		        	if (mSexPixApplication.revmob == null){
		    			mSexPixApplication.revmob = RevMob.start(cContext, mSexPixApplication.REVMOB_APPLICATION_ID); 
		    		}
		    		
		    		mSexPixApplication.revmobLink.open();

		    		Toast.makeText(cContext, R.string.str_redirect_playstore, Toast.LENGTH_LONG).show();	
		    		
		        	// reset views counter     
		            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext()); 
		    		SharedPreferences.Editor editor = preferences.edit();
		    		editor.putInt("intPicCounter", 0);
		    		editor.commit();	
		    		
		    		// send Flurry log
		        	Map<String, String> params = new HashMap<String, String>();
		        	params.put("Action", "App");
		        	FlurryAgent.logEvent("LockedPic screen", params);
		    		
		    		finish();
		    		
		       // 	showProtectedPicture();	        			          
		        }
		    }).setOnCancelListener(new DialogInterface.OnCancelListener() {					
				@Override
				public void onCancel(DialogInterface dialog) {
					
					// send Flurry log
		        	Map<String, String> params = new HashMap<String, String>();
		        	params.put("Action", "Dismissed");
		        	FlurryAgent.logEvent("LockedPic screen", params);
		        	
					finish();						
				}
			})
			.setNegativeButton(mSexPixApplication.getResources().getString(R.string.str_later), new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					
					// reset views counter     
		            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext()); 
		    		SharedPreferences.Editor editor = preferences.edit();
		    		editor.putInt("intPicCounter", 0);
		    		editor.commit();
		    		
					showProtectedPicture();						
				}			
			})
			
			.show();
		    
			
			break;
		}      
		
	}
    
    
    
    
    public void showProtectedPicture(){  	
    	String fName = (String) lastMsg.getProperty("filename");
    	
    	
    	// send feedback to sender						
		Message iSPacket = new Message(StringUtils.parseBareAddress(lastMsg.getFrom()), Message.Type.normal);
		iSPacket.setPacketID("TransferStatus-" + Tools.randomString(8));
		iSPacket.setBody("_"+fName+"_watched");
		iSPacket.setProperty("filename", fName);
		iSPacket.setProperty("transferStatus", "watched");
    	new sendFeedbackToSender().execute(iSPacket); 
    	
    	sendFeedbackHandler.post(new sendFeedbackRunnable(iSPacket));
    	
    	

    	
    	Bitmap bitmap = null;
    	
		try {
			
			byte[] bitmapdata = Tools.readFile(mSexPixApplication.getFilesDir().toString() + "/" + fName);
			
			bitmapdata = Tools.decrypt(bitmapdata, StringUtils.parseName(lastMsg.getFrom()));	
			
			bitmap = BitmapFactory.decodeByteArray(bitmapdata , 0, bitmapdata .length);
			File file = new File(mSexPixApplication.getFilesDir().toString() + "/" + fName);
			file.delete(); 
	        file = new File(mSexPixApplication.getFilesDir().toString() + "/" + fName + "_blur");      
	        
	        file.delete(); 

	        ArrayList<Message> messages = mSexPixApplication.getMessages();
	        boolean res = messages.remove(lastMsg);

	        Log.i("XMPPClient", "[ShowPic] delete from messages:" + res);

	        mSexPixApplication.setMessages(messages);       
	        mSexPixApplication.enableLock();
	        
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.str_error, Toast.LENGTH_SHORT).show();
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.str_out_of_memory, Toast.LENGTH_SHORT).show();
		}
        
        if (bitmap != null){
        	((ExtendedTouchImageView)findViewById(R.id.protectedImage)).setImageBitmap(bitmap);
        } 
    }
    


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_show_picture, menu);
        return true;
    }
    
    @Override 
    public boolean onKeyDown(int keyCode, KeyEvent event)  { 
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {         	        
        	if (showConfirmation){
        		Intent intent = new Intent(this, Home.class); 			
    			startActivity(intent);
        	}       	 
			finish();
        }      
        return super.onKeyDown(keyCode, event); 
    }
    
    public void butReply(View view) {
    	if (mSexPixApplication.isConnected()){ 		    		
			Intent nextIntent = new Intent(view.getContext(), Picture.class);
			nextIntent.putExtra("chosenUser", senderID); 	    					        		
			startActivity(nextIntent);
			finish();		        			
		} else {
			Toast.makeText(view.getContext(), R.string.str_connectionError, Toast.LENGTH_SHORT).show();
		}    	
    } 
    
    
    private class sendFeedbackToSender extends AsyncTask<Object, Void, String> {

		@Override
		protected String doInBackground(Object... params) {
			Message msg = (Message) params[0];
			try {
    			mSexPixApplication.connection.sendPacket(msg);  
    			Log.i("XMPPClient", "[ShowPicture - sendFeedbackToSender] TransferStatus feedback sent to " + StringUtils.parseName(msg.getTo()));
    			
    			
    			// send friend request if necessary			
				if(mSexPixApplication.connection != null){
					if(mSexPixApplication.connection.getRoster() != null){
						RosterEntry entry = mSexPixApplication.connection.getRoster().getEntry(StringUtils.parseBareAddress(msg.getTo()));
						if (entry == null){
							 Presence reply = new Presence(Presence.Type.subscribed); 
							 reply.setTo(StringUtils.parseBareAddress(msg.getTo())); 
							 mSexPixApplication.connection.sendPacket(reply);
							 
							 Presence requestSubscribe = new Presence(Presence.Type.subscribe); 
							 requestSubscribe.setTo(StringUtils.parseBareAddress(msg.getTo())); 
							 mSexPixApplication.connection.sendPacket(requestSubscribe);	
						}
					}
				}
    			
    		}
    		catch (Exception e){
    			e.printStackTrace();
    			sendFeedbackHandler.postDelayed(new sendFeedbackRunnable(msg), 10000);
				return "no connection";    			
    		}
			return null;
		}
    	
    }
}
