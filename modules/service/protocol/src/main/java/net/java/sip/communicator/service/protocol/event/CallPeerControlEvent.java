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

import net.java.sip.communicator.service.protocol.*;

/**
 * The CallPeerControlEvent is issued by the PhoneUIService as a result
 * of a user request to modify the way a CallPeer is associated with a
 * call, or in other words "Answer" the incoming call of a CallPeer or
 * "Hangup" and thus and the participation of a CallPeer in a call. The
 * source of the event is considered to be the CallPeer that is being
 * controlled. As the event might also be used to indicate a user request to
 * transfer a given call peer to a different number, the class also contains
 * a targetURI field, containing the address that a client is being redirected to
 * (the target uri might also have slightly different meanings depending on the
 * method dispatching the event).
 *
 * @author Emil Ivov
 */
public class CallPeerControlEvent
    extends java.util.EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final String targetURI;

    /**
     * Creates a new event instance with the specified source CallPeer
     * and targetURI, if any.
     * @param source the CallPeer that this event is pertaining to.
     * @param targetURI the URI to transfer to if this is a "Transfer" event
     * or null otherwise.
     */
    public CallPeerControlEvent(CallPeer source, String targetURI)
    {
        super(source);
        this.targetURI = targetURI;
    }

    /**
     * Returns the CallPeer that this event is pertaining to.
     * @return the CallPeer that this event is pertaining to.
     */
    public CallPeer getAssociatedCallPeer()
    {
        return (CallPeer) source;
    }

    /**
     * Returns the target URI if this is event is triggered by a transfer
     * request or null if not.
     * @return null or a tranfer URI.
     */
    public String getTargetURI()
    {
        return targetURI;
    }
}
