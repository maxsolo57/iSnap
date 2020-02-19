package de.guitarcollege.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.apparitionhq.instasnap.SexPixApplication;


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

public class HockeyReport {
	public static String HockeyURLString = "https://sdk.hockeyapp.net/api/2/apps/04fb231f34cbf970d2294307b91c64fb/crashes/";

	public static String APP_VERSION = null;
	public static String APP_PACKAGE = null;
	public static String ANDROID_VERSION = null;
	public static String PHONE_MODEL = null;
	public static String PHONE_MANUFACTURER = null;
	public static SexPixApplication mSexPixApplication;

	public static void loadFromContext(Context context){
		ANDROID_VERSION = Build.VERSION.RELEASE;
		PHONE_MODEL = Build.MODEL;
		PHONE_MANUFACTURER = Build.MANUFACTURER;
		
		try {
			mSexPixApplication = (SexPixApplication) context;	
			HockeyURLString = "https://sdk.hockeyapp.net/api/2/apps/" + mSexPixApplication.HockeyApp_KEY + "/crashes/";
		}
		catch (Exception e){			
		}
				

		PackageManager packageManager = context.getPackageManager();
		try {
			PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
			APP_VERSION = "" + packageInfo.versionCode;
			APP_PACKAGE = packageInfo.packageName;
		}
		catch (Exception e) {
			Log.e("XMPPClient", "Exception thrown when accessing the package info:");
			e.printStackTrace();
		}

	}
	  

	public static void submitStackTraces(Context context, String description)
	{
		Log.w("XMPPClient", "[HockeyReport]");

		try {
			
			loadFromContext(context);
			
			
			Date now = new Date();
			
			
			StringBuilder contents = new StringBuilder();

			contents.append("Package: " + APP_PACKAGE);	contents.append(System.getProperty("line.separator"));
			contents.append("Version: " + APP_VERSION);	contents.append(System.getProperty("line.separator"));
			contents.append("Android: " + ANDROID_VERSION);	contents.append(System.getProperty("line.separator"));
			contents.append("Manufacturer: " + PHONE_MANUFACTURER);	contents.append(System.getProperty("line.separator"));
			contents.append("Model: " + PHONE_MODEL);	contents.append(System.getProperty("line.separator"));
			contents.append("Date: " + now);	contents.append(System.getProperty("line.separator"));
			contents.append("");	contents.append(System.getProperty("line.separator"));
			contents.append(description);	contents.append(System.getProperty("line.separator"));
			
			Process process = Runtime.getRuntime().exec("logcat -d XMPPClient:I *:S");
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));

			
			String line = "";
			while ((line = bufferedReader.readLine()) != null) {
				contents.append(line); contents.append(System.getProperty("line.separator"));
				
			}
			
			String report = contents.toString();
	
			new sendHockeyReport().execute(report);	
			
		} 
		catch (Exception e) {}		
	}
	
	public static class sendHockeyReport extends AsyncTask<String, String, String> {
		@Override
    	protected String doInBackground(String... params) {			
			
			String report = (String) params[0];
		
			try
			{

				DefaultHttpClient httpClient = new DefaultHttpClient();
				HttpPost httpPost = new HttpPost(HockeyURLString);

				List parameters = new ArrayList();
				parameters.add(new BasicNameValuePair("raw", report));
				parameters.add(new BasicNameValuePair("userID", ""));
				parameters.add(new BasicNameValuePair("contact", ""));
				parameters.add(new BasicNameValuePair("description", ""));
				parameters.add(new BasicNameValuePair("sdk", "HockeySDK"));
				parameters.add(new BasicNameValuePair("sdk_version", "2.2.0"));

				httpPost.setEntity(new UrlEncodedFormEntity(parameters, "UTF-8"));
				httpClient.execute(httpPost);
				
				Log.w("XMPPClient", "[HockeyReport] report sent");
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		
			return null;
		}
	}


}
