/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message.toolBars;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;

import net.java.sip.communicator.impl.gui.main.customcontrols.MsgToolbarButton;
import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommButton;
import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommToolBar;
import net.java.sip.communicator.impl.gui.main.history.HistoryWindow;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.message.ChatWindow;
import net.java.sip.communicator.impl.gui.main.message.SmiliesSelectorBox;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;

public class MainToolBar extends SIPCommToolBar 
	implements ActionListener {

	private MsgToolbarButton copyButton 
		= new MsgToolbarButton(ImageLoader.getImage(ImageLoader.COPY_ICON));
	
	private MsgToolbarButton cutButton 
		= new MsgToolbarButton(ImageLoader.getImage(ImageLoader.CUT_ICON));
	
	private MsgToolbarButton pasteButton 
		= new MsgToolbarButton(ImageLoader.getImage(ImageLoader.PASTE_ICON));
	
	private MsgToolbarButton smilyButton 
		= new MsgToolbarButton(ImageLoader.getImage(ImageLoader.SMILIES_ICON));
	
	private MsgToolbarButton saveButton 
		= new MsgToolbarButton(ImageLoader.getImage(ImageLoader.SAVE_ICON));
			
	private MsgToolbarButton printButton 
		= new MsgToolbarButton(ImageLoader.getImage(ImageLoader.PRINT_ICON));

	private MsgToolbarButton previousButton 
		= new MsgToolbarButton(ImageLoader.getImage(ImageLoader.PREVIOUS_ICON));
	
	private MsgToolbarButton nextButton 
		= new MsgToolbarButton(ImageLoader.getImage(ImageLoader.NEXT_ICON));

	private MsgToolbarButton historyButton 
		= new MsgToolbarButton(ImageLoader.getImage(ImageLoader.HISTORY_ICON));
		
	private MsgToolbarButton sendFileButton 
		= new MsgToolbarButton(ImageLoader.getImage(ImageLoader.SEND_FILE_ICON));
	
	private MsgToolbarButton fontButton 
		= new MsgToolbarButton(ImageLoader.getImage(ImageLoader.FONT_ICON));
			
	private JLabel toolbarDivider = new JLabel(new ImageIcon
												(ImageLoader.getImage(ImageLoader.TOOLBAR_DIVIDER)));
	
	private ChatWindow messageWindow;
	
	public MainToolBar (ChatWindow messageWindow){
	
		this.messageWindow = messageWindow;
		
		this.setRollover(true);
		this.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 0));
		this.setBorder(BorderFactory.createEmptyBorder(2, 2, 5, 2));				
		
		this.add(saveButton);
		this.add(printButton);
		
		this.addSeparator();
		
		this.add(cutButton);
		this.add(copyButton);		
		this.add(pasteButton);		
		
		this.addSeparator();
		
		this.add(smilyButton);
		
		this.addSeparator();
		
		this.add(previousButton);
		this.add(nextButton);
		
		this.addSeparator();
		
		this.add(sendFileButton);
		this.add(historyButton);
		
		this.addSeparator();
		
		this.add(fontButton);
		
		this.saveButton.setName("save");
		this.saveButton.setToolTipText(Messages.getString("save"));
        
        this.printButton.setName("print");
        this.printButton.setToolTipText(Messages.getString("print"));
		
        this.cutButton.setName("cut");
        this.cutButton.setToolTipText(Messages.getString("cut"));
		
        this.copyButton.setName("copy");
        this.copyButton.setToolTipText(Messages.getString("copy"));
        
		this.pasteButton.setName("paste");
        this.pasteButton.setToolTipText(Messages.getString("paste"));
        
		this.smilyButton.setName("smily");
        this.smilyButton.setToolTipText(Messages.getString("smily"));
        
		this.previousButton.setName("previous");
        this.previousButton.setToolTipText(Messages.getString("previous"));
        
		this.nextButton.setName("next");
        this.nextButton.setToolTipText(Messages.getString("next"));
        
		this.sendFileButton.setName("sendFile");
        this.sendFileButton.setToolTipText(Messages.getString("sendFile"));
        
		this.historyButton.setName("history");
        this.historyButton.setToolTipText(Messages.getString("history"));
        
		this.fontButton.setName("font");
        this.fontButton.setToolTipText(Messages.getString("font"));
        
		this.saveButton.addActionListener(this);
		this.printButton.addActionListener(this);
		this.cutButton.addActionListener(this);
		this.copyButton.addActionListener(this);
		this.pasteButton.addActionListener(this);
		this.smilyButton.addActionListener(this);
		this.previousButton.addActionListener(this);
		this.nextButton.addActionListener(this);
		this.sendFileButton.addActionListener(this);
		this.historyButton.addActionListener(this);
		this.fontButton.addActionListener(this);
		
		//Disable all buttons that do nothing.
		this.saveButton.setEnabled(false);
		this.printButton.setEnabled(false);
		this.previousButton.setEnabled(false);
		this.nextButton.setEnabled(false);
		this.sendFileButton.setEnabled(false);
		this.historyButton.setEnabled(false);
		this.fontButton.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		
		MsgToolbarButton button = (MsgToolbarButton)e.getSource();
		String buttonText = button.getName();
		
		if (buttonText.equalsIgnoreCase("save")) {
			
		} else if (buttonText.equalsIgnoreCase("print")) {
			
		} else if (buttonText.equalsIgnoreCase("cut")) {
			
			JEditorPane editorPane 
				= this.messageWindow.getWriteMessagePanel().getEditorPane();
			
			editorPane.cut();
			
		} else if (buttonText.equalsIgnoreCase("copy")) {
			
			JEditorPane editorPane 
				= this.messageWindow.getConversationPanel()
					.getChatEditorPane();
			
			if(editorPane.getSelectedText() == null){
				editorPane 
					= this.messageWindow.getWriteMessagePanel().getEditorPane();
			}
			editorPane.copy();
			
		} else if (buttonText.equalsIgnoreCase("paste")) {
			
			JEditorPane editorPane 
				= this.messageWindow.getWriteMessagePanel().getEditorPane();
			
			editorPane.paste();
			
		} else if (buttonText.equalsIgnoreCase("smily")) {
			
			if (e.getSource() instanceof SIPCommButton){				
				
				SmiliesSelectorBox smiliesBox = new SmiliesSelectorBox(ImageLoader.getDefaultSmiliesPack());
				
				if (!smiliesBox.isVisible()) {
					
					smiliesBox.setInvoker((Component)e.getSource());
					
					smiliesBox.setMessageWindow(this.messageWindow);
					
					smiliesBox.setLocation(smiliesBox.getPopupLocation());
					
					smiliesBox.setVisible(true);			
				}		
			}
			
		} else if (buttonText.equalsIgnoreCase("previous")) {
			
		} else if (buttonText.equalsIgnoreCase("next")) {
			
		} else if (buttonText.equalsIgnoreCase("sendFile")) {
			
		} else if (buttonText.equalsIgnoreCase("history")) {
			
			HistoryWindow history = new HistoryWindow();
			
			history.setContacts(messageWindow
                                .getCurrentChatPanel().getChatContacts());
			history.setVisible(true);
			
		} else if (buttonText.equalsIgnoreCase("font")) {
			
		}
		
	}
}
