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

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * An extension of the message stanza for sending json messages.
 * The extension looks like follows:
 *
 *  <pre>{@code <json-message>json_content_here</json-message>}</pre>
 *
 * @author Praveen Kumar Gupta
 */
public class JsonMessageExtension
        extends AbstractPacketExtension
{
    /**
     * The namespace (xmlns attribute) of this json-message packet.
     */
    public static final String NAMESPACE = "http://jitsi.org/jitmeet";

    /**
     * The element name of this json-message packet.
     */
    public static final String ELEMENT_NAME = "json-message";

    /**
     * Initializes an {@link JsonMessageExtension} instance.
     *
     */
    public JsonMessageExtension(){ super(NAMESPACE, ELEMENT_NAME); }

    /**
     * Initializes an {@link JsonMessageExtension} instance with a given
     * string value for its json content.
     *
     * @param json the string value of the json content.
     */
    public JsonMessageExtension(String json)
    {
        super(NAMESPACE, ELEMENT_NAME);

        setText(json);
    }

    /**
     * Returns the content of the json-message packet.
     * @return the json string.
     */
    public String getJson(){ return getText(); }
}
