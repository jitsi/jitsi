/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.*;

/**
 * A <tt>ChatRoomPropertyChangeEvent</tt> is issued whenever a chat room
 * property has changed. Event codes defined in this class describe properties
 * whose changes are being announced through this event.
 *
 * @author Emil Ivov
 * @author Stephane Remy
 */
public class ChatRoomPropertyChangeEvent
    extends java.beans.PropertyChangeEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The name of the <tt>ChatRoom</tt> subject property.
     */
    public static final String CHAT_ROOM_SUBJECT = "ChatRoomSubject";

    /**
     * The name of the <tt>ChatRoom</tt> subject property.
     */
    public static final String CHAT_ROOM_USER_NICKNAME = "ChatRoomUserNickname";

    /**
     * Creates a <tt>ChatRoomPropertyChangeEvent</tt> indicating that a change
     * has occurred for property <tt>propertyName</tt> in the <tt>source</tt>
     * chat room and that its value has changed from <tt>oldValue</tt> to
     * <tt>newValue</tt>.
     * <p>
     * @param source the <tt>ChatRoom</tt> whose property has changed.
     * @param propertyName the name of the property that has changed.
     * @param oldValue the value of the property before the change occurred.
     * @param newValue the value of the property after the change.
     */
    public ChatRoomPropertyChangeEvent(ChatRoom source,
                                String propertyName,
                                Object oldValue,
                                Object newValue)
    {
        super(source, propertyName, oldValue, newValue);
    }

    /**
     * Returns the source chat room for this event.
     *
     * @return the <tt>ChatRoom</tt> associated with this
     * event.
     */
    public ChatRoom getSourceChatRoom()
    {
        return (ChatRoom)getSource();
    }

    /**
     * Returns a String representation of this event.
     *
     * @return String representation of this event
     */
    public String toString()
    {
        return "ChatRoomPropertyChangeEvent[type="
            + this.getPropertyName()
            + " sourceRoom="
            + this.getSource()
            + "oldValue="
            + this.getOldValue()
            + "newValue="
            + this.getNewValue()
            + "]";
    }
}
