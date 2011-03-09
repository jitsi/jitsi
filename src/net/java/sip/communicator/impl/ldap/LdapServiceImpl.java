/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.ldap;

import java.util.*;

import org.osgi.framework.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.ldap.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

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
     * Reference to the resource management service
     */
    private static ResourceManagementService resourceService;

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
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ResourceManagementService getResourceManagementService()
    {
        if(resourceService == null)
        {
            ServiceReference confReference
                = bundleContext.getServiceReference(
                        ResourceManagementService.class.getName());
            resourceService
                = (ResourceManagementService) bundleContext.getService(
                        confReference);
        }
        return resourceService;
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
     * Required bu interface LdapService.
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
     * Loads config form the user preferences
     * in the serverSet
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
