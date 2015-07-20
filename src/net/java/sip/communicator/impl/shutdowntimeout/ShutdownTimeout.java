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
package net.java.sip.communicator.impl.shutdowntimeout;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.launchutils.*;

import org.osgi.framework.*;

/**
 * In order to shut down Jitsi, we kill the Felix system bundle. However, this
 * sometimes doesn't work for reason of running non-daemon threads (such as the
 * Java Sound event dispatcher). This results in having instances of Jitsi
 * running in the background.
 *
 * We use this shutdown timeout bundle in order to fix this problem. When our
 * stop method is called, we assume that a shutdown is executed and start a 15
 * seconds daemon thread. If the application is still running once these 15
 * seconds expire, we System.exit() the application.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public class ShutdownTimeout
    implements BundleActivator
{
    private static final Logger logger
        = Logger.getLogger(ShutdownTimeout.class);

    /**
     * The system property which can be used to set custom timeout.
     */
    private static final String SHUTDOWN_TIMEOUT_PNAME
        = "org.jitsi.shutdown.SHUTDOWN_TIMEOUT";

    /**
     * The number of milliseconds that we wait before we force a shutdown.
     */
    private static final long SHUTDOWN_TIMEOUT_DEFAULT = 5000;//ms

    /**
     * The code that we exit with if the application is not down in 15 seconds.
     */
    private static final int SYSTEM_EXIT_CODE = 500;

    /**
     * Runs in a daemon thread started by {@link #stop(BundleContext)} and
     * forcibly terminates the currently running Java virtual machine after
     * {@link #SHUTDOWN_TIMEOUT_DEFAULT} (or {@link #SHUTDOWN_TIMEOUT_PNAME})
     * milliseconds.
     */
    private static void runInShutdownTimeoutThread()
    {
        long shutdownTimeout = SHUTDOWN_TIMEOUT_DEFAULT;

        // Check for a custom value specified through a System property.
        try
        {
            String s = System.getProperty(SHUTDOWN_TIMEOUT_PNAME);

            if ((s != null) && (s.length() > 0))
            {
                long l = Long.valueOf(s);

                // Make sure custom is not 0 to prevent waiting forever.
                if (l > 0)
                    shutdownTimeout = l;
            }
        }
        catch(Throwable t)
        {
        }

        if (logger.isTraceEnabled())
        {
            logger.trace(
                    "Starting shutdown countdown of " + shutdownTimeout
                        + "ms.");
        }
        try
        {
            Thread.sleep(shutdownTimeout);
        }
        catch (InterruptedException ex)
        {
            if (logger.isDebugEnabled())
                logger.debug("Interrupted shutdown timer.");
            return;
        }

        /*
         * We are going to forcibly terminate the currently running Java virtual
         * machine so it will not run DeleteOnExitHook. But the currently
         * running Java virtual machine is still going to terminate because of
         * our intention, not because it has crashed. Make sure that we delete
         * any files registered for deletion when Runtime.halt(int) is to be
         * invoked.
         */
        try
        {
            DeleteOnHaltHook.runHooks();
        }
        catch (Throwable t)
        {
            logger.warn("Failed to delete files on halt.", t);
        }

        logger.error("Failed to gently shutdown. Forcing exit.");
        Runtime.getRuntime().halt(SYSTEM_EXIT_CODE);
    }

    /**
     * Dummy impl of the bundle activator start method.
     *
     * @param context unused
     * @throws Exception if this method throws an exception (which won't happen)
     */
    public void start(BundleContext context)
        throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug("Starting the ShutdownTimeout service.");
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is still
     * marked as stopped, and the Framework will remove the bundle's listeners,
     * unregister all services registered by the bundle, and release all
     * services used by the bundle.
     */
    public void stop(BundleContext context)
        throws Exception
    {
        Thread shutdownTimeoutThread
            = new Thread()
            {
                @Override
                public void run()
                {
                    runInShutdownTimeoutThread();
                }
            };

        shutdownTimeoutThread.setDaemon(true);
        shutdownTimeoutThread.setName(ShutdownTimeout.class.getName());
        shutdownTimeoutThread.start();
    }
}
