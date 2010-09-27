/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * A SIP extension of the account ID property.
 * @author Emil Ivov
 */
public class SipAccountID
    extends AccountID
{
    /**
     * Removes the server part from a sip user id if there is one. Used when
     * calling the super constructor to ensure that we will be consistent about
     * the value of the user id.
     *
     * @param userID the sip user id that we'd like to remove a server from.
     *
     * @return the user part of the <tt>userID</tt>
     */
    private static String stripServerNameFromUserID(String userID)
    {
        int index = userID.indexOf("@");
        return (index > -1) ? userID.substring(0, index) : userID;
    }

    /**
     * Creates a SIP account id from the specified ide and account properties.
     *
     * @param userID the user id part of the SIP uri identifying this contact.
     * @param accountProperties any other properties necessary for the account.
     * @param serverName the name of the server that the user belongs to.
     */
    protected SipAccountID(String userID, Map<String, String> accountProperties,
        String serverName)
    {
        super(stripServerNameFromUserID(userID), accountProperties,
            ProtocolNames.SIP,
            serverName);
    }

    /**
     * Returns a string that could be directly used (or easily converted to) an
     * address that other users of the procotol can use to communicate with us.
     * By default this string is set to userid@servicename. Protocol
     * implementors should override it if they'd need it to respect a different
     * syntax.
     *
     * @return a String in the form of userid@service that other protocol users
     * should be able to parse into a meaningful address and use it to
     * communicate with us.
     */
    public String getAccountAddress()
    {
        StringBuffer accountAddress = new StringBuffer();
        accountAddress.append("sip:");
        accountAddress.append(getUserID());

        String service = getService();
        if (service != null)
        {
            accountAddress.append('@');
            accountAddress.append(service);
        }

        return accountAddress.toString();
    }

    /**
     * The reason we need to override this method here comes from the fact
     * that the user id that is standardly returned by the parent method
     * is not sufficient for the user to distinguish this account from other
     * sip accounts with the same user name. Besides we also need to handle
     * the case of registrar-less accounts.
     *
     * @return A String that can be showed to users when referring to this
     * account.
     */
    public String getDisplayName()
    {
        String returnValue = super.getAccountPropertyString(
                            ProtocolProviderFactory.USER_ID);

        String protocolName =
            getAccountPropertyString(ProtocolProviderFactory.PROTOCOL);
        String service = getService();

        if (service == null || service.trim().length() == 0)
        {
            // this is apparently a no registrar account
            protocolName = "RegistrarLess " + protocolName;
        }

        if (protocolName != null && protocolName.trim().length() > 0)
            returnValue += " (" + protocolName + ")";

        return returnValue;
    }

    /**
     * Indicates whether some other object is "equal to" this account id.
     * <p>
     * @param   obj   the reference object with which to compare.
     * @return  <tt>true</tt> if this object is the same as the obj
     *          argument; <tt>false</tt> otherwise.
     */
    @Override
    public boolean equals(Object obj)
    {
        // service name can be null when using registerless accounts
        // if its null ignore it.
        return super.equals(obj)
                && ((SipAccountID)obj).getProtocolName().equals(
                    getProtocolName())
                && (getService() != null ?
                    getService().equals(((SipAccountID)obj).getService()) : true);
    }

    /**
     * Returns the account property string corresponding to the given key.
     * If property is xcap password obtain it from credential storage.
     *
     * @param key the key, corresponding to the property string we're looking
     * for
     * @return the account property string corresponding to the given key
     */
    public String getAccountPropertyString(Object key)
    {
        if(key.equals(ProtocolProviderServiceSipImpl.XCAP_PASSWORD))
        {
            CredentialsStorageService credentialsStorage
                = ServiceUtils.getService(SipActivator.getBundleContext(),
                                          CredentialsStorageService.class);

            return credentialsStorage.loadPassword(
                    getAccountUniqueID() + ".xcap");
        }
        else
            return super.getAccountPropertyString(key);
    }
}
