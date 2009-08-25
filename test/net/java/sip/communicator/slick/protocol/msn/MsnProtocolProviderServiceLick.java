/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.msn;

import java.util.*;

import org.osgi.framework.*;
import junit.framework.*;

/**
 * Msn specific testing for a Msn Protocol Provider Service implementation.
 * The test suite registers two accounts for
 *
 * @author Damian Minkov
 */
public class MsnProtocolProviderServiceLick
    extends    TestSuite
    implements BundleActivator
{
    /**
     * The prefix used for property names containing settings for our first
     * testing account.
     */
    public static final String ACCOUNT_1_PREFIX
        = "accounts.msn.account1.";

    /**
     * The prefix used for property names containing settings for our second
     * testing account.
     */
    public static final String ACCOUNT_2_PREFIX
        = "accounts.msn.account2.";

    /**
     * The name of the property that indicates whether the user would like to
     * only run the offline tests.
     */
    public static final String DISABLE_ONLINE_TESTS_PROPERTY_NAME
        = "accounts.msn.DISABLE_ONLINE_TESTING";

    /**
     * The name of the property the value of which is a formatted string that
     * contains the contact list that.
     */
    public static final String CONTACT_LIST_PROPERTY_NAME
        = "accounts.msn.CONTACT_LIST";

    /**
     * Initializes and registers all tests that we'll run as a part of this
     * slick.
     *
     * @param context a currently valid bundle context.
     */
    public void start(BundleContext context)
    {
        setName("MsnProtocolProviderSlick");

        Hashtable properties = new Hashtable();
        properties.put("service.pid", getName());

        MsnSlickFixture.bc = context;

        // verify whether the user wants to avoid online testing
        String offlineMode = System.getProperty(
            DISABLE_ONLINE_TESTS_PROPERTY_NAME, null);

        if (offlineMode != null && offlineMode.equalsIgnoreCase("true"))
            MsnSlickFixture.onlineTestingDisabled = true;


        addTestSuite(TestAccountInstallation.class);
        addTestSuite(TestProtocolProviderServiceMsnImpl.class);

        addTest(TestOperationSetPresence.suite());

        //the following should only be run when we want online testing.
        if(!MsnSlickFixture.onlineTestingDisabled)
        {
            addTest(TestOperationSetPersistentPresence.suite());

            addTest(TestOperationSetBasicInstantMessaging.suite());

            addTest(TestOperationSetTypingNotifications.suite());

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
