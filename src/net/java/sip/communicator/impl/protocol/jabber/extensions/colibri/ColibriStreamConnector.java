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
package net.java.sip.communicator.impl.protocol.jabber.extensions.colibri;

import org.jitsi.service.neomedia.*;

/**
 * Implements a <tt>StreamConnector</tt> which allows sharing a specific
 * <tt>StreamConnector</tt> instance among multiple <tt>TransportManager</tt>s
 * for the purposes of the Jitsi Videobridge.
 *
 * @author Lyubomir Marinov
 */
public class ColibriStreamConnector
    extends StreamConnectorDelegate<StreamConnector>
{
    /**
     * Initializes a new <tt>ColibriStreamConnector</tt> instance which is to
     * share a specific <tt>StreamConnector</tt> instance among multiple
     * <tt>TransportManager</tt>s for the purposes of the Jitsi Videobridge.
     *
     * @param streamConnector the <tt>StreamConnector</tt> instance to be shared
     * by the new instance among multiple <tt>TransportManager</tt>s for the
     * purposes of the Jitsi Videobridge
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
