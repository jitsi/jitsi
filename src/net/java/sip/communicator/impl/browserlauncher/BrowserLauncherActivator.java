/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.browserlauncher;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The activator for the browserlauncher bundle.
 * @author Yana Stamcheva
 */
public class BrowserLauncherActivator
    implements
    BundleActivator
{
    private static Logger logger
        = Logger.getLogger(BrowserLauncherActivator.class.getName());
    
    private BundleContext bundleContext;
    
    private BrowserLauncherImpl browserLauncher;
    
    public void start(BundleContext bc)
        throws Exception
    {
        bundleContext = bc;

        //Create the browser launcher service
        this.browserLauncher = new BrowserLauncherImpl();

        logger.info("UI Service...[  STARTED ]");

        bundleContext.registerService(BrowserLauncherService.class.getName(),
                this.browserLauncher, null);

        logger.info("UI Service ...[REGISTERED]");
    }

    public void stop(BundleContext arg0) throws Exception
    {}
}
