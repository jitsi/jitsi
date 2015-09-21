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
package net.java.sip.communicator.slick.protocol.icq;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Tests whether accaounts are uninstalled properly. It is important that
 * tests from this class be called last since they will install the accounts
 * that have been used to test the implementations.
 *
 * @author Emil Ivov
 * @author Damian Minkov
 */
public class TestAccountUninstallation
    extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestAccountUninstallation.class);

    IcqSlickFixture fixture = new IcqSlickFixture();

    /**
     * An event adapter that would collec registation state change events
     */
    public RegistrationEventCollector regEvtCollector
        = new RegistrationEventCollector();

    /**
     * The lock that we wait on until registration is finalized.
     */
    private Object registrationLock = new Object();

    /**
     * Constructs a test instance
     * @param name The name of the test.
     */
    public TestAccountUninstallation(String name)
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
     * Returns a suite containing tests in this class in the order that we'd
     * like them executed.
     * @return a Test suite containing tests in this class in the order that
     * we'd like them executed.
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite();

        if(! IcqSlickFixture.onlineTestingDisabled)
        suite.addTest(
            new TestAccountUninstallation("testMultipleLogins"));
        suite.addTest(
            new TestAccountUninstallation("testInstallationPersistency"));
        suite.addTest(
            new TestAccountUninstallation("testUninstallAccount"));

        return suite;
    }


    /**
     * Before we uninstall the current account which is registered to the server
     * we add a registration change listener and wait for unregistered event
     * to be fired.
     * Then we use the tester agent to register to the servers with
     * the account info of the currently logged in account so we must
     * receive multiple logins event.
     */
    public void testMultipleLogins()
    {
        fixture.provider.addRegistrationStateChangeListener(regEvtCollector);

        String passwd = System.getProperty( IcqProtocolProviderSlick
                                            .TESTED_IMPL_PWD_PROP_NAME, null );
        String uin = System.getProperty( IcqProtocolProviderSlick
                                         .TESTED_IMPL_USER_ID_PROP_NAME, null);

        IcqTesterAgent testerAgent = new IcqTesterAgent(uin);
        testerAgent.register(passwd);

        // give time to the register process
        Object lock = new Object();
        synchronized(lock)
        {
            try
            {
                logger.debug("Giving the aim server time to notify for our arrival!");
                lock.wait(5000);
            }
            catch (Exception ex)
            {}
        }

        testerAgent.unregister();

        assertNotNull(
                    "No event was dispatched"
                    ,regEvtCollector.stateRecieved);

        assertEquals(
                    "Event is not UNREGISTERED event"
                    , regEvtCollector.stateRecieved
                    , RegistrationState.UNREGISTERED);

        assertEquals(
            "No registration event notifying of Multiple logins dispatched "
            , regEvtCollector.eventReason
            , RegistrationStateChangeEvent.REASON_MULTIPLE_LOGINS);
    }

    /**
     * Stops and removes the tested bundle, verifies that it has unregistered
     * its provider, then reloads and restarts the bundle and verifies that
     * the protocol provider is reRegistered in the bundle context.
     *
     * @throws java.lang.Exception if an exception occurs during testing.
     */
    public void testInstallationPersistency()
        throws Exception
    {
        Bundle providerBundle
            = IcqSlickFixture.findProtocolProviderBundle(fixture.provider);

        //set the global providerBundle reference that we will be using
        //in the last series of tests (Account uninstallation persistency)
        IcqSlickFixture.providerBundle = providerBundle;

        assertNotNull("Couldn't find a bundle for the tested provider"
                      , providerBundle);

        providerBundle.stop();

        assertTrue("Couldn't stop the protocol provider bundle. State was "
                   + providerBundle.getState()
                   ,    Bundle.ACTIVE   != providerBundle.getState()
                     && Bundle.STOPPING != providerBundle.getState());

        providerBundle.uninstall();

        assertEquals("Couldn't stop the protocol provider bundle."
                     , Bundle.UNINSTALLED, providerBundle.getState());

        //verify that the provider is no longer available
        ServiceReference[] icqProviderRefs = null;
        try
        {
            icqProviderRefs = IcqSlickFixture.bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&"
                + "(" + ProtocolProviderFactory.PROTOCOL
                      + "=" +ProtocolNames.ICQ + ")"
                + "(" + ProtocolProviderFactory.USER_ID
                      + "="+ IcqSlickFixture.icqAccountID.getUserID() + ")"
                + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            fail("We apparently got our filter wrong: " + ex.getMessage());
        }

        //make sure we didn't see a service
        assertTrue("A Protocol Provider Service was still regged as an osgi service "
                      +"for ICQ UIN:" + IcqSlickFixture.icqAccountID
                      + "After it was explicitly uninstalled"
                      ,icqProviderRefs == null || icqProviderRefs.length == 0);

        //verify that the provider factory knows that we have uninstalled the
        //provider.
        assertTrue(
          "The ICQ provider factory kept a reference to the provider we just "
          +"uninstalled (accID="+IcqSlickFixture.icqAccountID+")",
          fixture.providerFactory.getRegisteredAccounts().isEmpty()
          && fixture.providerFactory.getProviderForAccount(IcqSlickFixture.icqAccountID)
              == null);


        //Now reinstall the bundle
        providerBundle = IcqSlickFixture.bc.installBundle(providerBundle.getLocation());

        //set the global providerBundle reference that we will be using
        //in the last series of tests (Account uninstallation persistency)
        IcqSlickFixture.providerBundle = providerBundle;


        assertEquals("Couldn't re-install protocol provider bundle."
                     , Bundle.INSTALLED, providerBundle.getState());

        AccountManagerUtils.startBundleAndWaitStoredAccountsLoaded(
            IcqSlickFixture.bc, providerBundle, ProtocolNames.ICQ);
        assertEquals("Couldn't re-start protocol provider bundle."
                     , Bundle.ACTIVE, providerBundle.getState());

        //Make sure that the provider is there again.
        //verify that the provider is no longer available
        try
        {
            icqProviderRefs = IcqSlickFixture.bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&"
                + "(" + ProtocolProviderFactory.PROTOCOL
                      + "=" +ProtocolNames.ICQ + ")"
                + "(" + ProtocolProviderFactory.USER_ID
                      + "="+ IcqSlickFixture.icqAccountID.getUserID() + ")"
                + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            fail("We apparently got our filter wrong: " + ex.getMessage());
        }

        //make sure we didn't see a service
        assertTrue("A Protocol Provider Service was not restored after being"
                      +"reinstalled. ICQ UIN:" + IcqSlickFixture.icqAccountID
                      ,icqProviderRefs != null && icqProviderRefs.length > 0);

        ServiceReference[] icqFactoryRefs = null;
        try
        {
            icqFactoryRefs = IcqSlickFixture.bc.getServiceReferences(
                ProtocolProviderFactory.class.getName(),
                "(" + ProtocolProviderFactory.PROTOCOL
                      + "=" +ProtocolNames.ICQ + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            fail("We apparently got our filter wrong: " + ex.getMessage());
        }

        //we're the ones who've reinstalled the factory so it's our
        //responsibility to update the fixture.
        fixture.providerFactory
            = (ProtocolProviderFactory)IcqSlickFixture.bc.getService(icqFactoryRefs[0]);
        fixture.provider
            = (ProtocolProviderService)IcqSlickFixture.bc.getService(icqProviderRefs[0]);
        IcqSlickFixture.icqAccountID
            = fixture.provider.getAccountID();


        //verify that the provider is also restored in the provider factory
        //itself
        assertTrue(
          "The ICQ provider did not restore its own reference to the provider "
          +"that we just reinstalled (accID="+IcqSlickFixture.icqAccountID+")",
          !fixture.providerFactory.getRegisteredAccounts().isEmpty()
          && fixture.providerFactory.getProviderForAccount(IcqSlickFixture.icqAccountID)
              != null);
    }
    /**
     * Uinstalls our test account and makes sure it really has been removed.
     */
    public void testUninstallAccount()
    {
        assertFalse("No installed accounts found"
                    ,fixture.providerFactory.getRegisteredAccounts().isEmpty());

        assertNotNull(
            "Found no provider corresponding to account ID "
            + IcqSlickFixture.icqAccountID
            ,fixture.providerFactory.getProviderForAccount(IcqSlickFixture.icqAccountID));

        assertTrue(
            "Failed to remove a provider corresponding to acc id "
            + IcqSlickFixture.icqAccountID
            , fixture.providerFactory.uninstallAccount(IcqSlickFixture.icqAccountID));

        ServiceReference[] icqProviderRefs = null;
        try
        {
            icqProviderRefs = IcqSlickFixture.bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&"
                + "(" + ProtocolProviderFactory.PROTOCOL
                      + "=" +ProtocolNames.ICQ + ")"
                + "(" + ProtocolProviderFactory.USER_ID
                      + "="+ IcqSlickFixture.icqAccountID.getUserID() + ")"
                + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            fail("We apparently got our filter wrong: " + ex.getMessage());
        }

        //make sure we didn't see a service
        assertTrue("A Protocol Provider Service was still regged as an osgi service "
                      +"for ICQ UIN:" + IcqSlickFixture.icqAccountID
                      + "After it was explicitly uninstalled"
                      ,icqProviderRefs == null || icqProviderRefs.length == 0);

        //verify that the provider factory knows that we have uninstalled the
        //provider.
        assertTrue(
          "The ICQ provider factory kept a reference to the provider we just "
          +"uninstalled (accID="+IcqSlickFixture.icqAccountID+")"
          , fixture.providerFactory.getRegisteredAccounts().isEmpty()
          && fixture.providerFactory.getProviderForAccount(IcqSlickFixture.icqAccountID)
              == null);
    }

    /**
     * A class that would plugin as a registration listener to a protocol
     * provider and simply record all events that it sees and notify the
     * registrationLock if it sees an event that notifies us of a completed
     * registration.
     */
    public class RegistrationEventCollector implements RegistrationStateChangeListener
    {
        RegistrationState stateRecieved = null;
        int eventReason = -1;

        /**
         * The method would simply register all received events so that they
         * could be available for later inspection by the unit tests. In the
         * case where a registraiton event notifying us of a completed
         * registration is seen, the method would call notifyAll() on the
         * registrationLock.
         *
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            logger.debug("Received a RegistrationStateChangeEvent: " + evt);

            if(evt.getNewState().equals( RegistrationState.UNREGISTERED))
            {
                logger.debug("Connection FAILED!");
                stateRecieved = evt.getNewState();
                eventReason = evt.getReasonCode();

                synchronized(registrationLock)
                {
                    logger.debug(".");
                    registrationLock.notifyAll();
                    logger.debug(".");
                }
            }
        }
    }
}
