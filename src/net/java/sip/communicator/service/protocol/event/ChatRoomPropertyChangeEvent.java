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
    public static final String PROPERTY_SUBJECT_CHANGED = "SubjectChanged";
    
    /**
     * Indicates that this event was triggered as a result of the source
     * chat room ban list being changed.
     */
    public static final String PROPERTY_BAN_LIST_CHANGED = "BanListChanged";
    
    /**
     * Indicates that this event was triggered as a result of the source
     * chat room user limit being changed.
     */
    public static final String PROPERTY_USER_LIMIT_CHANGED = "UserLimitChanged";
    
    /**
     * Indicates that this event was triggered as a result of the source
     * chat room state changed.
     */
    public static final String PROPERTY_STATE_CHANGED = "StateChanged";
    
    /**
     * Indicates that this event was triggered as a result of the source
     * chat room password being changed.
     */
    public static final String PROPERTY_PASSWORD_CHANGED = "PasswordChanged";

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
