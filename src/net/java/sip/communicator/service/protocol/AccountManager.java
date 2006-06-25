/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;
import org.osgi.framework.*;

/**
 * The AccountManager is what actually creates instances of a
 * ProtocolProviderService implementation. An account managere would register,
 * persistently store, and remove when necessary, ProtocolProviders. The way
 * things are in the SIP Communicator, a user account is representedy (in a 1:1
 * relationship) by  an AccountID and a ProtocolProvider. In other words - one
 * would have as many protocol providers installed in a given moment as they
 * would user account registered through the various services.
 *
 * @author Emil Ivov
 */
public interface AccountManager
{
    /**
     * The name of the OSGI service property which represents the name of the
     * protocol for an AccountManager
     */
    public static final String PROTOCOL_PROPERTY_NAME = "protocol.name";

    /**
     * The name of the OSGI service property which represents the AccountID of
     * a ProtocolProvider.
     */
    public static final String ACCOUNT_ID_PROPERTY_NAME = "account.id";

    /**
     * Initializaed and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter. Note that account
     * registration is persistent and accounts that are registered during
     * a particular sip-communicator session would be automatically reloaded
     * during all following sessions until they are removed through the
     * removeAccount method.
     *
     * @param context the BundleContext parameter where the newly created
     * ProtocolProviderService would have to be registered.
     * @param userID tha/a user identifier uniquely representing the newly
     * created account within the protocol namespace.
     * @param accountProperties a set of protocol (or implementation) specific
     * properties defining the new account.
     * @return the AccountID of the newly created account.
     * @throws java.lang.IllegalArgumentException if userID does not correspond
     * to an identifier in the context of the underlying protocol or if
     * accountProperties does not contain a complete set of account installation
     * properties.
     * @throws java.lang.IllegalStateException if the account has already been
     * installed.
     * @throws java.lang.NullPointerException if any of the arguments is null.
     */
    public AccountID installAccount(BundleContext context,
                                     String userID, Map accountProperties)
        throws IllegalArgumentException,
               IllegalStateException,
               NullPointerException;


    /**
     * Returns a copy of the list containing all accounts currently registered
     * in this protocol provider.
     * @return ArrayList
     */
    public ArrayList getRegisteredAccounts();

    /**
     * Returns the ServiceReference for the protocol provider corresponding to
     * the specified accountID or null if the accountID is unknown.
     * @param accountID the accountID of the protocol provider we'd like to get
     * @return a ServiceReference object to the protocol provider with the
     * specified account id and null if the account id is unknwon to the
     * account manager.
     */
     public ServiceReference getProviderForAccount(AccountID accountID);

    /**
     * Removes the specified account from the list of accounts that this
     * account manager is handling. If the specified accountID is unknown to the
     * AccountManager, the call has no effect and false is returned. This method
     * is persistent in nature and once called the account corresponding to the
     * specified ID will not be loaded during future runs of the project.
     *
     * @param accountID the ID of the account to remove.
     * @return true if an account with the specified ID existed and was removed
     * and false otherwise.
     */
    public boolean uninstallAccount(AccountID accountID);


}
