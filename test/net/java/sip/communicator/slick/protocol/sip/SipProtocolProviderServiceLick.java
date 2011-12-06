/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.sip;

import java.util.*;

import org.osgi.framework.*;
import junit.framework.*;

/**
 * SIP specific testing for a SIP Protocol Provider Service implementation.
 * The test suite registers two accounts for
 *
 * @author Emil Ivov
 */
public class SipProtocolProviderServiceLick
    extends    TestSuite
    implements BundleActivator
{
    /**
     * The prefix used for property names containing settings for our first
     * testing account.
     */
    public static final String ACCOUNT_1_PREFIX
        = "accounts.sip.account1.";

    /**
     * The prefix used for property names containing settings for our second
     * testing account.
     */
    public static final String ACCOUNT_2_PREFIX
        = "accounts.sip.account2.";
    
    /**
     * The name of the property that indicates whether the user would like to
     * only run the offline tests.
     */
    public static final String DISABLE_ONLINE_TESTS_PROPERTY_NAME
        = "accounts.sip.DISABLE_ONLINE_TESTING";

    /**
     * The name of the property the value of which is a formatted string that
     * contains the contact list that.
     */
    public static final String CONTACT_LIST_PROPERTY_NAME
        = "accounts.sip.CONTACT_LIST";

    /**
     * The name of the property the value of which is XCAP server uri.
     */
    public static final String XCAP_SERVER_PROPERTY_NAME = "XCAP_SERVER";

    /**
     * Initializes and registers all tests that we'll run as a part of this
     * slick.
     *
     * @param context a currently valid bundle context.
     */
    public void start(BundleContext context)
    {
        setName("SipProtocolProviderServiceLick");

        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("service.pid", getName());

        SipSlickFixture.bc = context;
        
        // verify whether the user wants to avoid online testing
        String offlineMode = System.getProperty(
            DISABLE_ONLINE_TESTS_PROPERTY_NAME, null);

        if (offlineMode != null && offlineMode.equalsIgnoreCase("true"))
            SipSlickFixture.onlineTestingDisabled = true;
        
        // xcap parsing tests
        addTest(TestXCapParse.suite());

        //proxy detection tests
        addTestSuite(TestAutoProxyDetection.class);

        //First test account installation so that the service that has
        //been installed by it gets tested by the rest of the tests.
        addTestSuite(TestAccountInstallation.class);

        //This must remain second as that's where the protocol would be
        //made to login/authenticate/signon its service provider.
        addTestSuite(TestProtocolProviderServiceSipImpl.class);
        
        // presence tests
        addTest(TestOperationSetPresence.suite());

        // only in online mode
        if (!SipSlickFixture.onlineTestingDisabled)
        {
            // persistent presence
            addTest(TestOperationSetPersistentPresence.suite());
            
            //IM test
            addTest(TestOperationSetBasicInstantMessaging.suite());
    
            // telephony
            addTestSuite(TestOperationSetBasicTelephonySipImpl.class);

            // Server stored info
            addTest(TestOperationSetServerStoredInfo.suite());
        }

        //This must remain after all other tests using the accounts
        //are done since it tests account uninstallation and the
        //accounts we use for testing won't be available after that.
        addTest(TestAccountUninstallation.suite());

        //This must remain last since it counts on the fact that
        //account uninstallation has already been executed and that.
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
