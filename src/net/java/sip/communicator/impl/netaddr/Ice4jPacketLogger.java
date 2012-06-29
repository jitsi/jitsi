package net.java.sip.communicator.impl.netaddr;

import org.ice4j.stack.*;
import org.jitsi.service.packetlogging.*;

/**
 * Logs Packets coming and going through ice4j stack.
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
    public void logPacket(byte[] sourceAddress,
            int sourcePort,
            byte[] destinationAddress,
            int destinationPort,
            byte[] packetContent,
            boolean sender)
    {
        if(isEnabled())
        {
            NetaddrActivator.getPacketLogging()
                .logPacket(
                        PacketLoggingService.ProtocolName.ICE4J,
                        sourceAddress,
                        sourcePort,
                        destinationAddress,
                        destinationPort,
                        PacketLoggingService.TransportName.UDP,
                        sender,
                        packetContent
                );
        }
    }

    /**
     * Checks whether the logger is enabled.
     * @return <tt>true</tt> if the logger is enabled, <tt>false</tt>
     *  otherwise.
     */
    public boolean isEnabled()
    {
        return NetaddrActivator.getPacketLogging()
                .isLoggingEnabled(PacketLoggingService.ProtocolName.ICE4J);
    }
}
