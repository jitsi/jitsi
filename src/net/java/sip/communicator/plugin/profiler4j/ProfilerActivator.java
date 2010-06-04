/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.profiler4j;

import java.util.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import org.osgi.framework.*;

/**
 * Activates the profiler plug-in.
 *
 * @author Vladimir Skarupelov
 */
public class ProfilerActivator implements BundleActivator {

    public static BundleContext bundleContext;

    Logger logger = Logger.getLogger(ProfilerActivator.class);

    private ServiceRegistration menuRegistration = null;

    public void start(BundleContext bc) throws Exception {
        bundleContext = bc;

        SettingsWindowMenuEntry menuEntry = new SettingsWindowMenuEntry(
                Container.CONTAINER_TOOLS_MENU);

        Hashtable<String, String> toolsMenuFilter =
            new Hashtable<String, String>();
        toolsMenuFilter.put(Container.CONTAINER_ID,
                Container.CONTAINER_TOOLS_MENU.getID());

        menuRegistration = bc.registerService(PluginComponent.class
                .getName(), menuEntry, toolsMenuFilter);

        if (logger.isInfoEnabled())
            logger.info("PROFILER4J [REGISTERED]");

    }

    public void stop(BundleContext bc) throws Exception {
        if (menuRegistration != null)
        {
            menuRegistration.unregister();
            if (logger.isInfoEnabled())
                logger.info("PROFILER4J [UNREGISTERED]");
        }
    }

}
