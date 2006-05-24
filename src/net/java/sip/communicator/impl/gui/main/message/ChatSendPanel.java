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
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommSelectorBox;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;

/**
 * The ChatSendPanel is the panel in the bottom of the chat. It contains
 * the send button, the status panel, where typing notifications are 
 * shown and the selector box, where the protocol specific contact is 
 * choosen.
 * @author Yana Stamcheva
 */
public class ChatSendPanel extends JPanel implements ActionListener {

    private JButton sendButton = new JButton(Messages.getString("send"));

    private JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    private JPanel sendPanel = new JPanel(new BorderLayout(3, 0));

    private JLabel statusLabel = new JLabel();

    private ChatPanel chatPanel;

    private ArrayList protocolCList = new ArrayList();

    private SIPCommSelectorBox contactSelectorBox = new SIPCommSelectorBox();

    /**
     * Creates an instance of ChatSendPanel.
     * @param chatPanel The parent ChatPanel.
     */
    public ChatSendPanel(ChatPanel chatPanel) {

        super(new BorderLayout(5, 5));

        this.chatPanel = chatPanel;

        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        this.statusPanel.add(statusLabel);

        this.sendPanel.add(sendButton, BorderLayout.CENTER);
        this.sendPanel.add(contactSelectorBox, BorderLayout.WEST);

        this.add(statusPanel, BorderLayout.CENTER);
        this.add(sendPanel, BorderLayout.EAST);

        this.sendButton.addActionListener(this);
    }

    /**
     * Overrides the javax.swing.JComponent.paint() to provide
     * a new round border for the status panel.
     * @param g The Graphics object.
     */
    public void paint(Graphics g) {
        AntialiasingManager.activateAntialiasing(g);

        super.paint(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Constants.CONTACTPANEL_MOVER_START_COLOR);
        g2.setStroke(new BasicStroke(1f));

        g2.drawRoundRect(3, 4, this.statusPanel.getWidth() - 2,
                this.statusPanel.getHeight() - 2, 8, 8);
    }

    /**
     * Defines actions when send buttons is pressed.
     * @param e The ActionEvent object.
     */
    public void actionPerformed(ActionEvent e) {
        JEditorPane messagePane = this.chatPanel.getWriteMessagePanel()
                .getEditorPane();

        if (messagePane.getText() != null 
                && !messagePane.getText().equals("")) {
            OperationSetBasicInstantMessaging im = this.chatPanel
                    .getImOperationSet();

            Message msg = im.createMessage(messagePane.getText());

            this.chatPanel.getChatWindow().getMainFrame()
                    .getWaitToBeDeliveredMsgs().put(msg.getMessageUID(),
                            this.chatPanel);

            Contact contact = (Contact) contactSelectorBox.getSelectedObject();

            //Send TYPING STOPPED event before sending the message            
            chatPanel.getWriteMessagePanel().stopTyping();

            try {
                im.sendInstantMessage(contact, msg);
            } catch (IllegalStateException ex) {
                String errorMsg = Messages
                        .getString("msgSendConnectionProblem");

                String title = Messages.getString("msgDeliveryFailure");

                JOptionPane.showMessageDialog(this, errorMsg, title,
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    
    public JButton getSendButton() {
        return sendButton;
    }

    public void addProtocolContacts(MetaContact metaContact) {

        Iterator protocolContacts = metaContact.getContacts();
        while (protocolContacts.hasNext()) {
            Contact contact = (Contact) protocolContacts.next();

            if (!protocolCList.contains(contact))
                protocolCList.add(contact);

            String protocolName = contact.getProtocolProvider()
                    .getProtocolName();

            contactSelectorBox.addItem(contact.getDisplayName(), new ImageIcon(
                    Constants.getProtocolIcon(protocolName)),
                    new ProtocolItemListener());
        }
    }

    public void setTypingStatus(String statusMessage) {
        statusLabel.setText(statusMessage);
    }

    public void setSelectedProtocolContact(Contact protocolContact) {
        contactSelectorBox.setIcon(new ImageIcon(Constants
                .getProtocolIcon(protocolContact.getProtocolProvider()
                        .getProtocolName())));
        contactSelectorBox.setSelectedObject(protocolContact);
    }

    private class ProtocolItemListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            JMenuItem menuItem = (JMenuItem) e.getSource();
            String itemTitle = menuItem.getText();

            for (int i = 0; i < protocolCList.size(); i++) {
                Contact protocolContact = (Contact) protocolCList.get(i);

                if (protocolContact.getDisplayName().equals(itemTitle)) {
                    OperationSetBasicInstantMessaging im = chatPanel
                            .getChatWindow().getMainFrame().getProtocolIM(
                                    protocolContact.getProtocolProvider());

                    chatPanel.setImOperationSet(im);
                    chatPanel.setProtocolContact(protocolContact);

                    contactSelectorBox.setSelected(menuItem);
                }
            }
        }
    }
}
