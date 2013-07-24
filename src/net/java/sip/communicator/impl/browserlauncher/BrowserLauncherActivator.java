/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.browserlauncher;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.util.*;

/**
 * Implements <tt>BundleActivator</tt> for the browserlauncher bundle.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Pawel Domas
 */
public class BrowserLauncherActivator
    extends SimpleServiceActivator<BrowserLauncherImpl>
{

    /**
     * Creates new instance of <tt>BrowserLauncherActivator</tt>.
     */
    public BrowserLauncherActivator()
    {
        super(BrowserLauncherService.class, "Browser Launcher Service");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BrowserLauncherImpl createServiceImpl()
    {
        return new BrowserLauncherImpl();
    }
}
