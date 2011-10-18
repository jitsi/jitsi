/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
        ExamplePluginMenuItem examplePlugin = new ExamplePluginMenuItem();

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();
        containerFilter.put(
                Container.CONTAINER_ID,
                Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU.getID());

        bc.registerService(  PluginComponent.class.getName(),
                                        examplePlugin,
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
