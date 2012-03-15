/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import javax.swing.*;

import net.java.sip.communicator.util.*;

/**
 * @author Ingo Bauersachs
 */
public class SIPCommRadioButton
    extends JRadioButton
{
    private static final long serialVersionUID = 0L;

    private static final boolean setContentAreaFilled = (OSUtils.IS_WINDOWS
            || OSUtils.IS_LINUX);

    public SIPCommRadioButton()
    {
        init();
    }

    public SIPCommRadioButton(String text)
    {
        super(text);

        init();
    }

    public SIPCommRadioButton(String text, boolean selected)
    {
        super(text, selected);

        init();
    }

    private void init()
    {
        if (setContentAreaFilled)
            setContentAreaFilled(false);
    }
}
