/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

/**
 * The AccountID is an account identifier that, combined with the protocol
 * itself, uniquely represents a specific user account. The class needs to be
 * extended by every protocol implementation because of its protected
 * constructor. The reason why this constructor is protected is mostly avoiding
 * confusion and letting people (using the protocol provider service) believe
 * that they are the ones who are supposed to instantiate the accountid class.
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
     * A String uniquely identyfying the user for this particular service.
     */
    protected String accountID = null;

    /**
     * Creates an account id for the specified provider userid and
     * accountProperties
     * @param accountID a String that uniquely identifies the user.
     * @param accountProperties a Map containing any other protocol and
     * implementation specific account initialization properties
     */
    protected AccountID( String accountID,
                         Map accountProperties)
    {
        super();

        this.accountID = String.copyValueOf(accountID.toCharArray());
        this.accountProperties = new Hashtable(accountProperties);
    }

    /**
     * Returns the user id of this class.
     *
     * @return A String uniquely identyfying the user inside this particular
     * service.
     */
    public String getAccountID()
    {
        return accountID;
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
     * <code>java.util.Hashtable</code>.
     * <p>
     * @return  a hash code value for this object.
     * @see     java.lang.Object#equals(java.lang.Object)
     * @see     java.util.Hashtable
     */
    public int hashCode()
    {
        return accountID == null? 0 : accountID.hashCode();
    }

    /**
     * Indicates whether some other object is "equal to" this account id.
     * <p>
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if this object is the same as the obj
     *          argument; <code>false</code> otherwise.
     * @see     #hashCode()
     * @see     java.util.Hashtable
     */
    public boolean equals(Object obj)
    {
        if(     obj == null
          || ! (getClass().isInstance(obj))
          || ! (accountID.equals(((AccountID)obj).accountID))
          || ! (accountProperties.equals(((AccountID)obj).accountProperties)))
            return false;

        return true;
    }

}
