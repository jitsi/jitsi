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
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;

/**
 * The <tt>SearchFilter</tt> is a <tt>ContactListFilter</tt> that filters the
 * contact list content by a filter string.
 *
 * @author Yana Stamcheva
 */
public class SearchFilter
    implements ContactListSearchFilter
{
    /**
     * The string, which we're searching.
     */
    protected String filterString;

    /**
     * The pattern to filter.
     */
    protected Pattern filterPattern;

    /**
     * The <tt>MetaContactListSource</tt> to search in.
     */
    private final MetaContactListSource mclSource;

    /**
     * The source contact list.
     */
    protected ContactList sourceContactList;

    /**
     * The name of the property indicating if searches in call history are
     * enabled.
     */
    protected String DISABLE_CALL_HISTORY_SEARCH_PROP
        = "net.java.sip.communicator.impl.gui"
                + ".DISABLE_CALL_HISTORY_SEARCH_IN_CONTACT_LIST";

    /**
     * If set, we are searching a phone number and will use the phone number
     * service to try matching the numbers.
     */
    private boolean isSearchingPhoneNumber = false;

    /**
     * Creates an instance of <tt>SearchFilter</tt>.
     */
    public SearchFilter(MetaContactListSource contactListSource)
    {
        this.mclSource = contactListSource;
    }

    /**
     * Creates an instance of <tt>SearchFilter</tt>.
     */
    public SearchFilter(ContactList sourceContactList)
    {
        this.mclSource = null;
        this.sourceContactList = sourceContactList;
    }

    /**
     * Applies this filter to the default contact source.
     * @param filterQuery the query that tracks this filter.
     */
    public void applyFilter(FilterQuery filterQuery)
    {
        if (sourceContactList == null)
            sourceContactList = GuiActivator.getContactList();

        Iterator<UIContactSource> filterSources
            = sourceContactList.getContactSources().iterator();

        if (sourceContactList.getDefaultFilter()
                .equals(TreeContactList.presenceFilter))
        {
            final MetaContactQuery defaultQuery = new MetaContactQuery();

            defaultQuery.addContactQueryListener(sourceContactList);

            // First add the MetaContactListSource
            filterQuery.addContactQuery(defaultQuery);

            mclSource.startQuery(defaultQuery, filterPattern);
        }
        else if (sourceContactList.getDefaultFilter()
                    .equals(TreeContactList.historyFilter))
        {
            filterSources = sourceContactList.getContactSources(
                ContactSourceService.HISTORY_TYPE).iterator();
        }

        // If we have stopped filtering in the mean time we return here.
        if (filterQuery.isCanceled())
            return;

        if(sourceContactList instanceof TreeContactList)
        {
            ((TreeContactList) sourceContactList).setAutoSectionAllowed(true);
        }

        // Then we apply the filter on all its contact sources.
        while (filterSources.hasNext())
        {
            final UIContactSource filterSource
                = filterSources.next();

            // Don't search in history sources if this is disabled from the
            // corresponding configuration property.
            if (sourceContactList.getDefaultFilter()
                    .equals(TreeContactList.presenceFilter)
                && GuiActivator.getConfigurationService().getBoolean(
                    DISABLE_CALL_HISTORY_SEARCH_PROP, false)
                && filterSource.getContactSourceService().getType()
                    == ContactSourceService.HISTORY_TYPE)
                continue;

            if (sourceContactList.getDefaultFilter()
                .equals(TreeContactList.presenceFilter))
            {
                if(filterSource.getContactSourceService().getType()
                    == ContactSourceService.CONTACT_LIST_TYPE)
                {
                    //We are setting the index from contactSourceOrder map. This
                    //index is set to reorder the sources in the contact list.
                    filterSource.setContactSourceIndex(
                        this.mclSource.getIndex() + 1);
                }
            }
            // If we have stopped filtering in the mean time we return here.
            if (filterQuery.isCanceled())
                return;

            applyFilter(filterSource, filterQuery);
        }

        // Closes this filter to indicate that we finished adding queries to it.
        if (filterQuery.isRunning())
            filterQuery.close();
    }

    /**
     * Applies this filter to the given <tt>contactSource</tt>.
     *
     * @param contactSource the <tt>ExternalContactSource</tt> to apply the
     * filter to
     * @param filterQuery the filter query object.
     * @return the <tt>ContactQuery</tt> that tracks this filter
     */
    protected ContactQuery applyFilter(UIContactSource contactSource,
        FilterQuery filterQuery)
    {
        ContactSourceService sourceService
            = contactSource.getContactSourceService();

        ContactQuery contactQuery;
        if (sourceService instanceof ExtendedContactSourceService)
            contactQuery
                = ((ExtendedContactSourceService) sourceService)
                    .createContactQuery(filterPattern);
        else
            contactQuery = sourceService.createContactQuery(filterString);

        if(contactQuery == null)
            return null;

        contactQuery.addContactQueryListener(sourceContactList);

        if (contactQuery.getStatus() == ContactQuery.QUERY_IN_PROGRESS)
        {
            filterQuery.addContactQuery(contactQuery);
        }

        contactQuery.start();

        return contactQuery;
    }

    /**
     * Indicates if the given <tt>uiGroup</tt> matches this filter.
     * @param uiContact the <tt>UIGroup</tt> to check
     * @return <tt>true</tt> if the given <tt>uiGroup</tt> matches the current
     * filter, <tt>false</tt> - otherwise
     */
    public boolean isMatching(UIContact uiContact)
    {
        Iterator<String> searchStrings = uiContact.getSearchStrings();

        if (searchStrings != null)
        {
            while (searchStrings.hasNext())
            {
                if (isMatching(searchStrings.next()))
                    return true;
            }
        }
        return false;
    }

    /**
     * For all groups we return false. If some of the child contacts of this
     * group matches this filter the group would be automatically added when
     * the contact is added in the list.
     * @param uiGroup the <tt>UIGroup</tt> to check
     * @return false
     */
    public boolean isMatching(UIGroup uiGroup)
    {
        return false;
    }

    /**
     * Creates the <tt>SearchFilter</tt> by specifying the string used for
     * filtering.
     * @param filter the String used for filtering
     */
    public void setFilterString(String filter)
    {
        // First escape all special characters from the given filter string.
        this.filterString = filter;

        // Then create the pattern.
        // By default, case-insensitive matching assumes that only characters
        // in the US-ASCII charset are being matched, that's why we use
        // the UNICODE_CASE flag to enable unicode case-insensitive matching.
        // Sun Bug ID: 6486934 "RegEx case_insensitive match is broken"
        this.filterPattern
                = Pattern.compile(
                        Pattern.quote(filterString),
                        Pattern.MULTILINE
                            | Pattern.CASE_INSENSITIVE
                            | Pattern.UNICODE_CASE);

        this.isSearchingPhoneNumber
            = GuiActivator.getPhoneNumberI18nService().isPhoneNumber(filter);
    }

    /**
     * Indicates if the given string matches this filter.
     * @param text the text to check
     * @return <tt>true</tt> to indicate that the given <tt>text</tt> matches
     * this filter, <tt>false</tt> - otherwise
     */
    private boolean isMatching(String text)
    {
        if (filterPattern != null)
            return filterPattern.matcher(text).find();

        if(isSearchingPhoneNumber && this.filterString != null)
            return GuiActivator.getPhoneNumberI18nService()
                .phoneNumbersMatch(this.filterString, text);

        return true;

    }
}
