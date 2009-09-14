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
    private Hashtable<String, AdHocChatRoomMsnImpl> adHocChatRoomCache
                        = new Hashtable<String, AdHocChatRoomMsnImpl>();

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
    private Hashtable<Object, AdHocChatRoom> userCreatedAdHocChatRoomList
        = new Hashtable<Object, AdHocChatRoom>();

    /**
     * Creates an <tt>OperationSetAdHocMultiUserChatMsnImpl</tt> by specifying
     * the parent provider.
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
     * Creates an <tt>AdHocChatRoom</tt> from the specified _adHocChatRoomName
     * and the corresponding _switchboard.
     * 
     * @param adHocChatRoomName the specific chat room name.
     * @param switchboard The corresponding switchboard.
     * 
     * @return AdHocChatRoomMsnImpl the chat room that we've just created.
     */
    private AdHocChatRoomMsnImpl createAdHocChatRoom(String adHocChatRoomName,
        MsnSwitchboard switchboard)
    {
        synchronized (this.adHocChatRoomCache)
        {
            AdHocChatRoomMsnImpl adHocChatRoom = new AdHocChatRoomMsnImpl(
                    adHocChatRoomName, this.provider, switchboard);

            this.adHocChatRoomCache.put(adHocChatRoom.getName(), adHocChatRoom);
            adHocChatRoom.join();
            return adHocChatRoom;
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
     * including to the specified <tt>contacts</tt>. When the method
     * returns the ad-hoc room the local user will not have joined it and thus 
     * will not receive messages on it until the <tt>AdHocChatRoom.join()</tt> 
     * method is called.
     * 
     * NOTE: this method was done for the Yahoo! implementation. But since both
     * Yahoo! and MSN are implementing the ad-hoc multi-user-chat operation set,
     * we have to define this method here.
     * 
     * @param adHocRoomName the name of the ad-hoc room
     * @param contacts the list of contacts
     * 
     * @throws OperationFailedException
     * @throws OperationNotSupportedException
     */
    public AdHocChatRoom createAdHocChatRoom(String adHocRoomName,
        List<Contact> contacts) 
        throws OperationFailedException, OperationNotSupportedException
    {
        AdHocChatRoom adHocRoom = null;
        adHocRoom = findRoom(adHocRoomName);
        if (adHocRoom == null)
        { 
            // when the room hasn't been created, we create it.
            adHocRoom = createLocalAdHocChatRoomInstance(adHocRoomName);

            // we create an identifier object and create a new switchboard
            // we need to track this object to identify this chatRoom
            Object id = new Object();
            this.provider.getMessenger().newSwitchboard(id);
            // we put it into a hash table
            this.userCreatedAdHocChatRoomList.put(id, adHocRoom);
        }
        adHocRoom.join();
        return adHocRoom;
    }

    /**
     * Creates an <tt>AdHocChatRoom</tt> whose name is _adHocRoomName with the
     * properties contained in _adHocRoomProperties
     * 
     * @param adHocRoomName the name of the ad-hoc room
     * @param adHocRoomProperties the ad-hoc room's properties
     * 
     * @throws OperationFailedException
     * @throws OperationNotSupportedException
     */
    public AdHocChatRoom createAdHocChatRoom(String adHocRoomName,
            Map<String, Object> adHocRoomProperties)
            throws OperationFailedException, OperationNotSupportedException {
        AdHocChatRoom adHocRoom = null;
        adHocRoom = findRoom(adHocRoomName);
        if (adHocRoom == null)
        { 
            // when the room hasn't been created, we create it.
            adHocRoom = createLocalAdHocChatRoomInstance(adHocRoomName);

            // we create an identifier object and create a new switchboard
            // we need to track this object to identify this chatRoom
            Object id = new Object();
            this.provider.getMessenger().newSwitchboard(id);
            // we put it into a hash table
            this.userCreatedAdHocChatRoomList.put(id, adHocRoom);
        }
        adHocRoom.join();
        return adHocRoom;
    }

    /**
     * Creates a <tt>ChatRoom</tt> from the specified chatRoomName.
     * 
     * @param adHocChatRoomName the specific ad-hoc chat room name.
     * 
     * @return AdHocChatRoom the ad-hoc chat room that we've just created.
     */
    private AdHocChatRoom createLocalAdHocChatRoomInstance(
            String adHocChatRoomName)
    {
        synchronized (this.adHocChatRoomCache)
        {
            AdHocChatRoomMsnImpl adHocChatRoom =
                new AdHocChatRoomMsnImpl(adHocChatRoomName, this.provider);

            this.adHocChatRoomCache.put(adHocChatRoom.getName(), adHocChatRoom);
            return adHocChatRoom;
        }
    }

    /**
     * Returns a reference to a chatRoom named <tt>_adHocRoomName</tt> or null. 
     * Note: Only called by user.
     * 
     * @param adHocRoomName the name of the <tt>AdHocChatRoom</tt> that we're 
     * looking for.
     * @return the <tt>AdHocChatRoom</tt> named <tt>_adHocRoomName</tt> or null
     * if no such ad-hoc room exists on the server that this provider is 
     * currently connected to.
     * 
     * @throws OperationFailedException if an error occurs while trying to
     *             discover the ad-hoc room on the server.
     * @throws OperationNotSupportedException if the server does not support
     *             multi user chat
     */
    public AdHocChatRoom findRoom(String adHocRoomName)
        throws OperationFailedException, OperationNotSupportedException
    {
        assertConnected();

        AdHocChatRoom adHocRoom = 
            (AdHocChatRoom) this.adHocChatRoomCache.get(adHocRoomName);

        return adHocRoom;
    }

    /**
     * Returns a reference to an chatRoom named <tt>roomName</tt>. If the chat
     * room doesn't exist, a new chat room is created for the given
     * MsnSwitchboard.
     * 
     * @param switchboard The specific switchboard for the chat room.
     * 
     * @return the corresponding chat room
     * 
     * @throws OperationFailedException if an error occurs while trying to
     *             discover the room on the server.
     * @throws OperationNotSupportedException if the server does not support
     *             multi user chat
     */

    public AdHocChatRoom findRoom(MsnSwitchboard switchboard)
        throws OperationFailedException,
        OperationNotSupportedException
    {
        this.assertConnected();

        AdHocChatRoomMsnImpl adHocRoom = (AdHocChatRoomMsnImpl) 
            this.adHocChatRoomCache.get(String.valueOf(switchboard.hashCode()));

        if (adHocRoom == null)
        {
            String name = String.valueOf(switchboard.hashCode());
            adHocRoom = this.createAdHocChatRoom(name, switchboard);
            adHocRoom.setSwitchboard(switchboard);
            adHocRoom.updateParticipantsList(switchboard);

            this.adHocChatRoomCache.put(name, adHocRoom);

            // fireInvitationEvent(room,
            // switchboard.getMessenger().getOwner().getDisplayName(),
            // "You have been invited to a group chat", null);
            adHocRoom.join();
        }

        return adHocRoom;
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
    public void fireLocalUserPresenceEvent(    AdHocChatRoom     adHocChatRoom,
                                            String             eventType,
                                            String             reason)
    {
        LocalUserAdHocChatRoomPresenceChangeEvent evt =
            new LocalUserAdHocChatRoomPresenceChangeEvent(
                    this, 
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
        // //fileTransfer??
        // if (switchboard.getActiveFileTransfers() != null)
        // return false;

        Object attachment = switchboard.getAttachment();
        if (attachment == null)
        { // the user did not created the chat room by him/her self,
            // the only way to figure out if this is a group chat message
            // is to check the user count
            return (switchboard.getAllContacts().length > 1);

        }

        return this.userCreatedAdHocChatRoomList.containsKey(attachment);
    }

    public boolean isMultiChatSupportedByContact(Contact contact) 
    {
        return false;
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
        public void instantMessageReceived( MsnSwitchboard switchboard,
                                            MsnInstantMessage message,
                                            MsnContact contact)
        {
            if (!isGroupChatMessage(switchboard))
                return;

            Message newMessage = createMessage(message.getContent());

            logger.debug("Group chat message received.");
            Object attachment = switchboard.getAttachment();
            try
            {
                AdHocChatRoomMsnImpl chatRoom = null;

                if (attachment == null) // chat room session NOT created by
                                        // yourself
                {
                    chatRoom = (AdHocChatRoomMsnImpl) findRoom(switchboard);
                }

                // user created chat room session?
                if (attachment != null
                    && userCreatedAdHocChatRoomList.containsKey(attachment))
                {   chatRoom =
                        (AdHocChatRoomMsnImpl) userCreatedAdHocChatRoomList
                            .get(attachment);
                }

                if (chatRoom == null)
                {
                    return;
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
            catch (OperationFailedException e)
            {
                logger.error("Failed to find room with name: ", e);
            }
            catch (OperationNotSupportedException e)
            {
                logger.error("Failed to find room with name: ", e);
            }

        }

        public void initialEmailNotificationReceived(
            MsnSwitchboard switchboard, MsnEmailInitMessage message,
            MsnContact contact)
        {
        }

        public void initialEmailDataReceived(MsnSwitchboard switchboard,
            MsnEmailInitEmailData message, MsnContact contact)
        {
        }

        public void newEmailNotificationReceived(MsnSwitchboard switchboard,
            MsnEmailNotifyMessage message, MsnContact contact)
        {

        }

        public void activityEmailNotificationReceived(
            MsnSwitchboard switchboard, MsnEmailActivityMessage message,
            MsnContact contact)
        {
        }
    }

    /**
     * The Switchboard Listener, listens to all four switchboard events:
     * Switchboard started/closed and User joins/left.
     * 
     */
    private class MsnSwitchboardListener
        extends MsnSwitchboardAdapter
    {
        public void contactJoinSwitchboard(MsnSwitchboard switchboard,
            MsnContact contact)
        {
            if (!isGroupChatMessage(switchboard))
                return;

            Object attachment = switchboard.getAttachment();
            try
            {
                AdHocChatRoomMsnImpl chatRoom = null;
                if (attachment == null) // chat room session NOT created by
                                        // yourself
                    chatRoom = (AdHocChatRoomMsnImpl) findRoom(switchboard);

                // user created chat room session?
                if (attachment != null
                    && userCreatedAdHocChatRoomList.containsKey(attachment))
                    chatRoom =
                        (AdHocChatRoomMsnImpl) userCreatedAdHocChatRoomList
                            .get(attachment);

                if (chatRoom == null)
                    return;

                Contact msnContact = new ContactMsnImpl(
                        contact, new ServerStoredContactListMsnImpl(
                            new OperationSetPersistentPresenceMsnImpl(provider),
                            provider), true, true);
                chatRoom.addAdHocChatRoomParticipant(    contact.getId(), 
                                                        msnContact);
            }
            catch (Exception e)
            {
                logger.error("Failed to join switchboard.", e);
            }

        }

        public void contactLeaveSwitchboard(MsnSwitchboard switchboard,
                                            MsnContact contact)
        {
            logger
                .debug(contact.getDisplayName() + " has left the Switchboard");

            Object attachment = switchboard.getAttachment();

            try
            {
                AdHocChatRoomMsnImpl adHocChatRoom = null;
                if (attachment == null)// chat room session NOT created by
                                       // yourself
                    adHocChatRoom = (AdHocChatRoomMsnImpl)findRoom(switchboard);

                // user created chat room session?
                if (attachment != null
                    && userCreatedAdHocChatRoomList.containsKey(attachment))
                    adHocChatRoom =
                        (AdHocChatRoomMsnImpl) userCreatedAdHocChatRoomList
                            .get(attachment);

                if (adHocChatRoom == null)
                    return;

                String participantId = contact.getId();

                Contact participant =
                    adHocChatRoom.getAdHocChatRoomParticipant(participantId);

                if (participant != null)
                {
                    adHocChatRoom.removeParticipant(participantId);
                }
            }
            catch (OperationFailedException e)
            {
                logger.debug(   "Could not find a chat room corresponding" +
                                "to the given switchboard.", e);
            }
            catch (OperationNotSupportedException e)
            {
                logger.debug(   "Could not find a chat room corresponding" +
                                "to the given switchboard.", e);
            }
        }

        public void switchboardClosed(MsnSwitchboard switchboard)
        {
            Object attachment = switchboard.getAttachment();
            try
            {
                AdHocChatRoomMsnImpl adHocChatRoom = null;
                if (attachment == null)// chat room session NOT created by
                                       // yourself
                    adHocChatRoom = (AdHocChatRoomMsnImpl) findRoom(switchboard);
                // user created chat room session?
                if (attachment != null
                    && userCreatedAdHocChatRoomList.containsKey(attachment))
                    adHocChatRoom =
                        (AdHocChatRoomMsnImpl) userCreatedAdHocChatRoomList
                            .get(attachment);

                if (adHocChatRoom == null)
                    return;

                adHocChatRoom.setSwitchboard(null);

                 adHocChatRoom.leave();
                 fireLocalUserPresenceEvent(adHocChatRoom,
                 LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_DROPPED ,
                 "Switchboard closed.");
            }
            catch (Exception e)
            {
            }
        }
        
        public void switchboardStarted(MsnSwitchboard switchboard)
        {
           
            Object switchboardID = switchboard.getAttachment();
            AdHocChatRoomMsnImpl adHocChatRoom = null;
            if (switchboardID != null
                    && userCreatedAdHocChatRoomList.containsKey(switchboardID))
            {
                adHocChatRoom =
                    (AdHocChatRoomMsnImpl) userCreatedAdHocChatRoomList
                    .get(switchboardID);

                adHocChatRoom.setSwitchboard(switchboard);
                adHocChatRoom.updateParticipantsList(switchboard);
                adHocChatRoom.join();
            }
            else
            {
                logger.setLevelDebug();
                logger.debug("Could not join the Ad-hoc chat room.");
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
}
