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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
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
                MouseListener,
                MouseMotionListener,
                TreeExpansionListener
{
    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(TreeContactList.class);

    /**
     * The tree model.
     */
    private final ContactListTreeModel treeModel;

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

        treeModel = new ContactListTreeModel(
            GuiActivator.getContactListService().getRoot());

        this.setModel(treeModel);
        this.setRowHeight(0);
        this.setToggleClickCount(1);

        // By default we set the current filter to be the presence filter.
        presenceFilter.setShowOffline(ConfigurationManager.isShowOffline());
        applyFilter(presenceFilter);

        // We hide the root node as it doesn't represent a real group.
        if (isRootVisible())
            setRootVisible(false);

        this.initKeyActions();
    }

    /**
     * Reorders contact list nodes, when <tt>MetaContact</tt>-s in a
     * <tt>MetaContactGroup</tt> has been reordered.
     * @param evt the <tt>MetaContactGroupEvent</tt> that notified us
     */
    public void childContactsReordered(MetaContactGroupEvent evt)
    {
        final MetaContactGroup metaGroup = evt.getSourceMetaContactGroup();

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if (currentFilter.isMatching(metaGroup))
                {
                    GroupNode groupNode
                        = treeModel.findGroupNodeByMetaGroup(metaGroup);

                    if (groupNode != null)
                        groupNode.sort();
                }
            }
        });
    }

    /**
     * Adds a node in the contact list, when a <tt>MetaContact</tt> has been
     * added in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactAdded(MetaContactEvent evt)
    {
        final MetaContact metaContact = evt.getSourceMetaContact();

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if (currentFilter.isMatching(metaContact))
                    addContact(metaContact);
            }
        });
    }

    /**
     * Adds a group node in the contact list, when a <tt>MetaContactGroup</tt>
     * has been added in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactGroupEvent</tt> that notified us
     */
    public void metaContactGroupAdded(MetaContactGroupEvent evt)
    {
        final MetaContactGroup metaGroup = evt.getSourceMetaContactGroup();

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if (currentFilter.isMatching(metaGroup))
                    addGroup(metaGroup);
            }
        });
    }

    /**
     * Notifies the tree model, when a <tt>MetaContactGroup</tt> has been
     * modified in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactGroupEvent</tt> that notified us
     */
    public void metaContactGroupModified(MetaContactGroupEvent evt)
    {
        final MetaContactGroup metaGroup = evt.getSourceMetaContactGroup();

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if (currentFilter.isMatching(metaGroup))
                {
                    GroupNode groupNode
                        = treeModel.findGroupNodeByMetaGroup(metaGroup);

                    if (groupNode != null)
                        treeModel.nodeChanged(groupNode);
                }
            }
        });
    }

    /**
     * Removes the corresponding group node in the contact list, when a
     * <tt>MetaContactGroup</tt> has been removed from the
     * <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactGroupEvent</tt> that notified us
     */
    public void metaContactGroupRemoved(final MetaContactGroupEvent evt)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                removeGroup(evt.getSourceMetaContactGroup());
            }
        });
    }

    /**
     * Notifies the tree model, when a <tt>MetaContact</tt> has been
     * modified in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactModified(MetaContactModifiedEvent evt)
    {
        final MetaContact metaContact = evt.getSourceMetaContact();

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if (currentFilter.isMatching(metaContact))
                {
                    ContactNode contactNode
                        = treeModel.findContactNodeByMetaContact(metaContact);

                    if (contactNode != null)
                        treeModel.nodeChanged(contactNode);
                }
            }
        });
    }

    /**
     * Performs needed operations, when a <tt>MetaContact</tt> has been
     * moved in the <tt>MetaContactListService</tt> from one group to another.
     * @param evt the <tt>MetaContactMovedEvent</tt> that notified us
     */
    public void metaContactMoved(final MetaContactMovedEvent evt)
    {
        final MetaContact metaContact = evt.getSourceMetaContact();

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if (currentFilter.isMatching(metaContact))
                {
                    GroupNode oldParent
                        = treeModel.findGroupNodeByMetaGroup(evt.getOldParent());
                    GroupNode newParent
                        = treeModel.findGroupNodeByMetaGroup(evt.getNewParent());

                    if (oldParent != null)
                        oldParent.removeMetaContact(metaContact);
                    if (newParent != null)
                        newParent.sortedAddMetaContact(metaContact);
                }
            }
        });
    }

    /**
     * Removes the corresponding contact node in the contact list, when a
     * <tt>MetaContact</tt> has been removed from the
     * <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactRemoved(final MetaContactEvent evt)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                removeContact(  evt.getSourceMetaContact(),
                                evt.getParentGroup());
            }
        });
    }

    /**
     * Refreshes the corresponding node, when a <tt>MetaContact</tt> has been
     * renamed in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactRenamedEvent</tt> that notified us
     */
    public void metaContactRenamed(MetaContactRenamedEvent evt)
    {
        final MetaContact metaContact = evt.getSourceMetaContact();

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if (currentFilter.isMatching(metaContact))
                {
                    ContactNode contactNode
                        = treeModel.findContactNodeByMetaContact(metaContact);

                    if (contactNode != null)
                        treeModel.nodeChanged(contactNode);
                }
            }
        });
    }

    /**
     * Notifies the tree model, when the <tt>MetaContact</tt> avatar has been
     * modified in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactAvatarUpdated(MetaContactAvatarUpdateEvent evt)
    {
        final MetaContact metaContact = evt.getSourceMetaContact();

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if (currentFilter.isMatching(metaContact))
                {
                    ContactNode contactNode
                        = treeModel.findContactNodeByMetaContact(metaContact);

                    if (contactNode != null)
                        treeModel.nodeChanged(contactNode);
                }
            }
        });
    }

    /**
     * Adds a contact node corresponding to the parent <tt>MetaContact</tt> if
     * this last is matching the current filter and wasn't previously contained
     * in the contact list.
     * @param evt the <tt>ProtoContactEvent</tt> that notified us
     */
    public void protoContactAdded(ProtoContactEvent evt)
    {
        MetaContact parent = evt.getNewParent();

        ContactNode contactNode
            = treeModel.findContactNodeByMetaContact(parent);

        if (contactNode == null && currentFilter.isMatching(parent))
        {
            addContact(parent);
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
        // Remove old parent if not matching.
        MetaContact oldParent = evt.getOldParent();

        ContactNode oldContactNode
            = treeModel.findContactNodeByMetaContact(oldParent);

        if (oldContactNode != null && !currentFilter.isMatching(oldParent))
        {
            removeContact(oldParent,
                oldParent.getParentMetaContactGroup());
        }

        // Add new parent if matching.
        MetaContact newParent = evt.getNewParent();

        ContactNode newContactNode
            = treeModel.findContactNodeByMetaContact(newParent);

        if (newContactNode == null && currentFilter.isMatching(newParent))
        {
            addContact(newParent);
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
        MetaContact oldParent = evt.getOldParent();

        ContactNode contactNode
            = treeModel.findContactNodeByMetaContact(oldParent);

        if (contactNode != null && !currentFilter.isMatching(oldParent))
        {
            removeContact(oldParent,
                oldParent.getParentMetaContactGroup());
        }
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
        ContactNode contactNode
            = treeModel.findContactNodeByMetaContact(metaContact);

        if (contactNode != null)
        {
            contactNode.setActive(isActive);

            if (isActive)
            {
                activeContacts.add(contactNode);
//            SystrayService stray = GuiActivator.getSystrayService();
//
//            if (stray != null)
//                stray.setSystrayIcon(SystrayService.ENVELOPE_IMG_TYPE);
            }
            else
                activeContacts.remove(contactNode);

            treeModel.nodeChanged(contactNode);
        }
    }

    /**
     * Returns <tt>true</tt> if the given <tt>metaContact</tt> has been
     * previously set to active, otherwise returns <tt>false</tt>.
     * @param metaContact the <tt>MetaContact</tt> to check
     * @return <tt>true</tt> if the given <tt>metaContact</tt> has been
     * previously set to active, otherwise returns <tt>false</tt>
     */
    public boolean isContactActive(MetaContact metaContact)
    {
        ContactNode contactNode
            = treeModel.findContactNodeByMetaContact(metaContact);

        if (contactNode != null)
            return contactNode.isActive();
        return false;
    }

    /**
     * Adds the given <tt>MetaContact</tt> to this list.
     * @param metaContact the <tt>MetaContact</tt> to add
     */
    private void addContact(MetaContact metaContact)
    {
        MetaContactGroup metaGroup = metaContact.getParentMetaContactGroup();

        GroupNode groupNode = treeModel.findGroupNodeByMetaGroup(metaGroup);

        if (groupNode == null)
            groupNode = addGroup(metaGroup);

        groupNode.sortedAddMetaContact(metaContact);

        if (!currentFilter.equals(presenceFilter) || !groupNode.isCollapsed())
            this.expandGroup(groupNode);
    }

    /**
     * Removes the node corresponding to the given <tt>MetaContact</tt> from
     * this list.
     * @param metaContact the <tt>MetaContact</tt> to remove
     * @param parentMetaGroup the <tt>MetaContactGroup</tt> that is the parent
     * of this <tt>metaContact</tt>
     */
    private void removeContact( MetaContact metaContact,
                                MetaContactGroup parentMetaGroup)
    {
        GroupNode parentGroupNode
            = treeModel.findGroupNodeByMetaGroup(parentMetaGroup);

        // Nothing more to do here if we didn't find the parent.
        if (parentGroupNode == null)
            return;

        parentGroupNode.removeMetaContact(metaContact);

        // If the parent group is empty remove it.
        if (parentGroupNode.getChildCount() < 1
            && parentGroupNode.getParent() != null)
        {
            treeModel.removeNodeFromParent(parentGroupNode);
        }
    }

    /**
     * Adds the given group to this list.
     * @param metaGroup the <tt>MetaContactGroup</tt> to add
     * @return the created <tt>GroupNode</tt> corresponding to the group
     */
    private GroupNode addGroup(MetaContactGroup metaGroup)
    {
        MetaContactGroup parentGroup = metaGroup.getParentMetaContactGroup();

        GroupNode parentGroupNode
            = treeModel.findGroupNodeByMetaGroup(parentGroup);

        GroupNode groupNode = null;
        if (parentGroupNode == null)
            addGroup(parentGroup);
        else
            groupNode = parentGroupNode.sortedAddMetaContactGroup(metaGroup);

        expandPath(new TreePath(treeModel.getRoot().getPath()));

        return groupNode;
    }

    /**
     * Removes the given group and its children from the list.
     * @param metaGroup the <tt>MetaContactGroup</tt> to remove
     */
    private void removeGroup(MetaContactGroup metaGroup)
    {
        MetaContactGroup parentGroup = metaGroup.getParentMetaContactGroup();

        GroupNode parentGroupNode
            = treeModel.findGroupNodeByMetaGroup(parentGroup);

        // Nothing more to do here if we didn't find the parent.
        if (parentGroupNode == null)
            return;

        parentGroupNode.removeMetaContactGroup(metaGroup);

        // If the parent group is empty remove it.
        if (parentGroupNode.getChildCount() < 1)
            treeModel.removeNodeFromParent(parentGroupNode);
    }

    private void addAllMatching()
    {
        addMatching(GuiActivator.getContactListService().getRoot());
    }

    /**
     * Removes all contacts contained in the given <tt>MetaContactGroup</tt> not
     * matching the current filter.
     * @param metaGroup the <tt>MetaContactGroup</tt>, which unmatching contacts
     * to remove
     */
    private void removeUnmatching(MetaContactGroup metaGroup)
    {
        Iterator<MetaContact> childContacts = metaGroup.getChildContacts();

        while(childContacts.hasNext())
        {
            MetaContact metaContact = childContacts.next();
            if(!currentFilter.isMatching(metaContact))
                removeContact(metaContact,
                    metaContact.getParentMetaContactGroup());
        }

        Iterator<MetaContactGroup> subgroups = metaGroup.getSubgroups();
        while(subgroups.hasNext())
        {
            removeUnmatching(subgroups.next());
        }
    }

    /**
     * Adds all contacts contained in the given <tt>MetaContactGroup</tt>
     * matching the current filter and not contained in the contact list.
     * @param metaGroup the <tt>MetaContactGroup</tt>, which matching contacts
     * to add
     */
    private void addMatching(MetaContactGroup metaGroup)
    {
        Iterator<MetaContact> childContacts = metaGroup.getChildContacts();

        while(childContacts.hasNext())
        {
            MetaContact metaContact = childContacts.next();
            if(currentFilter.isMatching(metaContact))
                addContact(metaContact);
        }

        Iterator<MetaContactGroup> subgroups = metaGroup.getSubgroups();
        while(subgroups.hasNext())
        {
            MetaContactGroup subgroup = subgroups.next();

            if (subgroup.countChildContacts() == 0
                    && subgroup.countSubgroups() == 0
                    && currentFilter.isMatching(subgroup))
                addGroup(subgroup);
            else
                addMatching(subgroup);
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

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                ContactNode contactNode = treeModel
                    .findContactNodeByMetaContact(metaContact);

                if (contactNode == null
                    && currentFilter.isMatching(metaContact))
                {
                    addContact(metaContact);
                }
                else if (contactNode != null)
                {
                    if (!currentFilter.isMatching(metaContact))
                        removeContact(metaContact,
                            metaContact.getParentMetaContactGroup());
                    else
                        treeModel.nodeChanged(contactNode);
                }
            }
        });
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
     * Sets the current filter. This is a filter over the content of the contact
     * list.
     * @param filter the new filter to set
     * @return <tt>true</tt> to indicate that the filter has found a match, 
     * <tt>false</tt> if no matches were found and the contact list is then
     * empty.
     */
    public boolean applyFilter(ContactListFilter filter)
    {
        if (currentFilter == null || !currentFilter.equals(filter))
            this.currentFilter = filter;

        treeModel.clear();
        addAllMatching();

        if (treeModel.getChildCount(treeModel.getRoot()) > 0)
            return true;

        return false;
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
        ContactNode contactNode = treeModel.findFirstContactNode();

        if (contactNode != null)
            setSelectionPath(new TreePath(contactNode.getPath()));
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
     * 
     * @param contactListListeners
     * @param event
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
                case ContactListEvent.PROTOCOL_CONTACT_CLICKED:
                    listener.protocolContactClicked(event);
                    break;
                case ContactListEvent.GROUP_CLICKED:
                    listener.groupSelected(event);
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
        TreePath path = new TreePath(treeModel.getPathToRoot(groupNode));

        if (!isExpanded(path))
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

        Object lastComponent = path.getLastPathComponent();

        // We're interested only if the mouse is clicked over a tree node.
        if (!(lastComponent instanceof TreeNode))
            return;

        if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == 0)
            return;

        if (lastComponent instanceof ContactNode)
        {
            fireContactListEvent(
                ((ContactNode)lastComponent).getMetaContact(),
                ContactListEvent.CONTACT_CLICKED, e.getClickCount());
        }
        else if (lastComponent instanceof GroupNode)
        {
            fireContactListEvent(
                ((GroupNode)lastComponent).getMetaContactGroup(),
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
            ContactNode contactNode = (ContactNode) lastComponent;

            fireContactListEvent(
                contactNode.getMetaContact(),
                ContactListEvent.CONTACT_CLICKED, e.getClickCount());

            // Right click and Ctrl+LeftClick on the contact label opens
            // Popup menu
            if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
                || (e.isControlDown() && !e.isMetaDown()))
            {
                rightButtonMenu = new ContactRightButtonMenu(
                        contactNode.getMetaContact(), this);

                openRightButtonMenu(e.getPoint());
            }
        }
        else if (lastComponent instanceof GroupNode)
        {
            GroupNode groupNode = (GroupNode) lastComponent;

            fireContactListEvent(
                groupNode.getMetaContactGroup(),
                ContactListEvent.GROUP_CLICKED, e.getClickCount());

            if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
                || (e.isControlDown() && !e.isMetaDown()))
            {
                rightButtonMenu = new GroupRightButtonMenu(
                    GuiActivator.getUIService().getMainFrame(),
                        groupNode.getMetaContactGroup());
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
            ConfigurationManager
                .setContactListGroupCollapsed(
                    groupNode.getMetaContactGroup().getMetaUID(), true);
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
            ConfigurationManager
                .setContactListGroupCollapsed(
                    groupNode.getMetaContactGroup().getMetaUID(), false);
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
     * nothing happens.
     */
    public void startSelectedContactChat()
    {
        TreePath selectionPath = getSelectionPath();

        if (selectionPath != null
            && selectionPath.getLastPathComponent()
                instanceof ContactNode)
        {
            ContactNode contactNode
                = (ContactNode) selectionPath.getLastPathComponent();

            GuiActivator.getUIService().getChatWindowManager()
                .startChat(contactNode.getMetaContact());
        }
    }
}
