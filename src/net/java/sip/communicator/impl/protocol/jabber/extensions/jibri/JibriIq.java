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
     * The name of XML attribute which stores the stream id.
     */
    static final String STREAM_ID_ATTR_NAME = "streamid";

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
     * XMPPError stores error details for {@link Status#FAILED}.
     */
    private XMPPError error;

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
     * The ID of the stream which will be used to record the conference. The
     * value depends on recording service provider.
     */
    private String streamId = null;

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
     * Sets the value for {@link #STREAM_ID_ATTR_NAME} attribute.
     * @param streamId a <tt>String</tt> for the stream id attribute or
     *        <tt>null</tt> to remove it from XML element.
     */
    public void setStreamId(String streamId)
    {
        this.streamId = streamId;
    }

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
        xml.optAttribute(DISPLAY_NAME_ATTR_NAME, displayName);
        xml.optAttribute(SIP_ADDRESS_ATTR_NAME, sipAddress);

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

    /**
     * Sets the <tt>XMPPError</tt> which will provide details about Jibri
     * failure. It is expected to be set when this IQ's status value is
     * {@link Status#FAILED}.
     *
     * @param error <tt>XMPPError</tt> to be set on this <tt>JibriIq</tt>
     * instance.
     */
    public void setXMPPError(XMPPError error)
    {
        this.error = error;
    }

    /**
     * Returns {@link XMPPError} with Jibri error details when the status is
     * {@link Status#FAILED}.
     */
    public XMPPError getError()
    {
        return error;
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
         * The recorder has failed and the service is retrying on another
         * instance.
         */
        RETRYING("retrying"),

        /**
         * An error occurred any point during startup, recording or shutdown.
         */
        FAILED("failed"),

        /**
         * There are Jibri instances connected to the system, but all of them
         * are currently busy.
         */
        BUSY("busy"),

        /**
         * Unknown/uninitialized.
         */
        UNDEFINED("undefined"),

        /**
         * Used by {@link SipGatewayStatus} to signal that there are Jibris
         * available. SIP gateway does not use ON, OFF, PENDING nor RETRYING
         * states, because gateway availability and each SIP call's states are
         * signalled separately.
         * Check {@link SipGatewayStatus} and {@link SipCallState} for more
         * info.
         */
        AVAILABLE("available");

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
