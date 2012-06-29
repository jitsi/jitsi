/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.callhistory;

import java.util.*;

import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Activates the <tt>CallHistoryService</tt>.
 *
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public class CallHistoryActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallHistoryActivator</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(CallHistoryActivator.class);

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
     * @throws Exception if initializing and starting call history fails
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

            if (logger.isInfoEnabled())
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
            resourcesService
                = ServiceUtils.getService(
                        bundleContext,
                        ResourceManagementService.class);
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
            for (ServiceReference serRef : serRefs) 
            {
                ProtocolProviderFactory providerFactory
                    = (ProtocolProviderFactory)
                        bundleContext.getService(serRef);

                providerFactoriesMap.put(
                        serRef.getProperty(ProtocolProviderFactory.PROTOCOL),
                        providerFactory);
            }
        }
        return providerFactoriesMap;
    }
}
