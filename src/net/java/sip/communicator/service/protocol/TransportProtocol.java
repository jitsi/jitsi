/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * Enumerates the "transport" (OSI transport/application) protocols used to
 * carry the control channel for protocol services like xmmp, sip, etc.
 *
 * @author Vincent Lucas
 */
public enum TransportProtocol
{
    /**
     * The "transport" protocol is unknown.
     */
    UNKNOWN,

    /**
     * The "transport" protocol is UDP.
     */
    UDP,

    /**
     * The "transport" protocol is TCP.
     */
    TCP,

    /**
     * The "transport" protocol is TLS.
     */
    TLS;

    public static TransportProtocol parse(String transportProtocol)
        throws IllegalArgumentException
    {
        if(UNKNOWN.toString().equalsIgnoreCase(transportProtocol))
            return UNKNOWN;
        else if(UDP.toString().equalsIgnoreCase(transportProtocol))
            return UDP;
        else if(TCP.toString().equalsIgnoreCase(transportProtocol))
            return TCP;
        else if(TLS.toString().equalsIgnoreCase(transportProtocol))
            return TLS;

        throw new IllegalArgumentException(
                transportProtocol
                + "is not a currently supported TransportProtocolEnum");
    }
}
