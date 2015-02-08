/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import net.java.sip.communicator.util.*;

import com.ircclouds.irc.api.*;
import com.ircclouds.irc.api.domain.messages.*;
import com.ircclouds.irc.api.state.*;

/**
 * MONITOR presence watcher.
 *
 * @author Danny van Heumen
 */
class MonitorPresenceWatcher
    implements PresenceWatcher
{
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger
        .getLogger(MonitorPresenceWatcher.class);

    /**
     * IRCApi instance.
     */
    private final IRCApi irc;

    /**
     * IRC connection state.
     */
    private final IIRCState connectionState;

    /**
     * Complete nick watch list.
     */
    private final SortedSet<String> nickWatchList;

    /**
     * Set of nicks that are confirmed to be monitored by the server.
     */
    private final SortedSet<String> monitoredNickList;

    /**
     * Constructor.
     *
     * @param irc the IRCApi instance
     * @param connectionState the connection state
     * @param nickWatchList the nick watch list
     * @param operationSet the persistent presence operation set
     */
    MonitorPresenceWatcher(final IRCApi irc, final IIRCState connectionState,
        final SortedSet<String> nickWatchList, final OperationSetPersistentPresenceIrcImpl operationSet)
    {
        if (irc == null)
        {
            throw new IllegalArgumentException("irc cannot be null");
        }
        this.irc = irc;
        if (connectionState == null)
        {
            throw new IllegalArgumentException("connectionState cannot be null");
        }
        this.connectionState = connectionState;
        if (nickWatchList == null)
        {
            throw new IllegalArgumentException("nickWatchList cannot be null");
        }
        this.nickWatchList = nickWatchList;
        this.monitoredNickList =
            Collections.synchronizedSortedSet(new TreeSet<String>());
        this.irc.addListener(new MonitorReplyListener(operationSet));
        setUpMonitor(this.irc, this.nickWatchList);
        // FIXME add basic poller watcher as a fallback method
        // FIXME adhere to limits according to ISUPPORT MONITOR=# entry
    }

    /**
     * Set up monitor based on the nick watch list in its current state.
     *
     * Created a static method as not to interfere too much with a state that is
     * still being initialized.
     */
    private static void setUpMonitor(final IRCApi irc, final SortedSet<String> nickWatchList)
    {
        final List<String> current;
        synchronized (nickWatchList)
        {
            current = new LinkedList<String>(nickWatchList);
        }
        // FIXME compute actual limit
        final int maxLength = 400;
        final StringBuilder query = new StringBuilder();
        for (String nick : current)
        {
            if (nick.length() + 1 > maxLength)
            {
                // payload is full, send monitor query now
                irc.rawMessage(createMonitorCmd(query));
                query.delete(0, query.length());
            }
            else if (query.length() > 0)
            {
                query.append(",");
            }
            query.append(nick);
        }
        if (query.length() > 0)
        {
            // send query for remaining nicks
            irc.rawMessage(createMonitorCmd(query));
        }
    }

    /**
     * Create a MONITOR add command with the provided nick list query.
     *
     * @param query the query
     * @return returns the full command
     */
    private static String createMonitorCmd(final StringBuilder query)
    {
        return "MONITOR + " + query.toString();
    }

    @Override
    public void add(final String nick)
    {
        LOGGER.trace("Adding nick '" + nick + "' to MONITOR watch list.");
        this.nickWatchList.add(nick);
        this.irc.rawMessage("MONITOR + " + nick);
    }

    @Override
    public void remove(final String nick)
    {
        LOGGER.trace("Removing nick '" + nick + "' from MONITOR watch list.");
        this.nickWatchList.remove(nick);
        this.irc.rawMessage("MONITOR - " + nick);
    }

    /**
     * Listener for MONITOR replies.
     *
     * FIXME upon QUIT/ERROR/CLIENTERROR updateAll monitored OFFLINE
     *
     * @author Danny van Heumen
     */
    private final class MonitorReplyListener
        extends AbstractIrcMessageListener
    {
        /**
         * Numeric message id for ONLINE nick response.
         */
        private static final int IRC_RPL_MONONLINE = 730;

        /**
         * Numeric message id for OFFLINE nick response.
         */
        private static final int IRC_RPL_MONOFFLINE = 731;

        // Unused constants. Listed for completeness.
        // /**
        // * Numeric message id for MONLIST entry.
        // */
        // private static final int IRC_RPL_MONLIST = 732;
        //
        // /**
        // * Numeric message id for ENDOFMONLIST.
        // */
        // private static final int IRC_RPL_ENDOFMONLIST = 733;

        /**
         * Operation set persistent presence instance.
         */
        private final OperationSetPersistentPresenceIrcImpl operationSet;

        // TODO Update to act on ClientError once available.

        /**
         * Constructor.
         *
         * @param operationSet the persistent presence opset used to update nick
         *            presence statuses.
         */
        public MonitorReplyListener(
            final OperationSetPersistentPresenceIrcImpl operationSet)
        {
            super(MonitorPresenceWatcher.this.irc,
                MonitorPresenceWatcher.this.connectionState);
            if (operationSet == null)
            {
                throw new IllegalArgumentException(
                    "operationSet cannot be null");
            }
            this.operationSet = operationSet;
        }

        /**
         * Numeric messages received in response to MONITOR commands or presence
         * updates.
         */
        @Override
        public void onServerNumericMessage(final ServerNumericMessage msg)
        {
            final List<String> confirmed;
            switch (msg.getNumericCode())
            {
            case IRC_RPL_MONONLINE:
                confirmed = parseMonitorResponse(msg.getText());
                for (String nick : confirmed)
                {
                    update(nick, IrcStatusEnum.ONLINE);
                }
                MonitorPresenceWatcher.this.monitoredNickList.addAll(confirmed);
                break;
            case IRC_RPL_MONOFFLINE:
                confirmed = parseMonitorResponse(msg.getText());
                for (String nick : confirmed)
                {
                    update(nick, IrcStatusEnum.OFFLINE);
                }
                MonitorPresenceWatcher.this.monitoredNickList.addAll(confirmed);
                break;
            }
        }

        /**
         * Parse response messages.
         *
         * @param message the message
         * @return Returns the list of targets extracted.
         */
        private List<String> parseMonitorResponse(final String message)
        {
            final LinkedList<String> confirmed = new LinkedList<String>();
            final String[] targets = message.substring(1).split(",");
            for (String target : targets)
            {
                String[] parts = target.split("!");
                confirmed.add(parts[0]);
            }
            return confirmed;
        }

        /**
         * Update specified nick to specified presence status.
         *
         * @param nick the target nick
         * @param status the current status
         */
        private void update(final String nick, final IrcStatusEnum status)
        {
            this.operationSet.updateNickContactPresence(nick, status);
        }
    }
}
