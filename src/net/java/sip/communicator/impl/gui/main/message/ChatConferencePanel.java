/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ChatConferencePanel</tt> is the panel added on the right of the
 * chat conversation area, containing information for all contacts
 * participating the chat. It contains a list of <tt>ChatContactPanel</tt>s.
 * Each of these panels is containing the name, status, etc. of only one
 * <tt>MetaContact</tt>. There is also a button, which allows to add new
 * contact to the chat. May be we will add another button to remove a contact
 * from the chat which will be disabled for protocols that doesn't allow that.
 * <p>
 * Note that at this moment the conference functionality is not yet implemented.
 * 
 * @author Yana Stamcheva
 */
public class ChatConferencePanel extends JPanel {

    private JScrollPane contactsScrollPane = new JScrollPane();

    private JPanel contactsPanel = new JPanel();

    private JPanel mainPanel = new JPanel(new BorderLayout());

    private SIPCommButton addToChatButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.ADD_TO_CHAT_BUTTON), ImageLoader
            .getImage(ImageLoader.ADD_TO_CHAT_ROLLOVER_BUTTON), ImageLoader
            .getImage(ImageLoader.ADD_TO_CHAT_ICON), null);

    private JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    private ChatContactPanel chatContactPanel;
    
    private ChatPanel chatPanel;

    /**
     * Creates an instance of <tt>ChatConferencePanel</tt>.
     */
    public ChatConferencePanel(ChatPanel chatPanel) {

        super(new BorderLayout(5, 5));

        this.chatPanel = chatPanel;
        
        this.setMinimumSize(new Dimension(150, 100));

        this.init();
    }

    /**
     * Constructs the <tt>ChatConferencePanel</tt>.
     */
    private void init() {
        this.contactsPanel.setLayout(new BoxLayout(this.contactsPanel,
                BoxLayout.Y_AXIS));

        this.mainPanel.add(contactsPanel, BorderLayout.NORTH);
        this.contactsScrollPane.getViewport().add(this.mainPanel);

        this.buttonPanel.add(addToChatButton);

        this.add(contactsScrollPane, BorderLayout.CENTER);
        this.add(buttonPanel, BorderLayout.SOUTH);

        // Disable all unused buttons.
        this.addToChatButton.setEnabled(false);
    }

    /**
     * Adds a <tt>MetaContact</tt> to the chat.
     * @param contactItem The <tt>MetaContact</tt> to be added.
     */
    public void setChatMetaContact(MetaContact contactItem) {

        this.chatContactPanel = new ChatContactPanel(chatPanel, contactItem);

        this.contactsPanel.add(chatContactPanel);
    }

    /**
     * Adds a <tt>MetaContact</tt> to the chat, by specifying the contact
     * presence status.
     * @param contactItem The <tt>MetaContact</tt> to be added.
     * @param status The <tt>PresenceStatus</tt> of the contact.
     */
    public void setChatMetaContact(MetaContact contactItem, 
                                PresenceStatus status) {

        chatContactPanel = new ChatContactPanel(chatPanel, contactItem, status);

        this.contactsPanel.add(chatContactPanel);
    }

    /**
     * Updates the status icon of the contact in this
     * <tt>ChatConferencePanel</tt>.
     * @param status The new <tt>PresenceStatus</tt>.
     */
    public void updateContactStatus(PresenceStatus status) {
        this.chatContactPanel.setStatusIcon(status);
    }
    
    public void updateProtocolContact(Contact contact)
    {
        this.chatContactPanel.updateProtocolContact(contact);
    }
}
