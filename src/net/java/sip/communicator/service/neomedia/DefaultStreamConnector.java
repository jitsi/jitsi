/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import java.net.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.util.*;

/**
 * Represents a default implementation of <tt>StreamConnector</tt> which is
 * initialized with a specific pair of control and data <tt>DatagramSocket</tt>s
 * and which closes them (if they exist) when its {@link #close()} is invoked.
 *
 * @author Lubomir Marinov
 */
public class DefaultStreamConnector
    implements StreamConnector
{

    /**
     * The <tt>Logger</tt> used by the <tt>DefaultStreamConnector</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(DefaultStreamConnector.class);

    /**
     * The default number of binds that a Media Service Implementation should
     * execute in case a port is already bound to (each retry would be on a
     * new random port).
     */
    public static final int BIND_RETRIES_DEFAULT_VALUE = 50;

    /**
     * The name of the property containing the number of binds that a Media
     * Service Implementation should execute in case a port is already
     * bound to (each retry would be on a new port in the allowed boundaries).
     */
    public static final String BIND_RETRIES_PROPERTY_NAME
        = "net.java.sip.communicator.service.media.BIND_RETRIES";

    /**
     * The name of the property that contains the maximum port number that we'd
     * like our RTP managers to bind upon.
     */
    public static final String MAX_PORT_NUMBER_PROPERTY_NAME
        = "net.java.sip.communicator.service.media.MAX_PORT_NUMBER";

    /**
     * The maxium port number <tt>DefaultStreamConnector</tt> instances are to
     * attempt to bind to.
     */
    private static int maxPort = -1;

    /**
     * The name of the property that contains the minimum port number that we'd
     * like our RTP managers to bind upon.
     */
    public static final String MIN_PORT_NUMBER_PROPERTY_NAME
        = "net.java.sip.communicator.service.media.MIN_PORT_NUMBER";

    /**
     * The minimum port number <tt>DefaultStreamConnector</tt> instances are to
     * attempt to bind to.
     */
    private static int minPort = -1;

    /**
     * Creates a new <tt>DatagramSocket</tt> instance which is bound to the
     * specified local <tt>InetAddress</tt> and its port is within the range
     * defined by the <tt>ConfigurationService</tt> properties
     * {@link #MIN_PORT_NUMBER_PROPERTY_NAME} and
     * {@link #MAX_PORT_NUMBER_PROPERTY_NAME}. Attempts at most
     * {@link #BIND_RETRIES_PROPERTY_NAME} times to bind.
     *
     * @param bindAddr the local <tt>InetAddress</tt> the new
     * <tt>DatagramSocket</tt> is to bind to
     * @return a new <tt>DatagramSocket</tt> instance bound to the specified
     * local <tt>InetAddress</tt>
     */
    private static synchronized DatagramSocket createDatagramSocket(
            InetAddress bindAddr)
    {
        ConfigurationService config
            = NeomediaActivator.getConfigurationService();
        int bindRetries
            = config
                .getInt(BIND_RETRIES_PROPERTY_NAME, BIND_RETRIES_DEFAULT_VALUE);
        if (maxPort < 0)
            maxPort = config.getInt(MAX_PORT_NUMBER_PROPERTY_NAME, 6000);

        for (int i = 0; i < bindRetries; i++)
        {
            if ((minPort < 0) || (minPort > maxPort))
                minPort = config.getInt(MIN_PORT_NUMBER_PROPERTY_NAME, 5000);

            int port = minPort++;

            try
            {
                return new DatagramSocket(port, bindAddr);
            }
            catch (SocketException se)
            {
                logger
                    .warn(
                        "Retrying a bind because of a failure to bind to address "
                            + bindAddr
                            + " and port "
                            + port,
                        se);
            }
        }
        return null;
    }

    /**
     * The local <tt>InetAddress</tt> this <tt>StreamConnector</tt> attempts to
     * bind to on demand.
     */
    private final InetAddress bindAddr;

    /**
     * The <tt>DatagramSocket</tt> that a stream should use for control data
     * (e.g. RTCP) traffic.
     */
    protected DatagramSocket controlSocket;

    /**
     * The <tt>DatagramSocket</tt> that a stream should use for data (e.g. RTP)
     * traffic.
     */
    protected DatagramSocket dataSocket;

    /**
     * Initializes a new <tt>DefaultStreamConnector</tt> instance with no
     * control and data <tt>DatagramSocket</tt>s.
     * <p>
     * Suitable for extenders willing to delay the creation of the control and
     * data sockets. For example, they could override
     * {@link #getControlSocket()} and/or {@link #getDataSocket()} and create
     * them on demand.
     */
    public DefaultStreamConnector()
    {
        this(null, null);
    }

    /**
     * Initializes a new <tt>DefaultStreamConnector</tt> instance with a
     * specific bind <tt>InetAddress</tt>. The new instance is to attempt to
     * bind on demand to the specified <tt>InetAddress</tt> in the port range
     * defined by the <tt>ConfigurationService</tt> properties
     * {@link #MIN_PORT_NUMBER_PROPERTY_NAME} and
     * {@link #MAX_PORT_NUMBER_PROPERTY_NAME} at most
     * {@link #BIND_RETRIES_PROPERTY_NAME} times.
     *
     * @param bindAddr the local <tt>InetAddress</tt> the new instance is to
     * attempt to bind to
     */
    public DefaultStreamConnector(InetAddress bindAddr)
    {
        this.bindAddr = bindAddr;
    }

    /**
     * Initializes a new <tt>DefaultStreamConnector</tt> instance which is to
     * represent a specific pair of control and data <tt>DatagramSocket</tt>s.
     *
     * @param controlSocket the <tt>DatagramSocket</tt> to be used for control
     * data (e.g. RTCP) traffic
     * @param dataSocket the <tt>DatagramSocket</tt> to be used for data (e.g.
     * RTP) traffic
     */
    public DefaultStreamConnector(
            DatagramSocket controlSocket,
            DatagramSocket dataSocket)
    {
        this.controlSocket = controlSocket;
        this.dataSocket = dataSocket;
        this.bindAddr = null;
    }

    /*
     * Implements StreamConnector#close().
     */
    public void close()
    {
        if (controlSocket != null)
            controlSocket.close();
        if (dataSocket != null)
            dataSocket.close();
    }

    /*
     * Implements StreamConnector#getControlSocket().
     */
    public DatagramSocket getControlSocket()
    {
        if ((controlSocket == null) && (bindAddr != null))
            controlSocket = createDatagramSocket(bindAddr);
        return controlSocket;
    }

    /*
     * Implements StreamConnector#getDataSocket().
     */
    public DatagramSocket getDataSocket()
    {
        if ((dataSocket == null) && (bindAddr != null))
            dataSocket = createDatagramSocket(bindAddr);
        return dataSocket;
    }

    /*
     * Implements StreamConnector#started(). Does nothing.
     */
    public void started()
    {
    }

    /*
     * Implements StreamConnector#stopped(). Does nothing.
     */
    public void stopped()
    {
    }
}
