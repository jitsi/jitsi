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
package net.java.sip.communicator.plugin.otr;

import net.java.otr4j.io.*;
import net.java.sip.communicator.plugin.otr.OtrContactManager.OtrContact;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The Off-the-Record {@link TransformLayer} implementation.
 *
 * @author George Politis
 */
public class OtrTransformLayer
    implements TransformLayer
{
    /*
     * Implements TransformLayer#messageDelivered(MessageDeliveredEvent).
     */
    public MessageDeliveredEvent messageDelivered(MessageDeliveredEvent evt)
    {
        if (OtrActivator.scOtrEngine.isMessageUIDInjected(evt
            .getSourceMessage().getMessageUID()))
            // If this is a message otr4j injected earlier, don't display it,
            // this may have to change when we add support for fragmentation..
            return null;
        else
            return evt;
    }

    /*
     * Implements
     * TransformLayer#messageDeliveryFailed(MessageDeliveryFailedEvent).
     */
    public MessageDeliveryFailedEvent messageDeliveryFailed(
        MessageDeliveryFailedEvent evt)
    {
        return evt;
    }

    /*
     * Implements TransformLayer#messageDeliveryPending(MessageDeliveredEvent).
     */
    public MessageDeliveredEvent[] messageDeliveryPending(
        MessageDeliveredEvent evt)
    {
        Contact contact = evt.getDestinationContact();
        OtrContact otrContact =
            OtrContactManager.getOtrContact(contact, evt.getContactResource());

        // If this is a message otr4j injected earlier, return the event as is.
        if (OtrActivator.scOtrEngine.isMessageUIDInjected(evt
            .getSourceMessage().getMessageUID()))
            return new MessageDeliveredEvent[] {evt};

        // Process the outgoing message.
        String msgContent = evt.getSourceMessage().getContent();
        String[] processedMessageContent =
            OtrActivator.scOtrEngine.transformSending(otrContact, msgContent);

        if (processedMessageContent == null
            || processedMessageContent.length <= 0
            || processedMessageContent[0].length() < 1)
            return new MessageDeliveredEvent[0];

        if (processedMessageContent.length == 1
            && processedMessageContent[0].equals(msgContent))
            return new MessageDeliveredEvent[] {evt};

        final MessageDeliveredEvent[] processedEvents =
            new MessageDeliveredEvent[processedMessageContent.length];
        for (int i = 0; i < processedMessageContent.length; i++)
        {
            final String fragmentContent = processedMessageContent[i];
            // Forge a new message based on the new contents.
            OperationSetBasicInstantMessaging imOpSet =
                contact.getProtocolProvider().getOperationSet(
                    OperationSetBasicInstantMessaging.class);
            Message processedMessage =
                imOpSet.createMessage(fragmentContent, evt
                    .getSourceMessage().getContentType(), evt
                    .getSourceMessage().getEncoding(), evt.getSourceMessage()
                    .getSubject());

            // Create a new event and return.
            final MessageDeliveredEvent processedEvent =
                new MessageDeliveredEvent(processedMessage, contact,
                    evt.getTimestamp());

            if (processedMessage.getContent().contains(
                SerializationConstants.HEAD))
            {
                processedEvent.setMessageEncrypted(true);
            }

            processedEvents[i] = processedEvent;
        }

        return processedEvents;
    }

    /*
     * Implements TransformLayer#messageReceived(MessageReceivedEvent).
     */
    public MessageReceivedEvent messageReceived(MessageReceivedEvent evt)
    {
        Contact contact = evt.getSourceContact();
        OtrContact otrContact =
            OtrContactManager.getOtrContact(contact, evt.getContactResource());

        // Process the incoming message.
        String msgContent = evt.getSourceMessage().getContent();

        String processedMessageContent =
            OtrActivator.scOtrEngine.transformReceiving(otrContact, msgContent);

        if (processedMessageContent == null
            || processedMessageContent.length() < 1)
            return null;

        if (processedMessageContent.equals(msgContent))
            return evt;

        // Forge a new message based on the new contents.
        OperationSetBasicInstantMessaging imOpSet =
            contact.getProtocolProvider().getOperationSet(
                OperationSetBasicInstantMessaging.class);
        Message processedMessage =
            imOpSet.createMessageWithUID(
                processedMessageContent,
                evt.getSourceMessage().getContentType(),
                evt.getSourceMessage().getMessageUID());

        // Create a new event and return.
        MessageReceivedEvent processedEvent =
            new MessageReceivedEvent(processedMessage, contact, evt
                .getContactResource(), evt.getTimestamp(),
                evt.getCorrectedMessageUID());

        return processedEvent;
    }
}
