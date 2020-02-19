package com.apparitionhq.instasnap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.ReportedData;
import org.jivesoftware.smackx.ReportedData.Row;
import org.jivesoftware.smackx.search.UserSearchManager;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import de.guitarcollege.custom.dialog.CustomAlertDialog;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.telephony.TelephonyManager;
//import android.util.Log;
import com.apphance.android.Log;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ContactPicker extends Activity {
	public static final int ACTION_PICK_CONTACT = 325;
	public ArrayList<String> listItems = new ArrayList<String>(); 
	private SexPixApplication mSexPixApplication;
	public String contactName;
	public String chosenContact;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_picker);
        mSexPixApplication = (SexPixApplication) getApplication();
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
    	startActivityForResult(intent, ACTION_PICK_CONTACT);
    }
    
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getWindow().setBackgroundDrawable(getResources().getDrawable( R.drawable.bg_dark ));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_contact_picker, menu);
        return true;
    }
    
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
        
    public void butAdd(View view) {
    	
    	contactName = ((EditText) findViewById(R.id.contactsName)).getText().toString();
    	
    	if(chosenContact == null){
    		Toast.makeText(mSexPixApplication, mSexPixApplication.getResources().getString(R.string.str_error), Toast.LENGTH_SHORT).show();
    		
    		return;
    	}

    	try {
    		if (mSexPixApplication.connection == null || mSexPixApplication.connection.getRoster() == null){
    			Toast.makeText(mSexPixApplication, mSexPixApplication.getResources().getString(R.string.str_connectionError), Toast.LENGTH_SHORT).show();
    			return;
    		}
    		mSexPixApplication.connection.getRoster().createEntry(chosenContact + "@" + mSexPixApplication.strServerAddress, contactName, null);


    		Presence subscribe = new Presence(Presence.Type.subscribe); 
    		subscribe.setTo(chosenContact + "@" + mSexPixApplication.strServerAddress);
    		mSexPixApplication.connection.sendPacket(subscribe);

    		
    		Toast.makeText(mSexPixApplication, mSexPixApplication.getResources().getString(R.string.str_user_added), Toast.LENGTH_SHORT).show();
    		finish();
    	} catch (XMPPException e) {
    		Toast.makeText(mSexPixApplication, mSexPixApplication.getResources().getString(R.string.str_error), Toast.LENGTH_SHORT).show();
    		e.printStackTrace();
    	}

	}
    
    public void butInvite(View view) {
    	if(chosenContact == null || chosenContact.length()==0){
    		Toast.makeText(mSexPixApplication, mSexPixApplication.getResources().getString(R.string.str_error), Toast.LENGTH_SHORT).show();
    		return;
    	}
    	Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + chosenContact));
    	
    	intent.putExtra( "sms_body", getResources().getString(R.string.str_invite_sms));
    	startActivity(intent);
    	
    	finish();		
	}
    
    
    
    
    
    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
    	super.onActivityResult(reqCode, resultCode, data);

    	switch (reqCode) {
    	case (ACTION_PICK_CONTACT) :
    		if (resultCode == Activity.RESULT_OK) {
    			Uri contactData = data.getData();
    			Cursor cur =  managedQuery(contactData, null, null, null, null);
    			ContentResolver contect_resolver = getContentResolver();
    			
    			if (cur.moveToFirst()) {
    				String id = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
    				contactName = "";
    				ArrayList<String> numbers = new ArrayList<String>();
    				ArrayList<String> types = new ArrayList<String>();
                    String no = "";
                   
                    
                    Cursor phoneCur = contect_resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] { id }, null);
                    
                    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                    
                    TelephonyManager phoneManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
                    String simCountryIso = phoneManager.getSimCountryIso();
                    simCountryIso = simCountryIso.toUpperCase();
                    Log.i("XMPPClient", "[ContactPicker] sim country ISO: " + simCountryIso);
                    
                    while (phoneCur.moveToNext()) {
                    	contactName = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        no = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        int type = phoneCur.getInt(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                        Log.i("XMPPClient", "[ContactPicker] picked:" + contactName + "   data:" + no + "   type:" + type);
                        
                        try {
							PhoneNumber currentNumberProto = phoneUtil.parse(no, simCountryIso);
						//	if (phoneUtil.isValidNumber(currentNumberProto) && phoneUtil.getNumberType(currentNumberProto) == PhoneNumberUtil.PhoneNumberType.MOBILE){								
							if (phoneUtil.isValidNumber(currentNumberProto)){
								no = phoneUtil.format(currentNumberProto, PhoneNumberFormat.E164);							
								numbers.add(no);
								types.add(getPhoneNumType(type));
							}

						} catch (NumberParseException e) {
							e.printStackTrace();
						}                                           
                    }	
                    
                    
                	CustomAlertDialog.Builder alert = new CustomAlertDialog.Builder(this);
    				alert.setTitle(contactName);
    				alert.setMessage(getResources().getString(R.string.str_user_has_no_phone));
    				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		        		public void onClick(DialogInterface dialog, int whichButton) {
		        			finish();		        		
		        		}
    				});
    				alert.setOnCancelListener(new DialogInterface.OnCancelListener() {						
						@Override
						public void onCancel(DialogInterface dialog) {
							finish();							
						}
					});
                    
                    if (phoneCur.getCount() == 0){
                    	Log.i("XMPPClient", "[ContactPicker] user has no phone number at all");
                    	alert.show();
                    	return;
                    }
                    
                    if (numbers.size() == 0){ 
                    	Log.i("XMPPClient", "[ContactPicker] no valid mobile number found");
                    	alert.show();
                    	return;
                    }
                    
                    ((EditText) findViewById(R.id.contactsName)).setText(contactName);
                    
                    final CharSequence[] items = numbers.toArray(new CharSequence[numbers.size()]);

                    CustomAlertDialog.Builder alert2 = new CustomAlertDialog.Builder(this);
    				alert2.setTitle(getResources().getString(R.string.str_choose_phone_number));
    				alert2.setAdapter(new TwoArraysAdapter(mSexPixApplication, numbers, types), new DialogInterface.OnClickListener() { 
    			        public void onClick(DialogInterface dialog, int item) {       			        	
    			        	chosenContact = (String) items[item];
    			        	((TextView) findViewById(R.id.contactsPhone)).setText(chosenContact);
    			        	Log.i("XMPPClient", "[ContactPicker] chosen:" + chosenContact);
    			        	new SearchTask().execute(chosenContact);
    			        }
    				});   				    				
    				alert2.setOnCancelListener(new DialogInterface.OnCancelListener() {						
						@Override
						public void onCancel(DialogInterface dialog) {
							finish();							
						}
					});
    				
    				
    				if (numbers.size() > 1){   
                    	Log.i("XMPPClient", "[ContactPicker] more than 1 number found");
                    	alert2.show();
                    } else {                    	
                    	chosenContact = numbers.get(0); 
                    	((TextView) findViewById(R.id.contactsPhone)).setText(chosenContact);
                    	Log.i("XMPPClient", "[ContactPicker] chosen:" + chosenContact);
                    	new SearchTask().execute(chosenContact);
                    }   				    				
    			}
    		} else {
    			finish();
    		}  	    	
    	break;
    	}
    }
    
    public String getPhoneNumType(int type){
    	if (type == ContactsContract.CommonDataKinds.Phone.TYPE_HOME){
    		return "Home";
    	}
    	if (type == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE){
    		return "Mobile";
    	}
    	if (type == ContactsContract.CommonDataKinds.Phone.TYPE_WORK){
    		return "Work";
    	}
    	if (type == ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK){
    		return "Fax work";
    	}
    	if (type == ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME){
    		return "Fax home";
    	}
    	if (type == ContactsContract.CommonDataKinds.Phone.TYPE_OTHER){
    		return "Other";
    	}
    	if (type == ContactsContract.CommonDataKinds.Phone.TYPE_MAIN){
    		return "Main";
    	}
    	if (type == ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE){
    		return "Work mobile";
    	}   	    	
    	return "";
    }
    
    
    
    
    public class SearchTask extends AsyncTask<String, String, String> {		
    	private SexPixApplication mSexPixApplication;
    	public String chosenPhoneNumber;

		@Override
		protected String doInBackground(String... params) {
			
			
			publishProgress("searching"); 
			mSexPixApplication = (SexPixApplication) getApplication();
			
			chosenPhoneNumber = params[0];
			try {							
				UserSearchManager search = new UserSearchManager(mSexPixApplication.connection);   
				 
		        Form searchForm = search.getSearchForm(mSexPixApplication.strServerSearchAddress);   
		        Form answerForm = searchForm.createAnswerForm();   
		        answerForm.setAnswer("Username", true);   
		        
		        answerForm.setAnswer("search", chosenPhoneNumber);   
		        ReportedData data = search.getSearchResults(answerForm,mSexPixApplication.strServerSearchAddress);   		          
		        
		        Iterator<Row> rows = data.getRows();
		        listItems.clear();
		        
		        while (rows.hasNext()) {
		        	Row cRow = rows.next();
		        	
		        	Iterator<String> names = cRow.getValues("Username");
		        	while (names.hasNext()) {
		        		listItems.add(names.next());		        		
		        	}		        	
		        }
			} 
			catch (XMPPException ex) {
				Log.e("XMPPClient", "[SearchTask] " + ex.toString());
				Log.e("XMPPClient", "[SearchTask] server: " + mSexPixApplication.strServerConnectAddress + "    search: " + mSexPixApplication.strServerSearchAddress);
				return ex.toString();				
			}
			catch (IllegalStateException ex) {
				Log.e("XMPPClient", "[SearchTask] " + ex.toString());
				Log.e("XMPPClient", "[SearchTask] server: " + mSexPixApplication.strServerConnectAddress + "    search: " + mSexPixApplication.strServerSearchAddress);
				return "connection error";				
			}
			
			
			return "done";	
		}
		
		@Override
		protected void onPostExecute(String result) {
			
			if (result.equals("done")){	
				TextView txt = (TextView) findViewById(R.id.text_searching);
				txt.setVisibility(TextView.INVISIBLE);
				((ProgressBar) findViewById(R.id.progressBar2)).setVisibility(ProgressBar.INVISIBLE);	
				
				LinearLayout layout = (LinearLayout) findViewById(R.id.addContactLayout);
				layout.setVisibility(LinearLayout.VISIBLE);
				
				if (listItems.isEmpty()){
					((Button) findViewById(R.id.but_invite)).setVisibility(Button.VISIBLE);
					((Button) findViewById(R.id.but_add)).setVisibility(Button.INVISIBLE);
					((TextView) findViewById(R.id.inviteText)).setVisibility(TextView.VISIBLE);
				} else {
					((Button) findViewById(R.id.but_invite)).setVisibility(Button.INVISIBLE);
					((TextView) findViewById(R.id.inviteText)).setVisibility(TextView.INVISIBLE);
					((Button) findViewById(R.id.but_add)).setVisibility(Button.VISIBLE);				
				}

				((TextView) findViewById(R.id.text_searching)).setVisibility(TextView.INVISIBLE);
				((ProgressBar) findViewById(R.id.progressBar2)).setVisibility(ProgressBar.INVISIBLE);
								
			}
			else {		
				Toast.makeText(mSexPixApplication, R.string.str_connectionError, Toast.LENGTH_SHORT).show();
				finish();
			}					
		}
		
		
		@Override
		protected void onProgressUpdate(String... values) {
			if (values[0].equals("searching")){
				try{
					TextView txt = (TextView) findViewById(R.id.text_searching);
					txt.setVisibility(TextView.VISIBLE);
					((ProgressBar) findViewById(R.id.progressBar2)).setVisibility(ProgressBar.VISIBLE);	
				} catch (Exception e){					
				}					
			}			
		}
    }
    
    
    
    public class TwoArraysAdapter extends BaseAdapter {
    	private ArrayList<String> line1;
    	private ArrayList<String> line2;
    	private LayoutInflater mInflater;
    	
    	public TwoArraysAdapter(Context context, ArrayList<String> lineA, ArrayList<String> lineB) {        
    		line1 = lineA;
    		line2 = lineB;
    		mInflater = LayoutInflater.from(context);    
    	}

		@Override
		public int getCount() {
			return line1.size();
		}

		@Override
		public Object getItem(int arg0) {
			return line1.get(arg0);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			convertView = mInflater.inflate(R.layout.phonenum_list, null);
			((TextView) convertView.findViewById(R.id.phonenum_phone)).setText(line1.get(position));
			((TextView) convertView.findViewById(R.id.phonenum_type)).setText(line2.get(position));

			return convertView;
		}
    	
    }
}
