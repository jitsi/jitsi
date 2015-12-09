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
 * The IQ used to communicate with Jirecon recording container.
 *
 * @author lishunyang
 * @author Pawel Domas
 */
public class JibriIq
    extends IQ
{
    public static final String NAMESPACE = "http://jitsi.org/protocol/jibri";

    public static final String ELEMENT_NAME = "jibri";

    /**
     * Attribute name of "action".
     */
    public static final String ACTION_ATTR_NAME = "action";

    /**
     * Attribute name of "status".
     */
    public static final String STATUS_ATTR_NAME = "status";

    private Action action = Action.UNDEFINED;
    private Status status = Status.UNDEFINED;
    private String url = null;

    public String getStreamId()
    {
        return streamId;
    }

    public void setStreamId(String streamId)
    {
        this.streamId = streamId;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    private String streamId = null;

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
            printStringAttribute(xml, "url", url);
        }

        if (streamId != null)
        {
            printStringAttribute(xml, "streamid", streamId);
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
        START("start"),
        STOP("stop"),
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

    public enum Status
    {
        ON("on"),
        OFF("off"),
        PENDING("pending"),
        UNDEFINED("undefined");

        private String name;

        Status(String name)
        {
            this.name = name;
        }

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
