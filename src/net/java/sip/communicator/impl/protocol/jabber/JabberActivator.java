/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.hid.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.packetlogging.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Loads the Jabber provider factory and registers it with  service in the OSGI
 * bundle context.
 *
 * @author Damian Minkov
 * @author Symphorien Wanko
 * @author Emil Ivov
 */
public class JabberActivator
    implements BundleActivator
{
    /**
     * Service reference for the currently valid Jabber provider factory.
     */
    private ServiceRegistration jabberPpFactoryServReg = null;

    /**
     * Bundle context from OSGi.
     */
    static BundleContext bundleContext = null;

    /**
     * Configuration service.
     */
    private static ConfigurationService configurationService = null;

    /**
     * Media service.
     */
    private static MediaService mediaService = null;

    /**
     * A reference to the currently valid {@link NetworkAddressManagerService}.
     */
    private static NetworkAddressManagerService
                                        networkAddressManagerService = null;

    /**
     * The Jabber protocol provider factory.
     */
    private static ProtocolProviderFactoryJabberImpl
                                        jabberProviderFactory = null;

    /**
     * The <tt>UriHandler</tt> implementation that we use to handle "xmpp:" URIs
     */
    private UriHandlerJabberImpl uriHandlerImpl = null;

    /**
     * A reference to the currently valid <tt>UIService</tt>.
     */
    private static UIService uiService = null;

    /**
     * A reference to the currently valid <tt>ResoucreManagementService</tt>
     * instance.
     */
    private static ResourceManagementService resourcesService = null;

    /**
     * A reference to the currently valid <tt>HIDService</tt> instance.
     */
    private static HIDService hidService = null;

    private static PacketLoggingService packetLoggingService  = null;

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
    public void start(BundleContext context) throws Exception
    {
        JabberActivator.bundleContext = context;

        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.JABBER);

        jabberProviderFactory = new ProtocolProviderFactoryJabberImpl();

         /*
         * Install the UriHandler prior to registering the factory service in
         * order to allow it to detect when the stored accounts are loaded
         * (because they may be asynchronously loaded).
         */
        uriHandlerImpl = new UriHandlerJabberImpl(jabberProviderFactory);

        //register the jabber account man.
        jabberPpFactoryServReg =  context.registerService(
                    ProtocolProviderFactory.class.getName(),
                    jabberProviderFactory,
                    hashtable);
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return ConfigurationService a currently valid implementation of the
     * configuration service.
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            configurationService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configurationService;
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
     * @return a reference to the <tt>ProtocolProviderFactoryJabberImpl</tt>
     * instance that we have registered from this package.
     */
    static ProtocolProviderFactoryJabberImpl getProtocolProviderFactory()
    {
        return jabberProviderFactory;
    }

    /**
     * Returns a reference to a MediaService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     *
     * @return a reference to a MediaService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     */
//    public static MediaService getMediaService()
//    {
//        if(mediaService == null)
//        {
//            ServiceReference mediaServiceReference
//                = bundleContext.getServiceReference(
//                    MediaService.class.getName());
//
//            if (mediaServiceReference != null) {
//                mediaService = (MediaService)
//                    bundleContext.getService(mediaServiceReference);
//            }
//        }
//        return mediaService;
//    }

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
        jabberProviderFactory.stop();
        jabberPpFactoryServReg.unregister();

        if (uriHandlerImpl != null)
        {
            uriHandlerImpl.dispose();
            uriHandlerImpl = null;
        }
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
     * currently registered in the bundle context or <tt>null</tt> if no such
     * implementation was found.
     *
     * @return a reference to the ResourceManagementService implementation
     * currently registered in the bundle context or <tt>null</tt> if no such
     * implementation was found.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService
                = ResourceManagementServiceUtils.getService(bundleContext);
        return resourcesService;
    }

    /**
     * Returns a reference to a {@link MediaService} implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a reference to a {@link MediaService} implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
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
            hidService = (HIDService)bundleContext.getService(hidReference);
        }
        return hidService;
    }

    /**
     * Returns a reference to the PacketLoggingService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a reference to a PacketLoggingService implementation
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
}
