/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

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
     * Contact presence periodic task enable flag.
     */
    private boolean contactPresenceTaskEnabled = true;

    /**
     * Channel presence periodic task enable flag.
     */
    private boolean channelPresenceTaskEnabled = true;

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
}
