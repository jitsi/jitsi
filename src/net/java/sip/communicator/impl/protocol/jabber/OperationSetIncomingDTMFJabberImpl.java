/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
