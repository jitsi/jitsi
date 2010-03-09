/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.searchsource;

import java.util.*;

/**
 * The <tt>ContactReceivedEvent</tt> indicates that a
 * <tt>SearchSourceContact</tt> has been received as a result of a
 * <tt>ContactQuery</tt>.
 * @author Yana Stamcheva
 */
public class ContactReceivedEvent
    extends EventObject
{
    /**
     * The contact that has been received.
     */
    private final SearchSourceContact contact;

    /**
     * Creates a <tt>ContactReceivedEvent</tt> by specifying the contact search
     * source and the received <tt>searchContact</tt>.
     * @param source the source that triggered this event
     * @param contact the received contact
     */
    public ContactReceivedEvent(ContactQuery source,
                                SearchSourceContact contact)
    {
        super(source);

        this.contact = contact;
    }

    /**
     * Returns the <tt>ContactQuery</tt> that triggered this event.
     * @return the <tt>ContactQuery</tt> that triggered this event
     */
    public ContactQuery getSearchSource()
    {
        return (ContactQuery) source;
    }

    /**
     * Returns the received contact.
     * @return the received contact
     */
    public SearchSourceContact getContact()
    {
        return contact;
    }
}
