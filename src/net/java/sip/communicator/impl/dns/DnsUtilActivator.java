/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.dns;

import net.java.sip.communicator.service.dns.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.netaddr.event.*;
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
    implements BundleActivator,
               ServiceListener
{
    /** Class logger */
    private static final Logger logger
        = Logger.getLogger(DnsUtilActivator.class);

    /**
     * The name of the property that sets custom nameservers to use for all DNS
     * lookups when DNSSEC is enabled. Multiple servers are separated by a comma
     * (,).
     */
    public static final String PNAME_DNSSEC_NAMESERVERS
        = "net.java.sip.communicator.util.dns.DNSSEC_NAMESERVERS";

    private static ConfigurationService configurationService;
    private static NotificationService notificationService;
    private static ResourceManagementService resourceService;
    private static BundleContext bundleContext;

    /**
     * The address of the backup resolver we would use by default.
     */
    public static final String DEFAULT_BACKUP_RESOLVER
        = "backup-resolver.jitsi.net";

    /**
     * The name of the property that users may use to override the port
     * of our backup DNS resolver.
     */
    public static final String PNAME_BACKUP_RESOLVER_PORT
        = "net.java.sip.communicator.util.dns.BACKUP_RESOLVER_PORT";

    /**
     * The name of the property that users may use to override the
     * IP address of our backup DNS resolver. This is only used when the
     * backup resolver name cannot be determined.
     */
    public static final String PNAME_BACKUP_RESOLVER_FALLBACK_IP
        = "net.java.sip.communicator.util.dns.BACKUP_RESOLVER_FALLBACK_IP";

    /**
     * The default of the property that users may use to disable
     * our backup DNS resolver.
     */
    public static final boolean PDEFAULT_BACKUP_RESOLVER_ENABLED = true;

    /**
     * The name of the property that users may use to disable
     * our backup DNS resolver.
     */
    public static final String PNAME_BACKUP_RESOLVER_ENABLED
        = "net.java.sip.communicator.util.dns.BACKUP_RESOLVER_ENABLED";

    /**
     * The name of the property that users may use to override the
     * address of our backup DNS resolver.
     */
    public static final String PNAME_BACKUP_RESOLVER
        = "net.java.sip.communicator.util.dns.BACKUP_RESOLVER";

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
        logger.info("DNS service ... [STARTING]");
        bundleContext = context;
        context.addServiceListener(this);

        if(UtilActivator.getConfigurationService().getBoolean(
                DnsUtilActivator.PNAME_BACKUP_RESOLVER_ENABLED,
                DnsUtilActivator.PDEFAULT_BACKUP_RESOLVER_ENABLED)
            && !getConfigurationService().getBoolean(
                CustomResolver.PNAME_DNSSEC_RESOLVER_ENABLED,
                CustomResolver.PDEFAULT_DNSSEC_RESOLVER_ENABLED))
        {
            bundleContext.registerService(
                CustomResolver.class.getName(),
                new ParallelResolverImpl(),
                null);
            logger.info("ParallelResolver ... [REGISTERED]");
        }

        if(getConfigurationService().getBoolean(
            CustomResolver.PNAME_DNSSEC_RESOLVER_ENABLED,
            CustomResolver.PDEFAULT_DNSSEC_RESOLVER_ENABLED))
        {
            bundleContext.registerService(
                CustomResolver.class.getName(),
                new ConfigurableDnssecResolver(),
                null);
            logger.info("DnssecResolver ... [REGISTERED]");
        }

        logger.info("DNS service ... [STARTED]");
    }

    /**
     * Listens when network is going from down to up and
     * resets dns configuration.
     */
    private static class NetworkListener
        implements NetworkConfigurationChangeListener
    {
        /**
         * Fired when a change has occurred in the
         * computer network configuration.
         *
         * @param event the change event.
         */
        public void configurationChanged(ChangeEvent event)
        {
            if((event.getType() == ChangeEvent.IFACE_UP
                || event.getType() == ChangeEvent.IFACE_DOWN
                || event.getType() == ChangeEvent.DNS_CHANGE)
                && !event.isInitial())
            {
                reloadDnsResolverConfig();
            }
        }
    }

    /**
     * Reloads dns server configuration in the resolver.
     */
    public static void reloadDnsResolverConfig()
    {
        // reread system dns configuration
        ResolverConfig.refresh();
        if(logger.isInfoEnabled())
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Reloaded resolver config, default DNS servers are: ");
            for(String s : ResolverConfig.getCurrentConfig().servers())
            {
                sb.append(s);
                sb.append(", ");
            }
            logger.info(sb.toString());
        }

        // now reset an eventually present custom resolver
        if(Lookup.getDefaultResolver() instanceof CustomResolver)
        {
            if (logger.isInfoEnabled())
            {
                logger.info("Resetting custom resolver "
                    + Lookup.getDefaultResolver().getClass().getSimpleName());
            }

            ((CustomResolver)Lookup.getDefaultResolver()).reset();
        }
        else
        {
            // or the default otherwise
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

    /**
     * Listens on OSGi service changes and registers a listener for network
     * changes as soon as the change-notification service is available
     */
    public void serviceChanged(ServiceEvent event)
    {
        if (event.getType() != ServiceEvent.REGISTERED)
        {
            return;
        }

        Object service = bundleContext.getService(event.getServiceReference());
        if (!(service instanceof NetworkAddressManagerService))
        {
            return;
        }

        ((NetworkAddressManagerService)service)
            .addNetworkConfigurationChangeListener(new NetworkListener());
    }
}
