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
     * XML element name of the Jibri IQ.
     */
    public static final String ELEMENT_NAME = "jibri";

    /**
     * XML namespace of the Jibri IQ.
     */
    public static final String NAMESPACE = "http://jitsi.org/protocol/jibri";

    /**
     * The name of XML attribute which stores the recording status.
     */
    static final String STATUS_ATTR_NAME = "status";

    /**
     * The name of XML attribute which stores the stream id.
     */
    static final String STREAM_ID_ATTR_NAME = "streamid";

    /**
     * The name of XML attribute which stores the url.
     */
    static final String URL_ATTR_NAME = "url";

    /**
     * Holds the action.
     */
    private Action action = Action.UNDEFINED;

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
     * The conference URL which contains the full address like
     * "https://conference.com/room1".
     */
    private String url = null;

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
     * Returns the value of {@link #URL_ATTR_NAME} attribute.
     * @return a <tt>String</tt> which contains the value of the URL attribute
     *         or <tt>null</tt> if empty.
     * @see #url
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * Sets the value for {@link #URL_ATTR_NAME} attribute.
     * @param url a <tt>String</tt> for the URL attribute or
     *        <tt>null</tt> to remove it from XML element.
     * @see #url
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getChildElementXML()
    {
        StringBuilder xml = new StringBuilder();

        xml.append('<').append(ELEMENT_NAME);
        xml.append(" xmlns='").append(NAMESPACE).append("' ");

        if (action != Action.UNDEFINED)
        {
            printStringAttribute(xml, ACTION_ATTR_NAME, action.toString());
        }

        if (status != Status.UNDEFINED)
        {
            printStringAttribute(xml, STATUS_ATTR_NAME, status.toString());
        }

        if (url != null)
        {
            printStringAttribute(xml, URL_ATTR_NAME, url);
        }

        if (streamId != null)
        {
            printStringAttribute(xml, STREAM_ID_ATTR_NAME, streamId);
        }

        Collection<PacketExtension> extensions =  getExtensions();
        if (extensions.size() > 0)
        {
            xml.append(">");
            for (PacketExtension extension : extensions)
            {
                xml.append(extension.toXML());
            }
            xml.append("</").append(ELEMENT_NAME).append(">");
        }
        else
        {
            xml.append("/>");
        }

        return xml.toString();
    }

    private void printStringAttribute(
            StringBuilder xml, String attrName, String attr)
    {
        if (!StringUtils.isNullOrEmpty(attr))
            xml.append(attrName).append("='")
                .append(attr).append("' ");
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
