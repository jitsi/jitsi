/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.mock;

import net.java.sip.communicator.service.protocol.*;

/**
 * A simple, straightforward mock implementation of the Contact interface
 * that  can be manually created and used in testing a
 * MetaContactList service
 *
 * @author Emil Ivov
 */
public class MockContact
    implements Contact
{
    private String contactID = null;
    private MockProvider parentProvider = null;
    private MockContactGroup parentGroup = null;
    private PresenceStatus presenceStatus = MockStatusEnum.MOCK_STATUS_50;
    private boolean isPersistent = true;
    private boolean isResolved = true;

    /**
     * Creates an instance of a meta contact with the specified string used
     * as a name and identifier.
     *
     * @param id the identifier of this contact (also used as a name).
     * @param parentProvider the provider that created us.
     */
    public MockContact(String id,
                       MockProvider parentProvider)
    {
        this.contactID = id;
        this.parentProvider = parentProvider;
    }

    /**
     * This method is only called when the contact is added to a new
     * <tt>MockContactGroup</tt> by the MockContactGroup itself.
     * @param newParentGroup the <tt>MockContactGroup</tt> that is now parent
     * of this <tt>MockContact</tt>
     */
    void setParentGroup(MockContactGroup newParentGroup)
    {
        this.parentGroup = newParentGroup;
    }

    /**
     * Returns a String that can be used for identifying the contact.
     *
     * @return a String id representing and uniquely identifying the contact.
     */
    public String getAddress()
    {
        return contactID;
    }

    /**
     * Returns a String that could be used by any user interacting modules
     * for referring to this contact.
     *
     * @return a String that can be used for referring to this contact when
     *   interacting with the user.
     */
    public String getDisplayName()
    {
        return contactID;
    }


    /**
     * Modify the display name of this contact.
     *
     * @param displayName the new display name for this contact.
     */
    public void setDisplayName(String displayName)
    {
        if (isResolved)
        {
            // TODO
            // contactID = displayName;
        }
    }

    /**
     * Returns a byte array containing an image (most often a photo or an
     * avatar) that the contact uses as a representation.
     *
     * @return byte[] an image representing the contact.
     */
    public byte[] getImage()
    {
        return null;
    }

    /**
     * Returns the status of the contact.
     *
     * @return always IcqStatusEnum.ONLINE.
     */
    public PresenceStatus getPresenceStatus()
    {
        return this.presenceStatus;
    }

    /**
     * Sets <tt>mockPresenceStatus</tt> as the PresenceStatus that this contact
     * is currently in.
     * @param mockPresenceStatus the <tt>MockPresenceStatus</tt> currently valid
     * for this contact.
     */
    public void setPresenceStatus(MockStatusEnum mockPresenceStatus)
    {
        this.presenceStatus = mockPresenceStatus;
    }

    /**
     * Returns a reference to the protocol provider that created the contact.
     *
     * @return a refererence to an instance of the ProtocolProviderService
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return parentProvider;
    }

    /**
     * Determines whether or not this contact represents our own identity.
     *
     * @return true in case this is a contact that represents ourselves and
     *   false otherwise.
     */
    public boolean isLocal()
    {
        return false;
    }

    /**
     * Returns the group that contains this contact.
     * @return a reference to the MockContactGroup that contains this contact.
     */
    public ContactGroup getParentContactGroup()
    {
        return this.parentGroup;
    }

    /**
     * Returns a string representation of this contact, containing most of its
     * representative details.
     *
     * @return  a string representation of this contact.
     */
    public String toString()
    {
        StringBuffer buff = new StringBuffer("MockContact[ DisplayName=")
            .append(getDisplayName()).append("]");

        return buff.toString();
    }

    /**
     * Determines whether or not this contact is being stored by the server.
     * Non persistent contacts are common in the case of simple, non-persistent
     * presence operation sets. They could however also be seen in persistent
     * presence operation sets when for example we have received an event
     * from someone not on our contact list. Non persistent contacts are
     * volatile even when coming from a persistent presence op. set. They would
     * only exist until the application is closed and will not be there next
     * time it is loaded.
     * @return true if the contact is persistent and false otherwise.
     */
    public boolean isPersistent()
    {
        return isPersistent;
    }

    /**
     * Returns null as no persistent data is required and the contact address is
     * sufficient for restoring the contact.
     * <p>
     * @return null as no such data is needed.
     */
    public String getPersistentData()
    {
        return null;
    }

    /**
     * Determines whether or not this contact has been resolved against the
     * server. Unresolved contacts are used when initially loading a contact
     * list that has been stored in a local file until the presence operation
     * set has managed to retrieve all the contact list from the server and has
     * properly mapped contacts to their on-line buddies.
     * @return true if the contact has been resolved (mapped against a buddy)
     * and false otherwise.
     */
    public boolean isResolved()
    {
        return isResolved;
    }

    /**
     * Makes the contact resolved or unresolved.
     *
     * @param resolved  true to make the contact resolved; false to
     *                  make it unresolved
     */
    public void setResolved(boolean resolved)
    {
        this.isResolved = resolved;
    }

    /**
     * Indicates whether some other object is "equal to" this one which in terms
     * of contacts translates to having equal ids. The resolved status of the
     * contacts deliberately ignored so that contacts would be declared equal
     * even if it differs.
     * <p>
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if this contact has the same id as that of the
     * <code>obj</code> argument.
     */
    public boolean equals(Object obj)
    {
        if (obj == null
            || ! (obj instanceof MockContact))
            return false;

        MockContact mockContact = (MockContact) obj;

        return this.getAddress().equals(mockContact.getAddress());
    }
    
    /**
     * Return the current status message of this contact.
     * 
     * @return null as the protocol has currently no support of status messages
     */
    public String getStatusMessage()
    {
        return null;
    }
}
