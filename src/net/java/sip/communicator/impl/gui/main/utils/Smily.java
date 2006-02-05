/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.utils;

import java.awt.Image;

import net.java.sip.communicator.impl.gui.main.utils.ImageLoader.ImageID;

public class Smily {

	private ImageID imageID;
	
	private String[] smilyStrings;

	public Smily(String imagePath, String[] smilyStrings){
		
		this.setImageID(imageID);
		
		this.setSmilyStrings(smilyStrings);
	}
	
	public ImageID getImageID() {
		
		return imageID;
	}

	public void setImageID(ImageID imageID) {
		
		this.imageID = imageID;
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
	
	public String getImagePath(){
		
		return Images.getString(this.imageID.getId());
	}
	
}
