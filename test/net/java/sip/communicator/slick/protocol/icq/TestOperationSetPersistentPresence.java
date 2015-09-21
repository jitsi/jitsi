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
package net.java.sip.communicator.slick.protocol.icq;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * @todo describe
 *
 * @todo testing here would probably be best done if we could first log in
 * with one of the agents retrieve the contact list and then check that we
 * have the same thing with the other agent
 *
 * @author Emil Ivov
 * @author Damian Minkov
 */
public class TestOperationSetPersistentPresence
    extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestOperationSetPersistentPresence.class);

    private IcqSlickFixture fixture = new IcqSlickFixture();
    private OperationSetPersistentPresence opSetPersPresence = null;
    private static final String testGroupName = "NewGroup";
    private static final String testGroupName2 = "Renamed";

    public TestOperationSetPersistentPresence(String name)
    {
        super(name);
    }

    /**
     * Creates a test suite containing all tests of this class followed by
     * test methods that we want executed in a specified order.
     * @return the Test suite to run
     */
    public static Test suite()
    {
        TestSuite suite =
            new TestSuite(TestOperationSetPersistentPresence.class);

        //the following 2 need to be run in the specified order.
        //(postTestRemoveGroup() needs the group created from
        //postTestCreateGroup() )
        suite.addTest(
            new TestOperationSetPersistentPresence("postTestCreateGroup"));

//        suite.addTest( new TestOperationSetPersistentPresence(
//            "postTestPersistentSubscribe"));
//        suite.addTest( new TestOperationSetPersistentPresence(
//            "postTestPersistentUnsubscribe"));

        //rename
        suite.addTest( new TestOperationSetPersistentPresence(
            "postTestRenameGroup"));

        suite.addTest(
            new TestOperationSetPersistentPresence("postTestRemoveGroup"));

        return suite;
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();

        Map<String, OperationSet> supportedOperationSets =
            fixture.provider.getSupportedOperationSets();

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

    @Override
    protected void tearDown() throws Exception
    {
        fixture.tearDown();
        super.tearDown();
    }

    /**
     * Retrieves a server stored contact list and checks whether it contains
     * all contacts that have been added there during the initialization
     * phase by the icqTesterAgent.
     */
    public void testRetrievingServerStoredContactList()
    {
        ContactGroup rootGroup
            = opSetPersPresence.getServerStoredContactListRoot();

        logger.debug("=========== Server Stored Contact List =================");

        logger.debug("rootGroup="+rootGroup.getGroupName()
                     +" rootGroup.childContacts="+rootGroup.countContacts()
                     + "rootGroup.childGroups="+rootGroup.countSubgroups()
                     + "Printing rootGroupContents=\n"+rootGroup.toString());

        Hashtable<String, List<String>> expectedContactList
            = IcqSlickFixture.preInstalledBuddyList;

        logger.debug("============== Expected Contact List ===================");
        logger.debug(expectedContactList);

        //Go through the contact list retrieved by the persistence presence set
        //and remove the name of every contact and group that we find there from
        //the expected contct list hashtable.
        Iterator<ContactGroup> groups = rootGroup.subgroups();
        while (groups.hasNext() )
        {
            ContactGroup group = groups.next();

            List<String> expectedContactsInGroup
                = expectedContactList.get(group.getGroupName());

            // When sending the offline message
            // the sever creates a group NotInContactList,
            // beacuse the buddy we are sending message to is not in
            // the contactlist. So this group must be ignored
            // all not persistent groups must be ignored
            if(group.isPersistent())
            {
                assertNotNull("Group " + group.getGroupName() +
                              " was returned by "
                              +
                    "the server but was not in the expected contact list."
                              , expectedContactsInGroup);

                Iterator<Contact> contactsIter = group.contacts();
                while(contactsIter.hasNext())
                {
                    String contactID = contactsIter.next().getAddress();

                    expectedContactsInGroup.remove(contactID);
                }

                //If we've removed all the sub contacts, remove the group too.
                if(expectedContactsInGroup.size() == 0)
                    expectedContactList.remove(group.getGroupName());
            }
        }

        //whatever we now have in the expected contact list snapshot are groups,
        //that have been added by the IcqTesterAgent but that were not retrieved
        //by the persistent presence operation set.
        assertTrue("The following contacts were on the server sidec contact "
                   +"list, but were not returned by the pers. pres. op. set"
                   + expectedContactList.toString()
                   , expectedContactList.isEmpty());


    }

    /**
     * Adds the a contact to a group in our contact list. Verifies that event
     * dispatching goes ok. Makes sure that the contact is where it is supposed
     * to be.
     * <p>
     * Note that the method won't be testing presence event notifications since
     * these are being tested in TestOperationSetPresence.
     *
     * @throws java.lang.Exception in case network operations fail.
     */
    public void postTestPersistentSubscribe()
        throws Exception
    {
        logger.trace("Testing persistent subscriptions.");
        //find the group where we'll be adding the new contact
        ContactGroup group = opSetPersPresence.getServerStoredContactListRoot()
            .getGroup(testGroupName);

        //register a subscription event listener
        SubscriptionEventCollector evtCollector
            = new SubscriptionEventCollector();
        opSetPersPresence.addSubscriptionListener(evtCollector);

        //create the subscription and wait for a confirmation event.
        opSetPersPresence.subscribe(group, "38687470");

        evtCollector.waitForEvent(10000);

        opSetPersPresence.removeSubscriptionListener(evtCollector);

        //make sure the event delivery went as expected
        assertEquals("Number of dispatched events",
                     1, evtCollector.collectedEvents.size());

        assertEquals(
            "The SubscriptionEvent had a wrong event id.",
            SubscriptionEvent.SUBSCRIPTION_CREATED,
            ((SubscriptionEvent)evtCollector.collectedEvents.get(0)).getEventID());

        assertEquals(
            "The parent group in the subscription event did not match.",
            group,  ((SubscriptionEvent)evtCollector.collectedEvents.get(0))
                      .getParentGroup());

        Contact contact = group.getContact(IcqSlickFixture.testerAgent.getIcqUIN());

        //make sure that the contact appears in the new group.
        assertNotNull("Couldn't find contact where we created it", contact);
    }



    /**
     * Removes a contact from a group in our contact list. Verifies that event
     * dispatching goes ok. Makes sure that the contact is not in the group
     * any more.
     * <p>
     * Note that the method won't be testing presence event notifications since
     * these are being tested in TestOperationSetPresence.
     *
     * @throws java.lang.Exception in case network operations fail.
     */
    public void postTestPersistentUnsubscribe()
        throws Exception
    {
        logger.trace("Testing removal of persistent subscriptions.");
        //find the group where we'll be adding the new contact
        ContactGroup group = opSetPersPresence.getServerStoredContactListRoot()
            .getGroup(testGroupName);

        Contact contact = group.getContact(IcqSlickFixture.testerAgent.getIcqUIN());

        //register a subscription event listener
        SubscriptionEventCollector evtCollector
            = new SubscriptionEventCollector();
        opSetPersPresence.addSubscriptionListener(evtCollector);

        //remove the subscription and wait for a confirmation event.
        opSetPersPresence.unsubscribe(contact);

        evtCollector.waitForEvent(10000);

        opSetPersPresence.removeSubscriptionListener(evtCollector);

        //make sure the event delivery went as expected
        assertEquals("Number of dispatched events",
                     1, evtCollector.collectedEvents.size());

        assertEquals(
            "The SubscriptionEvent had a wrong event id.",
            SubscriptionEvent.SUBSCRIPTION_REMOVED,
            ((SubscriptionEvent)evtCollector.collectedEvents.get(0)).getEventID());

        assertEquals(
            "The parent group in the subscription event did not match.",
            group,  ((SubscriptionEvent)evtCollector.collectedEvents.get(0))
                      .getParentGroup());

        contact = group.getContact(IcqSlickFixture.testerAgent.getIcqUIN());

        //make sure that the contact is not in the group any more.
        assertNull("A contact was still present after removing its "
                      +"corresponding subscription", contact);
    }

    /**
     * Creates a group in the server stored contact list, makes sure that the
     * corresponding event has been generated and verifies that the group is
     * in the list.
     */
    public void postTestCreateGroup()
    {
        logger.trace("testing creation of server stored groups");
        //first add a listener
        GroupChangeCollector groupChangeCollector = new GroupChangeCollector();
        opSetPersPresence
            .addServerStoredGroupChangeListener(groupChangeCollector);

        //create the group
        try
        {
            opSetPersPresence.createServerStoredContactGroup(
                opSetPersPresence.getServerStoredContactListRoot(),
                testGroupName);
        }
        catch (OperationFailedException ex)
        {
            fail("Cannot create group " + ex.getMessage());
        }

        groupChangeCollector.waitForEvent(10000);

        opSetPersPresence
            .removeServerStoredGroupChangeListener(groupChangeCollector);

        // check whether we got group created event
        assertEquals("Collected Group Change events: ",
                     1, groupChangeCollector.collectedEvents.size());

        assertEquals("Group name.",  testGroupName,
            ((ServerStoredGroupEvent)groupChangeCollector.collectedEvents
                .get(0)).getSourceGroup().getGroupName());

        // check whether the group is retrievable
        ContactGroup group = opSetPersPresence.getServerStoredContactListRoot()
            .getGroup(testGroupName);

        assertNotNull("A newly created group was not in the contact list.",
                      group);

        assertEquals("New group name", testGroupName, group.getGroupName());
    }


    /**
     * Removes the group created in the server stored contact list by the create
     * group test, makes sure that the corresponding event has been generated
     * and verifies that the group is not in the list any more.
     */
    public void postTestRemoveGroup()
    {
        logger.trace("testing removal of server stored groups");

        //first add a listener
        GroupChangeCollector groupChangeCollector = new GroupChangeCollector();
        opSetPersPresence
            .addServerStoredGroupChangeListener(groupChangeCollector);

        try
        {
            // remove the group
            opSetPersPresence.removeServerStoredContactGroup(
                opSetPersPresence.getServerStoredContactListRoot()
                    .getGroup(testGroupName2));
        }
        catch(OperationFailedException ex)
        {
            logger.error("error removing group", ex);
        }

        groupChangeCollector.waitForEvent(10000);

        opSetPersPresence
            .removeServerStoredGroupChangeListener(groupChangeCollector);

        // check whether we got group created event
        assertEquals("Collected Group Change event",
                     1, groupChangeCollector.collectedEvents.size());

        assertEquals("Group name.",  testGroupName2,
            ((ServerStoredGroupEvent)groupChangeCollector.collectedEvents
                .get(0)).getSourceGroup().getGroupName());

        // check whether the group is still on the contact list
        ContactGroup group = opSetPersPresence.getServerStoredContactListRoot()
            .getGroup(testGroupName2);

        assertNull("A freshly removed group was still on the contact list.",
                      group);
    }

    /**
     * Renames our test group and checks whether corresponding events are
     * triggered. Verifies whether the group has really changed its name and
     * whether it is findable by its new name. Also makes sure that it does
     * not exist under its previous name any more.
     */
    public void postTestRenameGroup()
    {
        logger.trace("Testing renaming groups.");

        ContactGroup group = opSetPersPresence.getServerStoredContactListRoot()
                                .getGroup(testGroupName);

        //first add a listener
        GroupChangeCollector groupChangeCollector = new GroupChangeCollector();
        opSetPersPresence
            .addServerStoredGroupChangeListener(groupChangeCollector);

        //change the name and wait for a confirmation event
        opSetPersPresence.renameServerStoredContactGroup(group, testGroupName2);

        groupChangeCollector.waitForEvent(10000);

        opSetPersPresence
            .removeServerStoredGroupChangeListener(groupChangeCollector);

        //examine the event
        assertEquals("Collected Group Change event",
                     1, groupChangeCollector.collectedEvents.size());

        assertEquals("Group name.",  testGroupName2,
            ((ServerStoredGroupEvent)groupChangeCollector.collectedEvents
                .get(0)).getSourceGroup().getGroupName());

        // check whether the group is still on the contact list
        ContactGroup oldGroup = opSetPersPresence.getServerStoredContactListRoot()
            .getGroup(testGroupName);

        assertNull("A group was still findable by its old name after renaming.",
                      oldGroup);

        //make sure that we could find the group by its new name.
        ContactGroup newGroup = opSetPersPresence.getServerStoredContactListRoot()
            .getGroup(testGroupName2);

        assertNotNull("Could not find a renamed group by its new name.",
                      newGroup);
    }

    /**
     * The class would listen for and store received events delivered to
     * <tt>ServerStoredGroupListener</tt>s.
     */
    private class GroupChangeCollector implements ServerStoredGroupListener
    {
        public ArrayList<EventObject> collectedEvents = new ArrayList<EventObject>();

        /**
         * Blocks until at least one event is received or until waitFor
         * miliseconds pass (whicever happens first).
         *
         * @param waitFor the number of miliseconds that we should be waiting
         * for an event before simply bailing out.
         */
        public void waitForEvent(long waitFor)
        {
            synchronized(this)
            {
                if(collectedEvents.size() > 0)
                {
                    logger.trace("ServerStoredGroupEvent already received. " + collectedEvents);
                    return;
                }

                try{
                    wait(waitFor);
                }
                catch (InterruptedException ex)
                {
                    logger.debug(
                        "Interrupted while waiting for a subscription evt", ex);
                }
            }
        }

        /**
         * Called whnever an indication is received that a new server stored
         * group is created.
         * @param evt a ServerStoredGroupChangeEvent containing a reference to
         * the newly created group.
         */
        public void groupCreated(ServerStoredGroupEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected evt("+collectedEvents.size()+")= "+evt);
                collectedEvents.add(evt);
                notifyAll();
            }
        }

        /**
         * Called when an indication is received that the name of a server stored
         * contact group has changed.
         * @param evt a ServerStoredGroupChangeEvent containing the details of the
         * name change.
         */
        public void groupNameChanged(ServerStoredGroupEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected evt("+collectedEvents.size()+")= "+evt);
                collectedEvents.add(evt);
                notifyAll();
            }
        }

        /**
         * Called whnever an indication is received that an existing server stored
         * group has been removed.
         * @param evt a ServerStoredGroupChangeEvent containing a reference to the
         * newly created group.
         */
        public void groupRemoved(ServerStoredGroupEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected evt("+collectedEvents.size()+")= "+evt);
                collectedEvents.add(evt);
                notifyAll();
            }
        }

        /**
         * Called whnever an indication is received that an existing server
         * stored group has been resolved.
         * @param evt a ServerStoredGroupChangeEvent containing a reference to
         * the resolved group.
         */
        public void groupResolved(ServerStoredGroupEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected evt("+collectedEvents.size()+")= "+evt);
                collectedEvents.add(evt);
                notifyAll();
            }
        }
    }

    /**
     * The class would listen for and store received subscription modification
     * events.
     */
    private class SubscriptionEventCollector implements SubscriptionListener
    {
        public ArrayList<EventObject> collectedEvents = new ArrayList<EventObject>();

        /**
         * Blocks until at least one event is received or until waitFor
         * miliseconds pass (whicever happens first).
         *
         * @param waitFor the number of miliseconds that we should be waiting
         * for an event before simply bailing out.
         */
        public void waitForEvent(long waitFor)
        {
            logger.trace("Waiting for a persistent subscription event");

            synchronized(this)
            {
                if(collectedEvents.size() > 0)
                {
                    logger.trace("SubEvt already received. " + collectedEvents);
                    return;
                }

                try{
                    wait(waitFor);
                    if(collectedEvents.size() > 0)
                        logger.trace("Received a SubEvt in provider status.");
                    else
                        logger.trace("No SubEvt received for "+waitFor+"ms.");
                }
                catch (InterruptedException ex)
                {
                    logger.debug(
                        "Interrupted while waiting for a subscription evt", ex);
                }
            }
        }

        /**
         * Stores the received subsctiption and notifies all waiting on this
         * object
         * @param evt the SubscriptionEvent containing the corresponding contact
         */
        public void subscriptionCreated(SubscriptionEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected evt("+collectedEvents.size()+")= "+evt);
                collectedEvents.add(evt);
                notifyAll();
            }
        }

        /**
         * Stores the received subsctiption and notifies all waiting on this
         * object
         * @param evt the SubscriptionEvent containing the corresponding contact
         */
        public void subscriptionRemoved(SubscriptionEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected evt("+collectedEvents.size()+")= "+evt);
                collectedEvents.add(evt);
                notifyAll();
            }
        }

        /**
         * Stores the received subsctiption and notifies all waiting on this
         * object
         * @param evt the SubscriptionEvent containing the corresponding contact
         */
        public void subscriptionFailed(SubscriptionEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected evt("+collectedEvents.size()+")= "+evt);
                collectedEvents.add(evt);
                notifyAll();
            }
        }

        /**
         * Stores the received subsctiption and notifies all waiting on this
         * object
         * @param evt the SubscriptionEvent containing the corresponding contact
         */
        public void subscriptionResolved(SubscriptionEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected evt("+collectedEvents.size()+")= "+evt);
                collectedEvents.add(evt);
                notifyAll();
            }
        }


        /**
         * Stores the received subsctiption and notifies all waiting on this
         * object
         * @param evt the SubscriptionEvent containing the corresponding contact
         */
        public void subscriptionMoved(SubscriptionMovedEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected evt("+collectedEvents.size()+")= "+evt);
                collectedEvents.add(evt);
                notifyAll();
            }
        }

        /**
         * Stores the received subsctiption and notifies all waiting on this
         * object
         * @param evt the SubscriptionEvent containing the corresponding contact
         */
        public void contactModified(ContactPropertyChangeEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected evt("+collectedEvents.size()+")= "+evt);
                collectedEvents.add(evt);
                notifyAll();
            }
        }

    }
}
