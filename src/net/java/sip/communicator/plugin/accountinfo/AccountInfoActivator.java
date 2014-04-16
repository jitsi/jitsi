/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.accountinfo;

import java.util.*;

import net.java.sip.communicator.service.globaldisplaydetails.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import org.jitsi.service.resources.*;

import org.osgi.framework.*;

/**
 * Starts the account info bundle.
 *
 * @author Adam Glodstein
 * @author Marin Dzhigarov
 */
public class AccountInfoActivator
    implements BundleActivator
{
    private static final Logger logger =
        Logger.getLogger(AccountInfoActivator.class);

    /**
     * The OSGi bundle context.
     */
    public static BundleContext bundleContext;
    static ResourceManagementService R;

    private static GlobalDisplayDetailsService globalDisplayDetailsService;

    public void start(BundleContext bc) throws Exception
    {
        AccountInfoActivator.bundleContext = bc;
        
        R = ServiceUtils.getService(bc, ResourceManagementService.class);

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();
        containerFilter.put(
            Container.CONTAINER_ID,
            Container.CONTAINER_TOOLS_MENU.getID());

        bundleContext.registerService(
            PluginComponentFactory.class.getName(),
            new PluginComponentFactory(Container.CONTAINER_TOOLS_MENU)
            {
                @Override
                protected PluginComponent getPluginInstance()
                {
                    return new AccountInfoMenuItemComponent(
                        getContainer(), this);
                }
            },
            containerFilter);

        containerFilter = new Hashtable<String, String>();
        containerFilter.put(
            Container.CONTAINER_ID,
            Container.CONTAINER_ACCOUNT_RIGHT_BUTTON_MENU.getID());

        bundleContext.registerService(
            PluginComponentFactory.class.getName(),
            new PluginComponentFactory(
                Container.CONTAINER_ACCOUNT_RIGHT_BUTTON_MENU)
            {
                @Override
                protected PluginComponent getPluginInstance()
                {
                    return new AccountInfoMenuItemComponent(
                        getContainer(), this);
                }
            },
            containerFilter);
    }

    public void stop(BundleContext bc) throws Exception {}

    /**
     * Returns all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     * context.
     *
     * @return all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     *         context
     */
    public static Map<Object, ProtocolProviderFactory>
        getProtocolProviderFactories()
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

    /**
     * Returns the <tt>GlobalDisplayDetailsService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>GlobalDisplayDetailsService</tt> obtained from the bundle
     * context
     */
    public static GlobalDisplayDetailsService getGlobalDisplayDetailsService()
    {
        if (globalDisplayDetailsService == null)
        {
            globalDisplayDetailsService
                = ServiceUtils.getService(
                        bundleContext,
                        GlobalDisplayDetailsService.class);
        }
        return globalDisplayDetailsService;
    }
}
