/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.contactlist;

import junit.framework.*;
import org.osgi.framework.*;
import net.java.sip.communicator.util.*;
import java.util.*;
import net.java.sip.communicator.slick.contactlist.mockprovider.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.contactlist.*;

/**
 * Performs testing of the MetaContactListService. This SLICK would register
 * a mock protocol provider, manually fill in its protocol specific contact
 * list and then verify that it is proparly retrieved and manipulated by the
 * tested implementation of the MetaContactListService implementation.
 *
 * @author Emil Ivov
 */
public class MetaContactListServiceLick
    extends TestSuite
    implements BundleActivator
{
    private static final Logger logger =
        Logger.getLogger(MetaContactListServiceLick.class);

    /**
     * A reference to the registration of the mock provider.
     */
    private ServiceRegistration mockProviderRegistration = null;

    /**
     * Start, init and register the SLICK. Create the mock protocol provider
     * and register it as a service.
     *
     * @param context a currently valid bundle context.
     */
    public void start(BundleContext context)
    {
        MclSlickFixture.bundleContext = context;

        setName("MetaContactListServiceLick");
        Hashtable slickServiceProperties = new Hashtable();
        slickServiceProperties.put("service.pid", getName());

        logger.debug("Service  " + getClass().getName() + " [  STARTED ]");

        //initialize and register a mock protocol provider that woul fill the
        //meta contact list with dummy data.
        initMockProvider(context);

        //add the meta contact list tests.
        addTestSuite(TestMetaContactList.class);

        //register the slick itself
        context.registerService(getClass().getName(), this, slickServiceProperties);
        logger.debug("Service  " + getClass().getName() + " [REGISTERED]");
    }

    /**
     * Unregisters the mock provider that we registered in <tt>start()</tt>.
     *
     * @param context a currently valid bundle context
     */
    public void stop(BundleContext context)
    {
        mockProviderRegistration.unregister();
    }

    /**
     * Initializes the mock provider.
     *
     * @param context a currently valid bundle context.
     */
    private void initMockProvider(BundleContext context)
    {
        //create and init an instance of a MockProvider
        MockProvider provider = new MockProvider();

        //fill the provider with dummy contacts and contact groups
        fillMockContactList(provider);

        //in order to make sure that only our mock provider will be taken into
        //account by the meta contact list implementation, we need to set the
        //provider mask both as a system property and as one of the service
        //properties of the newly registered provider.
        System.setProperty(MetaContactListService.PROVIDER_MASK_PROPERTY, "1");

        Hashtable mockProvProperties = new Hashtable();
        mockProvProperties.put(MetaContactListService.PROVIDER_MASK_PROPERTY,
                               "1");

        mockProviderRegistration = context.registerService(
            ProtocolProviderService.class.getName(),
            provider,
            mockProvProperties);

        logger.debug("Registerd a mock protocol provider!");

        //store the created mock provider for later reference
        MclSlickFixture.mockProvider = provider;
    }

    /**
     * Creates a number of dummy contacts and fills the specified provider with
     * them.
     * @param provider the <tt>MockProvider</tt> to fill in.
     */
    private void fillMockContactList(MockProvider provider)
    {
        MockPersistentPresenceOperationSet mockOpSet
            = (MockPersistentPresenceOperationSet)provider.
                getSupportedOperationSets().get( OperationSetPersistentPresence.
                                                    class.getName());
        MockContactGroup root =
                (MockContactGroup)mockOpSet.getServerStoredContactListRoot();
        root.addContact( new MockContact("Ivan Ivanov", provider) );
        root.addContact( new MockContact("Martin Dupont", provider) );
        root.addContact( new MockContact("Joe Bloggs", provider) );

        MockContactGroup group1 = new MockContactGroup("SomePeople", provider);

        group1.addContact( new MockContact("Spencer", provider) );
        group1.addContact( new MockContact("Pencho", provider) );
        group1.addContact( new MockContact("Toto", provider) );

        root.addSubGroup(group1);
    }
}
