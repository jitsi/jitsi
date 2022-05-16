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

import java.util.*;

/**
 * A call change listener receives events indicating that a call has changed and
 * a peer has either left or joined.
 *
 * @author Emil Ivov
 */
public interface CallChangeListener
    extends EventListener
{
    /**
     * Indicates that a new call peer has joined the source call.
     *
     * @param evt the <tt>CallPeerEvent</tt> containing the source call
     * and call peer.
     */
    public void callPeerAdded(CallPeerEvent evt);

    /**
     * Indicates that a call peer has left the source call.
     *
     * @param evt the <tt>CallPeerEvent</tt> containing the source call
     * and call peer.
     */
    public void callPeerRemoved(CallPeerEvent evt);

    /**
     * Indicates that a change has occurred in the state of the source call.
     *
     * @param evt the <tt>CallChangeEvent</tt> instance containing the source
     * calls and its old and new state.
     */
    public void callStateChanged(CallChangeEvent evt);
}
