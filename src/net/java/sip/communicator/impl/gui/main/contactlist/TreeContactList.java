/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.impl.gui.main.contactlist.notifsource.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * The <tt>TreeContactList</tt> is a contact list based on JTree.
 *
 * @author Yana Stamcheva
 */
public class TreeContactList
    extends DefaultTreeContactList
    implements  ContactQueryListener,
                MetaContactQueryListener,
                MouseListener,
                MouseMotionListener,
                TreeExpansionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

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
        = new ArrayList<ContactNode>();

    /**
     * A list of all registered <tt>ContactListListener</tt>-s.
     */
    private final java.util.List<ContactListListener> contactListListeners
        = new ArrayList<ContactListListener>();

    /**
     * The presence filter.
     */
    public static final PresenceFilter presenceFilter = new PresenceFilter();

    public static final MetaContactListSource mclSource
        = new MetaContactListSource();

    /**
     * The search filter.
     */
    public static final SearchFilter searchFilter = new SearchFilter(mclSource);

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

    private static NotificationContactSource notificationSource;

    private FilterQuery currentFilterQuery;

    private FilterThread filterThread;

    /**
     * Indicates that the received call image search has been canceled.
     */
    private static boolean imageSearchCanceled = false;

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

        GuiActivator.getContactListService()
            .addMetaContactListListener(mclSource);

        treeModel = new ContactListTreeModel(this);

        setTreeModel(treeModel);

        // We hide the root node as it doesn't represent a real group.
        if (isRootVisible())
            setRootVisible(false);

        this.initKeyActions();

        this.initContactSources();
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
            addContact(event.getQuerySource(),
                uiContact,
                sourceUI.getUIGroup(), false);
        }
        else
            uiContact = null;
    }

    /**
     * Indicates that a <tt>MetaContact</tt> has been received for a search in
     * the <tt>MetaContactListService</tt>.
     * @param event the received <tt>MetaContactQueryEvent</tt>
     */
    public void metaContactReceived(MetaContactQueryEvent event)
    {
        MetaContact metaContact = event.getMetaContact();
        MetaContactGroup parentGroup = metaContact.getParentMetaContactGroup();

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
            newUIContact = MetaContactListSource.createUIContact(metaContact);
        }

        addContact( event.getQuerySource(),
                    newUIContact,
                    uiGroup,
                    true);
    }

    /**
     * Indicates that a <tt>MetaGroup</tt> has been received from a search in
     * the <tt>MetaContactListService</tt>.
     * @param event the <tt>MetaContactGroupQueryEvent</tt> that has been
     * received
     */
    public void metaGroupReceived(MetaGroupQueryEvent event)
    {
        MetaContactGroup metaGroup = event.getMetaGroup();

        UIGroup uiGroup;
        synchronized (metaGroup)
        {
            uiGroup = MetaContactListSource.createUIGroup(metaGroup);
        }

        if (uiGroup != null)
            GuiActivator.getContactList().addGroup(uiGroup, true);
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
            if (logger.isInfoEnabled())
                logger.info("Contact query error occured: "
                                + event.getQuerySource());
        }
        event.getQuerySource().removeContactQueryListener(this);
    }

    /**
     * Indicates that the status of a query has changed.
     * @param event the <tt>ContactQueryStatusEvent</tt> that notified us
     */
    public void metaContactQueryStatusChanged(MetaContactQueryStatusEvent event)
    {
        int eventType = event.getEventType();

        if (eventType == ContactQueryStatusEvent.QUERY_ERROR)
        {
            if (logger.isInfoEnabled())
                logger.info("Contact query error occured: "
                                + event.getQuerySource());
        }
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

        return (contactNode == null) ? false : contactNode.isActive();
    }

    /**
     * Adds the given <tt>contact</tt> to this list.
     * @param contact the <tt>UIContact</tt> to add
     * @param group the <tt>UIGroup</tt> to add to
     * @param isContactSorted indicates if the contact should be sorted
     * regarding to the <tt>GroupNode</tt> policy
     * @param isGroupSorted indicates if the group should be sorted regarding to
     * the <tt>GroupNode</tt> policy in case it doesn't exist and should be
     * added
     */
    public void addContact( final UIContact contact,
                            final UIGroup group,
                            final boolean isContactSorted,
                            final boolean isGroupSorted)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addContact(contact, group, isContactSorted, isGroupSorted);
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

                if (isGroupSorted)
                    groupNode = parentNode.sortedAddContactGroup(group);
                else
                    groupNode = parentNode.addContactGroup(group);
            }
        }

        contact.setParentGroup(groupNode.getGroupDescriptor());

        if (isContactSorted)
            groupNode.sortedAddContact(contact);
        else
            groupNode.addContact(contact);

        if ((!currentFilter.equals(presenceFilter)
                || !groupNode.isCollapsed()))
            this.expandGroup(groupNode);
        else
            this.expandGroup(treeModel.getRoot());
    }

    /**
     * Adds the given <tt>contact</tt> to this list.
     * @param query the <tt>MetaContactQuery</tt> that adds the given contact
     * @param contact the <tt>UIContact</tt> to add
     * @param group the <tt>UIGroup</tt> to add to
     * @param isSorted indicates if the contact should be sorted regarding to
     * the <tt>GroupNode</tt> policy
     */
    private void addContact(final MetaContactQuery query,
                            final UIContact contact,
                            final UIGroup group,
                            final boolean isSorted)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            LowPriorityEventQueue.invokeLater(new Runnable()
            {
                public void run()
                {
                    addContact(query, contact, group, isSorted);
                }
            });
            return;
        }

        // If in the meantime the corresponding query was canceled
        // we don't proceed with adding.
        if (query != null && !query.isCanceled())
            addContact(contact, group, isSorted, true);
    }

    /**
     * Adds the given <tt>contact</tt> to this list.
     * @param query the <tt>ContactQuery</tt> that adds the given contact
     * @param contact the <tt>UIContact</tt> to add
     * @param group the <tt>UIGroup</tt> to add to
     * @param isSorted indicates if the contact should be sorted regarding to
     * the <tt>GroupNode</tt> policy
     */
    public void addContact(final ContactQuery query,
                            final UIContact contact,
                            final UIGroup group,
                            final boolean isSorted)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            LowPriorityEventQueue.invokeLater(new Runnable()
            {
                public void run()
                {
                    addContact(query, contact, group, isSorted);
                }
            });
            return;
        }

        // If in the meantime the filter has changed we don't
        // add the contact.
        if (query != null
                && currentFilterQuery.containsQuery(query))
        {
            addContact(contact, group, isSorted, true);
        }
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
        if (parentGroupNode.getChildCount() == 0)
        {
            GroupNode parent = (GroupNode) parentGroupNode.getParent();

            if (parent != null)
                parent.removeContactGroup(parentGroup);
        }
    }

    /**
     * Indicates that the information corresponding to the given
     * <tt>contact</tt> has changed.
     *
     * @param contact the contact that has changed
     */
    public void refreshContact(final UIContact contact)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    refreshContact(contact);
                }
            });
            return;
        }

        treeModel.nodeChanged(contact.getContactNode());
    }

    /**
     * Adds the given group to this list.
     * @param group the <tt>UIGroup</tt> to add
     * @param isSorted indicates if the contact should be sorted regarding to
     * the <tt>GroupNode</tt> policy
     */
    public void addGroup(final UIGroup group, final boolean isSorted)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addGroup(group, isSorted);
                }
            });
            return;
        }

        GroupNode groupNode = group.getGroupNode();
        
        if(groupNode == null)
        {
            GroupNode parentNode = treeModel.getRoot();

            if (isSorted)
                parentNode.sortedAddContactGroup(group);
            else
                parentNode.addContactGroup(group);
        }

        expandGroup(treeModel.getRoot());
    }

    /**
     * Removes the given group and its children from the list.
     * @param group the <tt>UIGroup</tt> to remove
     */
    public void removeGroup(final UIGroup group)
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

        GroupNode parentGroupNode;

        if(parentGroup == null)
        {
            if(group.countChildContacts() == 0)
                parentGroupNode = treeModel.getRoot();
            else
                return;
        }
        else
            parentGroupNode = parentGroup.getGroupNode();

        // Nothing more to do here if we didn't find the parent.
        if (parentGroupNode == null)
            return;

        parentGroupNode.removeContactGroup(group);

        // If the parent group is empty remove it.
        if (parentGroupNode.getChildCount() == 0)
        {
            GroupNode parent = (GroupNode) parentGroupNode.getParent();

            if (parent != null)
                parent.removeContactGroup(parentGroup);
        }
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
     * Applies the default filter.
     * @return the filter query that keeps track of the filtering results
     */
    public FilterQuery applyDefaultFilter()
    {
        FilterQuery filterQuery = null;

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
            filterQuery = applyFilter(defaultFilter);
        }

        return filterQuery;
    }

    /**
     * Applies the given <tt>filter</tt>.
     * @param filter the <tt>ContactListFilter</tt> to apply.
     * @return the filter query
     */
    public FilterQuery applyFilter(ContactListFilter filter)
    {
        if (logger.isDebugEnabled())
            logger.debug("Contact list filter applied: " + filter);

        if (currentFilterQuery != null && !currentFilterQuery.isCanceled())
            currentFilterQuery.cancel();

        currentFilterQuery = new FilterQuery();

        if (filterThread == null)
        {
            filterThread = new FilterThread();
            filterThread.setFilter(filter);
            filterThread.start();
        }
        else
        {
            filterThread.setFilter(filter);

            synchronized (filterThread)
            {
                filterThread.notify();
            }
        }

        return currentFilterQuery;
    }

    /**
     * The <tt>SearchThread</tt> is meant to launch the search in a separate
     * thread.
     */
    private class FilterThread extends Thread
    {
        private ContactListFilter filter;

        public void setFilter(ContactListFilter filter)
        {
            this.filter = filter;
        }

        public void run()
        {
            while (true)
            {
                FilterQuery filterQuery = currentFilterQuery;
                ContactListFilter filter = this.filter;

                treeModel.clear();

                if (!filterQuery.isCanceled())
                {
                    if (currentFilter == null || !currentFilter.equals(filter))
                        currentFilter = filter;

                    // If something goes wrong in our filters, we don't want the
                    // whole gui to crash.
                    try
                    {
                        currentFilter.applyFilter(filterQuery);
                    }
                    catch (Throwable t)
                    {
                        if (logger.isInfoEnabled())
                            logger.info(
                                "One of our contact list filters has crashed.",
                                t);
                    }
                }

                synchronized (this)
                {
                    try
                    {
                        // If in the mean time someone has changed the filter
                        // we don't wait here.
                        if (filterQuery == currentFilterQuery)
                            this.wait();
                    }
                    catch (InterruptedException e)
                    {
                        if (logger.isInfoEnabled())
                            logger.info("Filter thread was interrupted.", e);
                    }
                }
            }
        }
    }

    /**
     * Sets the default filter to the given <tt>filter</tt>.
     * @param filter the <tt>ContactListFilter</tt> to set as default
     */
    public void setDefaultFilter(ContactListFilter filter)
    {
        this.defaultFilter = filter;

        if (defaultFilter.equals(presenceFilter))
            TreeContactList.searchFilter
                .setSearchSourceType(SearchFilter.DEFAULT_SOURCE);
        else if (defaultFilter.equals(historyFilter))
            TreeContactList.searchFilter
                .setSearchSourceType(SearchFilter.HISTORY_SOURCE);
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
     * Indicates if this contact list is empty.
     * @return <tt>true</tt> if this contact list contains no children,
     * otherwise returns <tt>false</tt>
     */
    public boolean isEmpty()
    {
        return (treeModel.getRoot().getChildCount() <= 0);
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
                expandPath(path);
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

        Object lastComponent = path.getLastPathComponent();

        // We're interested only if the mouse is clicked over a tree node.
        if (!(lastComponent instanceof TreeNode))
            return;

        boolean isSelected = path.equals(getSelectionPath());

        // Select the node under the right button click.
        if (!isSelected
            && (e.getModifiers() & InputEvent.BUTTON2_MASK) != 0
                || (e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
                || (e.isControlDown() && !e.isMetaDown()))
        {
            this.setSelectionPath(path);
        }

        // Open right button menu when right mouse is pressed.
        if (lastComponent instanceof ContactNode)
        {
            UIContact uiContact
                = ((ContactNode) lastComponent).getContactDescriptor();

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

            if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
                    || (e.isControlDown() && !e.isMetaDown()))
            {
                rightButtonMenu = uiGroup.getRightButtonMenu();

                openRightButtonMenu(e.getPoint());
            }
        }

        // If not already consumed dispatch the event to underlying
        // cell buttons.
        if (isSelected && e.getClickCount() < 2)
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

        if (logger.isDebugEnabled() && mouseComponent != null)
            logger.debug("DISPATCH MOUSE EVENT TO COMPONENT: "
                    + mouseComponent.getClass().getName()
                    + " with bounds: " + mouseComponent.getBounds()
                    + " for x: " + translatedX
                    + " and y: " + translatedY);

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

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "main-rename");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        imap.put(KeyStroke.getKeyStroke('+'), "openGroup");
        imap.put(KeyStroke.getKeyStroke('-'), "closeGroup");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "openGroup");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "closeGroup");

        amap.put("main-rename", new RenameAction());
        amap.put("enter", new AbstractAction()
        {
            private static final long serialVersionUID = 0L;

            public void actionPerformed(ActionEvent e)
            {
                startSelectedContactChat();
            }
        });

        amap.put("openGroup", new AbstractAction()
        {
            private static final long serialVersionUID = 0L;

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
            private static final long serialVersionUID = 0L;

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
     * Starts a video call with the currently selected contact in the contact
     * list.
     */
    public void startSelectedContactVideoCall()
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

        renderer.getCallVideoButton().doClick();
    }

    /**
     * Starts a desktop sharing session with the currently selected contact in
     * the contact list.
     */
    public void startSelectedContactDesktopSharing()
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

        renderer.getDesktopSharingButton().doClick();
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
     * Returns the notification contact source.
     *
     * @return the notification contact source
     */
    public static NotificationContactSource getNotificationContactSource()
    {
        if (notificationSource == null)
            notificationSource = new NotificationContactSource();
        return notificationSource;
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
     * Searches for a source contact image for the given peer string in one of
     * the available contact sources.
     *
     * @param contactString the address of the contact to search an image for
     * @param label the label to set the image to
     * @param imgWidth the desired image width
     * @param imgHeight the desired image height
     */
    public static void setSourceContactImage(   String contactString,
                                                final JLabel label,
                                                final int imgWidth,
                                                final int imgHeight)
    {
        // Re-init the imageSearchCanceled.
        imageSearchCanceled = false;

        // We make a pattern that matches the whole string only.
        Pattern filterPattern = Pattern.compile(
            "^" + Pattern.quote(contactString) + "$", Pattern.UNICODE_CASE);

        Iterator<ExternalContactSource> contactSources
            = TreeContactList.getContactSources().iterator();

        final Vector<ContactQuery> loadedQueries = new Vector<ContactQuery>();

        while (contactSources.hasNext())
        {
            // If the image search has been canceled from one of the previous
            // sources, we return here.
            if (imageSearchCanceled)
                return;

            ContactSourceService contactSource
                = contactSources.next().getContactSourceService();

            if (contactSource instanceof ExtendedContactSourceService)
            {
                ContactQuery query = ((ExtendedContactSourceService)
                        contactSource).queryContactSource(filterPattern);

                loadedQueries.add(query);

                query.addContactQueryListener(new ContactQueryListener()
                {
                    public void queryStatusChanged(ContactQueryStatusEvent event)
                    {}

                    public void contactReceived(ContactReceivedEvent event)
                    {
                        SourceContact sourceContact = event.getContact();

                        byte[] image = sourceContact.getImage();

                        if (image != null && image.length > 0)
                        {
                            setScaledLabelImage(
                                label, image, imgWidth, imgHeight);

                            // Cancel all already loaded queries.
                            cancelImageQueries(loadedQueries);

                            imageSearchCanceled = true;
                        }
                    }
                });

                // If the image search has been canceled from one of the
                // previous sources, we return here.
                if (imageSearchCanceled)
                    return;

                // Let's see if we find an image in the direct results.
                List<SourceContact> results = query.getQueryResults();

                if (results != null && !results.isEmpty())
                {
                    Iterator<SourceContact> resultsIter = results.iterator();

                    while (resultsIter.hasNext())
                    {
                        byte[] image = resultsIter.next().getImage();

                        if (image != null && image.length > 0)
                        {
                            setScaledLabelImage(
                                label, image, imgWidth, imgHeight);

                            // Cancel all already loaded queries.
                            cancelImageQueries(loadedQueries);

                            // As we found the image we return here.
                            return;
                        }
                    }
                }
            }
        }

        // If the image search has been canceled from one of the previous
        // sources, we return here.
        if (imageSearchCanceled)
            return;

        // If we didn't find anything we would check and try to remove the @
        // sign if such exists.
        int atIndex = contactString.indexOf("@");

        // If we find that the contact is actually a number, we get rid of the
        // @ if it exists.
        if (atIndex >= 0
                && StringUtils.isNumber(contactString.substring(0, atIndex)))
        {
            setSourceContactImage(  contactString.substring(0, atIndex),
                                    label, imgWidth, imgHeight);
        }
    }

    /**
     * Cancels the list of loaded <tt>ContactQuery</tt>s.
     *
     * @param loadedQueries the list of queries to cancel
     */
    private static void cancelImageQueries(
            Collection<ContactQuery> loadedQueries)
    {
        Iterator<ContactQuery> queriesIter = loadedQueries.iterator();

        while (queriesIter.hasNext())
            queriesIter.next().cancel();
    }

    /**
     * Sets the image of the incoming call notification.
     *
     * @param label the label to set the image to
     * @param image the image to set
     * @param width the desired image width
     * @param height the desired image height
     */
    private static void setScaledLabelImage(
        JLabel label, byte[] image, int width, int height)
    {
        label.setIcon(
            ImageUtils.getScaledRoundedIcon(image, width, height));

        label.repaint();
    }

    /**
     * Create an the add contact menu, taking into account the number of contact
     * details available in the given <tt>sourceContact</tt>.
     *
     * @param sourceContact the external source contact, for which we'd like
     * to create a menu
     * @return the add contact menu
     */
    public static JMenuItem createAddContactMenu(SourceContact sourceContact)
    {
        JMenuItem addContactComponentTmp = null;

        List<ContactDetail> details = sourceContact.getContactDetails();

        if (details.size() == 1)
        {
            addContactComponentTmp
                = new JMenuItem(GuiActivator.getResources().getI18NString(
                    "service.gui.ADD_CONTACT"),
                    new ImageIcon(ImageLoader
                        .getImage(ImageLoader.ADD_CONTACT_16x16_ICON)));

            final ContactDetail detail = details.get(0);

            addContactComponentTmp.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    showAddContactDialog(detail);
                }
            });
        }
        // If we have more than one details we would propose a separate menu
        // item for each one of them.
        else if (details.size() > 1)
        {
            addContactComponentTmp
                = new JMenu(GuiActivator.getResources().getI18NString(
                    "service.gui.ADD_CONTACT"));

            Iterator<ContactDetail> detailsIter = details.iterator();

            while (detailsIter.hasNext())
            {
                final ContactDetail detail = detailsIter.next();

                JMenuItem addMenuItem
                    = new JMenuItem(detail.getContactAddress());
                ((JMenu) addContactComponentTmp).add(addMenuItem);

                addMenuItem.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        showAddContactDialog(detail);
                    }
                });
            }
        }

        return addContactComponentTmp;
    }

    /**
     * Creates and shows an <tt>AddContactDialog</tt> with a predefined
     * <tt>contactAddress</tt> and <tt>protocolProvider</tt>.
     * @param contactDetail the contact detail to be added
     */
    public static void showAddContactDialog(ContactDetail contactDetail)
    {
        AddContactDialog dialog = new AddContactDialog(
            GuiActivator.getUIService().getMainFrame());

        // Try to obtain a preferred provider.
        ProtocolProviderService preferredProvider = null;
        List<Class<? extends OperationSet>> opSetClasses
            = contactDetail.getSupportedOperationSets();

        if (opSetClasses != null && opSetClasses.size() > 0)
        {
            preferredProvider
                = contactDetail.getPreferredProtocolProvider(
                    opSetClasses.get(0));
        }
        if (preferredProvider != null)
            dialog.setSelectedAccount(preferredProvider);

        dialog.setContactAddress(contactDetail.getContactAddress());
        dialog.setVisible(true);
    }

    /**
     * Listens for adding and removing of <tt>ContactSourceService</tt>
     * implementations.
     */
    private static class ContactSourceServiceListener
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
     * <tt>RenameAction</tt> is invoked when user presses the F2 key. Depending
     * on the selection opens the appropriate form for renaming.
     */
    private class RenameAction
        extends AbstractAction
    {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e)
        {
            Object selectedObject = getSelectedValue();

            if (selectedObject instanceof ContactNode)
            {
                UIContact uiContact
                    = ((ContactNode) selectedObject).getContactDescriptor();

                if (!(uiContact instanceof MetaUIContact))
                    return;

                MetaUIContact metaUIContact = (MetaUIContact) uiContact;

                RenameContactDialog dialog = new RenameContactDialog(
                        GuiActivator.getUIService().getMainFrame(),
                        (MetaContact) metaUIContact.getDescriptor());
                Dimension screenSize
                    = Toolkit.getDefaultToolkit().getScreenSize();

                dialog.setLocation(
                        screenSize.width/2 - 200,
                        screenSize.height/2 - 50);

                dialog.setVisible(true);

                dialog.requestFocusInFiled();
            }
            else if (selectedObject instanceof GroupNode)
            {
                UIGroup uiGroup
                    = ((GroupNode) selectedObject).getGroupDescriptor();

                if (!(uiGroup instanceof MetaUIGroup))
                    return;

                MetaUIGroup metaUIGroup = (MetaUIGroup) uiGroup;

                RenameGroupDialog dialog = new RenameGroupDialog(
                        GuiActivator.getUIService().getMainFrame(),
                        (MetaContactGroup) metaUIGroup.getDescriptor());

                Dimension screenSize =
                    Toolkit.getDefaultToolkit().getScreenSize();
                dialog.setLocation(screenSize.width / 2 - 200,
                    screenSize.height / 2 - 50);

                dialog.setVisible(true);

                dialog.requestFocusInFiled();
            }
        }
    }

    public ContactListTreeModel getTreeModel()
    {
        return treeModel;
    }

    public MetaContactListSource getMetaContactListSource()
    {
        return mclSource;
    }
}
