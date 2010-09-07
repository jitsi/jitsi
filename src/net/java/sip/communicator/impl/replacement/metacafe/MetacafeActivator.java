/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.metacafe;

import java.util.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activator for the Metacafe source bundle.
 * 
 * @author Purvesh Sahoo
 */
public class MetacafeActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>MetacafeActivator</tt> class.
     */
    private static final Logger logger =
        Logger.getLogger(MetacafeActivator.class);

    /**
     * The metacafe service registration.
     */
    private ServiceRegistration metacafeServReg = null;

    /**
     * The source implementation reference.
     */
    private static ReplacementService metacafeSource = null;

    /**
     * Starts the Metacafe replacement source bundle
     *
     * @param context the <tt>BundleContext</tt> as provided from the OSGi
     *            framework
     * @throws Exception if anything goes wrong
     */
    public void start(BundleContext context) throws Exception
    {
        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put(ReplacementService.SOURCE_NAME, "METACAFE");
        metacafeSource = new ReplacementServiceMetacafeImpl();

        metacafeServReg =
            context.registerService(ReplacementService.class.getName(),
                metacafeSource, hashtable);

        logger.info("Metacafe source implementation [STARTED].");
    }

    /**
     * Unregisters the Metacafe replacement service.
     *
     * @param context BundleContext
     * @throws Exception if anything goes wrong
     */
    public void stop(BundleContext context) throws Exception
    {
        metacafeServReg.unregister();
        logger.info("Metacafe source implementation [STOPPED].");
    }
}