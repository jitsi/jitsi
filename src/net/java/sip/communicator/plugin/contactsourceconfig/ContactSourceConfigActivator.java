/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.contactsourceconfig;

import java.util.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * @author Yana Stamcheva
 */
public class ContactSourceConfigActivator
    implements BundleActivator
{
    /**
     * The logger.
     */
    private static Logger logger
        = Logger.getLogger(ContactSourceConfigActivator.class);

    /**
     * The {@link BundleContext} of the {@link SecurityConfigActivator}.
     */
    public static BundleContext bundleContext;

    /**
     * The {@link ResourceManagementService} of the
     * {@link SecurityConfigActivator}. Can also be obtained from the
     * {@link SecurityConfigActivator#bundleContext} on demand, but we add it
     * here for convenience.
     */
    private static ResourceManagementService resources;

    /**
     * The <tt>ConfigurationService</tt> registered in {@link #bundleContext}
     * and used by the <tt>SecurityConfigActivator</tt> instance to read and
     * write configuration properties.
     */
    private static ConfigurationService configurationService;

    /**
     * The <tt>UIService</tt> registered in {@link #bundleContext}.
     */
    private static UIService uiService;

    /**
     * Starts this plugin.
     * @param bc the BundleContext
     * @throws Exception if some of the operations executed in the start method
     * fails
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        Dictionary<String, String> properties = new Hashtable<String, String>();

        // Registers the contact source panel as advanced configuration form.
        properties.put( ConfigurationForm.FORM_TYPE,
                        ConfigurationForm.ADVANCED_TYPE);

        bundleContext.registerService(
            ConfigurationForm.class.getName(),
            new LazyConfigurationForm(
                ContactSourceConfigForm.class.getName(),
                getClass().getClassLoader(),
                null,
                "plugin.contactsourceconfig.CONTACT_SOURCE_TITLE",
                101, true),
                properties);
    }

    /**
     * Invoked when this bundle is stopped.
     * @param bc the BundleContext
     * @throws Exception if some of the operations executed in the start method
     * fails
     */
    public void stop(BundleContext bc) throws Exception {}

    /**
     * Returns a reference to the ResourceManagementService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the ResourceManagementService
     */
    public static ResourceManagementService getResources()
    {
        if (resources == null)
        {
            resources
                = ResourceManagementServiceUtils.getService(bundleContext);
        }
        return resources;
    }

    /**
     * Returns a reference to the ConfigurationService implementation currently
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

    /**
     * Gets the <tt>UIService</tt> instance registered in the
     * <tt>BundleContext</tt> of the <tt>SecurityConfigActivator</tt>.
     *
     * @return the <tt>UIService</tt> instance registered in the
     * <tt>BundleContext</tt> of the <tt>SecurityConfigActivator</tt>
     */
    public static UIService getUIService()
    {
        if (uiService == null)
            uiService = ServiceUtils.getService(bundleContext, UIService.class);
        return uiService;
    }
}
