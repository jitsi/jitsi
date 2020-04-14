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

    /**
     * Parses a <tt>String</tt> and returns the appropriate
     * <tt>TransportProtocol</tt>.
     * @param transportProtocol string
     * @return appropriate <tt>TransportProtocol</tt>
     * @throws IllegalArgumentException if string is not a transport protocol
     * valid name
     */
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
