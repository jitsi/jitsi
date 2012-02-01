/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * A listener that would notify for incoming DTMF tones.
 *
 * @author Damian Minkov
 */
public interface DTMFListener
    extends EventListener
{
    /**
     * Called when a new incoming <tt>DTMFTone</tt> has been received.
     * @param evt the <tt>DTMFReceivedEvent</tt> containing the newly
     * received tone.
     */
    public void toneReceived(DTMFReceivedEvent evt);
}
