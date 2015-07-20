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

import org.osgi.framework.*;

/**
 * Jabber specific testing for a Jabber Protocol Provider Service implementation.
 * The test suite registers two accounts for
 *
 * @author Damian Minkov
 * @author Valentin Martinet
 */
public class JabberProtocolProviderServiceLick
    extends    TestSuite
    implements BundleActivator
{
    /**
     * The prefix used for property names containing settings for our first
     * testing account.
     */
    public static final String ACCOUNT_1_PREFIX
        = "accounts.jabber.account1.";

    /**
     * The prefix used for property names containing settings for our second
     * testing account.
     */
    public static final String ACCOUNT_2_PREFIX
        = "accounts.jabber.account2.";

    /**
     * The prefix used for property names containing settings for our third
     * testing account.
     */
    public static final String ACCOUNT_3_PREFIX
        = "accounts.jabber.account3.";

    /**
     * The name of the property that indicates whether the user would like to
     * only run the offline tests.
     */
    public static final String DISABLE_ONLINE_TESTS_PROPERTY_NAME
        = "accounts.jabber.DISABLE_ONLINE_TESTING";

    /**
     * The name of the chat room that we will create and use for multi user
     * chat testing.
     */
    public static final String CHAT_ROOM_NAME
        = "accounts.jabber.CHAT_ROOM_NAME";


    /**
     * The name of the property the value of which is a formatted string that
     * contains the contact list that.
     */
    public static final String CONTACT_LIST_PROPERTY_NAME
        = "accounts.jabber.CONTACT_LIST";

    /**
     * Initializes and registers all tests that we'll run as a part of this
     * slick.
     *
     * @param context a currently valid bundle context.
     */
    public void start(BundleContext context)
    {
        setName("JabberProtocolProviderSlick");

        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("service.pid", getName());

        JabberSlickFixture.bc = context;

        // verify whether the user wants to avoid online testing
        String offlineMode = System.getProperty(
            DISABLE_ONLINE_TESTS_PROPERTY_NAME, null);

        if (offlineMode != null && offlineMode.equalsIgnoreCase("true"))
            JabberSlickFixture.onlineTestingDisabled = true;


        addTestSuite(TestAccountInstallation.class);
        addTestSuite(TestProtocolProviderServiceJabberImpl.class);

        addTest(TestOperationSetPresence.suite());

        //the following should only be run when we want online testing.
        if(!JabberSlickFixture.onlineTestingDisabled)
        {
            addTest(TestOperationSetPersistentPresence.suite());

            addTest(TestOperationSetBasicInstantMessaging.suite());

            addTest(TestOperationSetInstantMessageTransformJabberImpl.suite());

            addTest(TestOperationSetTypingNotifications.suite());

            //tests currently fails due to jingle lib bugs.
            //will be activated as soon as bugs get fixed
            //addTestSuite(TestOperationSetBasicTelephonyJabberImpl.class);

            /** @todo UNCOMMENT */
//            addTest(TestOperationSetMultiUserChat.suite());

            // temporally disable multiuser chats
            //addTest(TestOperationSetMultiUserChat2.suite());

            addTestSuite(TestOperationSetFileTransferImpl.class);
        }


        addTest(TestAccountUninstallation.suite());
        addTestSuite(TestAccountUninstallationPersistence.class);

        context.registerService(getClass().getName(), this, properties);
    }

    /**
     * Prepares the slick for shutdown.
     *
     * @param context a currently valid bundle context.
     */
    public void stop(BundleContext context)
    {

    }
}
