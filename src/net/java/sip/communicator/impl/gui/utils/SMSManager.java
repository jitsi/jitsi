/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.contactlist.*;
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
    public static void sendSMS(JComponent invoker, final String to)
    {
        List<ProtocolProviderService> providers =
            AccountUtils
                .getRegisteredProviders(OperationSetSmsMessaging.class);

        if(providers.size() == 1)
        {
            //send
            sendSms(providers.get(0), to);
        }
        else if(providers.size() > 1)
        {
            ChooseCallAccountPopupMenu chooseAccountDialog
                = new ChooseCallAccountPopupMenu(
                        invoker,
                        to,
                        providers)
            {
                @Override
                protected void itemSelected(
                    Class<? extends OperationSet> opSetClass,
                    ProtocolProviderService protocolProviderService,
                    String contact,
                    UIContactImpl uiContact)
                {
                    sendSms(protocolProviderService, to);
                }

                @Override
                protected void itemSelected(
                    Class<? extends OperationSet> opSetClass,
                    ProtocolProviderService protocolProviderService,
                    String contact)
                {
                    sendSms(protocolProviderService, to);
                }
            };

            chooseAccountDialog.setLocation(invoker.getLocation());
            chooseAccountDialog.showPopupMenu();
        }
    }

    /**
     * Sends sms.
     * @param protocolProviderService
     * @param to
     */
    private static void sendSms(
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
}
