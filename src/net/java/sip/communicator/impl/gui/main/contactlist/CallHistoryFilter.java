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
    implements  ContactListFilter,
                ContactQueryListener
{
    /**
     * The <tt>ContactListTreeModel</tt>, where the results of this filter are
     * stored
     */
    private ContactListTreeModel resultTreeModel;

    /**
     * The current <tt>ContactQuery</tt>.
     */
    private ContactQuery currentQuery;

    /**
     * Applies this filter and stores the result in the given <tt>treeModel</tt>.
     *
     * @param treeModel the <tt>ContactListTreeModel</tt>, where the results
     * of this filter are stored
     */
    public void applyFilter(ContactListTreeModel treeModel)
    {
        this.resultTreeModel = treeModel;

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
            currentQuery = sourceService.queryContactSource("");

            // Add first available results.
            this.addMatching(   currentQuery.getQueryResults(),
                                contactSource);

            currentQuery.addContactQueryListener(this);
        }
    }

    public boolean isMatching(UIContact uiContact)
    {
        return false;
    }

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
            addHistoryContact(contactsIter.next(), uiSource);
        }
    }

    /**
     * Indicates that a contact has been received for a query.
     * @param event the <tt>ContactReceivedEvent</tt> that notified us
     */
    public void contactReceived(ContactReceivedEvent event)
    {
        synchronized (resultTreeModel)
        {
            ExternalContactSource sourceUI
                = TreeContactList.getContactSource(
                    event.getQuerySource().getContactSource());

            addHistoryContact(event.getContact(), sourceUI);
        }
    }

    /**
     * Indicates that the query status has changed.
     * @param event the <tt>ContactQueryStatusEvent</tt> that notified us
     */
    public void queryStatusChanged(ContactQueryStatusEvent event)
    {
        int eventType = event.getEventType();

        // Remove the current query when it's stopped for some reason.
        // QUERY_COMPLETED, QUERY_COMPLETED, QUERY_ERROR
        currentQuery = null;

        if (eventType == ContactQueryStatusEvent.QUERY_ERROR)
        {
            //TODO: Show the error to the user??
        }

        event.getQuerySource().removeContactQueryListener(this);
    }

    /**
     * Adds the given <tt>sourceContact</tt> to the contact list.
     * @param sourceContact the <tt>SourceContact</tt> to add
     * @param uiSource the UI adapter for the original contact source
     */
    private void addHistoryContact( SourceContact sourceContact,
                                    ExternalContactSource uiSource)
    {
        GuiActivator.getContactList()
            .addContact(resultTreeModel,
                        uiSource.getUIContact(sourceContact),
                        false,
                        false);
    }

    /**
     * Stops this filter current queries.
     */
    public void stopFilter()
    {
        if (currentQuery != null)
            currentQuery.cancel();
    }
}
