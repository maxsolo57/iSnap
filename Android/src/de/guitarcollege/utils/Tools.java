package de.guitarcollege.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.search.UserSearch;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.DisplayMetrics;
import android.util.Log;

public class Tools {
	private static final Random rgenerator = new Random();
	
	
	public static float convertDpToPixel(float dp,Context context){
	    Resources resources = context.getResources();
	    DisplayMetrics metrics = resources.getDisplayMetrics();
	    float px = dp * (metrics.densityDpi/160f);
	    return px;
	}
	
	public static float convertPixelsToDp(float px,Context context){
	    Resources resources = context.getResources();
	    DisplayMetrics metrics = resources.getDisplayMetrics();
	    float dp = px / (metrics.densityDpi / 160f);
	    return dp;

	}

	

	
	public static String randomString(int length) {
		String source = "qazwsxedcrfvtgbyhnujmikolpQAZWSXEDCRFVTGBYHNUJMIKOLP1234567890";
		String result = "";
		for (int idx = 0; idx < length; ++idx)					
			result = result + source.charAt(rgenerator.nextInt(source.length()));
		return result;
	}
	
	public static String randomNumString(int length) {
		String source = "1234567890";
		String result = "";
		for (int idx = 0; idx < length; ++idx)					
			result = result + source.charAt(rgenerator.nextInt(source.length()));
		return result;
	}
	
	public static byte[] encrypt(byte[] input, String pass){
    	
    	if (pass.length()<16){
    		pass = pass + "0123456789abcdef";
    	}
    	
    	if (pass.length()>16){
    		pass = pass.substring(0, 16);
    	}
    	    	
    	SecretKeySpec key = new SecretKeySpec(pass.getBytes(), "AES");
        Cipher cipher;
       
			try {
				cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
				cipher.init(Cipher.ENCRYPT_MODE, key);
				return cipher.doFinal(input);
			} catch (Exception e) {
				e.printStackTrace();
				return input; 
			} 		 			   	    	
    }
    
    public static byte[] decrypt(byte[] input, String pass){
    	
    	if (pass.length()<16){
    		pass = pass + "0123456789abcdef";
    	}
    	
    	if (pass.length()>16){
    		pass = pass.substring(0, 16);
    	}
    	
    	SecretKeySpec key = new SecretKeySpec(pass.getBytes(), "AES");
        Cipher cipher;
       
			try {
				cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");			
				cipher.init(Cipher.DECRYPT_MODE, key);
				return cipher.doFinal(input);
			} catch (Exception e) {
				e.printStackTrace();
				return input; 
			} 		 			   	    	
    }
    
    public static byte[] readFile (String filename) throws IOException {
    	
    	File file = new File(filename); 
        // Open file
        RandomAccessFile f = new RandomAccessFile(file, "r");

        try {
            // Get and check length
            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength) throw new IOException("File size >= 2 GB");

            // Read file and return data
            byte[] data = new byte[length];
            f.readFully(data);
            return data;
        }
        finally {
            f.close();
        }
    }
    
    public static void writeFile(byte[] data, String fileName) throws IOException{
    	FileOutputStream out = new FileOutputStream(fileName);
    	out.write(data);
    	out.flush();
    	out.close();
    	out = null;
    	System.gc();
    }
    
    public static void deleteFiles(String path) {

        File file = new File(path);

        if (file.exists()) {
            String deleteCmd = "rm -r " + path;
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec(deleteCmd);
            } catch (IOException e) { }
        }
    }
    
    public static String getNameByNumber(Context context, String number) {

		String name = null;
		String[] projection = new String[] {ContactsContract.PhoneLookup.DISPLAY_NAME};

		Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
		Cursor cursor = context.getContentResolver().query(contactUri, projection, null, null, null);

		if (cursor.moveToFirst()) {
		    name =      cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));		    
		    return name;
		} else {
		    return ""; // contact not found
		}
	}
    
    
    public static void configureProviderManager(ProviderManager pm) { 			 
	    pm.addIQProvider("query","jabber:iq:private", new PrivateDataManager.PrivateDataIQProvider()); 		 
	    try { 
	        pm.addIQProvider("query","jabber:iq:time", Class.forName("org.jivesoftware.smackx.packet.Time")); 
	    } catch (ClassNotFoundException e) { 
	        Log.w("TestClient", "Can't load class for org.jivesoftware.smackx.packet.Time"); 
	    } 
	    pm.addExtensionProvider("x","jabber:x:roster", new RosterExchangeProvider()); 
	    pm.addExtensionProvider("x","jabber:x:event", new MessageEventProvider()); 
	    pm.addExtensionProvider("active","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider()); 		 
	    pm.addExtensionProvider("composing","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider()); 		 
	    pm.addExtensionProvider("paused","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider()); 		 
	    pm.addExtensionProvider("inactive","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider()); 		 
	    pm.addExtensionProvider("gone","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider()); 
	    pm.addExtensionProvider("html","http://jabber.org/protocol/xhtml-im", new XHTMLExtensionProvider()); 
	    pm.addExtensionProvider("x","jabber:x:conference", new GroupChatInvitation.Provider());     
	    pm.addIQProvider("query","http://jabber.org/protocol/disco#items", new DiscoverItemsProvider()); 
	    pm.addIQProvider("query","http://jabber.org/protocol/disco#info", new DiscoverInfoProvider()); 
	    pm.addExtensionProvider("x","jabber:x:data", new DataFormProvider()); 
	    pm.addExtensionProvider("x","http://jabber.org/protocol/muc#user", new MUCUserProvider());    
	    pm.addIQProvider("query","http://jabber.org/protocol/muc#admin", new MUCAdminProvider()); 		    
	    pm.addIQProvider("query","http://jabber.org/protocol/muc#owner", new MUCOwnerProvider()); 
	    pm.addExtensionProvider("x","jabber:x:delay", new DelayInformationProvider()); 
	    try { 
	        pm.addIQProvider("query","jabber:iq:version", Class.forName("org.jivesoftware.smackx.packet.Version")); 
	    } catch (ClassNotFoundException e) { 
	        //  Not sure what's happening here. 
	    } 
	    pm.addIQProvider("vCard","vcard-temp", new VCardProvider()); 
	    pm.addIQProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageRequest.Provider()); 
	    pm.addExtensionProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageInfo.Provider()); 
	    pm.addIQProvider("query","jabber:iq:last", new LastActivity.Provider()); 
	    pm.addIQProvider("query","jabber:iq:search", new UserSearch.Provider()); 
	    pm.addIQProvider("sharedgroup","http://www.jivesoftware.org/protocol/sharedgroup", new SharedGroupsInfo.Provider()); 
	    pm.addExtensionProvider("addresses","http://jabber.org/protocol/address", new MultipleAddressesProvider()); 
	    pm.addIQProvider("si","http://jabber.org/protocol/si", new StreamInitiationProvider()); 		 
	    pm.addIQProvider("query","http://jabber.org/protocol/bytestreams", new BytestreamsProvider()); 		 
//	    pm.addIQProvider("open","http://jabber.org/protocol/ibb", new IBBProviders.Open()); 		 
//	    pm.addIQProvider("close","http://jabber.org/protocol/ibb", new IBBProviders.Close()); 		 
//	    pm.addExtensionProvider("data","http://jabber.org/protocol/ibb", new IBBProviders.Data()); 
	    pm.addIQProvider("query","jabber:iq:privacy", new PrivacyProvider()); 		 
	    pm.addIQProvider("command", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider()); 
	    pm.addExtensionProvider("malformed-action", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.MalformedActionError()); 
	    pm.addExtensionProvider("bad-locale", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadLocaleError()); 
	    pm.addExtensionProvider("bad-payload", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadPayloadError()); 
	    pm.addExtensionProvider("bad-sessionid", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadSessionIDError()); 
	    pm.addExtensionProvider("session-expired", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.SessionExpiredError()); 
	} 


}
