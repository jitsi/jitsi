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
package net.java.sip.communicator.impl.protocol.jabber.extensions.geolocation;

import org.jivesoftware.smack.packet.*;

/**
 * This class extends the smack Message class and allows creating a
 * GeolocationMessage automatically setting the geolocation packet extension.
 *
 * @author Guillaume Schreiner
 */
public class GeolocationMessage
    extends Message
{
    /**
     * Creates a new, "normal" message.
     *
     * @param geoloc the geolocation packet extension to add to this message.
     */
    public GeolocationMessage(GeolocationPacketExtension geoloc)
    {
        super();
        this.addExtension(geoloc);
    }

    /**
     * Creates a new "normal" message to the specified recipient and adds the
     * specified <tt>geoloc</tt> extension to it.
     *
     * @param to the recipient of the message.
     * @param geoloc the geolocation packet extension to add to this message.
     */
    public GeolocationMessage(String to, GeolocationPacketExtension geoloc)
    {
        super(to);
        this.addExtension(geoloc);
    }

    /**
     * Creates a new message with the specified type and recipient and adds the
     * specified <tt>geoloc</tt> extension to it.
     *
     * @param to the recipient of the message.
     * @param geoloc the geolocation packet extension to add to this message.
     * @param type the message type.
     */
    public GeolocationMessage(String                     to,
                              Message.Type               type,
                              GeolocationPacketExtension geoloc)
    {
        super(to, type);
        addExtension(geoloc);
    }
}
