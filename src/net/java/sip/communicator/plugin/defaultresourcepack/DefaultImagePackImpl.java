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
public class DefaultImagePackImpl
    implements ImagePack
{

    public String getResourcePackBaseName()
    {
        return "resources.images.images";
    }

    public String getName()
    {
        return "Default Image Resources";
    }

    public String getDescription()
    {
        return "Provide SIP Communicator default Image resource pack.";
    }
}
