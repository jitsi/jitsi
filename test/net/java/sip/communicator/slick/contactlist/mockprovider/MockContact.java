/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.contactlist.mockprovider;

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
    public MockContactGroup getParentGroup()
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

}
