/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.ldap;

import org.osgi.framework.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.ldap.*;

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
     * @throws Exception if something goes wrong when starting service
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

            logger.trace("LDAP Service ...[REGISTERED]");
        }
        finally
        {
            logger.logExit();
        }
    }

    /**
     * Stops the LDAP service
     *
     * @param bundleContext BundleContext
     * @throws Exception if something goes wrong when stopping service
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        if(ldapService != null)
            ldapService.stop(bundleContext);
    }
}
