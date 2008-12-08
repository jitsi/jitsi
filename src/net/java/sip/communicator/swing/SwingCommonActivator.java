/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.swing;

import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.imageio.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * @author Lubomir Marinov
 */
public class SwingCommonActivator
    implements BundleActivator
{
    private static BundleContext bundleContext;

    private static ConfigurationService configurationService;

    private static final Map<String, BufferedImage> imageCache =
        new HashMap<String, BufferedImage>();

    private static KeybindingsService keybindingsService;

    private static final Logger logger =
        Logger.getLogger(SwingCommonActivator.class);

    private static ResourceManagementService resources;

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

    public static BufferedImage getImage(String key)
    {
        if (imageCache.containsKey(key))
            return imageCache.get(key);

        URL url = getResources().getImageURL(key);
        BufferedImage image = null;
        if (url != null)
        {
            try
            {
                image = ImageIO.read(url);

                imageCache.put(key, image);
            }
            catch (IOException ex)
            {
                logger.error("Failed to load image " + key, ex);
            }
        }
        return image;
    }

    public static ResourceManagementService getResources()
    {
        if (resources == null)
            resources =
                ResourceManagementServiceUtils.getService(bundleContext);
        return resources;
    }

    public void start(BundleContext bundleContext)
    {
        SwingCommonActivator.bundleContext = bundleContext;
    }

    public void stop(BundleContext bundleContext)
    {
        if (SwingCommonActivator.bundleContext == bundleContext)
            SwingCommonActivator.bundleContext = null;
    }
}
