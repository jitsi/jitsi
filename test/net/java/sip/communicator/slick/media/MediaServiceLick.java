/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.media;

import java.util.*;

import org.osgi.framework.*;
import junit.framework.*;
import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.util.*;


/**
 * This class launches the bundle which test the media bundle.
 * this bundle is a set of (j)unit tests. It should be launched by the
 * cruisecontrol module.
 *
 * @author Martin Andre
 */
public class MediaServiceLick
    extends TestSuite
    implements BundleActivator
{
    private Logger logger = Logger.getLogger(getClass().getName());

    protected static MediaService mediaService = null;
    protected static BundleContext bc = null;
    public static TestCase tcase = new TestCase(){};

    /**
     * Start the Media Service Implementation Compatibility Kit.
     *
     * @param bundleContext BundleContext
     * @throws Exception
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        MediaServiceLick.bc = bundleContext;
        setName("MediaServiceLick");
        Hashtable properties = new Hashtable();
        properties.put("service.pid", getName());

        addTestSuite(TestMediaService.class);
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
