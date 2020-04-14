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
package net.java.sip.communicator.service.protocol.event;

/**
 * An abstract implementation of <tt>CallPeerConferenceListener</tt> which
 * exists only as a convenience to extenders. Additionally, provides a means to
 * receive the <tt>CallPeerConferenceEvent</tt>s passed to the various
 * <tt>CallPeerConferenceListener</tt> methods into a single method because
 * their specifics can be determined based on their <tt>eventID</tt>.
 *
 * @author Lyubomir Marinov
 */
public class CallPeerConferenceAdapter
    implements CallPeerConferenceListener
{
    /**
     * {@inheritDoc}
     *
     * Calls {@link #onCallPeerConferenceEvent(CallPeerConferenceEvent)}.
     */
    public void conferenceFocusChanged(CallPeerConferenceEvent ev)
    {
        onCallPeerConferenceEvent(ev);
    }

    /**
     * {@inheritDoc}
     *
     * Calls {@link #onCallPeerConferenceEvent(CallPeerConferenceEvent)}.
     */
    public void conferenceMemberAdded(CallPeerConferenceEvent ev)
    {
        onCallPeerConferenceEvent(ev);
    }

    /**
     * {@inheritDoc}
     *
     * Dummy implementation of 
     * {@link #conferenceMemberErrorReceived(CallPeerConferenceEvent)}.
     */
    public void conferenceMemberErrorReceived(CallPeerConferenceEvent ev) {}
    
    /**
     * {@inheritDoc}
     *
     * Calls {@link #onCallPeerConferenceEvent(CallPeerConferenceEvent)}.
     */
    public void conferenceMemberRemoved(CallPeerConferenceEvent ev)
    {
        onCallPeerConferenceEvent(ev);
    }

    /**
     * Notifies this listener about a specific <tt>CallPeerConferenceEvent</tt>
     * provided to one of the <tt>CallPeerConferenceListener</tt> methods. The
     * <tt>CallPeerConferenceListener</tt> method which was originally invoked
     * on this listener can be determined based on the <tt>eventID</tt> of the
     * specified <tt>CallPeerConferenceEvent</tt>. The implementation of
     * <tt>CallPeerConferenceAdapter</tt> does nothing.
     *
     * @param ev the <tt>CallPeerConferenceEvent</tt> this listener is being
     * notified about
     */
    protected void onCallPeerConferenceEvent(CallPeerConferenceEvent ev)
    {
    }
}
