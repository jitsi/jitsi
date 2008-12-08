/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import javax.swing.*;

/**
 * @author Lubomir Marinov
 */
public class SIPCommCheckBox
    extends JCheckBox
{
    private static final boolean setContentAreaFilled = isWindows();

    private static boolean isWindows()
    {
        String osName = System.getProperty("os.name");
        return (osName != null) && (osName.indexOf("Windows") != -1);
    }

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
