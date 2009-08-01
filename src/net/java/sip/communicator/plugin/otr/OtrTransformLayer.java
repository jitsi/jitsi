package net.java.sip.communicator.plugin.otr;

import java.security.*;
import java.util.*;

import net.java.otr4j.*;
import net.java.otr4j.message.MessageConstants;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

public class OtrTransformLayer
    implements TransformLayer
{
    private static Logger logger = Logger.getLogger(TransformLayer.class);

    private UserState us = new UserState(new OTR4jListener()
    {
//        @Override
        public void showWarning(String warn)
        {
            logger.warn(warn);
        }

//        @Override
        public void showError(String err)
        {
            logger.error(err);
        }

//        @Override
        public void injectMessage(String messageText, String account,
            String user, String protocol)
        {
            Contact contact = getContact(account, user, protocol);
            OperationSetBasicInstantMessaging imOpSet =
                (OperationSetBasicInstantMessaging) contact
                    .getProtocolProvider().getOperationSet(
                        OperationSetBasicInstantMessaging.class);

            Message message = imOpSet.createMessage(messageText);
            imOpSet.sendInstantMessage(contact, message);
        }

//        @Override
        public int getPolicy(ConnContext arg0)
        {
            return PolicyConstants.ALLOW_V2;
        }

//        @Override
        public KeyPair getKeyPair(String arg0, String arg1)
            throws NoSuchAlgorithmException
        {
            try
            {
                return CryptoUtils.generateDsaKeyPair();
            }
            catch (Exception e)
            {
                return null;
            }
        }
    });

    private List<Contact> contacts = new Vector<Contact>();

    private Contact getContact(String account, String user, String protocol)
    {
        for (Contact c : contacts)
        {
            String cuser = c.getAddress();
            ProtocolProviderService cprotoProvider = c.getProtocolProvider();
            String caccount = cprotoProvider.getAccountID().toString();
            String cprotocol = cprotoProvider.getProtocolName();

            if (user.equals(cuser) && account.equals(caccount)
                && protocol.equals(cprotocol))
                return c;
        }
        return null;
    }

    private void addContact(Contact contact)
    {
        if (contact == null)
            return;

        String user = contact.getAddress();
        ProtocolProviderService protoProvider = contact.getProtocolProvider();
        String account = protoProvider.getAccountID().toString();
        String protocol = protoProvider.getProtocolName();

        for (Contact c : contacts)
        {
            String cuser = c.getAddress();
            ProtocolProviderService cprotoProvider = c.getProtocolProvider();
            String caccount = cprotoProvider.getAccountID().toString();
            String cprotocol = cprotoProvider.getProtocolName();

            if (user.equals(cuser) && account.equals(caccount)
                && protocol.equals(cprotocol))
                return;
        }

        contacts.add(contact);
    }

//    @Override
    public MessageDeliveredEvent messageDelivered(MessageDeliveredEvent evt)
    {
        if (evt.getSourceMessage().getContent().contains(MessageConstants.BASE_HEAD))
            return null;
        else
            return evt;
    }

//    @Override
    public MessageDeliveryFailedEvent messageDeliveryFailed(
        MessageDeliveryFailedEvent evt)
    {
        return evt;
    }

//    @Override
    public MessageDeliveredEvent messageDeliveryPending(
        MessageDeliveredEvent evt)
    {   
        Contact contact = evt.getDestinationContact();
        addContact(contact);
        String user = contact.getAddress();

        ProtocolProviderService protoProvider = contact.getProtocolProvider();
        String account = protoProvider.getAccountID().toString();
        String protocol = protoProvider.getProtocolName();

        Message msg = evt.getSourceMessage();
        String msgContent = msg.getContent();

        OperationSetBasicInstantMessaging imOpSet =
            (OperationSetBasicInstantMessaging) contact.getProtocolProvider()
                .getOperationSet(OperationSetBasicInstantMessaging.class);

        String processedMessageContent =
            us.handleSendingMessage(user, account, protocol, msgContent);

        if (processedMessageContent == null
            || processedMessageContent.length() < 1)
            return null;

        if (processedMessageContent.equals(msgContent))
            return evt;

        Message processedMessage =
            imOpSet.createMessage(processedMessageContent);

        MessageDeliveredEvent processedEvent =
            new MessageDeliveredEvent(processedMessage, contact, evt
                .getTimestamp());

        return processedEvent;
    }

//    @Override
    public MessageReceivedEvent messageReceived(MessageReceivedEvent evt)
    {
        Contact contact = evt.getSourceContact();
        addContact(contact);
        String user = contact.getAddress();

        ProtocolProviderService protoProvider = contact.getProtocolProvider();
        String account = protoProvider.getAccountID().toString();
        String protocol = protoProvider.getProtocolName();

        Message msg = evt.getSourceMessage();
        String msgContent = msg.getContent();

        OperationSetBasicInstantMessaging imOpSet =
            (OperationSetBasicInstantMessaging) contact.getProtocolProvider()
                .getOperationSet(OperationSetBasicInstantMessaging.class);

        String processedMessageContent =
            us.handleReceivingMessage(user, account, protocol, msgContent);

        if (processedMessageContent == null
            || processedMessageContent.length() < 1)
            return null;

        if (processedMessageContent.equals(msgContent))
            return evt;

        Message processedMessage =
            imOpSet.createMessage(processedMessageContent);

        MessageReceivedEvent processedEvent =
            new MessageReceivedEvent(processedMessage, contact, evt
                .getTimestamp());

        return processedEvent;
    }
}
