/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The IRC implementation of a sip-communicator AccountID.
 * 
 * @author Stephane Remy
 * @author Loic Kempf
 */
public class IrcAccountID
    extends AccountID
{
    /**
     * Creates an account id from the specified id and account properties.
     *
     * @param userID the user identifier corresponding to this account
     * @param accountProperties any other properties necessary for the account.
     */
    IrcAccountID(String userID, Map<String, String> accountProperties)
    {
        super(  userID,
                accountProperties,
                ProtocolNames.IRC,
                getServiceName(accountProperties));
    }
    
    /**
     * Returns the service name - the server we are logging to
     * if it is null which is not supposed to be - we return for compatibility
     * the string we used in the first release for creating AccountID
     * (Using this string is wrong, but used for compatibility for now)
     * 
     * @param accountProperties Map the properties table configuring the account
     * @return String the service name
     */
    private static String getServiceName(Map<String, String> accountProperties)
    {
        String serviceName
            = accountProperties.get(ProtocolProviderFactory.SERVER_ADDRESS);

        return (serviceName == null) ? ProtocolNames.IRC : serviceName;
    }
}
