/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

/**
 * The listener which will gather notifications for message waiting.
 *
 * @author Damian Minkov
 */
public interface MessageWaitingListener
{
    /**
     * Notifies for new messages that are waiting.
     *
     * @param evt the notification event.
     */
    public void messageWaitingNotify(MessageWaitingEvent evt);
}
