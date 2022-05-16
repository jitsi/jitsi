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
package net.java.sip.communicator.impl.ldap;

import java.util.*;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.ldap.*;

import org.jitsi.service.configuration.*;

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
     * note that you won't be able to create an LdapDirectory with these
     * defaults (empty name, empty hostname forbidden by LdapDirectory)
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
        this.setGlobalPhonePrefix("");
        this.setQueryMode("");
        this.setCustomQuery("");
        this.setMangleQuery(true);
        this.setPhotoInline(false);
        // mail
        List<String> lst = new ArrayList<String>();
        lst.add("mail");
        lst.add("uid");
        mapAttributes.put("mail", lst);

        //work phone
        lst = new ArrayList<String>();
        lst.add("telephoneNumber");
        lst.add("primaryPhone");
        lst.add("companyPhone");
        lst.add("otherTelephone");
        lst.add("tel");
        mapAttributes.put("workPhone", lst);

        //mobile phone
        lst = new ArrayList<String>();
        lst.add("mobilePhone");
        lst.add("mobileTelephoneNumber");
        lst.add("mobileTelephoneNumber");
        lst.add("mobileTelephoneNumber");
        lst.add("carPhone");
        mapAttributes.put("mobilePhone", lst);

        //home phone
        lst = new ArrayList<String>();
        lst.add("homePhone");
        lst.add("otherHomePhone");
        mapAttributes.put("homePhone", lst);
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
        this.setGlobalPhonePrefix(settings.getGlobalPhonePrefix());
        this.mapAttributes = settings.mapAttributes;
        this.mailSuffix = settings.mailSuffix;
        this.queryMode = settings.queryMode;
        this.customQuery = settings.customQuery;
        this.mangleQuery = settings.mangleQuery;
        this.photoInline = settings.photoInline;
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
     * The global phone prefix.
     */
    private String globalPhonePrefix;

    /**
     * Mail suffix.
     */
    private String mailSuffix = null;

    /**
     * How the query should be constructed.
     */
    private String queryMode;

    /**
     * The user defined LDAP query.
     */
    private String customQuery;

    /**
     * Whether the query term should be automatically expanded for wildcards.
     */
    private boolean mangleQuery;

    /**
     * Whether photos are retrieved along with the other attributes.
     */
    private boolean photoInline;

    /**
     * Attributes map.
     */
    private Map<String, List<String> > mapAttributes =
        new HashMap<String, List<String> >();

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
     * @see net.java.sip.communicator.service.ldap.LdapConstants.Encryption
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
     * @see net.java.sip.communicator.service.ldap.LdapConstants.Encryption
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
     * @see net.java.sip.communicator.service.ldap.LdapConstants.Auth
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
     * @see net.java.sip.communicator.service.ldap.LdapConstants.Auth
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
     * Returns the user name associated with the corresponding ldap directory.
     *
     * @return the user name associated with the corresponding ldap directory
     */
    public String getUserName()
    {
        if (bindDN == null)
            return null;

        String userName = null;
        int uidIndex = bindDN.indexOf("uid=");
        if (uidIndex > -1)
        {
            int commaIndex = bindDN.indexOf(",", uidIndex + 5);

            if (commaIndex > -1)
                userName = bindDN.substring(uidIndex + 4, commaIndex);
            else
                userName = bindDN.substring(uidIndex + 4);
        }

        return userName;
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
     * @see net.java.sip.communicator.service.ldap.LdapConstants.Scope
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
     * @see net.java.sip.communicator.service.ldap.LdapConstants.Scope
     * @see LdapDirectorySettings#setScope
     */
    public void setScope(Scope scope)
    {
        this.scope = scope;
    }

    /**
     * Returns the global prefix to be used when calling phones from this ldap
     * source.
     *
     * @return the global prefix to be used when calling phones from this ldap
     * source
     */
    public String getGlobalPhonePrefix()
    {
        return globalPhonePrefix;
    }

    /**
     * Sets the global prefix to be used when calling phones from this ldap
     * source.
     *
     * @param prefix the global prefix to be used when calling phones from this
     * ldap source
     */
    public void setGlobalPhonePrefix(String prefix)
    {
        this.globalPhonePrefix = prefix;
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
            this.getScope().equals(other.getScope()) &&
            this.getGlobalPhonePrefix().equals(other.getGlobalPhonePrefix()) &&
            this.getCustomQuery().equals(other.getCustomQuery()) &&
            this.getQueryMode().equals(other.getQueryMode()) &&
            this.isMangleQuery() == other.isMangleQuery() &&
            this.isPhotoInline() == other.isPhotoInline();
    }

    /**
     * Returns the hash code for this instance.
     * It has to be consistent with equals.
     *
     * @return the hash code dor this instance
     *
     * @see java.lang.Object#hashCode
     */
    @Override
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
        hash = 31 * hash + (null == this.getGlobalPhonePrefix() ? 0 :
            this.getGlobalPhonePrefix().hashCode());
        hash = 31 * hash + (null == this.getCustomQuery() ? 0 :
            this.getCustomQuery().hashCode());
        hash = 32 * hash + (null == this.getQueryMode() ? 0 :
            this.getQueryMode().hashCode());
        hash = 33 * hash + (this.isMangleQuery() ? 1 : 0);
        hash = 34 * hash + (this.isPhotoInline() ? 1 : 0);
        return hash;
    }

    /**
     * Returns mail fields that we will lookup.
     *
     * @return mail fields that we will lookup
     */
    public List<String> getMailSearchFields()
    {
        return mapAttributes.get("mail");
    }

    /**
     * Set mail fields that we will lookup.
     *
     * @param list of mail fields that we will lookup
     */
    public void setMailSearchFields(List<String> list)
    {
        mapAttributes.put("mail", list);
    }

    /**
     * Returns mail suffix.
     *
     * @return mail suffix
     */
    public String getMailSuffix()
    {
        return mailSuffix;
    }

    /**
     * Set mail suffix.
     *
     * @param suffix mail suffix
     */
    public void setMailSuffix(String suffix)
    {
        this.mailSuffix = suffix;
    }

    /**
     * Gets the mode how the LDAP query is constructed.
     * @return the mode how the LDAP query is constructed.
     */
    @Override
    public String getQueryMode()
    {
        return this.queryMode;
    }

    /**
     * Sets the mode how the LDAP query is constructed.
     * @param queryMode the mode how the LDAP query is constructed.
     */
    @Override
    public void setQueryMode(String queryMode)
    {
        this.queryMode = queryMode;
    }

    /**
     * Gets the user-defined LDAP query.
     * @return the user-defined LDAP query.
     */
    @Override
    public String getCustomQuery()
    {
        return this.customQuery;
    }

    /**
     * Sets the user-defined LDAP query.
     * @param query the user-defined LDAP query.
     */
    @Override
    public void setCustomQuery(String query)
    {
        this.customQuery = query;
    }

    /**
     * Gets whether the query term gets mangled with wildcards.
     * @return whether the query term gets mangled with wildcards.
     */
    @Override
    public boolean isMangleQuery()
    {
        return this.mangleQuery;
    }

    /**
     * Sets whether the query term gets mangled with wildcards.
     * @param mangle whether the query term gets mangled with wildcards.
     */
    @Override
    public void setMangleQuery(boolean mangle)
    {
        this.mangleQuery = mangle;
    }

    /**
     * Gets whether photos are retrieved along with the other attributes.
     * @return whether photos are retrieved along with the other attributes.
     */
    @Override
    public boolean isPhotoInline()
    {
        return this.photoInline;
    }

    /**
     * Sets whether photos are retrieved along with the other attributes.
     * @param inline whether photos are retrieved along with the other
     *            attributes.
     */
    @Override
    public void setPhotoInline(boolean inline)
    {
        this.photoInline = inline;
    }

    /**
     * Returns work phone fields that we will lookup.
     *
     * @return work phone fields that we will lookup
     */
    public List<String> getWorkPhoneSearchFields()
    {
        return mapAttributes.get("workPhone");
    }

    /**
     * Set work phone fields that we will lookup.
     *
     * @param list of work phone fields that we will lookup
     */
    public void setWorkPhoneSearchFields(List<String> list)
    {
        mapAttributes.put("workPhone", list);
    }

    /**
     * Returns mobile phone fields that we will lookup.
     *
     * @return mobile phone fields that we will lookup
     */
    public List<String> getMobilePhoneSearchFields()
    {
        return mapAttributes.get("mobilePhone");
    }

    /**
     * Set mobile phone fields that we will lookup.
     *
     * @param list of mobile phone fields that we will lookup
     */
    public void setMobilePhoneSearchFields(List<String> list)
    {
        mapAttributes.put("mobilePhone", list);
    }

    /**
     * Returns home phone fields that we will lookup.
     *
     * @return home phone fields that we will lookup
     */
    public List<String> getHomePhoneSearchFields()
    {
        return mapAttributes.get("homePhone");
    }

    /**
     * Set home phone fields that we will lookup.
     *
     * @param list of home phone fields that we will lookup
     */
    public void setHomePhoneSearchFields(List<String> list)
    {
        mapAttributes.put("homePhone", list);
    }

    /**
     * Merge String elements from a list to a single String separated by space.
     *
     * @param lst list of <tt>String</tt>s
     * @return <tt>String</tt>
     */
    public static String mergeStrings(List<String> lst)
    {
        StringBuilder bld = new StringBuilder();

        for(String s : lst)
        {
            bld.append(s).append(" ");
        }

        return bld.toString();
    }

    /**
     * Merge String elements separated by space into a List.
     *
     * @param attrs <tt>String</tt>
     * @return list of <tt>String</tt>
     */
    public static List<String> mergeString(String attrs)
    {
        StringTokenizer token = new StringTokenizer(attrs, " ");
        List<String> lst = new ArrayList<String>();

        while(token.hasMoreTokens())
        {
            lst.add(token.nextToken());
        }

        return lst;
    }

    /**
     * Saves these settings through the configuration service
     *
     * @see LdapDirectorySettings#persistentSave
     */
    public void persistentSave()
    {
        ConfigurationService configService = LdapServiceImpl.getConfigService();
        CredentialsStorageService credentialsService =
            LdapServiceImpl.getCredentialsService();
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
        credentialsService.storePassword(directoriesPath + "." + node,
                this.getPassword());
        configService.setProperty(
                directoriesPath + "." + node + ".scope",
                this.getScope());
        configService.setProperty(
                directoriesPath + "." + node + ".baseDN",
                this.getBaseDN());
        configService.setProperty(
                directoriesPath + "." + node + ".overridemail",
                mergeStrings(this.getMailSearchFields()));
        configService.setProperty(
            directoriesPath + "." + node + ".overridemailsuffix",
            mailSuffix);
        configService.setProperty(
            directoriesPath + "." + node + ".overrideworkphone",
            mergeStrings(this.getWorkPhoneSearchFields()));
        configService.setProperty(
            directoriesPath + "." + node + ".overridemobilephone",
            mergeStrings(this.getMobilePhoneSearchFields()));
        configService.setProperty(
            directoriesPath + "." + node + ".overridehomephone",
            mergeStrings(this.getHomePhoneSearchFields()));
        configService.setProperty(
            directoriesPath + "." + node + ".globalPhonePrefix",
                        this.getGlobalPhonePrefix());
        configService.setProperty(
            directoriesPath + "." + node + ".querymode",
            this.getQueryMode());
        configService.setProperty(
            directoriesPath + "." + node + ".customquery",
            this.getCustomQuery());
        configService.setProperty(
            directoriesPath + "." + node + ".mangle",
            this.isMangleQuery());
        configService.setProperty(
            directoriesPath + "." + node + ".inlinephoto",
            this.isPhotoInline());
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
        CredentialsStorageService credentialsService =
            LdapServiceImpl.getCredentialsService();
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

        String password =
            credentialsService.loadPassword(directoriesPath + "." + node);
        if(password == null)
        {
            this.setPassword("");
        }
        else
        {
            this.setPassword(password);
        }

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

        if(configService.getProperty(directoriesPath + "." + node +
            ".overridemail") != null)
        {
            String ret = (String)configService.getProperty(
                directoriesPath + "." + node + ".overridemail");

            mapAttributes.put("mail", mergeString(ret));
        }

        if(configService.getProperty(directoriesPath + "." + node +
            ".overridemailsuffix") != null)
        {
            String ret = (String)configService.getProperty(
                directoriesPath + "." + node + ".overridemailsuffix");

            this.mailSuffix = ret;
        }

        if(configService.getProperty(directoriesPath + "." + node +
            ".overrideworkphone") != null)
        {
            String ret = (String)configService.getProperty(
                directoriesPath + "." + node + ".overrideworkphone");

            mapAttributes.put("workPhone", mergeString(ret));
        }

        if(configService.getProperty(directoriesPath + "." + node +
            ".overridemobilephone") != null)
        {
            String ret = (String)configService.getProperty(
                directoriesPath + "." + node + ".overridemobilephone");

            mapAttributes.put("mobilePhone", mergeString(ret));
        }

        if(configService.getProperty(directoriesPath + "." + node +
            ".overridehomephone") != null)
        {
            String ret = (String)configService.getProperty(
                directoriesPath + "." + node + ".overridehomephone");

            mapAttributes.put("homePhone", mergeString(ret));
        }

        if(configService.getProperty(directoriesPath + "." + node +
            ".globalPhonePrefix") != null)
        {
            String ret = (String)configService.getProperty(
                directoriesPath + "." + node + ".globalPhonePrefix");

            if (ret != null)
                setGlobalPhonePrefix(ret);
        }

        if(configService.getProperty(directoriesPath + "." + node +
            ".querymode") != null)
        {
            String ret = (String)configService.getProperty(
                directoriesPath + "." + node + ".querymode");

            if (ret != null)
                setQueryMode(ret);
        }

        if(configService.getProperty(directoriesPath + "." + node +
            ".customquery") != null)
        {
            String ret = (String)configService.getProperty(
                directoriesPath + "." + node + ".customquery");

            if (ret != null)
                setCustomQuery(ret);
        }

        if(configService.getProperty(directoriesPath + "." + node +
            ".mangle") != null)
        {
            Boolean ret = Boolean.parseBoolean(
                (String)configService.getProperty(
                    directoriesPath + "." + node + ".mangle"));

            if (ret != null)
                setMangleQuery(ret);
        }

        if(configService.getProperty(directoriesPath + "." + node +
            ".inlinephoto") != null)
        {
            Boolean ret = Boolean.parseBoolean(
                (String)configService.getProperty(
                    directoriesPath + "." + node + ".inlinephoto"));

            if (ret != null)
                setPhotoInline(ret);
        }
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
        CredentialsStorageService credentialsService =
            LdapServiceImpl.getCredentialsService();
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
        credentialsService.removePassword(directoriesPath + "." + node);
        configService.setProperty(
                directoriesPath + "." + node + ".baseDN",
                null);
        configService.setProperty(
                directoriesPath + "." + node + ".scope",
                null);
        configService.setProperty(
            directoriesPath + "." + node + ".overridemail",
            null);
        configService.setProperty(
            directoriesPath + "." + node + ".overridemailsuffix",
            null);
        configService.setProperty(
            directoriesPath + "." + node + ".overrideworkphone",
            null);
        configService.setProperty(
            directoriesPath + "." + node + ".overridemobilephone",
            null);
        configService.setProperty(
            directoriesPath + "." + node + ".overridehomephone",
            null);
        configService.setProperty(
            directoriesPath + "." + node + ".globalPhonePrefix",
            null);
        configService.setProperty(
                directoriesPath + "." + node,
                null);
        configService.setProperty(
            directoriesPath + "." + node + ".querymode",
            null);
        configService.setProperty(
            directoriesPath + "." + node + ".customquery",
            null);
        configService.setProperty(
            directoriesPath + "." + node + ".mangle",
            null);
        configService.setProperty(
            directoriesPath + "." + node + ".inlinephoto",
            null);
    }

    /**
     * meant for debugging
     *
     * @return a string description of this instance
     */
    @Override
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
            this.getBaseDN() + ", \n" +
            this.getGlobalPhonePrefix() + " \n" +
            this.queryMode + " \n" +
            this.customQuery + " \n" +
            this.mangleQuery + " \n" +
            this.photoInline + " \n}";
    }

    @Override
    public LdapDirectorySettings clone()
    {
        return new LdapDirectorySettingsImpl(this);
    }
}
