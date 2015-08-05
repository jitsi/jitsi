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
 * URI packet extension.
 *
 * @author Sebastien Vincent
 */
public class URIPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace that URI belongs to.
     */
    public static final String NAMESPACE = "";

    /**
     * The name of the element that contains the URI data.
     */
    public static final String ELEMENT_NAME = "uri";

    /**
     * Display text element name.
     */
    public static final String ELEMENT_DISPLAY_TEXT = "display-text";

    /**
     * Purpose element name.
     */
    public static final String ELEMENT_PURPOSE = "purpose";

    /**
     * Display text.
     */
    private String displayText = null;

    /**
     * Purpose.
     */
    private String purpose = null;

    /**
     * Constructor.
     *
     * @param elementName element name
     */
    public URIPacketExtension(String elementName)
    {
        super(NAMESPACE, elementName);
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
     * Get display text.
     *
     * @return display text
     */
    public String getDisplayText()
    {
        return displayText;
    }

    /**
     * Set the purpose.
     *
     * @param purpose purpose
     */
    public void setPurpose(String purpose)
    {
        this.purpose = purpose;
    }

    /**
     * Get purpose.
     *
     * @return purpose
     */
    public String getPurpose()
    {
        return purpose;
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

        if(purpose != null)
            bldr.append("<").append(ELEMENT_PURPOSE).append(">").append(
                    purpose).append("</").append(
                            ELEMENT_PURPOSE).append(">");

        for(PacketExtension ext : getChildExtensions())
        {
            bldr.append(ext.toXML());
        }

        bldr.append("</").append(getElementName()).append(">");

        return bldr.toString();
    }
}
