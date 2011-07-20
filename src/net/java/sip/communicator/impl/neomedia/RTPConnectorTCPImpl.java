/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.io.*;
import java.net.*;

import net.java.sip.communicator.service.neomedia.*;

/**
 * RTPConnector implementation for UDP.
 *
 * @author Sebastien Vincent
 */
public class RTPConnectorTCPImpl
    extends AbstractRTPConnector
{
    /**
     * The TCP socket this instance uses to send and receive RTP packets.
     */
    private Socket dataSocket;

    /**
     * The TCP socket this instance uses to send and receive RTCP packets.
     */
    private Socket controlSocket;

    /**
     * Initializes a new <tt>RTPConnectorTCPImpl</tt> which is to use a given
     * pair of sockets for RTP and RTCP traffic specified in the form
     * of a <tt>StreamConnector</tt>.
     *
     * @param connector the pair of sockets for RTP and RTCP traffic
     * the new instance is to use
     */
    public RTPConnectorTCPImpl(StreamConnector connector)
    {
        super(connector);
    }

    /**
     * Gets the TCP socket this instance uses to send and receive RTP packets.
     *
     * @return the TCP socket this instance uses to send and receive RTP packets
     */
    public Socket getDataSocket()
    {
        if (dataSocket == null)
            dataSocket = connector.getDataTCPSocket();
        return dataSocket;
    }

    /**
     * Gets the TCP Socket this instance uses to send and receive RTCP packets.
     *
     * @return the TCP Socket this instance uses to send and receive RTCP
     * packets
     */
    public Socket getControlSocket()
    {
        if (controlSocket == null)
            controlSocket = connector.getControlTCPSocket();
        return controlSocket;
    }

    /**
     * Creates the RTCP packet input stream to be used by <tt>RTPManager</tt>.
     *
     * @return a new RTCP packet input stream to be used by <tt>RTPManager</tt>
     * @throws IOException if an error occurs during the creation of the RTCP
     * packet input stream
     */
    protected RTPConnectorInputStream createControlInputStream()
        throws IOException
    {
        return new RTPConnectorTCPInputStream(getControlSocket());
    }

    /**
     * Creates the RTCP packet output stream to be used by <tt>RTPManager</tt>.
     *
     * @return a new RTCP packet output stream to be used by <tt>RTPManager</tt>
     * @throws IOException if an error occurs during the creation of the RTCP
     * packet output stream
     */
    protected RTPConnectorOutputStream createControlOutputStream()
        throws IOException
    {
        return new RTPConnectorTCPOutputStream(getControlSocket());
    }

    /**
     * Creates the RTP packet input stream to be used by <tt>RTPManager</tt>.
     *
     * @return a new RTP packet input stream to be used by <tt>RTPManager</tt>
     * @throws IOException if an error occurs during the creation of the RTP
     * packet input stream
     */
    protected RTPConnectorInputStream createDataInputStream()
        throws IOException
    {
        return new RTPConnectorTCPInputStream(getDataSocket());
    }

    /**
     * Creates the RTP packet output stream to be used by <tt>RTPManager</tt>.
     *
     * @return a new RTP packet output stream to be used by <tt>RTPManager</tt>
     * @throws IOException if an error occurs during the creation of the RTP
     * packet output stream
     */
    protected RTPConnectorOutputStream createDataOutputStream()
        throws IOException
    {
        return new RTPConnectorTCPOutputStream(getDataSocket());
    }
}