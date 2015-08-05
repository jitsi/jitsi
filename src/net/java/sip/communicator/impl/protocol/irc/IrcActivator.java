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
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Loads the IRC provider factory and registers its services in the OSGI bundle
 * context.
 *
 * @author Stephane Remy
 * @author Loic Kempf
 * @author Danny van Heumen
 */
public class IrcActivator
    implements BundleActivator
{
    /**
     * LOGGER instance.
     */
    private static final Logger LOGGER = Logger.getLogger(IrcActivator.class);

    /**
     * A reference to the IRC protocol provider factory.
     */
    private static ProtocolProviderFactoryIrcImpl
                                    ircProviderFactory = null;

    /**
     * The currently valid bundle context.
     */
    private static BundleContext bundleContext = null;

    /**
     * Resource management service instance.
     */
    private static ResourceManagementService resourceService;

    /**
     * Certificate Service instance.
     */
    private static CertificateService certificateService;

    /**
     * MultiUserChat Service instance.
     */
    private static MUCService mucService;

    /**
     * UI Service instance.
     */
    private static UIService uiService;

    /**
     * Configuration Service instance.
     */
    private static ConfigurationService configService;

    /**
     * Called when this bundle is started. In here we'll export the
     * IRC ProtocolProviderFactory implementation so that it could be
     * possible to register accounts with it in SIP Communicator.
     *
     * @param context The execution context of the bundle being started.
     * @throws Exception If this method throws an exception, this bundle is
     *   marked as stopped and the Framework will remove this bundle's
     *   listeners, unregister all services registered by this bundle, and
     *   release all services used by this bundle.
     */
    @Override
    public void start(final BundleContext context)
        throws Exception
    {
        bundleContext = context;

        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);

        ircProviderFactory = new ProtocolProviderFactoryIrcImpl();

        //Register the IRC provider factory.
        context.registerService(
                    ProtocolProviderFactory.class.getName(),
                    ircProviderFactory,
                    hashtable);

        if (LOGGER.isInfoEnabled())
        {
            LOGGER.info("IRC protocol implementation [STARTED].");
        }
    }

    /**
     * Returns a reference to the protocol provider factory that we have
     * registered.
     * @return a reference to the <tt>ProtocolProviderFactoryJabberImpl</tt>
     * instance that we have registered from this package.
     */
    public static ProtocolProviderFactoryIrcImpl getProtocolProviderFactory()
    {
        return ircProviderFactory;
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
    @Override
    public void stop(final BundleContext context)
        throws Exception
    {
        if (LOGGER.isInfoEnabled())
        {
            LOGGER.info("IRC protocol implementation [STOPPED].");
        }
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>.
     *
     * @return the <tt>ResourceManagementService</tt>.
     */
    public static ResourceManagementService getResources()
    {
        if (resourceService == null)
        {
            resourceService
                = ResourceManagementServiceUtils.getService(bundleContext);
        }
        return resourceService;
    }

    /**
     * Bundle Context.
     *
     * @return returns bundle context
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Return the MultiUserChat service impl.
     *
     * @return MUCService impl.
     */
    public static MUCService getMUCService()
    {
        if (mucService == null)
        {
            mucService =
                ServiceUtils.getService(bundleContext, MUCService.class);
        }
        return mucService;
    }

    /**
     * Return the UI service impl.
     *
     * @return returns UI Service instance
     */
    public static UIService getUIService()
    {
        if (uiService == null)
        {
            uiService = ServiceUtils.getService(bundleContext, UIService.class);
        }
        return uiService;
    }

    /**
     * Return the certificate verification service impl.
     *
     * @return the CertificateVerification service.
     */
    public static CertificateService getCertificateService()
    {
        if (certificateService == null)
        {
            certificateService =
               ServiceUtils.getService(bundleContext, CertificateService.class);
        }
        return certificateService;
    }

    /**
     * Return the configuration service impl.
     *
     * @return the Configuration service
     */
    public static ConfigurationService getConfigurationService()
    {
        if(configService == null)
        {
            configService = ServiceUtils.getService(bundleContext, ConfigurationService.class);
        }
        return configService;
    }
}
