/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import javax.swing.tree.*;

/**
 * The <tt>ContactNode</tt> is a <tt>ContactListNode</tt> corresponding to a
 * given <tt>UIContact</tt>.
 *
 * @author Yana Stamcheva
 */
public class ContactNode
    extends DefaultMutableTreeNode
    implements ContactListNode
{
    /**
     * The <tt>UIContact</tt> corresponding to this contact node.
     */
    private final UIContact contact;

    /**
     * Indicates if this node is currently active. Has unread messages waiting.
     */
    private boolean isActive;

    /**
     * Creates a <tt>ContactNode</tt> by specifying the corresponding
     * <tt>contact</tt>.
     * @param contact the <tt>UIContact</tt> corresponding to this node
     */
    public ContactNode(UIContact contact)
    {
        super(contact);
        this.contact = contact;
    }

    /**
     * Returns the corresponding <tt>UIContact</tt>.
     * @return the corresponding <tt>UIContact</tt>
     */
    public UIContact getContactDescriptor()
    {
        return (UIContact) getUserObject();
    }

    /**
     * Returns the index of this contact node in its parent group.
     * @return the index of this contact node in its parent group
     */
    public int getSourceIndex()
    {
        return contact.getSourceIndex();
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
