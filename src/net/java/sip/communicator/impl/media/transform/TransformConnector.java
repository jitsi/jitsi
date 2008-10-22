/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.transform;

import java.io.*;
import java.net.*;

import javax.media.protocol.*;
import javax.media.rtp.*;

/**
 * TransformConnector implements the RTPConnector interface. RTPConnector
 * is originally designed for programmers to abstract the underlying transport
 * mechanism for RTP control and data from the RTPManager. However, it provides
 * the possibility to modify / transform the RTP and RTCP packets before
 * they are sent to network, or after the have been received from the network.
 *
 * The RTPConnector interface is very powerful. But just to perform packets
 * transformation, we do not need all the flexibility. So, we designed this
 * TransformConnector, which uses UDP to transfer RTP/RTCP packets just like
 * normal RTP stack, and then provides the TransformInputStream interface for
 * people to define their own transformation.
 *
 * With TransformConnector, people can implement RTP/RTCP packets transformation
 * and/or manipulation by implementing the TransformEngine interface.
 *
 * @see TransformEngine
 * @see RTPConnector
 * @see RTPManager
 *
 * @author Bing SU (nova.su@gmail.com)
 */
public class TransformConnector
    implements RTPConnector
{
    /**
     * The customized TransformEngine object, which contains the concrete
     * transform logic.
     */
    protected TransformEngine engine;

    /**
     * Local RTP session listen address.
     */
    private SessionAddress localAddr;

    /**
     * RTP packet input stream object used by RTPManager.
     */
    private TransformInputStream dataInputStream;

    /**
     * RTCP packet input stream object used by RTPManager.
     */
    private TransformInputStream ctrlInputStream;

    /**
     * RTP packet output stream used by RTPManager.
     */
    private TransformOutputStream dataOutputStream;

    /**
     * RTCP packet output stream used by RTPManager.
     */
    private TransformOutputStream ctrlOutputStream;

    /**
     * UDP Socket we used to send and receive RTP packets.
     */
    private DatagramSocket dataSocket;

    /**
     * UDP Socket we used to send and receive RTCP packets.
     */
    private DatagramSocket ctrlSocket;

    /**
     * Construct a TransformConnector based on the given local RTP session
     * address and a customized TransformEngine.
     *
     * @param localAddr The local listen address of this RTP session
     * @param engine TransformEngine object which contains your transformation
     * logic
     *
     * @throws InvalidSessionAddressException if session address is invalid,
     */
    public TransformConnector(SessionAddress localAddr, TransformEngine engine)
        throws InvalidSessionAddressException
    {
        this.localAddr = localAddr;
        this.engine = engine;

        try
        {
            this.dataSocket = new DatagramSocket(this.localAddr.getDataPort(),
                                            this.localAddr.getDataAddress());
            this.ctrlSocket = new DatagramSocket(
                                            this.localAddr.getControlPort(),
                                            this.localAddr.getControlAddress());
        }
        catch (SocketException e)
        {
            throw new InvalidSessionAddressException();
        }
    }

    /**
     * Closes this RTPConnector object
     *
     * @see javax.media.rtp.RTPConnector#close()
     */
    public void close()
    {
        this.dataOutputStream = null;
        this.ctrlOutputStream = null;

        if (this.dataInputStream != null)
        {
            this.dataInputStream.close();
            this.dataInputStream = null;
        }

        if (this.ctrlInputStream != null)
        {
            this.ctrlInputStream.close();
            this.ctrlInputStream = null;
        }

        this.dataSocket.close();
        this.dataSocket = null;

        this.ctrlSocket.close();
        this.ctrlSocket = null;
    }

    /**
     * Add a stream target. A stream target is the destination address which
     * this RTP session will send its data to. For a single session, we can add
     * multiple SessionAddresses, and for each address, one copy of data will be
     * sent to.
     *
     * @param target Destination target address
     */
    public void addTarget(SessionAddress target)
    {
        if (this.ctrlOutputStream == null)
        {
            this.ctrlOutputStream =
                new TransformOutputStream(this.ctrlSocket,
                                          this.engine.getRTCPTransformer());
        }

        this.ctrlOutputStream.addTarget(target.getControlAddress(),
                                        target.getControlPort());

        if (this.dataOutputStream == null)
        {
            this.dataOutputStream =
                new TransformOutputStream(this.dataSocket,
                                          this.engine.getRTPTransformer());
        }

        this.dataOutputStream.addTarget(target.getDataAddress(),
                                        target.getDataPort());
    }

    /**
     * Removes a target from our session. If a target is removed, there will be
     * no data sent to that address.
     *
     * @param target Destination target to be removed
     * @return true if the target address is removed successfully
     *         false if there is something wrong
     */
    public boolean removeTarget(SessionAddress target)
    {
        boolean ok = true;

        if (this.ctrlOutputStream != null)
        {
            ok &= this.ctrlOutputStream.removeTarget(target.getControlAddress(),
                                                    target.getControlPort());
        }

        if (this.dataOutputStream != null)
        {
            ok &= this.dataOutputStream.removeTarget(target.getDataAddress(),
                                                    target.getDataPort());
        }

        return ok;
    }

    /**
     * Remove all stream targets. After this operation is done. There will be
     * no targets receiving data, so no data will be sent.
     */
    public void removeTargets()
    {
        if (this.ctrlOutputStream != null)
        {
            this.ctrlOutputStream.removeTargets();
        }

        if (this.dataOutputStream != null)
        {
            this.dataOutputStream.removeTargets();
        }
    }

    /* (non-Javadoc)
     * @see javax.media.rtp.RTPConnector#getControlInputStream()
     */
    public PushSourceStream getControlInputStream()
        throws IOException
    {
        if (this.ctrlInputStream == null)
        {
            this.ctrlInputStream =
                new TransformInputStream(this.ctrlSocket,
                                        this.engine.getRTCPTransformer());
        }

        return this.ctrlInputStream;
    }

    /* (non-Javadoc)
     * @see javax.media.rtp.RTPConnector#getControlOutputStream()
     */
    public OutputDataStream getControlOutputStream()
        throws IOException
    {
        if (this.ctrlOutputStream == null)
        {
            this.ctrlOutputStream =
                new TransformOutputStream(this.ctrlSocket,
                                        this.engine.getRTCPTransformer());
        }

        return this.ctrlOutputStream;
    }

    /* (non-Javadoc)
     * @see javax.media.rtp.RTPConnector#getDataInputStream()
     */
    public PushSourceStream getDataInputStream()
        throws IOException
    {
        if (this.dataInputStream == null)
        {
            this.dataInputStream =
                new TransformInputStream(this.dataSocket,
                                         this.engine.getRTPTransformer());
        }

        return this.dataInputStream;
    }

    /* (non-Javadoc)
     * @see javax.media.rtp.RTPConnector#getDataOutputStream()
     */
    public OutputDataStream getDataOutputStream()
        throws IOException
    {
        if (this.dataOutputStream == null)
        {
            this.dataOutputStream =
                new TransformOutputStream(this.dataSocket,
                                          this.engine.getRTPTransformer());
        }

        return this.dataOutputStream;
    }

    /* (non-Javadoc)
     * @see javax.media.rtp.RTPConnector#getRTCPBandwidthFraction()
     */
    public double getRTCPBandwidthFraction()
    {
        // Not applicable
        return -1;
    }

    /* (non-Javadoc)
     * @see javax.media.rtp.RTPConnector#getRTCPSenderBandwidthFraction()
     */
    public double getRTCPSenderBandwidthFraction()
    {
        // Not applicable
        return -1;
    }

    /* (non-Javadoc)
     * @see javax.media.rtp.RTPConnector#getReceiveBufferSize()
     */
    public int getReceiveBufferSize()
    {
        // Not applicable
        return -1;
    }

    /* (non-Javadoc)
     * @see javax.media.rtp.RTPConnector#setReceiveBufferSize(int)
     */
    public void setReceiveBufferSize(int size)
        throws IOException
    {
        // Nothing should be done here :-)
    }

    /* (non-Javadoc)
     * @see javax.media.rtp.RTPConnector#getSendBufferSize()
     */
    public int getSendBufferSize()
    {
        // Not applicable
        return -1;
    }

    /* (non-Javadoc)
     * @see javax.media.rtp.RTPConnector#setSendBufferSize(int)
     */
    public void setSendBufferSize(int size)
        throws IOException
    {
        // Nothing should be done here :-)
    }

    /**
     * Getter to use in derived classes.
     * (Could modify the member variable to protected instead for direct access)
     *
     * @return the control input stream
     */
    public TransformInputStream getCtrlInputStream()
    {
        return ctrlInputStream;
    }

    /**
     * Getter to use in derived classes.
     * (Could modify the member variable to protected instead for direct access)
     *
     * @return the control output stream
     */
    public TransformOutputStream getCtrlOutputStream()
    {
        return ctrlOutputStream;
    }

    /**
     * Setter to use in derived classes.
     * (Could modify the member variable to protected instead for direct access)
     *
     * @param ctrlOutputStream the control output stream to be set
     */
    public void setCtrlOutputStream(TransformOutputStream ctrlOutputStream)
    {
        this.ctrlOutputStream = ctrlOutputStream;
    }

    /**
     * Setter to use in derived classes.
     * (Could modify the member variable to protected instead for direct access)
     *
     * @param dataInputStream the data input stream to be set
     */
    public void setDataInputStream(TransformInputStream dataInputStream)
    {
        this.dataInputStream = dataInputStream;
    }

    /**
     * Setter to use in derived classes.
     * (Could modify the member variable to protected instead for direct access)
     *
     * @param ctrlInputStream the control input stream to be set
     */
    public void setCtrlInputStream(TransformInputStream ctrlInputStream)
    {
        this.ctrlInputStream = ctrlInputStream;
    }

    /**
     * Setter to use in derived classes.
     * (Could modify the member variable to protected instead for direct access)
     *
     * @param dataOutputStream the data output stream to be set
     */
    public void setDataOutputStream(TransformOutputStream dataOutputStream)
    {
        this.dataOutputStream = dataOutputStream;
    }

    /**
     * Getter to use in derived classes.
     * (Could modify the member variable to protected instead for direct access)
     *
     * @return the data socket
     */
    public DatagramSocket getDataSocket()
    {
        return dataSocket;
    }

    /**
     * Getter to use in derived classes.
     * (Could modify the member variable to protected instead for direct access)
     *
     * @return the control socket
     */
    public DatagramSocket getCtrlSocket()
    {
        return ctrlSocket;
    }

    /**
     * Getter to use in derived classes.
     * (Could modify the member variable to protected instead for direct access)
     *
     * @return the engine
     */
    public TransformEngine getEngine()
    {
        return engine;
    }
}
