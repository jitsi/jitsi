/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

        if(sourceContactList instanceof TreeContactList)
        {
            ((TreeContactList) sourceContactList).setAutoSectionAllowed(true);
        }

        // Then we apply the filter on all its contact sources.
        while (filterSourceIter.hasNext())
        {
            final UIContactSource filterSource
                = filterSourceIter.next();

            // If we have stopped filtering in the mean time we return here.
            if (filterQuery.isCanceled())
                return;

            applyFilter(filterSource, filterQuery);
        }

        // Closes this filter to indicate that we finished adding queries to it.
        if (filterQuery.isRunning())
            filterQuery.close();
    }
}
