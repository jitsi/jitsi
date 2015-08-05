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
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.*;

/**
 * A representation of a <tt>WhiteboardSession</tt>.
 *
 * @author Julien Waechter
 * @author Yana Stamcheva
 */
public class WhiteboardSessionJabberImpl
    implements  WhiteboardParticipantListener,
                WhiteboardSession
{
    /**
     * The logger of this class.
     */
    private static final Logger logger =
        Logger.getLogger(WhiteboardSessionJabberImpl.class);

    /**
     * A list of listeners registered for message events.
     */
    private Vector<WhiteboardObjectListener> messageListeners
        = new Vector<WhiteboardObjectListener>();

    /**
     * A list containing all <tt>WhiteboardParticipant</tt>s of this session.
     */
    private Hashtable<String, WhiteboardParticipant> wbParticipants
        = new Hashtable<String, WhiteboardParticipant>();

    /**
     * The state that this white-board is currently in.
     */
    private WhiteboardSessionState whiteboardState =
        WhiteboardSessionState.WHITEBOARD_INITIALIZATION;

    /**
     * The provider that created us.
     */
    private ProtocolProviderServiceJabberImpl jabberProvider = null;

    /**
     * An identifier uniquely representing the white-board.
     */
    private String whiteboardID = null;

    /**
     * A list of all listeners currently registered for
     * <tt>WhiteboardChangeEvent</tt>s
     */
    private Vector<WhiteboardChangeListener> whiteboardListeners
        = new Vector<WhiteboardChangeListener>();

    /**
     * Stores all white board objects contained in this session.
     */
    private final Vector<WhiteboardObject> whiteboardObjects
        = new Vector<WhiteboardObject>();

    /**
     * The <tt>OperationSet</tt> charged with the whiteboarding.
     */
    private OperationSetWhiteboardingJabberImpl whiteboardOpSet;

    /**
     * The corresponding smack chat.
     */
    private Chat smackChat;

    /**
     * WhiteboardSessionJabberImpl constructor.
     *
     * @param sourceProvider Jabber protocol provider
     * @param opSet the whiteboard operation set
     */
    public WhiteboardSessionJabberImpl(
        ProtocolProviderServiceJabberImpl sourceProvider,
        OperationSetWhiteboardingJabberImpl opSet)
    {
        this.jabberProvider = sourceProvider;
        this.whiteboardOpSet = opSet;

        //create the UID
        this.whiteboardID =
            String.valueOf(System.currentTimeMillis())
                + String.valueOf(super.hashCode());
    }

    /**
     * Returns an iterator over all white-board participants.
     *
     * @return an Iterator over all participants currently involved in the
     * white-board.
     */
    public Iterator<WhiteboardParticipant> getWhiteboardParticipants()
    {
        return
            new LinkedList<WhiteboardParticipant>(wbParticipants.values())
                    .iterator();
    }

    /**
     * Returns the number of participants currently associated
     * with this white-board session.
     *
     * @return an <tt>int</tt> indicating the number of participants currently
     * associated with this white-board.
     */
    public int getWhiteboardParticipantsCount()
    {
        return wbParticipants.size();
    }

    /**
     * Joins this white-board with the nickname of the local user so that the
     * user would start receiving events and WhiteboardObject for it.
     *
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    public void join() throws OperationFailedException
    {
        PacketExtensionFilter filterWhiteboard =
            new PacketExtensionFilter(
                WhiteboardObjectPacketExtension.NAMESPACE);

        this.jabberProvider.getConnection().addPacketListener(
            new WhiteboardSmackMessageListener(), filterWhiteboard);

        this.whiteboardOpSet.fireWhiteboardSessionPresenceEvent(
            this,
            WhiteboardSessionPresenceChangeEvent.LOCAL_USER_JOINED,
            null);
    }

    /**
     * Joins this white-board so that the user would start receiving events and
     * WhiteboardObject for it. The method uses the nickname of the local user
     * and the specified password in order to enter the white-board session.
     *
     * @param password the password to use when authenticating on the
     * white-board session.
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    public void join(byte[] password) throws OperationFailedException
    {
    }

    /**
     * Returns true if the local user is currently in the white-board session
     * (after white-boarding one of the {@link #join()} methods).
     *
     * @return true if currently we're currently in this white-board and false
     * otherwise.
     */
    public boolean isJoined()
    {
        return true;
    }

    /**
     * Leave this whiteboard. Once this method is whiteboarded, the user won't
     * be listed as a member of the whiteboard any more and no further
     * whiteboard events will be delivered. Depending on the underlying protocol
     * and implementation leave() might cause the room to be destroyed if it has
     * been created by the local user.
     */
    public void leave()
    {
        try
        {
            assertConnected();

            org.jivesoftware.smack.packet.Message msg =
                new org.jivesoftware.smack.packet.Message();

            WhiteboardSessionPacketExtension extension =
                new WhiteboardSessionPacketExtension(
                    this,
                    jabberProvider.getAccountID().getAccountAddress(),
                    WhiteboardSessionPacketExtension.ACTION_LEAVE);

            msg.addExtension(extension);
            //msg.addExtension(new Version());

            MessageEventManager.addNotificationsRequests(
                msg,
                true, // offline
                false, // delivered
                false, // displayed
                true); // composing

            smackChat.sendMessage(msg);
        }
        catch (XMPPException ex)
        {
            ex.printStackTrace();
            logger.error("message not send", ex);
        }

        // Inform all interested listeners that user has left the white board.
        whiteboardOpSet.fireWhiteboardSessionPresenceEvent(
            this,
            WhiteboardSessionPresenceChangeEvent.LOCAL_USER_LEFT,
            null);
    }

    /**
     * Invites another user to this room.
     * <p>
     * If the room is password-protected, the invitee will receive a password to
     * use to join the room. If the room is members-only, the the invitee may
     * be added to the member list.
     *
     * @param contactAddress the address of the user to invite to the room.
     * (one may also invite users not on their contact list).
     */
    public void invite(String contactAddress)
    {
        OperationSetPersistentPresenceJabberImpl presenceOpSet
            = (OperationSetPersistentPresenceJabberImpl) jabberProvider
                .getOperationSet(OperationSetPresence.class);

        // If there's no presence operation set we return, because there's
        // not contact to associate the event with.
        if (presenceOpSet == null)
            return;

        ContactJabberImpl sourceContact
            = (ContactJabberImpl) presenceOpSet.findContactByID(contactAddress);

        if (sourceContact == null)
        {
            sourceContact = presenceOpSet.createVolatileContact(contactAddress);
        }

        this.addWhiteboardParticipant(
            new WhiteboardParticipantJabberImpl(sourceContact, this));

        try
        {
            sendWhiteboardObject(
                createWhiteboardObject(WhiteboardObjectLine.NAME));
        }
        catch (OperationFailedException e)
        {
            logger.error("Could not send an invite whiteboard object.", e);
        }
    }

    /**
     * returns the current WhiteboardSession
     * @return current WhiteboardSession
     */
    public WhiteboardSession getWhiteboardSession()
    {
        return this;
    }

    /**
     * Verifies whether the whiteboard participant has entered a state.
     *
     * @param evt The <tt>WhiteboardParticipantChangeEvent</tt> instance
     * containing the source event as well as its previous and its new status.
     */
    public void participantStateChanged(WhiteboardParticipantChangeEvent evt)
    {
        Object newValue = evt.getNewValue();
        if ((newValue== WhiteboardParticipantState.DISCONNECTED)
                || (newValue == WhiteboardParticipantState.FAILED))
        {
            removeWhiteboardParticipant(evt
                .getSourceWhiteboardParticipant());
        }
    }

    /**
     * Indicates that a change has occurred in the display name of the source
     * WhiteboardParticipant.
     *
     * @param evt The <tt>WhiteboardParticipantChangeEvent</tt> instance
     * containing the source event as well as its previous and its new display
     * names.
     */
    public void participantDisplayNameChanged(
        WhiteboardParticipantChangeEvent evt)
    {
    }

    /**
     * Indicates that a change has occurred in the address of the source
     * WhiteboardParticipant.
     *
     * @param evt The <tt>WhiteboardParticipantChangeEvent</tt> instance
     * containing the source event as well as its previous and its new address.
     */
    public void participantAddressChanged(WhiteboardParticipantChangeEvent evt)
    {
    }

    /**
     * Indicates that a change has occurred in the transport address that we
     * use to communicate with the participant.
     *
     * @param evt The <tt>WhiteboardParticipantChangeEvent</tt> instance
     * containing the source event as well as its previous and its new transport
     * address.
     */
    public void participantTransportAddressChanged(
        WhiteboardParticipantChangeEvent evt)
    {
    }

    /**
     * Indicates that a change has occurred in the image of the source
     * WhiteboardParticipant.
     *
     * @param evt The <tt>WhiteboardParticipantChangeEvent</tt> instance
     * containing the source event as well as its previous and its new image.
     */
    public void participantImageChanged(WhiteboardParticipantChangeEvent evt)
    {
    }

    /**
     * Adds <tt>wbParticipant</tt> to the list of participants in this
     * white-board.
     * If the white-board participant is already included in the white-board,
     * the method has no effect.
     *
     * @param wbParticipant the new <tt>WhiteboardParticipant</tt>
     */
    public void addWhiteboardParticipant(WhiteboardParticipant wbParticipant)
    {
        if (wbParticipants.containsKey(wbParticipant.getContactAddress()))
            return;

        wbParticipant.addWhiteboardParticipantListener(this);

        this.wbParticipants.put(
            wbParticipant.getContactAddress(), wbParticipant);

        this.smackChat = jabberProvider.getConnection().getChatManager()
            .createChat(wbParticipant.getContactAddress(), null);

        fireWhiteboardParticipantEvent(wbParticipant,
            WhiteboardParticipantEvent.WHITEBOARD_PARTICIPANT_ADDED);
    }

    /**
     * Removes <tt>whiteboardParticipant</tt> from the list of participants in
     * this whiteboard. The method has no effect if there was no
     * such participant in the whiteboard.
     *
     * @param wbParticipant the <tt>WhiteboardParticipant</tt> leaving the
     * whiteboard;
     */
    public void removeWhiteboardParticipant(WhiteboardParticipant wbParticipant)
    {
        if (!wbParticipants.containsKey(wbParticipant.getContactAddress()))
            return;

        this.wbParticipants.remove(wbParticipant.getContactAddress());

        if (wbParticipant instanceof WhiteboardParticipantJabberImpl)
            ((WhiteboardParticipantJabberImpl) wbParticipant)
                .setWhiteboardSession(null);

        wbParticipant.removeWhiteboardParticipantListener(this);

        fireWhiteboardParticipantEvent(wbParticipant,
            WhiteboardParticipantEvent.WHITEBOARD_PARTICIPANT_REMOVED);

        if (wbParticipants.isEmpty())
            setWhiteboardSessionState(WhiteboardSessionState.WHITEBOARD_ENDED);
    }

    /**
     * Sets the state of this whiteboard and fires a whiteboard change event
     * notifying registered listeners for the change.
     *
     * @param newState a reference to the <tt>WhiteboardState</tt> instance that
     * the whiteboard is to enter.
     */
    public void setWhiteboardSessionState(WhiteboardSessionState newState)
    {
        WhiteboardSessionState oldState = getWhiteboardSessionState();

        if (oldState == newState)
            return;

        this.whiteboardState = newState;

        fireWhiteboardChangeEvent(
            WhiteboardChangeEvent.WHITEBOARD_STATE_CHANGE, oldState, newState);
    }

    /**
     * Returns the state that this whiteboard is currently in.
     * @return a reference to the <tt>WhiteboardState</tt>
     * instance that the whiteboard is currently in.
     */
    public WhiteboardSessionState getWhiteboardSessionState()
    {
        return whiteboardState;
    }

    /**
     * Registers <tt>listener</tt> so that it would receive events every time a
     * new WhiteboardObject is received on this whiteboard.
     *
     *
     * @param listener a <tt>WhiteboardObjectListener</tt> that would be
     * notified every time a new WhiteboardObject
     * is received on this whiteboard.
     */
    public void addWhiteboardObjectListener(WhiteboardObjectListener listener)
    {
        synchronized (messageListeners)
        {
            if (!messageListeners.contains(listener))
            {
                this.messageListeners.add(listener);
            }
        }
    }

    /**
     * Removes <tt>listener</tt> so that it won't receive
     * any further WhiteboardObject events from this room.
     *
     *
     * @param listener the <tt>WhiteboardObjectListener</tt>
     * to remove from this room
     */
    public void removeWhiteboardObjectListener(
        WhiteboardObjectListener listener)
    {
        synchronized (messageListeners)
        {
            this.messageListeners.remove(listener);
        }
    }

    /**
     * Create a WhiteboardObject instance with the specified type. This method
     * only creates the object locally and it would not be visible to other
     * session participants until it is resolved with the
     * sendWhiteboardObject(WhiteboardObject) method.
     *
     * @param name the name of the object to create (should be one of the
     * WhiteboardObjectXXX.NAME fields).
     *
     * @return the newly created WhiteboardObject with an id
     */
    public WhiteboardObject createWhiteboardObject(String name)
    {
        WhiteboardObjectJabberImpl wbObj = null;
        if (logger.isDebugEnabled())
            logger.debug("[log] WhiteboardObjectXXX.NAME: " + name);
        if (name.equals(WhiteboardObjectPath.NAME))
        {
            wbObj = new WhiteboardObjectPathJabberImpl();
        }
        else if (name.equals(WhiteboardObjectPolyLine.NAME))
        {
            wbObj = new WhiteboardObjectPolyLineJabberImpl();
        }
        else if (name.equals(WhiteboardObjectPolygon.NAME))
        {
            wbObj = new WhiteboardObjectPolygonJabberImpl();
        }
        else if (name.equals(WhiteboardObjectLine.NAME))
        {
            wbObj = new WhiteboardObjectLineJabberImpl();
        }
        else if (name.equals(WhiteboardObjectRect.NAME))
        {
            wbObj = new WhiteboardObjectRectJabberImpl();
        }
        else if (name.equals(WhiteboardObjectCircle.NAME))
        {
            wbObj = new WhiteboardObjectCircleJabberImpl();
        }
        else if (name.equals(WhiteboardObjectText.NAME))
        {
            wbObj = new WhiteboardObjectTextJabberImpl();
        }
        else if (name.equals(WhiteboardObjectImage.NAME))
        {
            wbObj = new WhiteboardObjectImageJabberImpl();
        }
        whiteboardObjects.add(wbObj);
        return wbObj;
    }

    /**
     * Returns the id of the specified Whiteboard.
     *
     * @return a String uniquely identifying the whiteboard.
     */
    public String getWhiteboardID()
    {
        return whiteboardID;
    }

    /**
     * Determines wheter the protocol provider (or the protocol itself) support
     * sending and receiving offline messages. Most often this method would
     * return true for protocols that support offline messages and false for
     * those that don't. It is however possible for a protocol to support these
     * messages and yet have a particular account that does not (i.e. feature
     * not enabled on the protocol server). In cases like this it is possible
     * for this method to return true even when offline messaging is not
     * supported, and then have the sendMessage method throw an
     * OperationFailedException with code - OFFLINE_MESSAGES_NOT_SUPPORTED.
     *
     * @return <tt>true</tt> if the protocol supports offline messages and
     * <tt>false</tt> otherwise.
     */
    public boolean isOfflineMessagingSupported()
    {
        return true;
    }

    /**
     * Sends a <tt>WhiteboardObject</tt> to modify
     * and modifies the local <tt>WhiteboardObject</tt>
     *
     * @param obj the <tt>WhiteboardObject</tt> to send and modify
     * @throws OperationFailedException if sending
     * the WhiteboardObject fails for some reason.
     */
    public void moveWhiteboardObject(WhiteboardObject obj)
        throws OperationFailedException
    {
        WhiteboardObject wbObj = updateWhiteboardObjects(obj);
        if (wbObj != null)
            sendWhiteboardObject(wbObj);
    }

    /**
     * Sends a <tt>WhiteboardObject</tt> to delete
     * and delete the local <tt>WhiteboardObject</tt>
     *
     * @param obj the <tt>WhiteboardObject</tt> to send and delete
     * @throws OperationFailedException if sending
     * the WhiteboardObject fails for some reason.
     */
    public void deleteWhiteboardObject(WhiteboardObject obj)
        throws OperationFailedException
    {
        Iterator<WhiteboardParticipant> participants
            = getWhiteboardParticipants();
        if (!participants.hasNext())
            return;

        WhiteboardParticipantJabberImpl participant
            = (WhiteboardParticipantJabberImpl) participants.next();
        Contact contact = participant.getContact();

        try
        {
            assertConnected();

            org.jivesoftware.smack.packet.Message msg =
                new org.jivesoftware.smack.packet.Message();

            WhiteboardObjectPacketExtension messageJI =
                new WhiteboardObjectPacketExtension(obj.getID(),
                    WhiteboardObjectPacketExtension.ACTION_DELETE);

            msg.addExtension(messageJI);
            //msg.addExtension(new Version());

            MessageEventManager.addNotificationsRequests(msg, true, false,
                false, true);

            smackChat.sendMessage(msg);

            WhiteboardObjectDeliveredEvent msgDeliveredEvt =
                new WhiteboardObjectDeliveredEvent(
                    this, obj, contact, new Date());

            fireMessageEvent(msgDeliveredEvt);

            int i = 0;
            while (i < whiteboardObjects.size())
            {
                WhiteboardObjectJabberImpl wbObj = (WhiteboardObjectJabberImpl)whiteboardObjects.get(i);
                if (wbObj.getID().equals(obj.getID()))
                    whiteboardObjects.remove(i);
                else
                    i++;
            }
        }
        catch (XMPPException ex)
        {
            ex.printStackTrace();
            logger.error("message not send", ex);
        }

    }

    /**
     * Sends the <tt>message</tt> to the destination.
     * @param message the <tt>Message</tt> to send.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>to</tt> is not an
     * instance of ContactImpl.
     */
    public void sendWhiteboardObject(WhiteboardObject message)
        throws OperationFailedException
    {
        Iterator<WhiteboardParticipant> participants
            = getWhiteboardParticipants();

        if (!participants.hasNext())
            return;

        WhiteboardParticipantJabberImpl participant
            = (WhiteboardParticipantJabberImpl) participants.next();
        Contact contact = participant.getContact();

        try
        {
            assertConnected();

            org.jivesoftware.smack.packet.Message msg =
                new org.jivesoftware.smack.packet.Message();

            WhiteboardObjectPacketExtension messageJI =
                new WhiteboardObjectPacketExtension(
                    (WhiteboardObjectJabberImpl) message,
                    WhiteboardObjectPacketExtension.ACTION_DRAW);

            msg.addExtension(messageJI);
            //msg.addExtension(new Version());

            MessageEventManager.addNotificationsRequests(msg, true, false,
                false, true);

            smackChat.sendMessage(msg);

            WhiteboardObjectDeliveredEvent msgDeliveredEvt =
                new WhiteboardObjectDeliveredEvent(
                    this, message, contact, new Date());

            fireMessageEvent(msgDeliveredEvt);
        }
        catch (XMPPException ex)
        {
            ex.printStackTrace();
            logger.error("message not send", ex);
        }
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     */
    private void assertConnected() throws IllegalStateException
    {
        if (jabberProvider == null)
            throw new IllegalStateException(
                "The provider must be non-null and signed on the "
                    + "service before being able to communicate.");
        if (!jabberProvider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the service before "
                    + "being able to communicate.");
    }

    /**
     * Compares the specified object with this whiteboard and returns true if it
     * the specified object is an instance of a Whiteboard object and if the
     * extending telephony protocol considers the whiteboards represented by
     * both objects to be the same.
     *
     * @param obj the whiteboard to compare this one with.
     * @return true in case both objects are pertaining to the same whiteboard
     * and false otherwise.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || !(obj instanceof WhiteboardSession))
            return false;
        if (obj == this
            || ((WhiteboardSession) obj).getWhiteboardID().equals(
                getWhiteboardID()))
            return true;

        return false;
    }

    /**
     * Returns a hash code value for this whiteboard.
     *
     * @return  a hash code value for this whiteboard.
     */
    @Override
    public int hashCode()
    {
        return getWhiteboardID().hashCode();
    }

    /**
     * Returns a string textually representing this Whiteboard.
     *
     * @return  a string representation of the object.
     */
    @Override
    public String toString()
    {
        return "Whiteboard: id=" + getWhiteboardID() + " participants="
            + getWhiteboardParticipantsCount();
    }

    /**
     * Adds a whiteboard change listener to this whiteboard so that it could
     * receive events on new whiteboard participants, theme changes and others.
     *
     * @param listener the listener to register
     */
    public void addWhiteboardChangeListener(WhiteboardChangeListener listener)
    {
        synchronized (whiteboardListeners)
        {
            if (!whiteboardListeners.contains(listener))
                this.whiteboardListeners.add(listener);
        }
    }

    /**
     * Removes <tt>listener</tt> to this whiteboard so that it won't receive
     * further <tt>WhiteboardChangeEvent</tt>s.
     *
     * @param listener the listener to register
     */
    public void removeWhiteboardChangeListener(WhiteboardChangeListener listener)
    {
        synchronized (whiteboardListeners)
        {
            this.whiteboardListeners.remove(listener);
        }
    }

    /**
     * Returns a reference to the <tt>ProtocolProviderService</tt> instance
     * that created this whiteboard.
     *
     * @return a reference to the <tt>ProtocolProviderService</tt> instance that
     * created this whiteboard.
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return this.jabberProvider;
    }

    /**
     * Creates a <tt>WhiteboardParticipantEvent</tt> with
     * <tt>sourceWhiteboardParticipant</tt> and <tt>eventID</tt> and dispatches
     * it on all currently registered listeners.
     *
     * @param sourceWhiteboardParticipant the source
     * <tt>WhiteboardParticipant</tt> for the newly created event.
     * @param eventID the ID of the event to create (see CPE member ints)
     */
    public void fireWhiteboardParticipantEvent(
        WhiteboardParticipant sourceWhiteboardParticipant, int eventID)
    {
        WhiteboardParticipantEvent cpEvent =
            new WhiteboardParticipantEvent(this, sourceWhiteboardParticipant,
                eventID);

        if (logger.isDebugEnabled())
            logger.debug("Dispatching a WhiteboardParticipant event to "
            + whiteboardListeners.size() + " listeners. event is: "
            + cpEvent.toString());

        Iterable<WhiteboardChangeListener> listeners;
        synchronized (whiteboardListeners)
        {
            listeners
                = new ArrayList<WhiteboardChangeListener>(whiteboardListeners);
        }

        for (WhiteboardChangeListener listener : listeners)
        {
            if (eventID
                    == WhiteboardParticipantEvent
                        .WHITEBOARD_PARTICIPANT_ADDED)
            {
                listener.whiteboardParticipantAdded(cpEvent);
            }
            else if (eventID
                    == WhiteboardParticipantEvent
                        .WHITEBOARD_PARTICIPANT_REMOVED)
            {
                listener.whiteboardParticipantRemoved(cpEvent);
            }
        }
    }

    /**
     * Creates a <tt>WhiteboardChangeEvent</tt> with this class as
     * <tt>sourceWhiteboard</tt>,  and the specified <tt>eventID</tt> and old
     * and new values and  dispatches it on all currently registered listeners.
     *
     * @param type the type of the event to create (see WhiteboardChangeEvent
     * member ints)
     * @param oldValue the value of the Whiteboard property that changed, before
     * the event had occurred.
     * @param newValue the value of the Whiteboard property that changed, after
     * the event has occurred.
     */
    public void fireWhiteboardChangeEvent(String type, Object oldValue,
        Object newValue)
    {
        WhiteboardChangeEvent ccEvent =
            new WhiteboardChangeEvent(this, type, oldValue, newValue);

        if (logger.isDebugEnabled())
            logger.debug("Dispatching a WhiteboardChange event to "
            + whiteboardListeners.size() + " listeners. event is: "
            + ccEvent.toString());

        Iterable<WhiteboardChangeListener> listeners;
        synchronized (whiteboardListeners)
        {
            listeners
                = new ArrayList<WhiteboardChangeListener>(whiteboardListeners);
        }

        for (WhiteboardChangeListener listener : listeners)
        {
            if (type.equals(WhiteboardChangeEvent.WHITEBOARD_STATE_CHANGE))
                listener.whiteboardStateChanged(ccEvent);
        }
    }

    /**
     * Returns the WhiteboardObjects in this whiteboard session.
     * @return an <tt>Vector</tt> of WhiteboardObjects associated
     * with this whiteboard.
     */
    public Vector<WhiteboardObject> getWhiteboardObjects()
    {
        return whiteboardObjects;
    }

    /**
     * Sets the state of this whiteboard
     *
     * @param newState a reference to the <tt>WhiteboardState</tt> instance that
     * the whiteboard is to enter.
     */
    public void setState(WhiteboardSessionState newState)
    {
        this.whiteboardState = newState;
    }

    /**
     * Returns the state that this whiteboard is currently in.
     *
     * @return a reference to the <tt>WhiteboardState</tt> instance
     * that the whiteboard is currently in.
     */
    public WhiteboardSessionState getState()
    {
        return this.whiteboardState;
    }

    /**
     * Delivers the specified event to all registered message listeners.
     * @param evt the <tt>EventObject</tt> that we'd like delivered to all
     * registered message listeners.
     */
    public void fireMessageEvent(EventObject evt)
    {
        if (logger.isDebugEnabled())
            logger.debug("Dispatching a WhiteboardMessageEvent event to "
            + messageListeners.size() + " listeners. event is: "
            + evt.toString());

        Iterable<WhiteboardObjectListener> listeners;
        synchronized (messageListeners)
        {
            listeners
                = new ArrayList<WhiteboardObjectListener>(messageListeners);
        }

        for (WhiteboardObjectListener listener : listeners)
        {
            if (evt instanceof WhiteboardObjectDeliveredEvent)
            {
                listener.whiteboardObjectDelivered(
                    (WhiteboardObjectDeliveredEvent) evt);
            }
            else if (evt instanceof WhiteboardObjectReceivedEvent)
            {
                WhiteboardObjectJabberImpl wbObj =
                    (WhiteboardObjectJabberImpl) (
                        (WhiteboardObjectReceivedEvent) evt)
                        .getSourceWhiteboardObject();

                listener.whiteboardObjectReceived(
                    (WhiteboardObjectReceivedEvent) evt);

                whiteboardObjects.add(wbObj);
            }
            else if (evt instanceof WhiteboardObjectDeletedEvent)
            {
                String wbObjID = ((WhiteboardObjectDeletedEvent) evt).getId();

                listener
                    .whiteboardObjectDeleted((WhiteboardObjectDeletedEvent) evt);
                int i = 0;
                while (i < whiteboardObjects.size())
                {
                    WhiteboardObjectJabberImpl wbObj = (WhiteboardObjectJabberImpl)whiteboardObjects.get(i);
                    if (wbObj.getID().equals(wbObjID))
                        whiteboardObjects.remove(i);
                    else
                        i++;
                }

            }
            else if (evt instanceof WhiteboardObjectModifiedEvent)
            {
                WhiteboardObjectModifiedEvent womevt
                    = (WhiteboardObjectModifiedEvent) evt;
                WhiteboardObjectJabberImpl wbObj
                    = (WhiteboardObjectJabberImpl)
                        womevt.getSourceWhiteboardObject();

                listener.whiteboardObjecModified(womevt);

                whiteboardObjects.remove(wbObj);//remove the old id object
                whiteboardObjects.add(wbObj); //add the new object for this id
            }
            else if (evt instanceof WhiteboardObjectDeliveryFailedEvent)
            {
                listener.whiteboardObjectDeliveryFailed(
                    (WhiteboardObjectDeliveryFailedEvent) evt);
            }
        }
    }

    private WhiteboardObject updateWhiteboardObjects(WhiteboardObject ws)
    {
        WhiteboardObjectJabberImpl wbObj = null;
        int i = 0;
        while (i < whiteboardObjects.size())
        {
            WhiteboardObjectJabberImpl wbObjTmp = (WhiteboardObjectJabberImpl)whiteboardObjects.get(i);
            if (wbObjTmp.getID().equals(ws.getID()))
            {
                wbObj = wbObjTmp;
                break;
            }
            else
                i++;
        }
        if (wbObj == null)
            return null;

        if (ws instanceof WhiteboardObjectPath)
        {
            WhiteboardObjectPathJabberImpl obj =
                (WhiteboardObjectPathJabberImpl) wbObj;
            obj.setPoints(((WhiteboardObjectPath) ws).getPoints());
            obj.setColor(ws.getColor());
            obj.setThickness(ws.getThickness());
        }
        else if (ws instanceof WhiteboardObjectPolyLine)
        {
            WhiteboardObjectPolyLineJabberImpl obj =
                (WhiteboardObjectPolyLineJabberImpl) wbObj;
            obj.setPoints(((WhiteboardObjectPolyLine) ws).getPoints());
            obj.setColor(ws.getColor());
            obj.setThickness(ws.getThickness());
        }
        else if (ws instanceof WhiteboardObjectPolygon)
        {
            WhiteboardObjectPolygonJabberImpl obj =
                (WhiteboardObjectPolygonJabberImpl) wbObj;
            obj.setPoints(((WhiteboardObjectPolygon) ws).getPoints());
            obj.setBackgroundColor(((WhiteboardObjectPolygon) ws)
                .getBackgroundColor());
            obj.setFill(((WhiteboardObjectPolygon) ws).isFill());
            obj.setColor(ws.getColor());
            obj.setThickness(ws.getThickness());
        }
        else if (ws instanceof WhiteboardObjectLine)
        {
            WhiteboardObjectLineJabberImpl obj =
                (WhiteboardObjectLineJabberImpl) wbObj;
            obj.setWhiteboardPointStart(((WhiteboardObjectLine) ws)
                .getWhiteboardPointStart());
            obj.setWhiteboardPointEnd(((WhiteboardObjectLine) ws)
                .getWhiteboardPointEnd());
            obj.setColor(ws.getColor());
            obj.setThickness(ws.getThickness());
        }
        else if (ws instanceof WhiteboardObjectRect)
        {
            WhiteboardObjectRectJabberImpl obj =
                (WhiteboardObjectRectJabberImpl) wbObj;
            obj.setFill(((WhiteboardObjectRect) ws).isFill());
            obj.setHeight(((WhiteboardObjectRect) ws).getHeight());
            obj.setWhiteboardPoint(((WhiteboardObjectRect) ws)
                .getWhiteboardPoint());
            obj.setWidth((((WhiteboardObjectRect) ws)).getWidth());
            obj.setColor(ws.getColor());
            obj.setThickness(ws.getThickness());
        }
        else if (ws instanceof WhiteboardObjectCircle)
        {
            WhiteboardObjectCircleJabberImpl obj =
                (WhiteboardObjectCircleJabberImpl) wbObj;
            obj.setFill(((WhiteboardObjectCircle) ws).isFill());
            obj.setRadius(((WhiteboardObjectCircle) ws).getRadius());
            obj.setWhiteboardPoint(((WhiteboardObjectCircle) ws)
                .getWhiteboardPoint());
            obj.setBackgroundColor((((WhiteboardObjectCircle) ws))
                .getBackgroundColor());
            obj.setColor(ws.getColor());
            obj.setThickness(ws.getThickness());
        }
        else if (ws instanceof WhiteboardObjectText)
        {
            WhiteboardObjectTextJabberImpl obj =
                (WhiteboardObjectTextJabberImpl) wbObj;
            obj.setFontName(((WhiteboardObjectText) ws).getFontName());
            obj.setFontSize(((WhiteboardObjectText) ws).getFontSize());
            obj.setText(((WhiteboardObjectText) ws).getText());
            obj.setWhiteboardPoint(((WhiteboardObjectText) ws)
                .getWhiteboardPoint());
            obj.setColor(ws.getColor());
            obj.setThickness(ws.getThickness());
        }
        else if (ws instanceof WhiteboardObjectImage)
        {
            WhiteboardObjectImageJabberImpl obj =
                (WhiteboardObjectImageJabberImpl) wbObj;
            obj.setBackgroundImage(((WhiteboardObjectImage) ws)
                .getBackgroundImage());
            obj.setHeight(((WhiteboardObjectImage) ws).getHeight());
            obj.setWhiteboardPoint(((WhiteboardObjectImage) ws)
                .getWhiteboardPoint());
            obj.setWidth(((WhiteboardObjectImage) ws).getWidth());
            obj.setColor(ws.getColor());
            obj.setThickness(ws.getThickness());
        }
        whiteboardObjects.set(i, wbObj);
        return wbObj;
    }

    /**
     * Returns all the type of WhiteboardObject that this whiteboard support.
     *
     * @return all the WhiteboardObject supported by this WhiteboardSession.
     */
    public String[] getSupportedWhiteboardObjects()
    {
        String[] type = new String[8];
        type[0] = WhiteboardObjectPath.NAME;
        type[1] = WhiteboardObjectPolyLine.NAME;
        type[2] = WhiteboardObjectPolygon.NAME;
        type[3] = WhiteboardObjectLine.NAME;
        type[4] = WhiteboardObjectRect.NAME;
        type[5] = WhiteboardObjectCircle.NAME;
        type[6] = WhiteboardObjectText.NAME;
        type[7] = WhiteboardObjectImage.NAME;

        return type;
    }

    /**
     * Listens for white-board messages and fires the appropriate events to
     * notify all interested listeners.
     */
    private class WhiteboardSmackMessageListener
        implements PacketListener
    {
        public void processPacket(Packet packet)
        {
            if (!(packet instanceof org.jivesoftware.smack.packet.Message))
                return;

            PacketExtension objectExt =
                packet.getExtension(
                    WhiteboardObjectPacketExtension.ELEMENT_NAME,
                    WhiteboardObjectPacketExtension.NAMESPACE);

            PacketExtension sessionExt =
                packet.getExtension(
                    WhiteboardSessionPacketExtension.ELEMENT_NAME,
                    WhiteboardSessionPacketExtension.NAMESPACE);

            org.jivesoftware.smack.packet.Message msg =
                (org.jivesoftware.smack.packet.Message) packet;

            if (sessionExt != null)
            {
                WhiteboardSessionPacketExtension sessionMessage
                    = (WhiteboardSessionPacketExtension) sessionExt;

                if (sessionMessage.getAction().equals(
                    WhiteboardSessionPacketExtension.ACTION_LEAVE))
                {
                    fireWhiteboardParticipantEvent(
                        findWhiteboardParticipantFromContactAddress(
                            sessionMessage.getContactAddress()),
                        WhiteboardParticipantEvent
                            .WHITEBOARD_PARTICIPANT_REMOVED);
                }
            }

            if (objectExt == null)
                return;

            String fromUserID = StringUtils.parseBareAddress(msg.getFrom());

            if (logger.isDebugEnabled())
            {
                logger.debug("Received from " + fromUserID + " the message "
                    + msg.toXML());
            }

            OperationSetPersistentPresenceJabberImpl presenceOpSet
                = (OperationSetPersistentPresenceJabberImpl) jabberProvider
                    .getOperationSet(OperationSetPresence.class);

            // If there's no presence operation set we return, because there's
            // not contact to associate the event with.
            if (presenceOpSet == null)
                return;

            Contact sourceContact
                = presenceOpSet.findContactByID(fromUserID);

            // If the sender is not our contact we don't care of this message
            if (!wbParticipants.containsKey(sourceContact.getAddress()))
                return;

            WhiteboardObjectPacketExtension newMessage
                = (WhiteboardObjectPacketExtension) objectExt;

            if (msg.getType()
                    == org.jivesoftware.smack.packet.Message.Type.error)
            {
                if (logger.isInfoEnabled())
                    logger.info("WBObject error received from " + fromUserID);

                int errorCode = packet.getError().getCode();
                int errorResultCode =
                    WhiteboardObjectDeliveryFailedEvent.UNKNOWN_ERROR;

                if (errorCode == 503)
                {
                    org.jivesoftware.smackx.packet.MessageEvent msgEvent =
                        (org.jivesoftware.smackx.packet.MessageEvent) packet
                            .getExtension("x", "jabber:x:event");
                    if (msgEvent != null && msgEvent.isOffline())
                    {
                        errorResultCode
                            = WhiteboardObjectDeliveryFailedEvent
                                .OFFLINE_MESSAGES_NOT_SUPPORTED;
                    }
                }

                WhiteboardObjectDeliveryFailedEvent evt =
                    new WhiteboardObjectDeliveryFailedEvent(
                        WhiteboardSessionJabberImpl.this,
                        newMessage.getWhiteboardObject(),
                        sourceContact,
                        errorResultCode,
                        new Date());

                fireMessageEvent(evt);

                return;
            }

            if (newMessage.getAction().equals(
                WhiteboardObjectPacketExtension.ACTION_DELETE))
            {
                WhiteboardObjectDeletedEvent msgDeletedEvt
                    = new WhiteboardObjectDeletedEvent(
                            WhiteboardSessionJabberImpl.this,
                            newMessage.getWhiteboardObjectID(),
                            sourceContact,
                            new Date());

                fireMessageEvent(msgDeletedEvt);
            }
            else if (newMessage.getAction().equals(
                WhiteboardObjectPacketExtension.ACTION_DRAW))
            {
                WhiteboardObjectReceivedEvent msgReceivedEvt
                    = new WhiteboardObjectReceivedEvent(
                        WhiteboardSessionJabberImpl.this,
                        newMessage.getWhiteboardObject(),
                        sourceContact,
                        new Date());

                fireMessageEvent(msgReceivedEvt);
            }
        }
    }

    /**
     * Checks if the participant given by <tt>participantName</tt> is contained
     * in this white-board session.
     *
     * @param participantName the name of the participant to search for
     * @return <code>true</code> if a participant with the given name is
     * contained in this session, <code>false</code> - otherwise
     */
    public boolean isParticipantContained(String participantName)
    {
        if (wbParticipants.containsKey(participantName))
            return true;

        return false;
    }

    /**
     * Searches all participants contained in this white board and returns the
     * one that corresponds to the given contact address.
     *
     * @param contactAddress the address of the contact to search for.
     * @return the <tt>WhiteboardParticipant</tt>, contained in this
     * white board session and corresponding to the given contact address
     */
    private WhiteboardParticipant findWhiteboardParticipantFromContactAddress(
        String contactAddress)
    {
        for (WhiteboardParticipant participant : wbParticipants.values())
            if (participant.getContactAddress().equals(contactAddress))
                return participant;
        return null;
    }
}
