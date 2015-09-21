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

import org.jivesoftware.smack.util.StringUtils;

/**
 * Tests multi user chat functionalities.
 *
 * @author Symphorien Wanko
 * @author Valentin Martinet
 */
public class TestOperationSetMultiUserChat2
    extends TestCase
{

    /**
     * logger for <tt>TestOperationSetMultiUserChat2</tt> class
     */
    private static final Logger logger =
        Logger.getLogger(TestOperationSetMultiUserChat2.class);

    /**
     * Room name for each test will be testRoomBaseName + roomID
     */
    private static String testRoomBaseName = "lmuctestroom";

    private static int roomID = 0;

    /**
     * Provides constants and some utilities method.
     */
    private final JabberSlickFixture fixture = new JabberSlickFixture();

    private OperationSetMultiUserChat opSetMUC1;
    private OperationSetMultiUserChat opSetMUC2;
    private OperationSetMultiUserChat opSetMUC3;

    private OperationSetPresence opSetPresence1;
    private OperationSetPresence opSetPresence2;
    private OperationSetPresence opSetPresence3;

    /**
     * Initializes the test with the specified <tt>name</tt>.
     *
     * @param name the name of the test to initialize.
     */
    public TestOperationSetMultiUserChat2(String name)
    {
        super(name);
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

        if (supportedOperationSets1 == null
            || (supportedOperationSets1.size() < 1))
        {
            throw new NullPointerException(
                "No OperationSet implementations are supported by " +
                "this implementation. ");
        }

        //get the operation set presence here.
        opSetMUC1 = (OperationSetMultiUserChat)
            supportedOperationSets1.get(
            OperationSetMultiUserChat.class.getName());

        if (opSetMUC1 == null)
        {
            throw new NullPointerException(
                "No implementation for MUC was found");
        }

        //we also need the presence op set in order to retrieve contacts.
        opSetPresence1 = (OperationSetPresence)
            supportedOperationSets1.get(OperationSetPresence.class.getName());

        //if the op set is null show that we're not happy.
        if (opSetPresence1 == null)
        {
            throw new NullPointerException(
                "An implementation of the service must provide an " +
                "implementation of at least one of the PresenceOperationSets");
        }

        Map<String, OperationSet> supportedOperationSets2 =
            fixture.provider2.getSupportedOperationSets();

        if (supportedOperationSets2 == null
            || (supportedOperationSets2.size() < 1))
        {
            throw new NullPointerException(
                "No OperationSet implementations are supported by " +
                "this implementation. ");
        }

        opSetMUC2 = (OperationSetMultiUserChat)
            supportedOperationSets2.get(
            OperationSetMultiUserChat.class.getName());

        if (opSetMUC2 == null)
        {
            throw new NullPointerException(
                "No implementation for MUC was found");
        }

        opSetPresence2 = (OperationSetPresence)
            supportedOperationSets2.get(OperationSetPresence.class.getName());

        //if the op set is null show that we're not happy.
        if (opSetPresence2 == null)
        {
            throw new NullPointerException(
                "An implementation of the service must provide an " +
                "implementation of at least one of the PresenceOperationSets");
        }

        Map<String, OperationSet> supportedOperationSets3 =
            fixture.provider3.getSupportedOperationSets();

        if (supportedOperationSets3 == null
            || (supportedOperationSets3.size() < 1))
        {
            throw new NullPointerException(
                "No OperationSet implementations are supported by " +
                "this implementation. ");
        }

        opSetMUC3 = (OperationSetMultiUserChat)
            supportedOperationSets3.get(
            OperationSetMultiUserChat.class.getName());

        if (opSetMUC3 == null)
        {
            throw new NullPointerException(
                "No implementation for MUC was found");
        }

        opSetPresence3 = (OperationSetPresence)
            supportedOperationSets3.get(OperationSetPresence.class.getName());

        //if the op set is null show that we're not happy.
        if (opSetPresence3 == null)
        {
            throw new NullPointerException(
                "An implementation of the service must provide an " +
                "implementation of at least one of the PresenceOperationSets");
        }
    }

    /**
     * JUnit teardown method.
     * @throws Exception in case anything goes wrong.
     */
    @Override
    protected void tearDown() throws Exception
    {
        fixture.tearDown();
        super.tearDown();
    }

    /**
     * Create the list to be sure that contacts exchanging messages
     * exists in each other lists
     * @throws Exception
     */
    public void prepareContactList()
        throws Exception
    {
        fixture.clearProvidersLists();

        Object o = new Object();
        synchronized (o)
        {
            o.wait(2000);
        }

        try
        {
            opSetPresence1.subscribe(fixture.userID2);
        }
        catch (OperationFailedException ex)
        {
            // the contact already exist its OK
        }

        try
        {
            opSetPresence1.subscribe(fixture.userID3);
        }
        catch (OperationFailedException ex)
        {
         // the contact already exist its OK
        }

        try
        {
            opSetPresence2.subscribe(fixture.userID1);
        }
        catch (OperationFailedException ex1)
        {
            // the contact already exist its OK
        }

        try
        {
            opSetPresence2.subscribe(fixture.userID3);
        }
        catch (OperationFailedException ex1)
        {
            // the contact already exist its OK
        }

        try
        {
            opSetPresence3.subscribe(fixture.userID1);
        }
        catch (OperationFailedException ex1)
        {
            // the contact already exist its OK
        }

        try
        {
            opSetPresence3.subscribe(fixture.userID2);
        }
        catch (OperationFailedException ex1)
        {
            // the contact already exist its OK
        }

        logger.info("will wait till the list prepare is completed");
        synchronized (o)
        {
            o.wait(4000);
        }
    }

    /**
     * utiliy method to know if a given nickname is on a
     * <tt>ChatRoomMember</tt> list
     *
     * @param name the name we are looking for
     * @param memberList the list where we search
     *
     * @return true if the name is found, false otherwise
     */
    private boolean nameIsOnMemberList(
        String name, List<ChatRoomMember> memberList)
    {
        for (ChatRoomMember member : memberList)
        {
            if (member.getName().equals(name))
            {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @return Test a testsuite containing all tests to execute.
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite();

        suite.addTest(
            new TestOperationSetMultiUserChat2("testCreateChatRoom"));

        suite.addTest(
            new TestOperationSetMultiUserChat2("testJoinRoom"));

        suite.addTest(
            new TestOperationSetMultiUserChat2("testGetJoinedChatRoom"));

        suite.addTest(
            new TestOperationSetMultiUserChat2("testFindRoom"));

        suite.addTest(
            new TestOperationSetMultiUserChat2("testInviteReject"));

        suite.addTest(
            new TestOperationSetMultiUserChat2("testInviteJoin"));

        suite.addTest(
            new TestOperationSetMultiUserChat2("testLeave"));

        //@todo the following two must be tested, they regulary fail, so we are
        //disabling them until fixed.
//        suite.addTest(
//            new TestOperationSetMultiUserChat2("testNickName"));
//
//        suite.addTest(
//            new TestOperationSetMultiUserChat2("testRoomSubject"));
//
//        suite.addTest(
//            new TestOperationSetMultiUserChat2("testConferenceChat"));

        suite.addTest(
            new TestOperationSetMultiUserChat2("testMemberBan"));

        suite.addTest(
            new TestOperationSetMultiUserChat2("testMemberKick"));

        /*
         * Unit tests who belong to users role functionalities are orderly added
         * below. They are added in a specific order which match a predefined
         * scenario. It's not recommended to change this order, some tests may
         * then fail.
         *
         * Please note:
         * - when the room is created, participants are considered (by default)
         *   as members (not guests).
         * - in this test suite, the room will use three participants.
         *
         * Scenario:
         * - testInitialParticipantsRoles: verify participants role in the newly
         *   created room.
         * - testRevokeMembership: administrator/owner of the room revokes
         *   membership to a participant of the room.
         * - testGrantMembership: administrator/owner of the room grants
         *   membership to the participant of the room who just loses it in the
         *   previous test.
         * - testGrantModerator: administrator/owner of the room grants
         *   moderator rights to a participant of the room.
         * - testRevokeVoice: the new moderator of the room (previous test) will
         *   revoke voice right to the remaining guest, who will become a silent
         *   member (the room currently includes the administrator/owner, the
         *   moderator from the previous test, and it remains a participant).
         * - testGrantVoice: moderator will grant voice the user he previously
         *   revokes voice right.
         * - testRevokeModerator: moderator of the room will have his rights
         *   revoked by the owner/admin of the room.
         * - testGrantAdmin: owner/admin of the room grants administrator rights
         *   to the revoked-moderator (previous test).
         * - testGrantOwnership: owner/admin of the room grants the new admin as
         *   co-owner of the room.
         * - testRevokeOwnership: owner/admin of the room revokes ownership to
         *   his co-owner (granted in previous test).
         * - testRevokeAdmin: after having been ownership-revoked, the
         *   considered member is supposed to be an administrator: here his
         *   administrative rights are revoked by owner/admin of the room.
         *
         */
        suite.addTest(
            new TestOperationSetMultiUserChat2("testInitialParticipantsRoles"));

        //suite.addTest(
        //    new TestOperationSetMultiUserChat2("testRevokeMembership"));

        //suite.addTest(
        //    new TestOperationSetMultiUserChat2("testGrantMembership"));

        suite.addTest(new TestOperationSetMultiUserChat2("testGrantModerator"));

        suite.addTest(new TestOperationSetMultiUserChat2("testRevokeVoice"));

        suite.addTest(new TestOperationSetMultiUserChat2("testGrantVoice"));

        suite.addTest(
            new TestOperationSetMultiUserChat2("testRevokeModerator"));

//        suite.addTest(new TestOperationSetMultiUserChat2("testGrantAdmin"));
//
//        suite.addTest(new TestOperationSetMultiUserChat2("testGrantOwnership"));
//
//        suite.addTest(
//            new TestOperationSetMultiUserChat2("testRevokeOwnership"));
//
//        suite.addTest(new TestOperationSetMultiUserChat2("testRevokeAdmin"));

        return suite;
    }

    /**
     * <tt>testCreateChatRoom</tt> creates a room using
     * <tt>OperationSetMultiUserChat#createRoom()</tt>.
     * We then look if the room is on the list returned by
     * <tt>OperationSetMultiUserChat#getExistingChatRooms()</tt>
     *
     * @throws OperationNotSupportedException
     * @throws OperationFailedException
     */
    public void testCreateChatRoom()
        throws OperationNotSupportedException,
               OperationFailedException
    {
        String testRoomName = testRoomBaseName + roomID++;

        // we create the test room
        ChatRoom opSet1Room =
            opSetMUC1.createChatRoom(testRoomName, null);

        assertNotNull("createChatRoom returned null", opSet1Room);

        /*
        Room listing has been disabled on testing server,
        so skip checks
        // and check if it exists on the server
        List<String> existingRooms = opSetMUC1.getExistingChatRooms();

        for (String roomName : existingRooms)
        {
            if (roomName.equals(opSet1Room.getName()))
            {
                return; // ok the created room is listed on server
            }
        }
        fail("the new created room is not listed on server");
        */
    }

    /**
     * <tt>testJoinRoom</tt> looks if <tt>someRoom.isJoined</tt> will returns
     * true for an user after he joins a room with <tt>someRoom.join()</tt>
     *
     * @throws OperationFailedException
     * @throws OperationNotSupportedException
     */
    public void testJoinRoom()
        throws OperationFailedException,
               OperationNotSupportedException
    {
        String testRoomName = testRoomBaseName + roomID++;

        MUCEventCollector opSet1Collector =
            new MUCEventCollector(opSetMUC1, MUCEventCollector.EVENT_PRESENCE);

        // we create the test room
        ChatRoom opSet1Room =
            opSetMUC1.createChatRoom(testRoomName, null);

        opSet1Room.join();

        opSet1Collector.waitForEvent(10000);

        assertEquals("user1 didn't get an event since he joined"
            , 1, opSet1Collector.collectedEvents.size());

        LocalUserChatRoomPresenceChangeEvent changeEvent =
            (LocalUserChatRoomPresenceChangeEvent)
            opSet1Collector.collectedEvents.get(0);

        assertEquals("the event user1 received after he joined is no " +
            "LOCAL_USER_JOINED"
            , LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED
            , changeEvent.getEventType());

        assertTrue(
            "we are not in the room we just joined", opSet1Room.isJoined());

        assertTrue("user is not listed in the room with his nickname",
            nameIsOnMemberList(
            opSet1Room.getUserNickname(), opSet1Room.getMembers()));
    }

    /**
     * After a user joins a room, we look if the room is present on the list
     * returned by
     * <tt>OperationSetMultiUserChat#getCurrentlyJoinedChatRooms</tt>.
     *
     */
    public void testGetJoinedChatRoom()
        throws OperationFailedException,
               OperationNotSupportedException
    {
        String testRoomName = testRoomBaseName + roomID++;

        // we create the test room
        ChatRoom opSet1Room =
            opSetMUC1.createChatRoom(testRoomName, null);

        opSet1Room.join();

        assertTrue(
            "we are not in the room we just joined", opSet1Room.isJoined());

        // is the joined room list up to date ?
        List<ChatRoom> joinedRooms = opSetMUC1.getCurrentlyJoinedChatRooms();

        for (ChatRoom room : joinedRooms)
        {
            if (room.getName().equals(opSet1Room.getName()))
            {
                return; // ok opSet1Room is on the joined rooms list
            }
        }
        fail("the joined room list does not contains a joined room");
    }

    /**
     * <tt>testFindRoom</tt> will looks for an unexisting room and an existing
     * one to see if results are what we expect.
     */
    public void testFindRoom()
        throws OperationFailedException,
               OperationNotSupportedException
    {
        String testRoomName = testRoomBaseName + roomID++;

        // we create the test room
        ChatRoom opSet1Room =
            opSetMUC1.createChatRoom(testRoomName, null);
        opSet1Room.join();

        ChatRoom foundRoom = null;
        /*
         //findRoom always returns ChatRoom instance. If it doesn't exists in 
         //the cache it creates an instance of ChatRoom.
        
        try
        {
            foundRoom = opSetMUC1.findRoom("WhoCreatedThatRoom");
        }
        catch (Exception ex)
        {
            //ok, no one created a room named "WhoCreatedThatRoom"
        }
        assertNull("wasnt expecting to find the room named " +
            "'WhoCreatedThatRoom' on server", foundRoom);
*/
        // to find the existing room created with opSetMUC1,
        // we will use opSetMUC2 to be sure the room will not be retrieved from
        // opSetMUC1 cache
        try
        {
            foundRoom = opSetMUC2.findRoom(testRoomName);
        }
        catch (Exception ex)
        {
            logger.warn(ex);
        }
        assertNotNull("failed to find an existing room on server", foundRoom);

        assertEquals(
            "the room found is not exactly the one we were looking for"
            , opSet1Room.getName(), foundRoom.getName());
    }

    /**
     * user1 send to user2 an invitation which will be rejected.
     */
    public void testInviteReject()
        throws OperationFailedException,
               OperationNotSupportedException
    {
        MUCEventCollector opSet1Collector =
            new MUCEventCollector(opSetMUC1, MUCEventCollector.EVENT_INVITE);
        MUCEventCollector opSet2Collector =
            new MUCEventCollector(opSetMUC2, MUCEventCollector.EVENT_INVITE);

        String testRoomName = testRoomBaseName + roomID++;

        // we create the test room
        ChatRoom opSet1Room =
            opSetMUC1.createChatRoom(testRoomName, null);
        opSet1Room.join();

        // an invitation user2 must reject
        opSet1Room.invite(fixture.userID2, "testInviteReject");

        opSet2Collector.waitForEvent(10000);

        //now we look if user2 received an event
        assertEquals(
            "The invitation sent from user1 to user2 has not been received "
            , 1, opSet2Collector.collectedEvents.size());

        // does the invitation comes from user1 with the correct reason
        ChatRoomInvitationReceivedEvent invitationReceivedEvent =
            (ChatRoomInvitationReceivedEvent)
            opSet2Collector.collectedEvents.get(0);

        ChatRoomInvitation invitation = invitationReceivedEvent.getInvitation();

        assertEquals("The inviter is not the expected user",
            StringUtils.parseBareAddress(fixture.userID1),
            StringUtils.parseBareAddress(invitation.getInviter()));

        assertEquals("The invitation reason received differs from the one sent"
            , "testInviteReject", invitation.getReason());

        //user2 reject the invitation as planned
        opSetMUC2.rejectInvitation(invitation, invitation.getReason());

        opSet1Collector.waitForEvent(10000);

        //is user1 aware his invitation has been declined
        assertEquals(
            "no response received from the invitation sent to user 2"
            , 1, opSet1Collector.collectedEvents.size());

        //who declined the invitation and why
        ChatRoomInvitationRejectedEvent invitationRejectedEvent =
            (ChatRoomInvitationRejectedEvent)
            opSet1Collector.collectedEvents.get(0);

        assertEquals("the invitation has been declined  by an unexpected user",
            StringUtils.parseBareAddress(fixture.userID2),
            StringUtils.parseBareAddress(invitationRejectedEvent.getInvitee()));

        assertEquals("the invitation is not declined for the expected reason",
            "testInviteReject", invitationRejectedEvent.getReason());
    }

    /**
     * <tt>testInviteJoin</tt> reproduces the following scenario : user1
     * invite user2. user2 retrieves the room where he is invited then,
     * he joins it.
     */
    public void testInviteJoin()
        throws OperationFailedException,
               OperationNotSupportedException
    {
        String testRoomName = testRoomBaseName + roomID++;

        // we create the test room
        ChatRoom opSet1Room =
            opSetMUC1.createChatRoom(testRoomName, null);
        opSet1Room.join();

        MUCEventCollector opSet1RoomCollector =
            new MUCEventCollector(
            opSet1Room, MUCEventCollector.EVENT_PRESENCE);

        MUCEventCollector opSet2Collector =
            new MUCEventCollector(opSetMUC2, MUCEventCollector.EVENT_INVITE);

        opSet1Room.invite(fixture.userID2, "testInviteAccept");

        opSet2Collector.waitForEvent(10000);

        //make sure the invitation has been received
        assertEquals("the invitation has not been received"
            , 1, opSet2Collector.collectedEvents.size());

        // does the invitation comes from user1 with the correct reason
        ChatRoomInvitationReceivedEvent invitationReceivedEvent =
            (ChatRoomInvitationReceivedEvent)
            opSet2Collector.collectedEvents.get(0);

        ChatRoomInvitation invitation = invitationReceivedEvent.getInvitation();

        assertEquals("The inviter is not the expected user",
            StringUtils.parseBareAddress(fixture.userID1),
            StringUtils.parseBareAddress(invitation.getInviter()));

        assertEquals("The invitation reason received differs from the one sent"
            , "testInviteAccept", invitation.getReason());

        //user accept the invitation
        ChatRoom opSet2Room = invitation.getTargetChatRoom();

        MUCEventCollector opSet2RoomCollector =
            new MUCEventCollector(opSet2Room, MUCEventCollector.EVENT_PRESENCE);

        opSet2Collector =
            new MUCEventCollector(opSetMUC2, MUCEventCollector.EVENT_PRESENCE);

        opSet2Room.join();

        opSet2Collector.waitForEvent(10000);      // listening for user2 own join
        opSet2RoomCollector.waitForEvent(10000);  // listening for user1 join
        opSet1RoomCollector.waitForEvent(10000);  // listening for user2 join

        // we know check if both member received events
        assertEquals("a room member has not been notified that someone " +
            "joined the room"
            , 1, opSet1RoomCollector.collectedEvents.size());

        assertEquals("a room member has not been notified that someone " +
            "joined the room"
            , 1, opSet2RoomCollector.collectedEvents.size());

        ChatRoomMemberPresenceChangeEvent memberEvent =
            (ChatRoomMemberPresenceChangeEvent)
            opSet1RoomCollector.collectedEvents.get(0);

        assertEquals("user received an event of the wrong type "
            , ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED
            , memberEvent.getEventType());

        assertEquals("the user who joined is not the one who was invited"
            , fixture.userID2
            , memberEvent.getChatRoomMember().getContactAddress());

        assertEquals("there is not exactly two members in the room"
            , 2, opSet1Room.getMembersCount());

        // is user2 notified of its own join
        assertEquals("user hasn't been notified of its own join"
            , 1, opSet2Collector.collectedEvents.size());

        LocalUserChatRoomPresenceChangeEvent localEvent =
            (LocalUserChatRoomPresenceChangeEvent)
            opSet2Collector.collectedEvents.get(0);

        assertEquals("the event user2 received is not LOCAL_USER_JOINED"
            , LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED
            , localEvent.getEventType());

        // both sides should report the same members number.
        // here we use opSet2Room.getMembers().size() rather than
        // opSet2Room.getMembersCount() because the last method will not
        // be always accurate for a newly joined jabber room (stated in
        // the smack lib)
        assertEquals("the same room reports different members count " +
            "from one side to other"
            , opSet1Room.getMembers().size()
            , opSet2Room.getMembers().size());

        //user2 must be on room members list now
        assertTrue("user is not listed in the room with his nickname",
            nameIsOnMemberList(
            opSet2Room.getUserNickname(), opSet2Room.getMembers()));

        //and he must be listed on both sides
        assertTrue("user is not listed in the room with his nickname ,from " +
            "peer side",
            nameIsOnMemberList(
            opSet2Room.getUserNickname(), opSet1Room.getMembers()));
    }

    /**
     * In <tt>testJoinLeave</tt>, we join a room then leave it and see if the
     * reported <tt>ChatRoom</tt> members are reflecting our actions.
     */
    public void testLeave()
        throws OperationFailedException,
               OperationNotSupportedException
    {
        String testRoomName = testRoomBaseName + roomID++;

        // we create the test room
        ChatRoom opSet1Room =
            opSetMUC1.createChatRoom(testRoomName, null);
        opSet1Room.join();

        ChatRoom opSet2Room = opSetMUC2.findRoom(testRoomName);

        // first be sure to be outside the room
        if (opSet2Room.isJoined())
            opSet2Room.leave();

        assertFalse("user is in a room he left or didnt joined"
            , opSet2Room.isJoined());

        MUCEventCollector opSet1RoomCollector =
            new MUCEventCollector(opSet1Room, MUCEventCollector.EVENT_PRESENCE);

        MUCEventCollector opSet2RoomCollector =
            new MUCEventCollector(opSet2Room, MUCEventCollector.EVENT_PRESENCE);

        opSet2Room.join();

        opSet2RoomCollector.waitForEvent(10000);
        opSet1RoomCollector.waitForEvent(10000);

        // does users received events since a member joined
        assertEquals("a room member has not been notified that someone else " +
            "joined the room"
            , 1, opSet1RoomCollector.collectedEvents.size());

        assertTrue("user failed to join a room", opSet2Room.isJoined());

        // checking if the user is listed with the right nickname
        assertTrue("user nickname not found in the room after join "
            , nameIsOnMemberList(
            opSet2Room.getUserNickname(), opSet2Room.getMembers()));

        //and we check on the other side
        assertTrue("user nickname not found in the room after join, " +
            "from peer side"
            , nameIsOnMemberList(
            opSet2Room.getUserNickname(), opSet1Room.getMembers()));

        opSet1RoomCollector.collectedEvents.clear();

        // leaving
        opSet2Room.leave();

        opSet1RoomCollector.waitForEvent(10000);

        assertFalse("an user is reported present in a room he left"
            , opSet2Room.isJoined());

        // the nickname shouldnt be listed anymore
        assertFalse("user nickname is still on the room list after he left"
            , nameIsOnMemberList(opSet2Room.getUserNickname()
            , opSet2Room.getMembers()));

        // and the other side

        // does user1 received notifcation since user2 left
        assertEquals("an user joined and left and a room member missed events "
            , 1, opSet1RoomCollector.collectedEvents.size());

        ChatRoomMemberPresenceChangeEvent memberEvent =
            (ChatRoomMemberPresenceChangeEvent)
            opSet1RoomCollector.collectedEvents.get(0);

        assertEquals("user received an event of the wrong type "
            , ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT
            , memberEvent.getEventType());

        assertEquals("the user who left is not the expected one"
            , fixture.userID2
            , memberEvent.getChatRoomMember().getContactAddress());

        assertFalse("user nickname is still on the room list after he left, " +
            "from the peer side"
            , nameIsOnMemberList(opSet2Room.getUserNickname()
            , opSet1Room.getMembers()));

    }

    /**
     * In <tt>testNickName</tt>, user nicknames will be changed
     * and we will check if changes are well reflected on both user
     * sides.
     */
    public void testNickName()
        throws OperationFailedException,
               OperationNotSupportedException
    {
        String testRoomName = testRoomBaseName + roomID++;

        // we create the test room
        ChatRoom opSet1Room =
            opSetMUC1.createChatRoom(testRoomName, null);
        opSet1Room.join();

        String user1FirstNick = opSet1Room.getUserNickname();
        String user1SecondNick = "user1nickchange";

        ChatRoom opSet2Room = opSetMUC2.findRoom(testRoomName);
        opSet2Room.join();

        // checking the nickname from the peer side
        assertTrue("user nickname not found by peer "
            , nameIsOnMemberList(user1FirstNick, opSet2Room.getMembers()));

        MUCEventCollector opSet2RoomCollector =
            new MUCEventCollector(opSet2Room, MUCEventCollector.EVENT_PROPERTY);

        // change the nickname
        opSet1Room.setUserNickname(user1SecondNick);

        assertEquals("failed to change user nickname"
            , user1SecondNick, opSet1Room.getUserNickname());

        //user should be listed with his new nick name
        assertTrue(
            "user nickname not found on member list after modification"
            , nameIsOnMemberList(user1SecondNick, opSet1Room.getMembers()));

        // user2 have to wait for the event
        opSet2RoomCollector.waitForEvent(10000);

        assertEquals("no event received since a member changed his nick"
            , 1, opSet2RoomCollector.collectedEvents.size());

        ChatRoomMemberPropertyChangeEvent changeEvent =
            (ChatRoomMemberPropertyChangeEvent)
            opSet2RoomCollector.collectedEvents.get(0);

        assertEquals("the change event doesnt comes from the expected member"
            , fixture.userID1
            , changeEvent.getSourceChatRoomMember().getContactAddress());

        assertTrue("user nickname not found on member list after modification, " +
            "from peer side",
            nameIsOnMemberList(user1SecondNick, opSet2Room.getMembers()));

        //previous nickname should not rest on the list after change
        assertFalse(
            "both old and new nick are listed in a room after nick change"
            , nameIsOnMemberList(user1FirstNick, opSet1Room.getMembers()));

        assertFalse(
            "both old and new nick are listed in a room after nick change, " +
            "from the peer side",
            nameIsOnMemberList(user1FirstNick, opSet2Room.getMembers()));

        // trying to steal a nickname
        try
        {
            opSet2Room.setUserNickname(user1SecondNick);
        }
        catch (OperationFailedException ex)
        {
            // stealing nickname is not allowed by the room
        }
        if (opSet1Room.getUserNickname().equals(opSet2Room.getUserNickname()))
        {
            fail(
                "different users are holding the same nick in the same room");
        }
    }

    /**
     * <tt>testRoomSubject</tt> test changing the room subject.
     */
    public void testRoomSubject()
        throws OperationFailedException,
        OperationNotSupportedException
    {
        String testRoomName = testRoomBaseName + roomID++;

        String oldSubject;
        String newSubjet = "bingo";

        // we create the test room
        ChatRoom opSet1Room =
            opSetMUC1.createChatRoom(testRoomName, null);
        opSet1Room.join();

        ChatRoom opSet2Room = opSetMUC2.findRoom(testRoomName);
        opSet2Room.join();

        MUCEventCollector opSet2RoomCollector =
            new MUCEventCollector(opSet2Room, MUCEventCollector.EVENT_PROPERTY);

        MUCEventCollector opSet1RoomCollector =
            new MUCEventCollector(opSet2Room, MUCEventCollector.EVENT_PROPERTY);

        oldSubject = opSet1Room.getSubject();

        opSet1Room.setSubject(newSubjet);

        opSet1RoomCollector.waitForEvent(20000);
        opSet2RoomCollector.waitForEvent(20000);

        assertEquals("user1 didnt received an event for room subject change"
            , 1, opSet1RoomCollector.collectedEvents.size());

        assertEquals("user2 didnt received an event for room subject change"
            , 1, opSet2RoomCollector.collectedEvents.size());

        assertEquals("the room subject is not up to date "
            , newSubjet, opSet1Room.getSubject());

        assertEquals("the room subject is not up to date " +
            "from peer side"
            , newSubjet, opSet2Room.getSubject());

        ChatRoomPropertyChangeEvent changeEvent =
            (ChatRoomPropertyChangeEvent)
            opSet1RoomCollector.collectedEvents.get(0);

        assertEquals("the old subject provided by the change event " +
            "is not the good one"
            , oldSubject, changeEvent.getOldValue());

        assertEquals("the new subject provided by the change event " +
            "is not the good one"
            , newSubjet, changeEvent.getNewValue());

        ChatRoomPropertyChangeEvent peerEvent =
            (ChatRoomPropertyChangeEvent)
            opSet2RoomCollector.collectedEvents.get(0);

        assertEquals("both sides didn't received similar change event ",
            changeEvent.getOldValue(), peerEvent.getOldValue());

        assertEquals("both sides didn't received similar change event ",
            changeEvent.getNewValue(), peerEvent.getNewValue());
    }

    /**
     * Here we test conference messaging : sending, receiving messages and
     * corresponding events.
     */
    public void testConferenceChat()
        throws OperationFailedException,
               OperationNotSupportedException
    {
        String testRoomName = testRoomBaseName + roomID++;

        logger.info("--- Start testConferenceChat room:" + testRoomName);

        // we create the test room
        ChatRoom opSet1Room =
            opSetMUC1.createChatRoom(testRoomName, null);
        opSet1Room.join();

        ChatRoom opSet2Room = opSetMUC2.findRoom(testRoomName);
        opSet2Room.join();

        // building and checking the message
        String message1 = "lorem ipsum first";
        Message opSet1Message = opSet1Room.createMessage(message1);

        assertEquals("created message content differ from the one provided "
            ,message1, opSet1Message.getContent());

        MUCEventCollector opSet1RoomCollector = new MUCEventCollector(
            opSet1Room, MUCEventCollector.EVENT_MESSAGE, true);

        MUCEventCollector opSet2RoomCollector = new MUCEventCollector(
            opSet2Room, MUCEventCollector.EVENT_MESSAGE, true);

        // ship it
        opSet1Room.sendMessage(opSet1Message);

        opSet1RoomCollector.waitForEvent(10000);
        opSet2RoomCollector.waitForEvent(10000);

        // does every member received the message
        assertEquals("user1 didn't received an event for message delivery "
            , 1, opSet1RoomCollector.collectedEvents.size());
        assertEquals("user2 didn't received the message"
            , 1, opSet2RoomCollector.collectedEvents.size());

        ChatRoomMessageDeliveredEvent deliveryEvent =
            (ChatRoomMessageDeliveredEvent)
                opSet1RoomCollector.collectedEvents.get(0);

        assertEquals("message type is not CONVERSATION_MESSAGE_DELIVERED"
            , ChatRoomMessageDeliveredEvent.CONVERSATION_MESSAGE_DELIVERED
            , deliveryEvent.getEventType());

        assertSame("the message has been delivered to another room instance"
            , opSet1Room, deliveryEvent.getSourceChatRoom());

        assertEquals(
            "delivered message differ from sent message"
            , message1, deliveryEvent.getMessage().getContent());

        // what happened on user2 side
        ChatRoomMessageReceivedEvent messageEvent =
            (ChatRoomMessageReceivedEvent)
            opSet2RoomCollector.collectedEvents.get(0);

        assertEquals("message type is not CONVERSATION_MESSAGE_RECEIVED"
            , ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED
            , messageEvent.getEventType());

        assertSame("the message comes from another room instance"
            , opSet2Room, messageEvent.getSourceChatRoom());

        ChatRoomMember member = messageEvent.getSourceChatRoomMember();

        assertEquals("message comes from an unexpected user"
            , opSet1Room.getUserNickname(), member.getName());

        assertEquals(
            "received message differ from sent message"
            , message1, messageEvent.getMessage().getContent());

        logger.info("--- End testConferenceChat room:" + testRoomName);
    }

    /**
     * In <tt>testMemberBan</tt>, a member joins, then he is banned
     * and try to join again
     */
    public void testMemberBan()
        throws OperationFailedException,
        OperationNotSupportedException
    {
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException ex)
        {}

        String testRoomName = testRoomBaseName + roomID++;

        logger.info("--- Start testMemberBan room:" + testRoomName);

        // we create the test room
        ChatRoom opSet1Room =
            opSetMUC1.createChatRoom(testRoomName, null);
        opSet1Room.join();

        MUCEventCollector opSet1RoomCollector =
            new MUCEventCollector(
            opSet1Room, MUCEventCollector.EVENT_PRESENCE);

        ChatRoom opSet2Room = opSetMUC2.findRoom(testRoomName);
        opSet2Room.join();

        opSet1RoomCollector.waitForEvent(10000);

        assertTrue("user2 not on member list after join"
            , nameIsOnMemberList(fixture.userID2, opSet1Room.getMembers()));

        List<ChatRoomMember> members = opSet1Room.getMembers();

        ChatRoomMember memberToBan = null;
        for (ChatRoomMember member : members)
            if (member.getContactAddress().equals(fixture.userID2))
            {
                memberToBan = member;
                break;
            }

        if (memberToBan == null)
            throw new IllegalStateException("member to ban not found");

        opSet1RoomCollector =
            new MUCEventCollector(
            opSet1Room, MUCEventCollector.EVENT_ROLE);

        assertTrue("user2 not in a room he joined "
            , opSet2Room.isJoined());

        MUCEventCollector opSet2RoomCollector =
            new MUCEventCollector(
            opSet2Room, MUCEventCollector.EVENT_PRESENCE);

        opSet1Room.banParticipant(memberToBan, "testMemberBan");

        // when an user is kicked or banned, the smack lib deliver corresponding
        // events to remaining room members but nothing to the kicked or banned
        // user. altough, if we dont wait some amount of time, the banned user
        // could still been reported as joined on his side.
        opSet2RoomCollector.waitForEvent(2000);

        assertFalse("user2 still in a room after been banned"
            , opSet2Room.isJoined());

        opSet1RoomCollector.waitForEvent(10000);

        assertEquals("user1 didnt received an event for user2 ban"
            , 1, opSet1RoomCollector.collectedEvents.size());

        assertFalse("user2 still on member list after ban"
            , nameIsOnMemberList(fixture.userID2, opSet1Room.getMembers()));

        try
        {
            opSet2Room.join();
        }
        catch (Exception ex)
        {
            // user2 has not been only kicked but banned so
            // he must not be able to join
        }
        assertFalse("user2 just joined a room where he is banned"
            , opSet2Room.isJoined());

        logger.info("--- End testMemberBan room:" + testRoomName);
    }


    /**
     * In <tt>testMemberKick</tt>, a member joins, then he is banned
     * and try to join again
     */
    public void testMemberKick()
        throws OperationFailedException,
        OperationNotSupportedException
    {
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException ex)
        {}

        String testRoomName = testRoomBaseName + roomID++;

        logger.info("--- Start testMemberKick room:" + testRoomName);

        // we create the test room
        ChatRoom opSet1Room =
            opSetMUC1.createChatRoom(testRoomName, null);
        opSet1Room.join();

        MUCEventCollector opSet1RoomCollector =
            new MUCEventCollector(
            opSet1Room, MUCEventCollector.EVENT_PRESENCE);

        ChatRoom opSet2Room = opSetMUC2.findRoom(testRoomName);
        opSet2Room.join();

        opSet1RoomCollector.waitForEvent(10000);

        assertTrue("user2 not on member list after join"
            , nameIsOnMemberList(fixture.userID2, opSet1Room.getMembers()));

        List<ChatRoomMember> members = opSet1Room.getMembers();

        ChatRoomMember memberToKick = null;
        for (ChatRoomMember member : members)
            if (member.getContactAddress().equals(fixture.userID2))
            {
                memberToKick = member;
                break;
            }
        if (memberToKick == null)
            throw new IllegalStateException("member to kick not found");

        opSet1RoomCollector =
            new MUCEventCollector(
            opSet1Room, MUCEventCollector.EVENT_PRESENCE);

        assertTrue("user2 not in a room he joined "
            , opSet2Room.isJoined());

        MUCEventCollector opSet2RoomCollector =
            new MUCEventCollector(
            opSet2Room, MUCEventCollector.EVENT_PRESENCE);

        opSet1Room.kickParticipant(memberToKick, "testMemberKick");

        // when an user is kicked or banned, the smack lib deliver corresponding
        // events to remaining room members but nothing to the kicked or banned
        // user. altough, if we dont wait some amount of time, the banned user
        // could stils been reported as joined on his side.
        opSet2RoomCollector.waitForEvent(2000);

        assertFalse("user2 still in a room after been kicked"
            , opSet2Room.isJoined());

        opSet1RoomCollector.waitForEvent(10000);

        assertEquals("user1 didnt received an event for user2 kick"
            , 1, opSet1RoomCollector.collectedEvents.size());

        ChatRoomMemberPresenceChangeEvent changeEvent =
            (ChatRoomMemberPresenceChangeEvent)
            opSet1RoomCollector.collectedEvents.get(0);

        assertEquals("the event received by user1 is not "
            , ChatRoomMemberPresenceChangeEvent.MEMBER_KICKED
            , changeEvent.getEventType());

        assertEquals("the kicked member is not the one expected"
            , memberToKick.getContactAddress()
            , changeEvent.getChatRoomMember().getContactAddress());

        assertFalse("user2 still on member list after kick"
            , nameIsOnMemberList(fixture.userID2, opSet1Room.getMembers()));

        // contrary to a ban, an user can join after been kicked
        opSet2Room.join();

        assertTrue("user2 can't join after been kicked "
            , opSet2Room.isJoined());

        opSet1RoomCollector.waitForEvent(10000);

        assertTrue("user2 not on list when joining after a kick"
            , nameIsOnMemberList(fixture.userID2, opSet1Room.getMembers()));

        logger.info("--- End testMemberKick room:" + testRoomName);
    }

    /**
     * Tests participant roles when the room just has been created.
     * The member who has created the room should have OWNER role.
     * Others participants who are invited to the room should be MEMBER.
     *
     * @throws OperationNotSupportedException
     * @throws OperationFailedException
     */
    public void testInitialParticipantsRoles()
    throws OperationFailedException, OperationNotSupportedException
    {
        String roomName = testRoomBaseName + roomID;

        logger.info("--- Start testInitialParticipantsRoles room:" + roomName);

        // We create the test room
        ChatRoom roomUser1 = opSetMUC1.createChatRoom(roomName, null);
        roomUser1.join();

        MUCEventCollector roomUser1Collector =
            new MUCEventCollector(roomUser1, MUCEventCollector.EVENT_PRESENCE);
        roomUser1Collector.waitForEvent(10000); // wait for user1

        // User1 who just created the room is supposed to be the owner:
        assertEquals("Unexpected role for user1",
            roomUser1.getUserRole(), ChatRoomMemberRole.OWNER);
        assertEquals("Unexpected role for user1", roomUser1.getUserRole(),
            getRole(fixture.userID1, roomUser1.getMembers(),
                    roomUser1.getName(), true));

        // Both of our peers join the room:
        ChatRoom roomUser2 = opSetMUC2.findRoom(roomName);
        assertNotNull("Room can't be retrieved on user2's side", roomUser2);
        roomUser2.join();
        roomUser1Collector.waitForEvent(10000); // wait for user2

        ChatRoom roomUser3 = opSetMUC3.findRoom(roomName);
        assertNotNull("Room can't be retrieved on user3's side", roomUser2);
        roomUser3.join();
        roomUser1Collector.waitForEvent(10000); // wait for user3

        assertTrue("User2 not on member list after having join the room"
            , nameIsOnMemberList(fixture.userID2, roomUser1.getMembers()));
        assertTrue("User3 not on member list after having join the room"
            , nameIsOnMemberList(fixture.userID3, roomUser1.getMembers()));

        List<ChatRoomMember> members = roomUser1.getMembers();

        // Three members are supposed to be in the room:
        assertEquals("Unexpected members count", 3, members.size());

        // We are sure two of these members are our peers (checked before the
        // last assertion), now we make sure they are both members:
        // (Note that the first element of the list refers to the owner)
        assertEquals("The current implementation requires that room's new " +
            "comers must have MEMBER role",
            ChatRoomMemberRole.MEMBER, getRole(fixture.userID2, members,
                                        roomName, true));
        assertEquals("The current implementation requires that room's new " +
            "comers must have the MEMBER role",
            ChatRoomMemberRole.MEMBER, getRole(fixture.userID3, members,
                                        roomName, true));

        logger.info("--- End testInitialParticipantsRoles room:" + roomName);
    }

    /**
     * Search the user in the list by address and return its role.
     * @param userID
     * @param members
     * @param  roomName
     * @param printDebugInfo
     * @return
     */
    private ChatRoomMemberRole getRole(String userID,
                                       List<ChatRoomMember> members,
                                       String roomName,
                                       boolean printDebugInfo)
    {
        if(printDebugInfo)
            logger.info("Search in mems of " + roomName + " for " + userID);

        ChatRoomMemberRole roleFound = null;
        for(int i = 0; i < members.size(); i++)
        {
            ChatRoomMember mem = members.get(i);
            if(mem.getContactAddress().equals(userID))
                roleFound = mem.getRole();

            if(printDebugInfo)
                logger.info("m[" + i + "] "
                    + mem.getContactAddress() + " r:" + mem.getRole());
        }

        if(printDebugInfo)
            logger.info("found role: " + roleFound);

        return roleFound;
    }

    /**
     * The owner of the room (the user who created it, and which is an
     * administrator too, typically user1) will grant membership's role to one
     * of our peers (user2).
     * Membership means a long-life affiliation to the room, rather than being a
     * guest.
     *
     * @throws OperationNotSupportedException
     * @throws OperationFailedException
     */
    public void testGrantMembership()
    throws OperationFailedException, OperationNotSupportedException
    {
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException ex)
        {}

        String roomName = testRoomBaseName + roomID;

        logger.info("--- Start testGrantMembership room:" + roomName);

        ChatRoom roomUser1 = opSetMUC1.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user1's side", roomUser1);

        ChatRoom roomUser3 = opSetMUC3.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user3's side", roomUser3);

        MUCEventCollector roomUser1Col = new MUCEventCollector(
            roomUser1, MUCEventCollector.EVENT_ROLE);
        MUCEventCollector roomUser3Col = new MUCEventCollector(
            roomUser3, MUCEventCollector.EVENT_ROLE);

        // Owner of the room grants user2 member rights:
        roomUser1.grantMembership(fixture.userID2);

        roomUser1Col.waitForEvent(10000);
        roomUser3Col.waitForEvent(10000);

        assertEquals("Wrong count of collected events on user1's side",
            1, roomUser1Col.collectedEvents.size());
        ChatRoomMemberRoleChangeEvent roleEventUser1 =
            (ChatRoomMemberRoleChangeEvent) roomUser1Col.collectedEvents.get(0);

        assertEquals("Wrong count of collected events on user3's side",
            1, roomUser3Col.collectedEvents.size());
        ChatRoomMemberRoleChangeEvent roleEventUser3 =
            (ChatRoomMemberRoleChangeEvent) roomUser3Col.collectedEvents.get(0);

        assertEquals("Collected event does not belong to the right chatroom",
            roomName, StringUtils.parseName(
                roleEventUser1.getSourceChatRoom().getName()));
        assertEquals("Collected event does not belong to the expected user",
            fixture.userID2,
            roleEventUser1.getSourceMember().getContactAddress());
        assertEquals("User2's previous role does not match",
            ChatRoomMemberRole.GUEST, roleEventUser1.getPreviousRole());
        assertEquals("Unexpected new role",
            ChatRoomMemberRole.MEMBER, roleEventUser1.getNewRole());

        assertEquals("Collected event does not belong to the right chatroom",
            roomName, StringUtils.parseName(
                roleEventUser3.getSourceChatRoom().getName()));
        assertEquals("Collected event does not belong to the expected user",
            fixture.userID2,
            roleEventUser3.getSourceMember().getContactAddress());
        assertEquals("User2's previous role does not match",
            ChatRoomMemberRole.GUEST, roleEventUser3.getPreviousRole());
        assertEquals("Unexpected new role",
            ChatRoomMemberRole.MEMBER, roleEventUser3.getNewRole());

        logger.info("--- End testGrantMembership room:" + roomName);
    }

    /**
     * The owner of the room (the user who created it, and which is an
     * administrator too, typically user1) will grant moderator role to one
     * of our peers (user2).
     * A moderator is allowed to kick users, grant and revoke voice,
     * invite other users, modify room's subject plus all the participants
     * privileges.
     *
     * @throws OperationNotSupportedException
     * @throws OperationFailedException
     */
    public void testGrantModerator()
    throws OperationFailedException, OperationNotSupportedException
    {
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException ex)
        {}

        String roomName = testRoomBaseName + roomID;

        logger.info("--- Start testGrantModerator room:" + roomName);

        ChatRoom roomUser1 = opSetMUC1.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user1's side", roomUser1);

        ChatRoom roomUser3 = opSetMUC3.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user3's side", roomUser3);

        MUCEventCollector roomUser1Col = new MUCEventCollector(
            roomUser1, MUCEventCollector.EVENT_ROLE);
        MUCEventCollector roomUser3Col = new MUCEventCollector(
            roomUser3, MUCEventCollector.EVENT_ROLE);

        // Owner of the room grants user2 moderator rights:
        roomUser1.grantModerator(fixture.userID2);

        roomUser1Col.waitForEvent(10000);
        roomUser3Col.waitForEvent(10000);

        assertEquals("Wrong count of collected events on user1's side",
            1, roomUser1Col.collectedEvents.size());
        ChatRoomMemberRoleChangeEvent roleEventUser1 =
            (ChatRoomMemberRoleChangeEvent) roomUser1Col.collectedEvents.get(0);

        assertEquals("Wrong count of collected events on user3's side",
            1, roomUser3Col.collectedEvents.size());
        ChatRoomMemberRoleChangeEvent roleEventUser3 =
            (ChatRoomMemberRoleChangeEvent) roomUser3Col.collectedEvents.get(0);

        assertEquals("Collected event does not belong to the right chatroom",
            roomName, StringUtils.parseName(
                roleEventUser1.getSourceChatRoom().getName()));
        assertEquals("Collected event does not belong to the expected user",
            fixture.userID2,
            roleEventUser1.getSourceMember().getContactAddress());
        assertEquals("User2's previous role does not match",
            ChatRoomMemberRole.MEMBER, roleEventUser1.getPreviousRole());
        assertEquals("Unexpected new role",
            ChatRoomMemberRole.MODERATOR, roleEventUser1.getNewRole());

        assertEquals("Collected event does not belong to the right chatroom",
            roomName, StringUtils.parseName(
                roleEventUser3.getSourceChatRoom().getName()));
        assertEquals("Collected event does not belong to the expected user",
            fixture.userID2,
            roleEventUser3.getSourceMember().getContactAddress());
        assertEquals("User2's previous role does not match",
            ChatRoomMemberRole.MEMBER, roleEventUser3.getPreviousRole());
        assertEquals("Unexpected new role",
            ChatRoomMemberRole.MODERATOR, roleEventUser3.getNewRole());

        logger.info("--- End testGrantModerator room:" + roomName);
    }

    /**
     * User2 which is currently moderator in the room for our scenario will
     * revoke voice to user3, thanks to his moderator privileges.
     *
     * We will first check that user3 has become a silent member before making
     * sure that he can't send messages through the room.
     *
     * @throws OperationNotSupportedException
     * @throws OperationFailedException
     */
    public void testRevokeVoice()
    throws OperationFailedException, OperationNotSupportedException
    {
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException ex)
        {}

        String roomName = testRoomBaseName + roomID;

        logger.info("---= Start test for RevokeVoice =--- room: " + roomName);

        ChatRoom roomUser1 = opSetMUC1.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user1's side", roomUser1);

        ChatRoom roomUser2 = opSetMUC2.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user2's side", roomUser2);

        ChatRoom roomUser3 = opSetMUC3.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user3's side", roomUser3);

        MUCEventCollector roomUser1Col = new MUCEventCollector(
            roomUser1, MUCEventCollector.EVENT_ROLE);
        MUCEventCollector roomUser2Col = new MUCEventCollector(
            roomUser2, MUCEventCollector.EVENT_ROLE);

        // User2 (moderator role) revokes voice right to user2 (guest):
        roomUser2.revokeVoice(fixture.userID3);

        roomUser1Col.waitForEvent(10000);
        roomUser2Col.waitForEvent(10000);

        assertEquals("Wrong count of collected events on user1's side",
            1, roomUser1Col.collectedEvents.size());
        ChatRoomMemberRoleChangeEvent roleEventUser1 =
            (ChatRoomMemberRoleChangeEvent) roomUser1Col.collectedEvents.get(0);

        assertEquals("Wrong count of collected events on user2's side",
            1, roomUser2Col.collectedEvents.size());
        ChatRoomMemberRoleChangeEvent roleEventUser2 =
            (ChatRoomMemberRoleChangeEvent) roomUser2Col.collectedEvents.get(0);

        assertEquals("Collected event does not belong to the right chatroom",
            roomName, StringUtils.parseName(
                roleEventUser1.getSourceChatRoom().getName()));
        assertEquals("Collected event does not belong to the expected user",
            fixture.userID3,
            roleEventUser1.getSourceMember().getContactAddress());
        assertEquals("User3's previous role does not match",
            ChatRoomMemberRole.MEMBER, roleEventUser1.getPreviousRole());
        assertEquals("Unexpected new role",
            ChatRoomMemberRole.SILENT_MEMBER, roleEventUser1.getNewRole());

        assertEquals("Collected event does not belong to the right chatroom",
            roomName, StringUtils.parseName(
                roleEventUser2.getSourceChatRoom().getName()));
        assertEquals("Collected event does not belong to the expected user",
            fixture.userID3,
            roleEventUser2.getSourceMember().getContactAddress());
        assertEquals("User3's previous role does not match",
            ChatRoomMemberRole.MEMBER, roleEventUser2.getPreviousRole());
        assertEquals("Unexpected new role",
            ChatRoomMemberRole.SILENT_MEMBER, roleEventUser2.getNewRole());

        logger.info("---= End test for RevokeVoice =--- room: " + roomName);
    }

    /**
     * User2 which is currently moderator in the room for our scenario will
     * grant voice to user3 (thanks to his moderator privileges) after having
     * revoked him it in the previous test of the suite.
     *
     * We will first check that user3 has become a guest before making
     * sure that he can again send messages through the room.
     *
     * @throws OperationNotSupportedException
     * @throws OperationFailedException
     */
    public void testGrantVoice()
    throws OperationFailedException, OperationNotSupportedException
    {
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException ex)
        {}

        String roomName = testRoomBaseName + roomID;

        logger.info("---= Start test for GrantVoice =--- room: " + roomName);

        ChatRoom roomUser1 = opSetMUC1.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user1's side", roomUser1);

        ChatRoom roomUser2 = opSetMUC2.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user2's side", roomUser2);

        MUCEventCollector roomUser1Col = new MUCEventCollector(
            roomUser1, MUCEventCollector.EVENT_ROLE);
        MUCEventCollector roomUser2Col = new MUCEventCollector(
            roomUser2, MUCEventCollector.EVENT_ROLE);

        // User2 of the room grants voice right to user2:
        roomUser2.grantVoice(fixture.userID3);

        roomUser1Col.waitForEvent(10000);
        roomUser2Col.waitForEvent(10000);

        assertEquals("Wrong count of collected events on user1's side",
            1, roomUser1Col.collectedEvents.size());
        ChatRoomMemberRoleChangeEvent roleEventUser1 =
            (ChatRoomMemberRoleChangeEvent) roomUser1Col.collectedEvents.get(0);

        assertEquals("Wrong count of collected events on user3's side",
            1, roomUser2Col.collectedEvents.size());
        ChatRoomMemberRoleChangeEvent roleEventUser2 =
            (ChatRoomMemberRoleChangeEvent) roomUser2Col.collectedEvents.get(0);

        assertEquals("Collected event does not belong to the right chatroom",
            roomName, StringUtils.parseName(
                roleEventUser1.getSourceChatRoom().getName()));
        assertEquals("Collected event does not belong to the expected user",
            fixture.userID3,
            roleEventUser1.getSourceMember().getContactAddress());
        assertEquals("User3's previous role does not match",
            ChatRoomMemberRole.SILENT_MEMBER, roleEventUser1.getPreviousRole());
        assertEquals("Unexpected new role",
            ChatRoomMemberRole.MEMBER, roleEventUser1.getNewRole());

        assertEquals("Collected event does not belong to the right chatroom",
            roomName, StringUtils.parseName(
                roleEventUser2.getSourceChatRoom().getName()));
        assertEquals("Collected event does not belong to the expected user",
            fixture.userID3,
            roleEventUser2.getSourceMember().getContactAddress());
        assertEquals("User3's previous role does not match",
            ChatRoomMemberRole.SILENT_MEMBER, roleEventUser2.getPreviousRole());
        assertEquals("Unexpected new role",
            ChatRoomMemberRole.MEMBER, roleEventUser2.getNewRole());

        logger.info("---= End test for GrantVoice =--- room: " + roomName);
    }

    /**
     * The owner of the room (the user who created it, and which is an
     * administrator too, typically user1) will grant administrator role to one
     * of our peers (user2).
     * An administrator is allowed to perform administrative functions such as
     * banning users and edit moderator list.
     *
     * @throws OperationNotSupportedException
     * @throws OperationFailedException
     */
    public void testGrantAdmin()
    throws OperationFailedException, OperationNotSupportedException
    {
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException ex)
        {}

        String roomName = testRoomBaseName + roomID;

        logger.info("--- Start testGrantAdmin room:" + roomName);

        ChatRoom roomUser1 = opSetMUC1.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user1's side", roomUser1);

        ChatRoom roomUser2 = opSetMUC2.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user3's side", roomUser2);

        ChatRoom roomUser3 = opSetMUC3.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user3's side", roomUser3);

        MUCEventCollector roomUser1Col = new MUCEventCollector(
            roomUser1, MUCEventCollector.EVENT_ROLE);
        MUCEventCollector roomUser3Col = new MUCEventCollector(
            roomUser3, MUCEventCollector.EVENT_ROLE);

        // Owner of the room grants administrator rights to user2:
        roomUser1.grantAdmin(fixture.userID2);

        roomUser1Col.waitForEvent(10000);
        roomUser3Col.waitForEvent(10000);

        assertEquals("Wrong count of collected events on user1's side",
            1, roomUser1Col.collectedEvents.size());
        ChatRoomMemberRoleChangeEvent roleEventUser1 =
            (ChatRoomMemberRoleChangeEvent) roomUser1Col.collectedEvents.get(0);

        assertEquals("Wrong count of collected events on user3's side",
            1, roomUser3Col.collectedEvents.size());
        ChatRoomMemberRoleChangeEvent roleEventUser3 =
            (ChatRoomMemberRoleChangeEvent) roomUser3Col.collectedEvents.get(0);

        assertEquals("Collected event does not belong to the right chatroom",
            roomName, StringUtils.parseName(
                roleEventUser1.getSourceChatRoom().getName()));
        assertEquals("Collected event does not belong to the expected user",
            fixture.userID2,
            roleEventUser1.getSourceMember().getContactAddress());
        assertEquals("User2's previous role does not match",
            ChatRoomMemberRole.MEMBER, roleEventUser1.getPreviousRole());
        assertEquals("Unexpected new role",
            ChatRoomMemberRole.ADMINISTRATOR, roleEventUser1.getNewRole());

        assertEquals("Collected event does not belong to the right chatroom",
            roomName, StringUtils.parseName(
                roleEventUser3.getSourceChatRoom().getName()));
        assertEquals("Collected event does not belong to the expected user",
            fixture.userID2,
            roleEventUser3.getSourceMember().getContactAddress());
        assertEquals("User2's previous role does not match",
            ChatRoomMemberRole.MEMBER, roleEventUser3.getPreviousRole());
        assertEquals("Unexpected new role",
            ChatRoomMemberRole.ADMINISTRATOR, roleEventUser3.getNewRole());

        logger.info("--- End testGrantAdmin room:" + roomName);
    }

    /**
     * The owner of the room (the user who created it, and which is an
     * administrator too, typically user1) will grant ownership privileges to
     * one of our peers (user2).
     * An owner is allowed to change defining room features as well as perform
     * all administrative functions.
     *
     * @throws OperationNotSupportedException
     * @throws OperationFailedException
     */
    public void testGrantOwnership()
    throws OperationFailedException, OperationNotSupportedException
    {
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException ex)
        {}

        String roomName = testRoomBaseName + roomID;

        logger.info("--- Start testGrantOwnership room:" + roomName);

        ChatRoom roomUser1 = opSetMUC1.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user1's side", roomUser1);

        ChatRoom roomUser3 = opSetMUC3.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user3's side", roomUser3);

        MUCEventCollector roomUser1Col = new MUCEventCollector(
            roomUser1, MUCEventCollector.EVENT_ROLE);
        MUCEventCollector roomUser3Col = new MUCEventCollector(
            roomUser3, MUCEventCollector.EVENT_ROLE);

        // Owner of the room grants ownership privileges to user2:
        roomUser1.grantOwnership(fixture.userID2);

        roomUser1Col.waitForEvent(10000);
        roomUser3Col.waitForEvent(10000);

        assertEquals("Wrong count of collected events on user1's side",
            1, roomUser1Col.collectedEvents.size());
        ChatRoomMemberRoleChangeEvent roleEventUser1 =
            (ChatRoomMemberRoleChangeEvent) roomUser1Col.collectedEvents.get(0);

        assertEquals("Wrong count of collected events on user3's side",
            1, roomUser3Col.collectedEvents.size());
        ChatRoomMemberRoleChangeEvent roleEventUser3 =
            (ChatRoomMemberRoleChangeEvent) roomUser3Col.collectedEvents.get(0);

        assertEquals("Collected event does not belong to the right chatroom",
            roomName, StringUtils.parseName(
                roleEventUser1.getSourceChatRoom().getName()));
        assertEquals("Collected event does not belong to the expected user",
            fixture.userID2,
            roleEventUser1.getSourceMember().getContactAddress());
        assertEquals("User2's previous role does not match",
            ChatRoomMemberRole.ADMINISTRATOR, roleEventUser1.getPreviousRole());
        assertEquals("Unexpected new role",
            ChatRoomMemberRole.OWNER, roleEventUser1.getNewRole());

        assertEquals("Collected event does not belong to the right chatroom",
            roomName, StringUtils.parseName(
                roleEventUser3.getSourceChatRoom().getName()));
        assertEquals("Collected event does not belong to the expected user",
            fixture.userID2,
            roleEventUser3.getSourceMember().getContactAddress());
        assertEquals("User2's previous role does not match",
            ChatRoomMemberRole.ADMINISTRATOR, roleEventUser3.getPreviousRole());
        assertEquals("Unexpected new role",
            ChatRoomMemberRole.OWNER, roleEventUser3.getNewRole());

        logger.info("--- End testGrantOwnership room:" + roomName);
    }

    /**
     * The owner of the room (the user who created it, and which is an
     * administrator too, typically user1) will revoke membership's role to one
     * of our peers (user2).
     * The considered participant is then supposed to be a guest.
     *
     * @throws OperationNotSupportedException
     * @throws OperationFailedException
     */
    public void testRevokeMembership()
    throws OperationFailedException, OperationNotSupportedException
    {
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException ex)
        {}

        String roomName = testRoomBaseName + roomID;

        logger.info("--- Start testRevokeMembership room:" + roomName);

        ChatRoom roomUser1 = opSetMUC1.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user1's side", roomUser1);

        ChatRoom roomUser3 = opSetMUC3.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user3's side", roomUser3);

        MUCEventCollector roomUser3Col = new MUCEventCollector(
            roomUser3, MUCEventCollector.EVENT_ROLE);

        // Owner of the room revokes user2 member rights:
        roomUser1.revokeMembership(fixture.userID2);

        roomUser3Col.waitForEvent(10000);

        assertEquals("Wrong count of collected events on user3's side",
            1, roomUser3Col.collectedEvents.size());
        ChatRoomMemberRoleChangeEvent roleEventUser3 =
            (ChatRoomMemberRoleChangeEvent) roomUser3Col.collectedEvents.get(0);

        assertEquals("Collected event does not belong to the right chatroom",
            roomName, StringUtils.parseName(
                roleEventUser3.getSourceChatRoom().getName()));
        assertEquals("Collected event does not belong to the expected user",
            fixture.userID2,
            roleEventUser3.getSourceMember().getContactAddress());
        assertEquals("User2's previous role does not match",
            ChatRoomMemberRole.MEMBER, roleEventUser3.getPreviousRole());
        assertEquals("Unexpected new role",
            ChatRoomMemberRole.GUEST, roleEventUser3.getNewRole());

        logger.info("--- End testRevokeMembership room:" + roomName);
    }

    /**
     * The owner of the room (the user who created it, and which is an
     * administrator too, typically user1) will revoke moderator role to user2.
     * The considered participant is then supposed to be a member.
     *
     * @throws OperationNotSupportedException
     * @throws OperationFailedException
     */
    public void testRevokeModerator()
    throws OperationFailedException, OperationNotSupportedException
    {
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException ex)
        {}

        String roomName = testRoomBaseName + roomID;

        logger.info("--- Start testRevokeModerator room:" + roomName);

        ChatRoom roomUser1 = opSetMUC1.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user1's side", roomUser1);

        ChatRoom roomUser3 = opSetMUC3.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user3's side", roomUser3);

        MUCEventCollector roomUser1Col = new MUCEventCollector(
            roomUser1, MUCEventCollector.EVENT_ROLE);
        MUCEventCollector roomUser3Col = new MUCEventCollector(
            roomUser3, MUCEventCollector.EVENT_ROLE);

        // Owner of the room revokes user2 member rights:
        roomUser1.revokeModerator(fixture.userID2);

        roomUser1Col.waitForEvent(10000);
        roomUser3Col.waitForEvent(10000);

        assertEquals("Wrong count of collected events on user1's side",
            1, roomUser1Col.collectedEvents.size());
        ChatRoomMemberRoleChangeEvent roleEventUser1 =
            (ChatRoomMemberRoleChangeEvent) roomUser1Col.collectedEvents.get(0);

        assertEquals("Wrong count of collected events on user3's side",
            1, roomUser3Col.collectedEvents.size());
        ChatRoomMemberRoleChangeEvent roleEventUser3 =
            (ChatRoomMemberRoleChangeEvent) roomUser3Col.collectedEvents.get(0);

        assertEquals("Collected event does not belong to the right chatroom",
            roomName, StringUtils.parseName(
                roleEventUser1.getSourceChatRoom().getName()));
        assertEquals("Collected event does not belong to the expected user",
            fixture.userID2,
            roleEventUser1.getSourceMember().getContactAddress());
        assertEquals("User2's previous role does not match",
            ChatRoomMemberRole.MODERATOR, roleEventUser1.getPreviousRole());
        assertEquals("Unexpected new role",
            ChatRoomMemberRole.MEMBER, roleEventUser1.getNewRole());

        assertEquals("Collected event does not belong to the right chatroom",
            roomName, StringUtils.parseName(
                roleEventUser3.getSourceChatRoom().getName()));
        assertEquals("Collected event does not belong to the expected user",
            fixture.userID2,
            roleEventUser3.getSourceMember().getContactAddress());
        assertEquals("User2's previous role does not match",
            ChatRoomMemberRole.MODERATOR, roleEventUser3.getPreviousRole());
        assertEquals("Unexpected new role",
            ChatRoomMemberRole.MEMBER, roleEventUser3.getNewRole());

        logger.info("--- End testRevokeModerator room:" + roomName);
    }

    /**
     * The owner of the room (the user who created it, and which is an
     * administrator too, typically user1) will revoke administrator role to
     * user2.
     * The considered participant is then supposed to be a moderator.
     *
     * @throws OperationNotSupportedException
     * @throws OperationFailedException
     */
    public void testRevokeAdmin()
    throws OperationFailedException, OperationNotSupportedException
    {
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException ex)
        {}

        String roomName = testRoomBaseName + roomID;

        logger.info("--- Start testRevokeAdmin room:" + roomName);

        ChatRoom roomUser1 = opSetMUC1.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user1's side", roomUser1);

        ChatRoom roomUser3 = opSetMUC3.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user3's side", roomUser3);

        MUCEventCollector roomUser1Col = new MUCEventCollector(
            roomUser1, MUCEventCollector.EVENT_ROLE);
        MUCEventCollector roomUser3Col = new MUCEventCollector(
            roomUser3, MUCEventCollector.EVENT_ROLE);

        // Owner of the room revokes user2 member rights:
        roomUser1.revokeAdmin(fixture.userID2);

        roomUser1Col.waitForEvent(10000);
        roomUser3Col.waitForEvent(10000);

        assertEquals("Wrong count of collected events on user1's side",
            1, roomUser1Col.collectedEvents.size());
        ChatRoomMemberRoleChangeEvent roleEventUser1 =
            (ChatRoomMemberRoleChangeEvent) roomUser1Col.collectedEvents.get(0);

        assertEquals("Wrong count of collected events on user3's side",
            1, roomUser3Col.collectedEvents.size());
        ChatRoomMemberRoleChangeEvent roleEventUser3 =
            (ChatRoomMemberRoleChangeEvent) roomUser3Col.collectedEvents.get(0);

        assertEquals("Collected event does not belong to the right chatroom",
            roomName, StringUtils.parseName(
                roleEventUser1.getSourceChatRoom().getName()));
        assertEquals("Collected event does not belong to the expected user",
            fixture.userID2,
            roleEventUser1.getSourceMember().getContactAddress());
        assertEquals("User2's previous role does not match",
            ChatRoomMemberRole.ADMINISTRATOR, roleEventUser1.getPreviousRole());
        assertEquals("Unexpected new role",
            ChatRoomMemberRole.MODERATOR, roleEventUser1.getNewRole());

        assertEquals("Collected event does not belong to the right chatroom",
            roomName, StringUtils.parseName(
                roleEventUser3.getSourceChatRoom().getName()));
        assertEquals("Collected event does not belong to the expected user",
            fixture.userID2,
            roleEventUser3.getSourceMember().getContactAddress());
        assertEquals("User2's previous role does not match",
            ChatRoomMemberRole.ADMINISTRATOR, roleEventUser3.getPreviousRole());
        assertEquals("Unexpected new role",
            ChatRoomMemberRole.MODERATOR, roleEventUser3.getNewRole());

        logger.info("--- End testRevokeAdmin room:" + roomName);
    }

    /**
     * The owner of the room (the user who created it, and which is an
     * administrator too, typically user1) will revoke ownership rights to
     * user2.
     * The considered participant is then supposed to be an admin.
     *
     * @throws OperationNotSupportedException
     * @throws OperationFailedException
     */
    public void testRevokeOwnership()
    throws OperationFailedException, OperationNotSupportedException
    {
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException ex)
        {}

        String roomName = testRoomBaseName + roomID;

        logger.info("--- Start testRevokeOwnership room:" + roomName);

        ChatRoom roomUser1 = opSetMUC1.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user1's side", roomUser1);

        ChatRoom roomUser3 = opSetMUC3.findRoom(roomName);
        assertNotNull("The room can't be retrieved on user3's side", roomUser3);

        MUCEventCollector roomUser1Col = new MUCEventCollector(
            roomUser1, MUCEventCollector.EVENT_ROLE);
        MUCEventCollector roomUser3Col = new MUCEventCollector(
            roomUser3, MUCEventCollector.EVENT_ROLE);

        // Owner of the room revokes user2 member rights:
        roomUser1.revokeOwnership(fixture.userID2);

        roomUser1Col.waitForEvent(10000);
        roomUser3Col.waitForEvent(10000);

        assertEquals("Wrong count of collected events on user1's side",
            1, roomUser1Col.collectedEvents.size());
        ChatRoomMemberRoleChangeEvent roleEventUser1 =
            (ChatRoomMemberRoleChangeEvent) roomUser1Col.collectedEvents.get(0);

        assertEquals("Wrong count of collected events on user3's side",
            1, roomUser3Col.collectedEvents.size());
        ChatRoomMemberRoleChangeEvent roleEventUser3 =
            (ChatRoomMemberRoleChangeEvent) roomUser3Col.collectedEvents.get(0);

        assertEquals("Collected event does not belong to the right chatroom",
            roomName, StringUtils.parseName(
                roleEventUser1.getSourceChatRoom().getName()));
        assertEquals("Collected event does not belong to the expected user",
            fixture.userID2,
            roleEventUser1.getSourceMember().getContactAddress());
        assertEquals("User2's previous role does not match",
            ChatRoomMemberRole.OWNER, roleEventUser1.getPreviousRole());
        assertEquals("Unexpected new role",
            ChatRoomMemberRole.ADMINISTRATOR, roleEventUser1.getNewRole());

        assertEquals("Collected event does not belong to the right chatroom",
            roomName, StringUtils.parseName(
                roleEventUser3.getSourceChatRoom().getName()));
        assertEquals("Collected event does not belong to the expected user",
            fixture.userID2,
            roleEventUser3.getSourceMember().getContactAddress());
        assertEquals("User2's previous role does not match",
            ChatRoomMemberRole.OWNER, roleEventUser3.getPreviousRole());
        assertEquals("Unexpected new role",
            ChatRoomMemberRole.ADMINISTRATOR, roleEventUser3.getNewRole());

        logger.info("--- End testRevokeOwnership room:" + roomName);
    }

    /**
     * Utility class used to collect events received during tests.
     */
    class MUCEventCollector
        implements ChatRoomInvitationRejectionListener,
                   ChatRoomInvitationListener,
                   LocalUserChatRoomPresenceListener,
                   ChatRoomMemberPresenceListener,
                   ChatRoomMessageListener,
                   ChatRoomMemberPropertyChangeListener,
                   ChatRoomPropertyChangeListener,
                   ChatRoomMemberRoleListener,
                   ChatRoomLocalUserRoleListener
    {
        private final ArrayList<EventObject> collectedEvents
            = new ArrayList<EventObject>();

        private int waitCount = 0;

        @SuppressWarnings("unused")
        private final OperationSetMultiUserChat opSet;

        @SuppressWarnings("unused")
        private final ChatRoom room;

        private static final int EVENT_INVITE = 1;

        private static final int EVENT_PRESENCE = 2;

        private static final int EVENT_MESSAGE = 3;

        private static final int EVENT_PROPERTY = 4;

        private static final int EVENT_ROLE = 5;

        /**
         * Should we skip system messages in chat rooms.
         */
        private boolean skipSystemMsgs = false;

        /**
         * Creates an event collector to listen for specific events from
         * the given opSet
         *
         * @param opSet the <tt>OperationSetMultiUserChat> from which we will
         * receive events.
         * @param  eventType indicates the kind of events we are looking for
         */
        public MUCEventCollector(
            OperationSetMultiUserChat opSet, int eventType)
        {
            this.opSet = opSet;
            room = null;
            switch (eventType)
            {
                case EVENT_INVITE:
                    opSet.addInvitationListener(this);
                    opSet.addInvitationRejectionListener(this);
                    break;
                case EVENT_PRESENCE:
                    opSet.addPresenceListener(this);
                    break;
                default:
                    throw new IllegalArgumentException(
                        "invalid event category " + eventType);
            }
        }

        /**
         * @param room the room we are listening to.
         * @param eventType indicates the kind of events we are looking for
         */
        public MUCEventCollector(ChatRoom room, int eventType)
        {
            this(room, eventType, false);
        }
        /**
         * @param room the room we are listening to.
         * @param eventType indicates the kind of events we are looking for
         * @param skipSystemMsgs should we skip system messages in chat rooms.
         */
        public MUCEventCollector(
            ChatRoom room, int eventType, boolean skipSystemMsgs)
        {
            this.room = room;
            this.skipSystemMsgs = skipSystemMsgs;
            opSet = null;
            switch (eventType)
            {
                case EVENT_PRESENCE:
                    room.addMemberPresenceListener(this);
                    break;
                case EVENT_MESSAGE:
                    room.addMessageListener(this);
                    break;
                case EVENT_PROPERTY:
                    room.addMemberPropertyChangeListener(this);
                    room.addPropertyChangeListener(this);
                    break;
                case EVENT_ROLE:
                    room.addMemberRoleListener(this);
                    room.addLocalUserRoleListener(this);
                    break;
                default:
                    throw new IllegalArgumentException(
                        "invalid event category " + eventType);
            }
        }

        /**
         * Blocks until at least one event is received or until waitFor
         * miliseconds pass (whichever happens first).
         *
         * @param waitFor the number of miliseconds that we should be waiting
         * for an event before simply bailing out.
         */
        public void waitForEvent(long waitFor)
        {
            logger.trace("Waiting for a MUC Event");

            synchronized (this)
            {
                if (collectedEvents.size() > waitCount)
                {
                    waitCount++;
                    logger.trace("Event already received. " + collectedEvents);
                    return;
                }
                try
                {
                    wait(waitFor);
                    if (collectedEvents.size() > waitCount)
                    {
                        logger.trace("Received a MUC event.");
                    }
                    else
                    {
                        logger.trace(
                            "MUC event missed after " + waitFor + "ms.");
                    }
                    waitCount++;
                }
                catch (InterruptedException ex)
                {
                    logger.debug(
                        "Interrupted while waiting for a MUC event", ex);
                }
            }
        }

        /**
         * utility method to collect an event
         * @param evt the event to collect
         */
        private void collectEvent(EventObject evt)
        {
            synchronized (this)
            {
                logger.debug(
                    "Collected evt(" + collectedEvents.size() + ")= " + evt);
                collectedEvents.add(evt);
                notifyAll();
            }
        }

        public void invitationRejected(
            ChatRoomInvitationRejectedEvent evt)
        {
            collectEvent(evt);
        }

        public void invitationReceived(
            ChatRoomInvitationReceivedEvent evt)
        {
            collectEvent(evt);
        }

        public void localUserPresenceChanged(
            LocalUserChatRoomPresenceChangeEvent evt)
        {
            collectEvent(evt);
        }

        public void memberPresenceChanged(
            ChatRoomMemberPresenceChangeEvent evt)
        {
            collectEvent(evt);
        }

        public void messageReceived(
            ChatRoomMessageReceivedEvent evt)
        {
            if(skipSystemMsgs && evt.getEventType()
                == ChatRoomMessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED)
                return;
            else
                collectEvent(evt);
        }

        public void messageDelivered(
            ChatRoomMessageDeliveredEvent evt)
        {
            collectEvent(evt);
        }

        public void messageDeliveryFailed(
            ChatRoomMessageDeliveryFailedEvent evt)
        {
            collectEvent(evt);
        }

        public void chatRoomPropertyChanged(
            ChatRoomMemberPropertyChangeEvent evt)
        {
            collectEvent(evt);
        }

        public void chatRoomPropertyChanged(
            ChatRoomPropertyChangeEvent evt)
        {
            collectEvent(evt);
        }

        public void chatRoomPropertyChangeFailed(
            ChatRoomPropertyChangeFailedEvent evt)
        {
            collectEvent(evt);
        }

        public void memberRoleChanged(
            ChatRoomMemberRoleChangeEvent evt)
        {
            collectEvent(evt);
        }

        public void localUserRoleChanged(
            ChatRoomLocalUserRoleChangeEvent evt)
        {
            collectEvent(evt);
        }
    }
}
