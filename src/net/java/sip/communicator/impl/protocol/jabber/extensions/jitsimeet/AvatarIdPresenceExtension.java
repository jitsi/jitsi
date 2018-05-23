/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2018 Atlassian Pty Ltd
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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jitsimeet;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * An extension of the presence which stores an ID of the avatar. The extension
 * looks like follows:
 *
 * <pre>{@code <avatar-id>some_unique_id</avatar-id>}</pre>
 *
 * @author Nik Vaessen
 */
public class AvatarIdPresenceExtension
    implements ExtensionElement
{

    /**
     * The namespace (xmlns attribute) of this avatar-id presence element
     */
    public static final String NAME_SPACE = "jabber:client";

    /**
     * The element name of this avatar-id presence element
     */
    public static final String ELEMENT_NAME = "avatar-id";

    /**
     * The avatar ID stored in this presence element
     */
    private String avatarId;

    /**
     * Initializes an {@link AvatarIdPresenceExtension} instance with a given
     * string value
     *
     * @param avatarId the string value representing the avatar id
     */
    public AvatarIdPresenceExtension(String avatarId)
    {
        this.avatarId = avatarId;
    }

    /**
     * Get the avatar-id value stored in this element
     *
     * @return the value of the avatar-id element as a string.
     */
    public String getAvatarId()
    {
        return avatarId;
    }


    /**
     * {@inheritDoc}
     */
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespace()
    {
        return NAME_SPACE;
    }

    /**
     * {@inheritDoc}
     */
    public String toXML()
    {
        final StringBuilder buf = new StringBuilder();

        buf.append("<").append(ELEMENT_NAME).append(">");
        buf.append(getAvatarId());
        buf.append("</").append(ELEMENT_NAME).append('>');

        return buf.toString();
    }

    /**
     * The {@link ExtensionElementProvider} which can create an instance of a
     * {@link AvatarIdPresenceExtension} when given the
     * {@link XmlPullParser} of an avatar-id element
     */
    public static class Provider
        extends ExtensionElementProvider<AvatarIdPresenceExtension>
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public AvatarIdPresenceExtension parse(XmlPullParser parser, int depth)
            throws Exception
        {
            parser.next();
            final String id = parser.getText();

            // Advance to end of extension.
            while(parser.getEventType() != XmlPullParser.END_TAG)
            {
                parser.next();
            }

            return new AvatarIdPresenceExtension(id);
        }
    }
}
