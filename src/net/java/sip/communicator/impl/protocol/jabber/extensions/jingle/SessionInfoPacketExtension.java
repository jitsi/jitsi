/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Represents <tt>session-info</tt> elements such as active, ringing, or hold
 * for example.
 *
 * @author Emil Ivov
 */
public class SessionInfoPacketExtension extends AbstractPacketExtension
{
    /**
     * The name space for RTP description elements.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:info:1";

    /**
     * The exact type of this info packet.
     */
    private final SessionInfoType type;

    /**
     * Creates a new info element of the specified type.
     *
     * @param type the name of the element we'd like to create (mute, active,
     * hold);
     */
    public SessionInfoPacketExtension(SessionInfoType type)
    {
        super(NAMESPACE, type.toString());
        this.type = type;
    }

    /**
     * Returns the exact type of this {@link SessionInfoPacketExtension}.
     *
     * @return the {@link SessionInfoType} of this extension.
     */
    public SessionInfoType getType()
    {
        return type;
    }
}
