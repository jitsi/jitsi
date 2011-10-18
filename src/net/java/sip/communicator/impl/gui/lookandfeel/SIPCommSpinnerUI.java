/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

public class SIPCommSpinnerUI
    extends BasicSpinnerUI
{
    public static ComponentUI createUI(JComponent c) {
        return new SIPCommSpinnerUI();
    }

    protected Component createNextButton()
    {
        Component c = createArrowButton(SwingConstants.NORTH);
            installNextButtonListeners(c);
            return c;
    }

    protected Component createPreviousButton()
    {
        Component c = createArrowButton(SwingConstants.SOUTH);
            installPreviousButtonListeners(c);
            return c;
    }

    private Component createArrowButton(int direction)
    {
        JButton b = new BasicArrowButton(direction);

        return b;
    }
}
