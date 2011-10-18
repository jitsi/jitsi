/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * Base class for security panels that show encryption specific UI controls.
 * 
 * @author Ingo Bauersachs
 */
public abstract class SecurityPanel
    extends TransparentPanel
    implements Skinnable
{
    /**
     * Creates the security panel depending on the concrete implementation of
     * the passed security controller.
     * 
     * @param srtpControl the security controller that provides the information
     *            to be shown on the UI
     * @return An instance of a {@link SecurityPanel} for the security
     *         controller or an {@link TransparentPanel} if the controller is
     *         unknown or does not have any controls to show.
     */
    public static SecurityPanel create(SrtpControl srtpControl)
    {
        if(srtpControl instanceof ZrtpControl)
            return new ZrtpSecurityPanel((ZrtpControl)srtpControl);

        return new SecurityPanel()
        {
            public void loadSkin()
            {}
            public void refreshStates()
            {}
        };
    }

    /**
     * Forces the panel to update the security information presented to the
     * user.
     */
    public abstract void refreshStates();
}
