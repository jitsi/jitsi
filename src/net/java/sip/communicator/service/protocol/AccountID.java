/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

/**
 * The AccountID is an account identifier that, uniquely represents a specific
 * user account over a specific protocol. The class needs to be extended by
 * every protocol implementation because of its protected
 * constructor. The reason why this constructor is protected is mostly avoiding
 * confusion and letting people (using the protocol provider service) believe
 * that they are the ones who are supposed to instantiate the accountid class.
 * <p>
 * Every instance of the <tt>ProtocolProviderService</tt>, created through the
 * ProtocolProviderFactory is assigned an AccountID instance, that uniquely
 * represents it and whose string representation (obtainted through the
 * getAccountUID() method) can be used for identification of persistently stored
 * account details.
 * <p>
 * Account id's are guaranteed to be different for different accounts and in the
 * same time are bound to be equal for multiple installations of the same
 * account.

 *
 * @author Emil Ivov
 */
public abstract class AccountID
{
    /**
     * Contains all implementation specific properties that define the account.
     * The exact names of the keys are protocol (and sometimes implementation)
     * specific.
     */
    protected Map accountProperties = null;

    /**
     * A String uniquely identifying the user for this particular account.
     */
    protected String userID = null;

    /**
     * A String uniquely identifying this account, that can also be used for
     * storing and unambiguously retrieving details concerning it.
     */
    protected String accountUID = null;

    /**
     * Creates an account id for the specified provider userid and
     * accountProperties.
     * @param userID a String that uniquely identifies the user.
     * @param accountProperties a Map containing any other protocol and
     * implementation specific account initialization properties
     * @param protocolName the name of the protocol implemented by the provider
     * that this id is meant for.
     * @param serviceName the name of the service (e.g. iptel.org, jabber.org,
     * icq.com) that this account is registered with.
     */
    protected AccountID( String userID,
                         Map accountProperties,
                         String protocolName,
                         String serviceName)
    {
        super();

        this.userID = userID;
        this.accountProperties = new Hashtable(accountProperties);

        //create a unique identifier string
        //NOTE: the service name is most probably already present in the userID
        //(e.g. in sip or jabber uris) but since this string is not meant to be
        //seen by the user, it won't hurt adding it again. It could avoid
        //duplicate IDs with protocols that are not meant to work accross
        //servers (e.g. imagine some weird case where you have the same icq id
        //with both the AIM server and some private ICQ server).
        this.accountUID = protocolName + ":" + userID + "@" + serviceName;
    }

    /**
     * Returns the user id associated with this account.
     *
     * @return A String identyfying the user inside this particular service.
     */
    public String getUserID()
    {
        return userID;
    }

    /**
     * Returns a String uniquely idnetifying this account, guaranteed to remain
     * the same accross multiple installations of the same account and to always
     * be unique for differing accounts.
     * @return String
     */
    public String getAccountUniqueID()
    {
        return accountUID;
    }

    /**
     * Returns a Map containing protocol and implementation account
     * initialization propeties.
     * @return a Map containing protocol and implementation account
     * initialization propeties.
     */
    public Map getAccountProperties()
    {
        return new Hashtable(accountProperties);
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hashtables such as those provided by
     * <tt>java.util.Hashtable</tt>.
     * <p>
     * @return  a hash code value for this object.
     * @see     java.lang.Object#equals(java.lang.Object)
     * @see     java.util.Hashtable
     */
    public int hashCode()
    {
        return accountUID == null? 0 : accountUID.hashCode();
    }

    /**
     * Indicates whether some other object is "equal to" this account id.
     * <p>
     * @param   obj   the reference object with which to compare.
     * @return  <tt>true</tt> if this object is the same as the obj
     *          argument; <tt>false</tt> otherwise.
     * @see     #hashCode()
     * @see     java.util.Hashtable
     */
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;

        if(     obj == null
          || ! (getClass().isInstance(obj))
          || ! (userID.equals(((AccountID)obj).userID)))
            return false;

        return true;
    }

    /**
     * Returns a string representation of this account id (same as calling
     * getAccountUniqueID()).
     *
     * @return  a string representation of this account id.
     */
    public String toString()
    {
        return getAccountUniqueID();
    }

}
