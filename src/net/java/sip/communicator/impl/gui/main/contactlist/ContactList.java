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
import javax.swing.plaf.basic.BasicTreeUI;
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

public class ContactList extends JList
    implements MetaContactListListener {

    private MetaContactListService contactList;

    private MetaContactGroup root;

    private ContactListModel listModel = new ContactListModel();

    public ContactList(MetaContactListService contactList){

        this.contactList = contactList;

        this.contactList.addContactListListener(this);

        this.root = contactList.getRoot();

        this.setModel(listModel);

        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        this.getSelectionModel().setSelectionMode
                (TreeSelectionModel	.SINGLE_TREE_SELECTION);

        this.setCellRenderer(new ContactListCellRenderer());

        this.putClientProperty("JTree.lineStyle", "None");

        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /**
     * Adds a child directly to the root node.
     *
     * @param child The child object to be added.
     * @return The added node.
     */
    public void addChild(MetaContactGroup group) {

        this.listModel.addElement(group);
    }


    /**
     * Adds a child to a given parent.
     *
     * @param parent The parent node.
     * @param child The child object.
     * @param shouldBeVisible
     * @return The added node.
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

    public void metaContactAdded(MetaContactEvent evt) {

        this.addChild(evt.getParentGroup(),
                new MetaContactNode(evt.getSourceContact()));
    }

    public void metaContactRemoved(MetaContactEvent evt) {

    }

    public void metaContactGroupAdded(MetaContactGroupEvent evt) {

        MetaContactGroup contactGroup = evt.getSourceMetaContactGroup();

        this.addChild(contactGroup);

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



    public void metaContactGroupRemoved(MetaContactGroupEvent evt) {

    }

    public MetaContactGroup getRoot() {
        return root;
    }


}


