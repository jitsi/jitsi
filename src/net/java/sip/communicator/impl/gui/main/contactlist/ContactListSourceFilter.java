/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.service.contactsource.*;

/**
 * The <tt>ContactListSourceFilter</tt> is a <tt>ContactListFilter</tt> that
 * allows to apply the filter to only one of its contact sources at a time.
 *
 * @author Yana Stamcheva
 */
public interface ContactListSourceFilter extends ContactListFilter
{
    /**
     * Applies this filter to the given <tt>contactSource</tt>.
     *
     * @param contactSource the <tt>ExternalContactSource</tt> to apply the
     * filter to
     * @return the <tt>ContactQuery</tt> that tracks this filter
     */
    public ContactQuery applyFilter(ExternalContactSource contactSource);

    /**
     * Returns the list of current <tt>ExternalContactSource</tt>s this filter
     * works with.
     * @return the list of current <tt>ExternalContactSource</tt>s this filter
     * works with
     */
    public Collection<ExternalContactSource> getContactSources();

    /**
     * Indicates if this filter contains a default source.
     * @return <tt>true</tt> if this filter contains a default source,
     * <tt>false</tt> otherwise
     */
    public boolean hasDefaultSource();
}
