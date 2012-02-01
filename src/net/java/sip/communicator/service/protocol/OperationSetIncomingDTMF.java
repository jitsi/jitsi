/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * An <tt>OperationSet</tt> that allows us to receive DTMF tones through
 * this protocol provider.
 *
 * @author Damian Minkov
 */
public interface OperationSetIncomingDTMF
    extends OperationSet
{
    /**
     * Registers the specified DTMFListener with this provider so that it could
     * be notified when incoming DTMF tone is received.
     * @param listener the listener to register with this provider.
     *
     */
    public void addDTMFListener(DTMFListener listener);

    /**
     * Removes the specified listener from the list of DTMF listeners.
     * @param listener the listener to unregister.
     */
    public void removeDTMFListener(DTMFListener listener);
}
