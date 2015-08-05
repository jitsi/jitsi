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
package net.java.sip.communicator.impl.protocol.jabber.extensions.mailnotification;

import java.util.*;

import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.packet.*;

/**
 * A straightforward extension of the IQ. A Mailbox object is created via the
 * MailboxProvider. It contains all the information we need to notify the user
 * about a new E-Mail message.
 *
 * @author Matthieu Helleringer
 * @author Alain Knaebel
 * @author Emil Ivov
 */
public class MailboxIQ extends IQ
{

    /**
     * Logger for this class
     */
    private static final Logger logger =
        Logger.getLogger(MailboxIQ.class);

    /**
     * A list of threads that this mailbox IQ refers to.
     */
    private List<MailThreadInfo> threads = null;

    /**
     * The time these results were generated, in milliseconds since the
     * UNIX epoch. This value should be cached and sent as the newer-than-time
     * attribute in the next email query.
     * not used for the moment
     */
    private long resultTime;

    /**
     * Number of unread messages.
     */
    private int totalMatched;

    /**
     * A number indicating whether total-matched is just an estimate:
     * 1 indicates it is; 0 or omitted indicates that it is not.
     */
    private boolean totalEstimate;

    /**
     * Indicates the URL of the email server
     */
    private String url;

    /**
     * Indicates the date of the most recent email message.
     */
    private long date;

    /**
     * The name space for new mail notification packets.
     */
    public static final String NAMESPACE = "google:mail:notify";

    /**
     * The name of the element that Google use to transport new mail
     * notifications.
     */
    public static final String ELEMENT_NAME = "mailbox";

    /**
     * Sets the date of the most recent unread mail content on the mail server,
     * in milliseconds since the UNIX epoch.
     *
     * @param date the date long
     */
    public void setDate(long date)
    {
        this.date = date;
    }

    /**
     * Returns the date of the most recent unread mail content on the mail
     * server, in milliseconds since the UNIX epoch.
     *
     * @return the date of the most recent unread mail content on the mail
     * server, in milliseconds since the UNIX epoch.
     */
    public long getDate()
    {
        return this.date;
    }

    /**
     * Returns the time when these results were generated, in milliseconds
     * since the UNIX epoch.
     *
     * @return the time when these results were generated, in milliseconds
     * since the UNIX epoch.
     */
    public long getResultTime()
    {
        return resultTime;
    }

    /**
     * Sets the time these results were generated, in milliseconds since the
     * UNIX epoch.
     *
     * @param l the resultTime long
     */
    public void setResultTime(long l)
    {
        this.resultTime = l;
    }

    /**
     * Returns the number of unread mail messages
     *
     * @return the total number of matched (unread) mail messages
     */
    public int getTotalMatched()
    {
        return totalMatched;
    }

    /**
     * Specifies the number of unread mail messages.
     *
     * @param totalMatched the number of matched mail messages
     */
    public void setTotalMatched(int totalMatched)
    {
        this.totalMatched = totalMatched;
    }

    /**
     * Determines whether the total of unread mail messages is an estimate or
     * not.
     *
     * @return <tt>true</tt> if the total number of mail messages is an estimate
     * and <tt>false</tt> otherwise.
     */
    public boolean isTotalEstimate()
    {
        return totalEstimate;
    }

    /**
     * Specifies whether the total number of unread mail messages contained in
     * this object is an estimate or a precise count.
     *
     * @param totalEstimate <tt>true</tt> if the number of total messages here
     * is an estimate and <tt>false</tt> otherwise.
     */
    public void setTotalEstimate(boolean totalEstimate)
    {
        this.totalEstimate = totalEstimate;
    }

    /**
     * Returns the sub-element XML section of the IQ packet, or null if
     * there isn't one. Packet extensions must be included, if any are defined.
     *
     * @return the child element section of the IQ XML.
     */
    @Override
    public String getChildElementXML()
    {
        if (logger.isDebugEnabled())
            logger.debug("Mailbox.getChildElementXML usage");
        String totalString = totalEstimate ? " total-estimate='1' " : "";
        return "<mailbox result-time='" + resultTime + "' total-matched='"
                + totalMatched + "'" + totalString + "/>";
    }

    /**
     * Specifies an HTTP URL of the mail server that contains the messages
     * indicated in this Mailbox IQ.
     *
     * @param url the http URL where users could check the messages that this
     * IQ refers to.
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * Returns the http URL of the mail server containing the messages that
     * this IQ refers to.
     *
     * @return the http URL of the mail server containing the messages that
     * this IQ refers to.
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * Adds a thread info element to the list of threads that this
     * <tt>MailboxIQ</tt> is referring to.
     *
     * @param threadInfo the new thread info instance that we should add to the
     * thread list.
     */
    public void addThread(MailThreadInfo threadInfo)
    {
        if(threads == null)
            threads = new LinkedList<MailThreadInfo>();

        threads.add(threadInfo);
    }

    /**
     * Returns the number of threads that are currently in this
     * <tt>MailboxIQ</tt>.
     *
     * @return the number of threads currently in this <tt>MailboxIQ</tt>.
     */
    public int getThreadCount()
    {
        return threads.size();
    }

    /**
     * Returns the list of threads that this <tt>MailboxIQ</tt> refers to.
     *
     * @return the list of threads that this <tt>MailboxIQ</tt> refers to.
     */
    public Iterator<MailThreadInfo> threads()
    {
        return threads.iterator();
    }
}
