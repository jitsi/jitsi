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
    
    private Hashtable chatContacts = new Hashtable(); 
    
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
     * Adds a <tt>ChatContact</tt> to the list of contacts contained in the
     * chat.
     * 
     * @param chatContact the <tt>ChatContact</tt> to add
     */
    public void addContact(ChatContact chatContact)
    {                
        ChatContactPanel chatContactPanel = new ChatContactPanel(
            chatPanel, chatContact);

        this.contactsPanel.add(chatContactPanel);
        
        this.chatContacts.put(chatContact, chatContactPanel);
    }

    /**
     * In the corresponding <tt>ChatContactPanel</tt> changes the name of the
     * given <tt>Contact</tt>.
     * 
     * @param chatContact the <tt>ChatContact</tt> to be renamed
     */
    public void renameContact(ChatContact chatContact)
    {
        ChatContactPanel chatContactPanel = null;
        if(chatContacts.containsKey(chatContact))
        {
            chatContactPanel = (ChatContactPanel)chatContacts.get(chatContact);
        
            chatContactPanel.renameContact(chatContact.getName());
        }        
    }

    /**
     * Returns the list of <tt>ChatContacts</tt> contained in this container. 
     * @return the list of <tt>ChatContacts</tt> contained in this container
     */
    public Enumeration getChatContacts()
    {
        return chatContacts.keys();
    }
    
    /**
     * Returns the <tt>ChatContactPanel</tt> corresponding to the given
     * <tt>ChatContact</tt>.
     *  
     * @param chatContact the <tt>ChatContact</tt> to search for.
     * @return the <tt>ChatContactPanel</tt> corresponding to the given
     * <tt>ChatContact</tt>
     */
    public ChatContactPanel getChatContactPanel(ChatContact chatContact)
    {
        return (ChatContactPanel) chatContacts.get(chatContact);
    }
}

