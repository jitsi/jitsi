/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.io.*;
import java.net.*;

import javax.media.rtp.*;

import net.java.sip.communicator.service.neomedia.*;

/**
 * @author Bing SU (nova.su@gmail.com)
 * @author Lubomir Marinov
 */
public class RTPConnectorImpl
    implements RTPConnector
{

    /**
     * The pair of datagram sockets for RTP and RTCP traffic that this instance
     * uses in the form of a <tt>StreamConnector</tt>.
     */
    private final StreamConnector connector;

    /**
     * RTCP packet input stream used by <tt>RTPManager</tt>.
     */
    private RTPConnectorInputStream controlInputStream;

    /**
     * RTCP packet output stream used by <tt>RTPManager</tt>.
     */
    private RTPConnectorOutputStream controlOutputStream;

    /**
     * The UDP socket this instance uses to send and receive RTCP packets.
     */
    private DatagramSocket controlSocket;

    /**
     * RTP packet input stream used by <tt>RTPManager</tt>.
     */
    private RTPConnectorInputStream dataInputStream;

    /**
     * RTP packet output stream used by <tt>RTPManager</tt>.
     */
    private RTPConnectorOutputStream dataOutputStream;

    /**
     * The UDP socket this instance uses to send and receive RTP packets.
     */
    private DatagramSocket dataSocket;

    /**
     * Initializes a new <tt>RTPConnectorImpl</tt> which is to use a given pair
     * of datagram sockets for RTP and RTCP traffic specified in the form of a
     * <tt>StreamConnector</tt>.
     *
     * @param connector the pair of datagram sockets for RTP and RTCP traffic
     * the new instance is to use
     */
    public RTPConnectorImpl(StreamConnector connector)
    {
        if (connector == null)
            throw new NullPointerException("connector");

        this.connector = connector;
    }

    /**
     * Add a stream target. A stream target is the destination address which
     * this RTP session will send its data to. For a single session, we can add
     * multiple SessionAddresses, and for each address, one copy of data will be
     * sent to.
     *
     * @param target Destination target address
     * @throws IOException if there was a socket-related error while adding the
     * specified target
     */
    public void addTarget(SessionAddress target)
        throws IOException
    {
        getControlOutputStream()
            .addTarget(
                target.getControlAddress(),
                target.getControlPort());
        getDataOutputStream()
            .addTarget(
                target.getDataAddress(),
                target.getDataPort());
    }

    /**
     * Closes all sockets, stream, and the <tt>StreamConnector</tt> that this
     * <tt>RTPConnector</tt> is using.
     */
    public void close()
    {
        dataOutputStream = null;
        controlOutputStream = null;

        if (dataInputStream != null)
        {
            dataInputStream.close();
            dataInputStream = null;
        }
        if (controlInputStream != null)
        {
            controlInputStream.close();
            controlInputStream = null;
        }

        connector.close();
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
        return new RTCPConnectorInputStream(getControlSocket());
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
        return new RTPConnectorOutputStream(getControlSocket());
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
        return new RTPConnectorInputStream(getDataSocket());
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
        return new RTPConnectorOutputStream(getDataSocket());
    }

    /**
     * Returns the input stream that is handling incoming RTCP packets.
     *
     * @return the input stream that is handling incoming RTCP packets.
     *
     * @throws IOException if an error occurs during the creation of the RTCP
     * packet input stream
     */
    public RTPConnectorInputStream getControlInputStream()
        throws IOException
    {
        return getControlInputStream(true);
    }

    /**
     * Gets the <tt>PushSourceStream</tt> which gives access to the RTCP data
     * received from the remote targets and optionally creates it if it does not
     * exist yet.
     *
     * @param create <tt>true</tt> to create the <tt>PushSourceStream</tt> which
     * gives access to the RTCP data received from the remote targets if it does
     * not exist yet; otherwise, <tt>false</tt>
     * @return the <tt>PushBufferStream</tt> which gives access to the RTCP data
     * received from the remote targets; <tt>null</tt> if it does not exist yet
     * and <tt>create</tt> is <tt>false</tt>
     * @throws IOException if creating the <tt>PushSourceStream</tt> fails
     */
    protected RTPConnectorInputStream getControlInputStream(boolean create)
        throws IOException
    {
        if ((controlInputStream == null) && create)
            controlInputStream = createControlInputStream();
        return controlInputStream;
    }

    /**
     * Returns the input stream that is handling outgoing RTCP packets.
     *
     * @return the input stream that is handling outgoing RTCP packets.
     *
     * @throws IOException if an error occurs during the creation of the RTCP
     * packet output stream
     */
    public RTPConnectorOutputStream getControlOutputStream()
        throws IOException
    {
        return getControlOutputStream(true);
    }

    /**
     * Gets the <tt>OutputDataStream</tt> which is used to write RTCP data to be
     * sent to from the remote targets and optionally creates it if it does not
     * exist yet.
     *
     * @param create <tt>true</tt> to create the <tt>OutputDataStream</tt> which
     * is to be used to write RTCP data to be sent to the remote targets if it
     * does not exist yet; otherwise, <tt>false</tt>
     * @return the <tt>OutputDataStream</tt> which is used to write RTCP data to
     * be sent to the remote targets; <tt>null</tt> if it does not exist yet and
     * <tt>create</tt> is <tt>false</tt>
     * @throws IOException if creating the <tt>OutputDataStream</tt> fails
     */
    protected RTPConnectorOutputStream getControlOutputStream(boolean create)
        throws IOException
    {
        if ((controlOutputStream == null) && create)
            controlOutputStream = createControlOutputStream();
        return controlOutputStream;
    }

    /**
     * Gets the UDP Socket this instance uses to send and receive RTCP packets.
     *
     * @return the UDP Socket this instance uses to send and receive RTCP
     * packets
     */
    public DatagramSocket getControlSocket()
    {
        if (controlSocket == null)
            controlSocket = connector.getControlSocket();
        return controlSocket;
    }

    /**
     * Returns the input stream that is handling incoming RTP packets.
     *
     * @return the input stream that is handling incoming RTP packets.
     *
     * @throws IOException if an error occurs during the creation of the RTP
     * packet input stream
     */
    public RTPConnectorInputStream getDataInputStream()
        throws IOException
    {
        return getDataInputStream(true);
    }

    /**
     * Gets the <tt>PushSourceStream</tt> which gives access to the RTP data
     * received from the remote targets and optionally creates it if it does not
     * exist yet.
     *
     * @param create <tt>true</tt> to create the <tt>PushSourceStream</tt> which
     * gives access to the RTP data received from the remote targets if it does
     * not exist yet; otherwise, <tt>false</tt>
     * @return the <tt>PushBufferStream</tt> which gives access to the RTP data
     * received from the remote targets; <tt>null</tt> if it does not exist yet
     * and <tt>create</tt> is <tt>false</tt>
     * @throws IOException if creating the <tt>PushSourceStream</tt> fails
     */
    protected RTPConnectorInputStream getDataInputStream(boolean create)
        throws IOException
    {
        if ((dataInputStream == null) && create)
            dataInputStream = createDataInputStream();
        return dataInputStream;
    }

    /**
     * Returns the input stream that is handling outgoing RTP packets.
     *
     * @return the input stream that is handling outgoing RTP packets.
     *
     * @throws IOException if an error occurs during the creation of the RTP
     */
    public RTPConnectorOutputStream getDataOutputStream()
        throws IOException
    {
        return getDataOutputStream(true);
    }

    /**
     * Gets the <tt>OutputDataStream</tt> which is used to write RTP data to be
     * sent to from the remote targets and optionally creates it if it does not
     * exist yet.
     *
     * @param create <tt>true</tt> to create the <tt>OutputDataStream</tt> which
     * is to be used to write RTP data to be sent to the remote targets if it
     * does not exist yet; otherwise, <tt>false</tt>
     * @return the <tt>OutputDataStream</tt> which is used to write RTP data to
     * be sent to the remote targets; <tt>null</tt> if it does not exist yet and
     * <tt>create</tt> is <tt>false</tt>
     * @throws IOException if creating the <tt>OutputDataStream</tt> fails
     */
    public RTPConnectorOutputStream getDataOutputStream(boolean create)
        throws IOException
    {
        if ((dataOutputStream == null) && create)
            dataOutputStream = createDataOutputStream();
        return dataOutputStream;
    }

    /**
     * Gets the UDP socket this instance uses to send and receive RTP packets.
     *
     * @return the UDP socket this instance uses to send and receive RTP packets
     */
    public DatagramSocket getDataSocket()
    {
        if (dataSocket == null)
            dataSocket = connector.getDataSocket();
        return dataSocket;
    }

    /**
     * Provides a dummy implementation to {@link
     * RTPConnector#getReceiveBufferSize()} that always returns <tt>-1</tt>.
     */
    public int getReceiveBufferSize()
    {
        // Not applicable
        return -1;
    }

    /**
     * Provides a dummy implementation to {@link
     * RTPConnector#getRTCPBandwidthFraction()} that always returns <tt>-1</tt>.
     */
    public double getRTCPBandwidthFraction()
    {
        // Not applicable
        return -1;
    }

    /**
     * Provides a dummy implementation to {@link
     * RTPConnector#getRTCPSenderBandwidthFraction()} that always returns
     * <tt>-1</tt>.
     */
    public double getRTCPSenderBandwidthFraction()
    {
        // Not applicable
        return -1;
    }

    /**
     * Provides a dummy implementation to {@link
     * RTPConnector#getSendBufferSize()} that always returns <tt>-1</tt>.
     */
    public int getSendBufferSize()
    {
        // Not applicable
        return -1;
    }

    /**
     * Removes a target from our session. If a target is removed, there will be
     * no data sent to that address.
     *
     * @param target Destination target to be removed
     */
    public void removeTarget(SessionAddress target)
    {
        if (controlOutputStream != null)
            controlOutputStream
                .removeTarget(
                    target.getControlAddress(),
                    target.getControlPort());

        if (dataOutputStream != null)
            dataOutputStream
                .removeTarget(
                    target.getDataAddress(),
                    target.getDataPort());
    }

    /**
     * Remove all stream targets. After this operation is done. There will be
     * no targets receiving data, so no data will be sent.
     */
    public void removeTargets()
    {
        if (controlOutputStream != null)
            controlOutputStream.removeTargets();

        if (dataOutputStream != null)
            dataOutputStream.removeTargets();
    }

    /**
     * Provides a dummy implementation to {@link
     * RTPConnector#setReceiveBufferSize(int)}.
     *
     * @param size ignored.
     */
    public void setReceiveBufferSize(int size)
        throws IOException
    {
        // Nothing should be done here :-)
    }

    /**
     * Provides a dummy implementation to {@link
     * RTPConnector#setSendBufferSize(int)}.
     *
     * @param size ignored.
     */
    public void setSendBufferSize(int size)
        throws IOException
    {
        // Nothing should be done here :-)
    }
}
