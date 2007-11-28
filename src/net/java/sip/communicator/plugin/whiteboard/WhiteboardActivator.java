/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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

        ServiceReference uiServiceRef
            = bc.getServiceReference (UIService.class.getName ());

        uiService = (UIService) bc.getService (uiServiceRef);

        session = new WhiteboardSessionManager ();

        if(uiService.isContainerSupported (
            UIService.CONTAINER_CONTACT_RIGHT_BUTTON_MENU))
        {
            WhiteboardMenuItem whiteboardPlugin =
                new WhiteboardMenuItem (session);

            uiService.addComponent (
                UIService.CONTAINER_CONTACT_RIGHT_BUTTON_MENU,
                whiteboardPlugin);
        }
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
    public static List getWhiteboardOperationSets()
    {
        List whiteboardOpSets = new ArrayList();

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

        for (int i = 0; i < serRefs.length; i++)
        {
            ProtocolProviderService protocolProvider
                = (ProtocolProviderService) bundleContext
                    .getService(serRefs[i]);

            OperationSet opSet
                = protocolProvider.getOperationSet(
                        OperationSetWhiteboarding.class);

            if(opSet != null)
                whiteboardOpSets.add(opSet);
        }

        return whiteboardOpSets;
    }
}