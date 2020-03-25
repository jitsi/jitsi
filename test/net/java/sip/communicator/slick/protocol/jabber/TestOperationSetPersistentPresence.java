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
package net.java.sip.communicator.slick.protocol.jabber;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * @author Damian Minkov
 */
public class TestOperationSetPersistentPresence
    extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestOperationSetPersistentPresence.class);

    private JabberSlickFixture fixture = new JabberSlickFixture();
    private OperationSetPersistentPresence opSetPersPresence1 = null;
    private OperationSetPersistentPresence opSetPersPresence2 = null;
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
            new TestSuite();

        //the following 2 need to be run in the specified order.
        //(postTestRemoveGroup() needs the group created from
        //postTestCreateGroup() )
        suite.addTest(
            new TestOperationSetPersistentPresence("postTestCreateGroup"));

        //rename
        suite.addTest( new TestOperationSetPersistentPresence(
            "postTestRenameGroup"));

        suite.addTest(
            new TestOperationSetPersistentPresence("postTestRemoveGroup"));

        // create the contact list
        suite.addTest(
            new TestOperationSetPersistentPresence("prepareContactList"));

        suite.addTestSuite(TestOperationSetPersistentPresence.class);

        return suite;
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();

        Map<String, OperationSet> supportedOperationSets1 =
            fixture.provider1.getSupportedOperationSets();

        if ( supportedOperationSets1 == null
            || supportedOperationSets1.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                +"this Jabber implementation. ");

        //get the operation set presence here.
        opSetPersPresence1 =
            (OperationSetPersistentPresence)supportedOperationSets1.get(
                OperationSetPersistentPresence.class.getName());

        //if still null then the implementation doesn't offer a presence
        //operation set which is unacceptable for jabber.
        if (opSetPersPresence1 == null)
            throw new NullPointerException(
                "An implementation of the Jabber service must provide an "
                + "implementation of at least the one of the Presence "
                + "Operation Sets");

        // lets do it once again for the second provider
        Map<String, OperationSet> supportedOperationSets2 =
            fixture.provider2.getSupportedOperationSets();

        if (supportedOperationSets2 == null
            || supportedOperationSets2.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                + "this Jabber implementation. ");

        //get the operation set presence here.
        opSetPersPresence2 =
            (OperationSetPersistentPresence) supportedOperationSets2.get(
                OperationSetPersistentPresence.class.getName());

        //if still null then the implementation doesn't offer a presence
        //operation set which is unacceptable for jabber.
        if (opSetPersPresence2 == null)
            throw new NullPointerException(
                "An implementation of the Jabber service must provide an "
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
     * phase by the testerAgent.
     */
    public void testRetrievingServerStoredContactList()
    {
        ContactGroup rootGroup
            = opSetPersPresence1.getServerStoredContactListRoot();

        logger.debug("=========== Server Stored Contact List =================");

        logger.debug("rootGroup="+rootGroup.getGroupName()
                     +" rootGroup.childContacts="+rootGroup.countContacts()
                     + "rootGroup.childGroups="+rootGroup.countSubgroups()
                     + "Printing rootGroupContents=\n"+rootGroup.toString());

        Hashtable<String, List<String>> expectedContactList
            = JabberSlickFixture.preInstalledBuddyList;

        logger.debug("============== Expected Contact List ===================");
        logger.debug(expectedContactList);

        //Go through the contact list retrieved by the persistence presence set
        //and remove the name of every contact and group that we find there from
        //the expected contact list hashtable.
        Iterator<ContactGroup> groups = rootGroup.subgroups();
        while (groups.hasNext() )
        {
            ContactGroup group = groups.next();

            List<String> expectedContactsInGroup
                = expectedContactList.get(group.getGroupName());

            // When sending the offline message
            // the sever creates a group NotInContactList,
            // because the buddy we are sending message to is not in
            // the contactlist. So this group must be ignored
            if(!group.getGroupName().equals("NotInContactList"))
            {
                assertNotNull("Group " + group.getGroupName() +
                              " was returned by "
                              +
                    "the server but was not in the expected contact list."
                              , expectedContactsInGroup);

                Iterator<Contact> contactsIter = group.contacts();
                while(contactsIter.hasNext())
                {
                    String contactID = contactsIter.next().
                        getAddress();
                    expectedContactsInGroup.remove(contactID);
                }

                //If we've removed all the sub contacts, remove the group too.
                if(expectedContactsInGroup.size() == 0)
                    expectedContactList.remove(group.getGroupName());
            }
        }

        //whatever we now have in the expected contact list snapshot are groups,
        //that have been added by the testerAgent but that were not retrieved
        //by the persistent presence operation set.
        assertTrue("The following contacts were on the server sidec contact "
                   +"list, but were not returned by the pers. pres. op. set"
                   + expectedContactList.toString()
                   , expectedContactList.isEmpty());
    }

    /**
     * Creates a group in the server stored contact list, makes sure that the
     * corresponding event has been generated and verifies that the group is
     * in the list.
     *
     * @throws java.lang.Exception
     */
    public void postTestCreateGroup()
        throws Exception
    {
        // first clear the list
        fixture.clearProvidersLists();

        Object o = new Object();
        synchronized(o)
        {
            o.wait(3000);
        }

        logger.trace("testing creation of server stored groups");
        //first add a listener
        GroupChangeCollector groupChangeCollector = new GroupChangeCollector();
        opSetPersPresence1
            .addServerStoredGroupChangeListener(groupChangeCollector);

        //create the group
        opSetPersPresence1.createServerStoredContactGroup(
            opSetPersPresence1.getServerStoredContactListRoot(), testGroupName);

        groupChangeCollector.waitForEvent(10000);

        opSetPersPresence1
            .removeServerStoredGroupChangeListener(groupChangeCollector);

        // check whether we got group created event
        assertEquals("Collected Group Change events: ",
                     1, groupChangeCollector.collectedEvents.size());

        assertEquals("Group name.",  testGroupName,
            ((ServerStoredGroupEvent)groupChangeCollector.collectedEvents
                .get(0)).getSourceGroup().getGroupName());

        // check whether the group is retrievable
        ContactGroup group = opSetPersPresence1.getServerStoredContactListRoot()
            .getGroup(testGroupName);

        assertNotNull("A newly created group was not in the contact list.",
                      group);

        assertEquals("New group name", testGroupName, group.getGroupName());

        // when operating with groups . the group must have entries
        // so changes to take effect. Otherwise group will be lost after loggingout
        try
        {
            opSetPersPresence1.subscribe(group, fixture.userID2);

            synchronized(o){o.wait(1500);}
        }
        catch (Exception ex)
        {
            fail("error adding entry to group : " +
                 group.getGroupName() + " " +
                 ex.getMessage());
        }
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
        opSetPersPresence1
            .addServerStoredGroupChangeListener(groupChangeCollector);

        //create the group
        try
        {
            opSetPersPresence1.removeServerStoredContactGroup(
                opSetPersPresence1.getServerStoredContactListRoot()
                    .getGroup(testGroupName2));
        }
        catch(OperationFailedException ex)
        {
            logger.error("error removing group", ex);
        }

        groupChangeCollector.waitForEvent(10000);

        opSetPersPresence1
            .removeServerStoredGroupChangeListener(groupChangeCollector);

        // check whether we got group created event
        assertEquals("Collected Group Change event",
                     1, groupChangeCollector.collectedEvents.size());

        assertEquals("Group name.",  testGroupName2,
            ((ServerStoredGroupEvent)groupChangeCollector.collectedEvents
                .get(0)).getSourceGroup().getGroupName());

        // check whether the group is still on the contact list
        ContactGroup group = opSetPersPresence1.getServerStoredContactListRoot()
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

        ContactGroup group = opSetPersPresence1.getServerStoredContactListRoot()
                                .getGroup(testGroupName);

        //first add a listener
        GroupChangeCollector groupChangeCollector = new GroupChangeCollector();
        opSetPersPresence1
            .addServerStoredGroupChangeListener(groupChangeCollector);

        //change the name and wait for a confirmation event
        opSetPersPresence1.renameServerStoredContactGroup(group, testGroupName2);

        groupChangeCollector.waitForEvent(10000);

        opSetPersPresence1
            .removeServerStoredGroupChangeListener(groupChangeCollector);

        //examine the event
        assertEquals("Collected Group Change event",
                     1, groupChangeCollector.collectedEvents.size());

        assertEquals("Group name.",  testGroupName2,
            ((ServerStoredGroupEvent)groupChangeCollector.collectedEvents
                .get(0)).getSourceGroup().getGroupName());

        // check whether the group is still on the contact list
        ContactGroup oldGroup = opSetPersPresence1.getServerStoredContactListRoot()
            .getGroup(testGroupName);

        assertNull("A group was still findable by its old name after renaming.",
                      oldGroup);

        //make sure that we could find the group by its new name.
        ContactGroup newGroup = opSetPersPresence1.getServerStoredContactListRoot()
            .getGroup(testGroupName2);

        assertNotNull("Could not find a renamed group by its new name.",
                      newGroup);
    }

    /**
     * Create the contact list. Later will be test to be sure that creating is ok
     * @throws Exception
     */
    public void prepareContactList()
        throws Exception
    {
        fixture.clearProvidersLists();

        Object o = new Object();
        synchronized(o)
        {
            o.wait(3000);
        }

        String contactList = System.getProperty(
            JabberProtocolProviderServiceLick.CONTACT_LIST_PROPERTY_NAME, null);

        logger.debug("The "
                     + JabberProtocolProviderServiceLick.CONTACT_LIST_PROPERTY_NAME
                     + " property is set to=" + contactList);

        if(    contactList == null
            || contactList.trim().length() < 6)//at least 4 for a UIN, 1 for the
                                               // dot and 1 for the grp name
            throw new IllegalArgumentException(
                "The " +
                JabberProtocolProviderServiceLick.CONTACT_LIST_PROPERTY_NAME +
                " property did not contain a contact list.");
        StringTokenizer tokenizer = new StringTokenizer(contactList, " \n\t");

        logger.debug("tokens contained by the CL tokenized="
            +tokenizer.countTokens());

        Hashtable<String, List<String>> contactListToCreate
            = new Hashtable<String, List<String>>();

        //go over all group.uin tokens
        while (tokenizer.hasMoreTokens())
        {
            String groupUinToken = tokenizer.nextToken();
            int dotIndex = groupUinToken.indexOf(".");

            if ( dotIndex == -1 )
            {
                throw new IllegalArgumentException(groupUinToken
                    + " is not a valid Group.UIN token");
            }

            String groupName = groupUinToken.substring(0, dotIndex);
            String uin = groupUinToken.substring(dotIndex + 1);

            if(    groupName.trim().length() < 1
                || uin.trim().length() < 4 )
            {
                throw new IllegalArgumentException(
                    groupName + " or " + uin +
                    " are not a valid group name or Jabber UIN.");
            }

            //check if we've already seen this group and if not - add it
            List<String> uinInThisGroup = contactListToCreate.get(groupName);
            if (uinInThisGroup == null)
            {
                uinInThisGroup = new ArrayList<String>();
                contactListToCreate.put(groupName, uinInThisGroup);
            }

            uinInThisGroup.add(uin);
        }

        // now init the list
        Enumeration<String> newGroupsEnum = contactListToCreate.keys();

        //go over all groups in the contactsToAdd table
        while (newGroupsEnum.hasMoreElements())
        {
            String groupName = newGroupsEnum.nextElement();
            logger.debug("Will add group " + groupName);

            opSetPersPresence1.createServerStoredContactGroup(
                opSetPersPresence1.getServerStoredContactListRoot(), groupName);

            ContactGroup newlyCreatedGroup =
                opSetPersPresence1.getServerStoredContactListRoot().getGroup(groupName);

            Iterator<String> contactsToAddToThisGroup
                = contactListToCreate.get(groupName).iterator();
            while (contactsToAddToThisGroup.hasNext())
            {
                String id = contactsToAddToThisGroup.next();

                logger.debug("Will add buddy " + id);
                opSetPersPresence1.subscribe(newlyCreatedGroup, id);
            }
        }

        //store the created contact list for later reference
        JabberSlickFixture.preInstalledBuddyList = contactListToCreate;
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
         * milliseconds pass (whicever happens first).
         *
         * @param waitFor the number of milliseconds that we should be waiting
         * for an event before simply bailing out.
         */
        public void waitForEvent(long waitFor)
        {
            synchronized(this)
            {
                if(collectedEvents.size() > 0)
                    return;

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
         * Called whenever an indication is received that a new server stored
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
         * Called whenever an indication is received that an existing server stored
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
         * Called whenever an indication is received that an existing server
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
        public final List<EventObject> collectedEvents
            = new ArrayList<EventObject>();

        /**
         * Blocks until at least one event is received or until waitFor
         * milliseconds pass (whichever happens first).
         *
         * @param waitFor the number of milliseconds that we should be waiting
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
         * Stores the received subscription and notifies all waiting on this
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
         * Stores the received subscription and notifies all waiting on this
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
         * Stores the received subscription and notifies all waiting on this
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
         * Stores the received subscription and notifies all waiting on this
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
         * Stores the received subscription and notifies all waiting on this
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
         * Stores the received subscription and notifies all waiting on this
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
