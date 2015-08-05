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
package net.java.sip.communicator.impl.protocol.jabber.debugger;

import net.java.sip.communicator.impl.protocol.jabber.*;

import org.jitsi.service.packetlogging.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;

/**
 * The jabber packet listener that logs the packets to the packet logging
 * service.
 * @author Damian Minkov
 */
public class SmackPacketDebugger
    implements PacketListener,
               PacketInterceptor
{
    /**
     * The current jabber connection.
     */
    private XMPPConnection connection = null;

    /**
     * Local address for the connection.
     */
    private byte[] localAddress;

    /**
     * Remote address for the connection.
     */
    private byte[] remoteAddress;

    /**
     * Instance for the packet logging service.
     */
    private PacketLoggingService packetLogging = null;

    /**
     * Creates the SmackPacketDebugger instance.
     */
    public SmackPacketDebugger()
    {
        packetLogging = JabberActivator.getPacketLogging();
    }

    /**
     * Sets current connection.
     * @param connection the connection.
     */
    public void setConnection(XMPPConnection connection)
    {
        this.connection = connection;
    }

    /**
     * Process the packet that is about to be sent to the server. The intercepted
     * packet can be modified by the interceptor.<p>
     * <p/>
     * Interceptors are invoked using the same thread that requested the packet
     * to be sent, so it's very important that implementations of this method
     * not block for any extended period of time.
     *
     * @param packet the packet to is going to be sent to the server.
     */
    public void interceptPacket(Packet packet)
    {
        try
        {
            if(packetLogging.isLoggingEnabled(
                    PacketLoggingService.ProtocolName.JABBER)
                && packet != null && connection.getSocket() != null)
            {
                if(remoteAddress == null)
                {
                    remoteAddress = connection.getSocket()
                        .getInetAddress().getAddress();
                    localAddress = connection.getSocket()
                        .getLocalAddress().getAddress();
                }

                byte[] packetBytes;

                if(packet instanceof Message)
                {
                    packetBytes = cloneAnonyMessage(packet)
                        .toXML().getBytes("UTF-8");
                }
                else
                {
                    packetBytes = packet.toXML().getBytes("UTF-8");
                }

                packetLogging.logPacket(
                        PacketLoggingService.ProtocolName.JABBER,
                        localAddress,
                        connection.getSocket().getLocalPort(),
                        remoteAddress,
                        connection.getPort(),
                        PacketLoggingService.TransportName.TCP,
                        true,
                        packetBytes
                    );
            }
        }
        catch(Throwable t)
        {
            t.printStackTrace();
        }
    }

    /**
     * Clones if messages and process subject and bodies.
     * @param packet
     * @return
     */
    private Message cloneAnonyMessage(Packet packet)
    {
        Message oldMsg = (Message)packet;

        // if the message has no body, or the bodies list is empty
        if(oldMsg.getBody() == null
            && (oldMsg.getBodies() == null || oldMsg.getBodies().size() == 0))
        {
            return oldMsg;
        }

        Message newMsg = new Message();

        newMsg.setPacketID(packet.getPacketID());
        newMsg.setTo(packet.getTo());
        newMsg.setFrom(packet.getFrom());

        // we don't modify them, just use existing
        for(PacketExtension pex : packet.getExtensions())
            newMsg.addExtension(pex);

        for(String propName : packet.getPropertyNames())
            newMsg.setProperty(propName, packet.getProperty(propName));

        newMsg.setError(packet.getError());

        newMsg.setType(oldMsg.getType());
        newMsg.setThread(oldMsg.getThread());
        newMsg.setLanguage(oldMsg.getLanguage());

        for(Message.Subject sub : oldMsg.getSubjects())
        {
            if(sub.getSubject() != null)
                newMsg.addSubject(sub.getLanguage(),
                    new String(new char[sub.getSubject().length()])
                            .replace('\0', '.'));
            else
                newMsg.addSubject(sub.getLanguage(), sub.getSubject());
        }

        for(Message.Body b : oldMsg.getBodies())
        {
            if(b.getMessage() != null)
                newMsg.addBody(b.getLanguage(),
                    new String(new char[b.getMessage().length()])
                            .replace('\0', '.'));
            else
                newMsg.addSubject(b.getLanguage(), b.getMessage());
        }

        return newMsg;
    }

    /**
     * Process the next packet sent to this packet listener.<p>
     * <p/>
     * A single thread is responsible for invoking all listeners, so
     * it's very important that implementations of this method not block
     * for any extended period of time.
     *
     * @param packet the packet to process.
     */
    public void processPacket(Packet packet)
    {
        try
        {
            if(packetLogging.isLoggingEnabled(
                    PacketLoggingService.ProtocolName.JABBER)
                && packet != null && connection.getSocket() != null)
            {
                byte[] packetBytes;

                if(packet instanceof Message)
                {
                    packetBytes = cloneAnonyMessage(packet)
                        .toXML().getBytes("UTF-8");
                }
                else
                {
                    packetBytes = packet.toXML().getBytes("UTF-8");
                }

                packetLogging.logPacket(
                    PacketLoggingService.ProtocolName.JABBER,
                    remoteAddress,
                    connection.getPort(),
                    localAddress,
                    connection.getSocket().getLocalPort(),
                    PacketLoggingService.TransportName.TCP,
                    false,
                    packetBytes
                );
            }
        }
        catch(Throwable t)
        {
            t.printStackTrace();
        }
    }
}
