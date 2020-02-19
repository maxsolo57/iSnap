package com.apparitionhq.instasnap;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.packet.VCard;

import android.os.Bundle;
import android.app.Activity;
import android.content.res.Configuration;
//import android.util.Log;
import com.apphance.android.Log;
import com.google.analytics.tracking.android.EasyTracker;

import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ProfileInfo extends Activity {
	
	private SexPixApplication mSexPixApplication;
	public VCard card;
	
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
		setContentView(R.layout.activity_profile_info);
		
		mSexPixApplication = (SexPixApplication) getApplication();
		
		((TextView) findViewById(R.id.profilePhone)).setText(mSexPixApplication.strUserLogin);

		if (mSexPixApplication.connection == null || !mSexPixApplication.connection.isAuthenticated()){
			Log.i("XMPPClient", "[ProfileInfo-onCreate] not logged in");	
			return;
		}
		
		card = new VCard();
			
		try {
			card.load(mSexPixApplication.connection);
		//	Log.i("XMPPClient", "[ProfileInfo-VCard] NickName:" + card.getNickName());	
			if (card.getNickName() != null){
				((EditText) findViewById(R.id.profileName)).setText(card.getNickName());
			}

		} catch (XMPPException e1) {
			e1.printStackTrace();
		}
	}
	
	public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        (findViewById(R.id.RootView)).setBackgroundDrawable(getResources().getDrawable( R.drawable.bg_bright));
        
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_profile_info, menu);
		return true;
	}
	
	
	public void butCancel(View view) {
		finish();
	}
	
	public void butSave(View view) {
		
		if (mSexPixApplication.connection == null || !mSexPixApplication.connection.isAuthenticated() || card == null){
			Log.i("XMPPClient", "[ProfileInfo-onCreate] not logged in");	
			Toast.makeText(view.getContext(), R.string.str_connectionError, Toast.LENGTH_SHORT).show();
			return;
		}
			
		try {
		//	card.load(mSexPixApplication.connection);
			card.setNickName(((EditText) findViewById(R.id.profileName)).getText().toString());
			card.save(mSexPixApplication.connection);
			Toast.makeText(view.getContext(), R.string.str_saved, Toast.LENGTH_SHORT).show();
			finish();

		} catch (XMPPException e1) {
			e1.printStackTrace();
			Toast.makeText(view.getContext(), R.string.str_connectionError, Toast.LENGTH_SHORT).show();
		}
		
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
	protected void onDestroy() {
		super.onDestroy();

		unbindDrawables(findViewById(R.id.RootView));
		Log.w("XMPPClient", "[Picture] System.gc()");
		System.gc();
	}

}
