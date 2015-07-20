/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.protocol.event;

import java.beans.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Dispatched to indicate that a change of a chat room property has failed.
 * The modification of a property could fail, because the implementation
 * doesn't support such a property.
 *
 * @author Yana Stamcheva
 */
public class ChatRoomPropertyChangeFailedEvent
    extends PropertyChangeEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates that the current implementation doesn't support the given
     * property.
     */
    public static final int PROPERTY_NOT_SUPPORTED = 0;

    /**
     * The reason of the failure.
     */
    private final String reason;

    /**
     * Indicates why the failure occurred.
     */
    private final int reasonCode;

    /**
     * Creates a <tt>ChatRoomPropertyChangeEvent</tt> indicating that a change
     * has occurred for property <tt>propertyName</tt> in the <tt>source</tt>
     * chat room and that its value has changed from <tt>oldValue</tt> to
     * <tt>newValue</tt>.
     * <p>
     * @param source the <tt>ChatRoom</tt>, to which the property belongs
     * @param propertyName the name of the property
     * @param propertyValue the value of the property
     * @param expectedValue the expected after the change value of the property
     * @param reasonCode the code indicating the reason for the failure
     * @param reason more detailed explanation of the failure
     */
    public ChatRoomPropertyChangeFailedEvent(   ChatRoom source,
                                                String propertyName,
                                                Object propertyValue,
                                                Object expectedValue,
                                                int reasonCode,
                                                String reason)
    {
        super(source, propertyName, propertyValue, expectedValue);

        this.reasonCode = reasonCode;
        this.reason = reason;
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
     * Returns the value of the property.
     *
     * @return the value of the property.
     */
    public Object getPropertyValue()
    {
        return getOldValue();
    }

    /**
     * Return the expected after the change value of the property.
     *
     * @return the expected after the change value of the property
     */
    public Object getExpectedValue()
    {
        return getNewValue();
    }

    /**
     * Returns the code of the failure. One of the static constants declared in
     * this class.
     * @return the code of the failure. One of the static constants declared in
     * this class
     */
    public int getReasonCode()
    {
        return reasonCode;
    }

    /**
     * Returns the reason of the failure.
     * @return the reason of the failure
     */
    public String getReason()
    {
        return reason;
    }

    /**
     * Returns a String representation of this event.
     *
     * @return String representation of this event
     */
    @Override
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
