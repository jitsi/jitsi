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
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>PresenceFilter</tt> is used to filter offline contacts from the
 * contact list.
 *
 * @author Yana Stamcheva
 */
public class PresenceFilter
    implements  ContactListFilter
{
    /**
     * Indicates if this presence filter shows or hides the offline contacts.
     */
    private boolean isShowOffline;

    /**
     * Indicates if there's a presence filtering going on.
     */
    private boolean isFiltering = false;

    /**
     * Creates an instance of <tt>PresenceFilter</tt>.
     */
    public PresenceFilter()
    {
        this.setShowOffline(ConfigurationManager.isShowOffline());
    }

    /**
     * Applies this filter. This filter is applied over the
     * <tt>MetaContactListService</tt>.
     * @param treeModel the model to which we should add the results from the
     * filtering operation
     */
    public void applyFilter(ContactListTreeModel treeModel)
    {
        isFiltering = true;

        addMatching(GuiActivator.getContactListService().getRoot(), treeModel);

        isFiltering = false;
    }

    /**
     * Indicates if the given <tt>uiContact</tt> is matching this filter.
     * @param uiContact the <tt>UIContact</tt> to check
     * @return <tt>true</tt> if the given <tt>uiContact</tt> is matching
     * this filter, otherwise returns <tt>false</tt>
     */
    public boolean isMatching(UIContact uiContact)
    {
        Object descriptor = uiContact.getDescriptor();
        if (descriptor instanceof MetaContact)
            return isMatching((MetaContact) descriptor);

        return false;
    }

    /**
     * Indicates if the given <tt>uiGroup</tt> is matching this filter.
     * @param uiGroup the <tt>UIGroup</tt> to check
     * @return <tt>true</tt> if the given <tt>uiGroup</tt> is matching
     * this filter, otherwise returns <tt>false</tt>
     */
    public boolean isMatching(UIGroup uiGroup)
    {
        Object descriptor = uiGroup.getDescriptor();
        if (descriptor instanceof MetaContactGroup)
            return isMatching((MetaContactGroup) descriptor);

        return false;
    }

    /**
     * Sets the show offline property.
     * @param isShowOffline indicates if offline contacts are shown
     */
    public void setShowOffline(boolean isShowOffline)
    {
        this.isShowOffline = isShowOffline;
    }

    /**
     * Returns <tt>true</tt> if offline contacts are shown, otherwise returns
     * <tt>false</tt>.
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
     * @param metaContact the <tt>MetaContact</tt> to check
     * @return <tt>true</tt> if the given <tt>MetaContact</tt> is matching this
     * filter
     */
    private boolean isMatching(MetaContact metaContact)
    {
        return isShowOffline || isContactOnline(metaContact);
    }

    /**
     * Returns <tt>true</tt> if offline contacts are shown or if the given
     * <tt>MetaContactGroup</tt> contains online contacts.
     * @param metaGroup the <tt>MetaContactGroup</tt> to check
     * @return <tt>true</tt> if the given <tt>MetaContactGroup</tt> is matching
     * this filter
     */
    private boolean isMatching(MetaContactGroup metaGroup)
    {
        return (isShowOffline || metaGroup.countOnlineChildContacts() > 0)
                ? true
                : false;
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
     * @param metaGroup the <tt>MetaContactGroup</tt>, which matching contacts
     * to add
     * @param resultTreeModel the <tt>ContactListTreeModel</tt>, where results
     * should be added
     */
    private void addMatching(   MetaContactGroup metaGroup,
                                ContactListTreeModel resultTreeModel)
    {
        Iterator<MetaContact> childContacts = metaGroup.getChildContacts();

        while(childContacts.hasNext() && isFiltering)
        {
            MetaContact metaContact = childContacts.next();

            if(isMatching(metaContact))
            {
                MetaContactGroup parentGroup
                    = metaContact.getParentMetaContactGroup();

                UIGroup uiGroup = null;
                if (!MetaContactListSource.isRootGroup(parentGroup))
                {
                    uiGroup = MetaContactListSource
                        .getUIGroup(parentGroup);

                    if (uiGroup == null)
                        uiGroup = MetaContactListSource
                            .createUIGroup(parentGroup);
                }
 
                GuiActivator.getContactList().addContact(
                    resultTreeModel,
                    MetaContactListSource.createUIContact(metaContact),
                    uiGroup,
                    true,
                    false);
            }
        }

        Iterator<MetaContactGroup> subgroups = metaGroup.getSubgroups();
        while(subgroups.hasNext() && isFiltering)
        {
            MetaContactGroup subgroup = subgroups.next();

            if (subgroup.countChildContacts() == 0
                    && subgroup.countSubgroups() == 0
                    && isMatching(subgroup))
                GuiActivator.getContactList().addGroup(
                    resultTreeModel,
                    MetaContactListSource.createUIGroup(subgroup),
                    false);
            else
                addMatching(subgroup, resultTreeModel);
        }
    }

    /**
     * Stops this filter current queries.
     */
    public void stopFilter()
    {
        isFiltering = false;
    }
}
