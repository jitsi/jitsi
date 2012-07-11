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
        <T extends AbstractCallPeerJabberGTalkImpl<?, ?>>
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
     * Initializes a new <tt>AbstractCallJabberGTalkImpl</tt> instance belonging
     * to <tt>sourceProvider</tt> and associated with the jingle session with
     * the specified <tt>jingleSID</tt> or <tt>sessionID</tt>. If the new
     * instance corresponds to an incoming jingle or Google Talk session, then
     * the jingleSID or sessionID would come from there.  Otherwise, one could
     * generate one using {@link JingleIQ#generateSID()} or {@link
     * SessionIQ#generateSID()}.
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
     * @param allowed if the local video is allowed or not
     * @throws OperationFailedException if problem occurred during message
     * generation or network problem
     */
    public abstract void modifyVideoContent(boolean allowed)
        throws OperationFailedException;
}
