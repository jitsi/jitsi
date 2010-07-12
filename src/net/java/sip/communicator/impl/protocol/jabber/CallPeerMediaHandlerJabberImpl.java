/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.net.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;

/**
 * @author Emil Ivov
 */
public class CallPeerMediaHandlerJabberImpl
    extends CallPeerMediaHandler<CallPeerJabberImpl>
{

    /**
     * Creates a new handler that will be managing media streams for
     * <tt>peer</tt>.
     *
     * @param peer that <tt>CallPeerSipImpl</tt> instance that we will be
     * managing media for.
     */
    public CallPeerMediaHandlerJabberImpl(CallPeerJabberImpl peer)
    {
        //TODO - callpeer jabber impl should implement otr listeenr
        super(peer, null);
    }

    /**
     * Should return a reference to the currently valid configuration service.
     * We need this method in order to keep the protocol-media bundle as light
     * as possible: we don't want it to have an activator and deal with a
     * bundle context.
     *
     * @return a reference to the currently valid {@link ConfigurationService}
     */
    @Override
    protected ConfigurationService getConfigurationService()
    {
        return JabberActivator.getConfigurationService();
    }

    /**
     * Returns a reference to the currently valid media service.
     *
     * @return a reference to the currently valid {@link MediaService}
     */
    @Override
    protected MediaService getMediaService()
    {
        return JabberActivator.getMediaService();
    }

    /**
     * Returns a reference to the currently valid network address service.
     *
     * @return a reference to the currently valid {@link
     * NetworkAddressManagerService}
     */
    @Override
    protected NetworkAddressManagerService getNetworkAddressManagerService()
    {
        return JabberActivator.getNetworkAddressManagerService();
    }

    /**
     * Lets the underlying implementation take note of this error and only
     * then throws it to the using bundles.
     *
     * @param message the message to be logged and then wrapped in a new
     * <tt>OperationFailedException</tt>
     * @param errorCode the error code to be assigned to the new
     * <tt>OperationFailedException</tt>
     * @param cause the <tt>Throwable</tt> that has caused the necessity to log
     * an error and have a new <tt>OperationFailedException</tt> thrown
     *
     * @throws OperationFailedException the exception that we wanted this method
     * to throw.
     */
    @Override
    protected void throwOperationFailedException(String message, int errorCode,
                    Throwable cause) throws OperationFailedException
    {
        // TODO Auto-generated method stub - implement
    }

    /**
     * Returns the <tt>InetAddress</tt> that is most likely to be to be used
     * as a next hop when contacting the specified <tt>destination</tt>. This is
     * an utility method that is used whenever we have to choose one of our
     * local addresses to put in the Via, Contact or (in the case of no
     * registrar accounts) From headers.
     *
     * @param peer the CallPeer that we would contact.
     *
     * @return the <tt>InetAddress</tt> that is most likely to be to be used
     * as a next hop when contacting the specified <tt>destination</tt>.
     *
     * @throws IllegalArgumentException if <tt>destination</tt> is not a valid
     * host/ip/fqdn
     */
    @Override
    protected InetAddress getIntendedDestination(CallPeerJabberImpl peer)
    {
        /* TODO implement */
        return null;
    }
}
