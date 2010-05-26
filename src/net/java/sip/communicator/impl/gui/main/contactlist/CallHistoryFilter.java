/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.service.contactsource.*;

/**
 * The <tt>CallHistoryFilter</tt> is a filter over the history contact sources.
 *
 * @author Yana Stamcheva
 */
public class CallHistoryFilter
    implements  ContactListFilter
{
    /**
     * Applies this filter and stores the result in the given <tt>treeModel</tt>.
     * @param filterQuery the <tt>FilterQuery</tt> that tracks the results of
     * this filtering
     */
    public void applyFilter(FilterQuery filterQuery)
    {
        Collection<ExternalContactSource> contactSources
            = TreeContactList.getContactSources();

        for (ExternalContactSource contactSource : contactSources)
        {
            ContactSourceService sourceService
                = contactSource.getContactSourceService();

            if (!sourceService.getIdentifier()
                    .equals(ContactSourceService.CALL_HISTORY))
                continue;

            // We're in a case of call history contact source.
            ContactQuery query = sourceService.queryContactSource("");
            filterQuery.addContactQuery(query);

            // Add first available results.
            this.addMatching(   query.getQueryResults(),
                                contactSource);

            // We know that this query should be finished here and we do not
            // expect any further results from it.
            filterQuery.removeQuery(query);
        }
        // Closes this filter to indicate that we finished adding queries to it.
        filterQuery.close();
    }

    /**
     * Indicates if the given <tt>uiContact</tt> is matching this filter.
     * @param uiContact the <tt>UIContact</tt> to check for match
     * @return <tt>true</tt> if the given <tt>uiContact</tt> is matching this
     * filter, <tt>false</tt> otherwise
     */
    public boolean isMatching(UIContact uiContact)
    {
        Object descriptor = uiContact.getDescriptor();

        if (descriptor instanceof SourceContact)
        {
            SourceContact sourceContact = (SourceContact) descriptor;

            if ((sourceContact.getContactSource().getIdentifier()
                    .equals(ContactSourceService.CALL_HISTORY)))
                return true;
        }
        return false;
    }

    /**
     * No group could match this filter.
     * @param uiGroup the <tt>UIGroup</tt> to check for match
     * @return <tt>false</tt> to indicate that no group could match this filter
     */
    public boolean isMatching(UIGroup uiGroup)
    {
        return false;
    }

    /**
     * Adds matching <tt>sourceContacts</tt> to the result tree model.
     * @param sourceContacts the list of <tt>SourceContact</tt>s to add
     * @param uiSource the <tt>ExternalContactSource</tt>, which contacts
     * we're adding
     */
    private void addMatching(   List<SourceContact> sourceContacts,
                                ExternalContactSource uiSource)
    {
        Iterator<SourceContact> contactsIter = sourceContacts.iterator();

        while (contactsIter.hasNext())
        {
            GuiActivator.getContactList()
                .addContact(uiSource.createUIContact(contactsIter.next()),
                            uiSource.getUIGroup(),
                            false);
        }
    }
}
