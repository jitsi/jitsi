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
package net.java.sip.communicator.plugin.exampleplugin;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>ExamplePluginActivator</tt> is the entering point for the example
 * plugin bundle.
 *
 * @author Yana Stamcheva
 */
public class ExamplePluginActivator
    implements BundleActivator
{
    Logger logger = Logger.getLogger(ExamplePluginActivator.class);

    /**
     * Called when this bundle is started so the Framework can perform the
     * bundle-specific activities necessary to start this bundle. In the case
     * of our example plug-in we create our menu item and register it as a
     * plug-in component in the right button menu of the contact list.
     */
    public void start(BundleContext bc)
        throws Exception
    {
        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();
        containerFilter.put(
                Container.CONTAINER_ID,
                Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU.getID());

        bc.registerService(
            PluginComponentFactory.class.getName(),
            new PluginComponentFactory(
                    Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU)
            {
                @Override
                protected PluginComponent getPluginInstance()
                {
                    return new ExamplePluginMenuItem(this);
                }
            },
            containerFilter);

        if (logger.isInfoEnabled())
            logger.info("CONTACT INFO... [REGISTERED]");
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle. In the case
     * of our example plug-in we have nothing to do here.
     */
    public void stop(BundleContext bc)
        throws Exception
    {
    }
}
