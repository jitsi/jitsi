/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import java.net.*;

/**
 * Implements a {@link StreamConnector} which wraps a specific
 * <tt>StreamConnector</tt> instance.
 *
 * @param <T> the very type of the <tt>StreamConnector</tt> wrapped by
 * <tt>StreamConnectorDelegate</tt>
 *
 * @author Lyubomir Marinov
 */
public class StreamConnectorDelegate<T extends StreamConnector>
    implements StreamConnector
{
    /**
     * The <tt>StreamConnector</tt> wrapped by this instance.
     */
    protected final T streamConnector;

    /**
     * Initializes a new <tt>StreamConnectorDelegate</tt> which is to wrap a
     * specific <tt>StreamConnector</tt>.
     *
     * @param streamConnector the <tt>StreamConnector</tt> to be wrapped by the
     * new instance
     */
    public StreamConnectorDelegate(T streamConnector)
    {
        if (streamConnector == null)
            throw new NullPointerException("streamConnector");

        this.streamConnector = streamConnector;
    }

    /**
     * Releases the resources allocated by this instance in the course of its
     * execution and prepares it to be garbage collected. Calls
     * {@link StreamConnector#close()} on the <tt>StreamConnector</tt> wrapped
     * by this instance.
     */
    public void close()
    {
        streamConnector.close();
    }

    public DatagramSocket getControlSocket()
    {
        return streamConnector.getControlSocket();
    }

    public Socket getControlTCPSocket()
    {
        return streamConnector.getControlTCPSocket();
    }

    public DatagramSocket getDataSocket()
    {
        return streamConnector.getDataSocket();
    }

    public Socket getDataTCPSocket()
    {
        return streamConnector.getDataTCPSocket();
    }

    public Protocol getProtocol()
    {
        return streamConnector.getProtocol();
    }

    public void started()
    {
        streamConnector.started();
    }

    public void stopped()
    {
        streamConnector.stopped();
    }
}
