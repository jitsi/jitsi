/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.securityconfig.masterpassword;

import javax.swing.*;

import net.java.sip.communicator.util.swing.*;

/**
 * Implements a Swing <tt>Component</tt> to represent the user interface of the
 * Passwords <tt>ConfigurationForm</tt>.
 *
 * @author Dmitri Melnikov
 * @author Lubomir Marinov
 */
public class ConfigurationPanel
    extends TransparentPanel
{
    /**
     * Initializes a new <tt>ConfigurationPanel</tt> instance.
     */
    public ConfigurationPanel()
    {
        add(new MasterPasswordPanel());
        add(Box.createVerticalStrut(10));
        add(new SavedPasswordsPanel());
    }
}
