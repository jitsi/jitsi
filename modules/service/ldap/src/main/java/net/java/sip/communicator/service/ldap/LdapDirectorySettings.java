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

import java.util.*;

/**
 * A wrapper around the settings needed to create an LdapDirectory
 * This object is mandatory to create an LdapServer. It's also the
 * retrieved object when calling getSettings() from LdapServer. It
 * also be used to retrieve, remove or store settings in the
 * persistent configuration.
 *
 * @author Sebastien Mazy
 */
public interface LdapDirectorySettings
    extends LdapConstants,
            Cloneable
{
    /**
     * simple getter for name
     *
     * @return the name property
     */
    public String getName();

    /**
     * simple setter for name
     *
     * @param name the name property
     */
    public void setName(String name);

    /**
     * simple getter for enabled
     *
     * @return whether the server is marked as enabled
     */
    public boolean isEnabled();

    /**
     * simple setter for enabled
     *
     * @param enabled whether the server is marked as enabled
     */
    public void setEnabled(boolean enabled);

    /**
     * simple getter for hostname
     *
     * @return the hostname property
     */
    public String getHostname();

    /**
     * simple setter for hostname
     *
     * @param hostname the hostname property
     */
    public void setHostname(String hostname);

    /**
     * simple getter for encryption
     *
     * @return the encryption property
     *
     * @see LdapConstants.Encryption
     */
    public Encryption getEncryption();

    /**
     * simple setter for encryption
     *
     * @param encryption the encryption property
     *
     * @see LdapConstants.Encryption
     */
    public void setEncryption(Encryption encryption);

    /**
     * simple getter for port
     *
     * @return the port property
     */
    public int getPort();

    /**
     * simple setter for port
     *
     * @param port the port property
     */
    public void setPort(int port);

    /**
     * simple getter for auth
     *
     * @return the auth property
     *
     * @see LdapConstants.Auth
     */
    public Auth getAuth();

    /**
     * simple setter for auth
     *
     * @param auth the auth property
     *
     * @see LdapConstants.Auth
     */
    public void setAuth(Auth auth);

    /**
     * simple getter for bindDN
     *
     * @return the bindDN property
     */
    public String getBindDN();

    /**
     * simple setter for bindDN
     *
     * @param bindDN the bindDN property
     */
    public void setBindDN(String bindDN);

    /**
     * simple getter for password
     *
     * @return the password property
     */
    public String getPassword();

    /**
     * simple setter for password
     *
     * @param password the password property
     */
    public void setPassword(String password);

    /**
     * simple getter for baseDN
     *
     * @return the baseDN property
     */
    public String getBaseDN();

    /**
     * simple setter for baseDN
     *
     * @param baseDN the baseDN property
     */
    public void setBaseDN(String baseDN);

    /**
     * Returns the search scope: one level under the base distinguished name
     * or all the subtree.
     *
     * @return the search scope
     *
     * @see LdapConstants.Scope
     */
    public Scope getScope();

    /**
     * Sets the search scope: one level under the base distinguished name
     * or all the subtree.
     *
     * @param scope the new search scope
     *
     * @see LdapConstants.Scope
     */
    public void setScope(Scope scope);

    /**
     * Returns mail fields that we will lookup.
     *
     * @return mail fields that we will lookup
     */
    public List<String> getMailSearchFields();

    /**
     * Set mail fields that we will lookup.
     *
     * @param list of mail fields that we will lookup
     */
    public void setMailSearchFields(List<String> list);

    /**
     * Returns mail suffix.
     *
     * @return mail suffix
     */
    public String getMailSuffix();

    /**
     * Set mail suffix.
     *
     * @param suffix mail suffix
     */
    public void setMailSuffix(String suffix);

    /**
     * Returns work phone fields that we will lookup.
     *
     * @return work phone fields that we will lookup
     */
    public List<String> getWorkPhoneSearchFields();

    /**
     * Set work phone fields that we will lookup.
     *
     * @param list of work phone fields that we will lookup
     */
    public void setWorkPhoneSearchFields(List<String> list);

    /**
     * Returns mobile phone fields that we will lookup.
     *
     * @return mobile phone fields that we will lookup
     */
    public List<String> getMobilePhoneSearchFields();

    /**
     * Set mobile phone fields that we will lookup.
     *
     * @param list of mobile phone fields that we will lookup
     */
    public void setMobilePhoneSearchFields(List<String> list);

    /**
     * Returns home phone fields that we will lookup.
     *
     * @return home phone fields that we will lookup
     */
    public List<String> getHomePhoneSearchFields();

    /**
     * Set home phone fields that we will lookup.
     *
     * @param list of home phone fields that we will lookup
     */
    public void setHomePhoneSearchFields(List<String> list);

    /**
     * Returns the global prefix to be used when calling phones from this ldap
     * source.
     *
     * @return the global prefix to be used when calling phones from this ldap
     * source
     */
    public String getGlobalPhonePrefix();

    /**
     * Sets the global prefix to be used when calling phones from this ldap
     * source.
     *
     * @param prefix the global prefix to be used when calling phones from this ldap
     * source
     */
    public void setGlobalPhonePrefix(String prefix);

    /**
     * Gets the mode how the LDAP query is constructed.
     * @return the mode how the LDAP query is constructed.
     */
    public String getQueryMode();

    /**
     * Sets the mode how the LDAP query is constructed.
     * @param queryMode the mode how the LDAP query is constructed.
     */
    public void setQueryMode(String queryMode);

    /**
     * Gets the user-defined LDAP query.
     * @return the user-defined LDAP query.
     */
    public String getCustomQuery();

    /**
     * Sets the user-defined LDAP query.
     * @param query the user-defined LDAP query.
     */
    public void setCustomQuery(String query);

    /**
     * Gets whether the query term gets mangled with wildcards.
     * @return whether the query term gets mangled with wildcards.
     */
    public boolean isMangleQuery();

    /**
     * Sets whether the query term gets mangled with wildcards.
     * @param mangle whether the query term gets mangled with wildcards.
     */
    public void setMangleQuery(boolean mangle);

    /**
     * Gets whether photos are retrieved along with the other attributes.
     * @return whether photos are retrieved along with the other attributes.
     */
    public boolean isPhotoInline();

    /**
     * Sets whether photos are retrieved along with the other attributes.
     * @param inline whether photos are retrieved along with the other
     *            attributes.
     */
    public void setPhotoInline(boolean inline);

    /**
     * Saves these settings through the configuration service
     *
     * @see LdapDirectorySettings#persistentSave
     */
    public void persistentSave();

    /**
     * Loads the settings with the given name from the config files
     * into the LdapDirectorySetting.
     *
     * @param name name of the settings
     *
     * @see LdapDirectorySettings#persistentLoad
     */
    public void persistentLoad(String name);

    /**
     * Removes settings with this name from the config files
     *
     */
    public void persistentRemove();

    /**
     * Clone this object.
     *
     * @return clone of this object
     */
    public LdapDirectorySettings clone();
}
