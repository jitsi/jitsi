/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactlist.event;

import net.java.sip.communicator.service.contactlist.*;

/**
 * Indicates that a meta contact has changed or added an avatar.
 *
 * @author Emil Ivov
 */
public class MetaContactAvatarUpdateEvent
    extends MetaContactPropertyChangeEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Creates an instance of this event using the specified arguments.
     *
     * @param source the <tt>MetaContact</tt> that this event is about.
     * @param oldAvatar the new avatar just of this meta contact.
     * @param newAvatar the old avatar that just got replaced or <tt>null</tt>.
     */
    public MetaContactAvatarUpdateEvent(MetaContact source,
                                   byte[] oldAvatar,
                                   byte[] newAvatar)
    {
        super(source, META_CONTACT_AVATAR_UPDATE, oldAvatar, newAvatar);
    }

    /**
     * Returns the updated avatar of the source meta contact as it is now, after
     * the change.
     *
     * @return the newly changed avatar for this meta contact.
     */
    public byte[] getNewAvatar()
    {
        return (byte[])getNewValue();
    }

    /**
     * Returns the previously active avatar of the source meta contact as it was
     * now, before the change.
     * @return the avatar that got replaced by the new one.
     */
    public byte[] getOldAvatar()
    {
        return (byte[])getOldValue();
    }
}
