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
 * XEP-0166 Jingle, stipulates that the value of the 'action' attribute MUST be
 * one of the values enumerated here. If an entity receives a value not defined
 * here, it MUST ignore the attribute and MUST return a <bad-request/> error to
 * the sender. There is no default value for the 'action' attribute.
 *
 * @author Emil Ivov
 */
public enum JingleAction
{
    /**
     * The <tt>content-accept</tt> action is used to accept a
     * <tt>content-add</tt> action received from another party.
     */
    CONTENT_ACCEPT("content-accept"),

    /**
     * The <tt>content-add</tt> action is used to add one or more new content
     * definitions to the session. The sender MUST specify only the added
     * content definition(s), not the added content definition(s) plus the
     * existing content definition(s). Therefore it is the responsibility of
     * the recipient to maintain a local copy of the current content
     * definition(s). If the recipient wishes to include the new content
     * definition in the session, it MUST send a <tt>content-accept</tt> action
     * to the other party; if not, it MUST send a <tt>content-reject</tt>
     * action to the other party.
     */
    CONTENT_ADD("content-add"),

    /**
     * The <tt>content-modify</tt> action is used to change the direction of an
     * existing content definition through modification of the 'senders'
     * attribute. If the recipient deems the directionality of a
     * <tt>content-modify</tt> action to be unacceptable, it MAY reply with a
     * contrary <tt>content-modify</tt> action, terminate the session, or simply
     * refuse to send or accept application data in the new direction. In any
     * case, the recipient MUST NOT send a <tt>content-accept</tt> action in
     * response to the <tt>content-modify</tt>.
     */
    CONTENT_MODIFY("content-modify"),

    /**
     * The <tt>content-reject</tt> action is used to reject a
     * <tt>content-add</tt> action received from another party.
     */
    CONTENT_REJECT("content-reject"),

    /**
     * The <tt>content-remove</tt> action is used to remove one or more content
     * definitions from the session. The sender MUST specify only the removed
     * content definition(s), not the removed content definition(s) plus the
     * remaining content definition(s). Therefore it is the responsibility of
     * the recipient to maintain a local copy of the current content
     * definition(s). Upon receiving a content-remove from the other party, the
     * recipient MUST NOT send a <tt>content-accept</tt> and MUST NOT continue
     * to negotiate the transport method or send application data related to
     * that content definition.
     * <p>
     * If the <tt>content-remove</tt> results in zero content definitions for
     * the session, the entity that receives the <tt>content-remove</tt> SHOULD
     * send a <tt>session-terminate</tt> action to the other party (since a
     * session with no content definitions is void).
     */
    CONTENT_REMOVE("content-remove"),

    /**
     * The <tt>description-info</tt> action is used to send informational hints
     * about parameters related to the application type, such as the suggested
     * height and width of a video display area or suggested configuration for
     * an audio stream.
     */
    DESCRIPTION_INFO("description-info"),

    /**
     * The <tt>security-info</tt> action is used to send information related to
     * establishment or maintenance of security preconditions.
     */
    SECURITY_INFO("security-info"),

    /**
     * The <tt>session-accept</tt> action is used to definitively accept a
     * session negotiation (implicitly this action also serves as a
     * <tt>content-accept</tt>). A <tt>session-accept</tt> action indicates a
     * willingness to proceed with the session (which might necessitate further
     * negotiation before media can be exchanged). The <tt>session-accept</tt>
     * action indicates acceptance only of the content definition(s) whose
     * disposition type is "session" (the default value of the <content/>
     * element's 'disposition' attribute), not any content definition(s) whose
     * disposition type is something other than "session" (e.g.,
     * "early-session" for early media).
     *
     * In the <tt>session-accept</tt> stanza, the <jingle/> element MUST
     * contain one or more <content/> elements, each of which MUST contain one
     * <description/> element and one <transport/> element.
     */
    SESSION_ACCEPT("session-accept"),

    /**
     * The <tt>session-info</tt> action is used to send information related to
     * establishment or maintenance of security preconditions.
     */
    SESSION_INFO("session-info"),

    /**
     * The <tt>session-initiate</tt> action is used to request negotiation of a
     * new Jingle session. When sending a <tt>session-initiate</tt> with one
     * <content/> element, the value of the <content/> element's 'disposition'
     * attribute MUST be "session" (if there are multiple <content/> elements
     * then at least one MUST have a disposition of "session"); if this rule is
     * violated, the responder MUST return a <bad-request/> error to the
     * initiator.
     */
    SESSION_INITIATE("session-initiate"),

    /**
     * The <tt>session-terminate</tt> action is used to end an existing session.
     */
    SESSION_TERMINATE("session-terminate"),

    /**
     * The <tt>transport-accept</tt> action is used to accept a
     * <tt>transport-replace</tt> action received from another party.
     */
    TRANSPORT_ACCEPT("transport-accept"),

    /**
     * The <tt>transport-info</tt> action is used to exchange transport
     * candidates; it is mainly used in Jingle ICE-UDP but might be used in
     * other transport specifications.
     */
    TRANSPORT_INFO("transport-info"),

    /**
     * The <tt>transport-reject</tt> action is used to reject a
     * <tt>transport-replace</tt> action received from another party.
     */
    TRANSPORT_REJECT("transport-reject"),

    /**
     * The <tt>transport-replace</tt> action is used to redefine a transport
     * method, typically for fallback to a different method (e.g., changing
     * from ICE-UDP to Raw UDP for a datagram transport, or changing from
     * SOCKS5 Bytestreams to In-Band Bytestreams [27] for a streaming
     * transport). If the recipient wishes to use the new transport definition,
     * it MUST send a transport-accept action to the other party; if not, it
     * MUST send a transport-reject action to the other party.
     */
    TRANSPORT_REPLACE("transport-replace"),

    /**
     * The "addsource" action used in Jitsi-Meet.
     */
    ADDSOURCE("addsource"),

    /**
     * The "removesource" action used in Jitsi-Meet.
     */
    REMOVESOURCE("removesource"),

    /**
     * The "source-add" action used in Jitsi-Meet.
     */
    SOURCEADD("source-add"),

    /**
     * The "source-remove" action used in Jitsi-Meet.
     */
    SOURCEREMOVE("source-remove");

    /**
     * The name of this direction.
     */
    private final String actionName;

    /**
     * Creates a <tt>JingleAction</tt> instance with the specified name.
     *
     * @param actionName the name of the <tt>JingleAction</tt> we'd like
     * to create.
     */
    private JingleAction(String actionName)
    {
        this.actionName = actionName;
    }

    /**
     * Returns the name of this <tt>JingleAction</tt> (e.g. "session-initiate"
     * or "transport-accept"). The name returned by this method is meant for
     * use directly in the XMPP XML string.
     *
     * @return Returns the name of this <tt>JingleAction</tt> (e.g.
     * "session-initiate" or "transport-accept").
     */
    @Override
    public String toString()
    {
        return actionName;
    }

    /**
     * Returns a <tt>JingleAction</tt> value corresponding to the specified
     * <tt>jingleActionStr</tt> or in other words {@link #SESSION_INITIATE} for
     * "session-initiate" or {@link #TRANSPORT_ACCEPT} for "transport-accept").
     *
     * @param jingleActionStr the action <tt>String</tt> that we'd like to
     * parse.
     * @return a <tt>JingleAction</tt> value corresponding to the specified
     * <tt>jingleActionStr</tt> or <tt>null</tt> if given <tt>String</tt> can
     * not be matched with any of enumeration values.
     */
    public static JingleAction parseString(String jingleActionStr)
    {
        for (JingleAction value : values())
            if (value.toString().equals(jingleActionStr))
                return value;

        return null;
    }
}
