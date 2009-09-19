/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 * The Facebook protocol provider factory creates instances of the Facebook
 * protocol provider service. One Service instance corresponds to one account.
 * 
 * @author Dai Zhiwei
 */
public class ProtocolProviderFactoryFacebookImpl
    extends ProtocolProviderFactory
{

    /**
     * Creates an instance of the ProtocolProviderFactoryFacebookImpl.
     */
    public ProtocolProviderFactoryFacebookImpl()
    {
        super(FacebookActivator.getBundleContext(), ProtocolNames.FACEBOOK);
    }

    /*
     * Implements ProtocolProviderFactory#createAccountID(String, Map).
     * Initializes a new FacebookAccountID with the specified properties.
     */
    protected AccountID createAccountID(
        String userID, Map<String, String> accountProperties)
    {
        return new FacebookAccountID(userID, accountProperties);
    }

    /*
     * Implements ProtocolProviderFactory#createService(String, AccountID).
     * Initializes a new ProtocolProviderServiceFacebookImpl with the specified
     * properties.
     */
    protected ProtocolProviderService createService(
        String userID, AccountID accountID)
    {
        ProtocolProviderServiceFacebookImpl service
            = new ProtocolProviderServiceFacebookImpl();

        service.initialize(userID, accountID);
        return service;
    }

    /**
     * Initialized and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter.
     * 
     * @param userIDStr the/a user identifier uniquely representing the newly
     *            created account within the protocol namespace.
     * @param accountProperties a set of protocol (or implementation) specific
     *            properties defining the new account.
     * @return the AccountID of the newly created account.
     */
    public AccountID installAccount(
        String userIDStr, Map<String, String> accountProperties)
    {
        BundleContext context = FacebookActivator.getBundleContext();
        if (context == null)
            throw new NullPointerException(
                "The specified BundleContext was null");

        if (userIDStr == null)
            throw new NullPointerException("The specified AccountID was null");

        if (accountProperties == null)
            throw new NullPointerException(
                "The specified property map was null");

        accountProperties.put(USER_ID, userIDStr);

        AccountID accountID =
            new FacebookAccountID(userIDStr, accountProperties);

        // make sure we haven't seen this account id before.
        if (registeredAccounts.containsKey(accountID))
            throw new IllegalStateException("An account for id " + userIDStr
                + " was already installed!");

        // first store the account and only then load it as the load generates
        // an osgi event, the osgi event triggers (through the UI) a call to the
        // ProtocolProviderService.register() method and it needs to acces
        // the configuration service and check for a stored password.
        this.storeAccount(accountID);

        accountID = loadAccount(accountProperties);

        return accountID;
    }

    /**
     * Modifies the account corresponding to the specified accountID. This
     * method is meant to be used to change properties of already existing
     * accounts. Note that if the given accountID doesn't correspond to any
     * registered account this method would do nothing.
     * 
     * @param protocolProvider
     * @param accountProperties a set of protocol (or implementation) specific
     *            properties defining the new account.
     * 
     * @throws java.lang.NullPointerException if any of the arguments is null.
     */
    public void modifyAccount(
        ProtocolProviderService protocolProvider,
        Map<String, String> accountProperties)
    {
        BundleContext context = FacebookActivator.getBundleContext();

        if (context == null)
            throw new NullPointerException(
                "The specified BundleContext was null");

        if (protocolProvider == null)
            throw new NullPointerException(
                "The specified Protocol Provider was null");

        FacebookAccountID accountID =
            (FacebookAccountID) protocolProvider.getAccountID();

        // If the given accountID doesn't correspond to an existing account
        // we return.
        if (!registeredAccounts.containsKey(accountID))
            return;

        ServiceRegistration registration = registeredAccounts.get(accountID);

        // kill the service
        if (registration != null)
            registration.unregister();

        if (accountProperties == null)
            throw new NullPointerException(
                "The specified property map was null");

        accountProperties.put(USER_ID, accountID.getUserID());

        if (!accountProperties.containsKey(PROTOCOL))
            accountProperties.put(PROTOCOL, ProtocolNames.FACEBOOK);

        accountID.setAccountProperties(accountProperties);

        /*
         * First store the account and only then load it as the load generates
         * an OSGi event, the OSGi event triggers (through the UI) a call to the
         * register() method and it needs to access the configuration service
         * and check for a password.
         */
        this.storeAccount(accountID);

        ((ProtocolProviderServiceFacebookImpl) protocolProvider).initialize(
            accountID.getUserID(), accountID);

        // We store again the account in order to store all properties added
        // during the protocol provider initialization.
        this.storeAccount(accountID);

        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(PROTOCOL, ProtocolNames.FACEBOOK);
        properties.put(USER_ID, accountID.getUserID());

        registration =
            context.registerService(ProtocolProviderService.class.getName(),
                protocolProvider, properties);

        // We store the modified account registration.
        registeredAccounts.put(accountID, registration);
    }
}
