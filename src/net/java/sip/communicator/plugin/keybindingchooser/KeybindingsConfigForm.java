/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.keybindingchooser;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.keybindings.*;

/**
 * The <tt>ConfigurationForm</tt> that would be added to the settings
 * configuration to configure the application keybindings.
 * 
 * @author Damian Johnson
 * @author Lubomir Marinov
 */
public class KeybindingsConfigForm
    implements ConfigurationForm
{
    private final KeybindingsService service;

    public KeybindingsConfigForm(KeybindingsService service)
    {
        this.service = service;
    }

    /**
     * Implements the <tt>ConfigurationForm.getTitle()</tt> method. Returns the
     * title of this configuration form.
     */
    public String getTitle()
    {
        return KeybindingChooserActivator.getResources()
            .getI18NString("plugin.keybindings.PLUGIN_NAME");
    }

    /**
     * Implements the <tt>ConfigurationForm.getIcon()</tt> method. Returns the
     * icon of this configuration form.
     */
    public byte[] getIcon()
    {
        return KeybindingChooserActivator.getResources()
            .getImageInBytes("plugin.keybinding.PLUGIN_ICON");
    }

    /**
     * Implements the <tt>ConfigurationForm.getForm()</tt> method. Returns the
     * component corresponding to this configuration form.
     */
    public Object getForm()
    {
        return new KeybindingsConfigPanel(service);
    }

    public int getIndex()
    {
        return -1;
    }
}
