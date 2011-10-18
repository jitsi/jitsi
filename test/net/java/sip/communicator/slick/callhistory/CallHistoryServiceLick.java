/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.callhistory;

import java.util.*;

import org.osgi.framework.*;
import junit.framework.*;
import net.java.sip.communicator.util.*;

/**
 *
 * @author Damian Minkov
 */
public class CallHistoryServiceLick extends TestSuite implements BundleActivator {
    private static Logger logger = Logger.getLogger(CallHistoryServiceLick.class);

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
        CallHistoryServiceLick.bc = bundleContext;

        setName("CallHistoryServiceLick");
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("service.pid", getName());

        addTest(TestCallHistoryService.suite());
        bundleContext.registerService(getClass().getName(), this, properties);

        logger.debug("Successfully registered " + getClass().getName());
    }

    /**
     * stop
     *
     * @param bundlecontext BundleContext
     * @throws Exception
     */
    public void stop(BundleContext bundlecontext)
        throws Exception
    {
    }
}
