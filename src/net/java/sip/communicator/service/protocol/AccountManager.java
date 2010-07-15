/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * Represents a manager of accounts which contains the details about the format
 * in which the accounts in question are stored (i.e. knows how to store and
 * load them) and takes care of loading them on start-up.
 * 
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public interface AccountManager
{
    /**
     * Registers a specific listener to be notified about events fired by this
     * <tt>AccountManager</tt>. If the <tt>listener</tt> is already
     * registered, it will not be registered again.
     * 
     * @param listener
     *            the listener to be registered for notification events fired by
     *            this <tt>AccountManager</tt>
     */
    void addListener(AccountManagerListener listener);

    /**
     * Determines whether the account store represented by this manager contains
     * stored accounts.
     * 
     * @param protocolName
     *            the name of the protocol for which the stored accounts are to
     *            be checked or <tt>null</tt> for all protocols
     * @param includeHidden
     *            <tt>true</tt> to take into account both non-hidden and hidden
     *            stored accounts; <tt>false</tt> for non-hidden only
     * @return <tt>true</tt> if the account store represented by this manager
     *         contains stored accounts; <tt>false</tt>, otherwise
     */
    boolean hasStoredAccounts(String protocolName, boolean includeHidden);

    /**
     * Unregisters a specific listener from this <tt>AccountManager</tt> so
     * that it no longer received notifications about events fired by this
     * manager.
     * 
     * @param listener
     *            the listener to be unregistered from this
     *            <tt>AccountManager</tt> so that it no longer receives
     *            notifications about events fired by this manager
     */
    void removeListener(AccountManagerListener listener);

    /**
     * Stores an account represented in the form of an <tt>AccountID</tt>
     * created by a specific <tt>ProtocolProviderFactory</tt>.
     * 
     * @param factory the <tt>ProtocolProviderFactory</tt> which created the
     * account to be stored
     * @param accountID the account in the form of <tt>AccountID</tt> to be
     * stored
     * @throws OperationFailedException if anything goes wrong while storing the
     * specified account
     */
    void storeAccount(ProtocolProviderFactory factory, AccountID accountID)
        throws OperationFailedException;

    /**
     * Removes the account with <tt>accountID</tt> from the set of accounts
     * that are persistently stored inside the configuration service.
     * <p>
     * @param factory the <tt>ProtocolProviderFactory</tt> which created the
     * account to be stored
     * @param accountID the AccountID of the account to remove.
     * <p>
     * @return true if an account has been removed and false otherwise.
     */
    boolean removeStoredAccount(ProtocolProviderFactory factory,
                                AccountID accountID);

    /**
     * Returns an <tt>Iterator</tt> over a list of all stored
     * <tt>AccountID</tt>s. The list of stored accounts include all registered
     * accounts and all disabled accounts. In other words in this list we could
     * find accounts that aren't loaded.
     * <p>
     * In order to check if an account is already loaded please use the
     * #isAccountLoaded(AccountID accountID) method. To load an account use the
     * #loadAccount(AccountID accountID) method.
     *
     * @return a <tt>Collection</tt> of all stored <tt>AccountID</tt>s
     */
    public Collection<AccountID> getStoredAccounts();

    /**
     * Loads the account corresponding to the given <tt>AccountID</tt>. An
     * account is loaded when its <tt>ProtocolProviderService</tt> is registered
     * in the bundle context. This method is meant to load the account through
     * the corresponding <tt>ProtocolProviderFactory</tt>.
     *
     * @param accountID the identifier of the account to load
     * @throws OperationFailedException if anything goes wrong while loading the
     * account corresponding to the specified <tt>accountID</tt>
     */
    public void loadAccount(AccountID accountID)
        throws OperationFailedException;

    /**
     * Unloads the account corresponding to the given <tt>AccountID</tt>. An
     * account is unloaded when its <tt>ProtocolProviderService</tt> is
     * unregistered in the bundle context. This method is meant to unload the
     * account through the corresponding <tt>ProtocolProviderFactory</tt>.
     *
     * @param accountID the identifier of the account to load
     * @throws OperationFailedException if anything goes wrong while unloading
     * the account corresponding to the specified <tt>accountID</tt>
     */
    public void unloadAccount(AccountID accountID)
        throws OperationFailedException;

    /**
     * Checks if the account corresponding to the given <tt>accountID</tt> is
     * loaded. An account is loaded if its <tt>ProtocolProviderService</tt> is
     * registered in the bundle context. By default all accounts are loaded.
     * However the user could manually unload an account, which would be
     * unregistered from the bundle context, but would remain in the
     * configuration file.
     *
     * @param accountID the identifier of the account to load
     * @return <tt>true</tt> to indicate that the account with the given
     * <tt>accountID</tt> is loaded, <tt>false</tt> - otherwise
     */
    public boolean isAccountLoaded(AccountID accountID);
}
