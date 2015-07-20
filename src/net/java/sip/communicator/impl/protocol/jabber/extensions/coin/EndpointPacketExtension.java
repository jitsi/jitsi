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
package net.java.sip.communicator.impl.protocol.jabber.extensions.coin;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

import org.jivesoftware.smack.packet.*;

/**
 * Endpoint packet extension.
 *
 * @author Sebastien Vincent
 */
public class EndpointPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace that endpoint belongs to.
     */
    public static final String NAMESPACE = null;

    /**
     * The name of the element that contains the endpoint data.
     */
    public static final String ELEMENT_NAME = "endpoint";

    /**
     * Entity attribute name.
     */
    public static final String ENTITY_ATTR_NAME = "entity";

    /**
     * Entity attribute name.
     */
    public static final String STATE_ATTR_NAME = "state";

    /**
     * Display text element name.
     */
    public static final String ELEMENT_DISPLAY_TEXT = "display-text";

    /**
     * Status element name.
     */
    public static final String ELEMENT_STATUS = "status";

    /**
     * Disconnection element name.
     */
    public static final String ELEMENT_DISCONNECTION = "disconnection-method";

    /**
     * Joining element name.
     */
    public static final String ELEMENT_JOINING = "joining-method";

    /**
     * Display text.
     */
    private String displayText = null;

    /**
     * Status.
     */
    private EndpointStatusType status = null;

    /**
     * Disconnection type.
     */
    private DisconnectionType disconnectionType = null;

    /**
     * Joining type.
     */
    private JoiningType joiningType = null;

    /**
     * Constructor.
     *
     * @param entity entity
     */
    public EndpointPacketExtension(String entity)
    {
        super(NAMESPACE, ELEMENT_NAME);
        setAttribute("entity", entity);
    }

    /**
     * Set the display text.
     *
     * @param displayText display text
     */
    public void setDisplayText(String displayText)
    {
        this.displayText = displayText;
    }

    /**
     * Set status.
     *
     * @param status status
     */
    public void setStatus(EndpointStatusType status)
    {
        this.status = status;
    }

    /**
     * Set disconnection type.
     *
     * @param disconnectionType disconnection type.
     */
    public void setDisconnectionType(DisconnectionType disconnectionType)
    {
        this.disconnectionType = disconnectionType;
    }

    /**
     * Set joining type.
     *
     * @param joiningType joining type.
     */
    public void setJoiningType(JoiningType joiningType)
    {
        this.joiningType = joiningType;
    }

    /**
     * Get display text.
     *
     * @return display text
     */
    public String getDisplayText()
    {
        return displayText;
    }

    /**
     * Get status.
     *
     * @return status.
     */
    public EndpointStatusType getStatus()
    {
        return status;
    }

    /**
     * Get disconnection type.
     *
     * @return disconnection type.
     */
    public DisconnectionType getDisconnectionType()
    {
        return disconnectionType;
    }

    /**
     * Get joining type.
     *
     * @return joining type.
     */
    public JoiningType getJoiningType()
    {
        return joiningType;
    }

    /**
     * Returns an XML representation of this extension.
     *
     * @return an XML representation of this extension.
     */
    @Override
    public String toXML()
    {
        StringBuilder bldr = new StringBuilder();

        bldr.append("<").append(getElementName()).append(" ");

        if(getNamespace() != null)
            bldr.append("xmlns='").append(getNamespace()).append("'");

        //add the rest of the attributes if any
        for(Map.Entry<String, Object> entry : attributes.entrySet())
        {
            bldr.append(" ")
                    .append(entry.getKey())
                        .append("='")
                            .append(entry.getValue())
                                .append("'");
        }

        bldr.append(">");

        if(displayText != null)
            bldr.append("<").append(ELEMENT_DISPLAY_TEXT).append(">").append(
                    displayText).append("</").append(
                            ELEMENT_DISPLAY_TEXT).append(">");
        if(status != null)
            bldr.append("<").append(ELEMENT_STATUS).append(">").append(
                    status).append("</").append(
                            ELEMENT_STATUS).append(">");

        if(disconnectionType != null)
            bldr.append("<").append(ELEMENT_DISCONNECTION).append(">").append(
                    disconnectionType).append("</").append(
                            ELEMENT_DISCONNECTION).append(">");

        if(joiningType != null)
            bldr.append("<").append(ELEMENT_JOINING).append(">").append(
                    joiningType).append("</").append(
                            ELEMENT_JOINING).append(">");

        for(PacketExtension ext : getChildExtensions())
        {
            bldr.append(ext.toXML());
        }

        bldr.append("</").append(ELEMENT_NAME).append(">");
        return bldr.toString();
    }
}
