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
import net.java.sip.communicator.slick.protocol.generic.*;
import net.java.sip.communicator.util.*;

public class TestOperationSetInstantMessageTransformJabberImpl
    extends TestCase
{

    private static final Logger logger =
        Logger
            .getLogger(TestOperationSetInstantMessageTransformJabberImpl.class);

    private JabberSlickFixture fixture = new JabberSlickFixture();

    private OperationSetBasicInstantMessaging opSetBasicIM1 = null;

    private OperationSetBasicInstantMessaging opSetBasicIM2 = null;

    private OperationSetInstantMessageTransform opSetTransform1 = null;

    private OperationSetInstantMessageTransform opSetTransform2 = null;

    private OperationSetPresence opSetPresence1 = null;

    private OperationSetPresence opSetPresence2 = null;

    public TestOperationSetInstantMessageTransformJabberImpl(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();

        Map<String, OperationSet> supportedOperationSets1 =
            fixture.provider1.getSupportedOperationSets();

        if (supportedOperationSets1 == null
            || supportedOperationSets1.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                    + "this implementation. ");

        // get the operation set presence here.
        opSetBasicIM1 =
            (OperationSetBasicInstantMessaging) supportedOperationSets1
                .get(OperationSetBasicInstantMessaging.class.getName());

        if (opSetBasicIM1 == null)
        {
            throw new NullPointerException(
                "No implementation for basic IM was found");
        }

        // we also need the presence op set in order to retrieve contacts.
        opSetTransform1 =
            (OperationSetInstantMessageTransform) supportedOperationSets1
                .get(OperationSetInstantMessageTransform.class.getName());

        // if the op set is null show that we're not happy.
        if (opSetTransform1 == null)
        {
            throw new NullPointerException(
                "An implementation of the service must provide an OperationSetInstantMessageTransform implementation");
        }

        // we also need the presence op set in order to retrieve contacts.
        opSetPresence1 =
            (OperationSetPresence) supportedOperationSets1
                .get(OperationSetPresence.class.getName());

        // if the op set is null show that we're not happy.
        if (opSetPresence1 == null)
        {
            throw new NullPointerException(
                "An implementation of the service must provide an "
                    + "implementation of at least one of the PresenceOperationSets");
        }

        Map<String, OperationSet> supportedOperationSets2 =
            fixture.provider2.getSupportedOperationSets();

        if (supportedOperationSets2 == null
            || supportedOperationSets2.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                    + "this implementation. ");

        // get the operation set presence here.
        opSetBasicIM2 =
            (OperationSetBasicInstantMessaging) supportedOperationSets2
                .get(OperationSetBasicInstantMessaging.class.getName());

        if (opSetBasicIM2 == null)
        {
            throw new NullPointerException(
                "No implementation for basic IM was found");
        }

        opSetTransform2 =
            (OperationSetInstantMessageTransform) supportedOperationSets2
                .get(OperationSetInstantMessageTransform.class.getName());

        // if the op set is null show that we're not happy.
        if (opSetTransform2 == null)
        {
            throw new NullPointerException(
                "An implementation of the service must provide an OperationSetInstantMessageTransform implementation");
        }

        opSetPresence2 =
            (OperationSetPresence) supportedOperationSets2
                .get(OperationSetPresence.class.getName());

        // if the op set is null show that we're not happy.
        if (opSetPresence2 == null)
        {
            throw new NullPointerException(
                "An implementation of the service must provide an "
                    + "implementation of at least one of the PresenceOperationSets");
        }

    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();

        fixture.tearDown();
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();

        suite.addTest(new TestOperationSetInstantMessageTransformJabberImpl(
            "firstTestTransformLayerInstallation"));

        suite
            .addTestSuite(TestOperationSetInstantMessageTransformJabberImpl.class);

        return suite;
    }

    public void firstTestTransformLayerInstallation()
    {
        PredictableTransformLayer transformLayer =
            new PredictableTransformLayer();

        opSetTransform1.addTransformLayer(transformLayer);
        if (!opSetTransform1.containsLayer(transformLayer))
            fail("Transform layer did not install.");

        opSetTransform2.addTransformLayer(transformLayer);
        if (!opSetTransform2.containsLayer(transformLayer))
            fail("Transform layer did not install.");
    }

    public void testMessageReceivedTransform()
    {
        String body =
            "This is an IM coming from the tester agent" + " on "
                + new Date().toString();

        // We expect out message to be transformed only from the
        // MessageDelivered Event.
        String expectedReceivedBody = "__RECEIVED____DELIVERY_PENDING__" + body;

        ImEventCollector receiversEventCollector = new ImEventCollector();

        opSetBasicIM1.addMessageListener(receiversEventCollector);

        Contact contact1 = opSetPresence2.findContactByID(fixture.userID1);

        logger.debug("Will send message \"" + body + "\" to: \"" + contact1
            + "\". We expect to get back: \"" + expectedReceivedBody + "\"");

        opSetBasicIM2.sendInstantMessage(contact1, opSetBasicIM2
            .createMessage(body));

        receiversEventCollector.waitForEvent(timeout);

        opSetBasicIM1.removeMessageListener(receiversEventCollector);

        // assert reception of a message event
        assertTrue("No events delivered upon a received message",
            receiversEventCollector.collectedEvents.size() > 0);

        // assert event instance of Message Received Evt
        assertTrue(
            "Received evt was not an instance of "
                + MessageReceivedEvent.class.getName(),
            receiversEventCollector.collectedEvents.get(0) instanceof MessageReceivedEvent);

        // assert source contact == testAgent.uin
        MessageReceivedEvent evt =
            (MessageReceivedEvent) receiversEventCollector.collectedEvents
                .get(0);
        assertEquals("message sender ", evt.getSourceContact().getAddress(),
            fixture.userID2);

        logger.debug("We got back: \"" + evt.getSourceMessage().getContent()
            + "\"");

        // assert messageBody == body
        assertEquals("message body", expectedReceivedBody, evt
            .getSourceMessage().getContent());

    }

    private static final long timeout = 1000;

    public void testMessageDeliveredTransform()
    {
        String body =
            "This is an IM coming from the tester agent" + " on "
                + new Date().toString();

        // The message will be transformed prior to sending and after being
        // received, we expect two underscores to be prepended and two
        // underscores to be appended.
        String expectedReceivedBody = "__DELIVERED__" + body;

        ImEventCollector sendersEventCollector = new ImEventCollector();
        opSetBasicIM2.addMessageListener(sendersEventCollector);

        Contact contact1 = opSetPresence2.findContactByID(fixture.userID1);

        logger.debug("Will send message \"" + body + "\" to: \"" + contact1
            + "\". We expect to get back: \"" + expectedReceivedBody + "\"");

        opSetBasicIM2.sendInstantMessage(contact1, opSetBasicIM2
            .createMessage(body));

        sendersEventCollector.waitForEvent(timeout);
        opSetBasicIM2.removeMessageListener(sendersEventCollector);

        // assert reception of a message event
        assertTrue("No events delivered upon a sent message",
            sendersEventCollector.collectedEvents.size() > 0);

        // assert event instance of Message Received Evt
        assertTrue(
            "Received evt was not an instance of "
                + MessageDeliveredEvent.class.getName(),
            sendersEventCollector.collectedEvents.get(0) instanceof MessageDeliveredEvent);

        // assert source contact == testAgent.uin
        MessageDeliveredEvent evtDelivered =
            (MessageDeliveredEvent) sendersEventCollector.collectedEvents
                .get(0);
        assertEquals("message sender ", evtDelivered.getDestinationContact()
            .getAddress(), fixture.userID1);

        logger.debug("We got back: \""
            + evtDelivered.getSourceMessage().getContent() + "\"");
        // assert messageBody == body
        assertEquals("message body", expectedReceivedBody, evtDelivered
            .getSourceMessage().getContent());
    }

    public void testMessageDeliveryFailedTransform()
    {
        // TODO not sure how this can be implemented.
    }
}
