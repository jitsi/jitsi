/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

/**
 * Represents a factory of <tt>StreamConnector</tt> instances.
 *
 * @author Lyubomir Marinov
 */
public interface StreamConnectorFactory
{
    /**
     * Initializes a <tt>StreamConnector</tt> instance.
     *
     * @return a <tt>StreamConnector</tt> instance
     */
    public StreamConnector createStreamConnector();
}
