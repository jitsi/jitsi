/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.msghistory;

import java.util.*;

import org.osgi.framework.*;
import junit.framework.*;
import net.java.sip.communicator.impl.protocol.mock.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.msghistory.MessageHistoryService;

/**
 *
 * @author Damian Minkov
 */
public class MsgHistoryServiceLick extends TestSuite implements BundleActivator {
    private static Logger logger = Logger.getLogger(MsgHistoryServiceLick.class);

    protected static BundleContext bc = null;

    static final String TEST_CONTACT_NAME = "Mincho_Penchev";

    /**
     * The provider that we use to make a dummy server-stored contactlist
     * used for testing. The mockProvider is instantiated and registered
     * by the metacontactlist slick activator.
     */
    public static MockProvider mockProvider = null;

    /**
     * The persistent presence operation set of the default mock provider.
     */
    public static MockPersistentPresenceOperationSet mockPresOpSet = null;

    public static MockBasicInstantMessaging mockBImOpSet = null;

    /**
     * A reference to the registration of the first mock provider.
     */
    public static ServiceRegistration mockPrServiceRegistration = null;

    private static ServiceReference msgHistoryServiceRef = null;
    public static MessageHistoryService msgHistoryService = null;


    /**
     * Start the History Sevice Implementation Compatibility Kit.
     *
     * @param bundleContext
     *            BundleContext
     * @throws Exception
     */
    public void start(BundleContext bundleContext) throws Exception {
        MsgHistoryServiceLick.bc = bundleContext;

        setName("MsgHistoryServiceLick");
        Hashtable properties = new Hashtable();
        properties.put("service.pid", getName());

        addTestSuite(TestMsgHistoryService.class);
        bundleContext.registerService(getClass().getName(), this, properties);

        logger.debug("Successfully registered " + getClass().getName());

        MockProvider provider = new MockProvider("SlickMockUser");

        //store thre presence op set of the new provider into the fixture
        Map supportedOperationSets =
            provider.getSupportedOperationSets();

        //get the operation set presence here.
        MsgHistoryServiceLick.mockPresOpSet =
            (MockPersistentPresenceOperationSet) supportedOperationSets.get(
                OperationSetPersistentPresence.class.getName());

        MsgHistoryServiceLick.mockBImOpSet =
            (MockBasicInstantMessaging) supportedOperationSets.get(
                OperationSetBasicInstantMessaging.class.getName());

        // fill in a contact to comunicate with
        MockContactGroup root =
            (MockContactGroup) MsgHistoryServiceLick.mockPresOpSet
            .getServerStoredContactListRoot();

        root.addContact(new MockContact(TEST_CONTACT_NAME, provider));


        MsgHistoryServiceLick.mockPrServiceRegistration
            = registerMockProviderService(provider);

        //store the created mock provider for later reference
        MsgHistoryServiceLick.mockProvider = provider;

        msgHistoryServiceRef =
            bundleContext.getServiceReference(MessageHistoryService.class.getName());

        msgHistoryService = (MessageHistoryService) bundleContext.getService(
            msgHistoryServiceRef);
    }

    /**
     * stop
     *
     * @param bundlecontext BundleContext
     * @throws Exception
     */
    public void stop(BundleContext bundlecontext) throws Exception {
        BundleContext context = MsgHistoryServiceLick.bc;

       context.ungetService(this.msgHistoryServiceRef);

       if (MsgHistoryServiceLick.mockPrServiceRegistration != null)
           MsgHistoryServiceLick.mockPrServiceRegistration.unregister();

       this.msgHistoryService = null;
       this.msgHistoryServiceRef = null;
    }

    /**
     * Registers the specified mock provider as an implementation of the
     * ProtocolProviderService in the currently valid bundle context.
     *
     * @param provider the protocol provider we'd like to export as an OSGI
     * service.
     * @return the ServiceRegistration reference returned when registering
     * the specified provider.
     */
    public static ServiceRegistration registerMockProviderService(
                                                        MockProvider provider)
    {
        ServiceRegistration osgiRegistration = null;
        Hashtable mockProvProperties = new Hashtable();
        mockProvProperties.put(ProtocolProviderFactory.
                               PROTOCOL_PROPERTY_NAME,
                               provider.getProtocolName());

        osgiRegistration
            = MsgHistoryServiceLick.bc.registerService(
                ProtocolProviderService.class.getName(),
                provider,
                mockProvProperties);
        logger.debug("Registered a mock protocol provider!");

        return osgiRegistration;
    }
}
