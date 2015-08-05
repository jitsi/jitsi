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
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

import javax.swing.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.customcontactactions.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>MetaContactListSource</tt> is an abstraction of the
 * <tt>MetaContactListService</tt>, which makes the correspondence between a
 * <tt>MetaContact</tt> and an <tt>UIContact</tt> and between a
 * <tt>MetaContactGroup</tt> and an <tt>UIGroup</tt>. It is also responsible
 * for filtering of the <tt>MetaContactListService</tt> through a given pattern.
 *
 * @author Yana Stamcheva
 */
public class MetaContactListSource
    implements  ContactPresenceStatusListener,
                MetaContactListListener
{
    /**
     * The data key of the MetaContactDescriptor object used to store a
     * reference to this object in its corresponding MetaContact.
     */
    public static final String UI_CONTACT_DATA_KEY
        = MetaUIContact.class.getName() + ".uiContactDescriptor";

    /**
     * The data key of the MetaGroupDescriptor object used to store a
     * reference to this object in its corresponding MetaContactGroup.
     */
    public static final String UI_GROUP_DATA_KEY
        = MetaUIGroup.class.getName() + ".uiGroupDescriptor";

    /**
     * The data key of the MetaGroupDescriptor object used to store whether
     * the group is newly created by user.
     */
    private static final String GROUP_IS_NEW_KEY
        = MetaUIGroup.class.getName() + ".groupIsNew";

    /**
     * The initial result count below which we insert all filter results
     * directly to the contact list without firing events.
     */
    private static final int INITIAL_CONTACT_COUNT = 30;

    /**
     * The list of action buttons for this meta contact.
     */
    private static Map<ContactAction<Contact>, SIPCommButton>
                                                            customActionButtons;

    /**
     * Currently selected custom action contact.
     */
    private static MetaUIContact customActionContact;

    /**
     * The current index.
     */
    private int index = 0;

    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(MetaContactListSource.class);

    /**
     * Returns the <tt>UIContact</tt> corresponding to the given
     * <tt>MetaContact</tt>.
     * @param metaContact the <tt>MetaContact</tt>, which corresponding UI
     * contact we're looking for
     * @return the <tt>UIContact</tt> corresponding to the given
     * <tt>MetaContact</tt>
     */
    public static UIContactImpl getUIContact(MetaContact metaContact)
    {
        return (UIContactImpl) metaContact.getData(UI_CONTACT_DATA_KEY);
    }

    /**
     * Returns the <tt>UIGroup</tt> corresponding to the given
     * <tt>MetaContactGroup</tt>.
     * @param metaGroup the <tt>MetaContactGroup</tt>, which UI group we're
     * looking for
     * @return the <tt>UIGroupImpl</tt> corresponding to the given
     * <tt>MetaContactGroup</tt>
     */
    public static UIGroupImpl getUIGroup(MetaContactGroup metaGroup)
    {
        return (UIGroupImpl) metaGroup.getData(UI_GROUP_DATA_KEY);
    }

    /**
     * Returns whether <tt>MetaContactGroup</tt> is newly created.
     * @param metaGroup the <tt>MetaContactGroup</tt>, which we will check.
     * @return whether the <tt>MetaContactGroup</tt> is newly created.
     */
    public static boolean isNewGroup(MetaContactGroup metaGroup)
    {
        Object data = metaGroup.getData(GROUP_IS_NEW_KEY);
        return data == null ? false : (Boolean)data;
    }

    /**
     * Creates a <tt>UIContact</tt> for the given <tt>metaContact</tt>.
     * @param metaContact the <tt>MetaContact</tt> for which we would like to
     * create an <tt>UIContact</tt>
     * @return an <tt>UIContact</tt> for the given <tt>metaContact</tt>
     */
    public static UIContactImpl createUIContact(final MetaContact metaContact)
    {
        final MetaUIContact descriptor
            = new MetaUIContact(metaContact);
        metaContact.setData(UI_CONTACT_DATA_KEY, descriptor);

        return descriptor;
    }

    /**
     * Removes the <tt>UIContact</tt> from the given <tt>metaContact</tt>.
     * @param metaContact the <tt>MetaContact</tt>, which corresponding UI
     * contact we would like to remove
     */
    public static void removeUIContact(MetaContact metaContact)
    {
        metaContact.setData(UI_CONTACT_DATA_KEY, null);
    }

    /**
     * Creates a <tt>UIGroupDescriptor</tt> for the given <tt>metaGroup</tt>.
     * @param metaGroup the <tt>MetaContactGroup</tt> for which we would like to
     * create an <tt>UIContact</tt>
     * @return a <tt>UIGroup</tt> for the given <tt>metaGroup</tt>
     */
    public static UIGroup createUIGroup(MetaContactGroup metaGroup)
    {
        MetaUIGroup descriptor = new MetaUIGroup(metaGroup);
        metaGroup.setData(UI_GROUP_DATA_KEY, descriptor);

        return descriptor;
    }

    /**
     * Removes the descriptor from the given <tt>metaGroup</tt>.
     * @param metaGroup the <tt>MetaContactGroup</tt>, which descriptor we
     * would like to remove
     */
    public static void removeUIGroup(
        MetaContactGroup metaGroup)
    {
        metaGroup.setData(UI_GROUP_DATA_KEY, null);
    }

    /**
     * Indicates if the given <tt>MetaContactGroup</tt> is the root group.
     * @param group the <tt>MetaContactGroup</tt> to check
     * @return <tt>true</tt> if the given <tt>group</tt> is the root group,
     * <tt>false</tt> - otherwise
     */
    public static boolean isRootGroup(MetaContactGroup group)
    {
        return group.equals(GuiActivator.getContactListService().getRoot());
    }

    /**
     * Starts the query.
     * 
     * @param filterPattern the pattern to filter through
     * @param query the query to be started
     */
    public void startQuery(final MetaContactQuery query,
        final Pattern filterPattern)
    {
        new Thread()
        {
            @Override
            public void run()
            {
                int resultCount = 0;
                queryMetaContactSource( filterPattern,
                        GuiActivator.getContactListService().getRoot(),
                        query,
                        resultCount);

                if (!query.isCanceled())
                    query.fireQueryEvent(
                        MetaContactQueryStatusEvent.QUERY_COMPLETED);
                else
                    query.fireQueryEvent(
                        MetaContactQueryStatusEvent.QUERY_CANCELED);
            }
        }.start();
    }

    /**
     * Filters the children in the given <tt>MetaContactGroup</tt> to match the
     * given <tt>filterPattern</tt> and stores the result in the given
     * <tt>treeModel</tt>.
     * @param filterPattern the pattern to filter through
     * @param parentGroup the <tt>MetaContactGroup</tt> to filter
     * @param query the object that tracks the query
     * @param resultCount the initial result count we would insert directly to
     * the contact list without firing events
     */
    public void queryMetaContactSource(Pattern filterPattern,
                                        MetaContactGroup parentGroup,
                                        MetaContactQuery query,
                                        int resultCount)
    {
        Iterator<MetaContact> childContacts = parentGroup.getChildContacts();

        while (childContacts.hasNext() && !query.isCanceled())
        {
            MetaContact metaContact = childContacts.next();

            if (isMatching(filterPattern, metaContact))
            {
                resultCount++;

                if (resultCount <= INITIAL_CONTACT_COUNT)
                {
                    UIGroup uiGroup = null;
                    if (!MetaContactListSource.isRootGroup(parentGroup))
                    {
                        synchronized (parentGroup)
                        {
                            uiGroup = MetaContactListSource
                                .getUIGroup(parentGroup);
                            if (uiGroup == null)
                                uiGroup = MetaContactListSource
                                    .createUIGroup(parentGroup);
                        }
                    }

                    UIContact newUIContact;
                    synchronized (metaContact)
                    {
                        newUIContact 
                            = MetaContactListSource.getUIContact(metaContact);
    
                        if (newUIContact == null)
                        {
                            newUIContact
                                = MetaContactListSource
                                    .createUIContact(metaContact);
                        }
                        
                        GuiActivator.getContactList().addContact(
                            newUIContact,
                            uiGroup,
                            true,
                            true);
                    }
                    
                    query.setInitialResultCount(resultCount);
                }
                else
                {
                    query.fireQueryEvent(metaContact);
                }
            }
        }

        // If in the meantime the query is canceled we return here.
        if(query.isCanceled())
            return;

        Iterator<MetaContactGroup> subgroups = parentGroup.getSubgroups();
        while (subgroups.hasNext() && !query.isCanceled())
        {
            MetaContactGroup subgroup = subgroups.next();

            queryMetaContactSource(filterPattern, subgroup, query, resultCount);
        }
    }

    /**
     * Checks if the given <tt>metaContact</tt> is matching the given
     * <tt>filterPattern</tt>.
     * A <tt>MetaContact</tt> would be matching the filter if one of the
     * following is true:<br>
     * - its display name contains the filter string
     * - at least one of its child protocol contacts has a display name or an
     * address that contains the filter string.
     * @param filterPattern the filter pattern to check for matches
     * @param metaContact the <tt>MetaContact</tt> to check
     * @return <tt>true</tt> to indicate that the given <tt>metaContact</tt> is
     * matching the current filter, otherwise returns <tt>false</tt>
     */
    private boolean isMatching(Pattern filterPattern, MetaContact metaContact)
    {
        Matcher matcher = filterPattern.matcher(metaContact.getDisplayName());

        if(matcher.find())
            return true;

        Iterator<Contact> contacts = metaContact.getContacts();
        while (contacts.hasNext())
        {
            Contact contact = contacts.next();

            matcher = filterPattern.matcher(contact.getDisplayName());

            if (matcher.find())
                return true;

            matcher = filterPattern.matcher(contact.getAddress());

            if (matcher.find())
                return true;
        }
        return false;
    }

    /**
     * Checks if the given <tt>metaGroup</tt> is matching the current filter. A
     * group is matching the current filter only if it contains at least one
     * child <tt>MetaContact</tt>, which is matching the current filter.
     * @param filterPattern the filter pattern to check for matches
     * @param metaGroup the <tt>MetaContactGroup</tt> to check
     * @return <tt>true</tt> to indicate that the given <tt>metaGroup</tt> is
     * matching the current filter, otherwise returns <tt>false</tt>
     */
    public boolean isMatching(Pattern filterPattern, MetaContactGroup metaGroup)
    {
        Iterator<MetaContact> contacts = metaGroup.getChildContacts();

        while (contacts.hasNext())
        {
            MetaContact metaContact = contacts.next();

            if (isMatching(filterPattern, metaContact))
                return true;
        }
        return false;
    }

    public void contactPresenceStatusChanged(
        ContactPresenceStatusChangeEvent evt)
    {
        if (evt.getOldStatus() == evt.getNewStatus()
            && !evt.isResourceChanged())
        {
            return;
        }

        final Contact sourceContact = evt.getSourceContact();
        final MetaContact metaContact
            = GuiActivator.getContactListService().findMetaContactByContact(
                    sourceContact);

        if (metaContact == null)
            return;

        boolean uiContactCreated = false;

        UIContactImpl uiContact;

        synchronized (metaContact)
        {
            uiContact = getUIContact(metaContact);

            if (uiContact == null)
            {
                uiContact = createUIContact(metaContact);
                uiContactCreated = true;
            }
        }

        ContactListFilter currentFilter
            = GuiActivator.getContactList().getCurrentFilter();

        if (uiContactCreated)
        {
            if (currentFilter != null && currentFilter.isMatching(uiContact))
            {
                MetaContactGroup parentGroup
                    = metaContact.getParentMetaContactGroup();

                UIGroup uiGroup = null;

                if (!MetaContactListSource.isRootGroup(parentGroup))
                {
                    synchronized (parentGroup)
                    {
                        uiGroup = MetaContactListSource.getUIGroup(parentGroup);
                        if (uiGroup == null)
                            uiGroup = MetaContactListSource
                                .createUIGroup(parentGroup);
                    }
                }

                if (logger.isDebugEnabled())
                    logger.debug(
                        "Add matching contact due to status change: "
                        + uiContact.getDisplayName());

                GuiActivator.getContactList()
                    .addContact(uiContact, uiGroup, true, true);
            }
            else
                removeUIContact(metaContact);
        }
        else
        {
            if (currentFilter != null
                && !currentFilter.isMatching(uiContact))
            {
                if (logger.isDebugEnabled())
                    logger.debug(
                        "Remove unmatching contact due to status change: "
                        + uiContact.getDisplayName());
                GuiActivator.getContactList().removeContact(uiContact);
            }
            else
            {
                synchronized (uiContact)
                {
                    GuiActivator.getContactList()
                        .nodeChanged(uiContact.getContactNode());
                }
            }

        }
    }

    /**
     * Reorders contact list nodes, when <tt>MetaContact</tt>-s in a
     * <tt>MetaContactGroup</tt> has been reordered.
     * @param evt the <tt>MetaContactGroupEvent</tt> that notified us
     */
    public void childContactsReordered(MetaContactGroupEvent evt)
    {
        MetaContactGroup metaGroup = evt.getSourceMetaContactGroup();
        UIGroupImpl uiGroup;

        ContactListTreeModel treeModel
            = GuiActivator.getContactList().getTreeModel();

        synchronized (metaGroup)
        {
            uiGroup = MetaContactListSource.getUIGroup(metaGroup);
            if (isRootGroup(metaGroup) && uiGroup == null)
              uiGroup = treeModel.getRoot().getGroupDescriptor();
        }

        if (uiGroup != null)
        {
            synchronized (uiGroup)
            {
                GroupNode groupNode = uiGroup.getGroupNode();

                if (groupNode != null)
                    groupNode.sort(treeModel);
            }
           
        }
    }

    /**
     * Adds a node in the contact list, when a <tt>MetaContact</tt> has been
     * added in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactAdded(final MetaContactEvent evt)
    {
        metaContactAdded(evt.getSourceMetaContact(),
                        evt.getParentGroup());
    }

    /**
     * Adds a node in the contact list, when a <tt>MetaContact</tt> has been
     * added in the <tt>MetaContactListService</tt>.
     * @param metaContact to add to the contact list.
     * @param parentGroup the group we add in.
     */
    private void metaContactAdded(final MetaContact metaContact,
                                 final MetaContactGroup parentGroup)
    {
        UIContactImpl uiContact;

        synchronized (metaContact)
        {
            uiContact = MetaContactListSource.getUIContact(metaContact);

            // If there's already an UIContact for this meta contact, we have
            // nothing to do here.
            if (uiContact != null)
                return;

            uiContact = MetaContactListSource.createUIContact(metaContact);
        }

        ContactListFilter currentFilter
            = GuiActivator.getContactList().getCurrentFilter();

        if (currentFilter.isMatching(uiContact))
        {
            UIGroup uiGroup = null;
            
            if (!MetaContactListSource.isRootGroup(parentGroup))
            {
                uiGroup = MetaContactListSource.getUIGroup(parentGroup);
                synchronized (parentGroup)
                {
                    if (uiGroup == null)
                        uiGroup = MetaContactListSource
                            .createUIGroup(parentGroup);
                }
            }

            GuiActivator.getContactList()
                .addContact(uiContact, uiGroup, true, true);
        }
        else
            MetaContactListSource.removeUIContact(metaContact);
    }

    /**
     * Adds a group node in the contact list, when a <tt>MetaContactGroup</tt>
     * has been added in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactGroupEvent</tt> that notified us
     */
    public void metaContactGroupAdded(MetaContactGroupEvent evt)
    {
        final MetaContactGroup metaGroup = evt.getSourceMetaContactGroup();

        UIGroup uiGroup;

        synchronized (metaGroup)
        {
            uiGroup = MetaContactListSource.getUIGroup(metaGroup);

            // If there's already an UIGroup for this meta contact, we have
            // nothing to do here.
            if (uiGroup != null)
                return;

            uiGroup = MetaContactListSource.createUIGroup(metaGroup);
        }

        // mark group as newly created
        if(metaGroup.countChildContacts() == 0)
        {
            metaGroup.setData(GROUP_IS_NEW_KEY, Boolean.TRUE);
        }

        ContactListFilter currentFilter
            = GuiActivator.getContactList().getCurrentFilter();

        if (currentFilter.isMatching(uiGroup))
            GuiActivator.getContactList().addGroup(uiGroup, true);
        else
            MetaContactListSource.removeUIGroup(metaGroup);

        // iterate over the contacts, they may need to be displayed
        // some protocols fire events for adding contacts after firing
        // that group has been created and this is not needed, but some don't
        Iterator<MetaContact> iterContacts = metaGroup.getChildContacts();
        while(iterContacts.hasNext())
        {
            metaContactAdded(iterContacts.next(), metaGroup);
        }
    }

    /**
     * Notifies the tree model, when a <tt>MetaContactGroup</tt> has been
     * modified in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactGroupEvent</tt> that notified us
     */
    public void metaContactGroupModified(MetaContactGroupEvent evt)
    {
        final MetaContactGroup metaGroup = evt.getSourceMetaContactGroup();

        UIGroupImpl uiGroup;
        synchronized (metaGroup)
        {
            uiGroup = MetaContactListSource.getUIGroup(metaGroup);
        }

        if (uiGroup != null)
        {
            synchronized (uiGroup)
            {
                GroupNode groupNode = uiGroup.getGroupNode();

                if (groupNode != null)
                    GuiActivator.getContactList().getTreeModel()
                        .nodeChanged(groupNode); 
            }
            
        }
    }

    /**
     * Removes the corresponding group node in the contact list, when a
     * <tt>MetaContactGroup</tt> has been removed from the
     * <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactGroupEvent</tt> that notified us
     */
    public void metaContactGroupRemoved(final MetaContactGroupEvent evt)
    {
        MetaContactGroup metaGroup = evt.getSourceMetaContactGroup();

        UIGroup uiGroup;
        synchronized (metaGroup)
        {
            uiGroup = MetaContactListSource.getUIGroup(metaGroup);
        }

        if (uiGroup != null)
            GuiActivator.getContactList().removeGroup(uiGroup);
    }

    /**
     * Notifies the tree model, when a <tt>MetaContact</tt> has been
     * modified in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactModified(final MetaContactModifiedEvent evt)
    {
        MetaContact metaContact = evt.getSourceMetaContact();

        UIContactImpl uiContact;
        synchronized (metaContact)
        {
            uiContact = MetaContactListSource.getUIContact(metaContact);
            if (uiContact == null)
                return;
        }

        synchronized (uiContact)
        {
            ContactNode contactNode
                = uiContact.getContactNode();

            if (contactNode != null)
                GuiActivator.getContactList().nodeChanged(contactNode);
        }
    }

    /**
     * Performs needed operations, when a <tt>MetaContact</tt> has been
     * moved in the <tt>MetaContactListService</tt> from one group to another.
     * @param evt the <tt>MetaContactMovedEvent</tt> that notified us
     */
    public void metaContactMoved(final MetaContactMovedEvent evt)
    {
        // fixes an issue with moving meta contacts where removeContact
        // will set data to null in swing thread and it will be after we have
        // set the data here, so we also move this set to the swing thread
        // to order the calls of setData.
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    metaContactMoved(evt);
                }
            });
            return;
        }

        final MetaContact metaContact = evt.getSourceMetaContact();
        final MetaContactGroup oldParent = evt.getOldParent();
        final MetaContactGroup newParent = evt.getNewParent();

        synchronized (metaContact)
        {
            UIContactImpl uiContact
                = MetaContactListSource.getUIContact(metaContact);

            if (uiContact == null)
                return;

            UIGroup oldUIGroup;

            if (MetaContactListSource.isRootGroup(oldParent))
                oldUIGroup = GuiActivator.getContactList().getTreeModel()
                    .getRoot().getGroupDescriptor();
            else
                synchronized (oldParent)
                {
                    oldUIGroup = MetaContactListSource.getUIGroup(oldParent);
                }

            if (oldUIGroup != null)
                GuiActivator.getContactList().removeContact(uiContact);

            // Add the contact to the new place.
            uiContact = MetaContactListSource.createUIContact(metaContact);
            

            UIGroup newUIGroup = null;
            
            if (!MetaContactListSource.isRootGroup(newParent))
            {
                synchronized (newParent)
                {
                    newUIGroup = MetaContactListSource.getUIGroup(newParent);
                    
                    if (newUIGroup == null)
                        newUIGroup
                            = MetaContactListSource.createUIGroup(newParent);
                }
            }

            ContactListFilter currentFilter
                = GuiActivator.getContactList().getCurrentFilter();

            synchronized (uiContact)
            {
                if (currentFilter.isMatching(uiContact) 
                    && uiContact.getContactNode() == null)
                    GuiActivator.getContactList()
                        .addContact(uiContact, newUIGroup, true, true);
                else
                    MetaContactListSource.removeUIContact(metaContact);
            }
        }
    }

    /**
     * Removes the corresponding contact node in the contact list, when a
     * <tt>MetaContact</tt> has been removed from the
     * <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactRemoved(final MetaContactEvent evt)
    {
        MetaContact metaContact = evt.getSourceMetaContact();

        UIContact uiContact;
        synchronized (metaContact)
        {
            uiContact
                = MetaContactListSource.getUIContact(metaContact);
        }

        if (uiContact != null)
            GuiActivator.getContactList().removeContact(uiContact);
    }

    /**
     * Refreshes the corresponding node, when a <tt>MetaContact</tt> has been
     * renamed in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactRenamedEvent</tt> that notified us
     */
    public void metaContactRenamed(final MetaContactRenamedEvent evt)
    {
        MetaContact metaContact = evt.getSourceMetaContact();

        UIContactImpl uiContact;
        synchronized (metaContact)
        {
            uiContact = MetaContactListSource.getUIContact(metaContact);
            if (uiContact == null)
                return;
        }

        synchronized (uiContact)
        {
            ContactNode contactNode = uiContact.getContactNode();

            if (contactNode != null)
                GuiActivator.getContactList().nodeChanged(contactNode);
        }
    }

    /**
     * Notifies the tree model, when the <tt>MetaContact</tt> avatar has been
     * modified in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactAvatarUpdated(final MetaContactAvatarUpdateEvent evt)
    {
        MetaContact metaContact = evt.getSourceMetaContact();

        UIContactImpl uiContact;
        synchronized (metaContact)
        {
            uiContact = MetaContactListSource.getUIContact(metaContact);
        }

        if (uiContact != null)
        {
            synchronized (uiContact)
            {
                ContactNode contactNode = uiContact.getContactNode();

                if (contactNode != null)
                    GuiActivator.getContactList().nodeChanged(contactNode);
            }
            
        }
    }

    /**
     * Adds a contact node corresponding to the parent <tt>MetaContact</tt> if
     * this last is matching the current filter and wasn't previously contained
     * in the contact list.
     * @param evt the <tt>ProtoContactEvent</tt> that notified us
     */
    public void protoContactAdded(ProtoContactEvent evt)
    {
        final MetaContact metaContact = evt.getNewParent();

        UIContact parentUIContact;
        boolean parentUIContactCreated = false;
        synchronized (metaContact)
        {
            parentUIContact = MetaContactListSource.getUIContact(metaContact);

            if (parentUIContact == null)
            {
                parentUIContactCreated = true;
                parentUIContact
                    = MetaContactListSource.createUIContact(metaContact);
            }
        }

        if (parentUIContact != null && parentUIContactCreated)
        {
            ContactListFilter currentFilter
                = GuiActivator.getContactList().getCurrentFilter();
            
            if (currentFilter.isMatching(parentUIContact))
            {
                MetaContactGroup parentGroup
                    = metaContact.getParentMetaContactGroup();

                UIGroup uiGroup = null;
                
                if (!MetaContactListSource
                        .isRootGroup(parentGroup))
                {
                    synchronized(parentGroup)
                    {
                        uiGroup = MetaContactListSource
                            .getUIGroup(parentGroup);
                        if (uiGroup == null)
                            uiGroup = MetaContactListSource
                                .createUIGroup(parentGroup);
                    }
                }
                
                GuiActivator.getContactList()
                    .addContact(parentUIContact, uiGroup, true, true);
            }
            else
                MetaContactListSource.removeUIContact(metaContact);
        }
    }

    /**
     * Notifies the UI representation of the parent <tt>MetaContact</tt> that
     * this contact has been modified.
     *
     * @param evt the <tt>ProtoContactEvent</tt> that notified us
     */
    public void protoContactModified(ProtoContactEvent evt)
    {
        MetaContact metaContact = evt.getNewParent();

        UIContactImpl uiContact;
        synchronized (metaContact)
        {
            uiContact = MetaContactListSource.getUIContact(metaContact);
            if (uiContact == null)
                return;
        }

        synchronized (uiContact)
        {
            ContactNode contactNode = uiContact.getContactNode();

            if (contactNode != null)
                GuiActivator.getContactList().nodeChanged(contactNode);
        }
    }

    /**
     * Adds the new <tt>MetaContact</tt> parent and removes the old one if the
     * first is matching the current filter and the last is no longer matching
     * it.
     * @param evt the <tt>ProtoContactEvent</tt> that notified us
     */
    public void protoContactMoved(ProtoContactEvent evt)
    {
        final MetaContact oldParent = evt.getOldParent();
        final MetaContact newParent = evt.getNewParent();

        UIContact oldUIContact;
        synchronized (oldParent)
        {
            oldUIContact = MetaContactListSource.getUIContact(oldParent);
        }

        // Remove old parent if not matching.
        if (oldUIContact != null
            && !GuiActivator.getContactList().getCurrentFilter()
                .isMatching(oldUIContact))
        {
            GuiActivator.getContactList().removeContact(oldUIContact);
        }

        UIContact newUIContact;
        boolean newUIContactCreated = false;
        synchronized (newParent)
        {
            // Add new parent if matching.
            newUIContact = MetaContactListSource.getUIContact(newParent);

            if (newUIContact == null)
            {
                newUIContactCreated = true;
                newUIContact
                    = MetaContactListSource.createUIContact(newParent);
            }
        }

        // if the contact is not created already created, we are just merging
        // don't do anything
        if (newUIContact != null && newUIContactCreated)
        {
            if (GuiActivator.getContactList().getCurrentFilter()
                    .isMatching(newUIContact))
            {
                MetaContactGroup parentGroup
                    = newParent.getParentMetaContactGroup();

                UIGroup uiGroup = null;
                
                if (!MetaContactListSource
                        .isRootGroup(parentGroup))
                {
                    uiGroup = MetaContactListSource
                        .getUIGroup(parentGroup);
                    synchronized (parentGroup)
                    {
                        if (uiGroup == null)
                            uiGroup = MetaContactListSource
                                .createUIGroup(parentGroup);
                    }
                }

                GuiActivator.getContactList()
                    .addContact(newUIContact, uiGroup, true, true);
            }
            else
                MetaContactListSource.removeUIContact(newParent);
        }
    }

    /**
     * Removes the contact node corresponding to the parent
     * <tt>MetaContact</tt> if the last is no longer matching the current filter
     * and wasn't previously contained in the contact list.
     * @param evt the <tt>ProtoContactEvent</tt> that notified us
     */
    public void protoContactRemoved(ProtoContactEvent evt)
    {
        final MetaContact oldParent = evt.getOldParent();

        UIContactImpl oldUIContact;
        synchronized (oldParent)
        {
            oldUIContact = MetaContactListSource.getUIContact(oldParent);
            if (oldUIContact == null)
                return;
        }

        synchronized (oldUIContact)
        {
            ContactNode contactNode = oldUIContact.getContactNode();

            if (contactNode != null)
                GuiActivator.getContactList().nodeChanged(contactNode);
        }
    }

    /**
     * Returns all custom action buttons for this meta contact.
     *
     * @return a list of all custom action buttons for this meta contact
     */
    public static Collection<SIPCommButton> getContactCustomActionButtons(
            final MetaUIContact metaContact)
    {
        customActionContact = metaContact;

        if (customActionButtons == null)
            initCustomActionButtons();

        Iterator<ContactAction<Contact>> customActionsIter
            = customActionButtons.keySet().iterator();

        Collection<SIPCommButton> availableCustomActionButtons
            = new LinkedList<SIPCommButton>();

        while (customActionsIter.hasNext())
        {
            ContactAction<Contact> contactAction = customActionsIter.next();
            SIPCommButton actionButton = customActionButtons.get(contactAction);

            if (isContactActionVisible(contactAction,
                (MetaContact) metaContact.getDescriptor()))
            {
                availableCustomActionButtons.add(actionButton);
            }
        }

        return availableCustomActionButtons;
    }

    /**
     * Indicates if the given <tt>ContactAction</tt> should be visible for the
     * given <tt>MetaContact</tt>.
     *
     * @param contactAction the <tt>ContactAction</tt> to verify
     * @param metaContact the <tt>MetaContact</tt> for which we verify if the
     * given action should be visible
     * @return <tt>true</tt> if the given <tt>ContactAction</tt> is visible for
     * the given <tt>MetaContact</tt>, <tt>false</tt> - otherwise
     */
    private static boolean isContactActionVisible(
                                            ContactAction<Contact> contactAction,
                                            MetaContact metaContact)
    {
        Iterator<Contact> contactDetails = metaContact.getContacts();

        while (contactDetails.hasNext())
        {
            if (contactAction.isVisible(contactDetails.next()))
                return true;
        }

        return false;
    }

    /**
     * Initializes custom action buttons for this contact source.
     */
    private static void initCustomActionButtons()
    {
        customActionButtons
                = new LinkedHashMap<ContactAction<Contact>, SIPCommButton>();

        for (CustomContactActionsService<Contact> ccas
                : getContactActionsServices())
        {
            Iterator<ContactAction<Contact>> actionIterator
                = ccas.getCustomContactActions();

            while (actionIterator!= null && actionIterator.hasNext())
            {
                final ContactAction<Contact> ca = actionIterator.next();

                SIPCommButton actionButton = customActionButtons.get(ca);

                if (actionButton == null)
                {
                    actionButton = new SIPCommButton();

                    actionButton.setToolTipText(ca.getToolTipText());

                    actionButton.setIconImage(
                        new ImageIcon(ca.getIcon()).getImage());
                    actionButton.setRolloverIcon(
                        new ImageIcon(ca.getRolloverIcon()).getImage());
                    actionButton.setPressedIcon(
                        new ImageIcon(ca.getPressedIcon()).getImage());

                    actionButton.addActionListener(new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            List<UIContactDetail> contactDetails
                                = customActionContact.getContactDetails();

                            UIContactDetailCustomAction contactAction
                                = new UIContactDetailCustomAction(ca);

                            if (contactDetails.size() > 1)
                            {
                                ChooseUIContactDetailPopupMenu
                                detailsPopupMenu
                                    = new ChooseUIContactDetailPopupMenu(
                                        (JButton) e.getSource(),
                                        customActionContact.getContactDetails(),
                                        contactAction);

                                detailsPopupMenu.showPopupMenu();
                            }
                            else if (contactDetails.size() == 1)
                            {
                                JButton button = (JButton) e.getSource();
                                Point location = new Point(button.getX(),
                                    button.getY() + button.getHeight());

                                SwingUtilities.convertPointToScreen(
                                    location, GuiActivator.getContactList());

                                location.y = location.y
                                    + GuiActivator.getContactList()
                                        .getPathBounds(
                                            GuiActivator.getContactList()
                                            .getSelectionPath()).y;

                                contactAction.actionPerformed(
                                    contactDetails.get(0),
                                    location.x,
                                    location.y);
                            }
                        }
                    });

                    customActionButtons.put(ca, actionButton);
                }
            }
        }
    }

    /**
     * Returns the index of the contact source in the result list.
     *
     * @return the index of the contact source in the result list
     */
    public int getIndex()
    {
        return index;
    }

    /**
     * Changes the index.
     * @param index the new value.
     */
    public void setIndex(int index)
    {
        this.index = index;
    }

    /**
     * An implementation of <tt>UIContactDetail</tt> for a custom action.
     */
    private static class UIContactDetailCustomAction
        implements UIContactDetailAction
    {
        /**
         * The contact action.
         */
        private final ContactAction<Contact> contactAction;

        /**
         * Creates an instance of <tt>UIContactDetailCustomAction</tt>.
         *
         * @param contactAction the contact action this detail is about
         */
        public UIContactDetailCustomAction(ContactAction<Contact> contactAction)
        {
            this.contactAction = contactAction;
        }

        /**
         * Performs the contact action on button click.
         */
        public void actionPerformed(UIContactDetail contactDetail,
                                    int x,
                                    int y)
        {
            try
            {
                contactAction.actionPerformed(
                    (Contact) contactDetail.getDescriptor(), x, y);
            }
            catch (OperationFailedException e)
            {
                new ErrorDialog(null,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.ERROR"),
                    e.getMessage());
            }
        }
    }

    /**
     * Returns a list of all custom contact action services.
     *
     * @return a list of all custom contact action services.
     */
    @SuppressWarnings ("unchecked")
    private static List<CustomContactActionsService<Contact>>
        getContactActionsServices()
    {
        List<CustomContactActionsService<Contact>> contactActionsServices
            = new ArrayList<CustomContactActionsService<Contact>>();

        ServiceReference[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs
                = GuiActivator.bundleContext.getServiceReferences(
                    CustomContactActionsService.class.getName(), null);
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("GuiActivator : " + e);
        }

        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                CustomContactActionsService<?> customActionService
                    = (CustomContactActionsService<?>)
                            GuiActivator.bundleContext.getService(serRef);

                if (customActionService.getContactSourceClass()
                        .equals(Contact.class))
                {
                    contactActionsServices.add(
                        (CustomContactActionsService<Contact>)
                            customActionService);
                }
            }
        }
        return contactActionsServices;
    }
}
