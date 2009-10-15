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
    JabberAccountID(String id, Map<String, String> accountProperties )
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
    private static String getServiceName(Map<String, String> accountProperties)
    {
        String serviceName
            = accountProperties.get(ProtocolProviderFactory.SERVER_ADDRESS);

        return (serviceName != null) ? serviceName : "jabber.org";
    }
}
