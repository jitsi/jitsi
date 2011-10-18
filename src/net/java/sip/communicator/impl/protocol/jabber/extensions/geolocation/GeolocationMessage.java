/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.geolocation;

import org.jivesoftware.smack.packet.Message;

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
