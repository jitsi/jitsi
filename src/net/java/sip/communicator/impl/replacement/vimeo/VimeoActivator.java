/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.vimeo;

import java.util.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activator for the Vimeo source bundle.
 * 
 * @author Purvesh Sahoo
 */
public class VimeoActivator
    implements BundleActivator
{
    /**
     * The logger for this class.
     */
    private static final Logger logger = Logger.getLogger(VimeoActivator.class);

    /**
     * The vimeo service registration.
     */
    private ServiceRegistration vimeoServReg = null;

    /**
     * The source implementation reference.
     */
    private static ReplacementService vimeoSource = null;

    /**
     * Starts the Vimeo replacement source bundle
     * 
     * @param context the <tt>BundleContext</tt> as provided from the OSGi
     *            framework
     * @throws Exception if anything goes wrong
     */
    public void start(BundleContext context) throws Exception
    {
        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put(ReplacementService.SOURCE_NAME,
            ReplacementServiceVimeoImpl.VIMEO_CONFIG_LABEL);
        vimeoSource = new ReplacementServiceVimeoImpl();

        vimeoServReg =
            context.registerService(ReplacementService.class.getName(),
                vimeoSource, hashtable);

        logger.info("Vimeo source implementation [STARTED].");
    }

    /**
     * Unregisters the Vimeo replacement service.
     * 
     * @param context BundleContext
     * @throws Exception if anything goes wrong
     */
    public void stop(BundleContext context) throws Exception
    {
        vimeoServReg.unregister();
        logger.info("Vimeo source implementation [STOPPED].");
    }
}