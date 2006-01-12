package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Image;

import net.java.sip.communicator.impl.gui.main.utils.Constants;

public class MsgToolbarButton extends SIPCommButton {

	
	public MsgToolbarButton(Image iconImage){
		super(	Constants.MSG_TOOLBAR_BUTTON_BG,
				Constants.MSG_TOOLBAR_ROLLOVER_BUTTON_BG,
				iconImage);
	}
}
