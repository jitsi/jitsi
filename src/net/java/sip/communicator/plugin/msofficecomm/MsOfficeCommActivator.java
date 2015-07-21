/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.msofficecomm;

import net.java.sip.communicator.util.ServiceUtils;

import org.jitsi.service.resources.*;
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
     * The <tt>ResourceManagementService</tt> through which we access resources.
     */
    private static ResourceManagementService resourceService;
    
    /**
     * The <tt>BundleContext</tt> in which the msofficecomm plug-in is started.
     */
    private static BundleContext bundleContext;

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
        
        MsOfficeCommActivator.bundleContext = bundleContext;
        
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
            RegistryHandler.checkRegistryKeys();
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
    
    
    /**
     * Gets the <tt>ResourceManagementService</tt> to be used by the
     * functionality of the plug-in.
     *
     * @return the <tt>ResourceManagementService</tt> to be used by the
     * functionality of the plug-in
     */
    public static ResourceManagementService getResources()
    {
        if (resourceService == null)
        {
            resourceService
                = ServiceUtils.getService(
                        bundleContext,
                        ResourceManagementService.class);
        }
        return resourceService;
    }
}
