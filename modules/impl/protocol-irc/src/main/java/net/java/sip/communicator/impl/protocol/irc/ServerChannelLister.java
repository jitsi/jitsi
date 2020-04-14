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

import net.java.sip.communicator.util.*;

import com.ircclouds.irc.api.*;
import com.ircclouds.irc.api.domain.messages.*;
import com.ircclouds.irc.api.state.*;

/**
 * Server channel lister for retrieving initial list and managing channel cache
 * for its period as well as cleaning up cache after the cache has expired.
 *
 * @author Danny van Heumen
 */
public class ServerChannelLister
{
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger
        .getLogger(ServerChannelLister.class);

    /**
     * Clean-up delay. The clean up task clears any remaining chat room list
     * cache. Since there's no pointing in timing it exactly, delay the clean up
     * until after expiration.
     */
    private static final long CACHE_CLEAN_UP_DELAY = 1000L;

    /**
     * Ratio of milliseconds to nanoseconds for conversions.
     */
    private static final long RATIO_MILLISECONDS_TO_NANOSECONDS = 1000000L;

    /**
     * Expiration time for chat room list cache.
     */
    private static final long CHAT_ROOM_LIST_CACHE_EXPIRATION = 60000000000L;

    /**
     * IRCApi instance.
     *
     * Instance must be thread-safe!
     */
    private final IRCApi irc;

    /**
     * IRCApi connection state.
     */
    private final IIRCState connectionState;

    /**
     * The cached channel list.
     *
     * Contained inside a simple container object in order to lock the container
     * while accessing the contents.
     */
    private final Container<List<String>> channellist =
        new Container<List<String>>(null);

    /**
     * Constructor.
     *
     * @param irc thread-safe irc api instance
     * @param connectionState irc connection state
     */
    public ServerChannelLister(final IRCApi irc,
        final IIRCState connectionState)
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
    }

    /**
     * Get a list of channels available on the IRC server.
     *
     * @return List of available channels.
     */
    public List<String> getList()
    {
        LOGGER.trace("Start retrieve server chat room list.");
        if (!connectionState.isConnected())
        {
            throw new IllegalStateException("Not connected to an IRC server.");
        }

        synchronized (this.channellist)
        {
            List<String> list =
                this.channellist.get(CHAT_ROOM_LIST_CACHE_EXPIRATION);
            if (list == null)
            {
                LOGGER
                    .trace("Chat room list null or outdated. Start retrieving "
                        + "new chat room list.");
                Result<List<String>, Exception> listSignal =
                    new Result<List<String>, Exception>(
                        new LinkedList<String>());
                synchronized (listSignal)
                {
                    try
                    {
                        this.irc
                            .addListener(new ChannelListListener(listSignal));
                        this.irc.rawMessage("LIST");
                        while (!listSignal.isDone())
                        {
                            LOGGER.trace("Waiting for list ...");
                            listSignal.wait();
                        }
                        LOGGER.trace("Done waiting for list.");
                    }
                    catch (InterruptedException e)
                    {
                        LOGGER.warn("INTERRUPTED while waiting for list.", e);
                    }
                }
                list = listSignal.getValue();
                this.channellist.set(list);
                LOGGER.trace("Finished retrieving server chat room list.");

                // Set timer to clean up the cache after use, since otherwise
                // this data could stay in memory for a long time.
                createCleanUpJob(this.channellist);
            }
            else
            {
                LOGGER.trace("Using cached list of server chat rooms.");
            }

            if (LOGGER.isDebugEnabled())
            {
                // Report on number of channels to give an impression of the
                // kind of result that will be returned.
                LOGGER.debug("Server channel list contains " + list.size()
                    + " channels.");
            }

            return Collections.unmodifiableList(list);
        }
    }

    /**
     * Create a clean up job that checks the container after the cache has
     * expired. If the container is still populated, then remove it. This clean
     * up makes sure that there are no references left to an otherwise useless
     * outdated list of channels.
     *
     * @param channellist the container carrying the list of channel names
     */
    private static void createCleanUpJob(
        final Container<List<String>> channellist)
    {
        final Timer cleanUpJob = new Timer();
        final long timestamp = channellist.getTimestamp();
        cleanUpJob.schedule(new ChannelListCacheCleanUpTask(channellist,
            timestamp), CHAT_ROOM_LIST_CACHE_EXPIRATION
            / RATIO_MILLISECONDS_TO_NANOSECONDS + CACHE_CLEAN_UP_DELAY);
    }

    /**
     * Task for cleaning up old channel list caches.
     *
     * @author Danny van Heumen
     */
    private static final class ChannelListCacheCleanUpTask
        extends TimerTask
    {
        /**
         * Expected timestamp on which the list cache was created. It is used as
         * an indicator to see whether the cache has been refreshed in the mean
         * time.
         */
        private final long timestamp;

        /**
         * Container holding the channel list cache.
         */
        private final Container<List<String>> container;

        /**
         * Construct new clean up job definition.
         *
         * @param listContainer container that holds the channel list cache
         * @param timestamp expected timestamp of list cache creation
         */
        private ChannelListCacheCleanUpTask(
            final Container<List<String>> listContainer, final long timestamp)
        {
            if (listContainer == null)
            {
                throw new IllegalArgumentException(
                    "listContainer cannot be null");
            }
            this.container = listContainer;
            this.timestamp = timestamp;
        }

        /**
         * Remove the list reference from the container. But only if the
         * timestamp matches. This makes sure that only one clean up job will
         * clean up a list.
         */
        @Override
        public void run()
        {
            synchronized (this.container)
            {
                // Only clean up old cache if this is the dedicated task for it.
                // If the timestamp has changed, another job is responsible for
                // the clean up.
                if (this.container.getTimestamp() != this.timestamp)
                {
                    LOGGER.trace("Not cleaning up channel list cache. The "
                        + "timestamp does not match.");
                    return;
                }
                this.container.set(null);
            }
            // We cannot clear the list itself, since the contents might still
            // be in use by the UI, inside the immutable wrapper.
            LOGGER.debug("Old channel list cache has been cleared.");
        }
    }

    /**
     * Special listener that processes LIST replies and signals once the list is
     * completely filled.
     */
    private final class ChannelListListener
        extends AbstractIrcMessageListener
    {
        /**
         * Start of an IRC server channel listing reply.
         */
        private static final int RPL_LISTSTART = 321;

        /**
         * Continuation of an IRC server channel listing reply.
         */
        private static final int RPL_LIST = 322;

        /**
         * End of an IRC server channel listing reply.
         */
        private static final int RPL_LISTEND = 323;

        /**
         * Reference to the provided list instance.
         */
        private final Result<List<String>, Exception> signal;

        /**
         * Constructor for channel list listener.
         *
         * @param api irc-api library instance
         * @param signal signal for sync signaling
         */
        private ChannelListListener(
            final Result<List<String>, Exception> signal)
        {
            super(ServerChannelLister.this.irc,
                ServerChannelLister.this.connectionState);
            this.signal = signal;
        }

        /**
         * Act on LIST messages:
         * <pre>
         * - 321 RPL_LISTSTART,
         * - 322 RPL_LIST,
         * - 323 RPL_LISTEND
         * </pre>
         *
         * Clears the list upon starting. All received channels are added to the
         * list. Upon receiving RPL_LISTEND finalize the list and signal the
         * waiting thread that it can continue processing the list.
         *
         * @param msg The numeric server message.
         */
        @Override
        public void onServerNumericMessage(final ServerNumericMessage msg)
        {
            if (this.signal.isDone())
            {
                return;
            }

            switch (msg.getNumericCode())
            {
            case RPL_LISTSTART:
                synchronized (this.signal)
                {
                    this.signal.getValue().clear();
                }
                break;
            case RPL_LIST:
                String channel = parse(msg.getText());
                if (channel != null)
                {
                    synchronized (this.signal)
                    {
                        this.signal.getValue().add(channel);
                    }
                }
                break;
            case RPL_LISTEND:
                synchronized (this.signal)
                {
                    // Done collecting channels. Remove listener and then we're
                    // done.
                    ServerChannelLister.this.irc.deleteListener(this);
                    this.signal.setDone();
                    this.signal.notifyAll();
                }
                break;
            // TODO Add support for REPLY 416: LIST :output too large, truncated
            default:
                break;
            }
        }

        /**
         * Parse an IRC server response RPL_LIST. Extract the channel name.
         *
         * @param text raw server response
         * @return returns the channel name
         */
        private String parse(final String text)
        {
            int endOfChannelName = text.indexOf(' ');
            if (endOfChannelName == -1)
            {
                return null;
            }
            // Create a new string to make sure that the original (larger)
            // strings can be GC'ed.
            return new String(text.substring(0, endOfChannelName));
        }
    }

    /**
     * Simplest possible container that we can use for locking while we're
     * checking/modifying the contents.
     *
     * @param <T> The type of instance to store in the container
     */
    private static final class Container<T>
    {
        /**
         * The stored instance. (Can be null)
         */
        private T instance;

        /**
         * Time of stored instance.
         */
        private long time;

        /**
         * Constructor that immediately sets the instance.
         *
         * @param instance the instance to set
         */
        private Container(final T instance)
        {
            this.instance = instance;
            this.time = System.nanoTime();
        }

        /**
         * Conditionally get the stored instance. Get the instance when time
         * difference is within specified bound. Otherwise return null.
         *
         * @param bound maximum time difference that is allowed.
         * @return returns set value if within bounds, or null otherwise
         */
        public T get(final long bound)
        {
            if (System.nanoTime() - this.time > bound)
            {
                return null;
            }
            return this.instance;
        }

        /**
         * Set an instance.
         *
         * @param instance the instance
         */
        public void set(final T instance)
        {
            this.instance = instance;
            this.time = System.nanoTime();
        }

        /**
         * Get the timestamp from when the instance was set.
         *
         * @return returns the timestamp
         */
        public long getTimestamp()
        {
            return this.time;
        }
    }
}
