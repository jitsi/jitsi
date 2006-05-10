/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import net.java.sip.communicator.impl.gui.utils.ImageLoader.ImageID;

public class Smily {

	private ImageID imageID;
	
	private String[] smilyStrings;

	public Smily(ImageID imageID, String[] smilyStrings){
		
		this.imageID = imageID;
		
		this.setSmilyStrings(smilyStrings);
	}
		
	public String[] getSmilyStrings() {
		
		return smilyStrings;
	}

	public void setSmilyStrings(String[] smilyStrings) {
		
		this.smilyStrings = smilyStrings;
	}
	
	public String getDefaultString(){
		
		return this.smilyStrings[0];
	}
	
	public ImageID getImageID(){
		
		return this.imageID;
	}
	
	public String getImagePath(){
		return Images.getString(this.getImageID().getId());
	}
}
