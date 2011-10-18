/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactlist.event;

import net.java.sip.communicator.service.contactlist.*;

/**
 * Indicates that a meta contact has chaned its display name.
 * @author Emil Ivov
 */
public class MetaContactRenamedEvent
    extends MetaContactPropertyChangeEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Creates an instance of this event using the specified arguments.
     * @param source the <tt>MetaContact</tt> that this event is about.
     * @param oldDisplayName the new display name of this meta contact.
     * @param newDisplayName the old display name of this meta contact.
     */
    public MetaContactRenamedEvent(MetaContact source,
                                   String oldDisplayName,
                                   String newDisplayName)
    {
        super(source, META_CONTACT_RENAMED, oldDisplayName, newDisplayName);
    }

    /**
     * Returns the display name of the source meta contact as it is now, after
     * the change.
     * @return the new display name of the meta contact.
     */
    public String getNewDisplayName()
    {
        return (String)getNewValue();
    }

    /**
     * Returns the display name of the source meta contact as it was now, before
     * the change.
     * @return the meta contact name as it was before the change.
     */
    public String getOldDisplayName()
    {
        return (String)getOldValue();
    }
}
