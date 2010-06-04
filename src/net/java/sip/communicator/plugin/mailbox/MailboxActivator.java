/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.mailbox;

import org.osgi.framework.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * Activates the Mailbox plug-in.
 *
 * @author Ryan Ricard
 */
public class MailboxActivator
    implements BundleActivator
{
    private static final Logger logger =
        Logger.getLogger(BundleActivator.class);

    /**
     * A reference to the currently valid mailbox plug-in instance.
     */
    private Mailbox mailbox = null;

    /**
     * A reference to the currently valid OSGi bundle context.
     */
    private static BundleContext bundleContext = null;

    /**
     * A reference to the ConfigurationService implementation instance that
     * is currently registered with the bundle context.
     */
    private static ConfigurationService configurationService = null;

    /**
     * A reference to the MediaService implementation instance that is
     * currently registered with the bundle context.
     */
    private static MediaService mediaService = null;

    /**
     * A reference to the <tt>FileAccessService</tt> implementation instance
     * that is currently registered with the bundle context.
     */
    private static FileAccessService fileAccessService = null;
    
    private static ResourceManagementService resourcesService;

    /**
     * Starts this bundle and adds the <tt>MailboxConfigurationForm</tt>
     * contained in it to the configuration window obtained from the
     * <tt>UIService</tt>.
     *
     * @param bundleContext a reference to the currently valid bundle context.
     *
     * @throws Exception if anything goes wrong while starting this plugin.
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        if (logger.isInfoEnabled())
            logger.info("Mailbox plug-in...[STARTING]");
        MailboxActivator.bundleContext = bundleContext;

        //Create and start the Mailbox Service
        mailbox = new Mailbox();
        mailbox.start(bundleContext);

        bundleContext
            .registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                    "net.java.sip.communicator.plugin.mailbox.MailboxConfigurationPanel",
                    getClass().getClassLoader(),
                    "plugin.mailbox.PLUGIN_ICON",
                    "plugin.mailbox.MAILBOX",
                    500),
                null);

        if (logger.isInfoEnabled())
            logger.info("Mailbox plug-in...[STARTED]");
    }

    /**
     * Stops the mailbox plug-in.
     *
     * @param bundleContext a reference to the currently valid bundle context.
     *
     * @throws java.lang.Exception if anything goes wrong while stopping the
     * mailbox plug-in.
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        if(mailbox != null)
            mailbox.stop(bundleContext);
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
            ServiceReference confReference
                = bundleContext.getServiceReference(
                    ConfigurationService.class.getName());
            configurationService
                = (ConfigurationService) bundleContext
                                        .getService(confReference);
        }
        return configurationService;
    }

    /**
     * Returns a reference to a FileAccessService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the
     * <tt>FileAccessService</tt>.
     */
    public static FileAccessService getFileAccessService()
    {
        if (fileAccessService == null && bundleContext != null)
        {
            ServiceReference faReference
                = bundleContext.getServiceReference(
                    FileAccessService.class.getName());

            fileAccessService = (FileAccessService)bundleContext
                .getService(faReference);
        }

        return fileAccessService;
    }

    /**
     * Returns a reference to a MediaService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     *
     * @return a reference to a MediaService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     */
    public static MediaService getMediaService()
    {
        if(mediaService == null)
        {
            ServiceReference mediaServiceReference
                = bundleContext.getServiceReference(
                    MediaService.class.getName());
            mediaService = (MediaService)bundleContext
                .getService(mediaServiceReference);
        }
        return mediaService;
    }
        
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService =
                ResourceManagementServiceUtils.getService(bundleContext);
        return resourcesService;
    }
}
