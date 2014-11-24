/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.service.argdelegation;

/**
 * This interface is meant to be implemented by all bundles that wish to handle
 * URIs passed as invocation arguments.
 *
 * @author Emil Ivov <emcho at sip-communicator.org>
 */
public interface UriHandler
{
    /**
     * The name of the property that we use in the service registration
     * properties to store a protocol name when registering <tt>UriHandler</tt>s
     */
    public static final String PROTOCOL_PROPERTY = "ProtocolName";

    /**
     * Returns the protocols that this handler is responsible for.
     *
     * @return protocols that this handler is responsible for
     */
    public String[] getProtocol();

    /**
     * Handles/opens the URI.
     *
     * @param uri the URI that the handler has to open.
     */
    public void handleUri(String uri);
}
