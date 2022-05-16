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
