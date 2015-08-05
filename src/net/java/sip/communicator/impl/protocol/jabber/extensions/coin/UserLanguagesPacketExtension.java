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
 * User languages packet extension.
 *
 * @author Sebastien Vincent
 */
public class UserLanguagesPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace that user languages belongs to.
     */
    public static final String NAMESPACE = "";

    /**
     * The name of the element that contains the user languages data.
     */
    public static final String ELEMENT_NAME = "languages";

    /**
     * The name of the element that contains the media data.
     */
    public static final String ELEMENT_LANGUAGES = "stringvalues";

    /**
     * The list of languages separated by space.
     */
    private String languages = null;

    /**
     * Constructor.
     */
    public UserLanguagesPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Set languages.
     *
     * @param languages list of languages
     */
    public void setLanguages(String languages)
    {
        this.languages = languages;
    }

    /**
     * Get languages.
     *
     * @return languages
     */
    public String getLanguages()
    {
        return languages;
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

        if(languages != null)
        {
            bldr.append("<").append(ELEMENT_LANGUAGES).append(">").append(
                    languages).append("</").append(
                            ELEMENT_LANGUAGES).append(">");
        }

        for(PacketExtension ext : getChildExtensions())
        {
            bldr.append(ext.toXML());
        }

        bldr.append("</").append(getElementName()).append(">");

        return bldr.toString();
    }
}
