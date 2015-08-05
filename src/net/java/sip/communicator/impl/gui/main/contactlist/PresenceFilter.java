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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>PresenceFilter</tt> is used to filter offline contacts from the
 * contact list.
 *
 * @author Yana Stamcheva
 */
public class PresenceFilter
    implements ContactListFilter
{
    /**
     * The <tt>Logger</tt> used by the <tt>PresenceFilter</tt> class and its
     * instances to print debugging information.
     */
    private static final Logger logger = Logger.getLogger(PresenceFilter.class);

    /**
     * Indicates if this presence filter shows or hides the offline contacts.
     */
    private boolean isShowOffline;

    /**
     * The initial result count below which we insert all filter results
     * directly to the contact list without firing events.
     */
    private static final int INITIAL_CONTACT_COUNT = 30;

    /**
     * Creates an instance of <tt>PresenceFilter</tt>.
     */
    public PresenceFilter()
    {
        isShowOffline = ConfigurationUtils.isShowOffline();
    }

    /**
     * Applies this filter. This filter is applied over the
     * <tt>MetaContactListService</tt>.
     *
     * @param filterQuery the query which keeps track of the filtering results
     */
    public void applyFilter(FilterQuery filterQuery)
    {
        // Create the query that will track filtering.
        MetaContactQuery query = new MetaContactQuery();

        // Add this query to the filterQuery.
        filterQuery.addContactQuery(query);

        TreeContactList contactList = GuiActivator.getContactList();

        Collection<UIContactSource> uiContactSourceCollection
            = contactList.getContactSources(
            ContactSourceService.CONTACT_LIST_TYPE);

        Iterator<UIContactSource> filterSources
            = uiContactSourceCollection.iterator();
        int maxIndex = 0;
        while (filterSources.hasNext())
        {
            UIContactSource filterSource = filterSources.next();
            int currIx = filterSource.getContactSourceService().getIndex();
            if(maxIndex < currIx)
                maxIndex = currIx;
        }

        contactList.getMetaContactListSource().setIndex(maxIndex + 1);

        filterSources = uiContactSourceCollection.iterator();
        while (filterSources.hasNext())
        {
            UIContactSource filterSource = filterSources.next();

            filterSource.setContactSourceIndex(
                filterSource.getContactSourceService().getIndex());

            ContactSourceService sourceService
                = filterSource.getContactSourceService();

            ContactQuery contactQuery
                = sourceService.createContactQuery(null);

            if(contactQuery == null)
                continue;

            // Add this query to the filterQuery.
            filterQuery.addContactQuery(contactQuery);

            contactQuery.addContactQueryListener(contactList);

            contactQuery.start();
        }

        // Closes this filter to indicate that we finished adding queries to it.
        filterQuery.close();

        query.addContactQueryListener(GuiActivator.getContactList());
        int resultCount = 0;

        addMatching(GuiActivator.getContactListService().getRoot(),
                    query,
                    resultCount);

        query.fireQueryEvent(
                query.isCanceled()
                    ? MetaContactQueryStatusEvent.QUERY_CANCELED
                    : MetaContactQueryStatusEvent.QUERY_COMPLETED);
    }

    /**
     * Indicates if the given <tt>uiContact</tt> is matching this filter.
     *
     * @param uiContact the <tt>UIContact</tt> to check
     * @return <tt>true</tt> if the given <tt>uiContact</tt> is matching
     * this filter, otherwise returns <tt>false</tt>
     */
    public boolean isMatching(UIContact uiContact)
    {
        Object descriptor = uiContact.getDescriptor();

        if (descriptor instanceof MetaContact)
            return isMatching((MetaContact) descriptor);
        else if (descriptor instanceof SourceContact)
            return isMatching((SourceContact)descriptor);
        else
            return false;
    }

    /**
     * Indicates if the given <tt>uiGroup</tt> is matching this filter.
     *
     * @param uiGroup the <tt>UIGroup</tt> to check
     * @return <tt>true</tt> if the given <tt>uiGroup</tt> is matching
     * this filter, otherwise returns <tt>false</tt>
     */
    public boolean isMatching(UIGroup uiGroup)
    {
        Object descriptor = uiGroup.getDescriptor();

        if (descriptor instanceof MetaContactGroup)
            return isMatching((MetaContactGroup) descriptor);
        else
            return false;
    }

    /**
     * Sets the show offline property.
     *
     * @param isShowOffline indicates if offline contacts are shown
     */
    public void setShowOffline(boolean isShowOffline)
    {
        this.isShowOffline = isShowOffline;

        ConfigurationUtils.setShowOffline(isShowOffline);
    }

    /**
     * Returns <tt>true</tt> if offline contacts are shown, otherwise returns
     * <tt>false</tt>.
     *
     * @return <tt>true</tt> if offline contacts are shown, otherwise returns
     * <tt>false</tt>
     */
    public boolean isShowOffline()
    {
        return isShowOffline;
    }

    /**
     * Returns <tt>true</tt> if offline contacts are shown or if the given
     * <tt>MetaContact</tt> is online, otherwise returns false.
     *
     * @param metaContact the <tt>MetaContact</tt> to check
     * @return <tt>true</tt> if the given <tt>MetaContact</tt> is matching this
     * filter
     */
    public boolean isMatching(MetaContact metaContact)
    {
        return isShowOffline || isContactOnline(metaContact);
    }

    /**
     * Returns <tt>true</tt> if offline contacts are shown or if the given
     * <tt>MetaContact</tt> is online, otherwise returns false.
     *
     * @param contact the <tt>MetaContact</tt> to check
     * @return <tt>true</tt> if the given <tt>MetaContact</tt> is matching this
     * filter
     */
    public boolean isMatching(SourceContact contact)
    {
        // make sure we always show chat rooms and recent messages
        return
            isShowOffline
                || contact.getPresenceStatus().isOnline()
                || contact.getContactSource().getType()
                        == ContactSourceService.CONTACT_LIST_TYPE;
    }

    /**
     * Returns <tt>true</tt> if offline contacts are shown or if the given
     * <tt>MetaContactGroup</tt> contains online contacts.
     *
     * @param metaGroup the <tt>MetaContactGroup</tt> to check
     * @return <tt>true</tt> if the given <tt>MetaContactGroup</tt> is matching
     * this filter
     */
    private boolean isMatching(MetaContactGroup metaGroup)
    {
        return
            isShowOffline
                || (metaGroup.countOnlineChildContacts() > 0)
                || MetaContactListSource.isNewGroup(metaGroup);
    }

    /**
     * Returns <tt>true</tt> if the given meta contact is online, <tt>false</tt>
     * otherwise.
     *
     * @param contact the meta contact
     * @return <tt>true</tt> if the given meta contact is online, <tt>false</tt>
     * otherwise
     */
    private boolean isContactOnline(MetaContact contact)
    {
        // If for some reason the default contact is null we return false.
        Contact defaultContact = contact.getDefaultContact();
        if(defaultContact == null)
            return false;

        // Lays on the fact that the default contact is the most connected.
        return defaultContact.getPresenceStatus().getStatus()
                >= PresenceStatus.ONLINE_THRESHOLD;
    }

    /**
     * Adds all contacts contained in the given <tt>MetaContactGroup</tt>
     * matching the current filter and not contained in the contact list.
     *
     * @param metaGroup the <tt>MetaContactGroup</tt>, which matching contacts
     * to add
     * @param query the <tt>MetaContactQuery</tt> that notifies interested
     * listeners of the results of this matching
     * @param resultCount the initial result count we would insert directly to
     * the contact list without firing events
     */
    private void addMatching(   MetaContactGroup metaGroup,
                                MetaContactQuery query,
                                int resultCount)
    {
        Iterator<MetaContact> childContacts = metaGroup.getChildContacts();

        while (childContacts.hasNext() && !query.isCanceled())
        {
            MetaContact metaContact = childContacts.next();

            if(isMatching(metaContact))
            {

                resultCount++;
                if (resultCount <= INITIAL_CONTACT_COUNT)
                {
                    UIGroup uiGroup = null;

                    if (!MetaContactListSource.isRootGroup(metaGroup))
                    {
                        synchronized (metaGroup)
                        {
                            uiGroup = MetaContactListSource
                                .getUIGroup(metaGroup);
                            if (uiGroup == null)
                                uiGroup = MetaContactListSource
                                    .createUIGroup(metaGroup);
                        }
                    }

                    if (logger.isDebugEnabled())
                        logger.debug("Presence filter contact added: "
                                + metaContact.getDisplayName());

                    UIContactImpl newUIContact;
                    synchronized (metaContact)
                    {
                        newUIContact
                            = MetaContactListSource.getUIContact(metaContact);

                        if (newUIContact == null)
                        {
                            newUIContact = MetaContactListSource
                                .createUIContact(metaContact);
                        }
                    }

                    GuiActivator.getContactList().addContact(
                        newUIContact,
                        uiGroup,
                        true,
                        true);

                    query.setInitialResultCount(resultCount);
                }
                else
                {
                    query.fireQueryEvent(metaContact);
                }
            }
        }

        // If in the meantime the filtering has been stopped we return here.
        if (query.isCanceled())
            return;

        Iterator<MetaContactGroup> subgroups = metaGroup.getSubgroups();
        while(subgroups.hasNext() && !query.isCanceled())
        {
            MetaContactGroup subgroup = subgroups.next();

            if (isMatching(subgroup))
            {
                UIGroup uiGroup;
                synchronized(subgroup)
                {
                    uiGroup = MetaContactListSource
                        .getUIGroup(subgroup);

                    if (uiGroup == null)
                        uiGroup = MetaContactListSource
                            .createUIGroup(subgroup);
                }

                GuiActivator.getContactList().addGroup(uiGroup, true);

                addMatching(subgroup, query, resultCount);
            }
        }
    }
}
