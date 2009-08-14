/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.shutdowntimeout;

import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * In order to shut down SIP Communicator we kill the Felix system bundle.
 * However, this sometimes doesn't work for reason of running non-daemon
 * threads (such as the javasound event dispatcher). This results in having
 * instances of SIP Communicator running in the background.
 *
 * We use this shutdown timout bundle in order to fix this problem. When our
 * stop method is called, we assume that a shutdown is executed and start a 15
 * seconds daemon thread. If the application is still running once these 15
 * seconds expire, we System.exit() the application.
 *
 * @author Emil Ivov
 */
public class ShutdownTimeout
    implements BundleActivator
{
    private static final Logger logger
        = Logger.getLogger(ShutdownTimeout.class);

    /**
     * The number of miliseconds that we wait before we force a shutdown.
     */
    public static final long SHUTDOWN_TIMEOUT = 15000;//ms

    /**
     * The code that we exit with if the application is not down in 15 seconds.
     */
    public static final int SYSTEM_EXIT_CODE = 500;

    /**
     * Dummy impl of the bundle activator start method.
     *
     * @param context unused
     * @throws Exception If this method throws an exception
     * (which won't happen).
     */
    public void start(BundleContext context)
        throws Exception
    {
        logger.debug("Starting the ShutdownTimeout service.");
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext context)
        throws Exception
    {
        Thread shutdownTimeoutThread = new Thread()
        {
            public void run()
            {
                synchronized(this)
                {
                    try{
                        logger.trace("Starting shutdown countdown of "
                                     + SHUTDOWN_TIMEOUT + "ms.");
                        wait(SHUTDOWN_TIMEOUT);
                        logger.error("Failed to gently shutdown. Forcing exit.");
                        System.exit(500);
                    }catch (InterruptedException ex){
                        logger.debug("Interrupted shutdown timer.");
                    }
                }
            }
        };
        logger.trace("Created the shutdown timer thread.");
        shutdownTimeoutThread.setDaemon(true);
        shutdownTimeoutThread.start();
    }
}
