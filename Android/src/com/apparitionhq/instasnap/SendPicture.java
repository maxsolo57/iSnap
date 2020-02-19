package com.apparitionhq.instasnap;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

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
import com.flurry.android.FlurryAgent;
import com.revmob.RevMob;

import de.guitarcollege.utils.Base64;
import de.guitarcollege.utils.Tools;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Matrix;
import android.text.format.Time;
//import android.util.Log;
import com.apphance.android.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;

public class SendPicture extends Activity {
	private SexPixApplication mSexPixApplication;
	private String chosenUser;
	public final String filename = "sptemp.jpg";
	public String thumbStorage = "thumbs.bin";
	public final int scaleHeight = 800;
	public final int scaleThumbHeight = 100;
	public final static String ACTION_REFRESH_CONTACTLIST = "com.apparitionhq.instasnap.action.REFRESH_CONTACTLIST";
	
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
		Log.w("XMPPClient", "[SendPicture] System.gc()");
		System.gc();
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_picture);   
        
        mSexPixApplication = (SexPixApplication) getApplication();
        Intent intent = getIntent(); 
        chosenUser = intent.getStringExtra("chosenUser"); 
        
        ((Button) findViewById(R.id.but_send)).setEnabled(false);
        
        new PictureResizeTask().execute(""); 
        
    }
    
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getWindow().setBackgroundDrawable(getResources().getDrawable( R.drawable.bg_dark ));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_send_picture, menu);
        return true;
    }
    
    public void showAd(){
    	if (!mSexPixApplication.upgrade){
    		
    		
    		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext()); 
        	SharedPreferences.Editor editor = preferences.edit();
        	int countFsAd = preferences.getInt("ad_picture_sent_frequency", 1);  
        	
        	Log.i("XMPPClient", "[SendPicture] trying to show full screen ad:" + mSexPixApplication.ad_picture_sent_provider + ", " + countFsAd + "/" + mSexPixApplication.ad_picture_sent_frequency);
        	boolean adShown = false;
        	
        	
        	if (countFsAd >= mSexPixApplication.ad_picture_sent_frequency){
        		
        		if (mSexPixApplication.ad_picture_sent_provider.equals("flurry")){
//        			FrameLayout container = new FrameLayout(mSexPixApplication);
//        		//	adShown = FlurryAgent.getAd(mSexPixApplication, "InstaSnap_ad_picture_sent", container, FlurryAdSize.FULLSCREEN, 3000);
//        			
//        			try {
//        				FlurryAgent.fetchAd(this, "InstaSnap_ad_picture_sent", container, FlurryAdSize.FULLSCREEN);
//            			FlurryAgent.displayAd(this, "InstaSnap_ad_picture_sent", container);
//        			}
//        			catch (Exception e){       				
//        			}
//        			     			
//        			adShown = true;
//        			
//        			Log.i("XMPPClient", "[SendPicture] showing Flurry full screen ad");

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
    
    @Override 
    protected void onResume() { 
    	super.onResume();
    	
    	mSexPixApplication.setBanner(this);
    }
	
	@Override
    public void onPause() {
        super.onPause();
        mSexPixApplication.removeBanner(this);
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    
	public void retake(View view) {
		
		Bitmap toRecycle = mSexPixApplication.photo;
		mSexPixApplication.photo = null;
		toRecycle.recycle();
		finish();
	}
	

	public void sendPic(View view) {
		new sendFileTask().execute("");	
		FlurryAgent.logEvent("Picture sent");
	}
	
	public void writeImageFile(Bitmap bitmap) {
		final int BUFFER_SIZE = 1024 * 8;
		
		String filename = "sptemp.jpg";
		FileOutputStream outputStream;
		
		try {
			outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
			
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

	
	public void rotateLeft(View view) {		
		Matrix matrix = new Matrix();		
		matrix.postRotate(-90); 
		Bitmap toRecycle = mSexPixApplication.photo;
		Bitmap rotated = Bitmap.createBitmap(mSexPixApplication.photo, 0, 0,  
				mSexPixApplication.photo.getWidth(), mSexPixApplication.photo.getHeight(), matrix, true);
		mSexPixApplication.photo = rotated;
		if (mSexPixApplication.photo != toRecycle){
			toRecycle.recycle();
		}
		((ImageView) findViewById(R.id.picToSend)).setImageBitmap(mSexPixApplication.photo); 	
		
	}
	
	public void rotateRight(View view) {		
		Matrix matrix = new Matrix();		
		matrix.postRotate(90); 
		Bitmap toRecycle = mSexPixApplication.photo;
		Bitmap rotated = Bitmap.createBitmap(mSexPixApplication.photo, 0, 0,  
				mSexPixApplication.photo.getWidth(), mSexPixApplication.photo.getHeight(), matrix, true);
		mSexPixApplication.photo = rotated;
		if (mSexPixApplication.photo != toRecycle){
			toRecycle.recycle();
		}
		((ImageView) findViewById(R.id.picToSend)).setImageBitmap(mSexPixApplication.photo); 	
	
	}
	
	
	private class PictureResizeTask extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {
			publishProgress("processing");
			
			int h;
			int w;
			
			if (mSexPixApplication.photo == null){
				return "error";
			}
			
			Bitmap toRecycle = mSexPixApplication.photo;
			
			if(mSexPixApplication.photo.getHeight() > mSexPixApplication.photo.getWidth())
			{
				h = mSexPixApplication.photo.getHeight();
				w = mSexPixApplication.photo.getWidth();
			} else {
				w = mSexPixApplication.photo.getHeight();
				h = mSexPixApplication.photo.getWidth();
			}		
			
			
			try{
				if (scaleHeight < h){
					float aspect = (float)h / w;
					float newHeight = scaleHeight;
					float newWidth = newHeight/aspect;	
					if(mSexPixApplication.photo.getHeight() > mSexPixApplication.photo.getWidth())
					{
						mSexPixApplication.photo = Bitmap.createScaledBitmap(mSexPixApplication.photo, (int)newWidth, (int)newHeight, true);
					} else {
						mSexPixApplication.photo = Bitmap.createScaledBitmap(mSexPixApplication.photo, (int)newHeight, (int)newWidth, true);
					}				
				}
				
				if (mSexPixApplication.photo != toRecycle){
					toRecycle.recycle();
				}
				
				
							
				if (mSexPixApplication.photo.getWidth() > mSexPixApplication.photo.getHeight()) {
					Bitmap toRecycle2 = mSexPixApplication.photo;
					Matrix matrix = new Matrix();	
					matrix.postRotate(90); 
					mSexPixApplication.photo = Bitmap.createBitmap(mSexPixApplication.photo, 0, 0,  
							mSexPixApplication.photo.getWidth(), mSexPixApplication.photo.getHeight(), matrix, true);
					if (mSexPixApplication.photo != toRecycle2){
						toRecycle2.recycle();
					}
				}
				
			} catch (OutOfMemoryError e){
				Toast.makeText(mSexPixApplication, R.string.str_out_of_memory, Toast.LENGTH_SHORT).show();
			} catch (Exception e){
				
			}
			
			
			
			return "done";
		}
		
		@Override
		protected void onPostExecute(String result) {
			((ImageView) findViewById(R.id.picToSend)).setImageBitmap(mSexPixApplication.photo);  
	     
	        ((Button) findViewById(R.id.but_send)).setEnabled(true);
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			Toast.makeText(mSexPixApplication, R.string.str_processing, Toast.LENGTH_SHORT).show();
		}
	}
	
	public void saveThumbnail(String fileName, int fSize){
		
		final int BUFFER_SIZE = 1024 * 8;
		
		Log.i("XMPPClient", "[saveThumbnail] file:" + fileName);
		float aspect = (float)mSexPixApplication.photo.getHeight() / mSexPixApplication.photo.getWidth();
		float newHeight = scaleThumbHeight;
		float newWidth = newHeight/aspect;			
		Bitmap thumbnail = Bitmap.createScaledBitmap(mSexPixApplication.photo, (int)newWidth, (int)newHeight, true);
		
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
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		thumbnail.recycle();
		thumbnail = null;
		outputStream = null;
		System.gc();
		
		
		// read thumbs from file 
		HashMap<String,ArrayList<HashMap<String,String>>> allRecent;
		
		FileInputStream fis;
		File thumbStorageFile = new File(mSexPixApplication.getFilesDir().toString() + "/" + thumbStorage);
		
		if (thumbStorageFile.exists()) {

			try {
				fis = openFileInput(thumbStorage);
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
			outStream = openFileOutput(thumbStorage, Context.MODE_PRIVATE);
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
	
	
	private class sendFileTask extends AsyncTask<String, String, String> {
		String fName = "";

		@Override
		protected String doInBackground(String... params) {	
			Thread.currentThread().setName("sendFileTask");
			publishProgress("sending");
			
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
			mSexPixApplication.photo.compress(Bitmap.CompressFormat.JPEG, 70, bao);
			byte[] ba = bao.toByteArray();	
			
			bao = null;
			System.gc();
			
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
			setResult(Activity.RESULT_OK);
			
			Intent intent = new Intent(ACTION_REFRESH_CONTACTLIST);
			LocalBroadcastManager.getInstance(mSexPixApplication.getApplicationContext()).sendBroadcast(intent);
			
			
			
			finish();			
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
				setResult(Activity.RESULT_OK);
				
				showAd();
				
			
				finish();				
			}
			
			if (values[0].equals("updateList")){
				Intent intent = new Intent(ACTION_REFRESH_CONTACTLIST);
				LocalBroadcastManager.getInstance(mSexPixApplication.getApplicationContext()).sendBroadcast(intent);				
			}			
		}
		
		public void setTransferStatus(String newStatus){
			// read thumbs from file 
			HashMap<String,ArrayList<HashMap<String,String>>> allRecent;
			
			FileInputStream fis;
			File thumbStorageFile = new File(mSexPixApplication.getFilesDir().toString() + "/" + thumbStorage);
			
			if (thumbStorageFile.exists()) {

				try {
					fis = openFileInput(thumbStorage);
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
				outStream = openFileOutput(thumbStorage, Context.MODE_PRIVATE);
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
