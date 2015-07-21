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
 * Execution packet extension.
 *
 * @author Sebastien Vincent
 */
public class ExecutionPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace that media belongs to.
     */
    public static final String NAMESPACE = null;

    /**
     * The name of the element that contains the media data.
     */
    public static final String ELEMENT_REFERRED_NAME = "referred";

    /**
     * The name of the element that contains the media data.
     */
    public static final String ELEMENT_DISCONNECTION_NAME =
        "disconnection-info";

    /**
     * The name of the element that contains the media data.
     */
    public static final String ELEMENT_JOINING_NAME = "joining-info";

    /**
     * The name of the element that contains the media data.
     */
    public static final String ELEMENT_MODIFIED_NAME = "modified";

    /**
     * "By" element name.
     */
    public static final String ELEMENT_BY = "by";

    /**
     * "Reason" element name.
     */
    public static final String ELEMENT_REASON = "reason";

    /**
     * "When" element name.
     */
    public static final String ELEMENT_WHEN = "display-text";

    /**
     * Date of the execution.
     */
    private String when = null;

    /**
     * By.
     */
    private String by = null;

    /**
     * Reason.
     */
    private String reason = null;

    /**
     * Set "by" field.
     *
     * @param by string to set
     */
    public void setBy(String by)
    {
        this.by = by;
    }

    /**
     * Get "by" field.
     *
     * @return "by" field
     */
    public String getBy()
    {
        return by;
    }

    /**
     * Set "when" field.
     *
     * @param when string to set
     */
    public void setWhen(String when)
    {
        this.when = when;
    }

    /**
     * Get "when" field.
     *
     * @return "when" field
     */
    public String getWhen()
    {
        return when;
    }

    /**
     * Set "reason" field.
     *
     * @param reason string to set
     */
    public void setReason(String reason)
    {
        this.reason = reason;
    }

    /**
     * Get "reason" field.
     *
     * @return "reason" field
     */
    public String getReason()
    {
        return reason;
    }

    /**
     * Constructor.
     *
     * @param elementName name of the element
     */
    public ExecutionPacketExtension(String elementName)
    {
        super(NAMESPACE, elementName);
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

        if(by != null)
            bldr.append("<").append(ELEMENT_BY).append(">").append(
                    by).append("</").append(
                            ELEMENT_BY).append(">");

        if(when != null)
            bldr.append("<").append(ELEMENT_WHEN).append(">").append(
                    when).append("</").append(
                            ELEMENT_WHEN).append(">");

        if(reason != null)
            bldr.append("<").append(ELEMENT_REASON).append(">").append(
                    reason).append("</").append(
                            ELEMENT_REASON).append(">");


        for(PacketExtension ext : getChildExtensions())
        {
            bldr.append(ext.toXML());
        }

        bldr.append("</").append(getElementName()).append(">");
        return bldr.toString();
    }
}
