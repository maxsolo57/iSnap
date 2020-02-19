package de.guitarcollege.custom.dialog;

import com.apparitionhq.instasnap.R;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;


public class CustomRingtonePreference extends DialogPreference {
	public Context cContext;
	public MediaPlayer mp;
	public CharSequence[] items;
	public int[] itemsValues;
	public int currentSound;

	public CustomRingtonePreference(Context context, AttributeSet attrs) {
		super(context, attrs);	
		cContext = context;		
	}
	
	@Override
	protected void showDialog(Bundle state){
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(cContext.getApplicationContext()); 
    	currentSound = preferences.getInt("optNotificationRingtoneWhich", 1); 
		
		
		CustomAlertDialog.Builder alert = new CustomAlertDialog.Builder(cContext);
		alert.setTitle(cContext.getResources().getString(R.string.str_notification_ringtone));
		alert.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(cContext.getApplicationContext());
				SharedPreferences.Editor editor = preferences.edit();
				
				editor.putInt("optNotificationRingtoneWhich", currentSound);
				editor.commit();				
			}
			
		});
		alert.setNegativeButton(R.string.str_cancel, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
			}
			
		});

		if (null != items && null != itemsValues){
			alert.setSingleChoiceItems(items, currentSound, new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					currentSound = which;
					if (null != mp){
						mp.reset();
					}
					if (itemsValues[which] != 0){
						mp = MediaPlayer.create(cContext, itemsValues[which]);
						mp.start();
					}
				}
			});
		}
		
		
		
		
		alert.show();
	}
	



}
