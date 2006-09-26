/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.customcontrols.wizard.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.WizardContainer;

/**
 * The <tt>AddContactPanel</tt> is the form for adding a contact. It's used
 * in the "Add Contact" wizard and the "Add Contact" dialog.
 *  
 * @author Yana Stamcheva
 */
public class AddContactPanel
    extends JPanel
    implements DocumentListener
{

    private JLabel uinLabel = new JLabel(Messages.getString("identifier"));
    
    private JTextField textField = new JTextField();
    
    private JPanel dataPanel = new JPanel(new BorderLayout(5, 5));
    
    private SIPCommMsgTextArea infoLabel 
        = new SIPCommMsgTextArea(Messages.getString("addContactIdentifier"));
    
    private JLabel infoTitleLabel = new JLabel(Messages.getString("addContact"));
    
    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ADD_CONTACT_WIZARD_ICON)));
    
    private JPanel labelsPanel = new JPanel(new GridLayout(0, 1));
    
    private JPanel rightPanel = new JPanel(new BorderLayout());
    
    private WizardContainer parentWizard;
    
    /**
     * Creates and initializes the <tt>AddContactPanel</tt>.
     */
    public AddContactPanel()
    {
        this(null);
    }
    
    /**
     * Creates and initializes the <tt>AddContactPanel</tt>.
     * @param wizard The parent wizard, where this add contact panel is added
     */
    public AddContactPanel(WizardContainer wizard)
    {
        super(new BorderLayout());
        
        this.parentWizard = wizard;
        
        this.setPreferredSize(new Dimension(650, 300));
        
        this.iconLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 10));
        
        this.infoLabel.setEditable(false);
                
        this.dataPanel.add(uinLabel, BorderLayout.WEST);
        
        this.dataPanel.add(textField, BorderLayout.CENTER);
        
        this.infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);
        this.infoTitleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18));
        
        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);        
        this.labelsPanel.add(dataPanel);
        
        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);
        
        this.add(iconLabel, BorderLayout.WEST);
        this.add(rightPanel, BorderLayout.CENTER);
    }
    
    /**
     * Returns the string identifier entered by user.
     * @return the string identifier entered by user
     */
    public String getUIN()
    {
        return textField.getText();
    }
    
    public void requestFocusInField() {
        this.textField.requestFocus();
    }

    public void changedUpdate(DocumentEvent e)
    {   
    }

    public void insertUpdate(DocumentEvent e)
    {
        this.setNextFinishButtonAccordingToUIN();
    }

    public void removeUpdate(DocumentEvent e)
    {
        this.setNextFinishButtonAccordingToUIN();
    }
    
    private void setNextFinishButtonAccordingToUIN()
    {
        if(parentWizard != null) {
            if(textField.getText() != null && textField.getText() != ""){
                parentWizard.setNextFinishButtonEnabled(true);
            }
            else {
                parentWizard.setNextFinishButtonEnabled(false);
            }
        }
    }
}
