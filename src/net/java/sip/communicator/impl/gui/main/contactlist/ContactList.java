/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.Cursor;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

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
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.contactlist.event.MetaContactEvent;
import net.java.sip.communicator.service.contactlist.event.MetaContactGroupEvent;
import net.java.sip.communicator.service.contactlist.event.MetaContactListListener;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.contactlist.event.ProtoContactEvent;
import net.java.sip.communicator.service.contactlist.event.
    MetaContactRenamedEvent;
import net.java.sip.communicator.service.contactlist.event.
    MetaContactMovedEvent;

public class ContactList extends JList
    implements MetaContactListListener {

    private MetaContactListService contactList;

    private ContactListModel listModel;

    private Hashtable removedContacts = new Hashtable();

    public ContactList(MetaContactListService contactList){

        this.contactList = contactList;

        this.listModel = new ContactListModel(contactList);

        this.setModel(listModel);

        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        this.getSelectionModel().setSelectionMode
                (TreeSelectionModel .SINGLE_TREE_SELECTION);

        this.setCellRenderer(new ContactListCellRenderer());

        this.putClientProperty("JTree.lineStyle", "None");

        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        this.addKeyListener(new CListKeySearchListener(this));

        this.contactList.addContactListListener(this);
    }

    /**
     * Removes all offline contacts from the contact list.
     */
    public void removeOfflineContacts(){
        /*
        for(int i = 0; i < listModel.getSize(); i++){

            Object element = listModel.get(i);

            if(element instanceof MetaContactNode){

                MetaContactNode contactNode = (MetaContactNode)element;

                if(contactNode.getStatus().equals(Constants.OFFLINE_STATUS)){

                    MetaContactGroup group
                        = this.contactList.findParentMetaContactGroup(contactNode.getContact());

                    this.removedContacts.put(contactNode, group);
                }
            }
        }

        Enumeration removedNodes = this.removedContacts.keys();

        while(removedNodes.hasMoreElements()){
            this.listModel.removeElement(removedNodes.nextElement());
        }
        */
    }

    /**
     * Adds all offline contacts back to the contact list.
     */
    public void addOfflineContacts(){
        /*
        Iterator i = this.removedContacts.entrySet().iterator();
        while(i.hasNext()){
            Map.Entry entry = (Map.Entry)i.next();

            this.addChild((MetaContactGroup)entry.getValue(), (MetaContactNode)entry.getKey());
        }
        */
    }

    /**
     * Indicates that a MetaContact has been added to the
     * MetaContactList.
     */
    public void metaContactAdded(MetaContactEvent evt) {
        // TODO Auto-generated method stub

    }

    /**
     * Indicates that a MetaContact has been modified.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactModified(MetaContactEvent evt) {
        // TODO Auto-generated method stub

    }

    /**
     * Indicates that a MetaContact has been removed from
     * the MetaContactList.
     */
    public void metaContactRemoved(MetaContactEvent evt) {
        // TODO Auto-generated method stub

    }

    /**
     * Indicates that a MetaContact has been moved inside the MetaContact list.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactMoved(MetaContactEvent evt) {
        // TODO Auto-generated method stub

    }


    /**
     * Indicates that a MetaContactGroup has been added.
     */
    public void metaContactGroupAdded(MetaContactGroupEvent evt) {

        MetaContactGroup sourceGroup = evt.getSourceMetaContactGroup();

        this.listModel.groupAdded(sourceGroup, this);
    }

    /**
     * Indicates that a MetaContactGroup has been modified (e.g. a proto contact
     * group was removed).
     *
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactGroupModified(MetaContactGroupEvent evt) {
        // TODO Auto-generated method stub
    }

    /**
     * Indicates that a MetaContactGroup has been removed.
     */
    public void metaContactPropertyChange(MetaContactEvent evt)
    {
        /**@todo implement metaContactModified() */
        System.out.println("@todo implement metaContactModified()");
    }

   /**
    * Indicates that a MetaContactGroup has been removed.
    */
    public void metaContactGroupRemoved(MetaContactGroupEvent evt) {
        // TODO Auto-generated method stub

    }

    /**
     * Indicates that a protocol specific <tt>Contact</tt> instance has been
     * added to the list of protocol specific buddies in this
     * <tt>MetaContact</tt>
     * @param evt a reference to the corresponding
     * <tt>ProtoContactEvent</tt>
     */
    public void protoContactAdded(ProtoContactEvent evt)
    {
    }

    /**
     * Indicates that a protocol specific <tt>Contact</tt> instance has been
     * removed from the list of protocol specific buddies in this
     * <tt>MetaContact</tt>
     * @param evt a reference to the corresponding
     * <tt>ProtoContactEvent</tt>
     */
    public void protoContactRemoved(ProtoContactEvent evt)
    {
    }

    /**
     * Indicates that the order under which the child contacts were ordered
     * inside the source group has changed.
     * @param evt the <tt>MetaContactGroupEvent</tt> containind details of this
     * event.
     */
    public void childContactsReordered(MetaContactGroupEvent evt)
    {

    }

    /**
     * Indicates that a MetaContact has been moved inside the MetaContact list.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactMoved(MetaContactMovedEvent evt)
    {

    }

    /**
     * Indicates that a MetaContact has been modified.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactRenamed(MetaContactRenamedEvent evt)
    {
    }

    /**
     * Indicates that a protocol specific <tt>Contact</tt> instance has been
     * moved from within one <tt>MetaContact</tt> to another.
     * @param evt a reference to the <tt>ProtoContactMovedEvent</tt> instance.
     */
    public void protoContactMoved(ProtoContactEvent evt)
    {

    }

}
