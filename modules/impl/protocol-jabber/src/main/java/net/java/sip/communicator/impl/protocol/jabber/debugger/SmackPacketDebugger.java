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

import java.nio.charset.*;
import net.java.sip.communicator.impl.protocol.jabber.*;

import org.jitsi.service.packetlogging.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;

import java.lang.reflect.*;
import java.net.*;

/**
 * The jabber packet listener that logs the packets to the packet logging
 * service.
 * @author Damian Minkov
 */
public class SmackPacketDebugger
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
    private final PacketLoggingService packetLogging;

    public final Inbound inbound = new Inbound();
    public final Outbound outbound = new Outbound();

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

    public class Outbound implements StanzaListener
    {
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
        public void processStanza(Stanza packet)
        {
            try
            {
                if(packetLogging.isLoggingEnabled(
                        PacketLoggingService.ProtocolName.JABBER)
                    && packet != null)
                {
                    Socket socket = getSocket();
                    if(remoteAddress == null)
                    {
                        if (socket != null)
                        {
                            remoteAddress = socket.getInetAddress().getAddress();
                            localAddress = socket.getLocalAddress().getAddress();
                        }
                        else
                        {
                            remoteAddress = new byte[4];
                            localAddress = new byte[] { 127, 0, 0, 1 };
                        }
                    }

                    int localPort = 0;
                    int remotePort = 5222;
                    if (socket != null)
                    {
                        localPort = socket.getLocalPort();
                    }
                    if (connection != null)
                    {
                        int port = connection.getPort();
                        if (port > 0)
                        {
                            remotePort = port;
                        }
                    }

                    byte[] packetBytes;

                    if(packet instanceof Message)
                    {
                        packetBytes = cloneAnonyMessage(packet)
                            .toXML().toString().getBytes(StandardCharsets.UTF_8);
                    }
                    else
                    {
                        packetBytes = packet.toXML().toString().getBytes(StandardCharsets.UTF_8);
                    }

                    packetLogging.logPacket(
                            PacketLoggingService.ProtocolName.JABBER,
                            localAddress,
                            localPort,
                            remoteAddress,
                            remotePort,
                            PacketLoggingService.TransportName.TCP,
                            true,
                            packetBytes
                        );
                }
            }
            catch(Exception t)
            {
                t.printStackTrace();
            }
        }
    }

    /**
     * Clones if messages and process subject and bodies.
     */
    private Message cloneAnonyMessage(Stanza packet)
    {
        Message oldMsg = (Message)packet;

        // if the message has no body, or the bodies list is empty
        if(oldMsg.getBody() == null && oldMsg.getBodies().size() == 0)
        {
            return oldMsg;
        }

        MessageBuilder builder = MessageBuilder.buildMessageFrom(oldMsg, oldMsg.getStanzaId());

        builder.removeExtension(Message.Subject.ELEMENT, Message.Subject.NAMESPACE);
        for(Message.Subject sub : oldMsg.getSubjects())
        {
            if(sub.getSubject() != null)
                builder.addSubject(
                    sub.getLanguage(),
                    new String(new char[sub.getSubject().length()]).replace('\0', '.'));
        }

        builder.removeExtension(Message.Body.ELEMENT, Message.Body.NAMESPACE);
        for(Message.Body b : oldMsg.getBodies())
        {
            if(b.getMessage() != null)
            {
                builder.addBody(b.getLanguage(), new String(new char[b.getMessage().length()]).replace('\0', '.'));
            }
        }

        return builder.build();
    }

    public class Inbound implements StanzaListener
    {
        /**
         * Process the next packet sent to this packet listener.<p>
         * <p/>
         * A single thread is responsible for invoking all listeners, so
         * it's very important that implementations of this method not block
         * for any extended period of time.
         *
         * @param packet the packet to process.
         */
        public void processStanza(Stanza packet)
        {
            try
            {
                if(packetLogging.isLoggingEnabled(
                        PacketLoggingService.ProtocolName.JABBER)
                    && packet != null)
                {
                    int localPort = 0;
                    int remotePort = 5222;
                    Socket socket = getSocket();
                    if (socket != null)
                    {
                        localPort = socket.getLocalPort();
                    }

                    if (connection != null)
                    {
                        int port = connection.getPort();
                        if (port > 0)
                        {
                            remotePort = port;
                        }
                    }

                    byte[] packetBytes;

                    if(packet instanceof Message)
                    {
                        packetBytes = cloneAnonyMessage(packet)
                            .toXML().toString().getBytes(StandardCharsets.UTF_8);
                    }
                    else
                    {
                        packetBytes = packet.toXML().toString().getBytes(StandardCharsets.UTF_8);
                    }

                    packetLogging.logPacket(
                        PacketLoggingService.ProtocolName.JABBER,
                        remoteAddress,
                        remotePort,
                        localAddress,
                        localPort,
                        PacketLoggingService.TransportName.TCP,
                        false,
                        packetBytes
                    );
                }
            }
            catch(Exception t)
            {
                t.printStackTrace();
            }
        }
    }

    private Socket getSocket()
    {
        if (this.connection == null)
        {
            return null;
        }

        try
        {
            Field socket = connection.getClass().getField("socket");
            socket.setAccessible(true);
            return (Socket)socket.get(connection);
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            return null;
        }
    }
}
