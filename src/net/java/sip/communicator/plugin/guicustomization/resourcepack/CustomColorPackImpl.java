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
public class CustomColorPackImpl
    implements ColorPack
{

    public String getResourcePackBaseName()
    {
        return "resources.colors.colorResources";
    }

    public String getName()
    {
        return "Color Resources";
    }

    public String getDescription()
    {
        return "Provide SIP Communicator Color resource pack.";
    }
}
