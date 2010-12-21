/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import javax.swing.*;

/**
 * A custom JTextField that trims the contents before returns it.
 *
 * @author Damian Minkov
 */
public class TrimTextField
    extends JTextField
{
    /**
     * Returns the trimmed value of the text contained in the field.
     * @return the trimmed value of the field.
     */
    public String getText()
    {
        String txt = super.getText();
        if(txt != null)
            return txt.trim();
        else
            return null;
    }
}
