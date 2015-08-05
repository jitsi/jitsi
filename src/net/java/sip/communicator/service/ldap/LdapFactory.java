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
 * The LdapFactory is used to
 * create LdapDirectoryS, LdapDirectorySettings, LdapQueryS, ...
 *
 * @author Sebastien Mazy
 */
public interface LdapFactory
{
    /**
     * Creates an LdapDirectory based on the provided settings
     * This method will not modify the <tt>settings</tt>
     * or save a reference to it, but may save a clone.
     *
     * @param settings settings for this new server
     *
     * @return a reference to the created LdapDirectory
     * @throws IllegalArgumentException if argument is invalid
     * @see LdapDirectorySettings
     */
    public LdapDirectory createServer(LdapDirectorySettings settings)
        throws IllegalArgumentException;

    /**
     * Return a new instance of LdapDirectorySettings,
     * a wrapper around a directory settings
     *
     * @return a new instance of LdapDirectorySettings
     *
     * @see LdapDirectorySettings
     */
    public LdapDirectorySettings createServerSettings();

    /**
     * Returns an LDAP query, ready to be sent to an LdapDirectory
     *
     * @param query query string
     * @return an LDAP query, ready to be sent to an LdapDirectory
     */
    public LdapQuery createQuery(String query);

    /**
     * Returns an LdapSearchSettings, to use when performing a search
     *
     * @return an LdapSearchSettings, to use when performing a search
     */
    public LdapSearchSettings createSearchSettings();
}
