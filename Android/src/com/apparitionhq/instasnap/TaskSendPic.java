package com.apparitionhq.instasnap;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import com.apparitionhq.instasnap.listeners.Ping;
import com.apphance.android.Log;

import de.guitarcollege.utils.Base64;
import de.guitarcollege.utils.Tools;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.widget.Toast;

public class TaskSendPic {
	
	private SexPixApplication mSexPixApplication;
	public final int scaleThumbHeight = 90;
	public String thumbStorage = "thumbs.bin";
	private String chosenUser;
	public final static String ACTION_REFRESH_CONTACTLIST = "com.apparitionhq.instasnap.action.REFRESH_CONTACTLIST";

	public TaskSendPic(SexPixApplication appLink, String chUser) {
		mSexPixApplication = appLink;
		chosenUser = chUser;
		
		new sendFileTask().execute("");	
	}
	
	
	
	
	
	
	
	
	
	
	private class sendFileTask extends AsyncTask<String, String, String> {
		String fName = "";
	
		@Override
		protected String doInBackground(String... params) {	
			Thread.currentThread().setName("sendFileTask");
			publishProgress("sending");
			
			File outFile = new File(mSexPixApplication.getFilesDir().toString() + "/feather_out.jpg");	
			
			byte[] ba;
			
			try {
				ba = FileUtils.readFileToByteArray(outFile);
			} catch (IOException e) {
				Log.w("XMPPClient", "[TaskSendPic] failed to read feather_out file");
				return "error";
			}		
			
			
			ba = Tools.encrypt(ba, mSexPixApplication.strUserLogin);
			int baLength = ba.length;
			String ba1 = Base64.encodeBytes(ba);
			
			ba = null;
			System.gc();
			
			
			fName = Tools.randomString(16);
			saveThumbnail(fName, baLength);
			
			publishProgress("updateList");
			
			ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("filename", fName));
			nameValuePairs.add(new BasicNameValuePair("receiver", StringUtils.parseName(chosenUser)));
			nameValuePairs.add(new BasicNameValuePair("file", ba1));
			
			try{
				HttpClient httpclient = new DefaultHttpClient();
				HttpPost httppost = new HttpPost(mSexPixApplication.strServerPutAddress);
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));			
				
				HttpResponse response = httpclient.execute(httppost);
				HttpEntity entity = response.getEntity();
				int sizeOnServer = Integer.parseInt(EntityUtils.toString(entity)); 
				
				
				ba1 = null;
				entity = null;
				response = null;
				httppost = null;
				httpclient = null;
				
				System.gc();
				
				if(baLength != sizeOnServer){
					return "error";
				}
							
			}catch(Exception e){
				e.printStackTrace();
				return "no connection";
			}
			
			
			Message iSPacket = new Message(chosenUser, Message.Type.normal);
    		iSPacket.setPacketID("InstaSnapFile-" + Tools.randomString(8));
    		iSPacket.setBody("_"+fName + "_ISFile");
    		iSPacket.setProperty("filename", fName);
    		iSPacket.setProperty("size", baLength);
    		Time now = new Time();
    		now.setToNow();
    		iSPacket.setProperty("date", now.toMillis(true));
    		
    		try {
    			// if user is offline then send packet right now
    			if(mSexPixApplication.connection.getRoster() != null){
    				Presence cPresence = mSexPixApplication.connection.getRoster().getPresence(chosenUser);
    				if((cPresence == null) || (cPresence.getType() == Presence.Type.unavailable)){
    					mSexPixApplication.connection.sendPacket(iSPacket);  
    					Log.i("XMPPClient", "[SendPicture] file sent to " + StringUtils.parseName(iSPacket.getTo()));
    					return "done";
    				}
    			}	
    			
    			// if user is online, send ping first and then send the packet
    			Ping ping = new Ping(mSexPixApplication.connection, chosenUser);
    			ping.sendPing();
    			mSexPixApplication.connection.sendPacket(iSPacket);
    			Log.i("XMPPClient", "[SendPicture] file sent to " + StringUtils.parseName(iSPacket.getTo()));
 			
    			return "done";
    		}
    		catch (Exception e){
    			e.printStackTrace();
				return "no connection";    			
    		}				
		}
		
		
		
		@Override
		protected void onPostExecute(String result) {
			if (result.equals("error")) {
				Toast.makeText(mSexPixApplication, R.string.str_connectionError, Toast.LENGTH_SHORT).show();
			}
			if (result.equals("no connection")) {
				Toast.makeText(mSexPixApplication, R.string.str_connectionError, Toast.LENGTH_SHORT).show();
			}
			if (result.equals("user offline")) {
				Toast.makeText(mSexPixApplication, R.string.str_user_offline, Toast.LENGTH_SHORT).show();
			}
			if (result.equals("done")) {		
			//	String userName = mSexPixApplication.connection.getRoster().getEntry(chosenUser).getName();
			//	Toast.makeText(mSexPixApplication.getApplicationContext(), getResources().getString(R.string.str_pic_sent) + " " + userName, Toast.LENGTH_SHORT).show();				
				setTransferStatus("uploaded");				
			}
			else {
				setTransferStatus("error");
			}
			
			
			Intent intent = new Intent(ACTION_REFRESH_CONTACTLIST);
			LocalBroadcastManager.getInstance(mSexPixApplication.getApplicationContext()).sendBroadcast(intent);				
		}
		
		
		@Override
		protected void onProgressUpdate(String... values) {	
			if (values[0].equals("sending")){
				String userName = null;
				if (mSexPixApplication.connection != null){
					if (mSexPixApplication.connection.getRoster()!=null){
						if(mSexPixApplication.connection.getRoster().getEntry(chosenUser)!=null){							
							userName = mSexPixApplication.connection.getRoster().getEntry(chosenUser).getName();
						}						
					}
				}
				if (userName == null){
					userName = StringUtils.parseName(chosenUser);
				}
				
				Toast.makeText(mSexPixApplication, mSexPixApplication.getResources().getString(R.string.str_sending_picture) + " " + userName, Toast.LENGTH_SHORT).show();

				
				showAd();
				
				
			}
			
			if (values[0].equals("updateList")){
				Intent intent = new Intent(ACTION_REFRESH_CONTACTLIST);
				LocalBroadcastManager.getInstance(mSexPixApplication.getApplicationContext()).sendBroadcast(intent);				
			}			
		}
		
		
		
	    public void showAd(){
	    	if (!mSexPixApplication.upgrade){
	    		
	    		
	    		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext()); 
	        	SharedPreferences.Editor editor = preferences.edit();
	        	int countFsAd = preferences.getInt("ad_picture_sent_frequency", 1);  
	        	
	        	Log.i("XMPPClient", "[TaskSendPic] trying to show full screen ad:" + mSexPixApplication.ad_picture_sent_provider + ", " + countFsAd + "/" + mSexPixApplication.ad_picture_sent_frequency);
	        	boolean adShown = false;
	        	
	        	
	        	if (countFsAd >= mSexPixApplication.ad_picture_sent_frequency){
	        		
	        		if (mSexPixApplication.ad_picture_sent_provider.equals("flurry")){

	        		} else if (mSexPixApplication.ad_picture_sent_provider.equals("revmob")){
	        			if (mSexPixApplication.ad_picture_sent != null && mSexPixApplication.ad_picture_sent.isAdLoaded()){
	        				mSexPixApplication.ad_picture_sent.show();   
	        				adShown = true;
	        				Log.i("XMPPClient", "[SendPicture] showing RevMob full screen ad");	
	        			}
	        		}
	        	}
	        	if (adShown){
	    			editor.putInt("ad_picture_sent_frequency", 1);
	    			editor.commit();
	    		} else {
	    			editor.putInt("ad_picture_sent_frequency", countFsAd+1);
	    			editor.commit();
	    		}
	        }
	    	
	    }
		
		
		
		
		public void setTransferStatus(String newStatus){
			// read thumbs from file 
			HashMap<String,ArrayList<HashMap<String,String>>> allRecent;
			
			FileInputStream fis;
			File thumbStorageFile = new File(mSexPixApplication.getFilesDir().toString() + "/" + thumbStorage);
			
			if (thumbStorageFile.exists()) {

				try {
					fis = mSexPixApplication.openFileInput(thumbStorage);
					ObjectInputStream is = new ObjectInputStream(fis); 				
					allRecent = (HashMap<String,ArrayList<HashMap<String,String>>>) is.readObject(); 
					is.close(); 
					fis.close();
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
			else {
				return;		
			}
			
			
			// get entries for chosenUser		
			ArrayList<HashMap<String,String>> recentEntries = allRecent.get(StringUtils.parseName(chosenUser));		
			if (recentEntries == null){
				return;	
			}			
			
			// update status
			if (recentEntries.size() > 0){
				for(HashMap<String,String> recEntry : recentEntries){
					String recFile = recEntry.get("filename");
					if (recFile.equals(fName)){
						String recSt = recEntry.get("status");
						if (!recSt.equals("packetreceived") && !recSt.equals("received")){
							recEntry.put("status", newStatus);
						}												
					}
				}
			}	
			
			// put everything back		
			allRecent.put(StringUtils.parseName(chosenUser), recentEntries);
			
			// save all thumbs to file
			
			FileOutputStream outStream;		
			
			try {
				outStream = mSexPixApplication.openFileOutput(thumbStorage, Context.MODE_PRIVATE);
				ObjectOutputStream os = new ObjectOutputStream(outStream); 
				os.writeObject(allRecent); 
				os.close(); 
				outStream.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}	
			
		}
		
		
		
		
		public void saveThumbnail(String fileName, int fSize){
			
			final int BUFFER_SIZE = 1024 * 8;
			
			String sourceFile = mSexPixApplication.getFilesDir().toString() + "/feather_out.jpg";
			
			BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
			bitmapOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(sourceFile, bitmapOptions);
			int imageWidth = bitmapOptions.outWidth;
			int imageHeight = bitmapOptions.outHeight;	
			
			
			bitmapOptions = new BitmapFactory.Options();
			bitmapOptions.inSampleSize = Math.max(imageWidth/scaleThumbHeight, imageHeight/scaleThumbHeight);
			
			Bitmap roughBitmap = BitmapFactory.decodeFile(sourceFile, bitmapOptions);			
			
			Log.i("XMPPClient", "[saveThumbnail] file:" + fileName);
			float aspect = (float)roughBitmap.getHeight() / (float)roughBitmap.getWidth();
			float newHeight = scaleThumbHeight;
			float newWidth = newHeight/aspect;								
			
			Bitmap thumbnail = Bitmap.createScaledBitmap(roughBitmap, (int)newWidth, (int)newHeight, true);
			
			roughBitmap.recycle();
			System.gc();
			
			FileOutputStream outputStream;
			try {
				
				
				File folder = new File(mSexPixApplication.getFilesDir().toString() + "/thumbnails");	
				if (!folder.exists()) {
					Log.i("XMPPClient", "[saveThumbnail] creating folder:" + folder.getAbsolutePath());
					folder.mkdir();
				}

				File newFile = new File(mSexPixApplication.getFilesDir().toString() + "/thumbnails/" + fileName + ".jpg");	

				outputStream = new FileOutputStream(newFile);

				final BufferedOutputStream bos = new BufferedOutputStream(outputStream,	BUFFER_SIZE);
				thumbnail.compress(CompressFormat.JPEG, 65, bos);
				bos.flush();
				bos.close();
				outputStream.close();

				thumbnail.recycle();
				thumbnail = null;
				outputStream = null;
				System.gc();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}



			
			// read thumbs from file 
			HashMap<String,ArrayList<HashMap<String,String>>> allRecent;
			
			FileInputStream fis;
			File thumbStorageFile = new File(mSexPixApplication.getFilesDir().toString() + "/" + thumbStorage);
			
			if (thumbStorageFile.exists()) {

				try {
					fis = mSexPixApplication.openFileInput(thumbStorage);
					ObjectInputStream is = new ObjectInputStream(fis); 				
					allRecent = (HashMap<String,ArrayList<HashMap<String,String>>>) is.readObject(); 
					is.close(); 
					fis.close();
					
				} catch (FileNotFoundException e) {
					allRecent = new HashMap<String,ArrayList<HashMap<String,String>>>();
					e.printStackTrace();
				} catch (IOException e) {
					allRecent = new HashMap<String,ArrayList<HashMap<String,String>>>();
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					allRecent = new HashMap<String,ArrayList<HashMap<String,String>>>();
					e.printStackTrace();
				}
			}
			else {
				allRecent = new HashMap<String,ArrayList<HashMap<String,String>>>();			
			}
			
			
			// get entries for chosenUser		
			ArrayList<HashMap<String,String>> recentEntries = allRecent.get(StringUtils.parseName(chosenUser));		
			if (recentEntries == null){
				recentEntries = new ArrayList<HashMap<String,String>>();
			}
			
			// TODO check if ACK received
			// delete old thumbs   -> keep history in in the new version of the app		
			if (recentEntries.size() > 0){
				File newFile;
				for(HashMap<String,String> recEntry : recentEntries){
					String fileToDelete = recEntry.get("filename");
					newFile = new File(mSexPixApplication.getFilesDir().toString() + "/thumbnails/" + fileToDelete + ".jpg");
					if (newFile.exists()){
						newFile.delete();
					}
				}
				recentEntries.clear();
			}	
			
			// create new recent entry	
			HashMap<String,String> newRecent = new HashMap<String,String>();
			newRecent.put("filename", fileName);
			newRecent.put("status", "upload");
			newRecent.put("size", "" + fSize);
			
			// put everything back		
			recentEntries.add(newRecent);	
			allRecent.put(StringUtils.parseName(chosenUser), recentEntries);
			
			// save all thumbs to file
			
			FileOutputStream outStream;		
			
			try {
				outStream = mSexPixApplication.openFileOutput(thumbStorage, Context.MODE_PRIVATE);
				ObjectOutputStream os = new ObjectOutputStream(outStream); 
				os.writeObject(allRecent); 
				os.close(); 
				outStream.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
		
		
		
	}
	

}
