/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.cobri;

import net.java.sip.communicator.service.neomedia.*;

/**
 * Implements a <tt>StreamConnector</tt> which allows sharing a specific
 * <tt>StreamConnector</tt> instance among multiple <tt>TransportManager</tt>s
 * for the purposes of the Jitsi VideoBridge.
 *
 * @author Lyubomir Marinov
 */
public class CobriStreamConnector
    extends StreamConnectorDelegate<StreamConnector>
{
    /**
     * Initializes a new <tt>CobriStreamConnector</tt> instance which is to
     * share a specific <tt>StreamConnector</tt> instance among multiple
     * <tt>TransportManager</tt>s for the purposes of the Jitsi VideoBridge.
     *
     * @param streamConnector the <tt>StreamConnector</tt> instance to be shared
     * by the new instance among multiple <tt>TransportManager</tt>s for the
     * purposes of the Jitsi VideoBridge
     */
    public CobriStreamConnector(StreamConnector streamConnector)
    {
        super(streamConnector);
    }

    @Override
    public void close()
    {
        /*
         * Do not close the shared StreamConnector because it is not clear
         * whether no TransportManager is using it.
         */
    }

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
