/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.certificate;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The certificate verification bundle activator.
 *
 * @author Yana Stamcheva
 */
public class CertificateVerificationActivator
    implements BundleActivator
{
    /**
     * The bundle context for this bundle.
     */
    protected static BundleContext bundleContext;

    /**
     * The configuration service.
     */
    private static ConfigurationService configService;

    /**
     * The service giving access to files.
     */
    private static FileAccessService fileAccessService;

    /**
     * The service giving access to all resources.
     */
    private static ResourceManagementService resourcesService;

    /**
     * Called when this bundle is started.
     *
     * @param bc The execution context of the bundle being started.
     * @throws Exception if the bundle is not correctly started
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        bundleContext.registerService(
            CertificateService.class.getName(),
            new CertificateServiceImpl(),
            null);
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param bc The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext bc) throws Exception
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
        if(configService == null)
        {
            configService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configService;
    }

    /**
     * Returns the <tt>FileAccessService</tt> obtained from the bundle context.
     *
     * @return the <tt>FileAccessService</tt> obtained from the bundle context
     */
    public static FileAccessService getFileAccessService()
    {
        if (fileAccessService == null)
        {
            fileAccessService
                = ServiceUtils.getService(
                        bundleContext,
                        FileAccessService.class);
        }
        return fileAccessService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     *
     * @return the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            resourcesService
                = ServiceUtils.getService(
                        bundleContext,
                        ResourceManagementService.class);
        }
        return resourcesService;
    }
}
