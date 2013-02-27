/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.colibri;

import org.jitsi.service.neomedia.*;

/**
 * Implements a <tt>StreamConnector</tt> which allows sharing a specific
 * <tt>StreamConnector</tt> instance among multiple <tt>TransportManager</tt>s
 * for the purposes of the Jitsi VideoBridge.
 *
 * @author Lyubomir Marinov
 */
public class ColibriStreamConnector
    extends StreamConnectorDelegate<StreamConnector>
{
    /**
     * Initializes a new <tt>ColibriStreamConnector</tt> instance which is to
     * share a specific <tt>StreamConnector</tt> instance among multiple
     * <tt>TransportManager</tt>s for the purposes of the Jitsi VideoBridge.
     *
     * @param streamConnector the <tt>StreamConnector</tt> instance to be shared
     * by the new instance among multiple <tt>TransportManager</tt>s for the
     * purposes of the Jitsi VideoBridge
     */
    public ColibriStreamConnector(StreamConnector streamConnector)
    {
        super(streamConnector);
    }

    /**
     * {@inheritDoc}
     *
     * Overrides {@link StreamConnectorDelegate#close()} in order to prevent the
     * closing of the <tt>StreamConnector</tt> wrapped by this instance because
     * the latter is shared and it is not clear whether no
     * <tt>TransportManager</tt> is using it.
     */
    @Override
    public void close()
    {
        /*
         * Do not close the shared StreamConnector because it is not clear
         * whether no TransportManager is using it.
         */
    }

    /**
     * {@inheritDoc}
     *
     * Invokes {@link #close()} on this instance when it is clear that no
     * <tt>TransportManager</tt> is using it in order to release the resources
     * allocated by this instance throughout its life time (that need explicit
     * disposal).
     */
    @Override
    protected void finalize()
        throws Throwable
    {
        try
        {
            /*
             * Close the shared StreamConnector because it is clear that no
             * TrasportManager is using it.
             */
            super.close();
        }
        finally
        {
            super.finalize();
        }
    }
}
