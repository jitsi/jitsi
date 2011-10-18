/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.rss;

import java.util.*;
import junit.framework.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * RSS specific testing for an RSS Protocol provider Service implementation.
 * [...]
 * @author Mihai Balan
 */
public class RssProtocolProviderServiceLick
    extends     TestSuite
    implements     BundleActivator
{
    /**
     * Initializes and registers all tests that we'll run as a part of this
     * SLICK.
     *
     * @param bundleContext a currently valid bundle context.
     */
    public void start(BundleContext bundleContext)
    {
        Logger logger =
                Logger.getLogger(RssProtocolProviderServiceLick.class);
        logger.setLevelAll();
        logger.debug("***Start() called on RSS slick***");

        setName("RssProtocolProviderServiceLick");

        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("service.pid", getName());

        RssSlickFixture.bc = bundleContext;

        //test account installation
        addTestSuite(TestAccountInstallation.class);

        //test Protocol Provider Service implementation
        addTestSuite(TestProtocolProviderServiceRssImpl.class);

        //test account uninstallation
        addTest(TestAccountUninstallation.suite());
        addTestSuite(TestAccountUninstallationPersistence.class);

        bundleContext.registerService(getClass().getName(), this, properties);
    }

    /**
     * Prepares the slick for shutdown.
     *
     * @param context a currently valid bundle context.
     */
    public void stop(BundleContext context)
    {

    }

}
