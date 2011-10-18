/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.packetlogging;

/**
 * A Packet Logging Service to log packets that were send/received
 * by protocols or any other network related services in various formats.
 * Its for debugging purposes.
 *
 * @author Damian Minkov
 */
public interface PacketLoggingService
{
    /**
     * These are the services that this packet logging service
     * cab handle.
     */
    public enum ProtocolName
    {
        SIP,
        JABBER,
        RTP,
        ICE4J
    }

    /**
     * The transport names.
     */
    public enum TransportName
    {
        UDP,
        TCP
    }

    /**
     * Checks is logging globally enabled for the service.
     * @return is logging enabled.
     */
    public boolean isLoggingEnabled();

    /**
     * Checks is logging globally enabled for and is it currently
     * available fo the given protocol.
     *.
     * @param protocol that is checked.
     * @return is logging enabled.
     */
    public boolean isLoggingEnabled(ProtocolName protocol);

    /**
     * Log a packet with all the required information.
     *
     * @param protocol the source protocol that logs this packet.
     * @param sourceAddress the source address of the packet.
     * @param sourcePort the source port of the packet.
     * @param destinationAddress the destination address.
     * @param destinationPort the destination port.
     * @param transport the transport this packet uses.
     * @param sender are we the sender of the packet or not.
     * @param packetContent the packet content.
     */
    public void logPacket(
            ProtocolName protocol,
            byte[] sourceAddress,
            int sourcePort,
            byte[] destinationAddress,
            int destinationPort,
            TransportName transport,
            boolean sender,
            byte[] packetContent);

    /**
     * Log a packet with all the required information.
     *
     * @param protocol the source protocol that logs this packet.
     * @param sourceAddress the source address of the packet.
     * @param sourcePort the source port of the packet.
     * @param destinationAddress the destination address.
     * @param destinationPort the destination port.
     * @param transport the transport this packet uses.
     * @param sender are we the sender of the packet or not.
     * @param packetContent the packet content.
     * @param packetOffset the packet content offset.
     * @param packetLength the packet content length. 
     */
    public void logPacket(
            ProtocolName protocol,
            byte[] sourceAddress,
            int sourcePort,
            byte[] destinationAddress,
            int destinationPort,
            TransportName transport,
            boolean sender,
            byte[] packetContent,
            int packetOffset,
            int packetLength);

    /**
     * Returns the current Packet Logging Configuration.
     *
     * @return the Packet Logging Configuration.
     */
    public PacketLoggingConfiguration getConfiguration();
}
