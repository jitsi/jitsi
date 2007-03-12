/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ChatContactListPanel</tt> is the panel added on the right of the
 * chat conversation area, containing information for all contacts
 * participating the chat. It contains a list of <tt>ChatContactPanel</tt>s.
 * Each of these panels is containing the name, status, etc. of only one
 * <tt>MetaContact</tt> or simple <tt>Contact</tt>. There is also a button,
 * which allows to add new contact to the chat.
 * 
 * @author Yana Stamcheva
 */
public class ChatContactListPanel
    extends JPanel
{
    private JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    
    private JScrollPane contactsScrollPane = new JScrollPane();

    private JPanel mainPanel = new JPanel(new BorderLayout());

    private SIPCommButton addToChatButton = new SIPCommButton(
        ImageLoader.getImage(ImageLoader.ADD_TO_CHAT_BUTTON),
        ImageLoader.getImage(ImageLoader.ADD_TO_CHAT_ROLLOVER_BUTTON),
        ImageLoader.getImage(ImageLoader.ADD_TO_CHAT_ICON), null);

    
    private JPanel contactsPanel = new JPanel();
    
    private Hashtable contacts = new Hashtable(); 
    
    private ChatPanel chatPanel;
    
    /**
     * Creates an instance of <tt>ChatContactListPanel</tt>.
     */
    public ChatContactListPanel(ChatPanel chatPanel)
    {
        super(new BorderLayout(5, 5));

        this.chatPanel = chatPanel;
               
        this.contactsPanel.setLayout(
            new BoxLayout(contactsPanel, BoxLayout.Y_AXIS));
        
        this.setMinimumSize(new Dimension(150, 100));

        this.init();
    }
    
    /**
     * Adds a simple <tt>Contact</tt> to the list of contacts contained in the
     * chat.
     * 
     * @param contact the <tt>Contact</tt> to be added
     */
    public void addContact(Contact contact)
    {
        ChatContactPanel chatContactPanel = new ChatContactPanel(
            chatPanel, contact);

        this.contactsPanel.add(chatContactPanel);
        
        this.contacts.put(contact, chatContactPanel);
    }
    
    /**
     * Adds a <tt>MetaContact</tt> to the list of contacts contained in the chat.
     *  
     * @param metaContact the <tt>MetaContact</tt> to be added
     * @param contact the subcontact which is initially selected
     */
    public void addContact(MetaContact metaContact, Contact contact)
    {
        ChatContactPanel chatContactPanel = new ChatContactPanel(
            chatPanel, metaContact, contact);

        this.contactsPanel.add(chatContactPanel);
        
        this.contacts.put(metaContact, chatContactPanel);
    }

    /**
     * Constructs the <tt>ChatContactListPanel</tt>.
     */
    private void init()
    {
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
     * Updates the status icon of the contact in this
     * <tt>ChatContactListPanel</tt>.
     * @param contact the <tt>Contact</tt>, which should be updated
     */
    public void updateContactStatus(Contact contact)
    {
        ChatContactPanel chatContactPanel = null;
        if(contacts.containsKey(contact))
        {
            chatContactPanel = (ChatContactPanel)contacts.get(contact);
        
            chatContactPanel.setStatusIcon(contact.getPresenceStatus());        
        }
    }

    /**
     * Updates the status icon of the contact in this
     * <tt>ChatContactListPanel</tt>.
     * @param metaContact the <tt>MetaContact</tt>, which should be updated
     */
    public void updateContactStatus(MetaContact metaContact)
    {
        ChatContactPanel chatContactPanel = null;        
        if(contacts.containsKey(metaContact))
        {
            chatContactPanel = (ChatContactPanel)contacts.get(metaContact);
        
            chatContactPanel.setStatusIcon(
                metaContact.getDefaultContact().getPresenceStatus());        
        }
    }

    /**
     * Updates the given protocol contact chat panel in the list. Disables or
     * enable buttons, according to the functionalities supported by this
     * contact.
     * 
     * @param contact the <tt>Contact</tt>, which chat contact panel to update
     */
    public void updateProtocolContact(Contact contact)
    {   
        ChatContactPanel chatContactPanel = null;
        if(contacts.containsKey(contact))
        {
            chatContactPanel = (ChatContactPanel)contacts.get(contact);
        
            chatContactPanel.updateProtocolContact(contact);
        }
    }

    /**
     * In the corresponding <tt>ChatContactPanel</tt> changes the name of the
     * given <tt>Contact</tt>.
     * 
     * @param contact the <tt>Contact</tt>, which has been renamed
     */
    public void renameContact(Contact contact)
    {
        ChatContactPanel chatContactPanel = null;
        if(contacts.containsKey(contact))
        {
            chatContactPanel = (ChatContactPanel)contacts.get(contact);
        
            chatContactPanel.renameContact(contact.getDisplayName());
        }        
    }
 
    /**
     * In the corresponding <tt>ChatContactPanel</tt> changes the name of the
     * given <tt>MetaContact</tt>.
     * 
     * @param contact the <tt>MetaContact</tt>, which has been renamed
     */
    public void renameContact(MetaContact contact)
    {
        ChatContactPanel chatContactPanel = null;
        if(contacts.containsKey(contact))
        {
            chatContactPanel = (ChatContactPanel)contacts.get(contact);
        
            chatContactPanel.renameContact(contact.getDisplayName());
        }        
    }
}

