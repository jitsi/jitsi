/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.popupmessagehandler;

import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;

import net.java.sip.communicator.util.*;

/**
 *
 * @author Symphorien Wanko
 */
public class PopupMessageHandlerSLick extends TestSuite implements BundleActivator
{
    /** Logger for this class */
    private static Logger logger =
            Logger.getLogger(PopupMessageHandlerSLick.class);

    /** our bundle context */
    protected static BundleContext bundleContext = null;

    /** implements BundleActivator.start() */
    public void start(BundleContext bc) throws Exception
    {
        logger.info("starting popup message test ");

        bundleContext = bc;

        setName("PopupMessageHandlerSLick");

        Hashtable<String, String> properties = new Hashtable<String, String>();

        properties.put("service.pid", getName());
        
        addTest(TestPopupMessageHandler.suite());

        bundleContext.registerService(getClass().getName(), this, properties);
    }

    /** implements BundleActivator.stop() */
    public void stop(BundleContext bc) throws Exception
    {}
    
}
