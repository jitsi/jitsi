/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.defaultresourcepack;

import net.java.sip.communicator.service.resources.*;

/**
 *
 * @author Damian Minkov
 */
public class DefaultSoundPackImpl
    implements SoundPack
{

    public String getResourcePackBaseName()
    {
        return "resources.sounds.sounds";
    }

    public String getName()
    {
        return "Default Sounds Resources";
    }

    public String getDescription()
    {
        return "Provide SIP Communicator default Sounds resource pack.";
    }
}
