/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import java.util.*;

/**
 * Implements <tt>OperationSetIncomingDTMF</tt> for the jabber protocol.
 *
 * @author Boris Grozev
 */
public class OperationSetIncomingDTMFJabberImpl
    implements OperationSetIncomingDTMF,
               DTMFListener
{
    private final Set<DTMFListener> listeners = new HashSet<DTMFListener>();

    /**
     * {@inheritDoc}
     *
     * Implements
     * {@link net.java.sip.communicator.service.protocol.OperationSetIncomingDTMF#addDTMFListener(net.java.sip.communicator.service.protocol.event.DTMFListener)}
     */
    @Override
    public void addDTMFListener(DTMFListener listener)
    {
        listeners.add(listener);
    }

    /**
     * {@inheritDoc}
     *
     * Implements
     * {@link net.java.sip.communicator.service.protocol.OperationSetIncomingDTMF#removeDTMFListener(net.java.sip.communicator.service.protocol.event.DTMFListener)}
     */
    @Override
    public void removeDTMFListener(DTMFListener listener)
    {
        listeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     *
     * Implements
     * {@link net.java.sip.communicator.service.protocol.event.DTMFListener#toneReceived(net.java.sip.communicator.service.protocol.event.DTMFReceivedEvent)}
     */
    @Override
    public void toneReceived(DTMFReceivedEvent evt)
    {
        for (DTMFListener listener : listeners)
            listener.toneReceived(evt);
    }
}
