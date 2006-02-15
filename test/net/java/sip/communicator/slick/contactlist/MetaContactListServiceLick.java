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
 *
 * @author Emil Ivov
 */
public class MetaContactListServiceLick
    extends TestSuite
    implements BundleActivator
{
    private static final Logger logger =
        Logger.getLogger(MetaContactListServiceLick.class);
    /**
     * The bundle context that we get upon activation.
     */
    protected static BundleContext bundleContext = null;

    /**
     */
    public void start(BundleContext context) throws Exception
    {
        this.bundleContext = context;

        setName("MetaContactListServiceLick");
        Hashtable properties = new Hashtable();
        properties.put("service.pid", getName());

        addTestSuite(TestMetaContactList.class);


        bundleContext.registerService(getClass().getName(), this, properties);
        logger.debug("Service  " + getClass().getName() + " [REGISTERED]");
    }

    /**
     */
    public void stop(BundleContext context) throws Exception
    {

    }

}
