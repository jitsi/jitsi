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

import java.util.regex.*;

import net.java.sip.communicator.util.*;

import com.ircclouds.irc.api.*;
import com.ircclouds.irc.api.domain.messages.*;
import com.ircclouds.irc.api.state.*;

/**
 * Identity manager.
 *
 * TODO Add support for Identity Service (NickServ) instance that can be used
 * for accessing remote identity facilities.
 *
 * TODO Implement OperationSetChangePassword once an identity service is
 * available.
 *
 * TODO Query remote identity service for current identity-state such as:
 * unknown, unauthenticated, authenticated.
 *
 * TODO Catch 900 (AUTHENTICATE LoggedIn) message and extract identity from
 * there so that we do not have to do a separate WHOIS query.
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
     * Pattern of a valid nick.
     */
    public static final Pattern NICK_PATTERN = Pattern
        .compile("[A-Za-z\\[\\]\\\\`\\^\\{\\}_\\|]"
            + "[A-Za-z0-9\\-\\[\\]\\\\`\\^\\{\\}_\\|]*");

    /**
     * The IRCApi instance.
     *
     * Instance must be thread-safe!
     */
    private final IRCApi irc;

    /**
     * The connection state.
     */
    private final IIRCState connectionState;

    /**
     * The protocol provider instance.
     */
    private final ProtocolProviderServiceIrcImpl provider;

    /**
     * The identity container.
     */
    private final Identity identity = new Identity();

    /**
     * Maximum nick length according to server ISUPPORT instructions.
     *
     * <p>This value is not guaranteed, so it may be <tt>null</tt>.</p>
     */
    private final Integer isupportNickLen;

    /**
     * Constructor.
     *
     * @param irc thread-safe IRCApi instance
     * @param connectionState the connection state
     * @param provider the protocol provider instance
     */
    public IdentityManager(final IRCApi irc, final IIRCState connectionState,
        final ProtocolProviderServiceIrcImpl provider)
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
        if (provider == null)
        {
            throw new IllegalArgumentException("provider cannot be null");
        }
        this.provider = provider;
        this.irc.addListener(new IdentityListener());
        // query user's WHOIS identity as perceived by the IRC server
        queryIdentity(this.irc, this.connectionState, new WhoisListener());
        isupportNickLen = parseISupportNickLen(this.connectionState);
    }

    /**
     * Issue WHOIS query to discover identity as seen by the server.
     */
    private static void queryIdentity(final IRCApi irc, final IIRCState state,
        final WhoisListener listener)
    {
        // This method should be as light-weight as possible, since it is called
        // from the constructor.
        new Thread()
        {

            public void run()
            {
                try
                {
                    irc.addListener(listener);
                    irc.rawMessage("WHOIS " + state.getNickname());
                }
                catch (final RuntimeException e)
                {
                    LOGGER.error("Failed to deliver WHOIS message.", e);
                }
            };
        }.start();
    }

    /**
     * Parse the ISUPPORT parameter for server's max nick length.
     *
     * @param state the connection state
     * @return returns instance with max nick length or <tt>null</tt> if not
     *         specified.
     */
    private static Integer parseISupportNickLen(final IIRCState state)
    {
        final String value =
            state.getServerOptions().getKey(ISupport.NICKLEN.name());
        if (value == null)
        {
            return null;
        }
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Setting ISUPPORT parameter "
                + ISupport.NICKLEN.name() + " to " + value);
        }
        return new Integer(value);
    }

    /**
     * Get the nick name of the user.
     *
     * @return Returns either the acting nick if a connection is established or
     *         the configured nick.
     */
    public String getNick()
    {
        return this.connectionState.getNickname();
    }

    /**
     * Set a new nick name.
     *
     * @param nick new nick
     */
    public void setNick(final String nick)
    {
        this.irc.changeNick(checkNick(nick, this.isupportNickLen));
    }

    /**
     * Verify nick name.
     *
     * @param nick nick name
     * @param isupportNickLen maximum nick length according to server
     *            parameters.
     * @return returns nick name
     */
    public static String checkNick(final String nick,
        final Integer isupportNickLen)
    {
        if (nick == null || nick.isEmpty())
        {
            throw new IllegalArgumentException("a nick name must be provided");
        }
        if (!NICK_PATTERN.matcher(nick).matches())
        {
            throw new IllegalArgumentException(
                "nick name contains invalid characters: only letters, "
                    + "digits and -, \\, [, ], `, ^, {, }, |, _ are allowed");
        }
        if (isupportNickLen != null && nick.length() > isupportNickLen)
        {
            throw new IllegalArgumentException("the nick name must not be "
                + "longer than " + isupportNickLen.intValue() + " characters "
                + "according to server parameters.");
        }
        return nick;
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
    private final class WhoisListener
        extends AbstractIrcMessageListener
    {
        /**
         * IRC reply for WHOIS query.
         */
        private static final int RPL_WHOISUSER = 311;

        /**
         * Constructor.
         */
        public WhoisListener()
        {
            super(IdentityManager.this.irc,
                IdentityManager.this.connectionState);
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
                if (!localUser(nick))
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
            IdentityManager.this.identity.setHost(host);
            IdentityManager.this.identity.setUser(user);
            LOGGER.debug(String.format("Current identity: %s!%s@%s",
                this.connectionState.getNickname(), user, host));
        }
    }

    /**
     * General listener for identity-related events.
     *
     * @author Danny van Heumen
     */
    private final class IdentityListener
        extends AbstractIrcMessageListener
    {
        /**
         * Constructor.
         */
        public IdentityListener()
        {
            super(IdentityManager.this.irc,
                IdentityManager.this.connectionState);
        }

        /**
         * Nick change event.
         *
         * @param msg nick change event message
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
            IdentityManager.this.provider.getPersistentPresence().updateNick(
                oldNick, newNick);
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
         * Check whether or not the Identity information is set.
         *
         * @return returns <tt>true</tt> if identity is set, or <tt>false</tt>
         *         if it is not (yet) set.
         */
        private boolean isSet()
        {
            return this.user != null && this.host != null;
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
