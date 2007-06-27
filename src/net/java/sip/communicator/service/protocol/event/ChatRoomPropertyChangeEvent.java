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
     * Indicates that this event was triggered as a result of the source
     * chat room subject being changed.
     */
    public static final String SUBJECT_CHANGED = "SubjectChanged";
    
    /**
     * Indicates that this event was triggered as a result of the source
     * chat room ban list being changed.
     */
    public static final String BAN_LIST_CHANGED = "BanListChanged";
    
    /**
     * Indicates that this event was triggered as a result of the source
     * chat room user limit being changed.
     */
    public static final String USER_LIMIT_CHANGED = "UserLimitChanged";
    
    /**
     * Indicates that this event was triggered as a result of the source
     * chat room password being changed.
     */
    public static final String PASSWORD_CHANGED = "PasswordChanged";

    /**
     * Indicates that this event was triggered as a result of the source
     * chat room isPasswordRequired property being changed.
     */
    public static final String IS_PASSWORD_REQUIRED_CHANGED
        = "IsPasswordRequiredChanged";
    
    /**
     * Indicates that this event was triggered as a result of the source
     * chat room isInvitationRequired property being changed.
     */
    public static final String IS_INVITATION_REQUIRED_CHANGED
        = "IsInvitationRequiredChanged";

    /**
     * Indicates that this event was triggered as a result of the source
     * chat room isMemberNumberLimited property being changed.
     */
    public static final String IS_MEMBER_NUMBER_LIMITED_CHANGED
        = "IsMemberNumberLimitedChanged";
    
    /**
     * Indicates that this event was triggered as a result of the source
     * chat room isMute property being changed.
     */
    public static final String IS_MUTE_CHANGED = "IsMuteChanged";
    
    /**
     * Indicates that this event was triggered as a result of the source
     * chat room isAllowExternalMessages property being changed.
     */
    public static final String IS_ALLOW_EXTERNAL_MESSAGES_CHANGED
        = "IsAllowExternalMessagesChanged";
    
    /**
     * Indicates that this event was triggered as a result of the source
     * chat room isRegistered property being changed.
     */
    public static final String IS_REGISTERED_CHANGED = "isRegisteredChanged";

    /**
     * Indicates that this event was triggered as a result of the source
     * chat room isSubjectLocked property being changed.
     */
    public static final String IS_SUBJECT_LOCKED_CHANGED
        = "IsSubjectLockedChanged";

    /**
     * Indicates that this event was triggered as a result of the source
     * chat room isAllowMessageFormatting property being changed.
     */
    public static final String IS_ALLOW_MESSAGE_FORMATTING_CHANGED
        = "IsAllowMessageFormattingChanged";
    
    /**
     * Indicates that this event was triggered as a result of the source
     * chat room isFilterMessageFormatting property being changed.
     */
    public static final String IS_FILTER_MESSAGE_FORMATTING_CHANGED
        = "IsFilterMessageFormattingChanged";

    /**
     * Indicates that this event was triggered as a result of the source
     * chat room isJoinTimeLimited property being changed.
     */
    public static final String IS_JOIN_TIME_LIMITED_CHANGED
        = "IsJoinTimeLimitedChanged";
    
    /**
     * Indicates that this event was triggered as a result of the source
     * chat room isInvitationAllowed property being changed.
     */
    public static final String IS_INVITATION_ALLOWED_CHANGED
        = "IsInvitationAllowedChanged";

    /**
     * Indicates that this event was triggered as a result of the source
     * chat room isInvitatioRequestAllowed property being changed.
     */
    public static final String IS_INVITATION_REQUEST_ALLOWED_CHANGED
        = "IsInvitationRequestAllowedChanged";
    
    /**
     * Indicates that this event was triggered as a result of the source
     * chat room isUserRedirected property being changed.
     */
    public static final String IS_USER_REDIRECTED_CHANGED
        = "IsUserRedirectedChanged";
    
    /**
     * Indicates that this event was triggered as a result of the source
     * chat room isUserNicknameLocked property being changed.
     */
    public static final String IS_USER_NICKNAME_LOCKED_CHANGED
        = "IsUserNicknameLockedChanged";

    /**
     * Indicates that this event was triggered as a result of the source
     * chat room isAllowKick property being changed.
     */
    public static final String IS_ALLOW_KICK_CHANGED
        = "IsAllowKickChanged";
    
    /**
     * Indicates that this event was triggered as a result of the source
     * chat room isRegisteredUserOnly property being changed.
     */
    public static final String IS_REGISTERED_USERS_ONLY_CHANGED
        = "IsRegisteredUserOnlyChanged";
    
    /**
     * Indicates that this event was triggered as a result of the source
     * chat room isAllowSpecialMessage property being changed.
     */
    public static final String IS_ALLOW_SPECIAL_MESSAGE_CHANGED
        = "isAllowSpecialMessageChanged";
    
    /**
     * Indicates that this event was triggered as a result of the source
     * chat room isNicknameListVisible property being changed.
     */
    public static final String IS_NICKNAME_LIST_VISIBLE_CHANGED
        = "isNicknameListVisibleChanged";

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
