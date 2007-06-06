/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.extendedcallhistorysearch;

import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.gui.*;

import org.osgi.framework.*;

/**
 * Call History Search PlugIn Activator
 * 
 * @author Bourdon Maxime & Meyer Thomas
 */
public class ExtendedCallHistorySearchActivator
    implements BundleActivator
{
    private static BundleContext context;

    public void start(BundleContext bc) throws Exception
    {
        context = bc;
        ServiceReference uiServiceRef = bc.getServiceReference(
            UIService.class.getName());

        UIService uiService = (UIService) bc.getService(uiServiceRef);

        if (uiService.isContainerSupported(UIService.CONTAINER_TOOLS_MENU))
        {
            ExtendedCallHistorySearchItem extendedSearch
                = new ExtendedCallHistorySearchItem();

            uiService.addComponent(UIService.CONTAINER_TOOLS_MENU,
                extendedSearch);
        }
    }

    public void stop(BundleContext bc) throws Exception
    {
    }

    /**
     * Returns an instance of the <tt>CallHistoryService</tt>.
     */
    public static CallHistoryService getCallHistoryService()
    {
        ServiceReference callHistoryServiceRef = context
            .getServiceReference(CallHistoryService.class.getName());

        CallHistoryService callHistoryService = (CallHistoryService) context
            .getService(callHistoryServiceRef);

        return callHistoryService;
    }

}
