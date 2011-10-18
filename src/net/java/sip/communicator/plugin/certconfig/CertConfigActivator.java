/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.certconfig;

import java.util.*;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.credentialsstorage.CredentialsStorageService;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * OSGi Activator for the Certificate Configuration Advanced Form.
 * 
 * @author Ingo Bauersachs
 */
public class CertConfigActivator
    implements BundleActivator
{
    private static BundleContext bundleContext;
    static ResourceManagementService R;

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(ConfigurationForm.FORM_TYPE,
            ConfigurationForm.ADVANCED_TYPE);

        R = ServiceUtils.getService(bc, ResourceManagementService.class);

        bc.registerService(ConfigurationForm.class.getName(),
            new LazyConfigurationForm(
                CertConfigPanel.class.getName(),
                getClass().getClassLoader(),
                null,
                "plugin.certconfig.TITLE",
                2000,
                true),
            properties
        );
    }

    public void stop(BundleContext arg0) throws Exception
    {
    }

    static BundleContext getBundleContext()
    {
        return bundleContext;
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
        return ServiceUtils.getService(bundleContext,
            ConfigurationService.class);
    }

    /**
     * Returns a reference to a CertificateService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     * 
     * @return a currently valid implementation of the CertificateService.
     */
    public static CertificateService getCertService()
    {
        return ServiceUtils.getService(bundleContext, CertificateService.class);
    }

    /**
     * Returns a reference to a UIService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     * 
     * @return a currently valid implementation of the UIService.
     */
    public static UIService getUIService()
    {
        return ServiceUtils.getService(bundleContext, UIService.class);
    }

    /**
     * Returns a reference to a CredentialsStorageService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     * 
     * @return a currently valid implementation of the
     *         CredentialsStorageService.
     */
    public static CredentialsStorageService getCredService()
    {
        return ServiceUtils.getService(bundleContext,
            CredentialsStorageService.class);
    }
}
