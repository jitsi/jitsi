/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui.internal;

import org.osgi.framework.*;

/**
 * @author Lubomir Marinov
 */
public class GuiServiceActivator
    implements BundleActivator
{
    /**
     * The <tt>BundleContext</tt> of the service.
     */
    private static BundleContext bundleContext;

    /**
     * Returns the <tt>BundleContext</tt>.
     *
     * @return bundle context
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Initialize and start GUI service
     *
     * @param bundleContext the <tt>BundleContext</tt>
     */
    public void start(BundleContext bundleContext)
    {
        GuiServiceActivator.bundleContext = bundleContext;
    }

    /**
     * Stops this bundle.
     *
     * @param bundleContext the <tt>BundleContext</tt>
     */
    public void stop(BundleContext bundleContext)
    {
        if (GuiServiceActivator.bundleContext == bundleContext)
            GuiServiceActivator.bundleContext = null;
    }
}
