/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.net.*;

/**
 * Basic implementation of ClientConfig.
 * 
 * <p>
 * The implementation of ClientConfig enables advanced options by default.
 * Options can be disabled at will.
 * </p>
 *
 * @author Danny van Heumen
 */
public class ClientConfigImpl
    implements ClientConfig
{
    /**
     * Allow IRC version 3 capabilities.
     */
    private boolean version3Allowed = true;

    /**
     * Contact presence periodic task enable flag.
     */
    private boolean contactPresenceTaskEnabled = true;

    /**
     * Channel presence periodic task enable flag.
     */
    private boolean channelPresenceTaskEnabled = true;

    /**
     * The proxy configuration.
     */
    private Proxy proxy = null;

    /**
     * Resolve addresses through proxy.
     */
    private boolean resolveByProxy = true;

    /**
     * Get version 3 allowed flag.
     *
     * @return returns <tt>true</tt> if allowed, or <tt>false</tt> if not.
     */
    @Override
    public boolean isVersion3Allowed()
    {
        return this.version3Allowed;
    }

    /**
     * Set version 3 allowed.
     *
     * @param allowed version 3 allowed
     */
    public void setVersion3Allowed(final boolean allowed)
    {
        this.version3Allowed = allowed;
    }

    /**
     * Get current value of contact presence enable flag.
     *
     * @return returns <tt>true</tt> if enabled.
     */
    @Override
    public boolean isContactPresenceTaskEnabled()
    {
        return this.contactPresenceTaskEnabled;
    }

    /**
     * Set new value for contact presence enable flag.
     *
     * @param value new value for flag
     */
    public void setContactPresenceTaskEnabled(final boolean value)
    {
        this.contactPresenceTaskEnabled = value;
    }

    /**
     * Get current value of channel presence enable flag.
     *
     * @return returns <tt>true</tt> if enabled.
     */
    @Override
    public boolean isChannelPresenceTaskEnabled()
    {
        return this.channelPresenceTaskEnabled;
    }

    /**
     * Set new value for channel presence enable flag.
     *
     * @param value new value for flag
     */
    public void setChannelPresenceTaskEnabled(final boolean value)
    {
        this.channelPresenceTaskEnabled = value;
    }

    /**
     * Get the proxy to use connecting to IRC server.
     *
     * @return returns proxy configuration or <tt>null</tt> if no proxy should
     *         be used
     */
    @Override
    public Proxy getProxy()
    {
        return this.proxy;
    }

    /**
     * Set a new proxy instance.
     *
     * The proxy may be <tt>null</tt> signaling that a proxy connection is not
     * necessary.
     *
     * @param proxy the new proxy instance
     */
    public void setProxy(final Proxy proxy)
    {
        this.proxy = proxy;
    }

    /**
     * Get resolve by proxy value.
     *
     * @return returns <tt>true</tt> to resolve by proxy, or <tt>false</tt> to
     *         resolve via (local) DNS.
     */
    public boolean isResolveByProxy()
    {
        return this.resolveByProxy;
    }

    /**
     * Set resolve by proxy value. Indicates whether or not to use the proxy to
     * resolve addresses.
     */
    public void setResolveByProxy(final boolean resolveByProxy)
    {
        this.resolveByProxy = resolveByProxy;
    }
}
