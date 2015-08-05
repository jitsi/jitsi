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
import java.nio.channels.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import javax.net.ssl.*;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import com.ircclouds.irc.api.*;
import com.ircclouds.irc.api.domain.*;
import com.ircclouds.irc.api.domain.messages.interfaces.*;
import com.ircclouds.irc.api.listeners.*;

/**
 * An implementation of IRC using the irc-api library.
 *
 * TODO Correctly disconnect IRC connection upon quitting.
 *
 * @author Danny van Heumen
 */
public class IrcStack implements IrcConnectionListener
{
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(IrcStack.class);

    /**
     * Parent provider for IRC.
     */
    private final ProtocolProviderServiceIrcImpl provider;

    /**
     * Server parameters that are set and provided during the connection
     * process.
     */
    private final ServerParameters params;

    /**
     * The persistent context that will survive (dis)connects.
     */
    private final PersistentContext context;

    /**
     * Instance of the irc connection contained in an AtomicReference.
     *
     * This field serves 2 purposes:
     *
     * First is the container itself that we use to synchronize on while
     * (dis)connecting and eventually setting new instance variable before
     * unlocking. By synchronizing we have connect and disconnect operations
     * wait for each other.
     *
     * Second is to get the current connection instance. AtomicReference ensures
     * that we either get the old or the new instance.
     */
    private final AtomicReference<IrcConnection> session =
        new AtomicReference<IrcConnection>(null);

    /**
     * Constructor.
     *
     * @param parentProvider Parent provider
     * @param nick User's nick name
     * @param login User's login name
     * @param version Version
     * @param finger Finger
     */
    public IrcStack(final ProtocolProviderServiceIrcImpl parentProvider,
        final String nick, final String login, final String version,
        final String finger)
    {
        if (parentProvider == null)
        {
            throw new NullPointerException("parentProvider cannot be null");
        }
        this.provider = parentProvider;
        this.params = new IrcStack.ServerParameters(nick, login, finger, null);
        this.context = new PersistentContext(this.provider);
    }

    /**
     * Connect to specified host, port, optionally using a password.
     *
     * @param host IRC server's host name
     * @param port IRC port
     * @param password password for the specified nick name
     * @param secureConnection true to set up secure connection, or false if
     *            not.
     * @param autoNickChange do automatic nick changes if nick is in use
     * @param config Client configuration
     * @throws OperationFailedException in case of user canceling because of
     *             certificate errors
     * @throws Exception throws exceptions
     */
    public void connect(final String host, final int port,
        final String password, final boolean secureConnection,
        final boolean autoNickChange, final ClientConfig config)
        throws OperationFailedException,
        Exception
    {
        final String plainPass = determinePlainPassword(password, config);
        final IRCServer server =
            createServer(config, host, port, secureConnection, plainPass);

        try
        {
            synchronized (this.session)
            {
                final IrcConnection current = this.session.get();
                if (current != null && current.isConnected())
                {
                    return;
                }

                this.params.setServer(server);

                final IRCApi irc = new IRCApiImpl(true);

                if (LOGGER.isTraceEnabled())
                {
                    // If tracing is enabled, register another listener that
                    // logs all IRC messages as published by the IRC client
                    // library.
                    irc.addListener(new DebugListener());
                }

                // Synchronized IRCApi instance passed on to the connection
                // instance.
                this.session.set(new IrcConnection(this.context, config, irc,
                    this.params, password, this));

                this.provider.setCurrentRegistrationState(
                    RegistrationState.REGISTERED,
                    RegistrationStateChangeEvent.REASON_USER_REQUEST);
            }
        }
        catch (IOException e)
        {
            if (isCausedByCertificateException(e))
            {
                LOGGER.info("Connection aborted due to server certificate.");
                // If it is caused by a certificate exception, it is because the
                // user doesn't trust the certificate. Set to unregistered
                // instead of indicating a failure to connect.
                this.provider.setCurrentRegistrationState(
                    RegistrationState.UNREGISTERED,
                    RegistrationStateChangeEvent.REASON_USER_REQUEST);
                throw new OperationFailedException(
                    "Failed certificate verification.",
                    OperationFailedException.OPERATION_CANCELED);
            }
            else
            {
                // SSL exceptions will be caught here too.
                this.provider.setCurrentRegistrationState(
                    RegistrationState.CONNECTION_FAILED,
                    RegistrationStateChangeEvent.REASON_NOT_SPECIFIED);
                throw e;
            }
        }
        catch (InterruptedException e)
        {
            this.provider.setCurrentRegistrationState(
                RegistrationState.UNREGISTERED,
                RegistrationStateChangeEvent.REASON_USER_REQUEST);
            throw e;
        }
        catch (NotYetConnectedException e)
        {
            this.provider.setCurrentRegistrationState(
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED);
            throw e;
        }
        catch (Exception e)
        {
            // For any other (unexpected error) first log the error itself for
            // debugging purposes. Then rethrow.
            LOGGER.error("Unanticipated exception occurred!", e);
            this.provider.setCurrentRegistrationState(
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_INTERNAL_ERROR);
            throw e;
        }
    }

    /**
     * Create matching IRCServer instances based on connection parameters.
     *
     * @param config the IRC config
     * @param host the IRC server host
     * @param port the IRC server port
     * @param secureConnection <tt>true</tt> for a secure connection,
     *            <tt>false</tt> for plain text connection
     * @param password the normal IRC password (<tt>Note</tt> this is not the
     *            password used for SASL authentication. This password may be
     *            null in case SASL authentication is required.)
     * @return Returns a server instance that matches the provided parameters.
     */
    private IRCServer createServer(final ClientConfig config,
        final String host, final int port, final boolean secureConnection,
        final String password)
    {
        final IRCServer server;
        if (secureConnection)
        {
            server =
                new SecureIRCServer(host, port, password,
                    getCustomSSLContext(host), config.getProxy(),
                    config.isResolveByProxy());
        }
        else
        {
            server =
                new IRCServer(host, port, password, false, config.getProxy(),
                    config.isResolveByProxy());
        }
        return server;
    }

    /**
     * Determine the correct plain IRC password for the provided IRC
     * configuration.
     *
     * @param password the user-specified password
     * @param config the IRC configuration, which includes possible SASL
     *            preferences
     * @return Returns the IRC plain password to use in the connection,
     *         determined by the provided IRC configuration.
     */
    private String determinePlainPassword(final String password,
        final ClientConfig config)
    {
        final String plainPass;
        if (config.isVersion3Allowed() && config.getSASL() != null) {
            plainPass = null;
        }
        else
        {
            plainPass = password;
        }
        return plainPass;
    }

    /**
     * Check to see if a certificate exception is the root cause for the
     * exception.
     *
     * @param e the exception
     * @return returns <tt>true</tt> if certificate exception is root cause, or
     *         <tt>false</tt> otherwise.
     */
    private boolean isCausedByCertificateException(final Exception e)
    {
        Throwable cause = e;
        while (cause != null)
        {
            if (cause instanceof CertificateException)
            {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    /**
     * Get the current connection instance.
     *
     * @return returns current connection instance or null if no connection is
     *         established.
     */
    public IrcConnection getConnection()
    {
        return this.session.get();
    }

    /**
     * Get the stack's persistent context instance.
     *
     * @return returns this stack's persistent context instance
     */
    PersistentContext getContext()
    {
        return this.context;
    }

    /**
     * Create a custom SSL context for this particular server.
     *
     * @param hostname host name of the host we are connecting to such that we
     *            can verify that the same host name is on the server
     *            certificate
     * @return returns a customized SSL context or <tt>null</tt> if one cannot
     *         be created.
     */
    private SSLContext getCustomSSLContext(final String hostname)
    {
        SSLContext context = null;
        try
        {
            CertificateService cs = IrcActivator.getCertificateService();
            X509TrustManager tm = cs.getTrustManager(hostname);
            context = cs.getSSLContext(tm);
        }
        catch (GeneralSecurityException e)
        {
            LOGGER.error("failed to create custom SSL context", e);
        }
        return context;
    }

    /**
     * Disconnect from the IRC server.
     */
    public void disconnect()
    {
        final IrcConnection connection;
        synchronized (this.session)
        {
            // synchronization needed to ensure that no other process (such as
            // connection attempt) is in progress

            // Set session to null first, such that we can identify that we
            // disconnect intentionally.
            connection = this.session.getAndSet(null);
            if (connection != null)
            {
                connection.disconnect();
            }
        }
        this.provider.setCurrentRegistrationState(
            RegistrationState.UNREGISTERED,
            RegistrationStateChangeEvent.REASON_USER_REQUEST);
    }

    /**
     * Dispose.
     */
    public void dispose()
    {
        disconnect();
    }

    /**
     * Listener for debugging purposes. If logging level is set high enough,
     * this listener is added to the irc-api client so it can show all IRC
     * messages as they are handled.
     *
     * <p>
     * This listener is <em>intentionally</em> not deleted upon disconnect
     * (ERROR or QUIT), for purpose of tracking any remaining activity that may
     * occur in case of a implementation issue.
     * </p>
     *
     * @author Danny van Heumen
     */
    private static final class DebugListener implements IMessageListener
    {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onMessage(final IMessage aMessage)
        {
            LOGGER.trace("(" + aMessage + ") " + aMessage.asRaw());
        }
    }

    /**
     * Container for storing server parameters.
     *
     * @author Danny van Heumen
     */
    private static final class ServerParameters
        implements IServerParameters
    {
        /**
         * Number of increments to try for alternative nick names.
         */
        private static final int NUM_INCREMENTS_FOR_ALTERNATIVES = 10;

        /**
         * Nick name.
         */
        private String nick;

        /**
         * Alternative nick names.
         */
        private List<String> alternativeNicks = new ArrayList<String>();

        /**
         * Real name.
         */
        private String real;

        /**
         * Ident.
         */
        private String ident;

        /**
         * IRC server.
         */
        private IRCServer server;

        /**
         * Construct ServerParameters instance.
         * @param nickName nick name
         * @param realName real name
         * @param ident ident
         * @param server IRC server instance
         */
        private ServerParameters(final String nickName, final String realName,
            final String ident, final IRCServer server)
        {
            this.nick = IdentityManager.checkNick(nickName, null);
            this.alternativeNicks.add(nickName + "_");
            this.alternativeNicks.add(nickName + "__");
            this.alternativeNicks.add(nickName + "___");
            this.alternativeNicks.add(nickName + "____");
            for (int i = 1; i < NUM_INCREMENTS_FOR_ALTERNATIVES; i++)
            {
                this.alternativeNicks.add(nickName + i);
            }
            this.real = realName;
            this.ident = ident;
            this.server = server;
        }

        /**
         * Get nick name.
         *
         * @return returns nick name
         */
        @Override
        public String getNickname()
        {
            return this.nick;
        }

        /**
         * Get alternative nick names.
         *
         * @return returns list of alternatives
         */
        @Override
        public List<String> getAlternativeNicknames()
        {
            return this.alternativeNicks;
        }

        /**
         * Get ident string.
         *
         * @return returns ident
         */
        @Override
        public String getIdent()
        {
            return this.ident;
        }

        /**
         * Get real name.
         *
         * @return returns real name
         */
        @Override
        public String getRealname()
        {
            return this.real;
        }

        /**
         * Get server.
         *
         * @return returns server instance
         */
        @Override
        public IRCServer getServer()
        {
            return this.server;
        }

        /**
         * Set server instance.
         *
         * @param server IRC server instance
         */
        public void setServer(final IRCServer server)
        {
            if (server == null)
            {
                throw new IllegalArgumentException("server cannot be null");
            }
            this.server = server;
        }
    }

    /**
     * Event for any kind of connection interruption, including normal QUIT
     * events.
     *
     * @param connection the connection that gets interrupted
     */
    @Override
    public void connectionInterrupted(final IrcConnection connection)
    {
        // Disconnected sessions are nulled before disconnect() is called. Hence
        // we can detect by IrcConnection instance contained in the session
        // whether or not the connection interruption is unintended.
        if (this.session.get() != connection)
        {
            // Interruption was intended: instance either nulled or a new
            // instance is already set.
            LOGGER.debug("Interrupted connection is not the current connection"
                + ", so assuming that connection interruption was intended.");
            return;
        }
        LOGGER.warn("IRC connection interrupted unexpectedly.");
        this.provider.setCurrentRegistrationState(
            RegistrationState.CONNECTION_FAILED,
            RegistrationStateChangeEvent.REASON_NOT_SPECIFIED);
    }

    /**
     * Persistent context that is used to survive (dis)connects.
     *
     * @author Danny van Heumen
     */
    static final class PersistentContext
    {
        /**
         * The protocol provider service instance.
         */
        final ProtocolProviderServiceIrcImpl provider;

        /**
         * The nick watch list as a SYNCHRONIZED sorted set.
         */
        final SortedSet<String> nickWatchList = Collections
            .synchronizedSortedSet(new TreeSet<String>());

        /**
         * Private constructor to ensure use only by IrcStack itself.
         *
         * @param provider the provider instance
         */
        private PersistentContext(final ProtocolProviderServiceIrcImpl provider)
        {
            if (provider == null)
            {
                throw new IllegalArgumentException("provider cannot be null");
            }
            this.provider = provider;
        }
    }
}
