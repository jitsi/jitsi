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

import net.java.sip.communicator.service.protocol.event.*;

import java.io.*;

/**
 * Provides basic functionality for sending and receiving SMS Messages.
 *
 * @author Damian Minkov
 */
public interface OperationSetSmsMessaging
    extends OperationSet
{
    /**
     * Default encoding for outgoing messages.
     */
    public static final String DEFAULT_MIME_ENCODING = "UTF-8";

    /**
     * Default mime type for outgoing messages.
     */
    public static final String DEFAULT_MIME_TYPE = "text/plain";

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param contentEncoding encoding used for <tt>content</tt>
     * @return the newly created message.
     */
    public Message createMessage(byte[] content,         String contentType,
                                 String contentEncoding);

    /**
     * Create a Message instance for sending a sms messages with default
     * (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @return Message the newly created message
     */
    public Message createMessage(String messageText);

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param message the <tt>Message</tt> to send.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>to</tt> is not an
     * instance belonging to the underlying implementation.
     */
    public void sendSmsMessage(Contact to, Message message)
        throws IllegalStateException, IllegalArgumentException;

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> parameter.
     * @param to the destination to send <tt>message</tt> to
     * @param message the <tt>Message</tt> to send.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>to</tt> is not an
     * instance belonging to the underlying implementation.
     */
    public void sendSmsMessage(String to, Message message)
        throws IllegalStateException, IllegalArgumentException;

    /**
     * Sends the <tt>file</tt> to the destination indicated by the
     * <tt>to</tt> parameter.
     * @param to the destination to send <tt>message</tt> to
     * @param file the <tt>file</tt> to send.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>to</tt> is not an
     * instance belonging to the underlying implementation.
     * @throws OperationNotSupportedException if the given contact client or
     * server does not support file transfers
     */
    public FileTransfer sendMultimediaFile(Contact to, File file)
        throws IllegalStateException,
               IllegalArgumentException,
               OperationNotSupportedException;

    /**
     * Registers a MessageListener with this operation set so that it gets
     * notifications of successful message delivery, failure or reception of
     * incoming messages..
     *
     * @param listener the <tt>MessageListener</tt> to register.
     */
    public void addMessageListener(MessageListener listener);

    /**
     * Unregisters <tt>listener</tt> so that it won't receive any further
     * notifications upon successful message delivery, failure or reception of
     * incoming messages..
     *
     * @param listener the <tt>MessageListener</tt> to unregister.
     */
    public void removeMessageListener(MessageListener listener);

    /**
     * Determines whether the protocol supports the supplied content type
     *
     * @param contentType the type we want to check
     * @return <tt>true</tt> if the protocol supports it and
     * <tt>false</tt> otherwise.
     */
    public boolean isContentTypeSupported(String contentType);

    /**
     * Returns the contact to send sms to.
     * @param to the number to send sms.
     * @return the contact representing the receiver of the sms.
     */
    public Contact getContact(String to);

    /**
     * Whether the implementation do not know how to send sms to the supplied
     * contact and should as for number.
     * @param to the contact to send sms.
     * @return whether user needs to enter number for the sms recipient.
     */
    public boolean askForNumber(Contact to);
}
