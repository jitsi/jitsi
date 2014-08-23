/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.service.protocol.*;

/**
 * Implementation of Basic Instant Messaging as utilized for IRC private
 * user-to-user messaging.
 *
 * @author Danny van Heumen
 */
public class OperationSetBasicInstantMessagingIrcImpl
    extends AbstractOperationSetBasicInstantMessaging
{
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
     * @param message message to send
     * @throws IllegalStateException in case of bad internal state
     * @throws IllegalArgumentException in case invalid arguments have been
     *             passed
     */
    @Override
    public void sendInstantMessage(final Contact to, final Message message)
        throws IllegalStateException,
        IllegalArgumentException
    {
        // OTR seems to be compatible with the command syntax (starts with '/')
        // and there were no other obvious problems so we decided to implement
        // IRC command support for IM infrastructure too.

        if (message instanceof MessageIrcImpl
            && ((MessageIrcImpl) message).isCommand())
        {
            this.provider.getIrcStack().command(to, (MessageIrcImpl) message);
        }
        else
        {
            this.provider.getIrcStack().message(to, message);
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
    protected void fireMessageReceived(final Message message, final Contact from)
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
}
