/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
     * The system property which can be used to set custom timeout.
     */
    public static String SHUTDOWN_TIMEOUT_PROP =
        "org.jitsi.shutdown.SHUTDOWN_TIMEOUT";

    /**
     * The number of miliseconds that we wait before we force a shutdown.
     */
    public static final long SHUTDOWN_TIMEOUT_DEFAULT = 5000;//ms

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
        if (logger.isDebugEnabled())
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
            @Override
            public void run()
            {
                synchronized(this)
                {
                    try
                    {

                        long shutDownTimeout = SHUTDOWN_TIMEOUT_DEFAULT;

                        // check for custom value available through system
                        // property
                        try
                        {
                            String shutdownCustomValue =
                                System.getProperty(SHUTDOWN_TIMEOUT_PROP);

                            if(shutdownCustomValue != null
                                && shutdownCustomValue.length() > 0)
                            {
                                long custom =
                                    Long.valueOf(shutdownCustomValue);

                                // make sure custom is not 0, or it will
                                // wait forever
                                if(custom > 0)
                                    shutDownTimeout = custom;
                            }
                        }
                        catch(Throwable t){}

                        if (logger.isTraceEnabled())
                            logger.trace("Starting shutdown countdown of "
                                     + shutDownTimeout + "ms.");
                        wait(shutDownTimeout);
                        logger.error("Failed to gently shutdown. Forcing exit.");
                        Runtime.getRuntime().halt(SYSTEM_EXIT_CODE);
                    }catch (InterruptedException ex){
                        if (logger.isDebugEnabled())
                            logger.debug("Interrupted shutdown timer.");
                    }
                }
            }
        };
        if (logger.isTraceEnabled())
            logger.trace("Created the shutdown timer thread.");
        shutdownTimeoutThread.setDaemon(true);
        shutdownTimeoutThread.start();
    }
}
