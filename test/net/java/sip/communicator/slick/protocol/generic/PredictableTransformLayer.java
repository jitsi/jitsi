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

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;


public class PredictableTransformLayer
    implements TransformLayer
{
    private static final Logger logger =
        Logger.getLogger(PredictableTransformLayer.class);

    public MessageDeliveredEvent messageDelivered(MessageDeliveredEvent evt)
    {
        logger
            .debug("Message Delivered Transformation, transform a message after it has been sent.");
        logger.debug("IN: " + evt.getSourceMessage().getContent());
        Message transformedMessage =
            createMessage(evt.getDestinationContact(), evt.getSourceMessage(),
                "DELIVERED");

        logger.debug("OUT: " + transformedMessage.getContent());
        return new MessageDeliveredEvent(transformedMessage, evt
            .getDestinationContact(), evt.getTimestamp());
    }

    public MessageDeliveryFailedEvent messageDeliveryFailed(
        MessageDeliveryFailedEvent evt)
    {
        logger
            .debug("Message Delivery Failed Transformation, transform a message after it has failed to be sent.");
        logger.debug("IN: " + evt.getSourceMessage().getContent());
        Message transformedMessage =
            createMessage(evt.getDestinationContact(), evt.getSourceMessage(),
                "DELIVERY_FAILED");

        logger.debug("OUT: " + transformedMessage.getContent());
        return new MessageDeliveryFailedEvent(transformedMessage, evt
            .getDestinationContact(), evt.getErrorCode());
    }

    public MessageDeliveredEvent[] messageDeliveryPending(
        MessageDeliveredEvent evt)
    {
        logger
            .debug("Message Delivered Transformation, transform a message after it has failed to be sent.");
        logger.debug("IN: " + evt.getSourceMessage().getContent());
        Message transformedMessage =
            createMessage(evt.getDestinationContact(), evt.getSourceMessage(),
                "DELIVERY_PENDING");

        logger.debug("OUT: " + transformedMessage.getContent());
        return new MessageDeliveredEvent[]
        { new MessageDeliveredEvent(transformedMessage,
            evt.getDestinationContact(), evt.getTimestamp()) };
    }

    public MessageReceivedEvent messageReceived(MessageReceivedEvent evt)
    {
        logger
            .debug("Message Received Transformation, transform a message after it has been received.");
        logger.debug("IN: " + evt.getSourceMessage().getContent());
        Message transformedMessage =
            createMessage(evt.getSourceContact(), evt.getSourceMessage(),
                "RECEIVED");

        logger.debug("OUT: " + transformedMessage.getContent());
        return new MessageReceivedEvent(transformedMessage, evt
            .getSourceContact(), evt.getTimestamp());

    }

    private Message createMessage(Contact contact, Message message,
        String action)
    {
        OperationSetBasicInstantMessaging imOpSet =
            contact.getProtocolProvider()
                .getOperationSet(OperationSetBasicInstantMessaging.class);
        return imOpSet.createMessage("__" + action + "__"
            + message.getContent());
    }

}
