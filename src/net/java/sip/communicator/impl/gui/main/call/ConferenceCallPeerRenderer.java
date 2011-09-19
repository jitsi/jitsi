/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * The conference peer renderer.
 *
 * @author Yana Stamcheva
 */
public interface ConferenceCallPeerRenderer
    extends CallPeerRenderer
{
    /**
     * Returns the associated sound level listener.
     *
     * @return the associated sound level listener
     */
    public ConferenceMembersSoundLevelListener
        getConferenceMembersSoundLevelListener();

    /**
     * Returns the stream sound level listener.
     *
     * @return the stream sound level listener
     */
    public SoundLevelListener getStreamSoundLevelListener();
}
