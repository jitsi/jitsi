/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.ldap;

import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.ldap.*;
import net.java.sip.communicator.service.gui.*;

/**
 * Activates the LdapService
 *
 * @author Sebastien Mazy
 */
public class LdapActivator
    implements BundleActivator
{
    /**
     * the logger for this class
     */
    private static Logger logger =
        Logger.getLogger(LdapActivator.class);

    /**
     * instance of the service
     */
    private static LdapServiceImpl ldapService = null;

    /**
     * Get LDAP service.
     *
     * @return LDAP service
     */
    public static LdapService getLdapService()
    {
        return ldapService;
    }

    /**
     * Starts the LDAP service
     *
     * @param bundleContext BundleContext
     * @throws Exception
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        try
        {
            logger.logEntry();

            /* Creates and starts the LDAP service. */
            ldapService =
                new LdapServiceImpl();

            ldapService.start(bundleContext);

            bundleContext.registerService(
                    LdapService.class.getName(), ldapService, null);

            /* registers the configuration form */
            Dictionary<String, String> properties =
                new Hashtable<String, String>();
            properties.put( ConfigurationForm.FORM_TYPE,
                            ConfigurationForm.ADVANCED_TYPE);

            bundleContext.registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                    "net.java.sip.communicator.impl.ldap.configform.LdapConfigForm",
                    getClass().getClassLoader(),
                    "impl.ldap.PLUGIN_ICON",
                    "impl.ldap.CONFIG_FORM_TITLE",
                    2000, true),
                properties);

            logger.trace("LDAP Service ...[REGISTERED]");
        }
        finally
        {
            logger.logExit();
        }
    }

    /**
     * Stops the LDAP service
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        if(ldapService != null)
            ldapService.stop(bundleContext);
    }
}
