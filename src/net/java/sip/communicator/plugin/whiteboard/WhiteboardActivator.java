/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.whiteboard;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Registers the <tt>WhiteboardMenuItem</tt> in the UI Service.
 *
 * @author Julien Waechter
 */
public class WhiteboardActivator implements BundleActivator
{
    private static Logger logger = Logger.getLogger(WhiteboardActivator.class);

    /**
     * OSGi bundle context.
     */
    public static BundleContext bundleContext;

    private WhiteboardSessionManager session;

    private static UIService uiService;

    /**
     * Starts this bundle.
     *
     * @param bc bundle context
     * @throws java.lang.Exception
     */
    public void start (BundleContext bc) throws Exception
    {
        bundleContext = bc;

        session = new WhiteboardSessionManager ();

        WhiteboardMenuItem whiteboardPlugin = new WhiteboardMenuItem (session);

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();
        containerFilter.put(
                Container.CONTAINER_ID,
                Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU.getID());

        bundleContext.registerService(  PluginComponent.class.getName(),
                                        whiteboardPlugin,
                                        containerFilter);

        if (logger.isInfoEnabled())
            logger.info("WHITEBOARD... [REGISTERED]");
    }

    /**
     * Stops this bundle.
     *
     * @param bc bundle context
     * @throws java.lang.Exception
     */
    public void stop (BundleContext bc) throws Exception
    {
    }

    /**
     * Returns the <tt>UIService</tt>, giving access to the main GUI.
     *
     * @return the <tt>UIService</tt>, giving access to the main GUI.
     */
    public static UIService getUiService()
    {
        return uiService;
    }

    /**
     * Returns all <tt>OperationSetWhiteboarding</tt>s obtained from the bundle
     * context.
     * @return all <tt>OperationSetWhiteboarding</tt>s obtained from the bundle
     * context
     */
    public static List<OperationSetWhiteboarding> getWhiteboardOperationSets()
    {
        List<OperationSetWhiteboarding> whiteboardOpSets
            = new ArrayList<OperationSetWhiteboarding>();

        ServiceReference[] serRefs = null;
        try
        {
            //get all registered provider factories
            serRefs = bundleContext.getServiceReferences(
                    ProtocolProviderService.class.getName(), null);

        } catch (InvalidSyntaxException e)
        {
            logger.error("Failed to obtain protocol provider service refs: "
                        + e);
        }

        if (serRefs == null)
            return null;

        for (ServiceReference serRef : serRefs)
        {
            ProtocolProviderService protocolProvider
                = (ProtocolProviderService) bundleContext.getService(serRef);

            OperationSetWhiteboarding opSet
                = protocolProvider
                        .getOperationSet(OperationSetWhiteboarding.class);

            if(opSet != null)
                whiteboardOpSets.add(opSet);
        }

        return whiteboardOpSets;
    }
}
