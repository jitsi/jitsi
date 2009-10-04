/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contacteventhandler.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * The contactlist panel not only contains the contact list but it has the role
 * of a message dispatcher. It process all sent and received messages as well as
 * all typing notifications. Here are managed all contact list mouse events.
 *
 * @author Yana Stamcheva
 */
public class ContactListPane
    extends SCScrollPane
    implements  MessageListener,
                TypingNotificationsListener,
                FileTransferListener,
                ContactListListener,
                PluginComponentListener
{
    private final MainFrame mainFrame;

    private ContactList contactList;

    private final TypingTimer typingTimer = new TypingTimer();

    private CommonRightButtonMenu commonRightButtonMenu;

    private final Logger logger = Logger.getLogger(ContactListPane.class);

    private final ChatWindowManager chatWindowManager;

    /**
     * Pseudo timer used to delay multiple typings notifications before
     * receiving the message.
     * 
     * Time to live : 1 minute
     */
    private Map<Contact,Long> proactiveTimer = new HashMap<Contact, Long>();

    /**
     * Creates the contactlist scroll panel defining the parent frame.
     *
     * @param mainFrame The parent frame.
     */
    public ContactListPane(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;

        this.chatWindowManager
            = GuiActivator.getUIService().getChatWindowManager();

        this.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.initPluginComponents();
    }

    /**
     * Initializes the contact list.
     *
     * @param contactListService The MetaContactListService which will be used
     *            for a contact list data model.
     */
    public void initList(MetaContactListService contactListService)
    {
        this.contactList = new ContactList(mainFrame);

        TransparentPanel transparentPanel
            = new TransparentPanel(new BorderLayout());

        transparentPanel.add(contactList, BorderLayout.NORTH);

        this.setViewportView(transparentPanel);

        this.contactList.addContactListListener(this);
        this.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
                if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
                {
                    commonRightButtonMenu = new CommonRightButtonMenu(mainFrame);

                    commonRightButtonMenu.setInvoker(ContactListPane.this);

                    commonRightButtonMenu.setLocation(e.getX()
                            + mainFrame.getX() + 5, e.getY() + mainFrame.getY()
                            + 105);

                    commonRightButtonMenu.setVisible(true);
                }
            }
        });

        this.contactList.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        this.getActionMap().put("runChat", new ContactListPanelEnterAction());

        InputMap imap = this.getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "runChat");
    }

    /**
     * Returns the contact list.
     *
     * @return the contact list
     */
    public ContactList getContactList()
    {
        return this.contactList;
    }

    /**
     * Implements the ContactListListener.contactSelected method.
     */
    public void contactClicked(ContactListEvent evt)
    {
        MetaContact metaContact = evt.getSourceContact();

        // Searching for the right proto contact to use as default for the
        // chat conversation.
        Contact defaultContact = metaContact.getDefaultContact();

        ProtocolProviderService defaultProvider
            = defaultContact.getProtocolProvider();

        OperationSetBasicInstantMessaging
            defaultIM = (OperationSetBasicInstantMessaging)
                defaultProvider.getOperationSet(
                        OperationSetBasicInstantMessaging.class);

        ProtocolProviderService protoContactProvider;
        OperationSetBasicInstantMessaging protoContactIM;

        boolean isOfflineMessagingSupported
            = defaultIM != null && !defaultIM.isOfflineMessagingSupported();

        if (defaultContact.getPresenceStatus().getStatus() < 1
                && (!isOfflineMessagingSupported
                    || !defaultProvider.isRegistered()))
        {
            Iterator<Contact> protoContacts = metaContact.getContacts();

            while(protoContacts.hasNext())
            {
                Contact contact = protoContacts.next();

                protoContactProvider = contact.getProtocolProvider();

                protoContactIM = (OperationSetBasicInstantMessaging)
                    protoContactProvider.getOperationSet(
                        OperationSetBasicInstantMessaging.class);

                if(protoContactIM != null
                        && protoContactIM.isOfflineMessagingSupported()
                        && protoContactProvider.isRegistered())
                {
                    defaultContact = contact;
                }
            }
        }

        ContactEventHandler contactHandler = mainFrame
            .getContactHandler(defaultContact.getProtocolProvider());

        contactHandler.contactClicked(defaultContact, evt.getClickCount());
    }

    /**
     * Implements the ContactListListener.groupSelected method.
     */
    public void groupSelected(ContactListEvent evt)
    {}

    /**
     * Implements the ContactListListener.protocolContactSelected method.
     */
    public void protocolContactClicked(ContactListEvent evt)
    {
        Contact protoContact = evt.getSourceProtoContact();

        ContactEventHandler contactHandler = mainFrame
            .getContactHandler(protoContact.getProtocolProvider());

        contactHandler.contactClicked(protoContact, evt.getClickCount());
    }

    /**
     * Runs the chat window for the specified contact. We examine different
     * cases here, depending on the chat window mode.
     *
     * In mode "Open messages in new window" a new window is opened for the
     * given <tt>MetaContact</tt> if there's no opened window for it,
     * otherwise the existing chat window is made visible and focused.
     *
     * In mode "Group messages in one chat window" a JTabbedPane is used to show
     * chats for different contacts in one window. A new tab is opened for the
     * given <tt>MetaContact</tt> if there's no opened tab for it, otherwise
     * the existing chat tab is selected and focused.
     *
     * @author Yana Stamcheva
     */
    public class RunMessageWindow implements Runnable
    {
        private MetaContact metaContact;

        private Contact protocolContact;

        private boolean isSmsSelected = false;

        /**
         * Creates an instance of <tt>RunMessageWindow</tt> by specifying the
         *
         * @param metaContact the meta contact to which we will talk.
         */
        public RunMessageWindow(MetaContact metaContact)
        {
            this.metaContact = metaContact;
        }

        /**
         * Creates a chat window
         *
         * @param metaContact
         * @param protocolContact
         */
        public RunMessageWindow(MetaContact metaContact,
            Contact protocolContact)
        {
            this.metaContact = metaContact;
            this.protocolContact = protocolContact;
        }

        /**
         * Creates a chat window
         *
         * @param metaContact
         * @param protocolContact
         * @param isSmsSelected
         */
        public RunMessageWindow(MetaContact metaContact,
            Contact protocolContact, boolean isSmsSelected)
        {
            this.metaContact = metaContact;
            this.protocolContact = protocolContact;
            this.isSmsSelected = isSmsSelected;
        }

        /**
         * Opens a chat window
         */
        public void run()
        {
            ChatPanel chatPanel
                = chatWindowManager
                    .getContactChat(metaContact, protocolContact);

            chatPanel.setSmsSelected(isSmsSelected);

            chatWindowManager.openChat(chatPanel, true);
        }
    }

    /**
     * When a message is received determines whether to open a new chat window
     * or chat window tab, or to indicate that a message is received from a
     * contact which already has an open chat. When the chat is found checks if
     * in mode "Auto popup enabled" and if this is the case shows the message in
     * the appropriate chat panel.
     *
     * @param evt the event containing details on the received message
     */
    public void messageReceived(MessageReceivedEvent evt)
    {
        logger.trace("MESSAGE RECEIVED from contact: "
            + evt.getSourceContact().getAddress());

        Contact protocolContact = evt.getSourceContact();
        Message message = evt.getSourceMessage();
        int eventType = evt.getEventType();

        MetaContact metaContact = mainFrame.getContactList()
                .findMetaContactByContact(protocolContact);

        if(metaContact != null)
        {
            // Obtain the corresponding chat panel.
            final ChatPanel chatPanel
                = chatWindowManager.getContactChat( metaContact,
                                                    protocolContact,
                                                    message.getMessageUID());

            // Show an envelope on the sender contact in the contact list and
            // in the systray.
            if (!chatPanel.isChatFocused())
                contactList.addActiveContact(metaContact);

            // Distinguish the message type, depending on the type of event that
            // we have received.
            String messageType = null;

            if(eventType == MessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED)
            {
                messageType = Chat.INCOMING_MESSAGE;
            }
            else if(eventType == MessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED)
            {
                messageType = Chat.SYSTEM_MESSAGE;
            }
            else if(eventType == MessageReceivedEvent.SMS_MESSAGE_RECEIVED)
            {
                messageType = Chat.SMS_MESSAGE;
            }

            chatPanel.addMessage(
                protocolContact.getDisplayName(),
                evt.getTimestamp(),
                messageType,
                message.getContent(),
                message.getContentType());

            // A bug Fix for Previous/Next buttons .
            // Must update buttons state after message is processed
            // otherwise states are not proper
            chatPanel.getChatWindow().getMainToolBar().
                changeHistoryButtonsState(chatPanel);

            // Opens the chat panel with the new message in the UI thread.
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    chatWindowManager.openChat(chatPanel, false);
                }
            });

            // Fire notification
            String title = GuiActivator.getResources().getI18NString(
                "service.gui.MSG_RECEIVED",
                new String[]{evt.getSourceContact().getDisplayName()});

            NotificationManager.fireChatNotification(
                                            protocolContact,
                                            NotificationManager.INCOMING_MESSAGE,
                                            title,
                                            message.getContent());

            ChatTransport chatTransport
                = chatPanel.getChatSession()
                    .findChatTransportForDescriptor(protocolContact);

            chatPanel.setSelectedChatTransport(chatTransport);
        }
        else
        {
            logger.trace("MetaContact not found for protocol contact: "
                    + protocolContact + ".");
        }
    }

    /**
     * When a sent message is delivered shows it in the chat conversation panel.
     *
     * @param evt the event containing details on the message delivery
     */
    public void messageDelivered(MessageDeliveredEvent evt)
    {
        Contact contact = evt.getDestinationContact();
        MetaContact metaContact
            = mainFrame.getContactList().findMetaContactByContact(contact);

        logger.trace("MESSAGE DELIVERED to contact: " + contact.getAddress());

        ChatPanel chatPanel
            = chatWindowManager.getContactChat(metaContact, false);

        if (chatPanel != null)
        {
            Message msg = evt.getSourceMessage();
            ProtocolProviderService protocolProvider
                = contact.getProtocolProvider();

            logger.trace(
                "MESSAGE DELIVERED: process message to chat for contact: "
                + contact.getAddress()
                + " MESSAGE: " + msg.getContent());

            chatPanel.addMessage(
                this.mainFrame.getAccount(protocolProvider),
                evt.getTimestamp(),
                Chat.OUTGOING_MESSAGE,
                msg.getContent(),
                msg.getContentType());
        }
    }

    /**
     * Shows a warning message to the user when message delivery has failed.
     *
     * @param evt the event containing details on the message delivery failure
     */
    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
    {
        logger.error(evt.getReason());

        String errorMsg = null;

        Message sourceMessage = (Message) evt.getSource();

        Contact sourceContact = evt.getDestinationContact();

        MetaContact metaContact = mainFrame.getContactList()
            .findMetaContactByContact(sourceContact);

        if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.OFFLINE_MESSAGES_NOT_SUPPORTED)
        {
            errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_DELIVERY_NOT_SUPPORTED");
        }
        else if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.NETWORK_FAILURE)
        {
            errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_NOT_DELIVERED",
                    new String[]{evt.getReason()});
        }
        else if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.PROVIDER_NOT_REGISTERED)
        {
            errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_SEND_CONNECTION_PROBLEM");
        }
        else if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.INTERNAL_ERROR)
        {
            errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_DELIVERY_INTERNAL_ERROR",
                    new String[]{evt.getReason()});
        }
        else
        {
            errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_DELIVERY_UNKNOWN_ERROR",
                    new String[]{evt.getReason()});
        }

        ChatPanel chatPanel
            = chatWindowManager.getContactChat(metaContact, sourceContact);

        chatPanel.addMessage(
                metaContact.getDisplayName(),
                System.currentTimeMillis(),
                Chat.OUTGOING_MESSAGE,
                sourceMessage.getContent(),
                sourceMessage.getContentType());

        chatPanel.addErrorMessage(
                metaContact.getDisplayName(),
                errorMsg);

        chatWindowManager.openChat(chatPanel, false);
    }

    /**
     * Informs the user what is the typing state of his chat contacts.
     *
     * @param evt the event containing details on the typing notification
     */
    public void typingNotificationReceived(TypingNotificationEvent evt)
    {
        if (typingTimer.isRunning())
            typingTimer.stop();

        String notificationMsg = "";

        MetaContact metaContact = mainFrame.getContactList()
                .findMetaContactByContact(evt.getSourceContact());
        String contactName = metaContact.getDisplayName() + " ";

        if (contactName.equals(""))
        {
            contactName = GuiActivator.getResources()
                .getI18NString("service.gui.UNKNOWN") + " ";
        }

        int typingState = evt.getTypingState();

        if (typingState == OperationSetTypingNotifications.STATE_TYPING)
        {
            notificationMsg
                = GuiActivator.getResources().getI18NString(
                    "service.gui.CONTACT_TYPING",
                    new String[]{contactName});

            typingTimer.setMetaContact(metaContact);
            typingTimer.start();

            // Proactive typing notification
            if (!chatWindowManager.isChatOpenedFor(metaContact))
            {
                this.fireProactiveNotification(evt.getSourceContact());
                return;
            }
        }
        else if (typingState == OperationSetTypingNotifications.STATE_PAUSED)
        {
            notificationMsg = GuiActivator.getResources().getI18NString(
                "service.gui.CONTACT_PAUSED_TYPING",
                new String[]{contactName});
            typingTimer.setMetaContact(metaContact);
            typingTimer.start();
        }
        else if (typingState == OperationSetTypingNotifications.STATE_STOPPED)
        {
            notificationMsg = "";
        }
        else if (typingState == OperationSetTypingNotifications.STATE_STALE)
        {
            notificationMsg
                = GuiActivator.getResources().getI18NString(
                    "service.gui.CONTACT_TYPING_STATE_STALE");
        }
        else if (typingState == OperationSetTypingNotifications.STATE_UNKNOWN)
        {
            // TODO: Implement state unknown
        }
        this.setChatNotificationMsg(metaContact, notificationMsg);
    }

    /**
     * When a request has been received we show it to the user through the
     * chat session renderer.
     * 
     * @param event <tt>FileTransferRequestEvent</tt>
     * @see FileTransferListener#fileTransferRequestReceived(FileTransferRequestEvent)
     */
    public void fileTransferRequestReceived(FileTransferRequestEvent event)
    {
        IncomingFileTransferRequest request = event.getRequest();

        Contact sourceContact = request.getSender();

        MetaContact metaContact = mainFrame.getContactList()
            .findMetaContactByContact(sourceContact);

        final ChatPanel chatPanel
            = chatWindowManager.getContactChat(metaContact, sourceContact);

        chatPanel.addIncomingFileTransferRequest(
            event.getFileTransferOperationSet(), request, event.getTimestamp());

        ChatTransport chatTransport
            = chatPanel.getChatSession()
                .findChatTransportForDescriptor(sourceContact);

        chatPanel.setSelectedChatTransport(chatTransport);

        // Opens the chat panel with the new message in the UI thread.
        chatWindowManager.openChat(chatPanel, false);

        // Fire notification
        String title = GuiActivator.getResources().getI18NString(
            "service.gui.FILE_RECEIVING_FROM",
            new String[]{sourceContact.getDisplayName()});

        NotificationManager
            .fireChatNotification(
                sourceContact,
                NotificationManager.INCOMING_FILE,
                title,
                request.getFileName());
    }

    /**
     * Nothing to do here, because we already know when a file transfer is
     * created.
     */
    public void fileTransferCreated(FileTransferCreatedEvent event)
    {}

    /**
     * Called when a new <tt>IncomingFileTransferRequest</tt> has been rejected.
     * Nothing to do here, because we are the one who rejects the request.
     *
     * @param event the <tt>FileTransferRequestEvent</tt> containing the
     * received request which was rejected.
     */
    public void fileTransferRequestRejected(FileTransferRequestEvent event)
    {
    }

    /**
     * Called when an <tt>IncomingFileTransferRequest</tt> has been canceled
     * from the contact who sent it.
     *
     * @param event the <tt>FileTransferRequestEvent</tt> containing the 
     * request which was canceled.
     */
    public void fileTransferRequestCanceled(FileTransferRequestEvent event)
    {
    }

    /**
     * Send a proactive notification according to the proactive timer.
     * The notification is fired only if another notification hasn't been
     * recieved for more than 1 minute
     *
     * @param contac The contact the notification comes from
     */
    private void fireProactiveNotification(Contact contact)
    {
        long currentTime = System.currentTimeMillis();

        if (this.proactiveTimer.size() > 0)
        {
            //first remove contacts that have been here longer than the timeout
            //to avoid memory leaks
            Iterator<Map.Entry<Contact, Long>> entries
                                    = this.proactiveTimer.entrySet().iterator();
            while (entries.hasNext())
            {
                Map.Entry<Contact, Long> entry = entries.next();
                Long lastNotificationDate = entry.getValue();
                if (lastNotificationDate.longValue() + 30000 <  currentTime)
                {
                    // The entry is outdated
                    entries.remove();
                }
            }

            // Now, check if the contact is still in the map
            if (this.proactiveTimer.containsKey(contact))
            {
                // We already notified the others about this
                return;
            }
        }

        this.proactiveTimer.put(contact, currentTime);

        NotificationManager.fireChatNotification(
                contact,
                NotificationManager.PROACTIVE_NOTIFICATION,
                contact.getDisplayName(),
                GuiActivator.getResources()
                    .getI18NString("service.gui.PROACTIVE_NOTIFICATION"));
    }

    /**
     * Sets the typing notification message at the appropriate chat.
     *
     * @param metaContact The meta contact.
     * @param notificationMsg The typing notification message.
     */
    public void setChatNotificationMsg(MetaContact metaContact,
            String notificationMsg)
    {
        ChatPanel chatPanel
            = chatWindowManager.getContactChat(metaContact, false);

        if (chatPanel != null)
            chatPanel.setStatusMessage(notificationMsg);
    }

    /**
     * Returns the right button menu of the contact list.
     * @return the right button menu of the contact list
     */
    public CommonRightButtonMenu getCommonRightButtonMenu()
    {
        return commonRightButtonMenu;
    }

    /**
     * The TypingTimer is started after a PAUSED typing notification is
     * received. It waits 5 seconds and if no other typing event occurs removes
     * the PAUSED message from the chat status panel.
     */
    private class TypingTimer extends Timer
    {

        private MetaContact metaContact;

        public TypingTimer() {
            // Set delay
            super(5 * 1000, null);

            this.addActionListener(new TimerActionListener());
        }

        private class TimerActionListener implements ActionListener
        {
            public void actionPerformed(ActionEvent e)
            {
                setChatNotificationMsg(metaContact, "");
            }
        }

        private void setMetaContact(MetaContact metaContact)
        {
            this.metaContact = metaContact;
        }
    }

    /**
     * Opens chat window when the selected value is a MetaContact and opens a
     * group when the selected value is a MetaContactGroup.
     */
    private class ContactListPanelEnterAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            Object selectedValue = contactList.getSelectedValue();

            if (selectedValue instanceof MetaContact)
            {
                MetaContact contact = (MetaContact) selectedValue;

                SwingUtilities.invokeLater(new RunMessageWindow(contact));
            }
        }
    }

    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_CONTACT_LIST.getID()+")";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                PluginComponent.class.getName(),
                osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            logger.error("Could not obtain plugin reference.", exc);
        }

        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                PluginComponent component
                    = (PluginComponent)
                        GuiActivator.bundleContext.getService(serRef);

                Object selectedValue =
                    mainFrame.getContactListPanel().getContactList()
                        .getSelectedValue();

                if(selectedValue instanceof MetaContact)
                {
                    component.setCurrentContact((MetaContact)selectedValue);
                }
                else if(selectedValue instanceof MetaContactGroup)
                {
                    component
                        .setCurrentContactGroup((MetaContactGroup)selectedValue);
                }

                String pluginConstraints = component.getConstraints();
                Object constraints = null;

                if (pluginConstraints != null)
                    constraints = UIServiceImpl
                        .getBorderLayoutConstraintsFromContainer(
                            pluginConstraints);
                else
                    constraints = BorderLayout.SOUTH;

                this.add((Component)component.getComponent(), constraints);

                this.repaint();
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent pluginComponent = event.getPluginComponent();

        // If the container id doesn't correspond to the id of the plugin
        // container we're not interested.
        if(!pluginComponent.getContainer()
                .equals(Container.CONTAINER_CONTACT_LIST))
            return;

        Object constraints = UIServiceImpl
            .getBorderLayoutConstraintsFromContainer(
                    pluginComponent.getConstraints());

        if (constraints == null)
            constraints = BorderLayout.SOUTH;

        this.add((Component) pluginComponent.getComponent(), constraints);

        Object selectedValue = mainFrame.getContactListPanel()
                .getContactList().getSelectedValue();

        if(selectedValue instanceof MetaContact)
        {
            pluginComponent
                .setCurrentContact((MetaContact)selectedValue);
        }
        else if(selectedValue instanceof MetaContactGroup)
        {
            pluginComponent
                .setCurrentContactGroup((MetaContactGroup)selectedValue);
        }

        this.revalidate();
        this.repaint();
    }

    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        // If the container id doesn't correspond to the id of the plugin
        // container we're not interested.
        if(!c.getContainer()
                .equals(Container.CONTAINER_CONTACT_LIST))
            return;

        this.remove((Component) c.getComponent());
    }
}
