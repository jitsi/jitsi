/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.contactlist;

import junit.framework.*;
import org.osgi.framework.*;
import net.java.sip.communicator.util.*;
import java.util.*;

/**
 * Performs testing of the MetaContactListService.
 * @author Emil Ivov
 */
public class MetaContactListServiceLick
    extends TestSuite
    implements BundleActivator
{
    private static final Logger logger =
        Logger.getLogger(MetaContactListServiceLick.class);

    /**
     * Register.
     */
    public void start(BundleContext context) throws Exception
    {
        MclSlickFixture.bundleContext = context;

        setName("MetaContactListServiceLick");
        Hashtable properties = new Hashtable();
        properties.put("service.pid", getName());

        logger.debug("Service  " + getClass().getName() + " [  STARTED ]");

        addTestSuite(TestMetaContactList.class);

        context.registerService(getClass().getName(), this, properties);
        logger.debug("Service  " + getClass().getName() + " [REGISTERED]");
    }

    /**
     */
    public void stop(BundleContext context) throws Exception
    {

    }

}
