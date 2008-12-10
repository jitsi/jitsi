/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media;

import net.java.sip.communicator.service.gui.*;

/**
 * @author Lubomir Marinov
 */
public class MediaConfigurationForm
    extends AbstractConfigurationForm
{
    protected String getFormClassName()
    {
        return "net.java.sip.communicator.impl.media.MediaConfigurationPanel";
    }

    public byte[] getIcon()
    {
        return MediaActivator.getResources().getImageInBytes(
            "plugin.mediaconfig.PLUGIN_ICON");
    }

    public String getTitle()
    {
        return MediaActivator.getResources().getI18NString(
            "impl.media.configform.TITLE");
    }
}
