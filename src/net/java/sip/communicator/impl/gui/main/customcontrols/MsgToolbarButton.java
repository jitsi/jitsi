/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Image;

import net.java.sip.communicator.impl.gui.utils.ImageLoader;

public class MsgToolbarButton extends SIPCommButton {
	
	public MsgToolbarButton(Image iconImage){
		super(	ImageLoader.getImage(ImageLoader.MSG_TOOLBAR_BUTTON_BG),
				ImageLoader.getImage(ImageLoader.MSG_TOOLBAR_ROLLOVER_BUTTON_BG),
				iconImage);
	}
}
