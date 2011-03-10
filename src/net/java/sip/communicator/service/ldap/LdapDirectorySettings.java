/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.ldap;

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
     * Checks if both LdapDirectorySettings instance have the same content
     *
     * @param other object to compare
     * @return whether both LdapDirectorySettings instance have the same content
     *
     * @see java.lang.Object#equals
     */
    public boolean equals(LdapDirectorySettings other);

    /**
     * Returns the hash code for this instance.
     * It has to be consistent with equals.
     *
     * @return the hash code dor this instance
     *
     * @see java.lang.Object#hashCode
     */
    public int hashCode();

    /**
     * Clone this object.
     *
     * @return clone of this object
     */
    public LdapDirectorySettings clone();
}
