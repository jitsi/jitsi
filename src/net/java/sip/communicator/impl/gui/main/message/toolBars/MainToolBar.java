package net.java.sip.communicator.impl.gui.main.message.toolBars;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JToolBar;

import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.customcontrols.MsgToolbarButton;
import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommButton;
import net.java.sip.communicator.impl.gui.main.history.HistoryWindow;
import net.java.sip.communicator.impl.gui.main.message.MessageWindow;

public class MainToolBar extends JToolBar 
	implements ActionListener {

	private MsgToolbarButton copyButton 
		= new MsgToolbarButton(Constants.COPY_ICON);
	
	private MsgToolbarButton cutButton 
		= new MsgToolbarButton(Constants.CUT_ICON);
	
	private MsgToolbarButton pasteButton 
		= new MsgToolbarButton(Constants.PASTE_ICON);
	
	private MsgToolbarButton smilyButton 
		= new MsgToolbarButton(Constants.SMILIES_ICON);
	
	private MsgToolbarButton saveButton 
		= new MsgToolbarButton(Constants.SAVE_ICON);
			
	private MsgToolbarButton printButton 
		= new MsgToolbarButton(Constants.PRINT_ICON);

	private MsgToolbarButton previousButton 
		= new MsgToolbarButton(Constants.PREVIOUS_ICON);
	
	private MsgToolbarButton nextButton 
		= new MsgToolbarButton(Constants.NEXT_ICON);

	private MsgToolbarButton historyButton 
		= new MsgToolbarButton(Constants.HISTORY_ICON);
		
	private MsgToolbarButton sendFileButton 
		= new MsgToolbarButton(Constants.SEND_FILE_ICON);
	
	private MsgToolbarButton fontButton 
		= new MsgToolbarButton(Constants.FONT_ICON);
			
	private JLabel toolbarDivider = new JLabel(new ImageIcon
												(Constants.TOOLBAR_DIVIDER));
	
	private MessageWindow messageWindow;
	
	public MainToolBar (MessageWindow messageWindow){
	
		this.messageWindow = messageWindow;
		
		this.setRollover(true);
		this.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 0));
		this.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));				
		
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
		this.printButton.setName("print");
		this.cutButton.setName("cut");
		this.copyButton.setName("copy");
		this.pasteButton.setName("paste");
		this.smilyButton.setName("smily");
		this.previousButton.setName("previous");
		this.nextButton.setName("next");
		this.sendFileButton.setName("sendFile");
		this.historyButton.setName("history");
		this.fontButton.setName("font");
		
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
	}

	public void actionPerformed(ActionEvent e) {
		
		MsgToolbarButton button = (MsgToolbarButton)e.getSource();
		String buttonText = button.getName();
		
		if (buttonText.equalsIgnoreCase("save")) {
			
		} else if (buttonText.equalsIgnoreCase("print")) {
			
		} else if (buttonText.equalsIgnoreCase("cut")) {
			
		} else if (buttonText.equalsIgnoreCase("copy")) {
			
		} else if (buttonText.equalsIgnoreCase("paste")) {
			
		} else if (buttonText.equalsIgnoreCase("smily")) {
			
		} else if (buttonText.equalsIgnoreCase("previous")) {
			
		} else if (buttonText.equalsIgnoreCase("next")) {
			
		} else if (buttonText.equalsIgnoreCase("sendFile")) {
			
		} else if (buttonText.equalsIgnoreCase("history")) {
			
			HistoryWindow history = new HistoryWindow();
			
			history.setContacts(messageWindow.getChatContacts());
			history.setVisible(true);
			
		} else if (buttonText.equalsIgnoreCase("font")) {
			
		}
		
	}
}
