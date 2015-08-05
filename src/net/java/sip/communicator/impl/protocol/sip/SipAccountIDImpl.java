/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.sip.*;
import net.java.sip.communicator.util.*;

/**
 * A SIP extension of the account ID property.
 * @author Emil Ivov
 */
public class SipAccountIDImpl
    extends SipAccountID
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
     * Extracts the user id part from the given <tt>sipUri</tt>.
     *
     * @param sipUri the initial SIP URI from which we would like to extract
     * the user id part
     * @return the user id part String from the given <tt>sipUri</tt>
     */
    static String sipUriToUserID(String sipUri)
    {
        int index = sipUri.indexOf("sip:");
        String userID = (index > -1) ? sipUri.substring(4) : sipUri;

        return stripServerNameFromUserID(userID);
    }

    /**
     * Extracts the user address part from the given <tt>sipUri</tt>.
     *
     * @param sipUri the initial SIP URI from which we would like to extract
     * the user id part
     * @return the user address part String from the given <tt>sipUri</tt>
     */
    static String sipUriToUserAddress(String sipUri)
    {
        int index = sipUri.indexOf("sip:");
        String userAddress = (index > -1) ? sipUri.substring(4) : sipUri;

        return userAddress;
    }

    /**
     * Creates a SIP account id from the specified ide and account properties.
     *
     * @param userID the user id part of the SIP uri identifying this contact.
     * @param accountProperties any other properties necessary for the account.
     * @param serverName the name of the server that the user belongs to.
     */
    protected SipAccountIDImpl( String userID,
                                Map<String, String> accountProperties,
                                String serverName )
    {
        super(stripServerNameFromUserID(userID), accountProperties, serverName);
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
    @Override
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
    @Override
    public String getDisplayName()
    {
        String protocolName =
            getAccountPropertyString(ProtocolProviderFactory.PROTOCOL);
        String service = getService();

        if (service == null || service.trim().length() == 0)
        {
            // this is apparently a no registrar account
            protocolName = "RegistrarLess " + protocolName;
        }

        // If the ACCOUNT_DISPLAY_NAME property has been set for this account
        // we'll be using it as a display name.
        String key = ProtocolProviderFactory.ACCOUNT_DISPLAY_NAME;
        String accountDisplayName = accountProperties.get(key);
        if (accountDisplayName != null && accountDisplayName.length() > 0)
        {
            return accountDisplayName + " (" + protocolName + ")";
        }

        String returnValue = super.getAccountPropertyString(
                                        ProtocolProviderFactory.USER_ID);

        if (protocolName != null && protocolName.trim().length() > 0)
            returnValue += " (" + protocolName + ")";

        return returnValue;
    }

    /**
     * Returns the actual name of this protocol: {@link ProtocolNames#SIP}.
     *
     * @return SIP: the name of this protocol.
     */
    public String getSystemProtocolName()
    {
        return ProtocolNames.SIP;
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
                && ((SipAccountIDImpl)obj).getProtocolName().equals(
                    getProtocolName())
                && (getService() != null ?
                    getService().equals(((SipAccountIDImpl)obj).getService()) : true);
    }

    /**
     * Returns the account property string corresponding to the given key.
     * If property is xcap password obtain it from credential storage.
     *
     * @param key the key, corresponding to the property string we're looking
     * for
     * @return the account property string corresponding to the given key
     */
    @Override
    public String getAccountPropertyString(Object key)
    {
        if(key.equals(SipAccountID.OPT_CLIST_PASSWORD))
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
