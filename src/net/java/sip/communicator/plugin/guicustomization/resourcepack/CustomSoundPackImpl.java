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
public class CustomSoundPackImpl
    implements SoundPack
{

    public String getResourcePackBaseName()
    {
        return "resources.sounds";
    }

    public String getName()
    {
        return "Sounds Resources";
    }

    public String getDescription()
    {
        return "Provide SIP Communicator Sounds resource pack.";
    }
}
