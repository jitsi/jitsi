/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactlist.event;

import net.java.sip.communicator.service.contactlist.*;

/**
 * Indicates that a meta contact has chaned.
 * @author Damian Minkov
 */
public class MetaContactModifiedEvent
    extends MetaContactPropertyChangeEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Name of the modification.
     */
    private String modificationName;

    /**
     * Creates an instance of this event using the specified arguments.
     * @param source the <tt>MetaContact</tt> that this event is about.
     * @param modificationName name of the modification
     * @param oldValue the new value for the modification of this meta contact.
     * @param newValue the old value for the modification of this meta contact.
     */
    public MetaContactModifiedEvent(MetaContact source,
                                   String modificationName,
                                   Object oldValue,
                                   Object newValue)
    {
        super(source, META_CONTACT_MODIFIED, oldValue, newValue);
        this.modificationName = modificationName;
    }

    /**
     * Returns the modification name of the source meta contact.
     * @return the modification name for the meta contact.
     */
    public String getModificationName()
    {
        return modificationName;
    }
}
