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
 * Creates a chat room on the server, then tries to make both users join the
 * chatroom. Users would then exchange messages and perform a number of chat
 * room operations.
 *
 * @author Emil Ivov
 */
public class TestOperationSetMultiUserChat
    extends TestCase
{
    private static final Logger logger
        = Logger.getLogger(TestOperationSetMultiUserChat.class);

    private JabberSlickFixture fixture = new JabberSlickFixture();

    private OperationSetPresence opSetPresence1 = null;
    private OperationSetPresence opSetPresence2 = null;

    private OperationSetMultiUserChat opSetMultiChat1 = null;
    private OperationSetMultiUserChat opSetMultiChat2 = null;

    /**
     * Creates the test with the specified method name.
     * @param name the name of the method to execute.
     */
    public TestOperationSetMultiUserChat(String name)
    {
        super(name);
    }

    /**
     * Creates a test suite containing tests of this class in a specific order.
     * We'll first execute a test where we receive a typing notification, and
     * a volatile contact is created for the sender. we'll then be able to
     * retrieve this volatile contact and them a notification on our turn.
     * We need to do things this way as the contact corresponding to the tester
     * agent has been removed in the previous test and we no longer have it
     * in our contact list.
     *
     * @return Test a testsuite containing all tests to execute.
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite();

        //make sure tests are executed in the right order as we need to first
        //create and join the room before actually being able to send and/or
        //receive messages and participant events.
        suite.addTest(
            new TestOperationSetMultiUserChat("testCreateChatRoom"));
        suite.addTest(
            new TestOperationSetMultiUserChat("testGetExistingChatRooms"));
        suite.addTest(
            new TestOperationSetMultiUserChat("testFindRoom"));
        suite.addTest(
            new TestOperationSetMultiUserChat("testOurJoin"));
        suite.addTest(
            new TestOperationSetMultiUserChat("testGetMembersAfterJoin"));
        suite.addTest(
            new TestOperationSetMultiUserChat("testParticipantJoin"));
        suite.addTest(
            new TestOperationSetMultiUserChat("testSendAndReceiveMessages"));
        suite.addTest(
            new TestOperationSetMultiUserChat("testParticipantLeave"));
        suite.addTest(
            new TestOperationSetMultiUserChat(
                                       "testGetMembersAfterParticipantLeave"));
        suite.addTest(
            new TestOperationSetMultiUserChat("testOurLeave"));

        return suite;
    }


    /**
     * JUnit setup method.
     * @throws Exception in case anything goes wrong.
     */
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
                +"this implementation. ");

        //get the operation set presence here.
        opSetMultiChat1 =
            (OperationSetMultiUserChat)supportedOperationSets1.get(
                OperationSetMultiUserChat.class.getName());

        if (opSetMultiChat1 == null)
        {
            throw new NullPointerException(
                "No implementation for multi user chat was found");
        }

        //we also need the presence op set in order to retrieve contacts.
        opSetPresence1 =
            (OperationSetPresence)supportedOperationSets1.get(
                OperationSetPresence.class.getName());

        //if the op set is null show that we're not happy.
        if (opSetPresence1 == null)
        {
            throw new NullPointerException(
                "An implementation of the service must provide an "
                + "implementation of at least one of the PresenceOperationSets");
        }

        Map<String, OperationSet> supportedOperationSets2 =
            fixture.provider2.getSupportedOperationSets();

        if ( supportedOperationSets2 == null
            || supportedOperationSets2.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                +"this implementation. ");

        //get the operation set presence here.
        opSetMultiChat2 =
            (OperationSetMultiUserChat)supportedOperationSets2.get(
                OperationSetMultiUserChat.class.getName());

        if (opSetMultiChat2 == null)
        {
            throw new NullPointerException(
                "No implementation for multi user chat was found");
        }

        opSetPresence2 =
            (OperationSetPresence) supportedOperationSets2.get(
                OperationSetPresence.class.getName());

        //if the op set is null show that we're not happy.
        if (opSetPresence2 == null)
        {
            throw new NullPointerException(
                "An implementation of the service must provide an "
                + "implementation of at least one of the PresenceOperationSets");
        }

    }

    /**
     * JUnit teardown method.
     *
     * @throws Exception in case anything goes wrong.
     */
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        fixture.tearDown();
    }

    /**
     * Creates a chat room and verifies that it has been properly created.
     *
     * @throws Exception if any Exception is thrown during the test.
     */
    public void testCreateChatRoom()
        throws Exception
    {
        //create room
        ChatRoom testChatRoom = opSetMultiChat1
            .createChatRoom(fixture.chatRoomName, null);

        //get available rooms
        assertNotNull("createChatRoom() returned null", testChatRoom);

        assertEquals("The name of the chat room that was "
                     +"created did not match the name that we wanted to have"
                     , fixture.chatRoomName, testChatRoom.getName());

        assertSame(
            "The newly created chat room did not had a properly set provider."
            , fixture.provider1
            , testChatRoom.getParentProvider());
    }

    /**
     * Retrieves existing chat room from the protocol provider and makes sure
     * that the room we created in previous tests is in there.
     *
     * @throws Exception if an exception is thrown while retrieving existing
     * chat rooms.
     */
    public void testGetExistingChatRooms()
        throws Exception
    {
        List<String> existingChatRooms = opSetMultiChat1.getExistingChatRooms();

        assertTrue (
            "No chat rooms found on the server, even after we "
            +"have created one. "
            , existingChatRooms.size() > 0);

        logger.info("Server returned the following list of chat rooms: "
            + existingChatRooms);

        boolean testRoomFound = false;

        for (String roomName : existingChatRooms)
            if (roomName.equals(fixture.chatRoomName))
                testRoomFound = true;

        assertTrue("The room we created in previous tests "
                   +fixture.chatRoomName
                   +" was not among the existing rooms list returned by "
                   +"the provider."
                   , testRoomFound);
    }

    /**
     * Tries to find the test room we created previously and makes sure it looks
     * as expected.
     * @throws Exception if we fail finding the chat room.
     */
    public void testFindRoom()
        throws Exception
    {
        ChatRoom testChatRoom = opSetMultiChat1.findRoom(fixture.chatRoomName);

        assertNotNull("Could not find the test chat room on the server"
                      , testChatRoom);

        String roomName = testChatRoom.getName();

        assertEquals("Name of the test chat room did not match the name of the "
                     +"room we created"
                     , fixture.chatRoomName, roomName);
    }

    /**
     * Join the chat room and verify that we are among its members
     * @throws Exception
     */
    public void testOurJoin()
        throws Exception
    {
        ChatRoom testChatRoom1
            = opSetMultiChat1.findRoom(fixture.chatRoomName);

//        testChatRoom1.addParticipantStatusListener();

        testChatRoom1.join();

        /** @todo add event handlers for us joining the room for our status */


        testChatRoom1.getParentProvider();
        testChatRoom1.getUserNickname();

        testChatRoom1.isJoined();

    }

    public void testGetMembersAfterJoin()
    {
        //member count > 0
        //are we among the members?
    }

    public void testParticipantJoin()
        throws Exception
    {
        /** @todo join provider 2 */
        /** @todo make sure there was an event delivered to provider 1 saying
         * that provider 2 has joined. */
    }

    public void testSendAndReceiveMessages()
        throws Exception
    {
        ChatRoom testChatRoom = opSetMultiChat1.findRoom(fixture.chatRoomName);

        testChatRoom.sendMessage(testChatRoom.createMessage("opla"));

        /** @todo make sure there is a message at BOTH provider 1 & 2 saying
         * that  the message was delivered  */
        /** @todo make sure there is a message at BOTH provider 1 & 2 saying
         * that  the message was delivered  */
    }

    public void testParticipantLeave()
    {

    }

    public void testGetMembersAfterParticipantLeave()
    {
        //are we not among the members?
    }

    public void testOurLeave()
    {
        //are we not among the members?
    }

    private class ParticipantStatusEventCollector
        implements ChatRoomMemberPresenceListener
    {

        /**
         * Stores the received event and notifies all waiting on this object
         *
         * @param event the event containing the source call.
         */
        public void memberPresenceChanged(
                        ChatRoomMemberPresenceChangeEvent evt)
        {
//            synchronized(this)
//            {
//                logger.debug(
//                    "Collected evt("+collectedEvents.size()+")= "+event);
//
//                if(((CallState)event.getNewValue()).equals(awaitedState))
//                {
//                    this.collectedEvents.add(event);
//                    notifyAll();
//                }
//            }
        }


    }
}
