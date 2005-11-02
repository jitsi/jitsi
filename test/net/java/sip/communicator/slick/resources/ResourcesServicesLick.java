package net.java.sip.communicator.slick.resources;

import org.osgi.framework.*;
import junit.framework.*;
import net.java.sip.communicator.service.resources.FileAccessService;

import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * This class launches the bundle of which test the resources bundle.
 * this bundle is a set of (j)unit tests. It should be launched by the
 * cruisecontrol module.
 *
 * @author Alexander Pelov
 */
public class ResourcesServicesLick
    extends TestSuite
    implements BundleActivator
{
    private Logger logger = Logger.getLogger(getClass().getName());

    protected static FileAccessService fileAccessService = null;
    protected static BundleContext bc = null;
    public static TestCase tcase = new TestCase(){};

    /**
     * Start the Resources Sevice Implementation Compatibility Kit.
     *
     * @param bundleContext BundleContext
     * @throws Exception
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        ResourcesServicesLick.bc = bundleContext;
        setName("ResourcesServicesLick");
        Hashtable properties = new Hashtable();
        properties.put("service.pid", getName());

        addTestSuite(TestFileAccessService.class);
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
