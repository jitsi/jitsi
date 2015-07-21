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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jirecon;

import org.jitsi.util.*;

import org.jivesoftware.smack.packet.*;

import java.util.*;

/**
 * The IQ used to communicate with Jirecon recording container.
 *
 * @author lishunyang
 * @author Pawel Domas
 */
public class JireconIq
    extends IQ
{
    /**
     * Name space of recording packet extension.
     */
    public static final String NAMESPACE = JireconIqProvider.NAMESPACE;

    /**
     * XML element name of recording packet extension.
     */
    public static final String ELEMENT_NAME = "recording";

    /**
     * Attribute name of "action".
     */
    public static final String ACTION_ATTR_NAME = "action";

    /**
     * Attribute name of "status".
     */
    public static final String STATUS_ATTR_NAME = "status";

    /**
     * Attribute name of "mucjid".
     */
    public static final String MUCJID_ATTR_NAME = "mucjid";

    /**
     * Attribute name of "dst".
     */
    public static final String OUTPUT_ATTR_NAME = "dst";

    /**
     * Attribute name of "rid".
     */
    public static final String RID_ATTR_NAME = "rid";

    /**
     * Jirecon container action.
     */
    private Action action = Action.UNDEFINED;

    /**
     * Muc JID of the participant that communicates with Jirecon.
     */
    private String mucJid;

    /**
     * Recording output path.
     */
    private String output;

    /**
     * Recording identifier.
     */
    private String rid;

    /**
     * Recording status returned by Jirecon.
     */
    private Status status = Status.UNDEFINED;

    @Override
    public String getChildElementXML()
    {
        StringBuilder xml = new StringBuilder();

        xml.append('<').append(ELEMENT_NAME);
        xml.append(" xmlns='").append(NAMESPACE).append("' ");

        printStringAttribute(xml, RID_ATTR_NAME, rid);

        if (action != Action.UNDEFINED)
        {
            printStringAttribute(xml, ACTION_ATTR_NAME, action.toString());
        }

        if (status != Status.UNDEFINED)
        {
            printStringAttribute(xml, STATUS_ATTR_NAME, status.toString());
        }

        printStringAttribute(xml, MUCJID_ATTR_NAME, mucJid);
        printStringAttribute(xml, OUTPUT_ATTR_NAME, output);

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
     * Returns the value of 'action' attribute. One of {@link JireconIq.Action}.
     */
    public Action getAction()
    {
        return action;
    }

    /**
     * Sets the value of 'status' attribute. One of {@link JireconIq.Status}.
     *
     * @param status one of {@link JireconIq.Status} to be set as 'status'
     *               attribute value of this IQ.
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
     * Sets the value of 'mucjid' attribute of this IQ.
     * @param mucJid the value to be set as 'mucjid' attribute in form of:
     *               roomname@muc.server.net/nickname
     */
    public void setMucJid(String mucJid)
    {
        this.mucJid = mucJid;
    }

    /**
     * Returns the value of 'mucjid' attribute of this IQ.
     */
    public String getMucJid()
    {
        return mucJid;
    }

    /**
     * Sets the value of 'output' attribute of this IQ.
     *
     * @param output the output path to be set in 'output' attribute.
     */
    public void setOutput(String output)
    {
        this.output = output;
    }

    /**
     * Returns the output path value contained in 'output' attribute.
     */
    public String getOutput()
    {
        return output;
    }

    /**
     * Sets the recording id value of this IQ.
     *
     * @param rid the recording identifier that will be stored in 'rid'
     *            attribute of this IQ.
     */
    public void setRid(String rid)
    {
        this.rid = rid;
    }

    /**
     * Returns the value of 'rid' attribute that is the recording id.
     */
    public String getRid()
    {
        return rid;
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
         * It can only be set in packet sent from client to component, in order
         * to let Jirecon component start a new recording session.
         */
        START("start"),

        /**
         * It can only be set in packet sent from client to component, in order
         * to let Jirecon component stop an specified recording session.
         */
        STOP("stop"),

        /**
         * It can be set both in packet sent from client to component or packet
         * sent from component to client, in order to notify the opposite with
         * some information, such as recording session status.
         */
        INFO("info"),

        /**
         * Means that the action has not been specified.
         */
        UNDEFINED("undefined");

        private String name;

        private Action(String name)
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
         *         {@link JireconIq.Action#UNDEFINED} if given string does not
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
     * Enumerative value of attribute "status" in recording extension.
     *
     * @author lishunyang
     * @author Pawel Domas
     *
     */
    public enum Status
    {
        /**
         * It can only be set in packet sent from component to client, notify
         * the opposite that "start" command has been received and recording
         * session is starting.
         */
        INITIATING("initiating"),

        /**
         * It can only be set in packet sent from component to client, notify
         * the opposite that recording session has been started successfully.
         */
        STARTED("started"),

        /**
         * It can only be set in packet sent from component to client, notify
         * the opposite that "stop" command has been received and recording
         * session is stopping.
         */
        STOPPING("stopping"),

        /**
         * It can only be set in packet sent from component to client, notify
         * the opposite that recording session has been stopped successfully.
         */
        STOPPED("stopped"),

        /**
         * It can only be set in packet sent from component to client, notify
         * the opposite that recording session has been aborted.
         */
        ABORTED("aborted"),

        /**
         * Means that the status has not been specified.
         */
        UNDEFINED("undefined");

        private String name;

        private Status(String name)
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
         *         {@link JireconIq.Status#UNDEFINED} if given string does not
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
