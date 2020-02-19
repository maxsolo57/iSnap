package com.apparitionhq.instasnap.listeners;

import java.util.Collection;

import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.packet.Presence;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.apparitionhq.instasnap.SexPixApplication;

public class TRosterListener implements RosterListener {
	private SexPixApplication mSexPixApplication;
	
	public final static String ACTION_REFRESH_CONTACTLIST = "com.apparitionhq.instasnap.action.REFRESH_CONTACTLIST";

	public TRosterListener(SexPixApplication theApp) {
		mSexPixApplication = theApp; 
	}

	@Override
	public void entriesAdded(Collection<String> arg0) {
		Log.i("XMPPClient", "[TRosterListener] entry added");
		Intent intent = new Intent(ACTION_REFRESH_CONTACTLIST);
		intent.putExtra("refreshList", true);
		LocalBroadcastManager.getInstance(mSexPixApplication.getApplicationContext()).sendBroadcast(intent);	
		
		if (mSexPixApplication.connection.getRoster() != null){
			
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext()); 
			SharedPreferences.Editor editor = preferences.edit();
			editor.putInt("roster_size", mSexPixApplication.connection.getRoster().getEntryCount());
			editor.commit();
		}
		
	}

	@Override
	public void entriesDeleted(Collection<String> arg0) {
		Log.i("XMPPClient", "[TRosterListener] entry deleted");
		Intent intent = new Intent(ACTION_REFRESH_CONTACTLIST);
		intent.putExtra("refreshList", true);
		LocalBroadcastManager.getInstance(mSexPixApplication.getApplicationContext()).sendBroadcast(intent);
		
		if (mSexPixApplication.connection.getRoster() != null){
			
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext()); 
			SharedPreferences.Editor editor = preferences.edit();
			editor.putInt("roster_size", mSexPixApplication.connection.getRoster().getEntryCount());
			editor.commit();
		}
	}

	@Override
	public void entriesUpdated(Collection<String> arg0) {
		Log.i("XMPPClient", "[TRosterListener] entries updated");
		Intent intent = new Intent(ACTION_REFRESH_CONTACTLIST);
		intent.putExtra("refreshList", true);
		LocalBroadcastManager.getInstance(mSexPixApplication.getApplicationContext()).sendBroadcast(intent);		
	}

	@Override
	public void presenceChanged(Presence arg0) {
		Intent intent = new Intent(ACTION_REFRESH_CONTACTLIST);
		LocalBroadcastManager.getInstance(mSexPixApplication.getApplicationContext()).sendBroadcast(intent);	
	}

}
