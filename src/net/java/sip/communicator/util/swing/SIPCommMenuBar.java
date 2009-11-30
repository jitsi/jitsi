/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import javax.swing.*;

import net.java.sip.communicator.util.swing.plaf.*;
/**
 * The SIPCommMenuBar is a <tt>JMenuBar</tt> without border decoration that can
 * be used as a container for other components, like selector boxes that won't
 * need a menu decoration.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommMenuBar
    extends JMenuBar
{
    private boolean isRollover;

    public SIPCommMenuBar()
    {
        this.setBorder(BorderFactory.createEmptyBorder());
        this.setUI(new SIPCommMenuBarUI());
    }
}
