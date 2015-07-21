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
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Test meta contact list functionality such as filling in the contact list from
 * existing protocol providers, properly handling events of modified server
 * stored contact lists and modifying server stored contact lists through the
 * meta contact list service. Testing is done against the MockProvider which
 * is directly accessible throughout the tests.
 * <p>
 * What we still need to test here:<br>
 * 1. Test that groups are automatically created when proto contacts are moved.
 * <br>
 * 2. Test that events are generated when creating moving and removing groups
 *    from the metacontact list itself.
 * <br>
 *
 * @author Emil Ivov
 */
public class TestMetaContactList
    extends TestCase
{
    /**
     * A reference to the SLICK fixture.
     */
    private MclSlickFixture fixture = new MclSlickFixture(getClass().getName());

    /**
     * The name of the new subscripiton that we create during testing.
     */
    private String newSubscriptionName = "NewSubscription";

    /**
     * The name to use when renaming the new contat group.
     */
    private String renamedGroupName = "RenamedContactGroup";

    private static final Logger logger =
        Logger.getLogger(TestMetaContactList.class);

    /**
     * Creates a unit test with the specified name.
     * @param name the name of one of the test methods in this class.
     */
    public TestMetaContactList(String name)
    {
        super(name);
    }

    /**
     * Initialize the environment.
     * @throws Exception if anything goes wrong.
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();
    }

    /**
     * Finalization
     * @throws Exception in case sth goes wrong.
     */
    @Override
    protected void tearDown() throws Exception
    {
        fixture.tearDown();
        super.tearDown();
    }

    /**
     * Verifies that the contacts retrieved by the meta contact list service,
     * matches the one that were in the mock provider.
     */
    public void testContactListRetrieving()
    {
        MockContactGroup expectedRoot
            = (MockContactGroup)
                MclSlickFixture.mockPresOpSet.getServerStoredContactListRoot();

        logger.info("============== Predefined contact List ==============");

        logger.info("rootGroup="+expectedRoot.getGroupName()
                +" rootGroup.childContacts="+expectedRoot.countContacts()
                + "rootGroup.childGroups="+expectedRoot.countSubgroups()
                + " Printing rootGroupContents=\n"+expectedRoot.toString());

        MetaContactGroup actualRoot = fixture.metaClService.getRoot();

        logger.info("================ Meta Contact List =================");

        logger.info("rootGroup="+actualRoot.getGroupName()
                     +" rootGroup.childContacts="+actualRoot.countChildContacts()
                     + " rootGroup.childGroups="+actualRoot.countSubgroups()
                     + " Printing rootGroupContents=\n"+actualRoot.toString());

        MclSlickFixture.assertGroupEquals(expectedRoot, actualRoot
            , false);//there's no reason to have empty meta groups here so
                     //they are to be considered a problem
    }

    /**
     * Verifies whether contacts are properly ordered according to their
     * current status and name Checks whether reordered events are issued
     * once a contact inside this group changes its status or is added
     * removed a contact.
     */
    public void testContactsOrder()
    {
        //first assert initial order.
        assertContactsOrder(fixture.metaClService.getRoot());

        MclEventCollector evtCollector = new MclEventCollector();

        //change a status
        fixture.metaClService.addMetaContactListListener(evtCollector);

        MclSlickFixture.mockPresOpSet.changePresenceStatusForContact(
                MetaContactListServiceLick.mockContactToReorder,
                MockStatusEnum.MOCK_STATUS_100);

        fixture.metaClService.removeMetaContactListListener(evtCollector);

        //make sure that the order didn't change
        assertEquals("Number of evts dispatched after a contact changed its status"
                     , 1
                     , evtCollector.collectedMetaContactGroupEvents.size());
        MetaContactGroupEvent evt = (MetaContactGroupEvent)evtCollector
                                    .collectedMetaContactGroupEvents.remove(0);

        assertEquals("ID of the generated event",
                     MetaContactGroupEvent.CHILD_CONTACTS_REORDERED,
                     evt.getEventID());

        assertEquals("Source meta contact."
                     , fixture.metaClService.getRoot()
                     , evt.getSourceMetaContactGroup());

        //then check the general order
        assertContactsOrder(fixture.metaClService.getRoot());

        //restore the contacts original status
        MclSlickFixture.mockPresOpSet.changePresenceStatusForContact(
                MetaContactListServiceLick.mockContactToReorder,
                MockStatusEnum.MOCK_STATUS_00);


        //repeat order tests but this time after changing the display name of a
        //contact.
        fixture.metaClService.addMetaContactListListener(evtCollector);
        MetaContact theReorderedContact = fixture.metaClService
            .findMetaContactByContact(MetaContactListServiceLick.mockContactToReorder);
        fixture.metaClService.renameMetaContact(theReorderedContact, "zzzzzz");

        fixture.metaClService.removeMetaContactListListener(evtCollector);

        //check whether a reordered event is dispatched
        assertEquals("Number of evts dispatched after a contact changed its "
                     +"display name"
                     , 1
                     , evtCollector.collectedMetaContactGroupEvents.size());
        evt = (MetaContactGroupEvent)evtCollector
                    .collectedMetaContactGroupEvents.remove(0);

        assertEquals("ID of the generated event",
                     MetaContactGroupEvent.CHILD_CONTACTS_REORDERED,
                     evt.getEventID());

        assertEquals("Source meta contact."
                     , fixture.metaClService.getRoot()
                     , evt.getSourceMetaContactGroup());

        //then check wether the contact is has been moved to the bottom of the list
        assertSame(MetaContactListServiceLick.mockContactToReorder
                   + " was not moved to the bottom of the list after being "
                   +"assigned 00 status and a heavy name."
                   , theReorderedContact
                   , fixture.metaClService.getRoot().getMetaContact(
                        fixture.metaClService.getRoot().countChildContacts()-1));
    }

    /**
     * Makes sure that all child contacs (both direct and children of subgroups)
     * are properly ordered.
     * @param group a ref to the <tt>MetaContactGroup</tt> where we'd like to
     * confirm order.
     */
    public void assertContactsOrder(MetaContactGroup group)
    {
        //first check order of contacts in this group
        Iterator<MetaContact> contacts = group.getChildContacts();

        MetaContact previousContact = null;
        while(contacts.hasNext())
        {
            MetaContact currentContact  = contacts.next();

            if (previousContact != null)
            {
                assertTrue( previousContact
                            + " was wrongfully before "
                            + currentContact
                            , previousContact.compareTo(currentContact) <= 0);
            }
            previousContact = currentContact;
        }

        //now go over the subgroups
        Iterator<MetaContactGroup> subgroups = group.getSubgroups();

        while(subgroups.hasNext())
        {
            assertContactsOrder(subgroups.next());
        }
    }

    /**
     * Performs several tests in order to verify that the findMetaContactByID
     * method of the tested implementation is working properly. We'll first
     * try to locate by using iterators a couple of contacts in different
     * levels. Once e get their references, we'll try to locate them through
     * the findMetaContactByMetaUID method.
     */
    public void testFindMetaContactByMetaUID()
    {
        MetaContactGroup root = fixture.metaClService.getRoot();

        //get a top level contact and then try to find it through the tested
        //findMetaContactByMetaUID method.
        Iterator<MetaContact> contactsIter = root.getChildContacts();

        assertTrue(
            "No contacts were found in the meta contact list"
            , contactsIter.hasNext());

        MetaContact expectedContact = contactsIter.next();

        MetaContact actualResult = fixture.metaClService
            .findMetaContactByMetaUID(expectedContact.getMetaUID());

        assertEquals("find failed for contact "+expectedContact.getDisplayName()
                     , expectedContact, actualResult);

        // get one of the subgroups, extract one of its child contacts and
        // repeat the same test.
        Iterator<MetaContactGroup> subgroupsIter = root.getSubgroups();

        assertTrue(
            "No sub groups were found in the meta contact list"
            , subgroupsIter.hasNext());

        MetaContactGroup subgroup = subgroupsIter.next();

        contactsIter = subgroup.getChildContacts();

        assertTrue(
            "No contacts were found in the meta group: "
            + subgroup.getGroupName()
            , contactsIter.hasNext());

        expectedContact = contactsIter.next();

        actualResult = fixture.metaClService
            .findMetaContactByMetaUID(expectedContact.getMetaUID());

        assertEquals("find failed for contact "+expectedContact.getDisplayName()
                     , expectedContact, actualResult);
    }

    /**
     * Performs several tests in order to verify that the findMetaContactByContact
     * method of the tested implementation is working properly. We'll first
     * try to locate by using iterators a couple of contacts in different
     * levels. Once we get their references, we'll try to locate them through
     * the findMetaContactByContact method.
     */
    public void testFindMetaContactByContact()
    {
        MetaContactGroup root = fixture.metaClService.getRoot();

        //get a top level contact and then try to find it through the tested
        //findMetaContactByContact method.
        Iterator<MetaContact> contactsIter = root.getChildContacts();

        assertTrue(
            "No contacts were found in the meta contact list"
            , contactsIter.hasNext());

        MetaContact expectedMetaContact = contactsIter.next();

        assertTrue(
            "No contacts are encapsulated by MetaContact: "
            + expectedMetaContact.getDisplayName()
            , expectedMetaContact.getContacts().hasNext());


        Contact mockContact = expectedMetaContact.getContacts().next();

        MetaContact actualResult = fixture.metaClService
                                .findMetaContactByContact(mockContact);

        assertEquals("find failed for contact "+expectedMetaContact.getDisplayName()
                     , expectedMetaContact, actualResult);

        // get one of the subgroups, extract one of its child contacts and
        // repeat the same test.
        Iterator<MetaContactGroup> subgroupsIter = root.getSubgroups();

        assertTrue(
            "No sub groups were found in the meta contact list"
            , subgroupsIter.hasNext());

        MetaContactGroup subgroup = subgroupsIter.next();

        contactsIter = subgroup.getChildContacts();

        assertTrue(
            "No contacts were found in MetaContactGroup: "
            + subgroup.getGroupName()
            , contactsIter.hasNext());


        expectedMetaContact = contactsIter.next();

        assertTrue(
            "No contacts were encapsulated by meta contact: "
            + expectedMetaContact.getDisplayName()
            , expectedMetaContact.getContacts().hasNext());


        mockContact = expectedMetaContact.getContacts().next();

        actualResult = fixture.metaClService
            .findMetaContactByContact(mockContact);

        assertEquals("find failed for contact "
                     + expectedMetaContact.getDisplayName()
                     , expectedMetaContact, actualResult);

    }

    /**
     * Performs several tests in order to verify that the
     * <tt>findMetaContactGroupByContactGroup</tt>  method of the tested
     * implementation is working properly. We'll first try to locate by using
     * iterators a couple of protocol specific groups in different levels.
     * Once we get their references, we'll try to locate them through
     * the findMetaContactGroupByContactGroup method.
     */
    public void testFindMetaContactGroupByContactGroup()
    {
        MetaContactGroup root = fixture.metaClService.getRoot();

        //get a group, extract its proto group and then try to obtain a
        //reference through the tested find method.
        Iterator<MetaContactGroup> groupsIter = root.getSubgroups();

        assertTrue(
            "No sub groups were found in the meta contact list"
            , groupsIter.hasNext());

        MetaContactGroup expectedMetaContactGroup = groupsIter.next();

        assertTrue(
            "There were no contact groups encapsulated in MetaContactGroup: "
            + expectedMetaContactGroup
            , expectedMetaContactGroup.getContactGroups().hasNext());

        assertTrue(
            "No ContactGroups are encapsulated by MetaContactGroup: "
            + expectedMetaContactGroup
            , expectedMetaContactGroup.getContactGroups().hasNext());

        ContactGroup mockContactGroup = expectedMetaContactGroup
                                                    .getContactGroups().next();
        MetaContactGroup actualMetaContactGroup = fixture.metaClService
            .findMetaContactGroupByContactGroup(mockContactGroup);

        assertSame("find failed for contact group " + mockContactGroup
                   , expectedMetaContactGroup, actualMetaContactGroup);

        //repeat the same tests for the root group as it seems to be causing
        //problems.
        actualMetaContactGroup = fixture.metaClService
            .findMetaContactGroupByContactGroup(
                    MclSlickFixture.mockPresOpSet.getServerStoredContactListRoot());

        assertSame("find failed for root contact group "
                   , root, actualMetaContactGroup);
    }


    /**
     * In this test we'll add and remove users to the mock provider, and check
     * whether the meta contact list dispatches the corresponding meta contact
     * list event.
     *
     * @throws java.lang.Exception if anything goes wrong.
     */
    public void testSubscriptionHandling() throws Exception
    {
        //add a subscription and check that the corresponding event is generated
        MclEventCollector mclEvtCollector = new MclEventCollector();

        fixture.metaClService.addMetaContactListListener(mclEvtCollector);
        MclSlickFixture.mockPresOpSet.subscribe(newSubscriptionName);

        fixture.metaClService.removeMetaContactListListener(mclEvtCollector);

        //first check that the newly created contact was really added
        MockContact newProtoContact = (MockContact)MclSlickFixture.mockPresOpSet
                                        .findContactByID(newSubscriptionName);
        MetaContact newMetaContact = fixture.metaClService
            .findMetaContactByContact(newProtoContact);

        assertNotNull("The meta contact list was not updated after adding "
                      +"contact "+ newProtoContact +" to the mock provider."
                      , newMetaContact);

        assertEquals("Number of evts dispatched while adding a contact"
                     , 1
                     , mclEvtCollector.collectedMetaContactEvents.size());
        MetaContactEvent evt = (MetaContactEvent)mclEvtCollector
                                                    .collectedMetaContactEvents.remove(0);

        assertEquals("ID of the generated event",
                     MetaContactEvent.META_CONTACT_ADDED,
                     evt.getEventID());

        assertEquals("Parent group of the source contact"
                    , fixture.metaClService.getRoot()
                    , evt.getParentGroup());

        assertEquals("Source meta contact."
                     , newMetaContact, evt.getSourceMetaContact());

        fixture.metaClService.addMetaContactListListener(mclEvtCollector);

        MclSlickFixture.mockPresOpSet.unsubscribe(newProtoContact);

        fixture.metaClService.removeMetaContactListListener(mclEvtCollector);

        //first check that the newly created contact was really added
        assertNull(
            "The impl contact list did not update after a subscr. was removed."
            ,fixture.metaClService.findMetaContactByContact(newProtoContact));

        assertEquals("Number of evts dispatched while adding a contact"
                     , 1
                     , mclEvtCollector.collectedMetaContactEvents.size());
        evt = (MetaContactEvent)mclEvtCollector.collectedMetaContactEvents.remove(0);

        assertEquals("ID of the generated event",
                     MetaContactEvent.META_CONTACT_REMOVED,
                     evt.getEventID());

        assertEquals("Parent group of the source contact"
                    , fixture.metaClService.getRoot()
                    , evt.getParentGroup());

        assertEquals("Source meta contact."
                     , newMetaContact, evt.getSourceMetaContact());

        MclSlickFixture.mockPresOpSet.createVolatileContact(
            newSubscriptionName + "1");
        MclSlickFixture.mockPresOpSet.createVolatileContact(
            newSubscriptionName + "2");
        // and now clear volatile
        MclSlickFixture.mockPresOpSet.removeServerStoredContactGroup(
            MclSlickFixture.mockPresOpSet.getNonPersistentGroup());
    }


    /**
     * In this test we'll add and remove groups to the mock provider, and check
     * whether the meta contact list dispatches the corresponding meta contact
     * list events and whether it gets properly updated.
     *
     * @throws java.lang.Exception if anything goes wrong.
     */
    public void testGroupChangeEventHandling() throws Exception
    {
        String newGroupName = "testGroupChangeEventHandling.NewContactGroup";
        String newInnerGroupName
            = "testGroupChangeEventHandling.NewInnderContactGroup";
        //add 2 nested groups and check for the event
        MclEventCollector mclEvtCollector = new MclEventCollector();

        MockContactGroup newContactGroup
            = new MockContactGroup(newGroupName, MclSlickFixture.mockProvider);
        MockContactGroup newInnerContactGroup
            = new MockContactGroup(newInnerGroupName, MclSlickFixture.mockProvider);

        newContactGroup.addSubgroup(newInnerContactGroup);

        fixture.metaClService.addMetaContactListListener(mclEvtCollector);
        MclSlickFixture.mockPresOpSet.addMockGroupAndFireEvent(
            (MockContactGroup)
                MclSlickFixture.mockPresOpSet.getServerStoredContactListRoot(),
            newContactGroup);

        fixture.metaClService.removeMetaContactListListener(mclEvtCollector);

        // first check whether event delivery went ok.
        assertEquals("Number of evts dispatched while adding a contact group"
                     , 1
                     , mclEvtCollector.collectedMetaContactGroupEvents.size());
        MetaContactGroupEvent evt = (MetaContactGroupEvent)mclEvtCollector
                                    .collectedMetaContactGroupEvents.remove(0);

        assertEquals("ID of the generated event",
                     MetaContactGroupEvent.META_CONTACT_GROUP_ADDED,
                     evt.getEventID());

        assertEquals("Name of the source group of the AddEvent."
                    , newContactGroup.getGroupName()
                    , evt.getSourceMetaContactGroup().getGroupName());

        MetaContactGroup newMetaGroup = evt.getSourceMetaContactGroup();

        assertSame("Contact group in the newly added meta group."
                    , newContactGroup
                    , newMetaGroup.getContactGroup(
                                            newContactGroup.getGroupName()
                                            , MclSlickFixture.mockProvider));

        assertEquals("Subgroups were not imported in the MetaContactList."
                    , newContactGroup.countSubgroups()
                    , evt.getSourceMetaContactGroup().countSubgroups());

        //first check that the newly created group was really added
        assertEquals("Source provider for the add event."
                     , MclSlickFixture.mockProvider, evt.getSourceProvider());

        ContactGroup newProtoGroup = newMetaGroup.getContactGroup(
                                            newGroupName, MclSlickFixture.mockProvider);

        assertNotNull("The new meta contact group did not contain a proto group"
                    , newProtoGroup);

        assertEquals("The new meta contact group did not seem to contain "
                     + "the right protocol contact group."
                     , newProtoGroup.getGroupName()
                     , newGroupName);

        assertEquals("The new meta contact group did not seem to contain "
                     + "the right protocol contact group."
                     , newProtoGroup.getProtocolProvider()
                     , MclSlickFixture.mockProvider);

        //rename the group and see that the corresponding events are handled
        //properly
        fixture.metaClService.addMetaContactListListener(mclEvtCollector);
        MclSlickFixture.mockPresOpSet.renameServerStoredContactGroup(
                                            newProtoGroup, renamedGroupName);

        fixture.metaClService.removeMetaContactListListener(mclEvtCollector);

        //first check that the group was really renamed
        assertEquals("Number of evts dispatched while renaming a contact group"
                     , 1
                     , mclEvtCollector.collectedMetaContactGroupEvents.size());

        evt = (MetaContactGroupEvent)mclEvtCollector
                                    .collectedMetaContactGroupEvents.remove(0);

        assertEquals("ID of the generated event",
                     MetaContactGroupEvent.CONTACT_GROUP_RENAMED_IN_META_GROUP,
                     evt.getEventID());

        assertEquals("Source group for the RemoveEvent."
                    , newMetaGroup
                    , evt.getSourceMetaContactGroup());

        assertEquals("Source provider for the remove event."
                     , MclSlickFixture.mockProvider, evt.getSourceProvider());

        //check whether the group was indeed renamed.
        Iterator<ContactGroup> groupsIter = evt.getSourceMetaContactGroup()
            .getContactGroupsForProvider(MclSlickFixture.mockProvider);

        assertTrue("A proto group was unexplicably removed after renaming.",
                   groupsIter.hasNext());

        assertEquals("The name of a protocol group after renaming."
                     , renamedGroupName
                     , groupsIter.next().getGroupName());

        //remove the group and check for the event.
        fixture.metaClService.addMetaContactListListener(mclEvtCollector);
        MclSlickFixture.mockPresOpSet.removeServerStoredContactGroup(newProtoGroup);

        fixture.metaClService.removeMetaContactListListener(mclEvtCollector);

        //first check that the group was really removed
        assertTrue("Number of evts dispatched while removing a contact group"
                , mclEvtCollector.collectedMetaContactGroupEvents.size() > 0);
        evt = (MetaContactGroupEvent)mclEvtCollector
                .collectedMetaContactGroupEvents.get(
                    mclEvtCollector
                        .collectedMetaContactGroupEvents.size() - 2);
        mclEvtCollector.collectedMetaContactGroupEvents.clear();

        assertEquals("ID of the generated event",
                     evt.getEventID(),
                     MetaContactGroupEvent.CONTACT_GROUP_REMOVED_FROM_META_GROUP);

        assertEquals("Source group for the RemoveEvent."
                    , newMetaGroup
                    , evt.getSourceMetaContactGroup());

        assertEquals("Source provider for the remove event."
                     , MclSlickFixture.mockProvider, evt.getSourceProvider());
    }

    /**
     * Perform manipulations of moving protocol contacts in and outside of a
     * meta contact and verify that they complete properly.
     */
    public void testAddMoveRemoveContactToMetaContact()
    {
        String newContactID = "TestyPesty";
        //get a ref to 2 contacts the we will experiment with.
        MetaContact metaContact = fixture.metaClService.getRoot()
                                                            .getMetaContact(0);
        MetaContact dstMetaContact = fixture.metaClService.getRoot()
                                                            .getMetaContact(1);

        MclEventCollector evtCollector = new MclEventCollector();
        fixture.metaClService.addMetaContactListListener(evtCollector);

        //add a new mock contact to a meta contact
        fixture.metaClService.addNewContactToMetaContact(
            MclSlickFixture.mockProvider
            , metaContact
            , newContactID);

        fixture.metaClService.removeMetaContactListListener(evtCollector);

        //verify that the contact has been added to the meta contact.
        assertEquals("Dest. meta Contact did not seem to contain an "
                     +"extra proto contact."
                     , 2
                     , metaContact.getContactCount());

        MockContact newContact = (MockContact)metaContact
                                .getContact(newContactID, MclSlickFixture.mockProvider);

        assertNotNull("newContact", newContact);

        //verify that a mock contact has been created in the mock contact list.
        //and that it is the same as the one added in the MetaContact
        assertSame("Proto specific contact in mock contact list."
                   , newContact
                   , MclSlickFixture.mockPresOpSet.getServerStoredContactListRoot()
                            .getContact(newContactID));

        //verify that events have been properly delivered.
        assertEquals("Events delivered while adding a new contact to a "
                   + "meta contact", 1, evtCollector.collectedMetaContactEvents.size());

        ProtoContactEvent event = (ProtoContactEvent)evtCollector
            .collectedMetaContactEvents.remove(0);

        assertSame ( "Source contact in ProtoContactEvent gen. upon add."
                     , newContact , event.getProtoContact());

        assertSame ( "Source provider in ProtoContactEvent gen. upon add."
             , MclSlickFixture.mockProvider
             , event.getProtoContact().getProtocolProvider());

        assertEquals ( "Event ID in MetaContactEvent gen. upon add."
                     , ProtoContactEvent.PROTO_CONTACT_ADDED
                     , event.getPropertyName());

        //move the mock contact to another meta contact
        fixture.metaClService.addMetaContactListListener(evtCollector);

        fixture.metaClService.moveContact(newContact, dstMetaContact);

        fixture.metaClService.removeMetaContactListListener(evtCollector);

        //verify that the old meta contact does not contain it anymore.
        assertEquals("Orig. Meta Contact did not seem restored after removing "
                     +"the newly added contact."
                     , 1
                     , metaContact.getContactCount());

        //verify that the new meta contact contains it.
        assertEquals("A Meta Contact did not seem updated after moving a "
                     +"contact inside it."
                     , 2
                     , dstMetaContact.getContactCount());

        newContact = (MockContact)dstMetaContact
                                .getContact(newContactID, MclSlickFixture.mockProvider);

        assertNotNull("newContact", newContact);

        //verify that events have been properly delivered.
        assertEquals("Events delivered while adding a moving a proto contact. "
                     , 1, evtCollector.collectedMetaContactEvents.size());

        event = (ProtoContactEvent) evtCollector.collectedMetaContactEvents.remove(0);

        assertSame("Source contact in ProtoContactEvent gen. upon move."
                   , newContact, event.getProtoContact());

        assertSame("Parent meta contact in ProtoContactEvent gen. upon move."
                   , dstMetaContact, event.getParent());

        assertSame("Source provider in ProtoContactEvent gen. upon move."
                   , MclSlickFixture.mockProvider
                   , event.getProtoContact().getProtocolProvider());

        assertEquals("Event ID in ProtoContactEvent gen. upon add."
                     , ProtoContactEvent.PROTO_CONTACT_MOVED
                     , event.getPropertyName());

        //remove the meta contact
        fixture.metaClService.addMetaContactListListener(evtCollector);

        fixture.metaClService.removeContact(newContact);

        fixture.metaClService.removeMetaContactListListener(evtCollector);

        //verify that it is no more in the meta contact
        assertEquals("Dest. Meta Contact did not seem restored after removing "
                     +"the newly added contact."
                     , 1
                     , dstMetaContact.getContactCount());

        //verify that it is no more in the mock contact list
        assertNull( "The MetaContactList did not remove a contact from the "
                    + "MockList on del."
                    , MclSlickFixture.mockPresOpSet.getServerStoredContactListRoot()
                        .getContact(newContactID));

        //verify that events have been properly delivered.
        assertEquals("Events delivered while adding a new contact to a "
                      +"meta contact", 1, evtCollector.collectedMetaContactEvents.size());

        event = (ProtoContactEvent)evtCollector
            .collectedMetaContactEvents.remove(0);

        assertSame ( "Source contact in ProtoContactEvent gen. upon remove."
                     , newContact, event.getProtoContact());

        assertSame ( "Parent meta contact in ProtoContactEvent gen. upon remove."
                     , dstMetaContact, event.getParent());

        assertSame ( "Source provider in ProtoContactEvent gen. upon remove."
                     , MclSlickFixture.mockProvider
                     , event.getProtoContact().getProtocolProvider());

        assertEquals ( "Event ID in ProtoContactEvent gen. upon remove."
                       , ProtoContactEvent.PROTO_CONTACT_REMOVED
                       , event.getPropertyName());
    }

    /**
     * Tests methods for creating moving and removing meta contacts.
     */
    public void testCreateMoveRemoveMetaContact()
    {
        String newContactID ="testCreateMoveRemoveMetaContact.ContactID";
        MetaContactGroup parentMetaGroup = fixture.metaClService.getRoot()
            .getMetaContactSubgroup(MetaContactListServiceLick.topLevelGroupName);

        MclEventCollector evtCollector = new MclEventCollector();

        fixture.metaClService.addMetaContactListListener(evtCollector);
        //create a new metacontact and, hence mock contact, in the meta
        //"SomePeople" non-toplevel group
        fixture.metaClService.createMetaContact(MclSlickFixture.mockProvider
            , parentMetaGroup
            , newContactID);

        fixture.metaClService.removeMetaContactListListener(evtCollector);

        //check that the contact has been successfully created in the meta cl
        MetaContact newMetaContact  =
            parentMetaGroup.getMetaContact(MclSlickFixture.mockProvider, newContactID);

        assertNotNull("create failed. couldn't find the new contact."
            , newMetaContact);

        //check that the contact has been successfully created in the mock cl
        assertEquals("create() created a meta contact with the wrong name."
            , newContactID, newMetaContact.getDisplayName());

        //verify that events have been properly delivered.
        assertEquals("Events delivered while creating a new meta contact"
                     , 1,  evtCollector.collectedMetaContactEvents.size());

        MetaContactEvent event = (MetaContactEvent)evtCollector
            .collectedMetaContactEvents.remove(0);

        assertSame ( "Source contact in MetaContactEvent gen. upon create."
                     , newMetaContact, event.getSourceMetaContact());

        assertEquals ( "Event ID in MetaContactEvent gen. upon create."
                       , MetaContactEvent.META_CONTACT_ADDED
                       , event.getEventID());

        //move the meta contact somewhere else
        fixture.metaClService.addMetaContactListListener(evtCollector);

        fixture.metaClService.moveMetaContact(
            newMetaContact, fixture.metaClService.getRoot());

        fixture.metaClService.removeMetaContactListListener(evtCollector);

        //check that the meta contact has moved.
        assertNull(newMetaContact.getDisplayName()
               + " was still in its old location after moving it."
               ,parentMetaGroup.getMetaContact( newMetaContact.getMetaUID()));

        assertNotNull(newMetaContact.getDisplayName()
                   + " was not in the new location after moving it."
                   ,fixture.metaClService.getRoot()
                        .getMetaContact(newMetaContact.getMetaUID()));

        //check that the mock contact has moved as well.
        assertNull("The mock contact corresponding to: "
                   + newMetaContact.getDisplayName()
                   + " was still in its old location after its "
                   +"encapsulating meta contact was moved"
                   ,MetaContactListServiceLick.topLevelMockGroup
                        .getContact(newContactID));

        //assert that the mock contact has indeed moved to its new parent.
        assertNotNull("The mock contact corresponding to: "
                   + newMetaContact.getDisplayName()
                   + " was not moved to its new location after its "
                   +"encapsulating meta contact was."
                   ,MclSlickFixture.mockPresOpSet.getServerStoredContactListRoot()
                        .getContact(newContactID));

        //verify that events have been properly delivered.
        assertEquals("Events delivered while moving a meta contact"
                     , 1,  evtCollector.collectedMetaContactEvents.size());

        MetaContactMovedEvent movedEvent = (MetaContactMovedEvent)evtCollector
            .collectedMetaContactEvents.remove(0);

        assertSame ( "Source contact in MetaContactEvent gen. upon move."
                     , newMetaContact, movedEvent.getSourceMetaContact());

        assertEquals ( "Event Property Name in MetaContactEvent gen. upon move."
                       , MetaContactPropertyChangeEvent.META_CONTACT_MOVED
                       , movedEvent.getPropertyName());

        assertEquals ( "Old Parent in MetaContactEvent gen. upon move."
               , parentMetaGroup
               , movedEvent.getOldParent());

        assertEquals ( "Old Parent in MetaContactEvent gen. upon move."
               , fixture.metaClService.getRoot()
               , movedEvent.getNewParent());

        //remove the contact
        fixture.metaClService.addMetaContactListListener(evtCollector);

        fixture.metaClService.removeMetaContact(newMetaContact);

        fixture.metaClService.removeMetaContactListListener(evtCollector);

        //check that the meta contact has been removed.
        assertNull(newMetaContact.getDisplayName()
               + " was still in its old location after it was removed."
               ,fixture.metaClService.getRoot().getMetaContact(
                   newMetaContact.getMetaUID()));


        //check that the mock contact has been removed.
        assertNull("The mock contact corresponding to: "
                   + newMetaContact.getDisplayName()
                   + " was not removed after its encapsulating meta contact was."
                   ,MclSlickFixture.mockPresOpSet.getServerStoredContactListRoot()
                        .getContact(newContactID));

        //verify that events have been properly delivered.
        assertEquals("Events delivered while removing a meta contact"
                     , 1,  evtCollector.collectedMetaContactEvents.size());

        event = (MetaContactEvent)evtCollector
            .collectedMetaContactEvents.remove(0);

        assertSame ( "Source contact in MetaContactEvent gen. upon remove."
                     , newMetaContact, event.getSourceMetaContact());

        assertEquals ( "Event ID in MetaContactEvent gen. upon remove."
                       , MetaContactEvent.META_CONTACT_REMOVED
                       , event.getEventID());
    }

    /**
     * Tests operations on meta groups.
     */
    public void testCreateRenameRemoveMetaContactGroup()
    {
        String newGroupName = "testCRRMetaContactGroup.NewContactGroup";
        String newContactID = "testCRRMetaContactGroup.NewContactID";

        //create a new meta contact group
        fixture.metaClService.createMetaContactGroup(
            fixture.metaClService.getRoot(), newGroupName);

        //check that the group exists in the meta contact list but not yet in
        //the mock provider
        MetaContactGroup newMetaGroup = fixture.metaClService.getRoot()
                .getMetaContactSubgroup(newGroupName);
        assertNotNull(
            "createMetaContactGroup failed - no group was created."
            , newMetaGroup);

        assertNull(
            "createMetaContactGroup tried to create a proto group too early."
            ,MclSlickFixture.mockPresOpSet.getServerStoredContactListRoot()
                .getGroup(newGroupName));

        //create a mock contcat through the meta contact list.
        fixture.metaClService.createMetaContact(
            MclSlickFixture.mockProvider, newMetaGroup, newContactID);

        //check that the mock group was created and added to the right meta grp.
        MockContactGroup newMockGroup = (MockContactGroup)MclSlickFixture.mockPresOpSet
            .getServerStoredContactListRoot().getGroup(newGroupName);

        assertNotNull(
            "createMetaContact did not create a parent proto group "
            + "when it had to."
            , newMockGroup);
        assertSame(
            "createMetaContact created a proto group but did not add it to the "
            + "right meta contact group."
            , newMockGroup
            , newMetaGroup.getContactGroup(newGroupName, MclSlickFixture.mockProvider));

        //check that the contact was added
        MetaContact newMetaContact = newMetaGroup
            .getMetaContact(MclSlickFixture.mockProvider, newContactID);

        assertNotNull("createMetaContact failed", newMetaContact);

        //rename the meta contact group
        String renamedGroupName = "new" + newGroupName;
        fixture.metaClService.renameMetaContactGroup(newMetaGroup,
                                                     renamedGroupName);

        //check that the meta group changed its name.
        assertEquals ( "renameMetaContactGroup failed"
                       , newMetaGroup.getGroupName(), renamedGroupName);

        //check that the mock group did not change name
        assertEquals("renameMetaContactGroup didn't renamed a proto group!"
            , newMockGroup.getGroupName(), renamedGroupName);

        //remove the meta contact group
        fixture.metaClService.removeMetaContactGroup(newMetaGroup);

        //check that the meta group is removed
        assertNull(
            "removeMetaContactGroup failed - group not removed."
            , fixture.metaClService.getRoot()
                .getMetaContactSubgroup(newGroupName));


        //check that the mock group is removed
        assertNull(
            "removeMetaContact did not remove the corresp. proto group."
            , MclSlickFixture.mockPresOpSet.getServerStoredContactListRoot()
                                                    .getGroup(newGroupName));
    }

    /**
     * Tests the MetaContactListService.findParentMetaContactGroup(MetaContact)
     * method for two different meta contacts.
     */
    public void testFindParentMetaContactGroup()
    {
        MetaContact metaContact1 = fixture.metaClService
            .findMetaContactByContact(MetaContactListServiceLick
                                      .subLevelContact);
        MetaContact metaContact2 = fixture.metaClService
            .findMetaContactByContact(MetaContactListServiceLick.subsubContact);

        //do testing for the first contact
        MetaContactGroup metaGroup = fixture.metaClService
            .findParentMetaContactGroup(metaContact1);

        assertNotNull("find failed for contact " + metaContact1, metaGroup);
        assertEquals("find failed (wrong group) for contact "
                     + metaContact1.getDisplayName()
                     , MetaContactListServiceLick.topLevelGroupName
                     , metaGroup.getGroupName());

        //do testing for the first contact
        metaGroup = fixture.metaClService.findParentMetaContactGroup(metaContact2);

        assertNotNull("find failed for contact " + metaContact2, metaGroup);
        assertEquals("find failed (wrong group) for contact "
                     + metaContact2.getDisplayName()
                     , MetaContactListServiceLick.subLevelGroup.getGroupName()
                     , metaGroup.getGroupName());
    }

    /**
     * Renames a contact in the contact list and verifies whether the new name
     * has taken effect and whether the corresponding event has been dispatched.
     */
    public void testRenameMetaContact()
    {
        String newName = "testRenameMetaContact.AyNewName";
        MockContact mockContact
            = MetaContactListServiceLick.mockContactToRename;

        //keep the name to later verify that it's untouched.
        String oldMockContactDisplayName = mockContact.getDisplayName();

        MetaContact contactToRename
            = fixture.metaClService.findMetaContactByContact(mockContact);

        MclEventCollector evtCollector = new MclEventCollector();

        //rename the meta contact
        fixture.metaClService.addMetaContactListListener(evtCollector);
        fixture.metaClService.renameMetaContact(contactToRename, newName);
        fixture.metaClService.removeMetaContactListListener(evtCollector);

        //check that an event has been dispatched
        assertEquals("Events delivered while renaming a meta contact"
                     , 1,  evtCollector.collectedMetaContactEvents.size());

        MetaContactRenamedEvent event = (MetaContactRenamedEvent)evtCollector
            .collectedMetaContactEvents.remove(0);

        assertSame ( "Source contact in MetaContactRenamedEvent gen. upon remove."
                     , contactToRename, event.getSourceMetaContact());

        assertEquals ( "Event ID in MetaContactEvent gen. upon remove."
                       , MetaContactPropertyChangeEvent.META_CONTACT_RENAMED
                       , event.getPropertyName());

        //check that the meta contact has been renamed
        assertEquals( "DisplayName of a MetaContact unchanged after renaming"
                     , newName
                     , contactToRename.getDisplayName() );

        //verify that the underlying mock contact has not been changed
        assertEquals( "Proto Contact modified after renaming a MetaContact"
                     , oldMockContactDisplayName
                     , mockContact.getDisplayName() );

    }

    /**
     * Tests the MetaContactListService
     *             .findParentMetaContactGroup(MetaContactGroup)
     * method for two different meta contact groups.
     */
    public void testFindParentMetaContactGroup2()
    {
        MetaContactGroup metaContactGroup1 = fixture.metaClService
            .findMetaContactGroupByContactGroup(MetaContactListServiceLick
                                      .topLevelMockGroup);
        MetaContactGroup metaContactGroup2 = fixture.metaClService
            .findMetaContactGroupByContactGroup(MetaContactListServiceLick
                                      .subLevelGroup);

        //do testing for the first contact
        MetaContactGroup metaGroup = fixture.metaClService
            .findParentMetaContactGroup(metaContactGroup1);

        assertNotNull("find failed for contact " + metaContactGroup1, metaGroup);
        assertEquals("find failed (wrong group) for group "
                     + metaContactGroup1.getGroupName()
                     , fixture.metaClService.getRoot().getGroupName()
                     , metaGroup.getGroupName());

        //do testing for the first contact
        metaGroup = fixture.metaClService.findParentMetaContactGroup(metaContactGroup2);

        assertNotNull("find failed for contact " + metaContactGroup2, metaGroup);
        assertEquals("find failed (wrong group) for group "
                     + metaContactGroup2.getGroupName()
                     , MetaContactListServiceLick.topLevelGroupName
                     , metaGroup.getGroupName());
    }


    private class MclEventCollector implements MetaContactListListener
    {
        public Vector<EventObject> collectedMetaContactEvents = new Vector<EventObject>();
        public Vector<EventObject> collectedMetaContactGroupEvents = new Vector<EventObject>();

        /**
         * Indicates that a MetaContact has been successfully added
         * to the MetaContact list.
         * @param evt the MetaContactListEvent containing the corresponding contact
         */
        public void metaContactAdded(MetaContactEvent evt)
        {
            collectedMetaContactEvents.add(evt);
        }


        /**
         * Indicates that a MetaContactGroup has been successfully added
         * to the MetaContact list.
         * @param evt the MetaContactListEvent containing the corresponding contact
         */
        public void metaContactGroupAdded(MetaContactGroupEvent evt)
        {
            collectedMetaContactGroupEvents.add(evt);
        }


        /**
         * Indicates that a MetaContact has been moved inside the MetaContact list.
         * @param evt the MetaContactListEvent containing the corresponding contact
         */
        public void metaContactMoved(MetaContactMovedEvent evt)
        {
            collectedMetaContactEvents.add(evt);
        }


        /**
         * Indicates that a MetaContact has been modified.
         * @param evt the MetaContactListEvent containing the corresponding contact
         */
        public void metaContactRenamed(MetaContactRenamedEvent evt)
        {
            collectedMetaContactEvents.add(evt);
        }

        /**
         * Indicates that a MetaContact has been modified.
         * @param evt the MetaContactListEvent containing the corresponding contact
         */
        public void metaContactModified(MetaContactModifiedEvent evt)
        {}

        /**
         * Indicates that a protocol specific <tt>Contact</tt> instance has been
         * moved from within one <tt>MetaContact</tt> to another.
         * @param evt a reference to the <tt>ProtoContactMovedEvent</tt> instance.
         */
        public void protoContactMoved(ProtoContactEvent evt)
        {
            collectedMetaContactEvents.add(evt);
        }

        /**
         * Implements the <tt>MetaContactListListener.protoContactModified</tt>
         * method with an empty body since we are not interested in proto contact
         * specific changes (such as the persistent data).
         */
        public void protoContactModified(ProtoContactEvent evt)
        {
            //currently ignored
        }

        /**
         * Indicates that a protocol specific <tt>Contact</tt> instance has been
         * removed from the list of protocol specific buddies in this
         * <tt>MetaContact</tt>
         * @param evt a reference to the corresponding
         * <tt>ProtoContactEvent</tt>
         */
        public void protoContactRemoved(ProtoContactEvent evt)
        {
            collectedMetaContactEvents.add(evt);
        }

        /**
         * Indicates that a MetaContactGroup has been modified (e.g. a proto contact
         * group was removed).
         *
         * @param evt the MetaContactListEvent containing the corresponding contact
         */
        public void metaContactGroupModified(MetaContactGroupEvent evt)
        {
            collectedMetaContactGroupEvents.add(evt);
        }


        /**
         * Indicates that a MetaContactGroup has been removed from the MetaContact
         * list.
         * @param evt the MetaContactListEvent containing the corresponding contact
         */
        public void metaContactGroupRemoved(MetaContactGroupEvent evt)
        {
            collectedMetaContactGroupEvents.add(evt);
        }


        /**
         * Indicates that a MetaContact has been removed from the MetaContact list.
         * @param evt the MetaContactListEvent containing the corresponding contact
         */
        public void metaContactRemoved(MetaContactEvent evt)
        {
            collectedMetaContactEvents.add(evt);
        }


        /**
         * Indicates that a protocol specific <tt>Contact</tt> instance has been
         * added to the list of protocol specific buddies in this
         * <tt>MetaContact</tt>
         * @param evt a reference to the corresponding
         * <tt>ProtoContactEvent</tt>
         */
        public void protoContactAdded(ProtoContactEvent evt)
        {
            collectedMetaContactEvents.add(evt);
        }

        /**
         * Indicates that the order under which the child contacts were ordered
         * inside the source group has changed.
         * @param evt the <tt>MetaContactGroupEvent</tt> containing details of
         * this event.
         */
        public void childContactsReordered(MetaContactGroupEvent evt)
        {
            collectedMetaContactGroupEvents.add(evt);
        }

        /**
         * Indicates that the avatar of a <tt>MetaContact</tt> has been updated.
         * @param evt the <tt>MetaContactAvatarUpdateEvent</tt> containing
         * details of this event
         */
        public void metaContactAvatarUpdated(MetaContactAvatarUpdateEvent evt)
        {
            collectedMetaContactGroupEvents.add(evt);
        }
    }
}
