/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
import net.java.sip.communicator.util.*;

/**
 * The <tt>SearchFilter</tt> is a <tt>ContactListFilter</tt> that filters the
 * contact list content by a filter string.
 *
 * @author Yana Stamcheva
 */
public class SearchFilter
    implements  ContactListSourceFilter
{
    /**
     * This class logger.
     */
    private final Logger logger = Logger.getLogger(SearchFilter.class);

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
     * The current operating query.
     */
    private ContactQuery currentQuery;

    /**
     * The type of the search source. One of the above defined DEFAUT_SOURCE or
     * HISTORY_SOURCE.
     */
    private int searchSourceType;

    /**
     * Creates an instance of <tt>SearchFilter</tt>.
     */
    public SearchFilter()
    {
        this.mclSource = new MetaContactListSource();
    }

    /**
     * Applies this filter and stores the result in the given <tt>treeModel</tt>.
     * @param treeModel the <tt>ContactListTreeModel</tt>, in which we store
     * results
     */
    public void applyFilter(ContactListTreeModel treeModel)
    {
        logger.debug("Search filter applied on default source");
        if (searchSourceType == DEFAULT_SOURCE)
            // First add the MetaContactListSource
            mclSource.filter(filterPattern, treeModel);
    }

    /**
     * Applies this filter to the given <tt>contactSource</tt> and stores the
     * result in the given <tt>treeModel</tt>.
     *
     * @param contactSource the <tt>ExternalContactSource</tt> to apply the
     * filter to
     * @param treeModel the <tt>ContactListTreeModel</tt> in which the results
     * are stored
     */
    public void applyFilter(ExternalContactSource contactSource,
                            ContactListTreeModel treeModel)
    {
        logger.debug("Search filter applied on source: "
                + contactSource.getContactSourceService());

        ContactSourceService sourceService
            = contactSource.getContactSourceService();

        if (sourceService instanceof ExtendedContactSourceService)
            currentQuery
                = ((ExtendedContactSourceService) sourceService)
                    .queryContactSource(filterPattern);
        else
            currentQuery = sourceService.queryContactSource(filterString);

        // Add first available results.
        this.addMatching(currentQuery.getQueryResults(), treeModel);

        currentQuery.addContactQueryListener(GuiActivator.getContactList());
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

        while (searchStrings != null && searchStrings.hasNext())
        {
            if (isMatching(searchStrings.next()))
                return true;
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
        this.filterString = Pattern.quote(filter);

        // Then create the pattern.
        // By default, case-insensitive matching assumes that only characters
        // in the US-ASCII charset are being matched, that's why we use
        // the UNICODE_CASE flag to enable unicode case-insensitive matching.
        // Sun Bug ID: 6486934 "RegEx case_insensitive match is broken"
        this.filterPattern = Pattern.compile(
            filterString, Pattern.MULTILINE
                            | Pattern.CASE_INSENSITIVE
                            | Pattern.UNICODE_CASE);
    }

    /**
     * Stops the current query.
     */
    public void stopFilter()
    {
        if (currentQuery != null)
            currentQuery.cancel();
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
        Matcher matcher = filterPattern.matcher(text);

        if(matcher.find())
            return true;

        return false;
    }

    /**
     * Adds the list of <tt>sourceContacts</tt> in the given <tt>treeModel</tt>.
     * @param sourceContacts the list of <tt>SourceContact</tt>s to add
     * @param treeModel the <tt>ContactListTreeModel</tt>, where the contacts
     * are added
     */
    private void addMatching(   List<SourceContact> sourceContacts,
                                ContactListTreeModel treeModel)
    {
        Iterator<SourceContact> contactsIter = sourceContacts.iterator();
        while (contactsIter.hasNext())
        {
            addSourceContact(contactsIter.next(), treeModel);
        }
    }

    /**
     * Adds the given <tt>sourceContact</tt> to the result tree model.
     * @param sourceContact the <tt>SourceContact</tt> to add
     * @param treeModel the <tt>ContactListTreeModel</tt> storing the result
     */
    private void addSourceContact(  SourceContact sourceContact,
                                    ContactListTreeModel treeModel)
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
                treeModel,
                sourceUI.createUIContact(sourceContact),
                sourceUI.getUIGroup(),
                false,
                false);
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
                ExternalContactSource historySource
                    = TreeContactList.getContactSource(
                        ContactSourceService.CALL_HISTORY);

                Collection<ExternalContactSource> historySources
                    = new LinkedList<ExternalContactSource>();

                historySources.add(historySource);
                contactSources = historySources;
                break;
            }
        }
    }

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
        if (searchSourceType == DEFAULT_SOURCE)
            return true;
        else
            return false;
    }
}
