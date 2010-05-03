/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.callhistory;

import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.contactsource.*;

/**
 * Activates the <tt>CallHistoryService</tt>.
 *
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public class CallHistoryActivator
    implements BundleActivator
{
    private static Logger logger =
        Logger.getLogger(CallHistoryActivator.class);

    /**
     * The bundle context.
     */
    public static BundleContext bundleContext;

    /**
     * The <tt>CallHistoryServiceImpl</tt> instantiated in the start method
     * of this bundle.
     */
    private static CallHistoryServiceImpl callHistoryService = null;

    /**
     * The service responsible for resources.
     */
    private static ResourceManagementService resourcesService;

    /**
     * The map containing all registered 
     */
    private static final Map<Object, ProtocolProviderFactory>
        providerFactoriesMap = new Hashtable<Object, ProtocolProviderFactory>();

    /**
     * Initialize and start call history
     *
     * @param bc the <tt>BundleContext</tt>
     * @throws Exception
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        try{
            logger.logEntry();

            ServiceReference refHistory = bundleContext.getServiceReference(
                HistoryService.class.getName());

            HistoryService historyService = (HistoryService)
                bundleContext.getService(refHistory);

            //Create and start the call history service.
            callHistoryService =
                new CallHistoryServiceImpl();
            // set the configuration and history service
            callHistoryService.setHistoryService(historyService);

            callHistoryService.start(bundleContext);

            bundleContext.registerService(
                CallHistoryService.class.getName(), callHistoryService, null);

            bundleContext.registerService(
                ContactSourceService.class.getName(),
                new CallHistoryContactSource(), null);

            logger.info("Call History Service ...[REGISTERED]");
        }
        finally
        {
            logger.logExit();
        }

    }

    /**
     * Stops this bundle.
     * @param bundleContext the <tt>BundleContext</tt>
     * @throws Exception if the stop operation goes wrong
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        if(callHistoryService != null)
            callHistoryService.stop(bundleContext);
    }

    /**
     * Returns the instance of <tt>CallHistoryService</tt> created in this
     * activator.
     * @return the instance of <tt>CallHistoryService</tt> created in this
     * activator
     */
    public static CallHistoryService getCallHistoryService()
    {
        return callHistoryService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     *
     * @return the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;

            resourcesService = (ResourceManagementService) bundleContext
                .getService(serviceReference);
        }
        return resourcesService;
    }

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

        if (serRefs != null) 
        {
            for (int i = 0; i < serRefs.length; i++) 
            {

                ProtocolProviderFactory providerFactory
                    = (ProtocolProviderFactory) bundleContext
                        .getService(serRefs[i]);

                providerFactoriesMap.put(serRefs[i]
                        .getProperty(ProtocolProviderFactory.PROTOCOL),
                        providerFactory);
            }
        }
        return providerFactoriesMap;
    }
}
