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
 * Host Information packet extension.
 *
 * @author Sebastien Vincent
 */
public class HostInfoPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace that media belongs to.
     */
    public static final String NAMESPACE = null;

    /**
     * The name of the element that contains the media data.
     */
    public static final String ELEMENT_NAME = "host-info";

    /**
     * Display text element name.
     */
    public static final String ELEMENT_DISPLAY_TEXT = "display-text";

    /**
     * Web page element name.
     */
    public static final String ELEMENT_WEB_PAGE = "web-page";

    /**
     * Display text.
     */
    private String displayText = null;

    /**
     * Web page.
     */
    private String webPage = null;

    /**
     * Constructor.
     */
    public HostInfoPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
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
     * Set web page.
     * @param webPage web page
     */
    public void setWebPage(String webPage)
    {
        this.webPage = webPage;
    }

    /**
     * Get web page.
     *
     * @return web page
     */
    public String getWebPage()
    {
        return webPage;
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

        if(webPage != null)
            bldr.append("<").append(ELEMENT_WEB_PAGE).append(">").append(
                    webPage).append("</").append(
                            ELEMENT_WEB_PAGE).append(">");

        for(PacketExtension ext : getChildExtensions())
        {
            bldr.append(ext.toXML());
        }

        bldr.append("</").append(ELEMENT_NAME).append(">");

        return bldr.toString();
    }
}
