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
package net.java.sip.communicator.slick.protocol.gibberish;

import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;

/**
 * Gibberish specific testing for a Gibberish Protocol Provider Service
 * implementation.
 * The test suite registers two accounts for the Gibberish protocol, makes them
 * communicate and make sure everything goes fine.
 *
 * @author Emil Ivov
 */
public class GibberishProtocolProviderServiceLick
    extends    TestSuite
    implements BundleActivator
{
    /**
     * The prefix used for property names containing settings for our first
     * testing account.
     */
    public static final String ACCOUNT_1_PREFIX
        = "accounts.gibberish.account1.";

    /**
     * The prefix used for property names containing settings for our second
     * testing account.
     */
    public static final String ACCOUNT_2_PREFIX
        = "accounts.gibberish.account2.";

    /**
     * The name of the property that indicates whether the user would like to
     * only run the offline tests.
     */
    public static final String DISABLE_ONLINE_TESTS_PROPERTY_NAME
        = "accounts.gibberish.DISABLE_ONLINE_TESTING";

    /**
     * The name of the property the value of which is a formatted string that
     * contains the contact list that.
     */
    public static final String CONTACT_LIST_PROPERTY_NAME
        = "accounts.gibberish.CONTACT_LIST";

    /**
     * Initializes and registers all tests that we'll run as a part of this
     * slick.
     *
     * @param context a currently valid bundle context.
     */
    public void start(BundleContext context)
    {
        setName("GibberishProtocolProviderServiceLick");

        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("service.pid", getName());

        GibberishSlickFixture.bc = context;

        // verify whether the user wants to avoid online testing
        String offlineMode = System.getProperty(
            DISABLE_ONLINE_TESTS_PROPERTY_NAME, null);

        if (offlineMode != null && offlineMode.equalsIgnoreCase("true"))
            GibberishSlickFixture.onlineTestingDisabled = true;


        addTestSuite(TestAccountInstallation.class);
        addTestSuite(TestProtocolProviderServiceGibberishImpl.class);

        addTest(TestOperationSetPresence.suite());

        //the following should only be run when we want online testing.
        if(!GibberishSlickFixture.onlineTestingDisabled)
        {
            addTest(TestOperationSetPersistentPresence.suite());

            addTest(TestOperationSetBasicInstantMessaging.suite());

            addTest(TestOperationSetTypingNotifications.suite());
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
