/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.msofficecomm;

import org.jitsi.util.*;
import org.osgi.framework.*;

/**
 * Implements {@link BundleActivator} for the <tt>msofficecomm</tt> bundle.
 *
 * @author Lyubomir Marinov
 */
public class MsOfficeCommActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>MsOfficeCommActivator</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MsOfficeCommActivator.class);

    /**
     * Starts the <tt>msofficecomm</tt> bundle in a specific
     * {@link BundleContext}.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the
     * <tt>msofficecomm</tt> bundle is to be started
     * @throws Exception if anything goes wrong while starting the
     * <tt>msofficecomm</tt> bundle in the specified <tt>BundleContext</tt>
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        // The msofficecomm bundle is available on Windows only.
        if (!OSUtils.IS_WINDOWS)
            return;

        if (logger.isInfoEnabled())
            logger.info("MsOfficeComm plugin ... [STARTED]");

        Messenger.start(bundleContext);

        boolean stopMessenger = true;

        try
        {
            int hresult = OutOfProcessServer.start();

            if(logger.isInfoEnabled())
                logger.info("MsOfficeComm started OutOfProcessServer HRESULT:"
                    + hresult);

            if (hresult < 0)
                throw new RuntimeException("HRESULT " + hresult);
            else
                stopMessenger = false;
        }
        finally
        {
            if (stopMessenger)
                Messenger.stop(bundleContext);
        }
    }

    /**
     * Stops the <tt>msofficecomm</tt> bundle in a specific
     * {@link BundleContext}.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the
     * <tt>msofficecomm</tt> bundle is to be stopped
     * @throws Exception if anything goes wrong while stopping the
     * <tt>msofficecomm</tt> bundle in the specified <tt>BundleContext</tt>
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        // The msofficecomm bundle is available on Windows only.
        if (!OSUtils.IS_WINDOWS)
            return;

        try
        {
            int hresult = OutOfProcessServer.stop();

            if (hresult < 0)
                throw new RuntimeException("HRESULT " + hresult);
        }
        finally
        {
            Messenger.stop(bundleContext);
        }

        if (logger.isInfoEnabled())
            logger.info("MsOfficeComm plugin ... [UNREGISTERED]");
    }
}
