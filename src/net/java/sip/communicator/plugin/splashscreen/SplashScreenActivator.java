/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.splashscreen;

import net.java.sip.communicator.service.gui.*;

import org.osgi.framework.*;

public class SplashScreenActivator
    implements  BundleActivator,
                ServiceListener,
                BundleListener
{
    private WelcomeWindow welcomeWindow;
    private BundleContext bundleContext;
    
    public void start(BundleContext bundleContext) throws Exception
    {
        this.bundleContext = bundleContext;
        
        welcomeWindow = new WelcomeWindow();
        
        welcomeWindow.pack();
        welcomeWindow.setVisible(true);
        
        this.bundleContext.addServiceListener(this);
        this.bundleContext.addBundleListener(this);
    }

    public void stop(BundleContext arg0) throws Exception
    {
        
    }

    public void serviceChanged(ServiceEvent evt)
    {   
        ServiceReference serviceRef = evt.getServiceReference();
        
        if(serviceRef.getBundle().getState() != Bundle.STARTING)
            return;
        
        if (bundleContext.getServiceReference(UIService.class.getName())
            == serviceRef)
        {
            this.welcomeWindow.close();
        }
    }

    public void bundleChanged(BundleEvent evt)
    {
        if(welcomeWindow != null && welcomeWindow.isShowing())
            welcomeWindow.setBundle(evt.getBundle().getHeaders()
                .get("Bundle-Name").toString());
    }            
}
