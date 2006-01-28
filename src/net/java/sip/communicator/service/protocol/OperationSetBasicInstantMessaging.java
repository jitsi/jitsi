/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.MessageListener;

/**
 * Provides basic functionality for sending and receiving InstantMessages.
 *
 * @author Emil Ivov
 */
public interface OperationSetBasicInstantMessaging
    extends OperationSet
{
    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType MIME-type
     * @param contentEncoding encoding used for the MIME-type
     * @return the newly created message.
     */
    Message 	createMessage(byte[] content,
                              String contentType,
                              String contentEncoding);
    /**
     * Create a Message instance for sending a simple text messages with default
     * (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @return Message the newly created message
     */
    Message 	createMessage(String messageText);

    public void sendInstantMessage(Contact to, Message message);

    /**
     * Registeres a MessageListener with this operation set.
     *
     * @param listener the message listener to register.
     */
    public void addMessageListener(MessageListener listener);

}
