/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>AdHocConferenceChatContact</tt> represents a <tt>ChatContact</tt> in
 * an ad-hoc conference chat.
 *
 * @author Valentin Martinet
 */
public class AdHocConferenceChatContact extends ChatContact
{
    /**
     * The contact associated with this <tt>AdHocConferenceChatContact</tt>.
     */
    private Contact participant;

    /**
     * Creates an instance of <tt>AdHocConferenceChatContact</tt> by passing to 
     * it the <tt>Contact</tt> for which it is created.
     *
     * @param participant the <tt>Contact</tt> for which this
     * <tt>AdHocConferenceChatContact</tt> is created.
     */
    public AdHocConferenceChatContact(Contact participant)
    {
        this.participant = participant;
    }

    /**
     * Returns the descriptor object corresponding to this chat contact.
     * 
     * @return the descriptor object corresponding to this chat contact.
     */
    public Object getDescriptor()
    {
        return participant;
    }

    /**
     * Returns the contact name.
     *
     * @return the contact name
     */
    public String getName()
    {
        String name = participant.getDisplayName();

        if (name == null || name.length() < 1)
            name = GuiActivator.getResources().getI18NString(
                    "service.gui.UNKNOWN");

        return name;
    }

    /**
     * Returns the current presence status for single user chat contacts and
     * null for multi user chat contacts.
     *
     * @return the current presence status for single user chat contacts and
     * null for multi user chat contacts
     */
    public ImageIcon getAvatar()
    {
        byte[] avatarBytes = participant.getImage();

        if (avatarBytes != null && avatarBytes.length > 0)
        {
            return ImageUtils.getScaledRoundedIcon(avatarBytes,
                    AVATAR_ICON_WIDTH,
                    AVATAR_ICON_HEIGHT
            );
        }
        else
            return null;
    }

    /*
     * Implements ChatContact#getUID(). Delegates to
     * Contact#getAddress() because it's supposed to be unique.
     */
    public String getUID()
    {
        return participant.getAddress();
    }

    @Override
    protected byte[] getAvatarBytes() {
        return this.participant.getImage();
    }
}
