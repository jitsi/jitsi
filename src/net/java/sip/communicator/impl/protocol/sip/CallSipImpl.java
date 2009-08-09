/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import javax.sip.*;

import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A SIP implementation of the Call abstract class encapsulating SIP dialogs.
 *
 * @author Emil Ivov
 */
public class CallSipImpl
    extends Call
    implements CallPeerListener
{
    private static final Logger logger = Logger.getLogger(CallSipImpl.class);

    /**
     * A list containing all <tt>CallPeer</tt>s of this call.
     */
    private final List<CallPeerSipImpl> callPeers =
        new Vector<CallPeerSipImpl>();

    /**
     * The <tt>CallSession</tt> that the media service has created for this
     * call.
     */
    private CallSession mediaCallSession = null;

    /**
     * Crates a CallSipImpl instance belonging to <tt>sourceProvider</tt> and
     * initiated by <tt>CallCreator</tt>.
     *
     * @param sourceProvider the ProtocolProviderServiceSipImpl instance in the
     *            context of which this call has been created.
     */
    protected CallSipImpl(ProtocolProviderServiceSipImpl sourceProvider)
    {
        super(sourceProvider);
    }

    /**
     * Adds <tt>callPeer</tt> to the list of peers in this call.
     * If the call peer is already included in the call, the method has
     * no effect.
     *
     * @param callPeer the new <tt>CallPeer</tt>
     */
    public void addCallPeer(CallPeerSipImpl callPeer)
    {
        if (callPeers.contains(callPeer))
            return;

        callPeer.addCallPeerListener(this);

        this.callPeers.add(callPeer);
        fireCallPeerEvent(callPeer,
            CallPeerEvent.CALL_PEER_ADDED);
    }

    /**
     * Removes <tt>callPeer</tt> from the list of peers in this
     * call. The method has no effect if there was no such peer in the
     * call.
     *
     * @param callPeer the <tt>CallPeer</tt> leaving the call;
     */
    public void removeCallPeer(CallPeerSipImpl callPeer)
    {
        if (!callPeers.contains(callPeer))
            return;

        this.callPeers.remove(callPeer);
        callPeer.removeCallPeerListener(this);

        try
        {
            fireCallPeerEvent(callPeer,
                CallPeerEvent.CALL_PEER_REMVOVED);
        }
        finally
        {

            /*
             * The peer should loose its state once it has finished
             * firing its events in order to allow the listeners to undo.
             */
            callPeer.setCall(null);
        }

        if (callPeers.size() == 0)
            setCallState(CallState.CALL_ENDED);
    }

    /**
     * Returns an iterator over all call peers.
     *
     * @return an Iterator over all peers currently involved in the call.
     */
    public Iterator<CallPeer> getCallPeers()
    {
        return new LinkedList<CallPeer>(callPeers).iterator();
    }

    /**
     * Returns the number of peers currently associated with this call.
     *
     * @return an <tt>int</tt> indicating the number of peers currently
     *         associated with this call.
     */
    public int getCallPeerCount()
    {
        return callPeers.size();
    }

    /**
     * Dummy implementation of a method (inherited from CallPeerListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void peerImageChanged(CallPeerChangeEvent evt)
    {
    }

    /**
     * Dummy implementation of a method (inherited from CallPeerListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void peerAddressChanged(CallPeerChangeEvent evt)
    {
    }

    /**
     * Dummy implementation of a method (inherited from CallPeerListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void peerTransportAddressChanged(
        CallPeerChangeEvent evt)
    {
    }

    /**
     * Dummy implementation of a method (inherited from CallPeerListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void peerDisplayNameChanged(CallPeerChangeEvent evt)
    {
    }

    /**
     * Verifies whether the call peer has entered a state.
     *
     * @param evt The <tt>CallPeerChangeEvent</tt> instance containing
     *            the source event as well as its previous and its new status.
     */
    public void peerStateChanged(CallPeerChangeEvent evt)
    {
        CallPeerState newState =
            (CallPeerState) evt.getNewValue();
        if (newState == CallPeerState.DISCONNECTED
            || newState == CallPeerState.FAILED)
        {
            removeCallPeer((CallPeerSipImpl) evt
                .getSourceCallPeer());
        }
        else if ((newState == CallPeerState.CONNECTED
               || newState == CallPeerState.CONNECTING_WITH_EARLY_MEDIA))
        {
            setCallState(CallState.CALL_IN_PROGRESS);
        }
    }

    /**
     * Returns <tt>true</tt> if <tt>dialog</tt> matches the jain sip dialog
     * established with one of the peers in this call.
     *
     * @param dialog the dialog whose corresponding peer we're looking
     *            for.
     * @return true if this call contains a call peer whose jain sip
     *         dialog is the same as the specified and false otherwise.
     */
    public boolean contains(Dialog dialog)
    {
        return findCallPeer(dialog) != null;
    }

    /**
     * Returns the call peer whose associated jain sip dialog matches
     * <tt>dialog</tt>.
     *
     * @param dialog the jain sip dialog whose corresponding peer we're
     *            looking for.
     * @return the call peer whose jain sip dialog is the same as the
     *         specified or null if no such call peer was found.
     */
    public CallPeerSipImpl findCallPeer(Dialog dialog)
    {
        Iterator<CallPeer> callPeers = this.getCallPeers();

        if (logger.isTraceEnabled())
        {
            logger.trace("Looking for peer with dialog: " + dialog
                + "among " + this.callPeers.size() + " calls");
        }

        while (callPeers.hasNext())
        {
            CallPeerSipImpl cp =
                (CallPeerSipImpl) callPeers.next();

            if (cp.getDialog() == dialog)
            {
                logger.trace("Returing cp=" + cp);
                return cp;
            }
            else
            {
                logger.trace("Ignoring cp=" + cp + " because cp.dialog="
                    + cp.getDialog() + " while dialog=" + dialog);
            }
        }

        return null;
    }

    /**
     * Sets the <tt>CallSession</tt> that the media service has created for this
     * call.
     *
     * @param callSession the <tt>CallSession</tt> that the media service has
     *            created for this call.
     */
    public void setMediaCallSession(CallSession callSession)
    {
        this.mediaCallSession = callSession;
    }

    /**
     * Sets the <tt>CallSession</tt> that the media service has created for this
     * call.
     *
     * @return the <tt>CallSession</tt> that the media service has created for
     *         this call or null if no call session has been created so far.
     */
    public CallSession getMediaCallSession()
    {
        return this.mediaCallSession;
    }
}
