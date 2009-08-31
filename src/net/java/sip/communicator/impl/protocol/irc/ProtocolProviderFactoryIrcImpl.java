/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The IRC protocol provider factory creates instances of the IRC protocol
 * provider service. One Service instance corresponds to one account.
 *
 * @author Stephane Remy
 * @author Loic Kempf
 */
public class ProtocolProviderFactoryIrcImpl
    extends ProtocolProviderFactory
{
    public ProtocolProviderFactoryIrcImpl()
    {
        super(IrcActivator.bundleContext, ProtocolNames.IRC);
    }

    /**
     * Initialized and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter.
     *
     * @param userIDStr the user identifier uniquely representing the newly
     *   created account within the protocol namespace.
     * @param accountProperties a set of protocol (or implementation)
     *   specific properties defining the new account.
     * @return the AccountID of the newly created account.
     */
    public AccountID installAccount( String userIDStr,
                                     Map<String, String> accountProperties)
    {
        BundleContext context = IrcActivator.bundleContext;
        
        if (context == null)
            throw new NullPointerException(
                "The specified BundleContext was null");

        if (userIDStr == null)
            throw new NullPointerException(
                "The specified AccountID was null");

        if (accountProperties == null)
            throw new NullPointerException(
                "The specified property map was null");

        accountProperties.put(USER_ID, userIDStr);

        AccountID accountID = new IrcAccountID(userIDStr, accountProperties);

        //make sure we haven't seen this account id before.
        if (registeredAccounts.containsKey(accountID))
            throw new IllegalStateException(
                "An account for id " + userIDStr + " was already installed!");

        //first store the account and only then load it as the load generates
        //an OSGI event, the OSGI event triggers (through the UI) a call to the
        //ProtocolProviderService.register() method and it needs to access
        //the configuration service and check for a stored password.
        this.storeAccount(accountID);

        accountID = loadAccount(accountProperties);

        return accountID;
    }

    protected AccountID createAccountID(String userID, Map<String, String> accountProperties)
    {
        return new IrcAccountID(userID, accountProperties);
    }

    protected ProtocolProviderService createService(String userID,
        AccountID accountID)
    {
        ProtocolProviderServiceIrcImpl service =
            new ProtocolProviderServiceIrcImpl();

        service.initialize(userID, accountID);
        return service;
    }

    @Override
    public void modifyAccount(  ProtocolProviderService protocolProvider,
                                Map<String, String> accountProperties)
        throws NullPointerException
    {
        // TODO Auto-generated method stub
        
    }
}
