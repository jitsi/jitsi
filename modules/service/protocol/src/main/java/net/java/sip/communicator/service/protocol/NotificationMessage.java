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
package net.java.sip.communicator.service.protocol;

/**
 * A notification message that is used to deliver notifications for an waiting
 * server message.
 *
 * @see MessageWaitingListener, MessageWaitingEvent
 *
 * @author Yana Stamcheva
 */
public class NotificationMessage
{
    /**
     * The contact from which the message is coming.
     */
    private final String fromContact;

    /**
     * The name of the group of messages to which this message belongs,
     * if there's any.
     */
    private final String messageGroup;

    /**
     * Additional details related to the message.
     */
    private final String messageDetails;

    /**
     * The text of the message.
     */
    private final String messageText;

    /**
     * The notification message source.
     */
    private final Object source;

    /**
     * Creates an instance of <tt>NotificationMessage</tt> by specifying the
     * name of the contact from which the message is, the message group, any
     * additional details and the message actual text.
     *
     * @param source the notification message source
     * @param fromContact the contact from which the message is coming
     * @param messageGroup the name of the group of messages to which this
     * message belongs
     * @param messageDetails additional details related to the message
     * @param messageText the text of the message
     */
    public NotificationMessage( Object source,
                                String fromContact,
                                String messageGroup,
                                String messageDetails,
                                String messageText)
    {
        this.source = source;
        this.fromContact = fromContact;
        this.messageGroup = messageGroup;
        this.messageDetails = messageDetails;
        this.messageText = messageText;
    }

    /**
     * Returns the notification message source.
     *
     * @return the notification message source
     */
    public Object getSource()
    {
        return source;
    }

    /**
     * Returns the contact from which the message is coming
     *
     * @return the contact from which the message is coming
     */
    public String getFromContact()
    {
        return fromContact;
    }

    /**
     * Returns the name of the group of messages to which this
     * message belongs.
     *
     * @return the name of the group of messages to which this
     * message belongs
     */
    public String getMessageGroup()
    {
        return messageGroup;
    }

    /**
     * Returns the additional details related to the message
     *
     * @return the additional details related to the message
     */
    public String getMessageDetails()
    {
        return messageDetails;
    }

    /**
     * Returns the text of the message
     *
     * @return the text of the message
     */
    public String getMessageText()
    {
        return messageText;
    }
}
