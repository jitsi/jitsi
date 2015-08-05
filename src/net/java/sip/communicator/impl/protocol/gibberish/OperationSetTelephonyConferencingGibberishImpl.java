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
    implements OperationSetTelephonyConferencing,
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
        return createConfCall(callees, null);
    }

    /**
     * Creates a conference call with the given list of <tt>callees</tt>
     *
     * @param callees the list of <tt>callees</tt> to invite in the call
     * @param conference the <tt>CallConference</tt> which represents the state
     * of the telephony conference into which the specified callees are to be
     * invited
     * @return the created call
     * @throws OperationNotSupportedException indicates that the operation is
     * not supported for the given <tt>callees</tt>.
     */
    public Call createConfCall(String[] callees, CallConference conference)
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

             if (logger.isTraceEnabled())
                 logger.trace(  "Removing call " + sourceCall + " from the list of "
                         + "active calls because it entered an ENDED state");

             telephonyOpSet.fireCallEvent(CallEvent.CALL_ENDED, sourceCall);
         }
    }

    /**
     * {@inheritDoc}
     *
     * Unimplemented, returns <tt>null</tt>
     */
    @Override
    public ConferenceDescription setupConference(ChatRoom chatRoom)
    {
        return null;
    }
}
