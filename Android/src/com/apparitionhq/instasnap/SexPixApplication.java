package com.apparitionhq.instasnap;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

import com.revmob.RevMob;
import com.revmob.ads.banner.RevMobBanner;
import com.revmob.ads.fullscreen.RevMobFullscreen;
import com.revmob.ads.link.RevMobLink;
import com.urbanairship.AirshipConfigOptions;
import com.urbanairship.UAirship;
import com.urbanairship.push.PushManager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.apparitionhq.instasnap.listeners.UAshipReceiver;

import com.apphance.android.Apphance;
//import android.util.Log;
import com.apphance.android.Log;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;


/**
 * This class contains informations that needs to be global in the application.
 * Theses informations must be necessary for the activities and the service.
 */

public class SexPixApplication extends Application {
	
	public 			String 				strUserLogin;
	public 			String 				strUserPassword;
	public 			String 				strSystem_service = "system_service";
	public			boolean				isBeta = true;
	public			String				FLURRY_APP_KEY = "WMRZGWTDZ7F3S3V2RGWP";
	public static final String 			Apphance_APP_KEY = "501d5dc06f7b76ce6269143a584560335fa8b06f";
//	public static final String			HockeyApp_KEY = "c5687af42bbcfb37776c5c2e278e8904";   // live key
	public static final String			HockeyApp_KEY = "04fb231f34cbf970d2294307b91c64fb";   // beta key
	public static final String			AdMob_KEY = "a15134069357335"; 
//	public static final String			UpRise_AFFILIATE_ID = "4188"; 
//	public static final String			UpRise_SECRET_KEY = "OTztu2nYMoLfA-TmHGgS_smndhlGgLJsnko0-U0TvqE"; 
	
	public final 	String 				strServerAddress = "s15757996.onlinehome-server.com";
	public static 	String 				strServerConnectAddress = "v42785.1blu.de";
	public static 	String 				strServerSearchAddress = "search.s15757996.onlinehome-server.com";  
	public static 	int 				strServerPort = 5222;
	
//	public final 	String 				strServerAddress = "192.168.0.100";
//	public static 	String 				strServerConnectAddress = "192.168.0.100";
//	public final 	String 				strServerSearchAddress = "search.192.168.0.100"; 
//	public final 	String 				strServerLoadBalancer  = "http://192.168.0.100/loadbalancer.php";	
	
//	public final 	String 				strServerCreateAccount = "http://192.168.0.100/createaccount.php";	
	
	public final 	String 				strServerLoadBalancer  = "http://v42785.1blu.de/loadbalancer.php";	
	public final 	String 				strServerCreateAccount = "http://s15757996.onlinehome-server.com/createaccount.php";
	public final 	String 				strServerPutAddress  = "http://s423348072.onlinehome.us/instasnap/put.php";
	public final 	String 				strServerGetAddress  = "http://s423348072.onlinehome.us/instasnap/get.php";
	public final 	String 				strServerDelAddress  = "http://s423348072.onlinehome.us/instasnap/del.php";
	public final 	String 				strAdSettingsAddress = "http://s423348072.onlinehome.us/instasnap/adsettings.xml";  // live
//	public final 	String 				strAdSettingsAddress = "http://s423348072.onlinehome.us/instasnap/adsettings-test.xml";  // beta
	public final 	String 				strResource = "InstaSnapAndroid";
	
	public 			AdSettingsRequest	adSettingsRequest;
	public 			boolean				upgrade = false;
	// placeholder settings 
	public 			int 				ad_app_closed_frequency = 8;
	public			String				ad_app_closed_provider = "revmob";
	public			String				ad_banner_bottom_provider = "admob";
	public 			int 				ad_picture_sent_frequency = 2;
	public			String				ad_picture_sent_provider = "revmob";
	public			String				ad_registration_completed_provider = "revmob";
	public 			int 				lockedpic_screen_frequency = 7;
	public 			int 				upgrade_screen_frequency = 7;
	
	
	public 			XMPPConnection 		connection;
	public 			TFileTransferManager fTManager;
	public 			List<RosterEntry> 	mContacts;
	public 			Presence.Mode 		currentPresence = null;
	public 			Bitmap 				photo = null;
	public 			BgService 			backgroundSerice = null;
	public    		NotificationManager mNotificationManager = null;
	private 		boolean 			mIsAccountConfigured;
	public 			Handler 			logHandler = new Handler();
	
	// revmob, flurry ads
	public 			RevMob 				revmob;
	public 			RevMobFullscreen 	fs;
	public 			RevMobFullscreen 	ad_picture_sent;
	public			RevMobBanner 		banner;
	public  		String 				REVMOB_APPLICATION_ID = "50a16aaab2bdc40c00000004";
	public			RevMobLink 			revmobLink;
	public 			AdView 				adView;
	
//	public			FrameLayout			flurry_banner;
	
	// protected activity parameters  
	protected 		int 				lockDelay = 80;
	protected 		boolean 			lockEnabled = false;	
	private 		ActivityManager 	actvityManager;
	
	
//	final Thread logThread = new Thread() { 
//		@Override 
//        public void run() { 
//			try {
//				Runtime.getRuntime().exec("logcat -c");
//				Process process = Runtime.getRuntime().exec("logcat -s ActivityManager:i");
//				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//				
//				String line = "";
//				while ((line = bufferedReader.readLine()) != null) {
//					if (line.contains("android.intent.action.DELETE") && line.contains(SexPixApplication.this.getPackageName())){						
//						Log.i("XMPPClient", "[logThread] starting Uninstall activity");
//						logHandler.post(startUninstallActivity);
//					}					
//				}
//				
//			} catch (IOException e) {
//			}			
//		}		
//	}; 
	
//	public Runnable startUninstallActivity = new Runnable() {
//
//		@Override
//		public void run() {
//			Intent intent = new Intent(getBaseContext(), Uninstall.class);
//			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			startActivity(intent);			
//		}	
//	};
    
    
	@SuppressLint({ "NewApi", "NewApi" })
	private Runnable lockRunnable = new Runnable() {
    	
    	private List<ActivityManager.RunningAppProcessInfo> pList;
    	protected ActivityManager.RunningAppProcessInfo currentPr;
    	private int lSize;
    	
		@SuppressLint("NewApi")
		@SuppressWarnings("deprecation")
		public void run() {
			
			if (lockEnabled) {		
			//	Log.i("XMPPClient", "[logThread] runnable executed");
				
								
				pList = actvityManager.getRunningAppProcesses(); 		        
		        lSize = pList.size();			
				
		        for(int i=0;i<lSize;i++){
		        	boolean kill = false;
		        	currentPr = pList.get(i);        	
		        	
		        	// disabling default screenshooter        	
		        	if (currentPr.processName.contains("com.sec.android.app.screencapture")) {
		        		kill = true;	        			        			
		        	}    
		        	
		        	// disabling 3rd party screenshooters        	
		        	else if (currentPr.processName.contains("fahrbot.apps.screen") ||
		        			currentPr.processName.contains("com.liveov.shotux") ||
		        			currentPr.processName.contains("com.geeksoft.screenshot")){
		        		kill = true;
		        	}
		        	else if (currentPr.processName.contains("com.edwardkim.android.screenshotit") ||
		        			currentPr.processName.contains("com.androidscreenshotapptool") ||
		        			currentPr.processName.contains("jp.tomorrowkey.android.screencap")){
		        		kill = true;
		        	}
		        	else if (currentPr.processName.contains("com.edwardkim.android.screenshotit") ||
		        			currentPr.processName.contains("com.conditiondelta.screenshot") ||
		        			currentPr.processName.contains("com.ScreenCapture2") ||
		        			currentPr.processName.contains("com.eva.Screenshot")){
		        		kill = true;
		        	}
		        	else if (currentPr.processName.contains("net.tedstein.AndroSS") ||
		        			currentPr.processName.contains("com.designkontrol.screenshot") ||
		        			currentPr.processName.contains("mobi.mgeek.ScreenCut") ||
		        			currentPr.processName.contains("com.longxk.ascreenshot")){
		        		kill = true;
		        	}
		        	else if (currentPr.processName.contains("com.acr.screenshot") ||
		        			currentPr.processName.contains("com.baidu.screenshot") ||
		        			currentPr.processName.contains("com.colosseum.quickscreenshot") ||
		        			currentPr.processName.contains("com.icecoldapps.screenshot")){
		        		kill = true;
		        	}
		        	else if (currentPr.processName.contains("com.infoshore.Screens") ||
		        			currentPr.processName.contains("com.jaredco.screengrab") ||
		        			currentPr.processName.contains("com.mobikasa.screenshot") ||
		        			currentPr.processName.contains("com.mojo.moneyshot")){
		        		kill = true;
		        	}
		        	else if (currentPr.processName.contains("com.poofinc.screenshot") ||
		        			currentPr.processName.contains("com.protocol.x.shot") ||
		        			currentPr.processName.contains("com.rultech.screenshot") ||
		        			currentPr.processName.contains("com.ryan.screenshot")){
		        		kill = true;
		        	}
		        	else if (currentPr.processName.contains("com.sunfuturetechno.takescreen") ||
		        			currentPr.processName.contains("com.tools.screenshot") ||
		        			currentPr.processName.contains("net.srcz.android.screenshot") ||
		        			currentPr.processName.contains("rubberbigpepper.Screenshot")){
		        		kill = true;
		        	}
		        
		        	
		        	if (kill){
		        		if (froyoOrNewer()) {		        			
		        			Log.i("XMPPClient", "[logThread] process closed: " + currentPr.processName);
		        			actvityManager.killBackgroundProcesses(currentPr.processName);		        			
		        		} else {		        		
		        			Log.i("XMPPClient", "[logThread] process closed: " + currentPr.processName);
			        		actvityManager.restartPackage(currentPr.processName);		        			
		        		}

		        	}
		        	
		        	
		        }      		        
		        
				
				logHandler.postDelayed(this, lockDelay);
			}
		}
		
		public boolean froyoOrNewer() { 
		    if (android.os.Build.VERSION.RELEASE.startsWith("1.") || 
		        android.os.Build.VERSION.RELEASE.startsWith("2.0") || 
		        android.os.Build.VERSION.RELEASE.startsWith("2.1")) {
		    	Log.i("XMPPClient", "[froyoOrNewer] older than Froyo");
		    	return false;
		    }
		         	 
		    return true; 
		}
	};
	
	public void enableLock(){
		lockEnabled = true;
		logHandler.postDelayed(lockRunnable, lockDelay);		
		Log.i("XMPPClient", "[logThread] lock enabled");
	}
	
	public void disableLock(){
		lockEnabled = false;
		logHandler.removeCallbacks(lockRunnable);
		Log.i("XMPPClient", "[logThread] lock disabled");
	}
	
	public Message getMessage(String msgID){
		String filename = "messages.bin";
		FileInputStream fis;		
		
		ArrayList<HashMap<String,String>> mappedMessages = new ArrayList<HashMap<String,String>>();
		
		try{
			fis = openFileInput(filename); 
			ObjectInputStream is = new ObjectInputStream(fis); 
			mappedMessages = (ArrayList<HashMap<String,String>>) is.readObject(); 
			is.close(); 
			fis.close();
			
			for (HashMap<String,String> hm : mappedMessages) {
				if (hm.get("PacketID").equals(msgID)){
					Message msg = new Message(hm.get("to"), Message.Type.normal);
					msg.setPacketID(hm.get("PacketID"));	    	
					msg.setFrom(hm.get("from"));
		    		msg.setProperty("filename", hm.get("filename"));
		    		String nick = hm.get("nickName");
		    		if (nick == null){
		    			nick = "";
		    		}
		    		msg.setProperty("nickName", nick);
		    		msg.setProperty("size", Integer.parseInt(hm.get("size")));	
		    		msg.setProperty("date", Long.parseLong(hm.get("date")));
		    		return msg;				
				}
			}			
			
		} catch (FileNotFoundException e) {
			Log.w("XMPPClient", "[SexPixApp getMessage] config file does not exist.");			
		} catch (IOException e) {
			Log.w("XMPPClient", "[SexPixApp getMessage] IO exception");	
			e.printStackTrace();
		} catch (ClassNotFoundException e) {			
			e.printStackTrace();
			Log.w("XMPPClient", "[SexPixApp getMessage] Class Not Found exception");	
		}
		
		return null;		
	}
	
	public ArrayList<Message> getMessages(){
		String filename = "messages.bin";
		FileInputStream fis;
		ArrayList<Message> messages = new ArrayList<Message>();
		
		ArrayList<HashMap<String,String>> mappedMessages = new ArrayList<HashMap<String,String>>();
		
		try{
			fis = openFileInput(filename); 
			ObjectInputStream is = new ObjectInputStream(fis); 
			mappedMessages = (ArrayList<HashMap<String,String>>) is.readObject(); 
			is.close(); 
			fis.close();
			
			for (HashMap<String,String> hm : mappedMessages) {
				Message msg = new Message(hm.get("to"), Message.Type.normal);
				msg.setPacketID(hm.get("PacketID"));	    	
				msg.setFrom(hm.get("from"));
	    		msg.setProperty("filename", hm.get("filename"));
	    		String nick = hm.get("nickName");
	    		if (nick == null){
	    			nick = "";
	    		}
	    		msg.setProperty("nickName", nick);
	    		msg.setProperty("size", Integer.parseInt(hm.get("size")));	
	    		msg.setProperty("date", Long.parseLong(hm.get("date")));
	    		messages.add(msg);
			}			
			
		} catch (FileNotFoundException e) {
			Log.w("XMPPClient", "[SexPixApp getMessages] config file does not exist.");			
		} catch (IOException e) {
			Log.w("XMPPClient", "[SexPixApp getMessages] IO exception");	
			e.printStackTrace();
		} catch (ClassNotFoundException e) {			
			e.printStackTrace();
			Log.w("XMPPClient", "[SexPixApp getMessages] Class Not Found exception");	
		}
		
		return messages;		
	}
	
	public void setMessages(ArrayList<Message> messages){
		ArrayList<HashMap<String,String>> mappedMessages = new ArrayList<HashMap<String,String>>();
		
		for (Message msg : messages) {
			HashMap<String,String> hm = new HashMap<String,String>();
			hm.put("PacketID", msg.getPacketID());
			hm.put("filename", (String) msg.getProperty("filename"));
			hm.put("nickName", (String) msg.getProperty("nickName"));
			hm.put("size", ""+(Integer) msg.getProperty("size"));
			hm.put("date", "" + (Long) msg.getProperty("date"));
			hm.put("from", msg.getFrom());		
			hm.put("to", msg.getTo());
			
			mappedMessages.add(hm);	
		}		
		
		String filename = "messages.bin";
		FileOutputStream outputStream;		
		
		
		try {
			outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
			ObjectOutputStream os = new ObjectOutputStream(outputStream); 
			os.writeObject(mappedMessages); 
			os.close(); 
			outputStream.close();
			
		} catch (FileNotFoundException e) {
			Log.w("XMPPClient", "[SexPixApp setMessages] Class Not Found exception");	
			e.printStackTrace();
		} catch (IOException e) {
			Log.w("XMPPClient", "[SexPixApp setMessages] IO exception");
			e.printStackTrace();
		}
			
	}
	
        
    
    /**
     * Constructor.
     */
    public SexPixApplication() {
    }
    
	@Override
	public void onCreate() {
		super.onCreate();			
		
//		flurry_banner = new FrameLayout(this);
//		FrameLayout.LayoutParams lPar = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
//		lPar.gravity = Gravity.CENTER_HORIZONTAL;
//		flurry_banner.setLayoutParams(lPar);
		
//		logThread.setName("logThread");		
//		logThread.start(); 
		actvityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		fTManager = new TFileTransferManager(this);
		mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		
		// starting the background service			
		Intent myIntent = new Intent(getApplicationContext(), BgService.class);
		startService(myIntent);
		        
		
		// start urban airship
		AirshipConfigOptions options = AirshipConfigOptions.loadDefaultOptions(this);
		
		try{
			UAirship.takeOff(this, options);
		} catch (Exception e){
			Log.e("XMPPClient", "[SexPixApp onCreate] UAirship internal error");
		}
		
		PushManager.enablePush();
		PushManager.shared().setIntentReceiver(UAshipReceiver.class);
		Log.w("XMPPClient", "[SexPixApp UAShip] APid:" + PushManager.shared().getAPID());		
		
		
		
		
		// Check if upgraded		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()); 
		this.upgrade = preferences.getBoolean("upgrade", false);             

		
		if (isBeta){
			// Starts an Apphance session using a dummy key and QA mode
			Apphance.startNewSession(this, Apphance_APP_KEY, Apphance.Mode.Silent);
			//    Apphance.startNewSession(this, Apphance_APP_KEY, Apphance.Mode.QA);		
		}




	}
	
	public void setFlurryBanner(Activity act){
//		FrameLayout flurryBottom = (FrameLayout) act.findViewById(R.id.flurryBottom);
//		if (flurry_banner.getChildCount() < 1){
//			try {
//				FlurryAgent.initializeAds(this);
//				FlurryAgent.fetchAd(this, "InstaSnap_banner_bottom", this.flurry_banner, FlurryAdSize.BANNER_BOTTOM);
//				FlurryAgent.displayAd(this, "InstaSnap_banner_bottom", this.flurry_banner);	
//			} catch (Exception e){}								
//		}
//		flurryBottom.addView(flurry_banner);		
//		flurryBottom.setVisibility(FrameLayout.VISIBLE);
	}
	
	public void setBanner(Activity act){
		
		LinearLayout banner_layout = (LinearLayout) act.findViewById(R.id.revmob_banner);
        FrameLayout flurryBottom = (FrameLayout) act.findViewById(R.id.flurryBottom);
		
		if (upgrade){
			banner_layout.setVisibility(LinearLayout.GONE);
			flurryBottom.setVisibility(FrameLayout.GONE);
		} else {
			
			banner_layout.removeAllViews();
			
			if (ad_banner_bottom_provider.equals("flurry")){
				
//				mSexPixApplication.setFlurryBanner(this);
//				
//				banner_layout.setVisibility(LinearLayout.GONE);
				
			} 
			else if (ad_banner_bottom_provider.equals("revmob")){
				if (revmob == null){
					revmob = RevMob.start(act, REVMOB_APPLICATION_ID); 
				}

				if (banner == null){
					banner = revmob.createBanner(act, "50d1fc8d50ccc31200000045");
				}
				banner_layout.addView(banner);
				banner_layout.setVisibility(LinearLayout.VISIBLE);				
				flurryBottom.setVisibility(FrameLayout.GONE);
			}
			else if (ad_banner_bottom_provider.equals("admob")){				
				if (adView == null){				
					adView = new AdView(act, AdSize.BANNER, AdMob_KEY);					
				}
				if (!adView.isReady()){					
					adView.loadAd(new AdRequest());
				}
				banner_layout.addView(adView);
				banner_layout.setVisibility(LinearLayout.VISIBLE);				
				flurryBottom.setVisibility(FrameLayout.GONE);
			}
		}
	}
	
	public void removeBanner(Activity act){
        LinearLayout banner_layout = (LinearLayout) act.findViewById(R.id.revmob_banner);
		banner_layout.removeAllViews();
//		FrameLayout flurryBottom = (FrameLayout) findViewById(R.id.flurryBottom);
//		flurryBottom.removeAllViews();
		
	}

	
	
	/**
	 * Tell if SexPix is connected to a XMPP server.
	 * @return false if not connected.
	 */
	public boolean isConnected() {
		return connection.isConnected();
	}



	/**
	 * Tell if a XMPP account is configured.
	 * @return false if there is no account configured.
	 */
	public boolean isAccountConfigured() {
		return mIsAccountConfigured;
	}    
	
}
