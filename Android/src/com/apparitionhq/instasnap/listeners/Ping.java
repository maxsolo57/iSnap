package com.apparitionhq.instasnap.listeners;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import android.util.Log;


public class Ping {
	public boolean gotPong = false;
	XMPPConnection connection;
	String chosenUser;	
	PacketListener pongListener;
	PacketFilter pongFilter;
	final int timeout = 45000;
	

	public Ping(XMPPConnection pconnection, String pchosenUser) {
		connection = pconnection;
		chosenUser = pchosenUser;
		
		
		pongFilter = new PacketFilter() {
		     public boolean accept(Packet packet) {
		    	 if (packet.getPacketID().contains("ISpong"))
		    	 {
		    		 return true;
		    	 }
		    	 else {
		    		 return false;
		    	 }
		     }
		 };
		
		
		pongListener = new PacketListener() {
			 public void processPacket(Packet packet) {
				 Log.w("XMPPPacket", "[Ping] got pong from:" + packet.getFrom());					 
				 if(StringUtils.parseBareAddress(packet.getFrom()).equals(chosenUser)){
					 gotPong = true;					 
				 }
			 }
		 }; 
	}
	
	public boolean sendPing(){
		connection.addPacketListener(pongListener, pongFilter);
		
		Message iSPacket = new Message(chosenUser, Message.Type.normal);
		iSPacket.setPacketID("ISping");
		iSPacket.setBody("_ping");

		try{
			connection.sendPacket(iSPacket); 
			Log.i("XMPPPacket", "[Ping] ping sent to " + StringUtils.parseName(chosenUser));
		} catch (Exception e){
			e.printStackTrace();
		}
		
		int waited = 0;
        while(!gotPong && (waited < timeout)) {
        	try {
				Thread.sleep(300);
				waited += 300;
			//	Log.i("XMPPPacket", "[Pinging]:" + StringUtils.parseName(chosenUser));
				
				// if user is offline then send packet right now
    			if(connection.getRoster() != null){
    				Presence cPresence = connection.getRoster().getPresence(chosenUser);
    				if((cPresence == null) || (cPresence.getType() == Presence.Type.unavailable)){
    					gotPong = true;
    					Log.w("XMPPPacket", "[Ping] no pong, user went offline:" + StringUtils.parseName(chosenUser));
    				}
    			}	
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
        
        
        connection.removePacketListener(pongListener);		
		return gotPong;		
	}

}
