/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The <tt>ChatRoomNamePanel</tt> is the form, where we should enter the chat
 * room name.
 *  
 * @author Yana Stamcheva
 */
public class JoinChatRoomPanel
    extends JPanel
{    
    private JLabel chatRoomLabel = new JLabel(
        Messages.getI18NString("chatRoomName").getText());
    
    private JTextField textField = new JTextField();
    
    private JPanel dataPanel = new JPanel(new BorderLayout(5, 5));
    
    private SIPCommMsgTextArea infoLabel 
        = new SIPCommMsgTextArea(
            Messages.getI18NString("joinChatRoomName").getText());
    
    private JLabel infoTitleLabel = new JLabel(
        Messages.getI18NString("joinChatRoomTitle").getText());
    
    
    private JPanel labelsPanel = new JPanel(new GridLayout(0, 1));
    
    private JPanel rightPanel = new JPanel(new BorderLayout());
        
    /**
     * Creates and initializes the <tt>ChatRoomNamePanel</tt>.
     */
    public JoinChatRoomPanel()
    {
        super(new BorderLayout());
        
        this.infoLabel.setEditable(false);
                
        this.dataPanel.add(chatRoomLabel, BorderLayout.WEST);
        
        this.dataPanel.add(textField, BorderLayout.CENTER);
                
        this.infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);
        this.infoTitleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18));
        
        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);
        this.labelsPanel.add(dataPanel);
        
        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);
        
        this.add(rightPanel, BorderLayout.CENTER);
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
     * Sets the given chat room name to the text field, contained in this panel.
     * 
     * @param chatRoomName the chat room name to set to the text field
     */
    public void setChatRoomName(String chatRoomName)
    {
        textField.setText(chatRoomName);
    }

    /**
     * Requests the focus in the name text field.
     */
    public void requestFocusInField()
    {
        this.textField.requestFocus();
    }
}
