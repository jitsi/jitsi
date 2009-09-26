/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.transform;

import java.io.*;

import javax.media.rtp.*;

import net.java.sip.communicator.impl.media.*;

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
 * @author Lubomir Marinov
 */
public class TransformConnector
    extends RTPConnectorImpl
{

    /**
     * The customized TransformEngine object, which contains the concrete
     * transform logic.
     */
    protected final TransformEngine engine;

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
        super(localAddr);

        this.engine = engine;
    }

    /*
     * Overrides RTPConnectorImpl#createControlInputStream() to use
     * TransformInputStream.
     */
    protected TransformInputStream createControlInputStream()
        throws IOException
    {
        return
            new TransformInputStream(
                    controlSocket,
                    engine.getRTCPTransformer());
    }

    /*
     * Overrides RTPConnectorImpl#createControlOutputStream() to use
     * TransformOutputStream.
     */
    protected TransformOutputStream createControlOutputStream()
        throws IOException
    {
        return
            new TransformOutputStream(
                    controlSocket,
                    engine.getRTCPTransformer());
    }

    /*
     * Overrides RTPConnectorImpl#createDataInputStream() to use
     * TransformInputStream.
     */
    protected TransformInputStream createDataInputStream()
        throws IOException
    {
        return new TransformInputStream(dataSocket, engine.getRTPTransformer());
    }

    /*
     * Overrides RTPConnectorImpl#createDataOutputStream() to use
     * TransformOutputStream.
     */
    protected TransformOutputStream createDataOutputStream()
        throws IOException
    {
        return
            new TransformOutputStream(dataSocket, engine.getRTPTransformer());
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
