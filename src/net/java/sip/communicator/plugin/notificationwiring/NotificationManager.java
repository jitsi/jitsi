/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.notificationwiring;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import org.osgi.framework.*;

import javax.imageio.*;
import java.awt.image.*;
import java.net.*;
import java.util.*;

/**
 * Listens for all kinds of events and triggers when needed a notification,
 * a popup or sound one or other.
 * @author Damian Minkov
 */
public class NotificationManager
    implements MessageListener,
               ServiceListener,
               FileTransferListener,
               TypingNotificationsListener,
               CallListener,
               CallChangeListener,
               CallPeerListener,
               CallPeerSecurityListener,
               ChatRoomMessageListener,
               LocalUserChatRoomPresenceListener,
               LocalUserAdHocChatRoomPresenceListener,
               AdHocChatRoomMessageListener,
               CallPeerConferenceListener,
               Recorder.Listener
{
    /**
     * Our logger.
     */
    private static final Logger logger =
        Logger.getLogger(NotificationManager.class);

    /**
     * The image used, when a contact has no photo specified.
     */
    public static final ImageID DEFAULT_USER_PHOTO
        = new ImageID("service.gui.DEFAULT_USER_PHOTO");

    /**
     * Stores all already loaded images.
     */
    private static final Map<ImageID, BufferedImage> loadedImages =
        new Hashtable<ImageID, BufferedImage>();

    /**
     * Pseudo timer used to delay multiple typings notifications before
     * receiving the message.
     *
     * Time to live : 1 minute
     */
    private Map<Contact,Long> proactiveTimer = new HashMap<Contact, Long>();

    /**
     * Stores notification references to stop them if a notification has expired
     * (e.g. to stop the dialing sound).
     */
    private Map<Call, NotificationData> callNotifications =
        new WeakHashMap<Call, NotificationData>();

    /**
     * Default event type for call been saved using a recorder.
     */
    public static final String CALL_SAVED = "CallSaved";

    /**
     * Default event type for incoming file transfers.
     */
    public static final String INCOMING_FILE = "IncomingFile";

    /**
     * Default event type for security error on a call.
     */
    public static final String CALL_SECURITY_ERROR = "CallSecurityError";

    /**
     * Default event type for activated security on a call.
     */
    public static final String CALL_SECURITY_ON = "CallSecurityOn";

    /**
     * Default event type when a secure message received.
     */
    public static final String SECURITY_MESSAGE = "SecurityMessage";

    /**
     * Default event type for
     * proactive notifications (typing notifications when chatting).
     */
    public static final String PROACTIVE_NOTIFICATION = "ProactiveNotification";

    /**
     * Default event type for hanging up calls.
     */
    public static final String HANG_UP = "HangUp";

    /**
     * Default event type for dialing.
     */
    public static final String DIALING = "Dialing";

    /**
     * Default event type for a busy call.
     */
    public static final String BUSY_CALL = "BusyCall";

    /**
     * Default event type for outgoing calls.
     */
    public static final String OUTGOING_CALL = "OutgoingCall";

    /**
     * Default event type for receiving calls (incoming calls).
     */
    public static final String INCOMING_CALL = "IncomingCall";

    /**
     * Default event type for receiving messages.
     */
    public static final String INCOMING_MESSAGE = "IncomingMessage";

    /**
     * Initialize, register default notifications and start listening for
     * new protocols or removed one and find any that are already registered.
     */
    void init()
    {
        registerDefaultNotifications();

        // listens for new protocols
        NotificationWiringActivator.bundleContext.addServiceListener(this);

        // enumerate currently registered protocols
        for(ProtocolProviderService pp : getProtocolProviders())
        {
            handleProviderAdded(pp);
        }

        NotificationWiringActivator.getMediaService().addRecorderListener(this);
    }

    /**
     * Register all default notifications.
     */
    private void registerDefaultNotifications()
    {
        NotificationService notificationService
            = NotificationWiringActivator.getNotificationService();

        if(notificationService == null)
            return;

        // Register incoming message notifications.
        notificationService.registerDefaultNotificationForEvent(
                INCOMING_MESSAGE,
                NotificationAction.ACTION_POPUP_MESSAGE,
                null,
                null);

        notificationService.registerDefaultNotificationForEvent(
                INCOMING_MESSAGE,
                NotificationAction.ACTION_SOUND,
                SoundProperties.INCOMING_MESSAGE,
                null);

        // Register incoming call notifications.
        notificationService.registerDefaultNotificationForEvent(
                INCOMING_CALL,
                NotificationAction.ACTION_POPUP_MESSAGE,
                null,
                null);

        SoundNotificationAction inCallSoundHandler
            = new SoundNotificationAction(SoundProperties.INCOMING_CALL, 2000);

        notificationService.registerDefaultNotificationForEvent(
                INCOMING_CALL,
                inCallSoundHandler);

        // Register outgoing call notifications.
        SoundNotificationAction outCallSoundHandler
            = new SoundNotificationAction(SoundProperties.OUTGOING_CALL, 3000);

        notificationService.registerDefaultNotificationForEvent(
                OUTGOING_CALL,
                outCallSoundHandler);

        // Register busy call notifications.
        SoundNotificationAction busyCallSoundHandler
            = new SoundNotificationAction(SoundProperties.BUSY, 1);

        notificationService.registerDefaultNotificationForEvent(
                BUSY_CALL,
                busyCallSoundHandler);

        // Register dial notifications.
        SoundNotificationAction dialSoundHandler
            = new SoundNotificationAction(SoundProperties.DIALING, 0);

        notificationService.registerDefaultNotificationForEvent(
                DIALING,
                dialSoundHandler);

        // Register the hangup sound notification.
        SoundNotificationAction hangupSoundHandler
            = new SoundNotificationAction(SoundProperties.HANG_UP, -1);

        notificationService.registerDefaultNotificationForEvent(
                HANG_UP,
                hangupSoundHandler);

        // Register proactive notifications.
        notificationService.registerDefaultNotificationForEvent(
                PROACTIVE_NOTIFICATION,
                NotificationAction.ACTION_POPUP_MESSAGE,
                null,
                null);

        // Register warning message notifications.
        notificationService.registerDefaultNotificationForEvent(
                SECURITY_MESSAGE,
                NotificationAction.ACTION_POPUP_MESSAGE,
                null,
                null);

        // Register sound notification for security state on during a call.
        notificationService.registerDefaultNotificationForEvent(
                CALL_SECURITY_ON,
                NotificationAction.ACTION_SOUND,
                SoundProperties.CALL_SECURITY_ON,
                null);

        // Register sound notification for security state off during a call.
        notificationService.registerDefaultNotificationForEvent(
                CALL_SECURITY_ERROR,
                NotificationAction.ACTION_SOUND,
                SoundProperties.CALL_SECURITY_ERROR,
                null);

        // Register sound notification for incoming files.
        notificationService.registerDefaultNotificationForEvent(
                INCOMING_FILE,
                NotificationAction.ACTION_POPUP_MESSAGE,
                null,
                null);

        notificationService.registerDefaultNotificationForEvent(
                INCOMING_FILE,
                NotificationAction.ACTION_SOUND,
                SoundProperties.INCOMING_FILE,
                null);

        // Register notification for saved calls.
        notificationService.registerDefaultNotificationForEvent(
            CALL_SAVED,
            NotificationAction.ACTION_POPUP_MESSAGE,
            null,
            null);
    }

    /**
     * Returns all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     * context.
     *
     * @return all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     *         context
     */
    public static Map<Object, ProtocolProviderFactory>
        getProtocolProviderFactories()
    {
        ServiceReference[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs
                = NotificationWiringActivator.bundleContext.getServiceReferences(
                        ProtocolProviderFactory.class.getName(),
                        null);
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("NotificationManager : " + e);
        }

        Map<Object, ProtocolProviderFactory>
            providerFactoriesMap = new Hashtable<Object, ProtocolProviderFactory>();

        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                ProtocolProviderFactory providerFactory
                    = (ProtocolProviderFactory)
                        NotificationWiringActivator.bundleContext.getService(serRef);

                providerFactoriesMap.put(
                        serRef.getProperty(ProtocolProviderFactory.PROTOCOL),
                        providerFactory);
            }
        }
        return providerFactoriesMap;
    }

    /**
     * Returns all protocol providers currently registered.
     * @return all protocol providers currently registered.
     */
    public static List<ProtocolProviderService>
        getProtocolProviders()
    {
        ServiceReference[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs
                = NotificationWiringActivator.bundleContext.getServiceReferences(
                        ProtocolProviderService.class.getName(),
                        null);
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("NotificationManager : " + e);
        }

        List<ProtocolProviderService>
            providersList = new ArrayList<ProtocolProviderService>();

        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                ProtocolProviderService pp
                    = (ProtocolProviderService)
                        NotificationWiringActivator.bundleContext.getService(serRef);

                providersList.add(pp);
            }
        }
        return providersList;
    }

    /**
     * Adds all listeners related to the given protocol provider.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     */
    private void handleProviderAdded(ProtocolProviderService protocolProvider)
    {
        if(!protocolProvider.getAccountID().isEnabled())
            return;

        Map<String, OperationSet> supportedOperationSets
            = protocolProvider.getSupportedOperationSets();

        // Obtain the basic instant messaging operation set.
        String imOpSetClassName = OperationSetBasicInstantMessaging
                                    .class.getName();

        if (supportedOperationSets.containsKey(imOpSetClassName))
        {
            OperationSetBasicInstantMessaging im
                = (OperationSetBasicInstantMessaging)
                    supportedOperationSets.get(imOpSetClassName);

            //Add to all instant messaging operation sets the Message
            //listener which handles all received messages.
            im.addMessageListener(this);
        }

        // Obtain the typing notifications operation set.
        String tnOpSetClassName = OperationSetTypingNotifications
                                    .class.getName();

        if (supportedOperationSets.containsKey(tnOpSetClassName))
        {
            OperationSetTypingNotifications tn
                = (OperationSetTypingNotifications)
                    supportedOperationSets.get(tnOpSetClassName);

            //Add to all typing notification operation sets the Message
            //listener implemented in the ContactListPanel, which handles
            //all received messages.
            tn.addTypingNotificationsListener(this);
        }

        // Obtain file transfer operation set.
        OperationSetFileTransfer fileTransferOpSet
            = protocolProvider.getOperationSet(OperationSetFileTransfer.class);

        if (fileTransferOpSet != null)
        {
            fileTransferOpSet.addFileTransferListener(this);
        }

        OperationSetMultiUserChat multiChatOpSet
            = protocolProvider.getOperationSet(OperationSetMultiUserChat.class);

        if (multiChatOpSet != null)
        {
            multiChatOpSet.addPresenceListener(this);
        }

        OperationSetAdHocMultiUserChat multiAdHocChatOpSet
            = protocolProvider.getOperationSet(OperationSetAdHocMultiUserChat.class);

        if (multiAdHocChatOpSet != null)
        {
            multiAdHocChatOpSet.addPresenceListener(this);
        }

        OperationSetBasicTelephony<?> basicTelephonyOpSet
            = protocolProvider.getOperationSet(OperationSetBasicTelephony.class);

        if (basicTelephonyOpSet != null)
        {
            basicTelephonyOpSet.addCallListener(this);
        }
    }

    /**
     * Removes all listeners related to the given protocol provider.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     */
    private void handleProviderRemoved(ProtocolProviderService protocolProvider)
    {
        Map<String, OperationSet> supportedOperationSets
            = protocolProvider.getSupportedOperationSets();

        // Obtain the basic instant messaging operation set.
        String imOpSetClassName = OperationSetBasicInstantMessaging
                                    .class.getName();

        if (supportedOperationSets.containsKey(imOpSetClassName))
        {
            OperationSetBasicInstantMessaging im
                = (OperationSetBasicInstantMessaging)
                    supportedOperationSets.get(imOpSetClassName);

            //Add to all instant messaging operation sets the Message
            //listener which handles all received messages.
            im.removeMessageListener(this);
        }

        // Obtain the typing notifications operation set.
        String tnOpSetClassName = OperationSetTypingNotifications
                                    .class.getName();

        if (supportedOperationSets.containsKey(tnOpSetClassName))
        {
            OperationSetTypingNotifications tn
                = (OperationSetTypingNotifications)
                    supportedOperationSets.get(tnOpSetClassName);

            //Add to all typing notification operation sets the Message
            //listener implemented in the ContactListPanel, which handles
            //all received messages.
            tn.removeTypingNotificationsListener(this);
        }

        // Obtain file transfer operation set.
        OperationSetFileTransfer fileTransferOpSet
            = protocolProvider.getOperationSet(OperationSetFileTransfer.class);

        if (fileTransferOpSet != null)
        {
            fileTransferOpSet.removeFileTransferListener(this);
        }

        OperationSetMultiUserChat multiChatOpSet
            = protocolProvider.getOperationSet(OperationSetMultiUserChat.class);

        if (multiChatOpSet != null)
        {
            multiChatOpSet.removePresenceListener(this);
        }

        OperationSetAdHocMultiUserChat multiAdHocChatOpSet
            = protocolProvider.getOperationSet(OperationSetAdHocMultiUserChat.class);

        if (multiAdHocChatOpSet != null)
        {
            multiAdHocChatOpSet.removePresenceListener(this);
        }

        OperationSetBasicTelephony<?> basicTelephonyOpSet
            = protocolProvider.getOperationSet(OperationSetBasicTelephony.class);

        if (basicTelephonyOpSet != null)
        {
            basicTelephonyOpSet.removeCallListener(this);
        }
    }

    /**
     * Implements the <tt>ServiceListener</tt> method. Verifies whether the
     * passed event concerns a <tt>ProtocolProviderService</tt> and adds the
     * corresponding listeners.
     *
     * @param event The <tt>ServiceEvent</tt> object.
     */
    public void serviceChanged(ServiceEvent event)
    {
        ServiceReference serviceRef = event.getServiceReference();

        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
        {
            return;
        }

        Object service =
            NotificationWiringActivator.bundleContext.getService(serviceRef);

        // we don't care if the source service is not a protocol provider
        if (!(service instanceof ProtocolProviderService))
        {
            return;
        }

        switch (event.getType())
        {
            case ServiceEvent.REGISTERED:
                this.handleProviderAdded((ProtocolProviderService) service);
                break;
            case ServiceEvent.UNREGISTERING:
                this.handleProviderRemoved((ProtocolProviderService) service);
                break;
        }
    }

    /**
     * Fires a message notification for the given event type through the
     * <tt>NotificationService</tt>.
     *
     * @param eventType the event type for which we fire a notification
     * @param messageTitle the title of the message
     * @param message the content of the message
     * @return A reference to the fired notification to stop it.
     */
    public static NotificationData fireNotification(String eventType,
                                        String messageTitle,
                                        String message)
    {
        NotificationService notificationService
            = NotificationWiringActivator.getNotificationService();

        if(notificationService == null)
            return null;

        return notificationService.fireNotification(   eventType,
                                                messageTitle,
                                                message,
                                                null,
                                                null);
    }

    /**
     * Fires a message notification for the given event type through the
     * <tt>NotificationService</tt>.
     *
     * @param eventType the event type for which we fire a notification
     * @param messageTitle the title of the message
     * @param message the content of the message
     * @param extra additional event data for external processing
     * @return A reference to the fired notification to stop it.
     */
    public static NotificationData fireNotification(String eventType,
                                        String messageTitle,
                                        String message,
                                        Map<String,String> extra)
    {
        NotificationService notificationService
            = NotificationWiringActivator.getNotificationService();

        if(notificationService == null)
            return null;

        return notificationService.fireNotification(eventType,
                                                    messageTitle,
                                                    message,
                                                    extra,
                                                    null,
                                                    null);
    }

    /**
     * Fires a chat message notification for the given event type through the
     * <tt>NotificationService</tt>.
     *
     * @param chatContact the chat contact to which the chat message corresponds;
     * the chat contact could be a Contact or a ChatRoom.
     * @param eventType the event type for which we fire a notification
     * @param messageTitle the title of the message
     * @param message the content of the message
     */
    public static void fireChatNotification(Object chatContact,
                                            String eventType,
                                            String messageTitle,
                                            String message)
    {
        NotificationService notificationService
            = NotificationWiringActivator.getNotificationService();

        if(notificationService == null)
            return;

        NotificationAction popupActionHandler = null;
        UIService uiService = NotificationWiringActivator.getUIService();

        Chat chatPanel = null;
        byte[] contactIcon = null;
        if (chatContact instanceof Contact)
        {
            Contact contact = (Contact) chatContact;

            if(uiService != null)
                chatPanel = uiService.getChat(contact);

            contactIcon = contact.getImage();
            if(contactIcon == null)
            {
                contactIcon =
                    ImageUtils.toByteArray(getImage(DEFAULT_USER_PHOTO));
            }
        }
        else if (chatContact instanceof ChatRoom)
        {
            ChatRoom chatRoom = (ChatRoom) chatContact;

            // For system rooms we don't want to send notification events.
            if (chatRoom.isSystem())
                return;

            if(uiService != null)
                chatPanel = uiService.getChat(chatRoom);
        }

        if (chatPanel != null)
        {
            if (eventType.equals(INCOMING_MESSAGE)
                    && chatPanel.isChatFocused())
            {
                popupActionHandler = notificationService
                        .getEventNotificationAction(eventType,
                                NotificationAction.ACTION_POPUP_MESSAGE);

                popupActionHandler.setEnabled(false);
            }
        }

        notificationService.fireNotification(   eventType,
                                                messageTitle,
                                                message,
                                                null,
                                                contactIcon,
                                                chatContact);

        if(popupActionHandler != null)
            popupActionHandler.setEnabled(true);
    }

    /**
     * Fires a notification for the given event type through the
     * <tt>NotificationService</tt>. The event type is one of the static
     * constants defined in this class.
     *
     * @param eventType the event type for which we want to fire a notification
     * @return A reference to the fired notification to stop it.
     */
    public static NotificationData fireNotification(String eventType)
    {
        NotificationService notificationService
            = NotificationWiringActivator.getNotificationService();

        if(notificationService == null)
            return null;

        return notificationService.fireNotification(eventType);
    }

    /**
     * Stops all sounds for the given event type.
     *
     * @param data the event type for which we should stop sounds. One of
     * the static event types defined in this class.
     */
    public static void stopSound(NotificationData data)
    {
        NotificationService notificationService
            = NotificationWiringActivator.getNotificationService();

        if(notificationService == null)
            return;

        Iterable<NotificationHandler> soundHandlers =
            notificationService.getActionHandlers(
                NotificationAction.ACTION_SOUND);

        // There could be no sound action handler for this event type
        if(soundHandlers != null)
            for(NotificationHandler handler : soundHandlers)
                ((SoundNotificationHandler)handler).stop(data);
    }

    /**
     * Loads an image from a given image identifier.
     *
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static BufferedImage getImage(ImageID imageID)
    {
        BufferedImage image = null;

        if (loadedImages.containsKey(imageID))
        {
            image = loadedImages.get(imageID);
        }
        else
        {
            URL path = NotificationWiringActivator.getResources()
                .getImageURL(imageID.getId());

            if (path != null)
            {
                try
                {
                    image = ImageIO.read(path);

                    loadedImages.put(imageID, image);
                }
                catch (Exception ex)
                {
                    logger.error("Failed to load image: " + path, ex);
                }
            }
        }

        return image;
    }

    /**
     * Checks if the contained call is a conference call.
     *
     * @param call the call to check
     * @return <code>true</code> if the contained <tt>Call</tt> is a conference
     * call, otherwise - returns <code>false</code>.
     */
    public boolean isConference(Call call)
    {
        // If we're the focus of the conference.
        if (call.isConferenceFocus())
            return true;

        // If one of our peers is a conference focus, we're in a
        // conference call.
        Iterator<? extends CallPeer> callPeers = call.getCallPeers();

        while (callPeers.hasNext())
        {
            CallPeer callPeer = callPeers.next();

            if (callPeer.isConferenceFocus())
                return true;
        }

        // the call can have two peers at the same time and there is no one
        // is conference focus. This is situation when some one has made an
        // attended transfer and has transfered us. We have one call with two
        // peers the one we are talking to and the one we have been transfered
        // to. And the first one is been hanguped and so the call passes through
        // conference call fo a moment and than go again to one to one call.
        return call.getCallPeerCount() > 1;
    }

    /**
     * Determines whether a specific <code>ChatRoom</code> is private i.e.
     * represents a one-to-one conversation which is not a channel. Since the
     * interface {@link ChatRoom} does not expose the private property, an
     * heuristic is used as a workaround: (1) a system <code>ChatRoom</code> is
     * obviously not private and (2) a <code>ChatRoom</code> is private if it
     * has only one <code>ChatRoomMember</code> who is not the local user.
     *
     * @param chatRoom
     *            the <code>ChatRoom</code> to be determined as private or not
     * @return <tt>true</tt> if the specified <code>ChatRoom</code> is private;
     *         otherwise, <tt>false</tt>
     */
    private static boolean isPrivate(ChatRoom chatRoom)
    {
        if (!chatRoom.isSystem()
            && chatRoom.isJoined()
            && (chatRoom.getMembersCount() == 1))
        {
            String nickname = chatRoom.getUserNickname();

            if (nickname != null)
            {
                for (ChatRoomMember member : chatRoom.getMembers())
                    if (nickname.equals(member.getName()))
                        return false;
                return true;
            }
        }
        return false;
    }

    /**
     *  Fired on new messages.
     * @param evt the <tt>MessageReceivedEvent</tt> containing
     * details on the received message
     */
    public void messageReceived(MessageReceivedEvent evt)
    {
        try
        {
            // Fire notification
            String title = NotificationWiringActivator.getResources().getI18NString(
                "service.gui.MSG_RECEIVED",
                new String[]{evt.getSourceContact().getDisplayName()});

            fireChatNotification(
                    evt.getSourceContact(),
                    INCOMING_MESSAGE,
                    title,
                    evt.getSourceMessage().getContent());
        }
        catch(Throwable t)
        {
            logger.error("Error notifying for message received", t);
        }
    }

    /**
     * Fired when message is delivered.
     * @param evt the <tt>MessageDeliveredEvent</tt> containing
     * details on the delivered message
     */
    public void messageDelivered(MessageDeliveredEvent evt)
    {}

    /**
     * Fired when message deliver fail.
     * @param evt the <tt>MessageDeliveryFailedEvent</tt> containing
     * details on the failed message
     */
    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
    {}

    /**
     * When a request has been received we show a notification.
     *
     * @param event <tt>FileTransferRequestEvent</tt>
     * @see FileTransferListener#fileTransferRequestReceived(FileTransferRequestEvent)
     */
    public void fileTransferRequestReceived(FileTransferRequestEvent event)
    {
        try
        {
            IncomingFileTransferRequest request = event.getRequest();
            Contact sourceContact = request.getSender();

            //Fire notification
            String title = NotificationWiringActivator.getResources().getI18NString(
                "service.gui.FILE_RECEIVING_FROM",
                new String[]{sourceContact.getDisplayName()});

            fireChatNotification(
                    sourceContact,
                    INCOMING_FILE,
                    title,
                    request.getFileName());
        }
        catch(Throwable t)
        {
            logger.error("Error notifying for file transfer req received", t);
        }
    }

    /**
     * Nothing to do here, because we already know when a file transfer is
     * created.
     * @param event the <tt>FileTransferCreatedEvent</tt> that notified us
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
    {}

    /**
     * Called when an <tt>IncomingFileTransferRequest</tt> has been canceled
     * from the contact who sent it.
     *
     * @param event the <tt>FileTransferRequestEvent</tt> containing the
     * request which was canceled.
     */
    public void fileTransferRequestCanceled(FileTransferRequestEvent event)
    {}

    /**
     * Informs the user what is the typing state of his chat contacts.
     *
     * @param event the event containing details on the typing notification
     */
    public void typingNotificationReceived(TypingNotificationEvent event)
    {
        try
        {
            Contact contact = event.getSourceContact();

            // check whether the current chat window shows the
            // chat we received a typing info for and in such case don't show
            // notifications
            UIService uiService = NotificationWiringActivator.getUIService();

            if(uiService != null)
            {
                Chat chat = uiService.getCurrentChat();
                if(chat != null)
                {
                    MetaContact metaContact = uiService.getChatContact(chat);

                    if(metaContact != null && metaContact.containsContact(contact)
                        && chat.isChatFocused())
                    {
                        return;
                    }
                }
            }

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

            fireChatNotification(
                contact,
                PROACTIVE_NOTIFICATION,
                contact.getDisplayName(),
                NotificationWiringActivator.getResources()
                    .getI18NString("service.gui.PROACTIVE_NOTIFICATION"));
        }
        catch(Throwable t)
        {
            logger.error("Error notifying for typing evt received", t);
        }
    }

    /**
     * Implements CallListener.incomingCallReceived. When a call is received
     * plays the ring phone sound to the user and gathers caller information
     * that may be used by a user-specified command (incomingCall event trigger).
     * @param event the <tt>CallEvent</tt>
     */
    public void incomingCallReceived(CallEvent event)
    {
        try
        {
            Call call = event.getSourceCall();
            CallPeer firstPeer = call.getCallPeers().next();
            String peerName = firstPeer.getDisplayName();

            Map<String,String> peerInfo = new HashMap<String, String>();
            peerInfo.put("caller.uri", firstPeer.getURI());
            peerInfo.put("caller.address", firstPeer.getAddress());
            peerInfo.put("caller.name", firstPeer.getDisplayName());
            peerInfo.put("caller.id", firstPeer.getPeerID());

            callNotifications.put(event.getSourceCall(),
                fireNotification(
                    INCOMING_CALL,
                    "",
                    NotificationWiringActivator.getResources()
                            .getI18NString("service.gui.INCOMING_CALL",
                                    new String[]{peerName}),
                    peerInfo));

            call.addCallChangeListener(this);

            if(call.getCallPeers().hasNext())
            {
                CallPeer peer = call.getCallPeers().next();
                peer.addCallPeerListener(this);
                peer.addCallPeerSecurityListener(this);
                peer.addCallPeerConferenceListener(this);
            }
        }
        catch(Throwable t)
        {
            logger.error("Error notifying for incoming call received", t);
        }
    }

    /**
     * Do nothing. Implements CallListener.outGoingCallCreated.
     * @param event the <tt>CallEvent</tt>
     */
    public void outgoingCallCreated(CallEvent event)
    {
        Call call = event.getSourceCall();
        call.addCallChangeListener(this);

        if(call.getCallPeers().hasNext())
        {
            CallPeer peer = call.getCallPeers().next();
            peer.addCallPeerListener(this);
            peer.addCallPeerSecurityListener(this);
            peer.addCallPeerConferenceListener(this);
        }
    }

    /**
     * Implements CallListener.callEnded. Stops sounds that are playing at
     * the moment if there're any.
     * @param event the <tt>CallEvent</tt>
     */
    public void callEnded(CallEvent event)
    {
        try
        {
            // Stop all telephony related sounds.
//            stopAllTelephonySounds();
            stopSound(callNotifications.get(event.getSourceCall()));

            // Play the hangup sound.
            fireNotification(HANG_UP);
        }
        catch(Throwable t)
        {
            logger.error("Error notifying for call ended", t);
        }
    }

    /**
     * Implements the <tt>CallChangeListener.callPeerAdded</tt> method.
     * @param evt the <tt>CallPeerEvent</tt> that notifies us for the change
     */
    public void callPeerAdded(CallPeerEvent evt)
    {
        CallPeer peer = evt.getSourceCallPeer();

        if(peer == null)
            return;

        peer.addCallPeerListener(this);
        peer.addCallPeerSecurityListener(this);
        peer.addCallPeerConferenceListener(this);
    }

    /**
     * Implements the <tt>CallChangeListener.callPeerRemoved</tt> method.
     * @param evt the <tt>CallPeerEvent</tt> that has been triggered
     */
    public void callPeerRemoved(CallPeerEvent evt)
    {
        CallPeer peer = evt.getSourceCallPeer();

        if(peer == null)
            return;

        peer.removeCallPeerListener(this);
        peer.removeCallPeerSecurityListener(this);
        peer.addCallPeerConferenceListener(this);
    }

    /**
     * Call state changed.
     * @param evt the <tt>CallChangeEvent</tt> instance containing the source
     */
    public void callStateChanged(CallChangeEvent evt)
    {
    }

    /**
     * Fired when peer's state is changed
     *
     * @param evt fired CallPeerEvent
     */
    public void peerStateChanged(CallPeerChangeEvent evt)
    {
        try
        {
            CallPeer sourcePeer = evt.getSourceCallPeer();
            Call call = sourcePeer.getCall();
            CallPeerState newState = (CallPeerState) evt.getNewValue();
            CallPeerState oldState = (CallPeerState) evt.getOldValue();

            // Play the dialing audio when in connecting and initiating call state.
            // Stop the dialing audio when we enter any other state.
            if (newState == CallPeerState.INITIATING_CALL
                || newState == CallPeerState.CONNECTING)
            {
                callNotifications.put(call, fireNotification(DIALING));
            }
            else
            {
                stopSound(callNotifications.get(call));
            }

            if (newState == CallPeerState.ALERTING_REMOTE_SIDE
                //if we were already in state CONNECTING_WITH_EARLY_MEDIA the server
                //is already taking care of playing the notifications so we don't
                //need to fire a notification here.
                && oldState != CallPeerState.CONNECTING_WITH_EARLY_MEDIA)
            {
                callNotifications.put(call, fireNotification(OUTGOING_CALL));
            }
            else if (newState == CallPeerState.BUSY)
            {
                // We start the busy sound only if we're in a simple call.
                if (!isConference(call))
                {
                    callNotifications.put(call, fireNotification(BUSY_CALL));
                }
            }
            else if (newState == CallPeerState.DISCONNECTED
                    || newState == CallPeerState.FAILED)
            {
                callNotifications.put(call, fireNotification(HANG_UP));
            }
        }
        catch(Throwable t)
        {
            logger.error("Error notifying after peer state changed", t);
        }
    }

    /**
     * Fired when peer's display name is changed
     *
     * @param evt fired CallPeerEvent
     */
    public void peerDisplayNameChanged(CallPeerChangeEvent evt)
    {}

    /**
     * Fired when peer's address is changed
     *
     * @param evt fired CallPeerEvent
     */
    public void peerAddressChanged(CallPeerChangeEvent evt)
    {}

    /**
     * Fired when peer's transport is changed
     *
     * @param evt fired CallPeerEvent
     */
    public void peerTransportAddressChanged(CallPeerChangeEvent evt)
    {}

    /**
     * Fired when peer's image is changed
     *
     * @param evt fired CallPeerEvent
     */
    public void peerImageChanged(CallPeerChangeEvent evt)
    {}

    /**
     * When a <tt>securityOnEvent</tt> is received.
     * @param evt the event we received
     */
    public void securityOn(CallPeerSecurityOnEvent evt)
    {
        try
        {
            CallPeer peer = (CallPeer) evt.getSource();

            if((evt.getSecurityController().requiresSecureSignalingTransport()
                && peer.getProtocolProvider().isSignalingTransportSecure())
                || !evt.getSecurityController().requiresSecureSignalingTransport())
            {
                fireNotification(CALL_SECURITY_ON);
            }
        }
        catch(Throwable t)
        {
            logger.error("Error for notify for security event", t);
        }
    }

    /**
     * Indicates the new state through the security indicator components.
     * @param securityOffEvent the event we received
     */
    public void securityOff(CallPeerSecurityOffEvent securityOffEvent)
    {}

    /**
     * The handler for the security event received. The security event
     * represents a timeout trying to establish a secure connection.
     * Most probably the other peer doesn't support it.
     *
     * @param securityTimeoutEvent
     *            the security timeout event received
     */
    public void securityTimeout(
        CallPeerSecurityTimeoutEvent securityTimeoutEvent)
    {}

    /**
     * The handler for the security event received. The security event
     * for starting establish a secure connection.
     *
     * @param securityNegotiationStartedEvent
     *            the security started event received
     */
    public void securityNegotiationStarted(
        CallPeerSecurityNegotiationStartedEvent securityNegotiationStartedEvent)
    {}

    /**
     * Processes the received security message.
     * @param event the event we received
     */
    public void securityMessageRecieved(CallPeerSecurityMessageEvent event)
    {
        try
        {
            int severity = event.getEventSeverity();

            String messageTitle = null;

            switch (severity)
            {
                // Don't play alert sound for Info or warning.
                case CallPeerSecurityMessageEvent.INFORMATION:
                {
                    messageTitle = NotificationWiringActivator.getResources()
                        .getI18NString("service.gui.SECURITY_INFO");
                    break;
                }
                case CallPeerSecurityMessageEvent.WARNING:
                {
                    messageTitle = NotificationWiringActivator.getResources()
                        .getI18NString("service.gui.SECURITY_WARNING");
                    break;
                }
                // Alert sound indicates: security cannot established
                case CallPeerSecurityMessageEvent.SEVERE:
                case CallPeerSecurityMessageEvent.ERROR:
                {
                    messageTitle = NotificationWiringActivator.getResources()
                        .getI18NString("service.gui.SECURITY_ERROR");
                    fireNotification(CALL_SECURITY_ERROR);
                }
            }

            fireNotification(
                SECURITY_MESSAGE,
                messageTitle,
                event.getI18nMessage());
        }
        catch(Throwable t)
        {
            logger.error("Error notifying for security message received", t);
        }
    }

    /**
     * Implements the <tt>ChatRoomMessageListener.messageReceived</tt> method.
     * <br>
     * Obtains the corresponding <tt>ChatPanel</tt> and process the message
     * there.
     * @param evt the <tt>ChatRoomMessageReceivedEvent</tt> that notified us
     * that a message has been received
     */
    public void messageReceived(ChatRoomMessageReceivedEvent evt)
    {
        try
        {
            ChatRoom sourceChatRoom = evt.getSourceChatRoom();
            ChatRoomMember sourceMember = evt.getSourceChatRoomMember();

            // Fire notification
            boolean fireChatNotification;

            String messageContent = evt.getMessage().getContent();

            /*
             * It is uncommon for IRC clients to display popup notifications for
             * messages which are sent to public channels and which do not mention
             * the nickname of the local user.
             */
            if (sourceChatRoom.isSystem()
                || isPrivate(sourceChatRoom)
                || (messageContent == null))
                fireChatNotification = true;
            else
            {
                String nickname = sourceChatRoom.getUserNickname();

                int atIx = -1;

                if(nickname != null)
                    atIx = nickname.indexOf("@");

                fireChatNotification =
                    (nickname == null)
                        || messageContent.toLowerCase().contains(
                            nickname.toLowerCase())
                        || ((atIx == -1)? false : messageContent.toLowerCase()
                            .contains(nickname.substring(0, atIx).toLowerCase()));
            }

            if (fireChatNotification)
            {
                String title
                    = NotificationWiringActivator.getResources().getI18NString(
                        "service.gui.MSG_RECEIVED",
                        new String[] { sourceMember.getName() });

                fireChatNotification(
                        sourceChatRoom,
                        INCOMING_MESSAGE,
                        title,
                        messageContent);
            }
        }
        catch(Throwable t)
        {
            logger.error("Error notifying for chat room message received", t);
        }
    }

    /**
     * Implements the <tt>ChatRoomMessageListener.messageDelivered</tt> method.
     * <br>
     * @param evt the <tt>ChatRoomMessageDeliveredEvent</tt> that notified us
     * that the message was delivered to its destination
     */
    public void messageDelivered(ChatRoomMessageDeliveredEvent evt)
    {}

    /**
     * Implements the <tt>ChatRoomMessageListener.messageDeliveryFailed</tt>
     * method.
     * <br>
     * @param evt the <tt>ChatRoomMessageDeliveryFailedEvent</tt> that notified
     * us of a delivery failure
     */
    public void messageDeliveryFailed(ChatRoomMessageDeliveryFailedEvent evt)
    {}

    /**
     * Implements the
     * <tt>LocalUserChatRoomPresenceListener.localUserPresenceChanged</tt>
     * method.
     * @param evt the <tt>LocalUserChatRoomPresenceChangeEvent</tt> that
     * notified us
     */
    public void localUserPresenceChanged(
                    LocalUserChatRoomPresenceChangeEvent evt)
    {
        ChatRoom sourceChatRoom = evt.getChatRoom();
        String eventType = evt.getEventType();

        if (LocalUserChatRoomPresenceChangeEvent
                .LOCAL_USER_JOINED.equals(eventType))
        {
            sourceChatRoom.addMessageListener(this);
        }
        else if (LocalUserChatRoomPresenceChangeEvent
                        .LOCAL_USER_LEFT.equals(eventType)
                    || LocalUserChatRoomPresenceChangeEvent
                            .LOCAL_USER_KICKED.equals(eventType)
                    || LocalUserChatRoomPresenceChangeEvent
                            .LOCAL_USER_DROPPED.equals(eventType))
        {
            sourceChatRoom.removeMessageListener(this);
        }
    }

    /**
     * Implements the
     * <tt>LocalUserAdHocChatRoomPresenceListener.localUserPresenceChanged</tt>
     * method
     *
     * @param evt the <tt>LocalUserAdHocChatRoomPresenceChangeEvent</tt> that
     * notified us of a presence change
     */
    public void localUserAdHocPresenceChanged(
                    LocalUserAdHocChatRoomPresenceChangeEvent evt)
    {
        String eventType = evt.getEventType();

        if (LocalUserAdHocChatRoomPresenceChangeEvent
                .LOCAL_USER_JOINED.equals(eventType))
        {
            evt.getAdHocChatRoom().addMessageListener(this);
        }
        else if (LocalUserAdHocChatRoomPresenceChangeEvent
                        .LOCAL_USER_LEFT.equals(eventType)
                    || LocalUserAdHocChatRoomPresenceChangeEvent
                            .LOCAL_USER_DROPPED.equals(eventType))
        {
            evt.getAdHocChatRoom().removeMessageListener(this);
        }
    }

    /**
     * Implements the <tt>AdHocChatRoomMessageListener.messageReceived</tt>
     * method.
     * <br>
     * @param evt the <tt>AdHocChatRoomMessageReceivedEvent</tt> that notified
     * us
     */
    public void messageReceived(AdHocChatRoomMessageReceivedEvent evt)
    {
        try
        {
            AdHocChatRoom sourceChatRoom = evt.getSourceChatRoom();
            Contact sourceParticipant = evt.getSourceChatRoomParticipant();

            // Fire notification
            boolean fireChatNotification;

            String nickname = sourceChatRoom.getName();
            String messageContent = evt.getMessage().getContent();

            fireChatNotification =
                (nickname == null)
                    || messageContent.toLowerCase().contains(
                            nickname.toLowerCase());

            if (fireChatNotification)
            {
                String title
                    = NotificationWiringActivator.getResources().getI18NString(
                            "service.gui.MSG_RECEIVED",
                            new String[] { sourceParticipant.getDisplayName() });

                fireChatNotification(
                    sourceChatRoom,
                    INCOMING_MESSAGE,
                    title,
                    messageContent);
            }
        }
        catch(Throwable t)
        {
            logger.error("Error notifying for adhoc message received", t);
        }
    }

    /**
     * Implements the <tt>ChatRoomMessageListener.messageDelivered</tt> method.
     * <br>
     * @param evt the <tt>ChatRoomMessageDeliveredEvent</tt> that notified us
     * that the message was delivered to its destination
     */
    public void messageDelivered(AdHocChatRoomMessageDeliveredEvent evt)
    {}

    /**
     * Implements <tt>AdHocChatRoomMessageListener.messageDeliveryFailed</tt>
     * method.
     * <br>
     * In the conversation area shows an error message, explaining the problem.
     * @param evt the <tt>AdHocChatRoomMessageDeliveryFailedEvent</tt> that
     * notified us
     */
    public void messageDeliveryFailed(AdHocChatRoomMessageDeliveryFailedEvent evt)
    {}

    /**
     * Call peer has changed.
     * @param conferenceEvent
     *            a <tt>CallPeerConferenceEvent</tt> with ID
     *            <tt>CallPeerConferenceEvent#CONFERENCE_FOCUS_CHANGED</tt>
     */
    public void conferenceFocusChanged(CallPeerConferenceEvent conferenceEvent)
    {}

    /**
     * Indicates that the given conference member has been added to the given
     * peer.
     *
     * @param conferenceEvent the event
     */
    public void conferenceMemberAdded(CallPeerConferenceEvent conferenceEvent)
    {
        try
        {
            CallPeer peer =
                conferenceEvent.getConferenceMember().getConferenceFocusCallPeer();

            if(peer.getConferenceMemberCount() > 0)
            {
                CallPeerSecurityStatusEvent securityEvent
                    = peer.getCurrentSecuritySettings();

                if (securityEvent instanceof CallPeerSecurityOnEvent)
                {
                    fireNotification(CALL_SECURITY_ON);
                }
            }
        }
        catch(Throwable t)
        {
            logger.error("Error notifying for secured call member", t);
        }
    }

    /**
     * Indicates that the given conference member has been removed from the
     * given peer.
     *
     * @param conferenceEvent the event
     */
    public void conferenceMemberRemoved(CallPeerConferenceEvent conferenceEvent)
    {}

    /**
     * Notifies that a specific <tt>Recorder</tt> has
     * stopped recording the media associated with it.
     *
     * @param recorder the <tt>Recorder</tt> which has stopped recording its
     * associated media
     */
    public void recorderStopped(Recorder recorder)
    {
        try
        {
            fireNotification(
                    CALL_SAVED,
                    NotificationWiringActivator.getResources().getI18NString(
                            "plugin.callrecordingconfig.CALL_SAVED"),
                    NotificationWiringActivator.getResources().getI18NString(
                            "plugin.callrecordingconfig.CALL_SAVED_TO",
                            new String[] { recorder.getFilename() }));
        }
        catch(Throwable t)
        {
            logger.error("Error notifying for recorder stopped", t);
        }
    }
}
