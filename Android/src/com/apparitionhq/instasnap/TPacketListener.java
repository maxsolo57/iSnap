package com.apparitionhq.instasnap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.packet.VCard;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

//import android.util.Log;
import com.apphance.android.Log;

import de.guitarcollege.utils.Tools;


public class TPacketListener {
	public static String thumbStorage = "thumbs.bin";
	public final static String ACTION_REFRESH_CONTACTLIST = "com.apparitionhq.instasnap.action.REFRESH_CONTACTLIST";
	
	public static void setPacketListener(final SexPixApplication mSexPixApplication){
		
		PacketFilter emptyFilter = new PacketFilter() {
		     public boolean accept(Packet packet) {
		         return true;
		     }
		 };
		 
		 PacketListener allPacketsListener = new PacketListener(){
			@Override
			public void processPacket(Packet packet) {
				Log.w("XMPPallPackets", "New packet:" + packet.getPacketID() + "    from:" + packet.getFrom());		
				Log.i("XMPPallPackets", "" + packet.toXML());		
			}
			 
		 };
		
		 
		 PacketFilter instaSexyFileFilter = new PacketFilter() {
		     public boolean accept(Packet packet) {
		    	 if (packet.getPacketID().contains("InstaSnapFile"))
		    	 {
		    		 return true;
		    	 }
		    	 else {
		    		 return false;
		    	 }
		     }
		 };
		 
		 PacketFilter pingFilter = new PacketFilter() {
		     public boolean accept(Packet packet) {
		    	 if (packet.getPacketID().contains("ISping"))
		    	 {
		    		 return true;
		    	 }
		    	 else {
		    		 return false;
		    	 }
		     }
		 };
		 
		 PacketFilter transferStatusFilter = new PacketFilter() {
		     public boolean accept(Packet packet) {
		    	 if (packet.getPacketID().contains("TransferStatus"))
		    	 {
		    		 return true;
		    	 }
		    	 else {
		    		 return false;
		    	 }
		     }
		 };
		 
		 PacketListener transferStatusListener = new PacketListener() {
			 public void processPacket(Packet packet) {
				 String receivedFile = (String) packet.getProperty("filename");
				 String newStatus = (String) packet.getProperty("transferStatus");
				 Log.w("XMPPClient", "New packet:" + packet.getPacketID() + "    from:" + packet.getFrom() + "    transfer status:" + newStatus);	
				 Log.w("XMPPPacket", "New packet:");		
				 Log.i("XMPPPacket", "ID:" + packet.getPacketID() + "    from:" + packet.getFrom());						
				 Log.i("XMPPPacket", "filename:" + receivedFile + "    transfer status:" + newStatus);	
				 
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
					ArrayList<HashMap<String,String>> recentEntries = allRecent.get(StringUtils.parseName(packet.getFrom()));		
					if (recentEntries == null){
						return;	
					}			
					
					// update status
					if (recentEntries.size() > 0){
						for(HashMap<String,String> recEntry : recentEntries){
							String recFile = recEntry.get("filename");
							if (recFile.equals(receivedFile)){
								String recSt = recEntry.get("status");
								if (!recSt.equals("watched")){
									recEntry.put("status", newStatus);	
								}													
							}
						}
					}	
					
					// put everything back		
					allRecent.put(StringUtils.parseName(packet.getFrom()), recentEntries);
					
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
					
					Intent intent = new Intent(ACTION_REFRESH_CONTACTLIST);
					LocalBroadcastManager.getInstance(mSexPixApplication.getApplicationContext()).sendBroadcast(intent);
							 				 
			 }
		 };
		 
		 PacketListener instaSexyFileListener = new PacketListener() {
			 public void processPacket(Packet packet) {
				 Log.w("XMPPClient", "New packet:" + packet.getPacketID() + "    from:" + packet.getFrom());
				 Log.w("XMPPPacket", "New packet:");		
				 Log.i("XMPPPacket", "ID:" + packet.getPacketID() + "    from:" + packet.getFrom());						
				 Log.i("XMPPPacket", "filename:" + (String) packet.getProperty("filename") + "    size:" + (Integer) packet.getProperty("size"));	
				 
				 StringUtils.parseName(packet.getFrom());
				 
				 String nickName = StringUtils.parseName(packet.getFrom());
				 
				 try {
					 RosterEntry cEntry = mSexPixApplication.connection.getRoster().getEntry(StringUtils.parseBareAddress(packet.getFrom()));
					 if (cEntry != null){
						 if (cEntry.getName() != null && !cEntry.getName().equals("")){
							 nickName = cEntry.getName();							
						 }							
					 }						 					 
				 } catch (Exception e) {				
					e.printStackTrace();
				 }
				 
				 packet.setProperty("nickName", nickName);
				 
				 
				 ArrayList<Message> messages = mSexPixApplication.getMessages();
				 messages.add((Message) packet);
				 
				 	
				 
				// send feedback to sender						
					Message iSPacket = new Message(StringUtils.parseBareAddress(packet.getFrom()), Message.Type.normal);
		    		iSPacket.setPacketID("TransferStatus-" + Tools.randomString(8));
		    		iSPacket.setBody("_" + (String) packet.getProperty("filename") + "_packetreceived");
		    		iSPacket.setProperty("filename", (String) packet.getProperty("filename"));
		    		iSPacket.setProperty("transferStatus", "packetreceived");
		    		try{
		    			mSexPixApplication.connection.sendPacket(iSPacket); 
		    			Log.i("XMPPClient", "[TPacketListener] TransferStatus feedback (packet received) sent to " + StringUtils.parseName(packet.getFrom()));
		    		} catch (Exception e){
		    			e.printStackTrace();
		    		}	
		    		
		    	 mSexPixApplication.setMessages(messages);				 
				 mSexPixApplication.fTManager.receiveFile((Message) packet);
			 }
		 };
		 
		 PacketListener subscriptionListener = new PacketListener() {
			 public void processPacket(Packet packet) {
				 Presence cPresence = (Presence) packet;
				 if (cPresence.getType().name().equals("subscribe")){
					 Log.w("XMPPPacket", "New packet:");				 
					 Log.i("XMPPPacket", "From:" + packet.getFrom());
					 Log.i("XMPPPacket", "type:" + cPresence.getType().name());
					 
					 String uName = cPresence.getFrom();
					 String phoneNumber = uName.replace("@" + mSexPixApplication.strServerAddress, "");
			//		 String nickName = uName.replace("@" + mSexPixApplication.strServerAddress, "");
					 
					 String nickName = Tools.getNameByNumber(mSexPixApplication, phoneNumber);
					 
					 if (nickName.equals("")){
						 VCard card = new VCard();
						 try {
							 card.load(mSexPixApplication.connection, uName);
							 String getName = card.getNickName();	
							 if (getName != null && !getName.equals("")){
								 nickName = getName;
							 }
						 } catch (Exception e1) {
							 e1.printStackTrace();
						 }
					 }
					 
					 if (nickName.equals("")){
						 nickName = phoneNumber;
					 }


					 try {
						 RosterEntry cEntry = mSexPixApplication.connection.getRoster().getEntry(uName);
						 if (cEntry != null){
							 if (cEntry.getName() != null && !cEntry.getName().equals("")){
								 nickName = cEntry.getName();							
							 }							
						 }						 
						 
						mSexPixApplication.connection.getRoster().createEntry(uName, nickName, null);
					 } catch (XMPPException e) {
						
						e.printStackTrace();
					 }
					 
					 Presence reply = new Presence(Presence.Type.subscribed); 
					 reply.setTo(cPresence.getFrom()); 
					 mSexPixApplication.connection.sendPacket(reply);
					 
					 Presence requestSubscribe = new Presence(Presence.Type.subscribe); 
					 requestSubscribe.setTo(cPresence.getFrom()); 
					 mSexPixApplication.connection.sendPacket(requestSubscribe);						 
				 }	 
		     }
		 };
		 
		 PacketListener pingListener = new PacketListener() {
			 public void processPacket(Packet packet) {
				 Log.w("XMPPClient", "New ping from:" + packet.getFrom());
				 Log.w("XMPPPacket", "New ping from:" + packet.getFrom());		

				 // send feedback to sender						
				 Message iSPacket = new Message(StringUtils.parseBareAddress(packet.getFrom()), Message.Type.normal);
				 iSPacket.setPacketID("ISpong");
				 iSPacket.setBody("_pong");

				 try{
					 mSexPixApplication.connection.sendPacket(iSPacket); 
					 Log.i("XMPPClient", "[TPacketListener] pong sent to " + StringUtils.parseName(packet.getFrom()));
				 } catch (Exception e){
					 e.printStackTrace();
				 }	

			 }
		 };
		 
		 
		 mSexPixApplication.connection.addPacketListener(subscriptionListener, new PacketTypeFilter(Presence.class));
		 mSexPixApplication.connection.addPacketListener(instaSexyFileListener, instaSexyFileFilter);
		 mSexPixApplication.connection.addPacketListener(transferStatusListener, transferStatusFilter);
		 mSexPixApplication.connection.addPacketListener(pingListener, pingFilter);
		 mSexPixApplication.connection.addPacketListener(allPacketsListener, emptyFilter);

		
		
	}

}
