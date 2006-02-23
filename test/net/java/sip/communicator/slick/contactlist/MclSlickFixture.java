package net.java.sip.communicator.slick.contactlist;

import org.osgi.framework.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.slick.contactlist.mockprovider.*;

/**
 * Fields, commonly used by the MetaContactListSlick.
 *
 * @author Emil Ivov
 */
public class MclSlickFixture
    extends junit.framework.TestCase
{
    /**
     * The bundle context that we received when the slick was activated.
     */
    public static BundleContext bundleContext = null;

    /**
     * A reference to the meta contact list service currently available on the
     * OSGI bus.
     */
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
        //find a reference to the meta contaact list service.
        ServiceReference ref = bundleContext.getServiceReference(
            MetaContactListService.class.getName());
        metaClService
            = (MetaContactListService)bundleContext.getService(ref);

    }

    /**
     *
     */
    public void tearDown()
    {
    }

}
