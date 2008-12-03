/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * A SIP extension of the account ID property.
 * @author Emil Ivov
 */
public class SipAccountID
    extends AccountID
{
    private static String getUserIDWithoutServerName(String userID)
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
    protected SipAccountID(String userID, Map accountProperties,
        String serverName)
    {
        super(getUserIDWithoutServerName(userID), accountProperties,
            (String) accountProperties.get(ProtocolProviderFactory.PROTOCOL),
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
     * Adds a property to the map of properties for this account identifier.
     * 
     * @param key the key of the property
     * @param value the property value
     */
    public void putProperty(Object key, Object value)
    {
        accountProperties.put(key, value);
    }
}
