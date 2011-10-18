/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.credentialsstorage;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activator for the {@link CredentialsStorageService}.
 *
 * @author Dmitri Melnikov
 */
public class CredentialsStorageActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>CredentialsStorageActivator</tt>
     * class and its instances.
     */
    private static final Logger logger
        = Logger.getLogger(CredentialsStorageActivator.class);

    /**
     * The {@link CredentialsStorageService} implementation.
     */
    private CredentialsStorageServiceImpl impl;

    /**
     * The {@link BundleContext}.
     */
    private static BundleContext bundleContext;

    /**
     * Starts the credentials storage service
     * 
     * @param bundleContext the <tt>BundleContext</tt> as provided from the OSGi
     * framework
     * @throws Exception if anything goes wrong
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Service Impl: " + getClass().getName() + " [  STARTED ]");
        }
        
        CredentialsStorageActivator.bundleContext = bundleContext;

        impl = new CredentialsStorageServiceImpl();
        impl.start(bundleContext);

        bundleContext.registerService(
            CredentialsStorageService.class.getName(), impl, null);

        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Service Impl: " + getClass().getName() + " [REGISTERED]");
        }
    }

    /**
     * Unregisters the credentials storage service.
     * 
     * @param bundleContext BundleContext
     * @throws Exception if anything goes wrong
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        logger.logEntry();
        impl.stop();
        logger
            .info("The CredentialsStorageService stop method has been called.");
    }

    /**
     * Returns the service corresponding to the <tt>ServiceReference</tt>.
     *
     * @param serviceReference service reference
     * @return service
     */
    public static Object getService(ServiceReference serviceReference)
    {
        return bundleContext.getService(serviceReference);
    }
}
