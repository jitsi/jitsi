/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactlist;


import net.java.sip.communicator.service.protocol.*;
import java.util.Iterator;

/**
 * A MetaContact is an abstraction used for merging mutltiple Contacts (most
 * often) belonging to different <tt>ProtocolProvider</tt>s.
 * <p>
 * Instances of a MetaContact are readonly objects that cannot be modified
 * directly but only through the corresponding MetaContactListService.
 * <p>
 * @author Emil Ivov
 */
public interface MetaContact
{
    /**
     * Returns the default protocol specific <tt>Contact</tt> to use when
     * communicating with this <tt>MetaContact</tt>.
     * @return the default <tt>Contact</tt> to use when communicating with
     * this <tt>MetaContact</tt>
     */
    public Contact getDefaultContact();

    /**
     * Returns a <tt>java.util.Iterator</tt> with all protocol specific
     * <tt>Contacts</tt> encapsulated by this <tt>MetaContact</tt>.
     * @return a <tt>java.util.Iterator</tt> containing all protocol specific
     * <tt>Contact</tt>s that were registered as subcontacts for this
     * <tt>MetaContact</tt>
     */
    public Iterator getContacts();

    /**
     * Returns the number of protocol speciic <tt>Contact</tt>s that this
     * <tt>MetaContact</tt> contains.
     * @return an int indicating the number of protocol specific contacts merged
     * in this <tt>MetaContact</tt>
     */
    public int getContactCount();

    /**
     * Returns a Contact, encapsulated by this MetaContact and coming from the
     * specified ProtocolProviderService. If none of the contacts encapsulated
     * by this MetaContact is originating from the specified provider then
     * <tt>null</tt> is returned.
     * <p>
     * @param provider a reference to the <tt>ProtocolProviderService</tt>
     * that we'd like to get a <tt>Contact</tt> for.
     * @return a <tt>Contact</tt> encapsulated in this
     * <tt>MetaContact</tt> and originating from the specified provider.
     */
    public Contact getContactForProvider(ProtocolProviderService provider);

    /**
     * Returns a String identifier (the actual contents is left to
     * implementations) that uniquely represents this <tt>MetaContact</tt>
     * in the containing <tt>MetaContactList</tt>
     * @return String
     */
    public String getMetaUID();

    /**
     * Returns a characteristic display name that can be used when including
     * this <tt>MetaContact</tt> in user interface.
     * @return a human readable String that represents this meta contact.
     */
    public String getDisplayName();

    /**
     * Returns a String representation of this <tt>MetaContact</tt>.
     * @return a String representation of this <tt>MetaContact</tt>.
     */
    public String toString();
}
