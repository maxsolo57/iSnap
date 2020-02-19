package com.apparitionhq.instasnap.listeners;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.apparitionhq.instasnap.Notification;
import com.apparitionhq.instasnap.SplashScreen;
import com.urbanairship.UAirship;
import com.urbanairship.push.PushManager;

public class UAshipReceiver extends BroadcastReceiver {
	private static final String logTag = "XMPPClient";

	@Override
	public void onReceive(Context context, Intent intent) {
	//	Log.i(logTag, "[UAshipReceiver] Received intent: " + intent.toString());
		String action = intent.getAction();
		if (action.equals(PushManager.ACTION_PUSH_RECEIVED)) {
			int id = intent.getIntExtra(PushManager.EXTRA_NOTIFICATION_ID, 0);
			Log.i(logTag, "[UAshipReceiver] Received push notification. Alert: " + intent.getStringExtra(PushManager.EXTRA_ALERT)
							+ " [NotificationID=" + id + "]");
			logPushExtras(intent);
		} else if (action.equals(PushManager.ACTION_NOTIFICATION_OPENED)) {
			Log.i(logTag,"[UAshipReceiver] User clicked notification. Message: " + intent.getStringExtra(PushManager.EXTRA_ALERT));
			logPushExtras(intent);
			
			Intent launch = new Intent(Intent.ACTION_MAIN);
			launch.setClass(UAirship.shared().getApplicationContext(), Notification.class);
			launch.putExtra("EXTRA_ALERT", intent.getStringExtra(PushManager.EXTRA_ALERT));
			launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			UAirship.shared().getApplicationContext().startActivity(launch);
		} else if (action.equals(PushManager.ACTION_REGISTRATION_FINISHED)) {
			Log.i(logTag, "[UAshipReceiver] Registration complete. APID:"
							+ intent.getStringExtra(PushManager.EXTRA_APID)
							+ ". Valid: "
							+ intent.getBooleanExtra(
									PushManager.EXTRA_REGISTRATION_VALID, false));
		}
	}

	/**
	 * Log the values sent in the payload's "extra" dictionary.
	 * 
	 * @param intent
	 *            A PushManager.ACTION_NOTIFICATION_OPENED or
	 *            ACTION_PUSH_RECEIVED intent.
	 */
	private void logPushExtras(Intent intent) {
		Set<String> keys = intent.getExtras().keySet();
		for (String key : keys) {
			// ignore standard extra keys (GCM + UA)
			List<String> ignoredKeys = (List<String>) Arrays.asList(
					"collapse_key",// GCM collapse key
					"from",// GCM sender
					PushManager.EXTRA_NOTIFICATION_ID,// int id of generated
														// notification
														// (ACTION_PUSH_RECEIVED
														// only)
					PushManager.EXTRA_PUSH_ID,// internal UA push id
					PushManager.EXTRA_ALERT);// ignore alert
			if (ignoredKeys.contains(key)) {
				continue;
			}
			Log.i(logTag, "[UAshipReceiver] Push Notification Extra: [" + key + " : " + intent.getStringExtra(key) + "]");
		}
	}
}
