/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

/**
 * This is a protocol provider extensions class. Its called by the protocol
 * provider. This is the place to put custom OperationSets used in custom
 * branches of SIP Communicator.
 *
 * @author Damian Minkov
 */
public class ProtocolProviderExtensions
{
    /**
     * Method called by the protocol provider which is passed as an argument.
     * A place to register any custom OperationSets.
     * @param provider the protocol provider.
     */
    public static void registerCustomOperationSets(
        ProtocolProviderServiceSipImpl provider)
    {
    }
}
