/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.history;

import java.util.*;

import org.osgi.framework.*;
import junit.framework.*;
import net.java.sip.communicator.util.*;

/**
 * This class launches the bundle of which test the history bundle. this bundle
 * is a set of (j)unit tests. It should be launched by the cruisecontrol module.
 *
 * @author Alexander Pelov
 */
public class HistoryServiceLick extends TestSuite implements BundleActivator {
    private static Logger logger = Logger.getLogger(HistoryServiceLick.class);

    protected static BundleContext bc = null;

    /**
     * Start the History Sevice Implementation Compatibility Kit.
     *
     * @param bundleContext
     *            BundleContext
     * @throws Exception
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        HistoryServiceLick.bc = bundleContext;

        setName("HistoryServiceLick");
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("service.pid", getName());

        addTest(TestHistoryService.suite());
        bundleContext.registerService(getClass().getName(), this, properties);

        logger.debug("Successfully registered " + getClass().getName());
    }

    /**
     * stop
     *
     * @param bundlecontext
     *            BundleContext
     * @throws Exception
     */
    public void stop(BundleContext bundlecontext)
        throws Exception
    {
    }

}
