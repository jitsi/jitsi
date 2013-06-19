/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.jabber.*;

/**
 * The Jabber implementation of a sip-communicator AccountID
 *
 * @author Damian Minkov
 * @author Sebastien Vincent
 */
public class JabberAccountIDImpl
    extends JabberAccountID
{
    /**
     * Creates an account id from the specified id and account properties.
     * @param id the id identifying this account
     * @param accountProperties any other properties necessary for the account.
     */
    JabberAccountIDImpl(String id, Map<String, String> accountProperties)
    {
        super( id, accountProperties );
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
        return accountProperties.get(ProtocolProviderFactory.SERVER_ADDRESS);
    }

    /**
     * Returns the list of STUN servers that this account is currently
     * configured to use.
     *
     * @return the list of STUN servers that this account is currently
     * configured to use.
     */
    public List<StunServerDescriptor> getStunServers()
    {
        Map<String, String> accountProperties = getAccountProperties();
        List<StunServerDescriptor> serList
            = new ArrayList<StunServerDescriptor>();

        for (int i = 0; i < StunServerDescriptor.MAX_STUN_SERVER_COUNT; i ++)
        {
            StunServerDescriptor stunServer
                = StunServerDescriptor.loadDescriptor(
                        accountProperties,
                        ProtocolProviderFactory.STUN_PREFIX + i);

            // If we don't find a stun server with the given index, it means
            // that there're no more servers left in the table so we've nothing
            // more to do here.
            if (stunServer == null)
                break;

            String password = this.loadStunPassword(
                ProtocolProviderFactory.STUN_PREFIX + i);

            if(password != null)
                stunServer.setPassword(password);

            serList.add(stunServer);
        }

        return serList;
    }

    /**
     * Load password for this STUN descriptor.
     *
     * @param namePrefix name prefix
     * @return password or null if empty
     */
    private String loadStunPassword(String namePrefix)
    {
        String password = null;
        String className = ProtocolProviderServiceJabberImpl.class.getName();
        String packageSourceName = className.substring(0,
                className.lastIndexOf('.'));

        String accountPrefix = ProtocolProviderFactory.findAccountPrefix(
                JabberActivator.bundleContext,
                this, packageSourceName);

        CredentialsStorageService credentialsService =
            JabberActivator.getCredentialsStorageService();

        try
        {
            password = credentialsService.
                loadPassword(accountPrefix + "." + namePrefix);
        }
        catch(Exception e)
        {
            return null;
        }

        return password;
    }
}
