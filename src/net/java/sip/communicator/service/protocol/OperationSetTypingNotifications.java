/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * The operation set allows user bundles (e.g. the user interface) to send
 * and receive typing notifications to and from other <tt>Contact</tt>s.
 * <p>
 * An important thing of typing notifications is that they do not have the
 * purpose of being relibable.
 * @author Emil Ivov
 */
public interface OperationSetTypingNotifications
    extends OperationSet
{
    /**
     * Indicates that the typing state of a source contact is not currently
     * known. You should not normally received events carrying such a state.
     */
    public static final int STATE_UNKNOWN = 0;

    /**
     * Indicates that a source contact is currently typing a message to us.
     */
    public static final int STATE_TYPING = 1;

    /**
     * Indicates that the typing state of a source contact has not been
     * updated for a while and is currently stale.
     */
    public static final int STATE_STALE = 2;

    /**
     * Indicates that a source contact had been typing a message to us but
     * has just paused.
     */
    public static final int STATE_PAUSED = 3;

    /**
     * Indicates that a source contact had been typing a message to us but
     * has stopped a while ago.
     */
    public static final int STATE_STOPPED = 4;

    /**
     * Adds <tt>l</tt> to the list of listeners registered for receiving
     * <tt>TypingNotificationEvent</tt>s
     * @param l the <tt>TypingNotificationsListener</tt> listener that we'd like
     * to add
     */
    public void addTypingNotificationsListener(TypingNotificationsListener l);

    /**
     * Removes <tt>l</tt> from the list of listeners registered for receiving
     * <tt>TypingNotificationEvent</tt>s
     * @param l the <tt>TypingNotificationsListener</tt> listener that we'd like
     * to remove
     */
    public void removeTypingNotificationsListener(TypingNotificationsListener l);


    /**
     * Sends a notification to <tt>notifiedContatct</tt> that we have entered
     * <tt>typingState</tt>.
     * @param notifiedContact the <tt>Contact</tt> to notify
     * @param typingState the typing state that we have entered.
     *
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>notifiedContact</tt> is
     * not an instance belonging to the underlying implementation.
     */
    public void sendTypingNotification(Contact notifiedContact, int typingState)
        throws IllegalStateException, IllegalArgumentException;
}
