package net.java.sip.communicator.service.httputil;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import org.osgi.framework.*;

/**
 * The http utils bundle activator. Do nothing just provide access to some
 * services.
 *
 * @author Damian Minkov
 */
public class HttpUtilActivator
    implements BundleActivator
{
    /**
     * The service we use to interact with user regarding certificates.
     */
    private static CertificateVerificationService guiCertificateVerification;

    /**
     * Reference to the credentials service
     */
    private static CredentialsStorageService credentialsService;

    /**
     * The bundle context.
     */
    private static BundleContext bundleContext;

    /**
     * The resource service.
     */
    private static ResourceManagementService resourceService;

    /**
     * A reference to the ConfigurationService implementation instance that
     * is currently registered with the bundle context.
     */
    private static ConfigurationService configurationService;

    /**
     * Return the certificate verification service impl.
     * @return the CertificateVerification service.
     */
    public static CertificateVerificationService
        getCertificateVerificationService()
    {
        if(guiCertificateVerification == null)
        {
            ServiceReference guiVerifyReference
                = bundleContext.getServiceReference(
                CertificateVerificationService.class.getName());
            if(guiVerifyReference != null)
                guiCertificateVerification = (CertificateVerificationService)
                    bundleContext.getService(guiVerifyReference);
        }

        return guiCertificateVerification;
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
     * Start the bundle.
     * @param bundleContext
     * @throws Exception
     */
    public void start(BundleContext bundleContext)
        throws
        Exception
    {
        HttpUtilActivator.bundleContext = bundleContext;
    }

    /**
     * Stops the bundle.
     * @param bundleContext
     * @throws Exception
     */
    public void stop(BundleContext bundleContext)
        throws
        Exception
    {
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
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
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
}
