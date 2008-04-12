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
    private static Logger logger = Logger.getLogger(SparkleActivator.class);
    
    // Native method declaration
    public native static void initSparkle(String pathToSparkleFramework);

   /**
     * Dynamically loads JNI object. Will fail if non-MacOSX 
     * or when libinit_sparkle.dylib is outside of the LD_LIBRARY_PATH
     */    
    static {
        System.loadLibrary("sparkle_init");
    }

    /**
     * Initialize and start Sparkle
     *
     * @param bundleContext BundleContext
     * @throws Exception
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        // TODO: better way to get the Sparkle Framework path?
        initSparkle(System.getProperty("user.dir") + "/../../Frameworks/Sparkle.framework");
        logger.info("Sparkle Plugin ...[Started]");
    }

    public void stop(BundleContext bundleContext) throws Exception
    {
        logger.info("Sparkle Plugin ...[Stopped]");
    }
}
