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
package net.java.sip.communicator.service.protocol;

import java.beans.*;

import org.jitsi.service.neomedia.*;

/**
 * Represents a member and its details in a telephony conference managed by a
 * <tt>CallPeer</tt> in its role as a conference focus.
 *
 * @author Lyubomir Marinov
 */
public interface ConferenceMember
{
    /**
     * The name of the property of <tt>ConferenceMember</tt> which specifies the
     * SSRC of the audio content/RTP stream sent by the respective
     * <tt>ConferenceMember</tt> in the conference.
     */
    public static final String AUDIO_SSRC_PROPERTY_NAME = "audioSsrc";

    /**
     * The name of the property of <tt>ConferenceMember</tt> which specifies the
     * status of the audio RTP stream from the point of view of the
     * <tt>ConferenceMember</tt>.
     */
    public static final String AUDIO_STATUS_PROPERTY_NAME = "audioStatus";

    /**
     * The name of the property of <tt>ConferenceMember</tt> which specifies the
     * user-friendly display name of the respective <tt>ConferenceMember</tt> in
     * the conference.
     */
    public static final String DISPLAY_NAME_PROPERTY_NAME = "displayName";

    /**
     * The name of the property of <tt>ConferenceMember</tt> which specifies the
     * state of the device and signaling session of the respective
     * <tt>ConferenceMember</tt> in the conference.
     */
    public static final String STATE_PROPERTY_NAME = "state";

    /**
     * The name of the property of <tt>ConferenceMember</tt> which specifies the
     * SSRC of the video content/RTP stream sent by the respective
     * <tt>ConferenceMember</tt> in the conference.
     */
    public static final String VIDEO_SSRC_PROPERTY_NAME = "videoSsrc";

    /**
     * The name of the property of <tt>ConferenceMember</tt> which specifies the
     * status of the video RTP stream from the point of view of the
     * <tt>ConferenceMember</tt>.
     */
    public static final String VIDEO_STATUS_PROPERTY_NAME = "videoStatus";

    /**
     * Adds a specific <tt>PropertyChangeListener</tt> to the list of
     * listeners interested in and notified about changes in the values of the
     * properties of this <tt>ConferenceMember</tt> such as
     * <tt>#DISPLAY_NAME_PROPERTY_NAME</tt> and
     * <tt>#STATE_PROPERTY_NAME</tt>.
     *
     * @param listener
     *            a <tt>PropertyChangeListener</tt> to be notified about
     *            changes in the values of the properties of this
     *            <tt>ConferenceMember</tt>. If the specified listener is
     *            already in the list of interested listeners (i.e. it has been
     *            previously added), it is not added again.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Gets the SIP address of this <tt>ConferenceMember</tt> as specified by
     * the conference-info XML received from its
     * <tt>conferenceFocusCallPeer</tt>.
     *
     * @return the SIP address of this <tt>ConferenceMember</tt> as specified by
     * the conference-info XML received from its
     * <tt>conferenceFocusCallPeer</tt>
     */
    public String getAddress();

    /**
     * Returns the SSRC of the audio content/RTP stream sent by this
     * <tt>ConferenceMember</tt> in the conference or <tt>-1</tt> if such
     * information is not currently available.
     *
     * @return the SSRC of the audio content/RTP stream sent by this
     * <tt>ConferenceMember</tt> in the conference or <tt>-1</tt> if such
     * information is not currently available
     */
    public long getAudioSsrc();

    /**
     * Gets the status in both directions of the audio RTP stream from the point
     * of view of this <tt>ConferenceMember</tt>.
     *
     * @return a <tt>MediaDIrection</tt> which represents the status in both
     * directions of the audio RTP stream from the point of view of this
     * <tt>ConferenceMember</tt>
     */
    public MediaDirection getAudioStatus();

    /**
     * Gets the <tt>CallPeer</tt> which is the conference focus of this
     * <tt>ConferenceMember</tt>.
     *
     * @return the <tt>CallPeer</tt> which is the conference focus of this
     * <tt>ConferenceMember</tt>
     */
    public CallPeer getConferenceFocusCallPeer();

    /**
     * Gets the user-friendly display name of this <tt>ConferenceMember</tt>
     * in the conference.
     *
     * @return the user-friendly display name of this
     *         <tt>ConferenceMember</tt> in the conference
     */
    public String getDisplayName();

    /**
     * Gets the state of the device and signaling session of this
     * <tt>ConferenceMember</tt> in the conference in the form of a
     * <tt>ConferenceMemberState</tt> value.
     *
     * @return a <tt>ConferenceMemberState</tt> value which represents the
     *         state of the device and signaling session of this
     *         <tt>ConferenceMember</tt> in the conference
     */
    public ConferenceMemberState getState();

    /**
     * Returns the SSRC of the video content/RTP stream sent by this
     * <tt>ConferenceMember</tt> in the conference or <tt>-1</tt> if such
     * information is not currently available.
     *
     * @return the SSRC of the video content/RTP stream sent by this
     * <tt>ConferenceMember</tt> in the conference or <tt>-1</tt> if such
     * information is not currently available
     */
    public long getVideoSsrc();

    /**
     * Gets the status in both directions of the video RTP stream from the point
     * of view of this <tt>ConferenceMember</tt>.
     *
     * @return a <tt>MediaDIrection</tt> which represents the status in both
     * directions of the video RTP stream from the point of view of this
     * <tt>ConferenceMember</tt>
     */
    public MediaDirection getVideoStatus();

    /**
     * Removes a specific <tt>PropertyChangeListener</tt> from the list of
     * listeners interested in and notified about changes in the values of the
     * properties of this <tt>ConferenceMember</tt> such as
     * <tt>#DISPLAY_NAME_PROPERTY_NAME</tt> and
     * <tt>#STATE_PROPERTY_NAME</tt>.
     *
     * @param listener
     *            a <tt>PropertyChangeListener</tt> to no longer be notified
     *            about changes in the values of the properties of this
     *            <tt>ConferenceMember</tt>
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);
}
