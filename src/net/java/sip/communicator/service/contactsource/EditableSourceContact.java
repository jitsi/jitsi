/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactsource;

/**
 * The <tt>EditableSourceContact</tt> is an extension to the
 * <tt>SourceContact</tt> interface that allows editing.
 *
 * @see SourceContact
 *
 * @author Yana Stamcheva
 */
public interface EditableSourceContact
    extends SourceContact
{
    /**
     * Adds a contact detail to the list of contact details.
     *
     * @param detail the <tt>ContactDetail</tt> to add
     */
    public void addContactDetail(ContactDetail detail);

    /**
     * Removes the given <tt>ContactDetail</tt> from the list of details for
     * this <tt>SourceContact</tt>.
     *
     * @param detail the <tt>ContactDetail</tt> to remove
     */
    public void removeContactDetail(ContactDetail detail);

    /**
     * Locks this object before adding or removing several contact details.
     */
    public void lock();

    /**
     * Unlocks this object before after or removing several contact details.
     */
    public void unlock();

}
