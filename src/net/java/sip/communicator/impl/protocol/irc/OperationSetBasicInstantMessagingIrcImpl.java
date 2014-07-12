package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.service.protocol.*;

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
        ProtocolProviderServiceIrcImpl provider)
    {
        if (provider == null)
            throw new IllegalArgumentException("provider cannot be null");
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
    public Message createMessage(String content, String contentType,
        String contentEncoding, String subject)
    {
        return new IrcMessage(content, contentType, contentEncoding, subject);
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
    public void sendInstantMessage(Contact to, Message message)
        throws IllegalStateException,
        IllegalArgumentException
    {
        this.provider.getIrcStack().message(to, message.getContent());
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
    public boolean isContentTypeSupported(String contentType)
    {
        return OperationSetBasicInstantMessaging.HTML_MIME_TYPE
            .equalsIgnoreCase(contentType);
    }

    /**
     * {@inheritDoc}
     * 
     * @param message the received message
     * @param contactId the sender
     */
    @Override
    protected void fireMessageReceived(Message message, Contact from)
    {
        super.fireMessageReceived(message, from);
    }

    /**
     * Implementation of an IRC basic instant message.
     * 
     * @author danny
     */
    static class IrcMessage
        extends AbstractMessage
    {
        /**
         * Constructor.
         * 
         * @param message instant message
         * @param contentType Message content type
         * @param contentEncoding Message content encoding
         * @param subject Message subject
         */
        IrcMessage(String message, String contentType,
            String contentEncoding, String subject)
        {
            super(message, contentType, contentEncoding, subject);
        }

        /**
         * Constructor for simple messages.
         * 
         * @param message instant message
         */
        IrcMessage(String message) {
            this(message, OperationSetBasicInstantMessaging.HTML_MIME_TYPE,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING, "");
        }

        /**
         * Constructor.
         * 
         * @param message instant message
         */
        private IrcMessage(String message, String contentType,
            String contentEncoding, String subject, String UUID)
        {
            super(message, contentType, contentEncoding, subject, UUID);
        }
    }
}
