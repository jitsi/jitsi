/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.rss;

/**
 * Instant messaging functionalites for the Rss protocol.
 *
 * @author Jean-Albert Vescovo
 */
public class RssThread
    extends Thread
{
     private OperationSetBasicInstantMessagingRssImpl opSet;
     private ContactRssImpl rssContact = null;

    /**
     * Creates a new instance of RssThread
     *
     * @param opSet the OperationSetBasicInstantMessagingRssImpl instance that
     * is managing the rss protocol.
     * @param rssContact the contact that the thread is going to do a query
     */
    public RssThread(OperationSetBasicInstantMessagingRssImpl opSet,
                     ContactRssImpl rssContact)
    {
        this.opSet = opSet;
        this.rssContact = rssContact;
        
        this.setDaemon(true);
    }

    /**
     * The task executed by the thread.
     * If no rss contact given as parameter, the query is launched for all
     * contacts
     */
    public void run()
    {
        this.opSet.refreshRssFeed(this.rssContact);
    }
}
