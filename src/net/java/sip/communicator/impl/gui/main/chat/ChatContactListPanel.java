/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
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
    implements  MouseListener,
                KeyListener
{
    private JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    private JScrollPane contactsScrollPane = new JScrollPane();

    private JPanel mainPanel = new JPanel(new BorderLayout());

    private SIPCommButton addToChatButton = new SIPCommButton(
        ImageLoader.getImage(ImageLoader.ADD_TO_CHAT_BUTTON),
        ImageLoader.getImage(ImageLoader.ADD_TO_CHAT_ROLLOVER_BUTTON),
        ImageLoader.getImage(ImageLoader.ADD_TO_CHAT_ICON), null);

    private JPanel contactsPanel = new JPanel();

    private LinkedHashMap chatContacts = new LinkedHashMap(); 

    private ChatPanel chatPanel;

    /**
     * Creates an instance of <tt>ChatContactListPanel</tt>.
     */
    public ChatContactListPanel(ChatPanel chat)
    {
        super(new BorderLayout(5, 5));

        this.chatPanel = chat;

        this.contactsPanel.setLayout(
            new BoxLayout(contactsPanel, BoxLayout.Y_AXIS));

        //this.setMinimumSize(new Dimension(150, 100));

        this.contactsPanel.setLayout(new BoxLayout(this.contactsPanel,
                BoxLayout.Y_AXIS));

        this.contactsScrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.mainPanel.add(contactsPanel, BorderLayout.NORTH);

        this.contactsScrollPane.getViewport().add(this.mainPanel);

        this.add(contactsScrollPane, BorderLayout.CENTER);

        if(chatPanel instanceof ConferenceChatPanel)
        {
            this.buttonPanel.add(addToChatButton);
            this.add(buttonPanel, BorderLayout.SOUTH);

            addToChatButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    ChatInviteDialog inviteDialog
                        = new ChatInviteDialog(chatPanel);

                    inviteDialog.setVisible(true);
                }
            });
        }

        this.addKeyListener(this);
    }

    /**
     * Adds a <tt>ChatContact</tt> to the list of contacts contained in the
     * chat.
     * 
     * @param chatContact the <tt>ChatContact</tt> to add
     */
    public void addContact(ChatContact chatContact)
    {
        synchronized (chatContacts)
        {
            ChatContactPanel chatContactPanel = new ChatContactPanel(
                chatPanel, chatContact);

            this.contactsPanel.add(chatContactPanel);
            
            this.contactsPanel.revalidate();
            this.contactsPanel.repaint();
            
            if(chatContacts.isEmpty())
                chatContactPanel.setSelected(true);
            
            this.chatContacts.put(chatContact, chatContactPanel);
            
            chatContactPanel.addMouseListener(this);
        }
    }
    
    /**
     * Removes the given <tt>ChatContact</tt> from the list of chat contacts.
     * 
     * @param chatContact the <tt>ChatContact</tt> to remove
     */
    public void removeContact(ChatContact chatContact)
    {
        synchronized (chatContacts)
        {   
            if(!chatContacts.containsKey(chatContact))
                return;

            ChatContactPanel chatContactPanel
                = (ChatContactPanel) chatContacts.get(chatContact);

            contactsPanel.remove(chatContactPanel);
            
            this.contactsPanel.revalidate();
            this.contactsPanel.repaint();
            
            chatContacts.remove(chatContact);
            
            chatContactPanel.removeMouseListener(this);            
        }
    }

    /**
     * In the corresponding <tt>ChatContactPanel</tt> changes the name of the
     * given <tt>Contact</tt>.
     * 
     * @param chatContact the <tt>ChatContact</tt> to be renamed
     */
    public void renameContact(ChatContact chatContact)
    {
        synchronized (chatContacts)
        {
            ChatContactPanel chatContactPanel = null;
            if(chatContacts.containsKey(chatContact))
            {
                chatContactPanel
                    = (ChatContactPanel)chatContacts.get(chatContact);
            
                chatContactPanel.renameContact(chatContact.getName());
            }
        }       
    }

    /**
     * Returns the list of <tt>ChatContacts</tt> contained in this container. 
     * @return the list of <tt>ChatContacts</tt> contained in this container
     */
    public Iterator getChatContacts()
    {
        synchronized (chatContacts)
        {
            return chatContacts.keySet().iterator();
        }
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

    public void mouseClicked(MouseEvent e)
    {   
    }

    public void mouseEntered(MouseEvent e)
    {   
    }

    public void mouseExited(MouseEvent e)
    {   
    }

    public void mousePressed(MouseEvent e)
    {
        synchronized (chatContacts)
        {
            // Reduce all other chat contact panels before expanding the
            // selected one.
            Iterator chatContactPanels = chatContacts.values().iterator();
            
            while(chatContactPanels.hasNext())
            {
                ChatContactPanel panel
                    = (ChatContactPanel) chatContactPanels.next();
                
                panel.setSelected(false);
            }
            
            ChatContactPanel chatContactPanel = (ChatContactPanel) e.getSource();
            
            chatContactPanel.setSelected(true);
            
            this.requestFocus();
        }
    }

    public void mouseReleased(MouseEvent e)
    {   
    }

    public void keyTyped(KeyEvent e)
    {   
    }
    
    public void keyPressed(KeyEvent e)
    {
        
        if (e.getKeyCode() == KeyEvent.VK_DOWN)
        {
            Iterator contacts = chatContacts.keySet().iterator();
            
            while(contacts.hasNext())
            {
                ChatContact chatContact = (ChatContact) contacts.next();
                
                if(chatContact.isSelected())
                {                    
                    if(contacts.hasNext())
                    {
                        ((ChatContactPanel) chatContacts.get(chatContact))
                            .setSelected(false);
                    
                        ((ChatContactPanel) chatContacts.get(
                            contacts.next())).setSelected(true);
                    }
                    
                    break;
                }   
            }
        }
        else if (e.getKeyCode() == KeyEvent.VK_UP)
        {
            ChatContact previousChatContact = null;
            
            Iterator contacts = chatContacts.keySet().iterator();
            
            while(contacts.hasNext())
            {
                ChatContact chatContact = (ChatContact) contacts.next();
                
                if(chatContact.isSelected())
                {
                    if(previousChatContact != null)
                    {
                        ((ChatContactPanel) chatContacts.get(chatContact))
                            .setSelected(false);
                    
                        ((ChatContactPanel) chatContacts.get(previousChatContact))
                            .setSelected(true);
                    
                    }
                    
                    break;
                }
                
                previousChatContact = chatContact;
            }
        }
    }

    public void keyReleased(KeyEvent e)
    {   
    }
}

