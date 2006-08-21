/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.security;

import javax.sip.header.AuthorizationHeader;
import java.util.Vector;
import net.java.sip.communicator.service.protocol.*;

/**
 * Used to cache credentials through a call.
 *
 * @author Emil Ivov
 */
class CredentialsCacheEntry
{
    //The header that was used last time
    //AuthorizationHeader authorization = null;

    UserCredentials userCredentials = null;

    /**
     * The transactionHistory list contains transactions where the entry
     * has been and that had not yet been responded to (or at least the response
     * has not reached this class). The transactionHistory's elements are Strings
     * corresponding to branch ids.
     */
    private Vector transactionHistory = new Vector();

    /**
     * Adds the specified branch id to the transaction history list.
     * @param requestBranchID the id to add to the list of uncofirmed transactions.
     */
    void processRequest(String requestBranchID)
    {
        transactionHistory.add(requestBranchID);
    }

    /**
     * Determines whether these credentials have been used for the specified
     * transaction. If yes - the transaction is removed and true is returned.
     * Otherwise we return false.
     *
     * @param responseBranchID the branchi id of the response to process.
     * @return true if this entry hase been used for the transaction.
     */
    boolean processResponse(String responseBranchID)
    {
        return transactionHistory.remove(responseBranchID);
    }
}
