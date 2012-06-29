package net.java.sip.communicator.slick.fileaccess;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.fileaccess.*;
import org.osgi.framework.*;

/**
 * This class launches the bundle which tests the fileaccess bundle. This bundle
 * is a set of (j)unit tests. It should be launched by the cruisecontrol module.
 *
 * @author Alexander Pelov
 */
public class FileAccessServiceLick extends TestSuite implements BundleActivator {
    private Logger logger = Logger.getLogger(getClass().getName());

    protected static FileAccessService fileAccessService = null;

    protected static BundleContext bc = null;

    public static TestCase tcase = new TestCase() {
    };

    /**
     * Start the File Access Sevice Implementation Compatibility Kit.
     *
     * @param bundleContext
     *            BundleContext
     * @throws Exception
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        FileAccessServiceLick.bc = bundleContext;
        setName("FileAccessServiceLick");
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("service.pid", getName());

        addTestSuite(TestFileAccessService.class);
        addTestSuite(TestFailSafeTransaction.class);
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
