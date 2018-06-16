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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jitsimeet;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * An implementation of a {@link PacketExtension} for json messages.
 *
 * @author Praveen Kumar Gupta
 */
public class JsonMessageExtension
        implements ExtensionElement
{
    public static final String NAMESPACE = "jabber:client";

    public static final String ELEMENT_NAME = "json-message";

    private String json = null;

    public JsonMessageExtension(String json){ this.json = json; }

    /**
     * Returns the content of the json-message packet.
     * @return the json string.
     */
    public String getJson(){ return json; }

    /**
     * Sets the content of the json-message packet.
     * @param json the json to set.
     */
    public void setJson(String json){ this.json = json; }

    /**
     * Returns the Element name for this extension.
     * @return element name.
     */
    public String getElementName(){ return ELEMENT_NAME; }

    /**
     * Returns the namespace for this extension.
     * @return the namespace
     */
    public String getNamespace(){ return NAMESPACE; }

    /**
     * Returns xml representation of this extension.
     * @return xml representation of this extension.
     */
    public String toXML()
    {
        final StringBuilder buf = new StringBuilder();

        buf.append("<").append(ELEMENT_NAME).append(">");
        buf.append(getJson());
        buf.append("</").append(ELEMENT_NAME).append('>');

        return buf.toString();
    }

    /**
     * The provider for this extension.
     */
    public static class Provider
            extends ExtensionElementProvider<JsonMessageExtension>
    {
        @Override
        public JsonMessageExtension parse(XmlPullParser parser, int depth)
                throws Exception
        {
            parser.next();
            final String json = parser.getText();

            // Advance to end of extension.
            while(parser.getEventType() != XmlPullParser.END_TAG)
            {
                parser.next();
            }

            return new JsonMessageExtension(json);
        }
    }
}
