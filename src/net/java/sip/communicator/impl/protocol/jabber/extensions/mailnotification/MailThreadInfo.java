/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.mailnotification;

import java.util.*;

/**
 * This class represents the "mail-thread-info" element the Google use in their
 * mail notifications to deliver detailed thread information.
 *
 * @author Emil Ivov
 */
public class MailThreadInfo
{
    /**
     * The name of the "mail-thread-info" element.
     */
    public static final String ELEMENT_NAME = "mail-thread-info";

    /**
     * Contains the list of senders that have participated in this thread.
     */
    private List<String> senders = new LinkedList<String>();

    /**
     * The thread id of this thread.
     */
    private String tid = null;

    /**
     * Indicates that the local user has not participated in this thread.
     */
    public static final int PARTICIPATION_NONE = 0;

    /**
     * Indicates that the user is one of many recipients listed in the thread.
     */
    public static final int PARTICIPATION_ONE_OF_MANY = 1;

    /**
     * PARTICIPATION_SOLE_RECIPIENT indicates that the user is the sole
     * recipient for messages in this thread.
     */
    public static final int PARTICIPATION_SOLE_RECIPIENT = 2;

    /**
     * A number indicating the local user's participation level in this thread.
     * One of the PARTICIPATION_XXX constants defined above.
     */
    private int participation = -1;

    /**
     * The number of messages in the thread.
     */
    private int messages;

    /**
     * A timestamp of the most recent message, in milliseconds since the UNIX
     * epoch.
     */
    private long date = -1;

    /**
     * The URL linking to this thread (as opposed to the URL delivered in the
     * <tt>MailboxIQ</tt> that links to the mailbox itself).
     */
    private String url = null;

    /**
     * A tag that contains a pipe ('|') delimited list of labels applied to
     * this thread.
     */
    private String labels = null;

    /**
     * The subject of this e-mail thread.
     */
    private String subject = null;

    /**
     * An html-encoded snippet from the body of the email.
     */
    private String snippet = null;

    /**
     * The class describes a single participant in this email thread.
     */
    public class Sender
    {
        /**
         * The email address of the sender.
         */
        public String address = null;

        /**
         * The display name of the sender.
         */
        public String name = null;

        /**
         * Indicates whether this sender originated this thread.
         */
        public boolean originator = false;

        /**
         * Indicates whether or not the thread contains an unread message from
         * this sender.
         */
        public boolean unread = false;
    }

    /**
     * Returns the participation index for this thread. The participation index
     * is a number indicating the local user's participation level in this
     * thread: PARTICIPATION_NONE indicates that the user has not participated;
     * PARTICIPATION_ONE_OF_MANY indicates that the user is one of many
     * recipients listed in the thread; PARTICIPATION_SOLE_RECIPIENT indicates
     * that the user is the sole recipient for messages in this thread.
     *
     * @return one of the PARTICIPATION_XXX values defines in this class and
     * indicating whether the local is the sole, one of many or not a
     * participant of this thread.
     */
    public int getParticipation()
    {
        return participation;
    }
}
