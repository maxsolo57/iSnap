package com.apparitionhq.instasnap;

import com.google.analytics.tracking.android.EasyTracker;

import de.guitarcollege.custom.dialog.CustomAlertDialog;
import android.os.Bundle;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class IMEIRequest extends Activity {
	
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imeirequest);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_imeirequest, menu);
        return true;
    }
    
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getWindow().setBackgroundDrawable(getResources().getDrawable( R.drawable.bg_dark ));
    }
    
    public void butSubmit(View view) {
		
		CustomAlertDialog.Builder alert = new CustomAlertDialog.Builder(this);
		alert.setMessage(getResources().getString(R.string.str_imei_tooshort));
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
 				        		
    		}
		});	
		
		String imei = ((EditText) findViewById(R.id.etImei)).getText().toString();
		if(imei.length()<14){
			alert.show();
			return;
		}
		
		Intent i = new Intent();
	    i.putExtra("message", imei);			
		setResult(Activity.RESULT_OK, i);
		finish(); 
	}
    
    public void butCancel(View view) {
    	
    	CustomAlertDialog.Builder alert = new CustomAlertDialog.Builder(this);
    	alert.setTitle(getResources().getString(R.string.str_quit));
		alert.setPositiveButton(getResources().getString(R.string.str_yes), new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    			setResult(Activity.RESULT_CANCELED);
    			finish(); 				        		
    		}
		});
		alert.setNegativeButton(getResources().getString(R.string.str_no), new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
		        		
    		}
		});
		
		alert.show();
	
	}
}
