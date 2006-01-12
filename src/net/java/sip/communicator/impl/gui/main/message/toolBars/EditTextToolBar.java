package net.java.sip.communicator.impl.gui.main.message.toolBars;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JToolBar;

import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommButton;

public class EditTextToolBar extends JToolBar {

	private SIPCommButton textBoldButton 
		= new SIPCommButton(Constants.TEXT_BOLD_BUTTON,
							Constants.TEXT_BOLD_ROLLOVER_BUTTON);
	
	private SIPCommButton textItalicButton 
		= new SIPCommButton(Constants.TEXT_ITALIC_BUTTON,
							Constants.TEXT_ITALIC_ROLLOVER_BUTTON);
	
	private SIPCommButton textUnderlinedButton 
		= new SIPCommButton(Constants.TEXT_UNDERLINED_BUTTON,
							Constants.TEXT_UNDERLINED_ROLLOVER_BUTTON);

	private SIPCommButton alignLeftButton 
		= new SIPCommButton(Constants.ALIGN_LEFT_BUTTON,
							Constants.ALIGN_LEFT_ROLLOVER_BUTTON);
	
	private SIPCommButton alignRightButton 
		= new SIPCommButton(Constants.ALIGN_RIGHT_BUTTON,
							Constants.ALIGN_RIGHT_ROLLOVER_BUTTON);
	
	private SIPCommButton alignCenterButton 
		= new SIPCommButton(Constants.ALIGN_CENTER_BUTTON,
							Constants.ALIGN_CENTER_ROLLOVER_BUTTON);
	
	private JComboBox fontSizeCombo = new JComboBox();
	
	private JComboBox fontNameCombo = new JComboBox();
	
	public EditTextToolBar (){
		
		this.setRollover(true);
		this.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
		this.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));				
		
		this.fontSizeCombo.setPreferredSize (new Dimension (55, 21));
		
		this.add(textBoldButton);
		this.add(textItalicButton);
		this.add(textUnderlinedButton);
		
		this.addSeparator();		
		
		this.add(fontNameCombo);
		this.add(fontSizeCombo);		
		
		this.addSeparator();
		
		this.add(alignLeftButton);
		this.add(alignCenterButton);
		this.add(alignRightButton);		
	}
}
