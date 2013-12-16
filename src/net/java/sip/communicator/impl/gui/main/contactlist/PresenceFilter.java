/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
     * Preferences for the external contact sources. Lists the type of contact 
     * contact sources that will be displayed in the filter and the order of the 
     * contact sources.
     */
    private static Map<Integer, Integer> contactSourcePreferences 
        = new HashMap<Integer, Integer>();

    /**
     * Creates an instance of <tt>PresenceFilter</tt>.
     */
    public PresenceFilter()
    {
        isShowOffline = ConfigurationUtils.isShowOffline();
        initContactSourcePreferences();
    }

    /**
     * Initializes the contact source preferences. The preferences are for the 
     * visibility of the contact source and their order.
     */
    private void initContactSourcePreferences()
    {
        //This entry will be used to set the index for chat room contact sources
        //The index is used to order the contact sources in the contact list.
        //The chat room sources will be ordered before the meta contact list.
        contactSourcePreferences.put(ContactSourceService.CHAT_ROOM_TYPE, 0);
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

        List<ContactQuery> contactQueryList = new ArrayList<ContactQuery>();
        
        for(int cssType : contactSourcePreferences.keySet())
        {
            Iterator<UIContactSource> filterSources 
                = GuiActivator.getContactList().getContactSources(cssType)
                    .iterator();
    
            while (filterSources.hasNext())
            {
                UIContactSource filterSource = filterSources.next();
                
                Integer prefValue = contactSourcePreferences.get(cssType);
                //We are setting the index from contactSourcePreferences map to 
                //the contact source. This index is set to reorder the sources 
                //in the contact list.
                if(prefValue != null)
                    filterSource.setContactSourceIndex(prefValue);
                
                ContactSourceService sourceService
                    = filterSource.getContactSourceService();
                ContactQuery contactQuery = sourceService.queryContactSource(null);
    
                contactQueryList.add(contactQuery);
    
                // Add this query to the filterQuery.
                filterQuery.addContactQuery(contactQuery);
            }
        }

        // Closes this filter to indicate that we finished adding queries to it.
        filterQuery.close();

        query.addContactQueryListener(GuiActivator.getContactList());

        int resultCount = 0;
        addMatching(GuiActivator.getContactListService().getRoot(),
                    query,
                    resultCount);

        for(ContactQuery contactQuery : contactQueryList)
        {
            for(SourceContact contact : contactQuery.getQueryResults())
            {
                addSourceContact(contact);
            }
            contactQuery.addContactQueryListener(GuiActivator.getContactList());
        }

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
     * @param metaContact the <tt>MetaContact</tt> to check
     * @return <tt>true</tt> if the given <tt>MetaContact</tt> is matching this
     * filter
     */
    public boolean isMatching(SourceContact contact)
    {
        return
            isShowOffline
                || contact.getPresenceStatus().isOnline()
                || (contact.getPreferredContactDetail(OperationSetMultiUserChat.class)
                        != null);
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

                    UIContact newUIContact;
                    synchronized (metaContact)
                    {
                        newUIContact = MetaContactListSource
                            .createUIContact(metaContact);
                    }

                    GuiActivator.getContactList().addContact(
                            newUIContact,
                            uiGroup,
                            true,
                            true);

                    query.setInitialResultCount(resultCount);
                }
                else
                    query.fireQueryEvent(metaContact);
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

    /**
     * Adds the given <tt>sourceContact</tt> to the contact list.
     * @param sourceContact the <tt>SourceContact</tt> to add
     */
    private void addSourceContact(SourceContact sourceContact)
    {
        ContactSourceService contactSource
            = sourceContact.getContactSource();

        TreeContactList sourceContactList = GuiActivator.getContactList();
        UIContactSource sourceUI
            = sourceContactList .getContactSource(contactSource);

        if (sourceUI != null
            // ExtendedContactSourceService has already matched the
            // SourceContact over the pattern
            && (contactSource instanceof ExtendedContactSourceService)
                || isMatching(sourceContact))
        {
            boolean isSorted = (sourceContact.getIndex() > -1) ? true : false;

            sourceContactList.addContact(
                sourceUI.createUIContact(sourceContact),
                sourceUI.getUIGroup(),
                isSorted,
                true);
        }
        else
            sourceUI.removeUIContact(sourceContact);
    }
}
