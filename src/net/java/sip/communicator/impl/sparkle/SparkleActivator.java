/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.sparkle;

import org.osgi.framework.*;
import net.java.sip.communicator.util.*;

/**
 * Activates the Sparkle Framework
 *
 * @author Romain Kuntz
 */
public class SparkleActivator
    implements BundleActivator
{
    /**
     * Our class logger.
     */
    private static Logger logger = Logger.getLogger(SparkleActivator.class);

    /**
     * Native method declaration
     *
     * @param pathToSparkleFramework the path to the Sparkle framerok
     * @param updateAtStartup specifies whether Sparkle should be checking for
     * updates on startup.
     * @param checkInterval specifies an interval for the update checks.
     */
    public native static void initSparkle(String pathToSparkleFramework,
                                          boolean updateAtStartup,
                                          int checkInterval);

    /**
     * Whether updates are checked at startup
     */
    private boolean updateAtStartup = true;

    /**
     * Check interval period, in seconds
     */
    private int checkInterval = 86400;  // 1 day

    /**
     * Internal flag that we use in order to determine whether the native
     * Sparkle libs have already been loaded.
     */
    private static boolean sparkleLibLoaded = false;

    /**
     * Initialize and start Sparkle
     *
     * @param bundleContext BundleContext
     * @throws Exception
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        /**
         * Dynamically loads JNI object. Will fail if non-MacOSX
         * or when libinit_sparkle.dylib is outside of the LD_LIBRARY_PATH
         */
        try
        {
            if ( ! SparkleActivator.sparkleLibLoaded)
            {
                System.loadLibrary("sparkle_init");
                SparkleActivator.sparkleLibLoaded = true;
            }
        }
        catch(Throwable t)
        {
            logger.warn("Couldn't load sparkle library.");
            logger.debug("Couldn't load sparkle library.", t);

            return;
        }

        // TODO: better way to get the Sparkle Framework path?
        initSparkle(System.getProperty("user.dir")
                    + "/../../Frameworks/Sparkle.framework",
                    updateAtStartup, checkInterval);
        logger.info("Sparkle Plugin ...[Started]");
    }

    /**
     * Stops this bundle
     *
     * @param bundleContext a reference to the currently valid
     * <tt>BundleContext</tt>
     *
     * @throws Exception if anything goes wrong (original, right ;) )
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        logger.info("Sparkle Plugin ...[Stopped]");
    }
}
