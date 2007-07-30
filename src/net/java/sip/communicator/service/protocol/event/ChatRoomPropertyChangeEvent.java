/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
     * The name of the <tt>ChatRoom</tt> subject property.
     */
    public static final String CHAT_ROOM_SUBJECT = "ChatRoomSubject";
    
    /**
     * The name of the <tt>ChatRoom</tt> redirectChatRoom property.
     */
    public static final String CHAT_ROOM_REDIRECT = "ChatRoomRedirect";
    
    /**
     * The name of the <tt>ChatRoom</tt> memberMaxNumber property.
     */
    public static final String CHAT_ROOM_MEMBER_MAX_NUMBER
        = "ChatRoomMemberMaxNumber";
    
    /**
     * The name of the <tt>ChatRoom</tt> join rate limit property.
     */
    public static final String CHAT_ROOM_JOIN_RATE_LIMIT
        = "ChatRoomJoinRateLimit";
    
    /**
     * The name of the <tt>ChatRoom</tt> password property.
     */
    public static final String CHAT_ROOM_PASSWORD = "ChatRoomPassword";

    /**
     * The name of the <tt>ChatRoom</tt> isVisible property.
     */
    public static final String CHAT_ROOM_IS_VISIBLE= "ChatRoomIsVisible";
    
    /**
     * The name of the <tt>ChatRoom</tt> isInvitationRequired property.
     */
    public static final String CHAT_ROOM_IS_INVITATION_REQUIRED
        = "ChatRoomIsInvitationRequired";
    
    /**
     * The name of the <tt>ChatRoom</tt> isMute property.
     */
    public static final String CHAT_ROOM_IS_MUTE = "ChatRoomIsMute";
    
    /**
     * The name of the <tt>ChatRoom</tt> isAllowExternalMessages property.
     */
    public static final String CHAT_ROOM_IS_ALLOW_EXTERNAL_MESSAGES
        = "ChatRoomIsAllowExternalMessages";
    
    /**
     * The name of the <tt>ChatRoom</tt> isRegistered property.
     */
    public static final String CHAT_ROOM_IS_REGISTERED = "ChatRoomisRegistered";

    /**
     * The name of the <tt>ChatRoom</tt> isSubjectLocked property.
     */
    public static final String CHAT_ROOM_IS_SUBJECT_LOCKED
        = "ChatRoomIsSubjectLocked";

    /**
     * The name of the <tt>ChatRoom</tt> isAllowMessageFormatting property.
     */
    public static final String CHAT_ROOM_IS_ALLOW_MESSAGE_FORMATTING
        = "ChatRoomIsAllowMessageFormatting";
    
    /**
     * The name of the <tt>ChatRoom</tt> isFilterMessageFormatting property.
     */
    public static final String CHAT_ROOM_IS_FILTER_MESSAGE_FORMATTING
        = "ChatRoomIsFilterMessageFormatting";
    
    /**
     * The name of the <tt>ChatRoom</tt> isAllowInvitation property.
     */
    public static final String CHAT_ROOM_IS_ALLOW_INVITATION
        = "ChatRoomIsAllowInvitation";

    /**
     * The name of the <tt>ChatRoom</tt> isAllowInvitationRequest property.
     */
    public static final String CHAT_ROOM_IS_ALLOW_INVITATION_REQUEST
        = "ChatRoomIsAllowInvitationRequest";

    /**
     * The name of the <tt>ChatRoom</tt> isMemberNickNameLocked property.
     */
    public static final String CHAT_ROOM_IS_MEMBER_NICKNAME_LOCKED
        = "ChatRoomIsMemberNicknameLocked";

    /**
     * The name of the <tt>ChatRoom</tt> isAllowKick property.
     */
    public static final String CHAT_ROOM_IS_ALLOW_KICK
        = "ChatRoomIsAllowKick";
    
    /**
     * The name of the <tt>ChatRoom</tt> isRegisteredUsersOnly property.
     */
    public static final String CHAT_ROOM_IS_REGISTERED_USERS_ONLY
        = "ChatRoomIsRegisteredUserOnly";
    
    /**
     * The name of the <tt>ChatRoom</tt> isAllowSpecialMessage property.
     */
    public static final String CHAT_ROOM_IS_ALLOW_SPECIAL_MESSAGE
        = "ChatRoomIsAllowSpecialMessage";
    
    /**
     * The name of the <tt>ChatRoom</tt> isNicknameListVisible property.
     */
    public static final String CHAT_ROOM_IS_NICKNAME_LIST_VISIBLE
        = "ChatRoomIsNicknameListVisible";

    /**
     * The value of the property before the change occurred.
     */
    private Object oldValue;
    
    /**
     * The value of the property after the change.
     */
    private Object newValue;
    
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
     * Return the value of the property before the change occurred.
     * 
     * @return the value of the property before the change occurred.
     */
    public Object getOldValue()
    {
        return oldValue; 
    }
    
    /**
     * Return the value of the property after the change.
     * 
     * @return the value of the property after the change.
     */
    public Object getNewValue()
    {
        return newValue;
    }
    
    /**
     * Returns a String representation of this event.
     */
    public String toString()
    {
        return "ChatRoomPropertyChangeEvent[type="
            + this.getPropertyName()
            + " sourceRoom="
            + this.getSource().toString()
            + "oldValue="
            + this.getOldValue().toString()
            + "newValue="
            + this.getNewValue().toString()
            + "]";
    }
}
