/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;
import org.jivesoftware.smack.packet.*;

/**
 * Jabber protocol provider implementation of {@link OperationSetJitsiMeetTools}
 *
 * @author Pawel Domas
 */
public class OperationSetJitsiMeetToolsJabberImpl
    implements OperationSetJitsiMeetTools
{
    /**
     * {@inheritDoc}
     */
    @Override
    public void sendPresenceExtension(ChatRoom chatRoom,
                                      PacketExtension extension)
    {
        ((ChatRoomJabberImpl)chatRoom).sendPresenceExtension(extension);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPresenceStatus(ChatRoom chatRoom, String statusMessage)
    {
        ((ChatRoomJabberImpl)chatRoom).publishPresenceStatus(statusMessage);
    }
}
