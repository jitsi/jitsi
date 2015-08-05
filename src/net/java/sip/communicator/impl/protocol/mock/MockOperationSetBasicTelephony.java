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
package net.java.sip.communicator.impl.protocol.mock;

import java.text.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

/**
 * A mock implementation of a basic telephony operation set
 *
 * @author Damian Minkov
 */
public class MockOperationSetBasicTelephony
    extends AbstractOperationSetBasicTelephony<MockProvider>
    implements CallChangeListener
{
    private static final Logger logger
        = Logger.getLogger(MockOperationSetBasicTelephony.class);

    /**
     * A reference to the <tt>ProtocolProviderServiceSipImpl</tt> instance
     * that created us.
     */
    private MockProvider protocolProvider = null;

    /**
     * A table mapping call ids against call instances.
     */
    private Hashtable<String, Call> activeCalls = new Hashtable<String, Call>();

    public MockOperationSetBasicTelephony(MockProvider protocolProvider)
    {
        this.protocolProvider = protocolProvider;
    }

    /**
     * Indicates a user request to answer an incoming call from the specified
     * CallPeer.
     *
     * @param peer the call peer that we'd like to answer.
     * @throws OperationFailedException with the corresponding code if we
     *   encounter an error while performing this operation.
     */
    public void answerCallPeer(CallPeer peer) throws
        OperationFailedException
    {
        MockCallPeer callPeer
            = (MockCallPeer)peer;
        if(peer.getState().equals(CallPeerState.CONNECTED))
        {
            if (logger.isInfoEnabled())
                logger.info("Ignoring user request to answer a CallPeer "
                        + "that is already connected. CP:" + peer);
            return;
        }

        callPeer.setState(CallPeerState.CONNECTED, null);
    }

    /**
     * {@inheritDoc}
     *
     * Ignores the <tt>CallConference</tt> argument.
     */
    public Call createCall(String callee, CallConference conference)
        throws OperationFailedException,
               ParseException
    {
        return createNewCall(callee);
    }

    /**
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt>
     * given by her <tt>Contact</tt> to it.
     *
     * @param callee the address of the callee who we should invite to a new
     * call
     * @param calleeResource the specific resource to which the invite should be
     * sent
     * @param conference the <tt>CallConference</tt> in which the newly-created
     * <tt>Call</tt> is to participate
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call
     */
    public Call createCall(Contact callee, ContactResource calleeResource,
                            CallConference conference)
        throws OperationFailedException
    {
        // We don't support callee resource.
        return createCall(callee, conference);
    }

    private Call createNewCall(String address)
    {
        MockCall newCall = new MockCall(protocolProvider);

        newCall.addCallChangeListener(this);
        activeCalls.put(newCall.getCallID(), newCall);

        new MockCallPeer(address, newCall);

        return newCall;
    }

    /**
     * Returns an iterator over all currently active calls.
     *
     * @return Iterator
     */
    public Iterator<Call> getActiveCalls()
    {
        return activeCalls.values().iterator();
    }

    /**
     * Indicates a user request to end a call with the specified call
     * particiapnt.
     *
     * @param peer the peer that we'd like to hang up on.
     * @throws OperationFailedException with the corresponding code if we
     *   encounter an error while performing this operation.
     */
    public void hangupCallPeer(CallPeer peer) throws
        OperationFailedException
    {
        //do nothing if the call is already ended
        if (peer.getState().equals(CallPeerState.DISCONNECTED))
        {
            if (logger.isDebugEnabled())
                logger.debug("Ignoring a request to hangup a call peer "
                         +"that is already DISCONNECTED");
            return;
        }

        MockCallPeer callPeer
            = (MockCallPeer)peer;

        if (logger.isInfoEnabled())
            logger.info("hangupCallPeer");
        callPeer.setState(CallPeerState.DISCONNECTED, null);
    }
    /**
     * Ends the call with the specified <tt>peer</tt>.
     *
     * @param peer the peer that we'd like to hang up on.
     * @param reasonCode indicates if the hangup is following to a call failure or
     * simply a disconnect indicate by the reason.
     * @param reason the reason of the hangup. If the hangup is due to a call
     * failure, then this string could indicate the reason of the failure
     *
     * @throws OperationFailedException if we fail to terminate the call.
     */
    public void hangupCallPeer(CallPeer peer, int reasonCode, String reason)
        throws
        OperationFailedException
    {
        hangupCallPeer(peer);
    }

    /**
     * Resumes communication with a call peer previously put on hold.
     *
     * @param peer the call peer to put on hold.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetBasicTelephony
     *   method
     */
    public void putOffHold(CallPeer peer)
    {

    }

    /**
     * Puts the specified CallPeer "on hold".
     *
     * @param peer the peer that we'd like to put on hold.
     * @throws OperationFailedException with the corresponding code if we
     *   encounter an error while performing this operation.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetBasicTelephony
     *   method
     */
    public void putOnHold(CallPeer peer) throws
        OperationFailedException
    {
    }

    public Call receiveCall(String fromAddress)
        throws Exception
    {
        Call newCall = createCall(fromAddress);
        fireCallEvent(CallEvent.CALL_RECEIVED, newCall);

        return newCall;
    }

    public Call placeCall(String toAddress)
        throws Exception
    {
        Call newCall = createCall(toAddress);
        fireCallEvent(CallEvent.CALL_INITIATED, newCall);

        // must have one peer
        MockCallPeer callPArt =
            (MockCallPeer)newCall.getCallPeers().next();

        callPArt.setState(CallPeerState.ALERTING_REMOTE_SIDE, "no reason");
        callPArt.setState(CallPeerState.CONNECTED, "no reason");

        return newCall;
    }

    public CallPeer addNewCallPeer(Call call, String address)
    {
        MockCallPeer callPArt = new MockCallPeer(address, (MockCall)call);

        callPArt.setState(CallPeerState.ALERTING_REMOTE_SIDE, "no reason");
        callPArt.setState(CallPeerState.CONNECTED, "no reason");

        return callPArt;
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
            MockCall sourceCall = (MockCall)this.activeCalls
                .remove(evt.getSourceCall().getCallID());

            if (logger.isTraceEnabled())
                logger.trace(  "Removing call " + sourceCall + " from the list of "
                         + "active calls because it entered an ENDED state");

            fireCallEvent(CallEvent.CALL_ENDED, sourceCall);
        }
    }

    /**
     * Returns the protocol provider that this operation set belongs to.
     *
     * @return a reference to the <tt>ProtocolProviderService</tt> that created
     * this operation set.
     */
    public MockProvider getProtocolProvider()
    {
        return protocolProvider;
    }
}
