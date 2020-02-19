package com.apparitionhq.instasnap.billing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static com.apparitionhq.instasnap.billing.C.*;


public class BillingReceiver extends BroadcastReceiver {

	private static final String TAG = "XMPPClient";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.i(TAG, "[BillingReceiver] Received action: " + action);
        if (ACTION_PURCHASE_STATE_CHANGED.equals(action)) {
            String signedData = intent.getStringExtra(INAPP_SIGNED_DATA);
            String signature = intent.getStringExtra(INAPP_SIGNATURE);
            purchaseStateChanged(context, signedData, signature);
        } else if (ACTION_NOTIFY.equals(action)) {
            String notifyId = intent.getStringExtra(NOTIFICATION_ID);
            notify(context, notifyId);
        } else if (ACTION_RESPONSE_CODE.equals(action)) {
            long requestId = intent.getLongExtra(INAPP_REQUEST_ID, -1);
            int responseCodeIndex = intent.getIntExtra(INAPP_RESPONSE_CODE, C.ResponseCode.RESULT_ERROR.ordinal());
            checkResponseCode(context, requestId, responseCodeIndex);
        } else {
           Log.e(TAG, "[BillingReceiver] unexpected action: " + action);
        }
	}


	public void purchaseStateChanged(Context context, String signedData, String signature) {
	//	Log.i(TAG, "[BillingReceiver-purchaseStateChanged] signedData:" + signedData + "    signature:" + signature);
		BillingHelper.verifyPurchase(signedData, signature);
	}
	
	public void notify(Context context, String notifyId) {
		Log.i(TAG, "[BillingReceiver] notify got id: " + notifyId);
		String[] notifyIds = {notifyId};
		BillingHelper.getPurchaseInformation(notifyIds);
	}
	
	public void checkResponseCode(Context context, long requestId, int responseCodeIndex) {
		Log.i(TAG, "[BillingReceiver-checkResponseCode] RequestId: " + requestId + "    ResponseCode: " + C.ResponseCode.valueOf(responseCodeIndex));
	}
}