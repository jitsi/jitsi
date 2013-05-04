/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;

/**
 * An implementation of the <tt>Call</tt> abstract class for the common part of
 * Jabber and Gtalk protocols.
 *
 * @author Vincent Lucas
 */
public abstract class AbstractCallJabberGTalkImpl
        <T extends AbstractCallPeerJabberGTalkImpl<?, ?, ?>>
    extends MediaAwareCall<
        T,
        OperationSetBasicTelephonyJabberImpl,
        ProtocolProviderServiceJabberImpl>
{
    /**
     * Indicates if the <tt>CallPeer</tt> will support <tt>inputevt</tt>
     * extension (i.e. will be able to be remote-controlled).
     */
    private boolean localInputEvtAware = false;

    /**
     * Initializes a new <tt>AbstractCallJabberGTalkImpl</tt> instance.
     *
     * @param parentOpSet the {@link OperationSetBasicTelephonyJabberImpl}
     * instance in the context of which this call has been created.
     */
    protected AbstractCallJabberGTalkImpl(
            OperationSetBasicTelephonyJabberImpl parentOpSet)
    {
        super(parentOpSet);

        //let's add ourselves to the calls repo. we are doing it ourselves just
        //to make sure that no one ever forgets.
        //parentOpSet.getActiveCallsRepository().addCall(this);
    }

    /**
     * Enable or disable <tt>inputevt</tt> support (remote control).
     *
     * @param enable new state of inputevt support
     */
    public void setLocalInputEvtAware(boolean enable)
    {
        localInputEvtAware = enable;
    }

    /**
     * Returns if the call support <tt>inputevt</tt> (remote control).
     *
     * @return true if the call support <tt>inputevt</tt>, false otherwise
     */
    public boolean getLocalInputEvtAware()
    {
        return localInputEvtAware;
    }

    /**
     * Send a <tt>content-modify</tt> message for all current <tt>CallPeer</tt>
     * to reflect possible video change in media setup.
     *
     * @throws OperationFailedException if problem occurred during message
     * generation or network problem
     */
    public abstract void modifyVideoContent()
        throws OperationFailedException;

    /**
     * Returns the peer whose corresponding session has the specified
     * <tt>sid</tt>.
     *
     * @param sid the ID of the session whose peer we are looking for.
     *
     * @return the {@link CallPeerJabberImpl} with the specified jingle
     * <tt>sid</tt> and <tt>null</tt> if no such peer exists in this call.
     */
    public T getPeer(String sid)
    {
        for(T peer : getCallPeerList())
        {
            if (peer.getSID().equals(sid))
                return peer;
        }
        return null;
    }

    /**
     * Determines if this call contains a peer whose corresponding session has
     * the specified <tt>sid</tt>.
     *
     * @param sid the ID of the session whose peer we are looking for.
     *
     * @return <tt>true</tt> if this call contains a peer with the specified
     * jingle <tt>sid</tt> and false otherwise.
     */
    public boolean containsSID(String sid)
    {
        return (getPeer(sid) != null);
    }

    /**
     * Returns the peer whose corresponding session-init ID has the specified
     * <tt>id</tt>.
     *
     * @param id the ID of the session-init IQ whose peer we are looking for.
     *
     * @return the {@link CallPeerJabberImpl} with the specified IQ
     * <tt>id</tt> and <tt>null</tt> if no such peer exists in this call.
     */
    public T getPeerBySessInitPacketID(String id)
    {
        for(T peer : getCallPeerList())
        {
            if (peer.getSessInitID().equals(id))
                return peer;
        }
        return null;
    }
}
