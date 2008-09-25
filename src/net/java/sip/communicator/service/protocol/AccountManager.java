/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * Represents a manager of accounts which contains the details about the format
 * in which the accounts in question are stored (i.e. knows how to store and load
 * them) and takes care of loading them on start-up.
 * 
 * @author Lubomir Marinov
 */
public interface AccountManager
{

    /**
     * Registers a specific listener to be notified about events fired by this
     * <code>AccountManager</code>. If the <code>listener</code> is already
     * registered, it will not be registered again.
     * 
     * @param listener the listener to be registered for notification events
     *            fired by this <code>AccountManager</code>
     */
    void addListener(AccountManagerListener listener);

    /**
     * Determines whether the account store represented by this manager contains
     * stored accounts.
     * 
     * @param protocolName the name of the protocol for which the stored
     *            accounts are to be checked or <tt>null</tt> for all protocols
     * @param includeHidden <tt>true</tt> to take into account both non-hidden
     *            and hidden stored accounts; <tt>false</tt> for non-hidden only
     * @return <tt>true</tt> if the account store represented by this manager
     *         contains stored accounts; <tt>false</tt>, otherwise
     */
    boolean hasStoredAccounts(String protocolName, boolean includeHidden);

    /**
     * Unregisters a specific listener from this <code>AccountManager</code> so
     * that it no longer received notifications about events fired by this
     * manager.
     * 
     * @param listener the listener to be unregistered from this
     *            <code>AccountManager</code> so that it no longer receives
     *            notifications about events fired by this manager
     */
    void removeListener(AccountManagerListener listener);

    /**
     * Stores an account represented in the form of an <code>AccountID</code>
     * created by a specific <code>ProtocolProviderFactory</code>.
     * 
     * @param factory the <code>ProtocolProviderFactory</code> which created the
     *            account to be stored
     * @param accountID the account in the form of <code>AccountID</code> to be
     *            stored
     */
    void storeAccount(ProtocolProviderFactory factory, AccountID accountID);
}
