package net.java.sip.communicator.slick.contactlist;

import org.osgi.framework.*;
import net.java.sip.communicator.service.contactlist.*;

public class MclSlickFixture
    extends junit.framework.TestCase
{
    public static BundleContext bundleContext = null;
    public MetaContactListService metaClService = null;

    public MclSlickFixture(Object obj)
    {
    }

    /**
     * Find a reference of the meta contact list service and set the
     * corresponding field.
     */
    public void setUp()
    {
        ServiceReference ref = bundleContext.getServiceReference(
            MetaContactListService.class.getName());
        metaClService
            = (MetaContactListService)bundleContext.getService(ref);

    }

    public void tearDown()
    {
    }

}
