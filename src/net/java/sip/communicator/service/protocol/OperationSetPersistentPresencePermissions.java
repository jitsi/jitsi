/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * This interface is addition to the persistence presence operation set, meant
 * to provide per group permissions for modification of the contacts and groups.
 * Can make the contact list read only or only some groups in it.
 *
 * @author Damian Minkov
 */
public interface OperationSetPersistentPresencePermissions
    extends OperationSet
{
    /**
     * Is the whole contact list for the current provider readonly.
     * @return <tt>true</tt> if the whole contact list is readonly, otherwise
     * <tt>false</tt>.
     */
    public boolean isReadOnly();

    /**
     * Checks whether the <tt>contact</tt> can be edited, removed, moved. If
     * the parent group is readonly.
     * @param contact the contact to check.
     * @return <tt>true</tt> if the contact is readonly, otherwise
     * <tt>false</tt>.
     */
    public boolean isReadOnly(Contact contact);

    /**
     * Checks whether the <tt>group</tt> is readonly.
     * @param group the group to check.
     * @return <tt>true</tt> if the group is readonly, otherwise
     * <tt>false</tt>.
     */
    public boolean isReadOnly(ContactGroup group);
}
