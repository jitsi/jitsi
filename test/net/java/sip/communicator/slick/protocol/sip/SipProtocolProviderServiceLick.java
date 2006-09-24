/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
     * Initializes and registers all tests that we'll run as a part of this
     * slick.
     *
     * @param context a currently valid bundle context.
     */
    public void start(BundleContext context)
    {
        setName("SipProtocolProviderServiceLick");

        Hashtable properties = new Hashtable();
        properties.put("service.pid", getName());

        SipSlickFixture.bc = context;

        //First test account installation so that the service that has
        //been installed by it gets tested by the rest of the tests.
        addTestSuite(TestAccountInstallation.class);

        //This must remain second as that's where the protocol would be
        //made to login/authenticate/signon its service provider.
        addTestSuite(TestProtocolProviderServiceSipImpl.class);

        addTestSuite(TestOperationSetBasicTelephonySipImpl.class);

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
