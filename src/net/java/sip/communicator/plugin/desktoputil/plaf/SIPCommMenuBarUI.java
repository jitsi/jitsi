/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil.plaf;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

/**
 * @author Yana Stamcheva
 */
public class SIPCommMenuBarUI
    extends BasicMenuBarUI
{
    /**
     * Creates a new SIPCommMenuUI instance.
     */
    public static ComponentUI createUI(JComponent x)
    {
        return new SIPCommMenuBarUI();
    }

    @Override
    protected void installDefaults()
    {
        super.installDefaults();

        LookAndFeel.installProperty(menuBar, "opaque", Boolean.FALSE);
    }
}
