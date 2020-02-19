package com.apparitionhq.instasnap;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.text.format.Time;

import com.android.vending.billing.IMarketBillingService;
import com.apparitionhq.instasnap.billing.BillingHelper;
import com.apparitionhq.instasnap.listeners.TRosterListener;

import de.guitarcollege.utils.Tools;
//import android.util.Log;
import com.apphance.android.Log;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;

public class BgService extends Service implements ServiceConnection {
	public final static String ACTION_NETWORK_CONNECT = "com.apparitionhq.instasnap.action.NETWORK_CONNECT";
	public final static String ACTION_NETWORK_DISCONNECT = "com.apparitionhq.instasnap.action.NETWORK_DISCONNECT";
	public final static String ACTION_REFRESH_CONTACTLIST = "com.apparitionhq.instasnap.action.REFRESH_CONTACTLIST";
	public final static String ACTION_WRONG_PASS = "com.apparitionhq.instasnap.action.WRONG_PASS";
	
	public static SexPixApplication mSexPixApplication;
	public XMPPConnection connection = null;
	public Handler bgHandler = new Handler();
	public ConnectTask connectTask = new ConnectTask();
	public DisconnectTask disconnectTask = new DisconnectTask();
	public int attempts = 0;
	public boolean passIsWrong = false;
	public int checkConnectionDelay = 60000 * 3; // 3 mins
	
	
	public IMarketBillingService mService;
	
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		Log.i("XMPPClient", "[BgService] Market Billing Service Connected.");
		mService = IMarketBillingService.Stub.asInterface(service);
		BillingHelper.instantiateHelper(getBaseContext(), mService);
		BillingHelper.setCompletedHandler(mTransactionHandler);
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext());
		int version = preferences.getInt("AppVersion", 0);
		
		PackageInfo pInfo;
		try {
			pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
			int curVersion = pInfo.versionCode;

			if (curVersion > version){
				// TODO first start after update/install

				if(BillingHelper.isBillingSupported()){
					Log.i("XMPPClient", "[BgService-Billing] Restoring transactions");		
					BillingHelper.restoreTransactions();
				}


				SharedPreferences.Editor editor = preferences.edit();				
				editor.putInt("AppVersion", curVersion);
				editor.commit();				
			}			
			
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}			
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		BillingHelper.setCompletedHandler(null);		
	}
	
	public static Handler mTransactionHandler = new Handler(){
		@SuppressLint("NewApi")
		public void handleMessage(android.os.Message msg) {
			
			
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext());
			SharedPreferences.Editor editor = preferences.edit();
			
			if (BillingHelper.latestPurchase != null){
				Log.i("XMPPClient", "[BgService-Billing] Transaction complete. Status:" + BillingHelper.latestPurchase.purchaseState + "   purchased item:" + BillingHelper.latestPurchase.productId);
				
				if(BillingHelper.latestPurchase.isPurchased()){
					mSexPixApplication.upgrade = true;
					
					boolean currentStateUpgraded = preferences.getBoolean(BillingHelper.latestPurchase.productId, false);

					if (!currentStateUpgraded){
						String upgradeButton = preferences.getString("str_upgradeButton", "noButton");
						int picsSent = preferences.getInt("intPicsSent", 0);
						long days = -1;

						if(android.os.Build.VERSION.SDK_INT > 9){
							try {
								long installed = mSexPixApplication.getPackageManager().getPackageInfo("com.apparitionhq.instasnap", 0).firstInstallTime;
								long nowTime = System.currentTimeMillis();
								days = (long)(nowTime - installed)/(1000*60*60*24);							
							} catch (Exception e) {
								e.printStackTrace();
							} 
						}

						// send GA feedback
						GoogleAnalytics myInstance = GoogleAnalytics.getInstance(mSexPixApplication.getApplicationContext());
						Tracker myDefaultTracker = myInstance.getDefaultTracker();

						myDefaultTracker.sendEvent("upgrade_status_change(completed)", upgradeButton, "" + picsSent + " pics", days);
					}				
					
					
					// Saving purchase									
					editor.putBoolean(BillingHelper.latestPurchase.productId, true);	
					
					
					

				}
				else {
					mSexPixApplication.upgrade = false;
				}
			}
			else {
				Log.i("XMPPClient", "[BgService-Billing] Transaction complete. Upgrade is not purchased");
				mSexPixApplication.upgrade = false;
				
				// Saving purchase								
				editor.putBoolean("upgrade", false);
			}
			
			editor.commit();
		};
	};
	
	
	final Thread bgThread = new Thread() { 
		
        @Override 
        public void run() { 
            try { 
            	Log.i("XMPPClient", "[BgService] thread started");
  
                bgRunnable.run(); 
            } finally { 
 
            } 
        } 
    }; 
    
    private Runnable bgRunnable = new Runnable() {
		@Override
		public void run() {			
			Time now = new Time();
			now.setToNow();			
			Log.i("XMPPClient", "[BgService] runnable: " + now.hour + ":" + now.minute + "." + now.second);						
		}
    	
    };
    
    private Runnable reconnectRunnable = new Runnable() {

		@Override
		public void run() {	
			if (connectTask.getStatus() != AsyncTask.Status.RUNNING){
				if (disconnectTask.getStatus() == AsyncTask.Status.RUNNING){
					disconnectTask.cancel(true);
				}
				Log.i("XMPPClient", "[reconnectRunnable] attempt #" + attempts);
				connectTask = new ConnectTask();
				connectTask.execute(""); 
			}				
		}    	
    };
    

    private Runnable checkConnection = new Runnable() {

		@Override
		public void run() {	
			Log.i("XMPPClient", "[BgService] checkConnection runnable started");	
			
			try{
				ConnectivityManager cm = (ConnectivityManager) mSexPixApplication.getSystemService(Context.CONNECTIVITY_SERVICE);
				if (cm == null) {
					return;
				}

				int connected = 0;

				for (NetworkInfo network : cm.getAllNetworkInfo()) {
					connected = connected + (network.isConnected()?1:0);
				}

				if (connected == 0){
					return;
				}

				if (mSexPixApplication.connection == null || !mSexPixApplication.connection.isConnected() || mSexPixApplication.connection.getRoster() == null){
					bgHandler.post(reconnectRunnable);
					Log.i("XMPPClient", "[BgService - checkConnection] reconnecting");
					return;
				}
				
			} catch (Exception e) {
			}

			bgHandler.postDelayed(checkConnection, checkConnectionDelay);			
		}    	
    };
    
    public int timeDelay() {
		if (attempts > 13) {
			return 300000;
		}
		if (attempts > 7) {
			return 60000;
		}
		return 15000;
	}

    

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		Log.i("XMPPClient", "[BgService] service created");		
		mSexPixApplication = (SexPixApplication) getApplication();
		
		if (mSexPixApplication == null){
			Log.i("XMPPClient", "[BgService] mSexPixApplication is null !!!");
		}
		else {
			mSexPixApplication.backgroundSerice = this;
			bgThread.start();
			
			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");		
			NetConnectReceiver ncReceiver = new NetConnectReceiver();		
			registerReceiver(ncReceiver, intentFilter);
			Log.i("XMPPClient", "[BgService] NetConnectReceiver is registered");	
			
			connection = mSexPixApplication.connection;
		}
		
		// billing stuff
		try {
			boolean bindResult = bindService(new Intent("com.android.vending.billing.MarketBillingService.BIND"), this, Context.BIND_AUTO_CREATE);
			if(bindResult){
				Log.i("XMPPClient","[BgService] Market Billing Service Successfully Bound");
			} else {
				Log.e("XMPPClient","[BgService] Market Billing Service could not be bound.");
			}
		} catch (SecurityException e){
			Log.e("XMPPClient","[BgService] Market Billing Service could not be bound. SecurityException: "+e);
		}		
	}
	
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		
		if (intent != null){
			if (intent.getAction() != null){
				
				if (intent.getAction().equals(BgService.ACTION_NETWORK_CONNECT)){					
					Log.i("XMPPClient", "[BgService] onStartCommand: CONNECT");	
					if (connectTask.getStatus() != AsyncTask.Status.RUNNING){
						if (disconnectTask.getStatus() == AsyncTask.Status.RUNNING){
							disconnectTask.cancel(true);
						}
						connectTask = new ConnectTask();
						connectTask.execute(""); 
					}					
				}
				
				if (intent.getAction().equals(BgService.ACTION_NETWORK_DISCONNECT)){
					
					Log.i("XMPPClient", "[BgService] onStartCommand: DISCONNECT");	
					if (disconnectTask.getStatus() != AsyncTask.Status.RUNNING){
						if (connectTask.getStatus() == AsyncTask.Status.RUNNING){
							connectTask.cancel(true);
						}
						disconnectTask = new DisconnectTask();
						disconnectTask.execute(""); 
					}
				}				
			}
		}			
		return START_STICKY;		
	}
	
	
	public class NetConnectReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	        if (cm == null) {
	            Log.e("XMPPClient", "[BgService] NetConnectReceiver: Connectivity Manager is null!");
	            return;
	        }
	        
	        int connected = 0;
	        
	        for (NetworkInfo network : cm.getAllNetworkInfo()) {
	        	connected = connected + (network.isConnected()?1:0);
	        }
	        
	        Log.i("XMPPClient", "[BgService - NetConnectReceiver] connected: " + connected);
	        
	        if (connected>0){
	        	Intent svcintent = new Intent(BgService.ACTION_NETWORK_CONNECT);          
	            context.startService(svcintent);	        	
	        } else if ((connected==0) && (connection!=null)){
	        	Intent svcintent = new Intent(BgService.ACTION_NETWORK_DISCONNECT);          
	            context.startService(svcintent);	        	
	        }
		}
	}
	
	
	public void getServerAddress(){
		try
		{

			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(mSexPixApplication.strServerLoadBalancer);

			HttpResponse response = httpClient.execute(httpPost);
			
			HttpEntity entity = response.getEntity();
			String fullAddress = EntityUtils.toString(entity);
			
			fullAddress.indexOf(":52");
			
			String address = fullAddress.substring(0, fullAddress.indexOf(":52"));
			
			String port = fullAddress.substring(fullAddress.indexOf(":52") + 1);
			
			if (port.length() != 4) { return;}
			
			SexPixApplication.strServerConnectAddress = address;
	//		SexPixApplication.strServerSearchAddress = "search." + address; 
			SexPixApplication.strServerPort = Integer.parseInt(port);
			
	//		Log.w("XMPPClient", "[BgService] ServerAddress: " + address + ";    port: " + port);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	
	}
	
	
	public String procedureConnect(){
		Log.i("XMPPClient", "[BgService-procedureConnect] config ");
		
		if (mSexPixApplication == null){
			Log.e("XMPPClient", "[BgService-procedureConnect] mSexPixApplication Is Null");
			return "ConnectionError";
		}
		
		if (mSexPixApplication.strUserLogin == null){
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext()); 
	        mSexPixApplication.strUserLogin = preferences.getString("strUserLogin", "");             
	        mSexPixApplication.strUserPassword = preferences.getString("strUserPassword", "");				
		}
		
		if(mSexPixApplication.strUserLogin.equals("")){
			return "noUserRegistered";
		}
		
		// Patching ProviderManager
		Tools.configureProviderManager(ProviderManager.getInstance());
		
		getServerAddress();
		
		// SSL off
		ConnectionConfiguration config = new ConnectionConfiguration(mSexPixApplication.strServerConnectAddress, mSexPixApplication.strServerPort, mSexPixApplication.strResource);
		config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
//		config.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
    	config.setReconnectionAllowed(false);
    	config.setCompressionEnabled(true);
//    	config.setSelfSignedCertificateEnabled(true);
//    	config.setVerifyChainEnabled(false);
    	
//    	config.setSocketFactory(new DummySSLSocketFactory());
//    	config.setSASLAuthenticationEnabled(true);
//    	config.setCompressionEnabled(false);
//    	config.setSASLAuthenticationEnabled(false);
		
		
//		ConnectionConfiguration config = new ConnectionConfiguration(mSexPixApplication.strServerAddress, 5223, mSexPixApplication.strResource);
//		config.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
//    	config.setReconnectionAllowed(false);
//    	config.setSocketFactory(new DummySSLSocketFactory());
//    	config.setCompressionEnabled(true);
    	config.setSASLAuthenticationEnabled(true);
    	mSexPixApplication.connection = new XMPPConnection(config);
		
		mSexPixApplication.backgroundSerice = this;
		
		connection = mSexPixApplication.connection;
		
		Log.i("XMPPClient", "[BgService-procedureConnect] connect attempt ");
		
		try {			
			TPacketListener.setPacketListener(mSexPixApplication);
			mSexPixApplication.connection.connect();			
			if (mSexPixApplication.connection.isConnected()) {		
				
				Log.i("XMPPClient", "[BgService-procedureConnect] Connected to "	+ connection.getHost());					
			} 
			else {
				Log.w("XMPPClient", "[BgService-procedureConnect] Can't connect to " + connection.getHost());
				return "ConnectionError";
			}	
			
			return "done";

			
		} catch (XMPPException ex) {
			Log.w("XMPPClient", "[BgService-procedureConnect] Failed to connect to " + connection.getHost());
			Log.w("XMPPClient", ex.toString());
			return "ConnectionError";
		}	

	}
	
	public String procedurePostLogin(){
		try{
			Log.i("XMPPClient", "[BgService-ConnectTask] Setting roster listener");
			mSexPixApplication.connection.getRoster().addRosterListener(new TRosterListener(mSexPixApplication));
			
			Log.i("XMPPClient", "[BgService-ConnectTask] Logged in as " + mSexPixApplication.connection.getUser());
			
			// Set the status to available
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext()); 
			String curStatus = preferences.getString("strStatusMessage", "Hey there! I am using InstaSnap");				
			mSexPixApplication.connection.sendPacket(new Presence(Presence.Type.available, curStatus, 0, Presence.Mode.available));				
			mSexPixApplication.currentPresence = Presence.Mode.available;
			
			mSexPixApplication.connection.getRoster().setSubscriptionMode(Roster.SubscriptionMode.manual);
			Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.accept_all);
			
			
			int rosterSize = preferences.getInt("roster_size", 0);
			if (mSexPixApplication.connection.getRoster().getEntryCount() > rosterSize){
				SharedPreferences.Editor editor = preferences.edit();
				editor.putInt("roster_size", mSexPixApplication.connection.getRoster().getEntryCount());
				editor.commit();
			}
			
			
			ServiceDiscoveryManager sDm = new ServiceDiscoveryManager(mSexPixApplication.connection); 
			
			
			
			bgHandler.postDelayed(checkConnection, checkConnectionDelay);
			
			Intent intent = new Intent(ACTION_REFRESH_CONTACTLIST);
			intent.putExtra("refreshList", true);
			LocalBroadcastManager.getInstance(mSexPixApplication.getApplicationContext()).sendBroadcast(intent);			
		} catch (Exception ex) {
			return "ConnectionError";
		}	
		return "done";
	}
	
	
	public class ConnectTask extends AsyncTask<String, String, String> {
		
		@Override
		protected String doInBackground(String... params) {	
			
			String result = procedureConnect();
			
			if(!result.equals("done")){
				return result;
			}
			
			
			try {			
				Log.i("XMPPClient", "[BgService-ConnectTask] trying to login as " + mSexPixApplication.strUserLogin + "@" + mSexPixApplication.strServerAddress + ":" + mSexPixApplication.strUserPassword);
				mSexPixApplication.connection.login(mSexPixApplication.strUserLogin + "@" + mSexPixApplication.strServerAddress, mSexPixApplication.strUserPassword, mSexPixApplication.strResource);
				
				Log.i("XMPPClient", "[BgService-ConnectTask] logged in. Status:" + mSexPixApplication.connection.isAuthenticated());
				
				if (mSexPixApplication.connection.isAuthenticated()) 
				{				
					// change password if needed
					
					TelephonyManager phoneManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);    	
		        	String phonesImei = phoneManager.getDeviceId();
		        	
		        	if (!phonesImei.equals(mSexPixApplication.strUserPassword)){
		        		
		        		// new device confirmed - changing the password
		        		org.jivesoftware.smack.AccountManager mAccount = new org.jivesoftware.smack.AccountManager (mSexPixApplication.connection); 
		        		try {
							mAccount.changePassword(phonesImei);
							mSexPixApplication.strUserPassword = phonesImei;
							Log.i("XMPPClient", "[BgService-LoginTask] password changed");
							
							// Saving Login and Password
							SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext());
							SharedPreferences.Editor editor = preferences.edit();
							if (mSexPixApplication.connection.isAuthenticated()) {
								editor.putString("strUserLogin", mSexPixApplication.strUserLogin);
								editor.putString("strUserPassword", mSexPixApplication.strUserPassword);
								editor.commit();
							}
							passIsWrong = false;
						} catch (XMPPException e) {
							Log.w("XMPPClient", "[BgService-LoginTask] can't change the password");
							e.printStackTrace();
							return "ConnectionError";
						}
		        	}
					
					result = procedurePostLogin();
					return result;	
				} 
				
			} catch (XMPPException ex) {
				Log.w("XMPPClient", "[BgService-LoginTask] Failed to connect to " + connection.getHost() + "  (XMPPException)");
				Log.w("XMPPClient", ex.toString());
				
				if (attempts < 10){
					return "ConnectionError";
				}
				
				return "LoginError";
			} catch (IllegalStateException ex) {
				Log.w("XMPPClient", "[BgService-LoginTask] Failed to connect to " + connection.getHost() + "  (IllegalStateException)");
				Log.w("XMPPClient", ex.toString());				
				return "ConnectionError";
			}		
			return "ConnectionError";
			
		}
		
		@Override
		protected void onPostExecute(String result) {
			Log.i("XMPPClient", "[BgService-ConnectTask-onPostExecute] result = " + result);
			
			if (result.equals("done")){		
				attempts = 0;				
			}
			else if (result.equals("ConnectionError")){		
				attempts = attempts + 1;
				bgHandler.postDelayed(reconnectRunnable, timeDelay());	
			}
			else if(result.equals("noUserRegistered")){
				Log.w("XMPPClient", "[BgService-ConnectTask-onPostExecute] Failed to login. No user registered in the app");
			}
			else if(result.equals("LoginError")){
				Log.w("XMPPClient", "[BgService-ConnectTask-onPostExecute] Failed to login. Wrong password");
				passIsWrong = true;
				Intent intent = new Intent(ACTION_WRONG_PASS);
				LocalBroadcastManager.getInstance(mSexPixApplication.getApplicationContext()).sendBroadcast(intent);	
			}			
		}		
	}
	
	
	
	public class DisconnectTask extends AsyncTask<String, String, String> {
		
		@Override
		protected String doInBackground(String... params) {	
			
//			if (mSexPixApplication == null){
//				Log.e("XMPPClient", "[LoginTask] mSexPixApplication Is Null");
//				return "mSexPixApplicationIsNull";
//			}
				
			Time now = new Time();
			now.setToNow();			
			Log.i("XMPPClient", "[BgService-DisconnectTask] attempt: " + now.hour + ":" + now.minute + "." + now.second);
			if (connection != null){
				if (mSexPixApplication != null){
					mSexPixApplication.connection = connection;
				}
				try {
					connection.disconnect();
				} catch (Exception e) {
					Log.w("XMPPClient", "[BgService-DisconnectTask] disconnect failed");
				}				
			}			
			now.setToNow();			
			Log.i("XMPPClient", "[BgService-DisconnectTask] disconnected: " + now.hour + ":" + now.minute + "." + now.second);

			return null;			
		}
		
	}

}
