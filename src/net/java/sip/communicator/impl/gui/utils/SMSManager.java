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
package net.java.sip.communicator.impl.gui.utils;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

import javax.swing.*;
import java.util.*;

/**
 * Handles any sms common actions.
 * @author Damian Minkov
 */
public class SMSManager
{
    /**
     * The <tt>Logger</tt> used by the <tt>ChatWindowManager</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(SMSManager.class);

    /**
     * Sends sms, chooses provider and sends the sms.
     * @param invoker the component invoker, used to get correct location
     * to show popup for choosing provider.
     * @param to the destination number
     */
    public static void sendSMS(
        JComponent invoker, String to)
    {
        sendSMSInternal(invoker, to, null, null);
    }

    /**
     * Sends sms, chooses provider and sends the sms.
     * @param invoker the component invoker, used to get correct location
     * to show popup for choosing provider.
     * @param to the destination number
     */
    private static void sendSMSInternal(
        JComponent invoker, String to, String messageText, ChatPanel chatPanel)
    {
        List<ProtocolProviderService> providers =
            AccountUtils
                .getRegisteredProviders(OperationSetSmsMessaging.class);

        if(providers.size() == 1)
        {
            //send
            if(messageText != null)
            {
                sendSMSInternal(
                    to,
                    messageText,
                    providers.get(0),
                    null,
                    chatPanel);
            }
            else
                sendSMSInternal(providers.get(0), to);
        }
        else if(providers.size() > 1)
        {
            ChooseSMSAccountPopupMenu chooseAccountDialog
                = new ChooseSMSAccountPopupMenu(
                        invoker,
                        to,
                        providers,
                        messageText,
                        chatPanel);

            chooseAccountDialog.setLocation(invoker.getLocation());
            chooseAccountDialog.showPopupMenu();
        }
    }

    /**
     * Sends sms message.
     * @param protocolProviderService
     * @param to the receive number
     * @param messageText the text
     */
    public static void sendSMS(
            ProtocolProviderService protocolProviderService,
            String to,
            String messageText)
        throws Exception
    {
        OperationSetSmsMessaging smsOpSet
            = protocolProviderService
                .getOperationSet(OperationSetSmsMessaging.class);

        Message smsMessage = smsOpSet.createMessage(messageText);

        smsOpSet.sendSmsMessage(to, smsMessage);
    }

    /**
     * Sends sms message.
     * @param contact the contact to send sms to
     * @param messageText the text.
     */
    public static void sendSMS(
            Contact contact,
            String messageText)
        throws Exception
    {
        OperationSetSmsMessaging smsOpSet = contact.getProtocolProvider()
            .getOperationSet(OperationSetSmsMessaging.class);

        Message smsMessage = smsOpSet.createMessage(messageText);

        smsOpSet.sendSmsMessage(contact, smsMessage);
    }

    /**
     * Sends sms.
     * @param protocolProviderService
     * @param to
     */
    private static void sendSMSInternal(
        ProtocolProviderService protocolProviderService,
        String to)
    {
        OperationSetSmsMessaging smsMessaging =
            protocolProviderService.getOperationSet(
                OperationSetSmsMessaging.class);
        Contact contact = smsMessaging.getContact(to);

        MetaContact metaContact = GuiActivator.getContactListService()
                .findMetaContactByContact(contact);

        if(metaContact == null)
        {
            logger.error("MetaContact not found for: " + contact);
            return;
        }

        GuiActivator.getUIService().getChatWindowManager()
            .startChat(metaContact, contact, true);
    }

    /**
     * Sends sms, chooses phone and chooses provider and sends the sms.
     * @param invoker the component invoker, used to get correct location
     * to show popup for choosing provider.
     * @param additionalNumbers the destination numbers to choose from
     */
    public static void sendSMS(final JComponent invoker,
                               List<UIContactDetail> additionalNumbers,
                               String messageText,
                               ChatPanel chatPanel)
    {
        if(additionalNumbers.size() == 1)
        {
            sendSMSInternal(invoker, additionalNumbers.get(0).getAddress(),
                messageText, chatPanel);
        }
        else
        {
            ChooseSMSAccountPopupMenu chooseAccountDialog
                = new ChooseSMSAccountPopupMenu(
                        invoker,
                        additionalNumbers,
                        OperationSetSmsMessaging.class,
                        messageText,
                        chatPanel);

            chooseAccountDialog.setLocation(invoker.getLocation());
            chooseAccountDialog.showPopupMenu();
        }
    }

    /**
     * Sends sms message using chatTransport otherwise.
     * @param phoneNumber
     * @param message
     * @param chatTransport the transport to use if protocol provider missing
     * @param chatPanel the panel where the message is sent, will be used for
     * success or fail messages
     */
    public static void sendSMS(
        String phoneNumber,
        String message,
        ChatTransport chatTransport,
        ChatPanel chatPanel)
    {
        sendSMSInternal(phoneNumber, message, null, chatTransport, chatPanel);
    }

    /**
     * Sends sms message using protocolProviderService if it is not null,
     * or using chatTransport otherwise.
     * @param phoneNumber
     * @param message
     * @param protocolProviderService the protocol provider service to use,
     * if not null.
     * @param chatTransport the transport to use if protocol provider missing
     * @param chatPanel the panel where the message is sent, will be used for
     * success or fail messages
     */
    private static void sendSMSInternal(
        String phoneNumber,
        String message,
        ProtocolProviderService protocolProviderService,
        ChatTransport chatTransport,
        ChatPanel chatPanel)
    {
        try
        {
            if(protocolProviderService != null)
            {
                sendSMS(protocolProviderService, phoneNumber, message);
            }
            else
            {
                if(phoneNumber != null)
                    chatTransport.sendSmsMessage(phoneNumber, message);
                else
                    chatTransport.sendSmsMessage(message);
            }
        }
        catch (IllegalStateException ex)
        {
            logger.error("Failed to send SMS.", ex);

            chatPanel.addMessage(
                phoneNumber,
                new Date(),
                Chat.OUTGOING_MESSAGE,
                message,
                "text/plain");

            chatPanel.addErrorMessage(
                phoneNumber,
                GuiActivator.getResources()
                    .getI18NString("service.gui.SMS_SEND_CONNECTION_PROBLEM"));
        }
        catch (Exception ex)
        {
            logger.error("Failed to send SMS.", ex);

            chatPanel.addMessage(
                phoneNumber == null ? chatTransport.getName() : phoneNumber,
                new Date(),
                Chat.OUTGOING_MESSAGE,
                message,
                "text/plain");

            chatPanel.addErrorMessage(
                phoneNumber == null ? chatTransport.getName() : phoneNumber,
                ex.getMessage());
        }

        chatPanel.refreshWriteArea();
    }

    /**
     * Extends ChooseCallAccountPopupMenu to use it for sms functionality.
     */
    private static class ChooseSMSAccountPopupMenu
        extends ChooseCallAccountPopupMenu
    {
        private String messageText = null;
        private ChatPanel chatPanel = null;

        /**
         * Creates popup menu.
         * @param invoker
         * @param contactToCall
         * @param telephonyProviders
         */
        public ChooseSMSAccountPopupMenu(
            JComponent invoker,
            final String contactToCall,
            List<ProtocolProviderService> telephonyProviders,
            String messageText,
            ChatPanel chatPanel)
        {
            super(invoker, contactToCall, telephonyProviders,
                OperationSetBasicTelephony.class);

            this.messageText = messageText;
            this.chatPanel = chatPanel;
        }

        /**
         * Creates popup menu.
         * @param invoker
         * @param telephonyObjects
         * @param opSetClass
         */
        public ChooseSMSAccountPopupMenu(
            JComponent invoker,
            List<?> telephonyObjects,
            Class<? extends OperationSet> opSetClass,
            String messageText,
            ChatPanel chatPanel)
        {
            super(invoker, telephonyObjects, opSetClass);

            this.messageText = messageText;
            this.chatPanel = chatPanel;
        }

        /**
         * Sends sms when number is selected and several providers are
         * available.
         * @param opSetClass the operation set to use.
         * @param providers list of available protocol providers
         * @param contact the contact address selected
         */
        @Override
        protected void itemSelected(
            Class<? extends OperationSet> opSetClass,
            List<ProtocolProviderService> providers,
            String contact)
        {
            SMSManager.sendSMSInternal(invoker, contact, messageText, chatPanel);
        }

        /**
         * Sends sms when we have a number and provider.
         * @param opSetClass the operation set to use.
         * @param protocolProviderService the protocol provider
         * @param contact the contact address
         * @param uiContact the <tt>MetaContact</tt> selected
         */
        @Override
        protected void itemSelected(
            Class<? extends OperationSet> opSetClass,
            ProtocolProviderService protocolProviderService,
            String contact,
            UIContactImpl uiContact)
        {
            if(messageText != null)
            {
                sendSMSInternal(
                    contact,
                    messageText,
                    protocolProviderService,
                    null,
                    chatPanel);
            }
            else
                sendSMSInternal(protocolProviderService, contact);
        }

        /**
         * Sends sms when we have a number and provider.
         * @param opSetClass the operation set to use.
         * @param protocolProviderService the protocol provider
         * @param contact the contact address selected
         */
        @Override
        protected void itemSelected(
            Class<? extends OperationSet> opSetClass,
            ProtocolProviderService protocolProviderService,
            String contact)
        {
            if(messageText != null)
            {
                sendSMSInternal(
                    contact,
                    messageText,
                    protocolProviderService,
                    null,
                    chatPanel);
            }
            else
                sendSMSInternal(protocolProviderService, contact);
        }

        @Override
        protected String getI18NKeyChooseContact()
        {
            return "service.gui.CHOOSE_NUMBER";
        }

        @Override
        protected String getI18NKeyCallVia()
        {
            return "service.gui.SEND_VIA";
        }
    }
}
