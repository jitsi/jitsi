/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.pluginmanager;

import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>ConfigurationForm</tt> that would be added in the user interface
 * configuration window. It contains a list of all installed plug-ins. Allows
 * installing new plugins and managing the existing ones.
 * 
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class PluginManagerConfigForm
    implements ConfigurationForm
{

    /**
     * Implements the <tt>ConfigurationForm.getTitle()</tt> method. Returns the
     * title of this configuration form.
     */
    public String getTitle()
    {
        return Resources.getString("plugin.pluginmanager.PLUGINS");
    }

    /**
     * Implements the <tt>ConfigurationForm.getIcon()</tt> method. Returns the
     * icon of this configuration form.
     */
    public byte[] getIcon()
    {
        return Resources.getResources().getImageInBytes(
            "plugin.pluginmanager.PLUGIN_ICON");
    }

    /**
     * Implements the <tt>ConfigurationForm.getForm()</tt> method. Returns the
     * component corresponding to this configuration form.
     */
    public Object getForm()
    {
        return new PluginManagerPanel();
    }

    public int getIndex()
    {
        return -1;
    }
}
