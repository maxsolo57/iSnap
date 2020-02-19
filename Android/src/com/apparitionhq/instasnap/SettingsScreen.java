package com.apparitionhq.instasnap;
import java.io.File;

import com.apphance.android.Log;
import com.google.analytics.tracking.android.EasyTracker;

import de.guitarcollege.custom.dialog.CustomAlertDialog;
import de.guitarcollege.custom.dialog.CustomRingtonePreference;
import de.guitarcollege.utils.Tools;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.RingtonePreference;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;


public class SettingsScreen extends PreferenceActivity {
	public static String thumbStorage = "thumbs.bin";
	private SexPixApplication mSexPixApplication;
	public final static String ACTION_REFRESH_CONTACTLIST = "com.apparitionhq.instasnap.action.REFRESH_CONTACTLIST";
	
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
		addPreferencesFromResource(R.xml.activity_settings);
		
		mSexPixApplication = (SexPixApplication) getApplication();         
		
		Preference optClear = (Preference) findPreference("optClear");		
		optClear.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				File thumbStorageFile = new File(mSexPixApplication.getFilesDir().toString() + "/" + thumbStorage);
				thumbStorageFile.delete();
				
				Tools.deleteFiles(mSexPixApplication.getFilesDir().toString() + "/thumbnails");
				
				Intent intent = new Intent(ACTION_REFRESH_CONTACTLIST);
				LocalBroadcastManager.getInstance(mSexPixApplication.getApplicationContext()).sendBroadcast(intent);
				
				Toast.makeText(SettingsScreen.this, R.string.str_history_cleared, Toast.LENGTH_SHORT).show();
				return true;
			}			
		});
		
		Preference optSignOut = (Preference) findPreference("optSignOut");		
		optSignOut.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent i = new Intent();
			    i.putExtra("message", "exit");			   
				setResult(Activity.RESULT_OK, i);
				finish();
				return true;
			}			
		});
		
		
		final CustomAlertDialog.Builder alert = new CustomAlertDialog.Builder(this);
		alert.setMessage(getResources().getString(R.string.str_delete_account_confirm));
		alert.setPositiveButton(getResources().getString(R.string.str_yes), new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    			Intent i = new Intent();
			    i.putExtra("message", "delete");			   
				setResult(Activity.RESULT_OK, i);
				finish();  				        		
    		}
		});
		alert.setNegativeButton(getResources().getString(R.string.str_no), new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
		        		
    		}
		});
		
		Preference optProfile = (Preference) findPreference("optProfile");		
		optProfile.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(SettingsScreen.this, ProfileInfo.class));
				return true;
			}						
		});
		
		Preference optDeleteAccount = (Preference) findPreference("optDeleteAccount");		
		optDeleteAccount.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				alert.show();
				return true;
			}						
		});
		
		
		CheckBoxPreference showHistory = (CheckBoxPreference) findPreference("optShowHistory");	
		showHistory.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				Intent intent = new Intent(ACTION_REFRESH_CONTACTLIST);
				LocalBroadcastManager.getInstance(mSexPixApplication.getApplicationContext()).sendBroadcast(intent);
				return true;
			}			
		});
		

		CustomRingtonePreference notifRingtone = (CustomRingtonePreference) findPreference("optNotificationRingtone");	
		notifRingtone.items = new CharSequence[]{"None","Default","Bubble","Pulse"};
		notifRingtone.itemsValues = new int[]{0,R.raw.default_sound,R.raw.bubble,R.raw.pulse};
		
		

//		notifRingtone.setOnPreferenceClickListener(new OnPreferenceClickListener(){
//
//			@Override
//			public boolean onPreferenceClick(Preference arg0) {
//				Log.w("XMPPClient", "[SettingsScreen] ringtone");
//				return true;
//			}
//			
//		});


	}

}
