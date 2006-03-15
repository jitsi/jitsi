/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.Cursor;
import java.util.Enumeration;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.text.Position;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.java.sip.communicator.impl.gui.main.ui.SIPCommTreeUI;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.contactlist.event.MetaContactEvent;
import net.java.sip.communicator.service.contactlist.event.MetaContactGroupEvent;
import net.java.sip.communicator.service.contactlist.event.MetaContactListListener;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.PresenceStatus;

public class ContactList extends JList
    implements MetaContactListListener {

    private MetaContactListService contactList;

    private ContactListModel listModel = new ContactListModel();

    public ContactList(MetaContactListService contactList){

        this.contactList = contactList;

        this.contactList.addContactListListener(this);

        this.setModel(listModel);

        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        this.getSelectionModel().setSelectionMode
                (TreeSelectionModel	.SINGLE_TREE_SELECTION);

        this.setCellRenderer(new ContactListCellRenderer());

        this.putClientProperty("JTree.lineStyle", "None");

        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        this.addKeyListener(new CListKeySearchListener(this));
    }

    /**
     * Adds a group directly to the root node.
     *
     * @param group The MetaContactGroup to be added.
     */
    public void addGroup(MetaContactGroup group) {

        this.listModel.addElement(group);
    }

    /**
     * Adds a contact directly to the root node.
     *
     * @param contact The MetaContactNode to be added.
     */
    public void addContact(MetaContactNode contact) {

        this.listModel.addElement(contact);
    }


    /**
     * Adds a child to a given parent.
     *
     * @param parentGroup The parent group.
     * @param contact The child contact node.
     */

    public void addChild(MetaContactGroup parentGroup,
                                MetaContactNode contact) {

        int index = 1;

        if(parentGroup != null){
            if(this.listModel.contains(parentGroup)){
                int i = this.listModel.indexOf(parentGroup);
                index = i + 1;
                this.listModel.add(index, contact);
            }
        }
        else{
            this.listModel.add(index, contact);
        }

        this.scrollRectToVisible(this.getCellBounds(index, index + 1));
    }

    /**
     * Indicates that a MetaContact has been added to the
     * MetaContactList.
     */
    public void metaContactAdded(MetaContactEvent evt) {

        this.addChild(evt.getParentGroup(),
                new MetaContactNode(evt.getSourceContact()));
    }

    /**
     * Indicates that a MetaContact has been removed from
     * the MetaContactList.
     */
    public void metaContactRemoved(MetaContactEvent evt)
    {
        /**@todo implement metaContactRemoved() */
        System.out.println("@todo implement metaContactRemoved()");
    }

    /**
     * Indicates that a MetaContact has been moved inside the MetaContact list.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactMoved(MetaContactEvent evt)
    {
        /**@todo implement metaContactMoved() */
        System.out.println("@todo implement metaContactMoved()");
    }

    /**
     * Indicates that a MetaContactGroup has been added.
     */
    public void metaContactGroupAdded(MetaContactGroupEvent evt) {

        MetaContactGroup contactGroup = evt.getSourceMetaContactGroup();

        this.addGroup(contactGroup);

        Iterator childContacts = contactGroup.getChildContacts();
        while (childContacts.hasNext()){

            MetaContact childContact
                = (MetaContact)childContacts.next();

            this.addChild(contactGroup, new MetaContactNode(childContact));
        }
    }

    /**
     * Indicates that a MetaContactGroup has been modified (e.g. a proto contact
     * group was removed).
     *
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactGroupModified(MetaContactGroupEvent evt)
    {
        /**@todo implement metaContactGroupModified() */
        System.out.println("@todo implement metaContactGroupModified()");
    }

    /**
     * Indicates that a MetaContact has been modified.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactModified(MetaContactEvent evt)
    {
        /**@todo implement metaContactModified() */
        System.out.println("@todo implement metaContactModified()");
    }

   /**
    * Indicates that a MetaContactGroup has been removed.
    */
    public void metaContactGroupRemoved(MetaContactGroupEvent evt) {

    }
}


