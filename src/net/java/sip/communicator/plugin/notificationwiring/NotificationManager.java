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
package net.java.sip.communicator.plugin.notificationwiring;

import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.apache.commons.lang3.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.recording.*;
import org.jitsi.service.protocol.event.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Listens to various events which are related to the display and/or playback of
 * notifications and shows/starts or hides/stops the notifications in question.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class NotificationManager
    implements AdHocChatRoomMessageListener,
               CallChangeListener,
               CallListener,
               CallPeerConferenceListener,
               CallPeerListener,
               CallPeerSecurityListener,
               ChatRoomMessageListener,
               FileTransferListener,
               LocalUserAdHocChatRoomPresenceListener,
               LocalUserChatRoomPresenceListener,
               MessageListener,
               Recorder.Listener,
               ServiceListener,
               TypingNotificationsListener
{
    /**
     * Default event type for a busy call.
     */
    public static final String BUSY_CALL = "BusyCall";

    /**
     * Default event type for call been saved using a recorder.
     */
    public static final String CALL_SAVED = "CallSaved";

    /**
     * Default event type for security error on a call.
     */
    public static final String CALL_SECURITY_ERROR = "CallSecurityError";

    /**
     * Default event type for activated security on a call.
     */
    public static final String CALL_SECURITY_ON = "CallSecurityOn";

    /**
     * The image used, when a contact has no photo specified.
     */
    public static final ImageID DEFAULT_USER_PHOTO
        = new ImageID("service.gui.DEFAULT_USER_PHOTO");

    /**
     * Default event type for dialing.
     */
    public static final String DIALING = "Dialing";

    /**
     * Default event type for hanging up calls.
     */
    public static final String HANG_UP = "HangUp";

    /**
     * Default event type for receiving calls (incoming calls).
     */
    public static final String INCOMING_CALL = "IncomingCall";

    /**
     * Default event type for incoming file transfers.
     */
    public static final String INCOMING_FILE = "IncomingFile";

    /**
     * Default event type for receiving messages.
     */
    public static final String INCOMING_MESSAGE = "IncomingMessage";

    /**
     * HTML content type.
     */
    private static final String HTML_CONTENT_TYPE = "text/html";

    /**
     * The <tt>Logger</tt> used by the <tt>NotificationManager</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(NotificationManager.class);

    /**
     * Default event type for outgoing calls.
     */
    public static final String OUTGOING_CALL = "OutgoingCall";

    /**
     * Default event type for
     * proactive notifications (typing notifications when chatting).
     */
    public static final String PROACTIVE_NOTIFICATION = "ProactiveNotification";

    /**
     * Default event type when a secure message received.
     */
    public static final String SECURITY_MESSAGE = "SecurityMessage";

    /**
     * Fires a chat message notification for the given event type through the
     * <tt>NotificationService</tt>.
     *
     * @param chatContact the chat contact to which the chat message corresponds;
     * the chat contact could be a Contact or a ChatRoom.
     * @param eventType the event type for which we fire a notification
     * @param messageTitle the title of the message
     * @param message the content of the message
     * @param messageUID the message UID
     */
    public static void fireChatNotification(Object chatContact,
                                            String eventType,
                                            String messageTitle,
                                            String message,
                                            String messageUID)
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
                chatPanel = uiService.getChat(contact, messageUID);

            contactIcon = contact.getImage();
            if(contactIcon == null)
            {
                contactIcon =
                    NotificationWiringActivator.getImageLoaderService()
                        .getImageBytes(DEFAULT_USER_PHOTO);
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

        Map<String,Object> extras = new HashMap<String,Object>();

        extras.put(
                NotificationData.POPUP_MESSAGE_HANDLER_TAG_EXTRA,
                chatContact);
        notificationService.fireNotification(
                eventType,
                messageTitle,
                message,
                contactIcon,
                extras);

        if(popupActionHandler != null)
            popupActionHandler.setEnabled(true);
    }

    /**
     * Fires a notification for the given event type through the
     * <tt>NotificationService</tt>. The event type is one of the static
     * constants defined in the <tt>NotificationManager</tt> class.
     * <p>
     * <b>Note</b>: The uses of the method at the time of this writing do not
     * take measures to stop looping sounds if the respective notifications use
     * them i.e. there is implicit agreement that the notifications fired
     * through the method do not loop sounds. Consequently, the method passes
     * arguments to <tt>NotificationService</tt> so that sounds are played once
     * only.
     * </p>
     *
     * @param eventType the event type for which we want to fire a notification
     */
    private static void fireNotification(String eventType)
    {
        NotificationService notificationService
            = NotificationWiringActivator.getNotificationService();

        if (notificationService != null)
            notificationService.fireNotification(eventType);
    }

    /**
     * Fires a notification for the given event type through the
     * <tt>NotificationService</tt>. The event type is one of the static
     * constants defined in the <tt>NotificationManager</tt> class.
     *
     * @param eventType the event type for which we want to fire a notification
     * @param loopCondition the method which will determine whether any sounds
     * played as part of the specified notification will continue looping
     * @return a reference to the fired notification to stop it.
     */
    private static NotificationData fireNotification(
            String eventType,
            Callable<Boolean> loopCondition)
    {
        return fireNotification(eventType, null, null, null, loopCondition);
    }

    /**
     * Fires a notification through the <tt>NotificationService</tt> with a
     * specific event type, a specific message title and a specific message.
     * <p>
     * <b>Note</b>: The uses of the method at the time of this writing do not
     * take measures to stop looping sounds if the respective notifications use
     * them i.e. there is implicit agreement that the notifications fired
     * through the method do not loop sounds. Consequently, the method passes
     * arguments to <tt>NotificationService</tt> so that sounds are played once
     * only.
     * </p>
     *
     * @param eventType the event type of the notification to be fired
     * @param messageTitle the title of the message to be displayed by the
     * notification to be fired if such a display is supported
     * @param message the message to be displayed by the notification to be
     * fired if such a display is supported
     */
    private static void fireNotification(
            String eventType,
            String messageTitle,
            String message)
    {
        NotificationService notificationService
            = NotificationWiringActivator.getNotificationService();

        if (notificationService != null)
        {
            notificationService.fireNotification(
                        eventType,
                        messageTitle,
                        message,
                        null);
        }
    }

    /**
     * Fires a message notification for the given event type through the
     * <tt>NotificationService</tt>.
     *
     * @param eventType the event type for which we fire a notification
     * @param messageTitle the title of the message
     * @param message the content of the message
     * @param cmdargs the value to be provided to
     * {@link CommandNotificationHandler#execute(CommandNotificationAction,
     * Map)} as the <tt>cmdargs</tt> argument
     * @param loopCondition the method which will determine whether any sounds
     * played as part of the specified notification will continue looping
     * @return a reference to the fired notification to stop it.
     */
    private static NotificationData fireNotification(
            String eventType,
            String messageTitle,
            String message,
            Map<String,String> cmdargs,
            Callable<Boolean> loopCondition)
    {
        NotificationService notificationService
            = NotificationWiringActivator.getNotificationService();

        if (notificationService == null)
            return null;
        else
        {
            Map<String,Object> extras = new HashMap<String,Object>();

            if (cmdargs != null)
            {
                extras.put(
                        NotificationData
                            .COMMAND_NOTIFICATION_HANDLER_CMDARGS_EXTRA,
                        cmdargs);
            }
            if (loopCondition != null)
            {
                extras.put(
                        NotificationData
                            .SOUND_NOTIFICATION_HANDLER_LOOP_CONDITION_EXTRA,
                        loopCondition);
            }
            return
                notificationService.fireNotification(
                        eventType,
                        messageTitle,
                        message,
                        null,
                        extras);
        }
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
     * Determines whether the <tt>DIALING</tt> sound notification should be
     * played for a specific <tt>CallPeer</tt>.
     *
     * @param weakPeer the <tt>CallPeer</tt> for which it is to be determined
     * whether the <tt>DIALING</tt> sound notification is to be played
     * @return <tt>true</tt> if the <tt>DIALING</tt> sound notification should
     * be played for the specified <tt>callPeer</tt>; otherwise, <tt>false</tt>
     */
    private static boolean shouldPlayDialingSound(
            WeakReference<CallPeer> weakPeer)
    {
        CallPeer peer = weakPeer.get();

        if (peer == null)
            return false;

        Call call = peer.getCall();

        if (call == null)
            return false;

        CallConference conference = call.getConference();

        if (conference == null)
            return false;

        boolean play = false;

        for (Call aCall : conference.getCalls())
        {
            Iterator<? extends CallPeer> peerIter = aCall.getCallPeers();

            while (peerIter.hasNext())
            {
                CallPeer aPeer = peerIter.next();

                /*
                 * The peer is still in a call/telephony conference so the
                 * DIALING sound may need to be played.
                 */
                if (peer == aPeer)
                    play = true;

                CallPeerState state = peer.getState();

                if (CallPeerState.INITIATING_CALL.equals(state)
                        || CallPeerState.CONNECTING.equals(state))
                {
                    /*
                     * The DIALING sound should be played for the first CallPeer
                     * only.
                     */
                    if (peer != aPeer)
                        return false;
                }
                else
                {
                    /*
                     * The DIALING sound should not be played if there is a
                     * CallPeer which does not require the DIALING sound to be
                     * played.
                     */
                    return false;
                }
            }
        }

        return play;
    }

    /**
     * Stores notification references to stop them if a notification has expired
     * (e.g. to stop the dialing sound).
     */
    private final Map<Call, NotificationData> callNotifications
        = new WeakHashMap<Call, NotificationData>();

    /**
     * The pseudo timer which is used to delay multiple typing notifications
     * before receiving the message.
     */
    private final Map<Contact, Long> proactiveTimer
        = new HashMap<Contact, Long>();

    /**
     * Implements CallListener.callEnded. Stops sounds that are playing at
     * the moment if there're any.
     *
     * @param ev the <tt>CallEvent</tt>
     */
    public void callEnded(CallEvent ev)
    {
        try
        {
            // Stop all telephony related sounds.
//            stopAllTelephonySounds();
            NotificationData notification
                = callNotifications.get(ev.getSourceCall());

            if (notification != null)
                stopSound(notification);

            // Play the hangup sound.
            fireNotification(HANG_UP);
        }
        catch(Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
            {
                logger.error(
                        "An error occurred while trying to notify"
                            + " about the end of a call.",
                        t);
            }
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
     * {@inheritDoc}
     *
     * Not used.
     */
    public void callStateChanged(CallChangeEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void conferenceFocusChanged(CallPeerConferenceEvent ev) {}

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
            CallPeer peer
                = conferenceEvent
                    .getConferenceMember()
                        .getConferenceFocusCallPeer();

            if(peer.getConferenceMemberCount() > 0)
            {
                CallPeerSecurityStatusEvent securityEvent
                    = peer.getCurrentSecuritySettings();

                if (securityEvent instanceof CallPeerSecurityOnEvent)
                    fireNotification(CALL_SECURITY_ON);
            }
        }
        catch(Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
                logger.error("Error notifying for secured call member", t);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void conferenceMemberErrorReceived(CallPeerConferenceEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void conferenceMemberRemoved(CallPeerConferenceEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void fileTransferCreated(FileTransferCreatedEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void fileTransferRequestCanceled(FileTransferRequestEvent ev) {}

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
                    request.getFileName(),
                    request.getID());
        }
        catch(Throwable t)
        {
            logger.error("Error notifying for file transfer req received", t);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void fileTransferRequestRejected(FileTransferRequestEvent ev) {}

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
     * Implements CallListener.incomingCallReceived. When a call is received
     * plays the ring phone sound to the user and gathers caller information
     * that may be used by a user-specified command (incomingCall event
     * trigger).
     *
     * @param ev the <tt>CallEvent</tt>
     */
    public void incomingCallReceived(CallEvent ev)
    {
        try
        {
            Call call = ev.getSourceCall();
            CallPeer peer = call.getCallPeers().next();
            Map<String,String> peerInfo = new HashMap<String, String>();
            String peerName = peer.getDisplayName();

            peerInfo.put("caller.uri", peer.getURI());
            peerInfo.put("caller.address", peer.getAddress());
            peerInfo.put("caller.name", peerName);
            peerInfo.put("caller.id", peer.getPeerID());

            /*
             * The loopCondition will stay with the notification sound until the
             * latter is stopped. If by any chance the sound fails to stop by
             * the time the call is no longer referenced, do try to stop it
             * then. That's why the loopCondition will weakly reference the
             * call.
             */
            final WeakReference<Call> weakCall = new WeakReference<Call>(call);
            NotificationData notification
                = fireNotification(
                        INCOMING_CALL,
                        "",
                        ev.isDesktopStreaming() ?
                            NotificationWiringActivator.getResources()
                                    .getI18NString(
                                            "service.gui.INCOMING_SCREEN_SHARE",
                                            new String[] { peerName })
                            : NotificationWiringActivator.getResources()
                                    .getI18NString(
                                            "service.gui.INCOMING_CALL",
                                            new String[] { peerName }),
                        peerInfo,
                        new Callable<Boolean>()
                        {
                            public Boolean call()
                            {
                                Call call = weakCall.get();

                                if (call == null)
                                    return false;

                                /*
                                 * INCOMING_CALL should be played for a Call
                                 * only while there is a CallPeer in the
                                 * INCOMING_CALL state.
                                 */
                                Iterator<? extends CallPeer> peerIter
                                    = call.getCallPeers();
                                boolean loop = false;

                                while (peerIter.hasNext())
                                {
                                    CallPeer peer = peerIter.next();

                                    if (CallPeerState.INCOMING_CALL.equals(
                                            peer.getState()))
                                    {
                                        loop = true;
                                        break;
                                    }
                                }
                                return loop;
                            }
                        });

            if (notification != null)
                callNotifications.put(call, notification);

            call.addCallChangeListener(this);

            peer.addCallPeerListener(this);
            peer.addCallPeerSecurityListener(this);
            peer.addCallPeerConferenceListener(this);
        }
        catch(Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
            {
                logger.error(
                        "An error occurred while trying to notify"
                            + " about an incoming call",
                        t);
            }
        }
    }

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
     * {@inheritDoc}
     *
     * Not used.
     */
    public void messageDelivered(AdHocChatRoomMessageDeliveredEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void messageDelivered(ChatRoomMessageDeliveredEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used
     */
    public void messageDelivered(MessageDeliveredEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void messageDeliveryFailed(
            AdHocChatRoomMessageDeliveryFailedEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void messageDeliveryFailed(ChatRoomMessageDeliveryFailedEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void messageDeliveryFailed(MessageDeliveryFailedEvent ev) {}

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
                final String htmlContent;
                if (HTML_CONTENT_TYPE.equals(evt.getMessage().getContentType()))
                {
                    htmlContent = messageContent;
                }
                else
                {
                    htmlContent = StringEscapeUtils.escapeHtml4(messageContent);
                }
                fireChatNotification(
                    sourceChatRoom,
                    INCOMING_MESSAGE,
                    title,
                    htmlContent,
                    evt.getMessage().getMessageUID());
            }
        }
        catch(Throwable t)
        {
            logger.error("Error notifying for adhoc message received", t);
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

            final Message sourceMsg = evt.getMessage();
            String messageContent = sourceMsg.getContent();

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
                final String htmlContent;
                if (HTML_CONTENT_TYPE.equals(sourceMsg.getContentType()))
                {
                    htmlContent = messageContent;
                }
                else
                {
                    htmlContent = StringEscapeUtils.escapeHtml4(messageContent);
                }
                fireChatNotification(
                        sourceChatRoom,
                        INCOMING_MESSAGE,
                        title,
                        htmlContent,
                        sourceMsg.getMessageUID());
            }
        }
        catch(Throwable t)
        {
            logger.error("Error notifying for chat room message received", t);
        }
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

            final Message sourceMsg = evt.getSourceMessage();
            final String htmlContent;
            if (HTML_CONTENT_TYPE.equals(sourceMsg.getContentType()))
            {
                htmlContent = sourceMsg.getContent();
            }
            else
            {
                htmlContent =
                    StringEscapeUtils.escapeHtml4(sourceMsg.getContent());
            }
            fireChatNotification(
                    evt.getSourceContact(),
                    INCOMING_MESSAGE,
                    title,
                    htmlContent,
                    sourceMsg.getMessageUID());
        }
        catch(Throwable t)
        {
            logger.error("Error notifying for message received", t);
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
     * {@inheritDoc}
     *
     * Not used.
     */
    public void peerAddressChanged(CallPeerChangeEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void peerDisplayNameChanged(CallPeerChangeEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void peerImageChanged(CallPeerChangeEvent ev) {}

    /**
     * Fired when peer's state is changed
     *
     * @param ev fired CallPeerEvent
     */
    public void peerStateChanged(CallPeerChangeEvent ev)
    {
        try
        {
            CallPeer peer = ev.getSourceCallPeer();
            Call call = peer.getCall();
            CallPeerState newState = (CallPeerState) ev.getNewValue();
            CallPeerState oldState = (CallPeerState) ev.getOldValue();

            // Play the dialing audio when in connecting and initiating call state.
            // Stop the dialing audio when we enter any other state.
            if ((newState == CallPeerState.INITIATING_CALL)
                    || (newState == CallPeerState.CONNECTING))
            {
                /*
                 * The loopCondition will stay with the notification sound until
                 * the latter is stopped. If by any chance the sound fails to
                 * stop by the time the peer is no longer referenced, do try to
                 * stop it then. That's why the loopCondition will weakly
                 * reference the peer.
                 */
                final WeakReference<CallPeer> weakPeer
                    = new WeakReference<CallPeer>(peer);

                /* We want to play the dialing once for multiple CallPeers. */
                if (shouldPlayDialingSound(weakPeer))
                {
                    NotificationData notification
                        = fireNotification(
                                DIALING,
                                new Callable<Boolean>()
                                {
                                    public Boolean call()
                                    {
                                        return shouldPlayDialingSound(weakPeer);
                                    }
                                });

                    if (notification != null)
                        callNotifications.put(call, notification);
                }
            }
            else
            {
                NotificationData notification = callNotifications.get(call);

                if (notification != null)
                    stopSound(notification);
            }

            if (newState == CallPeerState.ALERTING_REMOTE_SIDE
                //if we were already in state CONNECTING_WITH_EARLY_MEDIA the server
                //is already taking care of playing the notifications so we don't
                //need to fire a notification here.
                && oldState != CallPeerState.CONNECTING_WITH_EARLY_MEDIA)
            {
                final WeakReference<CallPeer> weakPeer
                    = new WeakReference<CallPeer>(peer);
                NotificationData notification
                    = fireNotification(
                            OUTGOING_CALL,
                            new Callable<Boolean>()
                            {
                                public Boolean call()
                                {
                                    CallPeer peer = weakPeer.get();

                                    return
                                        (peer != null)
                                            && CallPeerState
                                                .ALERTING_REMOTE_SIDE
                                                    .equals(peer.getState());
                                }
                            });

                if (notification != null)
                    callNotifications.put(call, notification);
            }
            else if (newState == CallPeerState.BUSY)
            {
                // We start the busy sound only if we're in a simple call.
                if (!isConference(call))
                {
                    final WeakReference<CallPeer> weakPeer
                        = new WeakReference<CallPeer>(peer);
                    NotificationData notification
                        = fireNotification(
                                BUSY_CALL,
                                new Callable<Boolean>()
                                {
                                    public Boolean call()
                                    {
                                        CallPeer peer = weakPeer.get();

                                        return
                                            (peer != null)
                                                && CallPeerState.BUSY.equals(
                                                        peer.getState());
                                    }
                                });

                    if (notification != null)
                        callNotifications.put(call, notification);
                }
            }
            else if ((newState == CallPeerState.DISCONNECTED)
                    || (newState == CallPeerState.FAILED))
            {
                fireNotification(HANG_UP);
            }
        }
        catch(Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
            {
                logger.error(
                        "An error occurred while trying to notify"
                            + " about a change in the state of a call peer.",
                        t);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void peerTransportAddressChanged(CallPeerChangeEvent ev) {}

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
            ResourceManagementService resources
                = NotificationWiringActivator.getResources();

            fireNotification(
                    CALL_SAVED,
                    resources.getI18NString(
                            "plugin.callrecordingconfig.CALL_SAVED"),
                    resources.getI18NString(
                            "plugin.callrecordingconfig.CALL_SAVED_TO",
                            new String[] { recorder.getFilename() }));
        }
        catch(Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
            {
                logger.error(
                        "An error occurred while trying to notify that"
                            + " the recording of a call has stopped.",
                        t);
            }
        }
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
                new SoundNotificationAction(
                    SoundProperties.INCOMING_MESSAGE, -1, true, false, false));

        // Register incoming call notifications.
        notificationService.registerDefaultNotificationForEvent(
                INCOMING_CALL,
                NotificationAction.ACTION_POPUP_MESSAGE,
                null,
                null);

        SoundNotificationAction inCallSoundHandler
            = new SoundNotificationAction(
                    SoundProperties.INCOMING_CALL, 2000, true, true, true);

        notificationService.registerDefaultNotificationForEvent(
                INCOMING_CALL,
                inCallSoundHandler);

        // Register outgoing call notifications.
        notificationService.registerDefaultNotificationForEvent(
                OUTGOING_CALL,
                new SoundNotificationAction(
                        SoundProperties.OUTGOING_CALL,
                        3000,
                        false, true, false));

        // Register busy call notifications.
        notificationService.registerDefaultNotificationForEvent(
                BUSY_CALL,
                new SoundNotificationAction(
                        SoundProperties.BUSY,
                        1,
                        false, true, false));

        // Register dial notifications.
        notificationService.registerDefaultNotificationForEvent(
                DIALING,
                new SoundNotificationAction(
                        SoundProperties.DIALING,
                        -1,
                        false, true, false));

        // Register the hangup sound notification.
        notificationService.registerDefaultNotificationForEvent(
                HANG_UP,
                new SoundNotificationAction(
                        SoundProperties.HANG_UP,
                        -1,
                        false, true, false));

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
                new SoundNotificationAction(
                        SoundProperties.CALL_SECURITY_ON, -1,
                        false, true, false));

        // Register sound notification for security state off during a call.
        notificationService.registerDefaultNotificationForEvent(
                CALL_SECURITY_ERROR,
                new SoundNotificationAction(
                        SoundProperties.CALL_SECURITY_ERROR, -1,
                        false, true, false));

        // Register sound notification for incoming files.
        notificationService.registerDefaultNotificationForEvent(
                INCOMING_FILE,
                NotificationAction.ACTION_POPUP_MESSAGE,
                null,
                null);

        notificationService.registerDefaultNotificationForEvent(
                INCOMING_FILE,
                new SoundNotificationAction(
                        SoundProperties.INCOMING_FILE, -1,
                        true, false, false));

        // Register notification for saved calls.
        notificationService.registerDefaultNotificationForEvent(
            CALL_SAVED,
            NotificationAction.ACTION_POPUP_MESSAGE,
            null,
            null);
    }

    /**
     * Processes the received security message.
     * @param ev the event we received
     */
    public void securityMessageRecieved(CallPeerSecurityMessageEvent ev)
    {
        try
        {
            String messageTitleKey;

            switch (ev.getEventSeverity())
            {
            // Don't play alert sound for Info or warning.
            case CallPeerSecurityMessageEvent.INFORMATION:
                messageTitleKey = "service.gui.SECURITY_INFO";
                break;

            case CallPeerSecurityMessageEvent.WARNING:
                messageTitleKey = "service.gui.SECURITY_WARNING";
                break;

            // Security cannot be established! Play an alert sound.
            case CallPeerSecurityMessageEvent.SEVERE:
            case CallPeerSecurityMessageEvent.ERROR:
                messageTitleKey = "service.gui.SECURITY_ERROR";
                fireNotification(CALL_SECURITY_ERROR);
                break;

            default:
                /*
                 * Whatever other severity there is or will be, we do not how to
                 * react to it yet.
                 */
                messageTitleKey = null;
            }

            if (messageTitleKey != null)
            {
                fireNotification(
                        SECURITY_MESSAGE,
                        NotificationWiringActivator.getResources()
                                .getI18NString(messageTitleKey),
                        ev.getI18nMessage());
            }
        }
        catch(Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
            {
                logger.error(
                        "An error occurred while trying to notify"
                            + " about a security message",
                        t);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void securityNegotiationStarted(
            CallPeerSecurityNegotiationStartedEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void securityOff(CallPeerSecurityOffEvent ev) {}

    /**
     * When a <tt>securityOnEvent</tt> is received.
     * @param ev the event we received
     */
    public void securityOn(CallPeerSecurityOnEvent ev)
    {
        try
        {
            SrtpControl securityController = ev.getSecurityController();
            CallPeer peer = (CallPeer) ev.getSource();

            if(!securityController.requiresSecureSignalingTransport()
                    || peer.getProtocolProvider().isSignalingTransportSecure())
            {
                fireNotification(CALL_SECURITY_ON);
            }
        }
        catch(Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
            {
                logger.error(
                        "An error occurred while trying to notify"
                            + " about a security-related event",
                        t);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void securityTimeout(CallPeerSecurityTimeoutEvent ev) {}

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
            return;

        Object service
            = NotificationWiringActivator.bundleContext.getService(serviceRef);

        // we don't care if the source service is not a protocol provider
        if (service instanceof ProtocolProviderService)
        {
            switch (event.getType())
            {
                case ServiceEvent.REGISTERED:
                    handleProviderAdded((ProtocolProviderService) service);
                    break;
                case ServiceEvent.UNREGISTERING:
                    handleProviderRemoved((ProtocolProviderService) service);
                    break;
            }
        }
    }

    /**
     * Stops all sounds for the given event type.
     *
     * @param data the event type for which we should stop sounds. One of
     * the static event types defined in this class.
     */
    private void stopSound(NotificationData data)
    {
        if (data == null)
            return;

        try
        {
            NotificationService notificationService
                = NotificationWiringActivator.getNotificationService();

            if(notificationService != null)
                notificationService.stopNotification(data);
        }
        finally
        {
            /*
             * The field callNotifications associates a Call with a
             * NotificationData for the purposes of the stopSound method so the
             * stopSound method should dissociate them upon stopping a specific
             * NotificationData.
             */
            Iterator<Map.Entry<Call, NotificationData>> i
                = callNotifications.entrySet().iterator();

            while (i.hasNext())
            {
                Map.Entry<Call, NotificationData> e = i.next();

                if (data.equals(e.getValue()))
                    i.remove();
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void typingNotificationDeliveryFailed(TypingNotificationEvent ev) {}

    /**
     * Informs the user what is the typing state of his chat contacts.
     *
     * @param ev the event containing details on the typing notification
     */
    public void typingNotificationReceived(TypingNotificationEvent ev)
    {
        try
        {
            Contact contact = ev.getSourceContact();

            // we don't care for proactive notifications, different than typing
            // sometimes after closing chat we can see someone is typing us
            // its just server sanding that the chat is inactive (STATE_STOPPED)
            if(ev.getTypingState()
                    != OperationSetTypingNotifications.STATE_TYPING)
            {
                return;
            }

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

                    if((metaContact != null)
                            && metaContact.containsContact(contact)
                            && chat.isChatFocused())
                    {
                        return;
                    }
                }
            }

            long currentTime = System.currentTimeMillis();

            if (proactiveTimer.size() > 0)
            {
                // first remove contacts that have been here longer than the
                // timeout to avoid memory leaks
                Iterator<Map.Entry<Contact, Long>> entries
                    = proactiveTimer.entrySet().iterator();

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
                if (proactiveTimer.containsKey(contact))
                {
                    // We already notified the others about this
                    return;
                }
            }

            proactiveTimer.put(contact, currentTime);

            fireChatNotification(
                    contact,
                    PROACTIVE_NOTIFICATION,
                    contact.getDisplayName(),
                    NotificationWiringActivator.getResources().getI18NString(
                            "service.gui.PROACTIVE_NOTIFICATION"),
                    null);
        }
        catch(Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
            {
                logger.error(
                        "An error occurred while handling"
                            + " a typing notification.",
                        t);
            }
        }
    }
}
