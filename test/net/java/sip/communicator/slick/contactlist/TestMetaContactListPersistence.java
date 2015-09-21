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

import org.osgi.framework.*;

/**
 * Tests in this class verify whether storing and reloading the meta contact
 * list after killing the meta contact list bundle work well.
 * @author Emil Ivov
 */
public class TestMetaContactListPersistence extends TestCase
{
    /**
     * A reference to the SLICK fixture.
     */
    private MclSlickFixture fixture = new MclSlickFixture(getClass().getName());

    public TestMetaContactListPersistence(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        fixture.setUp();
    }

    @Override
    protected void tearDown() throws Exception
    {
        fixture.tearDown();

        fixture = null;
        super.tearDown();
    }

    /**
     * Returns tests in this class in the order that they are supposed to be
     * executed.
     * @return a Test suite containing tests in this class in the order they are
     * to be executed.
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite();

        suite.addTest(new TestMetaContactListPersistence(
            "testCreateAndMoveMetaContact"));

        suite.addTest(new TestMetaContactListPersistence(
            "testReloadMetaContactListBundle"));

        suite.addTest( new TestMetaContactListPersistence(
            "testPartialContactListRestauration"));

        suite.addTest( new TestMetaContactListPersistence(
            "testCompleteContactListRestauration"));

        suite.addTest( new TestMetaContactListPersistence(
            "testPurgeLocalContactListCopy"));

        return suite;
    }

    /**
     * In this test we only create a meta contact and move it somewhere else.
     * This is a delicate operation since any underlying storage method would
     * have to not only move the meta contact itself but also make sure that all
     * proto contacts have been moved to protogroups accordingly. We don't do
     * any real testing inside this method. The testing would be happening
     * during reload and it would fail if the contacts moved in this method
     * were not properly moved in the underlying storage utilities.
     */
    public void testCreateAndMoveMetaContact()
    {
        String newContactID ="testCreateAndMoveMetaContact.ContactID";
        MetaContactGroup parentMetaGroup
            = fixture.metaClService.getRoot().getMetaContactSubgroup(
                    MetaContactListServiceLick.topLevelGroupName);

        //create a new metacontact and, hence mock contact, in the meta
        //"SomePeople" non-toplevel group
        fixture.metaClService.createMetaContact(MclSlickFixture.mockProvider
            , parentMetaGroup
            , newContactID);

        //check that the contact has been successfully created in the meta cl
        MetaContact newMetaContact
            = parentMetaGroup.getMetaContact(
                    MclSlickFixture.mockProvider,
                    newContactID);

        assertNotNull("create failed. couldn't find the new contact."
            , newMetaContact);

        //move the meta contact somewhere else
        fixture.metaClService.moveMetaContact(
            newMetaContact, fixture.metaClService.getRoot());
    }


    /**
     * Uninstalls the meta contact list bundle so that it would be forced to
     * persistently store its contents and load it again later.
     *
     * @throws java.lang.Exception in case stopping or uninstalling the bundle
     * fails.
     */
    public void testReloadMetaContactListBundle()
        throws Exception
    {
        Object o = new Object();
        synchronized(o){
            // wait other operations to finish before reloading
            o.wait(1000);
        }

        Bundle metaClBundle = findMetaClBundle();

        //uninstall the meta contact list service
        assertNotNull("Couldn't find the bundle that exports the meta "
                      + "contact list servce implementation that we're "
                      + "currently testing"
                      , metaClBundle);

        metaClBundle.stop();

        assertTrue("Couldn't stop the meta cl bundle. State was "
                   + metaClBundle.getState()
                   ,    Bundle.ACTIVE   != metaClBundle.getState()
                     && Bundle.STOPPING != metaClBundle.getState());

        metaClBundle.uninstall();

        assertEquals("Couldn't stop the meta cl bundle."
                     , Bundle.UNINSTALLED, metaClBundle.getState());

        //unregister all mock providers
        MclSlickFixture.mockPrServiceRegistration.unregister();
        MclSlickFixture.mockP1ServiceRegistration.unregister();
        MclSlickFixture.mockP2ServiceRegistration.unregister();

        //remove existing mock providers.
        MclSlickFixture.replacementMockPr = new MockProvider(
            MclSlickFixture.mockProvider.getAccountID().getUserID());
        MclSlickFixture.replacementMockP1 = new MockProvider(
            MclSlickFixture.mockP1.getAccountID().getUserID());
        MclSlickFixture.replacementMockP2 = new MockProvider(
            MclSlickFixture.mockP2.getAccountID().getUserID());

        //reinstall only one of the existing mock providers
        //we will reinstall the other mock providers later. our purpose is to
        //verify that the contact list would only reload contacts for the
        //re-registered mock provider upon startup and that it would later
        //complete its list with the rest of the mock providers once we
        //reregister them.
        MclSlickFixture.mockPrServiceRegistration = MetaContactListServiceLick
            .registerMockProviderService(MclSlickFixture.replacementMockPr);

        //reinstall the metacontactlist bundle
        metaClBundle = MclSlickFixture.bundleContext.installBundle(
                        metaClBundle.getLocation());

        assertEquals("Couldn't re-install meta cl bundle."
                     , Bundle.INSTALLED, metaClBundle.getState());

        metaClBundle.start();
        assertEquals("Couldn't re-start meta cl bundle."
                     , Bundle.ACTIVE, metaClBundle.getState());

        fixture.metaClService
            = (MetaContactListService)
                MclSlickFixture.bundleContext.getService(
                        MclSlickFixture.bundleContext.getServiceReference(
                                MetaContactListService.class.getName()));

        assertNotNull("The meta contact list service was not re-registered "
                      +"after reinstalling its bundle."
                      , fixture.metaClService);
    }

    /**
     * Tests whether the freshly reloaded meta contact list has properly
     * reloaded contacts for the provider that was registered at the time of its
     * re-installation
     */
    public void testPartialContactListRestauration()
    {
        //verify that contents of the meta contact list matches contents of
        //the mock provider we removed.
        ContactGroup oldProtoRoot =
            MclSlickFixture.mockProvider
                .getOperationSet(OperationSetPersistentPresence.class)
                .getServerStoredContactListRoot();

        MclSlickFixture.assertGroupEquals(
            (MockContactGroup)oldProtoRoot
            , fixture.metaClService.getRoot()
            , true);//we might have trailing empty meta groups here remaining
                    //from previous testing so ignore them.

        //verify that the new mock provider has created unresolved contacts
        //for all contacts in the meta cl.
        ContactGroup newProtoRoot =
            MclSlickFixture.replacementMockPr
                .getOperationSet(OperationSetPersistentPresence.class)
                .getServerStoredContactListRoot();

        assertEquals("Newly loaded provider does not match the old one."
                     , oldProtoRoot
                     , newProtoRoot);

        //verify that all contacts in the replacement provider are unresolved
        //as otherwise this would mean that the meta contact list has not
        //used the createUnresolvedContact() when creating them.
        Iterator<ContactGroup> subgroups = newProtoRoot.subgroups();

        while(subgroups.hasNext())
        {
            assertUnresolvedContents( subgroups.next() );
        }
    }

    /**
     * Register the remaining protocol providers and make sure that they too are
     * properly loaded inside the contact list. We also need to verify that
     * proto contacts that have been merged in a single meta contact are still
     * merged.
     */
    public void testCompleteContactListRestauration()
    {
        //reinstall remaining mock providers
        //we will reinstall the other mock providers later. our purpose is to
        MclSlickFixture.mockP1ServiceRegistration = MetaContactListServiceLick
            .registerMockProviderService(MclSlickFixture.replacementMockP1);

        MclSlickFixture.mockP2ServiceRegistration = MetaContactListServiceLick
            .registerMockProviderService(MclSlickFixture.replacementMockP2);

        //Get references to the root groups of the 2 providers we removed
        ContactGroup oldProtoMockP1Root =
            MclSlickFixture.mockP1
                .getOperationSet(OperationSetPersistentPresence.class)
                .getServerStoredContactListRoot();

        ContactGroup oldProtoMockP2Root =
            MclSlickFixture.mockP2
                .getOperationSet(OperationSetPersistentPresence.class)
                .getServerStoredContactListRoot();

        //verify that contacts tnat unresolved contacts that have been created
        //inside that the replacement mock providers match those in the
        //providers we removed.
        ContactGroup newProtoMockP1Root =
            MclSlickFixture.replacementMockP1
                .getOperationSet(OperationSetPersistentPresence.class)
                .getServerStoredContactListRoot();

        assertEquals("Newly loaded provider does not match the old one."
                     , oldProtoMockP1Root
                     , newProtoMockP1Root);

        ContactGroup newProtoMockP2Root =
            MclSlickFixture.replacementMockP2
                .getOperationSet(OperationSetPersistentPresence.class)
                .getServerStoredContactListRoot();

        assertEquals("Newly loaded provider does not match the old one."
                     , oldProtoMockP2Root
                     , newProtoMockP2Root);

        //verify that all contacts in the replacement providers are unresolved
        //as otherwise this would mean that the meta contact list has not
        //used the createUnresolvedContact() when creating them.
        Iterator<ContactGroup> subgroups = newProtoMockP1Root.subgroups();
        while(subgroups.hasNext())
        {
            assertUnresolvedContents( subgroups.next() );
        }


        subgroups = newProtoMockP2Root.subgroups();
        while(subgroups.hasNext())
        {
            assertUnresolvedContents( subgroups.next() );
        }
    }


    /**
     * Traverses all contacts and groups of <tt>root</tt> and throws a failure
     * exception the moment if finds one of them not to be unresolved.
     *
     * @param root the contact group where the recursive assertion should begin
     */
    private void assertUnresolvedContents(ContactGroup root)
    {
        assertEquals("isResolved for grp:" + root.getGroupName()
                     , false, root.isResolved());

        // verify all contacts
        Iterator<Contact> contacts = root.contacts();

        while(contacts.hasNext())
        {
            Contact contact = contacts.next();
            assertEquals("isResolved for contact:" + contact.getDisplayName()
                     , false, contact.isResolved());
        }

        //recurse all subgroups
        Iterator<ContactGroup> subgroups = root.subgroups();

        while(subgroups.hasNext()){
            assertUnresolvedContents(subgroups.next());
        }
    }

    /**
     * Removes the locally stored contact list copy. The purpose of this is to
     * leave the local list empty for a next round of testing.
     */
    public void testPurgeLocalContactListCopy()
    {
        fixture.metaClService.purgeLocallyStoredContactListCopy();
    }

    /**
     * Returns the bundle that has registered the meta contact list service
     * implementation that we're currently testing. The method would go through
     * all bundles currently installed in the framework and return the first
     * one that exports the same meta cl instance as the one we use in this
     * slick.
     * @return the Bundle that has registered the meta contact list service
     * we're using in the slick.
     */
    private Bundle findMetaClBundle()
    {
        Bundle[] bundles = MclSlickFixture.bundleContext.getBundles();

        for (int i = 0; i < bundles.length; i++)
        {
            ServiceReference[] registeredServices
                = bundles[i].getRegisteredServices();

            if(registeredServices == null)
                continue;

            for (int j = 0; j < registeredServices.length; j++)
            {
                Object service
                    = MclSlickFixture.bundleContext.getService(
                            registeredServices[j]);
                if(service == fixture.metaClService)
                    return bundles[i];
            }
        }

        return null;
    }
}
