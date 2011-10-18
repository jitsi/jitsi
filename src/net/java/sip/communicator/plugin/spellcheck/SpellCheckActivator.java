/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.spellcheck;

import java.util.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.service.gui.*;

import org.osgi.framework.*;

/**
 * Enabling and disabling osgi functionality for the spell checker.
 * 
 * @author Damian Johnson
 */
public class SpellCheckActivator
    implements BundleActivator
{
    static BundleContext bundleContext;

    private SpellChecker checker = new SpellChecker();

    private static UIService uiService;

    private static FileAccessService faService;

    private static ConfigurationService configService;

    /**
     * Called when this bundle is started.
     * 
     * @param context The execution context of the bundle being started.
     */
    public void start(BundleContext context) throws Exception
    {
        bundleContext = context;

        this.checker.start(context);

        // adds button to toggle spell checker
        Hashtable<String, String> containerFilter =
            new Hashtable<String, String>();
        containerFilter.put(Container.CONTAINER_ID,
            Container.CONTAINER_CHAT_TOOL_BAR.getID());

        // adds field to change language
        context.registerService(PluginComponent.class.getName(),
            LanguageMenuBar.makeSelectionField(this.checker),
            containerFilter);
    }

    /**
     * Returns the <tt>UIService</tt>.
     * 
     * @return the <tt>UIService</tt>
     */
    public static UIService getUIService()
    {
        if (uiService != null)
            return uiService;

        // retrieves needed services
        ServiceReference uiServiceRef =
            bundleContext.getServiceReference(UIService.class.getName());

        uiService = (UIService) bundleContext.getService(uiServiceRef);

        return uiService;
    }

    /**
     * Returns the <tt>FileAccessService</tt>.
     * 
     * @return the <tt>FileAccessService</tt>
     */
    public static FileAccessService getFileAccessService()
    {
        if (faService != null)
            return faService;

        ServiceReference faServiceReference =
            bundleContext
                .getServiceReference(FileAccessService.class.getName());
        faService =
            (FileAccessService) bundleContext.getService(faServiceReference);

        return faService;
    }

    /**
     * Returns the <tt>ConfigurationService</tt>.
     * 
     * @return the <tt>ConfigurationService</tt>
     */
    public static ConfigurationService getConfigService()
    {
        if (configService != null)
            return configService;

        ServiceReference configServiceRef =
            bundleContext.getServiceReference(ConfigurationService.class
                .getName());

        configService =
            (ConfigurationService) bundleContext.getService(configServiceRef);

        return configService;
    }

    /**
     * Stops this bundles.
     */
    public void stop(BundleContext arg0) throws Exception
    {
        this.checker.stop();
    }
}