/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * An <tt>OperationSet</tt> that allows us to receive DMF tones through
 * this protocol provider.
 *
 * @author Damian Minkov
 */
public class OperationSetIncomingDTMFSipImpl
    implements OperationSetIncomingDTMF
{
    /**
     * The parent provider.
     */
    private ProtocolProviderServiceSipImpl provider;

    /**
     * The send DTMF operation set holding dtmf implementations.
     */
    private OperationSetDTMFSipImpl opsetDTMFSip;

    /**
     * Creates operation set.
     * @param provider the parent provider
     * @param opsetDTMFSip the dtmf implementation.
     */
    OperationSetIncomingDTMFSipImpl(ProtocolProviderServiceSipImpl provider,
                                    OperationSetDTMFSipImpl opsetDTMFSip)
    {
        this.provider = provider;

        this.opsetDTMFSip = opsetDTMFSip;
    }

    /**
     * Registers the specified DTMFListener with this provider so that it could
     * be notified when incoming DTMF tone is received.
     * @param listener the listener to register with this provider.
     *
     */
    public void addDTMFListener(DTMFListener listener)
    {
        this.opsetDTMFSip.getDtmfModeInfo().addDTMFListener(listener);
    }

    /**
     * Removes the specified listener from the list of DTMF listeners.
     * @param listener the listener to unregister.
     */
    public void removeDTMFListener(DTMFListener listener)
    {
        this.opsetDTMFSip.getDtmfModeInfo().removeDTMFListener(listener);
    }
}
