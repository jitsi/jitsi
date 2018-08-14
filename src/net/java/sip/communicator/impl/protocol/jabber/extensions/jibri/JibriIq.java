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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jibri;

import org.jitsi.util.*;

import org.jivesoftware.smack.packet.*;
import org.jxmpp.jid.EntityBareJid;

import java.util.*;

/**
 * The IQ used to control conference recording with Jibri component.
 *
 * Start the recording:
 *
 * 1. Send Jibri IQ with {@link Action#START} to Jibri.
 * 2. Jibri replies with RESULT and status {@link Status#PENDING}.
 * 3. Jibri sends SET IQ with status {@link Status#ON} once recording actually
 *    starts.
 *
 * Stop the recording:
 *
 * 1. Send Jibri IQ with {@link Action#STOP} to Jibri.
 * 2. Jibri replies with {@link Status#OFF} immediately if the recording has
 *    been stopped already or sends separate Jibri SET IQ later on if it takes
 *    more time.
 *
 * @author lishunyang
 * @author Pawel Domas
 */
public class JibriIq
    extends IQ
{
    /**
     * Attribute name of "action".
     */
    public static final String ACTION_ATTR_NAME = "action";

    /**
     * The name of XML attribute name which holds the display name which will be
     * used by Jibri participant when it enters Jitsi Meet conference.
     * The value is "displayname".
     */
    static final String DISPLAY_NAME_ATTR_NAME = "displayname";

    /**
     * XML element name of the Jibri IQ.
     */
    public static final String ELEMENT_NAME = "jibri";

    /**
     * XML namespace of the Jibri IQ.
     */
    public static final String NAMESPACE = "http://jitsi.org/protocol/jibri";

    /**
     * The name of XML attribute which stores SIP address. The value is
     * "sipaddress".
     */
    static final String SIP_ADDRESS_ATTR_NAME = "sipaddress";

    /**
     * The name of XML attribute which stores the recording status.
     */
    static final String STATUS_ATTR_NAME = "status";

    /**
     * The name of XML attribute which stores the optional failure reason
     */
    static final String FAILURE_REASON_ATTR_NAME = "failure_reason";

    /**
     * The name of XML attribute which stores the stream id.
     */
    static final String STREAM_ID_ATTR_NAME = "streamid";

    /**
     * The name of the XML attribute which stores the YouTube
     * broadcast ID
     */
    static final String YOUTUBE_BROADCAST_ID_ATTR_NAME = "you_tube_broadcast_id";

    /**
     * The name of the XML attribute which stores the {@link #sessionId}
     */
    static final String SESSION_ID_ATTR_NAME = "session_id";

    /**
     * The name of the XML attribute which stores the {@link #appData}
     * field.
     */
    static final String APP_DATA_ATTR_NAME = "app_data";

    /**
     * The name of XML attribute which stores the recording mode which can be
     * either 'stream' or 'file'. If the attribute is not present, but
     * {@link #STREAM_ID_ATTR_NAME} is, then it defaults to 'stream'. But if
     * the {@link #STREAM_ID_ATTR_NAME} is not present then it defaults to
     * 'file'. Note that the defaults logic is handled on Jicofo level rather
     * than this packet's extension implementation.
     *
     * In the 'stream' mode Jibri live streams the conference recording.
     * The 'file' mode makes Jibri write the recording to a file.
     */
    static final String RECORDING_MODE_ATTR_NAME = "recording_mode";

    /**
     * The name of XML attribute which stores the name of the conference room to
     * be recorded.
     */
    static final String ROOM_ATTR_NAME = "room";

    /**
     * Holds the action.
     */
    private Action action = Action.UNDEFINED;

    /**
     * The display name which will be used by Jibri participant.
     */
    private String displayName;

    /**
     * The recording mode. See {@link #RECORDING_MODE_ATTR_NAME}.
     */
    private RecordingMode recordingMode = RecordingMode.UNDEFINED;

    /**
     * The SIP address of remote peer.
     */
    private String sipAddress;

    /**
     * Holds recording status.
     */
    private Status status = Status.UNDEFINED;

    /**
     * An optional description for the 'OFF' state which can be used
     * to describe an 'unclean' to transition to off (e.g. 'error')
     */
    private FailureReason failureReason = null;

    /**
     * The ID of the stream which will be used to record the conference. The
     * value depends on recording service provider.
     */
    private String streamId = null;

    /**
     * The YouTube broadcast ID for the currently active stream.  This is combined
     * with a known URL to generate the URL to view the stream.
     */
    private String youTubeBroadcastId = null;

    /**
     * The ID for this Jibri session.  This ID is used to uniquely identify
     * this session (i.e. this particular file recording, live stream or
     * SIP call).  It is returned in the ACK of the initial start request
     * and should be used in all subsequent IQ messages regarding this
     * session.  When Jibri joins the call, it will use this same
     * session ID in its presence so that an association can be made
     * between this signaling flow and the Jibri client.
     */
    private String sessionId = null;

    /**
     * A JSON-encoded string containing arbitrary Jibri application
     * data.  This allows new fields we want to pass to Jibri to
     * be added here so that we don't need to add new explicit
     * fields every time.
     */
    private String appData = null;

    /**
     * The name of the conference room to be recorded.
     */
    private EntityBareJid room = null;

    public JibriIq()
    {
        super(ELEMENT_NAME, NAMESPACE);
    }

    /**
     * @return the value for {@link #DISPLAY_NAME_ATTR_NAME}
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Sets new value for {@link #DISPLAY_NAME_ATTR_NAME}
     * @param displayName the new display name to be set
     */
    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    /**
     * @return the value for {@link #SIP_ADDRESS_ATTR_NAME}
     */
    public String getSipAddress()
    {
        return this.sipAddress;
    }

    /**
     * Sets new value for {@link #SIP_ADDRESS_ATTR_NAME}
     * @param sipAddress the new SIP address to be set
     */
    public void setSipAddress(String sipAddress)
    {
        this.sipAddress = sipAddress;
    }

    /**
     * Returns the value of {@link #STREAM_ID_ATTR_NAME} attribute.
     * @return a <tt>String</tt> which contains the value of "stream id"
     *         attribute or <tt>null</tt> if empty.
     */
    public String getStreamId()
    {
        return streamId;
    }

    /**
     * Returns the value of {@link #YOUTUBE_BROADCAST_ID_ATTR_NAME} attribute.
     * @return a <tt>String</tt> which contains the value of the
     * {@link #YOUTUBE_BROADCAST_ID_ATTR_NAME} attribute, or null if empty.
     */
    public String getYoutubeBroadcastId() { return youTubeBroadcastId; }

    /**
     * Sets the value for {@link #STREAM_ID_ATTR_NAME} attribute.
     * @param streamId a <tt>String</tt> for the stream id attribute or
     *        <tt>null</tt> to remove it from XML element.
     */
    public void setStreamId(String streamId)
    {
        this.streamId = streamId;
    }

    /**
     * Sets the value for {@link #YOUTUBE_BROADCAST_ID_ATTR_NAME} attribute.
     * @param youTubeBroadcastId a <tt>String</tt> for the stream id attribute or
     *        <tt>null</tt> to remove it from XML element.
     */
    public void setYouTubeBroadcastId(String youTubeBroadcastId)
    {
        this.youTubeBroadcastId = youTubeBroadcastId;
    }

    /**
     * Gets the value of the {@link #SESSION_ID_ATTR_NAME} attribute
     * @return the session ID
     */
    public String getSessionId() { return sessionId; }

    /**
     * Gets the value of the {@link #APP_DATA_ATTR_NAME} attribute
     *
     * @return the JSON-encoded application data
     */
    public String getAppData() { return appData; }

    /**
     * Sets the value of the {@link #SESSION_ID_ATTR_NAME} attribute
     * @param sessionId the session ID
     */
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    /**
     * Sets the value of the {@link #APP_DATA_ATTR_NAME} attribute
     *
     * @param appData a JSON-encoded string containing arbitrary application
     * data
     */
    public void setAppData(String appData) { this.appData = appData; }

    /**
     * Returns the value of {@link #ROOM_ATTR_NAME} attribute.
     * @return a <tt>String</tt> which contains the value of the room attribute
     *         or <tt>null</tt> if empty.
     * @see #room
     */
    public EntityBareJid getRoom()
    {
        return room;
    }

    /**
     * Sets the value for {@link #ROOM_ATTR_NAME} attribute.
     * @param room a <tt>String</tt> for the room attribute or <tt>null</tt> to
     *             remove it from XML element.
     * @see #room
     */
    public void setRoom(EntityBareJid room)
    {
        this.room = room;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IQ.IQChildElementXmlStringBuilder getIQChildElementBuilder(IQ.IQChildElementXmlStringBuilder xml)
    {
        if (action != Action.UNDEFINED)
        {
            xml.attribute(ACTION_ATTR_NAME, action.toString());
        }

        if (status != Status.UNDEFINED)
        {
            xml.attribute(STATUS_ATTR_NAME, status.toString());
        }

        if (recordingMode != RecordingMode.UNDEFINED)
        {
            xml.attribute(RECORDING_MODE_ATTR_NAME, recordingMode.toString());
        }

        xml.optAttribute(ROOM_ATTR_NAME, room);
        xml.optAttribute(STREAM_ID_ATTR_NAME, streamId);
        xml.optAttribute(YOUTUBE_BROADCAST_ID_ATTR_NAME, youTubeBroadcastId);
        xml.optAttribute(DISPLAY_NAME_ATTR_NAME, displayName);
        xml.optAttribute(SIP_ADDRESS_ATTR_NAME, sipAddress);
        xml.optAttribute(SESSION_ID_ATTR_NAME, sessionId);
        xml.optAttribute(FAILURE_REASON_ATTR_NAME, failureReason);
        xml.optAttribute(APP_DATA_ATTR_NAME, appData);

        xml.setEmptyElement();

        return xml;
    }

    /**
     * Sets the value of 'action' attribute.
     *
     * @param action the value to be set as 'action' attribute of this IQ.
     */
    public void setAction(Action action)
    {
        this.action = action;
    }

    /**
     * Returns the value of 'action' attribute.
     */
    public Action getAction()
    {
        return action;
    }

    /**
     * Returns the value of 'recording_mode' attribute.
     * @see JibriIq#RECORDING_MODE_ATTR_NAME
     */
    public RecordingMode getRecordingMode()
    {
        return recordingMode;
    }

    /**
     * Sets the value of 'recording_mode' attribute.
     * @param mode the new value to set as the recording mode attribute of this
     *             IQ instance.
     *
     * @see JibriIq#RECORDING_MODE_ATTR_NAME
     */
    public void setRecordingMode(RecordingMode mode)
    {
        this.recordingMode = mode;
    }

    /**
     * Sets the value of 'status' attribute.
     */
    public void setStatus(Status status)
    {
        this.status = status;
    }

    /**
     * Returns the value of 'status' attribute.
     */
    public Status getStatus()
    {
        return status;
    }

    public void setFailureReason(FailureReason failureReason)
    {
        this.failureReason = failureReason;
    }

    public FailureReason getFailureReason()
    {
        return this.failureReason;
    }

    public static JibriIq createResult(JibriIq request, String sessionId)
    {
        JibriIq result = new JibriIq();
        result.setType(IQ.Type.result);
        result.setStanzaId(request.getStanzaId());
        result.setTo(request.getFrom());
        result.setSessionId(sessionId);

        return result;
    }

    /**
     * Enumerative value of attribute "action" in recording extension.
     *
     * @author lishunyang
     * @author Pawel Domas
     *
     */
    public enum Action
    {
        /**
         * Start the recording.
         */
        START("start"),
        /**
         * Stop the recording.
         */
        STOP("stop"),
        /**
         * Unknown/uninitialized
         */
        UNDEFINED("undefined");

        private String name;

        Action(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }

        /**
         * Parses <tt>Action</tt> from given string.
         *
         * @param action the string representation of <tt>Action</tt>.
         *
         * @return <tt>Action</tt> value for given string or
         *         {@link #UNDEFINED} if given string does not
         *         reflect any of valid values.
         */
        public static Action parse(String action)
        {
            if (StringUtils.isNullOrEmpty(action))
                return UNDEFINED;

            try
            {
                return Action.valueOf(action.toUpperCase());
            }
            catch(IllegalArgumentException e)
            {
                return UNDEFINED;
            }
        }
    }

    /**
     * Enumerates available recording modes stored under
     * {@link #RECORDING_MODE_ATTR_NAME}.
     */
    public enum RecordingMode
    {
        /**
         * Jibri records to file.
         */
        FILE("file"),

        /**
         * Jibri live streaming mode.
         */
        STREAM("stream"),

        /**
         * No valid value specified.
         */
        UNDEFINED("undefined");

        /**
         * Recording mode name holder.
         */
        private String mode;

        /**
         * Creates new {@link RecordingMode} instance.
         * @param mode a string corresponding to one of {@link RecordingMode}
         *             values.
         */
        RecordingMode(String mode)
        {
            this.mode = mode;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return mode;
        }

        /**
         * Parses <tt>RecordingMode</tt> from given string.
         *
         * @param status the string representation of <tt>RecordingMode</tt>.
         *
         * @return <tt>RecordingMode</tt> value for given string or
         *         {@link #UNDEFINED} if given string does not
         *         reflect any of valid values.
         */
        public static RecordingMode parse(String status)
        {
            if (StringUtils.isNullOrEmpty(status))
                return UNDEFINED;

            try
            {
                return RecordingMode.valueOf(status.toUpperCase());
            }
            catch(IllegalArgumentException e)
            {
                return UNDEFINED;
            }
        }
    }

    public enum FailureReason
    {
        BUSY("busy"),
        ERROR("error"),
        UNDEFINED("undefined");

        private String name;

        FailureReason(String name) { this.name = name; }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return name;
        }

        /**
         * Parses <tt>Status</tt> from given string.
         *
         * @param status the string representation of <tt>Status</tt>.
         *
         * @return <tt>Status</tt> value for given string or
         *         {@link #UNDEFINED} if given string does not
         *         reflect any of valid values.
         */
        public static FailureReason parse(String status)
        {
            if (StringUtils.isNullOrEmpty(status))
                return UNDEFINED;

            try
            {
                return FailureReason.valueOf(status.toUpperCase());
            }
            catch(IllegalArgumentException e)
            {
                return UNDEFINED;
            }
        }
    }

    /**
     * The enumeration of recording status values.
     */
    public enum Status
    {
        /**
         * Recording is in progress.
         */
        ON("on"),

        /**
         * Recording stopped.
         */
        OFF("off"),

        /**
         * Starting the recording process.
         */
        PENDING("pending"),

        /**
         * Unknown/uninitialized.
         */
        UNDEFINED("undefined");

        /**
         * Status name holder.
         */
        private String name;

        /**
         * Creates new {@link Status} instance.
         * @param name a string corresponding to one of {@link Status} values.
         */
        Status(String name)
        {
            this.name = name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return name;
        }

        /**
         * Parses <tt>Status</tt> from given string.
         *
         * @param status the string representation of <tt>Status</tt>.
         *
         * @return <tt>Status</tt> value for given string or
         *         {@link #UNDEFINED} if given string does not
         *         reflect any of valid values.
         */
        public static Status parse(String status)
        {
            if (StringUtils.isNullOrEmpty(status))
                return UNDEFINED;

            try
            {
                return Status.valueOf(status.toUpperCase());
            }
            catch(IllegalArgumentException e)
            {
                return UNDEFINED;
            }
        }
    }
}
