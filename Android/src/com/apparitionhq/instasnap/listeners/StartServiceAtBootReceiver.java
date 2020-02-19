package com.apparitionhq.instasnap.listeners;

import com.apparitionhq.instasnap.BgService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartServiceAtBootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
        	Intent svcintent = new Intent(BgService.ACTION_NETWORK_CONNECT);          
            context.startService(svcintent);	
        }
	}
}