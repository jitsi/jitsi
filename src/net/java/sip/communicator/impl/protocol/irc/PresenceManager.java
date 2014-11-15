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
import com.ircclouds.irc.api.listeners.*;
import com.ircclouds.irc.api.state.*;

/**
 * Manager for presence status of IRC connection.
 *
 * TODO Support for 'a' (Away) user mode. (Check this again, since I also see
 * 'a' used for other purposes. This may be one of those ambiguous letters that
 * every server interprets differently.)
 *
 * TODO Jitsi is currently missing support for presence in MUC (ChatRoomMember).
 *
 * TODO Monitor presence using ISON, WATCH or MONITOR. (Monitor does not seem to
 * support away status, though)
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
     * Set of nicks to watch for presence changes.
     */
    private final SortedSet<String> nickWatchList = Collections
        .synchronizedSortedSet(new TreeSet<String>());

    /**
     * Maximum away message length according to server ISUPPORT instructions.
     *
     * <p>This value is not guaranteed, so it may be <tt>null</tt>.</p>
     */
    private final Integer isupportAwayLen;

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
     */
    public PresenceManager(final IRCApi irc, final IIRCState connectionState,
        final OperationSetPersistentPresenceIrcImpl operationSet)
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
        this.irc.addListener(new PresenceListener());
        this.isupportAwayLen = parseISupportAwayLen(this.connectionState);
        setUpPresenceWatcher();
    }

    /**
     * Set up a timer for watching the presence of nicks in the watch list.
     *
     * @param watchList the watch list
     * @param irc the IRC instance
     * @param connectionState the IRC connection state
     */
    private void setUpPresenceWatcher()
    {
        // FIFO query list to be shared between presence watcher task and
        // presence reply listener.
        final List<SortedSet<String>> queryList =
            Collections.synchronizedList(new LinkedList<SortedSet<String>>());
        final Timer presenceWatcher = new Timer();
        irc.addListener(new PresenceReplyListener(presenceWatcher, queryList));
        presenceWatcher.schedule(new PresenceWatcherTask(this.nickWatchList,
            queryList, this.irc), PRESENCE_WATCHER_PERIOD,
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
        extends VariousMessageListenerAdapter
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
         * Handle events for presence-related server replies.
         */
        @Override
        public void onServerNumericMessage(final ServerNumericMessage msg)
        {
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

        /**
         * In case the user quits, remove the presence listener.
         */
        @Override
        public void onUserQuit(final QuitMessage msg)
        {
            final String user = msg.getSource().getNick();
            if (user == null
                || !user.equals(
                        PresenceManager.this.connectionState.getNickname()))
            {
                return;
            }
            LOGGER.debug("Local user's QUIT message received: removing "
                + "presence listener.");
            PresenceManager.this.irc.deleteListener(this);
        }

        /**
         * In case a fatal error occurs, remove the presence listener.
         */
        @Override
        public void onError(final ErrorMessage aMsg)
        {
            // Errors signal fatal situation, so unregister and assume
            // connection lost.
            LOGGER.debug("Local user received ERROR message: removing presence "
                + "listener.");
            PresenceManager.this.irc.deleteListener(this);
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
         * Maximum length of an ISON query.
         */
        private static final int MAX_ISON_QUERY_LENGTH = 475;

        /**
         * List containing nicks that must be watched.
         */
        private final SortedSet<String> watchList;

        /**
         * FIFO list storing each ISON query that is sent, for use when
         * responses return.
         */
        private final List<SortedSet<String>> queryList;

        /**
         * IRC instance.
         */
        private final IRCApi irc;

        /**
         * Constructor.
         *
         * @param watchList the list of nicks to watch
         * @param irc the irc instance
         */
        public PresenceWatcherTask(final SortedSet<String> watchList,
            final List<SortedSet<String>> queryList, final IRCApi irc)
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
        }

        /**
         * The implementation of the task.
         */
        @Override
        public void run()
        {
            if (this.watchList.isEmpty())
            {
                return;
            }
            final StringBuilder query = new StringBuilder();
            TreeSet<String> nicks = new TreeSet<String>();
            for (String nick : this.watchList)
            {
                // FIXME determine maximum length of IRC ISON query
                if (query.length() + nick.length() >= MAX_ISON_QUERY_LENGTH)
                {
                    this.irc.rawMessage("ISON " + query.toString());
                    this.queryList.add(nicks);
                    // Initialize new data types
                    query.delete(0, query.length());
                    nicks = new TreeSet<String>();
                }
                query.append(nick);
                query.append(' ');
                nicks.add(nick);
            }
            if (query.length() > 0)
            {
                // Send remaining entries.
                this.irc.rawMessage("ISON " + query.toString());
                this.queryList.add(nicks);
            }
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
        extends VariousMessageListenerAdapter
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
         * FIFO query list containing set of nicks for each query.
         */
        private final List<SortedSet<String>> queryList;

        /**
         * Constructor.
         *
         * @param timer Timer for presence watcher task
         * @param queryList List of executed queries with expected nicks sets.
         */
        public PresenceReplyListener(final Timer timer,
            final List<SortedSet<String>> queryList)
        {
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
         * Message handling.
         *
         * FIXME update presence upon receiving PRIVMSG/NOTICE/ACTION from user
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
                if (LOGGER.isTraceEnabled())
                {
                    LOGGER.debug("RPL_ISON received: " + msg.asRaw());
                }
                final String[] nicks = msg.getText().substring(1).split(" ");
                final SortedSet<String> offline;
                if (this.queryList.isEmpty())
                {
                    offline = new TreeSet<String>();
                }
                else
                {
                    offline = this.queryList.remove(0);
                }
                for (String nick : nicks)
                {
                    PresenceManager.this.operationSet
                        .updateNickContactPresence(nick, IrcStatusEnum.ONLINE);
                    offline.remove(nick);
                }
                for (String nick : offline)
                {
                    PresenceManager.this.operationSet
                        .updateNickContactPresence(nick, IrcStatusEnum.OFFLINE);
                }
                break;
            case ERR_NOSUCHNICK:
                final String errortext = msg.getText();
                final int idx = errortext.indexOf(' ');
                if (idx == -1)
                {
                    LOGGER.debug("ERR_NOSUCHNICK message does not have "
                        + "expected format.");
                    return;
                }
                final String errNick = errortext.substring(0, idx);
                PresenceManager.this.operationSet.updateNickContactPresence(
                    errNick, IrcStatusEnum.OFFLINE);
                break;
            default:
                break;
            }
        }

        /**
         * Handler for channel join events.
         */
        @Override
        public void onChannelJoin(final ChanJoinMessage msg)
        {
            final String user = msg.getSource().getNick();
            if (user == null
                || !PresenceManager.this.nickWatchList.contains(user))
            {
                return;
            }
            PresenceManager.this.operationSet.updateNickContactPresence(user,
                IrcStatusEnum.ONLINE);
        }

        /**
         * Handler for user quit events.
         */
        @Override
        public void onUserQuit(final QuitMessage msg)
        {
            final String user = msg.getSource().getNick();
            if (user == null)
            {
                return;
            }
            if (user.equals(PresenceManager.this.connectionState.getNickname()))
            {
                // User is local user, stop all operations, cancel presence
                // watcher timer and unregister listener.
                LOGGER.debug("Local user's QUIT message received: removing "
                    + "PresenceReplyListener.");
                PresenceManager.this.irc.deleteListener(this);
                // Additionally, stop presence watcher task.
                this.timer.cancel();
            }
            else
            {
                // User is some other user, so check if we are watching that
                // nick.
                if (!PresenceManager.this.nickWatchList.contains(user))
                {
                    return;
                }
                PresenceManager.this.operationSet.updateNickContactPresence(
                    user, IrcStatusEnum.OFFLINE);
            }
        }

        /**
         * In case a fatal error occurs, remove the listener.
         */
        @Override
        public void onError(final ErrorMessage aMsg)
        {
            // Errors signal fatal situation, so unregister and assume
            // connection lost.
            LOGGER.debug("Local user received ERROR message: removing "
                + "PresenceReplyListener");
            PresenceManager.this.irc.deleteListener(this);
            // Additionally, stop presence watcher task.
            this.timer.cancel();
        }
    }
}
