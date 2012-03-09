/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.sf.jml.*;

/**
 * Implements ad-hoc chat rooms for MSN.
 *
 * @author Rupert Burchardi
 * @author Valentin Martinet
 */
public class AdHocChatRoomMsnImpl
    implements AdHocChatRoom
{
     private static final Logger logger
         = Logger.getLogger(AdHocChatRoomMsnImpl.class);

    /**
     * The protocol provider that created us
     */
    private final ProtocolProviderServiceMsnImpl provider;

    /**
     * The OperationSet for MSN multi user chats.
     */
    private OperationSetAdHocMultiUserChatMsnImpl opSetAdHocMuc = null;

    /**
     * The corresponding switchboard for the chat room. Each chat room has its
     * own switchboard and if it is closed the user cannot reconnect to it, see
     * MSN documentation for further infos.
     */
    private MsnSwitchboard switchboard = null;

    /**
     * Listeners that will be notified of changes in participants status in the
     * ad-hoc room, such as participant joined, left, etc.
     */
    private Vector<AdHocChatRoomParticipantPresenceListener>
        participantsPresenceListeners =
            new Vector<AdHocChatRoomParticipantPresenceListener>();

    /**
     * Listeners that will be notified every time a new message is received on
     * this ad-hoc chat room.
     */
    private Vector<AdHocChatRoomMessageListener> messageListeners
        = new Vector<AdHocChatRoomMessageListener>();

    /**
     * A Message buffer, will keep all messages until the MSN ad-hoc chat room
     * is ready.
     */
    public Vector<EventObject> messageBuffer = new Vector<EventObject>();

    /**
     * The name of this ad-hoc chat room
     */
    private String name;

    /**
     * The list of participants of this ad-hoc chat room.
     */
    private final Hashtable<String, Contact> participants =
        new Hashtable<String, Contact>();

    /**
     * List of unresolved member names.
     */
    private ArrayList<String> pendingInvitations = new ArrayList<String>();

    /**
     * The presence operation set for the Msn protocol.
     */
    private final OperationSetPersistentPresenceMsnImpl presenceOpSet;

    /**
     * Creates a new ad-hoc chat room for MSN named <tt>name</tt>, using the
     * protocol provider <tt>provider</tt>.
     *
     * @param name
     * @param provider
     */
    public AdHocChatRoomMsnImpl(String name,
                                ProtocolProviderServiceMsnImpl provider)
    {
        this.name = name;
        this.provider = provider;
        this.opSetAdHocMuc =
            (OperationSetAdHocMultiUserChatMsnImpl)
            this.provider.getOperationSet(OperationSetAdHocMultiUserChat.class);

        this.presenceOpSet
            = (OperationSetPersistentPresenceMsnImpl)
                this.provider.getOperationSet(
                    OperationSetPersistentPresence.class);
    }

    /**
     * Creates a new ad-hoc chat room for MSN named <tt>name</tt>, using the
     * protocol provider <tt>provider</tt> and the msn switchboard
     * <tt>switchboard</tt>.
     *
     * @param name
     * @param provider
     * @param switchboard
     */
    public AdHocChatRoomMsnImpl(String name,
                                ProtocolProviderServiceMsnImpl provider,
                                MsnSwitchboard switchboard)
    {
        this.name = name;
        this.provider = provider;
        this.opSetAdHocMuc
            = (OperationSetAdHocMultiUserChatMsnImpl)
            this.provider.getOperationSet(OperationSetAdHocMultiUserChat.class);

        this.presenceOpSet
            = (OperationSetPersistentPresenceMsnImpl)
                this.provider.getOperationSet(
                    OperationSetPersistentPresence.class);

        this.switchboard = switchboard;

        this.updateParticipantsList(switchboard);
    }

    /**
     * Adds a listener that will be notified of changes in our status in the
     * room.
     *
     * @param listener a participant status listener.
     */
    public void addParticipantPresenceListener(
        AdHocChatRoomParticipantPresenceListener listener)
    {
        synchronized(this.participantsPresenceListeners)
        {
            if (!this.participantsPresenceListeners.contains(listener))
                this.participantsPresenceListeners.add(listener);
        }
    }

    /**
     * Registers <tt>listener</tt> so that it would receive events every time a
     * new message is received on this chat room.
     *
     * @param listener a <tt>MessageListener</tt> that would be notified every
     *            time a new message is received on this chat room.
     */
    public void addMessageListener(AdHocChatRoomMessageListener listener)
    {
        synchronized(this.messageListeners)
        {
            if (!this.messageListeners.contains(listener))
                this.messageListeners.add(listener);
        }
    }

    /**
     * Removes <tt>listener</tt> so that it won't receive any further message
     * events from this room.
     *
     * @param listener the <tt>MessageListener</tt> to remove from this room
     */
    public void removeMessageListener(ChatRoomMessageListener listener)
    {
        synchronized (messageListeners)
        {
            if (messageListeners.contains(listener))
                messageListeners.remove(messageListeners.indexOf(listener));
        }
    }

    /**
     * Finds the participant of this ad-hoc chat room corresponding to the
     * given address.
     *
     * @param address the address to search for.
     * @return the participant of this chat room corresponding to the given
     * nick name.
     */
    public Contact findParticipantForAddress(String address)
    {
        Iterator<Contact> participantsIter
            = this.participants.values().iterator();

        while (participantsIter.hasNext())
        {
            Contact contact = participantsIter.next();

            if (contact.getAddress().equals(address))
            {
                return contact;
            }
        }

        return null;
    }

    /**
     * Creates a <tt>Message</tt> for this ad-hoc chat room containing
     * <tt>text</tt>.
     *
     * @param text
     * @return Message the newly created <tt>Message</tt>
     */
    public Message createMessage(String text)
    {
        Message msg =
            new MessageMsnImpl(text,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING, null);

        return msg;
    }

    /**
     * Returns the name of this ad-hoc chatroom
     *
     * @return String
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Returns the parent provider
     *
     * @return ProtocolProviderService
     */
    public ProtocolProviderService getParentProvider()
    {
        return this.provider;
    }

    /**
     * Returns a list containing all the <tt>Contact</tt>s who participate in
     * this ad-hoc chat room.
     *
     * @return List<Contact>
     */
    public List<Contact> getParticipants()
    {
        return new LinkedList<Contact>(this.participants.values());
    }

    /**
     * Returns the participant of this ad-hoc chat room which corresponds to
     * the given id.
     *
     * @param id ID of the participant
     * @return Contact the corresponding Contact
     */
    public Contact getAdHocChatRoomParticipant(String id)
    {
        return this.participants.get(id);
    }

    /**
     * Adds a participant to the participants list.
     *
     * @param id the ID
     * @param participant The participant (<tt>Contact</tt>) to add.
     */
    public void addAdHocChatRoomParticipant(String id, Contact participant)
    {
        this.participants.put(id, participant);

        fireParticipantPresenceEvent(participant,
            AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_JOINED, null);
    }

    /**
     * Removes the participant of this ad-hoc chat room which corresponds to
     * the given id.
     *
     * @param id ID of the participant
     */
    public void removeParticipant(String id)
    {
        Contact contact= this.participants.get(id);
        this.participants.remove(id);

        fireParticipantPresenceEvent(contact,
            AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_LEFT, null);
    }

    /**
     * Returns the number of <tt>Contact</tt>s who participate in this ad-hoc
     * chat room.
     */
    public int getParticipantsCount()
    {
        return this.participants.size();
    }

    /**
     * Returns the subject.
     *
     * @return null
     */
    public String getSubject()
    {
        return null;
    }

    /**
     * Invites another user to this room. If we're not joined nothing will
     * happen.
     *
     * @param userAddress the address of the user (email address) to invite to
     *            the room.(one may also invite users not on their contact
     *            list).
     * @param reason You cannot specify a Reason inside the msn protocol
     */
    public void invite(String userAddress, String reason)
    {
        // msn requires lower case email addresses
        userAddress = userAddress.toLowerCase();

        if (switchboard == null)
        {
            pendingInvitations.add(userAddress);
        }
        else
        {
            switchboard.inviteContact(Email.parseStr(userAddress));
        }
    }

    public void join()
    {
        // We don't specify a reason.
        this.opSetAdHocMuc.fireLocalUserPresenceEvent(this,
            LocalUserAdHocChatRoomPresenceChangeEvent.LOCAL_USER_JOINED, null);

        // We buffered the messages before the user has joined the chat, now the
        // user has joined so we fire them again
        for (EventObject evt : this.messageBuffer)
        {
            this.fireMessageEvent(evt);
        }
    }

    /**
     * Leave this chat room. Once this method is called, the user won't be
     * listed as a member of the chat room any more and no further chat events
     * will be delivered. Depending on the underlying protocol and
     * implementation leave() might cause the room to be destroyed if it has
     * been created by the local user.
     */
    public void leave()
    {
        if (switchboard != null)
        {
            switchboard.close();
            switchboard = null;
        }

        Iterator<Contact> participantsIter
            = participants.values().iterator();

        while (participantsIter.hasNext())
        {
            Contact participant = participantsIter.next();

            fireParticipantPresenceEvent(participant,
                AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_LEFT,
                "Local user has left the chat room.");
        }

        // Delete the list of members
        participants.clear();
    }

    /**
     * Returns if this chatroom is a system ones.
     *
     * @return false
     */
    public boolean isSystem()
    {
        return false;
    }

    /**
     * Sends the given message through the participants of this ad-hoc chat
     * room.
     *
     * @param message the message to delivered
     *
     * @throws OperationFailedException if send fails
     */
    public void sendMessage(Message message) throws OperationFailedException
    {
        if (logger.isInfoEnabled())
            logger.info("switchboard="+this.switchboard);
        this.switchboard.sendText(message.getContent());

        AdHocChatRoomMessageDeliveredEvent msgDeliveredEvt
            = new AdHocChatRoomMessageDeliveredEvent(
            this,
            System.currentTimeMillis(),
            message,
            AdHocChatRoomMessageDeliveredEvent.CONVERSATION_MESSAGE_DELIVERED);

        this.fireMessageEvent(msgDeliveredEvt);
    }

    /**
     * Sets the corresponding switchboard.
     *
     * @param switchboard Corresponding switchboard.
     */
    public void setSwitchboard(MsnSwitchboard switchboard)
    {
        this.switchboard = switchboard;
    }

    /**
     * Creates the corresponding AdHocChatRoomParticipantPresenceChangeEvent and
     * notifies all <tt>AdHocChatRoomParticipantPresenceListener</tt>s that a
     * participant has joined or left this <tt>AdHocChatRoom</tt>.
     *
     * @param participant the <tt>Contact</tt>
     * @param eventID the identifier of the event
     * @param eventReason the reason of the event
     */
    private void fireParticipantPresenceEvent(    Contact participant,
                                                String  eventID,
                                                String  eventReason)
    {
        AdHocChatRoomParticipantPresenceChangeEvent evt =
            new AdHocChatRoomParticipantPresenceChangeEvent(
                    this, participant, eventID, eventReason);

        if (logger.isTraceEnabled())
            logger.trace("Will dispatch the following AdHocChatRoom event: " + evt);

        Iterator<AdHocChatRoomParticipantPresenceListener> listeners = null;
        synchronized (this.participantsPresenceListeners)
        {
            listeners = new ArrayList<AdHocChatRoomParticipantPresenceListener>(
                                this.participantsPresenceListeners).iterator();
        }

        while (listeners.hasNext())
        {
            listeners.next().participantPresenceChanged(evt);
        }
    }

    /**
     * Notifies all interested listeners that a
     * <tt>AdHocChatRoomMessageDeliveredEvent</tt>,
     * <tt>AdHocChatRoomMessageReceivedEvent</tt> or a
     * <tt>AdHocChatRoomMessageDeliveryFailedEvent</tt> has been fired.
     *
     * @param evt The specific event
     */
    public void fireMessageEvent(EventObject evt)
    {
        Iterator<AdHocChatRoomMessageListener> listeners = null;
        synchronized (messageListeners)
        {
            listeners = new ArrayList<AdHocChatRoomMessageListener>(
                                messageListeners).iterator();
        }

        if (!listeners.hasNext())
        {
            messageBuffer.add(evt);
        }

        while (listeners.hasNext())
        {
            AdHocChatRoomMessageListener listener = listeners.next();

            if (evt instanceof AdHocChatRoomMessageDeliveredEvent)
            {
                listener.messageDelivered(
                        (AdHocChatRoomMessageDeliveredEvent) evt);
            }
            else if (evt instanceof AdHocChatRoomMessageReceivedEvent)
            {
                listener.messageReceived(
                        (AdHocChatRoomMessageReceivedEvent) evt);
            }
            else if (evt instanceof AdHocChatRoomMessageDeliveryFailedEvent)
            {
                listener.messageDeliveryFailed(
                            (AdHocChatRoomMessageDeliveryFailedEvent) evt);
            }
        }
    }

    /**
     * Fills the participants list with all participants inside the switchboard
     * (ad-hoc chat room).
     *
     * @param switchboard The corresponding switchboard
     */
    public void updateParticipantsList(MsnSwitchboard switchboard)
    {
        MsnContact[] contacts = switchboard.getAllContacts();

        for (MsnContact msnContact : contacts)
        {
            if (!this.participants.containsKey(msnContact.getId()))
            {
                // if the member is not inside the members list, create a
                // contact instance,
                // add it to the list and fire a member presence event
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

               this.participants.put(msnContact.getId(), contact);

                fireParticipantPresenceEvent(
                    (Contact) contact,
                    AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_JOINED,
                    null);
            }
        }

        for (String contactAddress: this.pendingInvitations)
        {
            this.invite(contactAddress, "");
        }
        // We have sent all invites and we can now clear the content of
        // pending invitations.
        pendingInvitations.clear();
    }

    /**
     * Returns the identifier of this ad-hoc chat room.
     *
     * @return a <tt>String</tt> containing the identifier of this ad-hoc room
     */
    public String getIdentifier()
    {
        return this.getName();
    }

    /**
     * Removes the given participant presence listener.
     *
     * @param listener the listener to remove
     */
    public void removeParticipantPresenceListener(
            AdHocChatRoomParticipantPresenceListener listener)
    {
        synchronized (this.participantsPresenceListeners)
        {
            if (this.participantsPresenceListeners.contains(listener))
                this.participantsPresenceListeners.remove(listener);
        }
    }

    /**
     * Removes the given message listener.
     *
     * @param listener the listener to remove
     */
    public void removeMessageListener(AdHocChatRoomMessageListener listener)
    {
        synchronized(this.messageListeners)
        {
            if(this.messageListeners.contains(listener))
            {
                this.messageListeners.remove(listener);
            }
        }
    }
}
