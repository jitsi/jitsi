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
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

/**
 * A Gibberish implementation of a basic telephony operation set.
 *
 * @author Yana Stamcheva
 */
public class OperationSetBasicTelephonyGibberishImpl
    extends AbstractOperationSetBasicTelephony<ProtocolProviderServiceGibberishImpl>
    implements CallChangeListener
{
    private static final Logger logger
        = Logger.getLogger(OperationSetBasicTelephonyGibberishImpl.class);

    /**
     * A reference to the <tt>ProtocolProviderServiceSipImpl</tt> instance
     * that created us.
     */
    private ProtocolProviderServiceGibberishImpl protocolProvider = null;

    /**
     * A table mapping call ids against call instances.
     */
    private Hashtable<String, Call> activeCalls = new Hashtable<String, Call>();

    /**
     * Creates an instance of <tt>OperationSetBasicTelephonyGibberishImpl</tt>
     * by specifying the corresponding <tt>protocolProvider</tt>
     * @param protocolProvider the protocol provider, where this operation set
     * is registered
     */
    public OperationSetBasicTelephonyGibberishImpl(
        ProtocolProviderServiceGibberishImpl protocolProvider)
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
        CallPeerGibberishImpl callPeer
            = (CallPeerGibberishImpl) peer;
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
        throws OperationFailedException
    {
        CallGibberishImpl call = new CallGibberishImpl(protocolProvider);

        call.addCallChangeListener(this);
        activeCalls.put(call.getCallID(), call);

        CallPeerGibberishImpl callPeer
            = new CallPeerGibberishImpl(callee, call);

        call.addCallPeer(callPeer);

        fireCallEvent(CallEvent.CALL_INITIATED, call);

        return call;
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

        CallPeerGibberishImpl callPeer
            = (CallPeerGibberishImpl) peer;

        if (logger.isInfoEnabled())
            logger.info("hangupCallPeer");
        callPeer.setState(CallPeerState.DISCONNECTED, null);

        CallGibberishImpl call = callPeer.getCall();

        call.removeCallPeer(callPeer);
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
     * @throws OperationFailedException if we encounter an error while
     * performing this operation
     */
    public void putOffHold(CallPeer peer)
        throws OperationFailedException
    {
        this.putOnHold(peer, false);
    }

    /**
     * Puts the specified CallPeer "on hold".
     *
     * @param peer the peer that we'd like to put on hold.
     * @throws OperationFailedException with the corresponding code if we
     * encounter an error while performing this operation.
     */
    public void putOnHold(CallPeer peer) throws
        OperationFailedException
    {
        this.putOnHold(peer, true);
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

            if (logger.isTraceEnabled())
                logger.trace(  "Removing call " + sourceCall + " from the list of "
                        + "active calls because it entered an ENDED state");

            fireCallEvent(CallEvent.CALL_ENDED, sourceCall);
        }
    }

    /**
     * Sets the mute state of the audio stream being sent to a specific
     * <tt>CallPeer</tt>.
     * <p>
     * The implementation sends silence through the audio stream.
     * </p>
     *
     * @param peer the <tt>CallPeer</tt> who receives the audio
     *            stream to have its mute state set
     * @param mute <tt>true</tt> to mute the audio stream being sent to
     *            <tt>peer</tt>; otherwise, <tt>false</tt>
     */
    public void setMute(CallPeer peer, boolean mute)
    {
        CallPeerGibberishImpl gibberishPeer = (CallPeerGibberishImpl) peer;

        gibberishPeer.setMute(mute);
    }

    /**
     * Puts the specified <tt>CallPeer</tt> on or off hold.
     *
     * @param peer the <tt>CallPeer</tt> to be put on or off hold
     * @param on <tt>true</tt> to have the specified <tt>CallPeer</tt>
     *            put on hold; <tt>false</tt>, otherwise
     * @throws OperationFailedException
     */
    private void putOnHold(CallPeer peer, boolean on)
        throws OperationFailedException
    {
        CallPeerGibberishImpl gibberishPeer = (CallPeerGibberishImpl) peer;

        CallPeerState state = gibberishPeer.getState();
        if (CallPeerState.ON_HOLD_LOCALLY.equals(state))
        {
            if (!on)
                gibberishPeer.setState(CallPeerState.CONNECTED);
        }
        else if (CallPeerState.ON_HOLD_MUTUALLY.equals(state))
        {
            if (!on)
                gibberishPeer.setState(CallPeerState.ON_HOLD_REMOTELY);
        }
        else if (CallPeerState.ON_HOLD_REMOTELY.equals(state))
        {
            if (on)
                gibberishPeer.setState(CallPeerState.ON_HOLD_MUTUALLY);
        }
        else if (on)
        {
            gibberishPeer.setState(CallPeerState.ON_HOLD_LOCALLY);
        }
    }

    /**
     * Returns the protocol provider that this operation set belongs to.
     *
     * @return a reference to the <tt>ProtocolProviderService</tt> that created
     * this operation set.
     */
    public ProtocolProviderServiceGibberishImpl getProtocolProvider()
    {
        return protocolProvider;
    }
}
