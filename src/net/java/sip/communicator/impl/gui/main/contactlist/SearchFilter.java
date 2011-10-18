/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.service.contactsource.*;

/**
 * The <tt>SearchFilter</tt> is a <tt>ContactListFilter</tt> that filters the
 * contact list content by a filter string.
 *
 * @author Yana Stamcheva
 */
public class SearchFilter
    implements ContactListFilter
{
    /**
     * The default contact source search type.
     */
    public static final int DEFAULT_SOURCE = 0;

    /**
     * The history contact source search type.
     */
    public static final int HISTORY_SOURCE = 1;

    /**
     * The string, which we're searching.
     */
    private String filterString;

    /**
     * The pattern to filter.
     */
    private Pattern filterPattern;

    /**
     * The <tt>MetaContactListSource</tt> to search in.
     */
    private final MetaContactListSource mclSource;

    /**
     * The list of external contact sources to search in.
     */
    private Collection<ExternalContactSource> contactSources;

    /**
     * The type of the search source. One of the above defined DEFAUT_SOURCE or
     * HISTORY_SOURCE.
     */
    private int searchSourceType = DEFAULT_SOURCE;

    /**
     * Creates an instance of <tt>SearchFilter</tt>.
     */
    public SearchFilter()
    {
        this.mclSource = new MetaContactListSource();
    }

    /**
     * Applies this filter to the default contact source.
     * @param filterQuery the query that tracks this filter.
     */
    public void applyFilter(FilterQuery filterQuery)
    {
        // If the filter has a default contact source, we apply it first.
        if (searchSourceType == DEFAULT_SOURCE)
        {
            MetaContactQuery defaultQuery
                = mclSource.queryMetaContactSource(filterPattern);

            defaultQuery.addContactQueryListener(GuiActivator.getContactList());

            // First add the MetaContactListSource
            filterQuery.addContactQuery(defaultQuery);
        }

        // If we have stopped filtering in the mean time we return here.
        if (filterQuery.isCanceled())
            return;

        Iterator<ExternalContactSource> filterSources
             = getContactSources().iterator();

        // Then we apply the filter on all its contact sources.
        while (filterSources.hasNext())
        {
            final ExternalContactSource filterSource = filterSources.next();

            // If we have stopped filtering in the mean time we return here.
            if (filterQuery.isCanceled())
                return;

            filterQuery.addContactQuery(
                applyFilter(filterSource));
        }

        // Closes this filter to indicate that we finished adding queries to it.
        filterQuery.close();
    }

    /**
     * Applies this filter to the given <tt>contactSource</tt>.
     *
     * @param contactSource the <tt>ExternalContactSource</tt> to apply the
     * filter to
     * @return the <tt>ContactQuery</tt> that tracks this filter
     */
    public ContactQuery applyFilter(ExternalContactSource contactSource)
    {
        ContactSourceService sourceService
            = contactSource.getContactSourceService();

        ContactQuery contactQuery;
        if (sourceService instanceof ExtendedContactSourceService)
            contactQuery
                = ((ExtendedContactSourceService) sourceService)
                    .queryContactSource(filterPattern);
        else
            contactQuery = sourceService.queryContactSource(filterString);

        // Add first available results.
        this.addMatching(contactQuery.getQueryResults());

        contactQuery.addContactQueryListener(GuiActivator.getContactList());

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
    }

    /**
     * Checks if the given <tt>contact</tt> is matching the current filter.
     * A <tt>SourceContact</tt> would be matching the filter if its display
     * name is matching the search string.
     * @param contact the <tt>ContactListContactDescriptor</tt> to check
     * @return <tt>true</tt> to indicate that the given <tt>contact</tt> is
     * matching the current filter, otherwise returns <tt>false</tt>
     */
    private boolean isMatching(SourceContact contact)
    {
        return isMatching(contact.getDisplayName());
    }

    /**
     * Indicates if the given string matches this filter.
     * @param text the text to check
     * @return <tt>true</tt> to indicate that the given <tt>text</tt> matches
     * this filter, <tt>false</tt> - otherwise
     */
    private boolean isMatching(String text)
    {
        return filterPattern.matcher(text).find();
    }

    /**
     * Adds the list of <tt>sourceContacts</tt> to the contact list.
     * @param sourceContacts the list of <tt>SourceContact</tt>s to add
     */
    private void addMatching(List<SourceContact> sourceContacts)
    {
        Iterator<SourceContact> contactsIter = sourceContacts.iterator();

        while (contactsIter.hasNext())
            addSourceContact(contactsIter.next());
    }

    /**
     * Adds the given <tt>sourceContact</tt> to the contact list.
     * @param sourceContact the <tt>SourceContact</tt> to add
     */
    private void addSourceContact(SourceContact sourceContact)
    {
        ContactSourceService contactSource
            = sourceContact.getContactSource();
        ExternalContactSource sourceUI
            = TreeContactList.getContactSource(contactSource);

        if (sourceUI != null
            // ExtendedContactSourceService has already matched the
            // SourceContact over the pattern
            && (contactSource instanceof ExtendedContactSourceService)
                || isMatching(sourceContact))
        {
            GuiActivator.getContactList().addContact(
                sourceUI.createUIContact(sourceContact),
                sourceUI.getUIGroup(),
                false,
                true);
        }
    }

    /**
     * Sets the search source type: DEFAULT_SOURCE or HISTORY_SOURCE.
     * @param searchSourceType the type of the search source to set
     */
    public void setSearchSourceType(int searchSourceType)
    {
        this.searchSourceType = searchSourceType;

        switch(searchSourceType)
        {
            case DEFAULT_SOURCE:
                contactSources = TreeContactList.getContactSources();
                break;
            case HISTORY_SOURCE:
            {
                Collection<ExternalContactSource> historySources
                    = new LinkedList<ExternalContactSource>();
                ExternalContactSource historySource
                    = TreeContactList.getContactSource(
                        ContactSourceService.CALL_HISTORY);

                historySources.add(historySource);
                contactSources = historySources;
                break;
            }
        }
    }

    /**
     * Returns the list of <tt>ExternalContactSource</tt> this filter searches
     * in.
     * @return the list of <tt>ExternalContactSource</tt> this filter searches
     * in
     */
    public Collection<ExternalContactSource> getContactSources()
    {
        if (contactSources == null)
            contactSources = TreeContactList.getContactSources();
        return contactSources;
    }

    /**
     * Indicates if this filter contains a default source.
     * @return <tt>true</tt> if this filter contains a default source,
     * <tt>false</tt> otherwise
     */
    public boolean hasDefaultSource()
    {
        return (searchSourceType == DEFAULT_SOURCE);
    }
}
