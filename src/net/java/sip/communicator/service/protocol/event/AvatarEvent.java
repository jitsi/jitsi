/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Instances of this class represent a change avatar of a protocol
 * 
 * @author Damien Roth
 */
public class AvatarEvent
    extends EventObject
{
    /**
     * The new avatar
     */
    private byte[] newAvatar;

    /**
     * The provider that has generated the event.
     */
    private ProtocolProviderService sourceProvider;

    /**
     * Creates an event instance indicating that the specified protocol
     * has changed its avatar to <tt>newAvatar</tt>.
     * 
     * @param sourceOp the operation set that generated this event
     * @param sourceProvider the protocol provider that the contact belongs to
     * @param newAvatar the new avatar
     */
    public AvatarEvent(OperationSetAvatar sourceOp,
            ProtocolProviderService sourceProvider, byte[] newAvatar)
    {
        super(sourceOp);
        this.sourceProvider = sourceProvider;
        this.newAvatar = newAvatar;
    }

    /**
     * Returns the provider that the source belongs to.
     *
     * @return the provider that the source belongs to.
     */
    public ProtocolProviderService getSourceProvider()
    {
        return this.sourceProvider;
    }

    /**
     * Returns the new avatar
     * @return the new avatar
     */
    public byte[] getNewAvatar()
    {
        return this.newAvatar;
    }

    /**
     * Returns the <tt>OperationSetAvatar</tt> instance that is the source
     * of this event.
     *
     * @return the <tt>OperationSetAvatar</tt> instance that is the source
     * of this event.
     */
    public OperationSetAvatar getSourceAvatarOperationSet()
    {
        return (OperationSetAvatar) getSource();
    }

    /**
     * Returns a String representation of this AvatarEvent
     * 
     * @return a <tt>String</tt> representation of this <tt>AvatarEvent</tt>.
     */
    public String toString()
    {
        return "AvatarEvent-[ Provider=" + getSourceProvider() + "]";
    }
}
