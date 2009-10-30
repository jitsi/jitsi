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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>ContactList</tt> is a JList that represents the contact list. A
 * custom data model and a custom list cell renderer is used. This class manages
 * all meta contact list events, like <code>metaContactAdded</code>,
 * <code>metaContactMoved</code>, <code>metaContactGroupAdded</code>, etc.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class ContactList
    extends DefaultContactList
    implements  MetaContactListListener,
                MouseListener
{
    private static final String ADD_OPERATION = "AddOperation";

    private static final String REMOVE_OPERATION = "RemoveOperation";

    private static final String MODIFY_OPERATION = "ModifyOperation";

    private final Logger logger = Logger.getLogger(ContactList.class);

    private final MetaContactListService contactListService;

    private final ContactListModel listModel;

    private Object currentlySelectedObject;

    private final java.util.List<ContactListListener> contactListListeners
        = new Vector<ContactListListener>();

    private final java.util.List<ContactListListener> excContactListListeners
        = new Vector<ContactListListener>();

    private final MainFrame mainFrame;

    private final Map<Object, String> contentToRefresh
        = new Hashtable<Object, String>();

    private boolean refreshEnabled = true;

    private GroupRightButtonMenu groupRightButtonMenu;

    private ContactRightButtonMenu contactRightButtonMenu;

    /**
     * A list of all contacts that are currently "active". An "active" contact
     * is a contact that has been sent a message. The list is used to indicate
     * these contacts with a special icon.
     */
    private final java.util.List<MetaContact> activeContacts
        = new Vector<MetaContact>();

    /**
     * If set to true prevents groups to be closed or opened using the mouse.
     */
    private boolean disableOpenClose = false;

    /**
     * Creates an instance of the <tt>ContactList</tt>.
     *
     * @param mainFrame The main application window.
     */
    public ContactList(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;

        this.contactListService = GuiActivator.getMetaContactListService();

        this.listModel = new ContactListModel(contactListService);
        this.setModel(listModel);

        this.setShowOffline(ConfigurationManager.isShowOffline());

        this.initListeners();

        new ContactListRefresh().start();
    }

    /**
     * Initialize all listeners.
     */
    private void initListeners()
    {
        this.contactListService.addMetaContactListListener(this);

        this.addMouseListener(this);

        this.addKeyListener(new CListKeySearchListener(this));

        this.addListSelectionListener(new ListSelectionListener()
        {
            public void valueChanged(ListSelectionEvent e)
            {
                if (!e.getValueIsAdjusting())
                {
                    currentlySelectedObject = getSelectedValue();
                }
            }
        });
    }

    /**
     * Handles the <tt>MetaContactEvent</tt>. Refreshes the list model.
     * @param evt the <tt>MetaContactEvent</tt> that notified us of the add
     */
    public void metaContactAdded(MetaContactEvent evt)
    {
        this.addContact(evt.getSourceMetaContact());
    }

    /**
     * Handles the <tt>MetaContactRenamedEvent</tt>. Refreshes the list when
     * a meta contact is renamed.
     * @param evt the <tt>MetaContactRnamedEvent</tt> that notified us of the
     * rename
     */
    public void metaContactRenamed(MetaContactRenamedEvent evt)
    {
        this.refreshContact(evt.getSourceMetaContact());
    }

    /**
     * Handles the <tt>MetaContactModifiedEvent</tt>.
     * Indicates that a MetaContact has been modified.
     * @param evt the MetaContactModifiedEvent containing the corresponding
     * contact
     */
    public void metaContactModified(MetaContactModifiedEvent evt) {}

    /**
     * Handles the <tt>ProtoContactEvent</tt>. Refreshes the list when a
     * protocol contact has been added.
     * @param evt the <tt>ProtoContactEvent</tt> that notified us of the add
     */
    public void protoContactAdded(ProtoContactEvent evt)
    {
        this.refreshContact(evt.getNewParent());
    }

    /**
     * Handles the <tt>ProtoContactEvent</tt>. Refreshes the list when a
     * protocol contact has been removed.
     * @param evt the <tt>ProtoContactEvent</tt> that notified us of the remove
     */
    public void protoContactRemoved(ProtoContactEvent evt)
    {
        this.refreshContact(evt.getOldParent());
    }

    /**
     * Handles the <tt>ProtoContactEvent</tt>. Refreshes the list when a
     * protocol contact has been moved.
     *  @param evt the <tt>ProtoContactEvent</tt> that notified us of the move
     */
    public void protoContactMoved(ProtoContactEvent evt)
    {
        this.refreshContact(evt.getOldParent());
        this.refreshContact(evt.getNewParent());
    }

    /**
     * Implements the <tt>MetaContactListListener.protoContactModified</tt>
     * method with an empty body since we are not interested in proto contact
     * specific changes (such as the persistent data) in the user interface.
     * @param evt the <tt>ProtoContactEvent</tt> that notified us of the
     *  modification
     */
    public void protoContactModified(ProtoContactEvent evt)
    {
        // currently ignored
    }

    /**
     * Handles the <tt>MetaContactEvent</tt>. Refreshes the list when a meta
     * contact has been removed.
     * @param evt the <tt>MetaContactEvent</tt> that notified us of the remove
     */
    public void metaContactRemoved(MetaContactEvent evt)
    {
        this.removeContact(evt);
    }

    /**
     * Handles the <tt>MetaContactMovedEvent</tt>. Refreshes the list when a
     * meta contact has been moved.
     * @param evt the <tt>MetaContactEvent</tt> that notified us of the move
     */
    public void metaContactMoved(MetaContactMovedEvent evt)
    {
        this.modifyGroup(evt.getNewParent());
        this.modifyGroup(evt.getOldParent());
    }

    /**
     * Handles the <tt>MetaContactGroupEvent</tt>. Refreshes the list model
     * when a new meta contact group has been added.
     * @param evt the <tt>MetaContactGroupEvent</tt> that notified us of the add
     */
    public void metaContactGroupAdded(MetaContactGroupEvent evt)
    {
        MetaContactGroup group = evt.getSourceMetaContactGroup();

        if (!group.equals(contactListService.getRoot()))
            this.addGroup(group);
    }

    /**
     * Handles the <tt>MetaContactGroupEvent</tt>. Refreshes the list when a
     * meta contact group has been modified.
     * @param evt the <tt>MetaContactGroupEvent</tt> that notified us of the
     * modification
     */
    public void metaContactGroupModified(MetaContactGroupEvent evt)
    {
        MetaContactGroup group = evt.getSourceMetaContactGroup();

        if (!group.equals(contactListService.getRoot()))
            this.modifyGroup(group);
    }

    /**
     * Handles the <tt>MetaContactGroupEvent</tt>. Refreshes the list when a
     * meta contact group has been removed.
     * @param evt the <tt>MetaContactGroupEvent</tt> that notified us of the
     * remove
     */
    public void metaContactGroupRemoved(MetaContactGroupEvent evt)
    {
        MetaContactGroup group = evt.getSourceMetaContactGroup();

        if (!group.equals(contactListService.getRoot()))
            this.removeGroup(group);
    }

    /**
     * Handles the <tt>MetaContactGroupEvent</tt>. Refreshes the list model
     * when the contact list groups has been reordered. Moves the selection
     * index to the index of the contact that was selected before the reordered
     * event. This way the selection depends on the contact and not on the
     * index.
     * @param evt the <tt>MetaContactGroupEvent</tt> that notified us of the
     * reordering
     */
    public void childContactsReordered(MetaContactGroupEvent evt)
    {
        if (currentlySelectedObject != null)
            setSelectedValue(currentlySelectedObject);
        this.modifyGroup(evt.getSourceMetaContactGroup());
    }

    /**
     * Returns the list of all groups.
     *
     * @return The list of all groups.
     */
    public Iterator<MetaContactGroup> getAllGroups()
    {
        return contactListService.getRoot().getSubgroups();
    }

    /**
     * Returns the Meta Contact Group corresponding to the given MetaUID.
     *
     * @param metaUID An identifier of a group.
     * @return The Meta Contact Group corresponding to the given MetaUID.
     */
    public MetaContactGroup getGroupByID(String metaUID)
    {
        Iterator<MetaContactGroup> i
            = contactListService.getRoot().getSubgroups();
        while (i.hasNext())
        {
            MetaContactGroup group = i.next();

            if (group.getMetaUID().equals(metaUID))
                return group;
        }
        return null;
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
     * Adds a listener for <tt>ContactListEvent</tt>s.
     *
     * @param listener the listener to add
     */
    public void addExcContactListListener(ContactListListener listener)
    {
        synchronized (excContactListListeners)
        {
            if (!excContactListListeners.contains(listener))
                excContactListListeners.add(listener);
        }
    }

    /**
     * Removes a listener previously added with <tt>addContactListListener</tt>.
     *
     * @param listener the listener to remove
     */
    public void removeExcContactListListener(ContactListListener listener)
    {
        synchronized (excContactListListeners)
        {
            excContactListListeners.remove(listener);
        }
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

        synchronized (excContactListListeners)
        {
            if (excContactListListeners.size() > 0)
            {
                fireContactListEvent(
                    new Vector<ContactListListener>(excContactListListeners),
                    evt);
                return;
            }
        }

        fireContactListEvent(contactListListeners, evt);
    }

    /**
     * Creates the corresponding ContactListEvent and notifies all
     * <tt>ContactListListener</tt>s that a contact is selected.
     *
     * @param sourceContact the contact that this event is about
     * @param protocolContact the protocol contact the this event is about
     * @param eventID the id indicating the exact type of the event to fire.
     */
    public void fireContactListEvent(MetaContact sourceContact,
        Contact protocolContact, int eventID)
    {
        fireContactListEvent(
            contactListListeners,
            new ContactListEvent(sourceContact, protocolContact, eventID));
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
                case ContactListEvent.CONTACT_SELECTED:
                    listener.contactClicked(event);
                    break;
                case ContactListEvent.PROTOCOL_CONTACT_SELECTED:
                    listener.protocolContactClicked(event);
                    break;
                case ContactListEvent.GROUP_SELECTED:
                    listener.groupSelected(event);
                    break;
                default:
                    logger.error("Unknown event type " + event.getEventID());
                }
            }
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
        int selectedIndex = this.getSelectedIndex();
        Object selectedValue = this.getSelectedValue();

        // If there's no index selected we have nothing to do here.
        if (selectedIndex < 0)
            return;

        ContactListCellRenderer renderer
            = (ContactListCellRenderer) this.getCellRenderer()
                .getListCellRendererComponent(  this,
                                                selectedValue,
                                                selectedIndex,
                                                true,
                                                true);

        Point selectedCellPoint = this.indexToLocation(selectedIndex);

        int translatedX = e.getX() - selectedCellPoint.x;

        if (selectedValue instanceof MetaContactGroup)
        {
            MetaContactGroup group = (MetaContactGroup) selectedValue;

            if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
                || (e.isControlDown() && !e.isMetaDown()))
            {

                groupRightButtonMenu = new GroupRightButtonMenu(mainFrame,
                    group);

                SwingUtilities.convertPointToScreen(selectedCellPoint, this);

                groupRightButtonMenu.setInvoker(this);

                groupRightButtonMenu.setLocation(selectedCellPoint.x,
                    selectedCellPoint.y + renderer.getHeight());

                groupRightButtonMenu.setVisible(true);
            }
            else if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0)
            {
                if(!disableOpenClose)
                {
                    if (listModel.isGroupClosed(group))
                        listModel.openGroup(group);
                    else
                        listModel.closeGroup(group);
                }

                fireContactListEvent(   group,
                                        ContactListEvent.GROUP_SELECTED,
                                        e.getClickCount());

                if(!disableOpenClose)
                {
                    // get the component under the mouse
                    Component component
                        = this.getHorizontalComponent(renderer, translatedX);

                    if ((component instanceof JPanel)
                            && ("buttonsPanel".equals(component.getName())))
                    {
                        JPanel panel = (JPanel) component;
                        int internalX
                            = translatedX
                                - (renderer.getWidth() - panel.getWidth() - 2);
                        Component c = getHorizontalComponent(panel, internalX);

                        if (c instanceof JLabel)
                        {
                            if (listModel.isGroupClosed(group))
                                listModel.openGroup(group);
                            else
                                listModel.closeGroup(group);
                        }
                    }
                }
            }
        }

        // Open message window, right button menu or contact info when
        // mouse is pressed. Distinguish on which component was pressed
        // the mouse and make the appropriate work.
        if (selectedValue instanceof MetaContact)
        {
            MetaContact contact = (MetaContact) selectedValue;

            // get the component under the mouse
            Component component = this.getHorizontalComponent(renderer,
                translatedX);

            if (component instanceof JLabel)
            {
                // Right click and Ctrl+LeftClick on the contact label opens
                // Popup menu
                if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
                    || (e.isControlDown() && !e.isMetaDown()))
                {

                    contactRightButtonMenu = new ContactRightButtonMenu(this,
                        contact);

                    SwingUtilities
                        .convertPointToScreen(selectedCellPoint, this);

                    contactRightButtonMenu.setInvoker(this);

                    contactRightButtonMenu.setLocation(selectedCellPoint.x,
                        selectedCellPoint.y + renderer.getHeight());

                    contactRightButtonMenu.setVisible(true);
                }
                // Left click on the contact label opens Chat window
                else if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0
                    && e.getClickCount() > 1)
                {
                    fireContactListEvent(contact,
                        ContactListEvent.CONTACT_SELECTED, e.getClickCount());
                }
            }
            else if (component instanceof JButton)
            {
                // Click on the info button opens the info popup panel
                SwingUtilities.invokeLater(new RunInfoWindow(selectedCellPoint,
                    contact));
            }
            else if (component instanceof JPanel)
            {
                if (component.getName() != null
                    && component.getName().equals("buttonsPanel"))
                {
                    JPanel panel = (JPanel) component;

                    int internalX = translatedX
                        - (renderer.getWidth() - panel.getWidth() - 2);

                    Component c = getHorizontalComponent(panel, internalX);

                    if (c instanceof ContactProtocolButton)
                    {
                        fireContactListEvent(contact,
                            ((ContactProtocolButton) c).getProtocolContact(),
                            ContactListEvent.PROTOCOL_CONTACT_SELECTED);
                    }
                }
            }
        }
    }

    /**
     * Selects the contact or group under the right mouse click.
     * @param e the <tt>MouseEvent</tt> that notified us of the press
     */
    public void mousePressed(MouseEvent e)
    {
        // Select the meta contact under the right button click.
        if ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0
            || (e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
            || (e.isControlDown() && !e.isMetaDown()))
        {
            int index = this.locationToIndex(e.getPoint());

            if (index != -1)
                this.setSelectedIndex(index);
        }
    }

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

    /**
     * Returns the component positioned at the given x in the given container.
     * It's used like getComponentAt.
     *
     * @param c the container where to search
     * @param x the x coordinate of the searched component
     * @return the component positioned at the given x in the given container
     */
    private Component getHorizontalComponent(Container c, int x)
    {
        for (int i = 0; i < c.getComponentCount(); i++)
        {
            Component component = c.getComponent(i);
            int componentX = component.getX();

            if ((x > componentX) && (x < componentX + component.getWidth()))
                return component;
        }
        return null;
    }

    /**
     * If set to true prevents groups to be closed or opened using the mouse.
     * @param disableOpenClose the disableOpenClose to set
     */
    public void setDisableOpenClose(boolean disableOpenClose)
    {
        this.disableOpenClose = disableOpenClose;
    }

    /**
     * Runs the info window for the specified contact at the appropriate
     * position.
     */
    private class RunInfoWindow
        implements Runnable
    {
        private final MetaContact contactItem;

        private final Point p;

        private RunInfoWindow(Point p, MetaContact contactItem)
        {

            this.p = p;
            this.contactItem = contactItem;
        }

        public void run()
        {
            ContactInfoDialog contactInfoPanel = new ContactInfoDialog(mainFrame,
                contactItem);

            SwingUtilities.convertPointToScreen(p, ContactList.this);

            // TODO: to calculate popup window position properly.
            contactInfoPanel.setPopupLocation(p.x - 140, p.y - 15);

            contactInfoPanel.setVisible(true);

            contactInfoPanel.requestFocusInWindow();
        }
    }

    /**
     * Takes care of keeping the contact list up to date.
     */
    private class ContactListRefresh
        extends Thread
    {
        public void run()
        {
            try
            {
                while (refreshEnabled)
                {
                    Map<Object, String> copyContentToRefresh;

                    synchronized (contentToRefresh)
                    {
                        if (contentToRefresh.isEmpty())
                            contentToRefresh.wait();

                        copyContentToRefresh
                            = new Hashtable<Object, String>(contentToRefresh);
                        contentToRefresh.clear();
                    }

                    for (Map.Entry<Object, String> groupEntry
                            : copyContentToRefresh.entrySet())
                    {
                        String operation = groupEntry.getValue();
                        Object o = groupEntry.getKey();

                        if (o instanceof MetaContactGroup)
                        {
                            MetaContactGroup group = (MetaContactGroup) o;

                            SwingUtilities.invokeLater(new RefreshGroup(group,
                                operation));
                        }
                        else if (o instanceof MetaContact)
                        {
                            MetaContact contact = (MetaContact) o;

                            SwingUtilities.invokeLater(new RefreshContact(
                                contact, contact.getParentMetaContactGroup(),
                                operation));
                        }
                        else if (o instanceof MetaContactEvent)
                        {
                            MetaContactEvent event = (MetaContactEvent) o;
                            MetaContact contact = event.getSourceMetaContact();
                            MetaContactGroup parentGroup = event
                                .getParentGroup();

                            SwingUtilities.invokeLater(new RefreshContact(
                                contact, parentGroup, operation));
                        }
                    }
                }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        /**
         * Refreshes the given group content.
         */
        private class RefreshGroup
            implements Runnable
        {
            private final MetaContactGroup group;

            private final String operation;

            public RefreshGroup(MetaContactGroup group, String operation)
            {
                this.group = group;
                this.operation = operation;
            }

            public void run()
            {
                if (operation.equals(MODIFY_OPERATION))
                {
                    if (!listModel.isGroupClosed(group))
                    {
                        int groupIndex = listModel.indexOf(group);
                        int lastIndex = listModel
                            .countContactsAndSubgroups(group);

                        listModel.contentChanged(groupIndex, lastIndex);
                    }
                }
                else if (operation.equals(ADD_OPERATION))
                {
                    int groupIndex = listModel.indexOf(group);
                    int addedCount = listModel.countContactsAndSubgroups(group);
                    int listSize = listModel.getSize();
                    if (listSize > 0)
                    {
                        listModel.contentChanged(
                            groupIndex, listSize - addedCount - 1);
                        listModel.contentAdded(
                            listSize - addedCount, listSize - 1);
                    }
                }
                else if (operation.equals(REMOVE_OPERATION))
                {
                    int groupIndex = listModel.indexOf(group);
                    int removeCount = listModel
                        .countContactsAndSubgroups(group);
                    int listSize = listModel.getSize();

                    if (listSize > 0)
                    {
                        listModel.contentRemoved(listSize - 1, listSize
                            + removeCount - 1);
                        listModel.contentChanged(groupIndex, listSize - 1);
                    }
                }
            }
        }

        /**
         * Refreshes the given contact content.
         */
        private class RefreshContact
            implements Runnable
        {
            private final MetaContact contact;

            private final MetaContactGroup parentGroup;

            private final String operation;

            public RefreshContact(MetaContact contact,
                MetaContactGroup parentGroup, String operation)
            {
                this.contact = contact;
                this.parentGroup = parentGroup;
                this.operation = operation;
            }

            public void run()
            {
                if (operation.equals(MODIFY_OPERATION))
                {
                    int contactIndex = listModel.indexOf(contact);

                    listModel.contentChanged(contactIndex, contactIndex);
                }
                else if (operation.equals(ADD_OPERATION))
                {
                    int contactIndex = listModel.indexOf(contact);

                    if (contactIndex != -1)
                        listModel.contentAdded(contactIndex, contactIndex);
                }
                else if (operation.equals(REMOVE_OPERATION))
                {
                    int groupIndex = listModel.indexOf(parentGroup);

                    int listSize = listModel.getSize();

                    if (groupIndex != -1 && listSize > 0)
                    {
                        listModel.contentChanged(groupIndex, listSize - 1);
                        listModel.contentRemoved(listSize, listSize);
                    }
                }
            }
        }
    }

    /**
     * Refreshes the given group content.
     *
     * @param group the group to refresh
     */
    public void modifyGroup(MetaContactGroup group)
    {
        synchronized (contentToRefresh)
        {
            if (group != null
                && (!contentToRefresh.containsKey(group) || contentToRefresh
                    .get(group).equals(REMOVE_OPERATION)))
            {

                contentToRefresh.put(group, MODIFY_OPERATION);
                contentToRefresh.notifyAll();
            }
        }
    }

    /**
     * Refreshes all the contact list.
     * @param group the <tt>MetaContactGroup</tt> to add
     */
    public void addGroup(MetaContactGroup group)
    {
        synchronized (contentToRefresh)
        {
            if (group != null
                && (!contentToRefresh.containsKey(group) || contentToRefresh
                    .get(group).equals(REMOVE_OPERATION)))
            {

                contentToRefresh.put(group, ADD_OPERATION);
                contentToRefresh.notifyAll();
            }
        }
    }

    /**
     * Refreshes all the contact list.
     * @param group the <tt>MetaContactGroup</tt> to remove
     */
    public void removeGroup(MetaContactGroup group)
    {
        synchronized (contentToRefresh)
        {
            if (group != null
                && (contentToRefresh.get(group) == null || !contentToRefresh
                    .get(group).equals(REMOVE_OPERATION)))
            {

                contentToRefresh.put(group, REMOVE_OPERATION);
                contentToRefresh.notifyAll();
            }
        }
    }

    /**
     * Refreshes the given meta contact content.
     *
     * @param contact the meta contact to refresh
     */
    public void refreshContact(MetaContact contact)
    {
        synchronized (contentToRefresh)
        {
            if (contact != null
                && !contentToRefresh.containsKey(contact)
                && !contentToRefresh.containsKey(
                        contact.getParentMetaContactGroup()))
            {

                contentToRefresh.put(contact, MODIFY_OPERATION);
                contentToRefresh.notifyAll();
            }
        }
    }

    /**
     * Refreshes the whole contact list.
     */
    public void refreshAll()
    {
        this.modifyGroup(contactListService.getRoot());
    }

    /**
     * Adds the given contact to the contact list.
     * @param contact the <tt>MetaContact</tt> to add
     */
    public void addContact(MetaContact contact)
    {
        synchronized (contentToRefresh)
        {
            if (contact != null
                && !contentToRefresh.containsKey(contact)
                && !contentToRefresh.containsKey(
                        contact.getParentMetaContactGroup()))
            {

                contentToRefresh.put(contact, ADD_OPERATION);
                contentToRefresh.notifyAll();
            }
        }
    }

    /**
     * Refreshes all the contact list.
     * @param event the <tt>MetaContactEvent</tt>, from which we get the contact
     * to remove
     */
    public void removeContact(MetaContactEvent event)
    {
        synchronized (contentToRefresh)
        {
            if (event.getSourceMetaContact() != null
                    && !contentToRefresh.containsKey(event))
            {
                contentToRefresh.put(event, REMOVE_OPERATION);
                contentToRefresh.notifyAll();
            }
        }
    }

    /**
     * Selects the given object in the list.
     *
     * @param o the object to select
     */
    public void setSelectedValue(Object o)
    {
        setSelectedIndex((o == null) ? -1 : listModel.indexOf(o));
    }

    /**
     * Returns the right button menu for a contact.
     *
     * @return the right button menu for a contact
     */
    public ContactRightButtonMenu getContactRightButtonMenu()
    {
        return contactRightButtonMenu;
    }

    /**
     * Returns the right button menu for a group.
     *
     * @return the right button menu for a group
     */
    public GroupRightButtonMenu getGroupRightButtonMenu()
    {
        return groupRightButtonMenu;
    }

    /**
     * Sets the showOffline property.
     *
     * @param isShowOffline TRUE to show all offline users, FALSE to hide
     * offline users.
     */
    public void setShowOffline(boolean isShowOffline)
    {
        Object selectedObject = null;
        int currentlySelectedIndex = this.getSelectedIndex();

        if(currentlySelectedIndex != -1)
        {
            selectedObject = listModel.getElementAt(currentlySelectedIndex);
        }

        int listSize = listModel.getSize();

        listModel.setShowOffline(isShowOffline);

        ConfigurationManager.setShowOffline(isShowOffline);

        int newListSize = listModel.getSize();

        //hide offline users
        if(!isShowOffline && listSize > 0)
        {
            if(newListSize > 0)
            {
                listModel.contentChanged(0, newListSize - 1);
                listModel.contentRemoved(newListSize - 1, listSize - 1);
            }
            else
                listModel.contentRemoved(0, listSize - 1);
        }
        //show offline users
        else if(isShowOffline && newListSize > 0)
        {
            if(listSize > 0)
            {
                listModel.contentChanged(0, listSize - 1);
                listModel.contentAdded(listSize - 1, newListSize - 1);
            }
            else
                listModel.contentAdded(0, newListSize - 1);
        }

        // Try to set the previously selected object.
        if (selectedObject != null)
        {
            this.setSelectedIndex(listModel.indexOf(selectedObject));
        }
    }

    /**
     * Returns the main frame.
     *
     * @return the main frame
     */
    public MainFrame getMainFrame()
    {
        return mainFrame;
    }

    /**
     * Moves the given <tt>srcContact</tt> to the <tt>destMetaContact</tt>.
     * @param srcContact the <tt>Contact</tt> to move
     * @param destMetaContact the destination <tt>MetaContact</tt> to move to
     */
    public void moveContactToMetaContact(   Contact srcContact,
                                            MetaContact destMetaContact)
    {
        new MoveContactToMetaContactThread(srcContact, destMetaContact).start();
    }

    /**
     * Moves the given <tt>Contact</tt> to the given <tt>MetaContact</tt> and
     * asks user for confirmation.
     */
    private class MoveContactToMetaContactThread extends Thread
    {
        private final Contact srcContact;
        private final MetaContact destMetaContact;

        public MoveContactToMetaContactThread(  Contact srcContact,
                                                MetaContact destMetaContact)
        {
            this.srcContact = srcContact;
            this.destMetaContact = destMetaContact;
        }

        @SuppressWarnings("fallthrough") //intentional
        public void run()
        {
            if (!ConfigurationManager.isMoveContactConfirmationRequested())
            {
                // we move the specified contact
                mainFrame.getContactList().moveContact(
                    srcContact, destMetaContact);

                return;
            }

            String message = GuiActivator.getResources().getI18NString(
                "service.gui.MOVE_SUBCONTACT_QUESTION",
                new String[]{   srcContact.getDisplayName(),
                                destMetaContact.getDisplayName()});

            MessageDialog dialog = new MessageDialog(
                    mainFrame,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE_CONTACT"),
                    message,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE"));

            switch (dialog.showDialog())
            {
            case MessageDialog.OK_DONT_ASK_CODE:
                ConfigurationManager.setMoveContactConfirmationRequested(false);
                // do fall through

            case MessageDialog.OK_RETURN_CODE:
                // we move the specified contact
                mainFrame.getContactList().moveContact(
                    srcContact, destMetaContact);
                break;
            }
        }
    }

    /**
     * Moves the given <tt>srcMetaContact</tt> to the <tt>destMetaContact</tt>.
     * @param srcMetaContact the <tt>MetaContact</tt> to move
     * @param destMetaContact the destination <tt>MetaContact</tt> to move to
     */
    public void moveMetaContactToMetaContact(   MetaContact srcMetaContact,
                                                MetaContact destMetaContact)
    {
        new MoveMetaContactToMetaContactThread(srcMetaContact, destMetaContact)
            .start();
    }

    /**
     * Moves the given <tt>Contact</tt> to the given <tt>MetaContact</tt> and
     * asks user for confirmation.
     */
    private class MoveMetaContactToMetaContactThread extends Thread
    {
        private final MetaContact srcMetaContact;
        private final MetaContact destMetaContact;

        public MoveMetaContactToMetaContactThread(  MetaContact srcContact,
                                                    MetaContact destMetaContact)
        {
            this.srcMetaContact = srcContact;
            this.destMetaContact = destMetaContact;
        }

        @SuppressWarnings("fallthrough") //intentional
        public void run()
        {
            if (!ConfigurationManager.isMoveContactConfirmationRequested())
            {
                // We move all subcontacts of the source MetaContact to the
                // destination MetaContact.
                this.moveAllSubcontacts();

                return;
            }

            String message = GuiActivator.getResources().getI18NString(
                "service.gui.MOVE_SUBCONTACT_QUESTION",
                new String[]{   srcMetaContact.getDisplayName(),
                                destMetaContact.getDisplayName()});

            MessageDialog dialog = new MessageDialog(
                    mainFrame,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE_CONTACT"),
                    message,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE"));

            switch (dialog.showDialog())
            {
            case MessageDialog.OK_DONT_ASK_CODE:
                ConfigurationManager.setMoveContactConfirmationRequested(false);
                // do fall through

            case MessageDialog.OK_RETURN_CODE:
                // We move all subcontacts of the source MetaContact to the
                // destination MetaContact.
                this.moveAllSubcontacts();
                break;
            }
        }

        /**
         * Move all subcontacts of the <tt>srcMetaContact</tt> to
         * <tt>destMetaContact</tt>.
         */
        private void moveAllSubcontacts()
        {
            Iterator<Contact> contacts = srcMetaContact.getContacts();
            while(contacts.hasNext())
            {
                mainFrame.getContactList().moveContact(
                    contacts.next(), destMetaContact);
            }
        }
    }

    /**
     * Moves the given <tt>srcContact</tt> to the <tt>destGroup</tt>.
     * @param srcContact the <tt>Contact</tt> to move
     * @param destGroup the destination <tt>MetaContactGroup</tt> to move to
     */
    public void moveContactToGroup( Contact srcContact,
                                    MetaContactGroup destGroup)
    {
        new MoveContactToGroupThread(srcContact, destGroup).start();
    }

    /**
     * Moves the given <tt>Contact</tt> to the given <tt>MetaContactGroup</tt>
     * and asks user for confirmation.
     */
    @SuppressWarnings("fallthrough")
    private class MoveContactToGroupThread extends Thread
    {
        private final Contact srcContact;
        private final MetaContactGroup destGroup;

        public MoveContactToGroupThread(Contact srcContact,
                                        MetaContactGroup destGroup)
        {
            this.srcContact = srcContact;
            this.destGroup = destGroup;
        }

        public void run()
        {
            if (!ConfigurationManager.isMoveContactConfirmationRequested())
            {
                // we move the specified contact
                mainFrame.getContactList().moveContact(
                    srcContact, destGroup);

                return;
            }

            String message = GuiActivator.getResources().getI18NString(
                "service.gui.MOVE_SUBCONTACT_QUESTION",
                new String[]{   srcContact.getDisplayName(),
                                destGroup.getGroupName()});

            MessageDialog dialog = new MessageDialog(
                    mainFrame,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE_CONTACT"),
                    message,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE"));

            switch (dialog.showDialog())
            {
            case MessageDialog.OK_DONT_ASK_CODE:
                ConfigurationManager.setMoveContactConfirmationRequested(false);
                // do fall through

            case MessageDialog.OK_RETURN_CODE:
                // we move the specified contact
                mainFrame.getContactList().moveContact(
                    srcContact, destGroup);
                break;
            }
        }
    }

    /**
     * Moves the given <tt>srcContact</tt> to the <tt>destGroup</tt>.
     * @param srcContact the <tt>MetaContact</tt> to move
     * @param destGroup the destination <tt>MetaContactGroup</tt> to move to
     */
    public void moveMetaContactToGroup( MetaContact srcContact,
                                        MetaContactGroup destGroup)
    {
        new MoveMetaContactThread(srcContact, destGroup).start();
    }

    /**
     * Moves the given <tt>MetaContact</tt> to the given <tt>MetaContactGroup</tt>
     * and asks user for confirmation.
     */
    private class MoveMetaContactThread
        extends Thread
    {
        private final MetaContact srcContact;
        private final MetaContactGroup destGroup;

        public MoveMetaContactThread(   MetaContact srcContact,
                                        MetaContactGroup destGroup)
        {
            this.srcContact = srcContact;
            this.destGroup = destGroup;
        }

        @SuppressWarnings("fallthrough")
        public void run()
        {
            if (!ConfigurationManager.isMoveContactConfirmationRequested())
            {
                // we move the specified contact
                try
                {
                    mainFrame.getContactList().moveMetaContact(
                        srcContact, destGroup);
                }
                catch (MetaContactListException e)
                {
                    
                }

                return;
            }

            String message = GuiActivator.getResources().getI18NString(
                "service.gui.MOVE_SUBCONTACT_QUESTION",
                new String[]{   srcContact.getDisplayName(),
                                destGroup.getGroupName()});

            MessageDialog dialog = new MessageDialog(
                    mainFrame,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE_CONTACT"),
                    message,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE"));

            switch (dialog.showDialog())
            {
            case MessageDialog.OK_DONT_ASK_CODE:
                ConfigurationManager.setMoveContactConfirmationRequested(false);
                // do fall through

            case MessageDialog.OK_RETURN_CODE:
                // we move the specified contact
                mainFrame.getContactList().moveMetaContact(
                    srcContact, destGroup);
                break;
            }
        }
    }

    /**
     * Adds the given <tt>MetaContact</tt> to the list of active contacts.
     *
     * @param metaContact the <tt>MetaContact</tt> to add.
     */
    public void addActiveContact(MetaContact metaContact)
    {
        synchronized (activeContacts)
        {
            if(activeContacts.size() == 0)
            {
                SystrayService stray = GuiActivator.getSystrayService();

                if (stray != null)
                    stray.setSystrayIcon(SystrayService.ENVELOPE_IMG_TYPE);
            }

            if(!activeContacts.contains(metaContact))
                activeContacts.add(metaContact);
        }

        this.refreshContact(metaContact);
    }

    /**
     * Removes the given <tt>MetaContact</tt> from the list of active contacts.
     *
     * @param metaContact the <tt>MetaContact</tt> to remove.
     */
    public void removeActiveContact(MetaContact metaContact)
    {
        synchronized (activeContacts)
        {
            activeContacts.remove(metaContact);

            if(activeContacts.size() == 0)
                GuiActivator.getSystrayService().setSystrayIcon(
                   SystrayService.SC_IMG_TYPE);
        }

        this.refreshContact(metaContact);
    }

    /**
     * Removes all contacts from the list of active contacts.
     */
    public void removeAllActiveContacts()
    {
        synchronized (activeContacts)
        {
            if(activeContacts.size() > 0)
            {
                activeContacts.clear();

                GuiActivator.getSystrayService().setSystrayIcon(
                   SystrayService.SC_IMG_TYPE);
            }
        }

        this.refreshAll();
    }

    /**
     * Checks if the given contact is currently active.
     *
     * @param metaContact the <tt>MetaContact</tt> to verify
     * @return TRUE if the given <tt>MetaContact</tt> is active, FALSE -
     * otherwise
     */
    public boolean isMetaContactActive(MetaContact metaContact)
    {
        synchronized (activeContacts)
        {
            return activeContacts.contains(metaContact);
        }
    }

    /**
     * Resets the contained mouse listeners and adds the given one. This allows
     * other components to integrate the contact list by specifying their own
     * mouse events.
     *
     * @param l the mouse listener to set.
     */
    public void setMouseListener(MouseListener l)
    {
        this.removeMouseListener(this);
        this.addMouseListener(l);
    }

    /**
     * Checks whether the group is closed.
     *
     * @param group The group to check.
     * @return True if the group is closed, false - otherwise.
     */
    @Override
    public boolean isGroupClosed(MetaContactGroup group)
    {
        return this.listModel.isGroupClosed(group);
    }
}
