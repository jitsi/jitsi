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
    private static BundleContext bundleContext;

    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    public void start(BundleContext bundleContext)
    {
        GuiServiceActivator.bundleContext = bundleContext;
    }

    public void stop(BundleContext bundleContext)
    {
        if (GuiServiceActivator.bundleContext == bundleContext)
            GuiServiceActivator.bundleContext = null;
    }
}
