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
package net.java.sip.communicator.slick.protocol.yahoo;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.slick.protocol.generic.*;

/**
 * Tests for the Yahoo! ad-hoc multi-user chat operation set.
 *
 * @author Valentin Martinet
 */
public class TestOperationSetAdHocMultiUserChatYahooImpl
extends TestOperationSetAdHocMultiUserChat
{
    /**
     * Creates the test with the specified method name.
     *
     * @param name the name of the method to execute.
     */
    public TestOperationSetAdHocMultiUserChatYahooImpl(String name)
    {
        super(name);
    }


    /**
     * Creates a test suite containing tests of this class in a specific order.
     *
     * @return Test a testsuite containing all tests to execute.
     */
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite();

        suite.addTest(new TestOperationSetAdHocMultiUserChatYahooImpl(
        "testRegisterAccount3"));
        suite.addTest(new TestOperationSetAdHocMultiUserChatYahooImpl(
        "prepareContactList"));
        suite.addTest(new TestOperationSetAdHocMultiUserChatYahooImpl(
        "testCreateRoomWithParticipants"));
        suite.addTest(new TestOperationSetAdHocMultiUserChatYahooImpl(
        "testInvitations"));
        suite.addTest(new TestOperationSetAdHocMultiUserChatYahooImpl(
        "testSendIM"));
        suite.addTest(new TestOperationSetAdHocMultiUserChatYahooImpl(
        "testPeerLeaved"));

        return suite;
    }

    /**
     * Register the third testing account.
     *
     * @throws OperationFailedException
     */
    public void testRegisterAccount3() throws OperationFailedException
    {
            fixture.provider3.register(
                    new SecurityAuthorityImpl(
                        System.getProperty(
                            YahooProtocolProviderServiceLick.ACCOUNT_3_PREFIX
                            + ProtocolProviderFactory.PASSWORD).toCharArray()));

            assertEquals(fixture.provider3.getRegistrationState(),
                RegistrationState.REGISTERED);
    }

    /**
     * JUnit setUp method.
     * @throws Exception
     *
     * @throws Exception
     */
    @Override
    public void start() throws Exception
    {
        fixture = new YahooSlickFixture();
        fixture.setUp();

        // Supported operation sets by each protocol provider.
        Map<String, OperationSet>
        supportedOpSets1, supportedOpSets2, supportedOpSets3;

        supportedOpSets1 = fixture.provider1.getSupportedOperationSets();
        supportedOpSets2 = fixture.provider2.getSupportedOperationSets();
        supportedOpSets3 = fixture.provider3.getSupportedOperationSets();

        //
        // Initialization of operation sets for the first testing account:
        //

        if (supportedOpSets1 == null || supportedOpSets1.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by " +
            "this implementation. ");

        opSetAHMUC1 = (OperationSetAdHocMultiUserChat) supportedOpSets1.get(
            OperationSetAdHocMultiUserChat.class.getName());

        if (opSetAHMUC1 == null)
            throw new NullPointerException(
            "No implementation for multi user chat was found");

        opSetPresence1 = (OperationSetPresence) supportedOpSets1.get(
            OperationSetPresence.class.getName());

        if (opSetPresence1 == null)
            throw new NullPointerException(
                "An implementation of the service must provide an " +
            "implementation of at least one of the PresenceOperationSets");


        //
        // Initialization of operation sets for the second testing account:
        //

        if (supportedOpSets2 == null || supportedOpSets2.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by " +
            "this implementation. ");

        opSetAHMUC2 = (OperationSetAdHocMultiUserChat) supportedOpSets2.get(
            OperationSetAdHocMultiUserChat.class.getName());

        if (opSetAHMUC2 == null)
            throw new NullPointerException(
            "No implementation for ad hoc multi user chat was found");

        opSetPresence2 = (OperationSetPresence) supportedOpSets2.get(
            OperationSetPresence.class.getName());

        if (opSetPresence2 == null)
            throw new NullPointerException(
                "An implementation of the service must provide an " +
            "implementation of at least one of the PresenceOperationSets");


        //
        // Initialization of operation sets for the third testing account:
        //

        if (supportedOpSets3 == null || supportedOpSets3.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by " +
            "this implementation. ");

        opSetAHMUC3 = (OperationSetAdHocMultiUserChat) supportedOpSets3.get(
            OperationSetAdHocMultiUserChat.class.getName());

        if (opSetAHMUC3 == null)
            throw new NullPointerException(
            "No implementation for ad hoc multi user chat was found");

        opSetPresence3 = (OperationSetPresence) supportedOpSets3.get(
            OperationSetPresence.class.getName());

        if (opSetPresence3 == null)
            throw new NullPointerException(
                "An implementation of the service must provide an " +
            "implementation of at least one of the PresenceOperationSets");
    }

}
