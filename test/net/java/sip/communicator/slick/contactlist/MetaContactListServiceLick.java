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
package net.java.sip.communicator.slick.contactlist;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.impl.protocol.mock.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

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

    //Convenience referenes to some groups and contacts.
    static final String topLevelGroupName = "SomePeople";

    static MockContactGroup topLevelMockGroup = null;

    static final String subLevelContactName = "Spencer";

    static MockContact subLevelContact = null;

    static MockContactGroup subLevelGroup = null;

    static MockContact subsubContact = null;

    static MockContact mockContactToRename = null;

    static MockContact mockContactToReorder = null;

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
        Hashtable<String, String> slickServiceProperties = new Hashtable<String, String>();
        slickServiceProperties.put("service.pid", getName());

        logger.debug("Service  " + getClass().getName() + " [  STARTED ]");

        //initialize and register a mock protocol provider that woul fill the
        //meta contact list with dummy data.
        //create and init an instance of a MockProvider
        MockProvider provider = new MockProvider("SlickMockUser");

        //fill the provider with dummy contacts and contact groups
        fillMockContactList(provider);

        MclSlickFixture.mockPrServiceRegistration
            = registerMockProviderService(provider);

        //store the created mock provider for later reference
        MclSlickFixture.mockProvider = provider;

        //store thre presence op set of the new provider into the fixture

        //get the operation set presence here.
        MclSlickFixture.mockPresOpSet =
            (MockPersistentPresenceOperationSet) MclSlickFixture.mockProvider
                .getOperationSet(OperationSetPersistentPresence.class);

        //add the meta contact list tests.
        addTestSuite(TestMetaContactList.class);
        addTestSuite(TestMetaContact.class);
        addTestSuite(TestMetaContactGroup.class);

        //tests that verify proper support of multiple protocol providers
        addTest(TestSupportForMultipleProviders.suite());

        //tests that verify persistence of the meta contact list.
        addTest(TestMetaContactListPersistence.suite());

        //register the slick itself
        context.registerService(getClass().getName()
                                , this
                                , slickServiceProperties);
        logger.debug("Service  " + getClass().getName() + " [REGISTERED]");
    }

    /**
     * Unregisters the mock provider that we registered in <tt>start()</tt>.
     *
     * @param context a currently valid bundle context
     */
    public void stop(BundleContext context)
    {
        if (MclSlickFixture.mockPrServiceRegistration != null)
            MclSlickFixture.mockPrServiceRegistration.unregister();
        if (MclSlickFixture.mockP1ServiceRegistration != null)
            MclSlickFixture.mockP1ServiceRegistration.unregister();
        if (MclSlickFixture.mockP2ServiceRegistration != null)
            MclSlickFixture.mockP2ServiceRegistration.unregister();

        //clear the meta contact list
        //find a reference to the meta contaact list service.
        ServiceReference ref = context.getServiceReference(
            MetaContactListService.class.getName());
        if(ref == null)
            return;
        MetaContactListService metaClService
            = (MetaContactListService)context.getService(ref);

        if(metaClService != null)
        {
            metaClService.purgeLocallyStoredContactListCopy();
        }

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
        //in order to make sure that only our mock provider will be taken into
        //account by the meta contact list implementation, we need to set the
        //provider mask both as a system property and as one of the service
        //properties of the newly registered provider.
        System.setProperty(MetaContactListService.PROVIDER_MASK_PROPERTY, "1");

        Hashtable<String, String> mockProvProperties = new Hashtable<String, String>();
        mockProvProperties.put(MetaContactListService.PROVIDER_MASK_PROPERTY,
                               "1");

        ServiceRegistration osgiRegistration
                    = MclSlickFixture.bundleContext.registerService(
                            ProtocolProviderService.class.getName(),
                            provider,
                            mockProvProperties);

        logger.debug("Registered a mock protocol provider!");

        return osgiRegistration;
    }

    /**
     * Creates a number of dummy contacts and fills the specified provider with
     * them.
     * @param provider the <tt>MockProvider</tt> to fill in.
     */
    private void fillMockContactList(MockProvider provider)
    {
        MockPersistentPresenceOperationSet mockOpSet =
            (MockPersistentPresenceOperationSet) provider
                .getOperationSet(OperationSetPersistentPresence.class);
        MockContactGroup root =
                (MockContactGroup)mockOpSet.getServerStoredContactListRoot();
        root.addContact( new MockContact("Ivan Ivanov", provider) );
        root.addContact( new MockContact("Martin Dupont", provider) );
        root.addContact( new MockContact("Joe Bloggs", provider) );

        MockContact someOfflineContact
            = new MockContact("I am offline", provider);
        someOfflineContact.setPresenceStatus(MockStatusEnum.MOCK_STATUS_00);
        root.addContact( someOfflineContact);

        mockContactToRename = new MockContact("Jane Doe", provider) ;
        root.addContact( mockContactToRename );

        mockContactToReorder = new MockContact("ZI'llChangeMyStatus", provider);
        //make sure that the contact starts at the bottom of the list.
        mockContactToReorder.setPresenceStatus(MockStatusEnum.MOCK_STATUS_00);
        root.addContact(mockContactToReorder);

        topLevelMockGroup = new MockContactGroup(topLevelGroupName, provider);

        subLevelContact = new MockContact(subLevelContactName, provider);
        topLevelMockGroup.addContact( subLevelContact );
        topLevelMockGroup.addContact( new MockContact("Pencho", provider) );
        topLevelMockGroup.addContact( new MockContact("Toto", provider) );

        subLevelGroup
            = new MockContactGroup("SubSubGroup", provider);

        subsubContact = new MockContact("SContact1", provider);

        subLevelGroup.addContact( subsubContact );
        subLevelGroup.addContact( new MockContact("SContact2", provider));
        subLevelGroup.addContact( new MockContact("SContact3", provider));
        subLevelGroup.addContact( new MockContact("SContact4", provider));

        topLevelMockGroup.addSubgroup(subLevelGroup);

        root.addSubgroup(topLevelMockGroup);

    }
}
