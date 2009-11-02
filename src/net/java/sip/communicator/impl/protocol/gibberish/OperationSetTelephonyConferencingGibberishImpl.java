/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.gibberish;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A basic implementation of the <tt>OperationSetTelephonyConferencing</tt> for
 * the Gibberish protocol (used for test purposes).
 *
 * @author Yana Stamcheva
 */
public class OperationSetTelephonyConferencingGibberishImpl
    implements  OperationSetTelephonyConferencing,
                CallChangeListener
{
    private static final Logger logger
        = Logger.getLogger(OperationSetTelephonyConferencingGibberishImpl.class);

    /**
     * A reference to the <tt>ProtocolProviderServiceGibberishImpl</tt> instance
     * that created us.
     */
    private final ProtocolProviderServiceGibberishImpl protocolProvider;

    /**
     * A reference to the <tt>OperationSetBasicTelephonyGibberishImpl<tt> used
     * to manage calls.
     */
    private final OperationSetBasicTelephonyGibberishImpl telephonyOpSet;

    /**
     * A table mapping call id-s against call instances.
     */
    private Hashtable<String, Call> activeCalls = new Hashtable<String, Call>();

    /**
     * Creates an <tt>OperationSetTelephonyConferencingGibberishImpl</tt> by
     * specifying the protocol <tt>provider</tt> and the according
     * <tt>telephonyOpSet</tt>.
     * @param provider the protocol provider
     * @param telephonyOpSet the according telephony operation set
     */
    public OperationSetTelephonyConferencingGibberishImpl(
        ProtocolProviderServiceGibberishImpl provider,
        OperationSetBasicTelephonyGibberishImpl telephonyOpSet)
    {
        this.protocolProvider = provider;
        this.telephonyOpSet = telephonyOpSet;
    }

    /**
     * Creates a conference call with the given list of <tt>callees</tt>
     * @param callees the list of <tt>callees</tt> to invite in the call
     * @return the created call
     * @throws OperationNotSupportedException indicates that the operation is
     * not supported for the given <tt>callees</tt>.
     */
    public Call createConfCall(String[] callees)
        throws OperationNotSupportedException
    {
        CallGibberishImpl newCall = new CallGibberishImpl(protocolProvider);

        newCall.addCallChangeListener(this);
        activeCalls.put(newCall.getCallID(), newCall);

        for (String callee : callees)
        {
            CallPeerGibberishImpl callPeer
                = new CallPeerGibberishImpl(callee, newCall);

            newCall.addCallPeer(callPeer);
        }

        telephonyOpSet.fireCallEvent(CallEvent.CALL_INITIATED, newCall);

        final Random random = new Random();
        Timer timer1 = new Timer(false);
        timer1.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                telephonyOpSet.fireLocalUserSoundLevelEvent(
                                            protocolProvider,
                                            random.nextInt(255));
            }
        }, 500, 100);

        return newCall;
    }

    /**
     * Invites the given <tt>callee</tt> to the given <tt>existingCall</tt>.
     * @param callee the address of the callee to invite
     * @param existingCall the call, to which she will be invited
     * @return the <tt>CallPeer</tt> corresponding  to the invited
     * <tt>callee</tt>
     * @throws OperationNotSupportedException if the operation is not supported
     */
    public CallPeer inviteCalleeToCall(String callee, Call existingCall)
        throws OperationNotSupportedException
    {
        CallGibberishImpl gibberishCall = (CallGibberishImpl) existingCall;

        CallPeerGibberishImpl callPeer
            = new CallPeerGibberishImpl(callee, gibberishCall);

        gibberishCall.addCallPeer(callPeer);

        return callPeer;
    }

    public void callPeerAdded(CallPeerEvent evt) {}

    public void callPeerRemoved(CallPeerEvent evt) {}

    public void callStateChanged(CallChangeEvent evt)
    {
        if(evt.getEventType().equals(CallChangeEvent.CALL_STATE_CHANGE)
            && ((CallState)evt.getNewValue()).equals(CallState.CALL_ENDED))
         {
             CallGibberishImpl sourceCall = (CallGibberishImpl) this.activeCalls
                 .remove(evt.getSourceCall().getCallID());

             logger.trace(  "Removing call " + sourceCall + " from the list of "
                          + "active calls because it entered an ENDED state");

             telephonyOpSet.fireCallEvent(CallEvent.CALL_ENDED, sourceCall);
         }
    }
}
