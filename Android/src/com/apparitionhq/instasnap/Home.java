package com.apparitionhq.instasnap;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import net.hockeyapp.android.UpdateManager;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import de.guitarcollege.custom.dialog.CustomAlertDialog;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.flurry.android.FlurryAgent;
import com.google.analytics.tracking.android.EasyTracker;

import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;

import com.apphance.android.Apphance;
//import android.util.Log;
import com.apphance.android.Log;
import com.apptentive.android.sdk.Apptentive;
import com.apptentive.android.sdk.ApptentiveActivity;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.Toast;

import android.widget.ListView;
import android.widget.TextView;

public class Home extends ApptentiveActivity {
	
	private SexPixApplication mSexPixApplication;
	private final ContactList mAdapterContactList = new ContactList();	
	private LayoutInflater mInflater;
	final Handler mHandler = new Handler(); 
	private static final int STATUS_REQUEST_CODE = 6;
	private static final Random rgenerator = new Random(); 
	private boolean showHistory = true;
	public static final int IMEI_REQUEST_CODE = 576;
	
	private String thumbStorage = "thumbs.bin";	
	private HashMap<String,ArrayList<HashMap<String,String>>> allRecent;
	public final static String ACTION_REFRESH_CONTACTLIST = "com.apparitionhq.instasnap.action.REFRESH_CONTACTLIST";
	public final static String ACTION_REFRESH_NEWMSG = "com.apparitionhq.instasnap.action.REFRESH_NEWMSG";
	public final static String ACTION_WRONG_PASS = "com.apparitionhq.instasnap.action.WRONG_PASS";
	
	
	final Runnable mUpdateContactList = new Runnable() { 
        public void run() {    
        //	Log.i("XMPPClient", "[Home - mUpdateContactList] refresh contact list runnable");
        	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext()); 
        	showHistory = preferences.getBoolean("optShowHistory", true);   
        	readRecents();
        	
        	int rosterSize = preferences.getInt("roster_size", 0);
        	
        	mAdapterContactList.notifyDataSetChanged();   
        	ListView homeList = (ListView) findViewById(R.id.homeContactList); 
        	TextView emptyList = (TextView) findViewById(R.id.textViewEmptyList);
        	if (mSexPixApplication.connection == null || mSexPixApplication.connection.getRoster() == null){
        		emptyList.setVisibility(TextView.GONE);
        		homeList.setVisibility(ListView.VISIBLE);
            	homeList.setBackgroundResource(R.drawable.connecting_no_stretch);
            } else if (mSexPixApplication.connection != null && mSexPixApplication.connection.getRoster() != null && rosterSize == 0){
            //	homeList.setBackgroundResource(R.drawable.home_bg_no_stretch);
            	emptyList.setVisibility(TextView.VISIBLE);
            	homeList.setVisibility(ListView.GONE);
            }
        	
        	else {
        		emptyList.setVisibility(TextView.GONE);
        		homeList.setVisibility(ListView.VISIBLE);
            	homeList.setBackgroundResource(0);
            }
        	
        	homeList.invalidateViews();
        } 
    }; 
    
    final Runnable mUpdateNewMessages = new Runnable() { 
        public void run() {         	
        	new refreshNewMessagesTask().execute(); 
        } 
    }; 
    
    final Runnable mWrongPass = new Runnable() { 
        public void run() {           	
        	Intent i = new Intent(Home.this, AccountProblem.class);
		    startActivityForResult(i, IMEI_REQUEST_CODE); 
        } 
    };
    
	private BroadcastReceiver mRefreshCLReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getBooleanExtra("refreshList", false) && (mSexPixApplication.connection.getRoster()!=null)){
				mSexPixApplication.mContacts = new ArrayList<RosterEntry>(mSexPixApplication.connection.getRoster().getEntries());
			}			
		//	Log.i("XMPPClient", "[Home - mMessageReceiver] refresh intent received");
			mHandler.post(mUpdateContactList);			
		}
	};
	
	private BroadcastReceiver mUpdateNumberNewMsgReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mHandler.post(mUpdateNewMessages);			
		}
	};
    
	private BroadcastReceiver mWrongPassReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mHandler.post(mWrongPass);			
		}
	};
	
	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this); 
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
		UpdateManager.unregister();
	}
	
    public void readRecents(){
    	// read allRecent from file
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
				allRecent = new HashMap<String,ArrayList<HashMap<String,String>>>();
				e.printStackTrace();
			}
		}
		else {
			allRecent = new HashMap<String,ArrayList<HashMap<String,String>>>();			
		} 	
    }
    
    private class refreshNewMessagesTask extends AsyncTask<Void, Void, String> {
    	public int numberOfMessages = 0;
		
		@Override
		protected String doInBackground(Void... params) {
			ArrayList<Message> messages = mSexPixApplication.getMessages();
	        
	        numberOfMessages = 0;
	        File file;
	        for (Message msg : messages) {
	        	String fName = (String) msg.getProperty("filename"); 
	        	file = new File(mSexPixApplication.getFilesDir().toString() + "/" + fName + "_blur"); 
	        	if (file.exists()){
	        		numberOfMessages ++;
	        	}	
	        }	
			return "done";
		}
		
		protected void onPostExecute(String result) {
			if (result.equals("done")) {
				TextView tv = (TextView) findViewById(R.id.txt_newmsg);
		        if (numberOfMessages>0){      	
		        	tv.setText("" + numberOfMessages);
		        	tv.setVisibility(TextView.VISIBLE);
		        } else {
		        	tv.setText("");
		        	tv.setVisibility(TextView.INVISIBLE);
		        }
			}
		}
	}
    
    public String randomString(int length) {
		String source = "qazwsxedcrfvtgbyhnujmikolpQAZWSXEDCRFVTGBYHNUJMIKOLP1234567890";
		String result = "";
		for (int idx = 0; idx < length; ++idx)					
			result = result + source.charAt(rgenerator.nextInt(source.length()));
		return result;
	}
    
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        (findViewById(R.id.homeContactListContainer)).setBackgroundDrawable(getResources().getDrawable( R.drawable.bg_bright));
        
    }

    
    
    // on Back button
    @Override 
    public boolean onKeyDown(int keyCode, KeyEvent event)  { 
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) { 

        	if (!mSexPixApplication.upgrade){
        		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext()); 
            	SharedPreferences.Editor editor = preferences.edit();
            	int countFsAd = preferences.getInt("ad_app_closed_frequency", 1);  
            	boolean adShown = false;
            	if (countFsAd >= mSexPixApplication.ad_app_closed_frequency){
            		
            		if (mSexPixApplication.ad_app_closed_provider.equals("flurry")){
//            			FrameLayout container = new FrameLayout(this);
//            			adShown = FlurryAgent.getAd(this, "InstaSnap_ad_app_closed", container, FlurryAdSize.FULLSCREEN, 3000);
//            			Log.i("XMPPClient", "[Home] showing Flurry full screen ad");

            		} else if (mSexPixApplication.ad_app_closed_provider.equals("revmob")){
            			if (mSexPixApplication.fs != null && mSexPixApplication.fs.isAdLoaded()){
            				mSexPixApplication.fs.show();   
            				adShown = true;
            				Log.i("XMPPClient", "[Home] showing RevMob full screen ad");	

            			}
            		}
            	}
            	if (adShown){
            		editor.putInt("ad_app_closed_frequency", 1);
            		editor.commit();
            	} else {
            		editor.putInt("ad_app_closed_frequency", countFsAd + 1);
            		editor.commit();
            	}
        	} 

			finish();
        }      
        return super.onKeyDown(keyCode, event); 
    }
    
    @Override 
    protected void onResume() { 
        super.onResume();
        new refreshNewMessagesTask().execute();         
        readRecents();  
        
        
        if (mSexPixApplication.upgrade){
        	(this.findViewById(R.id.but_favorites)).setVisibility(Button.GONE);
        }
        
        mSexPixApplication.setBanner(this);
        Apptentive.getRatingModule().run(this);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        mSexPixApplication.removeBanner(this);
        
        
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        (findViewById(R.id.homeContactListContainer)).setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_bright));
        
        mSexPixApplication = (SexPixApplication) getApplication();      
        
        mSexPixApplication.mContacts = new ArrayList<RosterEntry>();
        if (mSexPixApplication!=null){
        	if(mSexPixApplication.connection!=null){
        		if(mSexPixApplication.connection.getRoster()!=null){
        			mSexPixApplication.mContacts = new ArrayList<RosterEntry>(mSexPixApplication.connection.getRoster().getEntries());
        		}
        		
        	}
        }       
        mInflater = getLayoutInflater();
        
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext()); 
    	showHistory = preferences.getBoolean("optShowHistory", true); 
        
        final ListView homeList = (ListView) findViewById(R.id.homeContactList); 
        
        TextView emptyList = (TextView) findViewById(R.id.textViewEmptyList);
        
        int rosterSize = preferences.getInt("roster_size", 0);
        
        if (mSexPixApplication.connection != null && mSexPixApplication.connection.getRoster() != null && mSexPixApplication.connection.getRoster().getEntryCount() > 0){
        	SharedPreferences.Editor editor = preferences.edit();
        	editor.putInt("roster_size", mSexPixApplication.connection.getRoster().getEntryCount());
			editor.commit();
        	
        }
        
        if (mSexPixApplication.connection == null || mSexPixApplication.connection.getRoster() == null){
        	homeList.setBackgroundResource(R.drawable.connecting_no_stretch);
        } else if (mSexPixApplication.connection != null && mSexPixApplication.connection.getRoster() != null && rosterSize == 0){
        //	homeList.setBackgroundResource(R.drawable.home_bg_no_stretch);
        	
        	emptyList.setVisibility(TextView.VISIBLE);
        	homeList.setVisibility(ListView.GONE);
        	
        	// TODO
        } 

        
   //     Log.i("XMPPClient", "[Contact list] Trying to set an adapter");
    	homeList.setAdapter(mAdapterContactList);
    	
    	homeList.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				
				final Context cContext = view.getContext();	
				final int chosenUser = position;
				
				final CharSequence[] items = {
				//	cContext.getResources().getString(R.string.str_dialog_add_to_fav),
					cContext.getResources().getString(R.string.str_dialog_rename),
					cContext.getResources().getString(R.string.str_delete_user)				
				}; 
				
				if (mSexPixApplication.mContacts.get(chosenUser).getName() == null){
					if (mSexPixApplication.isConnected()){
	    				mSexPixApplication.mContacts.get(chosenUser).setName(StringUtils.parseName(mSexPixApplication.mContacts.get(chosenUser).getUser()));
	    			}					
				}
				
				CustomAlertDialog.Builder builder = new CustomAlertDialog.Builder(cContext); 			    
				
			    builder.setTitle(mSexPixApplication.mContacts.get(chosenUser).getName()); 			   
			    builder.setItems(items, new DialogInterface.OnClickListener() { 
			        public void onClick(DialogInterface dialog, int item) { 
			        	switch(item){
			        		
			        	// rename	
			        	case 0:
			        		CustomAlertDialog.Builder alert = new CustomAlertDialog.Builder(cContext);

			        		// Set an EditText view to get user input 
			        		final EditText input = new EditText(getApplicationContext());
			        		
			        	//	input.setTextColor(getResources().getColor(R.color.Black));
			        		input.setText(mSexPixApplication.mContacts.get(chosenUser).getName());
			        		alert.setView(input);			        		
			        		alert.setTitle(cContext.getResources().getString(R.string.str_dialog_rename));
			        		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			        		public void onClick(DialogInterface dialog, int whichButton) {
			        			String value = input.getText().toString();
			        			if (mSexPixApplication.isConnected()){
			        				Log.i("XMPPClient", "[Home] renaming " + mSexPixApplication.mContacts.get(chosenUser).getName() + " to " + value);
			        				mSexPixApplication.mContacts.get(chosenUser).setName(value);			    
			        			} else {
			        				Toast.makeText(cContext, R.string.str_connectionError, Toast.LENGTH_SHORT).show();
			        			}			        						        		  
			        		  }
			        		});

			        		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			        		  public void onClick(DialogInterface dialog, int whichButton) {
			        		    // Canceled.
			        		  }
			        		});

			        		alert.show();
			        		break;
			        		
			        	// delete	
			        	case 1:
			        		CustomAlertDialog.Builder alert2 = new CustomAlertDialog.Builder(cContext);
			        		
			        		alert2.setMessage(cContext.getResources().getString(R.string.str_really_delete_user));
			        		
			        		alert2.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			        		public void onClick(DialogInterface dialog, int whichButton) {
			        		
			        			if (mSexPixApplication.isConnected()){
			        				try {			        					
										mSexPixApplication.connection.getRoster().removeEntry(mSexPixApplication.mContacts.get(chosenUser));
										Toast.makeText(cContext, R.string.str_user_deleted, Toast.LENGTH_SHORT).show();
									} catch (XMPPException e) {
										e.printStackTrace();
										Toast.makeText(cContext, R.string.str_connectionError, Toast.LENGTH_SHORT).show();
									}
			        			} else {
			        				Toast.makeText(cContext, R.string.str_connectionError, Toast.LENGTH_SHORT).show();
			        			}			        						        		  
			        		  }
			        		});

			        		alert2.setNegativeButton("No", new DialogInterface.OnClickListener() {
			        		  public void onClick(DialogInterface dialog, int whichButton) {
			        		    // Canceled.
			        		  }
			        		});

			        		alert2.show();
			        		break;
			        	
			        	}
			        	
			        } 
			    }).show();
				
				return false;
			}  		
    	});
    	

    	homeList.setOnItemClickListener(new OnItemClickListener(){
    		@Override
    		public void onItemClick(AdapterView<?> parent, View view, int position,long id) {					

    			if (mSexPixApplication.isConnected()){
    				Intent nextIntent = new Intent(view.getContext(), Picture.class);			        		
    				nextIntent.putExtra("chosenUser", mSexPixApplication.mContacts.get(position).getUser()); 	    					        		
    				startActivity(nextIntent);     			
    			} else {
    				Toast.makeText(view.getContext(), R.string.str_connectionError, Toast.LENGTH_SHORT).show();
    			}				
    		}    		
    	});

    	LocalBroadcastManager.getInstance(this).registerReceiver(mRefreshCLReceiver, new IntentFilter(ACTION_REFRESH_CONTACTLIST));
    	LocalBroadcastManager.getInstance(this).registerReceiver(mUpdateNumberNewMsgReceiver, new IntentFilter(ACTION_REFRESH_NEWMSG));
    	LocalBroadcastManager.getInstance(this).registerReceiver(mWrongPassReceiver, new IntentFilter(ACTION_WRONG_PASS));


    	if (mSexPixApplication.backgroundSerice != null){
    		if(mSexPixApplication.backgroundSerice.passIsWrong){
    			mHandler.post(mWrongPass);
    		}
    	}
    	
    	

    	// TODO check app expiration
    	try{
    		int verInt = preferences.getInt("currentAppVersion", 0);
    		String validUntil = preferences.getString("appValidUntil", "01.01.2020");

    		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
    		Date strDate = sdf.parse(validUntil);
    			
			
    		
    		boolean versionIsValid = true;
    		if (getPackageManager().getPackageInfo(getPackageName(), 0).versionCode < verInt){
    			
    			if(System.currentTimeMillis() > strDate.getTime()){
    				versionIsValid = false;
    				
    				
    				CustomAlertDialog.Builder alert = new CustomAlertDialog.Builder(this);
    				alert.setTitle(mSexPixApplication.getResources().getString(R.string.str_please_update));
    				alert.setMessage(mSexPixApplication.getResources().getString(R.string.txt_expired));
    				alert.setPositiveButton(mSexPixApplication.getResources().getString(R.string.str_update_now), new DialogInterface.OnClickListener() {
    					public void onClick(DialogInterface dialog, int whichButton) {
    						// TODO goto Google Play
    						
    						try {
    						    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.apparitionhq.instasnap")));
    						} catch (android.content.ActivityNotFoundException anfe) {
    						    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.apparitionhq.instasnap")));
    						}
    						
    						finish();

    					}
    				});
    				
    				alert.setOnCancelListener(new DialogInterface.OnCancelListener() {					
    					@Override
    					public void onCancel(DialogInterface dialog) {
    						finish();
    					}
    				});

    				alert.show();
    				
    				
    				
    			} else {
    				
    				Log.i("XMPPClient", "[Home] CurrentAppVersion expires soon!!! " + (int)((strDate.getTime() - System.currentTimeMillis())/(1000 * 60 * 60 * 24)) + " days left");

    				CustomAlertDialog.Builder alert = new CustomAlertDialog.Builder(this);
    				alert.setTitle(mSexPixApplication.getResources().getString(R.string.str_please_update));

    				String message = mSexPixApplication.getResources().getString(R.string.txt_expires_soon) + " " + (int)((strDate.getTime() - System.currentTimeMillis())/(1000 * 60 * 60 * 24)) + " " + mSexPixApplication.getResources().getString(R.string.txt_expires_soon2); 
    				alert.setMessage(message);

    				alert.setPositiveButton(mSexPixApplication.getResources().getString(R.string.str_update_now), new DialogInterface.OnClickListener() {
    					public void onClick(DialogInterface dialog, int whichButton) {
    						// TODO goto Google Play
    						
    						try {
    						    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.apparitionhq.instasnap")));
    						} catch (android.content.ActivityNotFoundException anfe) {
    						    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.apparitionhq.instasnap")));
    						}

    					}
    				});

    				alert.setNegativeButton(mSexPixApplication.getResources().getString(R.string.str_later), new DialogInterface.OnClickListener() {
    					public void onClick(DialogInterface dialog, int whichButton) {
    						// Canceled.
    					}
    				});

    				alert.show();


    			}
    			
    			
    		}   				    				   
    		
    		Log.i("XMPPClient", "[Home] CurrentAppVersion " + verInt + ", since " + validUntil + ",   valid = " + versionIsValid + "   strDate:" + strDate.toString());
    		
    	} catch (Exception e){
    	}
    	
    	
    	
    	// check for HA update
    	if (mSexPixApplication.isBeta){
			checkForUpdates();
		}

    }
    
    
    
    private void checkForUpdates() {
		// Remove this for store builds!
		UpdateManager.register(this, mSexPixApplication.HockeyApp_KEY);
		
		Log.w("XMPPClient", "[SplashScreen] checking HA for updates");
	}
    
	@Override
	protected void onDestroy() {
		// Unregister since the activity is about to be closed.
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mRefreshCLReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mUpdateNumberNewMsgReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mWrongPassReceiver);
		FlurryAgent.onEndSession(mSexPixApplication);
		super.onDestroy();
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_home, menu);
        return true;
    }
    
//    public int pixToDp(int px) {
//    	DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
//    	return (int)(px / (metrics.densityDpi / 160f));    	
//    }
//    
//    public int dpToPix(int dp) { 
//    	DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
//        return (int)(dp * (metrics.densityDpi/160f));       
//    }
    
        
    
	public void statusDialog(View view) {
		Intent i = new Intent(mSexPixApplication.getApplicationContext(), SettingsScreen.class);
		startActivityForResult(i, STATUS_REQUEST_CODE);
	}

	public void butFavorites(View view) {
	}
	
	public void butGoPro(View view) {		
		startActivity(new Intent(this, UpgradeScreen.class));		
	}
	
	public void butContacts(View view) {
		startActivity(new Intent(this, ContactPicker.class));
	}
	
	public void butNewPix(View view) {
		startActivity(new Intent(this, SavedPix.class));
	}
	
	public void butInfo(View view) {
	//	startActivity(new Intent(this, InfoScreen.class));
		Intent i = new Intent(mSexPixApplication.getApplicationContext(), InfoScreen.class);
		startActivityForResult(i, STATUS_REQUEST_CODE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {		
		if (requestCode == STATUS_REQUEST_CODE) {
			if (resultCode == Activity.RESULT_OK) {
				if (data != null) {
					if (data.getExtras().getString("message").equals("exit")){
						finish();
						
						mSexPixApplication.backgroundSerice.stopSelf();
						
						



						try {
							if (mSexPixApplication.connection!=null){
								mSexPixApplication.connection.disconnect();
								
								// stop UAship									
								Intent s = new Intent(this, com.urbanairship.push.PushService.class);
								stopService(s);
								
							}	
						} catch (Exception e){
							Log.w("XMPPClient", "[Home-onActivityResult] disconnect failed");
						}
						Toast.makeText(Home.this, R.string.str_disconnected, Toast.LENGTH_SHORT).show();	

						android.os.Process.killProcess(android.os.Process.myPid());
					}
					if (data.getExtras().getString("message").equals("delete")){

						Log.i("XMPPClient", "[Home-onActivityResult] deleting the account");

						if (mSexPixApplication.connection!=null){
							AccountManager mAccount = new AccountManager (mSexPixApplication.connection); 
							try {
								mAccount.deleteAccount();

								// Saving Login and Password
								SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mSexPixApplication.getApplicationContext());
								SharedPreferences.Editor editor = preferences.edit();

								mSexPixApplication.strUserLogin = "";
								mSexPixApplication.strUserPassword = "";

								editor.putString("strUserLogin", mSexPixApplication.strUserLogin);
								editor.putString("strUserPassword", mSexPixApplication.strUserPassword);
								editor.commit();



								Intent svcintent = new Intent(BgService.ACTION_NETWORK_DISCONNECT);          
								startService(svcintent);
								Toast.makeText(Home.this, R.string.str_account_deleted, Toast.LENGTH_SHORT).show();
								Intent intent = new Intent(this, SplashScreen.class);
								intent.putExtra("deleted", true);
								startActivity(intent);
								finish();

							} catch (XMPPException e) {
								Toast.makeText(this, R.string.str_connectionError, Toast.LENGTH_SHORT).show();
								Log.w("XMPPClient", "[Home-onActivityResult] unable to delete account");								
								e.printStackTrace();
							}	
							catch (IllegalStateException e) {
								Toast.makeText(this, R.string.str_connectionError, Toast.LENGTH_SHORT).show();
								Log.w("XMPPClient", "[Home-onActivityResult] unable to delete account");
								e.printStackTrace();
							}
						}						
					}
				}			
			}
		}

		if (requestCode == IMEI_REQUEST_CODE) {
			if (resultCode == Activity.RESULT_OK) {
				if (data != null) {
					mSexPixApplication.strUserPassword = data.getExtras().getString("message");	
					Intent svcintent = new Intent(BgService.ACTION_NETWORK_CONNECT); 
					mSexPixApplication.startService(svcintent);
				}				
			}
			else {
				mSexPixApplication.backgroundSerice.stopSelf();
				if (mSexPixApplication.connection!=null){
					mSexPixApplication.connection.disconnect();
				}						
				
				android.os.Process.killProcess(android.os.Process.myPid());			
			}
			
		}
	}

		
	
	private class ContactList extends BaseAdapter implements Filterable {		
		
		// Constructor
		public ContactList() {		   
		}

		@Override
		public int getCount() {
			return mSexPixApplication.mContacts.size();
		}

		@Override
		public Object getItem(int position) {
			mSexPixApplication.mContacts.get(position);
			return null;
		}

		@Override
		public long getItemId(int position) {			
			return mSexPixApplication.mContacts.get(position).hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (convertView == null) {				
				v = mInflater.inflate(R.layout.contact_layout, null);
			}			
			RosterEntry cContact = mSexPixApplication.mContacts.get(position);					
			bindView(v, cContact);
			if (mSexPixApplication.connection == null || mSexPixApplication.connection.getRoster() == null){
				v.setVisibility(View.GONE);							
			} else {
				v.setVisibility(View.VISIBLE);	
			}
			return v;			
		}

		@Override
		public Filter getFilter() {
			return null;
		}
		
		private void bindView(View view, RosterEntry curContact) {
			if (curContact != null) {
			//	((TextView) view.findViewById(R.id.contactFullName)).setText(curContact.getUser());
				((TextView) view.findViewById(R.id.contactName)).setText(curContact.getName());
				if(curContact.getName() == null){
					((TextView) view.findViewById(R.id.contactName)).setText(StringUtils.parseName(curContact.getUser()));
				}
				
				
				// set recent
				// get entries for chosenUser		
				if (allRecent == null){
					allRecent = new HashMap<String,ArrayList<HashMap<String,String>>>();
				}
				
				if (showHistory){
								
					ArrayList<HashMap<String,String>> recentEntries = allRecent.get(StringUtils.parseName(curContact.getUser()));		
					if (recentEntries != null){
						if (recentEntries.size()>0){
							HashMap<String,String> lastRecent = recentEntries.get(recentEntries.size()-1);
							String fileName = lastRecent.get("filename");
							String recentPicStatus = lastRecent.get("status");
						
							if (recentPicStatus.equals("upload")){
								((ImageView) view.findViewById(R.id.transferStatus)).setImageDrawable(getResources().getDrawable(R.drawable.transfer_status_upload));						
							} 
							else if (recentPicStatus.equals("uploaded")){
								((ImageView) view.findViewById(R.id.transferStatus)).setImageDrawable(getResources().getDrawable(R.drawable.transfer_status_uploaded));						
							}
							else if (recentPicStatus.equals("received")){
								((ImageView) view.findViewById(R.id.transferStatus)).setImageDrawable(getResources().getDrawable(R.drawable.transfer_status_received));						
							}
							else if (recentPicStatus.equals("packetreceived")){
								((ImageView) view.findViewById(R.id.transferStatus)).setImageDrawable(getResources().getDrawable(R.drawable.transfer_status_packetreceived));						
							}
							else if (recentPicStatus.equals("watched")){
								((ImageView) view.findViewById(R.id.transferStatus)).setImageDrawable(getResources().getDrawable(R.drawable.transfer_status_watched));						
							}
							else if (recentPicStatus.equals("error")){
								((ImageView) view.findViewById(R.id.transferStatus)).setImageDrawable(getResources().getDrawable(R.drawable.transfer_status_error));						
							}
						
						
						
							//	Log.i("XMPPClient", "[bindView] set recent for:" + StringUtils.parseName(curContact.getUser()) + "  file:" + fileName + ".jpg");
						
							Bitmap recentBitmap = BitmapFactory.decodeFile(mSexPixApplication.getFilesDir().toString() + "/thumbnails/" + fileName + ".jpg");
						
							((ImageView) view.findViewById(R.id.recentPic)).setImageBitmap(recentBitmap);	
							((ImageView) view.findViewById(R.id.recentPic)).setVisibility(ImageView.VISIBLE);
							((ImageView) view.findViewById(R.id.transferStatus)).setVisibility(ImageView.VISIBLE);
						
						} else {
							((ImageView) view.findViewById(R.id.recentPic)).setImageBitmap(null);
							((ImageView) view.findViewById(R.id.recentPic)).setVisibility(ImageView.INVISIBLE);	
							((ImageView) view.findViewById(R.id.transferStatus)).setVisibility(ImageView.INVISIBLE);
						}
					} else {
						((ImageView) view.findViewById(R.id.recentPic)).setImageBitmap(null);
						((ImageView) view.findViewById(R.id.recentPic)).setVisibility(ImageView.INVISIBLE);
						((ImageView) view.findViewById(R.id.transferStatus)).setVisibility(ImageView.INVISIBLE);
					}
				} else {
					((ImageView) view.findViewById(R.id.recentPic)).setImageBitmap(null);
					((ImageView) view.findViewById(R.id.recentPic)).setVisibility(ImageView.INVISIBLE);
					((ImageView) view.findViewById(R.id.transferStatus)).setVisibility(ImageView.INVISIBLE);
				}

				
				
				try{
					Presence cPresence = mSexPixApplication.connection.getRoster().getPresence(curContact.getUser());	
					if (cPresence.isAvailable()){
						if (cPresence.getMode() != null){
							if (cPresence.getMode() == Presence.Mode.away){
							//	((ImageView) view.findViewById(R.id.img_status)).setImageResource(R.drawable.icon_st_away);
								((ImageView) view.findViewById(R.id.img_status)).setImageBitmap(null);	
							} else if (cPresence.getMode() == Presence.Mode.dnd) {
							//	((ImageView) view.findViewById(R.id.img_status)).setImageResource(R.drawable.icon_st_dnd);
								((ImageView) view.findViewById(R.id.img_status)).setImageBitmap(null);
							}		
						} else {
						
							((ImageView) view.findViewById(R.id.img_status)).setImageResource(R.drawable.icon_led_online);						
						}										
					} else {
					//	((ImageView) view.findViewById(R.id.img_status)).setImageResource(R.drawable.icon_st_offline);
						((ImageView) view.findViewById(R.id.img_status)).setImageBitmap(null);
					}							
				
					if (cPresence.getStatus() != null){
						((TextView) view.findViewById(R.id.cStatusMessage)).setText(cPresence.getStatus());	
					} else {
						((TextView) view.findViewById(R.id.cStatusMessage)).setText("");	
					}
				} catch (Exception e){
					//Log.w("XMPPClient", "[bindView] Can not bind view");
					//Log.w("XMPPClient", e.toString());
					
				}								
			}
		}	
	}
}
