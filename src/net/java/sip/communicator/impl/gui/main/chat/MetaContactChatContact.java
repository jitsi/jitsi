/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>MetaContactChatContact</tt> represents a <tt>ChatContact</tt> in a
 * user-to-user chat.
 * 
 * @author Yana Stamcheva
 */
public class MetaContactChatContact
    extends ChatContact
{
    private MetaContact metaContact;

    /**
     * Creates an instance of <tt>ChatContact</tt> by passing to it the
     * corresponding <tt>MetaContact</tt> and <tt>Contact</tt>.
     *
     * @param metaContact the <tt>MetaContact</tt> encapsulating the given
     * <tt>Contact</tt>
     * @param contact the <tt>Contact</tt> for which this <tt>ChatContact</tt>
     * is created
     */
    public MetaContactChatContact(MetaContact metaContact)
    {
        this.metaContact = metaContact;
    }

    /**
     * Returns the descriptor object corresponding to this chat contact.
     * 
     * @return the descriptor object corresponding to this chat contact.
     */
    public Object getDescriptor()
    {
        return metaContact;
    }

    /**
     * Returns the contact name.
     *
     * @return the contact name
     */
    public String getName()
    {
        String name = metaContact.getDisplayName();

        if (name == null || name.length() < 1)
            name = GuiActivator.getResources().getI18NString("unknown");

        return name;
    }

    /**
     * Returns the avatar image corresponding to the source contact. In the case
     * of multi user chat contact returns null.
     *
     * @return the avatar image corresponding to the source contact. In the case
     * of multi user chat contact returns null
     */
    public ImageIcon getAvatar()
    {
        byte[] contactImage = metaContact.getAvatar();

        if(contactImage != null && contactImage.length > 0)
        {
            return ImageUtils.getScaledRoundedImage(
                        contactImage,
                        AVATAR_ICON_WIDTH,
                        AVATAR_ICON_HEIGHT
                        );
        }
        else
            return null;
    }
}
