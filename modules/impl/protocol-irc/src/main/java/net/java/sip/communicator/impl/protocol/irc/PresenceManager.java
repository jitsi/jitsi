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

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import net.java.sip.communicator.impl.protocol.irc.collection.*;
import net.java.sip.communicator.util.*;

import com.ircclouds.irc.api.*;
import com.ircclouds.irc.api.domain.messages.*;
import com.ircclouds.irc.api.state.*;

/**
 * Manager for presence status of IRC connection.
 *
 * There is support for online presence by polling (periodically querying IRC
 * server with ISON requests) for each of the members in the contact list or, if
 * supported by the IRC server, for the MONITOR command to subscribe to presence
 * notifications for the specified nick.
 *
 * TODO Support for 'a' (Away) user mode. (Check this again, since I also see
 * 'a' used for other purposes. This may be one of those ambiguous letters that
 * every server interprets differently.)
 *
 * TODO Support away-notify extension (CAP) and handle AWAY messages
 * appropriately.
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
     * Presence watcher.
     */
    private final PresenceWatcher watcher;

    /**
     * Maximum away message length according to server ISUPPORT instructions.
     *
     * <p>This value is not guaranteed, so it may be <tt>null</tt>.</p>
     */
    private final Integer isupportAwayLen;

    /**
     * Maximum size of MONITOR list allowed by server.
     *
     * <p>
     * This value is not guaranteed, so it may be <tt>null</tt>. If it is
     * <tt>null</tt> this means that MONITOR is not supported by this server.
     * </p>
     */
    private final Integer isupportMonitor;

    /**
     * Maximum size of WATCH list allowed by server.
     *
     * <p>
     * This value is not guaranteed, so it may be <tt>null</tt>. If it is
     * <tt>null</tt> this means that WATCH is not supported by this server.
     * </p>
     */
    private final Integer isupportWatch;

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
        final SortedSet<String> nickWatchList;
        if (persistentNickWatchList == null)
        {
            // watch list will be non-persistent, since we create an instance at
            // initialization time
            nickWatchList =
                Collections.synchronizedSortedSet(new TreeSet<String>());
        }
        else
        {
            nickWatchList = persistentNickWatchList;
        }
        this.irc.addListener(new LocalUserPresenceListener());
        // TODO move parse methods to ISupport enum type
        this.isupportAwayLen = parseISupportAwayLen(this.connectionState);
        this.isupportMonitor = parseISupportMonitor(this.connectionState);
        this.isupportWatch = parseISupportWatch(this.connectionState);
        final boolean enablePresencePolling =
            config.isContactPresenceTaskEnabled();
        if (this.isupportMonitor != null)
        {
            // Share a list of monitored nicks between the
            // MonitorPresenceWatcher and the BasicPollerPresenceWatcher.
            // Now it is possible for the basic poller to determine whether
            // or not to poll for a certain nick, such that we do not poll
            // nicks that are already monitored.
            final SortedSet<String> monitoredNicks =
                Collections.synchronizedSortedSet(new TreeSet<String>());
            this.watcher =
                new MonitorPresenceWatcher(this.irc, this.connectionState,
                    nickWatchList, monitoredNicks, this.operationSet,
                    this.isupportMonitor);
            if (enablePresencePolling)
            {
                // Enable basic poller as fall back mechanism.

                // Create a dynamic set that automatically computes the
                // difference between the full nick list and the list of nicks
                // that are subscribed to MONITOR. The difference will be the
                // result that is used by the basic poller.
                final Set<String> unmonitoredNicks =
                    new DynamicDifferenceSet<String>(nickWatchList,
                        monitoredNicks);
                new BasicPollerPresenceWatcher(this.irc, this.connectionState,
                    this.operationSet, unmonitoredNicks, this.serverIdentity);
            }
        }
        else if (this.isupportWatch != null)
        {
            // Share a list of monitored nicks between the
            // WatchPresenceWatcher and the BasicPollerPresenceWatcher.
            // Now it is possible for the basic poller to determine whether
            // or not to poll for a certain nick, such that we do not poll
            // nicks that are already monitored.
            final SortedSet<String> monitoredNicks =
                Collections.synchronizedSortedSet(new TreeSet<String>());
            this.watcher =
                new WatchPresenceWatcher(this.irc, this.connectionState,
                    nickWatchList, monitoredNicks, this.operationSet,
                    this.isupportWatch);
            if (enablePresencePolling)
            {
                // Enable basic poller as fall back mechanism.

                // Create a dynamic set that automatically computes the
                // difference between the full nick list and the list of nicks
                // that are subscribed to WATCH. The difference will be the
                // result that is used by the basic poller.
                final Set<String> unmonitoredNicks =
                    new DynamicDifferenceSet<String>(nickWatchList,
                        monitoredNicks);
                new BasicPollerPresenceWatcher(this.irc, this.connectionState,
                    this.operationSet, unmonitoredNicks, this.serverIdentity);
            }
        }
        else if (enablePresencePolling)
        {
            // Enable basic poller as the only presence mechanism.
            this.watcher =
                new BasicPollerPresenceWatcher(this.irc, this.connectionState,
                    this.operationSet, nickWatchList, this.serverIdentity);
        } else {
            this.watcher = null;
        }
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
            LOGGER.trace("No ISUPPORT parameter " + ISupport.AWAYLEN.name()
                + " available.");
            return null;
        }
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Setting ISUPPORT parameter "
                + ISupport.AWAYLEN.name() + " to " + value);
        }
        try
        {
            return new Integer(value);
        }
        catch (RuntimeException e)
        {
            LOGGER.warn("Failed to parse AWAYLEN value.", e);
            return null;
        }
    }

    /**
     * Parse the ISUPPORT parameter for MONITOR command support and list size.
     *
     * @param state the connection state
     * @return Returns instance with maximum number of entries in MONITOR list.
     *         Additionally, having this MONITOR property available, indicates
     *         that MONITOR is supported by the server.
     */
    private Integer parseISupportMonitor(final IIRCState state)
    {
        final String value =
            state.getServerOptions().getKey(ISupport.MONITOR.name());
        if (value == null)
        {
            LOGGER.trace("No ISUPPORT parameter " + ISupport.MONITOR.name()
                + " available.");
            return null;
        }
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Setting ISUPPORT parameter "
                + ISupport.MONITOR.name() + " to " + value);
        }
        try
        {
            return new Integer(value);
        }
        catch (RuntimeException e)
        {
            LOGGER.warn("Failed to parse MONITOR value.", e);
            return null;
        }
    }

    /**
     * Parse the ISUPPORT parameter for WATCH command support and list size.
     *
     * @param state the connection state
     * @return Returns instance with maximum number of entries in WATCH list.
     *         Additionally, having this WATCH property available, indicates
     *         that WATCH is supported by the server.
     */
    private Integer parseISupportWatch(final IIRCState state)
    {
        final String value =
            state.getServerOptions().getKey(ISupport.WATCH.name());
        if (value == null)
        {
            LOGGER.trace("No ISUPPORT parameter " + ISupport.WATCH.name()
                + " available.");
            return null;
        }
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Setting ISUPPORT parameter " + ISupport.WATCH.name()
                + " to " + value);
        }
        try
        {
            return new Integer(value);
        }
        catch (RuntimeException e)
        {
            LOGGER.warn("Failed to parse WATCH value.", e);
            return null;
        }
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
     * Query presence of provided nick.
     *
     * @param nick the nick
     * @return returns presence status
     * @throws InterruptedException interrupted exception in case waiting for
     *             WHOIS reply is interrupted
     * @throws IOException an exception occurred during the querying process
     */
    public IrcStatusEnum query(final String nick)
        throws InterruptedException,
        IOException
    {
        final Result<IrcStatusEnum, IllegalStateException> result =
            new Result<IrcStatusEnum, IllegalStateException>(
                IrcStatusEnum.OFFLINE);
        synchronized (result)
        {
            this.irc.addListener(new WhoisReplyListener(nick, result));
            this.irc.rawMessage("WHOIS "
                + IdentityManager.checkNick(nick, null));
            while (!result.isDone())
            {
                LOGGER.debug("Waiting for presence status based on WHOIS "
                    + "reply ...");
                result.wait();
            }
        }
        final Exception exception = result.getException();
        if (exception == null)
        {
            return result.getValue();
        }
        else
        {
            throw new IOException(
                "An exception occured while querying whois info.",
                result.getException());
        }
    }

    /**
     * Add new nick to watch list.
     *
     * @param nick nick to add to watch list
     */
    public void addNickWatch(final String nick)
    {
        if (this.watcher != null)
        {
            this.watcher.add(nick);
        }
    }

    /**
     * Remove nick from watch list.
     *
     * @param nick nick to remove from watch list
     */
    public void removeNickWatch(final String nick)
    {
        if (this.watcher != null)
        {
            this.watcher.remove(nick);
        }
    }

    /**
     * Presence listener implementation for keeping track of presence changes in
     * the IRC connection.
     *
     * @author Danny van Heumen
     */
    private final class LocalUserPresenceListener
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
        public LocalUserPresenceListener()
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
     * Listener for WHOIS replies, such that we can receive information of the
     * user that we are querying.
     *
     * @author Danny van Heumen
     */
    private final class WhoisReplyListener
        extends AbstractIrcMessageListener
    {
        /**
         * Reply for away message.
         */
        private static final int IRC_RPL_AWAY = 301;

        /**
         * Reply for WHOIS query with user info.
         */
        private static final int IRC_RPL_WHOISUSER = 311;

        /**
         * Reply for signaling end of WHOIS query.
         */
        private static final int IRC_RPL_ENDOFWHOIS = 318;

        /**
         * The nick that is being queried.
         */
        private final String nick;

        /**
         * The result instance that will be updated after having received the
         * RPL_ENDOFWHOIS reply.
         */
        private final Result<IrcStatusEnum, IllegalStateException> result;

        /**
         * Intermediate presence status. Updated upon receiving new WHOIS
         * information.
         */
        private IrcStatusEnum presence;

        /**
         * Constructor.
         *
         * @param nick the nick
         * @param result the result
         */
        private WhoisReplyListener(final String nick,
            final Result<IrcStatusEnum, IllegalStateException> result)
        {
            super(PresenceManager.this.irc,
                PresenceManager.this.connectionState);
            if (nick == null)
            {
                throw new IllegalArgumentException("Invalid nick specified.");
            }
            this.nick = nick;
            if (result == null)
            {
                throw new IllegalArgumentException("Invalid result.");
            }
            this.result = result;
            this.presence = IrcStatusEnum.OFFLINE;
        }

        /**
         * Handle the numeric messages that the WHOIS answer consists of.
         *
         * @param msg the numeric message
         */
        @Override
        public void onServerNumericMessage(final ServerNumericMessage msg)
        {
            if (!this.nick.equals(msg.getTarget()))
            {
                return;
            }
            switch (msg.getNumericCode())
            {
            case IRC_RPL_WHOISUSER:
                if (this.presence != IrcStatusEnum.AWAY)
                {
                    // only update presence if not set to away, since away
                    // status is more specific than the more general information
                    // of being online
                    this.presence = IrcStatusEnum.ONLINE;
                }
                break;
            case IRC_RPL_AWAY:
                this.presence = IrcStatusEnum.AWAY;
                break;
            case IRC_RPL_ENDOFWHOIS:
                this.irc.deleteListener(this);
                synchronized (this.result)
                {
                    this.result.setDone(this.presence);
                    this.result.notifyAll();
                }
                break;
            default:
                break;
            }
        }

        /**
         * Upon connection quitting, set exception and return result.
         */
        @Override
        public void onUserQuit(QuitMessage msg)
        {
            super.onUserQuit(msg);
            if (localUser(msg.getSource().getNick()))
            {
                synchronized (this.result)
                {
                    this.result.setDone(new IllegalStateException(
                        "Local user quit."));
                    this.result.notifyAll();
                }
            }
        }

        /**
         * Upon receiving an error, set exception and return result.
         */
        @Override
        public void onError(ErrorMessage msg)
        {
            super.onError(msg);
            synchronized (this.result)
            {
                this.result.setDone(new IllegalStateException(
                    "An error occurred: " + msg.getText()));
                this.result.notifyAll();
            }
        }

        /**
         * In case a client-side fatal error occurs, remove the listener.
         *
         * @param msg the error message
         */
        @Override
        public void onClientError(ClientErrorMessage msg)
        {
            super.onClientError(msg);
            synchronized (this.result)
            {
                this.result.setDone(new IllegalStateException(
                    "An error occurred: " + msg.asRaw()));
                this.result.notifyAll();
            }
        }
    }
}
