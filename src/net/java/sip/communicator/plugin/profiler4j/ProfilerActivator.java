/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    /**
     * OSGi bundle context.
     */
    public static BundleContext bundleContext;

    Logger logger = Logger.getLogger(ProfilerActivator.class);

    private ServiceRegistration menuRegistration = null;

    public void start(BundleContext bc) throws Exception {
        bundleContext = bc;

        Hashtable<String, String> toolsMenuFilter =
            new Hashtable<String, String>();
        toolsMenuFilter.put(Container.CONTAINER_ID,
                Container.CONTAINER_TOOLS_MENU.getID());

        menuRegistration = bc.registerService(
            PluginComponentFactory.class.getName(),
            new PluginComponentFactory(Container.CONTAINER_TOOLS_MENU)
            {
                @Override
                protected PluginComponent getPluginInstance()
                {
                    return new SettingsWindowMenuEntry(
                                    Container.CONTAINER_TOOLS_MENU, this);
                }
            },
            toolsMenuFilter);

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
