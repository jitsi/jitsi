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
 * A Gibberish implementation of the <tt>Call</tt> interface.
 *
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public class CallGibberishImpl
    extends AbstractCall<CallPeerGibberishImpl, ProtocolProviderServiceGibberishImpl>
    implements CallPeerListener
{
    private static final Logger logger
        = Logger.getLogger(CallGibberishImpl.class);

    /**
     * Creates a <tt>CallGibberishImpl</tt> by specifying the
     * <tt>sourceProvider</tt>.
     * @param sourceProvider the source provider
     */
    public CallGibberishImpl(
        ProtocolProviderServiceGibberishImpl sourceProvider)
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
    public void addCallPeer(final CallPeerGibberishImpl callPeer)
    {
        if (!doAddCallPeer(callPeer))
            return;

        callPeer.addCallPeerListener(this);

        if (logger.isInfoEnabled())
            logger.info("Will fire peer added");

        fireCallPeerEvent(
            callPeer, CallPeerEvent.CALL_PEER_ADDED);

        callPeer.setState(CallPeerState.ALERTING_REMOTE_SIDE, "no reason");

        Timer timer1 = new Timer(false);

        timer1.schedule(
            new TimerTask()
            {
                @Override
                public void run()
                {
                    callPeer.setState(CallPeerState.CONNECTED, "no reason");
                }
            },
            1500);

        final Random random = new Random();
        Timer timer = new Timer(false);

        timer.scheduleAtFixedRate(
            new TimerTask()
            {
                @Override
                public void run()
                {
                    callPeer.fireStreamSoundLevelEvent(random.nextInt(255));
                }
            },
            1800,
            100);
    }

    /**
     * Removes <tt>callPeer</tt> from the list of peers in this
     * call. The method has no effect if there was no such peer in the
     * call.
     *
     * @param callPeer the <tt>CallPeer</tt> leaving the call;
     */
    public void removeCallPeer(CallPeerGibberishImpl callPeer)
    {
        if (!doRemoveCallPeer(callPeer))
            return;

        callPeer.removeCallPeerListener(this);

        fireCallPeerEvent(
            callPeer, CallPeerEvent.CALL_PEER_REMOVED);

        if(getCallPeerCount() == 0)
            setCallState(CallState.CALL_ENDED);
    }

    public void peerStateChanged(CallPeerChangeEvent evt)
    {
        CallPeerState newValue = (CallPeerState) evt.getNewValue();

        if ((newValue == CallPeerState.DISCONNECTED)
                || (newValue == CallPeerState.FAILED))
        {
            removeCallPeer(
                (CallPeerGibberishImpl) evt.getSourceCallPeer());
        }
        else if ((newValue == CallPeerState.CONNECTED)
                && getCallState().equals(CallState.CALL_INITIALIZATION))
        {
            setCallState(CallState.CALL_IN_PROGRESS);
        }
    }

    public void peerDisplayNameChanged(CallPeerChangeEvent evt)
    {}

    public void peerAddressChanged(CallPeerChangeEvent evt)
    {}

    public void peerImageChanged(CallPeerChangeEvent evt)
    {}

    public void peerTransportAddressChanged(CallPeerChangeEvent evt)
    {}

    /**
     * Gets the indicator which determines whether the local peer represented by
     * this <tt>Call</tt> is acting as a conference focus and thus should send
     * the &quot;isfocus&quot; parameter in the Contact headers of its outgoing
     * SIP signaling.
     *
     * @return <tt>true</tt> if the local peer represented by this <tt>Call</tt>
     * is acting as a conference focus; otherwise, <tt>false</tt>
     */
    @Override
    public boolean isConferenceFocus()
    {
        return true;
    }

    /**
     * Adds a specific <tt>SoundLevelListener</tt> to the list of
     * listeners interested in and notified about changes in local sound level
     * related information.
     *
     * @param l the <tt>SoundLevelListener</tt> to add
     */
    @Override
    public void addLocalUserSoundLevelListener(SoundLevelListener l)
    {
    }

    /**
     * Removes a specific <tt>SoundLevelListener</tt> of the list of
     * listeners interested in and notified about changes in local sound level
     * related information.
     *
     * @param l the <tt>SoundLevelListener</tt> to remove
     */
    @Override
    public void removeLocalUserSoundLevelListener(SoundLevelListener l)
    {
    }
}
