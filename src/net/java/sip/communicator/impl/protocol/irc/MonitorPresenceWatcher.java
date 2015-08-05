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
     * Static overhead in message payload required for 'MONITOR +' command.
     */
    private static final int MONITOR_ADD_CMD_STATIC_OVERHEAD = 10;

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
    private final Set<String> nickWatchList;

    /**
     * List of monitored nicks.
     *
     * The instance is stored here only for access in order to remove a nick
     * from the list, since 'MONITOR - ' command does not reply so we cannot
     * manage list removals from the reply listener.
     */
    private final Set<String> monitoredList;

    /**
     * Constructor.
     *
     * @param irc the IRCApi instance
     * @param connectionState the connection state
     * @param nickWatchList SYNCHRONIZED the nick watch list
     * @param monitored SYNCHRONIZED The shared collection which contains all
     *            the nicks that are confirmed to be subscribed to the MONITOR
     *            command.
     * @param operationSet the persistent presence operation set
     */
    MonitorPresenceWatcher(final IRCApi irc, final IIRCState connectionState,
        final Set<String> nickWatchList, final Set<String> monitored,
        final OperationSetPersistentPresenceIrcImpl operationSet,
        final int maxListSize)
    {
        if (irc == null)
        {
            throw new IllegalArgumentException("irc cannot be null");
        }
        this.irc = irc;
        if (connectionState == null)
        {
            throw new IllegalArgumentException(
                "connectionState cannot be null");
        }
        this.connectionState = connectionState;
        if (nickWatchList == null)
        {
            throw new IllegalArgumentException("nickWatchList cannot be null");
        }
        this.nickWatchList = nickWatchList;
        if (monitored == null)
        {
            throw new IllegalArgumentException("monitored cannot be null");
        }
        this.monitoredList = monitored;
        this.irc.addListener(new MonitorReplyListener(this.monitoredList,
            operationSet));
        setUpMonitor(this.irc, this.nickWatchList, maxListSize);
        LOGGER.debug("MONITOR presence watcher initialized.");
    }

    /**
     * Set up monitor based on the nick watch list in its current state.
     *
     * Created a static method as not to interfere too much with a state that is
     * still being initialized.
     */
    private static void setUpMonitor(final IRCApi irc,
        final Collection<String> nickWatchList, final int maxListSize)
    {
        List<String> current;
        synchronized (nickWatchList)
        {
            current = new LinkedList<String>(nickWatchList);
        }
        if (current.size() > maxListSize)
        {
            // cut off list to maximum number of entries allowed by server
            current = current.subList(0, maxListSize);
        }
        final int maxLength = 510 - MONITOR_ADD_CMD_STATIC_OVERHEAD;
        final StringBuilder query = new StringBuilder();
        for (String nick : current)
        {
            if (query.length() + nick.length() + 1 > maxLength)
            {
                // full payload, send monitor query now
                irc.rawMessage("MONITOR + " + query);
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
            irc.rawMessage("MONITOR + " + query);
        }
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
        // 'MONITOR - nick' command does not send confirmation, so immediately
        // remove nick from monitored list.
        this.monitoredList.remove(nick);
    }

    /**
     * Listener for MONITOR replies.
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
         * Error message signaling full list. Nick list provided are all nicks
         * that failed to be added to the monitor list.
         */
        private static final int IRC_ERR_MONLISTFULL = 734;

        /**
         * Operation set persistent presence instance.
         */
        private final OperationSetPersistentPresenceIrcImpl operationSet;

        /**
         * Set of nicks that are confirmed to be monitored by the server.
         */
        private final Set<String> monitoredNickList;

        /**
         * Constructor.
         *
         * @param monitored SYNCHRONIZED Collection of monitored nicks. This
         *            collection will be updated with all nicks that are
         *            confirmed to be subscribed by the MONITOR command.
         * @param operationSet the persistent presence opset used to update nick
         *            presence statuses.
         */
        public MonitorReplyListener(final Set<String> monitored,
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
            if (monitored == null)
            {
                throw new IllegalArgumentException("monitored cannot be null");
            }
            this.monitoredNickList = monitored;
        }

        /**
         * Numeric messages received in response to MONITOR commands or presence
         * updates.
         */
        @Override
        public void onServerNumericMessage(final ServerNumericMessage msg)
        {
            final List<String> acknowledged;
            switch (msg.getNumericCode())
            {
            case IRC_RPL_MONONLINE:
                acknowledged = parseMonitorResponse(msg.getText());
                for (String nick : acknowledged)
                {
                    update(nick, IrcStatusEnum.ONLINE);
                }
                monitoredNickList.addAll(acknowledged);
                break;
            case IRC_RPL_MONOFFLINE:
                acknowledged = parseMonitorResponse(msg.getText());
                for (String nick : acknowledged)
                {
                    update(nick, IrcStatusEnum.OFFLINE);
                }
                monitoredNickList.addAll(acknowledged);
                break;
            case IRC_ERR_MONLISTFULL:
                LOGGER.debug("MONITOR list full. Nick was not added. "
                    + "Fall back Basic Poller will be used if it is enabled. ("
                    + msg.getText() + ")");
                break;
            }
        }

        /**
         * Update all monitored nicks upon receiving a server-side QUIT message
         * for local user.
         */
        @Override
        public void onUserQuit(QuitMessage msg)
        {
            super.onUserQuit(msg);
            if (localUser(msg.getSource().getNick())) {
                updateAll(IrcStatusEnum.OFFLINE);
            }
        }

        /**
         * Update all monitored nicks upon receiving a server-side ERROR
         * response.
         */
        @Override
        public void onError(ErrorMessage msg)
        {
            super.onError(msg);
            updateAll(IrcStatusEnum.OFFLINE);
        }

        /**
         * Update all monitored nicks upon receiving a client-side ERROR
         * response.
         */
        @Override
        public void onClientError(ClientErrorMessage msg)
        {
            super.onClientError(msg);
            updateAll(IrcStatusEnum.OFFLINE);
        }

        /**
         * Parse response messages.
         *
         * @param message the message
         * @return Returns the list of targets extracted.
         */
        private List<String> parseMonitorResponse(final String message)
        {
            // Note: this should support both targets consisting of only a nick,
            // and targets consisting of nick!ident@host formats. (And probably
            // any variation on this that is typically allowed in IRC.)
            final LinkedList<String> acknowledged = new LinkedList<String>();
            final String[] targets = message.substring(1).split(",");
            for (String target : targets)
            {
                String[] parts = target.trim().split("!");
                acknowledged.add(parts[0]);
            }
            return acknowledged;
        }

        /**
         * Update all monitored nicks to specified status.
         *
         * @param status the desired status
         */
        private void updateAll(final IrcStatusEnum status)
        {
            final LinkedList<String> nicks;
            synchronized (monitoredNickList)
            {
                nicks = new LinkedList<String>(monitoredNickList);
            }
            for (String nick : nicks)
            {
                update(nick, status);
            }
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
