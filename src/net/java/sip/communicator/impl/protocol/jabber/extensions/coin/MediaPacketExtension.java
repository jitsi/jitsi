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
 * Media packet extension.
 *
 * @author Sebastien Vincent
 */
public class MediaPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace that media belongs to.
     */
    public static final String NAMESPACE = null;

    /**
     * The name of the element that contains the media data.
     */
    public static final String ELEMENT_NAME = "media";

    /**
     * Display text element name.
     */
    public static final String ELEMENT_DISPLAY_TEXT = "display-text";

    /**
     * Source ID element name.
     */
    public static final String ELEMENT_SRC_ID = "src-id";

    /**
     * Label element name.
     */
    public static final String ELEMENT_LABEL = "label";

    /**
     * Type element name.
     */
    public static final String ELEMENT_TYPE = "type";

    /**
     * Status element name.
     */
    public static final String ELEMENT_STATUS = "status";

    /**
     * ID attribute name.
     */
    public static final String ID_ATTR_NAME = "id";

    /**
     * Source ID.
     */
    private String srcId = null;

    /**
     * Type.
     */
    private String type = null;

    /**
     * Label.
     */
    private String label = null;

    /**
     * Display text.
     */
    private String displayText = null;

    /**
     * Media status.
     */
    private String status = null;

    /**
     * Constructor.
     *
     * @param id media ID
     */
    public MediaPacketExtension(String id)
    {
        super(NAMESPACE, ELEMENT_NAME);

        setAttribute(ID_ATTR_NAME, id);
    }

    /**
     * Set label.
     *
     * @param label label
     */
    public void setLabel(String label)
    {
        this.label = label;
    }

    /**
     * Set status.
     *
     * @param status status.
     */
    public void setStatus(String status)
    {
        this.status = status;
    }

    /**
     * Set src-id.
     *
     * @param srcId src-id
     */
    public void setSrcID(String srcId)
    {
        this.srcId = srcId;
    }

    /**
     * Set type.
     *
     * @param type type
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * Set display text.
     * @param displayText display text
     */
    public void setDisplayText(String displayText)
    {
        this.displayText = displayText;
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
     * Get type.
     *
     * @return type
     */
    public String getType()
    {
        return type;
    }

    /**
     * Get label.
     *
     * @return label
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * Get status.
     *
     * @return status.
     */
    public String getStatus()
    {
        return status;
    }

    /**
     * Get src-id.
     *
     * @return src-id
     */
    public String getSrcID()
    {
        return srcId;
    }

    /**
     * Get an XML string representation.
     *
     * @return XML string representation
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

        if(type != null)
            bldr.append("<").append(ELEMENT_TYPE).append(">").append(
                    type).append("</").append(
                            ELEMENT_TYPE).append(">");

        if(srcId != null)
            bldr.append("<").append(ELEMENT_SRC_ID).append(">").append(
                    srcId).append("</").append(
                            ELEMENT_SRC_ID).append(">");

        if(status != null)
            bldr.append("<").append(ELEMENT_STATUS).append(">").append(
                    status).append("</").append(
                            ELEMENT_STATUS).append(">");

        if(label != null)
            bldr.append("<").append(ELEMENT_LABEL).append(">").append(
                    label).append("</").append(
                            ELEMENT_LABEL).append(">");

        for(PacketExtension ext : getChildExtensions())
        {
            bldr.append(ext.toXML());
        }

        bldr.append("</").append(getElementName()).append(">");
        return bldr.toString();
    }
}
