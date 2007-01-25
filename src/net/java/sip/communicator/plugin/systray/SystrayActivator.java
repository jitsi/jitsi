/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.systray;

import net.java.sip.communicator.plugin.systray.jdic.*;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.util.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Registers the <tt>IcqAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Yana Stamcheva
 */
public class SystrayActivator
    implements BundleActivator
{
    public static BundleContext bundleContext;

    private static Logger logger = Logger.getLogger(
            SystrayActivator.class.getName());
    
    /**
     * Starts this bundle.
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        ServiceReference uiServiceRef = bundleContext
            .getServiceReference(UIService.class.getName());

        UIService uiService
            = (UIService) bundleContext.getService(uiServiceRef);
        
        Systray systray = new Systray(uiService);        
    }

    public void stop(BundleContext bundleContext) throws Exception {
    }
}
