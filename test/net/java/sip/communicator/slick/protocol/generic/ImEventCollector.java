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
package net.java.sip.communicator.slick.protocol.generic;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Collects instant messaging events.
 */
public class ImEventCollector
    implements MessageListener
{
    private static final Logger logger =
        Logger.getLogger(ImEventCollector.class);

    public List<EventObject> collectedEvents = new LinkedList<EventObject>();

    /**
     * Called when a new incoming <tt>Message</tt> has been received.
     *
     * @param evt the <tt>MessageReceivedEvent</tt> containing the newly
     *            received message, its sender and other details.
     */
    public void messageReceived(MessageReceivedEvent evt)
    {
        logger.debug("Received a MessageReceivedEvent: " + evt);

        synchronized (this)
        {
            collectedEvents.add(evt);
            notifyAll();
        }
    }

    /**
     * Called to indicated that delivery of a message sent earlier has failed.
     * Reason code and phrase are contained by the <tt>MessageFailedEvent</tt>
     *
     * @param evt the <tt>MessageFailedEvent</tt> containing the ID of the
     *            message whose delivery has failed.
     */
    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
    {
        logger.debug("Received a MessageDeliveryFailedEvent: " + evt);

        synchronized (this)
        {
            collectedEvents.add(evt);
            notifyAll();
        }
    }

    /**
     * Called when the underlying implementation has received an indication that
     * a message, sent earlier has been successfully received by the
     * destination.
     *
     * @param evt the MessageDeliveredEvent containing the id of the message
     *            that has caused the event.
     */
    public void messageDelivered(MessageDeliveredEvent evt)
    {
        logger.debug("Received a MessageDeliveredEvent: " + evt);

        synchronized (this)
        {
            collectedEvents.add(evt);
            notifyAll();
        }
    }

    /**
     * Blocks until at least one event is received or until waitFor miliseconds
     * pass (whichever happens first).
     *
     * @param waitFor the number of miliseconds that we should be waiting for an
     *            event before simply bailing out.
     */
    public void waitForEvent(long waitFor)
    {
        synchronized (this)
        {

            if (collectedEvents.size() > 0)
                return;

            try
            {
                wait(waitFor);
            }
            catch (InterruptedException ex)
            {
                logger.debug("Interrupted while waiting for a message evt", ex);
            }
        }
    }
}
