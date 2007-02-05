/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.i18n.I18NString;
import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.GuiUtils;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;

/**
 * The <tt>ChatSendPanel</tt> is the panel in the bottom of the chat. It
 * contains the send button, the status panel, where typing notifications are
 * shown and the selector box, where the protocol specific contact is choosen.
 * 
 * @author Yana Stamcheva
 */
public class ChatSendPanel
    extends JPanel
    implements ActionListener
{
    
    private I18NString sendString = Messages.getI18NString("send");
    
    private JButton sendButton = new JButton(sendString.getText());

    private JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    private JPanel sendPanel = new JPanel(new BorderLayout(3, 0));

    private JLabel statusLabel = new JLabel();

    private JLabel sendViaLabel = new JLabel(
        Messages.getI18NString("sendVia").getText());

    private ChatPanel chatPanel;

    private ProtocolContactSelectorBox contactSelectorBox;
    
    private MetaContact currentMetaContact;

    /**
     * Creates an instance of <tt>ChatSendPanel</tt>.
     * 
     * @param chatPanel The parent <tt>ChatPanel</tt>.
     */
    public ChatSendPanel(ChatPanel chatPanel,
            MetaContact metaContact, Contact protocolContact)
    {

        super(new BorderLayout(5, 5));

        this.chatPanel = chatPanel;

        contactSelectorBox = new ProtocolContactSelectorBox(
            chatPanel, metaContact, protocolContact);
        
        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        this.statusPanel.add(statusLabel);

        this.sendPanel.add(sendButton, BorderLayout.EAST);
        this.sendPanel.add(contactSelectorBox, BorderLayout.CENTER);
        this.sendPanel.add(sendViaLabel, BorderLayout.WEST);

        this.add(statusPanel, BorderLayout.CENTER);
        this.add(sendPanel, BorderLayout.EAST);

        this.sendButton.addActionListener(this);
        this.sendButton.setMnemonic(sendString.getMnemonic());
    }

    /**
     * Defines actions when send button is pressed.
     * 
     * @param e The <tt>ActionEvent</tt> object.
     */
    public void actionPerformed(ActionEvent e)
    {
        if (!this.chatPanel.isWriteAreaEmpty()) {
            OperationSetBasicInstantMessaging im = this.chatPanel
                .getImOperationSet();

            Message msg = im.createMessage(chatPanel.getTextFromWriteArea());

            this.chatPanel.getChatWindow().getMainFrame()
                .getWaitToBeDeliveredMsgs().put(msg.getMessageUID(),
                    this.chatPanel);

            Contact contact = (Contact) contactSelectorBox.getMenu()
                .getSelectedObject();

            if (chatPanel.getTnOperationSet() != null) {
                // Send TYPING STOPPED event before sending the message
                chatPanel.stopTypingNotifications();
            }

            chatPanel.requestFocusInWriteArea();

            try {
                im.sendInstantMessage(contact, msg);
            }
            catch (IllegalStateException ex) {
                chatPanel.refreshWriteArea();
                
                chatPanel.processMessage(
                        contact.getDisplayName(),
                        new Date(System.currentTimeMillis()),
                        Constants.OUTGOING_MESSAGE,
                        msg.getContent());
                
                chatPanel.processMessage(
                        contact.getDisplayName(),
                        new Date(System.currentTimeMillis()),
                        Constants.ERROR_MESSAGE,
                        Messages.getI18NString("msgSendConnectionProblem")
                            .getText());
            }
        }
    }

    /**
     * Returns the send button.
     * 
     * @return The send button.
     */
    public JButton getSendButton()
    {
        return sendButton;
    }

    /**
     * Sets the message text to the status panel in the bottom of the chat
     * window. Used to show typing notification messages, links' hrefs, etc.
     * 
     * @param statusMessage The message text to be displayed.
     */
    public void setStatusMessage(String statusMessage)
    {
        int stringWidth = GuiUtils.getStringWidth(statusLabel, statusMessage);

        while (stringWidth > statusPanel.getWidth() - 10) {
            if (statusMessage.endsWith("...")) {
                statusMessage = statusMessage.substring(0,
                    statusMessage.indexOf("...") - 1).concat("...");
            }
            else {
                statusMessage = statusMessage.substring(0,
                    statusMessage.length() - 3).concat("...");
            }
            stringWidth = GuiUtils.getStringWidth(statusLabel, statusMessage);
        }
        statusLabel.setText(statusMessage);
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
     * 
     * @return the protocol contact selector box.
     */
    public ProtocolContactSelectorBox getProtoContactSelectorBox()
    {
        return contactSelectorBox;
    }

    /**
     * Overrides the <code>javax.swing.JComponent.paint()</code> to provide a
     * new round border for the status panel.
     * 
     * @param g The Graphics object.
     */
    public void paint(Graphics g)
    {
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

    public MetaContact getCurrentMetaContact()
    {
        return currentMetaContact;
    }
}
