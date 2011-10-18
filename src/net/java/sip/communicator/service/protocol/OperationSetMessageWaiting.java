/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
