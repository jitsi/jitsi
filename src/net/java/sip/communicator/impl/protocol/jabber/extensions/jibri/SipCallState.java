/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ Atlassian Pty Ltd
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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jibri;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

import org.jivesoftware.smack.packet.*;

import java.util.*;

/**
 * Jicofo adds one <tt>SipCallState</tt> packet extension for each Jibri SIP
 * session to it's MUC presence in Jitsi Meet conference.
 *
 * Status meaning:
 * <tt>{@link JibriIq.Status#PENDING}</tt> - (initial) SIP call is being started
 * <tt>{@link JibriIq.Status#ON}</tt> - SIP call in progress
 * <tt>{@link JibriIq.Status#OFF}</tt> - SIP call has been stopped.  If it was
 * not a graceful transition to OFF, a FailureReason will also be given
 *
 * @author Pawel Domas
 */
public class SipCallState
    extends AbstractPacketExtension
{
    /**
     * The namespace of this packet extension.
     */
    public static final String NAMESPACE = JibriIq.NAMESPACE + "/call_state";

    /**
     * XML element name of this packet extension.
     */
    public static final String ELEMENT_NAME = "jibri-sip-call-state";

    /**
     * The name of XML attribute which holds the SIP session state.
     */
    private static final String STATE_ATTRIBUTE = "state";

    /**
     * The name of XML attribute which hold the SIP address of remote peer.
     */
    private static final String SIPADDRESS_ATTRIBUTE = "sipaddress";

    public SipCallState()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * @return value of {@link #SIPADDRESS_ATTRIBUTE}.
     */
    public String getSipAddress()
    {
        return getAttributeAsString(SIPADDRESS_ATTRIBUTE);
    }

    /**
     * Sets new value for {@link #SIPADDRESS_ATTRIBUTE}
     * @param sipAddress a SIP address
     */
    public void setSipAddress(String sipAddress)
    {
        setAttribute(SIPADDRESS_ATTRIBUTE, sipAddress);
    }

    /**
     * Returns the value of current SIP call status stored in it's attribute.
     * Check {@link SipCallState} description for status description.
     * @return one of {@link JibriIq.Status}
     */
    public JibriIq.Status getStatus()
    {
        String statusAttr = getAttributeAsString(STATE_ATTRIBUTE);

        return JibriIq.Status.parse(statusAttr);
    }

    /**
     * Sets new value for the recording status.
     * Check {@link SipCallState} description for status description.
     * @param status one of {@link JibriIq.Status}
     */
    public void setState(JibriIq.Status status)
    {
        setAttribute(STATE_ATTRIBUTE, String.valueOf(status));
    }

    /**
     * Returns the session ID stored in this element
     * @return the session ID
     */
    public String getSessionId()
    {
        return getAttributeAsString(JibriIq.SESSION_ID_ATTR_NAME);
    }

    /**
     * Set the session ID for this recording status element
     * @param sessionId the session ID
     */
    public void setSessionId(String sessionId)
    {
        setAttribute(JibriIq.SESSION_ID_ATTR_NAME, sessionId);
    }

    /**
     * Get the failure reason in this status, or UNDEFINED if there isn't one
     * @return the failure reason
     */
    public JibriIq.FailureReason getFailureReason()
    {
        String failureReasonStr = getAttributeAsString(JibriIq.FAILURE_REASON_ATTR_NAME);
        return JibriIq.FailureReason.parse(failureReasonStr);
    }

    /**
     * Set the failure reason in this status
     * @param failureReason the failure reason
     */
    public void setFailureReason(JibriIq.FailureReason failureReason)
    {
        if (failureReason != null)
        {
            setAttribute(JibriIq.FAILURE_REASON_ATTR_NAME, failureReason.toString());
        }
    }
}
