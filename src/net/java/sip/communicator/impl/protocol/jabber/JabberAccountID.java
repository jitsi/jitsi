/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The Jabber implementation of a sip-communicator AccountID
 *
 * @author Damian Minkov
 */
public class JabberAccountID
    extends AccountID
{
    /**
     * Creates an account id from the specified id and account properties.
     * @param id the id identifying this account
     * @param accountProperties any other properties necessary for the account.
     */
    JabberAccountID(String id, Map accountProperties )
    {
        super(id, accountProperties, ProtocolNames.JABBER,
              getServiceName(accountProperties));
    }

    /**
     * Returns the service name - the server we are logging to
     * if it is null which is not supposed to be - we return for compatibility
     * the string we used in the first release for creating AccountID
     * (Using this string is wrong, but used for compatibility for now)
     * @param accountProperties Map
     * @return String
     */
    private static String getServiceName(Map accountProperties)
    {
        String serviceName =
            (String)accountProperties.get(ProtocolProviderFactory.SERVER_ADDRESS);

        if(serviceName != null)
            return serviceName;
        else
            return "jabber.org";
    }

    /**
     * The super method is overridden here because jabber user id
     * is already the user address and it is therefore pointless to add the
     * service name.
     *
     * @return a String in the form of userid@service that other protocol users
     * should be able to parse into a meaningful address and use it to
     * communicate with us.
     */
    public String getAccountAddress()
    {
        return getUserID();
    }
}
