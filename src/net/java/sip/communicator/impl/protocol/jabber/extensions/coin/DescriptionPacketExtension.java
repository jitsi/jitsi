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
 * Description packet extension.
 *
 * @author Sebastien Vincent
 */
public class DescriptionPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace that description belongs to.
     */
    public static final String NAMESPACE = null;

    /**
     * The name of the element that contains the description data.
     */
    public static final String ELEMENT_NAME = "conference-description";

    /**
     * Subject element name.
     */
    public static final String ELEMENT_SUBJECT = "subject";

    /**
     * Display text element name.
     */
    public static final String ELEMENT_DISPLAY_TEXT = "display-text";

    /**
     * Free text element name.
     */
    public static final String ELEMENT_FREE_TEXT = "free-text";

    /**
     * Max user count element name.
     */
    public static final String ELEMENT_MAX_USER_COUNT =
        "maximum-user-count";

    /**
     * The subject.
     */
    private String subject = "";

    /**
     * Display text.
     */
    private String displayText = null;

    /**
     * Free text.
     */
    private String freeText = null;

    /**
     * Maximum user count.
     */
    private int maximumUserCount = 0;

    /**
     * Constructor.
     */
    public DescriptionPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Set subject.
     *
     * @param subject subject
     */
    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    /**
     * Set display text.
     *
     * @param displayText display text
     */
    public void setDisplayText(String displayText)
    {
        this.displayText = displayText;
    }

    /**
     * Set free text.
     *
     * @param freeText free text
     */
    public void setFreeText(String freeText)
    {
        this.freeText = freeText;
    }

    /**
     * Get subject.
     *
     * @return subject
     */
    public String getSubject()
    {
        return subject;
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
     * Get free text.
     *
     * @return free text
     */
    public String getFreeText()
    {
        return freeText;
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

        if(subject != null)
            bldr.append("<").append(ELEMENT_SUBJECT).append(">").append(
                    subject).append("</").append(ELEMENT_SUBJECT).append(">");

        if(displayText != null)
            bldr.append("<").append(ELEMENT_DISPLAY_TEXT).append(">").append(
                    displayText).append("</").append(
                            ELEMENT_DISPLAY_TEXT).append(">");

        if(freeText != null)
            bldr.append("<").append(ELEMENT_FREE_TEXT).append(">").append(
                    freeText).append("</").append(
                            ELEMENT_FREE_TEXT).append(">");

        if(maximumUserCount != 0)
            bldr.append("<").append(ELEMENT_MAX_USER_COUNT).append(">").append(
                    maximumUserCount).append("</").append(
                            ELEMENT_MAX_USER_COUNT).append(">");

        for(PacketExtension ext : getChildExtensions())
        {
            bldr.append(ext.toXML());
        }

        bldr.append("</").append(getElementName()).append(">");
        return bldr.toString();
    }
}
