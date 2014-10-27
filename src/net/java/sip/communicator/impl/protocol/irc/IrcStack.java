/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import javax.net.ssl.*;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import com.ircclouds.irc.api.*;
import com.ircclouds.irc.api.domain.*;
import com.ircclouds.irc.api.domain.messages.interfaces.*;
import com.ircclouds.irc.api.listeners.*;

/**
 * An implementation of IRC using the irc-api library.
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
     * @throws Exception throws exceptions
     */
    public void connect(final String host, final int port,
        final String password, final boolean secureConnection,
        final boolean autoNickChange) throws Exception
    {
        final IRCServer server;
        if (secureConnection)
        {
            server =
                new SecureIRCServer(host, port, password,
                    getCustomSSLContext(host));
        }
        else
        {
            server = new IRCServer(host, port, password, false);
        }

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
                this.session.set(new IrcConnection(this.provider, this.params,
                    new SynchronizedIRCApi(irc), this));

                this.provider
                    .setCurrentRegistrationState(RegistrationState.REGISTERED);
            }
        }
        catch (IOException e)
        {
            // SSL exceptions will be caught here too.
            this.provider
                .setCurrentRegistrationState(RegistrationState
                    .CONNECTION_FAILED);
            throw e;
        }
        catch (InterruptedException e)
        {
            this.provider
                .setCurrentRegistrationState(RegistrationState.UNREGISTERED);
            throw e;
        }
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
        this.provider
            .setCurrentRegistrationState(RegistrationState.UNREGISTERED);
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
        this.provider
            .setCurrentRegistrationState(RegistrationState.CONNECTION_FAILED);
    }
}
