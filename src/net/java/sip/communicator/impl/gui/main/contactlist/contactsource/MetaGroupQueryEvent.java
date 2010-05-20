/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;

/**
 * The <tt>MetaGroupQueryEvent</tt> is triggered each time a
 * <tt>MetaContactGroup</tt> is received as a result of a
 * <tt>MetaContactQuery</tt>.
 *
 * @author Yana Stamcheva
 */
public class MetaGroupQueryEvent
    extends EventObject
{
    /**
     * The <tt>MetaContactGroup</tt> this event is about.
     */
    private final MetaContactGroup metaGroup;

    /**
     * Creates an instance of <tt>MetaGroupQueryEvent</tt> by specifying the 
     * <tt>source</tt> query this event comes from and the <tt>metaGroup</tt>
     * this event is about.
     *
     * @param source the <tt>MetaContactQuery</tt> that triggered this event
     * @param metaGroup the <tt>MetaContactGroup</tt> this event is about
     */
    public MetaGroupQueryEvent( MetaContactQuery source,
                                MetaContactGroup metaGroup)
    {
        super(source);
        this.metaGroup = metaGroup;
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
     * Returns the <tt>MetaContactGroup</tt> this event is about.
     * @return the <tt>MetaContactGroup</tt> this event is about
     */
    public MetaContactGroup getMetaGroup()
    {
        return metaGroup;
    }
}
