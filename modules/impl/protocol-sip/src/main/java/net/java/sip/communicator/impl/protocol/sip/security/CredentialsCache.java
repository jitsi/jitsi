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
import java.util.Map.Entry;

import javax.sip.header.*;

/**
 * The class is used to cache all realms that a certain call has been authorized
 * against and all credentials that have been used for each realm. Note that
 * rfc3261 suggests keeping callId->credentials mapping where as we map
 * realm->credentials. This is done to avoid asking the user for a password
 * before each call.
 *
 * @author Emil Ivov
 * @author Nie Pin
 * @version 1.0
 */

class CredentialsCache
{
    /**
     * Contains call->realms mappings
     */
    private Hashtable<String, CredentialsCacheEntry> authenticatedRealms
                            = new Hashtable<String, CredentialsCacheEntry>();

    /**
     * Contains callid->authorization header mappings
     */
    private Hashtable<String, AuthorizationHeader> authenticatedCalls
                            =  new Hashtable<String, AuthorizationHeader>();

    /**
     * Cache credentials for the specified call and realm
     * @param realm the realm that the specify credentials apply to
     * @param cacheEntry the credentials
     */
    void cacheEntry(String realm,
                    CredentialsCacheEntry cacheEntry)
    {
        authenticatedRealms.put(realm, cacheEntry);
    }

    /**
     * Returns the credentials corresponding to the specified realm
     * or null if none could be found.
     *
     * @param realm the realm that the credentials apply to
     * @return the credentials corresponding to the specified realm
     * or null if none could be found.
     */
    CredentialsCacheEntry get(String realm)
    {
        return this.authenticatedRealms.get(realm);
    }

    /**
     * Returns the list of realms that <tt>branchID</tt> has been used to
     * authenticate against.
     *
     * @param branchID the transaction branchID that we are looking for.
     *
     * @return the list of realms that <tt>branchID</tt> has been used to
     * authenticate against.
     */
    List<String> getRealms(String branchID)
    {
        List<String> realms = new LinkedList<String>();

        Iterator<Entry<String, CredentialsCacheEntry>> credentials =
            authenticatedRealms.entrySet().iterator();

        while ( credentials.hasNext())
        {
            Entry<String, CredentialsCacheEntry> entry = credentials.next();

            if (entry.getValue().containsBranchID(branchID))
                realms.add(entry.getKey());
        }

        return realms;
    }

    /**
     * Returns the credentials corresponding to the specified realm
     * or null if none could be found and removes the entry from the cache.
     *
     * @param realm the realm that the credentials apply to
     * @return the credentials corresponding to the specified realm
     * or null if none could be found.
     */
    CredentialsCacheEntry remove(String realm)
    {
        return this.authenticatedRealms.remove(realm);
    }

    /**
     * Empty the credentials cache (all authorization challenges) would end up
     * requesting a password from the user.
     */
    void clear()
    {
        authenticatedRealms.clear();
    }

    /**
     * Cache the bindings of call-id and authorization header.
     *
     * @param callid the id of the call that the <tt>authorization</tt> header
     * belongs to.
     * @param authorization the authorization header that we'd like to cache.
     */
    void cacheAuthorizationHeader(String              callid,
                                  AuthorizationHeader authorization)
    {
        authenticatedCalls.put(callid, authorization);
    }

    /**
     * Returns an authorization header cached for the specified call id and null
     * if no authorization header has been previously cached for this call.
     *
     * @param callid the call id that we'd like to retrive a cached
     * authorization header for.
     *
     * @return authorization header corresponding to the specified callid
     */
    AuthorizationHeader getCachedAuthorizationHeader(String callid)
    {
        return this.authenticatedCalls.get(callid);
    }
}
