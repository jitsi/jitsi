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
 * SIP Dialog ID packet extension.
 *
 * @author Sebastien Vincent
 */
public class SIPDialogIDPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace that SIP Dialog ID belongs to.
     */
    public static final String NAMESPACE = "";

    /**
     * The name of the element that contains the SIP Dialog ID data.
     */
    public static final String ELEMENT_NAME = "sip";

    /**
     * Display text element name.
     */
    public static final String ELEMENT_DISPLAY_TEXT = "display-text";

    /**
     * Call ID element name.
     */
    public static final String ELEMENT_CALLID = "call-id";

    /**
     * From tag element name.
     */
    public static final String ELEMENT_FROMTAG = "from-tag";

    /**
     * From tag element name.
     */
    public static final String ELEMENT_TOTAG = "to-tag";

    /**
     * Display text.
     */
    private String displayText = null;

    /**
     * Call ID.
     */
    private String callID = null;

    /**
     * From tag.
     */
    private String fromTag = null;

    /**
     * To tag.
     */
    private String toTag = null;

    /**
     * Constructor
     */
    public SIPDialogIDPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
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

        if(callID != null)
            bldr.append("<").append(ELEMENT_CALLID).append(">").append(
                    callID).append("</").append(
                            ELEMENT_CALLID).append(">");

        if(fromTag != null)
            bldr.append("<").append(ELEMENT_FROMTAG).append(">").append(
                    fromTag).append("</").append(
                            ELEMENT_FROMTAG).append(">");

        if(toTag != null)
            bldr.append("<").append(ELEMENT_TOTAG).append(">").append(
                    toTag).append("</").append(
                            ELEMENT_TOTAG).append(">");

        for(PacketExtension ext : getChildExtensions())
        {
            bldr.append(ext.toXML());
        }

        bldr.append("</").append(getElementName()).append(">");

        return bldr.toString();
    }
}
