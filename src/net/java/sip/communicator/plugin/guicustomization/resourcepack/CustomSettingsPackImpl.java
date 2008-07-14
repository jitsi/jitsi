/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.guicustomization.resourcepack;

import net.java.sip.communicator.service.resources.*;

/**
 *
 * @author Damian Minkov
 */
public class CustomSettingsPackImpl
    implements SettingsPack
{

    public String getResourcePackBaseName()
    {
        return "resources.config.defaults";
    }

    public String getName()
    {
        return "Settings Resources";
    }

    public String getDescription()
    {
        return "Provide SIP Communicator Settings resource pack.";
    }
}
