/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.caps;

/**
 * A listener use to notify interested parties about a change in our version
 * string.
 *
 * This work is based on Jonas Adahl's smack fork.
 */
public interface CapsVerListener
{
    /**
     * Called whenever our <tt>ver</tt> string changes and we need to regenerate
     * our presence information.
     *
     * @param capsVer the new version value.
     */
    public void capsVerUpdated(String capsVer);
}
