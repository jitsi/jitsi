/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.zrtpconfigure;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.ResourceManagementService;
import net.java.sip.communicator.service.resources.ResourceManagementServiceUtils;

import org.osgi.framework.*;

/**
 * The <tt>BundleActivator</tt> for ZrtpConfigure.
 *
 * @author Werner Dittmann
 */
public class ZrtpConfigureActivator
    implements BundleActivator
{
    public static BundleContext bundleContext;

    /**
     * The {@link ResourceManagementService} of the {@link ZrtpConfigureActivator}.
     */
    public static ResourceManagementService resourceService;

    /**
     * The {@link UIService} of the {@link ZrtpConfigureActivator}. 
     */
    private static UIService uiService;

    /**
     * The {@link ConfigurationService} of the {@link ZrtpConfigureActivator}.
     */
    protected static ConfigurationService configService;

    /**
     * Starts this bundle and adds the <td>ZrtpConfigurePanel</tt>.
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        bundleContext
            .registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                    "net.java.sip.communicator.plugin.zrtpconfigure.ZrtpConfigurePanel",
                    getClass().getClassLoader(),
                    "impl.media.security.zrtp.CONF_ICON",
                    "impl.media.security.zrtp.TITLE",
                    1100),
                null);

        resourceService =
            ResourceManagementServiceUtils
                .getService(bundleContext);
        if (resourceService == null)
            return;

        ServiceReference refConfigService =
            bundleContext
                .getServiceReference(ConfigurationService.class.getName());

        if (refConfigService == null)
            return;

        configService =
            (ConfigurationService)bundleContext
                .getService(refConfigService);

    }

    /**
     * Stops this bundles.
     */
    public void stop(BundleContext arg0) throws Exception
    {
        resourceService = null;
        configService = null;
        uiService = null;
    }

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle context.
     *
     * @return the <tt>UIService</tt> obtained from the bundle context
     */
    public static UIService getUIService()
    {
        if (uiService == null)
        {
            ServiceReference uiReference =
                bundleContext.getServiceReference(UIService.class.getName());

            uiService =
                (UIService) bundleContext
                    .getService(uiReference);
        }

        return uiService;
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     *         context
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configService == null)
        {
            ServiceReference configReference =
                bundleContext.getServiceReference(ConfigurationService.class
                    .getName());

            configService =
                (ConfigurationService) bundleContext
                    .getService(configReference);
        }

        return configService;
    }

    /**
     * Determines whether <tt>bundle</tt> is system or not. We consider system
     * bundles those that we have explicitly marked as such with the
     * <tt>System-Bundle</tt> manifest property or those that belong to the
     * Apache framework itself.
     *
     * @param bundle the bundle that we need to determine as system or not.
     * @return true if <tt>bundle</tt> is a system bundle and <tt>false</tt>
     * otherwise.
     */
    public static boolean isSystemBundle(Bundle bundle)
    {
        if (bundle.getBundleId() <= 1)
        {
            //this is one of the felix bundles
            return true;
        }

        Object sysBundleProp = bundle.getHeaders().get("System-Bundle");

        //ignore if this is a system bundle
        return (sysBundleProp != null && sysBundleProp.equals("yes"));
    }
}
