/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

/**
 * The Jabber implementation for Volatile Contact
 * @author Damian Minkov
 */
public class VolatileContactJabberImpl
    extends ContactJabberImpl
{
    /**
     * This contact id
     */
    private String contactId = null;
    /**
     * Creates an Volatile JabberContactImpl with the specified id
     * @param id String the user id/address
     * @param ssclCallback a reference to the ServerStoredContactListImpl
     * instance that created us.
     */
    VolatileContactJabberImpl(String id,
                              ServerStoredContactListJabberImpl ssclCallback)
    {
        super(null, ssclCallback, false, false);
        this.contactId = id;
    }

    /**
     * Returns the Jabber Userid of this contact
     * @return the Jabber Userid of this contact
     */
    @Override
    public String getAddress()
    {
        return contactId;
    }

    /**
     * Returns a String that could be used by any user interacting modules for
     * referring to this contact. An alias is not necessarily unique but is
     * often more human readable than an address (or id).
     * @return a String that can be used for referring to this contact when
     * interacting with the user.
     */
    @Override
    public String getDisplayName()
    {
        return contactId;
    }

    /**
     * Returns a string representation of this contact, containing most of its
     * representative details.
     *
     * @return  a string representation of this contact.
     */
    @Override
    public String toString()
    {
        StringBuffer buff =  new StringBuffer("VolatileJabberContact[ id=");
        buff.append(getAddress()).append("]");

        return buff.toString();
    }

    /**
     * Determines whether or not this contact group is being stored by the
     * server. Non persistent contact groups exist for the sole purpose of
     * containing non persistent contacts.
     * @return true if the contact group is persistent and false otherwise.
     */
    @Override
    public boolean isPersistent()
    {
        return false;
    }
}
