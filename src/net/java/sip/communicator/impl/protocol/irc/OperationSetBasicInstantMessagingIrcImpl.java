/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.impl.protocol.irc.exception.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Implementation of Basic Instant Messaging as utilized for IRC private
 * user-to-user messaging.
 *
 * @author Danny van Heumen
 */
public class OperationSetBasicInstantMessagingIrcImpl
    extends AbstractOperationSetBasicInstantMessaging
    implements OperationSetBasicInstantMessagingTransport
{
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger
        .getLogger(OperationSetBasicInstantMessagingIrcImpl.class);

    /**
     * IRC protocol provider service.
     */
    private final ProtocolProviderServiceIrcImpl provider;

    /**
     * Constructor.
     *
     * @param provider IRC provider service.
     */
    public OperationSetBasicInstantMessagingIrcImpl(
        final ProtocolProviderServiceIrcImpl provider)
    {
        if (provider == null)
        {
            throw new IllegalArgumentException("provider cannot be null");
        }
        this.provider = provider;
    }

    /**
     * Create a new message.
     *
     * {@inheritDoc}
     *
     * @param content Message content
     * @param contentType Message content type
     * @param contentEncoding message encoding
     * @param subject Message subject
     */
    @Override
    public MessageIrcImpl createMessage(final String content,
        final String contentType, final String contentEncoding,
        final String subject)
    {
        return new MessageIrcImpl(content, contentType, contentEncoding,
            subject);
    }

    /**
     * Send instant message.
     *
     * @param to contact to send message to
     * @param original message to send
     * @throws IllegalStateException in case of bad internal state
     * @throws IllegalArgumentException in case invalid arguments have been
     *             passed
     */
    @Override
    public void sendInstantMessage(final Contact to, final Message original)
        throws IllegalStateException,
        IllegalArgumentException
    {
        if (!(original instanceof MessageIrcImpl))
        {
            LOGGER.error("Invalid class of Message implementation received. "
                + "Not sending message.");
            return;
        }

        // OTR seems to be compatible with the command syntax (starts with '/')
        // and there were no other obvious problems so we decided to implement
        // IRC command support for IM infrastructure too.

        final MessageDeliveredEvent[] msgDeliveryPendingEvts =
            messageDeliveryPendingTransform(new MessageDeliveredEvent(original,
                to));

        try
        {
            for (MessageDeliveredEvent event : msgDeliveryPendingEvts)
            {
                if (event == null)
                {
                    continue;
                }

                String transformedContent =
                    event.getSourceMessage().getContent();

                // Note: can't set subject since it leaks information while
                // message content actually did get encrypted.
                MessageIrcImpl message = this.createMessage(transformedContent,
                    original.getContentType(), original.getEncoding(), "");

                final IrcConnection connection =
                    this.provider.getIrcStack().getConnection();
                if (connection == null)
                {
                    throw new IllegalStateException(
                        "Connection is not available.");
                }
                try
                {
                    if (!event.isMessageEncrypted() && message.isCommand())
                    {
                        try
                        {
                            connection.getMessageManager().command(to, message);
                            fireMessageDelivered(original, to);
                        }
                        catch (final UnsupportedCommandException e)
                        {
                            fireMessageDeliveryFailed(message, to,
                                MessageDeliveryFailedEvent
                                    .UNSUPPORTED_OPERATION);
                        }
                    }
                    else
                    {
                        connection.getMessageManager().message(to, message);
                        fireMessageDelivered(original, to);
                    }
                }
                catch (RuntimeException e)
                {
                    LOGGER.debug("Failed to deliver (raw) message: " + message);
                    throw e;
                }
            }
        }
        catch (RuntimeException e)
        {
            LOGGER.warn("Failed to deliver message: " + original, e);
            fireMessageDeliveryFailed(original, to,
                MessageDeliveryFailedEvent.NETWORK_FAILURE);
        }
    }

    /**
     * Check if offline messaging is supported.
     *
     * @return returns true if offline messaging is supported or false
     *         otherwise.
     */
    @Override
    public boolean isOfflineMessagingSupported()
    {
        return false;
    }

    /**
     * Test content type support.
     *
     * {@inheritDoc}
     *
     * @param contentType contentType to test
     * @return returns true if content type is supported
     */
    @Override
    public boolean isContentTypeSupported(final String contentType)
    {
        return OperationSetBasicInstantMessaging.HTML_MIME_TYPE
            .equalsIgnoreCase(contentType);
    }

    /**
     * {@inheritDoc}
     *
     * @param message the received message
     * @param from the sender
     */
    @Override
    protected void fireMessageReceived(final Message message,
        final Contact from)
    {
        super.fireMessageReceived(message, from);
    }

    /**
     * {@inheritDoc}
     *
     * @param message Message that has been delivered successfully.
     * @param to Contact to deliver message to.
     */
    @Override
    protected void fireMessageDelivered(final Message message, final Contact to)
    {
        super.fireMessageDelivered(message, to);
    }

    /**
     * {@inheritDoc}
     *
     * @param message Message that was failed to deliver.
     * @param to Contact to deliver message to.
     * @param errorCode Error code of failed delivery.
     */
    @Override
    protected void fireMessageDeliveryFailed(final Message message,
        final Contact to, final int errorCode)
    {
        super.fireMessageDeliveryFailed(message, to, errorCode);
    }

    /**
     * Calculate the maximum message size for IRC messages.
     *
     * @param contact the contact receiving the message
     * @return returns the size the message can be at maximum to receive a
     *         complete message.
     */
    @Override
    public int getMaxMessageSize(final Contact contact)
    {
        IrcConnection connection = this.provider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new IllegalStateException("Connection is not available.");
        }
        return connection.getMessageManager().calculateMaximumMessageSize(
            contact);
    }

    /**
     * Calculate number of messages allowed to send over IRC.
     *
     * @param contact contact receiving the messages
     * @return returns number of messages that can be received at maximum
     */
    @Override
    public int getMaxNumberOfMessages(final Contact contact)
    {
        return OperationSetBasicInstantMessagingTransport.UNLIMITED;
    }
}
