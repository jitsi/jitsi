/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.generic;

import net.java.sip.communicator.service.protocol.*;

/**
 * A very simple straight forward implementation of a security authority
 * that would always return the same password (the one specified upon
 * construction) when asked for credentials.
 */
public class SecurityAuthorityImpl
    implements SecurityAuthority
{
    /**
     * The password to return when asked for credentials
     */
    private char[] passwd = null;

    private boolean isUserNameEditable = false;

    /**
     * Creates an instance of this class that would always return "passwd"
     * when asked for credentials.
     *
     * @param passwd the password that this class should return when
     * asked for credentials.
     */
    public SecurityAuthorityImpl(char[] passwd)
    {
        this.passwd = passwd;
    }

    /**
     * Returns a Credentials object associated with the specified realm.
     * <p>
     * @param realm The realm that the credentials are needed for.
     * @param defaultValues the values to propose the user by default
     * @return The credentials associated with the specified realm or null
     * if none could be obtained.
     */
    public UserCredentials obtainCredentials(String          realm,
                                             UserCredentials defaultValues)
    {
        defaultValues.setPassword(passwd);
        return defaultValues;
    }

    /**
     * Returns a Credentials object associated with the specified realm.
     * <p>
     * @param realm The realm that the credentials are needed for.
     * @param defaultValues the values to propose the user by default
     * @param reasonCode the reason for which we're obtaining the
     * credentials.
     * @return The credentials associated with the specified realm or null
     * if none could be obtained.
     */
    public UserCredentials obtainCredentials(String          realm,
                                             UserCredentials defaultValues,
                                             int reasonCode)
    {
        return obtainCredentials(realm, defaultValues);
    }

    /**
     * Sets the userNameEditable property, which should indicate if the
     * user name could be changed by user or not.
     * 
     * @param isUserNameEditable indicates if the user name could be changed
     */
    public void setUserNameEditable(boolean isUserNameEditable)
    {
        this.isUserNameEditable = isUserNameEditable;
    }
    
    /**
     * Indicates if the user name is currently editable, i.e. could be changed
     * by user or not.
     * 
     * @return <code>true</code> if the user name could be changed,
     * <code>false</code> - otherwise.
     */
    public boolean isUserNameEditable()
    {
        return isUserNameEditable;
    }
}

