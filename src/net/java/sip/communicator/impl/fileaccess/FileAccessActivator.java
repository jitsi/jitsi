/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.fileaccess;

import org.jitsi.service.fileaccess.*;
import org.jitsi.service.libjitsi.*;
import org.osgi.framework.*;

/**
 * Invoke "Service Binder" to parse the service XML and register all services.
 *
 * @author Alexander Pelov
 * @author Lyubomir Marinov
 */
public class FileAccessActivator
    implements BundleActivator
{

    /**
     * Initialize and start file service
     *
     * @param bundleContext the <tt>BundleContext</tt>
     * @throws Exception if initializing and starting file service fails
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        FileAccessService fileAccessService = LibJitsi.getFileAccessService();

        if (fileAccessService != null)
        {
            bundleContext.registerService(
                    FileAccessService.class.getName(),
                    fileAccessService,
                    null);
        }
    }

    /**
     * Stops this bundle.
     *
     * @param bundleContext the <tt>BundleContext</tt>
     * @throws Exception if the stop operation goes wrong
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
    }
}
