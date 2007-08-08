/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>ContactList</tt> is a JList that represents the contact list. A
 * custom data model and a custom list cell renderer is used. This class manages
 * all meta contact list events, like <code>metaContactAdded</code>,
 * <code>metaContactMoved</code>, <code>metaContactGroupAdded</code>, etc.
 * 
 * @author Yana Stamcheva
 */
public class ContactList
    extends JList
    implements  MetaContactListListener,
                MouseListener,
                MouseMotionListener
{

    private static final String ADD_OPERATION = "AddOperation";

    private static final String REMOVE_OPERATION = "RemoveOperation";

    private static final String MODIFY_OPERATION = "ModifyOperation";

    private Logger logger = Logger.getLogger(ContactList.class.getName());

    private MetaContactListService contactList;

    private ContactListModel listModel;

    private Object currentlySelectedObject;

    private Vector contactListListeners = new Vector();

    private Vector excContactListListeners = new Vector();

    private MainFrame mainFrame;

    private Hashtable contentToRefresh = new Hashtable();

    private boolean refreshEnabled = true;

    private GroupRightButtonMenu groupRightButtonMenu;

    private ContactRightButtonMenu contactRightButtonMenu;

    private Hashtable contactHistory = new Hashtable();

    private ContactListDraggable draggedElement;
    
    /**
     * Creates an instance of the <tt>ContactList</tt>.
     * 
     * @param mainFrame The main application window.
     */
    public ContactList(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;

        this.contactList = mainFrame.getContactList();

        this.listModel = new ContactListModel(contactList);

        this.setModel(listModel);

        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        this.getSelectionModel().setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION);

        this.setCellRenderer(new ContactListCellRenderer(mainFrame));

        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        this.contactList.addMetaContactListListener(this);

        this.addMouseListener(this);
        this.addMouseMotionListener(this);

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

        this.setShowOffline(ConfigurationManager.isShowOffline());
       
        new ContactListRefresh().start();
    }
    
    /**
     * Handles the <tt>MetaContactEvent</tt>. Refreshes the list model.
     */
    public void metaContactAdded(MetaContactEvent evt)
    {
        this.addContact(evt.getSourceMetaContact());
    }

    /**
     * Handles the <tt>MetaContactRenamedEvent</tt>. Refreshes the list when
     * a meta contact is renamed.
     */
    public void metaContactRenamed(MetaContactRenamedEvent evt)
    {
        this.refreshContact(evt.getSourceMetaContact());
    }

    /**
     * Handles the <tt>ProtoContactEvent</tt>. Refreshes the list when a
     * protocol contact has been added.
     */
    public void protoContactAdded(ProtoContactEvent evt)
    {
        MetaContact parentMetaContact = evt.getNewParent();
        
        this.refreshContact(parentMetaContact);
    }

    /**
     * Handles the <tt>ProtoContactEvent</tt>. Refreshes the list when a
     * protocol contact has been removed.
     */
    public void protoContactRemoved(ProtoContactEvent evt)
    {
        this.refreshContact(evt.getOldParent());
    }

    /**
     * Handles the <tt>ProtoContactEvent</tt>. Refreshes the list when a
     * protocol contact has been moved.
     */
    public void protoContactMoved(ProtoContactEvent evt)
    {
        MetaContact oldParentMetaContact = evt.getOldParent();
        MetaContact newParentMetaContact = evt.getNewParent();
        
        this.refreshContact(oldParentMetaContact);
        this.refreshContact(newParentMetaContact);
    }

    /**
     * Handles the <tt>MetaContactEvent</tt>. Refreshes the list when a meta
     * contact has been removed.
     */
    public void metaContactRemoved(MetaContactEvent evt)
    {
        this.removeContact(evt);
    }

    /**
     * Handles the <tt>MetaContactMovedEvent</tt>. Refreshes the list when a
     * meta contact has been moved.
     */
    public void metaContactMoved(MetaContactMovedEvent evt)
    {
        this.modifyGroup(evt.getNewParent());
        this.modifyGroup(evt.getOldParent());
    }

    /**
     * Handles the <tt>MetaContactGroupEvent</tt>. Refreshes the list model
     * when a new meta contact group has been added.
     */
    public void metaContactGroupAdded(MetaContactGroupEvent evt)
    {
        MetaContactGroup group = evt.getSourceMetaContactGroup();

        if (!group.equals(contactList.getRoot()))
            this.addGroup(group);
    }

    /**
     * Handles the <tt>MetaContactGroupEvent</tt>. Refreshes the list when a
     * meta contact group has been modified.
     */
    public void metaContactGroupModified(MetaContactGroupEvent evt)
    {
        MetaContactGroup group = evt.getSourceMetaContactGroup();

        if (!group.equals(contactList.getRoot()))
            this.modifyGroup(evt.getSourceMetaContactGroup());
    }

    /**
     * Handles the <tt>MetaContactGroupEvent</tt>. Refreshes the list when a
     * meta contact group has been removed.
     */
    public void metaContactGroupRemoved(MetaContactGroupEvent evt)
    {
        MetaContactGroup group = evt.getSourceMetaContactGroup();

        if (!group.equals(contactList.getRoot()))
            this.removeGroup(evt.getSourceMetaContactGroup());
    }

    /**
     * Handles the <tt>MetaContactGroupEvent</tt>. Refreshes the list model
     * when the contact list groups has been reordered. Moves the selection
     * index to the index of the contact that was selected before the reordered
     * event. This way the selection depends on the contact and not on the
     * index.
     */
    public void childContactsReordered(MetaContactGroupEvent evt)
    {
        if (currentlySelectedObject != null)
            setSelectedValue(currentlySelectedObject);
        this.modifyGroup(evt.getSourceMetaContactGroup());
    }

    /**
     * Returns the next list element that starts with a prefix.
     * 
     * @param prefix the string to test for a match
     * @param startIndex the index for starting the search
     * @param bias the search direction, either Position.Bias.Forward or
     *            Position.Bias.Backward.
     * @return the index of the next list element that starts with the prefix;
     *         otherwise -1
     */
    public int getNextMatch(String prefix, int startIndex, Position.Bias bias)
    {
        ContactListModel model = (ContactListModel) this.getModel();

        int max = model.getSize();

        if (prefix == null)
        {
            throw new IllegalArgumentException();
        }

        if (startIndex < 0 || startIndex >= max)
        {
            throw new IllegalArgumentException();
        }

        prefix = prefix.toUpperCase();

        // start search from the next element after the selected element
        int increment = (bias == Position.Bias.Forward) ? 1 : -1;
        int index = startIndex;
        do
        {
            Object o = model.getElementAt(index);

            if (o != null)
            {
                String contactName = null;

                if (o instanceof MetaContact)
                {
                    contactName = ((MetaContact) o).getDisplayName()
                        .toUpperCase();
                }

                if (contactName != null && contactName.startsWith(prefix))
                {
                    return index;
                }
            }
            index = (index + increment + max) % max;
        } while (index != startIndex);
        return -1;
    }

    /**
     * Returns the list of all groups.
     * 
     * @return The list of all groups.
     */
    public Iterator getAllGroups()
    {
        return contactList.getRoot().getSubgroups();
    }

    /**
     * Returns the Meta Contact Group corresponding to the given MetaUID.
     * 
     * @param metaUID An identifier of a group.
     * @return The Meta Contact Group corresponding to the given MetaUID.
     */
    public MetaContactGroup getGroupByID(String metaUID)
    {
        Iterator i = contactList.getRoot().getSubgroups();
        while (i.hasNext())
        {
            MetaContactGroup group = (MetaContactGroup) i.next();

            if (group.getMetaUID().equals(metaUID))
            {
                return group;
            }
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
                this.contactListListeners.add(listener);
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
            this.contactListListeners.remove(listener);
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
                this.excContactListListeners.add(listener);
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
            this.excContactListListeners.remove(listener);
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

        if (excContactListListeners.size() > 0)
        {
            synchronized (excContactListListeners)
            {
                Iterator listeners = new Vector(this.excContactListListeners)
                    .iterator();

                while (listeners.hasNext())
                {
                    ContactListListener listener
                        = (ContactListListener) listeners.next();
                    
                    switch (evt.getEventID())
                    {
                        case ContactListEvent.CONTACT_SELECTED:
                            listener.contactClicked(evt);
                            break;
                        case ContactListEvent.PROTOCOL_CONTACT_SELECTED:
                            listener.protocolContactClicked(evt);
                            break;
                        case ContactListEvent.GROUP_SELECTED:
                            listener.groupSelected(evt);
                            break;
                        default:
                            logger.error("Unknown event type "
                                + evt.getEventID());
                    }
                }
            }
        }
        else
        {
            synchronized (contactListListeners)
            {
                Iterator listeners = this.contactListListeners.iterator();

                while (listeners.hasNext())
                {
                    ContactListListener listener = (ContactListListener) listeners
                        .next();
                    switch (evt.getEventID())
                    {
                    case ContactListEvent.CONTACT_SELECTED:
                        listener.contactClicked(evt);
                        break;
                    case ContactListEvent.PROTOCOL_CONTACT_SELECTED:
                        listener.protocolContactClicked(evt);
                        break;
                    case ContactListEvent.GROUP_SELECTED:
                        listener.groupSelected(evt);
                        break;
                    default:
                        logger.error("Unknown event type " + evt.getEventID());
                    }
                }
            }
        }
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
        ContactListEvent evt = new ContactListEvent(sourceContact,
            protocolContact, eventID);

        synchronized (contactListListeners)
        {
            Iterator listeners = this.contactListListeners.iterator();

            while (listeners.hasNext())
            {
                ContactListListener listener = (ContactListListener) listeners
                    .next();
                switch (evt.getEventID())
                {
                case ContactListEvent.CONTACT_SELECTED:
                    listener.contactClicked(evt);
                    break;
                case ContactListEvent.PROTOCOL_CONTACT_SELECTED:
                    listener.protocolContactClicked(evt);
                    break;
                default:
                    logger.error("Unknown event type " + evt.getEventID());
                }
            }
        }
    }

    
    /**
     * Manages a mouse click over the contact list.
     * 
     * When the left mouse button is clicked on a contact cell different things
     * may happen depending on the contained component under the mouse. If the
     * mouse is clicked on the "contact name" the chat window is opened,
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
     */
    public void mouseClicked(MouseEvent e)
    {        
        int selectedIndex = this.getSelectedIndex();
        Object selectedValue = this.getSelectedValue();

        ContactListCellRenderer renderer = (ContactListCellRenderer) this
            .getCellRenderer().getListCellRendererComponent(this,
                selectedValue, selectedIndex, true, true);

        Point selectedCellPoint = this.indexToLocation(selectedIndex);

        int translatedX = e.getX() - selectedCellPoint.x;

        if (selectedValue instanceof MetaContactGroup)
        {
            MetaContactGroup group = (MetaContactGroup) selectedValue;
            
            // Closes or opens a group on a double click.
            if (e.getClickCount() > 1)
            {   
                if (listModel.isGroupClosed(group))
                {
                    listModel.openGroup(group);
                }
                else
                {
                    listModel.closeGroup(group);
                }
            }

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
                fireContactListEvent(   group,
                                        ContactListEvent.GROUP_SELECTED,
                                        e.getClickCount());

                // get the component under the mouse
                Component component = this.getHorizontalComponent(renderer,
                    translatedX);

                if (component instanceof JPanel)
                {
                    if (component.getName() != null
                        && component.getName().equals("buttonsPanel"))
                    {
                        JPanel panel = (JPanel) component;

                        int internalX = translatedX
                            - (renderer.getWidth() - panel.getWidth() - 2);

                        Component c = getHorizontalComponent(panel, internalX);

                        if (c instanceof JLabel)
                        {
                            if (listModel.isGroupClosed(group))
                            {
                                listModel.openGroup(group);
                            }
                            else
                            {
                                listModel.closeGroup(group);
                            }
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
                else if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0)
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

    public void mouseEntered(MouseEvent e)
    {
    }
    
    public void mouseExited(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
        // Request the focus in the mContact list when user clicks on it.
        this.requestFocus();

        draggedElement = null;

        // Select the mContact under the right button click.
        if ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0
            || (e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
            || (e.isControlDown() && !e.isMetaDown()))
        {
            this.setSelectedIndex(locationToIndex(e.getPoint()));
        }

        int selectedIndex = this.getSelectedIndex();
        Object selectedValue = this.getSelectedValue();

        ContactListCellRenderer renderer = (ContactListCellRenderer) this
            .getCellRenderer().getListCellRendererComponent(this,
                selectedValue, selectedIndex, true, true);

        Point selectedCellPoint = this.indexToLocation(selectedIndex);

        int translatedX = e.getX() - selectedCellPoint.x;

        if (selectedValue instanceof MetaContact)
        {
            MetaContact mContact = (MetaContact) selectedValue;

            // get the component under the mouse
            Component component
                = this.getHorizontalComponent(renderer, translatedX);

            if (component instanceof JLabel)
            {
                draggedElement = new ContactListDraggable(
                    mContact, null, component);
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
                        draggedElement = new ContactListDraggable(mContact,
                            ((ContactProtocolButton) c).getProtocolContact()
                                , component);
                    }
                }
            }
            if (draggedElement != null)
            {
                Component src = draggedElement.getComponent();
                Component c = src;

                Image image;
                if (c instanceof ContactProtocolButton)
                {
                    // TODO : how to get the icon in front of a metacontact
                    // in the contact list ?
                    image = new BufferedImage(
                        ((ContactProtocolButton) c).getWidth(),
                        ((ContactProtocolButton) c).getHeight(),
                        BufferedImage.TYPE_INT_ARGB);
                }
                else
                {
                    image = new BufferedImage(c.getWidth(), c.getHeight(),
                            BufferedImage.TYPE_INT_ARGB);
                }
                Graphics g = image.getGraphics();
                c.paint(g);
                draggedElement.setImage(image);

                Point p = (Point) e.getPoint().clone();
                SwingUtilities.convertPointToScreen(p, c);
                SwingUtilities.convertPointFromScreen(p, this);
                draggedElement.setLocation(p);
            }
        }
    }
    
    public void mouseReleased(MouseEvent e)
    {
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        int selectedIndex = this.locationToIndex(e.getPoint());

        ContactListModel listModel = (ContactListModel) this.getModel();

        Object dest = listModel.getElementAt(selectedIndex);
        if (draggedElement != null)
        {
            if (dest instanceof MetaContact)
            {
                MetaContact contactDest = (MetaContact) dest;
                if (draggedElement.getMetaContact() != contactDest)
                {
                    if (draggedElement.getContact() != null)
                    {
                        // we move the specified contact
                        mainFrame.getContactList().moveContact(
                            draggedElement.getContact(), contactDest);
                    }
                    else
                    {
                        // we move all contacts from this meta contact
                        Iterator i = draggedElement.
                            getMetaContact().getContacts();

                        while(i.hasNext())
                        {
                            Contact contact = (Contact) i.next();
                            mainFrame.getContactList().
                                moveContact(contact, contactDest);
                        }
 
                    }
                }
            }
            else if (dest instanceof MetaContactGroup)
            {
                MetaContactGroup contactDest = (MetaContactGroup) dest;
                if (draggedElement.getContact() != null)
                {
                    int i = draggedElement.getMetaContact().getContactCount();
                    if (i > 1)
                    {
                        mainFrame.getContactList().moveContact(
                            draggedElement.getContact(), contactDest);
                    }
                    else if (!contactDest.contains(
                        draggedElement.getMetaContact()))
                    {
                        mainFrame.getContactList().moveContact(
                            draggedElement.getContact(), contactDest);                        
                    }
                }
                else if (!contactDest.contains(draggedElement.getMetaContact()))
                {
                    mainFrame.getContactList().moveMetaContact(
                        draggedElement.getMetaContact(), contactDest);
                }
            }
        }
        draggedElement = null;
    }

    public void mouseDragged(MouseEvent e)
    {
        if (draggedElement != null)
        {
            try 
            {
                setCursor(Cursor.getSystemCustomCursor("MoveDrop.32x32"));
            }
            catch (Exception ex)
            {
                logger.debug("Cursor \"MoveDrop.32x32\" isn't available ");
                setCursor(Cursor.getDefaultCursor());
            }

            Point p = (Point) e.getPoint().clone();

            draggedElement.setLocation(p);

//            TODO: give a more attractive visual feedback for the DnD operation,
//            by drawing the image provided by draggedElement.getImage() at
//            coordinates draggedElement.getPoint()
//            Graphics2D g2 = (Graphics2D) getGraphics();
//            Image img = draggedElement.getImage();
//            g2.drawImage(img
//                , (int)
//                (draggedElement.getPoint().getX() - img.getWidth(this) / 2)
//                , (int)
//                (draggedElement.getPoint().getY() - img.getHeight(this) / 2)
//                , null);
        }
    }

    public void mouseMoved(MouseEvent e)
    {
    }

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
        Component innerComponent = null;
        int width;
        for (int i = 0; i < c.getComponentCount(); i++)
        {
            innerComponent = c.getComponent(i);
            width = innerComponent.getWidth();
            if (x > innerComponent.getX() && x < innerComponent.getX() + width)
            {
                return innerComponent;
            }
        }
        return null;
    }

    /**
     * Runs the info window for the specified contact at the appropriate
     * position.
     */
    private class RunInfoWindow
        implements Runnable
    {

        private MetaContact contactItem;

        private Point p;

        private RunInfoWindow(Point p, MetaContact contactItem)
        {

            this.p = p;
            this.contactItem = contactItem;
        }

        public void run()
        {

            ContactInfoPanel contactInfoPanel = new ContactInfoPanel(mainFrame,
                contactItem);

            SwingUtilities.convertPointToScreen(p, ContactList.this);

            // TODO: to calculate popup window posititon properly.
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
                Map copyContentToRefresh = null;

                while (refreshEnabled)
                {

                    synchronized (contentToRefresh)
                    {
                        if (contentToRefresh.isEmpty())
                            contentToRefresh.wait();

                        copyContentToRefresh = new Hashtable(contentToRefresh);
                        contentToRefresh.clear();
                    }

                    Iterator i = copyContentToRefresh.entrySet().iterator();
                    while (i.hasNext())
                    {
                        Map.Entry groupEntry = (Map.Entry) i.next();

                        String operation = (String) groupEntry.getValue();

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
         * 
         * @param group the group to update
         */
        private class RefreshGroup
            implements
            Runnable
        {
            private MetaContactGroup group;

            private String operation;

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
         * 
         * @param group the contact to refresh
         */
        private class RefreshContact
            implements
            Runnable
        {
            private MetaContact contact;

            private MetaContactGroup parentGroup;

            private String operation;

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
                && !contentToRefresh.containsKey(contact
                    .getParentMetaContactGroup()))
            {

                contentToRefresh.put(contact, MODIFY_OPERATION);
                contentToRefresh.notifyAll();
            }
        }
    }

    /**
     * Adds the given contact to the contact list.
     */
    public void addContact(MetaContact contact)
    {
        synchronized (contentToRefresh)
        {
            if (contact != null
                && !contentToRefresh.containsKey(contact)
                && !contentToRefresh.containsKey(contact
                    .getParentMetaContactGroup()))
            {

                contentToRefresh.put(contact, ADD_OPERATION);
                contentToRefresh.notifyAll();
            }
        }
    }

    /**
     * Refreshes all the contact list.
     */
    public void removeContact(MetaContactEvent event)
    {
        MetaContact metaContact = event.getSourceMetaContact();
        synchronized (contentToRefresh)
        {
            if (metaContact != null && !contentToRefresh.contains(event))
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
        if (o == null)
        {
            setSelectedIndex(-1);
        }
        else
        {
            int i = listModel.indexOf(o);
            this.setSelectedIndex(i);
        }
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
     *            offline users.
     */
    public void setShowOffline(boolean isShowOffline)
    {
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
     * Checks if there's an open history window for the given contact.
     * 
     * @param contact the contact to check for
     * @return TRUE if there's an opened history window for the given contact,
     *         FALSE otherwise.
     */
    public boolean containsHistoryWindowForContact(MetaContact contact)
    {
        return contactHistory.containsKey(contact);
    }

    /**
     * Returns the history window for the given contact.
     * 
     * @param contact the contact to search for
     * @return the history window for the given contact
     */
    public HistoryWindow getHistoryWindowForContact(MetaContact contact)
    {
        return (HistoryWindow) contactHistory.get(contact);
    }

    /**
     * Adds a history window for a given contact in the table of opened history
     * windows.
     * 
     * @param contact the contact to add
     * @param historyWindow the history window to add
     */
    public void addHistoryWindowForContact(MetaContact contact,
        HistoryWindow historyWindow)
    {
        contactHistory.put(contact, historyWindow);
    }

    /**
     * Removes the history window for the given contact.
     * 
     * @param contact the contact to remove the history window
     */
    public void removeHistoryWindowForContact(MetaContact contact)
    {
        contactHistory.remove(contact);
    }
}
