/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.youtube;

import java.util.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activator for the Youtube source bundle.
 * 
 * @author Purvesh Sahoo
 */
public class YoutubeActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>YoutubeActivator</tt> class.
     */
    private static final Logger logger =
        Logger.getLogger(YoutubeActivator.class);

    /**
     * The youtube source service registration.
     */
    private ServiceRegistration youtubeSourceServReg = null;

    /**
     * The source implementation reference.
     */
    private static ReplacementService youtubeSource = null;

    /**
     * Starts the Youtube replacement source bundle
     *
     * @param context the <tt>BundleContext</tt> as provided from the OSGi
     *            framework
     * @throws Exception if anything goes wrong
     */
    public void start(BundleContext context) throws Exception
    {
        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put(ReplacementService.SOURCE_NAME,
            ReplacementServiceYoutubeImpl.YOUTUBE_CONFIG_LABEL);
        youtubeSource = new ReplacementServiceYoutubeImpl();

        youtubeSourceServReg =
            context.registerService(ReplacementService.class.getName(),
                youtubeSource, hashtable);
        logger.info("Youtube source implementation [STARTED].");
    }

    /**
     * Unregisters the Youtube replacement service.
     * 
     * @param context BundleContext
     * @throws Exception if anything goes wrong
     */
    public void stop(BundleContext context) throws Exception
    {
        youtubeSourceServReg.unregister();
        logger.info("Youtube source implementation [STOPPED].");
    }
}