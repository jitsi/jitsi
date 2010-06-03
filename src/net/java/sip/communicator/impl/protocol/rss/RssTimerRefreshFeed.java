/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.rss;

import java.util.TimerTask;
import net.java.sip.communicator.util.*;

/**
 * Instant messaging functionalites for the Rss protocol.
 *
 * @author Jean-Albert Vescovo
 */

public class RssTimerRefreshFeed
    extends TimerTask
{
    private static final Logger logger
        = Logger.getLogger(RssTimerRefreshFeed.class);

    private OperationSetBasicInstantMessagingRssImpl opSet;

    /**
     * Creates an instance of timer used to seeking periodically the rss feeds
     * registered as contacts.
     * @param opSet the OperationSetBasicInstantMessagingRssImpl instance that
     * is managing the rss protocol.
     */
    public RssTimerRefreshFeed(OperationSetBasicInstantMessagingRssImpl opSet)
    {
        this.opSet = opSet;
    }

    /**
     * What the timer is supposed to do each time the PERIOD_REFRESH_RSS expire.
     * In facts, it launch a new thread responsible for starting one or more
     * rss queries
     */
    public void run()
    {
        if (logger.isTraceEnabled())
            logger.trace("Starting a periodic rss check.");
        this.opSet.refreshAllRssFeeds();
    }
}
