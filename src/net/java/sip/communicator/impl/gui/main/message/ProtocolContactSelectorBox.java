/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.message;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>ProtocolContactSelectorBox</tt> represents the send via menu in the
 * chat window. The menu contains all protocol specific contacts for the
 * currently selected meta contact chat.
 * 
 * @author Yana Stamcheva
 */
public class ProtocolContactSelectorBox
    extends JMenuBar
    implements ActionListener
{
    private static final Logger logger
        = Logger.getLogger(ProtocolContactSelectorBox.class);

    private ChatPanel chatPanel;
    
    private Hashtable contactsTable = new Hashtable();
    
    private SIPCommMenu menu = new SIPCommMenu();

    private MetaContact metaContact;
    
    private Contact currentProtoContact;
    
    public ProtocolContactSelectorBox(ChatPanel chatPanel,
        MetaContact metaContact, Contact protocolContact)
    {
        this.chatPanel = chatPanel;
        
        this.metaContact = metaContact;
        
        this.currentProtoContact = protocolContact;
        
        this.add(menu);
        
        Iterator protocolContacts = metaContact.getContacts();
        while (protocolContacts.hasNext()) {
            Contact contact = (Contact) protocolContacts.next();

            this.addProtoContact(contact);
        }
        
        this.setSelected(protocolContact);
    }

    /**
     * Adds a protocol contact to the "send via" menu.
     * 
     * @param contact the protocol contact to be added
     */
    public void addProtoContact(Contact contact)
    {
        Image img = createContactStatusImage(contact);

        JMenuItem menuItem = new JMenuItem(
                    contact.getDisplayName(),
                    new ImageIcon(img));

        menuItem.addActionListener(this);
        this.contactsTable.put(contact, menuItem);
        this.menu.add(menuItem);
    }
    
    /**
     * Removes a protocol contact from the "send via" menu. This method is used
     * to update the "send via" menu when a proto contact has been removed or
     * moved in the contact list. 
     * 
     * @param contact the proto contact to be removed
     */
    public void removeProtoContact(Contact contact)
    {   
        this.menu.remove((JMenuItem)contactsTable.get(contact));
        this.contactsTable.remove(contact);
    }
    
    /**
     * The listener of the protocol contact's selector box.
     */
    public void actionPerformed(ActionEvent e) {
        JMenuItem menuItem = (JMenuItem) e.getSource();

        Enumeration i = contactsTable.keys();
        while(i.hasMoreElements()) {
            Contact protocolContact = (Contact) i.nextElement();

            if (contactsTable.get(protocolContact).equals(menuItem))
            {
                this.setSelected(protocolContact, (ImageIcon)menuItem.getIcon());

                return;
            }
        }
        logger.debug( "Could not find contact for menu item "
                      + menuItem.getText() + ". contactsTable("
                      + contactsTable.size()+") is : "
                      + contactsTable);
    }

    /**
     * Obtains the status icon for the given protocol contact and
     * adds to it the account index information.
     * @param protoContact the proto contact for which to create the image
     * @return the indexed status image
     */
    public Image createContactStatusImage(Contact protoContact)
    {
        Image statusImage = ImageLoader.getBytesInImage(
                protoContact.getPresenceStatus().getStatusIcon());

        int index = chatPanel.getChatWindow().getMainFrame()
            .getProviderIndex(protoContact.getProtocolProvider());

        Image img = null;
        if(index > 0) {
            BufferedImage buffImage = new BufferedImage(
                    22, 16, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = (Graphics2D)buffImage.getGraphics();
            AlphaComposite ac =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER);

            AntialiasingManager.activateAntialiasing(g);
            g.setColor(Color.DARK_GRAY);
            g.setFont(Constants.FONT.deriveFont(Font.BOLD, 9));
            g.drawImage(statusImage, 0, 0, null);
            g.setComposite(ac);
            g.drawString(new Integer(index+1).toString(), 14, 8);

            img = buffImage;
        }
        else {
            img = statusImage;
        }
        return img;
    }

    /**
     * Updates the protocol contact status.
     * @param protoContact the protocol contact to update
     */
    public void updateContactStatus(Contact protoContact)
    {
        JMenuItem menuItem;
        Icon icon;
        
        if (protoContact.equals(currentProtoContact)
            && !protoContact.getPresenceStatus().isOnline())
        {
            Contact newContact
                = metaContact.getDefaultContact();
            
            if(newContact.getPresenceStatus().isOnline())
                this.setSelected(newContact);
        }
        
        if (!containsOtherOnlineContacts(protoContact)
            && protoContact.getPresenceStatus().isOnline())
        {
            this.setSelected(protoContact);
        }
                
        menuItem = (JMenuItem)contactsTable.get(protoContact);
        icon = new ImageIcon(createContactStatusImage(protoContact));
        
        menuItem.setIcon(icon);
        if(menu.getSelectedObject().equals(protoContact))
        {
            this.menu.setIcon(icon);
        }
    }

    /**
     * In the "send via" menu selects the given contact and sets the given icon
     * to the "send via" menu button.
     * 
     * @param protoContact
     * @param icon
     */
    private void setSelected(Contact protoContact, ImageIcon icon)
    {
        this.currentProtoContact = protoContact;
        
        this.menu.setSelected(protoContact, icon);
        
        String tooltipText;
        
        if(!protoContact.getDisplayName().equals(protoContact.getAddress()))
            tooltipText = protoContact.getDisplayName()
                + " (" + protoContact.getAddress() + ")";
        else
            tooltipText = protoContact.getDisplayName();
        
        this.menu.setToolTipText(tooltipText);        
    }
    
    /**
     * Sets the selected contact to the given proto contact.
     * @param protoContact the proto contact to select
     */
    public void setSelected(Contact protoContact)
    {
        this.setSelected(protoContact,
                new ImageIcon(createContactStatusImage(protoContact)));        
    }
    
    /**
     * Returns the protocol menu.
     * 
     * @return the protocol menu
     */
    public SIPCommMenu getMenu()
    {
        return menu;
    }

    /**
     * Returns the currently selected protocol contact.
     * @return the currently selected protocol contact
     */
    public Contact getSelectedProtocolContact()
    {
        return currentProtoContact;
    }
    
    /**
     * Searches online contacts in the send via combo box.
     * 
     * @return TRUE if the send via combo box contains online contacts, otherwise
     * returns FALSE.
     */
    private boolean containsOtherOnlineContacts(Contact contact)
    {
        Enumeration e = contactsTable.keys();
        
        while(e.hasMoreElements())
        {
            Contact comboContact = (Contact) e.nextElement();
            
            if(!comboContact.equals(contact)
                && comboContact.getPresenceStatus().isOnline())
                return true;
        }
        
        return false;
    }
}
