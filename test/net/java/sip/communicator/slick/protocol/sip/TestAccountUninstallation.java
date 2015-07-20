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
package net.java.sip.communicator.slick.protocol.sip;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Tests whether accaounts are uninstalled properly. It is important that
 * tests from this class be called last since they will install the accounts
 * that have been used to test the implementations. Apart from uninstallation
 * tests the class also contains tests that remove and reinstall the protocol
 * provider bundle in order to verify that accounts are persistent.
 *
 * @author Emil Ivov
 */
public class TestAccountUninstallation
    extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestAccountUninstallation.class);

    private SipSlickFixture fixture = new SipSlickFixture();

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

        suite.addTest(
            new TestAccountUninstallation("testProviderUnregister"));
        suite.addTest(
            new TestAccountUninstallation("testInstallationPersistency"));
        suite.addTest(
            new TestAccountUninstallation("testUninstallAccount"));


        return suite;
    }

    /**
     * Unregisters both providers and verifies whether they have changed state
     * accordingly.
     *
     * @throws OperationFailedException if unregister fails with an error.
     */
    public void testProviderUnregister()
        throws OperationFailedException
    {
        //make sure providers are still registered
        assertEquals(fixture.provider1.getRegistrationState()
                     , RegistrationState.REGISTERED);
        assertEquals(fixture.provider2.getRegistrationState()
                     , RegistrationState.REGISTERED);

        UnregistrationEventCollector collector1
            = new UnregistrationEventCollector();
        UnregistrationEventCollector collector2
            = new UnregistrationEventCollector();

        fixture.provider1.addRegistrationStateChangeListener(collector1);
        fixture.provider2.addRegistrationStateChangeListener(collector2);

        //unregister both providers
        fixture.provider1.unregister();
        fixture.provider2.unregister();

        collector1.waitForEvent(10000);
        collector2.waitForEvent(10000);

        assertTrue("Provider did not distribute unregister events"
                     , 2
                     <= collector1.collectedNewStates.size());
        assertTrue("Provider did not distribute unregister events"
                     , 2
                     <= collector2.collectedNewStates.size());

        //make sure both providers are now unregistered.
        assertEquals("Provider state after calling unregister()."
                     , RegistrationState.UNREGISTERED
                     , fixture.provider1.getRegistrationState());
        assertEquals("Provider state after calling unregister()."
                     , RegistrationState.UNREGISTERED
                     , fixture.provider2.getRegistrationState());
    }

    /**
     * Stops and removes the tested bundle, verifies that it has unregistered
     * its provider, then reloads and restarts the bundle and verifies that
     * the protocol provider is reRegistered in the bundle context.
     *
     * @throws java.lang.Exception if an exception occurs during testing.
     */
    public void testInstallationPersistency() throws Exception
    {
        Bundle providerBundle
            = SipSlickFixture.findProtocolProviderBundle(fixture.provider1);

        //set the global providerBundle reference that we will be using
        //in the last series of tests (Account uninstallation persistence)
        SipSlickFixture.providerBundle = providerBundle;

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
        ServiceReference[] sipProviderRefs = null;
        try
        {
            sipProviderRefs = SipSlickFixture.bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&"
                + "(" + ProtocolProviderFactory.PROTOCOL
                      + "=" +ProtocolNames.SIP + ")"
                + "(" + ProtocolProviderFactory.USER_ID
                      + "="+ fixture.userID1 + ")"
                + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            fail("We apparently got our filter wrong: " + ex.getMessage());
        }

        //make sure we didn't see a service
        assertTrue("A Protocol Provider Service was still regged as an osgi service "
                      +"for SIP URI:" + fixture.userID1
                      + "After it was explicitly uninstalled"
                      ,sipProviderRefs == null || sipProviderRefs.length == 0);

        //verify that the provider factory knows that we have uninstalled the
        //provider.
        assertTrue(
          "The SIP provider factory kept a reference to the provider we just "
          +"uninstalled (uri="+fixture.userID1+")",
          fixture.providerFactory.getRegisteredAccounts().isEmpty()
          && fixture.providerFactory.getProviderForAccount(
                fixture.provider1.getAccountID())
              == null);

        //Now reinstall the bundle
        providerBundle = SipSlickFixture.bc.installBundle(providerBundle.getLocation());

        //set the global providerBundle reference that we will be using
        //in the last series of tests (Account uninstallation persistency)
        SipSlickFixture.providerBundle = providerBundle;

        assertEquals("Couldn't re-install protocol provider bundle."
                     , Bundle.INSTALLED, providerBundle.getState());

        AccountManagerUtils.startBundleAndWaitStoredAccountsLoaded(SipSlickFixture.bc,
            providerBundle, ProtocolNames.SIP);
        assertEquals("Couldn't re-start protocol provider bundle."
                     , Bundle.ACTIVE, providerBundle.getState());

        //Make sure that the provider is there again.
        //verify that the provider is no longer available
        try
        {
            sipProviderRefs = SipSlickFixture.bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&"
                + "(" + ProtocolProviderFactory.PROTOCOL
                      + "=" +ProtocolNames.SIP + ")"
                + "(" + ProtocolProviderFactory.USER_ID
                      + "="+ fixture.userID1 + ")"
                + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            fail("We apparently got our filter wrong " + ex.getMessage());
        }

        //make sure we didn't see a service
        assertTrue("A Protocol Provider Service was not restored after being"
                      +"reinstalled. SIP URI:" + fixture.userID1
                      ,sipProviderRefs != null && sipProviderRefs.length > 0);

        ServiceReference[] sipFactoryRefs = null;
        try
        {
            sipFactoryRefs = SipSlickFixture.bc.getServiceReferences(
                ProtocolProviderFactory.class.getName(),
                "(" + ProtocolProviderFactory.PROTOCOL
                      + "=" +ProtocolNames.SIP + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            fail("We apparently got our filter wrong " + ex.getMessage());
        }

        //we're the ones who've reinstalled the factory so it's our
        //responsibility to update the fixture.
        fixture.providerFactory
            = (ProtocolProviderFactory)SipSlickFixture.bc.getService(sipFactoryRefs[0]);
        fixture.provider1
            = (ProtocolProviderService)SipSlickFixture.bc.getService(sipProviderRefs[0]);


        //verify that the provider is also restored in the provider factory
        //itself
        assertTrue(
          "The SIP provider did not restore its own reference to the provider "
          +"that we just reinstalled (URI="+fixture.userID1+")",
          !fixture.providerFactory.getRegisteredAccounts().isEmpty()
          && fixture.providerFactory.getProviderForAccount(
                fixture.provider1.getAccountID())
              != null);

    }

    /**
     * Uinstalls our test account and makes sure it really has been removed.
     *
     */
    public void testUninstallAccount()
    {
        assertFalse("No installed accounts found",
                    fixture.providerFactory.getRegisteredAccounts().isEmpty());

        assertNotNull(
            "Found no provider corresponding to URI " + fixture.userID1
            ,fixture.providerFactory.getProviderForAccount(
                fixture.provider1.getAccountID()));

        assertTrue(
            "Failed to remove a provider corresponding to URI "
            + fixture.userID1
            ,fixture.providerFactory.uninstallAccount(
                fixture.provider1.getAccountID()));
        assertTrue(
            "Failed to remove a provider corresponding to URI "
            + fixture.userID1
            ,fixture.providerFactory.uninstallAccount(
                fixture.provider2.getAccountID()));

        //make sure no providers have remained installed.
        ServiceReference[] sipProviderRefs = null;
        try
        {
            sipProviderRefs = SipSlickFixture.bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(" + ProtocolProviderFactory.PROTOCOL
                      + "=" +ProtocolNames.SIP + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            fail("We apparently got our filter wrong " + ex.getMessage());
        }

        //make sure we didn't see a service
        assertTrue("A Protocol Provider Service was still regged as an osgi "
                      + "service for SIP URI:" + fixture.userID1
                      + "After it was explicitly uninstalled"
                      ,sipProviderRefs == null || sipProviderRefs.length == 0);

        //verify that the provider factory knows that we have uninstalled the
        //provider.
        assertTrue(
          "The SIP provider factory kept a reference to the provider we just "
          +"uninstalled (uri="+fixture.userID1+")",
          fixture.providerFactory.getRegisteredAccounts().isEmpty()
          && fixture.providerFactory.getProviderForAccount(
                fixture.provider1.getAccountID())
              == null);

    }

    /**
     * A class that would plugin as a registration listener to a protocol
     * provider and simply record all events that it sees and notifyAll()
     *  if it sees an event that notifies us of a completed
     * registration.
     */
    public class UnregistrationEventCollector
        implements RegistrationStateChangeListener
    {
        public List<RegistrationState> collectedNewStates = new LinkedList<RegistrationState>();

        /**
         * The method would simply register all received events so that they
         * could be available for later inspection by the unit tests. In the
         * case where a registraiton event notifying us of a completed
         * registration is seen, the method would call notifyAll().
         *
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            logger.debug("Received a RegistrationStateChangeEvent: " + evt);

            collectedNewStates.add(evt.getNewState());

            if (evt.getNewState().equals(RegistrationState.UNREGISTERED))
            {
                logger.debug("We're registered and will notify those who wait");
                synchronized (this)
                {
                    notifyAll();
                }
            }
        }

        /**
         * Blocks until an event notifying us of the awaited state change is
         * received or until waitFor miliseconds pass (whichever happens first).
         *
         * @param waitFor the number of miliseconds that we should be waiting
         * for an event before simply bailing out.
         */
        public void waitForEvent(long waitFor)
        {
            logger.trace("Waiting for a RegistrationStateChangeEvent");

            synchronized (this)
            {
                if (collectedNewStates.contains(RegistrationState.UNREGISTERED))
                {
                    logger.trace("Event already received. "
                                 + collectedNewStates);
                    return;
                }

                try
                {
                    wait(waitFor);

                    if (collectedNewStates.size() > 0)
                        logger.trace(
                            "Received a RegistrationStateChangeEvent.");
                    else
                        logger.trace(
                            "No RegistrationStateChangeEvent received for "
                            + waitFor + "ms.");

                }
                catch (InterruptedException ex)
                {
                    logger.debug(
                        "Interrupted while waiting for a "
                        + "RegistrationStateChangeEvent"
                        , ex);
                }
            }
        }
    }
}
