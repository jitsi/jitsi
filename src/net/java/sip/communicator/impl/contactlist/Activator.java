/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.contactlist;

import org.osgi.framework.*;
import java.util.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.util.*;

/**
 *
 * @author Emil Ivov
 */
public class Activator
    implements BundleActivator
{
    private static final Logger logger =
        Logger.getLogger(Activator.class);

    ServiceRegistration mclServiceRegistration = null;
    /**
     * Called when this bundle is started.
     *
     * @param context The execution context of the bundle being started.
     * @throws Exception If
     */
    public void start(BundleContext context) throws Exception
    {
        logger.debug("Service Impl: " + getClass().getName() + " [  STARTED ]");
        Hashtable hashtable = new Hashtable();

        MetaContactListServiceImpl mclServiceImpl =
                                        new MetaContactListServiceImpl();

        //reg the icq account man.
        mclServiceRegistration =  context.registerService(
                    MetaContactListService.class.getName(),
                    mclServiceImpl,
                    hashtable);

        logger.debug("Service Impl: " + getClass().getName() + " [REGISTERED]");

    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext context) throws Exception
    {
    }
}
