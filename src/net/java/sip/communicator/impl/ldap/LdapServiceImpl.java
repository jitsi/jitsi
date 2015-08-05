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

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.ldap.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * The LDAP service allows other modules to query an LDAP server.
 *
 * @author Sebastien Mazy
 */
public class LdapServiceImpl
    implements  LdapService
{
    /**
     * All the servers registered
     */
    private LdapDirectorySet serverSet;

    /**
     * the LdapFactory, used to create LdapDirectory-s,
     * LdapDirectorySettings, ...
     */
    private LdapFactory factory = new LdapFactoryImpl();

    /**
     * The logger for this class.
     */
    private static Logger logger = Logger
        .getLogger(LdapServiceImpl.class);

    /**
     * BundleContext from the OSGI bus.
     */
    private static BundleContext bundleContext;

    /**
     * Reference to the configuration service
     */
    private static ConfigurationService configService;

    /**
     * Reference to the credentials service
     */
    private static CredentialsStorageService credentialsService;

    /**
     * Reference to the Certificate Verification Service.
     */
    private static CertificateService certService = null;

    /**
     * Starts the service.
     *
     * @param bc BundleContext
     */
    public void start(BundleContext bc)
    {
        logger.trace("Starting the LDAP implementation.");
        bundleContext = bc;

        serverSet = new LdapDirectorySetImpl(getConfigService());
        loadPersistentConfig();
    }

    /**
     * Stops the service.
     *
     * @param bc BundleContext
     */
    public void stop(BundleContext bc)
    {
        logger.trace("Stopping the LDAP implementation.");
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigService()
    {
        if(configService == null)
        {
            ServiceReference confReference
                = bundleContext.getServiceReference(
                        ConfigurationService.class.getName());
            configService
                = (ConfigurationService) bundleContext.getService(
                        confReference);
        }
        return configService;
    }

    /**
     * Returns a reference to a CredentialsStorageConfigurationService
     * implementation currently registered in the bundle context or null if no
     * such implementation was found.
     *
     * @return a currently valid implementation of the
     * CredentialsStorageService.
     */
    public static CredentialsStorageService getCredentialsService()
    {
        if(credentialsService == null)
        {
            ServiceReference confReference
                = bundleContext.getServiceReference(
                        CredentialsStorageService.class.getName());
            credentialsService
                = (CredentialsStorageService) bundleContext.getService(
                        confReference);
        }
        return credentialsService;
    }

    /**
     * Gets the <tt>CertificateService</tt> to be used by the functionality of
     * the addrbook plug-in.
     *
     * @return the <tt>CertificateService</tt> to be used by the functionality
     *         of the addrbook plug-in.
     */
    public static CertificateService getCertificateService()
    {
        if (certService == null)
        {
            certService
                = ServiceUtils.getService(
                        bundleContext,
                        CertificateService.class);
        }
        return certService;
    }

    /**
     * Returns all the LDAP directories
     *
     * @return the LdapDirectorySet containing all the LdapDirectory(s)
     * registered
     *
     * @see net.java.sip.communicator.service.ldap#getServerSet
     */
    public LdapDirectorySet getServerSet()
    {
        return serverSet;
    }

    /**
     * Required by interface LdapService.
     * Returns the LdapFactory, used to
     * create LdapDirectory-s, LdapDirectorySettings, LdapQuery, ...
     *
     * @return the LdapFactory
     *
     * @see net.java.sip.communicator.service.ldap#getFactory
     */
    public LdapFactory getFactory()
    {
        return factory;
    }

    /**
     * Creates a contact source corresponding to the given ldap directory.
     *
     * @param ldapDir the ldap directory, for which we're creating the contact
     * source
     * @return the created contact source service
     */
    public ContactSourceService createContactSource(LdapDirectory ldapDir)
    {
        return LdapActivator.registerContactSource(ldapDir);
    }

    /**
     * Removes the contact source corresponding to the given ldap directory.
     *
     * @param ldapDir the ldap directory, which contact source we'd like to
     * remove
     */
    public void removeContactSource(LdapDirectory ldapDir)
    {
        LdapActivator.unregisterContactSource(ldapDir);
    }

    /**
     * Loads configuration form the user preferences in the serverSet
     */
    private void loadPersistentConfig()
    {
        String name;
        LdapDirectorySettings settings = getFactory().createServerSettings();
        LdapDirectory server;
        List<String> list = getConfigService().
            getPropertyNamesByPrefix(
                    "net.java.sip.communicator.impl.ldap.directories", true);

        for(Object configEntry : list)
        {
            name = getConfigService().getString(configEntry.toString());
            if(name != null)
            {
                settings.persistentLoad(name);
                server = this.getFactory().createServer(settings);
                this.serverSet.addServer(server);
            }
        }
    }
}
