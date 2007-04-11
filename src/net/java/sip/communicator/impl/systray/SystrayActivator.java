/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.systray;

import net.java.sip.communicator.impl.systray.jdic.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Registers the <tt>Systray</tt> in the UI Service.
 *
 * @author Nicolas Chamouard
 */
public class SystrayActivator
    implements BundleActivator
{ 
    /**
     * A currently valid bundle context.
     */
    public static BundleContext bundleContext;

    public static UIService uiService;
    
    private static Logger logger = Logger.getLogger(
            SystrayActivator.class.getName());
    
    /**
     * Called when this bundle is started.
     *
     * @param bc The execution context of the bundle being started.
     * @throws Exception If
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        ServiceReference uiServiceRef = bundleContext
            .getServiceReference(UIService.class.getName());

        uiService = (UIService) bundleContext.getService(uiServiceRef);
        
        SystrayServiceJdicImpl systray = new SystrayServiceJdicImpl(uiService);               
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param bc The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext bc) throws Exception {
    }
}
