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
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Represents an event fired by a <code>CallPeer</code> to notify
 * interested <code>CallPeerConferenceListener</code>s about changes in
 * its conference-related information such as it acting or not acting as a
 * conference focus and conference membership details.
 *
 * @author Lubomir Marinov
 */
public class CallPeerConferenceEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The ID of <code>CallPeerConferenceEvent</code> which notifies
     * about a change in the characteristic of a specific
     * <code>CallPeer</code> being a conference focus. The event does not
     * carry information about a specific <code>ConferenceMember</code> i.e. the
     * <code>conferenceMember</code> property is of value <tt>null</tt>.
     */
    public static final int CONFERENCE_FOCUS_CHANGED = 1;

    /**
     * The ID of <code>CallPeerConferenceEvent</code> which notifies
     * about an addition to the list of <code>ConferenceMember</code>s managed
     * by a specific <code>CallPeer</code>. The
     * <code>conferenceMember</code> property specifies the
     * <code>ConferenceMember</code> which was added and thus caused the event
     * to be fired.
     */
    public static final int CONFERENCE_MEMBER_ADDED = 2;

    /**
     * The ID of <code>CallPeerConferenceEvent</code> which notifies
     * about a removal from the list of <code>ConferenceMember</code>s managed
     * by a specific <code>CallPeer</code>. The
     * <code>conferenceMember</code> property specifies the
     * <code>ConferenceMember</code> which was removed and thus caused the event
     * to be fired.
     */
    public static final int CONFERENCE_MEMBER_REMOVED = 3;

    /**
     * The ID of <tt>CallPeerConferenceEvent</tt> which notifies
     * about an error packet received from a <tt>CallPeer</tt>. 
     */
    public static final int CONFERENCE_MEMBER_ERROR_RECEIVED = 4;

    /**
     * The <code>ConferenceMember</code> which has been changed (e.g. added to
     * or removed from the conference) if this event has been fired because of
     * such a change; otherwise, <tt>null</tt>.
     */
    private final ConferenceMember conferenceMember;

    /**
     * The ID of this event which may be one of
     * {@link #CONFERENCE_FOCUS_CHANGED}, {@link #CONFERENCE_MEMBER_ADDED}, 
     * {@link #CONFERENCE_MEMBER_ERROR_RECEIVED} and
     * {@link #CONFERENCE_MEMBER_REMOVED} and indicates the specifics of the
     * change in the conference-related information and the details this event
     * carries.
     */
    private final int eventID;
    
    
    /**
     * The error message associated with the error packet that was received. If 
     * the eventID is not {@link #CONFERENCE_MEMBER_ERROR_RECEIVED} the value
     * should should be <tt>null</tt>.
     */
    private final String errorString;

    /**
     * Initializes a new <code>CallPeerConferenceEvent</code> which is to
     * be fired by a specific <code>CallPeer</code> and which notifies
     * about a change in its conference-related information not including a
     * change pertaining to a specific <code>ConferenceMember</code>.
     *
     * @param sourceCallPeer the <code>CallPeer</code> which is to fire the new
     * event
     * @param eventID
     *            the ID of this event which may be
     *            {@link #CONFERENCE_FOCUS_CHANGED} and indicates the specifics
     *            of the change in the conference-related information and the
     *            details this event carries
     */
    public CallPeerConferenceEvent(CallPeer sourceCallPeer, int eventID)
    {
        this(sourceCallPeer, eventID, null);
    }

    /**
     * Initializes a new <code>CallPeerConferenceEvent</code> which is to
     * be fired by a specific <code>CallPeer</code> and which notifies
     * about a change in its conference-related information pertaining to a
     * specific <code>ConferenceMember</code>.
     *
     * @param sourceCallPeer the <code>CallPeer</code> which is to fire the new
     * event
     * @param eventID
     *            the ID of this event which may be
     *            {@link #CONFERENCE_MEMBER_ADDED} and
     *            {@link #CONFERENCE_MEMBER_REMOVED} and indicates the specifics
     *            of the change in the conference-related information and the
     *            details this event carries
     * @param conferenceMember
     *            the <code>ConferenceMember</code> which caused the new event
     *            to be fired
     */
    public CallPeerConferenceEvent(
        CallPeer sourceCallPeer,
        int eventID,
        ConferenceMember conferenceMember)
    {
        this(sourceCallPeer, eventID, conferenceMember, null);
    }
    
    /**
     * Initializes a new <tt>CallPeerConferenceEvent</tt> which is to
     * be fired by a specific <tt>CallPeer</tt> and which notifies
     * about a change in its conference-related information pertaining to a
     * specific <tt>ConferenceMember</tt>.
     *
     * @param sourceCallPeer the <tt>CallPeer</tt> which is to fire the new
     * event
     * @param eventID
     *            the ID of this event which may be
     *            {@link #CONFERENCE_MEMBER_ADDED} and
     *            {@link #CONFERENCE_MEMBER_REMOVED} and indicates the specifics
     *            of the change in the conference-related information and the
     *            details this event carries
     * @param conferenceMember
     *            the <tt>ConferenceMember</tt> which caused the new event
     *            to be fired
     * @param errorString the error string associated with the error packet that
     *            is received
     */
    public CallPeerConferenceEvent(
        CallPeer sourceCallPeer,
        int eventID,
        ConferenceMember conferenceMember,
        String errorString)
    {
        super(sourceCallPeer);

        this.eventID = eventID;
        this.conferenceMember = conferenceMember;
        this.errorString = errorString;
    }

    /**
     * Gets the <code>ConferenceMember</code> which has been changed (e.g. added
     * to or removed from the conference) if this event has been fired because
     * of such a change.
     *
     * @return the <code>ConferenceMember</code> which has been changed if this
     *         event has been fired because of such a change; otherwise,
     *         <tt>null</tt>
     */
    public ConferenceMember getConferenceMember()
    {
        return conferenceMember;
    }

    /**
     * Gets the ID of this event which may be one of
     * {@link #CONFERENCE_FOCUS_CHANGED}, {@link #CONFERENCE_MEMBER_ADDED} and
     * {@link #CONFERENCE_MEMBER_REMOVED} and indicates the specifics of the
     * change in the conference-related information and the details this event
     * carries.
     *
     * @return the ID of this event which may be one of
     *         {@link #CONFERENCE_FOCUS_CHANGED},
     *         {@link #CONFERENCE_MEMBER_ADDED} and
     *         {@link #CONFERENCE_MEMBER_REMOVED} and indicates the specifics of
     *         the change in the conference-related information and the details
     *         this event carries
     */
    public int getEventID()
    {
        return eventID;
    }

    /**
     * Gets the <code>CallPeer</code> which is the source of/fired the
     * event.
     *
     * @return the <code>CallPeer</code> which is the source of/fired the
     *         event
     */
    public CallPeer getSourceCallPeer()
    {
        return (CallPeer) getSource();
    }

    /**
     * Gets the value of {@link #errorString}.
     * @return the error string.
     */
    public String getErrorString()
    {
        return errorString;
    }
}
