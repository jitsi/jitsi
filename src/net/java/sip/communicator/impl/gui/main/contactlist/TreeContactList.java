/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>TreeContactList</tt> is a contact list based on JTree.
 *
 * @author Yana Stamcheva
 */
public class TreeContactList
    extends DefaultTreeContactList
    implements  MetaContactListListener,
                ContactPresenceStatusListener,
                ContactQueryListener,
                MetaContactQueryListener,
                MouseListener,
                MouseMotionListener,
                TreeExpansionListener
{
    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(TreeContactList.class);

    /**
     * The default tree model.
     */
    private ContactListTreeModel treeModel;

    /**
     * The right button menu.
     */
    private JPopupMenu rightButtonMenu;

    /**
     * A list of all contacts that are currently "active". An "active" contact
     * is a contact that has been sent a message. The list is used to indicate
     * these contacts with a special icon.
     */
    private final java.util.List<ContactNode> activeContacts
        = new Vector<ContactNode>();

    /**
     * A list of all registered <tt>ContactListListener</tt>-s.
     */
    private final java.util.List<ContactListListener> contactListListeners
        = new Vector<ContactListListener>();

    /**
     * The presence filter.
     */
    public static final PresenceFilter presenceFilter = new PresenceFilter();

    /**
     * The search filter.
     */
    public static final SearchFilter searchFilter = new SearchFilter();

    /**
     * The call history filter.
     */
    public static final CallHistoryFilter historyFilter
        = new CallHistoryFilter();

    /**
     * The default filter is initially set to the PresenceFilter. But anyone
     * could change it by calling setDefaultFilter().
     */
    private ContactListFilter defaultFilter = presenceFilter;

    /**
     * The current filter.
     */
    private ContactListFilter currentFilter;

    /**
     * Indicates if the click on a group node has been already consumed. This
     * could happen in a move contact mode.
     */
    private boolean isGroupClickConsumed = false;

    /**
     * A list of all originally registered <tt>MouseListener</tt>-s.
     */
    private MouseListener[] originalMouseListeners;

    private static final Collection<ExternalContactSource> contactSources
        = new LinkedList<ExternalContactSource>();

    /**
     * The filter query used to track advanced source filtering.
     */
    FilterQuery filterQuery;

    /**
     * Creates the <tt>TreeContactList</tt>.
     */
    public TreeContactList()
    {
        // Remove default mouse listeners and keep them locally in order to
        // be able to consume some mouse events for custom use.
        originalMouseListeners = this.getMouseListeners();
        for (MouseListener listener : originalMouseListeners)
           this.removeMouseListener(listener);

        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addTreeExpansionListener(this);

        GuiActivator.getContactListService().addMetaContactListListener(this);

        treeModel = new ContactListTreeModel();

        setTreeModel(treeModel);

        // We hide the root node as it doesn't represent a real group.
        if (isRootVisible())
            setRootVisible(false);

        this.initKeyActions();

        this.initContactSources();

        MetaContactListSource.setMetaContactQueryListener(this);
    }

    /**
     * Reorders contact list nodes, when <tt>MetaContact</tt>-s in a
     * <tt>MetaContactGroup</tt> has been reordered.
     * @param evt the <tt>MetaContactGroupEvent</tt> that notified us
     */
    public void childContactsReordered(MetaContactGroupEvent evt)
    {
        MetaContactGroup metaGroup = evt.getSourceMetaContactGroup();

        UIGroup uiGroup = null;
        if (!MetaContactListSource.isRootGroup(metaGroup))
        {
            uiGroup = MetaContactListSource.getUIGroup(metaGroup);
        }
        else
        {
            uiGroup = treeModel.getRoot().getGroupDescriptor();
        }

        if (uiGroup != null)
        {
            GroupNode groupNode = uiGroup.getGroupNode();

            if (groupNode != null)
                groupNode.sort(treeModel);
        }
    }

    /**
     * Adds a node in the contact list, when a <tt>MetaContact</tt> has been
     * added in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactAdded(final MetaContactEvent evt)
    {
        final MetaContact metaContact = evt.getSourceMetaContact();
        final MetaContactGroup parentGroup = evt.getParentGroup();

        UIContact uiContact
            = MetaContactListSource.createUIContact(metaContact);

        if (currentFilter.isMatching(uiContact))
        {
            UIGroup uiGroup = null;
            if (!MetaContactListSource.isRootGroup(parentGroup))
            {
                uiGroup = MetaContactListSource
                    .getUIGroup(parentGroup);

                if (uiGroup == null)
                    uiGroup = MetaContactListSource
                        .createUIGroup(parentGroup);
            }

            addContact(uiContact, uiGroup, true);
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

        UIGroup uiGroup
            = MetaContactListSource.createUIGroup(metaGroup);

        if (currentFilter.isMatching(uiGroup))
            addGroup(uiGroup);
        else
            MetaContactListSource.removeUIGroup(metaGroup);
    }

    /**
     * Notifies the tree model, when a <tt>MetaContactGroup</tt> has been
     * modified in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactGroupEvent</tt> that notified us
     */
    public void metaContactGroupModified(MetaContactGroupEvent evt)
    {
        final MetaContactGroup metaGroup = evt.getSourceMetaContactGroup();

        UIGroup uiGroup
            = MetaContactListSource.getUIGroup(metaGroup);

        if (uiGroup != null)
        {
            GroupNode groupNode = uiGroup.getGroupNode();

            if (groupNode != null)
                treeModel.nodeChanged(groupNode);
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
        UIGroup uiGroup
            = MetaContactListSource.getUIGroup(
                    evt.getSourceMetaContactGroup());

        if (uiGroup != null)
            removeGroup(uiGroup);
    }

    /**
     * Notifies the tree model, when a <tt>MetaContact</tt> has been
     * modified in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactModified(final MetaContactModifiedEvent evt)
    {
        UIContact uiContact
            = MetaContactListSource.getUIContact(
                evt.getSourceMetaContact());

        if (uiContact != null)
        {
            ContactNode contactNode
                = uiContact.getContactNode();

            if (contactNode != null)
                treeModel.nodeChanged(contactNode);
        }
    }

    /**
     * Performs needed operations, when a <tt>MetaContact</tt> has been
     * moved in the <tt>MetaContactListService</tt> from one group to another.
     * @param evt the <tt>MetaContactMovedEvent</tt> that notified us
     */
    public void metaContactMoved(final MetaContactMovedEvent evt)
    {
        final MetaContact metaContact = evt.getSourceMetaContact();
        final MetaContactGroup oldParent = evt.getOldParent();
        final MetaContactGroup newParent = evt.getNewParent();

        UIContact uiContact
            = MetaContactListSource.getUIContact(metaContact);

        if (uiContact == null)
            return;

        UIGroup oldUIGroup = null;
        if (!MetaContactListSource.isRootGroup(oldParent))
        {
            oldUIGroup = MetaContactListSource.getUIGroup(oldParent);
        }
        else
        {
            oldUIGroup = treeModel.getRoot().getGroupDescriptor();
        }

        if (oldUIGroup != null)
            removeContact(uiContact);

        // Add the contact to the new place.
        uiContact = MetaContactListSource.createUIContact(
            evt.getSourceMetaContact());

        UIGroup newUIGroup = null;
        if (!MetaContactListSource.isRootGroup(newParent))
        {
            newUIGroup = MetaContactListSource.getUIGroup(newParent);

            if (newUIGroup == null)
                newUIGroup
                    = MetaContactListSource.createUIGroup(newParent);
        }

        if (currentFilter.isMatching(uiContact))
            addContact(uiContact, newUIGroup, true);
        else
            MetaContactListSource.removeUIContact(metaContact);
    }

    /**
     * Removes the corresponding contact node in the contact list, when a
     * <tt>MetaContact</tt> has been removed from the
     * <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactRemoved(final MetaContactEvent evt)
    {
        UIContact uiContact = MetaContactListSource.getUIContact(
                                evt.getSourceMetaContact());

        if (uiContact != null)
            removeContact(uiContact);
    }

    /**
     * Refreshes the corresponding node, when a <tt>MetaContact</tt> has been
     * renamed in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactRenamedEvent</tt> that notified us
     */
    public void metaContactRenamed(final MetaContactRenamedEvent evt)
    {
        UIContact uiContact
            = MetaContactListSource.getUIContact(
                evt.getSourceMetaContact());

        if (uiContact != null)
        {
            ContactNode contactNode = uiContact.getContactNode();

            if (contactNode != null)
                treeModel.nodeChanged(contactNode);
        }
    }

    /**
     * Notifies the tree model, when the <tt>MetaContact</tt> avatar has been
     * modified in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactAvatarUpdated(final MetaContactAvatarUpdateEvent evt)
    {
        UIContact uiContact
            = MetaContactListSource.getUIContact(
                evt.getSourceMetaContact());

        if (uiContact != null)
        {
            ContactNode contactNode = uiContact.getContactNode();

            if (contactNode != null)
                treeModel.nodeChanged(contactNode);
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

        UIContact parentUIContact
            = MetaContactListSource.getUIContact(metaContact);

        if (parentUIContact == null)
        {
            UIContact uiContact
                = MetaContactListSource.createUIContact(metaContact);

            if (currentFilter.isMatching(uiContact))
            {
                MetaContactGroup parentGroup
                    = metaContact.getParentMetaContactGroup();

                UIGroup uiGroup = null;
                if (!MetaContactListSource
                        .isRootGroup(parentGroup))
                {
                    uiGroup = MetaContactListSource
                        .getUIGroup(parentGroup);

                    if (uiGroup == null)
                        uiGroup = MetaContactListSource
                            .createUIGroup(parentGroup);
                }

                addContact(uiContact, uiGroup, true);
            }
            else
                MetaContactListSource.removeUIContact(metaContact);
        }
    }

    public void protoContactModified(ProtoContactEvent evt) {}

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

        UIContact oldUIContact
            = MetaContactListSource.getUIContact(oldParent);

        // Remove old parent if not matching.
        if (oldUIContact != null
            && !currentFilter.isMatching(oldUIContact))
        {
            removeContact(oldUIContact);
        }

        // Add new parent if matching.
        UIContact newUIContact
            = MetaContactListSource.getUIContact(newParent);

        if (newUIContact == null)
        {
            newUIContact
                = MetaContactListSource.createUIContact(newParent);

            if (currentFilter.isMatching(newUIContact))
            {
                MetaContactGroup parentGroup
                    = newParent.getParentMetaContactGroup();

                UIGroup uiGroup = null;
                if (!MetaContactListSource
                        .isRootGroup(parentGroup))
                {
                    uiGroup = MetaContactListSource
                        .getUIGroup(parentGroup);

                    if (uiGroup == null)
                        uiGroup = MetaContactListSource
                            .createUIGroup(parentGroup);
                }

                addContact(newUIContact, uiGroup, true);
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

        UIContact oldUIContact
            = MetaContactListSource.getUIContact(oldParent);

        if (oldUIContact != null)
            removeContact(oldUIContact);
    }

    /**
     * Indicates that a contact has been received for a query.
     * @param event the <tt>ContactReceivedEvent</tt> that notified us
     */
    public void contactReceived(ContactReceivedEvent event)
    {
        final SourceContact sourceContact = event.getContact();

        ContactSourceService contactSource
            = sourceContact.getContactSource();

        ExternalContactSource sourceUI
            = TreeContactList.getContactSource(contactSource);

        if (sourceUI == null)
            return;

        UIContact uiContact
            = sourceUI.createUIContact(sourceContact);

        // ExtendedContactSourceService has already matched the
        // SourceContact over the pattern
        if((contactSource instanceof ExtendedContactSourceService)
            || currentFilter.isMatching(uiContact))
        {
            addContact(uiContact, sourceUI.getUIGroup(), false);
        }
        else
            uiContact = null;
    }

    /**
     * Indicates that a <tt>MetaContact</tt> has been received for a search in
     * the <tt>MetaContactListService</tt>.
     * @param metaContact the received <tt>MetaContact</tt>
     */
    public void metaContactReceived(MetaContact metaContact)
    {
        MetaContactGroup parentGroup = metaContact.getParentMetaContactGroup();

        if (filterQuery != null)
            filterQuery.setSucceeded(true);

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
            MetaContactListSource.createUIContact(metaContact),
            uiGroup,
            true);
    }

    /**
     * Indicates that a <tt>MetaGroup</tt> has been received from a search in
     * the <tt>MetaContactListService</tt>.
     * @param metaGroup the <tt>MetaGroup</tt> that has been received
     */
    public void metaGroupReceived(MetaContactGroup metaGroup)
    {
        GuiActivator.getContactList().addGroup(
            MetaContactListSource.createUIGroup(metaGroup));
    }

    /**
     * Indicates that the status of a query has changed.
     * @param event the <tt>ContactQueryStatusEvent</tt> that notified us
     */
    public void queryStatusChanged(ContactQueryStatusEvent event)
    {
        int eventType = event.getEventType();

        if (eventType == ContactQueryStatusEvent.QUERY_ERROR)
        {
            //TODO: Show the error to the user??
        }
        searchFilter.removeCurrentQuery(event.getQuerySource());
        event.getQuerySource().removeContactQueryListener(this);
    }

    /**
     * Returns the right button menu opened over the contact list.
     *
     * @return the right button menu opened over the contact list
     */
    public Component getRightButtonMenu()
    {
        return rightButtonMenu;
    }

    /**
     * Deactivates all active contacts.
     */
    public void deactivateAll()
    {
        for (ContactNode contactNode : activeContacts)
        {
            if (contactNode != null)
                contactNode.setActive(false);
        }
        activeContacts.clear();
    }

    /**
     * Updates the active state of the contact node corresponding to the given
     * <tt>MetaContact</tt>.
     * @param metaContact the <tt>MetaContact</tt> to update
     * @param isActive indicates if the node should be set to active
     */
    public void setActiveContact(MetaContact metaContact, boolean isActive)
    {
        UIContact uiContact
            = MetaContactListSource.getUIContact(metaContact);

        if (uiContact == null)
            return;

        ContactNode contactNode = uiContact.getContactNode();

        if (contactNode != null)
        {
            contactNode.setActive(isActive);

            if (isActive)
            {
                activeContacts.add(contactNode);
//              SystrayService stray = GuiActivator.getSystrayService();
//
//              if (stray != null)
//                  stray.setSystrayIcon(SystrayService.ENVELOPE_IMG_TYPE);
            }
            else
                activeContacts.remove(contactNode);

            treeModel.nodeChanged(contactNode);
        }
    }

    /**
     * Returns <tt>true</tt> if the given <tt>metaContact</tt> has been
     * previously set to active, otherwise returns <tt>false</tt>.
     * @param contact the <tt>UIContact</tt> to check
     * @return <tt>true</tt> if the given <tt>metaContact</tt> has been
     * previously set to active, otherwise returns <tt>false</tt>
     */
    public boolean isContactActive(UIContact contact)
    {
        ContactNode contactNode = contact.getContactNode();

        if (contactNode != null)
            return contactNode.isActive();
        return false;
    }

    /**
     * Adds the given <tt>contact</tt> to this list.
     * @param contact the <tt>UIContact</tt> to add
     * @param group the <tt>UIGroup</tt> to add to
     * @param isSorted indicates if the contact should be sorted regarding to
     * the <tt>GroupNode</tt> policy
     */
    public void addContact( final UIContact contact,
                            final UIGroup group,
                            final boolean isSorted)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addContact(contact, group, isSorted);
                }
            });
            return;
        }

        GroupNode groupNode;
        if (group == null)
            groupNode = treeModel.getRoot();
        else
        {
            groupNode = group.getGroupNode();

            if (groupNode == null)
            {
                GroupNode parentNode = treeModel.getRoot();

                groupNode = parentNode.sortedAddContactGroup(group);
            }
        }

        contact.setParentGroup(groupNode.getGroupDescriptor());

        if (isSorted)
            groupNode.sortedAddContact(contact);
        else
            groupNode.addContact(contact);

        if ((!currentFilter.equals(presenceFilter)
            || !groupNode.isCollapsed()))
                this.expandGroup(groupNode);
    }

    /**
     * Removes the node corresponding to the given <tt>MetaContact</tt> from
     * this list.
     * @param contact the <tt>UIContact</tt> to remove
     */
    public void removeContact(final UIContact contact)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    removeContact(contact);
                }
            });
            return;
        }

        UIGroup parentGroup = contact.getParentGroup();

        if (parentGroup == null)
            return;

        GroupNode parentGroupNode = parentGroup.getGroupNode();

        // Nothing more to do here if we didn't find the parent.
        if (parentGroupNode == null)
            return;

        parentGroupNode.removeContact(contact);

        // If the parent group is empty remove it.
        if (parentGroupNode.getChildCount() < 1
            && parentGroupNode.getParent() != null)
        {
            treeModel.removeNodeFromParent(parentGroupNode);
        }
    }

    /**
     * Adds the given group to this list.
     * @param group the <tt>UIGroup</tt> to add
     */
    public void addGroup(final UIGroup group)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addGroup(group);
                }
            });
            return;
        }

        GroupNode parentNode = treeModel.getRoot();

        parentNode.sortedAddContactGroup(group);

        expandGroup(treeModel.getRoot());
    }

    /**
     * Removes the given group and its children from the list.
     * @param group the <tt>UIGroup</tt> to remove
     */
    private void removeGroup(final UIGroup group)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    removeGroup(group);
                }
            });
            return;
        }

        UIGroup parentGroup = group.getParentGroup();

        GroupNode parentGroupNode
            = parentGroup.getGroupNode();

        // Nothing more to do here if we didn't find the parent.
        if (parentGroupNode == null)
            return;

        parentGroupNode.removeContactGroup(group);

        // If the parent group is empty remove it.
        if (parentGroupNode.getChildCount() < 1)
            treeModel.removeNodeFromParent(parentGroupNode);
    }

    /**
     * Adds a listener for <tt>ContactListEvent</tt>s.
     *
     * @param listener the listener to add
     */
    public void addContactListListener(ContactListListener listener)
    {
        synchronized (contactListListeners)
        {
            if (!contactListListeners.contains(listener))
                contactListListeners.add(listener);
        }
    }

    /**
     * Removes a listener previously added with <tt>addContactListListener</tt>.
     *
     * @param listener the listener to remove
     */
    public void removeContactListListener(ContactListListener listener)
    {
        synchronized (contactListListeners)
        {
            contactListListeners.remove(listener);
        }
    }

    /**
     * If set to true prevents all operations coming in response to a mouse
     * click.
     * @param isGroupClickConsumed indicates if the group click event is
     * consumed by an external party
     */
    public void setGroupClickConsumed(boolean isGroupClickConsumed)
    {
        this.isGroupClickConsumed = isGroupClickConsumed;
    }

    /**
     * Stops the current filtering if there's one active.
     */
    public void stopFiltering()
    {
        currentFilter.stopFilter();

        if (filterQuery != null)
            filterQuery.cancel();
    }

    /**
     * Applies the default filter.
     */
    public void applyDefaultFilter()
    {
        final MainFrame mainFrame = GuiActivator.getUIService().getMainFrame();
        String currentSearchText = mainFrame.getCurrentSearchText();

        if (currentSearchText != null
             && currentSearchText.length() > 0)
        {
            // The clear will automatically apply the default filter after
            // the remove text event is triggered!
            if (!SwingUtilities.isEventDispatchThread())
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        mainFrame.clearCurrentSearchText();
                    }
                });
            else
                mainFrame.clearCurrentSearchText();
        }
        else
        {
            treeModel.clear();
            applyFilter(defaultFilter, null);
        }
    }

    /**
     * Applies the given <tt>filter</tt>.
     * @param filter the <tt>ContactListFilter</tt> to apply.
     */
    public void applyFilter(ContactListFilter filter)
    {
        treeModel.clear();
        applyFilter(filter, null);
    }

    /**
     * Applies the given <tt>ContactListSourceFilter</tt>.
     * @param filter the <tt>ContactListSourceFilter</tt> to apply
     * @return the <tt>FilterQuery</tt> through which the filter could be
     * tracked
     */
    public FilterQuery applyFilter(final ContactListSourceFilter filter)
    {
        filterQuery = new FilterQuery();

        treeModel.clear();

        // If the filter has a default contact source, we apply it first.
        if (filter.hasDefaultSource())
            applyFilter(filter, null);

        Iterator<ExternalContactSource> filterSources
             = filter.getContactSources().iterator();

        // Then we apply the filter on all its contact sources.
        while (filterSources.hasNext())
        {
            final ExternalContactSource filterSource = filterSources.next();

            if (filterQuery.isCanceled())
                return filterQuery;

            applyFilter(filter, filterSource);
        }
        return filterQuery;
    }

    /**
     * Applies the given <tt>filter</tt> and changes the content of the
     * contact list according to it.
     * @param filter the new filter to set
     * @param contactSource the <tt>ExternalContactSource</tt> to apply the
     * filter to
     */
    private void applyFilter(   final ContactListFilter filter,
                                final ExternalContactSource contactSource)
    {
     // If we're in the event dispatch thread we move to another thread
        // for the filtering.
        if (SwingUtilities.isEventDispatchThread())
        {
            new Thread()
            {
                public void run()
                {
                    applyFilter(filter, contactSource);
                }
            }.start();
            return;
        }

        if (currentFilter == null || !currentFilter.equals(filter))
            this.currentFilter = filter;

        // If we have a specific contact source and we're dealing with
        // a ContactListSourceFilter then we would apply the filter only
        // to this source.
        if (contactSource != null
            && filter instanceof ContactListSourceFilter)
        {
            ContactQuery contactQuery
                = ((ContactListSourceFilter) currentFilter)
                    .applyFilter(contactSource);

            filterQuery.addContactQuery(contactQuery);
        }
        else
            currentFilter.applyFilter();
    }

    /**
     * Sets the default filter to the given <tt>filter</tt>.
     * @param filter the <tt>ContactListFilter</tt> to set as default
     */
    public void setDefaultFilter(ContactListFilter filter)
    {
        this.defaultFilter = filter;
    }

    /**
     * Returns the currently applied filter.
     * @return the currently applied filter
     */
    public ContactListFilter getCurrentFilter()
    {
        return currentFilter;
    }

    /**
     * Selects the first found contact node from the beginning of the contact
     * list.
     */
    public void selectFirstContact()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                ContactNode contactNode = treeModel.findFirstContactNode();

                if (contactNode != null)
                    setSelectionPath(new TreePath(contactNode.getPath()));
            }
        });
    }

    /**
     * Creates the corresponding ContactListEvent and notifies all
     * <tt>ContactListListener</tt>s that a contact is selected.
     *
     * @param source the contact that this event is about.
     * @param eventID the id indicating the exact type of the event to fire.
     * @param clickCount the number of clicks accompanying the event.
     */
    public void fireContactListEvent(Object source, int eventID, int clickCount)
    {
        ContactListEvent evt = new ContactListEvent(source, eventID, clickCount);

        synchronized (contactListListeners)
        {
            if (contactListListeners.size() > 0)
            {
                fireContactListEvent(
                    new Vector<ContactListListener>(contactListListeners),
                    evt);
                return;
            }
        }
    }

    /**
     * Notifies all interested listeners that a <tt>ContactListEvent</tt> has
     * occurred.
     * @param contactListListeners the list of listeners to notify
     * @param event the <tt>ContactListEvent</tt> to trigger
     */
    protected void fireContactListEvent(
            java.util.List<ContactListListener> contactListListeners,
            ContactListEvent event)
    {
        synchronized (contactListListeners)
        {
            for (ContactListListener listener : contactListListeners)
            {
                switch (event.getEventID())
                {
                case ContactListEvent.CONTACT_CLICKED:
                    listener.contactClicked(event);
                    break;
                case ContactListEvent.GROUP_CLICKED:
                    listener.groupClicked(event);
                    break;
                default:
                    logger.error("Unknown event type " + event.getEventID());
                }
            }
        }
    }

    /**
     * Expands the given group node.
     * @param groupNode the group node to expand
     */
    private void expandGroup(GroupNode groupNode)
    {
        final TreePath path = new TreePath(treeModel.getPathToRoot(groupNode));

        if (!isExpanded(path))
            if (!SwingUtilities.isEventDispatchThread())
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        expandPath(path);
                    }
                });
            }
            else
            {
                expandPath(path);
            }
    }

    /**
     * Manages a mouse click over the contact list.
     *
     * When the left mouse button is clicked on a contact cell different things
     * may happen depending on the contained component under the mouse. If the
     * mouse is double clicked on the "contact name" the chat window is opened,
     * configured to use the default protocol contact for the selected
     * MetaContact. If the mouse is clicked on one of the protocol icons, the
     * chat window is opened, configured to use the protocol contact
     * corresponding to the given icon.
     *
     * When the right mouse button is clicked on a contact cell, the cell is
     * selected and the <tt>ContactRightButtonMenu</tt> is opened.
     *
     * When the right mouse button is clicked on a group cell, the cell is
     * selected and the <tt>GroupRightButtonMenu</tt> is opened.
     *
     * When the middle mouse button is clicked on a cell, the cell is selected.
     * @param e the <tt>MouseEvent</tt> that notified us of the click
     */
    public void mouseClicked(MouseEvent e)
    {
        TreePath path = this.getPathForLocation(e.getX(), e.getY());

        if (path == null)
            return;

        Object lastComponent = path.getLastPathComponent();

        // We're interested only if the mouse is clicked over a tree node.
        if (!(lastComponent instanceof TreeNode))
            return;

        if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == 0)
            return;

        if (lastComponent instanceof ContactNode)
        {
            fireContactListEvent(
                ((ContactNode) lastComponent).getContactDescriptor(),
                ContactListEvent.CONTACT_CLICKED, e.getClickCount());
        }
        else if (lastComponent instanceof GroupNode)
        {
            fireContactListEvent(
                ((GroupNode) lastComponent).getGroupDescriptor(),
                ContactListEvent.GROUP_CLICKED, e.getClickCount());
        }

        if (e.getClickCount() < 2)
            dispatchEventToButtons(e);
    }

    /**
     * When the right mouse button is clicked on a contact cell, the cell is
     * selected and the <tt>ContactRightButtonMenu</tt> is opened.
     *
     * When the right mouse button is clicked on a group cell, the cell is
     * selected and the <tt>GroupRightButtonMenu</tt> is opened.
     *
     * When the middle mouse button is clicked on a cell, the cell is selected.
     * @param e the <tt>MouseEvent</tt> that notified us of the press
     */
    public void mousePressed(MouseEvent e)
    {
        if (!isGroupClickConsumed)
        {
            // forward the event to the original listeners
            for (MouseListener listener : originalMouseListeners)
               listener.mousePressed(e);
        }

        TreePath path = this.getPathForLocation(e.getX(), e.getY());

        // If we didn't find any path for the given mouse location, we have
        // nothing to do here.
        if (path == null)
            return;

        // Select the node under the right button click.
        if (!path.equals(getSelectionPath())
            && (e.getModifiers() & InputEvent.BUTTON1_MASK) != 0
                || (e.getModifiers() & InputEvent.BUTTON2_MASK) != 0
                || (e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
                || (e.isControlDown() && !e.isMetaDown()))
        {
            this.setSelectionPath(path);
        }

        Object lastComponent = path.getLastPathComponent();

        // We're interested only if the mouse is clicked over a tree node.
        if (!(lastComponent instanceof TreeNode))
            return;

        // Open message window, right button menu when mouse is pressed.
        if (lastComponent instanceof ContactNode)
        {
            UIContact uiContact
                = ((ContactNode) lastComponent).getContactDescriptor();

            fireContactListEvent(
                uiContact,
                ContactListEvent.CONTACT_CLICKED, e.getClickCount());

            if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
                    || (e.isControlDown() && !e.isMetaDown()))
            {
                rightButtonMenu = uiContact.getRightButtonMenu();

                openRightButtonMenu(e.getPoint());
            }
        }
        else if (lastComponent instanceof GroupNode)
        {
            UIGroup uiGroup
                = ((GroupNode) lastComponent).getGroupDescriptor();

            fireContactListEvent(
                uiGroup,
                ContactListEvent.GROUP_CLICKED, e.getClickCount());

            if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
                    || (e.isControlDown() && !e.isMetaDown()))
            {
                rightButtonMenu = uiGroup.getRightButtonMenu();

                openRightButtonMenu(e.getPoint());
            }
        }

        // If not already consumed dispatch the event to underlying
        // cell buttons.
        dispatchEventToButtons(e);
    }

    /**
     * Forwards the given mouse <tt>event</tt> to the list of original
     * <tt>MouseListener</tt>-s.
     * @param event the <tt>MouseEvent</tt> that notified us
     */
    public void mouseEntered(MouseEvent event)
    {
        // forward the event to the original listeners
        for (MouseListener listener : originalMouseListeners)
           listener.mouseEntered(event);
    }

    /**
     * Forwards the given mouse <tt>event</tt> to the list of original
     * <tt>MouseListener</tt>-s.
     * @param event the <tt>MouseEvent</tt> that notified us
     */
    public void mouseExited(MouseEvent event)
    {
        // forward the event to the original listeners
        for (MouseListener listener : originalMouseListeners)
           listener.mouseExited(event);
    }

    /**
     * Forwards the given mouse <tt>event</tt> to the list of original
     * <tt>MouseListener</tt>-s.
     * @param event the <tt>MouseEvent</tt> that notified us
     */
    public void mouseReleased(MouseEvent event)
    {
        dispatchEventToButtons(event);

        // forward the event to the original listeners
        for (MouseListener listener : originalMouseListeners)
           listener.mouseReleased(event);
    }

    /**
     * Opens the current right button menu at the given point.
     * @param contactListPoint the point where to position the menu
     */
    private void openRightButtonMenu(Point contactListPoint)
    {
        // If the menu is null we have nothing to do here.
        if (rightButtonMenu == null)
            return;

        SwingUtilities.convertPointToScreen(contactListPoint, this);

        rightButtonMenu.setInvoker(this);

        rightButtonMenu.setLocation(contactListPoint.x, contactListPoint.y);

        rightButtonMenu.setVisible(true);
    }

    public void mouseMoved(MouseEvent e) {}

    public void mouseDragged(MouseEvent e) {}

    /**
     * Stores the state of the collapsed group.
     * @param event the <tt>TreeExpansionEvent</tt> that notified us for about
     * the expansion
     */
    public void treeCollapsed(TreeExpansionEvent event)
    {
        Object collapsedNode = event.getPath().getLastPathComponent();

        // For now we only save the group state only if we're in presence
        // filter mode.
        if (collapsedNode instanceof GroupNode
            && currentFilter.equals(presenceFilter))
        {
            GroupNode groupNode = (GroupNode) collapsedNode;
            String id = groupNode.getGroupDescriptor().getId();
            if (id != null)
                ConfigurationManager
                    .setContactListGroupCollapsed(id, true);
        }
    }

    /**
     * Stores the state of the expanded group.
     * @param event the <tt>TreeExpansionEvent</tt> that notified us for about
     * the expansion
     */
    public void treeExpanded(TreeExpansionEvent event)
    {
        Object collapsedNode = event.getPath().getLastPathComponent();

        // For now we only save the group state only if we're in presence
        // filter mode.
        if (collapsedNode instanceof GroupNode
            && currentFilter.equals(presenceFilter))
        {
            GroupNode groupNode = (GroupNode) collapsedNode;
            String id = groupNode.getGroupDescriptor().getId();
            if (id != null)
                ConfigurationManager
                    .setContactListGroupCollapsed(id, false);
        }
    }

    /**
     * Dispatches the given mouse <tt>event</tt> to the underlying buttons.
     * @param event the <tt>MouseEvent</tt> to dispatch
     */
    private void dispatchEventToButtons(MouseEvent event)
    {
        TreePath mousePath
            = this.getPathForLocation(event.getX(), event.getY());

        // If this is not the selection path we have nothing to do here.
        if (mousePath == null || !mousePath.equals(this.getSelectionPath()))
            return;

        JPanel renderer = (JPanel) getCellRenderer()
            .getTreeCellRendererComponent(  this,
                                            mousePath.getLastPathComponent(),
                                            true,
                                            true,
                                            true,
                                            this.getRowForPath(mousePath),
                                            true);

        // We need to translate coordinates here.
        Rectangle r = this.getPathBounds(mousePath);
        int translatedX = event.getX() - r.x;
        int translatedY = event.getY() - r.y;

        Component mouseComponent
            = renderer.findComponentAt(translatedX, translatedY);

        if (mouseComponent instanceof SIPCommButton)
        {
            MouseEvent evt = new MouseEvent(mouseComponent,
                                            event.getID(),
                                            event.getWhen(),
                                            event.getModifiers(),
                                            5, // we're in the button for sure
                                            5, // we're in the button for sure
                                            event.getClickCount(),
                                            event.isPopupTrigger());
            mouseComponent.dispatchEvent(evt);

            this.repaint();
        }
    }

    /**
     * Initializes key actions.
     */
    private void initKeyActions()
    {
        InputMap imap = getInputMap();
        ActionMap amap = getActionMap();

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        imap.put(KeyStroke.getKeyStroke('+'), "openGroup");
        imap.put(KeyStroke.getKeyStroke('-'), "closeGroup");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "openGroup");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "closeGroup");

        amap.put("enter", new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                startSelectedContactChat();
            }
        });

        amap.put("openGroup", new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                TreePath selectionPath = getSelectionPath();

                if (selectionPath != null
                    && selectionPath.getLastPathComponent()
                        instanceof GroupNode)
                {
                    GroupNode groupNode
                        = (GroupNode) selectionPath.getLastPathComponent();

                    expandGroup(groupNode);
                }
            }});

        amap.put("closeGroup", new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                TreePath selectionPath = getSelectionPath();

                if (selectionPath != null
                    && selectionPath.getLastPathComponent()
                        instanceof GroupNode)
                {
                    collapsePath(selectionPath);
                }
            }});
    }

    /**
     * Starts a chat with the currently selected contact if any, otherwise
     * nothing happens. A chat is started with only <tt>MetaContact</tt>s for
     * now.
     */
    public void startSelectedContactChat()
    {
        TreePath selectionPath = getSelectionPath();

        if (selectionPath != null
            && selectionPath.getLastPathComponent() instanceof ContactNode)
        {
            UIContact uiContact
                = ((ContactNode) selectionPath.getLastPathComponent())
                    .getContactDescriptor();

            if (uiContact instanceof MetaUIContact)
                GuiActivator.getUIService().getChatWindowManager()
                    .startChat((MetaContact) uiContact.getDescriptor());
        }
    }

    /**
     * Starts a call with the currently selected contact in the contact list.
     */
    public void startSelectedContactCall()
    {
        TreePath selectionPath = getSelectionPath();
        if (selectionPath == null)
            return;

        ContactListTreeCellRenderer renderer
            = (ContactListTreeCellRenderer) getCellRenderer()
                .getTreeCellRendererComponent(
                    this,
                    selectionPath.getLastPathComponent(),
                    true,
                    true,
                    true,
                    this.getRowForPath(selectionPath),
                    true);

        renderer.getCallButton().doClick();
    }

    /**
     * Sets the given <tt>treeModel</tt> as a model of this tree. Specifies
     * also some related properties.
     * @param treeModel the <tt>TreeModel</tt> to set.
     */
    private void setTreeModel(TreeModel treeModel)
    {
        setModel(treeModel);
        setRowHeight(0);
        setToggleClickCount(1);
    }

    /**
     * Indicates that a node has been changed. Transfers the event to the
     * default tree model.
     * @param node the <tt>TreeNode</tt> that has been refreshed
     */
    public void nodeChanged(TreeNode node)
    {
        treeModel.nodeChanged(node);
    }

    /**
     * Initializes the list of available contact sources for this contact list.
     */
    private void initContactSources()
    {
        for (ContactSourceService contactSource
                : GuiActivator.getContactSources())
        {
            contactSources.add(new ExternalContactSource(contactSource));
        }
        GuiActivator.bundleContext.addServiceListener(
            new ContactSourceServiceListener());
    }

    /**
     * Returns the list of registered contact sources to search in.
     * @return the list of registered contact sources to search in
     */
    public static Collection<ExternalContactSource> getContactSources()
    {
        return contactSources;
    }

    /**
     * Returns the <tt>ExternalContactSource</tt> corresponding to the given
     * <tt>ContactSourceService</tt>.
     * @param contactSource the <tt>ContactSourceService</tt>, which
     * corresponding external source implementation we're looking for
     * @return the <tt>ExternalContactSource</tt> corresponding to the given
     * <tt>ContactSourceService</tt>
     */
    public static ExternalContactSource getContactSource(
        ContactSourceService contactSource)
    {
        Iterator<ExternalContactSource> extSourcesIter
            = contactSources.iterator();

        while (extSourcesIter.hasNext())
        {
            ExternalContactSource extSource = extSourcesIter.next();

            if (extSource.getContactSourceService().equals(contactSource))
                return extSource;
        }
        return null;
    }

    /**
     * Returns the contact source with the given identifier.
     * @param identifier the identifier we're looking for
     * @return the contact source with the given identifier
     */
    public static ExternalContactSource getContactSource(String identifier)
    {
        Iterator<ExternalContactSource> extSourcesIter
            = contactSources.iterator();

        while (extSourcesIter.hasNext())
        {
            ExternalContactSource extSource = extSourcesIter.next();

            if (extSource.getContactSourceService().getIdentifier()
                    .equals(identifier))
                return extSource;
        }
        return null;
    }

    /**
     * Listens for adding and removing of <tt>ContactSourceService</tt>
     * implementations.
     */
    private class ContactSourceServiceListener
        implements ServiceListener
    {
        public void serviceChanged(ServiceEvent event)
        {
            ServiceReference serviceRef = event.getServiceReference();

            // if the event is caused by a bundle being stopped, we don't want
            // to know
            if (serviceRef.getBundle().getState() == Bundle.STOPPING)
                return;

            Object service = GuiActivator.bundleContext.getService(serviceRef);

            // we don't care if the source service is
            // not a contact source service
            if (!(service instanceof ContactSourceService))
                return;

            switch (event.getType())
            {
            case ServiceEvent.REGISTERED:
                ExternalContactSource contactSource
                    = new ExternalContactSource((ContactSourceService) service);
                contactSources.add(contactSource);
                break;
            case ServiceEvent.UNREGISTERING:
                ExternalContactSource cSource
                    = getContactSource((ContactSourceService) service);
                if (cSource != null)
                    contactSources.remove(cSource);
                break;
            }
        }
    }

    /**
     * Indicates that a contact has changed its status.
     *
     * @param evt the presence event containing information about the
     * contact status change
     */
    public void contactPresenceStatusChanged(
            final ContactPresenceStatusChangeEvent evt)
    {
        final Contact sourceContact = evt.getSourceContact();

        final MetaContact metaContact = GuiActivator.getContactListService()
            .findMetaContactByContact(sourceContact);

        if (metaContact == null
            || (evt.getOldStatus() == evt.getNewStatus()))
            return;

        UIContact uiContact
            = MetaContactListSource.getUIContact(metaContact);

        if (uiContact == null)
        {
            uiContact = MetaContactListSource
                .createUIContact(metaContact);

            if (currentFilter.isMatching(uiContact))
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

                addContact(uiContact, uiGroup, true);
            }
            else
                MetaContactListSource
                    .removeUIContact(metaContact);
        }
        else
        {
            if (!currentFilter.isMatching(uiContact))
                removeContact(uiContact);
            else
                treeModel
                    .nodeChanged(uiContact.getContactNode());
        }
    }
}
