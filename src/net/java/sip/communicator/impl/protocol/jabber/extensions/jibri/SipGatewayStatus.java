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

/**
 * The packet extension added to Jicofo MUC presence to broadcast current
 * Jibri SIP gateway status to all conference participants.
 *
 * Status meaning:
 * <tt>{@link JibriIq.Status#UNDEFINED}</tt> - Jibri SIP calling not available
 * <tt>{@link JibriIq.Status#AVAILABLE}</tt> - there is at least one SIP Jibri
 * currently available
 * <tt>{@link JibriIq.Status#BUSY}</tt> - all Jibri instances are busy and are
 * unable to handle any new sessions
 */
public class SipGatewayStatus
    extends AbstractPacketExtension
{
    /**
     * The namespace of this packet extension.
     */
    public static final String NAMESPACE
        = JibriIq.NAMESPACE + "/sip_availability";

    /**
     * XML element name of this packet extension.
     */
    public static final String ELEMENT_NAME = "jibri-sip-status";

    /**
     * The name of XML attribute which holds the recording status.
     */
    private static final String STATUS_ATTRIBUTE = "status";

    /**
     * Creates new {@link SipGatewayStatus}. It comes with no attributes set.
     */
    public SipGatewayStatus()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Returns the value of current Jibri SIP gateway status stored in it's
     * {@link #STATUS_ATTRIBUTE}.
     * @return see {@link SipGatewayStatus} description for status explanation.
     */
    public JibriIq.Status getStatus()
    {
        String statusAttr = getAttributeAsString(STATUS_ATTRIBUTE);

        return JibriIq.Status.parse(statusAttr);
    }

    /**
     * Sets new value for the Jibri SIP gateway status.
     * @param status see {@link SipGatewayStatus} description for status
     * explanation.
     */
    public void setStatus(JibriIq.Status status)
    {
        setAttribute(STATUS_ATTRIBUTE, String.valueOf(status));
    }
}
