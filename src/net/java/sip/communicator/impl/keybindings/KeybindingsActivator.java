/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.keybindings;

import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Enabling and disabling osgi functionality for keybindings.
 * 
 * @author Damian Johnson
 */
public class KeybindingsActivator
    implements BundleActivator
{
    private static final Logger logger =
        Logger.getLogger(KeybindingsActivator.class);

    private KeybindingsServiceImpl keybindingsService = null;

    /**
     * Called when this bundle is started.
     * 
     * @param context The execution context of the bundle being started.
     */
    public void start(BundleContext context)
    {
        if (this.keybindingsService == null)
        {
            if (logger.isDebugEnabled())
                logger.debug("Service Impl: " + getClass().getName()
                + " [  STARTED ]");
            this.keybindingsService = new KeybindingsServiceImpl();
            this.keybindingsService.start(context);
            context.registerService(KeybindingsService.class.getName(),
                this.keybindingsService, null);
        }
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     * 
     * @param context The execution context of the bundle being stopped.
     */
    public void stop(BundleContext context)
    {
        if (this.keybindingsService != null)
        {
            this.keybindingsService.stop();
            this.keybindingsService = null;
        }
    }
}
