/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.directimage;

import java.util.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activator for the direct image links source bundle.
 * @author Purvesh Sahoo
 */
public class DirectImageActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>DirectImageActivator</tt>
     * class.
     */
    private static final Logger logger =
        Logger.getLogger(DirectImageActivator.class);

    /**
     * The direct image source service registration.
     */
    private ServiceRegistration directImageSourceServReg = null;

    /**
     * The source implementation reference.
     */
    private static ReplacementService directImageSource = null;

    /**
     * Starts the Direct image links replacement source bundle
     *
     * @param context the <tt>BundleContext</tt> as provided from the OSGi
     * framework
     * @throws Exception if anything goes wrong
     */
    public void start(BundleContext context) throws Exception
    {
        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put(ReplacementService.SOURCE_NAME, "DIRECTIMAGE");
        directImageSource = new ReplacementServiceDirectImageImpl();

        directImageSourceServReg =
            context.registerService(ReplacementService.class.getName(),
                directImageSource, hashtable);
        logger.info("Direct Image Link source implementation [STARTED].");
    }

    /**
     * Unregisters the Direct image links replacement service.
     *
     * @param context BundleContext
     * @throws Exception if anything goes wrong
     */
    public void stop(BundleContext context) throws Exception
    {
        directImageSourceServReg.unregister();
        logger.info("Direct Image Link source implementation [STOPPED].");
    }
}