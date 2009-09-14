/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ConferenceChatContact</tt> represents a <tt>ChatContact</tt> in a
 * conference chat.
 * 
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class ConferenceChatContact
    extends ChatContact
{
    private final ChatRoomMember chatRoomMember;

    /**
     * Creates an instance of <tt>ChatContact</tt> by passing to it the
     * <tt>ChatRoomMember</tt> for which it is created.
     *
     * @param chatRoomMember the <tt>ChatRoomMember</tt> for which this
     * <tt>ChatContact</tt> is created.
     */
    public ConferenceChatContact(ChatRoomMember chatRoomMember)
    {
        this.chatRoomMember = chatRoomMember;
    }

    /**
     * Returns the descriptor object corresponding to this chat contact.
     * 
     * @return the descriptor object corresponding to this chat contact.
     */
    public Object getDescriptor()
    {
        return chatRoomMember;
    }

    /**
     * Returns the contact name.
     *
     * @return the contact name
     */
    public String getName()
    {
        String name = chatRoomMember.getName();

        if (name == null || name.length() < 1)
            name = GuiActivator.getResources()
                .getI18NString("service.gui.UNKNOWN");

        return name;
    }

    /**
     * Implements ChatContact#getAvatarBytes(). Delegates to chatRoomMember.
     */
    public byte[] getAvatarBytes()
    {
        return chatRoomMember.getAvatar();
    }

    public ChatRoomMemberRole getRole()
    {
        return chatRoomMember.getRole();
    }

    /**
     * Implements ChatContact#getUID(). Delegates to
     * ChatRoomMember#getContactAddress() because it's supposed to be unique.
     */
    public String getUID()
    {
        return chatRoomMember.getContactAddress();
    }
}
