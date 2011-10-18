/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.accountinfo;

import java.util.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Starts the account info bundle.
 * 
 * @author Adam Glodstein
 */
public class AccountInfoActivator
    implements BundleActivator
{
    private static final Logger logger =
        Logger.getLogger(AccountInfoActivator.class);

    public static BundleContext bundleContext;

    private static BrowserLauncherService browserLauncherService;

    public void start(BundleContext bc) throws Exception
    {
        AccountInfoActivator.bundleContext = bc;

//        new LazyConfigurationForm(
//            "net.java.sip.communicator.plugin.accountinfo.AccountInfoPanel",
//            getClass().getClassLoader(), "plugin.accountinfo.PLUGIN_ICON",
//            "plugin.accountinfo.TITLE");
    }

    public void stop(BundleContext bc) throws Exception
    {
    }

    /**
     * Returns all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     * context.
     * 
     * @return all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     *         context
     */
    public static Map<Object, ProtocolProviderFactory> getProtocolProviderFactories()
    {
        Map<Object, ProtocolProviderFactory> providerFactoriesMap =
            new Hashtable<Object, ProtocolProviderFactory>();

        ServiceReference[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs =
                bundleContext.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), null);

        }
        catch (InvalidSyntaxException e)
        {
            logger.error("LoginManager : " + e);
        }

        for (int i = 0; i < serRefs.length; i++)
        {

            ProtocolProviderFactory providerFactory =
                (ProtocolProviderFactory) bundleContext.getService(serRefs[i]);

            providerFactoriesMap
                .put(serRefs[i].getProperty(ProtocolProviderFactory.PROTOCOL),
                    providerFactory);
        }

        return providerFactoriesMap;
    }

    public static BrowserLauncherService getBrowserLauncher()
    {
        if (browserLauncherService == null)
        {
            ServiceReference serviceReference =
                bundleContext.getServiceReference(BrowserLauncherService.class
                    .getName());

            browserLauncherService =
                (BrowserLauncherService) bundleContext
                    .getService(serviceReference);
        }

        return browserLauncherService;
    }
}
