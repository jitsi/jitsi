/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import net.java.otr4j.*;
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
        Contact contact = evt.getDestinationContact();
        OtrContact otrContact =
            OtrContactManager.getOtrContact(contact, evt.getContactResource());

        OtrPolicy policy = OtrActivator.scOtrEngine.getContactPolicy(contact);
        ScSessionStatus sessionStatus =
            OtrActivator.scOtrEngine.getSessionStatus(otrContact);
        // If OTR is disabled and we are not over an encrypted session, don't
        // process anything.
        if (!policy.getEnableManual()
            && sessionStatus != ScSessionStatus.ENCRYPTED
            && sessionStatus != ScSessionStatus.FINISHED)
            return evt;

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
    public MessageDeliveredEvent messageDeliveryPending(
        MessageDeliveredEvent evt)
    {
        Contact contact = evt.getDestinationContact();
        OtrContact otrContact =
            OtrContactManager.getOtrContact(contact, evt.getContactResource());

        OtrPolicy policy = OtrActivator.scOtrEngine.getContactPolicy(contact);
        ScSessionStatus sessionStatus =
            OtrActivator.scOtrEngine.getSessionStatus(otrContact);
        // If OTR is disabled and we are not over an encrypted session, don't
        // process anything.
        if (!policy.getEnableManual()
            && sessionStatus != ScSessionStatus.ENCRYPTED
            && sessionStatus != ScSessionStatus.FINISHED)
            return evt;

        // If this is a message otr4j injected earlier, return the event as is.
        if (OtrActivator.scOtrEngine.isMessageUIDInjected(evt
            .getSourceMessage().getMessageUID()))
            return evt;

        // Process the outgoing message.
        String msgContent = evt.getSourceMessage().getContent();
        String processedMessageContent =
            OtrActivator.scOtrEngine.transformSending(otrContact, msgContent);

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
            imOpSet.createMessage(
                processedMessageContent,
                evt.getSourceMessage().getContentType(),
                evt.getSourceMessage().getEncoding(),
                evt.getSourceMessage().getSubject());

        // Create a new event and return.
        MessageDeliveredEvent processedEvent =
            new MessageDeliveredEvent(processedMessage, contact, evt
                .getTimestamp());

        if(processedMessage.getContent().contains(SerializationConstants.HEAD))
        {
            processedEvent.setMessageEncrypted(true);
        }

        return processedEvent;
    }

    /*
     * Implements TransformLayer#messageReceived(MessageReceivedEvent).
     */
    public MessageReceivedEvent messageReceived(MessageReceivedEvent evt)
    {
        Contact contact = evt.getSourceContact();
        OtrContact otrContact =
            OtrContactManager.getOtrContact(contact, evt.getContactResource());

        OtrPolicy policy = OtrActivator.scOtrEngine.getContactPolicy(contact);
        ScSessionStatus sessionStatus =
            OtrActivator.scOtrEngine.getSessionStatus(otrContact);
        // If OTR is disabled and we are not over an encrypted session, don't
        // process anything.
        if (!policy.getEnableManual()
            && sessionStatus != ScSessionStatus.ENCRYPTED
            && sessionStatus != ScSessionStatus.FINISHED)
            return evt;

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
