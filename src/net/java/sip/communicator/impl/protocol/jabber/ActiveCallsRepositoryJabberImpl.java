/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Keeps a list of all calls currently active and maintained by this protocol
 * provider. Offers methods for finding a call by its ID, peer session
 * and others.
 *
 * @author Emil Ivov
 * @author Symphorien Wanko
 */
public class ActiveCallsRepositoryJabberImpl
    extends ActiveCallsRepository<CallJabberImpl,
                                  OperationSetBasicTelephonyJabberImpl>
{
    /**
     * It's where we store all active calls
     *
     * @param opSet the <tt>OperationSetBasicTelphony</tt> instance which has
     * been used to create calls in this repository
     */
    public ActiveCallsRepositoryJabberImpl(
                                    OperationSetBasicTelephonyJabberImpl opSet)
    {
        super(opSet);
    }

    /**
     * Returns the {@link CallJabberImpl} containing a {@link
     * CallPeerJabberImpl} whose corresponding jingle session has the specified
     * jingle <tt>sid</tt>.
     *
     * @param sid the jingle <tt>sid</tt> we're looking for.
     *
     * @return the {@link CallJabberImpl} containing the peer with the
     * specified <tt>sid</tt> or <tt>null</tt> if we couldn't find one matching
     * it.
     */
    public CallJabberImpl findJingleSID(String sid)
    {
        Iterator<CallJabberImpl> calls = getActiveCalls();

        while (calls.hasNext())
        {
            CallJabberImpl call = calls.next();
            if (call.containsJingleSID(sid))
                return call;
        }

        return null;
    }

    /**
     * Returns the {@link CallPeerJabberImpl} whose jingle session has the
     * specified jingle <tt>sid</tt>.
     *
     * @param sid the jingle <tt>sid</tt> we're looking for.
     *
     * @return the {@link CallPeerJabberImpl} with the specified <tt>sid</tt>
     * or  tt>null</tt> if we couldn't find one matching it.
     */
    public CallPeerJabberImpl findCallPeer(String sid)
    {
        Iterator<CallJabberImpl> calls = getActiveCalls();

        while (calls.hasNext())
        {
            CallJabberImpl call = calls.next();
            CallPeerJabberImpl peer = call.getPeer(sid);
            if ( peer != null )
                return peer;
        }

        return null;
    }

    /**
     * Returns the {@link CallPeerJabberImpl} whose session-init's ID has
     * the specified IQ <tt>id</tt>.
     *
     * @param id the IQ <tt>id</tt> we're looking for.
     *
     * @return the {@link CallPeerJabberImpl} with the specified <tt>id</tt>
     * or <tt>null</tt> if we couldn't find one matching it.
     */
    public CallPeerJabberImpl findCallPeerBySessInitPacketID(String id)
    {
        Iterator<CallJabberImpl> calls = getActiveCalls();

        while (calls.hasNext())
        {
            CallJabberImpl call = calls.next();
            CallPeerJabberImpl peer = call.getPeerBySessInitPacketID(id);
            if ( peer != null )
                return peer;
        }

        return null;
    }

    /**
     * Creates and dispatches a <tt>CallEvent</tt> notifying registered
     * listeners that an event with id <tt>eventID</tt> has occurred on
     * <tt>sourceCall</tt>.
     *
     * @param eventID the ID of the event to dispatch
     * @param sourceCall the call on which the event has occurred
     * @see ActiveCallsRepository#fireCallEvent(int, Call)
     */
    protected void fireCallEvent(int eventID, Call sourceCall)
    {
        parentOperationSet.fireCallEvent(eventID, sourceCall);
    }
}
