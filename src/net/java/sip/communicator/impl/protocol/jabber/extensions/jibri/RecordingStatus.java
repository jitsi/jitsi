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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jibri;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

import org.jivesoftware.smack.packet.*;

import java.util.*;

/**
 * The packet extension added to Jicofo MUC presence to broadcast current
 * recording status to all conference participants.
 *
 * Status meaning:
 * <tt>{@link JibriIq.Status#UNDEFINED}</tt> - recording not available
 * <tt>{@link JibriIq.Status#OFF}</tt> - recording stopped(available to start)
 * <tt>{@link JibriIq.Status#PENDING}</tt> - starting recording
 * <tt>{@link JibriIq.Status#ON}</tt> - recording in progress
 */
public class RecordingStatus
    extends AbstractPacketExtension
{
    /**
     * The namespace of this packet extension.
     */
    public static final String NAMESPACE = JibriIq.NAMESPACE;

    /**
     * XML element name of this packet extension.
     */
    public static final String ELEMENT_NAME = "jibri-recording-status";

    /**
     * The name of XML attribute which holds the recording status.
     */
    private static final String STATUS_ATTRIBUTE = "status";

    public RecordingStatus()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Returns the value of current recording status stored in it's attribute.
     * @return one of {@link JibriIq.Status}
     */
    public JibriIq.Status getStatus()
    {
        String statusAttr = getAttributeAsString(STATUS_ATTRIBUTE);

        return JibriIq.Status.parse(statusAttr);
    }

    /**
     * Sets new value for the recording status.
     * @param status one of {@link JibriIq.Status}
     */
    public void setStatus(JibriIq.Status status)
    {
        setAttribute(STATUS_ATTRIBUTE, String.valueOf(status));
    }

    /**
     * Returns <tt>XMPPError</tt> associated with current
     * {@link RecordingStatus}.
     */
    public XMPPError getError()
    {
        XMPPErrorPE errorPe = getErrorPE();
        return errorPe != null ? errorPe.getError() : null;
    }

    /**
     * Gets <tt>{@link XMPPErrorPE}</tt> from the list of child packet
     * extensions.
     * @return {@link XMPPErrorPE} or <tt>null</tt> if not found.
     */
    private XMPPErrorPE getErrorPE()
    {
        List<? extends ExtensionElement> errorPe
            = getChildExtensionsOfType(XMPPErrorPE.class);

        return (XMPPErrorPE) (!errorPe.isEmpty() ? errorPe.get(0) : null);
    }

    /**
     * Sets <tt>XMPPError</tt> on this <tt>RecordingStatus</tt>.
     * @param error <tt>XMPPError</tt> to add error details to this
     * <tt>RecordingStatus</tt> instance or <tt>null</tt> to have it removed.
     */
    public void setError(XMPPError error)
    {
        if (error != null)
        {
            // Wrap and add XMPPError as packet extension
            XMPPErrorPE errorPe = getErrorPE();
            if (errorPe == null)
            {
                errorPe = new XMPPErrorPE(error);
                addChildExtension(errorPe);
            }
            errorPe.setError(error);
        }
        else
        {
            // Remove error PE
            getChildExtensions().remove(getErrorPE());
        }
    }
}
