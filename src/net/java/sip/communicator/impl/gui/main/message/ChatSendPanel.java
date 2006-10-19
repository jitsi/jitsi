/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.message;

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ChatSendPanel</tt> is the panel in the bottom of the chat. It
 * contains the send button, the status panel, where typing notifications are 
 * shown and the selector box, where the protocol specific contact is 
 * choosen.
 * 
 * @author Yana Stamcheva
 */
public class ChatSendPanel extends JPanel implements ActionListener {

    private JButton sendButton = new JButton(Messages.getString("send"));

    private JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    private JPanel sendPanel = new JPanel(new BorderLayout(3, 0));

    private JLabel statusLabel = new JLabel();
    
    private JLabel sendViaLabel = new JLabel(Messages.getString("sendVia"));

    private ChatPanel chatPanel;

    private ProtocolContactSelectorBox contactSelectorBox;

    /**
     * Creates an instance of <tt>ChatSendPanel</tt>.
     * @param chatPanel The parent <tt>ChatPanel</tt>.
     */
    public ChatSendPanel(ChatPanel chatPanel) {

        super(new BorderLayout(5, 5));

        this.chatPanel = chatPanel;

        contactSelectorBox = new ProtocolContactSelectorBox(this);
        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        this.statusPanel.add(statusLabel);

        this.sendPanel.add(sendButton, BorderLayout.EAST);
        this.sendPanel.add(contactSelectorBox, BorderLayout.CENTER);
        this.sendPanel.add(sendViaLabel, BorderLayout.WEST);

        this.add(statusPanel, BorderLayout.CENTER);
        this.add(sendPanel, BorderLayout.EAST);

        this.sendButton.addActionListener(this);
        this.sendButton.setToolTipText(
                Messages.getString("sendMessage") + " Ctrl-Enter");
        this.sendButton.setMnemonic(
                Messages.getString("mnemonic.sendMessage").charAt(0));
    }

    /**
     * Defines actions when send button is pressed.
     * @param e The <tt>ActionEvent</tt> object.
     */
    public void actionPerformed(ActionEvent e) {       

        if (!this.chatPanel.isWriteAreaEmpty()) {
            OperationSetBasicInstantMessaging im = this.chatPanel
                    .getImOperationSet();

            Message msg = im.createMessage(chatPanel.getTextFromWriteArea());

            this.chatPanel.getChatWindow().getMainFrame()
                    .getWaitToBeDeliveredMsgs().put(msg.getMessageUID(),
                            this.chatPanel);

            Contact contact = (Contact) contactSelectorBox.getSelectedObject();

            if(chatPanel.getTnOperationSet() != null) {
                //Send TYPING STOPPED event before sending the message
                chatPanel.stopTypingNotifications();
            }
            
            chatPanel.requestFocusInWriteArea();
            
            try {
                im.sendInstantMessage(contact, msg);
            } catch (IllegalStateException ex) {                
                SIPCommMsgTextArea errorMsg = new SIPCommMsgTextArea(
                        Messages.getString("msgSendConnectionProblem"));

                String title = Messages.getString("msgDeliveryFailure");

                JOptionPane.showMessageDialog(this, errorMsg, title,
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * Returns the send button.
     * @return The send button.
     */
    public JButton getSendButton() {
        return sendButton;
    }

    /**
     * Initializes the <tt>ContactSelectorBox</tt> with all protocol
     * specific contacts for the given <tt>MetaContact</tt>.
     * 
     * @param metaContact The <tt>MetaContact</tt>.
     */
    public void addProtocolContacts(MetaContact metaContact) {

        Iterator protocolContacts = metaContact.getContacts();
        while (protocolContacts.hasNext()) {
            Contact contact = (Contact) protocolContacts.next();
            
            contactSelectorBox.addContact(contact);
        }
    }    
    
    /**
     * Sets the message text to the status panel in the bottom of the chat
     * window. Used to show typing notification messages, links' hrefs, etc.
     * @param statusMessage The message text to be displayed. 
     */
    public void setStatusMessage(String statusMessage) {
        int stringWidth = StringUtils
            .getStringWidth(statusLabel, statusMessage);
        
        while (stringWidth > statusPanel.getWidth() - 10) {
            if (statusMessage.endsWith("...")) {
                statusMessage = statusMessage
                    .substring(0, statusMessage.indexOf("...") - 1)
                        .concat("...");
            }
            else {
                statusMessage = statusMessage
                    .substring(0, statusMessage.length() - 3)
                        .concat("...");
            }
            stringWidth = StringUtils
                .getStringWidth(statusLabel, statusMessage);
        }   
        statusLabel.setText(statusMessage);
    }
    
    /**
     * Selects the given protocol contact from the list of protocol specific
     * contacts and shows its icon in the component on the left of the "Send"
     * button.
     * 
     * @param protoContact The protocol specific contact to select.
     */
    public void setSelectedProtocolContact(Contact protoContact)
    {
        contactSelectorBox.setSelected(protoContact);
    }
    
    /**
     * 
     * @param protoContact
     */
    public void updateContactStatus(Contact protoContact)
    {
        contactSelectorBox.updateContactStatus(protoContact);
    }
    
    /**
     * Returns the protocol contact selector box.
     * @return the protocol contact selector box.
     */
    public SIPCommSelectorBox getContactSelectorBox() {
        return contactSelectorBox;
    }
    
    /**
     * Overrides the <code>javax.swing.JComponent.paint()</code> to provide
     * a new round border for the status panel.
     * @param g The Graphics object.
     */
    public void paint(Graphics g) {
        AntialiasingManager.activateAntialiasing(g);

        super.paint(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Constants.MOVER_START_COLOR);
        g2.setStroke(new BasicStroke(1f));

        g2.drawRoundRect(3, 4, this.statusPanel.getWidth() - 2,
                this.statusPanel.getHeight() - 2, 8, 8);
    }

    public ChatPanel getChatPanel()
    {
        return chatPanel;
    }
}
