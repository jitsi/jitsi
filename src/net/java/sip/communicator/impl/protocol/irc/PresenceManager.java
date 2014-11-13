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
    private String submittedMessage = "Away";

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
        final Timer presenceWatcher = new Timer();
        irc.addListener(new PresenceReplyListener(presenceWatcher));
        presenceWatcher.schedule(new PresenceWatcherTask(this.nickWatchList,
            this.irc), PRESENCE_WATCHER_PERIOD, PRESENCE_WATCHER_PERIOD);
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
         * List containing nicks that must be watched.
         */
        private final SortedSet<String> watchList;

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
            final IRCApi irc)
        {
            if (watchList == null)
            {
                throw new IllegalArgumentException("watchList cannot be null");
            }
            this.watchList = watchList;
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
            // FIXME naive implementation: should split on 510 byte message
            // limit.
            final StringBuilder query = new StringBuilder("ISON ");
            for (String nick : this.watchList)
            {
                query.append(nick);
                query.append(' ');
            }
            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace("Querying presence of nicks on watch list: "
                    + query.toString());
            }
            this.irc.rawMessage(query.toString());
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
         * Constructor.
         *
         * @param irc IRC instance
         * @param connectionState IRC connection state
         * @param timer Timer for presence watcher task
         * @param watchList List of nicks to watch for
         */
        public PresenceReplyListener(final Timer timer)
        {
            if (timer == null)
            {
                throw new IllegalArgumentException("timer cannot be null");
            }
            this.timer = timer;
        }

        /**
         * Message handling.
         *
         * TODO ERR_NOSUCHNICKCHAN: Check if target is Contact, then update
         * contact presence status to off-line since the nick apparently does
         * not exist anymore.
         */
        @Override
        public void onServerNumericMessage(final ServerNumericMessage msg)
        {
            if (msg == null)
            {
                return;
            }
            switch (msg.getNumericCode())
            {
            case RPL_ISON:
                LOGGER.debug(msg.getText());
                final HashSet<String> offline =
                    new HashSet<String>(PresenceManager.this.nickWatchList);
                String[] nicks = msg.getText().substring(1).split(" ");
                // FIXME naive implementation: only handles single reply
                // correctly.
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
                // FIXME optimize implementation, now not very efficient with
                // its sets
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
         * In case the user quits, remove the listener.
         */
        @Override
        public void onUserQuit(final QuitMessage msg)
        {
            final String user = msg.getSource().getNick();
            if (user == null
                || !user.equals(PresenceManager.this.connectionState
                    .getNickname()))
            {
                return;
            }
            LOGGER.debug("Local user's QUIT message received: removing "
                + "PresenceReplyListener.");
            PresenceManager.this.irc.deleteListener(this);
            // Additionally, stop presence watcher task.
            this.timer.cancel();
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
