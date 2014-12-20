/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;
import java.util.concurrent.atomic.*;

import net.java.sip.communicator.util.*;

import com.ircclouds.irc.api.*;
import com.ircclouds.irc.api.domain.*;
import com.ircclouds.irc.api.domain.messages.*;
import com.ircclouds.irc.api.state.*;

/**
 * Manager for presence status of IRC connection.
 *
 * There is (somewhat primitive) support for online presence by periodically
 * querying IRC server with ISON requests for each of the members in the contact
 * list.
 *
 * TODO Support for 'a' (Away) user mode. (Check this again, since I also see
 * 'a' used for other purposes. This may be one of those ambiguous letters that
 * every server interprets differently.)
 *
 * TODO Improve presence watcher by using WATCH or MONITOR. (Monitor does not
 * seem to support away status, though)
 *
 * @author Danny van Heumen
 */
public class PresenceManager
{
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger
        .getLogger(PresenceManager.class);

    /**
     * Delay before starting the presence watcher task for the first time.
     */
    private static final long INITIAL_PRESENCE_WATCHER_DELAY = 10000L;

    /**
     * Period for the presence watcher timer.
     */
    private static final long PRESENCE_WATCHER_PERIOD = 60000L;

    /**
     * IRC client library instance.
     *
     * Instance must be thread-safe!
     */
    private final IRCApi irc;

    /**
     * IRC client connection state.
     */
    private final IIRCState connectionState;

    /**
     * Instance of OperationSetPersistentPresence for updates.
     */
    private final OperationSetPersistentPresenceIrcImpl operationSet;

    /**
     * Synchronized set of nicks to watch for presence changes.
     */
    private final SortedSet<String> nickWatchList;

    /**
     * Maximum away message length according to server ISUPPORT instructions.
     *
     * <p>This value is not guaranteed, so it may be <tt>null</tt>.</p>
     */
    private final Integer isupportAwayLen;

    /**
     * Server identity.
     */
    private final AtomicReference<String> serverIdentity =
        new AtomicReference<String>(null);

    /**
     * Current presence status.
     */
    private volatile boolean away = false;

    /**
     * Active away message.
     */
    private volatile String currentMessage = "";

    /**
     * Proposed away message.
     */
    private volatile String submittedMessage = "Away";

    /**
     * Constructor.
     *
     * @param irc thread-safe irc client library instance
     * @param connectionState irc client connection state instance
     * @param operationSet OperationSetPersistentPresence irc implementation for
     *            handling presence changes.
     * @param config Client configuration
     * @param persistentNickWatchList persistent nick watch list to use (The
     *            sortedset implementation must be synchronized!)
     */
    public PresenceManager(final IRCApi irc, final IIRCState connectionState,
        final OperationSetPersistentPresenceIrcImpl operationSet,
        final ClientConfig config,
        final SortedSet<String> persistentNickWatchList)
    {
        if (connectionState == null)
        {
            throw new IllegalArgumentException(
                "connectionState cannot be null");
        }
        this.connectionState = connectionState;
        if (operationSet == null)
        {
            throw new IllegalArgumentException("operationSet cannot be null");
        }
        this.operationSet = operationSet;
        if (irc == null)
        {
            throw new IllegalArgumentException("irc cannot be null");
        }
        this.irc = irc;
        if (persistentNickWatchList == null)
        {
            this.nickWatchList =
                Collections.synchronizedSortedSet(new TreeSet<String>());
        }
        else
        {
            this.nickWatchList = persistentNickWatchList;
        }
        this.irc.addListener(new PresenceListener());
        this.isupportAwayLen = parseISupportAwayLen(this.connectionState);
        if (config.isContactPresenceTaskEnabled())
        {
            setUpPresenceWatcher();
        }
    }

    /**
     * Set up a timer for watching the presence of nicks in the watch list.
     */
    private void setUpPresenceWatcher()
    {
        // FIFO query list to be shared between presence watcher task and
        // presence reply listener.
        final List<List<String>> queryList =
            Collections.synchronizedList(new LinkedList<List<String>>());
        final Timer presenceWatcher = new Timer();
        irc.addListener(new PresenceReplyListener(presenceWatcher, queryList));
        final PresenceWatcherTask task =
            new PresenceWatcherTask(this.nickWatchList, queryList, this.irc,
                this.connectionState, this.serverIdentity);
        presenceWatcher.schedule(task, INITIAL_PRESENCE_WATCHER_DELAY,
            PRESENCE_WATCHER_PERIOD);
        LOGGER.trace("Presence watcher set up.");
    }

    /**
     * Parse the ISUPPORT parameter for server's away message length.
     *
     * @param state the connection state
     * @return returns instance with max away message length or <tt>null</tt> if
     *         not specified.
     */
    private Integer parseISupportAwayLen(final IIRCState state)
    {
        final String value =
            state.getServerOptions().getKey(ISupport.AWAYLEN.name());
        if (value == null)
        {
            return null;
        }
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Setting ISUPPORT parameter "
                + ISupport.AWAYLEN.name() + " to " + value);
        }
        return new Integer(value);
    }

    /**
     * Check current Away state.
     *
     * @return returns <tt>true</tt> if away or <tt>false</tt> if not away
     */
    public boolean isAway()
    {
        return this.away;
    }

    /**
     * Get away message.
     *
     * @return returns currently active away message or "" if currently not
     *         away.
     */
    public String getMessage()
    {
        return this.currentMessage;
    }

    /**
     * Set away status and message. Disable away status by providing
     * <tt>null</tt> message.
     *
     * @param isAway <tt>true</tt> to enable away mode + message, or
     *            <tt>false</tt> to disable
     * @param awayMessage away message, the message is only available when the
     *            local user is set to away. If <tt>null</tt> is provided, don't
     *            set a new away message.
     */
    public void away(final boolean isAway, final String awayMessage)
    {
        if (awayMessage != null)
        {
            this.submittedMessage = verifyMessage(awayMessage);
        }

        if (isAway && (!this.away || awayMessage != null))
        {
            // In case we aren't AWAY yet, or if there is a message to set.
            this.irc.rawMessage("AWAY :" + this.submittedMessage);
        }
        else if (isAway != this.away)
        {
            this.irc.rawMessage("AWAY");
        }
    }

    /**
     * Set new prepared away message for later moment when IRC connection is set
     * to away.
     *
     * @param message the away message to prepare
     * @return returns message after verification
     */
    private String verifyMessage(final String message)
    {
        if (message == null || message.isEmpty())
        {
            throw new IllegalArgumentException(
                "away message must be non-null and non-empty");
        }
        if (this.isupportAwayLen != null
            && message.length() > this.isupportAwayLen.intValue())
        {
            throw new IllegalArgumentException(
                "the away message must not be longer than "
                    + this.isupportAwayLen.intValue()
                    + " characters according to server's parameters.");
        }
        return message;
    }

    /**
     * Add new nick to watch list.
     *
     * @param nick nick to add to watch list
     */
    public void addNickWatch(final String nick)
    {
        this.nickWatchList.add(nick);
    }

    /**
     * Remove nick from watch list.
     *
     * @param nick nick to remove from watch list
     */
    public void removeNickWatch(final String nick)
    {
        this.nickWatchList.remove(nick);
    }

    /**
     * Presence listener implementation for keeping track of presence changes in
     * the IRC connection.
     *
     * @author Danny van Heumen
     */
    private final class PresenceListener
        extends AbstractIrcMessageListener
    {
        /**
         * Reply for acknowledging transition to available (not away any
         * longer).
         */
        private static final int IRC_RPL_UNAWAY = 305;

        /**
         * Reply for acknowledging transition to away.
         */
        private static final int IRC_RPL_NOWAWAY = 306;

        /**
         * Constructor.
         */
        public PresenceListener()
        {
            super(PresenceManager.this.irc,
                PresenceManager.this.connectionState);
        }

        /**
         * Handle events for presence-related server replies.
         */
        @Override
        public void onServerNumericMessage(final ServerNumericMessage msg)
        {
            if (PresenceManager.this.serverIdentity.get() == null)
            {
                PresenceManager.this.serverIdentity.set(msg.getSource()
                    .getHostname());
            }
            Integer msgCode = msg.getNumericCode();
            if (msgCode == null)
            {
                return;
            }
            int code = msgCode.intValue();
            switch (code)
            {
            case IRC_RPL_UNAWAY:
                PresenceManager.this.currentMessage = "";
                PresenceManager.this.away = false;
                operationSet.updatePresenceStatus(IrcStatusEnum.AWAY,
                    IrcStatusEnum.ONLINE);
                LOGGER.debug("Away status disabled.");
                break;
            case IRC_RPL_NOWAWAY:
                PresenceManager.this.currentMessage =
                    PresenceManager.this.submittedMessage;
                PresenceManager.this.away = true;
                operationSet.updatePresenceStatus(IrcStatusEnum.ONLINE,
                    IrcStatusEnum.AWAY);
                LOGGER.debug("Away status enabled with message \""
                    + PresenceManager.this.currentMessage + "\"");
                break;
            default:
                break;
            }
        }
    }

    /**
     * Task for watching nick presence.
     *
     * @author Danny van Heumen
     */
    private static final class PresenceWatcherTask extends TimerTask
    {
        /**
         * Static overhead for ISON response message.
         *
         * Additional 10 chars extra overhead as fail-safe, as I was not able to
         * find the exact number in the overhead computation.
         */
        private static final int ISON_RESPONSE_STATIC_MESSAGE_OVERHEAD = 18;

        /**
         * List containing nicks that must be watched.
         */
        private final SortedSet<String> watchList;

        /**
         * FIFO list storing each ISON query that is sent, for use when
         * responses return.
         */
        private final List<List<String>> queryList;

        /**
         * IRC instance.
         */
        private final IRCApi irc;

        /**
         * IRC connection state.
         */
        private final IIRCState connectionState;

        /**
         * Reference to the current server identity.
         */
        private final AtomicReference<String> serverIdentity;

        /**
         * Constructor.
         *
         * @param watchList the list of nicks to watch
         * @param queryList list containing list of nicks of each ISON query
         * @param irc the irc instance
         * @param connectionState the connection state instance
         * @param serverIdentity container with the current server identity for
         *            use in overhead calculation
         */
        public PresenceWatcherTask(final SortedSet<String> watchList,
            final List<List<String>> queryList, final IRCApi irc,
            final IIRCState connectionState,
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
                MessageManager.IRC_PROTOCOL_MAXIMUM_MESSAGE_SIZE - overhead();
            for (String nick : list)
            {
                if (query.length() + nick.length() >= maxQueryLength)
                {
                    this.queryList.add(nicks);
                    this.irc.rawMessage(createQuery(query));
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
                this.irc.rawMessage(createQuery(query));
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
                + this.connectionState.getNickname().length();
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
            super(PresenceManager.this.irc,
                PresenceManager.this.connectionState);
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
            synchronized (PresenceManager.this.nickWatchList)
            {
                if (PresenceManager.this.nickWatchList.contains(oldNick))
                {
                    PresenceManager.this.nickWatchList.remove(oldNick);
                    PresenceManager.this.nickWatchList.add(newNick);
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
         * Update the status of a single nick.
         *
         * @param nick the nick to update
         * @param status the new status
         */
        private void update(final String nick, final IrcStatusEnum status)
        {
            // User is some other user, so check if we are watching that nick.
            if (!PresenceManager.this.nickWatchList.contains(nick))
            {
                return;
            }
            PresenceManager.this.operationSet.updateNickContactPresence(nick,
                status);
        }

        /**
         * Update the status of all contacts in the nick watch list.
         *
         * @param status the new status
         */
        private void updateAll(final IrcStatusEnum status)
        {
            final LinkedList<String> list;
            synchronized (PresenceManager.this.nickWatchList)
            {
                list =
                    new LinkedList<String>(PresenceManager.this.nickWatchList);
            }
            for (String nick : list)
            {
                PresenceManager.this.operationSet.updateNickContactPresence(
                    nick, status);
            }
        }
    }
}
