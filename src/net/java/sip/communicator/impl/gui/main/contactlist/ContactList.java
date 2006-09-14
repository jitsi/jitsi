/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;

/**
 * The <tt>ContactList</tt> is a JList that represents the contact list. A
 * custom data model and a custom list cell renderer is used. This class
 * manages all meta contact list events, like <code>metaContactAdded</code>,
 * <code>metaContactMoved</code>, <code>metaContactGroupAdded</code>, etc. 
 *
 * @author Yana Stamcheva
 */
public class ContactList extends JList
    implements MetaContactListListener {

    private MetaContactListService contactList;

    private ContactListModel listModel;

    private MetaContact currentlySelectedContact;
    
    /**
     * Creates an instance of the <tt>ContactList</tt>.
     *
     * @param contactList The related meta contactlist.
     */
    public ContactList(MetaContactListService contactList) {

        this.contactList = contactList;

        this.listModel = new ContactListModel(contactList);

        this.setModel(listModel);

        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        this.getSelectionModel().setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        this.setCellRenderer(new ContactListCellRenderer());

        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        this.contactList.addMetaContactListListener(this);

        this.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (getSelectedValue() instanceof MetaContact) {
                    currentlySelectedContact = (MetaContact) getSelectedValue();
                }
            }
        });
    }

    /**
     * Handles the <tt>MetaContactEvent</tt>.
     * Refreshes the list model.
     */
    public void metaContactAdded(MetaContactEvent evt) {
        int index = this.listModel.indexOf(evt.getSourceMetaContact());

        this.listModel.contentAdded(index, index);
    }

    /**
     * Handles the <tt>MetaContactRenamedEvent</tt>.
     * Refreshes the list when a meta contact is renamed.
     */
    public void metaContactRenamed(MetaContactRenamedEvent evt) {
        this.revalidate();
    }

    /**
     * Handles the <tt>ProtoContactEvent</tt>.
     * Refreshes the list when a protocol contact has been added.
     */
    public void protoContactAdded(ProtoContactEvent evt) {
        this.revalidate();
    }

    /**
     * Handles the <tt>ProtoContactEvent</tt>.
     * Refreshes the list when a protocol contact has been removed.
     */
    public void protoContactRemoved(ProtoContactEvent evt) {
        this.revalidate();
    }

    /**
     * Handles the <tt>ProtoContactEvent</tt>.
     * Refreshes the list when a protocol contact has been moved.
     */
    public void protoContactMoved(ProtoContactEvent evt) {
        this.revalidate();
    }

    /**
     * Handles the <tt>MetaContactEvent</tt>.
     * Refreshes the list when a meta contact has been removed.
     */
    public void metaContactRemoved(MetaContactEvent evt) {
        this.revalidate();
    }

    /**
     * Handles the <tt>MetaContactMovedEvent</tt>.
     * Refreshes the list when a meta contact has been moved.
     */
    public void metaContactMoved(MetaContactMovedEvent evt) {
        this.revalidate();
    }

    /**
     * Handles the <tt>MetaContactGroupEvent</tt>.
     * Refreshes the list model when a new meta contact group has been added.
     */
    public void metaContactGroupAdded(MetaContactGroupEvent evt) {
        MetaContactGroup sourceGroup = evt.getSourceMetaContactGroup();

        this.groupAdded(sourceGroup);

        //this.ensureIndexIsVisible(0);
        
        this.revalidate();
    }

    /**
     * Handles the <tt>MetaContactGroupEvent</tt>.
     * Refreshes the list when a meta contact group has been modified.
     */
    public void metaContactGroupModified(MetaContactGroupEvent evt) {
        this.revalidate();
    }

    /**
     * Handles the <tt>MetaContactGroupEvent</tt>.
     * Refreshes the list when a meta contact group has been removed.
     */
    public void metaContactGroupRemoved(MetaContactGroupEvent evt) {
        this.revalidate();
    }

    /**
     * Handles the <tt>MetaContactGroupEvent</tt>.
     * Refreshes the list model when the contact list groups has been
     * reordered. Moves the selection index to the index of the contact
     * that was selected before the reordered event. This way the selection
     * depends on the contact and not on the index.
     */
    public void childContactsReordered(MetaContactGroupEvent evt) {

        MetaContactGroup group = evt.getSourceMetaContactGroup();

        int startIndex = this.listModel.indexOf(group.getMetaContact(0));
        int endIndex = this.listModel.indexOf(group.getMetaContact(group
                .countChildContacts() - 1));

        this.listModel.contentChanged(startIndex, endIndex);

        if (currentlySelectedContact != null)
            this.setSelectedValue(currentlySelectedContact, false);
    }

    /**
     * Refreshes the list model when a group is added.
     *
     * @param group The group which is added.
     */
    private void groupAdded(MetaContactGroup group) {

        int index = this.listModel.indexOf(group);

        this.listModel.contentAdded(index, index);

        Iterator childContacts = group.getChildContacts();

        while (childContacts.hasNext()) {
            MetaContact contact = (MetaContact) childContacts.next();

            int contactIndex = this.listModel.indexOf(contact);
            this.listModel.contentAdded(contactIndex, contactIndex);
        }

        Iterator subGroups = group.getSubgroups();

        while (subGroups.hasNext()) {
            MetaContactGroup subGroup = (MetaContactGroup) subGroups.next();

            this.groupAdded(subGroup);
        }
    }

    /**
     * Returns the next list element that starts with
     * a prefix.
     *
     * @param prefix the string to test for a match
     * @param startIndex the index for starting the search
     * @param bias the search direction, either
     * Position.Bias.Forward or Position.Bias.Backward.
     * @return the index of the next list element that
     * starts with the prefix; otherwise -1
     */
    public int getNextMatch(String prefix, int startIndex, Position.Bias bias) {
        ContactListModel model = (ContactListModel) this.getModel();
        int max = model.getSize();
        if (prefix == null) {
            throw new IllegalArgumentException();
        }
        if (startIndex < 0 || startIndex >= max) {
            throw new IllegalArgumentException();
        }
        prefix = prefix.toUpperCase();

        // start search from the next element after the selected element
        int increment = (bias == Position.Bias.Forward) ? 1 : -1;
        int index = startIndex;
        do {
            Object o = model.getElementAt(index);

            if (o != null) {
                String contactName = null;

                if (o instanceof MetaContact) {
                    contactName = ((MetaContact) o).getDisplayName()
                            .toUpperCase();
                }

                if (contactName != null && contactName.startsWith(prefix)) {
                    return index;
                }
            }
            index = (index + increment + max) % max;
        } while (index != startIndex);
        return -1;
    }

    /**
     * Returns the list of all groups. 
     * @return The list of all groups.
     */
    public Iterator getAllGroups() {        
        return contactList.getRoot().getSubgroups();
    }
    
    /**
     * Returns the Meta Contact Group corresponding to the given MetaUID.
     * 
     * @param metaUID An identifier of a group.
     * @return The Meta Contact Group corresponding to the given MetaUID.
     */
    public MetaContactGroup getGroupByID(String metaUID) {
        Iterator i = contactList.getRoot().getSubgroups();
        while(i.hasNext()){
            MetaContactGroup group = (MetaContactGroup)i.next();
            
            if(group.getMetaUID().equals(metaUID)) {
                return group;
            }
        }
        return null;
    }
}
