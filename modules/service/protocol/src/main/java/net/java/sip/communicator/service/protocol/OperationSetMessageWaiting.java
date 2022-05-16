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

import net.java.sip.communicator.service.protocol.event.*;

/**
 * Provides notifications for message waiting notifications.
 *
 * @author Damian Minkov
 */
public interface OperationSetMessageWaiting
    extends OperationSet
{
    /**
     * Message waiting types.
     */
    public enum MessageType
    {
        VOICE("voice-message"),
        FAX("fax-message"),
        PAGER("pager-message"),
        MULTIMEDIA("multimedia-message"),
        TEXT("text-message"),
        NONE("none");

        /**
         * Message type String.
         */
        private String type;

        /**
         * Creates new message type.
         * @param type the type.
         */
        private MessageType(String type)
        {
            this.type = type;
        }

        /**
         * Returns the type of the message type enum element.
         *
         * @return the message type.
         */
        @Override
        public String toString()
        {
            return type;
        }

        /**
         * Returns MessageType by its type name.
         *
         * @param type the type.
         * @return the corresponding MessageType.
         */
        public static MessageType valueOfByType(String type)
        {
            for(MessageType mt : values())
            {
                if(mt.toString().equals(type))
                    return mt;
            }

            return valueOf(type);
        }
    }

    /**
     * Registers a <tt>MessageWaitingListener</tt> with this
     * operation set so that it gets notifications of new and old
     * messages waiting.
     *
     * @param type register the listener for certain type of messages.
     * @param listener the <tt>MessageWaitingListener</tt>
     * to register.
     */
    public void addMessageWaitingNotificationListener(
            MessageType type,
            MessageWaitingListener listener);

    /**
     * Unregisters <tt>listener</tt> so that it won't receive any further
     * notifications upon new messages waiting notifications delivery.
     *
     * @param type register the listener for certain type of messages.
     * @param listener the <tt>MessageWaitingListener</tt>
     * to unregister.
     */
    public void removeMessageWaitingNotificationListener(
            MessageType type,
            MessageWaitingListener listener);
}
