/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.usersearch;

import net.java.sip.communicator.service.protocol.*;

/**
 * A interface for a listener that will be notified when providers that support
 * user search are added or removed.
 * @author Hristo Terezov
 */
public interface UserSearchSupportedProviderListener
{
    /**
     * Handles provider addition.
     *
     * @param provider the provider that was added.
     */
    public void providerAdded(ProtocolProviderService provider);

    /**
     * Handles provider removal.
     *
     * @param provider the provider that was removed.
     */
    public void providerRemoved(ProtocolProviderService provider);
}
