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
package net.java.sip.communicator.service.ldap;

/**
 * Represents an LDAP search query. A query needs
 * to be created in order to use searchPerson()
 * in LdapDirectorySettings or LdapDirectory
 *
 * @author Sebastien Mazy
 */
public interface LdapQuery
{
    /**
     * State of LDAP query.
     */
    public static enum State
    {
        /**
         * Pending state.
         */
        PENDING,
        /**
         * Completed state.
         */
        COMPLETED,

        /**
         * Results left.
         */
        RESULTS_LEFT,

        /**
         * Cancelled state.
         */
        CANCELLED;
    }

    /**
     * Sets the query state to newState
     *
     * @param newState the query state
     */
    public void setState(State newState);

    /**
     * Returns the query state
     *
     * @return the query state
     */
    public State getState();

    /**
     * Returns the query string
     * e.g "John Doe"
     *
     * @return the query string
     */
    public String toString();
}
