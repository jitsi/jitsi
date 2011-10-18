/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;

/**
 * The <tt>MetaContactQueryEvent</tt> is triggered each time a
 * <tt>MetaContact</tt> is received as a result of a <tt>MetaContactQuery</tt>.
 *
 * @author Yana Stamcheva
 */
public class MetaContactQueryEvent
    extends EventObject
{
    /**
     * The <tt>MetaContact</tt> this event is about.
     */
    private final MetaContact metaContact;

    /**
     * Creates an instance of <tt>MetaGroupQueryEvent</tt> by specifying the 
     * <tt>source</tt> query this event comes from and the <tt>metaContact</tt>
     * this event is about.
     *
     * @param source the <tt>MetaContactQuery</tt> that triggered this event
     * @param metaContact the <tt>MetaContact</tt> this event is about
     */
    public MetaContactQueryEvent(   MetaContactQuery source,
                                    MetaContact metaContact)
    {
        super(source);
        this.metaContact = metaContact;
    }

    /**
     * Returns the <tt>MetaContactQuery</tt> that triggered this event.
     * @return the <tt>MetaContactQuery</tt> that triggered this event
     */
    public MetaContactQuery getQuerySource()
    {
        return (MetaContactQuery) source;
    }

    /**
     * Returns the <tt>MetaContact</tt> this event is about.
     * @return the <tt>MetaContact</tt> this event is about
     */
    public MetaContact getMetaContact()
    {
        return metaContact;
    }
}
