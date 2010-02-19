/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.awt.image.*;
import java.net.*;

import javax.imageio.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.service.resources.*;

import org.osgi.framework.*;

/**
 * The only raison d'etre for this Activator is so that it would set a global
 * exception handler. It doesn't export any services and neither it runs any
 * initialization - all it does is call
 * <tt>Thread.setUncaughtExceptionHandler()</tt>
 * 
 * @author Emil Ivov
 */
public class UtilActivator
    implements BundleActivator, 
               Thread.UncaughtExceptionHandler
{
    private static final Logger logger
        = Logger.getLogger(UtilActivator.class);

    private static ConfigurationService configurationService;

    private static KeybindingsService keybindingsService;

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
        logger.trace("Setting default uncaught exception handler.");

        bundleContext = context;

        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * Method invoked when a thread would terminate due to the given uncaught
     * exception. All we do here is simply log the exception using the system
     * logger.
     *
     * <p>Any exception thrown by this method will be ignored by the
     * Java Virtual Machine and thus won't screw our application.
     *
     * @param thread the thread
     * @param exc the exception
     */
    public void uncaughtException(Thread thread, Throwable exc)
    {
        logger.error("An uncaught exception occurred in thread="
                     + thread
                     + " and message was: "
                     + exc.getMessage()
                     , exc);
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

    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            ServiceReference serviceReference =
                bundleContext.getServiceReference(ConfigurationService.class
                    .getName());

            if (serviceReference != null)
                configurationService =
                    (ConfigurationService) bundleContext
                        .getService(serviceReference);
        }
        return configurationService;
    }

    public static KeybindingsService getKeybindingsService()
    {
        if (keybindingsService == null)
        {
            ServiceReference serviceReference =
                bundleContext.getServiceReference(KeybindingsService.class
                    .getName());

            if (serviceReference != null)
                keybindingsService =
                    (KeybindingsService) bundleContext
                        .getService(serviceReference);
        }
        return keybindingsService;
    }

    /**
     * Returns the service giving access to all application resources.
     * 
     * @return the service giving access to all application resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourceService == null)
            resourceService =
                ResourceManagementServiceUtils.getService(bundleContext);

        return resourceService;
    }

    /**
     * Returns the image corresponding to the given <tt>imageID</tt>.
     * 
     * @param imageID the identifier of the image
     * @return the image corresponding to the given <tt>imageID</tt>
     */
    public static BufferedImage getImage(String imageID)
    {
        BufferedImage image = null;

        URL path = getResources().getImageURL(imageID);

        if (path == null)
        {
            return null;
        }

        try
        {
            image = ImageIO.read(path);
        }
        catch (Exception exc)
        {
            logger.error("Failed to load image:" + path, exc);
        }

        return image;
    }
}
