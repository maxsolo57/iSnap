package com.apparitionhq.instasnap;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
//import android.util.Log;
import com.apphance.android.Log;
import android.util.Xml;

public class AdSettingsRequest {
	private SexPixApplication mSexPixApplication;

	public AdSettingsRequest(SexPixApplication theApp) {
		mSexPixApplication = theApp;

	}
	
	public void run(){
		SettingsRequest sreq = new SettingsRequest();
		sreq.execute(); 
	}




	public class SettingsRequest extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {


			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
			HttpConnectionParams.setSoTimeout(httpParameters, 5000);

			HttpClient httpclient = new DefaultHttpClient(httpParameters);
			HttpPost httppost = new HttpPost(mSexPixApplication.strAdSettingsAddress);
			HttpResponse response;	

			try {
				response = httpclient.execute(httppost);			
				HttpEntity entity = response.getEntity();			
				parse(entity.getContent());		

			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}}
	

	public void parse(InputStream in) throws XmlPullParserException, IOException {
		
		try {

			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(in, null);

			Log.w("XMPPClient", "Parsing XML");
			while (parser.nextTag() != XmlPullParser.END_DOCUMENT){
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				if ((""+parser.getName()).equals("Placeholder") && parser.getAttributeValue(null, "name") != null){
					String name = parser.getAttributeValue(null, "name");
					String provider = parser.getAttributeValue(null, "provider");
					int frequency = 5;
					try {
						frequency = Integer.parseInt(parser.getAttributeValue(null, "frequency"));
					} catch (Exception e){}
					
					
					// Set values		
					if (name.equals("ad_banner_bottom")) {
						if (provider.equals("revmob") || provider.equals("admob")){
							mSexPixApplication.ad_banner_bottom_provider = provider;
							Log.i("XMPPClient", "Placeholder " + name + ":" + provider);
						}						
					}
					
					if (name.equals("ad_app_closed")) {
						if (provider.equals("revmob")){
							mSexPixApplication.ad_app_closed_provider = provider;
							mSexPixApplication.ad_app_closed_frequency = frequency;
							Log.i("XMPPClient", "Placeholder " + name + ":" + provider + " - " + frequency);
						}						
					}
					
					if (name.equals("ad_picture_sent")) {
						if (provider.equals("revmob")){
							mSexPixApplication.ad_picture_sent_provider = provider;
							mSexPixApplication.ad_picture_sent_frequency = frequency;
							Log.i("XMPPClient", "Placeholder " + name + ":" + provider + " - " + frequency);
						}						
					}
					
					if (name.equals("ad_registration_completed")) {
						if (provider.equals("revmob")){
							mSexPixApplication.ad_registration_completed_provider = provider;							
							Log.i("XMPPClient", "Placeholder " + name + ":" + provider);
						}						
					}
					
					if (name.equals("lockedpic_screen")) {
						mSexPixApplication.lockedpic_screen_frequency = frequency;							
						Log.i("XMPPClient", "Placeholder " + name + ":" + frequency);

					}
					
					if (name.equals("upgrade_screen")) {
						mSexPixApplication.upgrade_screen_frequency = frequency;							
						Log.i("XMPPClient", "Placeholder " + name + ":" + frequency);

					}
				}  				
				
				
				if ((""+parser.getName()).equals("CurrentAppVersion") && parser.getAttributeValue(null, "version") != null){
					try {
						int verInt = Integer.parseInt(parser.getAttributeValue(null, "version"));
						String validUntil = parser.getAttributeValue(null, "date");
											
						SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
						Date strDate = sdf.parse(validUntil);
						boolean versionIsValid = true;
						if (System.currentTimeMillis() > strDate.getTime()) {
							versionIsValid = false;
						}
						
						SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext());
				        SharedPreferences.Editor editor = preferences.edit();
				        editor.putInt("currentAppVersion", verInt);
				        editor.putString("appValidUntil", validUntil);
				        editor.commit();
						
						
						Log.i("XMPPClient", "CurrentAppVersion " + verInt + ", since " + validUntil + ",   valid = " + versionIsValid + "   strDate:" + strDate.toString());
					} catch (Exception e){}									
				}
			}

		} finally {
			in.close();
		}
	}

}
