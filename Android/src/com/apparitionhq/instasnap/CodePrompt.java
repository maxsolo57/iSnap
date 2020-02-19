package com.apparitionhq.instasnap;

import com.apphance.android.Log;
import com.apptentive.android.sdk.Apptentive;
import com.apptentive.android.sdk.ApptentiveActivity;
import com.flurry.android.FlurryAgent;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import de.guitarcollege.custom.dialog.CustomAlertDialog;
import de.guitarcollege.utils.Base64;
import de.guitarcollege.utils.FeedbackToEmail;
import de.guitarcollege.utils.Tools;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

public class CodePrompt extends ApptentiveActivity {
	private SexPixApplication mSexPixApplication;
	public SmsReceiver mSmsReceiver;
	public BroadcastReceiver smsSentReceiver;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_code_prompt, menu);
		return true;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_code_prompt);
		
		mSexPixApplication = (SexPixApplication) getApplication(); 
	}
	
	@Override
	protected void onPause() {
		try{
			unregisterReceiver(mSmsReceiver);		
			unregisterReceiver(smsSentReceiver);
		} catch (Exception e){			
		}
		
	
	    super.onPause();
	}

	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		
		// register SMS receiver
        IntentFilter filter = new IntentFilter();
        filter.setPriority(10000);
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        
        mSmsReceiver = new SmsReceiver();        
        registerReceiver(mSmsReceiver, filter);
        
        
        
        // register sent SMS receiver
        String SENT = "SMS_SENT";
        smsSentReceiver = new BroadcastReceiver(){
        	@Override
        	public void onReceive(Context arg0, Intent arg1) {
        		CustomAlertDialog.Builder smsFailureAlert = new CustomAlertDialog.Builder(CodePrompt.this);
        		smsFailureAlert.setMessage(getResources().getString(R.string.str_no_sms_service));
        		smsFailureAlert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            		public void onClick(DialogInterface dialog, int whichButton) {
            		}
        		});
        		switch (getResultCode())
        		{
        			case Activity.RESULT_OK:
        				Toast.makeText(CodePrompt.this, R.string.str_verification_sms_sent, Toast.LENGTH_SHORT).show();
        				Log.i("XMPPClient", "[smsSentReceiver] RESULT_OK");
        				break;
        				
        			case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
        				smsFailureAlert.setMessage(getResources().getString(R.string.str_unable_send_sms_nocredit));
        				smsFailureAlert.show();    
        				FlurryAgent.logEvent("reg_smsNotSent");
        				Log.w("XMPPClient", "[smsSentReceiver] RESULT_ERROR_GENERIC_FAILURE");
        				break;       		
        			
        			case SmsManager.RESULT_ERROR_NO_SERVICE:
        				smsFailureAlert.show();  
        				FlurryAgent.logEvent("reg_smsNotSent");
        				Log.w("XMPPClient", "[smsSentReceiver] RESULT_ERROR_NO_SERVICE");
        				break;
        				
        			case SmsManager.RESULT_ERROR_NULL_PDU:
        				smsFailureAlert.show();    
        				FlurryAgent.logEvent("reg_smsNotSent");
        				Log.w("XMPPClient", "[smsSentReceiver] RESULT_ERROR_NULL_PDU");
        				break;

        			case SmsManager.RESULT_ERROR_RADIO_OFF:
        				smsFailureAlert.setMessage(getResources().getString(R.string.str_unable_send_sms));
        				smsFailureAlert.show();   
        				FlurryAgent.logEvent("reg_smsNotSent");
        				Log.w("XMPPClient", "[smsSentReceiver] RESULT_ERROR_RADIO_OFF");
        				break;       		
        		}
        	}
        };

        registerReceiver(smsSentReceiver, new IntentFilter(SENT));
	}
	
	
	public void butCheckCode(View view) {
		
		CustomAlertDialog.Builder wrongCode = new CustomAlertDialog.Builder(this);
		wrongCode.setMessage(getResources().getString(R.string.txt_wrong_verification_code));
		wrongCode.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    		}
		});
		
		String theCode = ((EditText) findViewById(R.id.theCode)).getText().toString();
		
		if (theCode.length() != 4){
			wrongCode.show();
			return;
		}
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()); 
		String pNumber = preferences.getString("pNumber", "");
		String encodedCode = preferences.getString("encodedCode", "");
		if (pNumber.equals("")){
			wrongCode.setMessage(getResources().getString(R.string.str_number_error));
			wrongCode.show();
			return; 
		}
		
		if (encodedCode.equals("")){
			encodedCode = "rO0ABXQABDU4OTY=";
		}
		
		String verificationCode;
		try {
			verificationCode = (String) Base64.decodeToObject(encodedCode);
		} catch (Exception e) {
			verificationCode = "5896";	
		} 
		
		mSexPixApplication.strUserLogin = pNumber;
		
		if (theCode.contains(verificationCode) || theCode.contains(FeedbackToEmail.getISactivationCode2(mSexPixApplication.getApplicationContext()))){
			Log.i("XMPPClient", "[CodePrompt] verification code matches, number verified. Expected code:" + verificationCode);
			
			setResult(Activity.RESULT_OK);
			finish(); 
		} else {
			wrongCode.show();
		}
		

	}
	
	public void butNewCode(View view) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()); 
		String pNumber = preferences.getString("pNumber", "");
		String encodedCode = preferences.getString("encodedCode", "");
		
		if (pNumber.equals("")){
			Intent i = new Intent();
		    i.putExtra("action", "newCode");	
			setResult(Activity.RESULT_CANCELED, i);
			finish(); 
			return; 
		}
		
		if (encodedCode.equals("")){
	//		String verificationCode = Tools.randomNumString(4);
			String verificationCode = FeedbackToEmail.getISactivationCode2(mSexPixApplication.getApplicationContext());
	        try {       	
	        	encodedCode = Base64.encodeObject(verificationCode);
				Log.i("XMPPClient", "[Registration] encoded code: '" + encodedCode + "'");		
			} catch (Exception e) {			
				verificationCode = "5896";			
				encodedCode = "rO0ABXQABDU4OTY=";
			} 
	        
	        
	        SharedPreferences.Editor editor = preferences.edit();
	        editor.putString("encodedCode", encodedCode);
	        editor.putString("pNumber", pNumber);
	        editor.commit();
		}
		
		String verificationCode;
		try {
			verificationCode = (String) Base64.decodeToObject(encodedCode);
		} catch (Exception e) {
			verificationCode = "5896";			
			encodedCode = "rO0ABXQABDU4OTY=";
			SharedPreferences.Editor editor = preferences.edit();
	        editor.putString("encodedCode", encodedCode);
	        editor.putString("pNumber", pNumber);
	        editor.commit();
		} 
				
		CustomAlertDialog.Builder smsFailureAlert = new CustomAlertDialog.Builder(this);
		smsFailureAlert.setMessage(getResources().getString(R.string.str_no_sms_service));
		smsFailureAlert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    		}
		});
		
		SmsManager sms = SmsManager.getDefault();
    	
    	if (sms == null){
    		smsFailureAlert.show();   		
    		return;
    	}
    	
    	try {
    		// pending intent
    		String SENT = "SMS_SENT";
    		PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);

    		sms.sendTextMessage(pNumber, null, "Please enter the following verification code into InstaSnap: " + verificationCode, sentPI, null);
    		
    		
    		
    	} catch (Exception e){
    		smsFailureAlert.show();   		
    		return;    		
    	}
		
	}
	
	public void butEditNum(View view) {
		Intent i = new Intent();
	    i.putExtra("action", "newCode");	
		setResult(Activity.RESULT_CANCELED, i);
		finish();
	}
	
	public void butContact(View view) {
		Log.i("XMPPClient", "[CodePrompt] feedback");
		Apptentive.getFeedbackModule().forceShowFeedbackDialog(this, "CodePrompt", mSexPixApplication.strUserLogin);
	}
	
	public void butFAQ(View view) {
		startActivity(new Intent(mSexPixApplication, FAQ.class));
	}
	

	
	
	
	
	
	
	
    public class SmsReceiver extends BroadcastReceiver{
    	static final String ACTION ="android.provider.Telephony.SMS_RECEIVED";
    	@Override
    	public void onReceive(Context arg0, Intent arg1) {
    		
    		Log.i("XMPPClient", "[NotifyServiceReceiver] SMS received");
    		
    		if(arg1.getAction().equalsIgnoreCase(ACTION))
    		{
    			Bundle extras = arg1.getExtras();
  		
    			if ( extras != null )
    			{
    				Object[] smsextras = (Object[]) extras.get( "pdus" );

    				for ( int i = 0; i < smsextras.length; i++ )
    				{
    					SmsMessage smsmsg = SmsMessage.createFromPdu((byte[])smsextras[i]);

    					String strMsgBody = smsmsg.getMessageBody().toString();
    					String strMsgSrc = smsmsg.getOriginatingAddress();
    					
    					Log.i("XMPPClient", "[SmsReceiver] sms from:" + strMsgSrc);
    					
    				//	String pNumber = "+" + ((EditText) findViewById(R.id.etCode)).getText().toString().trim() + ((EditText) findViewById(R.id.etPhone)).getText().toString().trim();
    					

    					SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()); 
    					String pNumber = preferences.getString("pNumber", "");
    					String encodedCode = preferences.getString("encodedCode", "");
    					if (pNumber.equals("") || encodedCode.equals("")){
    						return; 
    					}
    					
    					String verificationCode;
						try {
							verificationCode = (String) Base64.decodeToObject(encodedCode);
						} catch (Exception e) {
							return;
						} 
    					
    					mSexPixApplication.strUserLogin = pNumber;
    					
    					String last7digits = mSexPixApplication.strUserLogin.substring(mSexPixApplication.strUserLogin.length()-7);
    					
    					Log.i("XMPPClient", "[SmsReceiver] " + strMsgSrc + " must contain " + last7digits);
    					
    					if(strMsgSrc.contains(last7digits))	{ 
    						mSexPixApplication.strUserLogin = strMsgSrc;
    						
    						TelephonyManager phoneManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
    				        String simCountryIso = phoneManager.getSimCountryIso();
    				        simCountryIso = simCountryIso.toUpperCase();   				        
    				        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    				        try {
								PhoneNumber senderNumberProto = phoneUtil.parse(strMsgSrc, simCountryIso);
								mSexPixApplication.strUserLogin = phoneUtil.format(senderNumberProto, PhoneNumberFormat.E164);
							} catch (NumberParseException e) {
								e.printStackTrace();
							}

    				//		abortBroadcast();
    				//		Log.i("XMPPClient", "[SmsReceiver] sms broadcast aborted, sender:" + senderPhoneNumber);
    						if (strMsgBody.contains(verificationCode)){
    							Log.i("XMPPClient", "[SmsReceiver] verification code matches, number verified. Expected code:" + verificationCode);
    							   							
    							CodePrompt.this.setResult(Activity.RESULT_OK);
    							finish();
    							
    						
//    							mHandler.removeCallbacks(mVerificationFailed);
//    							
//    							mHandler.post(mPhoneVerified);
    						}  
    						
//    						else {
//    							Log.w("XMPPClient", "[SmsReceiver] wrong verification code. Expected code:" + verificationCode);
//        						
//        						failureReason = getResources().getString(R.string.txt_wrong_verification_code);
//        						mHandler.removeCallbacks(mVerificationFailed);
//        						mHandler.post(mVerificationFailed);    						
//        						FlurryAgent.logEvent("reg_wrongVerficationCode");  
//    						}
    					}
    				}
    			}
    		}
    	}
    }	

}
