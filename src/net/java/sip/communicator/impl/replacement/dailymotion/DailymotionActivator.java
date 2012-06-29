/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.dailymotion;

import java.util.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activator for the Dailymotion source bundle.
 * @author Purvesh Sahoo
 */
public class DailymotionActivator
    implements BundleActivator
{
    /**
     * The currently valid bundle context.
     */
    private static final Logger logger =
        Logger.getLogger(DailymotionActivator.class);

    /**
     * The daily motion source service registration.
     */
    private ServiceRegistration dailymotionSourceServReg = null;

    /**
     * The source implementation reference.
     */
    private static ReplacementService dailymotionSource = null;

    /**
     * Starts the Dailymotion replacement source bundle
     * 
     * @param context the <tt>BundleContext</tt> as provided from the OSGi
     *            framework
     * @throws Exception if anything goes wrong
     */
    public void start(BundleContext context) throws Exception
    {
        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put(ReplacementService.SOURCE_NAME,
            ReplacementServiceDailymotionImpl.DAILYMOTION_CONFIG_LABEL);
        dailymotionSource = new ReplacementServiceDailymotionImpl();

        dailymotionSourceServReg =
            context.registerService(ReplacementService.class.getName(),
                dailymotionSource, hashtable);
        logger.info("Dailymotion source implementation [STARTED].");
    }

    /**
     * Unregisters the Dailymotion replacement service.
     *
     * @param context BundleContext
     * @throws Exception if anything goes wrong
     */
    public void stop(BundleContext context) throws Exception
    {
        dailymotionSourceServReg.unregister();
        logger.info("Dailymotion source implementation [STOPPED].");
    }
}
