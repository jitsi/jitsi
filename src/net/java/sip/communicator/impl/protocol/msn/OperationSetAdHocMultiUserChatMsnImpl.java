/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.sf.jml.*;
import net.sf.jml.event.*;
import net.sf.jml.message.*;

/**
 * A MSN implementation of the ad-hoc multi user chat operation set.
 *
 * @author Valentin Martinet
 */
public class OperationSetAdHocMultiUserChatMsnImpl
    implements  OperationSetAdHocMultiUserChat
{
    /**
     * The logger used to log messages.
     */
     private static final Logger logger
         = Logger.getLogger(OperationSetAdHocMultiUserChatMsnImpl.class);

    /**
     * Listeners that will be notified of changes in our status in the room.
     */
    private Vector<LocalUserAdHocChatRoomPresenceListener> presenceListeners
        = new Vector<LocalUserAdHocChatRoomPresenceListener>();

    /**
     * The currently valid MSN protocol provider service implementation.
     */
    private ProtocolProviderServiceMsnImpl provider = null;

    /**
     * The ad-hoc rooms we currently are in.
     */
    private Hashtable<MsnSwitchboard, AdHocChatRoomMsnImpl> adHocChatRoomCache
                        = new Hashtable<MsnSwitchboard, AdHocChatRoomMsnImpl>();

    /**
     * A list of listeners subscribed for invitations multi user chat events.
     */
    private Vector<AdHocChatRoomInvitationListener> invitationListeners
            = new Vector<AdHocChatRoomInvitationListener>();

    /**
     * A list of listeners subscribed for events indicating rejection of a multi
     * user chat invitation sent by us.
     */
    private Vector<AdHocChatRoomInvitationRejectionListener>
        invitationRejectionListeners
            = new Vector<AdHocChatRoomInvitationRejectionListener>();

    /**
     * A list of the ad-hoc rooms that are currently open and created by this
     * account.
     */
    private Hashtable<Object, AdHocChatRoom> pendingAdHocChatRoomList
        = new Hashtable<Object, AdHocChatRoom>();

    /**
     * Creates an <tt>OperationSetAdHocMultiUserChatMsnImpl</tt> by specifying
     * the parent provider.
     * @param provider the Msn provider
     */
    public OperationSetAdHocMultiUserChatMsnImpl(
            ProtocolProviderServiceMsnImpl provider)
    {
        this.provider = provider;
        this.provider.addRegistrationStateChangeListener(
                new RegistrationStateListener());
    }

    /**
     * Adds the given presence listener to existing presence listeners list.
     *
     * @param listener the listener to add
     */
    public void addPresenceListener(
            LocalUserAdHocChatRoomPresenceListener listener)
    {
        synchronized(presenceListeners)
        {
            if(!presenceListeners.contains(listener))
                presenceListeners.add(listener);
        }
    }

     /**
     * Adds a listener to invitation notifications.
     *
     * @param listener an invitation listener.
     */
    public void addInvitationListener(AdHocChatRoomInvitationListener listener)
    {
        synchronized (invitationListeners)
        {
            if (!invitationListeners.contains(listener))
                invitationListeners.add(listener);
        }
    }

    /**
     * Removes <tt>listener</tt> from the list of invitation listeners
     * registered to receive invitation events.
     *
     * @param listener the invitation listener to remove.
     */
    public void removeInvitationListener(
            AdHocChatRoomInvitationListener listener)
    {
        synchronized (invitationListeners)
        {
            invitationListeners.remove(listener);
        }
    }

    /**
     * Adds a listener to invitation notifications.
     *
     * @param listener an invitation listener.
     */
    public void addInvitationRejectionListener(
        AdHocChatRoomInvitationRejectionListener listener)
    {
        synchronized (invitationRejectionListeners)
        {
            if (!invitationRejectionListeners.contains(listener))
                invitationRejectionListeners.add(listener);
        }
    }

    /**
     * Removes <tt>listener</tt> from the list of invitation listeners
     * registered to receive invitation events.
     *
     * @param listener the invitation listener to remove.
     */
    public void removeInvitationRejectionListener(
        AdHocChatRoomInvitationRejectionListener listener)
    {
        synchronized (invitationRejectionListeners)
        {
            invitationRejectionListeners.remove(listener);
        }
    }

    /**
     * Creates a message by a given message text.
     *
     * @param messageText The message text.
     * @return the newly created message.
     */
    public Message createMessage(String messageText)
    {
        return new MessageMsnImpl(messageText,
            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE,
            OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING, null);
    }

    /**
     * Creates an ad-hoc room with the named <tt>adHocRoomName</tt> and in
     * including to the specified <tt>contacts</tt>.
     *
     * @param adHocRoomName the name of the ad-hoc room
     * @param contacts the list of contacts
     *
     * @return the ad-hoc room that has been just created
     * @throws OperationFailedException
     * @throws OperationNotSupportedException
     */
    public AdHocChatRoom createAdHocChatRoom(   String adHocRoomName,
                                                List<Contact> contacts)
        throws OperationFailedException, OperationNotSupportedException
    {
        AdHocChatRoom adHocChatRoom = createAdHocChatRoom(
                        adHocRoomName, new Hashtable<String, Object>());

        if (adHocChatRoom != null && contacts != null)
        {
            for (Contact contact : contacts)
            {
                ContactMsnImpl newContact = (ContactMsnImpl) contact;

                adHocChatRoom.invite(newContact.getAddress(), "");
            }
        }

        return adHocChatRoom;
    }

    /**
     * Creates an <tt>AdHocChatRoom</tt> whose name is _adHocRoomName with the
     * properties contained in _adHocRoomProperties
     *
     * @param adHocRoomName the name of the ad-hoc room
     * @param adHocRoomProperties the ad-hoc room's properties
     *
     * @return the created ad-hoc room
     * @throws OperationFailedException
     * @throws OperationNotSupportedException
     */
    public AdHocChatRoom createAdHocChatRoom(String adHocRoomName,
            Map<String, Object> adHocRoomProperties)
            throws OperationFailedException, OperationNotSupportedException
    {
        AdHocChatRoom adHocRoom = adHocChatRoomCache.get(adHocRoomName);

        if (adHocRoom == null)
        {
            assertConnected();

            // we create an identifier object and create a new switchboard
            // we need to track this object to identify this chatRoom
            Object id = new Object();
            this.provider.getMessenger().newSwitchboard(id);

            // when the room hasn't been created, we create it.
            adHocRoom = createLocalAdHocChatRoomInstance(adHocRoomName, id);
        }

        return adHocRoom;
    }

    /**
     * Creates a <tt>ChatRoom</tt> from the specified chatRoomName.
     *
     * @param adHocChatRoomName the specific ad-hoc chat room name.
     * @param switchboardId the identifier of the switchboard
     * @return the ad-hoc chat room that we've just created.
     */
    private AdHocChatRoomMsnImpl createLocalAdHocChatRoomInstance(
            String adHocChatRoomName, Object switchboardId)
    {
        synchronized (this.pendingAdHocChatRoomList)
        {
            AdHocChatRoomMsnImpl adHocChatRoom =
                new AdHocChatRoomMsnImpl(adHocChatRoomName, this.provider);

            // We put it to the pending ad hoc chat rooms, waiting for the
            // switchboard to be created.
            this.pendingAdHocChatRoomList.put(switchboardId, adHocChatRoom);

            adHocChatRoom.join();

            return adHocChatRoom;
        }
    }

    /**
     * Returns the <tt>AdHocChatRoomMsnImpl</tt> corresponding to the given
     * <tt>switchboard</tt>, if one exists, otherwise returns null.
     *
     * @param switchboard the Msn switchboard corresponding to a chat room
     *
     * @return the <tt>AdHocChatRoomMsnImpl</tt> corresponding to the given
     * <tt>switchboard</tt>, otherwise null
     */
    private AdHocChatRoomMsnImpl getLocalAdHocChatRoomInstance(
            MsnSwitchboard switchboard)
    {
        AdHocChatRoomMsnImpl adHocRoom = (AdHocChatRoomMsnImpl)
            this.adHocChatRoomCache.get(switchboard);

        return adHocRoom;
    }

    /**
     * Creates an <tt>AdHocChatRoomMsnImpl</tt> corresponding to the given
     * <tt>switchboard</tt>.
     * @param switchboard  the Msn switchboard that will correspond to the
     * created chat room
     * @return  an <tt>AdHocChatRoomMsnImpl</tt> corresponding to the given
     * <tt>switchboard</tt>
     */
    private AdHocChatRoomMsnImpl createLocalAdHocChatRoomInstance(
            MsnSwitchboard switchboard)
    {
        AdHocChatRoomMsnImpl adHocChatRoom = (AdHocChatRoomMsnImpl)
            this.adHocChatRoomCache.get(switchboard);

        if (adHocChatRoom == null)
        {
            String name = String.valueOf(switchboard.hashCode());
            adHocChatRoom
                = new AdHocChatRoomMsnImpl(name, provider, switchboard);

            this.adHocChatRoomCache.put(switchboard, adHocChatRoom);

            Object attachment = switchboard.getAttachment();
            if (attachment != null && pendingAdHocChatRoomList
                    .containsKey(attachment))
            {
                pendingAdHocChatRoomList.remove(attachment);
            }
        }
        adHocChatRoom.join();

        return adHocChatRoom;
    }

    /**
     * Delivers a <tt>LocalUserAdHocChatRoomPresenceChangeEvent</tt> to all
     * registered <tt>LocalUserAdHocChatRoomPresenceListener</tt>s.
     *
     * @param adHocChatRoom the <tt>AdHocChatRoom</tt> which has been joined,
     * left, etc.
     * @param eventType the type of this event; one of LOCAL_USER_JOINED,
     *            LOCAL_USER_LEFT, etc.
     * @param reason the reason
     */
    public void fireLocalUserPresenceEvent( AdHocChatRoom   adHocChatRoom,
                                            String          eventType,
                                            String          reason)
    {
        LocalUserAdHocChatRoomPresenceChangeEvent evt
            = new LocalUserAdHocChatRoomPresenceChangeEvent(this,
                                                            adHocChatRoom,
                                                            eventType,
                                                            reason);

        Iterator<LocalUserAdHocChatRoomPresenceListener> listeners = null;
        synchronized(this.presenceListeners)
        {
            listeners = new ArrayList<LocalUserAdHocChatRoomPresenceListener>
                            (this.presenceListeners).iterator();
        }

        while (listeners.hasNext())
        {
             LocalUserAdHocChatRoomPresenceListener listener = listeners.next();

            listener.localUserAdHocPresenceChanged(evt);
        }
    }

    /**
     * Checks if an incoming message is a multi user chat message. This is done
     * by the switchboard, if it is not created by the user, its an active file
     * transfer switchboard or the user count is too low then this method return
     * false.
     *
     * @param switchboard The corresponding MSNswitchboard.
     * @return true if it is a group chat message or false in the other case.
     */
    public boolean isGroupChatMessage(MsnSwitchboard switchboard)
    {
        if (getLocalAdHocChatRoomInstance(switchboard) != null)
            return true;
        else
        {
            Object attachment = switchboard.getAttachment();

            if (attachment != null)
            {
                return pendingAdHocChatRoomList.containsKey(attachment);
            }
            else
            {   // the user did not created the chat room by him/her self,
                // the only way to figure out if this is a group chat message
                // is to check the user count
                return (switchboard.getAllContacts().length > 1);
            }
        }
    }

    /**
     * Removes the given listener from presence listeners' list.
     *
     * @param listener the listener to remove
     */
    public void removePresenceListener(
            LocalUserAdHocChatRoomPresenceListener listener)
    {
        synchronized (this.presenceListeners)
        {
            if(this.presenceListeners.contains(listener))
            {
                this.presenceListeners.remove(listener);
            }
        }
    }

    /**
     * Makes sure that we are properly connected.
     *
     * @throws OperationFailedException if the provider is not connected.
     * @throws OperationNotSupportedException if the service is not supported by
     *             the server.
     */
    private void assertConnected()
        throws OperationFailedException,
        OperationNotSupportedException
    {
        if (this.provider == null)
            throw new IllegalStateException(
                "The provider must be non-null and signed on the "
                    + "service before being able to communicate.");
        if (!this.provider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the service before "
                    + "being able to communicate.");
    }

    /**
     * Our listener that will tell us when we're registered to msn.
     */
    private class RegistrationStateListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenever a
         * change in the registration state of the corresponding provider had
         * occurred.
         *
         * @param evt ProviderStatusChangeEvent the event describing the status
         *            change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                provider.getMessenger().addSwitchboardListener(
                    new MsnSwitchboardListener());
                provider.getMessenger().addMessageListener(
                    new MsnMessageListener());
            }
        }

    }

    /**
     * Our group chat message listener, it extends the MsnMessageAdapter from
     * the the jml library.
     */
    private class MsnMessageListener
        extends MsnMessageAdapter
        implements MsnEmailListener
    {
        /**
         * Indicates that an instant message has been received.
         * @param switchboard the switchboard
         * @param message the message
         * @param contact the contact sending the message
         */
        public void instantMessageReceived( MsnSwitchboard switchboard,
                                            MsnInstantMessage message,
                                            MsnContact contact)
        {
            if (!isGroupChatMessage(switchboard))
                return;

            Message newMessage = createMessage(message.getContent());

            logger.debug("Group chat message received.");

            AdHocChatRoomMsnImpl chatRoom
                = getLocalAdHocChatRoomInstance(switchboard);

            if (chatRoom == null)
            {
                chatRoom = createLocalAdHocChatRoomInstance(switchboard);
            }

            Contact participant =
                chatRoom.getAdHocChatRoomParticipant(contact.getId());

            AdHocChatRoomMessageReceivedEvent msgReceivedEvent =
                new AdHocChatRoomMessageReceivedEvent(
                    chatRoom,
                    participant,
                    System.currentTimeMillis(),
                    newMessage,
                    AdHocChatRoomMessageReceivedEvent
                        .CONVERSATION_MESSAGE_RECEIVED);

            chatRoom.fireMessageEvent(msgReceivedEvent);
        }

        /**
         * Not interested in this event.
         */
        public void initialEmailNotificationReceived(
            MsnSwitchboard switchboard, MsnEmailInitMessage message,
            MsnContact contact)
        {}

        /**
         * Not interested in this event.
         */
        public void initialEmailDataReceived(MsnSwitchboard switchboard,
            MsnEmailInitEmailData message, MsnContact contact)
        {}

        /**
         * Not interested in this event.
         */
        public void newEmailNotificationReceived(MsnSwitchboard switchboard,
            MsnEmailNotifyMessage message, MsnContact contact)
        {}

        /**
         * Not interested in this event.
         */
        public void activityEmailNotificationReceived(
            MsnSwitchboard switchboard, MsnEmailActivityMessage message,
            MsnContact contact)
        {}
    }

    /**
     * The Switchboard Listener, listens to all four switchboard events:
     * Switchboard started/closed and User joins/left.
     *
     */
    private class MsnSwitchboardListener
        extends MsnSwitchboardAdapter
    {
        /**
         * Indicates that the given <tt>msnContact</tt> has joined the given
         * <tt>switchboard</tt>.
         * @param switchboard the switchboard
         * @param msnContact the contact that has joined
         */
        public void contactJoinSwitchboard( MsnSwitchboard switchboard,
                                            MsnContact msnContact)
        {
            if (!isGroupChatMessage(switchboard))
                return;

            try
            {
                AdHocChatRoomMsnImpl chatRoom
                    = getLocalAdHocChatRoomInstance(switchboard);

                if (chatRoom == null)
                {
                    chatRoom = createLocalAdHocChatRoomInstance(switchboard);
                }

                OperationSetPersistentPresenceMsnImpl presenceOpSet
                    = (OperationSetPersistentPresenceMsnImpl) provider
                        .getOperationSet(OperationSetPersistentPresence.class);

                ContactMsnImpl contact
                    = presenceOpSet.getServerStoredContactList()
                        .findContactById(
                            msnContact.getEmail().getEmailAddress());

                if (contact == null)
                    contact = new ContactMsnImpl(
                                    msnContact,
                                    presenceOpSet.getServerStoredContactList(),
                                    false,
                                    false);

                chatRoom.addAdHocChatRoomParticipant(   msnContact.getId(),
                                                        contact);
            }
            catch (Exception e)
            {
                logger.error("Failed to join switchboard.", e);
            }
        }

        /**
         * Indicates that the given <tt>contact</tt> has left the given
         * <tt>switchboard</tt>.
         * @param switchboard the switchboard
         * @param contact the contact that has left
         */
        public void contactLeaveSwitchboard(MsnSwitchboard switchboard,
                                            MsnContact contact)
        {
            logger
                .debug(contact.getDisplayName() + " has left the Switchboard");

            AdHocChatRoomMsnImpl chatRoom
                = getLocalAdHocChatRoomInstance(switchboard);

            if (chatRoom == null)
                return;

            String participantId = contact.getId();

            Contact participant
                = chatRoom.getAdHocChatRoomParticipant(participantId);

            if (participant != null)
            {
                chatRoom.removeParticipant(participantId);
            }
        }

        /**
         * Indicates that a switchboard has been closed.
         * @param switchboard the switchboard that has been closed
         */
        public void switchboardClosed(MsnSwitchboard switchboard)
        {
            AdHocChatRoomMsnImpl adHocChatRoom
                = getLocalAdHocChatRoomInstance(switchboard);

            if (adHocChatRoom == null)
                return;
            else
            {
                adHocChatRoom.setSwitchboard(null);

                adHocChatRoom.leave();
                fireLocalUserPresenceEvent(adHocChatRoom,
                    LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_DROPPED ,
                     "Switchboard closed.");
            }
        }

        /**
         * Indicates that a switchboard has been started.
         * @param switchboard the switchboard that has been started
         */
        public void switchboardStarted(MsnSwitchboard switchboard)
        {
            Object switchboardID = switchboard.getAttachment();

            AdHocChatRoomMsnImpl adHocChatRoom = null;
            if (switchboardID != null
                    && pendingAdHocChatRoomList.containsKey(switchboardID))
            {
                adHocChatRoom
                    = (AdHocChatRoomMsnImpl) pendingAdHocChatRoomList
                        .get(switchboardID);

                // Remove this room from the list of pending chat rooms.
                pendingAdHocChatRoomList.remove(switchboardID);

                adHocChatRoom.setSwitchboard(switchboard);
                adHocChatRoom.updateParticipantsList(switchboard);

                // Add this room to the list of created chat rooms.
                adHocChatRoomCache.put(switchboard, adHocChatRoom);
            }
        }
    }

    /**
     * Supposed to reject an invitation for MUC.
     * Note: Not supported inside the MSN.
     */
    public void rejectInvitation(AdHocChatRoomInvitation invitation,
                                 String                  rejectReason)
    {
        // there is no way to block invitations, because there arn't any
        // invitations.
        // the only way would be to block the Friend and that shouldn't be done
        // here.
        return;
    }

    /**
     * Returns a list of all currently joined <tt>AdHocChatRoom</tt>-s.
     *
     * @return a list of all currently joined <tt>AdHocChatRoom</tt>-s
     */
    public List<AdHocChatRoom> getAdHocChatRooms()
    {
        return new ArrayList<AdHocChatRoom>(adHocChatRoomCache.values());
    }
}
