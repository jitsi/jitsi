/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import org.osgi.framework.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The Jabber implementation of the ProtocolProviderFactory.
 * @author Damian Minkov
 */
public class ProtocolProviderFactoryJabberImpl
    extends ProtocolProviderFactory
{
    /**
     * The table that we store our accounts in.
     */
    private Hashtable registeredAccounts = new Hashtable();

    /**
     * Creates an instance of the ProtocolProviderFactoryJabberImpl.
     */
    protected ProtocolProviderFactoryJabberImpl()
    {
    }

    /**
     * Returns a copy of the list containing all accounts currently
     * registered in this protocol provider.
     *
     * @return a copy of the llist containing all accounts currently installed
     * in the protocol provider.
     */
    public ArrayList getRegisteredAccounts()
    {
        return new ArrayList(registeredAccounts.keySet());
    }

    /**
     * Returns the ServiceReference for the protocol provider corresponding to
     * the specified accountID or null if the accountID is unknown.
     * @param accountID the accountID of the protocol provider we'd like to get
     * @return a ServiceReference object to the protocol provider with the
     * specified account id and null if the account id is unknwon to the
     * provider factory.
     */
    public ServiceReference getProviderForAccount(AccountID accountID)
    {
        ServiceRegistration registration
            = (ServiceRegistration)registeredAccounts.get(accountID);

        return (registration == null )
                    ? null
                    : registration.getReference();
    }

    /**
     * Initializes and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter. This method has a persistent
     * effect. Once created the resulting account will remain installed until
     * removed through the uninstall account method.
     *
     * @param userIDStr the user identifier for the new account
     * @param accountProperties a set of protocol (or implementation)
     *   specific properties defining the new account.
     * @return the AccountID of the newly created account
     */
    public AccountID installAccount( String userIDStr,
                                     Map accountProperties)
    {
        BundleContext context
            = JabberActivator.getBundleContext();
        if (context == null)
            throw new NullPointerException("The specified BundleContext was null");

        if (userIDStr == null)
            throw new NullPointerException("The specified AccountID was null");

        accountProperties.put(USER_ID, userIDStr);

        if (accountProperties == null)
            throw new NullPointerException("The specified property map was null");

        AccountID accountID = new JabberAccountID(userIDStr, accountProperties);

        //make sure we haven't seen this account id before.
        if( registeredAccounts.containsKey(accountID) )
            throw new IllegalStateException(
                "An account for id " + userIDStr + " was already installed!");

        //first store the account and only then load it as the load generates
        //an osgi event, the osgi event triggers (trhgough the UI) a call to
        //the register() method and it needs to acces the configuration service
        //and check for a password.
        this.storeAccount(
            JabberActivator.getBundleContext()
            , accountID);

        accountID = loadAccount(accountProperties);

        return accountID;
    }

    /**
     * Initializes and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter.
     *
     * @param accountProperties a set of protocol (or implementation)
     *   specific properties defining the new account.
     * @return the AccountID of the newly created account
     */
    public AccountID loadAccount( Map accountProperties)
    {
        BundleContext context
            = JabberActivator.getBundleContext();
        if(context == null)
            throw new NullPointerException("The specified BundleContext was null");

        String userIDStr = (String)accountProperties.get(USER_ID);

        AccountID accountID = new JabberAccountID(userIDStr, accountProperties);

        //get a reference to the configuration service and register whatever
        //properties we have in it.

        Hashtable properties = new Hashtable();
        properties.put(PROTOCOL, ProtocolNames.JABBER);
        properties.put(USER_ID, userIDStr);

        ProtocolProviderServiceJabberImpl jabberProtocolProvider
            = new ProtocolProviderServiceJabberImpl();

        jabberProtocolProvider.initialize(userIDStr, accountID);

        ServiceRegistration registration
            = context.registerService( ProtocolProviderService.class.getName(),
                                       jabberProtocolProvider,
                                       properties);

        registeredAccounts.put(accountID, registration);
        return accountID;
    }


    /**
     * Removes the specified account from the list of accounts that this
     * provider factory is handling. If the specified accountID is unknown to
     * the ProtocolProviderFactory, the call has no effect and false is returned.
     * This method is persistent in nature and once called the account
     * corresponding to the specified ID will not be loaded during future runs
     * of the project.
     *
     * @param accountID the ID of the account to remove.
     * @return true if an account with the specified ID existed and was removed
     * and false otherwise.
     */
    public boolean uninstallAccount(AccountID accountID)
    {
        ServiceRegistration registration
            = (ServiceRegistration)registeredAccounts.remove(accountID);

        if(registration == null)
            return false;

        //kill the service
        registration.unregister();

        return removeStoredAccount(
            JabberActivator.getBundleContext()
            , accountID);
    }

    /**
     * Loads (and hence installs) all accounts previously stored in the
     * configuration service.
     */
    public void loadStoredAccounts()
    {
        super.loadStoredAccounts( JabberActivator.getBundleContext());
    }

    /**
     * Prepares the factory for bundle shutdown.
     */
    public void stop()
    {
        Enumeration registrations = this.registeredAccounts.elements();

        while(registrations.hasMoreElements())
        {
            ServiceRegistration reg
                = ((ServiceRegistration)registrations.nextElement());

            reg.unregister();
        }

        Enumeration idEnum = registeredAccounts.keys();

        while(idEnum.hasMoreElements())
        {
            registeredAccounts.remove(idEnum.nextElement());
        }
    }

    /**
     * Returns the configuraiton service property name prefix that we use to
     * store properties concerning the account with the specified id.
     * @param accountID the AccountID whose property name prefix we're looking
     * for.
     * @return the prefix  of the configuration service property name that
     * we're using when storing properties for the specified account.
     */
    public String findAccountPrefix(AccountID accountID)
    {
        return super.findAccountPrefix(JabberActivator.getBundleContext()
                                       , accountID);
    }

    /**
     * Saves the password for the specified account after scrambling it a bit
     * so that it is not visible from first sight (Method remains highly
     * insecure).
     *
     * @param accountID the AccountID for the account whose password we're
     * storing.
     * @param passwd the password itself.
     *
     * @throws java.lang.IllegalArgumentException if no account corresponding
     * to <tt>accountID</tt> has been previously stored.
     */
    public void storePassword(AccountID accountID, String passwd)
        throws IllegalArgumentException
    {
        super.storePassword(JabberActivator.getBundleContext()
                            , accountID
                            , passwd);
    }

    /**
     * Returns the password last saved for the specified account.
     *
     * @param accountID the AccountID for the account whose password we're
     * looking for..
     *
     * @return a String containing the password for the specified accountID.
     *
     * @throws java.lang.IllegalArgumentException if no account corresponding
     * to <tt>accountID</tt> has been previously stored.
     */
    public String loadPassword(AccountID accountID)
        throws IllegalArgumentException
    {
        return super.loadPassword(JabberActivator.getBundleContext()
                                  , accountID );
    }
}
