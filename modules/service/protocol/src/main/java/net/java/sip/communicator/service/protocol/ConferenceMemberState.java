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
package net.java.sip.communicator.service.protocol;

/**
 * Represents the state of the device and signaling session of
 * <code>ConferenceMember</code> in the conference it is participating in.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public enum ConferenceMemberState
{
    /**
     * A Public Switched Telephone Network (PSTN) ALERTING or SIP 180 Ringing
     * was returned for the outbound call; endpoint is being alerted.
     */
    ALERTING("Alerting"),

    /**
     * The endpoint is a participant in the conference. Depending on the media
     * policies, he/she can send and receive media to and from other
     * participants.
     */
    CONNECTED("Connected"),

    /**
     * Endpoint is dialing into the conference, not yet in the roster (probably
     * being authenticated).
     */
    DIALING_IN("Dialing in"),

    /**
     * Focus has dialed out to connect the endpoint to the conference, but the
     * endpoint is not yet in the roster (probably being authenticated).
     */
    DIALING_OUT("Dialing out"),

    /**
     * The endpoint is not a participant in the conference, and no active dialog
     * exists between the endpoint and the focus.
     */
    DISCONNECTED("Disconnected"),

    /**
     * Focus is in the process of disconnecting the endpoint (e.g., in SIP a
     * DISCONNECT or BYE was sent to the endpoint).
     */
    DISCONNECTING("Disconnecting"),

    /**
     * Active signaling dialog exists between an endpoint and a focus and the
     * endpoint can "listen" to the conference, but the endpoint's media is not
     * being mixed into the conference.
     */
    MUTED_VIA_FOCUS("Muted via focus"),

    /**
     * Active signaling dialog exists between an endpoint and a focus, but
     * endpoint is "on-hold" for this conference, i.e., he/she is neither
     * "hearing" the conference mix nor is his/her media being mixed in the
     * conference.
     */
    ON_HOLD("On hold"),

    /**
     * Endpoint is not yet in the session, but it is anticipated that he/she
     * will join in the near future.
     */
    PENDING("Pending"),

    /**
     * The state of the device and signaling session of the associated
     * <code>ConferenceMember</code> in the conference is unknown.
     */
    UNKNOWN("Unknown");

    /**
     * The name of this state.
     */
    private final String stateName;

    /**
     * Creates an instance of <tt>ConferenceMemberState</tt> by specifying the
     * name of the state.
     * @param name the name of the state
     */
    private ConferenceMemberState(String name)
    {
        this.stateName = name;
    }

    /**
     * Returns the name of this <tt>ConferenceMemberState</tt> (e.g. "connected"
     * or "dialing out"). The name returned by this method is meant to be used
     * by user interface implementations in order to provide more readable
     * state string.
     *
     * @return the name of this <tt>ConferenceMemberState</tt> (e.g. "connected"
     * or "dialing out")
     */
    @Override
    public String toString()
    {
        return stateName;
    }
}
