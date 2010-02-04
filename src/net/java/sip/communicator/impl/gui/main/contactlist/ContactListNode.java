/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

/**
 * The <tt>ContactListNode</tt> represents a node in the contact list. An
 * implementation of this interface should be able to determine the index of
 * this node in the <tt>MetaContactListService</tt>.
 *
 * @author Yana Stamcheva
 */
public interface ContactListNode
{
    /**
     * Returns the index of this node in the <tt>MetaContactListService</tt>.
     * @return the index of this node in the <tt>MetaContactListService</tt>
     */
    public int getMetaContactListIndex();
}
