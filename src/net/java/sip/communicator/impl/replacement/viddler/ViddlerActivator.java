/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.viddler;

import java.util.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activator for the Viddler source bundle.
 * @author Purvesh Sahoo
 */
public class ViddlerActivator
    implements BundleActivator
{

    /**
     * The <tt>Logger</tt> used by the <tt>ViddlerActivator</tt>
     * class.
     */
    private static final Logger logger =
        Logger.getLogger(ViddlerActivator.class);

    /**
     * The Viddler source service registration.
     */
    private ServiceRegistration viddlerServReg = null;

    /**
     * The source implementation reference.
     */
    private static ReplacementService viddlerSource = null;

    /**
     * Starts this bundle.
     *
     * @param context bundle context.
     * @throws Exception 
     */
    public void start(BundleContext context) throws Exception
    {
        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put(ReplacementService.SOURCE_NAME, "VIDDLER");
        viddlerSource = new ReplacementServiceViddlerImpl();

        viddlerServReg =
            context.registerService(ReplacementService.class.getName(),
                viddlerSource, hashtable);

        logger.info("Viddler source implementation [STARTED].");
    }

    /**
     * Stops bundle.
     *
     * @param bc context.
     * @throws Exception
     */
    public void stop(BundleContext bc) throws Exception
    {
        viddlerServReg.unregister();
        logger.info("Viddler source implementation [STOPPED].");
    }
}