/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.media.*;

/**
 * An implementation of the <tt>CallPeerMediaHandler</tt> abstract class for the
 * common part of Jabber and Gtalk protocols.
 *
 * @author Vincent Lucas
 */
public abstract class AbstractCallPeerMediaHandlerJabberGTalkImpl
        <T extends AbstractCallPeerJabberGTalkImpl<?,?>>
    extends CallPeerMediaHandler<T>
{
    /**
     * Indicates if the <tt>CallPeer</tt> will support </tt>inputevt</tt>
     * extension (i.e. will be able to be remote-controlled).
     */
    private boolean localInputEvtAware = false;

    /**
     * Creates a new handler that will be managing media streams for
     * <tt>peer</tt>.
     *
     * @param peer that <tt>AbstractCallPeerJabberGTalkImpl</tt> instance that
     * we will be managing media for.
     */
    public AbstractCallPeerMediaHandlerJabberGTalkImpl(T peer)
    {
        super(peer, peer);
    }

    /**
     * Gets the <tt>inputevt</tt> support: true for enable, false for disable.
     *
     * @return The state of inputevt support: true for enable, false for
     * disable.
     */
    public boolean getLocalInputEvtAware()
    {
        return this.localInputEvtAware;
    }

    /**
     * Enable or disable <tt>inputevt</tt> support (remote-control).
     *
     * @param enable new state of inputevt support
     */
    public void setLocalInputEvtAware(boolean enable)
    {
        localInputEvtAware = enable;
    }
}
