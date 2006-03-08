/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.contactlist;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.slick.contactlist.mockprovider.*;
import net.java.sip.communicator.service.contactlist.event.*;
import java.util.*;

/**
 * Test meta contact list functionality such as filling in the contact list from
 * existing protocol providers, properly handling events of modified server
 * stored contact lists and modifying server stored contact lists through the
 * meta contact list service. Testing is done against the MockProvider which
 * is directly accessible throughout the tests.
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
     * The name of the new contat group  that we create during testing.
     */
    private String newGroupName = "NewContactGroup";

    /**
     * The name to use when renaming the new contat group.
     */
    private String renamedGroupName = "RenamedContactGroup";



    private static final Logger logger =
        Logger.getLogger(TestMetaContactList.class);

    private OperationSetPersistentPresence opSetPersPresence;

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
    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();

        Map supportedOperationSets =
            MclSlickFixture.mockProvider.getSupportedOperationSets();

        if ( supportedOperationSets == null
            || supportedOperationSets.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                +"this ICQ implementation. ");

        //get the operation set presence here.
        opSetPersPresence =
            (OperationSetPersistentPresence)supportedOperationSets.get(
                OperationSetPersistentPresence.class.getName());

        //if still null then the implementation doesn't offer a presence
        //operation set which is unacceptable for icq.
        if (opSetPersPresence == null)
            throw new NullPointerException(
                "An implementation of the ICQ service must provide an "
                + "implementation of at least the one of the Presence "
                + "Operation Sets");
    }

    /**
     * Finalization
     * @throws Exception in case sth goes wrong.
     */
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
        MockContactGroup expectedRoot = (MockContactGroup)opSetPersPresence
                                            .getServerStoredContactListRoot();

        logger.debug("============== Predefined contact List ==============");

        logger.debug("rootGroup="+expectedRoot.getGroupName()
                +" rootGroup.childContacts="+expectedRoot.countContacts()
                + "rootGroup.childGroups="+expectedRoot.countSubgroups()
                + " Printing rootGroupContents=\n"+expectedRoot.toString());

        MetaContactGroup actualRoot = fixture.metaClService.getRoot();

        logger.debug("================ Meta Contact List =================");

        logger.debug("rootGroup="+actualRoot.getGroupName()
                     +" rootGroup.childContacts="+actualRoot.countChildContacts()
                     + " rootGroup.childGroups="+actualRoot.countSubgroups()
                     + " Printing rootGroupContents=\n"+actualRoot.toString());

        assertGroupEquals(expectedRoot, actualRoot);
    }

    /**
     * Makes sure that the specified actualGroup contains the same contacts
     * and subgroups as the expectedGroup. (Method operates recursively).
     * @param expectedGroup a MockContactGroup instance used as a reference.
     * @param actualGroup the MetaContactGroup retrieved from the metacontact
     * list.
     */
    private void assertGroupEquals(MockContactGroup expectedGroup,
                                   MetaContactGroup actualGroup)
    {
        assertNotNull("Group " + expectedGroup.getGroupName() + " was "
                      + "returned by the MetaContactListService implementation "
                      + "but was not in the expected contact list."
                      , actualGroup);

        assertEquals("Group " + expectedGroup.getGroupName()
                     + " did not have the expected number of member contacts"
                     , expectedGroup.countContacts()
                     , actualGroup.countChildContacts());

        assertEquals("Group " + expectedGroup.getGroupName()
                     + " did not have the expected number of member contacts"
                     , expectedGroup.countContacts()
                     , actualGroup.countChildContacts());
        assertEquals("Group " + expectedGroup.getGroupName()
                     + " did not have the expected number of sub groups"
                     , expectedGroup.countSubgroups()
                     , actualGroup.countSubgroups());

        //go over the subgroups and check that they've been all added to the
        //meta contact list.
        Iterator expectedSubgroups = expectedGroup.subGroups();
        while (expectedSubgroups.hasNext() ){
            MockContactGroup expectedSubGroup
                = (MockContactGroup)expectedSubgroups.next();

            MetaContactGroup actualSubGroup
                = actualGroup
                    .getMetaContactSubgroup(expectedSubGroup.getGroupName());

            assertGroupEquals(expectedSubGroup, actualSubGroup);
        }

        Iterator actualContactsIter = actualGroup.getChildContacts();

        //check whether every contact in the meta list exists in the source
        //mock provider contact list.
        while (actualContactsIter.hasNext())
        {
            MetaContact actualMetaContact
                = (MetaContact) actualContactsIter.next();

            assertEquals("Number of protocol specific contacts in a MetaContact"
                          , 1, actualMetaContact.getContactCount());

            assertTrue(
                "No contacts were encapsulated by MetaContact: "
                + actualMetaContact
                , actualMetaContact.getContacts().hasNext());

            Contact actualProtoContact
                = (Contact)actualMetaContact.getContacts().next();

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
        Iterator contactsIter = root.getChildContacts();

        assertTrue(
            "No contacts were found in the meta contact list"
            , contactsIter.hasNext());

        MetaContact expectedContact = (MetaContact)contactsIter.next();

        MetaContact actualResult = fixture.metaClService
            .findMetaContactByMetaUID(expectedContact.getMetaUID());

        assertEquals("find failed for contact "+expectedContact.getDisplayName()
                     , expectedContact, actualResult);

        // get one of the subgroups, extract one of its child contacts and
        // repeat the same test.
        Iterator subgroupsIter = root.getSubgroups();

        assertTrue(
            "No sub groups were found in the meta contact list"
            , subgroupsIter.hasNext());

        MetaContactGroup subgroup = (MetaContactGroup)subgroupsIter.next();

        contactsIter = subgroup.getChildContacts();

        assertTrue(
            "No contacts were found in the meta group: "
            + subgroup.getGroupName()
            , contactsIter.hasNext());

        expectedContact = (MetaContact)contactsIter.next();

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
        Iterator contactsIter = root.getChildContacts();

        assertTrue(
            "No contacts were found in the meta contact list"
            , contactsIter.hasNext());

        MetaContact expectedMetaContact = (MetaContact)contactsIter.next();

        assertTrue(
            "No contacts are encapsulated by MetaContact: "
            + expectedMetaContact.getDisplayName()
            , expectedMetaContact.getContacts().hasNext());


        Contact mockContact = (Contact)expectedMetaContact.getContacts().next();

        MetaContact actualResult = fixture.metaClService
                                .findMetaContactByContact(mockContact);

        assertEquals("find failed for contact "+expectedMetaContact.getDisplayName()
                     , expectedMetaContact, actualResult);

        // get one of the subgroups, extract one of its child contacts and
        // repeat the same test.
        Iterator subgroupsIter = root.getSubgroups();

        assertTrue(
            "No sub groups were found in the meta contact list"
            , subgroupsIter.hasNext());

        MetaContactGroup subgroup = (MetaContactGroup)subgroupsIter.next();

        contactsIter = subgroup.getChildContacts();

        assertTrue(
            "No contacts were found in MetaContactGroup: "
            + subgroup.getGroupName()
            , contactsIter.hasNext());


        expectedMetaContact = (MetaContact)contactsIter.next();

        assertTrue(
            "No contacts were encapsulated by meta contact: "
            + expectedMetaContact.getDisplayName()
            , expectedMetaContact.getContacts().hasNext());


        mockContact = (Contact)expectedMetaContact.getContacts().next();

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
        Iterator groupsIter = root.getSubgroups();

        assertTrue(
            "No sub groups were found in the meta contact list"
            , groupsIter.hasNext());

        MetaContactGroup expectedMetaContactGroup
                                    = (MetaContactGroup)groupsIter.next();

        assertTrue(
            "There were no contact groups encapsulated in MetaContactGroup: "
            + expectedMetaContactGroup
            , expectedMetaContactGroup.getContactGroups().hasNext());

        assertTrue(
            "No ContactGroups are encapsulated by MetaContactGroup: "
            + expectedMetaContactGroup
            , expectedMetaContactGroup.getContactGroups().hasNext());

        ContactGroup mockContactGroup = (ContactGroup)expectedMetaContactGroup
                                                    .getContactGroups().next();
        MetaContactGroup actualMetaContactGroup = fixture.metaClService
            .findMetaContactGroupByContactGroup(mockContactGroup);

        assertSame("find failed for contact group " + mockContactGroup
                   , expectedMetaContactGroup, actualMetaContactGroup);
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

        fixture.metaClService.addContactListListener(mclEvtCollector);
        opSetPersPresence.subscribe(newSubscriptionName);

        fixture.metaClService.removeContactListListener(mclEvtCollector);

        //first check that the newly created contact was really added
        MockContact newProtoContact = (MockContact)opSetPersPresence
                                        .findContactByID(newSubscriptionName);
        MetaContact newMetaContact = fixture.metaClService
            .findMetaContactByContact(newProtoContact);

        assertNotNull("The meta contact list was not updated after adding "
                      +"contact "+ newProtoContact +" to the mock provider."
                      , newMetaContact);

        assertEquals("Number of evts dispatched while adding a contact"
                     , 1
                     , mclEvtCollector.collectedEvents.size());
        MetaContactEvent evt = (MetaContactEvent)mclEvtCollector
                                                    .collectedEvents.get(0);

        assertEquals("ID of the generated event",
                     MetaContactEvent.META_CONTACT_ADDED,
                     evt.getEventID());

        assertEquals("Parent group of the source contact"
                    , fixture.metaClService.getRoot()
                    , evt.getParentGroup());

        assertEquals("Source meta contact."
                     , newMetaContact, evt.getSourceContact());

        assertEquals("Source provider"
                     , fixture.mockProvider, evt.getSourceProvider());

        //remove the subscirption and check for the event
        mclEvtCollector.collectedEvents.clear();

        fixture.metaClService.addContactListListener(mclEvtCollector);

        opSetPersPresence.unsubscribe(newProtoContact);

        fixture.metaClService.removeContactListListener(mclEvtCollector);

        //first check that the newly created contact was really added
        assertNull(
            "The impl contact list did not update after a subscr. was removed."
            ,fixture.metaClService.findMetaContactByContact(newProtoContact));

        assertEquals("Number of evts dispatched while adding a contact"
                     , 1
                     , mclEvtCollector.collectedEvents.size());
        evt = (MetaContactEvent)mclEvtCollector.collectedEvents.get(0);

        assertEquals("ID of the generated event",
                     MetaContactEvent.META_CONTACT_REMOVED,
                     evt.getEventID());

        assertEquals("Parent group of the source contact"
                    , fixture.metaClService.getRoot()
                    , evt.getParentGroup());

        assertEquals("Source meta contact."
                     , newMetaContact, evt.getSourceContact());

        assertEquals("Source provider"
                     , fixture.mockProvider, evt.getSourceProvider());

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
        //add a group and check for the event
        MclEventCollector mclEvtCollector = new MclEventCollector();

        fixture.metaClService.addContactListListener(mclEvtCollector);
        opSetPersPresence.createServerStoredContactGroup(
            opSetPersPresence.getServerStoredContactListRoot(), newGroupName);

        fixture.metaClService.removeContactListListener(mclEvtCollector);

        // first check whether event delivery went ok.
        assertEquals("Number of evts dispatched while adding a contact group"
                     , 1
                     , mclEvtCollector.collectedEvents.size());
        MetaContactGroupEvent evt = (MetaContactGroupEvent)mclEvtCollector
                                                    .collectedEvents.get(0);

        assertEquals("ID of the generated event",
                     MetaContactGroupEvent.META_CONTACT_GROUP_ADDED,
                     evt.getEventID());

        assertEquals("Source group of the AddEvent."
                    , newGroupName
                    , evt.getSourceMetaContactGroup().getGroupName());

        //first check that the newly created group was really added
        MetaContactGroup newMetaGroup = evt.getSourceMetaContactGroup();

        assertEquals("Source provider for the add event."
                     , fixture.mockProvider, evt.getSourceProvider());

        ContactGroup newProtoGroup = newMetaGroup.getContactGroup(
                                            newGroupName, fixture.mockProvider);

        assertNotNull("The new meta contact group did not contain a proto group"
                    , newProtoGroup);

        assertEquals("The new meta contact group did not seem to contain "
                     + "the right protocol contact group."
                     , newProtoGroup.getGroupName()
                     , newGroupName);

        assertEquals("The new meta contact group did not seem to contain "
                     + "the right protocol contact group."
                     , newProtoGroup.getProtocolProvider()
                     , fixture.mockProvider);


        mclEvtCollector.collectedEvents.clear();
        //rename the group and see that the corresponding events are handled
        //properly
        fixture.metaClService.addContactListListener(mclEvtCollector);
        opSetPersPresence.renameServerStoredContactGroup(
                                            newProtoGroup, renamedGroupName);

        fixture.metaClService.removeContactListListener(mclEvtCollector);

        //first check that the group was really renamed
        assertEquals("Number of evts dispatched while renaming a contact group"
                     , 1
                     , mclEvtCollector.collectedEvents.size());

        evt = (MetaContactGroupEvent)mclEvtCollector.collectedEvents.get(0);

        assertEquals("ID of the generated event",
                     MetaContactGroupEvent.CONTACT_GROUP_RENAMED_IN_META_GROUP,
                     evt.getEventID());

        assertEquals("Source group for the RemoveEvent."
                    , newMetaGroup
                    , evt.getSourceMetaContactGroup());

        assertEquals("Source provider for the remove event."
                     , fixture.mockProvider, evt.getSourceProvider());

        //check whether the group was indeed renamed.
        Iterator groupsIter = evt.getSourceMetaContactGroup()
            .getContactGroupsForProvider(fixture.mockProvider);

        assertTrue("A proto group was unexplicably removed after renaming.",
                   groupsIter.hasNext());

        assertEquals("The name of a protocol group after renaming."
                     , renamedGroupName
                     , ((MockContactGroup)groupsIter.next()).getGroupName());


        mclEvtCollector.collectedEvents.clear();

        //remove the group and check for the event.
        fixture.metaClService.addContactListListener(mclEvtCollector);
        opSetPersPresence.removeServerStoredContactGroup(newProtoGroup);

        fixture.metaClService.removeContactListListener(mclEvtCollector);

        //first check that the group was really removed
        assertEquals("Number of evts dispatched while removing a contact group"
                     , 1
                     , mclEvtCollector.collectedEvents.size());
        evt = (MetaContactGroupEvent)mclEvtCollector.collectedEvents.get(0);

        assertEquals("ID of the generated event",
                     MetaContactGroupEvent.CONTACT_GROUP_REMOVED_FROM_META_GROUP,
                     evt.getEventID());

        assertEquals("Source group for the RemoveEvent."
                    , newMetaGroup
                    , evt.getSourceMetaContactGroup());

        assertEquals("Source provider for the remove event."
                     , fixture.mockProvider, evt.getSourceProvider());

        mclEvtCollector.collectedEvents.clear();
    }


    public void testAddMoveRemoveContactToMetaContact()
    {
        /**@todo implement testAddMoveRemoveContactToMetaContact() */
//        fail("@todo implement testAddMoveRemoveContactToMetaContact()");
    }

    public void testCreateMoveRemoveMetaContact()
    {
        /**@todo implement testCreateMoveRemoveMetaContact() */
//        fail("@todo implement testCreateMoveRemoveMetaContact()");
    }

    public void testCreateRemoveMetaContactGroup()
    {
        /**@todo implement testCreateRemoveMetaContactGroup() */
//        fail("@todo implement testCreateRemoveMetaContactGroup()");
    }

    private class MclEventCollector implements MetaContactListListener
    {
        public Vector collectedEvents = new Vector();
        /**
         * Indicates that a MetaContact has been successfully added
         * to the MetaContact list.
         * @param evt the MetaContactListEvent containing the corresponding contact
         */
        public void metaContactAdded(MetaContactEvent evt)
        {
            collectedEvents.add(evt);
        }

        /**
         * Indicates that a MetaContactGroup has been successfully added
         * to the MetaContact list.
         * @param evt the MetaContactListEvent containing the corresponding
         * contact
         */
        public void metaContactGroupAdded(MetaContactGroupEvent evt)
        {
            collectedEvents.add(evt);
        }

        /**
         * Indicates that a MetaContactGroup has been removed from the
         * MetaContact list.
         * @param evt the MetaContactListEvent containing the corresponding
         * contact
         */
        public void metaContactGroupRemoved(MetaContactGroupEvent evt)
        {
            collectedEvents.add(evt);
        }

        /**
         * Indicates that a MetaContact has been removed from the MetaContact
         * list.
         * @param evt the MetaContactListEvent containing the corresponding
         * contact
         */
        public void metaContactRemoved(MetaContactEvent evt)
        {
            collectedEvents.add(evt);
        }

        /**
         * Indicates that a MetaContact has been modified.
         * @param evt the MetaContactListEvent containing the corresponding
         * contact
         */
        public void metaContactModified(MetaContactEvent evt)
        {
            collectedEvents.add(evt);
        }

        /**
         * Indicates that a MetaContactGroup has been modified (e.g. a proto
         * contact group was removed).
         *
         * @param evt the MetaContactListEvent containing the corresponding
         * contact
         */
        public void metaContactGroupModified(MetaContactGroupEvent evt)
        {
            collectedEvents.add(evt);
        }

    }

}
