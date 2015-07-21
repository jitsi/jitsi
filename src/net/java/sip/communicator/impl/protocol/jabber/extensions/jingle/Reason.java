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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

/**
 * This enumeration contains the reason values that provide machine readable
 * information about the condition that prompted the corresponding jingle
 * action.
 *
 * @author Emil Ivov
 */
public enum Reason
{
    /**
     * A reason indicating that the party prefers to use an existing session
     * with the peer rather than initiate a new session; the Jingle session ID
     * of the alternative session SHOULD be provided as the XML character data
     * of the <sid/> child.
     */
    ALTERNATIVE_SESSION("alternative-session"),

    /**
     * A reason indicating that the party is busy and cannot accept a session.
     */
    BUSY("busy"),

    /**
     * A reason indicating that the initiator wishes to formally cancel the
     * session initiation request.
     */
    CANCEL("cancel"),

    /**
     * A reason indicating that the action is related to connectivity problems.
     */
    CONNECTIVITY_ERROR("connectivity-error"),

    /**
     * A reason indicating that the party wishes to formally decline the
     * session.
     */
    DECLINE("decline"),

    /**
     * A reason indicating that the session length has exceeded a pre-defined
     * time limit (e.g., a meeting hosted at a conference service).
     */
    EXPIRED("expired"),

    /**
     * A reason indicating that the party has been unable to initialize
     * processing related to the application type.
     */
    FAILED_APPLICATION("failed-application"),

    /**
     * A reason indicating that the party has been unable to establish
     * connectivity for the transport method.
     */
    FAILED_TRANSPORT("failed-transport"),

    /**
     * A reason indicating that the action is related to a non-specific
     * application error.
     */
    GENERAL_ERROR("general-error"),

    /**
     * A reason indicating that the entity is going offline or is no longer
     * available.
     */
    GONE("gone"),

    /**
     * A reason indicating that the party supports the offered application type
     * but does not support the offered or negotiated parameters.
     */
    INCOMPATIBLE_PARAMETERS("incompatible-parameters"),

    /**
     * A reason indicating that the action is related to media processing
     * problems.
     */
    MEDIA_ERROR("media-error"),

    /**
     * A reason indicating that the action is related to a violation of local
     * security policies.
     */
    SECURITY_ERROR("security-error"),

    /**
     * A reason indicating that the action is generated during the normal
     * course of state management and does not reflect any error.
     */
    SUCCESS("success"),

    /**
     * A reason indicating that a request has not been answered so the sender
     * is timing out the request.
     */
    TIMEOUT("timeout"),

    /**
     * A reason indicating that the party supports none of the offered
     * application types.
     */
    UNSUPPORTED_APPLICATIONS("unsupported-applications"),

    /**
     * A reason indicating that the party supports none of the offered
     * transport methods.
     */
    UNSUPPORTED_TRANSPORTS("unsupported-transports"),

    /**
     * A reason created for unsupported reasons(not defined in this enum).
     */
    UNDEFINED("undefined");

    /**
     * The name of this direction.
     */
    private final String reasonValue;

    /**
     * Creates a <tt>JingleAction</tt> instance with the specified name.
     *
     * @param reasonValue the name of the <tt>JingleAction</tt> we'd like
     * to create.
     */
    private Reason(String reasonValue)
    {
        this.reasonValue = reasonValue;
    }

    /**
     * Returns the name of this reason (e.g. "success" or "timeout"). The name
     * returned by this method is meant for use directly in the XMPP XML string.
     *
     * @return the name of this reason (e.g. "success" or "timeout").
     */
    @Override
    public String toString()
    {
        return reasonValue;
    }

    /**
     * Returns a <tt>Reason</tt> value corresponding to the specified
     * <tt>reasonValueStr</tt> or in other words {@link #SUCCESS} for
     * "success" or {@link #TIMEOUT} for "timeout").
     *
     * @param reasonValueStr the action <tt>String</tt> that we'd like to
     * parse.
     * @return a <tt>JingleAction</tt> value corresponding to the specified
     * <tt>jingleValueStr</tt>. Returns {@link #UNDEFINED} for invalid
     * <tt>jingleValueStr</tt> values.
     *
     */
    public static Reason parseString(String reasonValueStr)
    {
        for (Reason value : values())
            if (value.toString().equals(reasonValueStr))
                return value;

        return UNDEFINED;
    }
}
