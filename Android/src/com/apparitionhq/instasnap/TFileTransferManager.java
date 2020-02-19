package com.apparitionhq.instasnap;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import de.guitarcollege.utils.Base64;
import de.guitarcollege.utils.Tools;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
//import android.util.Log;
import com.apphance.android.Log;


public class TFileTransferManager {
	private SexPixApplication mSexPixApplication;
	public String incomingFilename = "received.jpg";	
	public String incomingFilenameBlur = "received_b.jpg";	
	public final int doNotShowLockScreen = 0;
	public final int showLockScreen = 1;
	final int BUFFER_SIZE = 1024 * 8;
	
	public Handler downloadHandler;
	public int attempts = 0;
	public final static String ACTION_REFRESH_NEWMSG = "com.apparitionhq.instasnap.action.REFRESH_NEWMSG";
	
	

	public TFileTransferManager(SexPixApplication theApp) {
		
		mSexPixApplication = theApp;		
		downloadHandler = mSexPixApplication.logHandler;
	}
	
	
	public void receiveFile(Message msg){
		Log.i("XMPPClient", "[receiveFile]");
		downloadHandler.post(new downloadRunnable(msg));
	}
	
	private class downloadRunnable implements Runnable {
		Message msg;
		
		downloadRunnable(Message packet) { msg = packet; }
        public void run() {
        	Time now = new Time();
			now.setToNow();			
			Log.i("XMPPClient", "[downloadRunnable] attempt at:" + now.hour + ":" + now.minute + "." + now.second);
        	new receiveFileTask().execute(msg);
        }
		
	}
	
	private class sendReceiveFeedbackRunnable implements Runnable {
		Message msg;
		
		sendReceiveFeedbackRunnable(Message packet) { msg = packet; }
        public void run() {
        	Time now = new Time();
			now.setToNow();			
			Log.i("XMPPClient", "[sendReceiveFeedbackRunnable] attempt at:" + now.hour + ":" + now.minute + "." + now.second);
			new sendReceiveFeedback().execute(msg);
        }	
	}
	
	private class deleteFromServerRunnable implements Runnable {
		Message msg;
		
		deleteFromServerRunnable(Message packet) { msg = packet; }
        public void run() {
        	Time now = new Time();
			now.setToNow();			
			Log.i("XMPPClient", "[deleteFromServerRunnable] attempt at:" + now.hour + ":" + now.minute + "." + now.second);
			new deleteFromServerTask().execute(msg);
        }	
	}

	
	private class sendReceiveFeedback extends AsyncTask<Object, Void, String> {
		private Message msg = null;
		
		@Override
		protected String doInBackground(Object... params) {
			msg = (Message) params[0];
			String fName = (String) msg.getProperty("filename"); 			
			
			// send feedback to sender						
			Message iSPacket = new Message(StringUtils.parseBareAddress(msg.getFrom()), Message.Type.normal);
    		iSPacket.setPacketID("TransferStatus-" + Tools.randomString(8));
    		iSPacket.setBody("_" + fName + "_received");
    		iSPacket.setProperty("filename", fName);
    		iSPacket.setProperty("transferStatus", "received");
    		attempts = 0;
    		
    		try {
    			mSexPixApplication.connection.sendPacket(iSPacket);  
    			Log.i("XMPPClient", "[sendReceiveFeedback] TransferStatus feedback sent to " + StringUtils.parseName(msg.getFrom()));
    			
    			
    			// send friend request if necessary			
				if(mSexPixApplication.connection != null){
					if(mSexPixApplication.connection.getRoster() != null){
						RosterEntry entry = mSexPixApplication.connection.getRoster().getEntry(StringUtils.parseBareAddress(msg.getFrom()));
						if (entry == null){
							 Presence reply = new Presence(Presence.Type.subscribed); 
							 reply.setTo(StringUtils.parseBareAddress(msg.getFrom())); 
							 mSexPixApplication.connection.sendPacket(reply);
							 
							 Presence requestSubscribe = new Presence(Presence.Type.subscribe); 
							 requestSubscribe.setTo(StringUtils.parseBareAddress(msg.getFrom())); 
							 mSexPixApplication.connection.sendPacket(requestSubscribe);	
						}
					}
				}
				
    			return "done";
    		}
    		catch (Exception e){
    			e.printStackTrace();
				return "cant send feedback";    			
    		}									
		}
		
		protected void onPostExecute(String result) {
			if (!result.equals("done")) {
				Log.w("XMPPClient", "[sendReceiveFeedback] couldn't send feedback, next try in 1 minute");
				downloadHandler.postDelayed(new sendReceiveFeedbackRunnable(msg), 60000);
			}
		}
	}
	
	
	private class deleteFromServerTask extends AsyncTask<Object, Void, String> {
		private Message msg = null;
		
		@Override
		protected String doInBackground(Object... params) {
			msg = (Message) params[0];
			String fName = (String) msg.getProperty("filename"); 
			
			ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("filename", fName));
			nameValuePairs.add(new BasicNameValuePair("receiver", mSexPixApplication.strUserLogin));
			
			try{
				HttpClient httpclient = new DefaultHttpClient();
				HttpPost httppost = new HttpPost(mSexPixApplication.strServerDelAddress);
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				httpclient.execute(httppost);
				return "done";
			}
			catch(Exception e){
				e.printStackTrace();
				return "no connection";				
			}						
		}
		
		protected void onPostExecute(String result) {
			if (!result.equals("done")) {
				Log.w("XMPPClient", "[deleteFromServerTask] couldn't delete file from server, next try in 5 minutes");
				downloadHandler.postDelayed(new deleteFromServerRunnable(msg), 300000);
			}
		}
	}
	

	private class receiveFileTask extends AsyncTask<Object, Void, String> {

		private Message msg = null;
		
		@Override
		protected String doInBackground(Object... params) {
			msg = (Message) params[0];
			
			String fName = (String) msg.getProperty("filename"); 
					
			ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("filename", fName));
			nameValuePairs.add(new BasicNameValuePair("receiver", mSexPixApplication.strUserLogin));
			
			try{
				HttpClient httpclient = new DefaultHttpClient();
				HttpPost httppost = new HttpPost(mSexPixApplication.strServerGetAddress);
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				HttpResponse response = httpclient.execute(httppost);
				HttpEntity entity = response.getEntity();
				String body = EntityUtils.toString(entity); 
				
				int expectedSize = (Integer) msg.getProperty("size");
				
				if (!body.equals("0")){
					byte[] bArray = Base64.decode(body);
					
					httpclient = null;
					httppost = null;
					response = null;
					entity = null;
					body = null;
					System.gc();
					
					
					
					if (bArray.length == expectedSize){
						Tools.writeFile(bArray, mSexPixApplication.getFilesDir().toString() + "/" + fName);
						
						bArray = null;
						System.gc();
						
						// send feedback to sender						
						new sendReceiveFeedback().execute(msg);
						
						// delete file from server
						new deleteFromServerTask().execute(msg);
						
						
						return "done";
					} else {
						Log.w("XMPPClient", "[FileTransfer] file:" + fName + "  expected size:" + expectedSize + ",   real size:" + bArray.length);
						bArray = null;
						System.gc();
						return "wrong file size";
					}

				} else {
					if (expectedSize == 0){
						return "empty file sent";
					}
					Log.e("XMPPClient", "[FileTransfer] Error: empty file");
					return "wrong file size";					
				}	
			}
			catch(Exception e){
				e.printStackTrace();
				return "no connection";				
			}
		}
		
		@Override
		protected void onPostExecute(String result) {
			if (result.equals("wrong file size")) {
				attempts = attempts + 1;
				Log.i("XMPPClient", "[FileTransfer] download failed - wrong file size, repeating attempt " + attempts);
				if (attempts < 20){
					downloadHandler.postDelayed(new downloadRunnable(msg), 15000);
				}	
			}
			if (result.equals("no connection")) {
				attempts = attempts + 1;
				Log.i("XMPPClient", "[FileTransfer] download failed - no connection, repeating attempt");
				if (attempts < 30){
					downloadHandler.postDelayed(new downloadRunnable(msg), 20000);
				}				
			}
			
			if (result.equals("done")) {
				attempts = 0;
				new blurBitmap().execute(msg); 				
			}			
		}			
	}
	
	public void setNotification(Message msg){
		final int YOUR_PI_REQ_CODE = 57;
		int YOUR_NOTIF_ID = 2;	
		

		
		String sender = StringUtils.parseName(msg.getFrom());
		String senderID = StringUtils.parseBareAddress(msg.getFrom()); 
		if (mSexPixApplication.connection!=null){
			if(mSexPixApplication.connection.getRoster()!=null){
				RosterEntry entry = mSexPixApplication.connection.getRoster().getEntry(senderID);
				if (entry != null){
					sender = entry.getName();
				}				
			}			
		}
		
		
		Intent showPicIntent = new Intent(mSexPixApplication.getApplicationContext(), ShowPicture.class); 
		
		ArrayList<Message> messages = mSexPixApplication.getMessages();
		if (messages.size()>1){
			showPicIntent = new Intent(mSexPixApplication.getApplicationContext(), SavedPix.class); 
		}
		
		
		
		showPicIntent.putExtra("messageID", msg.getPacketID());
		showPicIntent.putExtra("showConfiramtion", true);		
		showPicIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		PendingIntent pendingShowPicIntent = PendingIntent.
				getActivity(mSexPixApplication.getApplicationContext(), YOUR_PI_REQ_CODE, showPicIntent, PendingIntent.FLAG_UPDATE_CURRENT); 
		
		
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(mSexPixApplication.getApplicationContext()); 
		
		
		 
		builder.setContentIntent(pendingShowPicIntent) 
		            .setSmallIcon(R.drawable.ic_incoming_notification) 
		            .setLargeIcon(BitmapFactory.decodeResource(mSexPixApplication.getApplicationContext().getResources(), R.drawable.ic_instasnap)) 
		            .setTicker(mSexPixApplication.getResources().getString(R.string.str_new_photo)) 
		            .setWhen(System.currentTimeMillis()) 
		            .setAutoCancel(true) 
		            .setContentTitle(sender)
//		            .setSound(Uri.parse("android.resource://com.apparitionhq.instasnap/" + R.raw.default_sound))		            
		            .setContentText(mSexPixApplication.getResources().getString(R.string.str_new_photo)); 
		
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext()); 
    	int currentSound = preferences.getInt("optNotificationRingtoneWhich", 1);
    	if (currentSound != 0){
    		int[] itemsValues = new int[]{0,R.raw.default_sound,R.raw.bubble,R.raw.pulse};
    		builder.setSound(Uri.parse("android.resource://com.apparitionhq.instasnap/" + itemsValues[currentSound]));
    	}
    	
    	
    	
		Notification n = builder.build(); 
		
		mSexPixApplication.mNotificationManager.notify(YOUR_NOTIF_ID, n);
	}
	
	
	private class blurBitmap extends AsyncTask<Object, Void, String> {
		private Message msg = null;

		@Override
		protected String doInBackground(Object... params) {
			msg = (Message) params[0];
			
			String fName = (String) msg.getProperty("filename"); 
			
			Bitmap bitmap = null;

			try {				
				byte[] bitmapdata = Tools.readFile(mSexPixApplication.getFilesDir().toString() + "/" + fName);
				bitmapdata = Tools.decrypt(bitmapdata, StringUtils.parseName(msg.getFrom()));				

				BitmapFactory.Options options2 = new BitmapFactory.Options();
				options2.inSampleSize = 2;
				bitmap = BitmapFactory.decodeByteArray(bitmapdata , 0, bitmapdata .length, options2);
				
				if (bitmap != null){
					bitmap = boxBlur(bitmap,60);
					writeImageFile(bitmap, fName + "_blur");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return "done";	
		}
		
		protected void onPostExecute(String result) {
			if (result.equals("done")) {
				Intent intent = new Intent(ACTION_REFRESH_NEWMSG);
				LocalBroadcastManager.getInstance(mSexPixApplication.getApplicationContext()).sendBroadcast(intent);
				
				setNotification(msg);
			}
		}
		
		public void writeImageFile(Bitmap bitmap, String filename) {
			final int BUFFER_SIZE = 1024 * 8;
					
			FileOutputStream outputStream;
			
			try {
				outputStream = mSexPixApplication.openFileOutput(filename, Context.MODE_PRIVATE);
				
				final BufferedOutputStream bos = new BufferedOutputStream(outputStream,	BUFFER_SIZE);
				bitmap.compress(CompressFormat.JPEG, 70, bos);
				bos.flush();
				bos.close();
				outputStream.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
			}
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public static Bitmap boxBlur(Bitmap bmp, int range) { 
        assert (range & 1) == 0 : "Range must be odd."; 
     
   //     Bitmap blurred = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Config.ARGB_8888); 
        Bitmap blurred = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Config.RGB_565); 
        Canvas c = new Canvas(blurred); 
     
        int w = bmp.getWidth(); 
        int h = bmp.getHeight(); 
     
        int[] pixels = new int[bmp.getWidth() * bmp.getHeight()]; 
        bmp.getPixels(pixels, 0, w, 0, 0, w, h); 
     
        boxBlurHorizontal(pixels, w, h, range / 2); 
        boxBlurVertical(pixels, w, h, range / 2); 
     
        c.drawBitmap(pixels, 0, w, 0.0F, 0.0F, w, h, true, null); 
     
        return blurred; 
    } 
     
    private static void boxBlurHorizontal(int[] pixels, int w, int h, 
            int halfRange) { 
        int index = 0; 
        int[] newColors = new int[w]; 
     
        for (int y = 0; y < h; y++) { 
            int hits = 0; 
            long r = 0; 
            long g = 0; 
            long b = 0; 
            for (int x = -halfRange; x < w; x++) { 
                int oldPixel = x - halfRange - 1; 
                if (oldPixel >= 0) { 
                    int color = pixels[index + oldPixel]; 
                    if (color != 0) { 
                        r -= Color.red(color); 
                        g -= Color.green(color); 
                        b -= Color.blue(color); 
                    } 
                    hits--; 
                } 
     
                int newPixel = x + halfRange; 
                if (newPixel < w) { 
                    int color = pixels[index + newPixel]; 
                    if (color != 0) { 
                        r += Color.red(color); 
                        g += Color.green(color); 
                        b += Color.blue(color); 
                    } 
                    hits++; 
                } 
     
                if (x >= 0) { 
                	if(r<0)r=0;
                	if(g<0)g=0;
                	if(b<0)b=0;
                	newColors[x] = Color.argb(0xFF, (int) (r / hits), (int) (g / hits), (int) (b / hits));                     
                } 
            } 
     
            for (int x = 0; x < w; x++) { 
                pixels[index + x] = newColors[x]; 
            } 
     
            index += w; 
        } 
    } 
     
    private static void boxBlurVertical(int[] pixels, int w, int h, 
            int halfRange) { 
     
        int[] newColors = new int[h]; 
        int oldPixelOffset = -(halfRange + 1) * w; 
        int newPixelOffset = (halfRange) * w; 
     
        for (int x = 0; x < w; x++) { 
            int hits = 0; 
            long r = 0; 
            long g = 0; 
            long b = 0; 
            int index = -halfRange * w + x; 
            for (int y = -halfRange; y < h; y++) { 
                int oldPixel = y - halfRange - 1; 
                if (oldPixel >= 0) { 
                    int color = pixels[index + oldPixelOffset]; 
                    if (color != 0) { 
                        r -= Color.red(color); 
                        g -= Color.green(color); 
                        b -= Color.blue(color); 
                    } 
                    hits--; 
                } 
     
                int newPixel = y + halfRange; 
                if (newPixel < h) { 
                    int color = pixels[index + newPixelOffset]; 
                    if (color != 0) { 
                        r += Color.red(color); 
                        g += Color.green(color); 
                        b += Color.blue(color); 
                    } 
                    hits++; 
                } 
     
                if (y >= 0) { 
                	if(r<0)r=0;
                	if(g<0)g=0;
                	if(b<0)b=0;
                	newColors[y] = Color.argb(0xFF, (int) (r / hits), (int) (g / hits), (int) (b / hits));                  
                } 
     
                index += w; 
            } 
     
            for (int y = 0; y < h; y++) { 
                pixels[y * w + x] = newColors[y]; 
            } 
        } 
    } 
    
    


}
