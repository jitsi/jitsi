/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

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
 * TODO ERR_NOSUCHNICKCHAN: Check if target is Contact, then update contact
 * presence status to off-line since the nick apparently does not exist anymore.
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
    private String currentMessage = "";

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
}
