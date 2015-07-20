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

import net.java.sip.communicator.impl.protocol.mock.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 * Fields, commonly used by the MetaContactListSlick.
 *
 * @author Emil Ivov
 */
public class MclSlickFixture
    extends junit.framework.TestCase
{
    /**
     * The bundle context that we received when the slick was activated.
     */
    public static BundleContext bundleContext = null;

    /**
     * A reference to the meta contact list service currently available on the
     * OSGI bus.
     */
    public MetaContactListService metaClService = null;

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

    /**
     * A reference to the registration of the first mock provider.
     */
    public static ServiceRegistration mockPrServiceRegistration = null;

    /** The provider we will be using to replace mockProvider*/
    public static MockProvider replacementMockPr = null;


    //Mock Provider 1
    /**
     * A second mock provider that we will be using when testing support for
     * multiple protocol providers
     */
    public static MockProvider mockP1 = new MockProvider("mockP1");

    /** A test group for mockP1*/
    public static MockContactGroup mockP1Grp1
                                = new MockContactGroup("MockP1.Grp1", mockP1);

    /** A test sub group for mockP1*/
    public static MockContactGroup subMockP1Grp = new MockContactGroup(
        "Mock1.SubProtoGroup", mockP1);

    /** A test contact for mockP1*/
    public static MockContact emilP1 = new MockContact("emil@MockP1", mockP1);

    /** A test contact for mockP1*/
    public static MockContact subEmilP1
        = new MockContact("subemil@MockP1", mockP1);

    /** The meta contact group encapsulator of mockP1Grp1*/
    public static MetaContactGroup metaP1Grp1 = null;

    /** The presence operation set for the mockP1 provider */
    public static MockPersistentPresenceOperationSet mockPresOpSetP1 = null;

    /** A reference to the service registration of mock p1 */
    public static ServiceRegistration mockP1ServiceRegistration = null;

    /** The provider we will be using to replace mockP1*/
    public static MockProvider replacementMockP1 = null;

    //Mock Provider 2
    /**
     * Yet another mock provider that we will be using when testing support for
     * multiple providers.
     */
    public static MockProvider mockP2 = new MockProvider("mockP2");

    /** A test group for mockP2*/
    public static MockContactGroup mockP2Grp1
        = new MockContactGroup("MockP2.Grp1", mockP2);

    /** A test contact for mockP2*/
    public static MockContact emilP2 = new MockContact("emil@MockP2", mockP2);

    /** The meta encapsulator of the mockP2Grp1 group*/
    public static MetaContactGroup metaP2Grp1 = null;

    /** The presence operation set of the mockP2 provider.*/
    public static MockPersistentPresenceOperationSet mockPresOpSetP2 = null;

    /** A reference to the service registration of the mockP2 provider. */
    public static ServiceRegistration mockP2ServiceRegistration = null;

    /** The provider we will be using to replace mockP2*/
    public static MockProvider replacementMockP2 = null;

    /**
     * Initialize the contacts of the two test providers so that they could
     * be directly comparable to what has been parsed from the file.
     */
    static
    {
        //init mock provider 1
        subMockP1Grp.addContact(subEmilP1);
        mockP1Grp1.addContact(emilP1);
        mockP1Grp1.addSubgroup(subMockP1Grp);

        mockPresOpSetP1 =
            (MockPersistentPresenceOperationSet) mockP1
                .getOperationSet(OperationSetPresence.class);
        mockPresOpSetP1.addMockGroup(mockP1Grp1);

        //init mock provider 2
        mockP2Grp1.addContact(emilP2);

        mockPresOpSetP2 =
            (MockPersistentPresenceOperationSet) mockP2
                .getOperationSet(OperationSetPresence.class);
        mockPresOpSetP2.addMockGroup(mockP2Grp1);
    }

    public MclSlickFixture(Object obj)
    {
    }

    /**
     * Find a reference of the meta contact list service and set the
     * corresponding field.
     */
    @Override
    public void setUp()
    {
        //find a reference to the meta contaact list service.
        ServiceReference ref = bundleContext.getServiceReference(
            MetaContactListService.class.getName());
        metaClService
            = (MetaContactListService)bundleContext.getService(ref);

    }

    /**
     *
     */
    @Override
    public void tearDown()
    {
    }

    /**
     * Makes sure that the specified actualGroup contains the same contacts
     * and subgroups as the expectedGroup. (Method operates recursively).
     *
     * @param expectedGroup a MockContactGroup instance used as a reference.
     * @param actualGroup the MetaContactGroup retrieved from the metacontact
     * list.
     * @param ignoreEmptyMetaGroups determines whether empty meta groups should
     * be considered a problem.
     */
    static void assertGroupEquals(  MockContactGroup expectedGroup,
                                    MetaContactGroup actualGroup,
                                    boolean ignoreEmptyMetaGroups)
    {
        assertNotNull("Group " + expectedGroup.getGroupName() + " was "
                      + "returned by the MetaContactListService implementation "
                      + "but was not in the expected contact list."
                      , actualGroup);

        assertEquals("Group " + expectedGroup.getGroupName()
                     + ",  number of member contacts: "
                     , expectedGroup.countContacts()
                     , actualGroup.countChildContacts());

        if( !ignoreEmptyMetaGroups)
        {
            assertEquals("Group " + expectedGroup.getGroupName()
                         + ", numbber of subgroups: "
                         , expectedGroup.countSubgroups()
                         , actualGroup.countSubgroups());
        }
        else
        {
            int emptyMetaGroups = 0;
            for(int i = 0; i < actualGroup.countSubgroups(); i++)
            {
                if(actualGroup.getMetaContactSubgroup(i).countContactGroups() == 0)
                    emptyMetaGroups ++;
            }

            assertEquals("Group " + expectedGroup.getGroupName()
                         + ", numbber of subgroups: "
                         , expectedGroup.countSubgroups()
                         , actualGroup.countSubgroups() - emptyMetaGroups);

        }

        //go over the subgroups and check that they've been all added to the
        //meta contact list.
        Iterator<ContactGroup> expectedSubgroups = expectedGroup.subgroups();
        while (expectedSubgroups.hasNext() )
        {
            MockContactGroup expectedSubGroup
                = (MockContactGroup)expectedSubgroups.next();

            MetaContactGroup actualSubGroup
                = actualGroup
                    .getMetaContactSubgroup(expectedSubGroup.getGroupName());

            assertGroupEquals(
                expectedSubGroup, actualSubGroup, ignoreEmptyMetaGroups);
        }

        Iterator<MetaContact> actualContactsIter
            = actualGroup.getChildContacts();

        //check whether every contact in the meta list exists in the source
        //mock provider contact list.
        while (actualContactsIter.hasNext())
        {
            MetaContact actualMetaContact = actualContactsIter.next();

            assertEquals("Number of protocol specific contacts in a MetaContact"
                          , 1, actualMetaContact.getContactCount());

            assertTrue(
                "No contacts were encapsulated by MetaContact: "
                + actualMetaContact
                , actualMetaContact.getContacts().hasNext());

            Contact actualProtoContact
                = actualMetaContact.getContacts().next();

            assertNotNull("getContactForProvider returned null for MockProvider"
                          , actualProtoContact);

            Contact expectedProtoContact
                = expectedGroup.getContact(actualProtoContact.getAddress());

            assertNotNull("Contact " + actualMetaContact.getDisplayName()
                          + " was returned by "
                          + "the MetaContactListService implementation but was "
                          + "not in the expected contact list."
                          , expectedProtoContact);
        }
    }
}
