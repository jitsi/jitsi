package net.java.sip.communicator.impl.protocol.icq;

import net.java.sip.communicator.service.protocol.*;
import net.kano.joscar.snaccmd.*;
import net.kano.joustsim.oscar.oscar.service.ssi.*;
import net.java.sip.communicator.service.protocol.icqconstants.*;

/**
 * The ICQ implementation of the service.protocol.Contact interface.
 * @author Emil Ivov
 */
public class ContactIcqImpl
    implements Contact
{
    Buddy joustSimBuddy = null;
    private boolean isLocal = false;
    private byte[] image = null;
    private PresenceStatus icqStatus = IcqStatusEnum.OFFLINE;
    private ServerStoredContactListIcqImpl ssclCallback = null;

    /**
     * Creates an IcqContactImpl
     * @param buddy the JoustSIM object that we will be encapsulating.
     */
    ContactIcqImpl(Buddy buddy, ServerStoredContactListIcqImpl ssclCallback)
    {
        this.joustSimBuddy = buddy;
        this.isLocal = isLocal;
        this.ssclCallback = ssclCallback;
    }

    /**
     * Returns the ICQ uin (or AIM screen name)of this contact
     * @return the ICQ uin (or AIM screen name)of this contact
     */
    public String getUIN()
    {
        return joustSimBuddy.getScreenname().getFormatted();
    }

    /**
     * Returns the ICQ uin (or AIM screen name)of this contact
     * @return the ICQ uin (or AIM screen name)of this contact
     */
    public String getAddress(){
        return getUIN();
    }

    /**
     * Determines whether or not this Contact instance represents the user used
     * by this protocol provider to connect to the service.
     *
     * @return true if this Contact represents us (the local user) and false
     * otherwise.
     */
    public boolean isLocal()
    {
        return isLocal;
    }

    public byte[] getImage()
    {
        return image;
    }

    /**
     * Returns a hashCode for this contact. The returned hashcode is actually
     * that of the Contact's UIN
     * @return the hashcode of this Contact
     */
    public int hashCode()
    {
        return getUIN().hashCode();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     *
     * @param   obj   the reference object with which to compare.
     * @return  <tt>true</tt> if this object is the same as the obj
     *          argument; <tt>false</tt> otherwise.
     */
    public boolean equals(Object obj)
    {
        if (obj == null
            || !(obj instanceof ContactIcqImpl)
            || !((ContactIcqImpl)obj).getUIN().equals(getUIN()))
            return false;

        return true;
    }

    /**
     * Returns the joust sim buddy that this Contact is encapsulating.
     * @return Buddy
     */
    Buddy getJoustSimBuddy()
    {
        return joustSimBuddy;
    }

    /**
     * Returns a string representation of this contact, containing most of its
     * representative details.
     *
     * @return  a string representation of this contact.
     */
    public String toString()
    {
        StringBuffer buff =  new StringBuffer("IcqContact[ uin=");
        buff.append(getAddress()).append(", alias=")
            .append(getJoustSimBuddy().getAlias()).append("]");

        return buff.toString();
    }

    /**
     * Sets the status that this contact is currently in. The method is to
     * only be called as a result of a status update received from the AIM
     * server.
     *
     * @param status the IcqStatusEnum that this contact is currently in.
     */
    void updatePresenceStatus(PresenceStatus status)
    {
        this.icqStatus = status;
    }

    /**
     * Returns the status of the contact as per the last status update we've
     * received for it. Note that this method is not to perform any network
     * operations and will simply return the status received in the last
     * status update message. If you want a reliable way of retrieving someone's
     * status, you should use the <tt>queryContactStatus()</tt> method in
     * <tt>OperationSetPresence</tt>.
     * @return the PresenceStatus that we've received in the last status update
     * pertaining to this contact.
     */
    public PresenceStatus getPresenceStatus()
    {
        return icqStatus;
    }

    /**
     * Returns a String that could be used by any user interacting modules for
     * referring to this contact. An alias is not necessarily unique but is
     * often more human readable than an address (or id).
     * @return a String that can be used for referring to this contact when
     * interacting with the user.
     */
    public String getDisplayName()
    {
        //temporarily return the uin as we don't have alias support in joust
        //sim right now.
        return getUIN();
    }

    /**
     * Returns a reference to the protocol provider that created the contact.
     * @return a refererence to an instance of the ProtocolProviderService
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return ssclCallback.getParentProvider();
    }
}
