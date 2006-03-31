/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.util.Calendar;

public class ChatMessage {

    public static final String OUTGOING_MESSAGE = "OutgoingMessage";
    
    public static final String INCOMING_MESSAGE = "IncomingMessage";
    
	private String senderName;
	
	private Calendar calendar;
	
	private String message;
    
    private String messageType;
   
	public ChatMessage(	String senderName,
						Calendar calendar,
                        String messageType,
						String message){
		
		this.senderName = senderName;
		this.calendar = calendar;
		this.message = message;
        this.messageType = messageType;
	}
	
	public Calendar getCalendar() {
		return calendar;
	}

	public void setCalendar(Calendar calendar) {
		this.calendar = calendar;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}
    
    public String getMessageType(){
        return this.messageType;
    }
}
