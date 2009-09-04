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
    private List<Sender> senders = new LinkedList<Sender>();

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

    /**
     * Specifies the participation index for this thread. The participation
     * index  is a number indicating the local user's participation level in
     * this thread: PARTICIPATION_NONE indicates that the user has not
     * participated; PARTICIPATION_ONE_OF_MANY indicates that the user is one of
     * many recipients listed in the thread; PARTICIPATION_SOLE_RECIPIENT
     * indicates that the user is the sole recipient for messages in this
     * thread.
     *
     * @param participation one of the PARTICIPATION_XXX values defines in this
     * class and indicating whether the local is the sole, one of many or not a
     * participant of this thread.
     */
    protected void setParticipation(int participation)
    {
        this.participation = participation;
    }

    /**
     * Returns an iterator over a list of one or more sender instances, each of
     * which describes a participant in this thread.
     *
     * @return an iterator over a list of one or more sender instances, each of
     * which describes a participant in this thread.
     */
    public Iterator<Sender> senders()
    {
        return senders.iterator();
    }

    /**
     * Adds <tt>sender</tt> to the list of senders in this thread.
     *
     * @param sender the sender that we are adding.
     */
    protected void addSender(Sender sender)
    {
        senders.add(sender);
    }

    /**
     * Returns the number of messages in this thread.
     *
     * @return the number of messages in this thread.
     */
    public int getMessageCount()
    {
        return messages;
    }

    /**
     * Sets the number of messages in this thread.
     *
     * @param messageCount the number of messages in this thread.
     */
    protected void setMessageCount(int messageCount)
    {
        this.messages = messageCount;
    }

    /**
     * Returns the date of the most recent message in this thread.
     *
     * @return a timestamp of the most recent message, in milliseconds since
     * the UNIX epoch.
     */
    public long getDate()
    {
        return date;
    }

    /**
     * Sets the date of the most recent message in this thread.
     *
     * @param date a timestamp of the most recent message in this thread.
     */
    protected void setDate(long date)
    {
        this.date = date;
    }

    /**
     * Returns an URL linking to this thread. It is important to distinguish
     * between this URL and the one returned by the <tt>MailboxIQ</tt> which
     * points to the whole mailbox.
     *
     * @return the URL linking to this particular thread.
     */
    public String getURL()
    {
        return url;
    }

    /**
     * Sets an URL linking to this thread. It is important to distinguish
     * between this URL and the one returned by the <tt>MailboxIQ</tt> which
     * points to the whole mailbox.
     *
     * @param url the URL linking to this particular thread.
     */
    protected void setURL(String url)
    {
        this.url = url;
    }

    /**
     * Returns a pipe ('|') delimited list of labels applied to this thread.
     *
     * @return a pipe ('|') delimited list of labels applied to this thread.
     */
    public String getLabels()
    {
        return labels;
    }

    /**
     * Sets a pipe ('|') delimited list of labels that apply to this thread.
     *
     * @param labels a pipe ('|') delimited list of labels that apply to this
     * thread.
     */
    protected void setLabels(String labels)
    {
        this.labels = labels;
    }

    /**
     * Returns the ID of this thread.
     *
     * @return the ID of this thread.
     */
    public String getTid()
    {
        return tid;
    }

    /**
     * Specifies the ID of this thread.
     *
     * @param tid the ID of this thread.
     */
    protected void setTid(String tid)
    {
        this.tid = tid;
    }

    /**
     * Returns the subject of this e-mail thread.
     *
     * @return the subject of this e-mail thread.
     */
    public String getSubject()
    {
        return subject;
    }

    /**
     * Sets the subject of this e-mail thread.
     *
     * @param subject the subject of this e-mail thread.
     */
    protected void setSubject(String subject)
    {
        this.subject = subject;
    }

    /**
     * Returns an html-encoded snippet from the body of the email.
     *
     * @return an html-encoded snippet from the body of the email.
     */
    public String getSnippet()
    {
        return snippet;
    }

    /**
     *Sets an html-encoded snippet from the body of the email.
     *
     * @param snippet an html-encoded snippet from the body of the email.
     */
    protected void setSnippet(String snippet)
    {
        this.snippet = snippet;
    }
}
