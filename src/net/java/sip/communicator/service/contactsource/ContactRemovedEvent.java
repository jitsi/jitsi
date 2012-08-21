/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactsource;

import java.util.*;

/**
 * The <tt>ContactRemovedEvent</tt> indicates that a
 * <tt>SourceContact</tt> has been removed from the result of a
 * <tt>ContactQuery</tt>.
 * @author Yana Stamcheva
 */
public class ContactRemovedEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The contact that has been removed.
     */
    private final SourceContact contact;

    /**
     * Creates a <tt>ContactRemovedEvent</tt> by specifying the contact search
     * source and the removed <tt>searchContact</tt>.
     * @param source the source that triggered this event
     * @param contact the removed contact
     */
    public ContactRemovedEvent(ContactQuery source,
                               SourceContact contact)
    {
        super(source);

        this.contact = contact;
    }

    /**
     * Returns the <tt>ContactQuery</tt> that triggered this event.
     * @return the <tt>ContactQuery</tt> that triggered this event
     */
    public ContactQuery getQuerySource()
    {
        return (ContactQuery) source;
    }

    /**
     * Returns the removed contact.
     * @return the removed contact
     */
    public SourceContact getContact()
    {
        return contact;
    }
}
