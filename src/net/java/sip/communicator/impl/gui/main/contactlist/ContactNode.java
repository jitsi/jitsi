/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import javax.swing.tree.*;

import net.java.sip.communicator.service.contactlist.*;

/**
 * The <tt>ContactNode</tt> is a <tt>ContactListNode</tt> corresponding to a
 * given <tt>MetaContact</tt>.
 *
 * @author Yana Stamcheva
 */
public class ContactNode
    extends DefaultMutableTreeNode
    implements ContactListNode
{
    /**
     * The <tt>MetaContact</tt> corresponding to this contact node.
     */
    private MetaContact metaContact;

    /**
     * Indicates if this node is currently active. Has unread messages waiting.
     */
    private boolean isActive;

    /**
     * Creates a <tt>ContactNode</tt> by specifying the corresponding
     * <tt>metaContact</tt>.
     * @param metaContact the <tt>MetaContact</tt> corresponding to this node
     */
    public ContactNode(MetaContact metaContact)
    {
        super(metaContact);
        this.metaContact = metaContact;
    }

    /**
     * Returns the corresponding <tt>MetaContact</tt>.
     * @return the corresponding <tt>MetaContact</tt>
     */
    public MetaContact getMetaContact()
    {
        return (MetaContact) getUserObject();
    }

    /**
     * Returns the index of this contact node in its parent group in
     * the <tt>MetaContactListService</tt>.
     * @return the index in the <tt>MetaContactListService</tt>
     */
    public int getMetaContactListIndex()
    {
        return metaContact.getParentMetaContactGroup().indexOf(metaContact);
    }

    /**
     * Returns <tt>true</tt> if this contact node has unread received messages
     * waiting, otherwise returns <tt>false</tt>.
     * @return <tt>true</tt> if this contact node has unread received messages
     * waiting, otherwise returns <tt>false</tt>
     */
    public boolean isActive()
    {
        return isActive;
    }

    /**
     * Sets this contact node as active, which indicates it has unread received
     * messages waiting.
     * @param isActive indicates if this contact is active
     */
    public void setActive(boolean isActive)
    {
        this.isActive = isActive;
    }
}
