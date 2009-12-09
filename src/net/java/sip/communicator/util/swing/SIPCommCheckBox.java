/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import javax.swing.*;

import net.java.sip.communicator.util.*;

/**
 * @author Lubomir Marinov
 */
public class SIPCommCheckBox
    extends JCheckBox
{
    private static final long serialVersionUID = 0L;

    private static final boolean setContentAreaFilled = OSUtils.isWindows();

    public SIPCommCheckBox()
    {
        init();
    }

    public SIPCommCheckBox(String text)
    {
        super(text);

        init();
    }

    public SIPCommCheckBox(String text, boolean selected)
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
