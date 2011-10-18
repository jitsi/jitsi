/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.*;

/**
 * A <tt>ChatRoomMemberPropertyChangeEvent</tt> is issued whenever a chat room
 * member property has changed (such as the nickname for example). Event codes
 * defined in this class describe properties whose changes are being announced
 * through this event.
 *
 * @author Emil Ivov
 * @author Stephane Remy
 */
public class ChatRoomMemberPropertyChangeEvent
    extends java.beans.PropertyChangeEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The nick name of the <tt>ChatRoomMember</tt> property.
     */
    public static final String MEMBER_NICKNAME = "MemberNickname";

    /**
     * The <tt>ChatRoom</tt>, to which the corresponding member belongs.
     */
    private ChatRoom memberChatRoom;

    /**
     * Creates a <tt>ChatRoomMemberPropertyChangeEvent</tt> indicating that a
     * change has occurred for property <tt>propertyName</tt> in the
     * <tt>source</tt> chat room member and that its value has changed from
     * <tt>oldValue</tt> to <tt>newValue</tt>.
     * <p>
     * @param source the <tt>ChatRoomMember</tt> whose property has changed.
     * @param memberChatRoom the <tt>ChatRoom</tt> of the member
     * @param propertyName the name of the property that has changed.
     * @param oldValue the value of the property before the change occurred.
     * @param newValue the value of the property after the change.
     */
    public ChatRoomMemberPropertyChangeEvent(   ChatRoomMember source,
                                                ChatRoom memberChatRoom,
                                                String propertyName,
                                                Object oldValue,
                                                Object newValue)
    {
        super(  source,
                propertyName,
                oldValue,
                newValue);

        this.memberChatRoom = memberChatRoom;
    }

    /**
     * Returns the member of the chat room, for which this event is about.
     *
     * @return the <tt>ChatRoomMember</tt> for which this event is about
     */
    public ChatRoomMember getSourceChatRoomMember()
    {
        return (ChatRoomMember) getSource();
    }

    /**
     * Returns the chat room, to which the corresponding member belongs.
     *
     * @return the chat room, to which the corresponding member belongs
     */
    public ChatRoom getMemberChatRoom()
    {
        return memberChatRoom;
    }

    /**
     * Returns a String representation of this event.
     *
     * @return String representation of this event
     */
    public String toString()
    {
        return "ChatRoomMemberPropertyChangeEvent[type="
            + this.getPropertyName()
            + " sourceRoomMember="
            + this.getSource().toString()
            + "oldValue="
            + this.getOldValue().toString()
            + "newValue="
            + this.getNewValue().toString()
            + "]";
    }
}
