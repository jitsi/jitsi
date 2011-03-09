/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.ldap;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.ldap.*;

/**
 * @author Sebastien Mazy
 *
 * Implementation of LdapDirectorySettings
 * a wrapper around the settings needed to create an LdapDirectory
 */
public class LdapDirectorySettingsImpl
    implements LdapDirectorySettings
{
    /**
     * Simple constructor for this class,
     * sets default values,
     * note that you won't be able to create an LdapDirectory with these defaults
     * (empty name, empty hostname forbidden by LdapDirectory)
     */
    public LdapDirectorySettingsImpl()
    {
        this.setName("");
        this.setEnabled(true);
        this.setHostname("");
        this.setEncryption(Encryption.defaultValue());
        this.setPort(0);
        this.setAuth(Auth.defaultValue());
        this.setBindDN("");
        this.setPassword("");
        this.setBaseDN("");
        this.setScope(Scope.defaultValue());
    }

    /**
     * Constructor.
     *
     * @param settings existing settings
     */
    public LdapDirectorySettingsImpl(LdapDirectorySettingsImpl settings)
    {
        this();
        this.setName(settings.getName());
        this.setEnabled(settings.isEnabled());
        this.setHostname(settings.getHostname());
        this.setEncryption(settings.getEncryption());
        this.setPort(settings.getPort());
        this.setAuth(settings.getAuth());
        this.setBindDN(settings.getBindDN());
        this.setPassword(settings.getPassword());
        this.setBaseDN(settings.getBaseDN());
        this.setScope(settings.getScope());
    }

    /**
     * XML path where to store the directories settings
     */
    private final static String directoriesPath =
        "net.java.sip.communicator.impl.ldap.directories";

    /**
     * name that will be displayed in the UI
     * e.g. "My LDAP server"
     */
    private String name;

    /**
     * a marker
     */
    private boolean enabled;

    /**
     * the hostname,
     * e.g. "example.com"
     */
    private String hostname;

    /**
     * the encryption protocol
     *
     * @see net.java.sip.communicator.service.ldap.LdapConstants#Encryption
     */
    private Encryption encryption;

    /**
     * The network port number of the remote server
     */
    private int port;

    /**
     * the authentication method
     *
     * @see net.java.sip.communicator.service.ldap.LdapConstants#Auth
     */
    private Auth auth;

    /**
     * the bind distinguished name if authentication is needed
     * e.g. "cn=user,ou=People,dc=example,dc=com"
     */
    private String bindDN;

    /**
     * the password if authentication is needed
     */
    private String password;

    /**
     * distinguished name used as a base for searches
     * e.g. "dc=example,dc=com"
     */
    private String baseDN;

    /**
     * the search scope: one level under the base distinguished name
     * or all the subtree.
     */
    private Scope scope;

    /**
     * simple getter for name
     *
     * @return the name property
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * simple setter for name
     *
     * @param name the name property
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Simple getter for enabled. Required by LdapDirectorySettings interface.
     *
     * @return whether the server is marked as enabled
     *
     * @see LdapDirectorySettings#isEnabled
     */
    public boolean isEnabled()
    {
        return this.enabled;
    }

    /**
     * simple setter for enabled. Required by LdapDirectorySettings interface.
     *
     * @param enabled whether the server is marked as enabled
     *
     * @see LdapDirectorySettings#setEnabled
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * simple getter for hostname
     *
     * @return the hostname property
     */
    public String getHostname()
    {
        return this.hostname;
    }

    /**
     * simple setter for hostname
     *
     * @param hostname the hostname property
     */
    public void setHostname(String hostname)
    {
        this.hostname = hostname;
    }

    /**
     * simple getter for encryption
     *
     * @return the encryption property
     *
     * @see LdapConstants.Encryption
     */
    public Encryption getEncryption()
    {
        return this.encryption;
    }

    /**
     * simple setter for encryption
     *
     * @param encryption the encryption property
     *
     * @see LdapConstants.Encryption
     */
    public void setEncryption(Encryption encryption)
    {
        this.encryption = encryption;
    }

    /**
     * simple getter for port
     *
     * @return the port property
     */
    public int getPort()
    {
        return this.port;
    }

    /**
     * simple setter for port
     *
     * @param port the port property
     */
    public void setPort(int port)
    {
        this.port = port;
    }

    /**
     * simple getter for auth
     *
     * @return the auth property
     *
     * @see LdapConstants.Auth
     */
    public Auth getAuth()
    {
        return this.auth;
    }

    /**
     * simple setter for auth
     *
     * @param auth the auth property
     *
     * @see LdapConstants.Auth
     */
    public void setAuth(Auth auth)
    {
        this.auth = auth;
    }

    /**
     * simple getter for bindDN
     *
     * @return the bindDN property
     */
    public String getBindDN()
    {
        return this.bindDN;
    }

    /**
     * simple setter for bindDN
     *
     * @param bindDN the bindDN property
     */
    public void setBindDN(String bindDN)
    {
        this.bindDN = bindDN;
    }

    /**
     * simple getter for password
     *
     * @return the password property
     */
    public String getPassword()
    {
        return this.password;
    }

    /**
     * simple setter for password
     *
     * @param password the password property
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * simple getter for baseDN
     *
     * @return the baseDN property
     */
    public String getBaseDN()
    {
        return this.baseDN;
    }

    /**
     * simple setter for baseDN
     *
     * @param baseDN the baseDN property
     */
    public void setBaseDN(String baseDN)
    {
        this.baseDN = baseDN;
    }

    /**
     * Returns the search scope: one level under the base distinguished name
     * or all the subtree. Required by LdapDirectorySettings interface.
     *
     * @return the search scope
     *
     * @see LdapConstants.Scope
     * @see LdapDirectorySettings#getScope
     */
    public Scope getScope()
    {
        return this.scope;
    }

    /**
     * Sets the search scope: one level under the base distinguished name
     * or all the subtree. Required by LdapDirectorySettings interface.
     *
     * @param scope the new search scope
     *
     * @see LdapConstants.Scope
     * @see LdapDirectorySettings#setScope
     */
    public void setScope(Scope scope)
    {
        this.scope = scope;
    }

    /**
     * Checks if both LdapDirectorySettings instance have the same content
     *
     * @return whether both LdapDirectorySettings instance have the same content
     *
     * @see java.lang.Object#equals
     */
    public boolean equals(LdapDirectorySettings other)
    {
        /* enabled is not in equals on purpose */

        return this.getName().equals(other.getName()) &&
            this.getHostname().equals(other.getHostname()) &&
            this.getEncryption().equals(other.getEncryption()) &&
            this.getPort() == other.getPort() &&
            this.getAuth().equals(other.getAuth()) &&
            this.getBindDN().equals(other.getBindDN()) &&
            this.getPassword().equals(other.getPassword()) &&
            this.getBaseDN().equals(other.getBaseDN()) &&
            this.getScope().equals(other.getScope());
    }

    /**
     * Returns the hash code for this instance.
     * It has to be consistent with equals.
     *
     * @return the hash code dor this instance
     *
     * @see java.lang.Object#hashCode
     */
    public int hashCode()
    {
        /* enabled is not in the hashcode on purpose */

        int hash = 7;
        hash = 31 * hash + (null == this.getName() ? 0 :
            this.getName().hashCode());
        hash = 31 * hash + (null == this.getHostname() ? 0 :
            this.getHostname().hashCode());
        hash = 31 * hash + (null == this.getEncryption() ? 0 :
            this.getEncryption().hashCode());
        hash = 31 * hash + this.getPort();
        hash = 31 * hash + (null == this.getAuth() ? 0 :
            this.getAuth().hashCode());
        hash = 31 * hash + (null == this.getBindDN() ? 0 :
            this.getBindDN().hashCode());
        hash = 31 * hash + (null == this.getPassword() ? 0 :
            this.getPassword().hashCode());
        hash = 31 * hash + (null == this.getScope() ? 0 :
            this.getScope().hashCode());
        hash = 31 * hash + (null == this.getBaseDN() ? 0 :
            this.getBaseDN().hashCode());
        return hash;
    }

    /**
     * Saves these settings through the configuration service
     *
     * @see LdapDirectorySettings#persistentSave
     */
    public void persistentSave()
    {
        ConfigurationService configService = LdapServiceImpl.getConfigService();
        String node = "dir" + Math.abs(this.getName().hashCode());

        configService.setProperty(
                directoriesPath + "." + node,
                this.getName());
        configService.setProperty(
                directoriesPath + "." + node + ".enabled",
                this.isEnabled());
        configService.setProperty(
                directoriesPath + "." + node + ".hostname",
                this.getHostname());
        configService.setProperty(
                directoriesPath + "." + node + ".encryption",
                this.getEncryption().toString());
        configService.setProperty(
                directoriesPath + "." + node + ".port",
                String.valueOf(this.getPort()));
        configService.setProperty(
                directoriesPath + "." + node + ".auth",
                this.getAuth().toString());
        configService.setProperty(
                directoriesPath + "." + node + ".bindDN",
                this.getBindDN());
        configService.setProperty(
                directoriesPath + "." + node + ".password",
                this.getPassword());
        configService.setProperty(
                directoriesPath + "." + node + ".scope",
                this.getScope());
        configService.setProperty(
                directoriesPath + "." + node + ".baseDN",
                this.getBaseDN());
    }

    /**
     * Loads the settings with the given name from the config files
     * into the LdapDirectorySetting.
     *
     * @param name name of the settings
     *
     * @see LdapDirectorySettings#persistentLoad
     */
    public void persistentLoad(String name)
    {
        ConfigurationService configService = LdapServiceImpl.getConfigService();
        String node = "dir" + Math.abs(name.hashCode());

        if(configService.getProperty(directoriesPath + "." + node) == null)
            this.setName("");
        else
            this.setName( (String)
                    configService.getProperty(directoriesPath + "." + node));

        if(configService.getProperty(directoriesPath + "." + node + ".enabled")
                == null)
            this.setEnabled(true);
        else
            this.setEnabled(Boolean.parseBoolean(
                    (String)configService.getProperty(
                            directoriesPath + "." + node + ".enabled")));

        if(configService.getProperty(directoriesPath + "." + node + ".hostname")
                == null)
            this.setHostname("");
        else
            this.setHostname((String)
                    configService.getProperty(
                            directoriesPath + "." + node + ".hostname"));

        if(configService.getProperty(
                directoriesPath + "." + node + ".encryption") == null)
            this.setEncryption(Encryption.defaultValue());
        else
            this.setEncryption( Encryption.valueOf ((String)
                        configService.getProperty(
                                directoriesPath + "." + node + ".encryption")));

        if(configService.getProperty(directoriesPath + "." + node + ".port")
                == null)
            this.setPort(0);
        else
            this.setPort( Integer.parseInt ((String)
                        configService.getProperty(
                                directoriesPath + "." + node + ".port")));

        if(configService.getProperty(directoriesPath + "." + node + ".auth")
                == null)
            this.setAuth(Auth.defaultValue());
        else
            this.setAuth( Auth.valueOf ((String)
                        configService.getProperty(
                                directoriesPath + "." + node + ".auth")));

        if(configService.getProperty(
                directoriesPath + "." + node + ".bindDN") == null)
            this.setBindDN("");
        else
            this.setBindDN( (String)
                    configService.getProperty(
                            directoriesPath + "." + node + ".bindDN"));

        if(configService.getProperty(directoriesPath + "." + node + ".password")
                == null)
            this.setPassword("");
        else
            this.setPassword( (String)
                    configService.getProperty(
                            directoriesPath + "." + node + ".password"));

        if(configService.getProperty(directoriesPath + "." + node + ".scope")
                == null)
            this.setScope(Scope.defaultValue());
        else
            this.setScope( Scope.valueOf ((String)
                        configService.getProperty(
                                directoriesPath + "." + node + ".scope")));

        if(configService.getProperty(directoriesPath + "." + node + ".baseDN")
                == null)
            this.setBaseDN("");
        else
            this.setBaseDN( (String)
                    configService.getProperty(
                            directoriesPath + "." + node + ".baseDN"));
    }

    /**
     * Removes settings with this name from the configuration files
     * (package private)
     *
     * @see LdapDirectorySettings#persistentRemove
     */
    public void persistentRemove()
    {
        ConfigurationService configService = LdapServiceImpl.getConfigService();
        String node = "dir" + Math.abs(this.getName().hashCode());

        configService.setProperty(
                directoriesPath + "." + node + ".enabled",
                null);
        configService.setProperty(
                directoriesPath + "." + node + ".hostname",
                null);
        configService.setProperty(
                directoriesPath + "." + node + ".encryption",
                null);
        configService.setProperty(
                directoriesPath + "." + node + ".port",
                null);
        configService.setProperty(
                directoriesPath + "." + node + ".auth",
                null);
        configService.setProperty(
                directoriesPath + "." + node + ".bindDN",
                null);
        configService.setProperty(
                directoriesPath + "." + node + ".password",
                null);
        configService.setProperty(
                directoriesPath + "." + node + ".baseDN",
                null);
        configService.setProperty(
                directoriesPath + "." + node + ".scope",
                null);
        configService.setProperty(
                directoriesPath + "." + node,
                null);
    }

    /**
     * meant for debugging
     *
     * @return a string description of this instance
     */
    public String toString()
    {
        return "LdapDirectorySettings: {\n " +
            this.getName() + ", \n" +
            this.getHostname() + ", \n" +
            this.getEncryption() + ", \n" +
            this.getPort() + ", \n" +
            this.getAuth() + ", \n" +
            this.getBindDN() + ", \n" +
            this.getPassword() + ", \n" +
            this.getBaseDN() + " \n}";
    }

    public LdapDirectorySettings clone()
    {
        return new LdapDirectorySettingsImpl(this);
    }
}
