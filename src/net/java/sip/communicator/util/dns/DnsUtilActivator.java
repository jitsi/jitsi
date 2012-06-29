/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.dns;

import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;
import org.xbill.DNS.*;

/**
 * The DNS Util activator registers the DNSSEC resolver if enabled.
 *
 * @author Emil Ivov
 * @author Ingo Bauersachs
 */
public class DnsUtilActivator
    implements BundleActivator
{
    /**
     * The name of the property that enables or disables the DNSSEC resolver
     * (instead of a normal, non-validating local resolver).
     */
    public static final String PNAME_DNSSEC_RESOLVER_ENABLED
        = "net.java.sip.communicator.util.dns.DNSSEC_ENABLED";

    /**
     * Default value of @see PNAME_DNSSEC_RESOLVER_ENABLED.
     */
    public static final boolean PDEFAULT_DNSSEC_RESOLVER_ENABLED = false;

    /**
     * The name of the property that sets custom nameservers to use for all DNS
     * lookups when DNSSEC is enabled. Multiple servers are separated by a comma
     * (,).
     */
    public static final String PNAME_DNSSEC_NAMESERVERS
        = "net.java.sip.communicator.util.dns.DNSSEC_NAMESERVERS";

    /**
     * The <tt>Logger</tt> used by the <tt>UtilActivator</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(DnsUtilActivator.class);

    private static ConfigurationService configurationService;
    private static NotificationService notificationService;
    private static ResourceManagementService resourceService;
    private static BundleContext bundleContext;

    /**
     * Calls <tt>Thread.setUncaughtExceptionHandler()</tt>
     *
     * @param context The execution context of the bundle being started
     * (unused).
     * @throws Exception If this method throws an exception, this bundle is
     *   marked as stopped and the Framework will remove this bundle's
     *   listeners, unregister all services registered by this bundle, and
     *   release all services used by this bundle.
     */
    public void start(BundleContext context)
        throws Exception
    {
        bundleContext = context;

        if(getConfigurationService().getBoolean(
            PNAME_DNSSEC_RESOLVER_ENABLED,
            PDEFAULT_DNSSEC_RESOLVER_ENABLED))
        {
            getNotificationService().
                registerDefaultNotificationForEvent(
                    ConfigurableDnssecResolver.EVENT_TYPE,
                    NotificationAction.ACTION_POPUP_MESSAGE,
                    null, null);
        }
        refreshResolver();
    }

    /**
     * Sets a DNSSEC resolver as default resolver on lookup when DNSSEC is
     * enabled; creates a standard lookup otherwise.
     */
    public static void refreshResolver()
    {
        if(getConfigurationService().getBoolean(
            PNAME_DNSSEC_RESOLVER_ENABLED,
            PDEFAULT_DNSSEC_RESOLVER_ENABLED))
        {
            logger.trace("DNSSEC is enabled");
            ConfigurableDnssecResolver res = new ConfigurableDnssecResolver();
            for(int i = 1;;i++)
            {
                String anchor = getResources().getSettingsString(
                    "net.java.sip.communicator.util.dns.DS_ROOT." + i);
                if(anchor == null)
                    break;
                res.addTrustAnchor(anchor);
                if(logger.isTraceEnabled())
                    logger.trace("Loaded trust anchor " + anchor);
            }
            Lookup.setDefaultResolver(res);
        }
        else
        {
            logger.trace("DNSSEC is disabled, refresh default config");
            Lookup.refreshDefault();
        }
    }

    /**
     * Doesn't do anything.
     *
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext context)
        throws Exception
    {
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            configurationService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configurationService;
    }

    /**
     * Returns the <tt>NotificationService</tt> obtained from the bundle context.
     *
     * @return the <tt>NotificationService</tt> obtained from the bundle context
     */
    public static NotificationService getNotificationService()
    {
        if (notificationService == null)
        {
            notificationService
                = ServiceUtils.getService(
                        bundleContext,
                        NotificationService.class);
        }
        return notificationService;
    }

    /**
     * Returns the service giving access to all application resources.
     *
     * @return the service giving access to all application resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourceService == null)
        {
            resourceService
                = ResourceManagementServiceUtils.getService(bundleContext);
        }
        return resourceService;
    }
}
