package com.apparitionhq.instasnap;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.jivesoftware.smack.XMPPException;

import com.flurry.android.FlurryAgent;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import de.guitarcollege.custom.dialog.CustomAlertDialog;
import de.guitarcollege.utils.Base64;
import de.guitarcollege.utils.FeedbackToEmail;
import de.guitarcollege.utils.HockeyReport;
import de.guitarcollege.utils.Tools;

import android.accounts.Account;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;

//import android.util.Log;
import com.apphance.android.Log;

import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class Registration extends Activity {
	private SexPixApplication mSexPixApplication;
	public static final int IMEI_REQUEST_CODE = 575;
	public static final int CODE_PROMPT_CODE = 587;
	final Handler mHandler = new Handler(); 
	public String senderPhoneNumber = "";
	public boolean whatsappExists = false;
	public String line1number = "";
	public String pNumber = "";
	public BroadcastReceiver smsSentReceiver;
	public String failureReason = "Verification failed";
	
	
	final Runnable mPhoneVerified = new Runnable() { 
        public void run() { 
//        	if (senderPhoneNumber.length()<3){
//        		senderPhoneNumber = mSexPixApplication.strUserLogin;
//        	}
        	
//    		mSexPixApplication.strUserLogin = senderPhoneNumber;
    		((TextView) findViewById(R.id.txtRegStatus)).setText("verified");
    		hideLayout();
    		createNewAccount();   	
        }
	};
	
	final Runnable mVerificationFailed = new Runnable() { 
        public void run() { 
        	showLayout();
    		
    		CustomAlertDialog.Builder verificationFailureAlert = new CustomAlertDialog.Builder(Registration.this);
    		verificationFailureAlert.setMessage(failureReason);
    		
    		verificationFailureAlert.setPositiveButton(R.string.str_retry, new DialogInterface.OnClickListener() { 			
        		public void onClick(DialogInterface dialog, int whichButton) {
        			mHandler.post(mPhoneVerified);
        		}       		
    		});
    		
    		
    		CustomAlertDialog.Builder smsNotReceivedAlert = new CustomAlertDialog.Builder(Registration.this);
    		smsNotReceivedAlert.setMessage(failureReason);
    		smsNotReceivedAlert.setPositiveButton(R.string.str_wait_more, new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int whichButton) {
        			Log.i("XMPPClient", "[Registration - mVerificationFailed] wait more");
        			mHandler.postDelayed(mVerificationFailed, 60000);
        			hideLayout();
        		}
    		});
    		smsNotReceivedAlert.setNegativeButton(R.string.str_faq, new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int whichButton) {
        			startActivity(new Intent(mSexPixApplication, FAQ.class));
        			HockeyReport.submitStackTraces(mSexPixApplication, "InstaSnap.VerificationFailed.smsNotReceived");
        		}
    		});
    		smsNotReceivedAlert.setOnCancelListener(new DialogInterface.OnCancelListener() {					
				@Override
				public void onCancel(DialogInterface dialog) {
					HockeyReport.submitStackTraces(mSexPixApplication, "InstaSnap.VerificationFailed.smsNotReceived");
				}
			});
    		
    		
    		if (failureReason.equals(getResources().getString(R.string.str_verification_failed))){
    			Log.i("XMPPClient", "[Registration - mVerificationFailed] sms not received");
    			FlurryAgent.logEvent("reg_smsNotReceived"); 
    			smsNotReceivedAlert.show();
    		} 
    		else{
    			try{
    				verificationFailureAlert.show();
    			} catch (Exception e){    				
    			}    			
    		//	HockeyReport.submitStackTraces(mSexPixApplication, "InstaSnap.VerificationFailed");
    		}   		  		
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
	
	
	public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getWindow().setBackgroundDrawable(getResources().getDrawable( R.drawable.bg_dark ));
    }
	
	
	
//	@Override
//	public void onDestroy() {
//		super.onDestroy();		
//		unregisterReceiver(mSmsReceiver);		
//		unregisterReceiver(smsSentReceiver);			
//	}
	
	@Override
	protected void onPause() {
		try{	
			unregisterReceiver(smsSentReceiver);
		} catch (Exception e){		
			e.printStackTrace();
		}
		
	
	    super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		
		// register SMS receiver
//        IntentFilter filter = new IntentFilter();
//        filter.setPriority(10000);
//        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
//        
//        mSmsReceiver = new SmsReceiver();        
//        registerReceiver(mSmsReceiver, filter);
        
        
        
        // register sent SMS receiver
        String SENT = "SMS_SENT";
        smsSentReceiver = new BroadcastReceiver(){
        	@Override
        	public void onReceive(Context arg0, Intent arg1) {
        		CustomAlertDialog.Builder smsFailureAlert = new CustomAlertDialog.Builder(Registration.this);
        		smsFailureAlert.setMessage(getResources().getString(R.string.str_no_sms_service));
        		smsFailureAlert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            		public void onClick(DialogInterface dialog, int whichButton) {
            		}
        		});
        		
        		GoogleAnalytics myInstance = GoogleAnalytics.getInstance(mSexPixApplication);
        		Tracker myDefaultTracker = myInstance.getDefaultTracker();
        		switch (getResultCode())
        		{
        			case Activity.RESULT_OK:
        				Toast.makeText(Registration.this, R.string.str_verification_sms_sent, Toast.LENGTH_SHORT).show();
        				Log.i("XMPPClient", "[smsSentReceiver] RESULT_OK");
        				break;
        				
        			case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
        				smsFailureAlert.setMessage(getResources().getString(R.string.str_unable_send_sms_nocredit));
        				smsFailureAlert.show();  
//        				showLayout();
//        				mHandler.removeCallbacks(mVerificationFailed);   
        				FlurryAgent.logEvent("reg_smsNotSent");
        				
        				// send GA event         				
        				myDefaultTracker.sendEvent("error", "reg_smsNotSent", "GENERIC_FAILURE", null);
        				
        				Log.w("XMPPClient", "[smsSentReceiver] RESULT_ERROR_GENERIC_FAILURE");
        				break;       		
        			
        			case SmsManager.RESULT_ERROR_NO_SERVICE:
        				smsFailureAlert.show();  
//        				showLayout();
//        				mHandler.removeCallbacks(mVerificationFailed);  
        				FlurryAgent.logEvent("reg_smsNotSent");
        				
        				// send GA event 
        				myDefaultTracker.sendEvent("error", "reg_smsNotSent", "NO_SERVICE", null);
        				
        				Log.w("XMPPClient", "[smsSentReceiver] RESULT_ERROR_NO_SERVICE");
        				break;
        				
        			case SmsManager.RESULT_ERROR_NULL_PDU:
        				smsFailureAlert.show();  
//        				showLayout();
//        				mHandler.removeCallbacks(mVerificationFailed);   
        				FlurryAgent.logEvent("reg_smsNotSent");
        				
        				// send GA event 
        				myDefaultTracker.sendEvent("error", "reg_smsNotSent", "NULL_PDU", null);
        				
        				Log.w("XMPPClient", "[smsSentReceiver] RESULT_ERROR_NULL_PDU");
        				break;

        			case SmsManager.RESULT_ERROR_RADIO_OFF:
        				smsFailureAlert.setMessage(getResources().getString(R.string.str_unable_send_sms));
        				smsFailureAlert.show();  
//        				showLayout();
//        				mHandler.removeCallbacks(mVerificationFailed);    
        				FlurryAgent.logEvent("reg_smsNotSent");
        				
        				// send GA event 
        				myDefaultTracker.sendEvent("error", "reg_smsNotSent", "RADIO_OFF", null);
        				
        				Log.w("XMPPClient", "[smsSentReceiver] RESULT_ERROR_RADIO_OFF");
        				break;       		
        		}
        	}
        };

        registerReceiver(smsSentReceiver, new IntentFilter(SENT));
	}

	
	
//	public void showAd() {
//		
//		// show RevMob ad if ready     
//		Log.w("XMPPClient", "[Registration] trying to show ad. Upgrade:" + mSexPixApplication.upgrade + "  ad loaded:" + mSexPixApplication.fs.isAdLoaded());	
//        
//		if (!mSexPixApplication.upgrade){
//			
//			if (mSexPixApplication.ad_registration_completed_provider.equals("flurry")){
////    			FrameLayout container = new FrameLayout(this);
////    			FlurryAgent.getAd(this, "InstaSnap_ad_registration_completed", container, FlurryAdSize.FULLSCREEN, 3000);
////    			Log.i("XMPPClient", "[Registration] showing Flurry full screen ad");
//
//    		} else if (mSexPixApplication.ad_registration_completed_provider.equals("revmob")){
//    			if (mSexPixApplication.fs != null && mSexPixApplication.fs.isAdLoaded()){
//    				mSexPixApplication.fs.show();   
//    				Log.i("XMPPClient", "[Registration] showing RevMob full screen ad");	
//    			}
//    		}       	 
//        }		
//	}
	
	public void showLayout(){
		LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayoutRegister);
		layout.setVisibility(LinearLayout.VISIBLE);
		((ProgressBar) findViewById(R.id.progressBarRegistration)).setVisibility(ProgressBar.INVISIBLE);
		((TextView) findViewById(R.id.txtRegStatus)).setVisibility(TextView.INVISIBLE);
	}
	
	public void hideLayout(){
		LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayoutRegister);
		layout.setVisibility(LinearLayout.INVISIBLE);
		((ProgressBar) findViewById(R.id.progressBarRegistration)).setVisibility(ProgressBar.VISIBLE);
		((TextView) findViewById(R.id.txtRegStatus)).setVisibility(TextView.VISIBLE);
	}
	
	public void setEditViews(){
		TelephonyManager phoneManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
		String simCountryIso = phoneManager.getSimCountryIso();
        String operatorName = phoneManager.getNetworkOperatorName();
        simCountryIso = simCountryIso.toUpperCase();
        line1number = phoneManager.getLine1Number();
        if (line1number == null){line1number = "";}
        if (line1number.equals("null")){line1number = "";}
        
        Log.i("XMPPClient", "[Registration-onCreate] line1number:" + line1number + "   SimCountryIso:" + simCountryIso);
        Log.i("XMPPClient", "[Registration-onCreate] Network Operator Name:" + operatorName);
        
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

        ((EditText) findViewById(R.id.etCode)).setText("" + phoneUtil.getCountryCodeForRegion(simCountryIso));
        PhoneNumber simNumberProto;
        if (line1number.length()>4){
        	try {
    			simNumberProto = phoneUtil.parse(line1number, simCountryIso);
    			
    			line1number = phoneUtil.format(simNumberProto, PhoneNumberFormat.E164).substring(("+" + phoneUtil.getCountryCodeForRegion(simCountryIso)).length());
    			
    			((EditText) findViewById(R.id.etPhone)).setText("" + line1number);
    			Log.i("XMPPClient", "[Registration-onCreate] number shown to user:" + line1number);
    		
    			
    			// register with a number from SIM

    			if (phoneUtil.isValidNumber(simNumberProto)){
    				String msgToUser = getResources().getString(R.string.txt_simNumberPrompt_1) 
    						+ phoneUtil.format(simNumberProto, PhoneNumberFormat.INTERNATIONAL) 
    						+ getResources().getString(R.string.txt_simNumberPrompt_2);
    				
    				final String realNumber = phoneUtil.format(simNumberProto, PhoneNumberFormat.E164);

    				CustomAlertDialog.Builder isItRightNumberAlert = new CustomAlertDialog.Builder(this);
    				isItRightNumberAlert.setMessage(msgToUser);
    				
    				isItRightNumberAlert.setPositiveButton(R.string.str_yup, new DialogInterface.OnClickListener() {
    					public void onClick(DialogInterface dialog, int whichButton) {
    						mSexPixApplication.strUserLogin = realNumber;    		        	
    		        		Log.i("XMPPClient", "[Registration-setEditViews] SIM number accepted: " + mSexPixApplication.strUserLogin);
    		            	hideLayout();
    		            	createNewAccount(); 
    		            	return; 
    					}
    				});

    				isItRightNumberAlert.setNegativeButton(R.string.str_nope, new DialogInterface.OnClickListener() {
    					public void onClick(DialogInterface dialog, int whichButton) {

    					}
    				});

    				isItRightNumberAlert.setOnCancelListener(new DialogInterface.OnCancelListener() {					
    					@Override
    					public void onCancel(DialogInterface dialog) {
    						finish();
    					}
    				});
    				
    				isItRightNumberAlert.show();

    			}
	
    			
    			
    		} catch (NumberParseException e) {
    			e.printStackTrace();
    		}
        }
        
        
        
		      
        showLayout();
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        
        mSexPixApplication = (SexPixApplication) getApplication(); 
        
        
        
        // start CodePrompt if code exists        
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()); 
		String pNumber = preferences.getString("pNumber", "");
		String encodedCode = preferences.getString("encodedCode", "");
		
		if (!pNumber.equals("") && !encodedCode.equals("")){
			Intent i = new Intent(mSexPixApplication, CodePrompt.class);
		    startActivityForResult(i, CODE_PROMPT_CODE);
		    return;
		}       
        
        
        
        
        TelephonyManager phoneManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        
        // this is a tablet?  
        if(phoneManager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE){
        	CustomAlertDialog.Builder alert = new CustomAlertDialog.Builder(Registration.this);
			alert.setMessage(getResources().getString(R.string.str_no_mobile_network));
			alert.setPositiveButton(getResources().getString(R.string.str_ok), new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int whichButton) {
	    			finish();	    			  				        		
	    		}
			});				
			alert.setOnCancelListener(new DialogInterface.OnCancelListener() {					
				@Override
				public void onCancel(DialogInterface dialog) {
					finish();
				}
			});
			alert.show();           
        }
        
        
        
        
        // check if whatsapp account exists
        Account[] arrayOfAccount = android.accounts.AccountManager.get(getApplicationContext()).getAccounts();
        

        for (Account localAccount : arrayOfAccount){
        	if(localAccount.type.equals("com.whatsapp")){      		
        		mSexPixApplication.strUserLogin = "+" + localAccount.name;
        		whatsappExists = true;
        		Log.i("XMPPClient", "[Registration-onCreate] whatsapp found:" + mSexPixApplication.strUserLogin);
        	}     	
        }
        
        // TODO whatsapp        
        if (whatsappExists){
        	hideLayout();
        	
        	
        	try{
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("encodedCode", Base64.encodeObject("none"));
                editor.putString("pNumber", "used WhatsApp");
                editor.commit();
        		
        	} catch (Exception e){
        		
        	}
        	
        	
        	createNewAccount(); 
        	return;        	
        }
        
        

        setEditViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_registration, menu);
        return true;
    }
    
    @Override 
    public boolean onKeyDown(int keyCode, KeyEvent event)  { 
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) { 
        	mSexPixApplication.backgroundSerice.stopSelf();
			if (mSexPixApplication.connection!=null){
				mSexPixApplication.connection.disconnect();
			}	
	//		showAd();
			
			Map<String, String> params = new HashMap<String, String>();
			params.put("Attempted", "No");      		        	
			FlurryAgent.logEvent("Registation attempted", params);
			
//			mHandler.removeCallbacks(mVerificationFailed);
					
			try{
//				unregisterReceiver(mSmsReceiver);		
				unregisterReceiver(smsSentReceiver);
			} catch (Exception e){	
				e.printStackTrace();
			}
			
			finish();
			
		//	android.os.Process.killProcess(android.os.Process.myPid());
        }      
        return super.onKeyDown(keyCode, event); 
    }
    
    
    public void butVerify(View view) {
    	if (((EditText) findViewById(R.id.etPhone)).getText().toString().length() < 4) {
    		
    		CustomAlertDialog.Builder alert = new CustomAlertDialog.Builder(this);
    		alert.setMessage(getResources().getString(R.string.str_enter_phone_num));
    		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int whichButton) {

        		}
    		});
    		
    		alert.show();
    		
    		return;
    	}
    	
    	pNumber = "+" + ((EditText) findViewById(R.id.etCode)).getText().toString() + ((EditText) findViewById(R.id.etPhone)).getText().toString();
    	
    	if (pNumber.length() < 7){
    		
    		CustomAlertDialog.Builder alert = new CustomAlertDialog.Builder(this);
    		alert.setMessage(getResources().getString(R.string.str_short_number));
    		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int whichButton) {

        		}
    		});
    		
    		alert.show();
    		 				       
			return;
    	}
    	
    	// check if online
    	final ConnectivityManager conMgr =  (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
    	if (activeNetwork != null && activeNetwork.isConnected()) {
    	} else {
    		Toast.makeText(this, R.string.str_no_internet_error, Toast.LENGTH_LONG).show();    	    
    	}
    	
    	
    	
    	CustomAlertDialog.Builder alert = new CustomAlertDialog.Builder(this);
		alert.setMessage(getResources().getString(R.string.txt_will_be_verifying) + pNumber + getResources().getString(R.string.txt_is_this_ok));
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    			verification(); 
    			
    			Map<String, String> params = new HashMap<String, String>();
				params.put("Attempted", "Yes");      		        	
				FlurryAgent.logEvent("Registation attempted", params);
    		}
		});
		alert.setNegativeButton("Edit", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
		        		
    		}
		});
		
		alert.show();
	
	}
    
    public void verification(){
    	Log.i("XMPPClient", "[Registration-verification] started");
    	
    	
		
	//	pNumber = "+" + ((EditText) findViewById(R.id.etCode)).getText().toString() + ((EditText) findViewById(R.id.etPhone)).getText().toString();
		mSexPixApplication.strUserLogin = pNumber;							
		
		CustomAlertDialog.Builder smsFailureAlert = new CustomAlertDialog.Builder(this);
		smsFailureAlert.setMessage(getResources().getString(R.string.str_no_sms_service));
		smsFailureAlert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    		}
		});
		
		// send sms		
		Log.i("XMPPClient", "[Registration-verification] sending a verification sms to:" + pNumber);
	//	Log.i("XMPPClient", "[Registration-verification] sending a verification sms to:" + pNumber + "  code:" + verificationCode);
    	SmsManager sms = SmsManager.getDefault();
    	
    	if (sms == null){
    		smsFailureAlert.show();   		
    		return;
    	}
    	
        
//    	String verificationCode = Tools.randomNumString(4);
    	
    	String verificationCode = FeedbackToEmail.getISactivationCode2(mSexPixApplication.getApplicationContext());
    	String encodedCode = "";
        try {       	
        	encodedCode = Base64.encodeObject(verificationCode);
        	
        	// TODO show the code
   //     	Log.i("XMPPClient", "[Registration] encoded code: '" + encodedCode + "'   code:" + verificationCode);	
			Log.i("XMPPClient", "[Registration] encoded code: '" + encodedCode + "'");		
		} catch (Exception e) {	
			e.printStackTrace();
			verificationCode = "5896";		
			Log.e("XMPPClient", "[Registration] error while encodeObject.  code:" + verificationCode);	
			encodedCode = "rO0ABXQABDU4OTY=";
		} 
        
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("encodedCode", encodedCode);
        editor.putString("pNumber", pNumber);
        editor.commit();
		
    	
    	try {
    		// pending intent
    		String SENT = "SMS_SENT";
    		PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);

    		sms.sendTextMessage(pNumber, null, "Please enter the following verification code into InstaSnap: " + verificationCode, sentPI, null);
    		
    		Log.i("XMPPClient", "[Registration] sms sent successfully!");	
    		
    	} catch (Exception e){
    		e.printStackTrace();
    		smsFailureAlert.show();   		
    		return;    		
    	}
    	
    	hideLayout();
		
		((TextView) findViewById(R.id.txtRegStatus)).setText("verification");
		
		Intent i = new Intent(mSexPixApplication, CodePrompt.class);
	    startActivityForResult(i, CODE_PROMPT_CODE); 
        
     // use this to switch off sms verification
//      mHandler.post(mPhoneVerified);
		
//		failureReason = getResources().getString(R.string.str_verification_failed);
//        mHandler.postDelayed(mVerificationFailed, 60000);
    }
    
    public void createNewAccount(){
    	Log.i("XMPPClient", "[Registration-createNewAccount] creating new account");
    	
    	
    	TelephonyManager phoneManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);    	
    	mSexPixApplication.strUserPassword = phoneManager.getDeviceId();
    	if (mSexPixApplication.strUserPassword == null){
    		mSexPixApplication.strUserPassword = "null";
    	}
    	Log.i("XMPPClient", "[createNewAccount] DeviceId:" + mSexPixApplication.strUserPassword);
    	
    	// check if Internet is available
    	ConnectivityManager connectivityManager  = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    	
    	if (activeNetworkInfo != null && activeNetworkInfo.isConnected()){
    		new RegisterTask().execute("");
    	} else {
    		// no Internet
    		Log.w("XMPPClient", "[Registration-createNewAccount] no internet connection");
			
			failureReason = getResources().getString(R.string.txt_ver_no_inet);
			mHandler.removeCallbacks(mVerificationFailed);
			mHandler.post(mVerificationFailed);    		
			HockeyReport.submitStackTraces(mSexPixApplication, "InstaSnap.VerificationFailed.reg_connectNoInternet_createNewAccount");
			FlurryAgent.logEvent("reg_connectNoInternet"); 
			
			// send GA event  
			GoogleAnalytics myInstance = GoogleAnalytics.getInstance(mSexPixApplication);
    		Tracker myDefaultTracker = myInstance.getDefaultTracker();
			myDefaultTracker.sendEvent("error", "reg_connectNoInternet", "reg_connectNoInternet_createNewAccount", null);
    	}    			
    }
    
    public void saveAccountSettings(){
    	// Saving Login and Password
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext());
		SharedPreferences.Editor editor = preferences.edit();
		if (mSexPixApplication.connection.isAuthenticated()) {
			editor.putString("strUserLogin", mSexPixApplication.strUserLogin);
			editor.putString("strUserPassword", mSexPixApplication.strUserPassword);
			editor.commit();
		}
    }
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == IMEI_REQUEST_CODE) {

			if (resultCode == Activity.RESULT_OK) {
				if (data != null) {
					mSexPixApplication.strUserPassword = data.getExtras().getString("message");				
					
					hideLayout();
					
					new RegisterTask().execute("");

				}				
			}
			else {
				mSexPixApplication.backgroundSerice.stopSelf();
				if (mSexPixApplication.connection!=null){
					mSexPixApplication.connection.disconnect();
				}						
				
				android.os.Process.killProcess(android.os.Process.myPid());			
			}
		}
		
		if (requestCode ==  CODE_PROMPT_CODE){
			if (resultCode == Activity.RESULT_OK) {
				mHandler.post(mPhoneVerified);
			} 
			else {
				if (data != null) {
					String action = data.getExtras().getString("action");
					if (action.equals("newCode")){
						setEditViews();
						return;
					}
				}
				finish();
								
			}			
		}
    }


    public class RegisterTask extends AsyncTask<String, String, String> {

    	@Override
    	protected String doInBackground(String... params) {
    		Log.i("XMPPClient", "[Registration-RegisterTask] connecting");
    		
    		publishProgress("connecting"); 
    		
    		try {
    			// new registration procedure
    			
    			Log.i("XMPPClient", "[Registration-RegisterTask] pinging the google");
    			

    			URL url = new URL("http://www.google.com");

    			HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
    			urlc.setConnectTimeout(1000 * 10); // mTimeout is in seconds
    			urlc.connect();
    			Log.w("XMPPClient", "[Registration-RegisterTask] ping responce:" + urlc.getResponseCode());
    			if (urlc.getResponseCode() != 200) {    				
    				Log.w("XMPPClient", "[Registration-RegisterTask] ping was not successful. No internet");
    				
            		// no Internet    			
        			failureReason = getResources().getString(R.string.txt_ver_no_inet);
        			mHandler.removeCallbacks(mVerificationFailed);
        			mHandler.post(mVerificationFailed);    						
        			FlurryAgent.logEvent("reg_connectNoInternet"); 
        			
        			HockeyReport.submitStackTraces(mSexPixApplication, "InstaSnap.VerificationFailed.reg_connectNoInternet_RegisterTask");
        			
        			// send GA event  
        			GoogleAnalytics myInstance = GoogleAnalytics.getInstance(mSexPixApplication);
            		Tracker myDefaultTracker = myInstance.getDefaultTracker();
        			myDefaultTracker.sendEvent("error", "reg_connectNoInternet", "reg_connectNoInternet_RegisterTask", null);    
        			
  				
        			return "ConnectionError";
    			}    			
    			

    			
    			
    			
    			
    			
    
    			String regString = mSexPixApplication.strServerCreateAccount 
    					+ "?username=" + URLEncoder.encode(mSexPixApplication.strUserLogin, "UTF-8") 
    					+ "&pass=" + URLEncoder.encode(mSexPixApplication.strUserPassword, "UTF-8");    
    			
    			HttpParams httpParameters = new BasicHttpParams();
    			HttpConnectionParams.setConnectionTimeout(httpParameters, 10000);
    			HttpConnectionParams.setSoTimeout(httpParameters, 10000);

    			HttpClient httpclient = new DefaultHttpClient(httpParameters);
    			HttpPost httppost = new HttpPost(regString);
    			HttpResponse response;	
    			

    				response = httpclient.execute(httppost);	
    				
    				if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
    					Log.w("XMPPClient", "[Registration-RegisterTask] ping was not successful. Server is down or no internet");
    					
    					HockeyReport.submitStackTraces(mSexPixApplication, "InstaSnap.VerificationFailed.reg_http_no_respond");
    					
    					// send GA event  
            			GoogleAnalytics myInstance = GoogleAnalytics.getInstance(mSexPixApplication);
                		Tracker myDefaultTracker = myInstance.getDefaultTracker();
            			myDefaultTracker.sendEvent("error", "reg_http_no_respond", "reg_http_no_respond", null);  
    					
            			return "ConnectionError";
    				}   				  				
    				
    				HttpEntity entity = response.getEntity();
    				String body = EntityUtils.toString(entity);
    				Log.i("XMPPClient", "[Registration-RegisterTask] server response: " + body);
    				if (body.contains("error")){
    					
    					HockeyReport.submitStackTraces(mSexPixApplication, "InstaSnap.VerificationFailed.reg_http_error");
    					
    					// send GA event  
            			GoogleAnalytics myInstance = GoogleAnalytics.getInstance(mSexPixApplication);
                		Tracker myDefaultTracker = myInstance.getDefaultTracker();
            			myDefaultTracker.sendEvent("error", "reg_http_error", "reg_http_error", null);  
    					
    					return "ConnectionError";
    				}


    		} catch (Exception e) {
    			e.printStackTrace();

    			return "ConnectionError";
    		}


    		String result = mSexPixApplication.backgroundSerice.procedureConnect();
    		
    		if(!result.equals("done")){
				return result;
			}
    	    	
	    	
	    	// login    		    	
	    	try {	
	    		publishProgress("authenticating");
	    		
	    		mSexPixApplication.connection.login(mSexPixApplication.strUserLogin + "@" + mSexPixApplication.strServerAddress, mSexPixApplication.strUserPassword, mSexPixApplication.strResource);

	    	} catch (XMPPException ex) {
	    		Log.w("XMPPClient", "[Registration-RegisterTask] Failed to connect to " + mSexPixApplication.connection.getHost());
	    		Log.w("XMPPClient", ex.toString());
	    	//	return "ConnectionError";
	    	} catch (IllegalStateException ex) {
	    		Log.w("XMPPClient", "[Registration-RegisterTask] Failed to connect to " + mSexPixApplication.connection.getHost());
	    		Log.w("XMPPClient", ex.toString());
	    		return "ConnectionError";
	    	}
	    	
	    	
	    	// postLogin stuff
	    	
	    	if (mSexPixApplication.connection.isAuthenticated()) 
    		{					
	    		publishProgress("connected");
	    		
	    		TelephonyManager phoneManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);    	
	        	String phonesImei = phoneManager.getDeviceId();
	        	if (phonesImei == null){
	        		phonesImei = "null";
	        	}
	        	
	        	if (!phonesImei.equals(mSexPixApplication.strUserPassword)){
	        		
	        		// new device confirmed - changing the password
	        		org.jivesoftware.smack.AccountManager mAccount = new org.jivesoftware.smack.AccountManager (mSexPixApplication.connection); 
	        		try {
						mAccount.changePassword(phonesImei);
						mSexPixApplication.strUserPassword = phonesImei;
						Log.i("XMPPClient", "[Registration-RegisterTask] password changed");
					} catch (XMPPException e) {
						Log.w("XMPPClient", "[Registration-RegisterTask] can't change the password");
						e.printStackTrace();
						return "ConnectionError";
					}
	        	}
	    		
	    		// saving login and pass
	        	saveAccountSettings();
	        	
    			result = mSexPixApplication.backgroundSerice.procedurePostLogin();
    			return result;	
    		} else {
    			// wrong password
    			Log.w("XMPPClient", "[Registration-RegisterTask] wrong password");
    			publishProgress("");
    			return "LoginError";
    		}
    	}
    	
    	@Override
		protected void onPostExecute(String result) {
			Log.i("XMPPClient", "[Registration-RegisterTask-onPostExecute] result = " + result);
			
			LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayoutRegister);
    		layout.setVisibility(LinearLayout.INVISIBLE);
    		((ProgressBar) findViewById(R.id.progressBarRegistration)).setVisibility(ProgressBar.INVISIBLE); 
    		((TextView) findViewById(R.id.txtRegStatus)).setVisibility(TextView.INVISIBLE);
			
			if (result.equals("done")){		
				Toast.makeText(Registration.this, R.string.str_user_created, Toast.LENGTH_LONG).show();
				startActivity(new Intent(Registration.this, Home.class));
				
	//			showAd();
				
				startActivity(new Intent(Registration.this, ProfileInfo.class));

				Map<String, String> params = new HashMap<String, String>();
				if (whatsappExists){
					params.put("Verification", "WhatsApp");
				} else {
					params.put("Verification", "SMS");
				}	        		        	
				FlurryAgent.logEvent("User registered", params);
				
				finish();
			}
			else if (result.equals("ConnectionError")){	
				
		    	CustomAlertDialog.Builder alert = new CustomAlertDialog.Builder(Registration.this);
				alert.setMessage(getResources().getString(R.string.txt_conn_error));
				alert.setPositiveButton(getResources().getString(R.string.str_retry), new DialogInterface.OnClickListener() {
		    		public void onClick(DialogInterface dialog, int whichButton) {
		    			((ProgressBar) findViewById(R.id.progressBarRegistration)).setVisibility(ProgressBar.VISIBLE); 
		    			((TextView) findViewById(R.id.txtRegStatus)).setVisibility(TextView.VISIBLE);
		    			((TextView) findViewById(R.id.txtRegStatus)).setText("");
		    			new RegisterTask().execute("");		    			  				        		
		    		}
				});
				alert.setNegativeButton(getResources().getString(R.string.str_close), new DialogInterface.OnClickListener() {
		    		public void onClick(DialogInterface dialog, int whichButton) {
	//	    			showAd();
		    			finish();
				        		
		    		}
				});		
				alert.setOnCancelListener(new DialogInterface.OnCancelListener() {					
					@Override
					public void onCancel(DialogInterface dialog) {
	//					showAd();
						finish();
					}
				});
				
				try {
					alert.show();
				} catch (Exception e){					
				}
				
				FlurryAgent.logEvent("reg_connectXmppDown"); 
				
				// send GA event  
//				GoogleAnalytics myInstance = GoogleAnalytics.getInstance(mSexPixApplication);
//	    		Tracker myDefaultTracker = myInstance.getDefaultTracker();
//				myDefaultTracker.sendEvent("error", "reg_connectXmppDown", "reg_connectXmppDown", null);
			}
			else if (result.equals("LoginError")){					
				Intent i = new Intent(Registration.this, AccountProblem.class);
			    startActivityForResult(i, IMEI_REQUEST_CODE); 	
			    FlurryAgent.logEvent("reg_IMEIRequest");
			    
			    HockeyReport.submitStackTraces(mSexPixApplication, "InstaSnap.VerificationFailed.reg_loginError");
			    
			    // send GA event  
				GoogleAnalytics myInstance = GoogleAnalytics.getInstance(mSexPixApplication);
	    		Tracker myDefaultTracker = myInstance.getDefaultTracker();
				myDefaultTracker.sendEvent("error", "reg_loginError", "reg_loginError", null);
			}
			else if (result.equals("ConnectionServerDown")){						
				failureReason = getResources().getString(R.string.txt_conn_serv_down);
				mHandler.removeCallbacks(mVerificationFailed);
				mHandler.post(mVerificationFailed);    						
				FlurryAgent.logEvent("reg_connectServerDown"); 
				
				HockeyReport.submitStackTraces(mSexPixApplication, "InstaSnap.VerificationFailed.reg_connectServerDown");
				
				// send GA event  
				GoogleAnalytics myInstance = GoogleAnalytics.getInstance(mSexPixApplication);
	    		Tracker myDefaultTracker = myInstance.getDefaultTracker();
				myDefaultTracker.sendEvent("error", "reg_connectServerDown", "reg_connectServerDown", null);
			}
		
		}
    	
    	@Override
		protected void onProgressUpdate(String... values) {   		
    		((TextView) findViewById(R.id.txtRegStatus)).setText(values[0]);
		}
    	
    }
    
}
