package com.apparitionhq.instasnap.billing;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import com.apparitionhq.instasnap.billing.C.PurchaseState;



/**
 * Security-related methods. For a secure implementation, all of this code
 * should be implemented on a server that communicates with the application on
 * the device. For the sake of simplicity and clarity of this example, this code
 * is included here and is executed on the device. If you must verify the
 * purchases on the phone, you should obfuscate this code to make it harder for
 * an attacker to replace the code with stubs that treat all purchases as
 * verified.
 */
public class BillingSecurity {
	private static final String TAG = "XMPPClient";

	private static final String KEY_FACTORY_ALGORITHM = "RSA";
	private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
	private static final SecureRandom RANDOM = new SecureRandom();

	/**
	 * This keeps track of the nonces that we generated and sent to the server.
	 * We need to keep track of these until we get back the purchase state and
	 * send a confirmation message back to Android Market. If we are killed and
	 * lose this list of nonces, it is not fatal. Android Market will send us a
	 * new "notify" message and we will re-generate a new nonce. This has to be
	 * "static" so that the {@link BillingReceiver} can check if a nonce exists.
	 */
	private static HashSet<Long> sKnownNonces = new HashSet<Long>();

	/**
	 * A class to hold the verified purchase information.
	 */
	public static class VerifiedPurchase {
		public PurchaseState purchaseState;
		public String notificationId;
		public String productId;
		public String orderId;
		public long purchaseTime;
		public String developerPayload;

		public VerifiedPurchase(PurchaseState purchaseState, String notificationId, String productId, String orderId, long purchaseTime,
				String developerPayload) {
			this.purchaseState = purchaseState;
			this.notificationId = notificationId;
			this.productId = productId;
			this.orderId = orderId;
			this.purchaseTime = purchaseTime;
			this.developerPayload = developerPayload;
		}
		
		public boolean isPurchased(){
			return purchaseState.equals(PurchaseState.PURCHASED);
		}
		
		
	}

	/** Generates a nonce (a random number used once). */
	public static long generateNonce() {
		long nonce = RANDOM.nextLong();
	//	Log.i(TAG, "[BillingSecurity] Nonce generated:" + nonce);
		sKnownNonces.add(nonce);
		return nonce;
	}

	public static void removeNonce(long nonce) {
		sKnownNonces.remove(nonce);
	}

	public static boolean isNonceKnown(long nonce) {
		return sKnownNonces.contains(nonce);
	}

	public static ArrayList<VerifiedPurchase> verifyPurchase(String signedData, String signature) {
	//	Log.i(TAG, "[BillingSecurity-verifyPurchase] signedData:" + signedData);// + "    signature:" + signature);
		
		Log.i(TAG, "[BillingSecurity-verifyPurchase] verifying");
		if (signedData == null) {
			Log.e(TAG, "[BillingSecurity-verifyPurchase] data is null");
			return null;
		}
		
		boolean verified = false;
		if (!TextUtils.isEmpty(signature)) {

		//	String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzO+lMI1a4Ibt/Mc8V7Gh0jCPph5iLWuisiCp3TmLwqx/SbcqWouaGBHLktAQmewXOsww3D2I9Z8FNL9raAvWeAJODlJpj5lbhSelaplWUe6dKlHJJjps2bE3br8OUlfrk2tpXjvTmksqYPyTOUC878fC8yO5NltTWsncY3PsSyjAX5cKd2M5ruP61M3ET4eko0zgau7eUWrP9s83mHdlhDDlDe4/B06UQaKlvhCB8aKm6P0sCqLlGjDFe/yTu648VPffYsqTrVrQi0rttoGBotyfbmj5kGalF1Q5gfOtWemJ584Xhd71Z27uruqDIGvuM70FN/ZvKlobpfObwQfrHwIDAQAB";
			String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkZfoqGHNbxeUYjWQs2F8dGxSCqtJOe/+r5pj1FYem1mdzp3uwpKVKlf+nyvi03gxWqYim0Jhi0MW7wbHW+xyOj+xxpq4O4Ki4nZWFhYMnUIaf4ph5cwB+u1yQtGHFKBrzzm/ojZlOCzEQoM3PlyA/A+79hzf7zw4Ezg6rRAqDR6LurMmpQqPHo3Wtp8m6QZ2FOIbZ90q5IraJrSe8Jw5ayUHArHjXG2UXFfuTr+7PhEA9pwnWOI1mD2M7v4YAMDd4MdfI8VMxJ1JWCSA7m/TSudLxl1iw0Vgbc5143rgTjvnS7hMgqJQOhhJhSqDj08KRYwYJhOwquNvktjUXTCiawIDAQAB";
			PublicKey key = BillingSecurity.generatePublicKey(base64EncodedPublicKey);
		//	Log.i(TAG, "[BillingSecurity-verifyPurchase] verifying: key:" + key + "      signedData:" + signedData + "     signature:" + signature);
			verified = BillingSecurity.verify(key, signedData, signature);
			if (!verified) {
				Log.e(TAG, "[BillingSecurity-verifyPurchase] Signature verification failed");// for:" + signature);
				return null;
			}
			Log.i(TAG, "[BillingSecurity-verifyPurchase] Signature verification successful");
		}

		JSONObject jObject;
		JSONArray jTransactionsArray = null;
		int numTransactions = 0;
		long nonce = 0L;
		try {
			jObject = new JSONObject(signedData);

			// The nonce might be null if the user backed out of the buy page.
			nonce = jObject.optLong("nonce");
			jTransactionsArray = jObject.optJSONArray("orders");
			if (jTransactionsArray != null) {
				numTransactions = jTransactionsArray.length();
			}
		} catch (JSONException e) {
			return null;
		}

		if (!BillingSecurity.isNonceKnown(nonce)) {
			Log.w(TAG, "[BillingSecurity-verifyPurchase] Nonce not found:" + nonce);
			return null;
		}

		ArrayList<VerifiedPurchase> purchases = new ArrayList<VerifiedPurchase>();
		try {
			for (int i = 0; i < numTransactions; i++) {
				JSONObject jElement = jTransactionsArray.getJSONObject(i);
				int response = jElement.getInt("purchaseState");
				PurchaseState purchaseState = PurchaseState.valueOf(response);
				String productId = jElement.getString("productId");
				String packageName = jElement.getString("packageName");
				long purchaseTime = jElement.getLong("purchaseTime");
				String orderId = jElement.optString("orderId", "");
				String notifyId = null;
				if (jElement.has("notificationId")) {
					notifyId = jElement.getString("notificationId");
				}
				String developerPayload = jElement.optString("developerPayload", null);

				// If the purchase state is PURCHASED, then we require a
				// verified nonce.
				if (purchaseState == PurchaseState.PURCHASED && !verified) {
					continue;
				}
				purchases.add(new VerifiedPurchase(purchaseState, notifyId, productId, orderId, purchaseTime, developerPayload));
			}
		} catch (JSONException e) {
			Log.e(TAG, "[BillingSecurity-verifyPurchase] JSON exception: ", e);
			return null;
		}
		removeNonce(nonce);
		return purchases;
	}


	public static PublicKey generatePublicKey(String encodedPublicKey) {
		try {
			byte[] decodedKey = Base64.decode(encodedPublicKey);
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
			return keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (InvalidKeySpecException e) {
			Log.e(TAG, "Invalid key specification.");
			throw new IllegalArgumentException(e);
		} catch (Base64DecoderException e) {
			Log.e(TAG, "Base64DecoderException.", e);
			return null;
		}
	}


	public static boolean verify(PublicKey publicKey, String signedData, String signature) {

		Signature sig;
		try {
			sig = Signature.getInstance(SIGNATURE_ALGORITHM);
			sig.initVerify(publicKey);
			sig.update(signedData.getBytes());
			if (!sig.verify(Base64.decode(signature))) {
				
				return false;
			}
			return true;
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "NoSuchAlgorithmException.");
		} catch (InvalidKeyException e) {
			Log.e(TAG, "Invalid key specification.");
		} catch (SignatureException e) {
			Log.e(TAG, "Signature exception.");
		}  catch (Base64DecoderException e) {
			Log.e(TAG, "Base64DecoderException.", e);
		}
		return false;
	}
}
