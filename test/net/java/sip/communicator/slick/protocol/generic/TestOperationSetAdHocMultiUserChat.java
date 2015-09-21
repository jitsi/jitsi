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
package net.java.sip.communicator.slick.protocol.generic;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Generic tests suite for the ad-hoc multi-user chat functionality.
 *
 * @author Valentin Martinet
 */
public abstract class TestOperationSetAdHocMultiUserChat extends TestCase
{
    private Logger logger = Logger.getLogger(
        TestOperationSetAdHocMultiUserChat.class);

    /**
     * The name for the AdHocChatRoom we will use in this test case.
     */
    protected static String adHocChatRoomName = "AdHocMUC-test";

    /**
     * A reason to be sent with an invitation for an ad-hoc chatroom.
     */
    protected static String invitationReason = "Free 4 a chat?";

    /**
     * A reason for the rejection of an invitation.
     */
    protected static String invitationRejectReason = "Sorry, no time 4 U.";

    /**
     * Fixture.
     */
    protected AdHocMultiUserChatSlickFixture fixture = null;

    public OperationSetPresence opSetPresence1 = null;
    public OperationSetPresence opSetPresence2 = null;
    public OperationSetPresence opSetPresence3 = null;

    public OperationSetAdHocMultiUserChat opSetAHMUC1 = null;
    public OperationSetAdHocMultiUserChat opSetAHMUC2 = null;
    public OperationSetAdHocMultiUserChat opSetAHMUC3 = null;

    /**
     * Constructor: creates the test with the specified method name.
     *
     * @param name the name of the method to execute.
     */
    public TestOperationSetAdHocMultiUserChat(String name)
    {
        super(name);
    }

    /**
     * JUnit setUp method.
     */
    @Override
    public void setUp() throws Exception
    {
        start();
    }

    /**
     * JUnit tearDown method.
     */
    @Override
    public void tearDown() throws Exception
    {
        fixture.tearDown();
    }

    public void start() throws Exception {}
    public void stop() {}

    /**
     * Creates an ad-hoc chat room and check if it's registered on the server.
     *
     * @throws OperationFailedException
     * @throws OperationNotSupportedException
     */
    public void testCreateRoom()
    throws OperationFailedException, OperationNotSupportedException
    {
        AdHocChatRoom adHocChatRoom = opSetAHMUC1.createAdHocChatRoom(
            adHocChatRoomName, new Hashtable<String,Object>());

        assertNotNull("The created ad-hoc room is null.", adHocChatRoom);

        // We wait some time to let the MsnSwitchboard attached to this room
        // started...
        Object wait = new Object();
        synchronized (wait)
        {
            try
            {
                wait.wait(10000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        // Check that we retrieved the only one room that should be available:
        assertEquals("The room can't be retrieved",
            1, opSetAHMUC1.getAdHocChatRooms().size());

    }

    /**
     * Creates an ad-hoc chat room in including participants, then check if it's
     * registered on the server.
     *
     * -Users will be part of the participants to invite when creating the room.
     * -Thez will accept the invitation so we'll check that thez'll be actually
     * in the room.
     * -Then they will leave the room. They will be invited again in another
     * test.
     *
     * NOTE that this test will be especially used by Yahoo! protocol because of
     * the fact that creating a conference chat with this protocol fails if any
     * participants are given to the dedicated constructor of the library.
     *
     * @throws OperationNotSupportedException
     * @throws OperationFailedException
     */
    public void testCreateRoomWithParticipants()
    throws OperationFailedException, OperationNotSupportedException
    {
        List<String> contacts = new ArrayList<String>();
        contacts.add(fixture.userID2);
        contacts.add(fixture.userID3);

        // Collectors allows to gather the events which are generated:
        AHMUCEventCollector collectorUser1 = null;
        AHMUCEventCollector collectorUser2 = new AHMUCEventCollector(
            opSetAHMUC2, AHMUCEventCollector.INVITATION_EVENT);
        AHMUCEventCollector collectorUser3 = new AHMUCEventCollector(
            opSetAHMUC3, AHMUCEventCollector.INVITATION_EVENT);

        // We create the room with the given contacts:
        // (NOTE that in Yahoo! adHocChatRoomName won't be considered!)
        AdHocChatRoom room = opSetAHMUC1.createAdHocChatRoom(
            adHocChatRoomName, contacts, invitationReason);

        assertNotNull("Returned room is null", room);

        // A room should have been created on user1's side:
        assertEquals("The room can't be retrieved",
            1, opSetAHMUC1.getAdHocChatRooms().size());
        assertNotNull("The newly created room is null",
            opSetAHMUC1.getAdHocChatRooms().get(0));

        collectorUser2.waitForEvent(40000);

        // Check that an event has been generated on the other side
        assertEquals("User2 didn't receive an invitation. Wrong number of " +
            "collected events", 1, collectorUser2.events.size());
        assertTrue("Unexpected event type", collectorUser2.events.get(0)
            instanceof AdHocChatRoomInvitationReceivedEvent);

        collectorUser3.waitForEvent(40000);

        assertEquals("User3 didn't receive an invitation. Wrong number of " +
            "collected events", 1, collectorUser3.events.size());
        assertTrue("Unexpected event type", collectorUser3.events.get(0)
            instanceof AdHocChatRoomInvitationReceivedEvent);

        // Check event's properties for user2:
        AdHocChatRoomInvitationReceivedEvent event2 =
            (AdHocChatRoomInvitationReceivedEvent) collectorUser2.events.get(0);

        assertEquals("Received invitation does NOT concern the right chatroom",
            opSetAHMUC1.getAdHocChatRooms().get(0).getName(),
            event2.getInvitation().getTargetAdHocChatRoom().getName());
        assertEquals("Received invitation does NOT come from expected user",
            fixture.userID1, event2.getInvitation().getInviter());
        assertEquals("Invitation's reason does NOT match",
            invitationReason, event2.getInvitation().getReason());

        // Check event's properties for user3:
        AdHocChatRoomInvitationReceivedEvent event3 =
            (AdHocChatRoomInvitationReceivedEvent) collectorUser3.events.get(0);

        assertEquals("Received invitation does NOT concern the right chatroom",
            opSetAHMUC1.getAdHocChatRooms().get(0).getName(),
            event3.getInvitation().getTargetAdHocChatRoom().getName());
        assertEquals("Received invitation does NOT come from expected user",
            fixture.userID1, event3.getInvitation().getInviter());
        assertEquals("Invitation's reason does NOT match",
            invitationReason, event3.getInvitation().getReason());

        collectorUser1 = new AHMUCEventCollector(
            opSetAHMUC1.getAdHocChatRooms().get(0),
            AHMUCEventCollector.PRESENCE_EVENT);

        //
        // Our guest accepts our invitation
        //
        assertEquals(1, opSetAHMUC2.getAdHocChatRooms().size());
        assertNotNull(opSetAHMUC2.getAdHocChatRooms().get(0));

        event2.getInvitation().getTargetAdHocChatRoom().join();

        collectorUser1.waitForEvent(40000);

        assertEquals("Wrong count of generated events",
            1, collectorUser1.events.size());
        assertTrue("Wrong event instance", collectorUser1.events.get(0)
            instanceof AdHocChatRoomParticipantPresenceChangeEvent);

        // First peer
        AdHocChatRoomParticipantPresenceChangeEvent presenceEvent2 =
            (AdHocChatRoomParticipantPresenceChangeEvent)
            collectorUser1.events.get(0);

        assertEquals("Presence event does NOT concern expected chatroom",
            opSetAHMUC1.getAdHocChatRooms().get(0).getName(),
            presenceEvent2.getAdHocChatRoom().getName());
        assertEquals("Wrong event type",
            AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_JOINED,
            presenceEvent2.getEventType());
        assertEquals("Presence event does NOT come from the expected user",
            fixture.userID2, presenceEvent2.getParticipant().getAddress());

        assertEquals("Unexpected participants count", 1,
            opSetAHMUC1.getAdHocChatRooms().get(0).getParticipantsCount());
        assertEquals("Unexpected room participant",
            fixture.userID2,
            opSetAHMUC1.getAdHocChatRooms().get(0).getParticipants().get(0)
            .getAddress());

        // Second peer
        assertEquals(1, opSetAHMUC3.getAdHocChatRooms().size());
        assertNotNull(opSetAHMUC3.getAdHocChatRooms().get(0));

        event3.getInvitation().getTargetAdHocChatRoom().join();

        collectorUser1.waitForEvent(20000);

        assertEquals("Wrong count of generated events",
            2, collectorUser1.events.size());
        assertTrue("Wrong event instance", collectorUser1.events.get(1)
            instanceof AdHocChatRoomParticipantPresenceChangeEvent);

        AdHocChatRoomParticipantPresenceChangeEvent presenceEvent3 =
            (AdHocChatRoomParticipantPresenceChangeEvent)
            collectorUser1.events.get(1);

        assertEquals("Presence event does NOT concern expected chatroom",
            opSetAHMUC1.getAdHocChatRooms().get(0).getName(),
            presenceEvent3.getAdHocChatRoom().getName());
        assertEquals("Wrong event type",
            AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_JOINED,
            presenceEvent3.getEventType());
        assertEquals("Presence event does NOT come from the expected user",
            fixture.userID3, presenceEvent3.getParticipant().getAddress());

        assertEquals("Unexpected participants count", 2,
            opSetAHMUC1.getAdHocChatRooms().get(0).getParticipantsCount());
        assertEquals("Unexpected room participant",
            fixture.userID3,
            opSetAHMUC1.getAdHocChatRooms().get(0).getParticipants().get(1)
            .getAddress());


        //
        // Ok, our guests are actually in the room, now they leave:
        //
        opSetAHMUC2.getAdHocChatRooms().get(0).leave();

        collectorUser1.waitForEvent(40000);

        // Check the generated events and what information they give:
        presenceEvent2 = (AdHocChatRoomParticipantPresenceChangeEvent)
            collectorUser1.events.get(2);

        assertEquals("Wrong type of event", presenceEvent2.getEventType(),
            AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_LEFT);
        assertEquals("The event belongs to an unexpected room",
            opSetAHMUC1.getAdHocChatRooms().get(0).getName(),
            presenceEvent2.getAdHocChatRoom().getName());
        assertEquals("The event belongs to an unexpected user", fixture.userID2,
            presenceEvent2.getParticipant().getAddress());

        // Check the current state of the room:
        assertEquals("Wrong count of participants", 1,
            opSetAHMUC1.getAdHocChatRooms().get(0).getParticipantsCount());


        opSetAHMUC3.getAdHocChatRooms().get(0).leave();

        collectorUser1.waitForEvent(10000);

        // Check the generated events and what information they give:
        presenceEvent3 = (AdHocChatRoomParticipantPresenceChangeEvent)
            collectorUser1.events.get(3);

        assertEquals("Wrong type of event", presenceEvent3.getEventType(),
            AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_LEFT);
        assertEquals("The event belongs to an unexpected room",
            opSetAHMUC1.getAdHocChatRooms().get(0).getName(),
            presenceEvent3.getAdHocChatRoom().getName());
        assertEquals("The event belongs to an unexpected user", fixture.userID3,
            presenceEvent3.getParticipant().getAddress());

        // Check the current state of the room:
        assertEquals("The room was supposed to be empty, but it still contains"+
            " participants", 0,
            opSetAHMUC1.getAdHocChatRooms().get(0).getParticipantsCount());

    }

    /**
     * Invite both second and third users and check that they correctly have
     * joined the room. MSN does not support invitations (with rejection), so we
     * just have to check if the users are present in the room.
     *
     * @throws OperationFailedException
     */
    public void testPeerJoined() throws OperationFailedException
    {
        // First make sure the cache contains rooms:
        // (If no, the test will fails and not generate an index error)
        assertEquals("There are any rooms to retrieve on user 1 side's", 1,
            opSetAHMUC1.getAdHocChatRooms().size());

        // Then make sure the room is still here:
        AdHocChatRoom adHocChatRoom = opSetAHMUC1.getAdHocChatRooms().get(0);
        assertNotNull("The room can NOT been retrieved.", adHocChatRoom);

        // Collectors allows to gather the events which are generated:
        AHMUCEventCollector collector = new AHMUCEventCollector(
            adHocChatRoom, AHMUCEventCollector.PRESENCE_EVENT);

        // We invite and wait for the other side:
        // (Here with MSN and the ad-hoc group chat, we have to invite at least
        // two users if we want a (ad-hoc) chatroom to be created on the other
        // side: it means you are NOT able to start an ad-hoc MULTI user chat
        // with just one peer, else it will be considered as a simple one-to-one
        // chat).
        adHocChatRoom.invite(fixture.userID2, "");
        adHocChatRoom.invite(fixture.userID3, "");

        collector.waitForEvent(10000);
        collector.waitForEvent(10000);

        // We first check if presence events have been generated:
        // (one event for user2, and another one for user3)
        assertEquals("Wrong number of collected events",
            2, collector.events.size());

        // Check generated event's properties:
        AdHocChatRoomParticipantPresenceChangeEvent presenceEvent1 =
            (AdHocChatRoomParticipantPresenceChangeEvent)
            collector.events.get(0);
        assertEquals("Wrong event type",
            AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_JOINED,
            presenceEvent1.getEventType());
        assertEquals("Unexpected chatroom",
            adHocChatRoom.getName(),
            presenceEvent1.getAdHocChatRoom().getName());

        AdHocChatRoomParticipantPresenceChangeEvent presenceEvent2 =
            (AdHocChatRoomParticipantPresenceChangeEvent)
            collector.events.get(1);
        assertEquals("Wrong event type",
            AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_JOINED,
            presenceEvent2.getEventType());
        assertEquals("Unexpected chatroom",
            adHocChatRoom.getName(),
            presenceEvent2.getAdHocChatRoom().getName());

        // Two users are supposed to be in the room now:
        assertEquals("Wrong number of participants",
            2, adHocChatRoom.getParticipantsCount());

        // Gather room participants address...
        List<String> participantsAdress = new ArrayList<String>();
        for(Contact c : adHocChatRoom.getParticipants())
        {
            participantsAdress.add(c.getAddress());
        }

        // ... and finally check that both of our guests are here by searching
        // for their identity:
        assertTrue("A participant is missing",
            participantsAdress.contains(fixture.userID2));
        assertTrue("A participant is missing",
            participantsAdress.contains(fixture.userID3));

        // We force the creation of an ad-hoc chatroom on each side:
        // (In some cases, the chat room is created when an instant message has
        // been received).
        AHMUCEventCollector collector2 = new AHMUCEventCollector(
            adHocChatRoom, AHMUCEventCollector.MESSAGE_EVENT);

        Message message = adHocChatRoom.createMessage("Don't ask your country" +
        "what it can do for you, ask you what you can do for it.");

        adHocChatRoom.sendMessage(message);

        collector2.waitForEvent(10000);

        // Check event's properties:
        AdHocChatRoomMessageDeliveredEvent deliveredMessage =
            (AdHocChatRoomMessageDeliveredEvent) collector2.events.get(0);

        assertEquals("Message delivered to an unexpected room",
            adHocChatRoom.getName(),
            deliveredMessage.getSourceAdHocChatRoom().getName());
        assertEquals("Wrong message type",
            AdHocChatRoomMessageDeliveredEvent.CONVERSATION_MESSAGE_DELIVERED,
            deliveredMessage.getEventType());
        assertEquals("Message's content does NOT match", message.getContent(),
            deliveredMessage.getMessage().getContent());
    }

    /**
     * Make sure that invitations have been received on both side (user2 and
     * user3). Note that it only make sense to use this method with protocol
     * who support invitations (Yahoo! and ICQ).
     *
     * We will first test that after having accept an invitation the concerned
     * user joins the room and be a part of participants.
     *
     * We will then test that after having reject an invitation we receive the
     * rejection message and the concerned user is not a part of the room
     * participants.
     *
     * Note that before the end of this test we will invite again the user who
     * rejected our invitation and he will accept our invitation because we will
     * need his presence for the remaining tests.
     *
     * @throws OperationFailedException if something wrong happens when joining
     * the room
     */
    public void testInvitations() throws OperationFailedException
    {
        // First make sure the cache contains rooms:
        // (If no, the test will fails and not generate an index error)
        assertEquals("There are no rooms to retrieve on user 1 side's", 1,
            opSetAHMUC1.getAdHocChatRooms().size());

        // Then make sure the room is still here:
        AdHocChatRoom adHocChatRoom = opSetAHMUC1.getAdHocChatRooms().get(0);
        assertNotNull("The room can NOT been retrieved.", adHocChatRoom);

        // Collectors allows to gather the events which are generated:
        AHMUCEventCollector collectorUser1 = new AHMUCEventCollector(
            opSetAHMUC1, AHMUCEventCollector.PRESENCE_EVENT);
        AHMUCEventCollector collectorUser2 = new AHMUCEventCollector(
            opSetAHMUC2, AHMUCEventCollector.INVITATION_EVENT);
        AHMUCEventCollector collectorUser3 = new AHMUCEventCollector(
            opSetAHMUC3, AHMUCEventCollector.INVITATION_EVENT);

        // We invite and wait for the other side:
        adHocChatRoom.invite(fixture.userID2, invitationReason);
        adHocChatRoom.invite(fixture.userID3, invitationReason);

        collectorUser2.waitForEvent(10000);
        collectorUser3.waitForEvent(10000);

        // We check if invitations have been well delivered:
        // (one event for user2, and another one for user3)
        assertEquals("Wrong number of collected events",
            1, collectorUser2.events.size());
        assertEquals("Wrong number of collected events",
            1, collectorUser3.events.size());

        assertTrue("Unexpected event type", collectorUser2.events.get(0)
            instanceof AdHocChatRoomInvitationReceivedEvent);
        assertTrue("Unexpected event type", collectorUser3.events.get(0)
            instanceof AdHocChatRoomInvitationReceivedEvent);


        // Check event's properties for user2:
        AdHocChatRoomInvitationReceivedEvent event2 =
            (AdHocChatRoomInvitationReceivedEvent) collectorUser2.events.get(0);

        assertEquals("Received invitation does NOT concern the right chatroom",
            adHocChatRoom.getName(),
            event2.getInvitation().getTargetAdHocChatRoom().getName());
        assertEquals("Received invitation does NOT come from expected user",
            fixture.userID1, event2.getInvitation().getInviter());
        assertEquals("Invitation's reason does NOT match",
            invitationReason, event2.getInvitation().getReason());

        // Check event's properties for user3:
        AdHocChatRoomInvitationReceivedEvent event3 =
            (AdHocChatRoomInvitationReceivedEvent) collectorUser3.events.get(0);

        assertEquals("Received invitation does NOT concern the right chatroom",
            adHocChatRoom.getName(),
            event3.getInvitation().getTargetAdHocChatRoom().getName());
        assertEquals("Received invitation does NOT come from expected user",
            fixture.userID1, event3.getInvitation().getInviter());
        assertEquals("Invitation's reason does NOT match",
            invitationReason, event3.getInvitation().getReason());


        //
        // User2 accepts our invitation
        //
        event2.getInvitation().getTargetAdHocChatRoom().join();

        try { Thread.sleep(10000); }
        catch (InterruptedException e) { e.printStackTrace(); }

        assertEquals("Wrong count of generated events",
            1, collectorUser1.events.size());
        assertTrue("Wrong event instance", collectorUser1.events.get(0)
            instanceof AdHocChatRoomParticipantPresenceChangeEvent);

        AdHocChatRoomParticipantPresenceChangeEvent presenceEvent2 =
            (AdHocChatRoomParticipantPresenceChangeEvent)
            collectorUser1.events.get(0);

        assertEquals("Presence event does NOT concern expected chatroom",
            adHocChatRoom.getName(),
            presenceEvent2.getAdHocChatRoom().getName());
        assertEquals("Wrong event type",
            AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_JOINED,
            presenceEvent2.getEventType());
        assertEquals("Presence event does NOT come from the expected user",
            fixture.userID2, presenceEvent2.getParticipant().getAddress());

        assertEquals("Unexpected participants count",
            1, adHocChatRoom.getParticipantsCount());
        assertEquals("Unexpected room participant",
            fixture.userID2,
            adHocChatRoom.getParticipants().get(0).getAddress());


        //
        // User3 rejects our invitation (we invite him again then, and he joins)
        //
        opSetAHMUC3.rejectInvitation(
            event3.getInvitation(), invitationRejectReason);

        try { Thread.sleep(10000); }
        catch (InterruptedException e) { e.printStackTrace(); }

        assertEquals("Wrong count of generated events",
            2, collectorUser1.events.size());
        assertTrue("Wrong event instance", collectorUser1.events.get(1)
            instanceof AdHocChatRoomInvitationRejectedEvent);

        AdHocChatRoomInvitationRejectedEvent rejectEvent =
            (AdHocChatRoomInvitationRejectedEvent)
            collectorUser1.events.get(1);

        assertEquals("Reject event does NOT concern expected room",
            adHocChatRoom.getName(), rejectEvent.getChatRoom().getName());
        assertEquals("Reject event does NOT come from expected user",
            fixture.userID3, rejectEvent.getInvitee());
        assertEquals("Reject event's reason does NOT match with expected one",
            invitationRejectReason,
            rejectEvent.getReason());

        // Makes sure that previous user is still the only one participant
        assertEquals("Unexpected participants count",
            1, adHocChatRoom.getParticipantsCount());
        assertEquals("Unexpected room participant",
            fixture.userID2,
            adHocChatRoom.getParticipants().get(0).getAddress());

        // Now invite again user3:
        adHocChatRoom.invite(fixture.userID3, "");

        try { Thread.sleep(10000); }
        catch (InterruptedException e) { e.printStackTrace(); }

        assertEquals("Wrong number of collected events",
            2, collectorUser3.events.size());
        assertTrue("Unexpected event type", collectorUser3.events.get(1)
            instanceof AdHocChatRoomInvitationReceivedEvent);

        // Check event's properties for user3:
        event3 =
            (AdHocChatRoomInvitationReceivedEvent) collectorUser3.events.get(1);

        assertEquals("Received invitation does NOT concern the right chatroom",
            adHocChatRoom.getName(),
            event3.getInvitation().getTargetAdHocChatRoom().getName());
        assertEquals("Received invitation does NOT come from expected user",
            fixture.userID1, event3.getInvitation().getInviter());
        assertEquals("Invitation's reason does NOT match",
            invitationReason, event3.getInvitation().getReason());

        event3.getInvitation().getTargetAdHocChatRoom().join();

        try { Thread.sleep(10000); }
        catch (InterruptedException e) { e.printStackTrace(); }

        assertEquals("Wrong count of generated events",
            3, collectorUser1.events.size() == 1);
        assertTrue("Wrong event instance", collectorUser1.events.get(0)
            instanceof AdHocChatRoomParticipantPresenceChangeEvent);

        AdHocChatRoomParticipantPresenceChangeEvent presenceEvent3 =
            (AdHocChatRoomParticipantPresenceChangeEvent)
            collectorUser1.events.get(2);

        assertEquals("Presence event does NOT concern expected chatroom",
            adHocChatRoom.getName(),
            presenceEvent3.getAdHocChatRoom().getName());
        assertEquals("Wrong event type",
            AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_JOINED,
            presenceEvent3.getEventType());
        assertEquals("Presence event does NOT come from the expected user",
            fixture.userID3, presenceEvent3.getParticipant().getAddress());

        assertEquals("Unexpected participants count",
            2, adHocChatRoom.getParticipantsCount());
        assertEquals("Unexpected room participant",
            fixture.userID3,
            adHocChatRoom.getParticipants().get(1).getAddress());
    }

    /**
     * Send an instant message to the room and check that second user in the
     * room receives it.
     *
     * @throws OperationFailedException if an error occurs while sending a
     * message
     * @throws OperationNotSupportedException
     */
    public void testSendIM()
    throws OperationFailedException, OperationNotSupportedException
    {
        // First make sure the cache contains the expected rooms:
        // (If no, the test will fails and not generate an index error)
        assertEquals("There are any rooms to retrieve on user 1 side's", 1,
            opSetAHMUC1.getAdHocChatRooms().size());
        assertEquals("There are any rooms to retrieve on user 2 side's", 1,
            opSetAHMUC2.getAdHocChatRooms().size());
        assertEquals("There are any rooms to retrieve on user 3 side's", 1,
            opSetAHMUC3.getAdHocChatRooms().size());

        // Then make sure the room is still here:
        AdHocChatRoom roomUser1 = opSetAHMUC1.getAdHocChatRooms().get(0);
        AdHocChatRoom roomUser2 = opSetAHMUC2.getAdHocChatRooms().get(0);
        AdHocChatRoom roomUser3 = opSetAHMUC3.getAdHocChatRooms().get(0);

        assertNotNull("The room can NOT been retrieved on user's 1 side.",
            roomUser1);
        assertNotNull("The room can NOT been retrieved on user's 2 side.",
            roomUser2);
        assertNotNull("The room can NOT been retrieved on user's 3 side.",
            roomUser3);

        // Collectors allows to gather the events which are generated:
        AHMUCEventCollector collectorUser1 = new AHMUCEventCollector(
            roomUser1, AHMUCEventCollector.MESSAGE_EVENT);

        AHMUCEventCollector collectorUser2 = new AHMUCEventCollector(
            roomUser2, AHMUCEventCollector.MESSAGE_EVENT);

        AHMUCEventCollector collectorUser3 = new AHMUCEventCollector(
            roomUser3, AHMUCEventCollector.MESSAGE_EVENT);

        // We create a new message to be sent through the room:
        Message message =
            roomUser1.createMessage("Quick brown fox jumps over the lazy dog");
        roomUser1.sendMessage(message);

        try { Thread.sleep(10000); }
        catch (InterruptedException e) { e.printStackTrace(); }

        // Check that events are dispatched on each side:
        assertEquals("User 1 did NOT receive a message delivered event. Wrong" +
            " event collected number", 1, collectorUser1.events.size());
        assertEquals("User 2 did NOT receive a message received event. Wrong " +
            "event collected number", 1, collectorUser2.events.size());
        assertEquals("User 3 did NOT receive a message received event. Wrong " +
            "event collected number", 1, collectorUser3.events.size());


        // Check event's pertinency on user's 1 side:
        AdHocChatRoomMessageDeliveredEvent deliveredMessage =
            (AdHocChatRoomMessageDeliveredEvent) collectorUser1.events.get(0);

        assertEquals("Message delivered to an unexpected room",
            roomUser1.getName(),
            deliveredMessage.getSourceAdHocChatRoom().getName());
        assertEquals("Wrong message type",
            AdHocChatRoomMessageDeliveredEvent.CONVERSATION_MESSAGE_DELIVERED,
            deliveredMessage.getEventType());
        assertEquals("Message's content does NOT match", message.getContent(),
            deliveredMessage.getMessage().getContent());

        // Check event's pertinency on user's 2 side:
        AdHocChatRoomMessageReceivedEvent receivedMessage =
            (AdHocChatRoomMessageReceivedEvent) collectorUser2.events.get(0);

        assertEquals("Message does NOT belong to this room",
            roomUser2.getName(),
            receivedMessage.getSourceChatRoom().getName());
        assertEquals("Wrong message type",
            AdHocChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED,
            receivedMessage.getEventType());
        assertEquals("Message's content does NOT match", message.getContent(),
            receivedMessage.getMessage().getContent());

        // Check event's pertinency on user's 3 side:
        receivedMessage =
            (AdHocChatRoomMessageReceivedEvent) collectorUser3.events.get(0);

        assertEquals("Message does NOT belong to this room",
            roomUser3.getName(),
            receivedMessage.getSourceChatRoom().getName());
        assertEquals("Wrong message type",
            AdHocChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED,
            receivedMessage.getEventType());
        assertEquals("Message's content does NOT match", message.getContent(),
            receivedMessage.getMessage().getContent());
    }

    /**
     * Our peer leave the room: we check that there is no more participants in
     * the room.
     */
    public void testPeerLeaved()
    {
        // First make sure the cache contains rooms:
        // (If no, the test will fails and not generate an index error)
        assertEquals("There are any rooms to retrieve on user 1 side's", 1,
            opSetAHMUC1.getAdHocChatRooms().size());
        assertEquals("There are any rooms to retrieve on user 2 side's", 1,
            opSetAHMUC2.getAdHocChatRooms().size());
        assertEquals("There are any rooms to retrieve on user 3 side's", 1,
            opSetAHMUC3.getAdHocChatRooms().size());

        AdHocChatRoom room = opSetAHMUC1.getAdHocChatRooms().get(0);
        AHMUCEventCollector collector = new AHMUCEventCollector(
            room, AHMUCEventCollector.PRESENCE_EVENT);

        //
        // Our first peer leaves the room:
        //
        opSetAHMUC2.getAdHocChatRooms().get(0).leave();

        collector.waitForEvent(10000);

        // Check the generated events and what information they give:
        assertEquals("Wrong events count when first peer leaved the room",
            1, collector.events.size());

        AdHocChatRoomParticipantPresenceChangeEvent presenceEvent =
            (AdHocChatRoomParticipantPresenceChangeEvent)
            collector.events.get(0);

        assertEquals("Wrong type of event", presenceEvent.getEventType(),
            AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_LEFT);
        assertEquals("The event belongs to an unexpected room", room.getName(),
            presenceEvent.getAdHocChatRoom().getName());
        assertEquals("The event belongs to an unexpected user", fixture.userID2,
            presenceEvent.getParticipant().getAddress());

        // Check the current state of the room:
        assertEquals("No event was generated when second peer leaved the room",
            1, room.getParticipantsCount());
        assertEquals("The room was not supposed to contain this user anymore",
            fixture.userID3, room.getParticipants().get(0).getAddress());


        //
        // Our second peer leaves the room:
        //
        opSetAHMUC3.getAdHocChatRooms().get(0).leave();

        collector.waitForEvent(10000);

        // Check the generated events and what information they give:
        presenceEvent = (AdHocChatRoomParticipantPresenceChangeEvent)
        collector.events.get(1);

        assertEquals("Wrong type of event", presenceEvent.getEventType(),
            AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_LEFT);
        assertEquals("The event belongs to an unexpected room", room.getName(),
            presenceEvent.getAdHocChatRoom().getName());
        assertEquals("The event belongs to an unexpected user", fixture.userID3,
            presenceEvent.getParticipant().getAddress());

        // Check the current state of the room:
        assertEquals("The room was supposed to be empty, but it still contains"+
            " participants", 0, room.getParticipantsCount());
    }

    /**
     * Make sure each user is on the contact list of others.
     *
     * @throws Exception
     */
    public void prepareContactList() throws Exception
    {
        fixture.clearProvidersLists();

        try
        {
            opSetPresence1.setAuthorizationHandler(new AuthHandler());
            opSetPresence1.subscribe(fixture.userID2);
            opSetPresence1.subscribe(fixture.userID3);
        }
        catch (OperationFailedException e)
        {
            // means that the contacts already exits.
        }

        try
        {
            opSetPresence2.setAuthorizationHandler(new AuthHandler());
            opSetPresence2.subscribe(fixture.userID1);
            opSetPresence2.subscribe(fixture.userID3);
        }
        catch (OperationFailedException e)
        {
            // means that the contacts already exits.
        }

        try
        {
            opSetPresence3.setAuthorizationHandler(new AuthHandler());
            opSetPresence3.subscribe(fixture.userID1);
            opSetPresence3.subscribe(fixture.userID2);
        }
        catch (OperationFailedException e)
        {
            // means that the contacts already exits.
        }

        logger.info("Will wait until the list prepare is completed");
        Object o = new Object();
        synchronized(o)
        {
            o.wait(2000);
        }
    }
}
