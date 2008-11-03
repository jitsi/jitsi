/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chatroomslist.createforms;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>ChatRoomNamePanel</tt> is the form, where we should enter the chat
 * room name.
 *  
 * @author Yana Stamcheva
 */
public class ChatRoomNamePanel
    extends JPanel
    implements DocumentListener
{
   
   private JLabel nameLabel = new JLabel(
        Messages.getI18NString("chatRoomName").getText());
    
    private JTextField textField = new JTextField();
    
    private JPanel dataPanel = new JPanel(new BorderLayout(5, 5));
    
    private SIPCommMsgTextArea infoLabel 
        = new SIPCommMsgTextArea(
            Messages.getI18NString("chatRoomNameInfo").getText());
    
    private JLabel infoTitleLabel = new JLabel(
        Messages.getI18NString("createChatRoom").getText());
    
    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ADD_CONTACT_WIZARD_ICON)));
    
    private JPanel labelsPanel = new JPanel(new GridLayout(0, 1, 10, 10));
    
    private JPanel rightPanel = new JPanel(new BorderLayout());
    
    private WizardContainer parentWizard;
    
    /**
     * Creates and initializes the <tt>ChatRoomNamePanel</tt>.
     */
    public ChatRoomNamePanel()
    {
        this(null);
    }
    
    /**
     * Creates and initializes the <tt>ChatRoomNamePanel</tt>.
     * @param wizard The parent wizard, where this panel will be added
     */
    public ChatRoomNamePanel(WizardContainer wizard)
    {
        super(new BorderLayout());

        this.parentWizard = wizard;

        this.setBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.iconLabel.setBorder(
            BorderFactory.createEmptyBorder(0, 10, 10, 10));

        this.infoLabel.setEditable(false);

        this.dataPanel.add(nameLabel, BorderLayout.WEST);

        this.dataPanel.add(textField, BorderLayout.CENTER);

        this.infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);
        this.infoTitleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18));

        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);
        this.labelsPanel.add(dataPanel);

        this.rightPanel.setBorder(
            BorderFactory.createEmptyBorder(0, 10, 10, 10));

        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);

        this.add(iconLabel, BorderLayout.WEST);
        this.add(rightPanel, BorderLayout.CENTER);

        this.textField.getDocument().addDocumentListener(this);
    }

    /**
     * Returns the chat room name entered by user.
     * @return the chat room name entered by user
     */
    public String getChatRoomName()
    {
        return textField.getText();
    }

    /**
     * Requests the current focus in the chat room name field.
     */
    public void requestFocusInField()
    {
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

    /**
     * Enables or disables the Next/Finish button of the parent wizard,
     * depending on whether the text field is empty.
     */
    public void setNextFinishButtonAccordingToUIN()
    {
        if(parentWizard != null)
        {
            if(textField.getText() != null && textField.getText().length() > 0)
            {
                parentWizard.setNextFinishButtonEnabled(true);
            }
            else
            {
                parentWizard.setNextFinishButtonEnabled(false);
            }
        }
    }
}
