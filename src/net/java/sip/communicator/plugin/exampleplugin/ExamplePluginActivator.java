/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.exampleplugin;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

public class ExamplePluginActivator implements BundleActivator
{
    Logger logger = Logger.getLogger(ExamplePluginActivator.class);

    public void start(BundleContext bc) throws Exception
    {
        ExamplePluginMenuItem examplePlugin = new ExamplePluginMenuItem();

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();
        containerFilter.put(
                Container.CONTAINER_ID,
                Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU.getID());

        bc.registerService(  PluginComponent.class.getName(),
                                        examplePlugin,
                                        containerFilter);

        logger.info("CONTACT INFO... [REGISTERED]");
    }

    public void stop(BundleContext bc) throws Exception
    {
    }
}
