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

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The WhiteboardParticipantJabberImpl is a class that represents participants
 * in a whiteboard.
 *
 * @author Julien Waechter
 */
public class WhiteboardParticipantJabberImpl
    implements WhiteboardParticipant
{
    /**
     * The logger of this class.
     */
    private static final Logger logger =
        Logger.getLogger(WhiteboardParticipantJabberImpl.class);

    /**
     * The participant
     */
    private ContactJabberImpl participant = null;

    /**
     * The state of the whiteboard participant.
     */
    protected WhiteboardParticipantState whiteboardParticipantState =
        WhiteboardParticipantState.UNKNOWN;

    /**
     * Indicates the date when is whiteboard participant passed into its current
     * state.
     */
    protected Date currentStateStartDate = new Date();

    /**
     * A byte array containing the image/photo representing the whiteboard
     * participant.
     */
    private byte[] image;

    /**
     * A string uniquely identifying the participant.
     */
    private String participantID;

    /**
     * The whiteboard this participant belongs to.
     */
    private WhiteboardSessionJabberImpl whiteboard;

    /**
     * Creates a new whiteboard participant with address
     * <tt>participantAddress</tt>.
     *
     * @param participant the JAIN SIP <tt>Address</tt> of the new whiteboard
     *            participant.
     *
     * @param owningWhiteboard the whiteboard that contains this whiteboard
     *            participant.
     */
    public WhiteboardParticipantJabberImpl(ContactJabberImpl participant,
        WhiteboardSessionJabberImpl owningWhiteboard)
    {
        this.participant = participant;
        this.whiteboard = owningWhiteboard;
        whiteboard.addWhiteboardParticipant(this);

        // create the uid
        this.participantID =
            String.valueOf(System.currentTimeMillis())
                + String.valueOf(hashCode());
    }

    /**
     * Returns the contact identifier representing this contact.
     *
     * @return a String contact address
     */
    public String getContactAddress()
    {
        return this.participant.getAddress();
    }

    /**
     * Returns an object representing the current state of that participant.
     * WhiteboardParticipantState may vary among CONNECTING, BUSY, CONNECTED...
     *
     * @return a WhiteboardParticipantState instance representin the
     *         participant's state.
     */
    public WhiteboardParticipantState getState()
    {
        return whiteboardParticipantState;
    }

    /**
     * Causes this WhiteboardParticipant to enter the specified state. The
     * method also sets the currentStateStartDate field and fires a
     * WhiteboardParticipantChangeEvent.
     *
     * @param newState the state this whiteboard participant should enter.
     * @param reason a string that could be set to contain a human readable
     *            explanation for the transition (particularly handy when moving
     *            into a FAILED state).
     */
    protected void setState(WhiteboardParticipantState newState, String reason)
    {
        WhiteboardParticipantState oldState = getState();

        if (oldState == newState)
            return;

        this.whiteboardParticipantState = newState;
        this.currentStateStartDate = new Date();
        fireWhiteboardParticipantChangeEvent(
            WhiteboardParticipantChangeEvent.WHITEBOARD_PARTICIPANT_STATE_CHANGE,
            oldState, newState);
    }

    /**
     * Causes this WhiteboardParticipant to enter the specified state. The
     * method also sets the currentStateStartDate field and fires a
     * WhiteboardParticipantChangeEvent.
     *
     * @param newState the state this whiteboard participant should enter.
     */
    protected void setState(WhiteboardParticipantState newState)
    {
        setState(newState, null);
    }

    /**
     * Returns the date (time) when this whiteboard participant acquired its
     * current status.
     *
     * @return a java.util.Date object containing the date when this whiteboard
     *         participant entered its current state.
     */
    public Date getCurrentStateStartDate()
    {
        return currentStateStartDate;
    }

    /**
     * Returns a human readable name representing this participant.
     *
     * @return a String containing a name for that participant.
     */
    public String getDisplayName()
    {
        String displayName = participant.getDisplayName();
        return (displayName == null) ? "" : displayName;
    }

    /**
     * Sets a human readable name representing this participant.
     *
     * @param displayName the participant's display name
     */
    protected void setDisplayName(String displayName)
    {
        String oldName = getDisplayName();
        /*
         * try { this.participant.setDisplayName(displayName); } catch
         * (ParseException ex) { //couldn't happen logger.error(ex.getMessage(),
         * ex); throw new IllegalArgumentException(ex.getMessage()); }
         */
        // Fire the Event
        fireWhiteboardParticipantChangeEvent(
            WhiteboardParticipantChangeEvent.WHITEBOARD_PARTICIPANT_DISPLAY_NAME_CHANGE,
            oldName, displayName);
    }

    /**
     * The method returns an image representation of the whiteboard participant
     * (e.g.
     *
     * @return byte[] a byte array containing the image or null if no image is
     *         available.
     */
    public byte[] getImage()
    {
        return image;
    }

    /**
     * Sets the byte array containing an image representation (photo or picture)
     * of the whiteboard participant.
     *
     * @param image a byte array containing the image
     */
    protected void setImage(byte[] image)
    {
        byte[] oldImage = getImage();
        this.image = image;

        // Fire the Event
        fireWhiteboardParticipantChangeEvent(
            WhiteboardParticipantChangeEvent.WHITEBOARD_PARTICIPANT_IMAGE_CHANGE,
            oldImage, image);
    }

    /**
     * Returns a unique identifier representing this participant.
     *
     * @return an identifier representing this whiteboard participant.
     */
    public String getParticipantID()
    {
        return participantID;
    }

    /**
     * Sets the String that serves as a unique identifier of this
     * WhiteboardParticipant.
     *
     * @param participantID the ID of this whiteboard participant.
     */
    protected void setParticipantID(String participantID)
    {
        this.participantID = participantID;
    }

    /**
     * Returns the chat room that this member is participating in.
     *
     * @return the <tt>WhiteboardSession</tt> instance that this member
     *         belongs to.
     */
    public WhiteboardSession getWhiteboardSession()
    {
        return whiteboard;
    }

    /**
     * Sets the whiteboard containing this participant.
     *
     * @param whiteboard the whiteboard that this whiteboard participant is
     *            partdicipating in.
     */
    protected void setWhiteboard(WhiteboardSessionJabberImpl whiteboard)
    {
        this.whiteboard = whiteboard;
    }

    /**
     * Returns the protocol provider instance that this member has originated
     * in.
     *
     * @return the <tt>ProtocolProviderService</tt> instance that created this
     *         member and its containing cht room
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return this.getWhiteboardSession().getProtocolProvider();
    }

    /**
     * Returns the contact corresponding to this participant or null if no
     * particular contact has been associated.
     * <p>
     *
     * @return the <tt>Contact</tt> corresponding to this participant or null
     *         if no particular contact has been associated.
     */
    public Contact getContact()
    {
        return this.participant;
    }

    /**
     * All the WhiteboardParticipant listeners registered with this
     * WhiteboardParticipant.
     */
    protected final List<WhiteboardParticipantListener> whiteboardParticipantListeners
        = new ArrayList<WhiteboardParticipantListener>();

    /**
     * Allows the user interface to register a listener interested in changes
     *
     * @param listener a listener instance to register with this participant.
     */
    public void addWhiteboardParticipantListener(
        WhiteboardParticipantListener listener)
    {
        synchronized (whiteboardParticipantListeners)
        {
            if (!whiteboardParticipantListeners.contains(listener))
                this.whiteboardParticipantListeners.add(listener);
        }
    }

    /**
     * Unregisters the specified listener.
     *
     * @param listener the listener to unregister.
     */
    public void removeWhiteboardParticipantListener(
        WhiteboardParticipantListener listener)
    {

        synchronized (whiteboardParticipantListeners)
        {
            if (listener == null)
                return;
            whiteboardParticipantListeners.remove(listener);
        }
    }

    /**
     * Constructs a <tt>WhiteboardParticipantChangeEvent</tt> using this
     * whiteboard participant as source, setting it to be of type
     * <tt>eventType</tt> and the corresponding <tt>oldValue</tt> and
     * <tt>newValue</tt>,
     *
     * @param eventType the type of the event to create and dispatch.
     * @param oldValue the value of the source property before it changed.
     * @param newValue the current value of the source property.
     */
    protected void fireWhiteboardParticipantChangeEvent(String eventType,
        Object oldValue, Object newValue)
    {
        this.fireWhiteboardParticipantChangeEvent(eventType, oldValue,
            newValue, null);
    }

    /**
     * Constructs a <tt>WhiteboardParticipantChangeEvent</tt> using this
     * whiteboard participant as source, setting it to be of type
     * <tt>eventType</tt> and the corresponding <tt>oldValue</tt> and
     * <tt>newValue</tt>,
     *
     * @param eventType the type of the event to create and dispatch.
     * @param oldValue the value of the source property before it changed.
     * @param newValue the current value of the source property.
     * @param reason a string that could be set to contain a human readable
     *            explanation for the transition (particularly handy when moving
     *            into a FAILED state).
     */
    protected void fireWhiteboardParticipantChangeEvent(String eventType,
        Object oldValue, Object newValue, String reason)
    {
        WhiteboardParticipantChangeEvent evt =
            new WhiteboardParticipantChangeEvent(this, eventType, oldValue,
                newValue, reason);

        if (logger.isDebugEnabled())
            logger.debug("Dispatching a WhiteboardParticipantChangeEvent event to "
            + whiteboardParticipantListeners.size() + " listeners. event is: "
            + evt.toString());

        Iterable<WhiteboardParticipantListener> listeners;
        synchronized (whiteboardParticipantListeners)
        {
            listeners
                = new ArrayList<WhiteboardParticipantListener>(
                        whiteboardParticipantListeners);
        }

        for (WhiteboardParticipantListener listener : listeners)
        {
            if (eventType
                .equals(WhiteboardParticipantChangeEvent.WHITEBOARD_PARTICIPANT_DISPLAY_NAME_CHANGE))
            {
                listener.participantDisplayNameChanged(evt);
            }
            else if (eventType
                .equals(WhiteboardParticipantChangeEvent.WHITEBOARD_PARTICIPANT_IMAGE_CHANGE))
            {
                listener.participantImageChanged(evt);
            }
            else if (eventType
                .equals(WhiteboardParticipantChangeEvent.WHITEBOARD_PARTICIPANT_STATE_CHANGE))
            {
                listener.participantStateChanged(evt);
            }
        }
    }

    /**
     * Returns a string representation of the participant in the form of <br>
     * Display Name <address>;status=WhiteboardParticipantStatus
     *
     * @return a string representation of the participant and its state.
     */
    @Override
    public String toString()
    {
        return getDisplayName();
    }

    /**
     * Returns the name of this member
     *
     * @return the name of this member in the room (nickname).
     */
    public String getName()
    {
        return this.participant.getDisplayName();
    }

    /**
     * Sets the chat room that this member is participating in.
     *
     * @param session
     */
    public void setWhiteboardSession(WhiteboardSessionJabberImpl session)
    {
        this.whiteboard = session;
    }

}
