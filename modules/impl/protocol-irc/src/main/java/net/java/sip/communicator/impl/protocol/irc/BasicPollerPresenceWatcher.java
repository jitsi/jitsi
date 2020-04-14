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
import java.util.concurrent.atomic.*;

import net.java.sip.communicator.util.*;

import com.ircclouds.irc.api.*;
import com.ircclouds.irc.api.domain.*;
import com.ircclouds.irc.api.domain.messages.*;
import com.ircclouds.irc.api.state.*;

class BasicPollerPresenceWatcher
    implements PresenceWatcher
{
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger
        .getLogger(BasicPollerPresenceWatcher.class);

    /**
     * Delay before starting the presence watcher task for the first time.
     */
    private static final long INITIAL_PRESENCE_WATCHER_DELAY = 10000L;

    /**
     * Period for the presence watcher timer.
     */
    private static final long PRESENCE_WATCHER_PERIOD = 60000L;

    /**
     * Instance of IRCAPi.
     */
    private final IRCApi irc;

    /**
     * Connection state instance.
     */
    private final IIRCState connectionState;

    /**
     * The persistent presence operation set used to issue presence updates.
     */
    private final OperationSetPersistentPresenceIrcImpl operationSet;

    /**
     * Synchronized set of nicks to watch for presence changes.
     */
    private final Set<String> nickWatchList;

    /**
     * Constructor.
     *
     * @param irc the IRCApi instance
     * @param connectionState the connection state
     * @param operationSet the persistent presence operation set
     * @param nickWatchList SYNCHRONIZED the nick watch list
     * @param serverIdentity the server identity
     */
    BasicPollerPresenceWatcher(final IRCApi irc,
        final IIRCState connectionState,
        final OperationSetPersistentPresenceIrcImpl operationSet,
        final Set<String> nickWatchList,
        final AtomicReference<String> serverIdentity)
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
        if (operationSet == null)
        {
            throw new IllegalArgumentException("operationSet cannot be null");
        }
        this.operationSet = operationSet;
        if (nickWatchList == null)
        {
            throw new IllegalArgumentException("nickWatchList cannot be null");
        }
        this.nickWatchList = nickWatchList;
        setUpPresenceWatcher(serverIdentity);
        LOGGER.debug("Basic Poller presence watcher initialized.");
    }

    /**
     * Set up a timer for watching the presence of nicks in the watch list.
     */
    private void setUpPresenceWatcher(
        final AtomicReference<String> serverIdentity)
    {
        // FIFO query list to be shared between presence watcher task and
        // presence reply listener.
        final List<List<String>> queryList =
            Collections.synchronizedList(new LinkedList<List<String>>());
        final Timer presenceWatcher = new Timer();
        irc.addListener(new PresenceReplyListener(presenceWatcher, queryList));
        final PresenceWatcherTask task =
            new PresenceWatcherTask(this.nickWatchList, queryList,
                serverIdentity);
        presenceWatcher.schedule(task, INITIAL_PRESENCE_WATCHER_DELAY,
            PRESENCE_WATCHER_PERIOD);
        LOGGER.trace("Basic Poller presence watcher set up.");
    }

    @Override
    public void add(String nick)
    {
        this.nickWatchList.add(nick);
    }

    @Override
    public void remove(String nick)
    {
        this.nickWatchList.remove(nick);
    }

    /**
     * Task for watching nick presence.
     *
     * @author Danny van Heumen
     */
    private final class PresenceWatcherTask extends TimerTask
    {
        /**
         * Static overhead for ISON response message.
         *
         * Additional 10 chars extra overhead as fail-safe, as I was not able to
         * find the exact number in the overhead computation.
         */
        private static final int ISON_RESPONSE_STATIC_MESSAGE_OVERHEAD = 18;

        /**
         * Set containing nicks that must be watched.
         */
        private final Set<String> watchList;

        /**
         * FIFO list storing each ISON query that is sent, for use when
         * responses return.
         */
        private final List<List<String>> queryList;

        /**
         * Reference to the current server identity.
         */
        private final AtomicReference<String> serverIdentity;

        /**
         * Constructor.
         *
         * @param watchList the list of nicks to watch
         * @param queryList list containing list of nicks of each ISON query
         * @param serverIdentity container with the current server identity for
         *            use in overhead calculation
         */
        public PresenceWatcherTask(final Set<String> watchList,
            final List<List<String>> queryList,
            final AtomicReference<String> serverIdentity)
        {
            if (watchList == null)
            {
                throw new IllegalArgumentException("watchList cannot be null");
            }
            this.watchList = watchList;
            if (queryList == null)
            {
                throw new IllegalArgumentException("queryList cannot be null");
            }
            this.queryList = queryList;
            if (serverIdentity == null)
            {
                throw new IllegalArgumentException(
                    "serverIdentity reference cannot be null");
            }
            this.serverIdentity = serverIdentity;
        }

        /**
         * The implementation of the task.
         */
        @Override
        public void run()
        {
            if (this.watchList.isEmpty())
            {
                LOGGER.trace("Watch list is empty. Not querying for online "
                    + "presence.");
                return;
            }
            if (this.serverIdentity.get() == null)
            {
                LOGGER.trace("Server identity not available yet. Skipping "
                    + "this presence status query.");
                return;
            }
            LOGGER
                .trace("Watch list contains nicks: querying presence status.");
            final StringBuilder query = new StringBuilder();
            final LinkedList<String> list;
            synchronized (this.watchList)
            {
                list = new LinkedList<String>(this.watchList);
            }
            LinkedList<String> nicks = new LinkedList<String>();
            // The ISON reply contains the most overhead, so base the maximum
            // number of nicks limit on that.
            final int maxQueryLength =
                MessageManager.IRC_PROTOCOL_MAX_MESSAGE_SIZE
                    - MessageManager.SAFETY_NET - overhead();
            for (String nick : list)
            {
                if (query.length() + nick.length() >= maxQueryLength)
                {
                    this.queryList.add(nicks);
                    BasicPollerPresenceWatcher.this.irc
                        .rawMessage(createQuery(query));
                    // Initialize new data types
                    query.delete(0, query.length());
                    nicks = new LinkedList<String>();
                }
                query.append(nick);
                query.append(' ');
                nicks.add(nick);
            }
            if (query.length() > 0)
            {
                // Send remaining entries.
                this.queryList.add(nicks);
                BasicPollerPresenceWatcher.this.irc
                    .rawMessage(createQuery(query));
            }
        }

        /**
         * Create an ISON query from the StringBuilder containing the list of
         * nicks.
         *
         * @param nicklist the list of nicks as a StringBuilder instance
         * @return returns the ISON query string
         */
        private String createQuery(final StringBuilder nicklist)
        {
            return "ISON " + nicklist;
        }

        /**
         * Calculate overhead for ISON response message.
         *
         * @return returns amount of overhead in response message
         */
        private int overhead()
        {
            return ISON_RESPONSE_STATIC_MESSAGE_OVERHEAD
                + this.serverIdentity.get().length()
                + BasicPollerPresenceWatcher.this.connectionState.getNickname()
                    .length();
        }
    }

    /**
     * Presence reply listener.
     *
     * Listener that acts on various replies that give an indication of actual
     * presence or presence changes, such as RPL_ISON and ERR_NOSUCHNICKCHAN.
     *
     * @author Danny van Heumen
     */
    private final class PresenceReplyListener
        extends AbstractIrcMessageListener
    {
        /**
         * Reply for ISON query.
         */
        private static final int RPL_ISON = 303;

        /**
         * Error reply in case nick does not exist on server.
         */
        private static final int ERR_NOSUCHNICK = 401;

        /**
         * Timer for presence watcher task.
         */
        private final Timer timer;

        /**
         * FIFO list containing list of nicks for each query.
         */
        private final List<List<String>> queryList;

        /**
         * Constructor.
         *
         * @param timer Timer for presence watcher task
         * @param queryList List of executed queries with expected nicks lists.
         */
        public PresenceReplyListener(final Timer timer,
            final List<List<String>> queryList)
        {
            super(BasicPollerPresenceWatcher.this.irc,
                BasicPollerPresenceWatcher.this.connectionState);
            if (timer == null)
            {
                throw new IllegalArgumentException("timer cannot be null");
            }
            this.timer = timer;
            if (queryList == null)
            {
                throw new IllegalArgumentException("queryList cannot be null");
            }
            this.queryList = queryList;
        }

        /**
         * Update nick watch list upon receiving a nick change message for a
         * nick that is on the watch list.
         *
         * NOTE: This nick change event could be handled earlier than the
         * handler that fires the contact rename event. This will result in a
         * missed presence update. However, since the nick change was just
         * announced, it is reasonable to assume that the user is still online.
         *
         * @param msg the nick message
         */
        @Override
        public void onNickChange(final NickMessage msg)
        {
            if (msg == null || msg.getSource() == null)
            {
                return;
            }
            final String oldNick = msg.getSource().getNick();
            final String newNick = msg.getNewNick();
            if (oldNick == null || newNick == null)
            {
                LOGGER.error("Incomplete nick change message. Old nick: '"
                    + oldNick + "', new nick: '" + newNick + "'.");
                return;
            }
            synchronized (BasicPollerPresenceWatcher.this.nickWatchList)
            {
                if (BasicPollerPresenceWatcher.this.nickWatchList
                    .contains(oldNick))
                {
                    update(oldNick, IrcStatusEnum.OFFLINE);
                }
                if (BasicPollerPresenceWatcher.this.nickWatchList
                    .contains(newNick))
                {
                    update(newNick, IrcStatusEnum.ONLINE);
                }
            }
        }

        /**
         * Message handling for RPL_ISON message and other indicators.
         *
         * @param msg the message
         */
        @Override
        public void onServerNumericMessage(final ServerNumericMessage msg)
        {
            if (msg == null || msg.getNumericCode() == null)
            {
                return;
            }
            switch (msg.getNumericCode())
            {
            case RPL_ISON:
                final String[] nicks = msg.getText().substring(1).split(" ");
                final List<String> offline;
                if (this.queryList.isEmpty())
                {
                    // If no query list exists, we can only update nicks that
                    // are online, since we do not know who we have actually
                    // queried for.
                    offline = new LinkedList<String>();
                }
                else
                {
                    offline = this.queryList.remove(0);
                }
                for (String nick : nicks)
                {
                    update(nick, IrcStatusEnum.ONLINE);
                    offline.remove(nick);
                }
                for (String nick : offline)
                {
                    update(nick, IrcStatusEnum.OFFLINE);
                }
                break;
            case ERR_NOSUCHNICK:
                final String errortext = msg.getText();
                final int idx = errortext.indexOf(' ');
                if (idx == -1)
                {
                    LOGGER.info("ERR_NOSUCHNICK message does not have "
                        + "expected format.");
                    return;
                }
                final String errNick = errortext.substring(0, idx);
                update(errNick, IrcStatusEnum.OFFLINE);
                break;
            default:
                break;
            }
        }

        /**
         * Upon receiving a private message from a user, conclude that the user
         * must then be online and update its presence status.
         *
         * @param msg the message
         */
        @Override
        public void onUserPrivMessage(final UserPrivMsg msg)
        {
            if (msg == null || msg.getSource() == null)
            {
                return;
            }
            final IRCUser user = msg.getSource();
            update(user.getNick(), IrcStatusEnum.ONLINE);
        }

        /**
         * Upon receiving a notice from a user, conclude that the user
         * must then be online and update its presence status.
         *
         * @param msg the message
         */
        @Override
        public void onUserNotice(final UserNotice msg)
        {
            if (msg == null || msg.getSource() == null)
            {
                return;
            }
            final IRCUser user = msg.getSource();
            update(user.getNick(), IrcStatusEnum.ONLINE);
        }

        /**
         * Upon receiving an action from a user, conclude that the user
         * must then be online and update its presence status.
         *
         * @param msg the message
         */
        @Override
        public void onUserAction(final UserActionMsg msg)
        {
            if (msg == null || msg.getSource() == null)
            {
                return;
            }
            final IRCUser user = msg.getSource();
            update(user.getNick(), IrcStatusEnum.ONLINE);
        }

        /**
         * Handler for channel join events.
         */
        @Override
        public void onChannelJoin(final ChanJoinMessage msg)
        {
            if (msg == null || msg.getSource() == null)
            {
                return;
            }
            final String user = msg.getSource().getNick();
            update(user, IrcStatusEnum.ONLINE);
        }

        /**
         * Handler for user quit events.
         *
         * @param msg the quit message
         */
        @Override
        public void onUserQuit(final QuitMessage msg)
        {
            super.onUserQuit(msg);
            final String user = msg.getSource().getNick();
            if (user == null)
            {
                return;
            }
            if (localUser(user))
            {
                // Stop presence watcher task.
                this.timer.cancel();
                updateAll(IrcStatusEnum.OFFLINE);
            }
            else
            {
                update(user, IrcStatusEnum.OFFLINE);
            }
        }

        /**
         * In case a fatal error occurs, remove the listener.
         *
         * @param msg the error message
         */
        @Override
        public void onError(final ErrorMessage msg)
        {
            super.onError(msg);
            // Stop presence watcher task.
            this.timer.cancel();
            updateAll(IrcStatusEnum.OFFLINE);
        }

        /**
         * In case a fatal error occurs, remove the listener.
         *
         * @param msg the error message
         */
        @Override
        public void onClientError(ClientErrorMessage msg)
        {
            super.onClientError(msg);
            // Stop presence watcher task.
            this.timer.cancel();
            updateAll(IrcStatusEnum.OFFLINE);
        }

        /**
         * Update the status of a single nick.
         *
         * @param nick the nick to update
         * @param status the new status
         */
        private void update(final String nick, final IrcStatusEnum status)
        {
            // User is some other user, so check if we are watching that nick.
            if (!BasicPollerPresenceWatcher.this.nickWatchList.contains(nick))
            {
                return;
            }
            BasicPollerPresenceWatcher.this.operationSet
                .updateNickContactPresence(nick, status);
        }

        /**
         * Update the status of all contacts in the nick watch list.
         *
         * @param status the new status
         */
        private void updateAll(final IrcStatusEnum status)
        {
            final LinkedList<String> list;
            synchronized (BasicPollerPresenceWatcher.this.nickWatchList)
            {
                list =
                    new LinkedList<String>(
                        BasicPollerPresenceWatcher.this.nickWatchList);
            }
            for (String nick : list)
            {
                BasicPollerPresenceWatcher.this.operationSet
                    .updateNickContactPresence(nick, status);
            }
        }
    }
}
