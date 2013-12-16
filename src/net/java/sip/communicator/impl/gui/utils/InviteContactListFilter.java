/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.util.*;

import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>InviteContactListFilter</tt> is a <tt>SearchFilter</tt> that filters
 * the contact list to fit invite operations.
 *
 * @author Yana Stamcheva
 */
public class InviteContactListFilter
    extends SearchFilter
{
    /**
     * Creates an instance of <tt>InviteContactListFilter</tt>.
     *
     * @param sourceContactList the contact list to filter
     */
    public InviteContactListFilter(ContactList sourceContactList)
    {
        super(sourceContactList);
    }

    /**
     * Applies this filter to the default contact source.
     * @param filterQuery the query that tracks this filter.
     */
    @Override
    public void applyFilter(FilterQuery filterQuery)
    {
        filterQuery.setMaxResultShown(-1);

        List<UIContactSource> filterSources
            = sourceContactList.getContactSources(
                ContactSourceService.DEFAULT_TYPE);

        if (filterString != null && filterString.length() > 0)
        {
            filterSources.addAll(sourceContactList
                .getContactSources(ContactSourceService.SEARCH_TYPE));

            filterSources.addAll(sourceContactList
                .getContactSources(ContactSourceService.HISTORY_TYPE));
        }

        Iterator<UIContactSource> filterSourceIter = filterSources.iterator();

        // If we have stopped filtering in the mean time we return here.
        if (filterQuery.isCanceled())
            return;

        // Then we apply the filter on all its contact sources.
        while (filterSourceIter.hasNext())
        {
            final UIContactSource filterSource
                = filterSourceIter.next();

            // If we have stopped filtering in the mean time we return here.
            if (filterQuery.isCanceled())
                return;

            ContactQuery query = applyFilter(filterSource);

            if (query.getStatus() == ContactQuery.QUERY_IN_PROGRESS)
                filterQuery.addContactQuery(query);
        }

        // Closes this filter to indicate that we finished adding queries to it.
        if (filterQuery.isRunning())
            filterQuery.close();
        else if (!sourceContactList.isEmpty())
            sourceContactList.selectFirstContact();
    }
}