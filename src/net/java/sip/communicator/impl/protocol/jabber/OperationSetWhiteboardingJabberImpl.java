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
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.whiteboard.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.whiteboardobjects.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.util.StringUtils;

/**
 * Provides basic functionality for white-board.
 *
 * @author Julien Waechter
 * @author Yana Stamcheva
 */
public class OperationSetWhiteboardingJabberImpl
    implements OperationSetWhiteboarding
{
    /**
     * The logger of this class.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetWhiteboardingJabberImpl.class);

    /**
     * The provider that created us.
     */
    private ProtocolProviderServiceJabberImpl jabberProvider = null;

    /**
     * A list of listeners subscribed for invitations multi user chat events.
     */
    private Vector<WhiteboardInvitationListener> invitationListeners = new Vector<WhiteboardInvitationListener>();

    /**
     * A list of listeners subscribed for events indicating rejection of a
     * multi user chat invitation sent by us.
     */
    private Vector<WhiteboardInvitationRejectionListener> invitationRejectionListeners = new Vector<WhiteboardInvitationRejectionListener>();

    /**
     * Listeners that will be notified of changes in our status in the
     * room such as us being kicked, banned, or granted admin permissions.
     */
    private Vector<WhiteboardSessionPresenceListener> presenceListeners = new Vector<WhiteboardSessionPresenceListener>();

    private Vector<WhiteboardSession> whiteboardSessions = new Vector<WhiteboardSession>();

    private OperationSetPersistentPresenceJabberImpl presenceOpSet;

    /**
     * Creates an instance of this operation set.
     * @param provider a ref to the <tt>ProtocolProviderServiceImpl</tt>
     * that created us and that we'll use for retrieving the underlying aim
     * connection.
     */
    public OperationSetWhiteboardingJabberImpl(
        ProtocolProviderServiceJabberImpl provider)
    {
        this.jabberProvider = provider;

        provider.addRegistrationStateChangeListener(
            new RegistrationStateListener());

        // Add the custom WhiteboardObjectJabberProvider to the Smack library
        ProviderManager pManager = ProviderManager.getInstance();

        pManager.addExtensionProvider(
            WhiteboardObjectPacketExtension.ELEMENT_NAME,
            WhiteboardObjectPacketExtension.NAMESPACE,
            new WhiteboardObjectJabberProvider());

        pManager.addExtensionProvider(
            WhiteboardSessionPacketExtension.ELEMENT_NAME,
            WhiteboardSessionPacketExtension.NAMESPACE,
            new WhiteboardObjectJabberProvider());
    }

    /**
     * Adds a listener to invitation notifications.
     *
     * @param listener an invitation listener.
     */
    public void addInvitationListener(WhiteboardInvitationListener listener)
    {
        synchronized(invitationListeners)
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
    public void removeInvitationListener(WhiteboardInvitationListener listener)
    {
        synchronized(invitationListeners)
        {
            invitationListeners.remove(listener);
        }
    }

    /**
     * Subscribes <tt>listener</tt> so that it would receive events indicating
     * rejection of a multi-user chat invitation that we've sent earlier.
     *
     * @param listener the listener that we'll subscribe for invitation
     * rejection events.
     */
    public void addInvitationRejectionListener(
                                WhiteboardInvitationRejectionListener listener)
    {
        synchronized(invitationRejectionListeners)
        {
            if (!invitationRejectionListeners.contains(listener))
                invitationRejectionListeners.add(listener);
        }
    }

    /**
     * Removes <tt>listener</tt> from the list of invitation listeners
     * registered to receive invitation rejection events.
     *
     * @param listener the invitation listener to remove.
     */
    public void removeInvitationRejectionListener(
                                WhiteboardInvitationRejectionListener listener)
    {
        synchronized(invitationRejectionListeners)
        {
            invitationRejectionListeners.remove(listener);
        }
    }

    /**
     * Adds a listener that will be notified of changes in our status in a chat
     * room such as us being kicked, banned or dropped.
     *
     * @param listener the <tt>LocalUserChatRoomPresenceListener</tt>.
     */
    public void addPresenceListener(WhiteboardSessionPresenceListener listener)
    {
        synchronized(presenceListeners)
        {
            if (!presenceListeners.contains(listener))
                presenceListeners.add(listener);
        }
    }

    /**
     * Removes a listener that was being notified of changes in our status in
     * a room such as us being kicked, banned or dropped.
     *
     * @param listener the <tt>LocalUserChatRoomPresenceListener</tt>.
     */
    public void removePresenceListener(
        WhiteboardSessionPresenceListener listener)
    {
        synchronized(presenceListeners)
        {
            presenceListeners.remove(listener);
        }
    }

    /**
     * Creates a <tt>WhiteboardSession</tt>. For now the session is created
     * locally and neither the sessionName, nor the sessionProperties are
     * used.
     * @param sessionName the name of the session
     * @param sessionProperties the settings of the session
     * @throws OperationFailedException if the room couldn't be created for some
     * reason (e.g. room already exists; user already joined to an existent
     * room or user has no permissions to create a chat room).
     * @throws OperationNotSupportedException if chat room creation is not
     * supported by this server
     * @return the created white-board session
     */
    public WhiteboardSession createWhiteboardSession(
        String sessionName,
        Hashtable<Object, Object> sessionProperties)
        throws  OperationFailedException,
                OperationNotSupportedException
    {
        WhiteboardSessionJabberImpl session
            = new WhiteboardSessionJabberImpl(jabberProvider, this);

        whiteboardSessions.add(session);

        return session;
    }

    /**
     * Returns a reference to a <tt>WhiteboardSession</tt> named
     * <tt>sessionName</tt> or null if no such session exists.
     * <p>
     * @param sessionName the name of the <tt>WhiteboardSession</tt> that we're
     * looking for.
     * @return the <tt>WhiteboardSession</tt> named <tt>sessionName</tt> or null
     * if no such session exists on the server that this provider is currently
     * connected to.
     *
     * @throws OperationFailedException if an error occurs while trying to
     * discover the white-board session on the server.
     * @throws OperationNotSupportedException if the server does not support
     * white-boarding
     */
    public WhiteboardSession findWhiteboardSession(String sessionName)
        throws OperationFailedException,
        OperationNotSupportedException
    {
        // TODO: Implement findWhiteboardSession
        return null;
    }

    /**
     * Returns a list of the white-board sessions that we have joined and are
     * currently active in.
     *
     * @return a <tt>List</tt> of the white-board sessions where the user has
     * joined using a given connection.
     */
    public List<WhiteboardSession> getCurrentlyJoinedWhiteboards()
    {
        synchronized(whiteboardSessions)
        {
            List<WhiteboardSession> joinedWhiteboards
                = new LinkedList<WhiteboardSession>(whiteboardSessions);
            Iterator<WhiteboardSession> joinedWhiteboardsIter
                = whiteboardSessions.iterator();

            while (joinedWhiteboardsIter.hasNext())
            {
                if (!joinedWhiteboardsIter.next().isJoined())
                    joinedWhiteboardsIter.remove();
            }

            return joinedWhiteboards;
        }
    }

    /**
     * Returns a list of the <tt>WhiteboardSession</tt>s that
     * <tt>WhiteboardParticipant</tt> has joined and is currently active in.
     *
     * @param participant the participant whose current
     * <tt>WhiteboardSession</tt>s we will be querying.
     * @return a list of the <tt>WhiteboardSession</tt>s that
     * <tt>WhiteboardParticipant</tt> has joined and is currently active in.
     *
     * @throws OperationFailedException if an error occurs while trying to
     * discover the session.
     * @throws OperationNotSupportedException if the server does not support
     * white-boarding
     */
    public List<WhiteboardSession> getCurrentlyJoinedWhiteboards(WhiteboardParticipant participant)
        throws OperationFailedException,
        OperationNotSupportedException
    {
        // TODO: Implement getCurrentlyJoinedWhiteboards(
        // WhiteboardParticipant participant)
        return null;
    }

    /**
     * Returns true if <tt>contact</tt> supports white-board sessions.
     *
     * @param contact reference to the contact whose support for white-boards
     * we are currently querying.
     * @return a boolean indicating whether <tt>contact</tt> supports
     * white-boards.
     */
    public boolean isWhiteboardingSupportedByContact(Contact contact)
    {
        if(contact.getProtocolProvider()
            .getOperationSet(OperationSetWhiteboarding.class) != null)
            return true;

        return false;
    }

    /**
     * Informs the sender of an invitation that we decline their invitation.
     *
     * @param invitation the invitation we are rejecting.
     * @param rejectReason the reason to reject the invitation (optional)
     */
    public void rejectInvitation(WhiteboardInvitation invitation,
        String rejectReason)
    {
        // TODO: Implement rejectInvitation(WhiteboardInvitation invitation,
        // String rejectReason)
    }

    /**
     * Our listener that will tell us when we're registered to
     */
    private class RegistrationStateListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenever
         * a change in the registration state of the corresponding provider had
         * occurred.
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                presenceOpSet
                    = (OperationSetPersistentPresenceJabberImpl) jabberProvider
                        .getOperationSet(OperationSetPresence.class);

                PacketExtensionFilter filterWhiteboard =
                    new PacketExtensionFilter(
                        WhiteboardObjectPacketExtension.ELEMENT_NAME,
                        WhiteboardObjectPacketExtension.NAMESPACE);

                jabberProvider.getConnection().addPacketListener(
                    new WhiteboardSmackMessageListener(), filterWhiteboard);
            }
        }
    }

    /**
     * Listens for white-board messages and checks if a white-board session
     * already exists and if not simulates an invitation to the user.
     */
    private class WhiteboardSmackMessageListener
        implements PacketListener
    {
        public void processPacket(Packet packet)
        {
            if (!(packet instanceof org.jivesoftware.smack.packet.Message))
                return;

            PacketExtension ext =
                packet.getExtension(
                    WhiteboardObjectPacketExtension.ELEMENT_NAME,
                    WhiteboardObjectPacketExtension.NAMESPACE);

            org.jivesoftware.smack.packet.Message msg =
                (org.jivesoftware.smack.packet.Message) packet;

            if (ext == null)
                return;

            String fromUserID = StringUtils.parseBareAddress(msg.getFrom());

            // We check if a white-board session with the given contact already
            // exists and if this is the case we don't continue.
            for (int i = 0; i < whiteboardSessions.size(); i ++)
            {
                WhiteboardSessionJabberImpl session
                    = (WhiteboardSessionJabberImpl) whiteboardSessions.get(i);

                // Should be replaced by getParticipants when implementing
                // the multi user white-boarding
                if(session.isJoined()
                        && session.isParticipantContained(fromUserID))
                    return;
            }

            // If we're here this means that no white board session has been
            // found and we will send an invitation to the user to join a
            // white-board session created by us.
            WhiteboardObjectPacketExtension newMessage
                = (WhiteboardObjectPacketExtension) ext;

            WhiteboardSessionJabberImpl session
                = new WhiteboardSessionJabberImpl(
                    jabberProvider,
                    OperationSetWhiteboardingJabberImpl.this);

            whiteboardSessions.add(session);

            ContactJabberImpl sourceContact
                = (ContactJabberImpl) presenceOpSet.findContactByID(fromUserID);

            if (sourceContact == null)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Received a message from an unknown contact: "
                    + fromUserID);

                //create the volatile contact
                sourceContact
                    = presenceOpSet.createVolatileContact(fromUserID);
            }

            session.addWhiteboardParticipant(
                new WhiteboardParticipantJabberImpl(sourceContact, session));

            fireInvitationEvent(session,
                                newMessage.getWhiteboardObject(),
                                fromUserID,
                                null,
                                null);
        }
    }

    /**
     * Delivers a <tt>WhiteboardInvitationEvent</tt> to all
     * registered <tt>WhiteboardInvitationListener</tt>s.
     *
     * @param targetWhiteboard the white-board that invitation refers to
     * @param whiteboardObject the white-board object that inviter send
     * with this invitation and which will be shown on the white-board if the
     * user accepts the invitation
     * @param inviter the inviter that sent the invitation
     * @param reason the reason why the inviter sent the invitation
     * @param password the password to use when joining the room
     */
    public void fireInvitationEvent(WhiteboardSession targetWhiteboard,
                                    WhiteboardObject whiteboardObject,
                                    String inviter,
                                    String reason,
                                    byte[] password)
    {
        WhiteboardInvitationJabberImpl invitation
            = new WhiteboardInvitationJabberImpl(   targetWhiteboard,
                                                    whiteboardObject,
                                                    inviter,
                                                    reason,
                                                    password);

        WhiteboardInvitationReceivedEvent evt
            = new WhiteboardInvitationReceivedEvent(this, invitation,
                new Date(System.currentTimeMillis()));

        if (logger.isDebugEnabled())
            logger.debug("Dispatching a WhiteboardInvitation event to "
            + invitationListeners.size() + " listeners. event is: "
            + evt.toString());

        Iterable<WhiteboardInvitationListener> listeners;
        synchronized (invitationListeners)
        {
            listeners
                = new ArrayList<WhiteboardInvitationListener>(
                        invitationListeners);
        }

        for (WhiteboardInvitationListener listener : listeners)
            listener.invitationReceived(evt);
    }

    /**
     * Delivers a <tt>WhiteboardSessionPresenceChangeEvent</tt> to all
     * registered <tt>WhiteboardSessionPresenceChangeEvent</tt>s.
     *
     * @param session the <tt>WhiteboardSession</tt> which has been joined,
     * left, etc.
     * @param eventType the type of this event; one of LOCAL_USER_JOINED,
     * LOCAL_USER_LEFT, etc.
     * @param reason the reason
     */
    public void fireWhiteboardSessionPresenceEvent( WhiteboardSession session,
                                                    String eventType,
                                                    String reason)
    {
        WhiteboardSessionPresenceChangeEvent evt
            = new WhiteboardSessionPresenceChangeEvent( this,
                                                        session,
                                                        eventType,
                                                        reason);

        Iterable<WhiteboardSessionPresenceListener> listeners;
        synchronized (presenceListeners)
        {
            listeners
                = new ArrayList<WhiteboardSessionPresenceListener>(
                        presenceListeners);
        }

        for (WhiteboardSessionPresenceListener listener : listeners)
            listener.whiteboardSessionPresenceChanged(evt);
    }
}
