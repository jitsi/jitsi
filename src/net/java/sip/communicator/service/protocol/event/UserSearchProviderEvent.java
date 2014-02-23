/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.EventObject;

import net.java.sip.communicator.service.protocol.*;

/**
 * Notifies <tt>UserSearchProviderListener</tt> that a provider that supports
 * user search is added or removed.
 * @author Hristo Terezov
 */
public class UserSearchProviderEvent
    extends EventObject
{
    /**
     * The serial ID.
     */
    private static final long serialVersionUID = -1285649707213476360L;

    /**
     * A type that indicates that the provider is added.
     */
    public static int PROVIDER_ADDED = 0;

    /**
     * A type that indicates that the provider is removed.
     */
    public static int PROVIDER_REMOVED = 1;

    /**
     * The type of the event.
     */
    private final int type;

    /**
     * Constructs new <tt>UserSearchProviderEvent</tt> event.
     * @param provider the provider.
     * @param type the type of the event.
     */
    public UserSearchProviderEvent(ProtocolProviderService provider, int type)
    {
        super(provider);
        this.type = type;
    }

    /**
     * Returns the provider associated with the event.
     * @return the provider associated with the event.
     */
    public ProtocolProviderService getProvider()
    {
        return (ProtocolProviderService) getSource();
    }

    /**
     * Returns the type of the event.
     * @return the type of the event.
     */
    public int getType()
    {
        return type;
    }

}
