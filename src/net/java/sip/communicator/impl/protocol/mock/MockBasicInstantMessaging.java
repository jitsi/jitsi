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
package net.java.sip.communicator.impl.protocol.mock;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * Instant messaging functionality for the mock protocol.
 *
 * @author Damian Minkov
 * @author Emil Ivov
 */
public class MockBasicInstantMessaging
    extends AbstractOperationSetBasicInstantMessaging
{

    /**
     * The currently valid persistent presence operation set..
     */
    private final MockPersistentPresenceOperationSet opSetPersPresence;

    /**
     * The protocol provider that created us.
     */
    private final MockProvider parentProvider;

    /**
     * Creates an instance of this operation set keeping a reference to the
     * parent protocol provider and presence operation set.
     *
     * @param provider The provider instance that creates us.
     * @param opSetPersPresence the currently valid
     * <tt>MockPersistentPresenceOperationSet</tt> instance.
     */
    public MockBasicInstantMessaging(
                    MockProvider                       provider,
                    MockPersistentPresenceOperationSet opSetPersPresence)
    {
        this.opSetPersPresence = opSetPersPresence;
        this.parentProvider = provider;
    }

    @Override
    public Message createMessage(String content, String contentType,
        String encoding, String subject)
    {
        return new MockMessage(content, contentType, encoding, subject);
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param message the <tt>Message</tt> to send.
     * @throws IllegalStateException if the underlying ICQ stack is not
     *             registered and initialized.
     * @throws IllegalArgumentException if <tt>to</tt> is not an instance
     *             belonging to the underlying implementation.
     */
    public void sendInstantMessage(Contact to, Message message)
        throws IllegalStateException,
        IllegalArgumentException
    {
        fireMessageEvent(
            new MessageDeliveredEvent(message, to, new Date()));
    }

    /**
     * Determines whether the protocol provider (or the protocol itself) support
     * sending and receiving offline messages. Most often this method would
     * return true for protocols that support offline messages and false for
     * those that don't. It is however possible for a protocol to support these
     * messages and yet have a particular account that does not (i.e. feature
     * not enabled on the protocol server). In cases like this it is possible
     * for this method to return true even when offline messaging is not
     * supported, and then have the sendMessage method throw an
     * OperationFailedException with code - OFFLINE_MESSAGES_NOT_SUPPORTED.
     *
     * @return <tt>true</tt> if the protocol supports offline messages and
     * <tt>false</tt> otherwise.
     */
    public boolean isOfflineMessagingSupported()
    {
        return true;
    }

    /**
     * Determines whether the protocol supports the supplied content type
     *
     * @param contentType the type we want to check
     * @return <tt>true</tt> if the protocol supports it and
     * <tt>false</tt> otherwise.
     */
    public boolean isContentTypeSupported(String contentType)
    {
        return contentType.equals(DEFAULT_MIME_TYPE);
    }

    /**
     * Methods for manipulating mock operation set as deliver(receive) messageop
     *
     * @param to the address of the contact whom we are to deliver the message.
     * @param msg the message that we are to deliver.
     */
    public void deliverMessage(String to, Message msg)
    {
        Contact sourceContact = opSetPersPresence.findContactByID(to);

        fireMessageEvent(
            new MessageReceivedEvent(
                    msg, sourceContact, new Date()));
    }
}
