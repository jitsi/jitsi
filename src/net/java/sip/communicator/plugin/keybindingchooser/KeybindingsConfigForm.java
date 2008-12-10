/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.keybindingchooser;

import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>ConfigurationForm</tt> that would be added to the settings
 * configuration to configure the application keybindings.
 * 
 * @author Damian Johnson
 * @author Lubomir Marinov
 */
public class KeybindingsConfigForm
    extends AbstractConfigurationForm
{
    /**
     * Implements the <tt>ConfigurationForm.getTitle()</tt> method. Returns the
     * title of this configuration form.
     */
    public String getTitle()
    {
        return KeybindingChooserActivator.getResources().getI18NString(
            "plugin.keybindings.PLUGIN_NAME");
    }

    /**
     * Implements the <tt>ConfigurationForm.getIcon()</tt> method. Returns the
     * icon of this configuration form.
     */
    public byte[] getIcon()
    {
        return KeybindingChooserActivator.getResources().getImageInBytes(
            "plugin.keybinding.PLUGIN_ICON");
    }

    protected String getFormClassName()
    {
        return "net.java.sip.communicator.plugin.keybindingchooser.KeybindingsConfigPanel";
    }
}
