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
     * A table mapping call ids against call instances.
     */
    private Hashtable<String, Call> activeCalls = new Hashtable<String, Call>();

    public OperationSetTelephonyConferencingGibberishImpl(
        ProtocolProviderServiceGibberishImpl provider,
        OperationSetBasicTelephonyGibberishImpl telephonyOpSet)
    {
        this.protocolProvider = provider;
        this.telephonyOpSet = telephonyOpSet;
    }

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

        return newCall;
    }

    public CallPeer inviteCalleeToCall(String uri, Call existingCall)
        throws OperationNotSupportedException
    {
        return null;
    }

    public void callPeerAdded(CallPeerEvent evt)
    {
    }

    public void callPeerRemoved(CallPeerEvent evt)
    {
    }

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
