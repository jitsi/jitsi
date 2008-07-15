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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>ChatContact</tt> is a wrapping class for the <tt>Contact</tt> and
 * <tt>ChatRoomMember</tt> interface.
 * 
 * @author Yana Stamcheva
 */
public class ChatContact
{
    private Logger logger = Logger.getLogger(ChatContact.class);

    static final int AVATAR_ICON_HEIGHT = 45;

    static final int AVATAR_ICON_WIDTH = 40;

    private String name;

    private String address;

    private ImageIcon image;

    private ProtocolProviderService protocolProvider;

    private boolean isMultiChatContact;

    private Object sourceContact;

    private boolean isSelected;

    /**
     * Creates an instance of <tt>ChatContact</tt> by passing to it the
     * <tt>Contact</tt> for which it is created.
     * 
     * @param contact the <tt>Contact</tt> for which this <tt>ChatContact</tt>
     * is created
     */
    public ChatContact(Contact contact)
    {
        this(null, contact);
    }
    
    /**
     * Creates an instance of <tt>ChatContact</tt> by passing to it the
     * corresponding <tt>MetaContact</tt> and <tt>Contact</tt>.
     * 
     * @param metaContact the <tt>MetaContact</tt> encapsulating the given
     * <tt>Contact</tt>
     * @param contact the <tt>Contact</tt> for which this <tt>ChatContact</tt>
     * is created
     */
    public ChatContact(MetaContact metaContact, Contact contact)
    {
        this.sourceContact = contact;
        this.address = contact.getAddress();
        this.isMultiChatContact = false;
        this.protocolProvider = contact.getProtocolProvider();

        if(metaContact != null)
            name = metaContact.getDisplayName();
        else
            name = contact.getDisplayName();

        if (name == null || name.length() < 1)
            name = Messages.getI18NString("unknown").getText();
    }
    
    /**
     * Creates an instance of <tt>ChatContact</tt> by passing to it the
     * <tt>ChatRoomMember</tt> for which it is created.
     * 
     * @param chatRoomMember the <tt>ChatRoomMember</tt> for which this
     * <tt>ChatContact</tt> is created.
     */
    public ChatContact(ChatRoomMember chatRoomMember)
    {
        this.sourceContact = chatRoomMember;
        this.address = chatRoomMember.getContactAddress();
        this.isMultiChatContact = true;
        this.protocolProvider = chatRoomMember.getProtocolProvider();
        this.name = chatRoomMember.getName();

        if (name == null || name.length() < 1)
            name = Messages.getI18NString("unknown").getText();
    }

    /**
     * Returns the contact identifier.
     * 
     * @return the contact identifier
     */
    public String getAddress()
    {
        return address;
    }

    /**
     * Returns the contact name.
     * 
     * @return the contact name
     */  
    public String getName()
    {
        return name;
    }
    
    /**
     * Returns the current presence status for single user chat contacts and
     * null for multi user chat contacts.
     * 
     * @return the current presence status for single user chat contacts and
     * null for multi user chat contacts
     */
    public ImageIcon getStatusIcon()
    {
        if(!isMultiChatContact)
        {
            return new ImageIcon(Constants.getStatusIcon(
                ((Contact)sourceContact).getPresenceStatus()));
        }
        else
        {
            return new ImageIcon(
                Constants.getStatusIcon(Constants.ONLINE_STATUS));
        }
    }

    /**
     * Returns the <tt>ProtocolProviderService</tt> of the contact.
     * 
     * @return the <tt>ProtocolProviderService</tt> of the contact
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return protocolProvider;
    }
    
    /**
     * Returns the source contact. It could be an instance of <tt>Contact</tt>
     * or <tt>ChatRoomMember</tt> interface.
     * 
     * @return the source contact
     */
    public Object getSourceContact()
    {
        return sourceContact;
    }
    
    /**
     * Returns the avatar image corresponding to the source contact. In the case
     * of multi user chat contact returns null.
     * 
     * @return the avatar image corresponding to the source contact. In the case
     * of multi user chat contact returns null
     */
    public ImageIcon getImage()
    {
        byte[] contactImage = null;
        
        if(!(sourceContact instanceof Contact))
            return null;
        
        Contact contact = (Contact)sourceContact;
        
        MetaContact metaContact = GuiActivator.getMetaContactListService()
            .findMetaContactByContact(contact);
        
        if(metaContact != null)
        {
            Iterator i = metaContact.getContacts();
            
            while(i.hasNext())
            {
                Contact protoContact = (Contact) i.next();
                
                try
                {
                    contactImage = protoContact.getImage();
                }
                catch (Exception ex)
                {
                    logger.error("Failed to load contact photo.", ex);
                }
                
                if(contactImage != null && contactImage.length > 0)
                    break;
            }
        }
        else if(contact != null)
        {
            try
            {
                contactImage = contact.getImage();
            }
            catch (Exception ex)
            {
                logger.error("Failed to load contact photo.", ex);
            }
        }
        
        if(contactImage != null)
        {
            Image image = ImageLoader.getBytesInImage(contactImage);
            return ImageUtils.scaleIconWithinBounds(
                        new ImageIcon(image),
                        AVATAR_ICON_WIDTH,
                        AVATAR_ICON_HEIGHT
                        );
        }
        else
            return null;
    }
    
    /**
     * Returns <code>true</code> if this is the currently selected contact in
     * the list of contacts for the chat, otherwise returns <code>false</code>.
     * @return <code>true</code> if this is the currently selected contact in
     * the list of contacts for the chat, otherwise returns <code>false</code>.
     */
    public boolean isSelected()
    {
        return isSelected;
    }

    /**
     * Sets this isSelected property of this chat contact.
     * 
     * @param isSelected <code>true</code> to indicate that this contact would
     * be the selected contact in the list of chat window contacts, otherwise -
     * <code>false</code>
     */
    public void setSelected(boolean isSelected)
    {
        this.isSelected = isSelected;
    }
}
