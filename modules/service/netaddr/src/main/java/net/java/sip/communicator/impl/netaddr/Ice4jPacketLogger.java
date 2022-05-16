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
package net.java.sip.communicator.impl.netaddr;

import org.ice4j.stack.*;
import org.jitsi.service.packetlogging.*;

/**
 * Logs Packets coming and going through ice4j stack.
 *
 * @author Damian Minkov
 */
public class Ice4jPacketLogger
    implements PacketLogger
{
    /**
     * Logs a incoming or outgoing packet.
     *
     * @param sourceAddress the source address of the packet.
     * @param sourcePort the source port.
     * @param destinationAddress the destination address of the packet.
     * @param destinationPort the destination port.
     * @param packetContent the content of the packet.
     * @param sender whether we are sending or not the packet.
     */
    public void logPacket(
            byte[] sourceAddress,
            int sourcePort,
            byte[] destinationAddress,
            int destinationPort,
            byte[] packetContent,
            boolean sender)
    {
        if (isEnabled())
        {
            NetaddrActivator.getPacketLogging().logPacket(
                    PacketLoggingService.ProtocolName.ICE4J,
                    sourceAddress,
                    sourcePort,
                    destinationAddress,
                    destinationPort,
                    PacketLoggingService.TransportName.UDP,
                    sender,
                    packetContent);
        }
    }

    /**
     * Checks whether the logger is enabled.
     *
     * @return <tt>true</tt> if the logger is enabled; <tt>false</tt>,
     * otherwise
     */
    public boolean isEnabled()
    {
        PacketLoggingService packetLoggingService
            = NetaddrActivator.getPacketLogging();

        return
            (packetLoggingService != null)
                && packetLoggingService.isLoggingEnabled(
                        PacketLoggingService.ProtocolName.ICE4J);
    }
}
