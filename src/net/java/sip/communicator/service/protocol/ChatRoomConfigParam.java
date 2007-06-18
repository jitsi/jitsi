/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * The <tt>ChatRoomConfigParam</tt> class defines some parameters which allow to
 * add additional configurations to the chat room, like for example define that
 * the chat room requires a password or an invitation, or that the number of
 * users that join the chat is limitted, etc. A <tt>ChatRoomConfigParam</tt>
 * could be set to the chat room using the <tt>addConfigParam</tt> method in the
 * <tt>ChatRoom</tt> interface.
 * 
 * @author Stephane Remy
 * @author Yana Stamcheva
 */
public class ChatRoomConfigParam
{
    /**
     * This constant contains the String representation of the CHAT_ROOM_VISIBLE
     * configuration parameter.
     * <p>
     * This constant has the String value "Visible".
     */
    protected static final String _CHAT_ROOM_VISIBLE = "Visible";

    /**
     * The visible configuration parameter. Indicates that the associated chat
     * room is visible.
     */
    public static final ChatRoomConfigParam CHAT_ROOM_VISIBLE
        = new ChatRoomConfigParam(_CHAT_ROOM_VISIBLE);

    /**
     * This constant contains the String representation of the
     * CHAT_ROOM_PASSWORD_REQUIRED configuration parameter.
     * <p>
     * This constant has the String value "Password Required".
     */
    protected static final String _CHAT_ROOM_PASSWORD_REQUIRED
        = "Password Required";

    /**
     * The password required configuration parameter. Indicates that the
     * associated chat room requires password.
     */
    public static final ChatRoomConfigParam CHAT_ROOM_PASSWORD_REQUIRED
        = new ChatRoomConfigParam(_CHAT_ROOM_PASSWORD_REQUIRED);

    /**
     * This constant contains the String representation of the
     * CHAT_ROOM_INVITATION_REQUIRED configuration parameter.
     * <p>
     * This constant has the String value "Invitation Required".
     */
    protected static final String _CHAT_ROOM_INVITATION_REQUIRED
        = "Invitation Required";

    /**
     * The invitation required parameter. Indicates that the associated chat
     * room requires an invitation.
     */
    public static final ChatRoomConfigParam CHAT_ROOM_INVITATION_REQUIRED
        = new ChatRoomConfigParam(_CHAT_ROOM_INVITATION_REQUIRED);

    /**
     * This constant contains the String representation of the
     * CHAT_ROOM_USER_NUMBER_LIMITED configuration parameter.
     * <p>
     * This constant has the String value "User Number Limited".
     */
    protected  static final String _CHAT_ROOM_USER_NUMBER_LIMITED
        = "User Number Limited";

    /**
     * The limitted user number parameter. Indicates that the associated chat
     * room have a limit on number of users.
     */
    public static final ChatRoomConfigParam CHAT_ROOM_USER_NUMBER_LIMITED
        = new ChatRoomConfigParam(_CHAT_ROOM_USER_NUMBER_LIMITED);

    /**
     * This constant contains the String representation of the CHAT_ROOM_MUTE
     * configuration parameter.
     * <p>
     * This constant has the String value "Mute".
     */
    protected static final String _CHAT_ROOM_MUTE = "Mute";

    /**
     * Indicates that the associated chat room is currently
     * in a mute mode and no messages could be obtained from it.
     */
    public static final ChatRoomConfigParam CHAT_ROOM_MUTE
        = new ChatRoomConfigParam(_CHAT_ROOM_MUTE);

    /**
     * This constant contains the String representation of the
     * CHAT_ROOM_EXTERNAL_MESSAGES_ALLOWED configuration parameter.
     * <p>
     * This constant has the String value "External Messages Allowed".
     */
    protected static final String _CHAT_ROOM_EXTERNAL_MESSAGE_ALLOWED
        = "External Messages Allowed";

    /**
     * Indicates that it's possible for someone to send messages to this chat
     * room even if they are not present inside the chat room.
     */
    public static final ChatRoomConfigParam CHAT_ROOM_EXTERNAL_MESSAGE_ALLOWED
        = new ChatRoomConfigParam(_CHAT_ROOM_EXTERNAL_MESSAGE_ALLOWED);

    /**
     * This constant contains the String representation of the
     * CHAT_ROOM_REGISTERED configuration parameter.
     * <p>
     * This constant has the String value "Registered".
     */
    protected static final String _CHAT_ROOM_REGISTERED = "Registered";

    /**
     * Indicates that the associated chat room is registered.
     */
    public static final ChatRoomConfigParam CHAT_ROOM_REGISTERED
        = new ChatRoomConfigParam(_CHAT_ROOM_REGISTERED);

    /**
     * This constant contains the String representation of the
     * CHAT_ROOM_SUBJECT_LOCKED configuration parameter.
     * <p>
     * This constant has the String value "Subject Locked".
     */
    protected static final String _CHAT_ROOM_SUBJECT_LOCKED = "Subject Locked";

    /**
     * Indicates that only admin users can change the subject of this chat room.
     */
    public static final ChatRoomConfigParam CHAT_ROOM_SUBJECT_LOCKED
        = new ChatRoomConfigParam(_CHAT_ROOM_SUBJECT_LOCKED);

    /**
     * This constant contains the String representation of the
     * CHAT_ROOM_MESSAGE_FORMAT_ALLOWED configuration parameter.
     * <p>
     * This constant has the String value "Message Format Allowed".
     */
    protected static final String _CHAT_ROOM_MESSAGE_FORMAT_ALLOWED
        = "Message Format Allowed";

    /**
     * Indicates that the message format in this chat room could be modified.
     * Colored, underlined, etc. messages are allowed.
     */
    public static final ChatRoomConfigParam CHAT_ROOM_MESSAGE_FORMAT_ALLOWED
        = new ChatRoomConfigParam(_CHAT_ROOM_MESSAGE_FORMAT_ALLOWED);

    /**
     * This constant contains the String representation of the
     * CHAT_ROOM_MESSAGE_FORMAT_FILTERED configuration parameter.
     * <p>
     * This constant has the String value "Message Format Filtered".
     */
    protected static final String _CHAT_ROOM_MESSAGE_FORMAT_FILTERED
        = "Message Format Filtered";

    /**
     * Indicates that the associated chat room is currently in a message format
     * filtered mode. All formatted messages (colored, underlined, etc.)are seen
     * in standard format by other users.
     */
    public static final ChatRoomConfigParam CHAT_ROOM_MESSAGE_FORMAT_FILTERED
        = new ChatRoomConfigParam(_CHAT_ROOM_MESSAGE_FORMAT_FILTERED);

    /**
     * This constant contains the String representation of the
     * CHAT_ROOM_JOIN_TIME_LIMITED configuration parameter.
     * <p>
     * This constant has the String value "Join Time Limited".
     */
    protected static final String _CHAT_ROOM_JOIN_TIME_LIMITED
        = "Join Time Limited";

    /**
     * Indicates that users can only join this chat room in an interval of X
     * seconds.
     */
    public static final ChatRoomConfigParam CHAT_ROOM_JOIN_TIME_LIMITED
        = new ChatRoomConfigParam(_CHAT_ROOM_JOIN_TIME_LIMITED);

    /**
     * This constant contains the String representation of the
     * CHAT_ROOM_INVITATION_REQUEST_ALLOWED configuration parameter.
     * <p>
     * This constant has the String value "Invitation Request Allowed".
     */
    protected static final String _CHAT_ROOM_INVITATION_REQUEST_ALLOWED
        = "Invitation Request Allowed";

    /**
     * Indicates that all invitation requests are allowed.
     */
    public static final ChatRoomConfigParam CHAT_ROOM_INVITATION_REQUEST_ALLOWED
        = new ChatRoomConfigParam(_CHAT_ROOM_INVITATION_REQUEST_ALLOWED);

    /**
     * This constant contains the String representation of the
     * CHAT_ROOM_USER_REDIRECTED configuration parameter.
     * <p>
     * This constant has the String value "User Redirected".
     */
    protected static final String _CHAT_ROOM_USER_REDIRECTED = "User Redirected";

    /**
     * Indicates that all users which join this chat room are redirected to
     * another one.
     */
    public static final ChatRoomConfigParam CHAT_ROOM_USER_REDIRECTED
        = new ChatRoomConfigParam(_CHAT_ROOM_USER_REDIRECTED);

    /**
     * This constant contains the String representation of the
     * CHAT_ROOM_NICKNAMES_LOCKED configuration parameter.
     * <p>
     * This constant has the String value "Nicknames Locked".
     */
    protected static final String _CHAT_ROOM_NICKNAMES_LOCKED
        = "Nicknames Locked";

    /**
     * Indicates that users in this chat room can not change their nickname.
     */
    public static final ChatRoomConfigParam CHAT_ROOM_NICKNAMES_LOCKED
        = new ChatRoomConfigParam(_CHAT_ROOM_NICKNAMES_LOCKED);

    /**
     * This constant contains the String representation of the
     * CHAT_ROOM_KICK_LOCKED configuration parameter.
     * <p>
     * This constant has the String value "Kick Locked".
     */
    protected static final String _CHAT_ROOM_KICK_LOCKED = "Kick Locked";

    /**
     * Indicates that kicks are locked on this chat room. A kick tells the server
     * to force a user leave the chat room.
     */
    public static final ChatRoomConfigParam CHAT_ROOM_KICK_LOCKED
        = new ChatRoomConfigParam(_CHAT_ROOM_KICK_LOCKED);

    /**
     * This constant contains the String representation of the
     * CHAT_ROOM_USERS_REGISTERED configuration parameter.
     * <p>
     * This constant has the String value "Only Registered User".
     */
    protected static final String _CHAT_ROOM_ONLY_REGISTERED_USER
        = "Only Registered User";

    /**
     * Indicates that only registered users can join this chat room.
     */
    public static final ChatRoomConfigParam CHAT_ROOM_ONLY_REGISTERED_USER
        = new ChatRoomConfigParam(_CHAT_ROOM_ONLY_REGISTERED_USER);

    /**
     * This constant contains the String representation of the
     * CHAT_ROOM_SPECIAL_MESSAGE_ALLOWED configuration parameter.
     * <p>
     * This constant has the String value "Special Message Allowed".
     */
    protected static final String _CHAT_ROOM_SPECIAL_MESSAGE_ALLOWED
        = "Special Messages Allowed";

    /**
     * Indicates that the associated chat room allows special messages.
     */
    public static final ChatRoomConfigParam CHAT_ROOM_SPECIAL_MESSAGES_ALLOWED
        = new ChatRoomConfigParam(_CHAT_ROOM_SPECIAL_MESSAGE_ALLOWED);

    /**
     * This constant contains the String representation of the
     * CHAT_ROOM_NICKNAME_LIST_VISIBLE configuration parameter.
     * <p>
     * This constant has the String value "Nickname List Visible".
     */
    protected static final String _CHAT_ROOM_NICKNAME_LIST_VISIBLE
        = "Nickname List Visible";

    /**
     * Indicates that the list of nicknames in this chat room is currently
     * visible.
     */
    public static final ChatRoomConfigParam CHAT_ROOM_NICKNAME_LIST_VISIBLE
        = new ChatRoomConfigParam(_CHAT_ROOM_NICKNAME_LIST_VISIBLE);

    /**
     * This constant contains the String representation of the
     * CHAT_ROOM_INVITATION_ALLOWED configuration parameter.
     * <p>
     * This constant has the String value "Invitation Allowed".
     */
    protected static final String _CHAT_ROOM_INVITATION_ALLOWED
        = "Invitation Allowed";

    /**
     * Indicates that users can invite other users in this chat room.
     */
    public static final ChatRoomConfigParam CHAT_ROOM_INVITATION_ALLOWED
        = new ChatRoomConfigParam(_CHAT_ROOM_INVITATION_ALLOWED);

    /**
     * A string representation of this Chat Room State.
     */
    private String configParamString;
    
    /**
     * Creates a chat room configuration parameter object with a value
     * corresponding to the specified string.
     * 
     * @param configParamString a string representation of the configuration
     * parameter
     */
    protected ChatRoomConfigParam(String configParamString)
    {
        this.configParamString = configParamString;
    }

    /**
     * Returns a String representation of that chat room configuration parameter.
     * 
     * @return a string value (one of the _CHAT_ROOM_XXX constants) representing
     * this chat room config parameter).
     */
    public String getConfigParamString()
    {
        return configParamString;
    }

    /**
     * Returns a string represenation of this chat room configuration parameter.
     * Strings returned by this method have the following format:
     * "ChatRoomConfigParam:<CONFIG_PARAM_STRING>" and are meant to be used for
     * loggin/debugging purposes.
     * 
     * @return a string representation of this object.
     */
    public String toString()
    {
        return getClass().getName() + ":" + getConfigParamString();
    }
}
