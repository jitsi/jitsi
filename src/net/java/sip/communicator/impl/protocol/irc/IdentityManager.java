package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.util.*;

import com.ircclouds.irc.api.*;
import com.ircclouds.irc.api.domain.messages.*;
import com.ircclouds.irc.api.listeners.*;
import com.ircclouds.irc.api.state.*;

/**
 * Identity manager.
 *
 * @author Danny van Heumen
 */
public class IdentityManager
{
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger
        .getLogger(IdentityManager.class);

    /**
     * The IRCApi instance.
     */
    private final IRCApi irc;

    /**
     * The connection state.
     */
    private final IIRCState connectionState;

    /**
     * The identity container.
     */
    private final Identity identity = new Identity();

    /**
     * Constructor.
     *
     * @param irc the IRCApi instance
     * @param connectionState the connection state
     */
    public IdentityManager(final IRCApi irc, final IIRCState connectionState)
    {
        if (irc == null)
        {
            throw new IllegalArgumentException("irc instance cannot be null");
        }
        this.irc = irc;
        if (connectionState == null)
        {
            throw new IllegalArgumentException(
                "connectionState instance cannot be null");
        }
        this.connectionState = connectionState;
        // query user's WHOIS identity as perceived by the IRC server
        queryIdentity(this.irc, this.connectionState, this.identity);
    }

    /**
     * Issue WHOIS query to discover identity as seen by the server.
     *
     * @param irc IRCApi instance
     * @param connectionState the connection state
     * @param identity the identity container
     */
    private static void queryIdentity(final IRCApi irc,
        final IIRCState connectionState, final Identity identity)
    {
        synchronized (irc)
        {
            irc.addListener(new WhoisListener(irc, connectionState, identity));
            irc.rawMessage("WHOIS " + connectionState.getNickname());
        }
    }

    /**
     * Get the current identity string, based on nick, user and host of local
     * user.
     *
     * @return returns identity string
     */
    public String getIdentityString()
    {
        final String currentNick = this.connectionState.getNickname();
        return this.identity.getString(currentNick);
    }

    /**
     * The Whois listener which uses the WHOIS data for the local user to update
     * the identity information in the provided container.
     *
     * @author Danny van Heumen
     */
    private static final class WhoisListener
        extends VariousMessageListenerAdapter
    {
        /**
         * IRC reply for WHOIS query.
         */
        private static final int RPL_WHOISUSER = 311;

        /**
         * The IRCApi instance.
         */
        private final IRCApi irc;

        /**
         * The connection state.
         */
        private final IIRCState connectionState;

        /**
         * The identity container that contains the WHOIS data for user and
         * host.
         */
        private final Identity identity;

        /**
         * Constructor.
         *
         * @param irc the IRCApi instance
         * @param connectionState the connection state
         * @param identity the identity container
         */
        private WhoisListener(final IRCApi irc, final IIRCState connectionState,
            final Identity identity)
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
            if (identity == null)
            {
                throw new IllegalArgumentException("identity cannot be null");
            }
            this.identity = identity;
        }

        /**
         * On receiving a server numeric message.
         *
         * @param msg Server numeric message
         */
        @Override
        public void onServerNumericMessage(final ServerNumericMessage msg)
        {
            switch (msg.getNumericCode().intValue())
            {
            case RPL_WHOISUSER:
                final String whoismsg = msg.getText();
                final int endNickIndex = whoismsg.indexOf(' ');
                final String nick = whoismsg.substring(0, endNickIndex);
                if (!this.connectionState.getNickname().equals(nick))
                {
                    // We can only use WHOIS info about ourselves to discover
                    // our identity on the IRC server. So skip other WHOIS
                    // replies.
                    return;
                }
                updateIdentity(whoismsg);
                // Once the WHOIS reply is processed and the identity is
                // updated, we can delete the listener as the purpose is
                // fulfilled.
                this.irc.deleteListener(this);
                break;
            default:
                break;
            }
        }

        /**
         * Update the identity container instance with received WHOIS data.
         *
         * @param whoismsg the WHOIS reply message content
         */
        private void updateIdentity(final String whoismsg)
        {
            final int endNickIndex = whoismsg.indexOf(' ');
            final int endUserIndex = whoismsg.indexOf(' ', endNickIndex + 1);
            final int endHostIndex = whoismsg.indexOf(' ', endUserIndex + 1);
            final String user =
                whoismsg.substring(endNickIndex + 1, endUserIndex);
            final String host =
                whoismsg.substring(endUserIndex + 1, endHostIndex);
            this.identity.setHost(host);
            this.identity.setUser(user);
            LOGGER.debug(String.format("Current identity: %s!%s@%s",
                this.connectionState.getNickname(), user, host));
        }
    }

    /**
     * Storage container for identity components.
     *
     * IRC identity components user and host are stored. The nick name component
     * isn't stored, because it changes too frequently. When getting the
     * identity string, the nick name component is provided at calling time.
     *
     * @author Danny van Heumen
     */
    private static final class Identity
    {
        /**
         * User name.
         */
        private String user = null;

        /**
         * Host name.
         */
        private String host = null;

        /**
         * Constructor.
         *
         * @param user user
         * @param host host
         */
        private Identity()
        {
        }

        /**
         * Set user.
         *
         * @param user the new user
         */
        private void setUser(final String user)
        {
            this.user = user;
        }

        /**
         * Set host.
         *
         * @param host the new host
         */
        private void setHost(final String host)
        {
            this.host = host;
        }

        /**
         * Get identity string.
         *
         * @param currentNick the current nick
         * @return returns identity string
         */
        private String getString(final String currentNick)
        {
            return String.format("%s!%s@%s", currentNick, this.user, this.host);
        }
    }
}
