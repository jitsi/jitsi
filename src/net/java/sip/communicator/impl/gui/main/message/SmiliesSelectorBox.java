/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JEditorPane;

import net.java.sip.communicator.impl.gui.main.customcontrols.BoxPopupMenu;
import net.java.sip.communicator.impl.gui.main.customcontrols.MsgToolbarButton;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.impl.gui.main.utils.Smily;

public class SmiliesSelectorBox extends BoxPopupMenu 
	implements ActionListener {

	private ChatWindow messageWindow;

	private ArrayList imageList;
	
	public SmiliesSelectorBox(ArrayList imageList) {
		
		super(imageList.size());	
		
		this.imageList = imageList;
		
		for (int i = 0; i < imageList.size(); i ++){
			
			Smily smily = (Smily)this.imageList.get(i);
						
			MsgToolbarButton imageButton 
								= new MsgToolbarButton(ImageLoader.getImage(smily.getImageID()));
			
			imageButton.setToolTipText(smily.getSmilyStrings()[0]);	
			
			imageButton.addActionListener(this);
						
			this.add(imageButton);
		}
		
	}

	public void actionPerformed(ActionEvent e) {
		
		JButton imageButton = (JButton)e.getSource(); 
		String  buttonText = imageButton.getToolTipText();
		
		for (int i = 0; i < this.imageList.size(); i ++){
			
			Smily smily = (Smily)this.imageList.get(i);
			
			if(buttonText.equals(smily.getSmilyStrings()[0])){
				
				JEditorPane editorPane = this.messageWindow.getWriteMessagePanel().getEditorPane();
				
				editorPane.setText(editorPane.getText() + smily.getSmilyStrings()[0] + " ");
				
				editorPane.requestFocus();
			}
		}		
	}
	
	public ChatWindow getMessageWindow() {
		return messageWindow;
	}

	public void setMessageWindow(ChatWindow messageWindow) {
		this.messageWindow = messageWindow;
	}
}
