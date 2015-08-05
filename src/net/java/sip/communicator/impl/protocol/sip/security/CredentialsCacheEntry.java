/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.sip.security;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Used to cache credentials through a call.
 *
 * @author Emil Ivov
 */
class CredentialsCacheEntry
{
    /**
     * The user credentials associated with this cache entry.
     */
    public UserCredentials userCredentials = null;

    /**
     * The transactionHistory list contains transactions where the entry
     * has been and that had not yet been responded to (or at least the response
     * has not reached this class). The transactionHistory's elements are
     * <tt>String</tt>s corresponding to branch id-s.
     */
    private Vector<String> transactionHistory = new Vector<String>();

    /**
     * Adds the specified branch id to the transaction history list so that we
     * know that we've seen it and don't try to authenticate with the same
     * credentials if we get an unauthorized response for it.
     *
     * @param requestBranchID the id to add to the list of unconfirmed
     * transactions.
     */
    void pushBranchID(String requestBranchID)
    {
        transactionHistory.add(requestBranchID);
    }

    /**
     * Determines whether these credentials have been used for the specified
     * transaction. If yes - the transaction is removed and true is returned.
     * Otherwise we return false. We remove the transaction simply because
     * there is no point in keeping it. We can't get an unauthorized response
     * more than once in the same transaction.
     *
     * @param responseBranchID the branch id of the response to process.
     * @return true if this entry has been used for the transaction.
     */
    boolean popBranchID(String responseBranchID)
    {
        return transactionHistory.remove(responseBranchID);
    }

    /**
     * Determines whether these credentials have been used for the specified
     * transaction.
     *
     * @param branchID the branch id of the transaction that we are looking for.
     *
     * @return <tt>true</tt> if this entry has been used for the transaction
     * and <tt>false</tt> otherwise.
     */
    boolean containsBranchID(String branchID)
    {
        return transactionHistory.contains(branchID);
    }
}
