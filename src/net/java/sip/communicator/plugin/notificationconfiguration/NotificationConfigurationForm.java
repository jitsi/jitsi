/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.notificationconfiguration;

import net.java.sip.communicator.service.gui.*;

/**
 * Implements the <code>ConfigurationForm</code> interface in order to allow
 * integrating the UI of this plug-in into the configuration UI.
 * <p>
 * The interface implementation in question is separated from the very UI
 * implementation in order to allow the <code>ConfigurationForm</code> to be
 * loaded at startup without creating and loading the very UI implementation.
 * </p>
 * 
 * @author Lubomir Marinov
 */
public class NotificationConfigurationForm
    implements ConfigurationForm
{

    /**
     * Implements the <tt>ConfigurationForm.getForm()</tt> method. Returns the
     * component corresponding to this configuration form.
     */
    public Object getForm()
    {
        return new NotificationConfigurationPanel();
    }

    /**
     * Implements the <tt>ConfigurationForm.getIcon()</tt> method. Returns the
     * icon of this configuration form.
     */
    public byte[] getIcon()
    {
        return Resources.getImageInBytes("plugin.notificationconfig.PLUGIN_ICON");
    }

    public int getIndex()
    {
        return -1;
    }
    
    /**
     * Implements the <tt>ConfigurationForm.getTitle()</tt> method. Returns the
     * title of this configuration form.
     */
    public String getTitle()
    {
        return Resources.getString("notifications");
    }
}