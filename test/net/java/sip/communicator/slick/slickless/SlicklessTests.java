/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.slickless;

import java.util.*;

import org.osgi.framework.*;
import junit.framework.*;
import net.java.sip.communicator.slick.slickless.util.*;
import net.java.sip.communicator.slick.slickless.util.xml.*;
import net.java.sip.communicator.util.*;


/**
 * Runs all unit tests that do not belong to any SLICK.
 *
 * @author Emil Ivov
 */
public class SlicklessTests
    extends TestSuite
    implements BundleActivator
{
    private Logger logger = Logger.getLogger(getClass().getName());

    protected static BundleContext bc = null;

    /**
     * Start the Configuration Sevice Implementation Compatibility Kit.
     *
     * @param bundleContext BundleContext
     * @throws Exception
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        this.bc = bundleContext;
        setName("SlicklessTests");
        Hashtable properties = new Hashtable();
        properties.put("service.pid", getName());

        addTestSuite(TestXMLUtils.class);
        addTestSuite(TestBase64.class);
        addTestSuite(TestFailSafeTransaction.class);
        bundleContext.registerService(getClass().getName(), this, properties);

        logger.debug("Successfully registered " + getClass().getName());
    }

    /**
     * stop
     *
     * @param bundlecontext BundleContext
     * @throws Exception
     */
    public void stop(BundleContext bundlecontext) throws Exception
    {
    }



}
