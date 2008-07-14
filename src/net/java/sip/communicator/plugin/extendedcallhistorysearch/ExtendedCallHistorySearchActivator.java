/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.extendedcallhistorysearch;

import java.util.*;

import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Call History Search PlugIn Activator
 * 
 * @author Bourdon Maxime & Meyer Thomas
 */
public class ExtendedCallHistorySearchActivator
    implements BundleActivator
{
    private Logger logger
        = Logger.getLogger(ExtendedCallHistorySearchActivator.class);

    static BundleContext context;

    public void start(BundleContext bc) throws Exception
    {
        context = bc;

        ExtendedCallHistorySearchItem extendedSearch
            = new ExtendedCallHistorySearchItem();

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();
        containerFilter.put(
                Container.CONTAINER_ID,
                Container.CONTAINER_TOOLS_MENU.getID());

        context.registerService(  PluginComponent.class.getName(),
                                  extendedSearch,
                                  containerFilter);

        logger.info("EXTENDED CALL HISTORY SEARCH... [REGISTERED]");
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
