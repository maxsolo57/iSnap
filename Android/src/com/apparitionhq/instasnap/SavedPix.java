package com.apparitionhq.instasnap;

import java.io.File;
import java.util.ArrayList;

import org.jivesoftware.smack.packet.Message;

import com.flurry.android.FlurryAgent;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.google.analytics.tracking.android.EasyTracker;
import com.revmob.RevMob;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class SavedPix extends Activity {
	private SexPixApplication mSexPixApplication;
	private ArrayList<Message> messages;
	private final PixList mAdapterPixList = new PixList();	
	private LayoutInflater mInflater;
	final Handler mHandler = new Handler(); 
	public final static String ACTION_REFRESH_NEWMSG = "com.apparitionhq.instasnap.action.REFRESH_NEWMSG";
	
	private BroadcastReceiver mUpdateNumberNewMsgReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mHandler.post(mUpdateNewMessages);			
		}
	};
	
	public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getWindow().setBackgroundDrawable(getResources().getDrawable( R.drawable.bg_dark ));
    }
	
	final Runnable mUpdateNewMessages = new Runnable() { 
        public void run() {         	
        	messages = new ArrayList<Message>();
            ArrayList<Message> allMessages = mSexPixApplication.getMessages();
            File file;
            for (Message msg : allMessages) {
            	String fName = (String) msg.getProperty("filename"); 
            	file = new File(mSexPixApplication.getFilesDir().toString() + "/" + fName + "_blur"); 
            	if (file.exists()){
            		messages.add(msg);
            	}	
            }	
            
            mAdapterPixList.notifyDataSetChanged();
            GridView gridView = (GridView) findViewById(R.id.grid_new_pix); 
            gridView.invalidateViews();
            
            if (messages.size()==0){
            	((TextView) findViewById(R.id.txt_saved_pix_title)).setText(getResources().getString(R.string.str_no_incoming_pix));	
            } else {
            	((TextView) findViewById(R.id.txt_saved_pix_title)).setText(getResources().getString(R.string.str_incoming));	
            }
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
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_pix);
        
        mSexPixApplication = (SexPixApplication) getApplication();  
        FlurryAgent.onStartSession(mSexPixApplication, mSexPixApplication.FLURRY_APP_KEY);
        
        messages = new ArrayList<Message>();
        ArrayList<Message> allMessages = mSexPixApplication.getMessages();
        File file;
        for (Message msg : allMessages) {
        	String fName = (String) msg.getProperty("filename"); 
        	file = new File(mSexPixApplication.getFilesDir().toString() + "/" + fName + "_blur"); 
        	if (file.exists()){
        		messages.add(msg);
        	}	
        }	
        
        
    //    Log.i("XMPPClient", "[SavedPix] messages size:" + messages.size());
        
        mInflater = getLayoutInflater();
        
        final GridView gridView = (GridView) findViewById(R.id.grid_new_pix); 
        
        gridView.setAdapter(mAdapterPixList);
        
        gridView.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				
				
				
				Intent showPicIntent = new Intent(mSexPixApplication.getApplicationContext(), ShowPicture.class); 
				Message msg = messages.get(position);
				showPicIntent.putExtra("messageID", msg.getPacketID());
				showPicIntent.putExtra("showConfiramtion", false);	
				startActivity(showPicIntent);
				
				if (messages.size() <2){
					finish();
				}								
			}
        	
        });
        
        LocalBroadcastManager.getInstance(this).registerReceiver(mUpdateNumberNewMsgReceiver, new IntentFilter(ACTION_REFRESH_NEWMSG));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_saved_pix, menu);
        return true;
    }
    
    
    @Override 
    protected void onResume() { 
    	super.onResume();
    	
    	if (messages.size()==0){
        	((TextView) findViewById(R.id.txt_saved_pix_title)).setText(getResources().getString(R.string.str_no_incoming_pix));	
        }

    	mSexPixApplication.setBanner(this);
    	
    	mHandler.post(mUpdateNewMessages);	
    }
	
	@Override
    public void onPause() {
        super.onPause();
        mSexPixApplication.removeBanner(this);
    }
    

    
    
    
    private class PixList extends BaseAdapter implements Filterable {

		@Override
		public int getCount() {
			return messages.size();
		}

		@Override
		public Object getItem(int position) {
			return messages.get(position);
		}

		@Override
		public long getItemId(int position) {
			return messages.get(position).hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (convertView == null) {
				v = mInflater.inflate(R.layout.new_pix_layout, null);
			}		
			Message msg = messages.get(position);
						
			bindView(v, msg);
			return v;	
		
		}

		@Override
		public Filter getFilter() {
			return null;
		}
		
		private void bindView(View view, Message msg) {
			
			if (msg != null) {
				
				String sender = (String) msg.getProperty("nickName");
						
				Long whenMillis = (Long) msg.getProperty("date");
				if (whenMillis != null){
					Time when = new Time();
					when.set(whenMillis);
					when.switchTimezone(Time.getCurrentTimezone());
					
					String month = "" + (when.month + 1);
					if (month.length()<2){
						month = "0" + month;
					}
					
					String monthDay = "" + when.monthDay;
					if (monthDay.length()<2){
						monthDay = "0" + monthDay;
					}					
					
					String hour = "" + when.hour;
					if (hour.length()<2){
						hour = "0" + hour;
					}
					String minute = "" + when.minute;
					if (minute.length()<2){
						minute = "0" + minute;
					}
					((TextView) view.findViewById(R.id.txt_date)).setText("" + hour + ":" + minute + "    " + monthDay + "." + month + "." + when.year);
				}

				((TextView) view.findViewById(R.id.txt_from)).setText(sender);
				String fName = (String) msg.getProperty("filename");
				((ImageView) view.findViewById(R.id.image_new_pix)).setImageBitmap(BitmapFactory.decodeFile(mSexPixApplication.getFilesDir().toString() + "/" + fName + "_blur"));

			}
		}    	
    }
}
