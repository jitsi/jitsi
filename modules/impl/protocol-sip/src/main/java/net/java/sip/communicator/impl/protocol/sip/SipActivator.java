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
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.hid.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.protocol.*;

import net.java.sip.communicator.util.osgi.DependentActivator;
import net.java.sip.communicator.util.osgi.ServiceUtils;
import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.packetlogging.*;
import org.jitsi.service.resources.*;
import org.jitsi.utils.version.*;
import org.osgi.framework.*;

/**
 * Activates the SIP package
 * @author Emil Ivov
 */
public class SipActivator
    extends DependentActivator
{
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SipActivator.class);

    private        ServiceRegistration  sipPpFactoryServReg   = null;
            static BundleContext        bundleContext         = null;
    private static ConfigurationService configurationService  = null;
    private static NetworkAddressManagerService networkAddressManagerService
                                                              = null;
    private static MediaService         mediaService          = null;
    private static VersionService       versionService        = null;
    private static UIService            uiService             = null;
    private static HIDService           hidService            = null;
    private static PacketLoggingService packetLoggingService  = null;
    private static CertificateService   certService           = null;

    /**
     * The resource service. Used for checking for default values
     * and loding status icons.
     */
    private static ResourceManagementService resources        = null;

    private static ProtocolProviderFactorySipImpl sipProviderFactory = null;

    private UriHandlerSipImpl uriHandlerSipImpl = null;

    public SipActivator()
    {
        super(
            ConfigurationService.class,
            NetworkAddressManagerService.class,
            MediaService.class,
            VersionService.class,
            UIService.class,
            HIDService.class,
            PacketLoggingService.class,
            CertificateService.class
        );
    }

    /**
     * Called when this bundle is started so the Framework can perform the
     * bundle-specific activities necessary to start this bundle.
     *
     * @param context The execution context of the bundle being started.
     * @throws Exception If this method throws an exception, this bundle is
     *   marked as stopped and the Framework will remove this bundle's
     *   listeners, unregister all services registered by this bundle, and
     *   release all services used by this bundle.
     */
    @Override
    public void startWithServices(BundleContext context) throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug("Started.");

        SipActivator.bundleContext = context;

        sipProviderFactory = new ProtocolProviderFactorySipImpl(context);

        /*
         * Install the UriHandler prior to registering the factory service in
         * order to allow it to detect when the stored accounts are loaded
         * (because they may be asynchronously loaded).
         */
        uriHandlerSipImpl = new UriHandlerSipImpl(sipProviderFactory);

        //reg the sip account man.
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.SIP);
        sipPpFactoryServReg =  context.registerService(
                    ProtocolProviderFactory.class.getName(),
                    sipProviderFactory,
                    properties);

        if (logger.isDebugEnabled())
            logger.debug("SIP Protocol Provider Factory ... [REGISTERED]");
    }

    /**
     * Return the certificate verification service impl.
     * @return the CertificateVerification service.
     */
    public static CertificateService getCertificateVerificationService()
    {
        if(certService == null)
        {
            ServiceReference guiVerifyReference
                = bundleContext.getServiceReference(
                    CertificateService.class.getName());
            if(guiVerifyReference != null)
                certService = (CertificateService)
                    bundleContext.getService(guiVerifyReference);
        }

        return certService;
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigurationService()
    {
        if(configurationService == null)
        {
            ServiceReference confReference
                = bundleContext.getServiceReference(
                    ConfigurationService.class.getName());
            configurationService
                = (ConfigurationService) bundleContext.getService(confReference);
        }
        return configurationService;
    }

    /**
     * Returns a reference to a NetworkAddressManagerService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * NetworkAddressManagerService .
     */
    public static NetworkAddressManagerService getNetworkAddressManagerService()
    {
        if(networkAddressManagerService == null)
        {
            ServiceReference confReference
                = bundleContext.getServiceReference(
                    NetworkAddressManagerService.class.getName());
            networkAddressManagerService = (NetworkAddressManagerService)
                bundleContext.getService(confReference);
        }
        return networkAddressManagerService;
    }

    /**
     * Returns a reference to <tt>HIDService</tt> implementation currently
     * registered in the bundle context or null if no such implementation was
     * found
     *
     * @return a currently valid implementation of the <tt>HIDService</tt>
     */
    public static HIDService getHIDService()
    {
        if(hidService == null)
        {
            ServiceReference hidReference =
                bundleContext.getServiceReference(
                        HIDService.class.getName());

            if(hidReference == null)
                return null;

            hidService = (HIDService)bundleContext.getService(hidReference);
        }
        return hidService;
    }

    /**
     * Returns a reference to the bundle context that we were started with.
     * @return a reference to the BundleContext instance that we were started
     * witn.
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Retrurns a reference to the protocol provider factory that we have
     * registered.
     * @return a reference to the <tt>ProtocolProviderFactorySipImpl</tt>
     * instance that we have registered from this package.
     */
    public static ProtocolProviderFactorySipImpl getProtocolProviderFactory()
    {
        return sipProviderFactory;
    }

    /**
     * Returns a reference to a MediaService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     *
     * @return a reference to a MediaService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     */
    public static MediaService getMediaService()
    {
        if(mediaService == null)
        {
            ServiceReference mediaServiceReference
                = bundleContext.getServiceReference(
                    MediaService.class.getName());
            mediaService = (MediaService)bundleContext
                .getService(mediaServiceReference);
        }
        return mediaService;
    }

    /**
     * Returns a reference to a VersionService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     *
     * @return a reference to a VersionService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     */
    public static VersionService getVersionService()
    {
        if(versionService == null)
        {
            ServiceReference versionServiceReference
                = bundleContext.getServiceReference(
                    VersionService.class.getName());
            versionService = (VersionService)bundleContext
                .getService(versionServiceReference);
        }
        return versionService;
    }

    /**
     * Returns a reference to the UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     *
     * @return a reference to a UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     */
    public static UIService getUIService()
    {
        if(uiService == null)
        {
            ServiceReference uiServiceReference
                = bundleContext.getServiceReference(
                    UIService.class.getName());
            uiService = (UIService)bundleContext
                .getService(uiServiceReference);
        }
        return uiService;
    }

    /**
     * Returns a reference to the ResourceManagementService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a reference to a ResourceManagementService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     */
    public static ResourceManagementService getResources()
    {
        if (resources == null)
        {
            resources
                = ServiceUtils.getService(
                        bundleContext, ResourceManagementService.class);
        }
        return resources;
    }

    /**
     * Returns a reference to the <tt>PacketLoggingService</tt> implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a reference to a <tt>PacketLoggingService</tt> implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     */
    public static PacketLoggingService getPacketLogging()
    {
        if (packetLoggingService == null)
        {
            packetLoggingService
                = ServiceUtils.getService(
                        bundleContext, PacketLoggingService.class);
        }
        return packetLoggingService;
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
    public void stop(BundleContext context) throws Exception
    {
        if (sipProviderFactory != null)
        {
            sipProviderFactory.stop();
        }

        sipPpFactoryServReg.unregister();

        if (uriHandlerSipImpl != null)
        {
            uriHandlerSipImpl.dispose();
            uriHandlerSipImpl = null;
        }

        configurationService = null;
        networkAddressManagerService = null;
        mediaService = null;
        versionService = null;
        uiService = null;
        hidService = null;
        packetLoggingService = null;
        certService = null;
    }
}
